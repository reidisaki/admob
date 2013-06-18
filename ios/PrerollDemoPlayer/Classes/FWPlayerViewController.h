#import "FWPlayerCommon.h"

#define FWPLAYER_CONTENT_VIDEO_URL [NSURL URLWithString:@"http://playerdemo.freewheel.tv/jsam/22s.m4v"]
#define FWPLAYER_MOVIE_PLAYER_SUPERVIEW_FRAME CGRectMake(10, 1, 270, 165)
#define FWPLAYER_MOVIE_PLAYER_VIEW_FRAME CGRectMake(0, 0, 270, 165)
#define FWPLAYER_AD_REQUEST_TIMEOUT 5

@interface FWPlayerViewController : UIViewController {
	XRETAIN UIView *moviePlayerSuperview;
	XRETAIN MPMoviePlayerController *moviePlayerController;
	XRETAIN id<FWContext> adContext;
	XRETAIN NSMutableArray *temporalSlots;
	XASSIGN NSTimer *_tickTimer;
}

@property (nonatomic, strong) UIView *moviePlayerSuperview;
@property (nonatomic, strong) MPMoviePlayerController *moviePlayerController;
@property (nonatomic, strong) id<FWContext> adContext;
@property (nonatomic, strong) NSMutableArray *temporalSlots;

- (void)dealloc;
- (void)_loadMoviePlayerCover;
- (void)_playMovie;
- (void)_onAdRequestCompleted:(NSNotification *)notification;
- (void)_playPrerollAdSlots;
- (void)_playPostrollAdSlots;
- (void)_onAdSlotEnded:(NSNotification *)notification;
- (void)_playContentMovie;
- (void)_onMovieLoadStateChanged:(NSNotification *)notification;
- (void)_onMoviePlaybackFinished:(NSNotification *)notification;
- (void)_onMoviePlaybackStateDidChange:(NSNotification *)notification;
- (void)_cleanup;

@end
