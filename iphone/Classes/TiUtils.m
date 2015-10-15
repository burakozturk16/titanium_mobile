/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2015 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#import <QuartzCore/QuartzCore.h>
#import <CommonCrypto/CommonDigest.h>

#import "TiBase.h"
#import "TiUtils.h"
#import "TiHost.h"
#import "TiPoint.h"
#import "TiProxy.h"
#import "ImageLoader.h"
#import "WebFont.h"
#import "TiDimension.h"
#import "TiColor.h"
#import "TiFile.h"
#import "TiBlob.h"
#import "TiExceptionHandler.h"
#import "SVGKit.h"
#import "TiFileSystemHelper.h"
#import	"Ti2DMatrix.h"

#import "TouchDelegate_Views.h"
#import "TiUIView.h"
#import "TiApp.h"

#import "DDMathEvaluator.h"

// for checking version
#import <sys/utsname.h>

#import "UIImage+Resize.h"

#if TARGET_IPHONE_SIMULATOR
extern NSString * const TI_APPLICATION_RESOURCE_DIR;
#endif

static NSDictionary* encodingMap = nil;
static NSDictionary* typeMap = nil;
static NSDictionary* sizeMap = nil;
static NSString* kAppUUIDString = @"com.appcelerator.uuid"; // don't obfuscate

const TiCap TiCapUndefined = {{TiDimensionTypeUndefined, 0}, {TiDimensionTypeUndefined, 0}, {TiDimensionTypeUndefined, 0}, {TiDimensionTypeUndefined, 0}};

@implementation TiUtils

+(TiOrientationFlags) TiOrientationFlagsFromObject:(id)args
{
    if (![args isKindOfClass:[NSArray class]]) {
        return TiOrientationNone;
    }
    
    TiOrientationFlags result = TiOrientationNone;
    for (id mode in args) {
        UIInterfaceOrientation orientation = (UIInterfaceOrientation)[TiUtils orientationValue:mode def:-1];
        switch ((int)orientation)
        {
            case UIDeviceOrientationPortrait:
            case UIDeviceOrientationPortraitUpsideDown:
            case UIDeviceOrientationLandscapeLeft:
            case UIDeviceOrientationLandscapeRight:
                TI_ORIENTATION_SET(result,orientation);
                break;
            case UIDeviceOrientationUnknown:
                DebugLog(@"[WARN] Ti.Gesture.UNKNOWN / Ti.UI.UNKNOWN is an invalid orientation mode.");
                break;
            case UIDeviceOrientationFaceDown:
                DebugLog(@"[WARN] Ti.Gesture.FACE_DOWN / Ti.UI.FACE_DOWN is an invalid orientation mode.");
                break;
            case UIDeviceOrientationFaceUp:
                DebugLog(@"[WARN] Ti.Gesture.FACE_UP / Ti.UI.FACE_UP is an invalid orientation mode.");
                break;
            default:
                DebugLog(@"[WARN] An invalid orientation was requested. Ignoring.");
                break;
        }
    }
    return result;
}

+(int) dpi
{
    static int dpi;
    static dispatch_once_t predicate;
    dispatch_once(&predicate, ^{
        if ([TiUtils isIPad]) {
            if ([TiUtils isRetinaDisplay]) {
                dpi = 260;
            }
            else {
                dpi = 130;
            }
        }
        else {
          	if ([TiUtils isRetinaHDDisplay]) {
         	   dpi = 480;
        	} else if ([TiUtils isRetinaDisplay]) {
        	    dpi = 320;
        	}
            else {
                dpi = 160;
            }
        }
    });
    
    return dpi;
}

+(BOOL)isRetinaFourInch
{
    static BOOL isRetinaFourInch = NO;
    static dispatch_once_t predicate;
    dispatch_once(&predicate, ^{
        CGSize mainScreenBoundsSize = [[UIScreen mainScreen] bounds].size;
        if ([TiUtils isIOS8OrGreater]) {
            isRetinaFourInch = (mainScreenBoundsSize.height == 568 || mainScreenBoundsSize.width == 568);
        } else {
            isRetinaFourInch =  (mainScreenBoundsSize.height == 568);
        }
    });
    return isRetinaFourInch;
}

+(BOOL)isRetinaiPhone6
{
    static BOOL isRetinaiPhone6 = NO;
    static dispatch_once_t predicate;
    dispatch_once(&predicate, ^{
        if ([TiUtils isIOS8OrGreater]) {
            CGSize mainScreenBoundsSize = [[UIScreen mainScreen] bounds].size;
            isRetinaiPhone6 = (mainScreenBoundsSize.height == 667 || mainScreenBoundsSize.width == 667);
        }
    });
    return isRetinaiPhone6;
}

+(BOOL)isRetinaHDDisplay
{
    static BOOL isRetinaHDDisplay;
    static dispatch_once_t predicate;
    dispatch_once(&predicate, ^{
        isRetinaHDDisplay = ([UIScreen mainScreen].scale == 3.0);
    });
    return isRetinaHDDisplay;
}


+(CGFloat)screenScale {
    static CGFloat scale = 0.0;
    if (scale == 0.0)
    {
        // NOTE: iPad in iPhone compatibility mode will return a scale factor of 2.0
        // when in 2x zoom, which leads to false positives and bugs. This tries to
        // future proof against possible different model names, but in the event of
        // an iPad with a retina display, this will need to be fixed.
        // Credit to Brion on github for the origional fix.
        if(UI_USER_INTERFACE_IDIOM()==UIUserInterfaceIdiomPhone)
        {
            NSRange iPadStringPosition = [[[UIDevice currentDevice] model] rangeOfString:@"iPad"];
            if(iPadStringPosition.location != NSNotFound)
            {
                scale = 1.0;
            }
        }
        scale = [[UIScreen mainScreen] scale];
    }
    return scale;
}

+(BOOL)isRetinaDisplay
{
    static BOOL isRetinaDisplay;
    static dispatch_once_t predicate;
    dispatch_once(&predicate, ^{
        isRetinaDisplay = [TiUtils screenScale] > 1.0;
    });
    return isRetinaDisplay;
}


//+(BOOL)isIOS4_2OrGreater
//{
//    static BOOL isIOS4_2OrGreater;
//    static dispatch_once_t predicate;
//    dispatch_once(&predicate, ^{
//        isIOS4_2OrGreater = [UIView instancesRespondToSelector:@selector(drawRect:forViewPrintFormatter:)];
//    });
//    
//    return isIOS4_2OrGreater;
//}

//+(BOOL)isIOS5OrGreater
//{
//    static BOOL isIOS5OrGreater;
//    static dispatch_once_t predicate;
//    dispatch_once(&predicate, ^{
//        isIOS5OrGreater = [UIAlertView instancesRespondToSelector:@selector(alertViewStyle)];
//    });
//    
//    return isIOS5OrGreater;
//}
//
//+(BOOL)isIOS6OrGreater
//{
//    static BOOL isIOS6OrGreater;
//    static dispatch_once_t predicate;
//    dispatch_once(&predicate, ^{
//        isIOS6OrGreater = [UIViewController instancesRespondToSelector:@selector(shouldAutomaticallyForwardRotationMethods)];
//    });
//    
//    return isIOS6OrGreater;
//}

//+(BOOL)isIOS7OrGreater
//{
//    static BOOL isIOS7OrGreater;
//    static dispatch_once_t predicate;
//    dispatch_once(&predicate, ^{
//        isIOS7OrGreater = [UIViewController instancesRespondToSelector:@selector(childViewControllerForStatusBarStyle)];
//    });
//
//    return isIOS7OrGreater;
//}

+(BOOL)isIOS8OrGreater
{
    static BOOL isIOS8OrGreater;
    static dispatch_once_t predicate;
    dispatch_once(&predicate, ^{
        isIOS8OrGreater = [UIView instancesRespondToSelector:@selector(layoutMarginsDidChange)];
    });
    
    return isIOS8OrGreater;
}

+(BOOL)isIOS9OrGreater
{
    static BOOL isIOS9OrGreater;
    static dispatch_once_t predicate;
    dispatch_once(&predicate, ^{
        isIOS9OrGreater = [UIImage instancesRespondToSelector:@selector(flipsForRightToLeftLayoutDirection)];
    });
    
    return isIOS9OrGreater;
}

+(BOOL)isIPad
{
    static BOOL isIPad;
    static dispatch_once_t predicate;
    dispatch_once(&predicate, ^{
        isIPad = [[UIDevice currentDevice] userInterfaceIdiom] == UIUserInterfaceIdiomPad;
    });
    
    return isIPad;
}

+(BOOL)isIPhone4
{
    static BOOL iphone4;
    static dispatch_once_t predicate;
    dispatch_once(&predicate, ^{
        iphone4 = NO;
        if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPhone)
		{
			struct utsname u;
			uname(&u);
			if (!strcmp(u.machine, "iPhone3,1"))
			{
				iphone4 = YES;
			}
		}
    });
    
	return iphone4;
}

+(NSDate *)dateValue:(id)value def:(NSDate *)def
{
	if ([value isKindOfClass:[NSDate class]])
	{
		return value;
	}
    else {
        NSInteger milliseconds = [self intValue:value def:-1];
        if (milliseconds != -1) {
            return [[[NSDate alloc] initWithTimeIntervalSince1970:milliseconds/1000] autorelease];
        }
    }
	return def;
}


+(NSDate *)dateValue:(id)object
{
	return [self dateValue:object def:nil];
}

+(NSDate *)dateValue:(NSString*)name properties:(NSDictionary*)properties def:(NSDate *)def exists:(BOOL*) exists
{
	if ([properties isKindOfClass:[NSDictionary class]])
	{
		id value = [properties objectForKey:name];
        if (value == [NSNull null])
		{
			if (exists != NULL) *exists = YES;
			return nil;
		}
		if (value != nil)
		{
			if (exists != NULL)
			{
				*exists = YES;
			}
			return [self dateValue:value];
		}
	}
	if (exists != NULL)
	{
		*exists = NO;
	}
	return def;
	
}

+(NSDate *)dateValue:(NSString*)name properties:(NSDictionary*)props def:(NSDate *)def;
{
	return [self dateValue:name properties:props def:def exists:NULL];
}
+(NSDate *)dateValue:(NSString*)name properties:(NSDictionary*)props;
{
	return [self dateValue:name properties:props def:nil exists:NULL];
}

+(NSString *)UTCDateForDate:(NSDate*)data
{
	NSDateFormatter *dateFormatter = [[[NSDateFormatter alloc] init] autorelease];
	NSTimeZone *timeZone = [NSTimeZone timeZoneWithName:@"UTC"];
	[dateFormatter setTimeZone:timeZone];

	NSLocale * USLocale = [[NSLocale alloc] initWithLocaleIdentifier:@"en_US"];
	[dateFormatter setLocale:USLocale];
	[USLocale release];


	//Example UTC full format: 2009-06-15T21:46:28.685+0000
	[dateFormatter setDateFormat:@"yyyy-MM-dd'T'HH:mm:ss'.'SSS+0000"];
	return [dateFormatter stringFromDate:data];
}

+(NSDate *)dateForUTCDate:(NSString*)date
{
	NSDateFormatter *dateFormatter = [[[NSDateFormatter alloc] init] autorelease];
	NSTimeZone* timeZone = [NSTimeZone timeZoneWithName:@"UTC"];
	[dateFormatter setTimeZone:timeZone];
	
	NSLocale* USLocale = [[NSLocale alloc] initWithLocaleIdentifier:@"en_US"];
	[dateFormatter setLocale:USLocale];
	[USLocale release];
	
	[dateFormatter setDateFormat:@"yyyy-MM-dd'T'HH:mm:ss'.'SSS+0000"];
	return [dateFormatter dateFromString:date];
}

+(NSString *)UTCDate
{
	return [TiUtils UTCDateForDate:[NSDate date]];
}

+(NSString*)createUUID
{
	CFUUIDRef resultID = CFUUIDCreate(NULL);
	NSString * resultString = (NSString *) CFUUIDCreateString(NULL, resultID);
	CFRelease(resultID);
	return [resultString autorelease];
}

