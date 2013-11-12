package tv.freewheel.vi;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class BaseAd implements Comparable<BaseAd> {
	private static final String CLASSTAG = "BaseAd";
	String slotType;
	String baseUnit = "fixed-size-interactive";
	String contentType = "text/html_doc_lit_mobile";
	String creativeApi;
	String url = "";
	String content = "";
	double duration = -1;
	JSONArray otherAssets;
	JSONArray param;
	double slotTimePos = -1;
	int slotWidth = 0;
	int slotHeight = 0;
	String defaultClickThrough = "http://www.freewheel.tv";
	int adId = 0;
	
	public BaseAd(JSONObject data) throws JSONException {
		this.slotType = data.getString("slotType").toLowerCase();
		if (data.has("baseUnit")) {
			this.baseUnit = data.getString("baseUnit");
		}
		if (data.has("url")) {
			this.url = data.getString("url");
		}
		if (data.has("content")) {
			this.content = data.getString("content");
		}
		if (data.has("duration")) {
			this.duration = data.getDouble("duration");
		}
		if (data.has("otherAssets")) {
			this.otherAssets = data.getJSONArray("otherAssets");
		}
		if (data.has("param")) {
			this.param = data.getJSONArray("param");
			Log.e(CLASSTAG, "param length:" + this.param.length());
		}
		if (data.has("slotTimePos")) {
			this.slotTimePos = data.getDouble("slotTimePos");
		}
		if (data.has("slotWidth")) {
			this.slotWidth = data.getInt("slotWidth");
		}
		if (data.has("slotHeight")) {
			this.slotHeight = data.getInt("slotHeight");
		}
		if (data.has("eventCallback")) {
			this.defaultClickThrough = data.getString("eventCallback");
		}
		if (data.has("defaultClickThrough")) {
			this.defaultClickThrough = data.getString("defaultClickThrough");
		}
		if (data.has("contentType")) {
			this.contentType = data.getString("contentType");
		}
		if (data.has("creativeApi")) {
			this.creativeApi = data.getString("creativeApi");
		}
	}
	
	public XMLElement buildXMLElement() {
		XMLElement node = new XMLElement("ad");
		node.setAttribute("adId", this.adId);
		XMLElement renditionElement = new XMLElement("creativeRendition");
		renditionElement.setAttribute("creativeRenditionId", this.getCreativeRenditionId());
		if (this.creativeApi != null) {
			renditionElement.setAttribute("creativeApi", this.creativeApi);
		}
		if (this.slotHeight > 0 && this.slotWidth > 0) {
			renditionElement.setAttribute("width", this.slotWidth);
			renditionElement.setAttribute("height", this.slotHeight);
		}
		XMLElement assetElement = new XMLElement("asset");
		assetElement.setAttribute("id", this.adId + 3);
		assetElement.setAttribute("contentType", this.contentType);
		assetElement.setAttribute("mimeType", "text/html");
		if (this.content.length() == 0 && this.url.length() > 0) {
			assetElement.setAttribute("url", this.url);
		} else if (this.content.length() > 0) {
			XMLElement contentNode = new XMLElement("content");
			contentNode.setCDATAContent(this.content);
			assetElement.appendChild(contentNode);
		}
		renditionElement.appendChild(assetElement);
		if (this.otherAssets != null) {
			for (int i = 0; i < this.otherAssets.length(); ++i) {
				XMLElement otherAssetElement = new XMLElement("asset");
				otherAssetElement.setAttribute("id", this.adId + 4);
				otherAssetElement.setAttribute("contentType", this.contentType);
				otherAssetElement.setAttribute("mimeType", "text/html");
				try {
					JSONObject temp = this.otherAssets.getJSONObject(i);
					Iterator<String> iter = temp.keys();
					if (iter.hasNext()) {
						String key = iter.next();
						String value = temp.getString(key);
						otherAssetElement.setAttribute(key, value);
					}
				} catch (JSONException e) {
					Log.w(CLASSTAG, e.getMessage());
				}
				renditionElement.appendChild(otherAssetElement);
			}
		}
		XMLElement renditionsElement = new XMLElement("creativeRenditions");
		renditionsElement.appendChild(renditionElement);
		XMLElement creativeElement = new XMLElement("creative");
		creativeElement.setAttribute("creativeId", this.getCreativeId());
		creativeElement.setAttribute("baseUnit", this.baseUnit);
		if (this.duration > 0) {
			creativeElement.setAttribute("duration", this.duration);
		}
		if (this.param != null) {
			XMLElement parametersNode = new XMLElement("parameters");
			for (int j = 0; j < this.param.length(); ++j) {
				XMLElement paramElement = new XMLElement("parameter");
				try {
					JSONObject temp = this.param.getJSONObject(j);
					Iterator<String> iter = temp.keys();
					if (iter.hasNext()) {
						String key = iter.next();
						String value = temp.getString(key);
						paramElement.setAttribute("name", key);
						//paramElement.setCDATAContent(value);
						paramElement.setText(value);
					}
					parametersNode.appendChild(paramElement);
				} catch (JSONException e) {
					Log.w(CLASSTAG, e.getMessage());
				}
			}
			creativeElement.appendChild(parametersNode);
		}
		creativeElement.appendChild(renditionsElement);
		XMLElement creativesElement = new XMLElement("creatives");
		creativesElement.appendChild(creativeElement);
		node.appendChild(creativesElement);
		
		return node;
	}
	
	@Override
	public int compareTo(BaseAd another) {
		if (this.slotType.equals(another.slotType)) {
			return (int) (this.slotTimePos - another.slotTimePos);
		} else {
			return this.slotType.compareTo(another.slotType);
		}
	}
	
	public int getCreativeId() {
		return this.adId + 1;
	}
	
	public int getCreativeRenditionId() {
		return this.adId + 2;
	}
}
