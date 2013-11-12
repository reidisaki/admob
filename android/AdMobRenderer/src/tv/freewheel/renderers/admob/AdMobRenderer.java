package tv.freewheel.renderers.admob;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.Bundle;
import android.os.Handler;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import com.google.ads.*;
import com.google.ads.searchads.SearchAdRequest;
import tv.freewheel.ad.interfaces.IConstants;
import tv.freewheel.ad.interfaces.ISlot;
import tv.freewheel.utils.Logger;
import tv.freewheel.utils.renderer.RendererTimer;
import tv.freewheel.ad.interfaces.IAdInstance;
import tv.freewheel.ad.interfaces.ICreativeRendition;
import tv.freewheel.renderers.interfaces.IRenderer;
import tv.freewheel.renderers.interfaces.IRendererContext;

public class AdMobRenderer implements IRenderer, AdListener, RendererTimer.IRendererTimerService {
	private IRendererContext rendererContext = null;
	private IConstants constants = null;
	private Parameters params = null;
	private IAdInstance adInstance = null;
	private ISlot slot = null;

	// AdMob
	private AdRequest admobAdRequest = null;
	private Ad admobAd = null;

	// auto-refresh
	private boolean firstAd = true;

	// duration
	private double duration = -1;

	// renderer timer
	private AtomicInteger playHeadTime = new AtomicInteger(-1);
	private RendererTimer rendererTimer = null;

	private boolean interstitialDismissed = false;

	private static int RENDERER_STATE_PLAYING = 0;
	private static int RENDERER_STATE_PAUSED = 1;
	private static int RENDERER_STATE_PENDING = 2; // need to show ad on resume
	private static int RENDERER_STATE_STOPPED = 3;
	private AtomicInteger rendererState = new AtomicInteger(RENDERER_STATE_PLAYING);

	private Logger logger;

	public AdMobRenderer() {
		this.logger = Logger.getLogger(this);
		this.logger.info(FreeWheelVersion.RENDERER_VERSION);
	}
	