+(TiFile*)createTempFile:(NSString*)extension
{
	return [TiFile createTempFile:extension];
}

+(NSString *)encodeQueryPart:(NSString *)unencodedString
{
	NSString * result = (NSString *)CFURLCreateStringByAddingPercentEscapes(
															   NULL,
															   (CFStringRef)unencodedString,
															   NULL,
															   (CFStringRef)@"!*'();:@+$,/?%#[]=", 
															   kCFStringEncodingUTF8 );
	[result autorelease];
	return result;
}

+(NSString *)encodeURIParameters:(NSString *)unencodedString
{
	// NOTE: we must encode each individual part for the to successfully work
	
	NSMutableString *result = [[[NSMutableString alloc]init] autorelease];
	
	NSArray *parts = [unencodedString componentsSeparatedByString:@"&"];
	for (int c=0;c<[parts count];c++)
	{
		NSString *part = [parts objectAtIndex:c];
		NSRange range = [part rangeOfString:@"="];
		
		if (range.location != NSNotFound)
		{
			[result appendString:[TiUtils encodeQueryPart:[part substringToIndex:range.location]]];
			[result appendString:@"="];
			[result appendString:[TiUtils encodeQueryPart:[part substringFromIndex:range.location+1]]];
		}
		else 
		{
			[result appendString:[TiUtils encodeQueryPart:part]];
		}
		
		
		if (c + 1 < [parts count])
		{
			[result appendString:@"&"];
		}
	}
	
	return result;
}

+(NSString*)stringValue:(id)value
{
	if(value == nil) {
		return nil;
	}
	
	if ([value isKindOfClass:[NSString class]])
	{
		return (NSString*)value;
	}
	if ([value isKindOfClass:[NSURL class]])
	{
		return [(NSURL *)value absoluteString];
	}
	else if ([value isKindOfClass:[NSNull class]])
	{
		return nil;
	}
	if ([value respondsToSelector:@selector(stringValue)])
	{
		return [value stringValue];
	}
	return [value description];
}

+(NSString*)stringValue:(id)value def:(NSString*)def
{
    NSString* result = [self stringValue:value];
    if (result)
        return result;
    return def;
}

+(BOOL)boolValue:(id)value def:(BOOL)def;
{
	if ([value respondsToSelector:@selector(boolValue)])
	{
		return [value boolValue];
	}
	return def;
}

+(BOOL)boolValue:(id)value
{
	return [self boolValue:value def:NO];
}

+(double)doubleValue:(id)value
{
	return [self doubleValue:value def:0];
}

+(double)doubleValue:(id)value def:(double) def
{
	return [self doubleValue:value def:def valid:NULL];
}

+(double)doubleValue:(id)value def:(double) def valid:(BOOL *) isValid {
	if ([value respondsToSelector:@selector(doubleValue)])
	{
	   if(isValid != NULL) *isValid = YES;
	   return [value doubleValue];
	}
	return def;	
}

+(UIEdgeInsets)contentInsets:(id)value
{
	if ([value isKindOfClass:[NSDictionary class]])
	{
		NSDictionary *dict = (NSDictionary*)value;
		CGFloat t = [TiUtils floatValue:@"top" properties:dict def:0];
		CGFloat l = [TiUtils floatValue:@"left" properties:dict def:0];
		CGFloat b = [TiUtils floatValue:@"bottom" properties:dict def:0];
		CGFloat r = [TiUtils floatValue:@"right" properties:dict def:0];
		return UIEdgeInsetsMake(t, l, b, r);
	}
	return UIEdgeInsetsMake(0,0,0,0);
}

+(CGRect)rectValue:(id)value
{
	if ([value isKindOfClass:[NSDictionary class]])
	{
		NSDictionary *dict = (NSDictionary*)value;
		CGFloat x = [TiUtils floatValue:@"x" properties:dict def:0];
		CGFloat y = [TiUtils floatValue:@"y" properties:dict def:0];
		CGFloat w = [TiUtils floatValue:@"width" properties:dict def:0];
		CGFloat h = [TiUtils floatValue:@"height" properties:dict def:0];
		return CGRectMake(x, y, w, h);
	}
	return CGRectMake(0, 0, 0, 0);
}


+(CGPoint)pointValue:(id)value def:(CGPoint)defaultValue
{
	if ([value isKindOfClass:[TiPoint class]])
	{
		return [value point];
	}
	if ([value isKindOfClass:[NSDictionary class]])
	{
		return CGPointMake([[value objectForKey:@"x"] floatValue],[[value objectForKey:@"y"] floatValue]);
	}
    if ([value isKindOfClass:[NSArray class]] && [value count] >= 2)
    {
        return CGPointMake([[value objectAtIndex:0] floatValue],[[value objectAtIndex:1] floatValue]);
    }
	return defaultValue;
}

+(CGPoint)pointValue:(id)value
{
	return [self pointValue:value def:CGPointZero];
}

+(CGPoint)pointValue:(id)value valid:(BOOL*)isValid
{
	if ([value isKindOfClass:[TiPoint class]]) {
        if (isValid) {
            *isValid = YES;
        }
		return [value point];
    } else if ([value isKindOfClass:[NSDictionary class]]) {
        id xVal = [value objectForKey:@"x"];
        id yVal = [value objectForKey:@"y"];
        if (xVal && yVal) {
            if (![xVal respondsToSelector:@selector(floatValue)] ||
                ![yVal respondsToSelector:@selector(floatValue)])
            {
                if (isValid) {
                    *isValid = NO;
                }
                return CGPointMake(0.0, 0.0);
            }
            
            if (isValid) {
                *isValid = YES;
            }
            return CGPointMake([xVal floatValue], [yVal floatValue]);
        }
    } else if ([value isKindOfClass:[NSArray class]] && [value count] >= 2) {
        id xVal = [value objectAtIndex:0];
        id yVal = [value objectAtIndex:1];
        if (xVal && yVal) {
            if (![xVal respondsToSelector:@selector(floatValue)] ||
                ![yVal respondsToSelector:@selector(floatValue)])
            {
                if (isValid) {
                    *isValid = NO;
                }
                return CGPointMake(0.0, 0.0);
            }
            
            if (isValid) {
                *isValid = YES;
            }
            return CGPointMake([xVal floatValue], [yVal floatValue]);
        }
    }
    if (isValid) {
        *isValid = NO;
    }
	return CGPointMake(0,0);
}

+(CGPoint)pointValue:(id)value bounds:(CGRect)bounds defaultOffset:(CGPoint)defaultOffset;
{
	TiDimension xDimension;
	TiDimension yDimension;
	CGPoint result;

	if ([value isKindOfClass:[TiPoint class]])
	{
		xDimension = [value xDimension];
		yDimension = [value yDimension];
	}
	else if ([value isKindOfClass:[NSDictionary class]])
	{
		xDimension = [self dimensionValue:@"x" properties:value];
		yDimension = [self dimensionValue:@"y" properties:value];
	}
    else if ([value isKindOfClass:[NSArray class]] && [value count] >= 2)
    {
        xDimension = [self dimensionValue:[value objectAtIndex:0]];
        yDimension = [self dimensionValue:[value objectAtIndex:1]];
    }
	else
	{
		xDimension = TiDimensionUndefined;
		yDimension = TiDimensionUndefined;
	}

	if (!TiDimensionDidCalculateValue(xDimension, bounds.size.width, &result.x))
	{
		result.x = defaultOffset.x * bounds.size.width;
	}
	if (!TiDimensionDidCalculateValue(yDimension, bounds.size.height, &result.y))
	{
		result.y = defaultOffset.y * bounds.size.height;
	}

	return CGPointMake(result.x + bounds.origin.x,result.y + bounds.origin.y);
}

+(NSNumber *) numberFromObject:(id) obj {
	if([obj isKindOfClass:[NSNumber class]]) {
		return obj;
	}
	
	NSNumberFormatter *formatter = [[[NSNumberFormatter alloc] init] autorelease];

	return [formatter numberFromString:[self stringValue:obj]];
}

+(CGFloat)floatValue:(id)value def:(CGFloat) def
{
	return [self floatValue:value def:def valid:NULL];
}

+(CGFloat) floatValue:(id)value def:(CGFloat) def valid:(BOOL *) isValid {
	if([value respondsToSelector:@selector(floatValue)]) {
		if(isValid != NULL) *isValid = YES;
		return [value floatValue];
	}
    if (isValid != NULL) {
        *isValid = NO;
    }
	return def;
}

+(CGFloat)floatValue:(id)value
{
	return [self floatValue:value def:NSNotFound];
}

/* Example:
 shadow = {
    offset: {
        width: 10,
        height: 10
    },
    blurRadius: 10,
    color: 'red'
 }
 */
+(NSShadow*)shadowValue:(id)value
{
    if(![value isKindOfClass:[NSDictionary class]]) return nil;
    
    NSShadow *shadow = [[NSShadow alloc] init];

    id offset = [value objectForKey:@"offset"];
    if (offset != nil && [offset isKindOfClass:[NSDictionary class]]) {
        id w = [offset objectForKey:@"width"];
        id h = [offset objectForKey:@"height"];
        [shadow setShadowOffset: CGSizeMake([TiUtils floatValue:w def:0], [TiUtils floatValue:h def:0])];
    }
    id blurRadius = [value objectForKey:@"blurRadius"];
    if (blurRadius != nil) {
        [shadow setShadowBlurRadius:[TiUtils floatValue:blurRadius def:0]];
    }
    id color = [value objectForKey:@"color"];
    if(color != nil) {
        [shadow setShadowColor:[[TiUtils colorValue:color] _color]];
    }
    return [shadow autorelease];
}

+(NSInteger)intValue:(id)value def:(NSInteger)def valid:(BOOL *) isValid {
	if ([value respondsToSelector:@selector(intValue)])
	{	
		if(isValid != NULL) {
			*isValid = YES;			
		}
		return [value intValue];
	}
    if (isValid != NULL) {
        *isValid = NO;
    }
	return def;	
}

+(NSInteger)intValue:(id)value def:(NSInteger)def
{
	return [self intValue:value def:def valid:NULL];
}

+(NSInteger)intValue:(id)value
{
	return [self intValue:value def:0];
}

+(TiColor*)colorValue:(id)value
{
	if ([value isKindOfClass:[TiColor class]])
	{
		return (TiColor*)value;
	}
	if ([value respondsToSelector:@selector(stringValue)])
	{
		value = [value stringValue];
	}
	if ([value isKindOfClass:[NSString class]])
	{
		return [TiColor colorNamed:value]; 
	}
	return nil;
}

+(TiDimension)dimensionValue:(id)value
{
	return TiDimensionFromObject(value);
}

+(TiPoint*)tiPointValue:(id)value
{
	return [TiPoint pointWithObject:value];
}

+(TiPoint*)tiPointValue:(id)value def:(TiPoint*)def
{
    TiPoint* result = [TiPoint pointWithObject:value];
    if (result)
        return result;
    return def;
}


+(Ti2DMatrix*)matrixValue:(id)value
{
	return [Ti2DMatrix matrixWithObject:value];
}

+(Ti2DMatrix*)matrixValue:(id)value def:(Ti2DMatrix*)def
{
    Ti2DMatrix* result = [Ti2DMatrix matrixWithObject:value];
    if (result)
        return result;
    return def;
}

+(TiCap)capValue:(id)value def:(TiCap)def
{
    if (!value) {
        return def;
    }
    TiCap result;
    if ([value isKindOfClass:[NSDictionary class]])
    {
        result.leftCap = TiDimensionFromObject([value objectForKey:@"left"]);
        result.rightCap = TiDimensionFromObject([value objectForKey:@"right"]);
        result.topCap = TiDimensionFromObject([value objectForKey:@"top"]);
        result.bottomCap = TiDimensionFromObject([value objectForKey:@"bottom"]);
    } else {
        TiDimension dim = TiDimensionFromObject(value);
        result.leftCap = dim;
        result.rightCap = dim;
        result.topCap = dim;
        result.bottomCap = dim;
    }
    return result;
}

