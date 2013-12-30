#import "FWPlayerCommon.h"

#define FWPLAYER_CONTENT_VIDEO_URL [NSURL URLWithString:@"http://mediapm.edgesuite.net/osmf/content/test/spacealonehd_sounas_640_700.mp4"]
#define FWPLAYER_MOVIE_PLAYER_SUPERVIEW_FRAME CGRectMake(10, 1, 270, 165)
#define FWPLAYER_MOVIE_PLAYER_VIEW_FRAME CGRectMake(0, 0, 270, 165)
#define FWPLAYER_AD_REQUEST_TIMEOUT 5
#define TIMELINE_TIMER_INTERVAL 2

@interface FWPlayerViewController : UIViewController {
	XRETAIN UIView *moviePlayerSuperview;
	XRETAIN MPMoviePlayerController *moviePlayerController;
	XRETAIN id<FWContext> adContext;
	XRETAIN NSMutableArray *temporalSlots;
	XASSIGN NSTimer *_tickTimer;
    XRETAIN UIView *_contentVideoView;
    XRETAIN UIView *_adVideoView;
    XRETAIN NSTimer *_timer;
	XRETAIN UIView *_airplayView;
}

@property (nonatomic, strong) UIView *moviePlayerSuperview;
@property (nonatomic, strong) MPMoviePlayerController *moviePlayerController;
@property (nonatomic, strong) id<FWContext> adContext;
@property (nonatomic, strong) NSMutableArray *temporalSlots;

- (void)dealloc;
- (void)_onAdRequestCompleted:(NSNotification *)notification;
- (void)_playPrerollAdSlots;
- (void)_playPostrollAdSlots;
- (void)_onAdSlotEnded:(NSNotification *)notification;
- (void)_playContentMovie;
- (void)_cleanup;

@end
