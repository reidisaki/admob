#import "FWAdMobBaseAdRenderer.h"

@interface FWAdMobViewAdRenderer : FWAdMobBaseAdRenderer<FWRenderer, GADBannerViewDelegate> {
	GADBannerView *_adMobAd;
	NSTimeInterval _duration;
	NSTimeInterval _playheadTime;
}

- (void)dealloc;
- (id)initWithRendererController:(id<FWRendererController>)rendererController;
- (void)start;
- (void)stop;
- (NSDictionary *)moduleInfo;
- (void)adViewDidReceiveAd:(GADBannerView *)adView;
- (void)adView:(GADBannerView *)view didFailToReceiveAdWithError:(GADRequestError *)error;
- (void)adViewWillPresentScreen:(GADBannerView *)adView;
- (void)adViewWillLeaveApplication:(GADBannerView *)adView;

@property (nonatomic, retain) GADBannerView *adMobAd;
@property (nonatomic, retain) NSTimer *durationTimer;

@end
