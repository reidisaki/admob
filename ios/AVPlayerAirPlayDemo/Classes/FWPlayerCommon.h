//
// FreeWheel AdManager Sample Player
// It is a universal app which supports iPhone, iPad, iPhone4 with iOS >= 3.0.
//
//

#import <UIKit/UIKit.h>
#import <CoreLocation/CoreLocation.h>
#import <MediaPlayer/MediaPlayer.h>
#import <AVFoundation/AVFoundation.h>
#import "AdManager/FWSDK.h"

#define FWPLAYER_AD_REQUEST_TIMEOUT 5
#define XRETAIN // internal macro
#define XASSIGN // internal macro

// NOTE: Typically, one app has one adManager object
id<FWAdManager> adManager;
