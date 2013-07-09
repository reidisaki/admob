//
//  AVPlayerWrapper.m
//  RendererRunner
//
//  Copyright (c) 2011 FreeWheel Media Inc. All rights reserved.
//

#import "AVPlayerWrapper.h"

@implementation AVPlayerWrapper

@synthesize avPlayer;
@synthesize playLayer;
@synthesize state, mediaURL, containerView;

- (void)dealloc
{
	[playLayer release];
    [avPlayer release];
	[containerView release];
	[super dealloc];
}

- (id)initWithFrame:(CGRect)frame
{
    if ((self = [super init]) != NULL) {
		containerView = [[PlayerContainerUIView alloc] initWithFrame:frame];
        containerView.contentMode = UIViewContentModeScaleAspectFit;
        containerView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
        state = VIDEO_PLAYBACK_STATE_STOPPED;
	}
    return self;
}

- (void)load:(NSString *)url
{
    self.mediaURL = url;
	avPlayer = [[AVPlayer alloc] initWithURL:[url characterAtIndex:0]=='/' ? [NSURL fileURLWithPath:url] : [NSURL URLWithString:url]];
    avPlayer.actionAtItemEnd = AVPlayerActionAtItemEndNone;
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(playerItemDidReachEnd:)
                                                 name:AVPlayerItemDidPlayToEndTimeNotification
                                               object:[avPlayer currentItem]];
    self.playLayer = [AVPlayerLayer playerLayerWithPlayer:avPlayer];
	playLayer.frame = CGRectMake(0, 0, self.containerView.frame.size.width, self.containerView.frame.size.height);
    self.containerView.subLayer = playLayer;
	[self.containerView.layer addSublayer:playLayer];
}
- (void)play
{
    if (self.state != VIDEO_PLAYBACK_STATE_PLAYING) {
        [avPlayer play];
        self.state = VIDEO_PLAYBACK_STATE_PLAYING;
    } else if (avPlayer.status == AVPlayerStatusFailed) {
        [self unload];
        [self load:self.mediaURL];
        [self play];
    }
}
- (void)pause
{
    [avPlayer pause];
    self.state = VIDEO_PLAYBACK_STATE_PAUSED;
}
- (void)resume
{
    [avPlayer play];
    self.state = VIDEO_PLAYBACK_STATE_PLAYING;
}
- (void)stop
{
    [self pause];
    [self seek:0];
    self.state = VIDEO_PLAYBACK_STATE_STOPPED;
}
- (void)seek:(NSTimeInterval)timePosition
{
    [avPlayer seekToTime:CMTimeMakeWithSeconds(timePosition, 1)];
}

- (void)playerItemDidReachEnd:(NSNotification *)notification {
    [self stop];
}

- (void)unload
{
    [[NSNotificationCenter defaultCenter] removeObserver:self name:AVPlayerItemDidPlayToEndTimeNotification object:nil];
    self.containerView.subLayer = nil;
    [playLayer removeFromSuperlayer];
    [self.containerView removeFromSuperview];
}

@end