+(TiCap)capValue:(id)value
{
    TiCap result;
    if ([value isKindOfClass:[NSDictionary class]])
    {
        result.leftCap = TiDimensionFromObject([value objectForKey:@"left"]);
        result.rightCap = TiDimensionFromObject([value objectForKey:@"right"]);
        result.topCap = TiDimensionFromObject([value objectForKey:@"top"]);
        result.bottomCap = TiDimensionFromObject([value objectForKey:@"bottom"]);
    } else {
        TiDimension dim = TiDimensionFromObject(value);
        result.leftCap = dim;
        result.rightCap = dim;
        result.topCap = dim;
        result.bottomCap = dim;
    }
    return result;
}


+(id)valueFromDimension:(TiDimension)dimension
{
	switch (dimension.type)
	{
		case TiDimensionTypeUndefined:
			return [NSNull null];
		case TiDimensionTypeAuto:
			return @"auto";
		case TiDimensionTypeDip:
			return [NSNumber numberWithFloat:dimension.value];
		default: {
			break;
		}
	}
	return nil;
}

+(UIImage*)scaleImage:(UIImage *)image toSize:(CGSize)newSize
{
	if (!CGSizeEqualToSize(newSize, CGSizeZero))
	{
		CGSize imageSize = [image size];
		if (newSize.width==0)
		{
			newSize.width = imageSize.width;
		}
		if (newSize.height==0)
		{
			newSize.height = imageSize.height;
		}
		if (!CGSizeEqualToSize(newSize, imageSize))
		{
			image = [UIImageResize resizedImage:newSize interpolationQuality:kCGInterpolationLow image:image hires:NO];
//            imageSize = [image size];
		}
	}
	return image;
}

+(UIImage*)toImage:(id)object proxy:(TiProxy*)proxy size:(CGSize)imageSize
{
	if ([object isKindOfClass:[TiBlob class]])
	{
		return [self scaleImage:[(TiBlob *)object image] toSize:imageSize];
	}

	if ([object isKindOfClass:[TiFile class]])
	{
		TiFile *file = (TiFile*)object;
		UIImage *image = [UIImage imageWithContentsOfFile:[file path]];
		return [self scaleImage:image toSize:imageSize];
	}

	NSURL * urlAttempt = [self toURL:object proxy:proxy];
	UIImage * image = [[ImageLoader sharedLoader] loadImmediateImage:urlAttempt withSize:imageSize];
	return image;
	//Note: If url is a nonimmediate image, this returns nil.
}

+(UIImage*)toImage:(id)object proxy:(TiProxy*)proxy
{
	if ([object isKindOfClass:[TiBlob class]])
	{
		return [(TiBlob *)object image];
	}

	if ([object isKindOfClass:[TiFile class]])
	{
		TiFile *file = (TiFile*)object;
		UIImage *image = [UIImage imageWithContentsOfFile:[file path]];
		return image;
	}

	NSURL * urlAttempt = [self toURL:object proxy:proxy];
	UIImage * image = [[ImageLoader sharedLoader] loadImmediateImage:urlAttempt];
	return image;
	//Note: If url is a nonimmediate image, this returns nil.
}

+(NSURL*)checkFor2XImage:(NSURL*)url
{
	NSString * path = nil;
	
	if([url isFileURL])
	{
		path = [url path];
	}
	
	if([[url scheme] isEqualToString:@"app"])
	{ //Technically, this will have an extra /, but iOS ignores this.
		path = [url resourceSpecifier];
	}

	NSString *ext = [path pathExtension];

	if(![ext isEqualToString:@"png"] && ![ext isEqualToString:@"jpg"] && ![ext isEqualToString:@"jpeg"])
	{ //It's not an image.
		return url;
	}

	//NOTE; I'm not sure the order here.. the docs don't necessarily 
	//specify the exact order 
	NSFileManager *fm = [NSFileManager defaultManager];
	NSString *partial = [path stringByDeletingPathExtension];

	NSString *os = [TiUtils isIPad] ? @"~ipad" : @"~iphone";

	if ([TiUtils isRetinaHDDisplay]) {
		// first try -736h@3x iphone6 Plus specific
		NSString *testpath = [NSString stringWithFormat:@"%@-736h@3x.%@",partial,ext];
		if ([fm fileExistsAtPath:testpath]) {
			return [NSURL fileURLWithPath:testpath];
		}
		// second try plain @3x
		testpath = [NSString stringWithFormat:@"%@@3x.%@",partial,ext];
		if ([fm fileExistsAtPath:testpath]) {
			return [NSURL fileURLWithPath:testpath];
		}
	}
	if([TiUtils isRetinaDisplay]){
		if ([TiUtils isRetinaiPhone6]) {
			// first try -667h@2x iphone6 specific
			NSString *testpath = [NSString stringWithFormat:@"%@-667h@2x.%@",partial,ext];
			if ([fm fileExistsAtPath:testpath]) {
				return [NSURL fileURLWithPath:testpath];
			}
		} else if ([TiUtils isRetinaFourInch]) {
			// first try -568h@2x iphone5 specific
			NSString *testpath = [NSString stringWithFormat:@"%@-568h@2x.%@",partial,ext];
			if ([fm fileExistsAtPath:testpath]) {
				return [NSURL fileURLWithPath:testpath];
			}
		}
		// first try 2x device specific
		NSString *testpath = [NSString stringWithFormat:@"%@@2x%@.%@",partial,os,ext];
		if ([fm fileExistsAtPath:testpath])
		{
			return [NSURL fileURLWithPath:testpath];
		}
		// second try plain 2x
		testpath = [NSString stringWithFormat:@"%@@2x.%@",partial,ext];
		if ([fm fileExistsAtPath:testpath])
		{
			return [NSURL fileURLWithPath:testpath];
		}
	}
	// third try just device specific normal res
	NSString *testpath = [NSString stringWithFormat:@"%@%@.%@",partial,os,ext];
	if ([fm fileExistsAtPath:testpath])
	{
		return [NSURL fileURLWithPath:testpath];
	}

	return url;
}

const CFStringRef charactersThatNeedEscaping = NULL;
const CFStringRef charactersToNotEscape = CFSTR(":[]@!$' ()*+,;\"<>%{}|\\^~`#");

+(NSURL*)toURL:(NSString *)relativeString relativeToURL:(NSURL *)rootPath
{
/*
Okay, behavior: Bad values are either converted or ejected.
sms:, tel:, mailto: are all done

If the new path is HTTP:// etc, then punt and massage the code.

If the new path starts with / and the base url is app://..., we have to massage the url.


*/
	if((relativeString == nil) || ((void*)relativeString == (void*)[NSNull null]))
	{
		return nil;
	}

	if(![relativeString isKindOfClass:[NSString class]])
	{
		relativeString = [TiUtils stringValue:relativeString];
	}

	if ([relativeString hasPrefix:@"sms:"] || 
		[relativeString hasPrefix:@"tel:"] ||
		[relativeString hasPrefix:@"mailto:"])
	{
		return [NSURL URLWithString:relativeString];
	}

	if ([relativeString hasPrefix:@"file://"])
	{
        return [NSURL fileURLWithPath:[TiFileSystemHelper pathFromComponents:@[relativeString]]];
    }

    NSURL *result = nil;
    
    // don't bother if we don't at least have a path and it's not remote
    //TODO: What is this mess? -BTH
    if ([relativeString hasPrefix:@"http://"] || [relativeString hasPrefix:@"https://"])
    {
        NSRange range = [relativeString rangeOfString:@"/" options:0 range:NSMakeRange(7, [relativeString length]-7)];
        if (range.location!=NSNotFound)
        {
            NSString *firstPortion = [relativeString substringToIndex:range.location];
            NSString *pathPortion = [relativeString substringFromIndex:range.location];
            CFStringRef escapedPath = CFURLCreateStringByAddingPercentEscapes(kCFAllocatorDefault,
                                                                              (CFStringRef)pathPortion, charactersToNotEscape,charactersThatNeedEscaping,
                                                                              kCFStringEncodingUTF8);
            relativeString = [firstPortion stringByAppendingString:(NSString *)escapedPath];
            if(escapedPath != NULL)
            {
                CFRelease(escapedPath);
            }
        }
        result = [NSURL URLWithString:relativeString relativeToURL:rootPath];
    } else {
        //only add percentescape if there are spaces in relativestring
        if ([[relativeString componentsSeparatedByString:@" "] count] -1 == 0) {
            result = [NSURL URLWithString:relativeString relativeToURL:rootPath];
        }
        else {
            result = [NSURL URLWithString:[relativeString stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding] relativeToURL:rootPath];
        }
    }
    //TIMOB-18262
    if (result && ([[result scheme] isEqualToString:@"file"])){
        BOOL isDir = NO;
        BOOL exists = [[NSFileManager defaultManager] fileExistsAtPath:[result path] isDirectory:&isDir];
        
        if (exists && !isDir) {
            return [TiUtils checkFor2XImage:result];
        }
    }

	//TODO: Make this less ugly.
	if ([relativeString hasPrefix:@"/"])
	{
		NSString * rootScheme = [rootPath scheme];
		NSString * resourcePath = [TiHost resourcePath];
		BOOL usesApp = [rootScheme isEqualToString:@"app"];
		if(!usesApp && [rootScheme isEqualToString:@"file"])
		{
			usesApp = [[rootPath path] hasPrefix:resourcePath];
		}
		if(usesApp)
		{
			result = [NSURL fileURLWithPath:[resourcePath stringByAppendingPathComponent:relativeString]];
		}
	}

	
	if (result==nil)
	{
		//encoding problem - fail fast and make sure we re-escape
		NSRange range = [relativeString rangeOfString:@"?"];
		if (range.location != NSNotFound)
		{
			NSString *qs = [TiUtils encodeURIParameters:[relativeString substringFromIndex:range.location+1]];
			NSString *newurl = [NSString stringWithFormat:@"%@?%@",[relativeString substringToIndex:range.location],qs];
			return [TiUtils checkFor2XImage:[NSURL URLWithString:newurl]];
		}
	}
	return [TiUtils checkFor2XImage:result];			  
}

+(NSURL*)toURL:(NSString *)object proxy:(TiProxy*)proxy
{
	return [self toURL:object relativeToURL:[proxy _baseURL]];  
}

+(UIImage *)stretchableImage:(id)object proxy:(TiProxy*)proxy
{
	return [[ImageLoader sharedLoader] loadImmediateStretchableImage:[self toURL:object proxy:proxy]];
}

+(UIImage *)image:(id)object proxy:(TiProxy*)proxy
{
    if ([object isKindOfClass:[TiBlob class]]) {
        return [(TiBlob*)object image];
    }
    else if ([object isKindOfClass:[NSString class]]) {
        id result = [[ImageLoader sharedLoader] loadImmediateImage:[self toURL:object proxy:proxy]];
        if ([result isKindOfClass:[UIImage class]]) return result;
        else if ([result isKindOfClass:[SVGKImage class]]) return [((SVGKImage*)result) UIImage];
    }
    
    return nil;
}

+(int)intValue:(NSString*)name properties:(NSDictionary*)properties def:(int)def exists:(BOOL*) exists
{
	if ([properties isKindOfClass:[NSDictionary class]])
	{
		id value = [properties objectForKey:name];
		if ([value respondsToSelector:@selector(intValue)])
		{
			if (exists != NULL) *exists = YES;
			return [value intValue];
		}
	}
	if (exists != NULL) *exists = NO;
	return def;
}

+(double)doubleValue:(NSString*)name properties:(NSDictionary*)properties def:(double)def exists:(BOOL*) exists
{
	if ([properties isKindOfClass:[NSDictionary class]])
	{
		id value = [properties objectForKey:name];
		if ([value respondsToSelector:@selector(doubleValue)])
		{
			if (exists != NULL) *exists = YES;
			return [value doubleValue];
		}
	}
	if (exists != NULL) *exists = NO;
	return def;
}

