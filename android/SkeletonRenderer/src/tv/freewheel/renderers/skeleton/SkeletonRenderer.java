package tv.freewheel.renderers.skeleton;

import java.util.HashMap;

import android.util.Log;

import tv.freewheel.renderers.interfaces.IRenderer;
import tv.freewheel.renderers.interfaces.IRendererContext;

public class SkeletonRenderer implements IRenderer {
	private static final String CLASSTAG = "SkeletonRenderer";
	IRendererContext rendererContext;

	public SkeletonRenderer() {
		Log.i(CLASSTAG, FreeWheelVersion.RENDERER_VERSION);
	}
	
	@Override
	public void load(IRendererContext rendererContext) {
		this.rendererContext = rendererContext;
		this.rendererContext.dispatchEvent(rendererContext.getConstants().EVENT_AD_LOADED());
	}

	@Override
	public void start() {
		// invoke this when the ad actually showed
		this.rendererContext.dispatchEvent(rendererContext.getConstants().EVENT_AD_STARTED());
	}

	@Override
	public void pause() {
		// if possible, please implement it
		
	}

	@Override
	public void resume() {
		// if possible, please implement it
		
	}

	@Override
	public void stop() {
		// need to send this event to AM for disposing 
		rendererContext.dispatchEvent(rendererContext.getConstants().EVENT_AD_STOPPED());
	}

	@Override
	public void dispose() {
		// do the cleanup thing, this will be the last method AdManager will invoke
		
	}

	@Override
	public double getDuration() {
		// if possible return valid value otherwise -1
		return -1;
	}

	@Override
	public double getPlayheadTime() {
		// if possible return valid value otherwise -1
		return -1;
	}

	@Override
	public void resize() {
		// if possible, please implement it

	}

	@Override
	public HashMap<String, String> getModuleInfo() {
		HashMap<String, String> ret = new HashMap<String, String>();
		ret.put("moduleType", "renderer");
		ret.put("requiredAPIVersion", FreeWheelVersion.FW_SDK_INTERFACE_VERSION);
		return ret;
	}

}
