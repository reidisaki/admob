//
//  NSMutableString+XMLEntities.m
//  RendererRunner
//
//  Created by Xiaoqiang Chen on 1/20/12.
//  Copyright (c) 2012 FreeWheel Media Inc. All rights reserved.
//

#import "NSString+XMLEntities.h"

@implementation NSMutableString (XMLEntities)

-(NSMutableString *)escapedXMLString {
	/*
	 http://www.w3.org/TR/REC-xml/#syntax
	 quot	"	U+0022 (34)	XML 1.0	double quotation mark
	 amp	&	U+0026 (38)	XML 1.0	ampersand
	 apos	'	U+0027 (39)	XML 1.0	apostrophe (= apostrophe-quote)
	 lt		<	U+003C (60)	XML 1.0	less-than sign
	 gt		>	U+003E (62)	XML 1.0	greater-than sign
	 */
	NSMutableString * ref = [NSMutableString stringWithString:self];
	[ref replaceOccurrencesOfString:@"&"  withString:@"&amp;"  options:NSLiteralSearch range:NSMakeRange(0, [ref length])];
	[ref replaceOccurrencesOfString:@"\"" withString:@"&quot;" options:NSLiteralSearch range:NSMakeRange(0, [ref length])];
	[ref replaceOccurrencesOfString:@"'"  withString:@"&apos;" options:NSLiteralSearch range:NSMakeRange(0, [ref length])];
	[ref replaceOccurrencesOfString:@"<"  withString:@"&lt;"   options:NSLiteralSearch range:NSMakeRange(0, [ref length])];
	[ref replaceOccurrencesOfString:@">"  withString:@"&gt;"   options:NSLiteralSearch range:NSMakeRange(0, [ref length])];
	return ref;
}

@end

@implementation NSString (XMLEntities)

-(NSString *)escapedXMLString {
	NSMutableString * mutStr = [NSMutableString stringWithString:self];
	return [mutStr escapedXMLString];
}

@end
