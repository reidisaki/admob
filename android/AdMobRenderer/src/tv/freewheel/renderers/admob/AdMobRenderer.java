package tv.freewheel.renderers.admob;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.android.gms.ads.*;
import com.google.android.gms.ads.search.SearchAdRequest;
import com.google.android.gms.ads.search.SearchAdView;
import com.google.android.gms.ads.search.SearchAdRequest.Builder;
import com.google.android.gms.ads.AdRequest;

import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import tv.freewheel.ad.FreeWheelVersion;
import tv.freewheel.ad.interfaces.IConstants;
import tv.freewheel.ad.interfaces.ISlot;
import tv.freewheel.utils.Logger;
import tv.freewheel.utils.renderer.RendererTimer;
import tv.freewheel.ad.interfaces.IAdInstance;
import tv.freewheel.ad.interfaces.ICreativeRendition;
import tv.freewheel.renderers.interfaces.IRenderer;
import tv.freewheel.renderers.interfaces.IRendererContext;

public class AdMobRenderer extends AdListener implements IRenderer, RendererTimer.IRendererTimerService {
	private IRendererContext rendererContext = null;
	private IConstants constants = null;
	private Parameters params = null;
	private IAdInstance adInstance = null;
	private ISlot slot = null;

	// AdMob
	private com.google.android.gms.ads.AdRequest.Builder admobAdRequest  = null;
	private Builder admobSearchAdRequest = null;
	private InterstitialAd interstitial = null;
	//	private AdRequest admobAdRequest = null;
	//	private SearchAdRequest admobSearchAdRequest = null;
	private AdView admobAd = null;
	private SearchAdView admobSearchAdView = null;
	// auto-refresh
	private boolean firstAd = true,interstitialDismissed = false, isInterstitial = false, isBanner = false;

	// duration
	private double duration = -1;

	// renderer timer
	private AtomicInteger playHeadTime = new AtomicInteger(-1);
	private RendererTimer rendererTimer = null;


	private static int RENDERER_STATE_PLAYING = 0;
	private static int RENDERER_STATE_PAUSED = 1;
	private static int RENDERER_STATE_PENDING = 2; // need to show ad on resume
	private static int RENDERER_STATE_STOPPED = 3;
	private AtomicInteger rendererState = new AtomicInteger(RENDERER_STATE_PLAYING);

	private Logger logger;
	///Find out if this is an interstitial ad or not, find out if this is a search ad or banner ad 

