#import "SampleExtension.h"

@implementation SampleExtension

@synthesize adContext;


- (id)initWithFWContext:(id<FWContext>)context {
	if (!(self=[self init])) {
		return nil;
	}
	
	NSLog(@"SampleExtension initWithFWContext");
	
	self.adContext = context;
	
	// register handler for notifications posted by FreeWheel AdContext
	// there are more event than listed here, please refer to FW SDK Doc
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onSlotStarted:) name:FW_NOTIFICATION_SLOT_STARTED object:self.adContext];
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onSlotEnded:) name:FW_NOTIFICATION_SLOT_ENDED object:self.adContext];
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onInAppViewOpen:) name:FW_NOTIFICATION_IN_APP_VIEW_OPEN object:self.adContext];
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onInAppViewClose:) name:FW_NOTIFICATION_IN_APP_VIEW_CLOSE object:self.adContext];
	return self;
}

- (void)_cleanUp {
	// do the necessary clean up
}

- (void)stop {
	NSLog(@"SampleExtension stop");
	[[NSNotificationCenter defaultCenter] removeObserver:self];
	[self _cleanUp];
	self.adContext = nil;
}

- (void)_onSlotStarted:(NSNotification *)notification {
	
}

- (void)_onSlotEnded:(NSNotification *)notification {
	
}

- (void)_onInAppViewOpen:(NSNotification *)notification {
	
}

- (void)_onInAppViewClose:(NSNotification *)notification {
    
}

@end
