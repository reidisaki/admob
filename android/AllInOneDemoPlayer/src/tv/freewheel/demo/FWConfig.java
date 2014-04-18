package tv.freewheel.demo;

public class FWConfig {
	
	// ***NOTE***
	// Each client gets their own network ID, AdManager URLs, and ad server URLs
	// Please see your FreeWheel account manager or sales engineer for the
	// appropriate values to use in your production player.
	public static String adserverUrl = "http://demo.v.fwmrm.net/";
	public static int networkId = 42015;
	
	// Request parameters --- these may change from request to request
	// depending on how your content is arranged in the MRM system
	
	public static String profile = "android_demo_since_5.13";
	public static String siteSectionId = "android_allinone_demo_site_section";
	public static String videoAssetId = "android_allinone_demo_video";
	
	public static int displayWidth = 320;
	public static int displayHeight = 50;
	
	public static String splashCompatibleDimensions = "";
	
}
