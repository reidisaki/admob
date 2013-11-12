var theAdManager;
var theContext;
var adDataRequested = false;
var prerollSlots = [];
var midrollSlots = [];
var overlaySlots = [];
var postrollSlots= [];
var currentPlayingSlotType = null;
var sourceUrlQueue = ["http://playerdemo.freewheel.tv/www/resource/content/22s.m4v",
"http://playerdemo.freewheel.tv/www/resource/content/22s.m4v",
"http://playerdemo.freewheel.tv/www/resource/content/22s.m4v"];

//Parameters for FreeWheel JSAM (JavaScript AdManager)
var theNetworkId = 96749;
var theServerURL = "http://demo.v.fwmrm.net/ad/g/1";
var theDisplayBaseId = "displayBase";
var theProfileId = "global-js";
var theVideoAssetId = "DemoVideoGroup.01";
var theSiteSectionId = "DemoSiteGroup.01";
var theVideoDuration = 500;

theAdManager = new tv.freewheel.SDK.AdManager();
theAdManager.setNetwork(theNetworkId);
theAdManager.setServer(theServerURL);

function init() {
	log("init");

	adDataRequested = false;
	prerollSlots = [];
	midrollSlots = [];
	overlaySlots = [];
	postrollSlots= [];
	currentPlayingSlotType = null;

	theContext = theAdManager.newContext();
	theContext.registerVideoDisplayBase(theDisplayBaseId);
	theContext.setProfile(theProfileId);
	theContext.setVideoAsset(theVideoAssetId,theVideoDuration,theNetworkId);
	theContext.setSiteSection(theSiteSectionId,theNetworkId);
	
	//Add key-values for ad targeting.
	theContext.addKeyValue("module","DemoPlayer");
	theContext.addKeyValue("feature","trackingURLs");
	theContext.addKeyValue("feature", "simpleAds");
	
	//Listen to AdManager Events
	theContext.addEventListener(tv.freewheel.SDK.EVENT_REQUEST_COMPLETE,onRequestComplete);
	theContext.addEventListener(tv.freewheel.SDK.EVENT_SLOT_ENDED,onSlotEnded);

	//To make sure video ad playback in poor network condition, set video ad timeout parameters.
	theContext.setParameter(tv.freewheel.SDK.PARAMETER_RENDERER_VIDEO_START_DETECT_TIMEOUT,10000,tv.freewheel.SDK.PARAMETER_LEVEL_GLOBAL);
	theContext.setParameter(tv.freewheel.SDK.PARAMETER_RENDERER_VIDEO_PROGRESS_DETECT_TIMEOUT,10000,tv.freewheel.SDK.PARAMETER_LEVEL_GLOBAL);

	//Add 1 preroll, 1 midroll, 2 overlay, 1 postroll slot
	theContext.addTemporalSlot("Preroll_1", tv.freewheel.SDK.ADUNIT_PREROLL, 0);
	theContext.addTemporalSlot("Midroll_1", tv.freewheel.SDK.ADUNIT_MIDROLL, 25);
	theContext.addTemporalSlot("Overlay_1", tv.freewheel.SDK.ADUNIT_OVERLAY, 3);
	theContext.addTemporalSlot("Overlay_2", tv.freewheel.SDK.ADUNIT_OVERLAY, 13);
	theContext.addTemporalSlot("Postroll_1", tv.freewheel.SDK.ADUNIT_POSTROLL, 60);
	loadAdData();
}
function onRequestComplete(event) {
	log("onRequestComplete");
	prerollSlots = [];
	midrollSlots = [];
	overlaySlots = [];
	postrollSlots = [];
	if (event.success){
		var fwTemporalSlots = theContext.getTemporalSlots();
		for (var i=0; i<fwTemporalSlots.length; i++)
		{
			var slot = fwTemporalSlots[i];
			switch (slot.getTimePositionClass())
			{
				case tv.freewheel.SDK.TIME_POSITION_CLASS_PREROLL:
					prerollSlots.push(slot);
					break;
				case tv.freewheel.SDK.TIME_POSITION_CLASS_MIDROLL:
					midrollSlots.push(slot);
					break;
				case tv.freewheel.SDK.TIME_POSITION_CLASS_OVERLAY:
					overlaySlots.push(slot);
					break;
				case tv.freewheel.SDK.TIME_POSITION_CLASS_POSTROLL:
					postrollSlots.push(slot);
					break;
			}
		}
		$("#start").attr('disabled', false);
	}
}
function onSlotEnded(event){
	var slotTimePositionClass = event.slot.getTimePositionClass();
	log("onSlotEnded slotTimePositionClass: " + slotTimePositionClass );
	switch (slotTimePositionClass)
	{
		case tv.freewheel.SDK.TIME_POSITION_CLASS_PREROLL:
			if (prerollSlots.length){
				prerollSlots.shift();
				if (prerollSlots.length)
					prerollSlots[0].play();
				else{
					$('#videoPlayer').bind('ended', onContentVideoEnded);
					$('#videoPlayer').bind('timeupdate', onContentVideoTimeUpdated);
					$('#videoPlayer').attr('controls', true);
					$('#videoPlayer')[0].play();
					theContext.setVideoState(tv.freewheel.SDK.VIDEO_STATE_PLAYING);
				}
			}
			break;
		case tv.freewheel.SDK.TIME_POSITION_CLASS_MIDROLL:
			currentPlayingSlotType = null;
			midrollSlots.shift();
			actualPlayback();
			break;
		case tv.freewheel.SDK.TIME_POSITION_CLASS_OVERLAY:
			currentPlayingSlotType = null;
			break;
		case tv.freewheel.SDK.TIME_POSITION_CLASS_POSTROLL:
			if (postrollSlots.length){
				postrollSlots.shift();
				if (postrollSlots.length)
					postrollSlots[0].play();
				else{
					theContext.setVideoState(tv.freewheel.SDK.VIDEO_STATE_COMPLETED);
					onRequestComplete({success:true});
				}
			}
			break;
	}
}
function onContentVideoTimeUpdated() {
	if (! overlaySlots.length){
		$('#videoPlayer').unbind('timeupdate',onContentVideoTimeUpdated);
		return ;
	}
	if(currentPlayingSlotType === tv.freewheel.SDK.TIME_POSITION_CLASS_MIDROLL){
		log("Received content video timeUpdate event dispatched when midroll playing, ignore");
		return;
	}

	for(var i=0; i<overlaySlots.length; i++){
		var slot = overlaySlots[i];
		var slotTimePosition = slot.getTimePosition();
		var videoCurrentTime = $('#videoPlayer')[0].currentTime;

		if ( videoCurrentTime - slotTimePosition >= 0 && videoCurrentTime - slotTimePosition <= 1 && !currentPlayingSlotType){
			overlaySlots.splice(i,1);
			log("onContentVideoTimeUpdated timePositionClass: "+ slot.getTimePositionClass());
			slot.play();
			return;
		}
	}
}
function onContentVideoEnded() {
	if(currentPlayingSlotType === tv.freewheel.SDK.TIME_POSITION_CLASS_MIDROLL){
		log("Received content video ended event dispatched from midroll, ignore");
		return;
	}
	log("onContentVideoEnded");
	sourceUrlQueue.shift();
	theContext.setVideoState(tv.freewheel.SDK.VIDEO_STATE_STOPPED);
	if (midrollSlots.length) {
		currentPlayingSlotType = tv.freewheel.SDK.TIME_POSITION_CLASS_MIDROLL;
		midrollSlots[0].play();
	} else if (sourceUrlQueue.length) {
		actualPlayback();
	} else if(postrollSlots && postrollSlots.length){
		$('#videoPlayer').unbind('timeupdate', onContentVideoTimeUpdated);
		$('#videoPlayer').unbind('ended', onContentVideoEnded);
		$('#videoPlayer').attr('controls', false);
		postrollSlots[0].play();
	} else {
		theContext.setVideoState(tv.freewheel.SDK.VIDEO_STATE_COMPLETED);
	}
}
function loadAdData() {
	log("loadAd");
	theContext.submitRequest();
	adDataRequested = true;
	$("#start").attr('disabled', true);
}
function startPlayback() {
	log("start");
	if (!adDataRequested) {
		loadAdData();
	} else {
		$("#start").attr('disabled', true);
		if (!(document.createElement("video").canPlayType("video/mp4"))){
			sourceUrlQueue = ["http://playerdemo.freewheel.tv/www/resource/content/22s.webm",
			"http://playerdemo.freewheel.tv/www/resource/content/22s.webm",
			"http://playerdemo.freewheel.tv/www/resource/content/22s.webm"];
		}else{
			sourceUrlQueue = ["http://playerdemo.freewheel.tv/www/resource/content/22s.m4v",
			"http://playerdemo.freewheel.tv/www/resource/content/22s.m4v",
			"http://playerdemo.freewheel.tv/www/resource/content/22s.m4v"];
		}
		
		actualPlayback();
	}
}
//content src must be set before preroll slot playback
function actualPlayback() {
	if (sourceUrlQueue.length) {
		$("#videoPlayer")[0].src = sourceUrlQueue[0];
	}

	log("will play "+$("#videoPlayer")[0].src);
	if (prerollSlots.length){
		prerollSlots[0].play();
	}else{
		$('#videoPlayer')[0].play();
		$('#videoPlayer').attr('controls', true);
	}
}
function log(msg){
	if (window.console){
		window.console.log('[Demo Player] '+msg);
	}
}
$(function(){
	init();
});

