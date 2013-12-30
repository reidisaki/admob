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

- (void)disableContentAirPlay {
    moviePlayerController.allowsAirPlay = NO;
}

- (void)enableContentAirPlay {
    moviePlayerController.allowsAirPlay = YES;
}

- (void)loadView {
	[super loadView];
	
	self.view.backgroundColor = [UIColor whiteColor];
	self.view.autoresizingMask = (UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight);
	moviePlayerSuperview = [[UIView alloc] initWithFrame:FWPLAYER_MOVIE_PLAYER_SUPERVIEW_FRAME];
	moviePlayerSuperview.backgroundColor = [UIColor blackColor];
	[self.view addSubview:moviePlayerSuperview];
    
    _adVideoView = [[UIView alloc] initWithFrame:moviePlayerSuperview.bounds];
    [moviePlayerSuperview addSubview:_adVideoView];
    
    _airplayView = [[UIImageView alloc] initWithImage:[UIImage imageNamed:@"airplay.png"]];
    _airplayView.frame = moviePlayerSuperview.bounds;
    _airplayView.contentMode = UIViewContentModeScaleAspectFill;
    _airplayView.autoresizingMask = (UIViewAutoresizingFlexibleBottomMargin|UIViewAutoresizingFlexibleHeight|UIViewAutoresizingFlexibleLeftMargin|UIViewAutoresizingFlexibleRightMargin|UIViewAutoresizingFlexibleTopMargin|UIViewAutoresizingFlexibleWidth);
	
	[self submitAdRequest];
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
    [adContext setVideoDisplayBase:_adVideoView];
	
	//regist callback handler for ad request complete event
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onAdRequestCompleted:) name:FW_NOTIFICATION_REQUEST_COMPLETE object:adContext];
	
	//submit ad request to the Freewheel ad server
	[adContext submitRequestWithTimeout:FWPLAYER_AD_REQUEST_TIMEOUT];
}

- (void)_onAdRequestCompleted:(NSNotification *)notification {
	[temporalSlots addObjectsFromArray:[adContext getSlotsByTimePositionClass:FW_TIME_POSITION_CLASS_PREROLL]];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onAdSlotEnded:) name:FW_NOTIFICATION_SLOT_ENDED object:adContext];
    [self disableContentAirPlay];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onAirPlayStarted:) name:FW_NOTIFICATION_SLOT_EXTERNAL_PLAYBACK_STARTED object:adContext];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onAirPlayStopped:) name:FW_NOTIFICATION_SLOT_EXTERNAL_PLAYBACK_STOPPED object:adContext];
	[self _playPrerollAdSlots];
}

- (void)_onAirPlayStarted:(NSNotification *)notification {
    if (notification) {
         NSLog(@"Ad AirPlay started");
    }
    [_airplayView removeFromSuperview];
    [moviePlayerSuperview addSubview:_airplayView];
}

