#import "FWAdMobBaseAdRenderer.h"

@interface FWAdMobRenderer : NSObject <FWRenderer> {
	id<FWRendererController> _rendererController;
	BOOL _shouldFail;
}

@property (nonatomic, retain) FWAdMobBaseAdRenderer *renderer;

@end
