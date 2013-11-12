/**
 This demo will show you how to write a simple overlay ad renderer. Divided in 3 parts.
 
 NOTE.
 This sample is only for demonstrating how overlay renderer works, how does it interact with user input. More sophisticated changes are required on production environment.
 */


/*
 RendererTimer class for counting elapsed time of ad
 */
enum RendererTimerState {
	INIT = -1,
	STARTED  = 0,
	PAUSED = 1,
	STOPPED = 2
};

@interface RendererTimer : NSObject {
    NSTimeInterval          _duration;
    NSTimeInterval          _ticks;
    enum RendererTimerState _state;
    NSTimer*                _tickTimer;
    NSInvocation*           _invocation;
}

@property(nonatomic,retain) NSTimer* tickTimer;
@property(nonatomic,retain) NSInvocation* invocation;

- (id)initWithDuration:(NSTimeInterval)duration invocation:(NSInvocation *)invocation;
- (void)start;
- (void)stop;
- (void)pause;
- (void)resume;
- (void)reset;
- (NSTimeInterval)timeElapsed;
@end


/*
 Sample Overlay Renderer
 */
@interface FWOverlayRendererDemo : NSObject<FWRenderer, UIWebViewDelegate> {
	id<FWRendererController>    _rendererController;    // renderer controller
    UIView                      *_baseView;             // slot base view
    UIWebView                   *_webView;              // overlay container
    UIInterfaceOrientation      _orientation;           // current orientation
    uint                        _anchorX;               // alignment X
	uint                        _anchorY;               // alignment Y
    RendererTimer               *_timer;                // ad timer
}

@property (nonatomic, assign) id<FWRendererController> rendererController;
@property (nonatomic, assign) UIView *baseView;
@property (nonatomic, retain) UIWebView *webView;
@property (nonatomic, assign) UIInterfaceOrientation orientation;
@property (nonatomic, assign) uint anchorX;
@property (nonatomic, assign) uint anchorY;
@property (nonatomic, assign) int marginWidth;
@property (nonatomic, assign) int marginHeight;
@property (nonatomic, retain) RendererTimer *timer;

- (void) calculatePosition;
- (void) processClick:(NSURL *)url;

/*
 Callback triggered by orientation change event
 */
- (void) onOrientationChange:(NSNotification *)notification;

/*
 Callback triggered when closing InAPPView
 */
- (void) onInAPPViewClosed;
@end