Renderer Runner
===============
The Renderer Runner is used for third party developers to create and test ad renderers or creatives that integrates with FreeWheel's ad platform.

## Requirements
The Renderer Runner will load ads from Resources directory, it requires either Resources/response.xml or Resources/config.json, by following the priorities as below:
1. If there is a Resources/response.xml, load ads from response.xml.
2. If there is a Resources/config.json, create ads from the configuration and load.
3. Trigger error if there's no configuration.

## Simple Usage
1. Configure response.xml or config.json.
2. Open the configured file from iOS devices that has RendererRunner installed.
**Note** by default RendererRunner supports renderers that are bundled in AdManager.

## Developer's Usage
0. Copy AdManager.framework to <ProjectFolder>/Frameworks folder.
1. Add your renderer to the project as a framework. For how to create a renderer framework, please refer to SkeletonRenderer.framework.
2. Configure the ad type, ad content, etc in config.json or response.xml.
3. Configure renderer class name in response.xml or config.json.
4. Run.

## Configuration
### Configure config.json
Update the value of rendererClass in config.json. 
For example the configuration "rendererClass":"FWVideoAdRenderer" will make Renderer Runner to load FWVideoAdRenderer to render ads.
**Limitation**  Renderer Runner supports only one renderer class per time.
### Required parameters for config.json
There're requirements for each parameter in config.json to work properly.
This is a minimium list of parameter for different types of ads that required for an ad to be properly handled.
#### temporal ads
	* slotType
	value can be one of "preroll", "midroll", "overlay", "postroll"
	* slotTimePos
	a float value >= 0
	* creativeApi
	optional parameter. Specify "MRAID-1.0" for MRAID ad.
	* slotWidth
	integer value > 0
	* slotHeight
	integer value > 0
	* duration
	a float value > 0
	* defaultClickThrough
	optional, a click-through URL that will be opened in a browser
	* url or content
#### display ads
	* slotType
	"display"
	* creativeApi
	optional parameter. Specify "MRAID-1.0" for MRAID ad.
	* slotWidth
	integer value > 0
	* slotHeight
	integer value > 0
	* defaultClickThrough
	optional, a click-through URL that will be opened in a browser
	* url or content
It's strongly recommended to set slotWidth, slotHeight and duration to a noticable value.
All parameter names are case-sensitive.
And the file should be in a proper JSON format.


### Configure response.xml
Update the value of RendererManifest element in response.xml.
For exmaple, "class://FWHTMLRenderer" will make Renderer Runner to load FWHTMLRenderer to render ads.

## Copyright
Copyright (c) 2012 FreeWheel Media Inc. All rights reserved.
JSONKit is dual licensed under either the terms of the BSD License, or alternatively 
under the terms of the Apache License, Version 2.0, as specified in JSONKit.h.
