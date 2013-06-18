//
//  FWTimeline.m
//
//  Copyright (c) 2012 FreeWheel Inc. All rights reserved.
//

#import "FWTimeline.h"

NSTimeInterval const TIMELINE_TIMER_INTERVAL = 0.5f;
NSString *const NOTIFICATION_CUEPOINT_REACHED = @"cuePointReached";

@interface FWTimeline (private)

- (void)_onTimer:(NSTimer*)timer;

@end

@implementation FWTimeline

- (void)dealloc{
	[self stopTimer];
}

- (id)initWithMediaPlayer:(MPMoviePlayerController *)player{
	self = [super init];
	if (self){
		_cuePoints = [[NSMutableArray alloc] init];
		_names = [[NSMutableArray alloc] init];
		self.player = player;
		_lastFiredCuePointIndex = NSNotFound;
	}
	return self;
}

- (void)addCuePointForSlot:(id<FWSlot>)slot{
	NSNumber *time = [NSNumber numberWithDouble:[slot timePosition]];
	[_cuePoints addObject:time];
	[_cuePoints sortUsingComparator:^(NSNumber *tp1, NSNumber *tp2) {
		return tp1.doubleValue < tp2.doubleValue;
	}];
	uint index = [_cuePoints indexOfObject:time];
	[_names insertObject:[slot customId] atIndex:index];
}

- (void)removeCuePointForSlot:(id<FWSlot>)slot{
	uint index = [_names indexOfObject:[slot customId]];
	if (index != NSNotFound){
		[_names removeObjectAtIndex:index];
		[_cuePoints removeObjectAtIndex:index];
	}
}

- (void)startTimer{
	if (_timer != nil){
		return;
	}
	_timer = [NSTimer scheduledTimerWithTimeInterval:TIMELINE_TIMER_INTERVAL target:self selector:@selector(_onTimer:) userInfo:nil repeats:YES];
}

- (void)stopTimer{
	[_timer invalidate];
	_timer = nil;
}

- (void)_onTimer:(NSTimer*)timer{
	NSTimeInterval currentPlayheadTime = [_player currentPlaybackTime];
	for (uint i = 0; i < _cuePoints.count; i++){
		NSNumber *time = [_cuePoints objectAtIndex:i];
		if (time.doubleValue - TIMELINE_TIMER_INTERVAL < currentPlayheadTime &&
			time.doubleValue + TIMELINE_TIMER_INTERVAL > currentPlayheadTime){
			// Current playhead time sits in the tolerance range
			if (_lastFiredCuePointIndex != i){
				// Not the last one fired
				NSLog(@"Firing CuePoint at time %@", time);
				_lastFiredCuePointIndex = i;
				[[NSNotificationCenter defaultCenter] postNotificationName:NOTIFICATION_CUEPOINT_REACHED object:self userInfo:[NSDictionary dictionaryWithObjectsAndKeys:[_names objectAtIndex:i], FW_INFO_KEY_CUSTOM_ID, nil]];
			}
			// else: this cuepoint has been fired shortly before. Prevent refiring the same cuepoint over and over again.
			return;
		}
	}
}

@end
