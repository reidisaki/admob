//
//  FWDisplayAdDemoAppDelegate.h
//  FWDisplayAdDemo
//
//  Created by Stone Wang on 3/26/12.
//  Copyright 2012 FreeWheel. All rights reserved.
//

#import <UIKit/UIKit.h>

#import "FWDisplayAdViewController.h"


@interface FWDisplayAdDemoAppDelegate : UIResponder <UIApplicationDelegate>{
    UIWindow *window;
    FWDisplayAdViewController *viewController;
}

@property (strong, nonatomic) UIWindow *window;
@property (nonatomic, retain) FWDisplayAdViewController *viewController;

@end
