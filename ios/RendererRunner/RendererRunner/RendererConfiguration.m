//
//  RendererConfiguration.m
//  RendererRunner
//
//  Copyright (c) 2011 FreeWheel Media Inc. All rights reserved.
//

#import "RendererConfiguration.h"
#import "BaseAd.h"
#import "NSString+XMLEntities.h"
#import "JSONKit.h"

@interface RendererConfiguration (privateMethods)
-(void)parseData:(NSDictionary *)dict;
-(NSString*)rendererManifestXml;
-(NSString*)adsToXml;
-(NSString*)siteSectionsXml;
+(NSString *)stringByEncodingToUrlComponent:(NSString *)str;
@end

@implementation RendererConfiguration

@synthesize rendererClassName;
@synthesize ads;

-(id)initWithConfigFile:(NSString *)configFile
{
	if ((self = [super init]) != NULL) {
		NSData* jsonData = [NSData dataWithContentsOfFile: configFile];
		[self parseData:[jsonData objectFromJSONDataWithParseOptions:JKParseOptionComments]];
	}
	return self;
}

-(NSString *)writeToFile:(NSString *)filename
{
	//get the documents directory:
	NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
	NSString *documentsDirectory = [paths objectAtIndex:0];
	NSString *filePath = [NSString stringWithFormat:@"%@/%@", 
						  documentsDirectory, filename];
	
	NSString *content = [NSString stringWithFormat:@"<adResponse version=\"1\">%@%@%@</adResponse>",
						 [self rendererManifestXml],
						 [self adsToXml],
						 [self siteSectionsXml]];
	
	NSLog(@"content:\n%@", content);
	
	[content writeToFile:filePath 
			  atomically:NO 
				encoding:NSUTF8StringEncoding
				   error:nil];
	return filePath;
}

-(NSString *)writeToFile
{
	return [self writeToFile:@"adResponse.xml"];
}

@end

@implementation RendererConfiguration(privateMethods)

-(void)parseData:(NSDictionary *)dict {
	NSLog(@"Parsing data: %@", dict ? dict : @"<NO DATA> Error parsing JSON"); //TODO more visible alert
	rendererClassName = [dict objectForKey:@"rendererClass"];
	self.ads = [dict objectForKey:@"testAds"];
	NSMutableArray * adHolder = [[NSMutableArray alloc] init];
	uint assignedId = 10000;
	for (NSDictionary * ad in ads) {
		BaseAd * base = [[BaseAd alloc] initWithData:ad];
		base.adId = assignedId;
		[adHolder addObject:base];
		[base release];
		assignedId += 100;
	}
	self.ads = adHolder;
	[self.ads sortedArrayUsingComparator:^NSComparisonResult(BaseAd* obj1, BaseAd* obj2) {
		if ([obj1.slotType isEqualToString:obj2.slotType]) {
			return [[NSNumber numberWithFloat:obj1.slotTimePos] compare:[NSNumber numberWithFloat:obj2.slotTimePos]];
		} else {
			return [obj1.slotType compare:obj2.slotType];
		}
	}];
	[adHolder release];
	NSLog(@"Parsed data: %@", self.ads);
}

//TODO: replace hand-crafted xml string join method with libxml2 XMLDocument class.

-(NSString *)rendererManifestXml {
	NSString * ret = [NSString stringWithFormat:@""
					  "<rendererManifest version='1'>"
					  "&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;"
					  "&lt;adRenderers version=&apos;1&apos;&gt;"
					  "&lt;adRenderer name=&apos;%@&apos; url=&apos;class://%@&apos;&gt;"
					  "&lt;/adRenderer&gt;"
					  "&lt;/adRenderers&gt;"
					  "</rendererManifest>",
					  self.rendererClassName,
					  self.rendererClassName];
	return ret;
}

-(NSString *)adsToXml {
	NSString * ret = @"<ads>";
	for (BaseAd* ad in self.ads) {
		ret = [ret stringByAppendingString:ad.xmlRepresentation];
	}
	ret = [ret stringByAppendingString:@"</ads>"];
	return ret;
}

