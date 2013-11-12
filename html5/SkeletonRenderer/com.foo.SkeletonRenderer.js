/*
 * SkeletonRenderer is an implementation of ad renderer based on FreeWheel ad technology.
 * To use this renderer, host the js and pass the url to RendererRunner.
 */

com = com || {};
com.foo = com.foo || {};
com.foo.SkeletonRenderer = function() {};
com.foo.SkeletonRenderer.prototype = {
	start: function(rendererController) {
		// retrieve ad info from controller
		var ad = rendererController.getAdInstance();
		var slot = ad.getSlot();
		var tpc = slot.getTimePositionClass();

		// the DOM element that the ad should be placed within
		var slotBase = slot.getBase();

		var rendition = ad.getActiveCreativeRendition();
		var duration = rendition.getDuration();
		var asset = rendition.getPrimaryCreativeRenditionAsset();

		// Check url or content value before use them
		var url = asset.getUrl();
		var content = asset.getContent();

		// do rendere staff, display ad on the slotBase, etc.
		// On any error, call rendererController.handleStateTransition(tv.freewheel.SDK.RENDERER_STATE_FAILED) and return;

		// if the rederer starts successfully, notify the controller to send impression
		rendererController.handleStateTransition(tv.freewheel.SDK.RENDERER_STATE_STARTED);
		tv.freewheel.SDK.log("SkeletonRenderer started.");

		// notify the controller that renderer work has been completed
		rendererController.handleStateTransition(tv.freewheel.SDK.RENDERER_STATE_COMPLETING);
		rendererController.handleStateTransition(tv.freewheel.SDK.RENDERER_STATE_COMPLETED);
	},

	/* Return renderer info, indicate this is a renderer or translator */
	info: function() {
		return {'moduleType': tv.freewheel.SDK.MODULE_TYPE_RENDERER};
	},

	/* Return playhead time in seconds. */
	getPlayheadTime: function() {
		// return -1 if playhead time is not available
		return -1;
	},

	/* Return duration in seconds. */
	getDuration: function() {
		// return -1 if duration is not available
		return -1;
	}
};
com.foo.SkeletonRenderer.prototype.constructor = com.foo.SkeletonRenderer;

