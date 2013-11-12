//
//  ViewController.m
//  RendererRunner
//
//  Copyright (c) 2011 FreeWheel Media Inc. All rights reserved.
//

#import "ViewController.h"
#import "RendererConfiguration.h"
#import "BaseAd.h"

@protocol SizeSettable <NSObject>
-(void)setWidth:(uint)width;
-(void)setHeight:(uint)height;
@end

@implementation ViewController

@synthesize matrixView;
@synthesize player;
@synthesize adManager, adContext, midRollTimes, mMidRollObserver, rendererConfig, urlOpenedOutside;

- (void)dealloc {
	[player release];
	[midRollTimes release];
	[adContext release];
    [adManager release];
    [matrixView release];
	if (!rendererConfig) {
		[rendererConfig release];
	}
    [super dealloc];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Release any cached data, images, etc that aren't in use.
}

#pragma mark - View lifecycle

- (void)viewDidLoad {
	NSLog(@"viewDidLoad");
    [super viewDidLoad];
	// Do any additional setup after loading the view, typically from a nib.
    
    self.matrixView.contentMode = UIViewContentModeScaleAspectFit;
    self.matrixView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    if (!urlOpenedOutside) {// if opened from other app, there will be a openURL call after viewdidload
		[self setupAdManager];
		[self refreshView:nil];
	}
}

- (void)setupAdManager {
	NSString * filePath = nil;
	if (urlOpenedOutside) {
		NSLog(@"Loading configuration from url opened from other apps, full path: %@", urlOpenedOutside);
		if ([[[urlOpenedOutside pathExtension] lowercaseString] isEqualToString:@"json"]) {
			rendererConfig = [[RendererConfiguration alloc] initWithConfigFile:[urlOpenedOutside path]];
			filePath = [rendererConfig writeToFile];
		} else if ([[[urlOpenedOutside pathExtension] lowercaseString] isEqualToString:@"xml"]) {
			rendererConfig = nil;
			filePath = [urlOpenedOutside path];
		}
	} else {
		filePath = [[NSBundle mainBundle] pathForResource:@"response" ofType:@"xml"];
		if (!filePath) {
			filePath = [[NSBundle mainBundle] pathForResource:@"config" ofType:@"json"];
			NSLog(@"Loading configuration from config.json, full path: %@", filePath);
			rendererConfig = [[RendererConfiguration alloc] initWithConfigFile:filePath];
			filePath = [rendererConfig writeToFile];
		} else {
			NSLog(@"Loading configuration from response.xml, full path: %@", filePath);
			rendererConfig = nil;
		}
	}
    
    self.adManager = newAdManager();
    [adManager setLocation:[[[CLLocation alloc] initWithLatitude:40.738878 longitude:-73.992491] autorelease]];
    [adManager setCurrentViewController:self];
    [adManager setServerUrl:[[NSURL fileURLWithPath:filePath] description]];
}

- (void)viewDidUnload {
    [super viewDidUnload];
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
}

- (void)viewWillDisappear:(BOOL)animated {
	[super viewWillDisappear:animated];
}

- (void)viewDidDisappear:(BOOL)animated {
	[super viewDidDisappear:animated];
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation {
    return YES;
}

- (IBAction)startVideo:(id)sender {
    [self.player play];
}

- (IBAction)pauseVideo:(id)sender {
    [self.player pause];
}

- (IBAction)stopVideo:(id)sender {
    [self.player stop];
}

- (IBAction)refreshView:(id)sender {
	if (sender != nil) {
		[[NSNotificationCenter defaultCenter] removeObserver:self name:nil object:adContext];
		[player.containerView removeFromSuperview];
		[player release];
		[midRollTimes release];
	}
	adContext = [adManager newContext];
	midRollTimes = [[NSMutableArray alloc] init];
	
	if (rendererConfig) {
		uint siteSectionSlotCount = 0;
		for (BaseAd * ad in rendererConfig.ads) {
			if ([ad.slotType isEqualToString:@"display"]) {
				[adContext addSiteSectionNonTemporalSlot:[NSString stringWithFormat:@"display-%u", siteSectionSlotCount] :nil :[ad slotWidth] :[ad slotHeight] :nil :YES :FW_SLOT_OPTION_INITIAL_AD_STAND_ALONE :nil :nil :nil];
				siteSectionSlotCount ++;
			}
		}
	} else {
		;
	}
	
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onAdRequestCompleted:) name:FW_NOTIFICATION_REQUEST_COMPLETE object:adContext];	
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(_onAdSlotEnded:) name:FW_NOTIFICATION_SLOT_ENDED object:adContext];
	
    //[adContext setParameter:FW_PARAMETER_COUNTDOWN_TIMER_DISPLAY withValue:@"YES" forLevel:FW_PARAMETER_LEVEL_OVERRIDE];
	//[adContext setParameter:FW_PARAMETER_COUNTDOWN_TIMER_POSITION withValue:@"top" forLevel:FW_PARAMETER_LEVEL_OVERRIDE];
    [adContext setVideoDisplayBase:self.matrixView];

	player = [[AVPlayerWrapper alloc] initWithFrame:CGRectMake(0, 0, self.matrixView.frame.size.width, self.matrixView.frame.size.height)];
    [player load:[[NSBundle mainBundle] pathForResource:@"movie" ofType:@"m4v"]];
    [self.matrixView addSubview:self.player.containerView];
	
    [adContext submitRequest:3];
}

