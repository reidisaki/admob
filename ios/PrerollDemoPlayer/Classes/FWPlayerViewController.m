#import "FWPlayerViewController.h"

@implementation FWPlayerViewController

@synthesize moviePlayerSuperview;
@synthesize moviePlayerController;
@synthesize adContext;
@synthesize temporalSlots;

- (void)dealloc {
	[[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation{
	return YES;
}

- (void)loadView {
	[super loadView];
	
	self.view.backgroundColor = [UIColor whiteColor];
	self.view.autoresizingMask = (UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight);
	moviePlayerSuperview = [[UIView alloc] initWithFrame:FWPLAYER_MOVIE_PLAYER_SUPERVIEW_FRAME];
	moviePlayerSuperview.backgroundColor = [UIColor blackColor];
	[self.view addSubview:moviePlayerSuperview];
	
	[self submitAdRequest];
}

- (void)_loadMoviePlayerCover {
	UIImage *image = [[UIImage alloc] initWithContentsOfFile:[[NSBundle mainBundle] pathForResource:@"play" ofType:@"png"]];
	UIImageView *imageView = [[UIImageView alloc] initWithImage:image];
	UIControl *imageControl = [[UIControl alloc] initWithFrame:imageView.frame];
	[imageControl addTarget:self action:@selector(_playMovie) forControlEvents:UIControlEventTouchUpInside];
	[moviePlayerSuperview addSubview:imageView];
	[moviePlayerSuperview addSubview:imageControl];
}

- (void)submitAdRequest {
	self.temporalSlots = [NSMutableArray array];
	
	//Set current viewController object to AdManager if you are also using AdMob Renderer or Millennial Renderer.
	//[adManager setCurrentViewController:self];
	
	//create new adContext for each playback; the adContext object is used to gather info for ad request, send ad request to ad server,
	//parse ad info from ad response and take controll of all ads play
	adContext = [adManager newContext];
	
	//contact FreeWheel Integration Support Engineer about configuration of profiles/slots/keyvalue for your integration.
	[adContext setPlayerProfile:@"96749:global-cocoa" defaultTemporalSlotProfile:nil defaultVideoPlayerSlotProfile:nil defaultSiteSectionSlotProfile:nil];
	[adContext setSiteSectionId:@"ios" idType:FW_ID_TYPE_CUSTOM pageViewRandom:0 networkId:0 fallbackId:0];
	[adContext setVideoAssetId:@"ios_pre" idType:FW_ID_TYPE_CUSTOM duration:160 durationType:FW_VIDEO_ASSET_DURATION_TYPE_EXACT location:nil autoPlayType:true videoPlayRandom:0 networkId:0 fallbackId:0];
	//tell the ad context on which base view object to render video ads
    [adContext setVideoDisplayBase:moviePlayerSuperview];
	
	//regist callback handler for ad request complete event
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onAdRequestCompleted:) name:FW_NOTIFICATION_REQUEST_COMPLETE object:adContext];
	
	//submit ad request to the Freewheel ad server
	[adContext submitRequestWithTimeout:FWPLAYER_AD_REQUEST_TIMEOUT];
}

- (void)_playMovie {
	for (UIView *view in moviePlayerSuperview.subviews) {
		[view removeFromSuperview];
	}
	
	moviePlayerController = [[MPMoviePlayerController alloc] initWithContentURL:FWPLAYER_CONTENT_VIDEO_URL];
	moviePlayerController.view.frame = FWPLAYER_MOVIE_PLAYER_VIEW_FRAME;
	[moviePlayerSuperview addSubview:[moviePlayerController view]];
	
	// Set your player to fullscreen if you need preroll ads played in fullscreen
	//[moviePlayerController setFullscreen:YES];
	
	//register callback handler for ad slot playback complete event
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onAdSlotEnded:) name:FW_NOTIFICATION_SLOT_ENDED object:adContext];
	[self _playPrerollAdSlots];
}

- (void)_onAdRequestCompleted:(NSNotification *)notification {
	[self _loadMoviePlayerCover];
	
	[temporalSlots addObjectsFromArray:[adContext getSlotsByTimePositionClass:FW_TIME_POSITION_CLASS_PREROLL]];
	for (id<FWSlot> slot in temporalSlots) {
		[slot preload];
	}
}

