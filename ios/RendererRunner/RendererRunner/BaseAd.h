//
//  BaseAd.h
//  RendererRunner
//
//  Copyright (c) 2011 FreeWheel Media Inc. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface BaseAd : NSObject

@property (nonatomic, retain) NSString * slotType;
@property (nonatomic, retain) NSString * baseUnit;
@property (nonatomic, retain) NSString * url;
@property (nonatomic, retain) NSString * content;
@property (nonatomic) float duration;
@property (nonatomic, retain) NSArray * otherAssets;
@property (nonatomic, retain) NSArray * param;
@property (nonatomic) float slotTimePos;
@property (nonatomic) uint slotWidth;
@property (nonatomic) uint slotHeight;
@property (nonatomic, retain) NSString * defaultClickThrough;
@property (nonatomic, retain) NSString * creativeApi;

@property (nonatomic, readonly, getter=getXmlRepresentation) NSString * xmlRepresentation;
@property (nonatomic) uint adId;
@property (nonatomic, readonly, getter=getAdCreativeId) uint adCreativeId;
@property (nonatomic, readonly, getter=getAdCreativeRenditionId) uint adCreativeRenditionId;

-(id)initWithData:(NSDictionary *)dict;

@end
