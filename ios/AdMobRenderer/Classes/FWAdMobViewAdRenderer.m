#import "FWAdMobViewAdRenderer.h"
#import "FWAdMobConstants.h"

@implementation FWAdMobViewAdRenderer

@synthesize adMobAd = _adMobAd;
@synthesize durationTimer;

- (void)dealloc { 
	FWLog(@"");
	_adMobAd.delegate = nil;
	self.durationTimer = nil;
	[_adMobAd removeFromSuperview];
	[_adMobAd release];
	[super dealloc];
}

#pragma mark -
#pragma mark FWRenderer Methods

- (id)initWithRendererController:(id<FWRendererController>)rendererController {
	FWLog(@"");
	if (!rendererController) {
		return nil;
	}
	if ((self = [super initWithRendererController:rendererController])) {
		durationTimer = nil;
		[_rendererController setCapability:FW_EVENT_AD_CLICK status:FW_CAPABILITY_STATUS_OFF];
	}
	
	return self;
}


- (void)start {
	if ([self checkParameter] == NO) {
		NSString *errorCode = FW_ERROR_MISSING_PARAMETER;
		if ([_errorInfo isEqualToString:FW_ERROR_MSG_MISSING_CURRENT_VIEW_CONTROLLER]) {
			errorCode = FW_ERROR_UNKNOWN;
		}
		[_rendererController handleStateTransition:FW_RENDERER_STATE_FAILED 
											  info:[NSDictionary dictionaryWithObjectsAndKeys:errorCode, FW_INFO_KEY_ERROR_CODE, 
													[[self class] description], FW_INFO_KEY_ERROR_MODULE, 
													_errorInfo, FW_INFO_KEY_ERROR_INFO, nil]];
		return;
	} 

	CGRect adFrame = CGRectZero;
	adFrame.size = GAD_SIZE_320x50;
	id<FWCreativeRendition> rendition = [[_rendererController adInstance] primaryCreativeRendition];
	NSUInteger width = [[[_rendererController adInstance] slot] width];
	NSUInteger height = [[[_rendererController adInstance] slot] height];	
	width = [rendition width] > 0 ? [rendition width] : width;
	height = [rendition height] > 0 ? [rendition height] : height;
	if (width > 0 && height > 0) {
		adFrame.size = CGSizeMake(width, height);
	}
	if ([[[_rendererController adInstance] slot] timePositionClass] != FW_TIME_POSITION_CLASS_DISPLAY) {
		_duration = [rendition duration];
		_playheadTime = -1;
	}
	_adMobAd = [[GADBannerView alloc] initWithFrame:adFrame];
	_adMobAd.delegate = self;
	_adMobAd.rootViewController = _rootViewController;
	_adMobAd.adUnitID = _adMobAdUnitID;
	[[[[_rendererController adInstance] slot] slotBase] addSubview:_adMobAd];
	[_adMobAd loadRequest:[self buildGADRequest]];
}


- (void)stop {
	[durationTimer invalidate];
	self.durationTimer = nil;
	[_adMobAd setDelegate:nil];
	if (_duration > 0) {
		[_adMobAd.superview removeFromSuperview];
	}
	[_adMobAd removeFromSuperview];  
	self.adMobAd = nil;
	[_rendererController handleStateTransition:FW_RENDERER_STATE_COMPLETED info:nil];
}


- (NSDictionary *)moduleInfo {
	return [NSDictionary dictionaryWithObjectsAndKeys:
			FW_MODULE_TYPE_RENDERER, FW_INFO_KEY_MODULE_TYPE, FW_SDK_VERSION, FW_INFO_KEY_REQUIRED_API_VERSION, nil];
}

// Sent when an ad request loaded an ad; this is a good opportunity to attach
// the ad view to the hierarchy.
- (void)adViewDidReceiveAd:(GADBannerView *)adView {
	FWDebug(@"AdMob: Did receive ad");
	
	UIView *baseView = [[[_rendererController adInstance] slot] slotBase];
	UIView *theView;
	if (_duration > 0) {
		UIView *backgroundView = [[[UIView alloc] initWithFrame:baseView.bounds] autorelease];
		_adMobAd.center = baseView.center;
		backgroundView.backgroundColor = [UIColor blackColor];
		[backgroundView addSubview:_adMobAd];
		theView = backgroundView;
	} else {
		theView = _adMobAd;
	}
	[baseView addSubview:theView];
	
	if (_duration > 0) {
		_playheadTime = 0;
		self.durationTimer = [NSTimer scheduledTimerWithTimeInterval:1 target:self selector:@selector(_onDurationTimer:) userInfo:nil repeats:YES];
	}
	
	[_rendererController handleStateTransition:FW_RENDERER_STATE_STARTED info:nil];
}

// Sent when an ad request failed to load an ad
- (void)adView:(GADBannerView *)view didFailToReceiveAdWithError:(GADRequestError *)error {
	FWDebug(@"AdMob: Did fail to receive ad %d", error.code);
	// no need to remove adMobAd from superview since it is not added when fail
	self.adMobAd = nil;
	
	NSArray *key = [NSArray arrayWithObjects:FW_INFO_KEY_ERROR_CODE, nil];
	NSArray *value = [NSArray arrayWithObjects:FW_ERROR_NO_AD_AVAILABLE, nil];
	[_rendererController handleStateTransition:FW_RENDERER_STATE_FAILED
										  info:[NSDictionary dictionaryWithObjects:value forKeys:key]];
}

- (void)adViewWillPresentScreen:(GADBannerView *)adView {
	if (durationTimer) {
		[durationTimer invalidate];
		self.durationTimer = nil;
	} else {
		[_rendererController requestTimelinePause];
	}
	[[NSNotificationCenter defaultCenter] postNotificationName:FW_NOTIFICATION_ADMOB_PRESENT_FULL_SCREEN_MODAL object:[_rendererController notificationContext]];
}

- (void)adViewWillDismissScreen:(GADBannerView *)adView {
	if (_duration > 0) {
		self.durationTimer = [NSTimer scheduledTimerWithTimeInterval:1 target:self selector:@selector(_onDurationTimer:) userInfo:nil repeats:YES];
	}
	[[NSNotificationCenter defaultCenter] postNotificationName:FW_NOTIFICATION_ADMOB_DISMISS_FULL_SCREEN_MODAL object:[_rendererController notificationContext]];
}

- (void)adViewDidDismissScreen:(GADBannerView *)adView {
	FWDebug(@"AdMob did dismiss full screen modal");
	if (!self.durationTimer) {
		[_rendererController requestTimelineResume];
	}
}

- (void)adViewWillLeaveApplication:(GADBannerView *)adView {
}

- (NSTimeInterval)duration {
	if (_duration > 0) {
		return _duration;
	}
	return -1;
}

- (NSTimeInterval)playheadTime {
	if (_duration > 0) {
		return _playheadTime;
	}
	return -1;
}

- (void)_onDurationTimer:(NSTimer *)timer {
	_playheadTime += 1;
	if (_playheadTime >= _duration) {
		[self stop];
	}
}

@end