	@Override
	public void load(IRendererContext rendererContext) {
		this.logger.info("load");
		this.rendererContext = rendererContext;
		this.constants = this.rendererContext.getConstants();
		this.adInstance = this.rendererContext.getAdInstance();
		this.slot = this.adInstance.getSlot();
		this.params = Parameters.parseParameters(this.rendererContext);

		if (!this.params.validate()) {
			failWithError(this.params.errorCode, this.params.errorString);
			return;
		}

		ICreativeRendition cr = adInstance.getActiveCreativeRendition();

		if (cr.getBaseUnit().toLowerCase().contains("interstitial")
				|| cr.getContentType().toLowerCase().contains("interstitial")) {
			// interstitial
			if (slot.getTimePositionClass() == this.constants.TIME_POSITION_CLASS_OVERLAY()) {
				failWithError(this.constants.ERROR_INVALID_SLOT(),
						"Interstitial Ad is not supported in overlay slot");
				return;
			} else if (cr.getContentType().equalsIgnoreCase("external/admob-view")) {
				failWithError(this.constants.ERROR_INVALID_VALUE(),
						"baseUnit interstitial is incompatible with contentType external/admob-view");
				return;
			} else {
				this.logger.debug("Interstitial Ad");
				this.admobAdRequest = new AdRequest();
				this.admobAd = new InterstitialAd(this.rendererContext.getActivity(),
						this.params.publisherId, this.params.shortTimeout);
			}
		} else {
			// banner Ad
			if (this.slot.getType() == this.constants.SLOT_TYPE_TEMPORAL()) {
				if (cr.getDuration() <= 0.) {
					failWithError(this.constants.ERROR_INVALID_VALUE(), "Invalid duration");
					return;
				}
				this.duration = cr.getDuration();
				this.logger.debug("Duration " + this.duration);
			}

			if (this.params.searchString != null) {
				// search Ad
				this.logger.debug("Search Ad for string " + this.params.searchString);
				SearchAdRequest searchAdRequest = new SearchAdRequest();
				searchAdRequest.setQuery(this.params.searchString);

				if (this.params.backgroundColor != 0) {
					searchAdRequest.setBackgroundColor(this.params.backgroundColor);
				}
				if (this.params.headerTextColor != 0) {
					searchAdRequest.setHeaderTextColor(this.params.headerTextColor);
				}
				if (this.params.descriptionTextColor != 0) {
					searchAdRequest.setDescriptionTextColor(this.params.descriptionTextColor);
				}
				this.admobAdRequest = searchAdRequest;
			} else {
				this.logger.debug("Banner Ad");
				this.admobAdRequest = new AdRequest();
			}

			AdSize adSize = AdSize.BANNER;
			if (this.params.bannerSize != null) {
				this.logger.debug("Parameter set AdSize " + adSize);
				adSize = this.params.bannerSize;
			} else {
				int bannerWidth = 0, bannerHeight = 0;
				if (cr.getWidth() > 0 && cr.getHeight() > 0) {
					bannerWidth = cr.getWidth();
					bannerHeight = cr.getHeight();
					this.logger.debug("Creative size " + bannerWidth + "x" + bannerHeight);
				} else if (this.slot.getWidth() > 0 && this.slot.getHeight() > 0) {
					bannerWidth = this.slot.getWidth();
					bannerHeight = this.slot.getHeight();
					this.logger.debug("Slot size " + bannerWidth + "x" + bannerHeight);
				}

				// find first matched AdSize, it is the biggest that can fit into creative/slot size
				for (AdSize s : Parameters.adSizes) {
					if (bannerWidth >= s.getWidthInPixels(this.rendererContext.getActivity())
							&& bannerHeight >= s.getHeightInPixels(this.rendererContext.getActivity())) {
						adSize = s;
						this.logger.debug("Fit AdSize " + adSize);
						break;
					}
				}
			}

			this.admobAd = new AdView(this.rendererContext.getActivity(),adSize, this.params.publisherId);
		}

		if (this.params.dateOfBirth != null) {
			this.admobAdRequest.setBirthday(this.params.dateOfBirth);
		}
		if (this.params.gender != null) {
			this.admobAdRequest.setGender(this.params.gender);
		}
		if (this.params.keywords != null) {
			this.admobAdRequest.addKeyword(this.params.keywords);
		}
		if (this.params.testDeviceIds != null && !this.params.testDeviceIds.isEmpty()) {
			Set<String> testDeviceIds = new HashSet<String>(this.params.testDeviceIds);
			testDeviceIds.add(AdRequest.TEST_EMULATOR);
			this.admobAdRequest.setTestDevices(testDeviceIds);
		}
		if (this.rendererContext.getLocation() != null) {
			this.admobAdRequest.setLocation(this.rendererContext.getLocation());
		}

		this.admobAd.setAdListener(this);
		this.rendererContext.setRendererCapability(this.constants.EVENT_AD_CLICK(),
				this.constants.CAPABILITY_STATUS_OFF());
		this.rendererContext.dispatchEvent(this.constants.EVENT_AD_LOADED());
	}

	@Override
	public void start() {
		this.logger.info("start");
		admobAd.loadAd(admobAdRequest);
	}

	@Override
	public void pause() {
		this.logger.info("pause");
		this.rendererState.set(RENDERER_STATE_PAUSED);
		if (this.rendererTimer != null) {
			this.rendererTimer.pause();
		}
	}

	@Override
	public void resume() {
		this.logger.info("resume");
		if (this.rendererState.getAndSet(RENDERER_STATE_PLAYING) == RENDERER_STATE_PENDING) {
			this.logger.debug("Received ad when pause, show it now");
			new Handler(this.rendererContext.getActivity().getMainLooper()).post(new showAdTask());
			return;
		}

		// normal pause/resume
		if (this.rendererTimer != null) {
			this.rendererTimer.resume();
		}

		// resume when interstitial dismissed
		if (this.admobAd instanceof InterstitialAd && interstitialDismissed) {
			this.stop();
		}
	}

