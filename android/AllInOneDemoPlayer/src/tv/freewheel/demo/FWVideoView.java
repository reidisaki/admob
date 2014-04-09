package tv.freewheel.demo;

import tv.freewheel.ad.interfaces.IAdContext;
import tv.freewheel.ad.interfaces.IConstants;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.VideoView;

/**
 * This class extends the standard Android VideoView. If a
 * FreeWheel IAdContext is set, then update the video state
 * in response to start(), play(), pause() calls. 
 * 
 * @author freewheeler
 *
 */
public class FWVideoView extends VideoView {

	private static final String TAG = "FWVideoView";
	
	private IAdContext fwContext = null;
	private IConstants fwConstants = null;
	
	private OnCompletionListener externalOnCompletionListener = null;
	private PlayPauseListener mPlayPauseListener = null;
	
	public FWVideoView(Context androidContext) {
		super(androidContext);
	}
	
	public FWVideoView(Context androidContext, AttributeSet attributeSet) {
		super(androidContext, attributeSet);
	}

	@Override
	public void setOnCompletionListener(OnCompletionListener l) {
		externalOnCompletionListener = l;

		// Register a listener that fires the provided listener l
		// as well as sets the video state to complete if a
		// FreeWheel IAdContext is provided
		super.setOnCompletionListener(new OnCompletionListener() {
			public void onCompletion(MediaPlayer mp) {
				if (externalOnCompletionListener != null) {
					externalOnCompletionListener.onCompletion(mp);
				}
				
				if (fwContext != null) {
					Log.d(TAG, "Setting video state to completed");
					fwContext.setVideoState(fwConstants.VIDEO_STATE_COMPLETED());
				}				
			}
		});
	}

	/**
	 * Provide a FreeWheel ad context to be used to communicate
	 * video playback state changes
	 * @param context
	 */
	public void setFWContext(IAdContext context) {
		this.fwContext = context;

		if (context != null) {
			this.fwConstants = fwContext.getConstants();
			this.setOnCompletionListener(null); // Do this in case a completion listener never gets set
		}
	}
	
	@Override
	public void pause() {
		super.pause();
		if (fwContext != null) {
			Log.d(TAG, "pause(): Setting video state to paused");
			fwContext.setVideoState(fwConstants.VIDEO_STATE_PAUSED());
		}
		if (this.mPlayPauseListener != null) {
            mPlayPauseListener.onPause();
        }
	}

	@Override
	public void resume() {
		super.resume();
		if (fwContext != null) {
			Log.d(TAG, "resume(): Setting video state to playing");
			fwContext.setVideoState(fwConstants.VIDEO_STATE_PLAYING());
		}
		if (this.mPlayPauseListener != null) {
            mPlayPauseListener.onPlay();
        }
	}

	@Override
	public void start() {
		super.start();
		if (fwContext != null) {
			Log.d(TAG, "start(): Setting video state to playing");
			fwContext.setVideoState(fwConstants.VIDEO_STATE_PLAYING());
		}
	}
	
    interface PlayPauseListener {
        void onPlay();
        void onPause();
    }
    
    public void setPlayPauseListener(PlayPauseListener listener) {
        mPlayPauseListener = listener;
    }
}