- (void)_onAirPlayStopped:(NSNotification *)notification {
    if (notification) {
        NSLog(@"Ad AirPlay stopped");
    }
    [_airplayView removeFromSuperview];
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

- (void)_prepareMidrollSlots {
    [temporalSlots removeAllObjects];
    [temporalSlots addObjectsFromArray:[adContext getSlotsByTimePositionClass:FW_TIME_POSITION_CLASS_MIDROLL]];
}

- (void)startTimer{
	_timer = [NSTimer scheduledTimerWithTimeInterval:TIMELINE_TIMER_INTERVAL target:self selector:@selector(_onTimer:) userInfo:nil repeats:YES];
}

- (void)stopTimer{
	[_timer invalidate];
	_timer = nil;
}

- (void)_onTimer:(NSTimer*)timer{
	NSTimeInterval currentPlayheadTime = [moviePlayerController currentPlaybackTime];
    
	for (uint i = 0; i < temporalSlots.count; i++){
        NSTimeInterval cuepoint = [[temporalSlots objectAtIndex:i] timePosition];
        NSLog(@"%f %f", cuepoint, currentPlayheadTime);
		if (fabs(cuepoint - currentPlayheadTime) < TIMELINE_TIMER_INTERVAL){
            NSLog(@"Firing CuePoint at time %f", cuepoint);
            id<FWSlot> slot = [temporalSlots objectAtIndex:i];
            [temporalSlots removeObjectAtIndex:i];
            [slot play];
			return;
		}
	}
}

- (void)_onContentPauseRequest:(NSNotification *)notification{
	[self _pauseContentMovie];
	[adContext setVideoState:FW_VIDEO_STATE_PAUSED];
}

- (void)_onContentResumeRequest:(NSNotification *)notification{
	[self _resumeContentMovie];
	[adContext setVideoState:FW_VIDEO_STATE_PLAYING];
}

//all FreeWheel slots will send SlotEnded notification when it ends
- (void)_onAdSlotEnded:(NSNotification *)notification {
    NSLog(@"Slot ended");
	if (FW_TIME_POSITION_CLASS_PREROLL == [[adContext getSlotByCustomId:[[notification userInfo] objectForKey:FW_INFO_KEY_CUSTOM_ID]] timePositionClass]) {
        // When the preroll slot ends, play content video.
		[self _playNextPreroll];
	} else if (FW_TIME_POSITION_CLASS_POSTROLL == [[adContext getSlotByCustomId:[[notification userInfo] objectForKey:FW_INFO_KEY_CUSTOM_ID]] timePositionClass]) {
        [self _playNextPostroll];
	}
}

- (void)_playContentMovie {
    NSLog(@"Playing content video...");
    [_adVideoView removeFromSuperview];
    moviePlayerController = [[MPMoviePlayerController alloc] initWithContentURL:FWPLAYER_CONTENT_VIDEO_URL];
	moviePlayerController.view.frame = moviePlayerSuperview.bounds;
    [moviePlayerSuperview addSubview:moviePlayerController.view];
    [self enableContentAirPlay];
    
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onMovieLoadStateChanged:) name:MPMoviePlayerLoadStateDidChangeNotification object:moviePlayerController];
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onMoviePlaybackStateDidChange:) name:MPMoviePlayerPlaybackStateDidChangeNotification object:moviePlayerController];
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onMoviePlaybackFinished:) name:MPMoviePlayerPlaybackDidFinishNotification object:moviePlayerController];
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onAirPlayStateChanged) name:MPMoviePlayerIsAirPlayVideoActiveDidChangeNotification object:moviePlayerController];
    
    [self _prepareMidrollSlots];
    [self startTimer];
    
    // When AdManager is about to play a linear slot, it instructs the player to temporarily pause itself.
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onContentPauseRequest:) name:FW_NOTIFICATION_CONTENT_PAUSE_REQUEST object:adContext];
	// When AdManager has finished playing a linear slot, it instructs the player to resume.
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onContentResumeRequest:) name:FW_NOTIFICATION_CONTENT_RESUME_REQUEST object:adContext];
    
    [moviePlayerController play];
}

- (void)_pauseContentMovie {
    NSLog(@"Pause content video...");
    [self disableContentAirPlay];
	[moviePlayerController pause];
    [moviePlayerController.view removeFromSuperview];
    [moviePlayerSuperview addSubview:_adVideoView];
}

- (void)_resumeContentMovie {
    NSLog(@"Resume content video");
    [_adVideoView removeFromSuperview];
    [moviePlayerSuperview addSubview:moviePlayerController.view];
    [self enableContentAirPlay];
    [moviePlayerController play];
}

- (void)_onMovieLoadStateChanged:(NSNotification *)notification {
	NSLog(@"_onMovieLoadStateChanged %@ %d", notification, [moviePlayerController loadState]);
	if ([moviePlayerController loadState] & MPMovieLoadStatePlayable) {
		//this notification might be triggered several times. only need to handle the first playable notification
		[[NSNotificationCenter defaultCenter] removeObserver:self name:MPMoviePlayerLoadStateDidChangeNotification object:moviePlayerController];
		[adContext setVideoState:FW_VIDEO_STATE_PLAYING];
		if (moviePlayerController.airPlayVideoActive) {
		    [self _onAirPlayStarted:nil];
		}
	}
}

- (void)_onAirPlayStateChanged:(NSNotification *)notification {
	NSLog(@"_onAirPlayStateChanged %@", notification);
	if (moviePlayerController.airPlayVideoActive) {
		[self _onAirPlayStarted:nil];
	} else {
		[self _onAirPlayStopped:nil];
	}
}

- (void)_onMoviePlaybackFinished:(NSNotification *)notification {
	NSLog(@"_onMoviePlaybackFinished %@", notification);
	[[NSNotificationCenter defaultCenter] removeObserver:self name:MPMoviePlayerPlaybackDidFinishNotification object:moviePlayerController];
	[adContext setVideoState:FW_VIDEO_STATE_COMPLETED];
    [self _pauseContentMovie];
    [self stopTimer];
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
}

@end
