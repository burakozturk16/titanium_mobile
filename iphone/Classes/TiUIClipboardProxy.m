/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiUIClipboardProxy.h"
#import "TiUtils.h"
#import "TiApp.h"
#import "TiBlob.h"
#import "TiFile.h"
#import "TiUtils.h"

#import <MobileCoreServices/UTType.h>
#import <MobileCoreServices/UTCoreTypes.h>

typedef enum {
	CLIPBOARD_TEXT,
	CLIPBOARD_URI_LIST,
	CLIPBOARD_IMAGE,
	CLIPBOARD_UNKNOWN
} ClipboardType;

static ClipboardType mimeTypeToDataType(NSString *mimeType)
{
	mimeType = [mimeType lowercaseString];
    
	// Types "URL" and "Text" are for IE compatibility. We want to have
	// a consistent interface with WebKit's HTML 5 DataTransfer.
	if ([mimeType isEqualToString: @"text"] || [mimeType hasPrefix: @"text/plain"])
	{
		return CLIPBOARD_TEXT;
	}
	else if ([mimeType isEqualToString: @"url"] || [mimeType hasPrefix: @"text/uri-list"])
	{
		return CLIPBOARD_URI_LIST;
	}
	else if ([mimeType hasPrefix: @"image"])
	{
		return CLIPBOARD_IMAGE;
	}
	else
	{
		// Something else, work from the MIME type.
		return CLIPBOARD_UNKNOWN;
	}
}

static NSString * dataTypeToString(ClipboardType mimeType)
{
    switch (mimeType ){
        case CLIPBOARD_URI_LIST:
            return @"url";
            case CLIPBOARD_IMAGE:
            return @"image";
        case CLIPBOARD_TEXT:
            return @"text";
            default:
            return nil;
    }
 }

static NSString *mimeTypeToUTType(NSString *mimeType)
{
	NSString *uti = [(NSString *)UTTypeCreatePreferredIdentifierForTag(kUTTagClassMIMEType, (CFStringRef)mimeType, NULL) autorelease];
	if (uti == nil) {
		// Should we do this? Lets us copy/paste custom data, anyway.
		return mimeType;
	}
	return uti;
}

@implementation TiUIClipboardProxy {
    NSUInteger pasteboardChangeCount_;
    BOOL _registeredForChange;
}

-(NSString*)apiName
{
    return @"Ti.UI.Clipboard";
}
-(void)startup
{
    pasteboardChangeCount_ = 0;
    _registeredForChange = NO;
    [super startup];
}

-(void)dealloc
{
    TiThreadPerformOnMainThread(^{
        [[NSNotificationCenter defaultCenter] removeObserver:self];
    }, YES);
    
    [super dealloc];
}

-(void)_listenerAdded:(NSString*)type count:(NSInteger)count
{
    if (count == 1 && [type isEqual:@"change"])
    {
        pasteboardChangeCount_ = 0;
        _registeredForChange = YES;
        TiThreadPerformOnMainThread(^{
            [[NSNotificationCenter defaultCenter]
             addObserver:self
             selector:@selector(pasteboardChangedNotification:)
             name:UIPasteboardChangedNotification
             object:[UIPasteboard generalPasteboard]];
            [[NSNotificationCenter defaultCenter]
             addObserver:self
             selector:@selector(pasteboardChangedNotification:)
             name:UIPasteboardRemovedNotification
             object:[UIPasteboard generalPasteboard]];
        }, YES);
    }
}

-(void)_listenerRemoved:(NSString*)type count:(NSInteger)count
{
    if (count == 0 && [type isEqual:@"change"])
    {
        _registeredForChange = NO;
        TiThreadPerformOnMainThread(^{
            [[NSNotificationCenter defaultCenter] removeObserver:self name:UIPasteboardChangedNotification object:[UIPasteboard generalPasteboard]];
            [[NSNotificationCenter defaultCenter] removeObserver:self name:UIPasteboardRemovedNotification object:[UIPasteboard generalPasteboard]];
        }, YES);
    }
}

- (void)pasteboardChangedNotification:(NSNotification*)notification {
    pasteboardChangeCount_ = [UIPasteboard generalPasteboard].changeCount;
    UIPasteboard *board = [UIPasteboard generalPasteboard];
    NSMutableDictionary* dict = [NSMutableDictionary dictionary];
    if (board.URL) {
        [dict setObject:[board.URL absoluteString] forKey:dataTypeToString(CLIPBOARD_URI_LIST)];
    }
    if (board.image) {
        [dict setObject:[[[TiBlob alloc] initWithImage: board.image] autorelease] forKey:dataTypeToString(CLIPBOARD_IMAGE)];
    }
    if (board.string) {
        [dict setObject:board.string forKey:dataTypeToString(CLIPBOARD_TEXT)];
    }
    [self fireEvent:@"change" withObject:dict];
}

-(void)resumed:(id)sender
{
    if (pasteboardChangeCount_ != [UIPasteboard generalPasteboard].changeCount) {
        [[NSNotificationCenter defaultCenter]
         postNotificationName:UIPasteboardChangedNotification
         object:[UIPasteboard generalPasteboard]];
    }
}


