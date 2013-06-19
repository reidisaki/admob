#import "FWAdMobRenderer.h"
#import "FWAdMobViewAdRenderer.h"
#import "FWAdMobInterstitialAdRenderer.h"

@implementation FWAdMobRenderer

@synthesize renderer;

#pragma mark FWRenderer Methods

- (void)dealloc {
	[renderer release];
	[super dealloc];
}

- (NSDictionary *)moduleInfo {
	return [renderer moduleInfo];
}

- (id)initWithRendererController:(id<FWRendererController>)rendererController {
	NSLog(@"FWAdMobRenderer initWithRendererController: %@", rendererController);
	if (!rendererController) {
		return nil;
	}
	if ((self = [super init])) {
		_rendererController = rendererController;
		id<FWSlot> slot = [[_rendererController adInstance] slot];
		id<FWCreativeRendition> cr = [[_rendererController adInstance] primaryCreativeRendition];
		NSString *contentType = [cr contentType];
		NSString *baseUnit = [cr baseUnit];
		_shouldFail = YES;
		if ([contentType isEqualToString:@"external/admob-view"]) {
			if ([slot type] != FW_SLOT_TYPE_TEMPORAL) {
				renderer = [[FWAdMobViewAdRenderer alloc] initWithRendererController:_rendererController];
				_shouldFail = NO;
			}
		} else if ([contentType isEqualToString:@"external/admob-interstitial"]) {
			if ([slot type] == FW_SLOT_TYPE_TEMPORAL && [slot timePositionClass] != FW_TIME_POSITION_CLASS_OVERLAY) {
				renderer = [[FWAdMobInterstitialAdRenderer alloc] initWithRendererController:_rendererController];
				_shouldFail = NO;
			}
		} else if ([baseUnit isEqualToString:@"fixed-size-interactive"]) {
			renderer = [[FWAdMobViewAdRenderer alloc] initWithRendererController:_rendererController];
			_shouldFail = NO;
		} else if ([baseUnit isEqualToString:@"app-interstitial"]) {
			renderer = [[FWAdMobInterstitialAdRenderer alloc] initWithRendererController:_rendererController];
			_shouldFail = NO;
		}
	}
	return self;
}

- (void)start {
	NSLog(@"FWAdMobRenderer start");
	if (_shouldFail) {
		NSArray *key = [NSArray arrayWithObjects:FW_INFO_KEY_ERROR_CODE, nil];
		NSArray *value = [NSArray arrayWithObjects:FW_ERROR_INVALID_SLOT, nil];
		[_rendererController handleStateTransition:FW_RENDERER_STATE_FAILED 
											  info:[NSDictionary dictionaryWithObjects:value forKeys:key]];
	} else {
		[renderer start];
	}
}

- (void)stop {
	NSLog(@"FWAdMobRenderer stop");
	[renderer stop];
}

- (NSTimeInterval)duration {
	return [renderer duration];
}

- (NSTimeInterval)playheadTime {
	return [renderer playheadTime];
}

@end
