package tv.freewheel.renderers.admob;

import org.json.JSONObject;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;

import tv.freewheel.ad.interfaces.IConstants;
import tv.freewheel.utils.Logger;
import tv.freewheel.utils.renderer.ParamParser;
import tv.freewheel.renderers.interfaces.IRendererContext;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class Parameters {
	public static final String NAMESPACE = "renderer.admob";
	
	public static final String PARAM_PUBLISHER_ID = "publisherId";
	public static final String PARAM_BACKGROUND_COLOR = "backgroundColor";
	public static final String PARAM_HEADER_TEXT_COLOR = "headerTextColor";
	public static final String PARAM_DESCRIPTION_TEXT_COLOR = "descriptionTextColor";
	public static final String PARAM_SHORT_TIMEOUT = "shortTimeout";
 	public static final String PARAM_TEST_DEVICE_IDS = "testDeviceIds";
	public static final String PARAM_PRIMARY_ANCHOR = "primaryAnchor";
	public static final String PARAM_MARGIN_WIDTH = "marginWidth";
	public static final String PARAM_MARGIN_HEIGHT = "marginHeight";

	public static final String PARAM_BANNER_SIZE = "bannerSize";
	public static final String PARAM_BANNER = "BANNER";
	public static final String PARAM_IAB_MRECT = "IAB_MRECT";
	public static final String PARAM_IAB_BANNER = "IAB_BANNER";
	public static final String PARAM_IAB_LEADERBOARD = "IAB_LEADERBOARD";
	
	public static final String PARAM_GENDER_MALE = "male";
	public static final String PARAM_GENDER_FEMALE = "female";

	public String publisherId = null;
	public GregorianCalendar dateOfBirth = null;
	
	public int gender = AdRequest.GENDER_UNKNOWN;
	public String keywords = null;
	public String searchString = null;
	public int backgroundColor = 0;
	public int headerTextColor = 0;
	public int descriptionTextColor = 0;
	public boolean shortTimeout = false;
	public List<String> testDeviceIds = null;
	public String primaryAnchor = "bc";
	public int marginWidth = 0;
	public int marginHeight = 0;

	public AdSize bannerSize = null;
	public static final AdSize[] adSizes = {AdSize.LEADERBOARD, AdSize.BANNER, AdSize.MEDIUM_RECTANGLE, AdSize.BANNER};

	public String errorCode = null;
	public String errorString = null;

	private IConstants constants;
	
	public static Parameters parseParameters(IRendererContext context) {
		Parameters p = new Parameters();
		ParamParser pp = new ParamParser(context, NAMESPACE);
		p.constants = context.getConstants();
		
		p.publisherId = pp.parseString(PARAM_PUBLISHER_ID, p.publisherId);
		p.keywords = pp.parseString(p.constants.PARAMETER_KEYWORDS(), p.keywords);
		p.searchString = pp.parseString(p.constants.PARAMETER_SEARCH_STRING(), p.searchString);
		p.dateOfBirth = pp.parseDate(p.constants.PARAMETER_DATE_OF_BIRTH(), p.dateOfBirth);
		
		String genderStr = pp.parseEnum(p.constants.PARAMETER_GENDER(), null, new String[]{PARAM_GENDER_MALE, PARAM_GENDER_FEMALE});
		if(genderStr != null) p.gender = (genderStr == PARAM_GENDER_MALE ? AdRequest.GENDER_MALE: AdRequest.GENDER_FEMALE);
		
		p.headerTextColor = pp.parseColor(PARAM_HEADER_TEXT_COLOR, p.headerTextColor);
		p.descriptionTextColor = pp.parseColor(PARAM_DESCRIPTION_TEXT_COLOR, p.descriptionTextColor);
		p.backgroundColor = pp.parseColor(PARAM_BACKGROUND_COLOR, p.backgroundColor);

		p.shortTimeout = pp.parseBoolean(PARAM_SHORT_TIMEOUT, p.shortTimeout);
		p.testDeviceIds = pp.parseList(PARAM_TEST_DEVICE_IDS);

		p.primaryAnchor = pp.parseString(PARAM_PRIMARY_ANCHOR, p.primaryAnchor);
		p.marginWidth = pp.parseInt(PARAM_MARGIN_WIDTH, p.marginWidth);
		p.marginHeight = pp.parseInt(PARAM_MARGIN_HEIGHT, p.marginHeight);

		String bannerSizeStr = pp.parseEnum(PARAM_BANNER_SIZE, null,
				new String[]{PARAM_BANNER, PARAM_IAB_MRECT, PARAM_IAB_BANNER, PARAM_IAB_LEADERBOARD});
		if (bannerSizeStr != null) {
			if (bannerSizeStr == PARAM_IAB_LEADERBOARD) {
				p.bannerSize = AdSize.LEADERBOARD;
			} else if (bannerSizeStr == PARAM_IAB_BANNER) {
				p.bannerSize = AdSize.BANNER;
			} else if (bannerSizeStr == PARAM_IAB_MRECT) {
				p.bannerSize = AdSize.MEDIUM_RECTANGLE;
			} else {
				p.bannerSize = AdSize.BANNER;
			}
		}

		Logger.getLogger("Parameters").debug(p.toString());
		return p;
	}
	
	public boolean validate() {
		errorString = null;
		if(publisherId == null) {
			errorString = "publisherId is not set";
			errorCode = constants.ERROR_MISSING_PARAMETER();
		} else if (publisherId.length() == 0) {
			errorString = "publisherId is empty";
			errorCode = constants.ERROR_INVALID_VALUE();
		}
		return (errorString == null);
	}
	
	public String toString() {
		JSONObject json = new JSONObject();
		for (int i = 0; i < Parameters.class.getDeclaredFields().length; i++) {
			Field field = Parameters.class.getDeclaredFields()[i];
			if(!Modifier.isStatic(field.getModifiers())) {
				try {
					Object value = field.get(this);
					if(value instanceof Calendar) value = ((Calendar)value).getTime().toString();
					if(value instanceof Integer && field.getName().indexOf("Color") >= 0) value = Integer.toHexString(((Integer)value));
					json.put(field.getName(), value);
				} catch (Exception e) {
				}
			}
		}
		return json.toString();
	}
}
