//
//  RendererConfiguration.h
//  RendererRunner
//
//  Copyright (c) 2011 FreeWheel Media Inc. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface RendererConfiguration : NSObject

@property (nonatomic, retain) NSString * rendererClassName; 
@property (nonatomic, retain) NSArray * ads;

-(id)initWithConfigFile:(NSString *)configFile;
-(NSString *)writeToFile:(NSString *)filename;
-(NSString *)writeToFile;
@end