+(float)floatValue:(NSString*)name properties:(NSDictionary*)properties def:(float)def exists:(BOOL*) exists
{
	if ([properties isKindOfClass:[NSDictionary class]])
	{
		id value = [properties objectForKey:name];
		if ([value respondsToSelector:@selector(floatValue)])
		{
			if (exists != NULL) *exists = YES;
			return [value floatValue];
		}		
	}
	if (exists != NULL) *exists = NO;
	return def;
}

+(BOOL)boolValue:(NSString*)name properties:(NSDictionary*)properties def:(BOOL)def exists:(BOOL*) exists
{
	if ([properties isKindOfClass:[NSDictionary class]])
	{
		id value = [properties objectForKey:name];
		if ([value respondsToSelector:@selector(boolValue)])
		{
			if (exists != NULL) *exists = YES;
			return [value boolValue];
		}
	}
	if (exists != NULL) *exists = NO;
	return def;
}

+(NSString*)stringValue:(NSString*)name properties:(NSDictionary*)properties def:(NSString*)def exists:(BOOL*) exists
{
	if ([properties isKindOfClass:[NSDictionary class]])
	{
		id value = [properties objectForKey:name];
		if ([value isKindOfClass:[NSString class]])
		{
			if (exists != NULL) *exists = YES;
			return value;
		}
		else if (value == [NSNull null])
		{
			if (exists != NULL) *exists = YES;
			return nil;
		}
		else if ([value respondsToSelector:@selector(stringValue)])
		{
			if (exists != NULL) *exists = YES;
			return [value stringValue];
		}
	}
	if (exists != NULL) *exists = NO;
	return def;
}

+(CGPoint)pointValue:(NSString*)name properties:(NSDictionary*)properties def:(CGPoint)def exists:(BOOL*) exists
{
    if ([properties isKindOfClass:[NSDictionary class]])
    {
        id value = [properties objectForKey:name];
        if (value)
        {
            if (exists != NULL) *exists = YES;
            return [TiUtils pointValue:value def:def];
        }
    }
    
    if (exists != NULL) *exists = NO;
    return def;
}

+(TiColor*)colorValue:(NSString*)name properties:(NSDictionary*)properties def:(TiColor*)def exists:(BOOL*) exists
{
	TiColor * result = nil;
	if ([properties isKindOfClass:[NSDictionary class]])
	{
		id value = [properties objectForKey:name];
		if (value == [NSNull null])
		{
			if (exists != NULL) *exists = YES;
			return nil;
		}
		if ([value respondsToSelector:@selector(stringValue)])
		{
			value = [value stringValue];
		}
		if ([value isKindOfClass:[NSString class]])
		{
			// need to retain here since we autorelease below and since colorName also autoreleases
			result = [[TiColor colorNamed:value] retain]; 
		}
	}
	if (result != nil)
	{
		if (exists != NULL) *exists = YES;
		return [result autorelease];
	}
	
	if (exists != NULL) *exists = NO;
	return def;
}

+(TiDimension)dimensionValue:(NSString*)name properties:(NSDictionary*)properties def:(TiDimension)def exists:(BOOL*) exists
{
	if ([properties isKindOfClass:[NSDictionary class]])
	{
		id value = [properties objectForKey:name];
		if (value != nil)
		{
			if (exists != NULL)
			{
				*exists = YES;
			}
			return [self dimensionValue:value];
		}
	}
	if (exists != NULL)
	{
		*exists = NO;
	}
	return def;
	
}


+(TiPoint*)tiPointValue:(NSString*)name properties:(NSDictionary*)properties def:(TiPoint*)def exists:(BOOL*) exists
{
	if ([properties isKindOfClass:[NSDictionary class]])
	{
		id value = [properties objectForKey:name];
        if (value == [NSNull null])
		{
			if (exists != NULL) *exists = YES;
			return nil;
		}
		if (value != nil)
		{
			if (exists != NULL)
			{
				*exists = YES;
			}
			return [self tiPointValue:value];
		}
	}
	if (exists != NULL)
	{
		*exists = NO;
	}
	return def;
	
}


+(Ti2DMatrix*)matrixValue:(NSString*)name properties:(NSDictionary*)properties def:(Ti2DMatrix*)def exists:(BOOL*) exists
{
	if ([properties isKindOfClass:[NSDictionary class]])
	{
		id value = [properties objectForKey:name];
        if (value == [NSNull null])
		{
			if (exists != NULL) *exists = YES;
			return nil;
		}
		if (value != nil)
		{
			if (exists != NULL)
			{
				*exists = YES;
			}
			return [self matrixValue:value];
		}
	}
	if (exists != NULL)
	{
		*exists = NO;
	}
	return def;
	
}

+(TiCap)capValue:(NSString*)name properties:(NSDictionary*)properties def:(TiCap)def exists:(BOOL*) exists
{
    if ([properties isKindOfClass:[NSDictionary class]])
    {
        id value = [properties objectForKey:name];
        if (value == [NSNull null])
        {
            if (exists != NULL) *exists = YES;
            return TiCapUndefined;
        }
        if (value != nil)
        {
            if (exists != NULL)
            {
                *exists = YES;
            }
            return [self capValue:value];
        }
    }
    if (exists != NULL)
    {
        *exists = NO;
    }
    return def;
    
}


+(int)intValue:(NSString*)name properties:(NSDictionary*)props def:(int)def;
{
	return [self intValue:name properties:props def:def exists:NULL];
}

+(double)doubleValue:(NSString*)name properties:(NSDictionary*)props def:(double)def;
{
	return [self doubleValue:name properties:props def:def exists:NULL];
}

+(float)floatValue:(NSString*)name properties:(NSDictionary*)props def:(float)def;
{
	return [self floatValue:name properties:props def:def exists:NULL];
}

+(BOOL)boolValue:(NSString*)name properties:(NSDictionary*)props def:(BOOL)def;
{
	return [self boolValue:name properties:props def:def exists:NULL];
}

+(NSString*)stringValue:(NSString*)name properties:(NSDictionary*)properties def:(NSString*)def;
{
	return [self stringValue:name properties:properties def:def exists:NULL];
}

+(CGPoint)pointValue:(NSString*)name properties:(NSDictionary*)properties def:(CGPoint)def;
{
	return [self pointValue:name properties:properties def:def exists:NULL];
}

+(TiColor*)colorValue:(NSString*)name properties:(NSDictionary*)properties def:(TiColor*)def;
{
	return [self colorValue:name properties:properties def:def exists:NULL];
}

+(TiDimension)dimensionValue:(NSString*)name properties:(NSDictionary*)properties def:(TiDimension)def
{
	return [self dimensionValue:name properties:properties def:def exists:NULL];
}

+(TiPoint*)tiPointValue:(NSString*)name properties:(NSDictionary*)properties def:(TiPoint*)def
{
	return [self tiPointValue:name properties:properties def:def exists:NULL];
}

+(TiCap)capValue:(NSString*)name properties:(NSDictionary*)properties def:(TiCap)def
{
    return [self capValue:name properties:properties def:def exists:NULL];
}

+(Ti2DMatrix*)matrixValue:(NSString*)name properties:(NSDictionary*)properties def:(Ti2DMatrix*)def
{
    return [self matrixValue:name properties:properties def:def exists:NULL];
}

+(int)intValue:(NSString*)name properties:(NSDictionary*)props;
{
	return [self intValue:name properties:props def:0 exists:NULL];
}

+(double)doubleValue:(NSString*)name properties:(NSDictionary*)props;
{
	return [self doubleValue:name properties:props def:0.0 exists:NULL];
}

+(float)floatValue:(NSString*)name properties:(NSDictionary*)props;
{
	return [self floatValue:name properties:props def:0.0 exists:NULL];
}

+(BOOL)boolValue:(NSString*)name properties:(NSDictionary*)props;
{
	return [self boolValue:name properties:props def:NO exists:NULL];
}

+(NSString*)stringValue:(NSString*)name properties:(NSDictionary*)properties;
{
	return [self stringValue:name properties:properties def:nil exists:NULL];
}

+(CGPoint)pointValue:(NSString*)name properties:(NSDictionary*)properties;
{
	return [self pointValue:name properties:properties def:CGPointZero exists:NULL];
}

+(TiColor*)colorValue:(NSString*)name properties:(NSDictionary*)properties;
{
	return [self colorValue:name properties:properties def:nil exists:NULL];
}

+(TiDimension)dimensionValue:(NSString*)name properties:(NSDictionary*)properties
{
	return [self dimensionValue:name properties:properties def:TiDimensionUndefined exists:NULL];
}

+(TiPoint*)tiPointValue:(NSString*)name properties:(NSDictionary*)properties
{
	return [self tiPointValue:name properties:properties def:nil exists:NULL];
}


+(Ti2DMatrix*)matrixValue:(NSString*)name properties:(NSDictionary*)properties
{
	return [self matrixValue:name properties:properties def:nil exists:NULL];
}

+(TiCap)capValue:(NSString*)name properties:(NSDictionary*)properties
{
    return [self capValue:name properties:properties def:TiCapUndefined exists:NULL];
}


+(NSMutableDictionary*)dictionaryFromPoint:(CGPoint)localPoint inView:(UIView*)view
{
    CGPoint globalPoint = [view convertPoint:localPoint toView:nil];
    NSString* xProp = @"x";
    NSString* yProp = @"y";
    float xFactor = 1;
    float yFactor = 1;
    
    UIInterfaceOrientation o = (UIInterfaceOrientation)[[UIApplication sharedApplication] statusBarOrientation];
    
    switch (o) {
        case UIInterfaceOrientationPortraitUpsideDown:
            
            xFactor = -1;
            yFactor = -1;
            break;
            
        case UIInterfaceOrientationLandscapeLeft:
            xProp = @"y";
            yProp = @"x";
            yFactor = -1;
            break;
            
        case UIInterfaceOrientationLandscapeRight:
            xProp = @"y";
            yProp = @"x";
            xFactor = -1;
            break;
        default:
            break;
    }
    
    return [NSMutableDictionary dictionaryWithObjectsAndKeys:
            [NSNumber numberWithDouble:xFactor*localPoint.x],xProp,
            [NSNumber numberWithDouble:yFactor*localPoint.y],yProp,
            [NSDictionary dictionaryWithObjectsAndKeys:
             [NSNumber numberWithDouble:xFactor*globalPoint.x],xProp,
             [NSNumber numberWithDouble:yFactor*globalPoint.y],yProp,
             nil], @"globalPoint",
            nil];
}

+(NSMutableDictionary*)dictionaryFromTouchableEvent:(id)touch inView:(UIView*)view
{
    NSMutableDictionary* result = [NSMutableDictionary dictionary] ;
    if (touch == nil) {
        return result;
    }
    CGPoint localPoint = [touch locationInView:view];
    CGPoint globalPoint = [touch locationInView:nil];
    NSString* xProp = @"x";
    NSString* yProp = @"y";
    float xFactor = 1;
    float yFactor = 1;
    
    if (IS_OF_CLASS(view, TiUIView)) {
        [result setObject:[(TiUIView*)view proxy] forKey:@"source"];
    } else if ([view respondsToSelector:@selector(touchDelegate)]) {
        id touchDelegate = [(id)view touchDelegate];
        if (IS_OF_CLASS(touchDelegate, TiUIView)) {
            [result setObject:[(TiUIView*)touchDelegate proxy] forKey:@"source"];
        }
    }
    
 	UIInterfaceOrientation o = (UIInterfaceOrientation)[[UIApplication sharedApplication] statusBarOrientation];
    
    switch (o) {
        case UIInterfaceOrientationPortraitUpsideDown:
            
            xFactor = -1;
            yFactor = -1;
            break;
            
        case UIInterfaceOrientationLandscapeLeft:
            xProp = @"y";
            yProp = @"x";
            yFactor = -1;
            break;
            
        case UIInterfaceOrientationLandscapeRight:
            xProp = @"y";
            yProp = @"x";
            xFactor = -1;
            break;
        default:
            break;
    }
    [result setObject:@{
                       xProp: [NSNumber numberWithDouble:xFactor*globalPoint.x],
                       yProp: [NSNumber numberWithDouble:yFactor*globalPoint.y]
                       } forKey:@"globalPoint"];
    [result setObject:[NSNumber numberWithDouble:xFactor*localPoint.x] forKey:xProp];
    [result setObject:[NSNumber numberWithDouble:yFactor*localPoint.y] forKey:yProp];
    return result;
}