-(NSString*)siteSectionsXml{
	NSString * ret= @"<siteSection><videoPlayer><videoAsset><adSlots>";
	
	// add temporal ads
	
	
	BaseAd * prev = nil;
	for (BaseAd *ad in ads) {
		if ([ad.slotType isEqualToString:@"preroll"] ||
			[ad.slotType isEqualToString:@"midroll"] ||
			[ad.slotType isEqualToString:@"pause_midroll"] ||
			[ad.slotType isEqualToString:@"postroll"] ||
			[ad.slotType isEqualToString:@"overlay"]
			) {
			
			if ((prev != nil) && (ad.slotTimePos != prev.slotTimePos || ![ad.slotType isEqualToString:prev.slotType])) {
				ret = [ret stringByAppendingFormat:@"</selectedAds></temporalAdSlot>"];
			}
			
			NSString * callbackElements = [NSString stringWithFormat:@"<eventCallback type='GENERIC' url='http://demo.v.fwmrm.net/ad/l/1?s=dbg-3rd&amp;n=96749&amp;t=1362468439682087002&amp;adid=%u&amp;reid=%u&amp;arid=0&amp;iw=&amp;uxnw=&amp;uxss=&amp;uxct='/><eventCallback name='defaultImpression' type='IMPRESSION' url='http://demo.v.fwmrm.net/ad/l/1?s=dbg-3rd&amp;n=96749&amp;t=1362468439682087002&amp;adid=%u&amp;reid=%u&amp;arid=0&amp;auid=&amp;cn=defaultImpression&amp;et=i&amp;_cc=&amp;tpos=0&amp;iw=&amp;uxnw=&amp;uxss=&amp;uxct=&amp;init=1&amp;cr='/><eventCallback name='defaultClick' type='CLICK' showBrowser='true' url='http://nycadvip1-d.fwmrm.net/ad/l/1?s=debug&amp;n=96749&amp;t=1343028808684880004&amp;adid=%u&amp;reid=%u&amp;arid=0&amp;auid=&amp;cn=defaultClick&amp;et=c&amp;_cc=&amp;tpos=&amp;cr=%@'/>", ad.adId, ad.adCreativeRenditionId, ad.adId, ad.adCreativeRenditionId, ad.adId, ad.adCreativeRenditionId, [RendererConfiguration stringByEncodingToUrlComponent:ad.defaultClickThrough]];
			
			if (prev == nil || ad.slotTimePos != prev.slotTimePos || ![ad.slotType isEqualToString:prev.slotType]) {
				ret = [ret stringByAppendingFormat:@"<temporalAdSlot adUnit=\"%@\" customId=\"video-slot-%@-%f\" timePosition=\"%f\" timePositionClass=\"%@\">"
					   "<selectedAds><adReference adId=\"%u\" creativeId=\"%u\" creativeRenditionId=\"%u\">"
					   "<eventCallbacks>%@</eventCallbacks>"
					   "</adReference>"
					   ,
					   [ad.slotType escapedXMLString],
					   [ad.slotType escapedXMLString],
					   ad.slotTimePos,
					   ad.slotTimePos,
					   [ad.slotType escapedXMLString],
					   ad.adId,
					   ad.adCreativeId,
					   ad.adCreativeRenditionId,
					   callbackElements
					   ];
			} else {
				ret = [ret stringByAppendingFormat:@"<adReference adId=\"%u\" creativeId=\"%u\" creativeRenditionId=\"%u\">"
					   "<eventCallbacks>%@</eventCallbacks>"
					   "</adReference>"
					   ,
					   ad.adId,
					   ad.adCreativeId,
					   ad.adCreativeRenditionId,
					   callbackElements
					   ];
			}
			prev = ad;
		}
	}
	if (prev != nil) {
		ret = [ret stringByAppendingFormat:@"</selectedAds></temporalAdSlot>"];
		prev = nil;
	}
	
	ret = [ret stringByAppendingString:@"</adSlots>"];
	ret = [ret stringByAppendingString:@"</videoAsset></videoPlayer><adSlots>"];
	
	//add nonTemporalAdSlotSet
	uint count = 0;
	for (BaseAd *ad in ads) {
		if ([ad.slotType isEqualToString:@"display"]) {
			NSString *callbackElements = [NSString stringWithFormat:@"<eventCallback type='GENERIC' url='http://demo.v.fwmrm.net/ad/l/1?s=dbg-3rd&amp;n=96749&amp;t=1362468439682087002&amp;adid=%u&amp;reid=%u&amp;arid=0&amp;iw=&amp;uxnw=&amp;uxss=&amp;uxct='/><eventCallback name='defaultImpression' type='IMPRESSION' url='http://demo.v.fwmrm.net/ad/l/1?s=dbg-3rd&amp;n=96749&amp;t=1362468439682087002&amp;adid=%u&amp;reid=%u&amp;arid=0&amp;auid=&amp;cn=defaultImpression&amp;et=i&amp;_cc=&amp;tpos=0&amp;iw=&amp;uxnw=&amp;uxss=&amp;uxct=&amp;init=1&amp;cr='/><eventCallback name='defaultClick' type='CLICK' showBrowser='true' url='http://nycadvip1-d.fwmrm.net/ad/l/1?s=debug&amp;n=96749&amp;t=1343028808684880004&amp;adid=%u&amp;reid=%u&amp;arid=0&amp;auid=&amp;cn=defaultClick&amp;et=c&amp;_cc=&amp;tpos=&amp;cr=%@'/>", ad.adId, ad.adCreativeRenditionId, ad.adId, ad.adCreativeRenditionId, ad.adId, ad.adCreativeRenditionId, [RendererConfiguration stringByEncodingToUrlComponent:ad.defaultClickThrough]];
			ret = [ret stringByAppendingFormat:@"<adSlot customId=\"display-%u\"><selectedAds><adReference adId=\"%u\" creativeId=\"%u\" creativeRenditionId=\"%u\"><eventCallbacks>%@</eventCallbacks></adReference></selectedAds></adSlot>",
				   count,
				   ad.adId,
				   ad.adCreativeId,
				   ad.adCreativeRenditionId,
				   callbackElements];
			count ++;
		}
	}
	ret = [ret stringByAppendingString:@"</adSlots></siteSection>"];
	return ret;
}

+ (NSString *)stringByEncodingToUrlComponent:(NSString *)str {
	// Encode all the reserved characters, per RFC 3986 (<http://www.ietf.org/rfc/rfc3986.txt>)
	CFStringRef result = CFURLCreateStringByAddingPercentEscapes(kCFAllocatorDefault,(CFStringRef)str, NULL, (CFStringRef)@"!*'();:@&=+$,/?%#[]", kCFStringEncodingUTF8);
	return [(NSString *)result autorelease];
}

@end
