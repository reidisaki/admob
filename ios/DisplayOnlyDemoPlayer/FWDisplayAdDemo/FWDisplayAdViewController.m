//
//  FWDisplayAdViewController.m
//  FWDisplayAdDemo
//
//  Created by Shitong Wang on 3/26/12.
//  Copyright 2012 FreeWheel. All rights reserved.
//

#import "FWDisplayAdViewController.h"

@implementation FWDisplayAdViewController

@synthesize adContext;

- (id)init{
    self = [super init];
    if (self){
        adManager = newAdManager();
        [adManager setLocation:nil];
        [adManager setNetworkId:FW_NETWORK_ID];
        [adManager setServerUrl:FW_AD_SERVER_URL];
    }
    return self;
}

#pragma mark - View lifecycle

- (void)viewDidLoad{
    [super viewDidLoad];
    
    // Create a new FwContext instance for the new ad request
    self.adContext = [adManager newContext];
    [self.adContext setVideoDisplayBase:self.view];
    // Add handler for FW_NOTIFICATION_REQUEST_COMPLETE notification. When this notification is received it means ad server has returned a response. 
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onAdRequestCompleted:) name:FW_NOTIFICATION_REQUEST_COMPLETE object:adContext];
    
    // Now set necessary parameters. Please check out FreeWheel iOS AdManager API docs for details.
    [self.adContext setProfile:FW_PROFILE :nil :nil :nil];
    [self.adContext setSiteSection:FW_SITE_SECTION :0 :0 :FW_ID_TYPE_CUSTOM :0];
    [self.adContext setVideoAsset:FW_VIDEO_ASSET :600 :nil :true :0 :0 :FW_ID_TYPE_CUSTOM :0 :FW_VIDEO_ASSET_DURATION_TYPE_EXACT];
    
    // Add a display slot (site section non temporal slot)
    [self.adContext addSiteSectionNonTemporalSlot:SLOT_300_X_250 :nil :300 :250 :nil :YES :FW_SLOT_OPTION_INITIAL_AD_STAND_ALONE :nil :nil :nil];
    [self.adContext addSiteSectionNonTemporalSlot:SLOT_300_X_60 :nil :300 :60 :nil :YES :FW_SLOT_OPTION_INITIAL_AD_STAND_ALONE :nil :nil :nil];
    [self.adContext submitRequest:10];
    
    // Next, check out onAdRequestCompleted to see what happens after we get the response from FreeWheel ad server.
}

- (void)viewDidUnload
{
    [super viewDidUnload];
    [self.adContext release];
    [adManager release];
}

- (void)onAdRequestCompleted:(NSNotification *)notification{
    // Get the FWSlot instance
    id<FWSlot> slot1 = [self.adContext getSlotByCustomId:SLOT_300_X_250];
    UIView *slotBase = [slot1 slotBase];
    // Put the slotBase in desired place
    slotBase.frame = CGRectMake(10, 10, 300, 250);
    // Add the slotBase to view
    [self.view addSubview:slotBase];
    // Play it if in portrait mode
    if (UIInterfaceOrientationIsPortrait([[UIApplication sharedApplication] statusBarOrientation])){
        [slot1 play];
    }
    
    id<FWSlot> slot2 = [self.adContext getSlotByCustomId:SLOT_300_X_60];
    UIView *slotBase2 = [slot2 slotBase];
    slotBase2.frame = CGRectMake(10, 10, 300, 60);
    [self.view addSubview:slotBase2];
    if (UIInterfaceOrientationIsLandscape([[UIApplication sharedApplication] statusBarOrientation])){
        [slot2 play];
    }
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation
{
    return YES;
}

- (void)willRotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration{
    id<FWSlot> slot1 = [self.adContext getSlotByCustomId:SLOT_300_X_250];
    id<FWSlot> slot2 = [self.adContext getSlotByCustomId:SLOT_300_X_60];
    if (UIInterfaceOrientationIsPortrait(toInterfaceOrientation)){
        [slot1 play];
        [slot2 stop];
    }
    else{
        [slot1 stop];
        [slot2 play];
    }
}

@end
