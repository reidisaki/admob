//
//  FWDisplayAdViewController.h
//  FWDisplayAdDemo
//
//  Created by Shitong Wang on 3/26/12.
//  Copyright 2012 FreeWheel. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "AdManager/FWSDK.h"

/*
 Global parameters:
 1. Ad server URL
 2. Network ID
 3. Profile
 
 These parameters only need to be set once for an AdManager instance. Typically you only need one 
 global AdManager instance for your app.
 
 Please ask your FreeWheel point of contact for appropriate values to set into these parameters. 
 Below is an example of our test network and profile.
 */
#define FW_AD_SERVER_URL @"http://cue.v.fwmrm.net"
#define FW_NETWORK_ID 90750
#define FW_PROFILE @"3pqa_ios"

/*
 Site / Video specific parameters:
 1. Site section ID
 2. Video asset ID
 
 These parameters are used mainly for targeting purposes. When AdManager passes the values to FreeWheel
 ad server, ad server will look at what ads are targetted on the specified site section and/or video
 asset, and return appropriate ads.
 
 In the following example, a 300x250 display unit is targetted on site section "FWDisplayAdDemo_section"
 and video asset "FwDisplayAdDemo_video_asset", which will be returned and rendered.
 */
#define FW_SITE_SECTION @"FWDisplayAdDemo_section"
#define FW_VIDEO_ASSET @"FWDisplayAdDemo_video_asset"

// Give a name to the 300x250 slot, which only shows in portrait mode
#define SLOT_300_X_250 @"300x250slot"
// The 300x60 slot only shows in landscape mode
#define SLOT_300_X_60 @"300x60slot"

/*
 For an app, we recommend the usage of a singleton global FWAdManager instance. 
 There is usually no need of creating multiple FWAdManager instances in a single app.
*/

id<FWAdManager> adManager;

@interface FWDisplayAdViewController : UIViewController{
    id<FWContext> adContext;
}

/*
 For each new ad request, a new FWContext instance should be created.
 It is used to:
 1. Send a new ad request to FreeWheel ad server
 2. Receive and parse the ad response returned from FreeWheel ad server
 3. Handle various notifications
 4. etc.
 
 In this demo we only have one ad request that requests a 300x250 display unit.
 
 adContext is retained and should be released manually when it's no longer in use.
 */
@property (nonatomic, retain) id<FWContext> adContext;

- (void)onAdRequestCompleted:(NSNotification *)notification;

@end
