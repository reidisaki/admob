#import "FWAdMobBaseAdRenderer.h"

@interface FWAdMobInterstitialAdRenderer : FWAdMobBaseAdRenderer<FWRenderer, GADInterstitialDelegate> {
	GADInterstitial *_adMobInterstitialAd;
}

@property (nonatomic, retain) GADInterstitial *adMobInterstitialAd;

@end
