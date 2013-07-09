//
//  ViewController.h
//  RendererRunner
//
//  Copyright (c) 2011 FreeWheel Media Inc. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <CoreLocation/CLLocation.h>
#import "AVPlayerWrapper.h"
#import "RendererConfiguration.h"

@interface ViewController : UIViewController
{
}

- (void)dealloc;

- (void)_onAdRequestCompleted:(NSNotification *)notification;
- (void)_onAdSlotEnded:(NSNotification *)notification;
- (void)_lazySetSlotWidthHeight:(id<FWSlot>)iter withWidth:(uint)width withHeight:(uint)height;

- (void)setupAdManager;

- (IBAction)startVideo:(id)sender;
- (IBAction)pauseVideo:(id)sender;
- (IBAction)stopVideo:(id)sender;
- (IBAction)refreshView:(id)sender;
- (IBAction)quitApp:(id)sender;

@property (nonatomic, retain) IBOutlet UIView *matrixView;
@property (nonatomic, retain) AVPlayerWrapper * player;
@property (nonatomic, retain) id<FWAdManager> adManager;
@property (nonatomic, retain) id<FWContext> adContext;
@property (nonatomic, retain) NSMutableArray * midRollTimes;
@property (nonatomic, retain) id mMidRollObserver;
@property (nonatomic, retain) RendererConfiguration * rendererConfig;
@property (nonatomic, retain) NSURL* urlOpenedOutside;
@end