- (IBAction)quitApp:(id)sender {
    exit(0);
}

- (void)_onAdRequestCompleted:(NSNotification *)notification
{
    id<FWContext> ctx = [notification object];
    
    uint y = 0;
    for (id<FWSlot> iter in [ctx siteSectionNonTemporalSlots]) {
        UIView * nonTView = [iter slotBase];
		uint width = [iter width];
		uint height = [iter height];
		if (!rendererConfig || width == 0 || height == 0) {
			id<FWCreativeRendition> firstRendition = [[[[iter adInstances] objectAtIndex:0] creativeRenditions] objectAtIndex:0];
			width = [firstRendition width];
			height = [firstRendition height];
		}
		nonTView.frame = CGRectMake(0, y, width, height);
		[self _lazySetSlotWidthHeight:iter withWidth:width withHeight:height];
		y += height;
		[matrixView addSubview:nonTView];
		[iter play];
    }
    
    for (id<FWSlot> iter in [ctx temporalSlots]) {
        if ([iter timePositionClass] == FW_TIME_POSITION_CLASS_MIDROLL
            || [iter timePositionClass] == FW_TIME_POSITION_CLASS_OVERLAY) {
            [midRollTimes addObject:[NSNumber numberWithFloat:[iter timePosition]]];
        }
    }
    
    for (id<FWSlot> iter in [ctx temporalSlots]) {
        if ([iter timePositionClass] == FW_TIME_POSITION_CLASS_PREROLL) {
            [iter play];
            return;
        }
    }
}

/*
 This method is a workaround to reset slot width/height after ad request has been finished.
 This is not a public or supported API by FreeWheel. Clients should NOT follow this usage.
 */
- (void)_lazySetSlotWidthHeight:(id<FWSlot>)iter withWidth:(uint)width withHeight:(uint)height
{
	[((id<SizeSettable>)iter) setWidth:width];
	[((id<SizeSettable>)iter) setHeight:height];
}

- (void)_onAdSlotEnded:(NSNotification *)notification {
	FWTimePositionClass timePositionClass = [[adContext getSlotByCustomId:[[notification userInfo] objectForKey:FW_INFO_KEY_CUSTOM_ID]] timePositionClass];
    if (timePositionClass == FW_TIME_POSITION_CLASS_PREROLL) {
        // add MIDROLL/OVERLAY time observer
        if (self.midRollTimes && ([self.midRollTimes count] > 0)) {
            self.mMidRollObserver = [self.player.avPlayer
                                     addBoundaryTimeObserverForTimes:self.midRollTimes
                                     queue:dispatch_get_main_queue()
                                     usingBlock:^(void)
                                     {
                                         CMTime currentPlayTime = [self.player.avPlayer currentTime];   
                                         for (id<FWSlot> iter in [adContext temporalSlots]) {
                                             if ([iter timePositionClass] == FW_TIME_POSITION_CLASS_MIDROLL &&
                                                 fabs(CMTimeGetSeconds(currentPlayTime) - [iter timePosition]) < 2) {
                                                 [self pauseVideo:nil];
                                                 [iter play];
                                                 return;
                                             }
                                             if ([iter timePositionClass] == FW_TIME_POSITION_CLASS_OVERLAY &&
                                                 fabs(CMTimeGetSeconds(currentPlayTime) - [iter timePosition]) < 2) {
                                                 [iter play];
                                                 return;
                                             }
                                         }
                                     }
                                     ];
        }
        [self startVideo:nil];
    } else if (timePositionClass == FW_TIME_POSITION_CLASS_MIDROLL) {
        [self startVideo:nil];
    }
}
@end
