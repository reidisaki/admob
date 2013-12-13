#import "FWPlayerCommon.h"
#import "FWPlayerViewController.h"

@interface FWPlayerAppDelegate : NSObject <UIApplicationDelegate, CLLocationManagerDelegate> {
    XRETAIN UIWindow *window;
	XRETAIN FWPlayerViewController *viewController;
	XRETAIN CLLocationManager *locationManager;
}

@property (nonatomic, strong) UIWindow *window;
@property (nonatomic, strong) FWPlayerViewController *viewController;
@property (nonatomic, strong) CLLocationManager *locationManager;

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions;


@end

