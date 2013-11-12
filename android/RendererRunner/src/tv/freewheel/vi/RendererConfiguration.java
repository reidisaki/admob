package tv.freewheel.vi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONObject;

import android.os.Environment;
import android.util.Log;

public class RendererConfiguration {
	private static final String CLASSTAG = "RendererConfiguration";
	protected String outputFilename = "response_rendererRunner.xml";
	private StringBuilder builder = new StringBuilder();
	String rendererClassName;
	JSONArray ads;
	ArrayList<BaseAd> adsArray = new ArrayList<BaseAd>();

	protected static final String defaultImpressionUrl = "http://demo.v.fwmrm.net/ad/l/1?s=dbg-3rd&n=96749&t=1362468439682087002&adid=<t_adid>&reid=<t_reid>&arid=0&auid=&cn=defaultImpression&et=i&_cc=&tpos=0&iw=&uxnw=&uxss=&uxct=&init=1&cr=";
	protected static final String genericUrl = "http://demo.v.fwmrm.net/ad/l/1?s=dbg-3rd&n=96749&t=1362468439682087002&adid=<t_adid>&reid=<t_reid>&arid=0&iw=&uxnw=&uxss=&uxct=";
	protected static final String defaultClickUrl = "http://nycadvip1-d.fwmrm.net/ad/l/1?s=debug&n=96749&t=1343028808684880004&adid=<t_adid>&reid=<t_reid>&arid=0&auid=&cn=defaultClick&et=c&_cc=&tpos=&cr=";

	public RendererConfiguration(FileInputStream is) throws IOException {
		this.parseData(new BufferedReader(new InputStreamReader(is)));
	}
	
