//
//  StreamStitcherWithAVPlayerViewController.h
//  StreamStitcherDemo
//
//  Copyright (c) 2013 FreeWheel. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>
#import "HybridStreamStitcherSDK/FWSDK.h"


@interface PlayerView : UIView
@property (nonatomic) AVPlayer *player;
@end

@interface StreamStitcherWithAVPlayerViewController : UIViewController{
}

@property (nonatomic, strong) AVPlayer *player;
@property (nonatomic, strong) FWStreamStitcherHelper *streamStitcherHelper;
@property (strong, nonatomic) IBOutlet UIButton *playButton;
@property (strong, nonatomic) IBOutlet UILabel *statusLabel;
@property (strong, nonatomic) IBOutlet PlayerView *playerView;

@end
