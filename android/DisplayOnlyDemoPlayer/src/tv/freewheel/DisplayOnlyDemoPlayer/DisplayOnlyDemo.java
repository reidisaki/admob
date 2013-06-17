package tv.freewheel.DisplayOnlyDemoPlayer;

import tv.freewheel.ad.AdManager;
import tv.freewheel.ad.interfaces.IAdContext;
import tv.freewheel.ad.interfaces.IAdManager;
import tv.freewheel.ad.interfaces.IConstants;
import tv.freewheel.ad.interfaces.IEvent;
import tv.freewheel.ad.interfaces.ISlot;
import tv.freewheel.ad.interfaces.IEventListener;
import android.os.Bundle;
import android.app.Activity;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.RelativeLayout.LayoutParams;
import tv.freewheel.utils.Logger;

public class DisplayOnlyDemo extends Activity {
	private RelativeLayout rootLayout;
	private Logger logger = Logger.getLogger(this);
	private IAdManager adManager;
	private IAdContext adContext;
	private IConstants adConstants;
	private String serverUrl = "http://demo.v.fwmrm.net";
	private int networkId = 42015;
	private String playerProfile = "Android_Tutorial_Profile";
	private String siteSectionId = "Green";
	private String customId = "SSNT_1";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        
        rootLayout = new RelativeLayout(this);
        TextView textview = new TextView(this);
        textview.setText("FreeWheel's Demo Player with Display ads only");
        
        rootLayout.addView(textview);
        
        RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        p.addRule(RelativeLayout.CENTER_HORIZONTAL);
        
        setContentView(rootLayout);
        
        this.start();
    }

    public void start () {
    	Logger.setLogLevel(2);
		adManager = AdManager.getInstance(this.getApplicationContext());
		if (adManager == null) {
			logger.error("Failed to initialize AdManager");
			return;
		}
		
		adManager.setServer(serverUrl);
		adManager.setNetwork(networkId);
		
		adContext = adManager.newContext();
		adConstants = adContext.getConstants();
		adContext.setProfile(playerProfile, null, null, null);
		adContext.setSiteSection(siteSectionId, 0, 0, 0, 0);
		adContext.addSiteSectionNonTemporalSlot(customId, null, 300, 60, null, false, "text/html_doc_lit_mobile", "text/html_doc_lit_mobile");
		adContext.setActivity(this);
		
		IEventListener requestCompleteListener = new IEventListener(){	    
			public void run(final IEvent event) {
				onRequestComplete(event);
			}
		};

		adContext.addEventListener(adConstants.EVENT_REQUEST_COMPLETE(), requestCompleteListener);
		
		adContext.submitRequest(10);//timeout if ad server doesn't response in 5 seconds
	}
	
	public void onRequestComplete(IEvent event) {
		logger.info("onRequestComplete: request completed. site section non-temporal slots: " + adContext.getSiteSectionNonTemporalSlots().size());
		final ISlot nslot = adContext.getSlotByCustomId(customId);
		if (nslot != null) {
			this.rootLayout.post( new Runnable() {
				public void run() {
					RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
					p.addRule(RelativeLayout.CENTER_HORIZONTAL);
					rootLayout.addView(nslot.getBase(), p);
		    		nslot.play();
				}
			});
		}
	}
    
}
