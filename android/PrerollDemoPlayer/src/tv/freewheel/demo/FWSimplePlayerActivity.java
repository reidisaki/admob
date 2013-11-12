package tv.freewheel.demo;

import java.util.List;

import android.view.SurfaceView;
import tv.freewheel.ad.AdManager;
import tv.freewheel.ad.interfaces.IAdContext;
import tv.freewheel.ad.interfaces.IAdManager;
import tv.freewheel.ad.interfaces.IConstants;
import tv.freewheel.ad.interfaces.IEvent;
import tv.freewheel.ad.interfaces.IEventListener;
import tv.freewheel.ad.interfaces.ISlot;
import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;

/**
 * Implements a very simple video player that handles preroll advertising.
 * 
 * @author freewheeler
 *
 */
public class FWSimplePlayerActivity extends Activity implements MediaPlayer.OnErrorListener {
	
	private static final String TAG = "FWSimplePlayer";

	// Video file to play
	private Uri videoURI = null;
	
	// ***NOTE***
	// Each client gets their own network ID, AdManager URLs, and ad server URLs
	// Please see your FreeWheel account manager or sales engineer for the
	// appropriate values to use in your production player.
	private static final int fwNetworkId = 42015;
	private static final String fwAdsURL = "http://demo.v.fwmrm.net/";
	
	// Request parameters --- these may change from request to request
	// depending on how your content is arranged in the MRM system
	private static final String fwProfile = "fw_tutorial_android";	
	private static final String fwSiteSectionId = "fw_tutorial_android";
	private static final String fwVideoAssetId = "fw_simple_tutorial_asset";

	private FWVideoView videoPlayer = null;
	private SurfaceView holderView = null;
	private MediaController mediaController = null;
	private double videoDuration = 0.0;

	// Ideally, your IAdManager instance is reused throughout your app
	private IAdManager fwAdm = null;
	private IAdContext fwContext = null;
	private IConstants fwConstants = null;
	
	private List<ISlot> fwPrerollSlots;
	
    /** Get a random value for cache busting and tracking video views */
	private int random() {
		return (int)Math.floor(Math.random() * Integer.MAX_VALUE);
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Use our fw_tutorial raw resource
        videoURI = Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/" + R.raw.fw_tutorial);
        
        // Get and setup our video player
        videoPlayer = (FWVideoView) findViewById(R.id.videoView1);
		holderView = (SurfaceView) findViewById(R.id.holderView1);
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
		fwContext.setSiteSection(fwSiteSectionId, random(), 0, fwConstants.ID_TYPE_CUSTOM(), 0);
		fwContext.setVideoAsset(fwVideoAssetId, videoDuration, null, fwConstants.VIDEO_ASSET_AUTO_PLAY_TYPE_ATTENDED(), random(), 0, fwConstants.ID_TYPE_CUSTOM(), 0, fwConstants.VIDEO_ASSET_DURATION_TYPE_EXACT());
		
		fwContext.setActivity(this);
		fwContext.registerVideoDisplay(holderView);
		
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
		
		// Submit request with 3s timeout
		fwContext.submitRequest(3.0);
	}
	
	private void handleAdManagerRequestComplete() {
		Log.d(TAG, "Playing preroll slots");
		
		videoPlayer.setFWContext(fwContext);
		fwPrerollSlots = fwContext.getSlotsByTimePositionClass(fwConstants.TIME_POSITION_CLASS_PREROLL());

		// Play preroll slots, then play main video
		
		// We do this by treating slots returned from AdManager as a stack
		// and play next slot once the current slot ends:
		
		fwContext.addEventListener(fwConstants.EVENT_SLOT_ENDED(), new IEventListener() {
			public void run(IEvent e) {
				String completedSlotID = (String)e.getData().get(fwConstants.INFO_KEY_CUSTOM_ID());
				ISlot completedSlot = fwContext.getSlotByCustomId(completedSlotID);
				Log.d(TAG, "Completed playing slot: " + completedSlotID);
				
				// EVENT_SLOT_ENDED could be fired for several types of slots
				// (pre-, mid-, post-, pause, overlay, display)
				if (completedSlot.getTimePositionClass() == fwConstants.TIME_POSITION_CLASS_PREROLL()) {
					playNextPreroll();
				}
			}
		});

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

	public void playMainVideo() {
		Log.d(TAG, "Starting main video");
		
		videoPlayer.setOnErrorListener(this);

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
		if (fwContext != null) {
			fwContext.setActivityState(fwConstants.ACTIVITY_STATE_PAUSE());
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (fwContext != null) {
			fwContext.setActivityState(fwConstants.ACTIVITY_STATE_RESUME());
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
}