+(NSString*) stateStringFromGesture:(UIGestureRecognizer *)recognizer
{
    NSString* swipeString;
    switch ([recognizer state]) {
        case UIGestureRecognizerStateBegan:
            return @"start";
            break;
        case UIGestureRecognizerStateEnded:
            return @"end";
            break;
        case UIGestureRecognizerStateChanged:
            return @"move";
            break;
        default:
        case UIGestureRecognizerStateCancelled:
            return @"cancel";
            break;
    }
}

+(NSString*) swipeStringFromGesture:(UISwipeGestureRecognizer *)recognizer
{
    NSString* swipeString;
    switch ([recognizer direction]) {
        case UISwipeGestureRecognizerDirectionUp:
            swipeString = @"up";
            break;
        case UISwipeGestureRecognizerDirectionDown:
            swipeString = @"down";
            break;
        case UISwipeGestureRecognizerDirectionLeft:
            swipeString = @"left";
            break;
        case UISwipeGestureRecognizerDirectionRight:
            swipeString = @"right";
            break;
        default:
            swipeString = @"unknown";
            break;
    }
    return swipeString;
}

+(NSMutableDictionary*)dictionaryFromTouch:(UITouch*)touch inView:(UIView*)view
{
    return [self dictionaryFromTouchableEvent:touch inView:view];
}

+(NSMutableDictionary*)dictionaryFromGesture:(UIGestureRecognizer*)recognizer inView:(UIView*)view
{
    NSMutableDictionary* event = [self dictionaryFromTouchableEvent:recognizer inView:view];
    [event setValue:[TiUtils stateStringFromGesture:((UISwipeGestureRecognizer*)recognizer)] forKey:@"state"];
    if (IS_OF_CLASS(recognizer, UIPinchGestureRecognizer)) {
        [event setValue:NUMDOUBLE(((UIPinchGestureRecognizer*)recognizer).scale) forKey:@"scale"];
        [event setValue:NUMDOUBLE(((UIPinchGestureRecognizer*)recognizer).velocity) forKey:@"velocity"];
    } else if(IS_OF_CLASS(recognizer, UISwipeGestureRecognizer)) {
        [event setValue:[TiUtils swipeStringFromGesture:((UISwipeGestureRecognizer*)recognizer)] forKey:@"direction"];
    } else if(IS_OF_CLASS(recognizer, UIPanGestureRecognizer)) {
        CGPoint translation = [((UIPanGestureRecognizer*)recognizer) translationInView:view];
        [event setValue:[TiUtils pointToDictionary:[((UIPanGestureRecognizer*)recognizer) translationInView:view]] forKey:@"translation"];
        [event setValue:[TiUtils pointToDictionary:[((UIPanGestureRecognizer*)recognizer) velocityInView:view]] forKey:@"velocity"];
    } else if(IS_OF_CLASS(recognizer, UIRotationGestureRecognizer)) {
        [event setValue:NUMDOUBLE(((UIRotationGestureRecognizer*)recognizer).rotation * 180 / M_PI) forKey:@"angle"];
        [event setValue:NUMDOUBLE(((UIRotationGestureRecognizer*)recognizer).velocity) forKey:@"velocity"];

    }
    return event;
}

+(NSDictionary*)pointToDictionary:(CGPoint)point
{
    return [NSDictionary dictionaryWithObjectsAndKeys:
            [NSNumber numberWithDouble:point.x],@"x",
            [NSNumber numberWithDouble:point.y],@"y",
            nil];
}

+(NSDictionary*)rectToDictionary:(CGRect)rect
{
    return [NSDictionary dictionaryWithObjectsAndKeys:
            [NSNumber numberWithDouble:rect.origin.x],@"x",
            [NSNumber numberWithDouble:rect.origin.y],@"y",
            [NSNumber numberWithDouble:rect.size.width],@"width",
            [NSNumber numberWithDouble:rect.size.height],@"height",
            nil];
}

+(NSDictionary*)sizeToDictionary:(CGSize)size
{
    return [NSDictionary dictionaryWithObjectsAndKeys:
            [NSNumber numberWithDouble:size.width],@"width",
            [NSNumber numberWithDouble:size.height],@"height",
            nil];
}

+(CGRect)contentFrame:(BOOL)window
{
	double height = 0;
	if (window && ![[UIApplication sharedApplication] isStatusBarHidden])
	{
		CGRect statusFrame = [[UIApplication sharedApplication] statusBarFrame];
		height = statusFrame.size.height;
	}
	
	CGRect f = [[UIScreen mainScreen] applicationFrame];
	return CGRectMake(f.origin.x, height, f.size.width, f.size.height);
}


+(CGRect)appFrame
{
	CGRect result = [[UIScreen mainScreen] bounds];
    if ([TiUtils isIOS8OrGreater]) {
        return result;
    }
	switch ([[UIApplication sharedApplication] statusBarOrientation])
	{
		case UIInterfaceOrientationLandscapeLeft:
		case UIInterfaceOrientationLandscapeRight:
		{
			CGFloat leftMargin = result.origin.y;
			CGFloat topMargin = result.origin.x;
			CGFloat newHeight = result.size.width;
			CGFloat newWidth = result.size.height;
			result = CGRectMake(leftMargin, topMargin, newWidth, newHeight);
			break;
		}
		default: {
			break;
		}
	}
	return result;
}


+(CGFloat)sizeValue:(id)value
{
	if ([value isKindOfClass:[NSString class]])
	{
		NSString *s = [(NSString*) value stringByReplacingOccurrencesOfString:@"px" withString:@""];
		return [[s stringByReplacingOccurrencesOfString:@" " withString:@""] floatValue];
	}
	return [value floatValue];
}

+(WebFont*)fontValue:(id)value def:(WebFont *)def
{
	if ([value isKindOfClass:[NSDictionary class]])
	{
		WebFont *font = [[WebFont alloc] init];
		[font updateWithDict:value inherits:nil];
		return [font autorelease];
	}
	if ([value isKindOfClass:[NSString class]])
	{
		WebFont *font = [[WebFont alloc] init];
		font.family = value;
		font.size = 14;
		return [font autorelease];
	}
	return def;
}


+(WebFont*)fontValue:(id)value
{
	WebFont * result = [self fontValue:value def:nil];
	if (result == nil) {
		result = [WebFont defaultFont];
	}
	return result;
}

+(UIEdgeInsets)insetValue:(id)value
{
    UIEdgeInsets inset = UIEdgeInsetsZero;
    if ([value isKindOfClass:[NSDictionary class]])
	{
        NSDictionary* paddingDict = (NSDictionary*)value;
        if ([paddingDict objectForKey:@"left"]) {
            inset.left = [TiUtils floatValue:[paddingDict objectForKey:@"left"]];
        }
        if ([paddingDict objectForKey:@"right"]) {
            inset.right = [TiUtils floatValue:[paddingDict objectForKey:@"right"]];
        }
        if ([paddingDict objectForKey:@"top"]) {
            inset.top = [TiUtils floatValue:[paddingDict objectForKey:@"top"]];
        }
        if ([paddingDict objectForKey:@"bottom"]) {
            inset.bottom = [TiUtils floatValue:[paddingDict objectForKey:@"bottom"]];
        }
    } else if (IS_OF_CLASS(value, NSArray) && [value count] == 4) {
        NSArray* array = (NSArray*)value;
        inset = UIEdgeInsetsMake([TiUtils floatValue:[array objectAtIndex:0]],
                                 [TiUtils floatValue:[array objectAtIndex:1]],
                                 [TiUtils floatValue:[array objectAtIndex:2]],
                                 [TiUtils floatValue:[array objectAtIndex:3]]);
    } else if (IS_OF_CLASS(value, NSNumber)) {
        CGFloat padding = [value floatValue];
        inset = UIEdgeInsetsMake(padding, padding, padding, padding);
    }

    return inset;
}

+(TiScriptError*) scriptErrorValue:(id)value;
{
	if ((value == nil) || (value == [NSNull null])){
		return nil;
	}
	if ([value isKindOfClass:[TiScriptError class]]){
		return value;
	}
	if ([value isKindOfClass:[NSDictionary class]]) {
		return [[[TiScriptError alloc] initWithDictionary:value] autorelease];
	}
	return [[[TiScriptError alloc] initWithMessage:[value description] sourceURL:nil lineNo:0] autorelease];
}

+(NSTextAlignment)textAlignmentValue:(id)alignment
{
	NSTextAlignment align = NSTextAlignmentLeft;

	if ([alignment isKindOfClass:[NSString class]])
	{
		if ([alignment isEqualToString:@"left"])
		{
			align = NSTextAlignmentLeft;
		}
		else if ([alignment isEqualToString:@"middle"] || [alignment isEqualToString:@"center"])
		{
			align = NSTextAlignmentCenter;
		}
		else if ([alignment isEqualToString:@"right"])
		{
			align = NSTextAlignmentRight;
		}
	}
	else if ([alignment isKindOfClass:[NSNumber class]])
	{
		align = [alignment intValue];
	}
	return align;
}

+(UIControlContentVerticalAlignment)contentVerticalAlignmentValue:(id)alignment
{
	UIControlContentVerticalAlignment align = UIControlContentVerticalAlignmentCenter;

	if ([alignment isKindOfClass:[NSString class]])
	{
		if ([alignment isEqualToString:@"top"])
		{
			align = UIControlContentVerticalAlignmentTop;
		}
		else if ([alignment isEqualToString:@"middle"] || [alignment isEqualToString:@"center"])
		{
			align = UIControlContentVerticalAlignmentCenter;
		}
		else if ([alignment isEqualToString:@"bottom"])
		{
			align = UIControlContentVerticalAlignmentBottom;
		}
	}
	else if ([alignment isKindOfClass:[NSNumber class]])
	{
		align = [alignment intValue];
		if (align < UIControlContentVerticalAlignmentCenter || align > UIControlContentVerticalAlignmentBottom)
			align = UIControlContentVerticalAlignmentCenter;
	}
	return align;
}

+(UIControlContentHorizontalAlignment)contentHorizontalAlignmentValue:(id)alignment
{
	UIControlContentHorizontalAlignment align = UIControlContentHorizontalAlignmentCenter;
    
	if ([alignment isKindOfClass:[NSString class]])
	{
		if ([alignment isEqualToString:@"left"])
		{
			align = UIControlContentHorizontalAlignmentLeft;
		}
		else if ([alignment isEqualToString:@"center"])
		{
			align = UIControlContentHorizontalAlignmentCenter;
		}
		else if ([alignment isEqualToString:@"right"])
		{
			align = UIControlContentHorizontalAlignmentRight;
		}
	}
	else if ([alignment isKindOfClass:[NSNumber class]])
	{
		align = [alignment intValue];
		if (align < UIControlContentHorizontalAlignmentCenter || align > UIControlContentHorizontalAlignmentRight)
			align = UIControlContentHorizontalAlignmentCenter;
	}
	return align;
}

+(UIControlContentHorizontalAlignment)contentHorizontalAlignmentValueFromTextAlignment:(id)alignment
{
	UIControlContentHorizontalAlignment align = UIControlContentHorizontalAlignmentCenter;
    
	if ([alignment isKindOfClass:[NSString class]])
	{
		return [TiUtils contentHorizontalAlignmentValue:alignment];
	}
	else if ([alignment isKindOfClass:[NSNumber class]])
	{
		align = [alignment intValue];
		switch (align) {
            case NSTextAlignmentLeft:
                align = UIControlContentHorizontalAlignmentLeft;
                break;
            case NSTextAlignmentRight:
                align = UIControlContentHorizontalAlignmentRight;
                break;
            default:
                align = UIControlContentHorizontalAlignmentCenter;
                break;
        }
	}
	return align;
}

