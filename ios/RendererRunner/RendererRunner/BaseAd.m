//
//  BaseAd.m
//  RendererRunner
//
//  Copyright (c) 2011 FreeWheel Media Inc. All rights reserved.
//

#import "NSString+XMLEntities.h"
#import "BaseAd.h"

@interface NSDictionary(RendererRunner)
-(id)valueForKeyWithDefaultNil:(NSString *)key;
-(id)valueForKey:(NSString *)key valueForUndefined:(id)value;
@end

@implementation NSDictionary(RendererRunner)

-(id)valueForKeyWithDefaultNil:(NSString *)key {
	return [self valueForKey:key valueForUndefined:nil];
}

-(id)valueForKey:(NSString *)key valueForUndefined:(id)value {
	@try {
		return [self valueForKey:key];
	}
	@catch (NSException *exception) {
		return value;
	}
}

@end


@implementation BaseAd

@synthesize slotType, baseUnit, url, content, duration, otherAssets, param, xmlRepresentation, adId, adCreativeId, adCreativeRenditionId;
@synthesize slotTimePos, slotWidth, slotHeight, defaultClickThrough, creativeApi;

-(id)initWithData:(NSDictionary *)dict {
	if ((self = [super init]) != NULL) {
		self.slotType = [dict valueForKey:@"slotType"];
		self.slotType = [self.slotType lowercaseString];
		self.baseUnit = [dict valueForKey:@"baseUnit" valueForUndefined:@"fixed-size-interactive"];
		if (self.baseUnit.length == 0) {
			self.baseUnit = @"fixed-size-interactive";
		}
		self.url = [dict valueForKey:@"url" valueForUndefined:@""];
		self.content = [dict valueForKey:@"content" valueForUndefined:@""];
		self.duration = [[dict valueForKey:@"duration" valueForUndefined:[NSNumber numberWithFloat:-1.0]] floatValue];
		self.otherAssets = [dict valueForKeyWithDefaultNil:@"otherAssets"];
		self.creativeApi = [dict valueForKeyWithDefaultNil:@"creativeApi"];
		self.param = [dict valueForKeyWithDefaultNil:@"param"];
		self.slotTimePos = [[dict valueForKey:@"slotTimePos" valueForUndefined:[NSNumber numberWithFloat:-1.0]] floatValue];
		self.slotWidth = [[dict valueForKey:@"slotWidth" valueForUndefined:[NSNumber numberWithUnsignedInt:0]] unsignedIntValue];
		self.slotHeight = [[dict valueForKey:@"slotHeight" valueForUndefined:[NSNumber numberWithUnsignedInt:0]] unsignedIntValue];
		self.defaultClickThrough = [dict valueForKey:@"defaultClickThrough" valueForUndefined:@"http://www.freewheel.tv"];
		self.adId = 0;
	}
	return self;
	
}

-(NSString *) getXmlRepresentation {
	NSString * durationStr = (self.duration > 0) ? [NSString stringWithFormat:@" duration=\"%f\"", self.duration] :@"";
	NSString * rectStr = (self.slotWidth > 0 && self.slotHeight > 0) ? [NSString stringWithFormat:@" width=\"%u\" height=\"%u\"", self.slotWidth, self.slotHeight] : @"";
	NSString * paramStr = @"";
	NSString * otherAssetsStr = @"";
	
	if (self.param != nil) {
		paramStr = @"<parameters>";
		for (NSDictionary * kvp in self.param) {
			for (NSString * key in kvp)
			{
				paramStr = [paramStr stringByAppendingFormat:@"<parameter name=\"%@\">%@</parameter>", [key escapedXMLString], [[kvp valueForKey:key] escapedXMLString]];
			}
		}
		paramStr = [paramStr stringByAppendingString:@"</parameters>"];
	}
	if (self.otherAssets != nil) {
		otherAssetsStr = @"<otherAssets>";
		uint otherAssetsId = self.adCreativeRenditionId + 2; // start after renditionid+1. renditionid+1 is reserved for main asset id.
		for (NSDictionary * kvp in self.otherAssets) {
			for (NSString * key in kvp)
			{
				otherAssetsStr = [otherAssetsStr stringByAppendingFormat:
					@"<asset id=\"%u\" name=\"%@\" contentType=\"text/html_doc_lit_mobile\" mimeType=\"text/html\" url=\"%@\"></asset>",
					otherAssetsId, [key escapedXMLString], [[kvp valueForKey:key] escapedXMLString]];
				otherAssetsId = otherAssetsId + 1;
			}
		}
		otherAssetsStr = [otherAssetsStr stringByAppendingString:@"</otherAssets>"];
	}
	NSString * ret = [NSString stringWithFormat:@""
		"<ad adId=\"%u\">"
		"<creatives>"
		"<creative creativeId=\"%u\"  baseUnit=\"%@\" %@>"
		"%@"
		"<creativeRenditions><creativeRendition creativeRenditionId=\"%u\"%@ %@>"
		"<asset id=\"%u\" contentType=\"text/html_doc_lit_mobile\" mimeType=\"text/html\"%@>%@</asset>%@</creativeRendition></creativeRenditions></creative></creatives></ad>",
		self.adId,
		self.adCreativeId,
		[self.baseUnit escapedXMLString],
		durationStr,//optional duration
		paramStr,
		self.adCreativeRenditionId,
		(self.creativeApi) ? [NSString stringWithFormat:@" creativeApi=\"%@\"", self.creativeApi] : @"",
		rectStr,//optional width/height
		self.adCreativeRenditionId+1,//using the renditionid +1 for asset id.
		([self.content length] == 0 && [self.url length] > 0) ? [NSString stringWithFormat:@" url=\"%@\"", [self.url escapedXMLString]] : @"", //optional url
		([self.content length] > 0) ? [NSString stringWithFormat:@"<content><![CDATA[\n%@\n]]></content>", self.content] : @"", //optional content
		otherAssetsStr // optional otherAssets
		];
	return ret;
}

-(uint)getAdCreativeId{
	return self.adId+1;
}

-(uint)getAdCreativeRenditionId{
	return self.adId+2;
}

-(NSString *)description{
	return [NSString stringWithFormat:@""
		"adId: %u\n"
		"slotType: %@\n"
		"url: %@\n"
		"content: %@\n"
		"duration: %f\n"
		"otherAssets: %@\n"
		"defaultClickThrough: %@\n"
		"param: %@\n"
		,
		self.adId,
		self.slotType,
		self.url,
		self.content,
		self.duration,
		self.otherAssets,
		self.defaultClickThrough,
		self.param
		];
}

@end
