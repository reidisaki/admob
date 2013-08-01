package tv.freewheel.renderer.SkeletonRenderer
{
	import flash.display.Sprite;
	import flash.utils.Timer;
	import flash.events.TimerEvent;
	import tv.freewheel.ad.behavior.IConstants;
	import tv.freewheel.ad.behavior.IRenderer;
	import tv.freewheel.ad.behavior.IRendererContext;
	import tv.freewheel.ad.behavior.IRendererController;
	
	public class SkeletonRenderer extends Sprite implements IRenderer{
	
		private var ctrl:IRendererController;
		private var ctxt:IRendererContext;
		private var constants:IConstants;
		private var timer:Timer;
		
		public function SkeletonRenderer()
		{
			trace( "SkeletonRenderer ");
		}
		
		public function getInfo():Object
		{
			return {moduleType: "renderer"};
		}
		
		public function init(context:IRendererContext, controller:IRendererController):void
		{
			this.ctxt = context;
			this.ctrl = controller;
			this.constants = this.ctxt.getConstants();
			this.ctrl.handleStateTransition(this.constants.RENDERER_STATE_INITIALIZE_COMPLETE);
			this.ctrl.setCapability( this.constants.RENDERER_CAPABILITY_VIDEOSTATUSCONTROL, true);
		}
		
		public function preload():void
		{
			this.ctrl.handleStateTransition(this.constants.RENDERER_STATE_PRELOAD_COMPLETE);
		}
		
		
		public function start():void
		{
			this.timer = new Timer(1000,1);
			this.timer.addEventListener(TimerEvent.TIMER,onTimer);
			this.timer.start();
			this.ctrl.handleStateTransition(this.constants.RENDERER_STATE_PLAYING);
		}
		private function onTimer(e:TimerEvent):void
		{
			this.stop(true);			
		}
		
		public function stop(immediate:Boolean=false):void
		{
			this.ctrl.handleStateTransition(this.constants.RENDERER_STATE_STOP_COMPLETE);
		}
		
		
		public function resize():void
		{
		}
		
		public function getPlayheadTime():Number
		{
			return -1;
		}
		
		public function getTotalBytes():int
		{
			return -1;
		}
		
		public function getBytesLoaded():int
		{
			return -1;
		}
		
		public function getDuration():Number
		{
			return -1;
		}
		
		public function setAdVolume(_volume:uint):void
		{
		}
		
		public function dispose():void
		{
			if(this.timer){
				this.timer.removeEventListener(TimerEvent.TIMER,onTimer);
				this.timer.stop();
			}
			this.timer = null;
		}
		
		public function pause():void
		{
		}
		
		public function getNewInstance(rendererInterfaceVersion:int):IRenderer
		{
			return this;
		}
		
		public function resume():void
		{
		}
	}
}

