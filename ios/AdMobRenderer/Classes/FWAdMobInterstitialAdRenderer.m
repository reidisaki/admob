#import "FWAdMobInterstitialAdRenderer.h"
#import "FWAdMobConstants.h"

@implementation FWAdMobInterstitialAdRenderer

@synthesize adMobInterstitialAd = _adMobInterstitialAd;

- (void)dealloc {
	_adMobInterstitialAd.delegate = nil;
	[_adMobInterstitialAd release];
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
		[_rendererController setCapability:FW_EVENT_AD_CLICK status:FW_CAPABILITY_STATUS_OFF];
	}
	
	return self;
}

- (void)start {
	if ([super checkParameter] == NO) {
		NSString *errorCode = FW_ERROR_MISSING_PARAMETER;
		if ([_errorInfo isEqualToString:FW_ERROR_MSG_MISSING_CURRENT_VIEW_CONTROLLER]) {
			errorCode = FW_ERROR_UNKNOWN;
		}
		[_rendererController handleStateTransition:FW_RENDERER_STATE_FAILED 
											  info:[NSDictionary dictionaryWithObjectsAndKeys:errorCode, FW_INFO_KEY_ERROR_CODE,
													_errorInfo, FW_INFO_KEY_ERROR_INFO, nil]];
		return;
	} else {
		_adMobInterstitialAd = [[GADInterstitial alloc] init];
		_adMobInterstitialAd.delegate = self;
		_adMobInterstitialAd.adUnitID = _adMobAdUnitID;
		[_adMobInterstitialAd loadRequest:[self buildGADRequest]];
	}
}


- (void)stop {
	FWDebug(@"can not stop a admob interstitial ad, just ignore");
}


- (NSDictionary *)moduleInfo {
	return [NSDictionary dictionaryWithObjectsAndKeys:
			FW_MODULE_TYPE_RENDERER, FW_INFO_KEY_MODULE_TYPE, FW_SDK_VERSION, FW_INFO_KEY_REQUIRED_API_VERSION, nil];
}

#pragma mark -
#pragma mark FWAdMobInterstitialDelegate Methods

- (void)interstitialDidReceiveAd:(GADInterstitial *)ad {
	FWDebug(@"interstitialDidReceiveAd");
	[_adMobInterstitialAd presentFromRootViewController:_rootViewController];
}


- (void)interstitial:(GADInterstitial *)ad didFailToReceiveAdWithError:(GADRequestError *)error {
	FWDebug(@"didFailToReceiveAdWithError %d", error.code);
	NSArray *key = [NSArray arrayWithObjects:FW_INFO_KEY_ERROR_CODE, nil];
	NSArray *value = [NSArray arrayWithObjects:FW_ERROR_NO_AD_AVAILABLE, nil];
	[_rendererController handleStateTransition:FW_RENDERER_STATE_FAILED 
										  info:[NSDictionary dictionaryWithObjects:value forKeys:key]];
}


- (void)interstitialWillPresentScreen:(GADInterstitial *)ad {
	FWDebug(@"interstitialWillPresentScreen");
	[_rendererController handleStateTransition:FW_RENDERER_STATE_STARTED info:nil];
}


- (void)interstitialWillDismissScreen:(GADInterstitial *)ad {
	FWDebug(@"interstitialWillDismissScreen");
}


- (void)interstitialDidDismissScreen:(GADInterstitial *)ad {
	FWDebug(@"interstitialDidDismissScreen");
	[_rendererController handleStateTransition:FW_RENDERER_STATE_COMPLETED info:nil];
}

- (void)interstitialWillLeaveApplication:(GADInterstitial *)ad {
	FWDebug(@"interstitialWillLeaveApplication");
}

- (NSTimeInterval)duration {
	return -1;
}

- (NSTimeInterval)playheadTime {
	return -1;
}

@end
