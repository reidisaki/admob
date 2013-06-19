#import "FWAdMobBaseAdRenderer.h"
#import "FWAdMobConstants.h"


@implementation FWAdMobBaseAdRenderer

@synthesize rendererController = _rendererController;
@synthesize adMobAdUnitID = _adMobAdUnitID;
@synthesize locationDescription = _locationDescription;
@synthesize dateOfBirth = _dateOfBirth;
@synthesize gender = _gender;
@synthesize keywords = _keywords;
@synthesize rootViewController = _rootViewController;
@synthesize errorInfo = _errorInfo;
@synthesize parameterNamespace = _parameterNamespace;

- (void)dealloc { 
	FWLog(@"");
	[_adMobAdUnitID release];
	[_locationDescription release];
	[_dateOfBirth release];
	[_gender release];
	[_keywords release];
	[_rootViewController release];
	[_errorInfo release];
	[_parameterNamespace release];
	[super dealloc];
}

- (NSString *)parseStringParameter:(NSString *)paramName :(NSString *)defaultValue {
	NSString *ret = (NSString *)[_rendererController getParameter:[NSString stringWithFormat:@"%@.%@",  _parameterNamespace, paramName]];
	if (!ret) {
		ret = (NSString *)[_rendererController getParameter:paramName];
	}
	
	if (ret) {
		return [ret stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
	} else {
		return defaultValue;
	}
}

- (BOOL)checkParameter {
	NSString *param = [self parseStringParameter:FW_PARAMETER_ADMOB_PUBLISHER_ID :nil];
	if (param == nil || [param length] == 0) {
		self.errorInfo = @"Missing publisher id";
		return NO;
	} else {
		id currentController = [_rendererController currentViewController];
		if (currentController == nil || ([currentController isKindOfClass:[UIViewController class]] == NO)) {
			self.errorInfo = FW_ERROR_MSG_MISSING_CURRENT_VIEW_CONTROLLER;
			return NO;
		}
		self.adMobAdUnitID = param;
		self.rootViewController = currentController;
		
		// fetch optional parameters
		param = [self parseStringParameter:FW_PARAMETER_ADMOB_LOCATION_DESCRIPTION :nil];
		if (param != nil) {
			self.locationDescription = param;
		}
		
		param = [self parseStringParameter:FW_PARAMETER_GENDER :nil];
		if ([param isEqualToString:@"female"] || [param isEqualToString:@"male"]) {
			self.gender = [param substringToIndex:1];
		}
		
		param = [self parseStringParameter:FW_PARAMETER_KEYWORDS :nil];
		if (param != nil) {
			self.keywords = param;
		}
		
		param = [self parseStringParameter:FW_PARAMETER_ADMOB_TEST_DEVICES :nil];
		if (param) {
			_testDevices = [param componentsSeparatedByCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
		}
		
		id birthDay = [_rendererController getParameter:[NSString stringWithFormat:@"%@.%@",  _parameterNamespace, FW_PARAMETER_DATE_OF_BIRTH]];
		if (!birthDay) {
			birthDay = [_rendererController getParameter:FW_PARAMETER_DATE_OF_BIRTH];
		}
		
		if (birthDay != nil && ([birthDay isMemberOfClass:[NSDate class]] || [[[birthDay class] description] isEqualToString:@"__NSDate"])) {
			self.dateOfBirth = birthDay;
		}
		
		return YES;
	}
}

- (id)initWithRendererController:(id<FWRendererController>)rendererController {
	FWLog(@"");
	if (!rendererController) {
		return nil;
	}
	if ((self = [super init])) {
		_rendererController = rendererController;
		self.parameterNamespace = @"renderer.admob";
		FWLog(@"AdMob version:%@", [GADRequest sdkVersion]);
	}
	
	return self;
}

- (NSArray *)testDevices {
	NSString *param = [self parseStringParameter:FW_PARAMETER_ADMOB_TEST_MODE :nil];
	if (param != nil && [param boolValue]) {
		if (_testDevices) {
			return [_testDevices arrayByAddingObject:GAD_SIMULATOR_ID];
		} else {
			return [NSArray arrayWithObjects:GAD_SIMULATOR_ID, nil];
		}
	} else {
		return nil;
	}
}

- (GADRequest *)buildGADRequest {
	GADRequest *adMobRequest = [GADRequest request];
	adMobRequest.testDevices = [self testDevices];
	if (_rendererController.location) {
		[adMobRequest setLocationWithLatitude:_rendererController.location.coordinate.latitude 
									longitude:_rendererController.location.coordinate.longitude
									 accuracy:_rendererController.location.horizontalAccuracy];
	} else if (_locationDescription) {
		[adMobRequest setLocationWithDescription:_locationDescription];
	}
	if (_dateOfBirth) {
		adMobRequest.birthday = _dateOfBirth;
	}
	if (_keywords) {
		NSArray *keywords = [_keywords componentsSeparatedByString:@","];
		for (NSString *kw in keywords) {
			[adMobRequest addKeyword:kw];
		}
	}
	if (_gender) {
		if ([_gender isEqualToString:@"f"]) {
			adMobRequest.gender = kGADGenderFemale;
		} else {
			adMobRequest.gender = kGADGenderMale;
		}
	}
	NSArray *devices = [self testDevices];
	if (devices) {
		adMobRequest.testDevices = devices;
	}
	return adMobRequest;
}

- (NSDictionary *)moduleInfo {
	NSLog(@"WARNING: FWAdMobBaseAdRenderer moduleInfo should be override!");
	return nil;
}

- (void)start {
	NSLog(@"WARNING: FWAdMobBaseAdRenderer start should be override!");
}

- (void)stop {
	NSLog(@"WARNING: FWAdMobBaseAdRenderer stop should be override!");
}

- (NSTimeInterval)duration {
	NSLog(@"WARNING: FWAdMobBaseAdRenderer duration should be override!");
	return -1;
}

- (NSTimeInterval)playheadTime {
	NSLog(@"WARNNING: FWAdMobBaseAdRenderer playheadTime should be override!");
	return -1;
}

@end
