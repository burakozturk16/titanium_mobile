/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UILABEL

#import "TiViewProxy.h"

typedef enum {
    kContentTypeText,
    kContentTypeHTML
} ContentType;

@interface TiUILabelProxy : TiViewProxy {
    id _realLabelContent;
    NSString * contentString;
    int _contentHash;
    ContentType _contentType;
    
    NSMutableDictionary * options;
    BOOL attributeTextNeedsUpdate;
    BOOL configSet;
}
-(id)getLabelContent;
-(void)updateAttributeText;

@end

#endif