	public void writeToFile() {
		try {
			File root = Environment.getExternalStorageDirectory();
			FileOutputStream fos = new FileOutputStream(new File(root, this.outputFilename));
			OutputStreamWriter out = new OutputStreamWriter(fos);
			XMLElement rootNode = new XMLElement("adResponse");
			rootNode.setAttribute("version", 1);
			rootNode.appendChild(this.buildAdsXML());
			rootNode.appendChild(this.buildSiteSectionXML());
			String s = XMLHandler.createXMLDocument(rootNode);
			Log.i(CLASSTAG, s);
			out.write(s);
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void parseData(BufferedReader br) throws IOException {
		String line;
		while ((line = br.readLine()) != null) {
			builder.append(line + "\n");
		}
		try {
			JSONObject jsonObject = new JSONObject(builder.toString());
			this.rendererClassName = jsonObject.getString("rendererClass");
			Log.i(CLASSTAG, "for renderer " + this.rendererClassName);
			this.ads = jsonObject.getJSONArray("testAds");
			int assignedId = 10000;
			for (int i = 0; i < this.ads.length(); ++i) {
				BaseAd ad = new BaseAd(this.ads.getJSONObject(i));
				ad.adId = assignedId;
				this.adsArray.add(ad);
				assignedId += 100;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Collections.sort(this.adsArray);
	}
	
	/*
	private String getRendererManifestXmlStr() {
		String ret = "<rendererManifest version='1'>" +
		             "&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;" +
		             "&lt;adRenderers version=&apos;1&apos;&gt;" +
		             "&lt;adRenderer name=&apos;" + this.rendererClassName + "&apos; url=&apos;class://" + this.rendererClassName + "&apos;&gt;" +
		             "&lt;/adRenderer&gt;" +
		             "&lt;/adRenderers&gt;" +
		             "</rendererManifest>";
		return ret;
	}
	*/
	
	private XMLElement buildSiteSectionXML() {
		XMLElement adSlots_t = new XMLElement("adSlots");
		XMLElement adSlots_nt = new XMLElement("adSlots");
		BaseAd prev = null;
		XMLElement temporalAdSlotNode = null;
		XMLElement selectedAdsNode = null;
		int count = 0;
		for (BaseAd ad : this.adsArray) {
			XMLElement callbacksNode = new XMLElement("eventCallbacks");

			// GENERIC eventCallback
			XMLElement callbackNode = new XMLElement("eventCallback");
			callbackNode.setAttribute("type", "GENERIC");
			callbackNode.setAttribute("url", genericUrl.replace("<t_adid>", ""+ad.adId)
					.replace("<t_reid>", ""+ad.getCreativeRenditionId()));
			callbacksNode.appendChild(callbackNode);

			// defaultImpression eventCallback
			callbackNode = new XMLElement("eventCallback");
			callbackNode.setAttribute("name", "defaultImpression");
			callbackNode.setAttribute("type", "IMPRESSION");
			callbackNode.setAttribute("url", defaultImpressionUrl.replace("<t_adid>", ""+ad.adId)
					.replace("<t_reid>", ""+ad.getCreativeRenditionId()));
			callbacksNode.appendChild(callbackNode);

			// defaultClick eventCallback
			callbackNode = new XMLElement("eventCallback");
			callbackNode.setAttribute("name", "defaultClick");
			callbackNode.setAttribute("type", "CLICK");
			callbackNode.setAttribute("showBrowser", "true");
			callbackNode.setAttribute("url", defaultClickUrl.replace("<t_adid>", ""+ad.adId)
					.replace("<t_reid>", ""+ad.getCreativeRenditionId())
					.concat(ad.defaultClickThrough));
			callbacksNode.appendChild(callbackNode);

			if (ad.slotType.equals("display")) {
				XMLElement adSlotNode = new XMLElement("adSlot");
				adSlotNode.setAttribute("customId", "display-" + String.valueOf(count++));
				XMLElement adReferenceNode = new XMLElement("adReference");
				adReferenceNode.setAttribute("adId", ad.adId);
				adReferenceNode.setAttribute("creativeId", ad.getCreativeId());
				adReferenceNode.setAttribute("creativeRenditionId", ad.getCreativeRenditionId());
				adReferenceNode.appendChild(callbacksNode);
				selectedAdsNode = new XMLElement("selectedAds");
				selectedAdsNode.appendChild(adReferenceNode);
				adSlotNode.appendChild(selectedAdsNode);
				adSlots_nt.appendChild(adSlotNode);
			} else {
				if (prev != null && !ad.slotType.equals(prev.slotType)) {
					adSlots_t.appendChild(temporalAdSlotNode);
				}
				temporalAdSlotNode = new XMLElement("temporalAdSlot");
				temporalAdSlotNode.setAttribute("adUnit", ad.slotType);
				temporalAdSlotNode.setAttribute("customId", "video-slot-" + ad.slotType + "-" + String.valueOf(ad.slotTimePos));
				temporalAdSlotNode.setAttribute("timePosition", String.valueOf(ad.slotTimePos));
				temporalAdSlotNode.setAttribute("timePositionClass", ad.slotType);
				selectedAdsNode = new XMLElement("selectedAds");
				temporalAdSlotNode.appendChild(selectedAdsNode);
				if (prev == null || ad.slotTimePos == prev.slotTimePos) {
					XMLElement adReferenceNode = new XMLElement("adReference");
					adReferenceNode.setAttribute("adId", ad.adId);
					adReferenceNode.setAttribute("creativeId", ad.getCreativeId());
					adReferenceNode.setAttribute("creativeRenditionId", ad.getCreativeRenditionId());
					adReferenceNode.appendChild(callbacksNode);
					selectedAdsNode.appendChild(adReferenceNode);
				}
				prev = ad;
			}
		}
		if (prev != null) {
			adSlots_t.appendChild(temporalAdSlotNode);
		}
		XMLElement videoAssetNode = new XMLElement("videoAsset");
		videoAssetNode.appendChild(adSlots_t);
		XMLElement videoPlayerNode = new XMLElement("videoPlayer");
		videoPlayerNode.appendChild(videoAssetNode);
		XMLElement siteSectionNode = new XMLElement("siteSection");
		siteSectionNode.appendChild(videoPlayerNode);
		siteSectionNode.appendChild(adSlots_nt);
		return siteSectionNode;
	}
	
	private XMLElement buildAdsXML() {
		XMLElement adsNode = new XMLElement("ads");
		
		for (BaseAd ad : this.adsArray) {
			adsNode.appendChild(ad.buildXMLElement());
		}
		
		return adsNode;
	}
}
