#import "OverlayRendererDemo.h"

@implementation FWOverlayRendererDemo

@synthesize rendererController = _rendererController;
@synthesize webView = _webView;
@synthesize baseView = _baseView;
@synthesize timer = _timer;

#pragma mark  -
#pragma mark Common Methods
- (void)dealloc {
	NSLog(@"%@", self);
    
	[[NSNotificationCenter defaultCenter] removeObserver:self];
	[_webView release];
    [_timer release];
	[super dealloc];
}

#pragma mark -
#pragma mark FWRenderer Methods
- (NSDictionary *)moduleInfo {
	return [NSDictionary dictionaryWithObject:FW_MODULE_TYPE_RENDERER forKey:FW_INFO_KEY_MODULE_TYPE];
}

- (id)initWithRendererController:(id<FWRendererController>)rendererController {
	if (!rendererController) {
		[self release];
		return nil;
	}
    
	if (!(self = [super init]))
		return nil;
    
	_rendererController = rendererController;
    
    // initialize web view
    _webView = [[UIWebView alloc] init];
    if ([_webView respondsToSelector:@selector(setAllowsInlineMediaPlayback:)]) {
		_webView.allowsInlineMediaPlayback = YES;
	}
	if ([_webView respondsToSelector:@selector(setMediaPlaybackRequiresUserAction:)]) {
		_webView.mediaPlaybackRequiresUserAction = NO;
	}
	_webView.autoresizingMask = (UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight);
	_webView.delegate = self;
	_webView.userInteractionEnabled = YES;
    _webView.opaque = NO;
    _webView.backgroundColor = [UIColor clearColor];
    
	// register stop timer
	NSInvocation* invocation = [NSInvocation invocationWithMethodSignature:[[self class] instanceMethodSignatureForSelector:@selector(stop)]];
	[invocation setTarget:self];
	[invocation setSelector:@selector(stop)];
	self.timer = [[[RendererTimer alloc] initWithDuration:[self duration] invocation:invocation] autorelease];
    
	// notify controller
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onInAppViewOpen:) name:FW_NOTIFICATION_IN_APP_VIEW_OPEN object:nil];
    
	return self;
}

- (void)start {
	NSLog(@"FWOverlayRendererDemo started");
    
	// prepare views
    self.baseView = [[[_rendererController adInstance] slot] slotBase];
	self.baseView.hidden = NO;
	
	// load ad
	id<FWAdInstance> ad = [_rendererController adInstance];
	NSString *url = [[[ad primaryCreativeRendition] primaryCreativeRenditionAsset] url];
    NSString *content = [[[ad primaryCreativeRendition] primaryCreativeRenditionAsset] content];
	
    if (url && [url length] > 0) {
		NSLog(@"load ad %d from url %@", [ad adId], url);
        [_webView loadRequest:[NSURLRequest requestWithURL:[NSURL URLWithString:url]]];
	} else if (content && [content length] > 0) {
        NSLog(@"load ad %d from content %@", [ad adId], content);
        [_webView loadHTMLString:content baseURL:[NSURL URLWithString:url]];
    } else {
		NSLog(@"invalid ad %d", [ad adId]);
		[_rendererController handleStateTransition:FW_RENDERER_STATE_FAILED info:[NSDictionary dictionaryWithObjectsAndKeys:FW_ERROR_NULL_ASSET, FW_INFO_KEY_ERROR_CODE, nil]];
	}
    
    // prepare web view
    _webView.frame = _baseView.frame;
    [_baseView addSubview:_webView];
    _webView.hidden = NO;
    
    // notify controller
    [_rendererController handleStateTransition:FW_RENDERER_STATE_STARTED info:nil];
    
    // initialize web view position
    [self calculatePosition];
    
    // register orientation change callback
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onOrientationChange:) name:UIDeviceOrientationDidChangeNotification object:nil];
    
    // register InAPPView closed callback
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onInAPPViewClosed) name:FW_NOTIFICATION_IN_APP_VIEW_CLOSE object:nil];
    
    // start ad timer
	NSLog(@"Timer start, will end in %f second", [self duration]);
    [self.timer start];
}

- (void)stop {
	NSLog(@"FWOverlayRendererDemo stopped");
    
    // stop timer
    [self.timer stop];
	self.timer = nil;
    
    // reset web view
	if (self.webView) {
		[self.webView loadRequest:[NSURLRequest requestWithURL:[NSURL URLWithString:@"about:blank"]]];
    }
    self.webView.delegate = nil;
	[self.webView removeFromSuperview];
    
    // revoke event listener
	[[NSNotificationCenter defaultCenter] removeObserver:self];
    
    // notify controller
	[_rendererController handleStateTransition:FW_RENDERER_STATE_COMPLETED info:nil];
}

- (NSTimeInterval)duration {
	return [[[_rendererController adInstance] primaryCreativeRendition] duration];
}

- (NSTimeInterval)playheadTime {
	if (self.timer)
        return [self.timer timeElapsed];
	return 0;
}

#pragma mark -
#pragma mark Auxiliary Methods
/*
 Calculate web view position when rotating
 */
