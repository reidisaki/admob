//
//  PlayerContainerUIView.h
//  RendererRunner
//
//  Copyright (c) 2011 FreeWheel Media Inc. All rights reserved.
//
#import <UIKit/UIKit.h>
#import <QuartzCore/CALayer.h>

@interface PlayerContainerUIView : UIView

@property (atomic, assign) CALayer * subLayer;

- (void)layoutSubviews;
@end
