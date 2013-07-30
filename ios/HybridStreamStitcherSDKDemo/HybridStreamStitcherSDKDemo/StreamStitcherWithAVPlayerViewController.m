//
//  StreamStitcherWithAVPlayerViewController.m
//  StreamStitcherDemo
//
//  Copyright (c) 2013 FreeWheel. All rights reserved.
//

#import "StreamStitcherWithAVPlayerViewController.h"
#import <AVFoundation/AVMediaFormat.h>

@implementation PlayerView
+ (Class)layerClass {
    return [AVPlayerLayer class];
}
- (AVPlayer*)player {
    return [(AVPlayerLayer *)[self layer] player];
}
- (void)setPlayer:(AVPlayer *)player {
    [(AVPlayerLayer *)[self layer] setPlayer:player];
}
@end

@interface StreamStitcherWithAVPlayerViewController ()
@property (nonatomic, retain) NSString *streamStitcherUrl;
@end

@implementation StreamStitcherWithAVPlayerViewController

static NSString * const kItemTracksKey = @"currentItem.tracks";
static NSString * const kRateKey = @"rate";

- (void)viewDidLoad
{
	[super viewDidLoad];
	self.playerView.backgroundColor = [UIColor blackColor];
	// Create a stream stitcher url with the url template provided by FreeWheel. Consult your FreeWheel sales engineer about this url template.
	// A random string and the content stream url should be replaced into the template.
	// The stream stitcher url should be used in both your player and stream stitcher helper.
	NSString *contentUrl = @"http://playerdemo.freewheel.tv/hls/stream/live.m3u8";
	self.streamStitcherUrl = [NSString stringWithFormat:@"http://hls.v.fwmrm.net/ad/g/1?_dv=2&nw=96749&caid=StreamStitcherDemo&asnw=96749&vdur=186&flag=+sltp+exvt+rema+slcb+aeti&prof=96749:hls-cocoa&resp=m3u8;module=LiteSSHelper&_fw_syncing_token=%@&_fw_lpu=%@;",
							  [NSString stringWithFormat:@"%d", arc4random() % 10000000],
							  (NSString *)CFBridgingRelease(CFURLCreateStringByAddingPercentEscapes(kCFAllocatorDefault,
																									(CFStringRef)contentUrl,
																									NULL,
																									(CFStringRef)@"!*'();:@&=+$,/?%#[]",
																									kCFStringEncodingUTF8 ))];

	// Optional: Set SDK Log Level
	[FWStreamStitcherHelper setLogLevel:5];
	// Create new Stream Stitcher Helper instance with the same Stream Stitcher URL.
	// When player switches to another stream, the current stream stitcher helper should be released and a new one should be created.
	self.streamStitcherHelper = [[FWStreamStitcherHelper alloc] initWithStreamStitcherUrl:[NSURL URLWithString:self.streamStitcherUrl]];

	[self configPlayer];
	
    // Configuration is done, ready to start.
    [self.streamStitcherHelper start];
}

-(void)configPlayer
{
	[self.player removeObserver:self forKeyPath:kItemTracksKey];
	[self.player removeObserver:self forKeyPath:kRateKey];
	self.player = [[AVPlayer alloc] initWithURL:[NSURL URLWithString:self.streamStitcherUrl]];
	[self.player addObserver:self forKeyPath:kItemTracksKey options:0 context:nil];
	[self.player addObserver:self forKeyPath:kRateKey options:0 context:nil];
	[self.playerView setPlayer:self.player];
	// Notify Stream Stitcher Helper the new AVPlayer instance.
	[self.streamStitcherHelper setAVPlayer:self.player];
}

- (IBAction)playButtonTouched:(id)sender
{
	if ([self.player rate] != 0.f) {
		[self.player pause];
	} else {
		[self.player play];
	}
}

- (void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary *)change context:(void *)context{
	AVPlayer *player = object;
	if (player != self.player) {
		return;
	}
	if ([keyPath isEqualToString:kItemTracksKey]){
		dispatch_async(dispatch_get_main_queue(), ^{
			bool audioOnly = YES;
			for (AVPlayerItemTrack* t in player.currentItem.tracks) {
				if ([t.assetTrack.mediaType isEqualToString:AVMediaTypeVideo]) {
					audioOnly = NO;
					break;
				}
			}
			if (audioOnly) {
				[self.statusLabel setText:@"AudioOnly"];
			} else {
				[self.statusLabel setText:@""];
			}
		});
	}
	else if ([keyPath isEqualToString:kRateKey]){
		if (player.rate == 0.0){
			[self.playButton setTitle:@"Play" forState:UIControlStateNormal];
		}
		else if (player.rate == 1.0){
			[self.playButton setTitle:@"Pause" forState:UIControlStateNormal];
		}
	}
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation{
	return YES;
}

- (void)viewWillDisappear:(BOOL)animated
{
	[self.player pause];
	[super viewWillDisappear:animated];
}

- (void)viewDidUnload {
	[self.streamStitcherHelper setAVPlayer:nil];
	[self setPlayButton:nil];
	[self setStatusLabel:nil];
	[self setPlayerView:nil];
	[super viewDidUnload];
}
@end
