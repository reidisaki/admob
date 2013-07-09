//
//  PlayerContainerUIView.m
//  RendererRunner
//
//  Copyright (c) 2011 FreeWheel Media Inc. All rights reserved.
//

#import "PlayerContainerUIView.h"

@implementation PlayerContainerUIView

@synthesize subLayer;

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code
        subLayer = nil;
    }
    return self;
}

- (void)layoutSubviews
{
    [super layoutSubviews];
    if (subLayer != nil) {
        subLayer.frame = self.bounds;
    }
}

@end
