//
//  SimpleViewController.h
//  SimpleMidrollDemo
//
//  Created by Stone Wang on 1/15/13.
//  Copyright (c) 2013 FreeWheel. All rights reserved.
//

/* This demo shows how to trigger time position based slots at the correct time. An ad request will be made, and returned
 in the response are four slots with time position classes equals to FW_TIME_POSITION_CLASS_PREROLL, 
 FW_TIME_POSITION_CLASS_MIDROLL, FW_TIME_POSITION_CLASS_OVERLAY, FW_TIME_POSITION_CLASS_POSTROLL respectively. This demo
 only focuses on the midroll and overlay slots. To learn how to properly play preroll and postroll slots, please refer to SimpleDemoPlayer.
 
 The overlay will appear 5 seconds after the movie starts, and the midroll will start playing when playhead time reaches 25s.
 */

#import <UIKit/UIKit.h>
#import <MediaPlayer/MediaPlayer.h>
#import "FWTimeline.h"

// Typically you only need one adManager instance for your app.
id<FWAdManager> adManager;

@interface SimpleViewController : UIViewController{
	MPMoviePlayerController *_player;
	FWTimeline *_timeline;
	id<FWContext> _adContext;
}

@end