	@Override
	public void stop() {
		this.logger.info("stop");
		if (this.rendererState.getAndSet(RENDERER_STATE_STOPPED) == RENDERER_STATE_STOPPED) {
			this.logger.debug("Renderer already stopped");
			return;
		}
		if (this.admobAd != null && !this.admobAd.isReady()) {
			this.admobAd.stopLoading();
		}

		rendererContext.dispatchEvent(this.constants.EVENT_AD_STOPPED());
	}

	@Override
	public void dispose() {
		this.logger.info("dispose");
		this.rendererState.set(RENDERER_STATE_STOPPED);
		if (this.admobAd != null) {
			this.admobAd.setAdListener(null);
		}
		if (this.admobAd instanceof AdView) {
			new Handler(this.rendererContext.getActivity().getMainLooper()).post(new Runnable() {
				@Override
				public void run() {
					AdView adView = (AdView)admobAd;
					adView.setVisibility(View.GONE);
					slot.getBase().removeView(adView);
				}
			});
		}
		if (this.rendererTimer != null) {
			this.rendererTimer.stop();
			this.rendererTimer = null;
		}
	}

	@Override
	public double getDuration() {
		return this.duration;
	}

	@Override
	public double getPlayheadTime() {
		return this.playHeadTime.get();
	}

	@Override
	public HashMap<String, String> getModuleInfo() {
		HashMap<String, String> ret = new HashMap<String, String>();
		ret.put("moduleType", "renderer");
		ret.put("requiredAPIVersion", FreeWheelVersion.FW_SDK_INTERFACE_VERSION);
		return ret;
	}

	private void failWithError(String errorCode, String errorMessage) {
		this.logger.error("errorMessage: " + errorMessage);
		Bundle info = new Bundle();
		info.putString(constants.INFO_KEY_ERROR_CODE(), errorCode);
		info.putString(constants.INFO_KEY_ERROR_INFO(), errorMessage);
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put(constants.INFO_KEY_EXTRA_INFO(), info);
		this.rendererContext.dispatchEvent(constants.EVENT_ERROR(), map);
	}

	private class showAdTask implements Runnable {
		@Override
		public void run() {
			if (admobAd instanceof AdView) {
				// Banner Ad
				final AdView adView = (AdView)admobAd;

				if (slot.getType() != constants.SLOT_TYPE_TEMPORAL()) {
					// display
					logger.debug("Show display banner");
					final RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(
							ViewGroup.LayoutParams.WRAP_CONTENT,
							ViewGroup.LayoutParams.WRAP_CONTENT);
					p.addRule(RelativeLayout.CENTER_IN_PARENT);
					slot.getBase().addView(adView, p);
				} else {
					final FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
							ViewGroup.LayoutParams.WRAP_CONTENT,
							ViewGroup.LayoutParams.WRAP_CONTENT
					);
					if (slot.getTimePositionClass() == constants.TIME_POSITION_CLASS_OVERLAY()) {
						// overlay
						logger.debug("Show overlay banner"
								+ ", primaryAnchor " + params.primaryAnchor
								+ ", marginWidth " + params.marginWidth
								+ ", marginHeight " + params.marginHeight);
						p.gravity = Gravity.NO_GRAVITY;

						if (params.primaryAnchor.contains("t")) {
							p.gravity |= Gravity.TOP;
							p.topMargin = params.marginHeight;
						}
						if (params.primaryAnchor.contains("l")) {
							p.gravity |= Gravity.LEFT;
							p.leftMargin = params.marginWidth;
						}
						if (params.primaryAnchor.contains("r")) {
							p.gravity |= Gravity.RIGHT;
							p.rightMargin = params.marginWidth;
						}
						if (params.primaryAnchor.contains("b")) {
							p.gravity |= Gravity.BOTTOM;
							p.bottomMargin = params.marginHeight;
						}
						if (params.primaryAnchor.contains("c")) {
							p.gravity |= Gravity.CENTER_HORIZONTAL;
						}
						if (params.primaryAnchor.contains("m")) {
							p.gravity |= Gravity.CENTER_VERTICAL;
						}
						if (params.primaryAnchor == "c" || params.primaryAnchor == "m"
								|| params.primaryAnchor == "cm" || params.primaryAnchor == "mc") {
							p.gravity = Gravity.CENTER;
						}

						// when an overlay ad is clicked and return to the player later,
						// the overlay ad may be covered by the main video
						slot.getBase().setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
							@Override
							public void onChildViewAdded(View parent, View child) {
								new Handler(rendererContext.getActivity().getMainLooper()).post(new Runnable() {
									@Override
									public void run() {
										adView.bringToFront();
									}
								});
							}

							@Override
							public void onChildViewRemoved(View parent, View child) {
							}
						});
					} else {
						// preroll, midroll, postroll
						logger.debug("Show preroll/midroll/postroll banner");
						p.gravity = Gravity.CENTER;
					}
					slot.getBase().addView(adView, p);
				}