#define RETURN_IF_ORIENTATION_STRING(str,orientation) \
if ([str isEqualToString:@#orientation]) return (UIDeviceOrientation)orientation;

+(UIDeviceOrientation)orientationValue:(id)value def:(UIDeviceOrientation)def
{
	if ([value isKindOfClass:[NSString class]])
	{
		if ([value isEqualToString:@"portrait"])
		{
			return UIDeviceOrientationPortrait;
		}
		if ([value isEqualToString:@"landscape"])
		{
			return (UIDeviceOrientation)UIInterfaceOrientationLandscapeRight;
		}
		
		RETURN_IF_ORIENTATION_STRING(value,UIInterfaceOrientationPortrait)
		RETURN_IF_ORIENTATION_STRING(value,UIInterfaceOrientationPortraitUpsideDown)
		RETURN_IF_ORIENTATION_STRING(value,UIInterfaceOrientationLandscapeLeft)
		RETURN_IF_ORIENTATION_STRING(value,UIInterfaceOrientationLandscapeRight)
	}

	if ([value respondsToSelector:@selector(intValue)])
	{
		return [value intValue];
	}
	return def;
}

+(BOOL)isOrientationPortait
{
	return UIInterfaceOrientationIsPortrait([self orientation]);
}

+(BOOL)isOrientationLandscape
{
	return UIInterfaceOrientationIsLandscape([self orientation]);
}

+(UIInterfaceOrientation)orientation 
{
	UIDeviceOrientation orient = [UIDevice currentDevice].orientation;
//	TODO: A previous bug was DeviceOrientationUnknown == 0, which is always true. Uncomment this when pushing.
	if (UIDeviceOrientationUnknown == orient) 
	{
		return (UIInterfaceOrientation)UIDeviceOrientationPortrait;
	} 
	else 
	{
		return (UIInterfaceOrientation)orient;
	}
}

+(CGRect)screenRect
{
	return [UIScreen mainScreen].bounds;
}

//TODO: rework these to be more accurate and multi-device

+(CGRect)navBarRect
{
	CGRect rect = [self screenRect];
	rect.size.height = TI_NAVBAR_HEIGHT;
	return rect;
}

+(CGSize)navBarTitleViewSize
{
	CGRect rect = [self screenRect];
	return CGSizeMake(rect.size.width-TI_NAVBAR_BUTTON_WIDTH, TI_NAVBAR_HEIGHT);
}

+(CGRect)navBarTitleViewRect
{
	CGRect rect = [self screenRect];
	rect.size.height = TI_NAVBAR_HEIGHT;
	rect.size.width-=TI_NAVBAR_BUTTON_WIDTH; // offset for padding on both sides
	return rect;
}

+(CGPoint)centerSize:(CGSize)smallerSize inRect:(CGRect)largerRect
{
	return CGPointMake(
		largerRect.origin.x + (largerRect.size.width - smallerSize.width)/2,
		largerRect.origin.y + (largerRect.size.height - smallerSize.height)/2);
}

+(CGRect)centerRect:(CGRect)smallerRect inRect:(CGRect)largerRect
{
	smallerRect.origin = [self centerSize:smallerRect.size inRect:largerRect];

	return smallerRect;
}

#define USEFRAME	0

+(void)setView:(UIView *)view positionRect:(CGRect)frameRect
{
#if	USEFRAME
	[view setFrame:frameRect];
	return;
#endif
	
	CGPoint anchorPoint = [[view layer] anchorPoint];
	CGPoint newCenter;
	newCenter.x = frameRect.origin.x + (anchorPoint.x * frameRect.size.width);
	newCenter.y = frameRect.origin.y + (anchorPoint.y * frameRect.size.height);
	CGRect newBounds = CGRectMake(0, 0, frameRect.size.width, frameRect.size.height);

	[view setBounds:newBounds];
	[view setCenter:newCenter];
}

+(CGRect)viewPositionRect:(UIView *)view
{
#if	USEFRAME
	return [view frame];
#endif

	if(view == nil)
	{
		return CGRectZero;
	}
	
	CGPoint anchorPoint = [[view layer] anchorPoint];
	CGRect bounds = [view bounds];
	CGPoint center = [view center];
	
	return CGRectMake(center.x - (anchorPoint.x * bounds.size.width),
			center.y - (anchorPoint.y * bounds.size.height),
			bounds.size.width, bounds.size.height);
}

+(NSData *)loadAppResource:(NSURL*)url
{
	BOOL app = [[url scheme] hasPrefix:@"app"];
	if ([url isFileURL] || app)
	{
		BOOL leadingSlashRemoved = NO;
        NSString *urlstring = [[url standardizedURL] path];
        NSString *resourceurl = [[TiFileSystemHelper resourcesDirectory] stringByStandardizingPath];
		NSRange range = [urlstring rangeOfString:resourceurl];
		NSString *appurlstr = urlstring;
		if (range.location!=NSNotFound)
		{
			appurlstr = [urlstring substringFromIndex:range.location + range.length];
		}
		if ([appurlstr hasPrefix:@"/"])
		{
#ifndef __clang_analyzer__
			leadingSlashRemoved = YES;
#endif
			appurlstr = [appurlstr substringFromIndex:1];
		}
#if TARGET_IPHONE_SIMULATOR
		if (app==YES && leadingSlashRemoved)
		{
			// on simulator we want to keep slash since it's coming from file
			appurlstr = [@"/" stringByAppendingString:appurlstr];
		}
		if (TI_APPLICATION_RESOURCE_DIR!=nil && [TI_APPLICATION_RESOURCE_DIR isEqualToString:@""]==NO)
		{
			if ([appurlstr hasPrefix:TI_APPLICATION_RESOURCE_DIR])
			{
				if ([[NSFileManager defaultManager] fileExistsAtPath:appurlstr])
				{
					return [NSData dataWithContentsOfFile:appurlstr];
				}
			}
			// this path is only taken during a simulator build
			// in this path, we will attempt to load resources directly from the
			// app's Resources directory to speed up round-trips
			NSString *filepath = [TI_APPLICATION_RESOURCE_DIR stringByAppendingPathComponent:appurlstr];
			if ([[NSFileManager defaultManager] fileExistsAtPath:filepath])
			{
				return [NSData dataWithContentsOfFile:filepath];
			}
		}
#endif
		static id AppRouter;
		if (AppRouter==nil)
		{
			AppRouter = NSClassFromString(@"ApplicationRouting");
		}
		if (AppRouter!=nil)
		{
			appurlstr = [appurlstr stringByReplacingOccurrencesOfString:@"." withString:@"_"];
			if ([appurlstr characterAtIndex:0]=='/')
			{
				appurlstr = [appurlstr substringFromIndex:1];
			}
			DebugLog(@"[DEBUG] Loading: %@, Resource: %@",urlstring,appurlstr);
			return [AppRouter performSelector:@selector(resolveAppAsset:) withObject:appurlstr];
		}
	}
	return nil;
}


+(NSArray *)getDirectoryListing:(NSString*)path
{
    NSURL *url = [NSURL fileURLWithPath:path];
    NSString *urlstring = [[url standardizedURL] path];
    NSString *resourceurl = [[TiFileSystemHelper resourcesDirectory] stringByStandardizingPath];
    NSRange range = [urlstring rangeOfString:resourceurl];
    NSString *appurlstr = urlstring;
    if (range.location!=NSNotFound)
    {
        appurlstr = [urlstring substringFromIndex:range.location + range.length];
    }
    if ([appurlstr hasPrefix:@"/"])
    {
        appurlstr = [appurlstr substringFromIndex:1];
    }
    static id AppRouter;
    if (AppRouter==nil)
    {
        AppRouter = NSClassFromString(@"ApplicationRouting");
    }
    if (AppRouter!=nil)
    {
        DebugLog(@"[DEBUG] getDirectoryListing: %@, Resource: %@",urlstring,appurlstr);
        NSArray* result = [AppRouter performSelector:@selector(getDirectoryListing:) withObject:appurlstr];
        result = [result sortedArrayUsingSelector:@selector(localizedCaseInsensitiveCompare:)];
        return result;
    }
}

+(BOOL)barTranslucencyForColor:(TiColor *)color
{
	return [color _color]==[UIColor clearColor];
}

+(UIColor *)barColorForColor:(TiColor *)color
{
	UIColor * result = [color _color];
	// TODO: Return nil for the appropriate colors once Apple fixes how the 'cancel' button
	// is displayed on nil-color bars.
//	if ((result == [UIColor clearColor]))
//	{
//		return nil;
//	}
	return result;
}

+(UIBarStyle)barStyleForColor:(TiColor *)color
{
	UIColor * result = [color _color];
	// TODO: Return UIBarStyleBlack for the appropriate colors once Apple fixes how the 'cancel' button
	// is displayed on nil-color bars.
	if ((result == [UIColor clearColor]))
	{
		return UIBarStyleBlack;
	}
	return UIBarStyleDefault;
}

+ (UIImage *)imageFromColor:(UIColor *)color {
    CGRect rect = CGRectMake(0, 0, 1, 1);
    UIGraphicsBeginImageContextWithOptions( rect.size, NO, [UIScreen mainScreen].scale );
    CGContextRef context = UIGraphicsGetCurrentContext();
    CGContextSetFillColorWithColor(context, [color CGColor]);
    CGContextFillRect(context, rect);
    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return image;
}

+(NSUInteger)extendedEdgesFromProp:(id)prop
{
    if (![prop isKindOfClass:[NSArray class]]) {
        return 0;
    }
    
    NSUInteger result = 0;
    for (id mode in prop) {
        NSInteger value = [TiUtils intValue:mode def:0];
        switch (value) {
            case 0:
            case 1:
            case 2:
            case 4:
            case 8:
            case 15:
                result = result | value;
                break;
            default:
                DebugLog(@"Invalid value passed for extendEdges %d",value);
                break;
        }
    }
    return result;
}

+(void)setVolume:(float)volume onObject:(id)theObject
{
    //Must be called on the main thread
    if ([NSThread isMainThread]) {
        if ([theObject respondsToSelector:@selector(setVolume:)]) {
            [(id<VolumeSupport>)theObject setVolume:volume];
        } else {
            DebugLog(@"[WARN] The Object %@ does not respond to method -(void)setVolume:(float)volume",[theObject description]);
        }
    }
}

+(float)volumeFromObject:(id)theObject default:(float)def
{
    //Must be called on the main thread
    float returnValue = def;
    if ([NSThread isMainThread]) {
        if ([theObject respondsToSelector:@selector(volume)]) {
            returnValue = [(id<VolumeSupport>)theObject volume];
        } else {
            DebugLog(@"[WARN] The Object %@ does not respond to method -(float)volume",[theObject description]);
        }
    }
    return returnValue;
}

+(void)configureController:(UIViewController*)controller withObject:(id)object
{
    id edgesValue = nil;
    id includeOpaque = nil;
    id autoAdjust = nil;
    if ([object isKindOfClass:[TiProxy class]]) {
        edgesValue = [(TiProxy*)object valueForUndefinedKey:@"extendEdges"];
        includeOpaque = [(TiProxy*)object valueForUndefinedKey:@"includeOpaqueBars"];
        autoAdjust = [(TiProxy*)object valueForUndefinedKey:@"autoAdjustScrollViewInsets"];
    } else if ([object isKindOfClass:[NSDictionary class]]){
        edgesValue = [(NSDictionary*)object objectForKey:@"extendEdges"];
        includeOpaque = [(NSDictionary*)object objectForKey:@"includeOpaqueBars"];
        autoAdjust = [(NSDictionary*)object objectForKey:@"autoAdjustScrollViewInsets"];
    } 
    
    [controller setEdgesForExtendedLayout:[self extendedEdgesFromProp:edgesValue]];
    [controller setExtendedLayoutIncludesOpaqueBars:[self boolValue:includeOpaque def:NO]];
    [controller setAutomaticallyAdjustsScrollViewInsets:[self boolValue:autoAdjust def:NO]];
}


+(CGRect)frameForController:(UIViewController*)theController
{
    CGRect rect = [[UIScreen mainScreen] applicationFrame];
    NSUInteger edges = [theController edgesForExtendedLayout];
    //Check if I cover status bar
    if ( ((edges & UIRectEdgeTop) != 0) ){
        rect = [[UIScreen mainScreen] bounds];
    }
    
    return rect;
}

+(void)applyColor:(TiColor *)color toNavigationController:(UINavigationController *)navController
{
    UIColor * barColor = [self barColorForColor:color];
    UIBarStyle barStyle = [self barStyleForColor:color];
    BOOL isTranslucent = [self barTranslucencyForColor:color];

    UINavigationBar * navBar = [navController navigationBar];
    [navBar setBarStyle:barStyle];
    [navBar setTranslucent:isTranslucent];
    [navBar setBarTintColor:barColor];
    
    //This should not be here but in setToolBar. But keeping in place. Clean in 3.2.0
    UIToolbar * toolBar = [navController toolbar];
    [toolBar setBarStyle:barStyle];
    [toolBar setTranslucent:isTranslucent];
    [toolBar setBarTintColor:barColor];
}

+(NSString*)replaceString:(NSString *)string characters:(NSCharacterSet *)characterSet withString:(NSString *)replacementString
{
	if(string == nil)
	{
		return nil;
	}

	NSRange setRange = [string rangeOfCharacterFromSet:characterSet];

	if(setRange.location == NSNotFound)
	{
		return string;
	}

	return [[string componentsSeparatedByCharactersInSet:characterSet] componentsJoinedByString:replacementString];
}

+(NSStringEncoding)charsetToEncoding:(NSString*)type
{
    if (encodingMap == nil) {
        encodingMap = [[NSDictionary alloc] initWithObjectsAndKeys:
                       NUMUINT(NSASCIIStringEncoding),kTiASCIIEncoding,
                       NUMUINT(NSISOLatin1StringEncoding),kTiISOLatin1Encoding,
                       NUMUINT(NSUTF8StringEncoding),kTiUTF8Encoding,
                       NUMUINT(NSUTF16StringEncoding),kTiUTF16Encoding,
                       NUMUINT(NSUTF16BigEndianStringEncoding),kTiUTF16BEEncoding,
                       NUMUINT(NSUTF16LittleEndianStringEncoding),kTiUTF16LEEncoding,
                       nil];
    }
    return [[encodingMap valueForKey:type] unsignedIntegerValue];
}

+(TiDataType)constantToType:(NSString *)type
{
    if (typeMap == nil) {
        typeMap = [[NSDictionary alloc] initWithObjectsAndKeys:
                   NUMINT(TI_BYTE),kTiByteTypeName,
                   NUMINT(TI_SHORT),kTiShortTypeName,
                   NUMINT(TI_INT),kTiIntTypeName,
                   NUMINT(TI_LONG),kTiLongTypeName,
                   NUMINT(TI_FLOAT),kTiFloatTypeName,
                   NUMINT(TI_DOUBLE),kTiDoubleTypeName,
                   nil];
    }
    return [[typeMap valueForKey:type] intValue];
}

+(int)dataSize:(TiDataType)type
{
    if (sizeMap == nil) {
        sizeMap = [[NSDictionary alloc] initWithObjectsAndKeys:
                   NUMINT(sizeof(char)), NUMINT(TI_BYTE),
                   NUMINT(sizeof(uint16_t)), NUMINT(TI_SHORT),
                   NUMINT(sizeof(uint32_t)), NUMINT(TI_INT),
                   NUMINT(sizeof(uint64_t)), NUMINT(TI_LONG),
                   NUMINT(sizeof(Float32)), NUMINT(TI_FLOAT),
                   NUMINT(sizeof(Float64)), NUMINT(TI_DOUBLE),
                   nil];
    }
    return [[sizeMap objectForKey:NUMINT(type)] intValue];
}

+(int)encodeString:(NSString *)string toBuffer:(TiBuffer *)dest charset:(NSString*)charset offset:(NSUInteger)destPosition sourceOffset:(NSUInteger)srcPosition length:(NSUInteger)srcLength
{
    // TODO: Define standardized behavior.. but for now:
    // 1. Throw exception if destPosition extends past [dest length]
    // 2. Throw exception if srcPosition > [string length]
    // 3. Use srcLength as a HINT (as in all other buffer ops)
    
    if (destPosition >= [[dest data] length]) {
        return BAD_DEST_OFFSET;
    }
    if (srcPosition >= [string length]) {
        return BAD_SRC_OFFSET;
    }
    
    NSStringEncoding encoding = [TiUtils charsetToEncoding:charset];
    
    if (encoding == 0) {
        return BAD_ENCODING;
    }
    
    NSUInteger length = MIN(srcLength, [string length] - srcPosition);
    NSData* encodedString = [[string substringWithRange:NSMakeRange(srcPosition, length)] dataUsingEncoding:encoding];
    NSUInteger encodeLength = MIN([encodedString length], [[dest data] length] - destPosition);
    
    void* bufferBytes = [[dest data] mutableBytes];
    const void* stringBytes = [encodedString bytes];
    
    memcpy(bufferBytes+destPosition, stringBytes, encodeLength);
    
    return (int)(destPosition+encodeLength);
}

+(int)encodeNumber:(NSNumber *)data toBuffer:(TiBuffer *)dest offset:(int)position type:(NSString *)type endianness:(CFByteOrder)byteOrder
{
    switch (byteOrder) {
        case CFByteOrderBigEndian:
        case CFByteOrderLittleEndian:
            break;
        default:
            return BAD_ENDIAN;
    }
    
    if (position >= [[dest data] length]) {
        return BAD_DEST_OFFSET;
    }
    
    void* bytes = [[dest data] mutableBytes];
    TiDataType dataType = [TiUtils constantToType:type];
    int size = [TiUtils dataSize:dataType];
    
    if (size > MIN([[dest data] length], [[dest data] length] - position)) {
        return TOO_SMALL;
    }
    
    switch ([self constantToType:type]) {
        case TI_BYTE: {
            char byte = [data charValue];
            memcpy(bytes+position, &byte, size);
            break;
        }
        case TI_SHORT: {
            uint16_t val = [data shortValue];
            switch (byteOrder) {
                case CFByteOrderLittleEndian: {
                    val = CFSwapInt16HostToLittle(val);
                    break;
                }
                case CFByteOrderBigEndian: {
                    val = CFSwapInt16HostToBig(val);
                    break;
                }
            }
            memcpy(bytes+position, &val, size);
            break;
        }
        case TI_INT: {
            uint32_t val = [data intValue];
            switch (byteOrder) {
                case CFByteOrderLittleEndian: {
                    val = CFSwapInt32HostToLittle(val);
                    break;
                }
                case CFByteOrderBigEndian: {
                    val = CFSwapInt32HostToBig(val);
                    break;
                }
            }
            memcpy(bytes+position, &val, size);
            break;
        }
        case TI_LONG: {
            uint64_t val = [data longLongValue];
            switch (byteOrder) {
                case CFByteOrderLittleEndian: {
                    val = CFSwapInt64HostToLittle(val);
                    break;
                }
                case CFByteOrderBigEndian: {
                    val = CFSwapInt64HostToBig(val);
                    break;
                }
            }
            memcpy(bytes+position, &val, size);
            break;
        }
        case TI_FLOAT: {
            // To prevent type coercion, we use a union where we assign the floatVaue as a Float32, and then access the integer byte representation off of the CFSwappedFloat struct.
            union {
                Float32 f;
                CFSwappedFloat32 sf;
            } val;
            val.f = [data floatValue];
            switch (byteOrder) {
                case CFByteOrderLittleEndian: {
                    val.sf.v = CFSwapInt32HostToLittle(val.sf.v);
                    break;
                }
                case CFByteOrderBigEndian: {
                    val.sf.v = CFSwapInt32HostToBig(val.sf.v);
                    break;
                }
            }
            memcpy(bytes+position, &(val.sf.v), size);
            break;
        }
        case TI_DOUBLE: {
            // See above for why we do union encoding.
            union {
                Float64 f;
                CFSwappedFloat64 sf;
            } val;
            val.f = [data doubleValue];
            switch (byteOrder) {
                case CFByteOrderLittleEndian: {
                    val.sf.v = CFSwapInt64HostToLittle(val.sf.v);
                    break;
                }
                case CFByteOrderBigEndian: {
                    val.sf.v = CFSwapInt64HostToBig(val.sf.v);
                    break;
                }
            }
            memcpy(bytes+position, &(val.sf.v), size);
            break;
        }
        default:
            return BAD_TYPE;
    }
    
    return (position+size);
}

+(NSString*)convertToHex:(unsigned char*)result length:(size_t)length
{
	NSMutableString* encoded = [[NSMutableString alloc] initWithCapacity:length];
	for (int i=0; i < length; i++) {
		[encoded appendFormat:@"%02x",result[i]];
	}
	NSString* value = [encoded lowercaseString];
	[encoded release];
	return value;
}

+(NSString*)md5:(NSData*)data
{
	unsigned char result[CC_MD5_DIGEST_LENGTH];
	CC_MD5([data bytes], (CC_LONG)[data length], result);
	return [self convertToHex:(unsigned char*)&result length:CC_MD5_DIGEST_LENGTH];    
}

+(NSString*)appIdentifier
{
    NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
    NSString* uid = [defaults stringForKey:kAppUUIDString];
    if (uid == nil) {
        uid = [TiUtils createUUID];
        [defaults setObject:uid forKey:kAppUUIDString];
        [defaults synchronize];
    }
    
    return uid;
}

// In pre-iOS 5, it looks like response headers were case-mangled.
// (i.e. WWW-Authenticate became Www-Authenticate). So we have to take this
// mangling into mind; headers such as FooBar-XYZ may also have been mangled
// to be case-correct. We can't be certain.
//
// This means we need to follow the RFC2616 implied MUST that headers are case-insensitive.

+(NSString*)getResponseHeader:(NSString *)header fromHeaders:(NSDictionary *)responseHeaders
{
    // Do a direct comparison first, and then iterate through the headers if we have to.
    // This makes things faster in almost all scenarios, and ALWAYS so under iOS 5 unless
    // the developer is also taking advantage of RFC2616's header spec.
    __block NSString* responseHeader = [responseHeaders valueForKey:header];
    if (responseHeader != nil) {
        return responseHeader;
    }
    
    [responseHeaders enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL* stop) {
        if ([key localizedCaseInsensitiveCompare:header] == NSOrderedSame) {
            *stop = YES;
            responseHeader = obj;
        }
    }];
    
    return responseHeader;
}

+(id)loadBackgroundImage:(id)image forProxy:(TiProxy*)proxy
{
    if ([image isKindOfClass:[UIImage class]]) {
        return image;
    } else if ([image isKindOfClass:[TiBlob class]]) {
        return ((TiBlob*)image).image;
    }
    else if ([image isKindOfClass:[NSString class]]) {
        NSURL *bgURL = [TiUtils toURL:image proxy:proxy];
        id resultImage = [[ImageLoader sharedLoader] loadImmediateImage:bgURL];
        if (resultImage==nil && [image isEqualToString:@"Default.png"])
        {
            // special case where we're asking for Default.png and it's in Bundle not path
            return [UIImage imageNamed:image];
        }
        if((resultImage != nil) && ([resultImage isKindOfClass:[UIImage class]]) && ([resultImage imageOrientation] != UIImageOrientationUp))
        {
            return [UIImageResize resizedImage:[(UIImage*)resultImage size]
                                 interpolationQuality:kCGInterpolationNone 
                                                image:resultImage 
                                                hires:NO];
        }
        return resultImage;
    }
    return nil;
}

+(UIImage*)stretchedImage:(UIImage*)image withCap:(TiCap)cap {
    CGFloat maxWidth = [image size].width * image.scale;
    CGFloat maxHeight = [image size].height * image.scale;
    
    NSInteger left = (TiDimensionIsAuto(cap.leftCap) || TiDimensionIsUndefined(cap.leftCap) || cap.leftCap.value == 0) ?
    maxWidth/2  :
    TiDimensionCalculateValue(cap.leftCap, maxWidth);
    NSInteger top = (TiDimensionIsAuto(cap.topCap) || TiDimensionIsUndefined(cap.topCap) || cap.topCap.value == 0) ?
    maxHeight/2  :
    TiDimensionCalculateValue(cap.topCap, maxHeight);
    
    if ([image respondsToSelector:@selector(resizableImageWithCapInsets:resizingMode:)]) {
        
        if (left >= maxWidth) {
            left = maxWidth - 2;
        }
        if (top >= maxHeight) {
            top = maxHeight - 2;
        }
        
        NSInteger right = TiDimensionIsUndefined(cap.rightCap)?left:TiDimensionCalculateValue(cap.rightCap, maxWidth);
        NSInteger bottom = TiDimensionIsUndefined(cap.bottomCap)?top:TiDimensionCalculateValue(cap.bottomCap, maxHeight);
        
        if ((left + right) >= maxWidth) {
            right = maxWidth - (left + 1);
        }
        if ((top + bottom) >= maxHeight) {
            bottom = maxHeight - (top + 1);
        }
        
        return [image resizableImageWithCapInsets:UIEdgeInsetsMake(top, left, bottom, right) resizingMode:UIImageResizingModeStretch];
    }
    else
    {
        if (left >= maxWidth) {
            left = maxWidth - 2;
        }
        
        if (top > maxHeight) {
            top = maxHeight - 2;
        }
        return [image stretchableImageWithLeftCapWidth:left topCapHeight:top];
    }
    return image;
}

+(id)loadBackgroundImage:(id)image forProxy:(TiProxy*)proxy withCap:(TiCap)cap
{
    UIImage* resultImage = nil;
    if ([image isKindOfClass:[UIImage class]]) {
        resultImage = image;
    } else if ([image isKindOfClass:[TiBlob class]]) {
        resultImage = ((TiBlob*)image).image;
    }
    else if ([image isKindOfClass:[NSString class]]) {
        NSURL *bgURL = [TiUtils toURL:image proxy:proxy];
        id resultImage = [[ImageLoader sharedLoader] loadImmediateStretchableImage:bgURL withCap:cap];
        if (resultImage==nil && [image isEqualToString:@"Default.png"])
        {
            // special case where we're asking for Default.png and it's in Bundle not path
            return [UIImage imageNamed:image];
        }
        if((resultImage != nil) && ([resultImage isKindOfClass:[UIImage class]]) && ([resultImage imageOrientation] != UIImageOrientationUp))
        {
            return [UIImageResize resizedImage:[(UIImage*)resultImage size]
                          interpolationQuality:kCGInterpolationNone
                                         image:resultImage
                                         hires:NO];
        }
        return resultImage;    }
    else if ([image isKindOfClass:[TiBlob class]]) {
        resultImage = [image image];
    }
    return resultImage;
}

+ (BOOL) isSVG:(id)arg
{
    return ([arg isKindOfClass:[NSString class]] && [((NSString*)arg) hasSuffix:@".svg"]) ||
        ([arg isKindOfClass:[NSURL class]] && [[((NSURL*)arg) absoluteString] hasSuffix:@".svg"]);
}

+ (NSString*)messageFromError:(NSError *)error
{
	if (error == nil) {
		return nil;
	}
	NSString * result = [error localizedDescription];
	NSString * userInfoMessage = [[error userInfo] objectForKey:@"message"];
	if (result == nil)
	{
		result = userInfoMessage;
	}
	else if(userInfoMessage != nil)
	{
		result = [result stringByAppendingFormat:@" %@",userInfoMessage];
	}
	return result;
}

+ (NSMutableDictionary *)dictionaryWithCode:(NSInteger)code message:(NSString *)message
{
	return [NSMutableDictionary dictionaryWithObjectsAndKeys:
			NUMBOOL(code==0), @"success",
			NUMLONG(code), @"code",
			message,@"error", nil];
}

+(UIView*)UIViewWithFrame:(CGRect)frame
{
    UIView *view = [[UIView alloc] initWithFrame:frame];
//    view.layer.shouldRasterize = YES;
    view.layer.rasterizationScale = [[UIScreen mainScreen] scale];
    return [view autorelease];
}

+(NSString*)jsonStringify:(id)value error:(NSError**)error
{
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:value
                                                       options:kNilOptions
                                                         error:error];
    if (jsonData == nil || [jsonData length] == 0) {
        return nil;
    } else {
        NSString *str = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
        return [str autorelease];
    }

}
+(id)jsonParse:(NSString*)value error:(NSError**)error;
{
    return [NSJSONSerialization JSONObjectWithData: [value dataUsingEncoding: NSUTF8StringEncoding]
                                            options: NSJSONReadingMutableContainers
                                              error: error];
}
+(NSString*)jsonStringify:(id)value
{
    NSError *error = nil;
    NSString *r = [self jsonStringify:value error:&error];
    if(error != nil) {
        NSLog(@"Could not stringify JSON. Error: %@", error);
    }
    return r;
}
+(id)jsonParse:(NSString*)value
{
    NSError *error = nil;
    id r = [self jsonParse:value error:&error];
    if(error != nil) {
        NSLog(@"Could not parse JSON. Error: %@", error);
    }
    return r;
}

