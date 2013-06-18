#import "FWPlayerAppDelegate.h"
#import "FWPlayerViewController.h"

@implementation FWPlayerAppDelegate

@synthesize window;
@synthesize viewController;
@synthesize locationManager;


- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions { 
	window = [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]];

	viewController = [[FWPlayerViewController alloc] init];
	window.rootViewController = viewController;
	
	adManager = newAdManager();	
	// contact FreeWheel Integration Support Engineer about configuration of ad network/serverUrl/...
    //set networkId of the customer MRM network
	[adManager setNetworkId:42015];
    //set the ad server URL to which all ad requests will go
	[adManager setServerUrl:@"http://cue.v.fwmrm.net"];
	
	[window makeKeyAndVisible];
	return YES;
}



@end