- (void)_playNextPreroll {
	if (temporalSlots.count > 0) {
		id<FWSlot> slot = [temporalSlots objectAtIndex:0];
		[temporalSlots removeObjectAtIndex:0];
		[slot play];
	} else {
		//if there is no preroll slot, start the content video
		[self _playContentMovie];
	}
}

- (void)_playNextPostroll {
	if (temporalSlots.count > 0) {
		id<FWSlot> slot = [temporalSlots objectAtIndex:0];
		[temporalSlots removeObjectAtIndex:0];
		[slot play];
	} else {
		//if there is no postroll slot, clean up
		[self _cleanup];
	}
}

- (void)_playPrerollAdSlots {
    [self _playNextPreroll];
}

- (void)_playPostrollAdSlots {
	[temporalSlots removeAllObjects];
	[temporalSlots addObjectsFromArray:[adContext getSlotsByTimePositionClass:FW_TIME_POSITION_CLASS_POSTROLL]];
    [self _playNextPostroll];
}

//all FreeWheel slots will send SlotEnded notification when it ends
- (void)_onAdSlotEnded:(NSNotification *)notification {
	if (FW_TIME_POSITION_CLASS_PREROLL == [[adContext getSlotByCustomId:[[notification userInfo] objectForKey:FW_INFO_KEY_CUSTOM_ID]] timePositionClass]) {
        // When the preroll slot ends, play content video.
		[self _playNextPreroll];
	} else if (FW_TIME_POSITION_CLASS_POSTROLL == [[adContext getSlotByCustomId:[[notification userInfo] objectForKey:FW_INFO_KEY_CUSTOM_ID]] timePositionClass]) {
        [self _playNextPostroll];
	}
}

- (void)_playContentMovie {
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onMovieLoadStateChanged:) name:MPMoviePlayerLoadStateDidChangeNotification object:moviePlayerController];
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onMoviePlaybackStateDidChange:) name:MPMoviePlayerPlaybackStateDidChangeNotification object:moviePlayerController];
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onMoviePlaybackFinished:) name:MPMoviePlayerPlaybackDidFinishNotification object:moviePlayerController];
	[moviePlayerController play];
}

- (void)_onMovieLoadStateChanged:(NSNotification *)notification {
	NSLog(@"_onMovieLoadStateChanged %@ %d", notification, [moviePlayerController loadState]);	
	if ([moviePlayerController loadState] & MPMovieLoadStatePlayable) {
		//this notification might be triggered several times. only need to handle the first playable notification
		[[NSNotificationCenter defaultCenter] removeObserver:self name:MPMoviePlayerLoadStateDidChangeNotification object:moviePlayerController];
		[adContext setVideoState:FW_VIDEO_STATE_PLAYING];
	}
}

- (void)_onMoviePlaybackFinished:(NSNotification *)notification {
	NSLog(@"_onMoviePlaybackFinished %@", notification);	
	[[NSNotificationCenter defaultCenter] removeObserver:self name:MPMoviePlayerPlaybackDidFinishNotification object:moviePlayerController];
	[adContext setVideoState:FW_VIDEO_STATE_COMPLETED];
	[self _playPostrollAdSlots];
}

- (void)_onMoviePlaybackStateDidChange:(NSNotification *)notification {
	NSLog(@"_onMoviePlaybackStateDidChange %d", moviePlayerController.playbackState);
	if (moviePlayerController.playbackState == MPMoviePlaybackStatePlaying) {
		[adContext setVideoState:FW_VIDEO_STATE_PLAYING];
	} else if (moviePlayerController.playbackState == MPMoviePlaybackStatePaused) {
		[adContext setVideoState:FW_VIDEO_STATE_PAUSED];
	}
}

- (void)_cleanupFreeWheel {
	//tell adManager to release the ViewController
	[adManager setCurrentViewController:nil];
	[[NSNotificationCenter defaultCenter] removeObserver:self name:nil object:adContext];	
	self.adContext = nil;
}

- (void)_cleanup {
	if (moviePlayerController) {
		[[moviePlayerController view] removeFromSuperview];
		[[NSNotificationCenter defaultCenter] removeObserver:self name:nil object:moviePlayerController];
		[moviePlayerController stop];
		self.moviePlayerController = nil;
	}
	[self _cleanupFreeWheel];
	[self _loadMoviePlayerCover];
}

@end
