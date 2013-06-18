package
{
	import flash.display.Sprite;
	import flash.system.Security;
	
	import org.osmf.elements.VideoElement;
	import org.osmf.events.MediaPlayerStateChangeEvent;
	import org.osmf.events.TimelineMetadataEvent;
	import org.osmf.media.MediaPlayerSprite;
	import org.osmf.media.MediaPlayerState;
	import org.osmf.media.URLResource;
	import org.osmf.metadata.CuePoint;
	import org.osmf.metadata.CuePointType;
	import org.osmf.metadata.TimelineMetadata;
	
	import tv.freewheel.ad.behavior.IAdManager;
	import tv.freewheel.ad.behavior.IConstants;
	import tv.freewheel.ad.behavior.IEvent;
	import tv.freewheel.ad.behavior.ISlot;
	import tv.freewheel.ad.loader.AdManagerLoader;
	
	[SWF(backgroundColor="0x333333", width=640, height=480)]
	public class DemoPlayer extends Sprite
	{
		// Consult your FreeWheel sales engineer about the following values
		private static const FW_ADMANAGER_URL:String = "http://adm.fwmrm.net/p/vitest-as3/AdManager.swf?logLevel=VERBOSE";
		private static const FW_SERVER_URL:String = "http://demo.v.fwmrm.net";
		private static const FW_NETWORK_ID:int = 90750;
		private static const FW_PROFILE:String = "3pqa_profile";
		
		// Player
		private var player:MediaPlayerSprite;
		private var media:VideoElement;
		private var timeline:TimelineMetadata;
		private var isPlaying:Boolean = false;
		
		// FreeWheel related members
		private var loader:AdManagerLoader;
		private var adManager:IAdManager;
		private var constants:IConstants;
		
		private var prerollSlots:Array;
		private var postrollSlots:Array;
		
		private var inPlayerSlot300x50:Sprite;
		
		public function DemoPlayer()
		{
			trace("new DemoPlayer()");
			flash.system.Security.allowDomain("*");
			
			player = new MediaPlayerSprite();
			player.width = 640;
			player.height = 480;
			player.mediaPlayer.addEventListener(MediaPlayerStateChangeEvent.MEDIA_PLAYER_STATE_CHANGE, onPlayerStateChange);
			addChild(player);
			
			var media:VideoElement = new VideoElement(new URLResource("http://playerdemo.freewheel.tv/video/test.mp4"));
			// Use TimelineMetadata to fire cue points for midroll and overlay slots
			timeline = new TimelineMetadata(media);
			timeline.addEventListener(TimelineMetadataEvent.MARKER_TIME_REACHED, onCuePoint);
			media.addMetadata("FW_CUEPOINT", timeline);
			player.mediaPlayer.autoPlay = false; // set to false because after preroll slots are played we will start content video automatically.
			player.media = media;
			
			// Initialize AdManager loader
			loader = new AdManagerLoader();
			loader.loadAdManager(player, onAdManagerLoaded, AdManagerLoader.LEVEL_DEBUG,
				FW_ADMANAGER_URL, new Date().valueOf());
		}
		
		private function onAdManagerLoaded(success:Boolean, message:String):void{
			trace("onAdManagerLoaded(" + arguments.join(", ") + ")");
			if (success){
				// Create a new AdManager instance
				adManager = loader.newAdManager();
				// Store constants for convenience
				constants = adManager.getConstants();
				// Configure global settings
				adManager.setServer(FW_SERVER_URL);
				adManager.setNetwork(FW_NETWORK_ID);
				adManager.setProfile(FW_PROFILE);
				/* Create a new Sprite and paste on to top of display objects hierachy.
				  You can register any Sprite as the video display based on your player layout,
				  perferrably one that covers the whole video player display area.
				*/
				var videoDisplay:Sprite = new Sprite();
				addChild(videoDisplay);
				adManager.registerVideoDisplay(videoDisplay);
				// Paste the in player display slot in place
				inPlayerSlot300x50 = new Sprite();
				inPlayerSlot300x50.width = 300;
				inPlayerSlot300x50.height = 50;
				addChild(inPlayerSlot300x50);
				adManager.setVideoDisplaySize(0, 0, player.width, player.height, 0, 0, player.width, player.height);
				// Add event listeners
				adManager.addEventListener(constants.EVENT_REQUEST_COMPLETE, onRequestComplete);
				adManager.addEventListener(constants.EVENT_SLOT_ENDED, onSlotEnded);
				adManager.addEventListener(constants.EVENT_PAUSESTATECHANGE_REQUEST, onPauseStateChangeRequest);
				
				submitRequest();
			}
		}
		
		private function submitRequest():void{
			trace("submitRequest()");
			// Request specific settings according to current location of site section / video asset, etc.
			adManager.setSiteSection("as3_demo_site_section");
			// Duration is mocked, change to your actual video duration.
			adManager.setVideoAsset("as3_demo_video_asset", 600);
			
			adManager.addVideoPlayerNonTemporalSlot("inPlayerSlot300x50", player, 300, 50);

			adManager.addTemporalSlot("preroll1", constants.ADUNIT_PREROLL, 0);
			adManager.addTemporalSlot("midroll1", constants.ADUNIT_MIDROLL, 10);
			adManager.addTemporalSlot("postroll1", constants.ADUNIT_POSTROLL, 600);
			/*
			Scan page slots defined in such format:
			<span id="slotId" class="_fwph" >
			<form id="_fw_form_slotId" style="display:none">
			<input type="hidden" name="_fw_input_slotId" id="_fw_input_slotId" value="slot_params_url">
			</form>
			<span id="_fw_container_slotId">org text</span>
			</span>
			
			In this demo the following page tag is used:
			<span id="300x250" class="_fwph" >
			<form id="_fw_form_300x250" style="display:none">
			<input type="hidden" name="_fw_input_300x250" id="_fw_input_300x250" value="w=300&h=250"/>
			</form>
			<span id="_fw_container_300x250">Advertisement</span>
			</span>
			 */
			adManager.scanSlotsOnPage();
			
			adManager.submitRequest();
		}
		
		private function onRequestComplete(evt:IEvent):void{
			trace("onRequestComplete(", evt.success, ")");
			prerollSlots = new Array();
			postrollSlots = new Array();
			var slot:ISlot;
			
			/*
			Store all temporal slots.
			Preroll and postroll slots are store in arrays, they will be played one after another.
			Midroll and overlay slots are stored as cue points, which will be triggered when video playhead time reaches their time positions.
			*/
			
			prerollSlots = adManager.getSlotsByTimePositionClass(constants.TIME_POSITION_CLASS_PREROLL);
			postrollSlots = adManager.getSlotsByTimePositionClass(constants.TIME_POSITION_CLASS_POSTROLL);
			
			var cuePoint:CuePoint;
			for each (slot in adManager.getSlotsByTimePositionClass(constants.TIME_POSITION_CLASS_MIDROLL)){
				cuePoint = new CuePoint(CuePointType.ACTIONSCRIPT, slot.getTimePosition(), slot.getCustomId(), null);
				timeline.addMarker(cuePoint);
			}
			
			for each (slot in adManager.getSlotsByTimePositionClass(constants.TIME_POSITION_CLASS_OVERLAY)){
				cuePoint = new CuePoint(CuePointType.ACTIONSCRIPT, slot.getTimePosition(), slot.getCustomId(), null);
				timeline.addMarker(cuePoint);
			}
			
			// Play all the non-temporal slots immediately
			for each (slot in adManager.getSiteSectionNonTemporalSlots()){
				slot.play();
			}
			for each (slot in adManager.getVideoPlayerNonTemporalSlots()){
				slot.play();
			}
			playPrerollSlots();
		}
		
		private function playPrerollSlots():void{
			trace("playPrerollSlots()");
			if (prerollSlots.length > 0){
				var slot:ISlot = prerollSlots.shift();
				slot.play();
			}
			else{
				trace("No more preroll slots to play, start content video now.");
				player.mediaPlayer.play();
			}
		}
		
		private function playPostrollSlots():void{
			trace("playPostrollSlots()");
			if (postrollSlots.length > 0){
				var slot:ISlot = postrollSlots.shift();
				slot.play();
			}
			else{
				trace("No more postroll slots to play, stop.");
			}
		}
		
		private function onSlotEnded(evt:IEvent):void{
			trace("onSlotEnded()");
			var slot:ISlot = adManager.getSlotByCustomId(evt.slotCustomId);
			if (slot.getTimePositionClass() == constants.TIME_POSITION_CLASS_PREROLL){
				playPrerollSlots();
			}
			else if (slot.getTimePositionClass() == constants.TIME_POSITION_CLASS_POSTROLL){
				playPostrollSlots();
			}
		}
		
		private function onPlayerStateChange(evt:MediaPlayerStateChangeEvent):void{
			trace("onPlayerStateChange(" + evt.state + ")");
			if (adManager){
				switch (evt.state){
					case MediaPlayerState.PLAYING:
						adManager.setVideoPlayStatus(constants.VIDEO_STATUS_PLAYING);
						isPlaying = true;
						break;
					case MediaPlayerState.PAUSED:
						adManager.setVideoPlayStatus(constants.VIDEO_STATUS_PAUSED);
						break;
					case MediaPlayerState.READY:
						if (isPlaying){
							adManager.setVideoPlayStatus(constants.VIDEO_STATUS_COMPLETED);
							isPlaying = false;
							trace("Content video playback completed, play postroll slots.");
							playPostrollSlots();
						}
						break;
				}
			}
		}
		
		/**
		 * When a cuepoint fires, a midroll or overlay slot will be played. AdManager will then
		 * give the media player instructions on whether to pause/resume accordingly. This event only
		 * fires after AdManager receives the notification that the content video has started by calling
		 * adManager.setVideoPlayStatus(VIDEO_STATUS_PLAYING).
		 */
		private function onPauseStateChangeRequest(evt:IEvent):void{
			trace("onPauseStateChangeRequest(" + evt.videoPause + ")");
			if (evt.videoPause){
				player.mediaPlayer.pause();
			}
			else{
				player.mediaPlayer.play();
			}
		}
		
		private function onCuePoint(evt:TimelineMetadataEvent):void{
			trace("onCuePoint(" + evt.marker.time + ")");
			var slot:ISlot = adManager.getSlotByCustomId((evt.marker as CuePoint).name);
			if (slot){
				slot.play();
			}
		}
	}
}