+(BOOL)forceTouchSupported
{
#if IS_XCODE_7
    if ([self isIOS9OrGreater] == NO) {
        return NO;
    }
    return [[[[TiApp app] window] traitCollection] forceTouchCapability] == UIForceTouchCapabilityAvailable;
#else
    return NO;
#endif
}

+(NSString*)currentArchitecture
{
#ifdef __arm64__
    return @"arm64";
#endif
#ifdef __arm__
    return @"armv7";
#endif
#ifdef __x86_64__
    return @"x86_64";
#endif
#ifdef __i386__
    return @"i386";
#endif
    return @"Unknown";
}

+(NSString*)base64encode:(NSData*)toEncode
{
    NSString* result = nil;
//    if ([toEncode respondsToSelector:@selector(base64EncodedStringWithOptions:)]) {
        return [toEncode base64EncodedStringWithOptions:0];
//    } else {
//        return [toEncode base64Encoding];
//    }
}

+(NSData*)base64decode:(NSString*)encoded
{
//    if ([NSData respondsToSelector:@selector(initWithBase64EncodedString:options:)]) {
        return [[[NSData alloc] initWithBase64EncodedString:encoded options:0] autorelease];
//    } else {
//        return [[[NSData alloc] initWithBase64Encoding:encoded] autorelease];
//    }
}

