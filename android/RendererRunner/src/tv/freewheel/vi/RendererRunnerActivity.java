package tv.freewheel.vi;

import tv.freewheel.ad.AdManager;
import tv.freewheel.ad.interfaces.IAdManager;
import android.app.ListActivity;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class RendererRunnerActivity extends ListActivity {
	private static final String CLASSTAG = "RendererRunner";
	
	public static final String ADMANAGER_URL = null;
	public static IAdManager ADMANAGER;
	private boolean adManagerLoadFinish = false;
	
    private static final String[] items = {
    	"Test Renderer",
    	"Test NonFullScreenVideo"
    };
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items));
        this.setupAdManager();
    }
    
    public void onListItemClick(ListView parent, View v, int position, long id) {
    	String title = items[position];
    	Log.i(CLASSTAG, title + " clicked");
    	
    	Intent thisIntent = this.getIntent();
        if (thisIntent == null) {
        	Log.d(CLASSTAG, "This activity is invoked without an intent");
        }
        Uri theUri = thisIntent.getData();
        if (theUri != null) {
        	Log.d(CLASSTAG, "external config Uri:" + theUri.toString());
        }
        
    	if (title.equals("Test Renderer")) {
    		if (this.adManagerLoadFinish) {
    			Intent intent = new Intent(this, RendererTester.class);
    			if (theUri != null) {
    				intent.setData(theUri);
    			}
    			startActivity(intent);
    		} else {
    			Log.w(CLASSTAG, "Waiting for FreeWheel AdManagerLoad");
    		}
    	} else if (title.equals("Test NonFullScreenVideo")) {
    		Intent intent = new Intent(this, NonFullScreenActivity.class);
    		if (theUri != null) {
				intent.setData(theUri);
			}
    		startActivity(intent);
    	}
    }
    
    protected void setupAdManager() {
    	this.adManagerLoadFinish = true;
    	ADMANAGER = AdManager.getInstance(this.getApplicationContext());
    	
		final int networkId = 42015;
		
		ADMANAGER.setNetwork(networkId);
		Location location = new Location("myProvider");
		location.setLatitude(40.738878);
        location.setLongitude(-73.992491);
        ADMANAGER.setLocation(location);
	}
}