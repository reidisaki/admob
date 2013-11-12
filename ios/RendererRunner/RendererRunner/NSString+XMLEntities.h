//
//  NSMutableString+XMLEntities.h
//  RendererRunner
//
//  Created by Xiaoqiang Chen on 1/20/12.
//  Copyright (c) 2012 FreeWheel Media Inc. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface NSMutableString (XMLEntities)
-(NSMutableString *)escapedXMLString;
@end
@interface NSString (XMLEntities)
-(NSString *)escapedXMLString;
@end
