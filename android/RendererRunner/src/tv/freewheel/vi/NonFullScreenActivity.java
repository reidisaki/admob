package tv.freewheel.vi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import tv.freewheel.ad.interfaces.IAdContext;
import tv.freewheel.ad.interfaces.IConstants;
import tv.freewheel.ad.interfaces.IEvent;
import tv.freewheel.ad.interfaces.IEventListener;
import tv.freewheel.ad.interfaces.ISlot;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.VideoView;

public class NonFullScreenActivity extends Activity implements OnCompletionListener, OnPreparedListener {
	private static final String CLASSTAG = "NonFullScreenActivity";
	private RendererConfiguration rendererConfig;
	private VideoView videoPlayer;
	private FrameLayout videoBase;
	private RelativeLayout siteBase;
	private MediaController mc;
	private IAdContext adContext;
	private IConstants adConstants;
	private ArrayList<ISlot> prerollSlots;
	private ArrayList<ISlot> postrollSlots;
	// mid roll related data structures
	private HashMap<Double, String> midrolls = new HashMap<Double, String>();
	private ArrayList<Double> midrollkeys = new ArrayList<Double>();
	private Timer checkTimer;
	private int currentPlayheadTime = -1;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		Log.d(CLASSTAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.nonfullscreen);
		this.siteBase = (RelativeLayout) findViewById(R.id.relativeLayout1);
		this.videoBase = (FrameLayout)findViewById(R.id.videoBase1);
		this.videoPlayer = (VideoView) findViewById(R.id.videoView1);
		this.checkTimer = new Timer();
		this.prepare();
	}
	
	@Override
	public void onStart() {
		super.onStart();
		Log.d(CLASSTAG, "onStart");
	}
	
	private void prepare() {
		Log.d(CLASSTAG, "prepare");
		String filePath = "/sdcard/response_.xml";
		try {
			new FileInputStream(new File(filePath));
		} catch (FileNotFoundException e) {
			Log.d(CLASSTAG, "response_.xml not found, use config.json");
			filePath = "/sdcard/config.json";
			Uri theUri = this.getIntent().getData();
			if (theUri != null) {
				filePath = theUri.getPath();
				Log.i(CLASSTAG, "external config path:" + filePath);
			}
			try {
				rendererConfig = new RendererConfiguration(new FileInputStream(new File(filePath)));
				rendererConfig.writeToFile();
				filePath = "/sdcard/" + rendererConfig.outputFilename;
			} catch (FileNotFoundException e1) {
				Log.e(CLASSTAG, "no config.json on sdcard, exit");
				finish();
			} catch (IOException e1) {
				Log.e(CLASSTAG, "IOError occur");
				finish();
			}
		}
		RendererRunnerActivity.ADMANAGER.setServer(filePath);
		this.setupVideo();
		this.sendAdRequestToFreeWheel();
	}
	
	private void setupVideo() {
		this.mc = new MediaController(this);
		this.mc.setMediaPlayer(this.videoPlayer);
		this.mc.hide();
		this.videoPlayer.setMediaController(this.mc);
		this.videoPlayer.setOnCompletionListener(this);
		this.videoPlayer.setOnPreparedListener(this);
		this.videoPlayer.setVisibility(View.GONE);
		Uri videoURI = Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/" + R.raw.fw_demo_content);
		videoPlayer.setVideoURI(videoURI);
	}
	
	public void sendAdRequestToFreeWheel() {
		IEventListener eventListener = new IEventListener(){	    
			public void run(final IEvent event) {
				onFreeWheelEvent(event);
			}
		};
		
		adContext = RendererRunnerActivity.ADMANAGER.newContext();
		adContext.setActivity(this);
		adConstants = adContext.getConstants();
		adContext.registerVideoDisplay(this.videoPlayer);
		
		if (this.rendererConfig != null) {
			int displaySlotCount = 0;
			for (BaseAd ad : this.rendererConfig.adsArray) {
				if (ad.slotType.equals("display")) {
					adContext.addSiteSectionNonTemporalSlot("display-" + String.valueOf(displaySlotCount++), 
							null, ad.slotWidth, ad.slotHeight, null, true, null, null);
				}
			}
		}
		
		adContext.addEventListener(adConstants.EVENT_REQUEST_COMPLETE(), eventListener);
		adContext.addEventListener(adConstants.EVENT_REQUEST_CONTENT_VIDEO_RESUME(), eventListener);
		adContext.addEventListener(adConstants.EVENT_SLOT_ENDED(), eventListener);
		
		if (this.rendererConfig != null) {
			adContext.addRenderer(this.rendererConfig.rendererClassName, null, null, null, null, null);
		}
		adContext.submitRequest(10.0);
	}
	
	protected void onFreeWheelEvent(IEvent event) {
		Log.i(CLASSTAG, event.getType());
		if (event.getType().equals(adConstants.EVENT_REQUEST_COMPLETE())) {
			Log.d(CLASSTAG, "ad request complete");
			this.prerollSlots = adContext.getSlotsByTimePositionClass(
					adConstants.TIME_POSITION_CLASS_PREROLL());
			this.postrollSlots = adContext.getSlotsByTimePositionClass(
					adConstants.TIME_POSITION_CLASS_POSTROLL());
			this.showDisplayAd();
			this.prepareMidRoll();
			this.playAdSlot(adConstants.TIME_POSITION_CLASS_PREROLL());
		} else if (event.getType().equals(adConstants.EVENT_SLOT_ENDED())) {
			String customId = (String) event.getData().get(adConstants.INFO_KEY_CUSTOM_ID());
			ISlot endedSlot = adContext.getSlotByCustomId(customId);
			if (endedSlot.getTimePositionClass() == adConstants.TIME_POSITION_CLASS_PREROLL() ||
					endedSlot.getTimePositionClass() == adConstants.TIME_POSITION_CLASS_POSTROLL()) {
				this.playAdSlot(endedSlot.getTimePositionClass());
			} else if (endedSlot.getTimePositionClass() == adConstants.TIME_POSITION_CLASS_MIDROLL()) {
				
			}
		} else if (event.getType().equals(adConstants.EVENT_REQUEST_CONTENT_VIDEO_RESUME())) {
			this.resumeMainVideo();
		}
	}
	
	public void playAdSlot(int tpc) {
		if (tpc == adConstants.TIME_POSITION_CLASS_PREROLL()) {
			if(!this.prerollSlots.isEmpty()) {
				ISlot slot = this.prerollSlots.remove(0);
				Log.d(CLASSTAG, "playing slot: " + slot.toString());
				slot.play();
			} else {
				this.playMainVideo();
			}
		} else if (tpc == adConstants.TIME_POSITION_CLASS_POSTROLL()) {
			if(!this.postrollSlots.isEmpty()) {
				ISlot slot = this.postrollSlots.remove(0);
				Log.d(CLASSTAG, "playing slot: " + slot.toString());
				slot.play();
			} else {
				this.finish();
			}
		}
	}
	
	public void showDisplayAd() {
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				ArrayList<ISlot> slots = adContext.getSlotsByTimePositionClass(adConstants.TIME_POSITION_CLASS_DISPLAY());
				View v = null;
				int id = 2012021613;
				for (ISlot slot : slots) {
					Log.d(CLASSTAG, "show display:" + slot.getCustomId());
					slot.play();
					RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
					p.addRule(RelativeLayout.CENTER_HORIZONTAL);
					if (v == null) {
						p.addRule(RelativeLayout.ALIGN_PARENT_TOP);
					} else {
						p.addRule(RelativeLayout.ALIGN_BOTTOM, v.getId());
					}
					
					v = slot.getBase();
					v.setId(id++);
					siteBase.addView(v, p);
				}
			}
		});
	}
	
	private void playMainVideo() {
		this.runOnUiThread(new Runnable() {
			@Override
			public void run () {
				videoPlayer.setVisibility(View.VISIBLE);
				videoPlayer.bringToFront();
				videoPlayer.requestFocus();
				if (currentPlayheadTime > 0) {
					videoPlayer.seekTo(currentPlayheadTime);
				}
				videoPlayer.start();
				adContext.setVideoState(adConstants.VIDEO_STATE_PLAYING());
			}
		});
	}
	
	private void prepareMidRoll() {
		final ArrayList<ISlot> mid_slots = adContext.getSlotsByTimePositionClass(
				adConstants.TIME_POSITION_CLASS_MIDROLL());
		
		Iterator<ISlot> iter = mid_slots.iterator();
		while (iter.hasNext()) {
			ISlot slot = iter.next();
			double tp = slot.getTimePosition();
			Log.i(CLASSTAG, "at " + tp + " has midroll");
			this.midrolls.put(tp, slot.getCustomId());
			this.midrollkeys.add(tp);
		}
		
		// get all overlay slots
		final ArrayList<ISlot> overlay_slots = adContext.getSlotsByTimePositionClass(
				adConstants.TIME_POSITION_CLASS_OVERLAY());
		
		// TODO: what if the overlay has the same time position with a mid-roll?
		iter = overlay_slots.iterator();
		while (iter.hasNext()) {
			ISlot slot = iter.next();
			double tp = slot.getTimePosition();
			Log.i(CLASSTAG, "at " + tp + " has overlay");
			this.midrolls.put(tp, slot.getCustomId());
			this.midrollkeys.add(tp);
		}
		
		Collections.sort(this.midrollkeys);
	}
	
	@Override
	public void onCompletion(MediaPlayer mp) {
		if(this.videoPlayer != null) {
			this.videoPlayer.setVisibility(View.GONE);
			this.videoBase.removeView(this.videoPlayer);
			this.videoPlayer = null;
		}
		
		if (this.adContext != null) {
			this.adContext.setVideoState(adConstants.VIDEO_STATE_COMPLETED());
			Log.d(CLASSTAG, "will play postroll");
			this.playAdSlot(adConstants.TIME_POSITION_CLASS_POSTROLL());
		} else {
			finish();
		}
	}
	
	@Override
	public void onPrepared(MediaPlayer mp) {
		Log.i(CLASSTAG, "video prepared");
		if (this.midrollkeys.size() > 0) {
			this.startMidrollDetectTask();
		}
	}
	
	private void startMidrollDetectTask() {
		// monitor play head time very 1s and insert midroll when time point reached
		this.checkTimer.cancel();
		this.checkTimer.purge();
		this.checkTimer = new Timer();
		
		TimerTask midRollDetectTask = new TimerTask() {
			@Override
			public void run() {
				
				int playHeadTime = videoPlayer.getCurrentPosition()/1000;
				
				Double firstMidrollTimePosition = midrollkeys.get(0);
				if (firstMidrollTimePosition <= playHeadTime) {
					String slotName = midrolls.get(firstMidrollTimePosition);
					midrollkeys.remove(0);
					midrolls.remove(firstMidrollTimePosition);
					
					if(midrollkeys.size() == 0) {
						checkTimer.cancel();
					}
					playMidroll(slotName);
				}
				
			}
		};
		
		this.checkTimer.scheduleAtFixedRate(midRollDetectTask, 1000, 1000);
	}
	
	private void playMidroll(String slotName) {
		Handler midrollHandler = new Handler(getMainLooper());

		if (slotName != null) {
			final ISlot slot = adContext.getSlotByCustomId(slotName);
			if (slot.getTimePositionClass() == this.adConstants.TIME_POSITION_CLASS_MIDROLL()) {
				this.stopMainVideo();
			}
			if (slot != null) {
				Log.d(CLASSTAG, "will play midroll/overlay: " + slotName);
				midrollHandler.post(new Runnable() {
					@Override
					public void run () {
						slot.play();
					}
				});
			}
		}
	}
	
	private void stopMainVideo() {
		this.currentPlayheadTime = this.videoPlayer.getCurrentPosition();
		this.checkTimer.cancel();
		this.adContext.setVideoState(adConstants.VIDEO_STATE_PAUSED());
		this.runOnUiThread(new Runnable() {
			@Override
			public void run () {
				videoPlayer.pause();
				videoPlayer.setVisibility(View.GONE);
			}
		});
	}
	
	private void resumeMainVideo() {
		this.playMainVideo();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		Log.d(CLASSTAG, "onPause");
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Log.d(CLASSTAG, "onResume");
	}
	
	@Override
	public void onRestart() {
		super.onRestart();
		Log.d(CLASSTAG, "onRestart");
		this.playMainVideo();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		Log.d(CLASSTAG, "onStop");
	}
}
