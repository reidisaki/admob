#import "AdMobRenderer.h"
#import "FWCommon.h"

@interface FWAdMobBaseAdRenderer : NSObject {
	id<FWRendererController> _rendererController;
	NSString *_adMobAdUnitID;
	NSString *_locationDescription;
	NSDate *_dateOfBirth;
	NSString *_gender;
	NSString *_keywords;
	UIViewController *_rootViewController;
	NSString *_errorInfo;
	NSString *_parameterNamespace;
	NSArray *_testDevices;
}

- (BOOL)checkParameter;
- (id)initWithRendererController:(id<FWRendererController>)rendererController;
- (NSString *)parseStringParameter:(NSString *)paramName :(NSString *)defaultValue;
- (NSArray *)testDevices;
- (GADRequest *)buildGADRequest;
- (NSDictionary *)moduleInfo;
- (void)start;
- (void)stop;
- (NSTimeInterval)duration;
- (NSTimeInterval)playheadTime;

@property (nonatomic, assign) id<FWRendererController> rendererController;
@property (nonatomic, retain) NSString *adMobAdUnitID;
@property (nonatomic, retain) NSString *locationDescription;
@property (nonatomic, retain) NSDate *dateOfBirth;
@property (nonatomic, retain) NSString *gender;
@property (nonatomic, retain) NSString *keywords;
@property (nonatomic, retain) UIViewController *rootViewController;
@property (nonatomic, retain) NSString *errorInfo;
@property (nonatomic, retain) NSString *parameterNamespace;

@end
