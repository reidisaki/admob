package tv.freewheel.demo;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import tv.freewheel.ad.AdManager;
import tv.freewheel.ad.interfaces.IAdContext;
import tv.freewheel.ad.interfaces.IAdManager;
import tv.freewheel.ad.interfaces.IConstants;
import tv.freewheel.ad.interfaces.IEvent;
import tv.freewheel.ad.interfaces.IEventListener;
import tv.freewheel.ad.interfaces.ISlot;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.Toast;

/**
 * Implements a video player that handles advertising of all kinds of ads.
 * 
 * @author freewheeler
 *
 */
public class FWPlayerActivity extends Activity implements MediaPlayer.OnErrorListener {
	
	private static final String TAG = "FWPlayer";

	// Video file to play
	private Uri videoURI = null;
	
	
	private int fwNetworkId;
	private String fwAdsURL;
	private String fwProfile;	
	private String fwSiteSectionId;
	private String fwVideoAssetId;

	private FWVideoView videoPlayer = null;
	private FrameLayout videoFrameLayout = null;
	private FrameLayout displayFrameLayout = null;
	private MediaController mediaController = null;
	private ImageButton pauseAdCloseButton = null;
	private double videoDuration = 0.0;

	// Ideally, your IAdManager instance is reused throughout your app
	private IAdManager fwAdm = null;
	private IAdContext fwContext = null;
	private IConstants fwConstants = null;
	
	private List<ISlot> fwPrerollSlots;
	private List<ISlot> fwPostrollSlots;
	private List<ISlot> fwPauseMidrollSlots;
	private List<ISlot> fwMidrollAndOverlaySlots;
	private ISlot fwDisplaySlot;
	private ISlot currentTemporalSlot;
	
	private int deviceWidthInDip;
	private int deviceHeightInDip;
	private DisplayMetrics displaymetrics;
	private int pausedTimePosition = 0;
	private boolean appInFront = true;
	private Timer fwCuepointTimer = null;
	private boolean started = false;
	
    /** Get a random value for cache busting and tracking video views */
	private int random(int ceiling) {
		return (int)Math.floor(Math.random() * ceiling);
	}
	
	private void showActionBar() {
		this.getActionBar().show();
	}
	
	private void hideActionBar() {
		this.getActionBar().hide();
	}
	
	private void loadFWConfig() {
		this.fwAdsURL = FWConfig.adserverUrl;
		this.fwNetworkId = FWConfig.networkId;
		this.fwProfile = FWConfig.profile;
		this.fwSiteSectionId = FWConfig.siteSectionId;
		this.fwVideoAssetId = FWConfig.videoAssetId;
	}
	
