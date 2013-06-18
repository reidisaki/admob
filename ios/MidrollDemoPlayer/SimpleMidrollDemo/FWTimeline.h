//
//  FWTimeline.h
//
//  Copyright (c) 2012 FreeWheel Inc. All rights reserved.
//

#import <MediaPlayer/MediaPlayer.h>
#import "AdManager/FWSDK.h"

__private_extern__ NSTimeInterval const TIMELINE_TIMER_INTERVAL;
__private_extern__ NSString *const NOTIFICATION_CUEPOINT_REACHED;

@interface FWTimeline : NSObject{
	NSMutableArray *_cuePoints;
	NSMutableArray *_names;
	NSTimer *_timer;
	MPMoviePlayerController *_player;
	uint _lastFiredCuePointIndex;
}

@property (nonatomic, strong) MPMoviePlayerController *player;

- (id)initWithMediaPlayer:(MPMoviePlayerController *)player;
- (void)addCuePointForSlot:(id<FWSlot>)slot;
- (void)removeCuePointForSlot:(id<FWSlot>)slot;
- (void)startTimer;
- (void)stopTimer;

@end