				adView.setVisibility(View.VISIBLE);
				adView.bringToFront();
			} else {
				// Interstitial Ad
				InterstitialAd interstitialAd = (InterstitialAd)admobAd;
				interstitialAd.show();
			}
			rendererContext.dispatchEvent(constants.EVENT_AD_STARTED());
		}
	}

	@Override
	public void onReceiveAd(Ad ad) {
		this.logger.debug("onReceiveAd");

		if (this.rendererState.get() == RENDERER_STATE_STOPPED) {
			this.logger.warn("Renderer already stopped");
			return;
		}
		if (ad != this.admobAd) {
			this.logger.warn("Unknown received ad");
			return;
		}
		if (!this.firstAd) {
			this.logger.debug("Auto refreshed ad");
			return;
		}
		firstAd = false;
		if (this.duration > 0) {
			this.rendererTimer = new RendererTimer((int)this.duration, this);
			this.rendererTimer.start();
		}

		if (!this.rendererState.compareAndSet(RENDERER_STATE_PAUSED, RENDERER_STATE_PENDING)) {
			// not in paused state when receiving ad, show it
			new Handler(this.rendererContext.getActivity().getMainLooper()).post(new showAdTask());
		}
	}

	@Override
	public void onFailedToReceiveAd(Ad ad, AdRequest.ErrorCode errorCode) {
		this.logger.debug("onFailedToReceiveAd");
		if (this.rendererState.get() == RENDERER_STATE_STOPPED) {
			this.logger.warn("Renderer already stopped");
			return;
		}
		switch (errorCode) {
			case NO_FILL:
				failWithError(this.constants.ERROR_NO_AD_AVAILABLE(), errorCode.toString());
				break;
			case NETWORK_ERROR:
				failWithError(this.constants.ERROR_IO(), errorCode.toString());
				break;
			case INVALID_REQUEST:
			case INTERNAL_ERROR:
			default:
				failWithError(this.constants.ERROR_3P_COMPONENT(), errorCode.toString());
				break;
		}
	}

	@Override
	public void onPresentScreen(Ad ad) {
		this.logger.debug("onPresentScreen");
	}

	@Override
	public void onDismissScreen(Ad ad) {
		this.logger.debug("onDismissScreen");
		if (this.rendererState.get() == RENDERER_STATE_STOPPED) {
			this.logger.warn("Renderer already stopped");
			return;
		}
		if (ad == this.admobAd && this.admobAd instanceof InterstitialAd) {
			this.interstitialDismissed = true;
			// stop will be called by resume() later
		}
	}

	@Override
	public void onLeaveApplication(Ad ad) {
		this.logger.debug("onLeaveApplication");
	}

	@Override
	public void timeOut() {
		this.logger.debug("duration timeout");
		this.stop();
	}

	@Override
	public void playHeadTime(int headTime) {
		this.playHeadTime.set(headTime);
	}

	@Override
	public void resize() {}
}