	public AdMobRenderer() {
		this.logger = Logger.getLogger(this);
		this.logger.info(FreeWheelVersion.FW_SDK_INTERFACE_VERSION);
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

			isInterstitial =true;
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

				interstitial = new InterstitialAd(this.rendererContext.getActivity());
				interstitial.setAdUnitId(this.params.publisherId);
				//				interstitial.loadAd(adRequest);
				//				this.admobAd = new InterstitialAd(this.rendererContext.getActivity(),
				//						this.params.publisherId, this.params.shortTimeout);
			}
		} else {
			isInterstitial = false;
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
				isBanner = false;

				// search Ad
				this.logger.debug("Search Ad for string " + this.params.searchString);
				Builder searchAdRequest = new SearchAdRequest.Builder().setQuery(this.params.searchString);

				if (this.params.backgroundColor != 0) {
					searchAdRequest.setBackgroundColor(this.params.backgroundColor);
				}
				if (this.params.headerTextColor != 0) {
					searchAdRequest.setHeaderTextColor(this.params.headerTextColor);
				}
				if (this.params.descriptionTextColor != 0) {
					searchAdRequest.setDescriptionTextColor(this.params.descriptionTextColor);
				}
				this.admobSearchAdRequest = searchAdRequest;
			} else {
				this.logger.debug("Banner Ad");
				isBanner = true;
				this.admobAdRequest = new AdRequest.Builder();
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
				DisplayMetrics dm = this.rendererContext.getActivity().getResources().getDisplayMetrics();
				bannerWidth *= dm.density;
				bannerHeight *= dm.density;
				this.logger.debug("Slot size in pixel: " + bannerWidth + "x" + bannerHeight);

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

			//create searchAdView
			this.admobSearchAdView = new SearchAdView(this.rendererContext.getActivity());
			this.admobSearchAdView.setAdUnitId(this.params.publisherId);
			this.admobSearchAdView.setAdSize(adSize);

			//create non searchRequest adView
			this.admobAd = new AdView(this.rendererContext.getActivity());
			this.admobAd.setAdSize(adSize);
			this.admobAd.setAdUnitId(this.params.publisherId);

		}
		setAdParameters(this.params);

		this.admobSearchAdView.setAdListener(this);
		this.admobAd.setAdListener(this);
		this.rendererContext.setRendererCapability(this.constants.EVENT_AD_CLICK(),
				this.constants.CAPABILITY_STATUS_OFF());
		this.rendererContext.dispatchEvent(this.constants.EVENT_AD_LOADED());
	}

	private void setAdParameters(Parameters params) {

		//set banner stuff
		if(isBanner) {
			if (this.params.dateOfBirth != null) {
				this.admobAdRequest.setBirthday(this.params.dateOfBirth.getTime());
			}
			if (this.params.gender != AdRequest.GENDER_UNKNOWN) {
				this.admobAdRequest.setGender(this.params.gender);
			}
			if (this.params.keywords != null) {
				this.admobAdRequest.addKeyword(this.params.keywords);
			}
			if (this.params.testDeviceIds != null && !this.params.testDeviceIds.isEmpty()) {
				Set<String> testDeviceIds = new HashSet<String>(this.params.testDeviceIds);
				this.admobAdRequest.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
				for(String testDeviceId : testDeviceIds) {
					this.admobAdRequest.addTestDevice(testDeviceId);	
				}
			}
			if (this.rendererContext.getLocation() != null) {
				this.admobAdRequest.setLocation(this.rendererContext.getLocation());
			}
		} else {
			//set searchRequestAd stuff
			if (this.params.testDeviceIds != null && !this.params.testDeviceIds.isEmpty()) {
				Set<String> testDeviceIds = new HashSet<String>(this.params.testDeviceIds);
				this.admobSearchAdRequest.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
				for(String testDeviceId : testDeviceIds) {
					this.admobAdRequest.addTestDevice(testDeviceId);	
				}
			}
			if (this.rendererContext.getLocation() != null) {
				this.admobSearchAdRequest.setLocation(this.rendererContext.getLocation());
			}
		}

	}

	@Override
	public void start() {
		this.logger.info("start");
		//this can be either an interstitial ad, or regular ad, and searchAd or BannerAd

		if(isInterstitial) {
			this.interstitial.loadAd(admobAdRequest.build());
		} else {
			if(isBanner) {
				admobAd.loadAd(admobAdRequest.build());
			} else {
				this.admobSearchAdView.loadAd(admobSearchAdRequest.build());
			}
		}
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
		if (this.interstitial instanceof InterstitialAd && interstitialDismissed) {
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

		//might not need this anymore.
		//		if (this.admobAd != null && !this.admobAd.isReady()) {
		//			this.admobAd.stopLoading();
		//		}

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
						DisplayMetrics dm = rendererContext.getActivity().getResources().getDisplayMetrics();
						logger.debug("Show overlay banner"
								+ ", primaryAnchor " + params.primaryAnchor
								+ ", marginWidth " + params.marginWidth * dm.density
								+ "px, marginHeight " + params.marginHeight * dm.density + "px");
						p.gravity = Gravity.NO_GRAVITY;

						if (params.primaryAnchor.contains("t")) {
							p.gravity |= Gravity.TOP;
							p.topMargin = (int)(params.marginHeight * dm.density);
						}
						if (params.primaryAnchor.contains("l")) {
							p.gravity |= Gravity.LEFT;
							p.leftMargin = (int)(params.marginWidth * dm.density);
						}
						if (params.primaryAnchor.contains("r")) {
							p.gravity |= Gravity.RIGHT;
							p.rightMargin = (int)(params.marginWidth * dm.density);
						}
						if (params.primaryAnchor.contains("b")) {
							p.gravity |= Gravity.BOTTOM;
							p.bottomMargin = (int)(params.marginHeight * dm.density);
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
				//				InterstitialAd interstitialAd = (InterstitialAd)admobAd;
				//				interstitialAd.show();
				interstitial.show();

			}
			rendererContext.dispatchEvent(constants.EVENT_AD_STARTED());
		}
	}
	/*
	 * 
	 * void	 onAdClosed()
Called when the user is about to return to the application after clicking on an ad.
void	 onAdFailedToLoad(int errorCode)
Called when an ad request failed.
void	 onAdLeftApplication()
Called when an ad leaves the application (e.g., to go to the browser).
void	 onAdLoaded()
Called when an ad is received.
void	 onAdOpened()
Called when an ad opens an overlay that covers the screen.

	 */

	@Override
	public void onAdLoaded() {
		this.logger.debug("onReceiveAd");

		if (this.rendererState.get() == RENDERER_STATE_STOPPED) {
			this.logger.warn("Renderer already stopped");
			return;
		}
		//		if (ad != this.admobAd) {
		//			this.logger.warn("Unknown received ad");
		//			return;
		//		}
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
	public void onAdFailedToLoad(int errorCode) {
		this.logger.debug("onFailedToReceiveAd");
		if (this.rendererState.get() == RENDERER_STATE_STOPPED) {
			this.logger.warn("Renderer already stopped");
			return;
		}
		switch (errorCode) {
		case AdRequest.ERROR_CODE_NO_FILL:
			failWithError(this.constants.ERROR_NO_AD_AVAILABLE(), String.valueOf(errorCode));
			break;
		case AdRequest.ERROR_CODE_NETWORK_ERROR:
			failWithError(this.constants.ERROR_IO(), String.valueOf(errorCode));
			break;
		case AdRequest.ERROR_CODE_INVALID_REQUEST:
		case AdRequest.ERROR_CODE_INTERNAL_ERROR:
		default:
			failWithError(this.constants.ERROR_3P_COMPONENT(), String.valueOf(errorCode));
			break;
		}
	}


	@Override
	public void onAdOpened() {
		this.logger.debug("onPresentScreen");
	}

	@Override
	public void onAdClosed() {
		this.logger.debug("onDismissScreen");
		if (this.rendererState.get() == RENDERER_STATE_STOPPED) {
			this.logger.warn("Renderer already stopped");
			return;
		}

		if(!isBanner) {
			this.interstitialDismissed = true;
			// stop will be called by resume() later
		}

	}

	@Override
	public void onAdLeftApplication() {
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
