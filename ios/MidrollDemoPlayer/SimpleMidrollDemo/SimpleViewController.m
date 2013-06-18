//
//  SimpleViewController.m
//  SimpleMidrollDemo
//
//  Created by Stone Wang on 1/15/13.
//  Copyright (c) 2013 FreeWheel. All rights reserved.
//

#import "SimpleViewController.h"

@interface SimpleViewController ()

- (void)_onAdRequestCompleted:(NSNotification *)notification;
- (void)_onContentPauseRequest:(NSNotification *)notification;
- (void)_onContentResumeRequest:(NSNotification *)notification;
- (void)_onCuePoint:(NSNotification *)notification;
@end

@implementation SimpleViewController

- (void)dealloc{
	[_player stop];
}

- (void)viewDidLoad
{
    [super viewDidLoad];
	// Content video player initialization
	_player = [[MPMoviePlayerController alloc] initWithContentURL:[NSURL URLWithString:@"http://playerdemo.freewheel.tv/www/resource/content/152s.m4v"]];
	_player.view.frame = CGRectMake(10, 10, 400, 300);
	[self.view addSubview:_player.view];
	
	// Create a timeline for the player
	_timeline = [[FWTimeline alloc] initWithMediaPlayer:_player];
	
	/* Create a new adContext for each playback; the adContext object is used to gather info for ad request, send ad request to ad server, parse ad info from ad response and take controll of all ads play. */
	_adContext = [adManager newContext];
	
	// Please contact FreeWheel Integration Support Engineer about configuration of profiles/slots/keyvalue for your integration.
	[_adContext setPlayerProfile:@"42015:idn_ios" defaultTemporalSlotProfile:nil defaultVideoPlayerSlotProfile:nil defaultSiteSectionSlotProfile:nil];
	[_adContext setSiteSectionId:@"ios" idType:FW_ID_TYPE_CUSTOM pageViewRandom:0 networkId:0 fallbackId:0];
	[_adContext setVideoAssetId:@"ios_pre" idType:FW_ID_TYPE_CUSTOM duration:160 durationType:FW_VIDEO_ASSET_DURATION_TYPE_EXACT location:nil autoPlayType:true videoPlayRandom:0 networkId:0 fallbackId:0];
	// Tell the ad context on which base UIView object to render video/overlay ads
    [_adContext setVideoDisplayBase:_player.view];
	
	// Handle FW_NOTIFICATION_REQUEST_COMPLETE notification.
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onAdRequestCompleted:) name:FW_NOTIFICATION_REQUEST_COMPLETE object:_adContext];
	// When AdManager is about to play a linear slot, it instructs the player to temporarily pause itself.
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onContentPauseRequest:) name:FW_NOTIFICATION_CONTENT_PAUSE_REQUEST object:_adContext];
	// When AdManager has finished playing a linear slot, it instructs the player to resume.
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onContentResumeRequest:) name:FW_NOTIFICATION_CONTENT_RESUME_REQUEST object:_adContext];
	// FWTimeline will fire NOTIFICATION_CUEPOINT_REACHED notification when the player's playhead time has reached the cuepoint's time position.
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onCuePoint:) name:NOTIFICATION_CUEPOINT_REACHED object:_timeline];
	// Submit ad request to the Freewheel ad server
	[_adContext submitRequestWithTimeout:5];
}

- (void)_onAdRequestCompleted:(NSNotification *)notification{
	if ([notification object] == _adContext){
		if ([[notification userInfo] objectForKey:FW_INFO_KEY_ERROR]){
			// Request has failed, start movie player without ads
			[_player play];
		}
		else{
			// Request was successful. Add the overlay and midroll slots as cuepoints to the timeline.
			id<FWSlot> slot;
			for (slot in [_adContext getSlotsByTimePositionClass:FW_TIME_POSITION_CLASS_MIDROLL]) {
				[_timeline addCuePointForSlot:slot];
			}
			// To adjust the overlay position, refer to iOS HTML Renderer parameters: http://techhub.freewheel.tv/display/techdocs/iOS+HTML+Renderer
			for (slot in [_adContext getSlotsByTimePositionClass:FW_TIME_POSITION_CLASS_OVERLAY]) {
				[_timeline addCuePointForSlot:slot];
			}
			
			// Start the playback
			[_player play];
			// Start the timeline along with the player
			[_timeline startTimer];
			/* Tell adContext that the video has started. Setting the correct video state at the right time is crucial for FW_NOTIFICATION_CONTENT_PAUSE_REQUEST and FW_NOTIFICATION_CONTENT_RESUME_REQUEST to work properly. */
			[_adContext setVideoState:FW_VIDEO_STATE_PLAYING];
		}
	}
}

- (void)_onCuePoint:(NSNotification *)notification{
	NSLog(@"Cuepoint fired for slot %@", [notification.userInfo objectForKey:FW_INFO_KEY_CUSTOM_ID]);
	id<FWSlot> slot = [_adContext getSlotByCustomId:[notification.userInfo objectForKey:FW_INFO_KEY_CUSTOM_ID]];
	[slot play];
}

- (void)_onContentPauseRequest:(NSNotification *)notification{
	// Linear slot is about to start, pause the player temporarily.
	[_player pause];
	[_adContext setVideoState:FW_VIDEO_STATE_PAUSED];
}

- (void)_onContentResumeRequest:(NSNotification *)notification{
	// Linear slot has finished, resume.
	[_player play];
	[_adContext setVideoState:FW_VIDEO_STATE_PLAYING];
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation{
	return YES;
}

@end