-(void)clearData:(id)arg
{
	ENSURE_UI_THREAD(clearData, arg);
	ENSURE_STRING_OR_NIL(arg);

	NSString *mimeType = arg;
	UIPasteboard *board = [UIPasteboard generalPasteboard];
	ClipboardType dataType = mimeTypeToDataType(mimeType);
	switch (dataType)
	{
		case CLIPBOARD_TEXT:
		{
			board.strings = nil;
			break;
		}
		case CLIPBOARD_URI_LIST:
		{
			board.URLs = nil;
			break;
		}
		case CLIPBOARD_IMAGE:
		{
			board.images = nil;
			break;
		}
		case CLIPBOARD_UNKNOWN:
		default:
		{
			[board setData: nil forPasteboardType: mimeTypeToUTType(mimeType)];
		}
	}
}
	 
-(void)clearText:(id)args
{
	ENSURE_UI_THREAD(clearText,args);

	UIPasteboard *board = [UIPasteboard generalPasteboard];
	board.strings = nil;
}
	 
-(id)getData:(id)args
{
    id arg = nil;
    if ([args isKindOfClass:[NSArray class]])
    {
        if ([args count] > 0)
        {
            arg = [args objectAtIndex:0];
        }
    }
    else 
    {
        arg = args;
    }
	ENSURE_STRING(arg);
	NSString *mimeType = arg;
	__block id result;
	TiThreadPerformOnMainThread(^{
		result = [[self getData_: mimeType] retain];
	}, YES);
	return [result autorelease];
}

// Must run on main thread.
-(id)getData_:(NSString *)mimeType
{
	UIPasteboard *board = [UIPasteboard generalPasteboard];
	ClipboardType dataType = mimeTypeToDataType(mimeType);
	switch (dataType)
	{
		case CLIPBOARD_TEXT:
		{
			return board.string;
		}
		case CLIPBOARD_URI_LIST:
		{
			return [board.URL absoluteString];
		}
		case CLIPBOARD_IMAGE:
		{
			UIImage *image = board.image;
			if (image) {
				return [[[TiBlob alloc] initWithImage: image] autorelease];
			} else {
				return nil;
			}
		}
		case CLIPBOARD_UNKNOWN:
		default:
		{
			NSData *data = [board dataForPasteboardType: mimeTypeToUTType(mimeType)];

			if (data) {
				return [[[TiBlob alloc] initWithData: data mimetype: mimeType] autorelease];
			} else {
				return nil;
			}
		}
	}
}
	 
-(NSString *)getText:(id)args
{
	return [self getData: @"text/plain"];
}
	 
-(id)hasData:(id)args
{
    id arg = nil;
    if ([args isKindOfClass:[NSArray class]])
    {
        if ([args count] > 0)
        {
            arg = [args objectAtIndex:0];
        }
    }
    else 
    {
        arg = args;
    }
	ENSURE_STRING_OR_NIL(arg);
	NSString *mimeType = arg;
	__block BOOL result=NO;
	TiThreadPerformOnMainThread(^{
		UIPasteboard *board = [UIPasteboard generalPasteboard];
		ClipboardType dataType = mimeTypeToDataType(mimeType);
		
		switch (dataType)
		{
			case CLIPBOARD_TEXT:
			{
				result=[board containsPasteboardTypes: UIPasteboardTypeListString];
				break;
			}
			case CLIPBOARD_URI_LIST:
			{
				result=[board containsPasteboardTypes: UIPasteboardTypeListURL];
				break;
			}
			case CLIPBOARD_IMAGE:
			{
				result=[board containsPasteboardTypes: UIPasteboardTypeListImage];
				break;
			}
			case CLIPBOARD_UNKNOWN:
			default:
			{
				result=[board containsPasteboardTypes: [NSArray arrayWithObject: mimeTypeToUTType(mimeType)]];
				break;
			}
		}
	}, YES);
	return NUMBOOL(result);
}
	 
-(id)hasText:(id)args
{
	return [self hasData: @"text/plain"];
}

-(void)setData:(id)args
{
	ENSURE_ARG_COUNT(args,2);
	ENSURE_UI_THREAD(setData,args);
	
	NSString *mimeType = [TiUtils stringValue: [args objectAtIndex: 0]];
	id data = [args objectAtIndex: 1];
    if (data == nil) {
        DebugLog(@"[WARN] setData: data object was nil.");
        return;
    }
	UIPasteboard *board = [UIPasteboard generalPasteboard];
	ClipboardType dataType = mimeTypeToDataType(mimeType);
	
	switch (dataType)
	{
		case CLIPBOARD_TEXT:
		{
			board.string = [TiUtils stringValue: data];
			break;
		}
		case CLIPBOARD_URI_LIST:
		{
			board.URL = [NSURL URLWithString: [TiUtils stringValue: data]];
			break;
		}
		case CLIPBOARD_IMAGE:
		{
			board.image = [TiUtils toImage: data proxy: self];
			break;
		}
		case CLIPBOARD_UNKNOWN:
		default:
		{
			NSData *raw;
			if ([data isKindOfClass:[TiBlob class]])
			{
				raw = [(TiBlob *)data data];
			}
			else if ([data isKindOfClass:[TiFile class]])
			{
				raw = [[(TiFile *)data blob] data];
			}
			else
			{
				raw = [[TiUtils stringValue: data] dataUsingEncoding: NSUTF8StringEncoding];
			}

			[board setData: raw forPasteboardType: mimeTypeToUTType(mimeType)];
		}
	}
}

-(void)setText:(id)arg
{
	ENSURE_STRING(arg);
	NSString *text = arg;
	[self setData: [NSArray arrayWithObjects: @"text/plain",text,nil]];
}
	 
 @end