-(void)calculatePosition {
	NSLog(@"recalculate position");
	int w = [[[_rendererController adInstance] primaryCreativeRendition] width];
	int h = [[[_rendererController adInstance] primaryCreativeRendition] height];
	int sw = self.baseView.frame.size.width;
	int sh = self.baseView.frame.size.height;
    
    // center
	int x = (sw - w) / 2;
    // bottom
	int y = sh - h;
	
	NSLog(@"x,y,w,h,sw,sh: %d,%d,%d,%d,%d,%d",x,y,w,h,sw,sh);
	_webView.frame = CGRectMake(x, y, w,h);
}

/*
 Send callback when clicking OVERLAY ad
 */
- (void)processClick:(NSURL *)url {
    if ([[url host] hasSuffix:@".fwmrm.net"]  && [[url description] rangeOfString:@"ad/l/1"].location != NSNotFound) {
        // for FW standard click tracking events.
		NSLog(@"User click on fw url: %@",url);
		[_rendererController processEvent:FW_EVENT_AD_CLICK info:[NSDictionary dictionaryWithObjectsAndKeys:@"YES", @"skipTracking", nil]];
	} else {
		// for 3rd party click tracking events.
		NSLog(@"User click on 3p url: %@",url);
		[_rendererController processEvent:FW_EVENT_AD_CLICK info:[NSDictionary dictionaryWithObjectsAndKeys:[url description], @"url", nil]];
	}
}

#pragma mark -
#pragma mark Callbacks
/*
 Handle orientation change, recalculate OVERLAY ad position
 */
- (void)onOrientationChange:(NSNotification *)notification {
	UIInterfaceOrientation orientation = [[UIApplication sharedApplication] statusBarOrientation];
	NSLog(@"onOrientationChange %d => %d", self.orientation, orientation);
    
	if (orientation == self.orientation) return;
	self.orientation = orientation;
    
    [self calculatePosition];
}

#pragma mark -
#pragma mark UIWebViewDelegate
/*
 Open an InAPPView when clicking ad
 */
- (BOOL)webView:(UIWebView *)webView shouldStartLoadWithRequest:(NSURLRequest *)request navigationType:(UIWebViewNavigationType)navigationType {
	NSURL *url = [request URL];
	
	if( navigationType == UIWebViewNavigationTypeLinkClicked) {
        NSLog(@"pause content video");
		[_rendererController requestTimelinePause];
		[self processClick:url];
		return NO;
	}
	return YES;
}

/*
 When close the InAPPView, content will be resumed
 */
- (void)onInAPPViewClosed
{
    NSLog(@"resume content video");
	[_rendererController requestTimelineResume];
}

@end

/*
 Implementation of auxiliary timer class
 */
@implementation RendererTimer
@synthesize tickTimer = _tickTimer;
@synthesize invocation = _invocation;

- (void)dealloc {
	NSLog(@"dealloc");
	if(self.tickTimer){
		[_tickTimer invalidate];
		self.tickTimer = nil;
	}
    self.invocation = nil;
	[super dealloc];
}

- (id)initWithDuration:(NSTimeInterval)duration invocation:(NSInvocation *)invocation {
	NSLog(@"initWithDuration:invocation:, duration=%lf",duration);
	if ((self = [super init])) {
		if (duration >=0.) {
			NSUInteger uiduration = (NSUInteger)duration;
			_duration = duration > uiduration? uiduration+1:uiduration;
		}else{
            _duration = -1.;
        }
		self.invocation = invocation;
		_ticks = -1.;
		_state = INIT;
	}
	return self;
}

- (void)start {
	NSLog(@"start, _state=%d, _duration=%lf",_state,_duration);
	if(_state != INIT)
		return;
	_state = STARTED;
	_ticks = 0.;
	self.tickTimer = [NSTimer scheduledTimerWithTimeInterval:1. target:self selector:@selector(_tickTimerFire:) userInfo:nil repeats:YES];
}

- (void)stop {
	NSLog(@"stop, _state=%d",_state);
	if (_state != STARTED && _state != PAUSED)
		return;
	_state = STOPPED;
	if(self.tickTimer){
		[_tickTimer invalidate];
		self.tickTimer = nil;
	}
}

- (void)pause {
	NSLog(@"pause, _state=%d",_state);
	if (_state != STARTED)
		return;
	_state = PAUSED;
}

- (void)resume {
	NSLog(@"resume, _state=%d",_state);
	if (_state != PAUSED)
		return;
	_state = STARTED;
}

- (void)reset {
	NSLog(@"reset, _state=%d",_state);
	[self stop];
	_state = INIT;
	_ticks = -1.;
}

- (void)_tickTimerFire:(NSTimer*)theTimer {
	NSLog(@"_tickTimerFire, _state=%d, _ticks=%lf",_state,_ticks);
	if (_state != STARTED)
		return;
	_ticks = _ticks+1.;
	if (_duration > 0. && _ticks >= _duration){//if duration < 0,the timer is never stopped automatically until the stop method is called.
		[self stop];
        if (_invocation) {
            [_invocation invoke];
        }
	}
}

- (NSTimeInterval)timeElapsed {
	//NSLog(@"timeElapsed, _state=%d, _ticks=%lf",_state,_ticks);
	return _ticks;
}

@end