	private void loadDeviceDimension() {
		displaymetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		int height = displaymetrics.heightPixels;
		int width = displaymetrics.widthPixels;
		this.deviceWidthInDip = (int) (width/displaymetrics.density);
		this.deviceHeightInDip = (int) (height/displaymetrics.density);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case R.id.action_settings:
	        this.startConfigActivity();
	        break;
	    default:
	        break;
	    }
	    return true;
	}
	
    private void startConfigActivity() {
    	Intent intent = new Intent(this, FWConfigActivity.class);
    	this.startActivityForResult(intent, 1);
	}
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode == 1) {
    		if(resultCode == RESULT_OK) {
    			this.refreshAds();
    	    } else if (resultCode == RESULT_CANCELED) {    
    	        Log.d(FWPlayerActivity.TAG, "Settings change canceled");
    	    }
    	}
    }
    
    private void refreshAds() {
    	if (this.currentTemporalSlot != null) {
    		this.currentTemporalSlot.stop();
    	}
    	if (this.fwDisplaySlot != null) {
    		this.fwDisplaySlot.stop();
    		this.displayFrameLayout.removeAllViews();
    	}
    	this.videoPlayer.stopPlayback();
    	if (this.fwCuepointTimer != null) {
    		this.fwCuepointTimer.cancel();
    		this.fwCuepointTimer = null;
    	}
    	fwContext.setVideoState(fwConstants.VIDEO_STATE_COMPLETED());
    	this.fwContext = null;
    	started = false;
    	this.hideActionBar();
    	this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    	
    	this.loadFWConfig();
    	this.initAdManager();
    }

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.hideActionBar();

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        this.loadFWConfig();
        this.loadDeviceDimension();
        setContentView(R.layout.main);
        
        videoURI = Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/" + R.raw.fw_tutorial);
        
        // Get and setup our video player
        videoPlayer = (FWVideoView) findViewById(R.id.videoView1);
		videoFrameLayout = (FrameLayout)findViewById(R.id.frameLayout1);
		displayFrameLayout = (FrameLayout)findViewById(R.id.displayView);
		pauseAdCloseButton = (ImageButton)findViewById(R.id.pauseAdCloseButton);
        mediaController = new MediaController(this);
        mediaController.setMediaPlayer(videoPlayer);
        mediaController.hide();
        videoPlayer.setMediaController(mediaController);
  
        buildVideoData();
    }
	
    /**
     * Obtain any needed asset metadata
     */
	private void buildVideoData() {
		// Obtain duration by preparing the video content via setVideoPath
		
		// Once main video content is prepared our OnPreparedListener will
		// execute
		
		videoPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
			public void onPrepared(MediaPlayer mp) {
				videoDuration = mp.getDuration() / 1000;
				Log.d(TAG, "Got video duration " + videoDuration);
				videoPlayer.setOnPreparedListener(null);
				initAdManager();
			}
		});
		
		Log.d(TAG, "Attempting to obtain video duration from content");
		videoPlayer.setOnErrorListener(this);
		videoPlayer.setVideoURI(videoURI);
	}
	
	private void initAdManager() {
    	Log.d(TAG, "Loading AdManager");
		fwAdm = AdManager.getInstance(this.getApplicationContext());
		fwAdm.setServer(fwAdsURL);
		fwAdm.setNetwork(fwNetworkId);

		submitAdRequest();
	}
	
	private void submitAdRequest() {
		
		// A context is a specific ad request/response pair.
		// A context may represent one or more ad slots
		
		fwContext = fwAdm.newContext();
		fwConstants = fwContext.getConstants();
		
		// Setup the profile, site section, and video asset info
		fwContext.setProfile(fwProfile, null, null, null);
		fwContext.setSiteSection(fwSiteSectionId, random(Integer.MAX_VALUE), 0, fwConstants.ID_TYPE_CUSTOM(), 0);
		fwContext.setVideoAsset(fwVideoAssetId, videoDuration, null, fwConstants.VIDEO_ASSET_AUTO_PLAY_TYPE_ATTENDED(), random(Integer.MAX_VALUE), 0, fwConstants.ID_TYPE_CUSTOM(), 0, fwConstants.VIDEO_ASSET_DURATION_TYPE_EXACT());
		
		// add a display slot which accepts only interstitial ads
		fwContext.addSiteSectionNonTemporalSlot("splash_slot", null, this.deviceWidthInDip, this.deviceHeightInDip, null, false, null, null, 0, FWConfig.splashCompatibleDimensions);
		
		// add a display slot which accepts companion
		fwContext.addSiteSectionNonTemporalSlot("display_slot", null, FWConfig.displayWidth, FWConfig.displayHeight, null, true, null, null);
		
		// add temporal slots
		fwContext.addTemporalSlot("pre_slot", "preroll", 0, null, 0, 0, null, null, 0);
		fwContext.addTemporalSlot("post_slot", "postroll", 60, null, 0, 0, null, null, 0);
		fwContext.addTemporalSlot("mid_slot", "midroll", 5, null, 0, 0, null, null, 0);
		fwContext.addTemporalSlot("overlay_slot", "overlay", 2, null, 0, 0, null, null, 0);
		fwContext.addTemporalSlot("pause_slot", "pause_midroll", -1, null, -1, 0, null, null, 0);
		fwContext.addTemporalSlot("pause_slot2", "pause_midroll", -1, null, -1, 0, null, null, 0);
		
		fwContext.setActivity(this);
		fwContext.registerVideoDisplayBase(this.videoFrameLayout);
		
		// Listen for the request complete event
		fwContext.addEventListener(fwConstants.EVENT_REQUEST_COMPLETE(), new IEventListener() {
			public void run(IEvent e) {
				String eType = e.getType();
				String eSuccess = e.getData().get(fwConstants.INFO_KEY_SUCCESS()).toString();
				
				if (fwConstants != null) {
					if (fwConstants.EVENT_REQUEST_COMPLETE().equals(eType) &&
							Boolean.valueOf(eSuccess)) {
						Log.d(TAG, "Request completed successfully");
						Log.d(TAG, "Ads booked in placement 307877 are expected to return.");
						handleAdManagerRequestComplete();
					} else {
						Log.d(TAG, "Request failed. Playing main content.");
						playMainVideo();
					}
				}
			}
		});
		
		// Submit request with 5s timeout
		fwContext.submitRequest(5.0);
	}
	
	private void handleAdManagerRequestComplete() {
		Log.d(TAG, "Playing preroll slots");
		
		videoPlayer.setFWContext(fwContext);
		
		// Retrieve temporal slots from ad context
		fwPrerollSlots = fwContext.getSlotsByTimePositionClass(fwConstants.TIME_POSITION_CLASS_PREROLL());
		fwPostrollSlots = fwContext.getSlotsByTimePositionClass(fwConstants.TIME_POSITION_CLASS_POSTROLL());
		fwPauseMidrollSlots = fwContext.getSlotsByTimePositionClass(fwConstants.TIME_POSITION_CLASS_PAUSE_MIDROLL());
		fwMidrollAndOverlaySlots = fwContext.getSlotsByTimePositionClass(fwConstants.TIME_POSITION_CLASS_MIDROLL());
		fwMidrollAndOverlaySlots.addAll(fwContext.getSlotsByTimePositionClass(fwConstants.TIME_POSITION_CLASS_OVERLAY()));
		
		// We do this by treating slots returned from AdManager as a stack
		// and play next slot once the current slot ends:
		fwContext.addEventListener(fwConstants.EVENT_SLOT_STARTED(), new IEventListener() {
			public void run(IEvent e) {
				String startedSlotID = (String)e.getData().get(fwConstants.INFO_KEY_CUSTOM_ID());
				ISlot startedSlot = fwContext.getSlotByCustomId(startedSlotID);
				Log.d(TAG, "Started playing slot: " + startedSlotID);
				if (startedSlot.getTimePositionClass() != fwConstants.TIME_POSITION_CLASS_DISPLAY()) {
					currentTemporalSlot = startedSlot;
				}
			}
		});
		fwContext.addEventListener(fwConstants.EVENT_SLOT_ENDED(), new IEventListener() {
			public void run(IEvent e) {
				String completedSlotID = (String)e.getData().get(fwConstants.INFO_KEY_CUSTOM_ID());
				ISlot completedSlot = fwContext.getSlotByCustomId(completedSlotID);
				Log.d(TAG, "Completed playing slot: " + completedSlotID);
				
				// EVENT_SLOT_ENDED could be fired for several types of slots
				// (pre-, mid-, post-, pause, overlay, display)
				if (completedSlot == null) {
					return;
				}
				if (completedSlot.getTimePositionClass() != fwConstants.TIME_POSITION_CLASS_DISPLAY()) {
					currentTemporalSlot = null;
				}
				if (completedSlot.getTimePositionClass() == fwConstants.TIME_POSITION_CLASS_PREROLL()) {
					playNextPreroll();
				} else if (completedSlot.getTimePositionClass() == fwConstants.TIME_POSITION_CLASS_POSTROLL()) {
					playNextPostroll();
				} else if (completedSlot.getTimePositionClass() == fwConstants.TIME_POSITION_CLASS_PAUSE_MIDROLL()) {
					pauseAdCloseButton.setVisibility(View.INVISIBLE);
					videoPlayer.setVisibility(View.VISIBLE);
					videoPlayer.seekTo(pausedTimePosition);
				} else if (completedSlot.getCustomId().equals("splash_slot")) {
					enterMainActivity();
				}
			}
		});
		fwContext.addEventListener(fwConstants.EVENT_AD_IMPRESSION(), new IEventListener() {
			@Override
			public void run(IEvent e) {
				String customId = (String) e.getData().get(fwConstants.INFO_KEY_CUSTOM_ID());
				ISlot slot = fwContext.getSlotByCustomId(customId);
				int adId = (Integer) e.getData().get(fwConstants.INFO_KEY_AD_ID());
				Log.d(TAG, "Ad event: " + e.getType() + ", slot customId: " + customId + ", adId: " + adId);
				
				// stop the splash slot after the splash ad lasts for 5 seconds
				
				if (customId.equals("splash_slot")) {
					Timer splash_timer = new Timer();
					splash_timer.schedule(new TimerTask() {
						@Override
						public void run() {
							new Handler(getMainLooper()).post(new Runnable() {
								public void run() {
									ISlot splashSlot = fwContext.getSlotByCustomId("splash_slot");
									splashSlot.stop();
								}
							});
						}
					}, 5000);
				} else if (slot.getTimePositionClass() == fwConstants.TIME_POSITION_CLASS_PAUSE_MIDROLL()) {
					pauseAdCloseButton.setVisibility(View.VISIBLE);
				} else if (slot.getTimePositionClass() == fwConstants.TIME_POSITION_CLASS_MIDROLL()) {
					if (videoPlayer.isShown()) {
						videoPlayer.setVisibility(View.INVISIBLE);
					}
				}
			}
		});
		fwContext.addEventListener(fwConstants.EVENT_REQUEST_CONTENT_VIDEO_PAUSE(), new IEventListener() {
			@Override
			public void run(IEvent e) {
				pausedTimePosition = videoPlayer.getCurrentPosition();
				videoPlayer.pauseByPlayer();
			}
		});
		fwContext.addEventListener(fwConstants.EVENT_REQUEST_CONTENT_VIDEO_RESUME(), new IEventListener() {
			@Override
			public void run(IEvent e) {
				videoPlayer.setVisibility(View.VISIBLE);
				videoPlayer.seekTo(pausedTimePosition);
				videoPlayer.start();
			}
		});
		
		// show splash slot
		ISlot splashSlot = this.fwContext.getSlotByCustomId("splash_slot");
		if (splashSlot != null && splashSlot.getAdInstances().size() > 0) {
			splashSlot.play();
		} else {
			this.enterMainActivity();
		}
	}
	
	private void enterMainActivity() {
		started = true;
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			this.showActionBar();
		}
		
		// play standalone display
		fwDisplaySlot = fwContext.getSlotByCustomId("display_slot");
		if (fwDisplaySlot != null) {
		    this.displayFrameLayout.addView(fwDisplaySlot.getBase(),  FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
		    if (fwDisplaySlot.getAdInstances().size() > 0)
			    fwDisplaySlot.play();
		    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
		    	this.displayFrameLayout.setVisibility(View.VISIBLE);
		    } else {
		    	this.displayFrameLayout.setVisibility(View.INVISIBLE);
		    }
		}
		
		this.playPrerollSlots();
	}
	
	private void playPrerollSlots() {
		// Play preroll slots, then play main video
		// Hide video player so that advertising is visible then start playing prerolls
		// Android VideoView visibility can only be set by thread that created the VideoView instance
		new Handler(this.getMainLooper()).post(new Runnable() {
			public void run() {
				videoPlayer.setVisibility(View.GONE);
				playNextPreroll();
			}
		});
	}
	
	private void playNextPreroll() {
		if (fwPrerollSlots != null) {
			if (fwPrerollSlots.size() > 0) {
				ISlot nextSlot = fwPrerollSlots.remove(0);
				Log.d(TAG, "Playing preroll slot: " + nextSlot.getCustomId());
				nextSlot.play();
			} else {
				Log.d(TAG, "Finished all prerolls. Starting main content.");
				playMainVideo();
			}
		} else {
			playMainVideo();
		}
	}
	
	private void playNextPostroll() {
		if (fwPostrollSlots != null) {
			if (fwPostrollSlots.size() > 0) {
				ISlot nextSlot = fwPostrollSlots.remove(0);
				Log.d(TAG, "Playing postroll slot: " + nextSlot.getCustomId());
				nextSlot.play();
			} else {
				Log.d(TAG, "Finished all postrolls.");
				new Handler(getMainLooper()).post(new Runnable() {
					public void run() {
				        videoPlayer.setVisibility(View.VISIBLE);
					}
				});
			}
		}
	}

	public void playMainVideo() {
		Log.d(TAG, "Starting main video");
		
		videoPlayer.setOnErrorListener(this);
		videoPlayer.setPlayPauseListener(new FWVideoView.PlayPauseListener() {
		    @Override
		    public void onPlay() {
		        Log.d(FWPlayerActivity.TAG, "Play/Resume content video");
		        if (fwContext == null) {
		        	return;
		        }
		        if (currentTemporalSlot != null && currentTemporalSlot.getTimePositionClass() == fwConstants.TIME_POSITION_CLASS_PAUSE_MIDROLL()) {
		        	currentTemporalSlot.stop();
		        }
		    }

		    @Override
		    public void onPause() {
		    	Log.d(FWPlayerActivity.TAG, "Pause content video");
		    	if (fwContext == null) {
		        	return;
		        }
		    	pausedTimePosition = videoPlayer.getCurrentPosition();
		    	if (appInFront && currentTemporalSlot == null && fwPauseMidrollSlots.size() > 0 && currentTemporalSlot == null) {
		    		int index = random(fwPauseMidrollSlots.size());
		    		ISlot pauseMidroll = fwPauseMidrollSlots.get(index);
		    		videoPlayer.setVisibility(View.INVISIBLE);
		    		pauseMidroll.play();
		    	}
		    }
		});
		videoPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mediaPlayer) {
				new Handler(getMainLooper()).post(new Runnable() {
					public void run() {
						fwContext.setVideoState(fwConstants.VIDEO_STATE_COMPLETED());
						videoPlayer.setVisibility(View.INVISIBLE);
						pausedTimePosition = 0;
						if (fwCuepointTimer != null) {
							fwCuepointTimer.cancel();
							fwCuepointTimer = null;
						}
						playNextPostroll();
					}
				});
			}	
		});
		
		if (this.fwMidrollAndOverlaySlots != null && this.fwMidrollAndOverlaySlots.size() > 0) {
			Log.d(TAG, "Start midroll/overlay timer");
			this.fwCuepointTimer = new Timer();
			this.fwCuepointTimer.scheduleAtFixedRate(new TimerTask() {
				  @Override
				  public void run() {
					  if (fwMidrollAndOverlaySlots.size() > 0) {
						  int i;
						  for (i=0; i < fwMidrollAndOverlaySlots.size(); i++) {
							  ISlot slot = fwMidrollAndOverlaySlots.get(i);
							  int currentPosition = videoPlayer.getCurrentPosition();
							  int slotTimePosition = (int)slot.getTimePosition() * 1000;
							  double diff = slotTimePosition > currentPosition? slotTimePosition  - currentPosition : currentPosition - slotTimePosition;
							  if (diff <= 1000) {
								  Log.d(TAG, "Playing midroll or overlay slot:" + slot.getCustomId());
								  break;
							  }
						  }
						  if (i == fwMidrollAndOverlaySlots.size())
							  return;
						  final int index = i;
						  new Handler(getMainLooper()).post(new Runnable() {
							  public void run() {
								  ISlot slotToPlay = fwMidrollAndOverlaySlots.get(index);
								  fwMidrollAndOverlaySlots.remove(index);
								  slotToPlay.play();
							  }
						  });
					  }
				  }
				}, 500, 500);
		}

		new Handler(this.getMainLooper()).post(new Runnable() {
			public void run() {
				mediaController.show(3);
				videoPlayer.setVisibility(View.VISIBLE);
				
				// Remember that we've extended the Android VideoView class.
				// Our FWVideoView class will call .setVideoState() on
				// the IAdContext when we call videoPlayer.start().
				videoPlayer.start();
			}
		});
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.e(TAG, "MediaPlayer encountered an unexpected error. what=" + what + ", extra=" + extra);
		return false;
	}
	
    @Override
	protected void onPause() {
		super.onPause();
		appInFront = false;
		if (fwContext != null) {
			fwContext.setActivityState(fwConstants.ACTIVITY_STATE_PAUSE());
		}
		if (videoPlayer.isPlaying()) {
			videoPlayer.pauseByPlayer();
			pausedTimePosition = videoPlayer.getCurrentPosition();
		}
		if (currentTemporalSlot != null) {
			currentTemporalSlot.pause();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		appInFront = true;
		if (fwContext != null) {
			fwContext.setActivityState(fwConstants.ACTIVITY_STATE_RESUME());
		}
		if (pausedTimePosition > 0 && !videoPlayer.isPausedByUser() && (currentTemporalSlot == null || currentTemporalSlot.getTimePositionClass() == fwConstants.TIME_POSITION_CLASS_OVERLAY())) {
			videoPlayer.seekTo(pausedTimePosition);
			videoPlayer.resumeByPlayer();
		}
		if (currentTemporalSlot != null) {
			currentTemporalSlot.resume();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (fwContext != null) {
			fwContext.setActivityState(fwConstants.ACTIVITY_STATE_START());
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (fwContext != null) {
			fwContext.setActivityState(fwConstants.ACTIVITY_STATE_STOP());
		}
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		if (fwContext != null) {
			fwContext.setActivityState(fwConstants.ACTIVITY_STATE_RESTART());
		}
	}
	
	private void goFullscreen() {
		new Handler(this.getMainLooper()).post(new Runnable() {
			public void run() {
		        displayFrameLayout.setVisibility(View.INVISIBLE);
		        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		        videoFrameLayout.setLayoutParams(p);
		        videoFrameLayout.invalidate();
			}
		});
	}
	
	private void goNonFullScreen() {
		new Handler(this.getMainLooper()).post(new Runnable() {
			public void run() {
		        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int)(500 * displaymetrics.density));
		        videoFrameLayout.setLayoutParams(p);
		        videoFrameLayout.invalidate();
		        displayFrameLayout.setVisibility(View.VISIBLE);
			}
		});
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
	        Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
	        this.loadDeviceDimension();
	        this.hideActionBar();
	        this.goFullscreen();
	    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
	        Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
	        this.loadDeviceDimension();
	        if (started) {
	        	this.showActionBar();
	        	this.goNonFullScreen();
	        }
	    }
	}
	
	public void onPauseAdCloseButtonClicked(View view) {
		Log.d(TAG, "Pause ad close button clicked");
		new Handler(this.getMainLooper()).post(new Runnable() {
			public void run() {
				pauseAdCloseButton.setVisibility(View.INVISIBLE);
				if (currentTemporalSlot != null && currentTemporalSlot.getTimePositionClass() == fwConstants.TIME_POSITION_CLASS_PAUSE_MIDROLL()) {
					currentTemporalSlot.stop();
				}
			}
		});
	}
}