+(NSString *)colorHexString:(UIColor *)color {
    
    if (!color || color == [UIColor clearColor]) {
        return @"transparent";
    }
    
    if (color == [UIColor whiteColor]) {
        // Special case, as white doesn't fall into the RGB color space
        return @"ffffff";
    }
    
    CGFloat red;
    CGFloat blue;
    CGFloat green;
    CGFloat alpha;
    
    [color getRed:&red green:&green blue:&blue alpha:&alpha];
    
    int redDec = (int)(red * 255);
    int greenDec = (int)(green * 255);
    int blueDec = (int)(blue * 255);
    
    if (alpha == 1.0f) {
        return [NSString stringWithFormat:@"#%02x%02x%02x", (unsigned int)redDec, (unsigned int)greenDec, (unsigned int)blueDec];
    }
    else {
        int alphaDec = (int)(alpha * 255);
        return [NSString stringWithFormat:@"#%02x%02x%02x%02x", (unsigned int)alphaDec, (unsigned int)redDec, (unsigned int)greenDec, (unsigned int)blueDec];
    }
}


+ (NSString *)replacingStringsIn:(NSString*)string fromDictionary:(NSDictionary *)dict
{
    NSMutableString *result = [string mutableCopy];
    [dict enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop) {
        [result replaceOccurrencesOfString:key withString:obj
                                   options:0 range:NSMakeRange(0, [result length])];
        
    }];
    for (NSString *target in dict) {
        
    }
    return [result autorelease];
}

+ (NSString *)replacingStringsIn:(NSString*)string fromDictionary:(NSDictionary *)dict withPrefix:(NSString*)prefix
{
    NSMutableString *result = [string mutableCopy];
    [dict enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop) {
        [result replaceOccurrencesOfString:[prefix stringByAppendingString:key] withString:[TiUtils stringValue:obj]
                                   options:0 range:NSMakeRange(0, [result length])];
        
    }];
    for (NSString *target in dict) {
        
    }
    return [result autorelease];
}

+(BOOL)applyMathDict:(NSDictionary*)mathDict forEvent:(NSDictionary*)event fromProxy:(TiProxy*)proxy
{
    NSMutableDictionary* variables = nil;
    NSMutableDictionary* expressions = nil;

    NSDictionary* vars = [mathDict objectForKey:@"variables"];
    if (vars) {
        variables = [NSMutableDictionary dictionaryWithCapacity:[vars count]];
        [vars enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop) {
            id value = [event valueForKeyPath:obj];
            if (value) {
                [variables setObject:value forKey:key];
            }
        }];
    }
    
    DDMathEvaluator *eval = [DDMathEvaluator defaultMathEvaluator];
    if ([mathDict objectForKey:@"condition"]) {
        NSNumber* result = [eval evaluateString:[mathDict objectForKey:@"condition"] withSubstitutions:variables];
        if ([result boolValue] == NO) {
            return NO;
        }
    }
    
    NSDictionary* exps = [mathDict objectForKey:@"expressions"];
    if ([mathDict objectForKey:@"expressions"]) {
        expressions = [NSMutableDictionary dictionaryWithDictionary:variables];
        [exps enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop) {
            NSNumber* result = [eval evaluateString:obj withSubstitutions:expressions];
            if (result) {
                [expressions setObject:result forKey:key];
            }
            
        }];
    } else {
        expressions = variables;
    }
    
    NSArray* targets = [mathDict objectForKey:@"targets"];
    
    if (targets) {
        for (NSDictionary* targetDict in targets) {
            id target = [targetDict valueForKey:@"target"];
            
            if (!target) {
                target = proxy;
            } else if (IS_OF_CLASS(target, NSString)) {
                target = [proxy valueForKey:target];
            }
            if (!target) continue;
            NSDictionary* props = [targetDict valueForKey:@"properties"];
            NSDictionary* targetVariables = [targetDict valueForKey:@"targetVariables"];
            NSMutableDictionary* targetVariablesComputed = nil;
            if (targetVariables) {
                targetVariablesComputed = [NSMutableDictionary dictionaryWithCapacity:[targetVariables count]];
                [targetVariables enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop) {
                    id value = [target valueForKey:obj];
                    [targetVariablesComputed setObject:value?value:@"0" forKey:key];
                }];
            }
            NSMutableDictionary* realProps = [NSMutableDictionary dictionaryWithCapacity:[props count]];
            __block NSError* error;
            __block NSMutableDictionary* toUse;
            [props enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop) {
                toUse = [NSMutableDictionary dictionaryWithDictionary:expressions];
                if (targetVariablesComputed) {
                    [toUse addEntriesFromDictionary:targetVariablesComputed];
                }
                id current = [target valueForKey:key];
                if (!current || IS_OF_CLASS(current, NSNull)) {
                    current = @"0";
                }
                [toUse setObject:current forKey:@"current"];

                [realProps setValue:[eval evaluateString:obj withSubstitutions:toUse error:&error] forKey:key];
                if (error) {
                    [realProps setValue:[TiUtils replacingStringsIn:obj fromDictionary:toUse withPrefix:@"_"] forKey:key];
                }
            }];
            [target applyProperties:realProps];
        }
    }
    return YES;
}


@end
