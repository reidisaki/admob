//
//  AVPlayerWrapper.h
//  RendererRunner
//
//  Copyright (c) 2011 FreeWheel Media Inc. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
#import "PlayerContainerUIView.h"

@interface AVPlayerWrapper : NSObject
{
    // for iOS 4.0 and later
    AVPlayer * avPlayer;
    AVPlayerLayer * playLayer;
}

@property (atomic, retain) PlayerContainerUIView * containerView;
@property (nonatomic, retain) NSString * state;
@property (nonatomic, retain) NSString * mediaURL;

- (id)initWithFrame:(CGRect)frame;
- (void)load:(NSString *)url;
- (void)play;
- (void)pause;
- (void)resume;
- (void)stop;
- (void)seek:(NSTimeInterval)timePosition;
- (void)unload;
- (void)playerItemDidReachEnd:(NSNotification *)notification;

@property (atomic, retain) AVPlayer * avPlayer;
@property (atomic, retain) AVPlayerLayer * playLayer;



#define VIDEO_FRAMEWORK_MEDIAPLAYER     1
#define VIDEO_FRAMEWORK_AVFOUNDATION    2

#define VIDEO_PLAYBACK_STATE_PLAYING    @"playing"
#define VIDEO_PLAYBACK_STATE_PAUSED	    @"paused"
#define VIDEO_PLAYBACK_STATE_STOPPED    @"stopped"
@end
