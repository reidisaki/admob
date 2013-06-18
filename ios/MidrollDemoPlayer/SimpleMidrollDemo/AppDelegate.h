//
//  AppDelegate.h
//  SimpleMidrollDemo
//
//  Created by Stone Wang on 1/15/13.
//  Copyright (c) 2013 FreeWheel. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "SimpleViewController.h"

@interface AppDelegate : UIResponder <UIApplicationDelegate>{
	SimpleViewController *_vc;
}

@property (strong, nonatomic) UIWindow *window;

@end
