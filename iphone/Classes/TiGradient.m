/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiGradient.h"

#import "TiUtils.h"

#define byte unsigned char
#define F2CC(x) ((byte)(255 * x))
#define RGBAF(r,g,b,a) (F2CC(r) << 24 | F2CC(g) << 16 | F2CC(b) << 8 | F2CC(a))
#define RGBA(r,g,b,a) ((byte)r << 24 | (byte)g << 16 | (byte)b << 8 | (byte)a)
#define RGBA_R(c) ((uint)c >> 24 & 255)
#define RGBA_G(c) ((uint)c >> 16 & 255)
#define RGBA_B(c) ((uint)c >> 8 & 255)
#define RGBA_A(c) ((uint)c >> 0 & 255)

@implementation TiGradientLayer
@synthesize gradient;

- (void) dealloc
{
	RELEASE_TO_NIL(gradient)
	[super dealloc];
}

-(void)drawInContext:(CGContextRef)ctx
{
	[gradient paintContext:ctx bounds:[self bounds]];
}

@end

@interface TiGradient()
{
    UIImage* cachedImage;
    CGSize cacheSize;
    CGImageRef cachedSweepImage;
    CGFloat sweepStartAngle;
    CGRect gradientRect;
}
@end

@implementation TiGradient
@synthesize backfillStart, backfillEnd;


- (id)init {
    //a trick to make sure we add our animationKeys
    if (self = [super init])
    {
        sweepStartAngle = 0;
        gradientRect = CGRectNull;
    }
    return self;
}


-(void)ensureOffsetArraySize:(int)newSize
{
	if (newSize <= arraySize)
	{
		return;
	}
	colorOffsets = realloc(colorOffsets, (sizeof(CGFloat) * newSize));
	for (int i=arraySize; i<newSize; i++)
	{
		colorOffsets[i]=-1;
	}
	arraySize = newSize;
}

-(CGGradientRef) cachedGradient
{
	if ((cachedGradient == NULL) && (colorValues != NULL))
	{
		CGColorSpaceRef rgb = CGColorSpaceCreateDeviceRGB();
		BOOL needsFreeing = NO;

		CGFloat * tempOffsets;
		if (offsetsDefined == CFArrayGetCount(colorValues))
		{
			tempOffsets = colorOffsets;
		}
		else
		{
			tempOffsets = NULL;
		}
		//TODO: Between these extremes, we should do intelligent gradient computation.

		cachedGradient = CGGradientCreateWithColors(rgb, colorValues, tempOffsets);

		if (needsFreeing)
		{
			free(tempOffsets);
		}
		CGColorSpaceRelease(rgb);
	}
	return cachedGradient;
}

-(void)clearCache
{
	if (cachedGradient != NULL)
	{
		CGGradientRelease(cachedGradient);
		cachedGradient = NULL;
	}
    if (cachedSweepImage != nil) {
        CGImageRelease(cachedSweepImage);
        cachedSweepImage = nil;
    }
    RELEASE_TO_NIL(cachedImage);
    cacheSize = CGSizeZero;
}

- (void) dealloc
{
	if (colorValues != NULL)
	{
		CFRelease(colorValues);
        colorValues = NULL;
	}
    RELEASE_TO_NIL(endPoint)
    RELEASE_TO_NIL(startPoint)
	[self clearCache];
	free(colorOffsets);
	[super dealloc];
}



-(id)type
{
	switch (type)
	{
		case TiGradientTypeRadial:
			return @"radial";
        case TiGradientTypeSweep:
			return @"sweep";
		default: {
			break;
		}
	}
	return @"linear";
}

-(void)setType:(id)newType
{
	ENSURE_TYPE(newType,NSString);
	[self clearCache];
	if ([newType compare:@"linear" options:NSCaseInsensitiveSearch]==NSOrderedSame)
	{
		type = TiGradientTypeLinear;
		return;
	}

	else if ([newType compare:@"radial" options:NSCaseInsensitiveSearch]==NSOrderedSame)
	{
		type = TiGradientTypeRadial;
		return;
	}
    
    else if ([newType compare:@"sweep" options:NSCaseInsensitiveSearch]==NSOrderedSame)
	{
		type = TiGradientTypeSweep;
		return;
	}

	[self throwException:TiExceptionInvalidType subreason:@"Must be either 'linear' or 'radial' or 'sweep'" location:CODELOCATION];
}

-(void)setStartPoint:(id)newStart
{
	if (startPoint == nil)
	{
		startPoint = [[TiPoint alloc] initWithObject:newStart];
	}
	else
	{
		[startPoint setValues:newStart];
	}
	[self clearCache];
}


-(void)setStartAngle:(id)value
{
	sweepStartAngle = [TiUtils floatValue:value def:0] *M_PI / 180;
	[self clearCache];
}

-(void)setEndPoint:(id)newEnd
{
	if (endPoint == nil)
	{
		endPoint = [[TiPoint alloc] initWithObject:newEnd];
	}
	else
	{
		[endPoint setValues:newEnd];
	}
	[self clearCache];
}

-(void)setStartRadius:(id)newRadius
{
	startRadius = [TiUtils dimensionValue:newRadius];
}

-(void)setEndRadius:(id)newRadius
{
	endRadius = [TiUtils dimensionValue:newRadius];
}

-(void)setRect:(id)rect
{
	gradientRect = [TiUtils rectValue:rect];
}

-(void)setColors:(NSArray *)newColors;
{
	ENSURE_TYPE(newColors,NSArray);
	if (colorValues == NULL)
	{
		colorValues = CFArrayCreateMutable(NULL, [newColors count], &kCFTypeArrayCallBacks);
	}
	else
	{
		CFArrayRemoveAllValues(colorValues);
	}

	[self ensureOffsetArraySize:[newColors count]];
	int currentIndex=0;
	offsetsDefined = 0;

	Class dictClass = [NSDictionary class];
	for (id thisEntry in newColors)
	{
		CGFloat thisOffset = -1;
		if ([thisEntry isKindOfClass:dictClass])
		{
			thisOffset = [TiUtils floatValue:@"offset" properties:thisEntry def:-1];
			thisEntry = [thisEntry objectForKey:@"color"];
		}

		UIColor * thisColor = [[TiUtils colorValue:thisEntry] _color];

		if (thisColor == nil)
		{
			[self throwException:TiExceptionInvalidType subreason:
					@"Colors must be an array of colors or objects with a color property"
					location:CODELOCATION];
		}	   


		CGColorSpaceRef colorspace = CGColorGetColorSpace([thisColor CGColor]);
		if(CGColorSpaceGetModel(colorspace) == kCGColorSpaceModelMonochrome) //Colorize this! Where's Ted Turner?
		{
			const CGFloat *components = CGColorGetComponents([thisColor CGColor]);
			thisColor = [UIColor colorWithRed:components[0] green:components[0]
					blue:components[0] alpha:components[1]];
		}

		colorOffsets[currentIndex] = thisOffset;
		if (thisOffset != -1)
		{
			offsetsDefined ++;
		}

		CFArrayAppendValue(colorValues, [thisColor CGColor]);
	    currentIndex ++;
	}
	[self clearCache];
}

#define PYTHAG(bounds)	sqrt(bounds.width * bounds.width + bounds.height * bounds.height)/2


-(void)createCache:(CGRect)bounds
{
    CGRect actualBounds = CGRectIntegral(bounds);
    if (!CGRectIsNull(gradientRect)) {
        actualBounds = gradientRect;
    }
    cacheSize = actualBounds.size;
  
    if (type == TiGradientTypeSweep) {
        CGImageRef imgRef = [self newSweepImageGradientInRect:actualBounds];
        cachedImage = [[UIImage imageWithCGImage:imgRef] retain];
        CGImageRelease(imgRef);
       return;
    }
    CGColorSpaceRef space = CGColorSpaceCreateDeviceRGB();
	CGContextRef cacheContext = CGBitmapContextCreate(nil, cacheSize.width, cacheSize.height, 8, cacheSize.width * (CGColorSpaceGetNumberOfComponents(space) + 1), space, kCGImageAlphaPremultipliedLast);
	CGColorSpaceRelease(space);
	CGGradientDrawingOptions options = 0;
	if(backfillStart)
	{
		options |= kCGGradientDrawsBeforeStartLocation;
	}
	if(backfillEnd)
	{
		options |= kCGGradientDrawsAfterEndLocation;
	}
    
	switch (type)
	{
		case TiGradientTypeLinear:
			CGContextDrawLinearGradient(cacheContext, [self cachedGradient],
                                        [TiUtils pointValue:startPoint bounds:actualBounds defaultOffset:CGPointZero],
                                        [TiUtils pointValue:endPoint bounds:actualBounds defaultOffset:CGPointMake(0, 1)],
                                        options);
			break;
		case TiGradientTypeRadial:
        {
			CGFloat startRadiusPixels;
			CGFloat endRadiusPixels;
			switch (startRadius.type)
			{
				case TiDimensionTypeDip:
					startRadiusPixels = startRadius.value;
					break;
				case TiDimensionTypePercent:
					startRadiusPixels = startRadius.value * PYTHAG(actualBounds.size);
					break;
				default:
					startRadiusPixels = 0;
			}
			
			switch (endRadius.type)
			{
				case TiDimensionTypeDip:
					endRadiusPixels = endRadius.value;
					break;
				case TiDimensionTypePercent:
					endRadiusPixels = endRadius.value * PYTHAG(actualBounds.size);
					break;
				default:
					endRadiusPixels = PYTHAG(bounds.size);
			}
			
			CGContextDrawRadialGradient(cacheContext, [self cachedGradient],
                                        [TiUtils pointValue:startPoint bounds:actualBounds defaultOffset:CGPointMake(0.5, 0.5)],startRadiusPixels,
                                        [TiUtils pointValue:endPoint bounds:actualBounds defaultOffset:CGPointMake(0.5, 0.5)],endRadiusPixels,
                                        options);
			break;
        }
        default:
            break;
	}
    CGImageRef imgRef = CGBitmapContextCreateImage(cacheContext);
    cachedImage = [[UIImage imageWithCGImage:imgRef] retain];
    CGImageRelease(imgRef);
	CGContextRelease(cacheContext);
}

-(void)paintContext:(CGContextRef)context bounds:(CGRect)bounds
{
    if (CGRectIsNull(gradientRect) && !CGSizeEqualToSize(cacheSize, bounds.size) || cachedImage == nil){
        [self clearCache];
        [self createCache:bounds];
    }
    
//    CGContextTranslateCTM(context, 0, bounds.size.height);
//    CGContextScaleCTM(context, 1.0, -1.0);
    CGRect imageRect = CGRectMake(0, 0, cachedImage.size.width, cachedImage.size.height);
    CGContextDrawTiledImage(context, imageRect, cachedImage.CGImage);
}

- (CGImageRef)newSweepImageGradientInRect:(CGRect)rect
{
    cacheSize = rect.size;
	int w = CGRectGetWidth(rect);
	int h = CGRectGetHeight(rect);
	int bitsPerComponent = 8;
	int bpp = 4 * bitsPerComponent / 8;
	int byteCount = w * h * bpp;
    
	int colorCount = CFArrayGetCount(colorValues);
	int locationCount = 0;
	int* colors = NULL;
	float* locations = NULL;
    
	if (colorCount > 0) {
		colors = calloc(colorCount, bpp);
		int *p = colors;
        for (int i=0; i<colorCount; i++) {
            CGColorRef c = (CGColorRef)CFArrayGetValueAtIndex(colorValues, i);
            float r, g, b, a;
            
			size_t n = CGColorGetNumberOfComponents(c);
			const CGFloat *comps = CGColorGetComponents(c);
			if (comps == NULL) {
				*p++ = 0;
				continue;
			}
			r = comps[0];
			if (n >= 4) {
				g = comps[1];
				b = comps[2];
				a = comps[3];
			}
			else {
				g = b = r;
				a = comps[1];
			}
			*p++ = RGBAF(r, g, b, a);
        }
	}
    CGFloat * tempOffsets;
    if (offsetsDefined == CFArrayGetCount(colorValues))
    {
        locations = colorOffsets;
        locationCount = offsetsDefined;
    }
    
	byte* data = malloc(byteCount);
    CGPoint center = [TiUtils pointValue:startPoint bounds:rect defaultOffset:CGPointMake(0.5, 0.5)];
	angleGradient(data, w, h, colors, colorCount, locations, locationCount, center, sweepStartAngle);
    
	if (colors) free(colors);
    
	CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
	CGBitmapInfo bitmapInfo = kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Little;
	CGContextRef ctx = CGBitmapContextCreate(data, w, h, bitsPerComponent, w * bpp, colorSpace, bitmapInfo);
	CGColorSpaceRelease(colorSpace);
	CGImageRef img = CGBitmapContextCreateImage(ctx);
	CGContextRelease(ctx);
	free(data);
	return img;
}

static inline byte blerp(byte a, byte b, float w)
{
	return a + w * (b - a);
}
static inline int lerp(int a, int b, float w)
{
	return RGBA(blerp(RGBA_R(a), RGBA_R(b), w),
				blerp(RGBA_G(a), RGBA_G(b), w),
				blerp(RGBA_B(a), RGBA_B(b), w),
				blerp(RGBA_A(a), RGBA_A(b), w));
}

void angleGradient(byte* data, int w, int h, int* colors, int colorCount, float* locations, int locationCount, CGPoint center, float startAngle)
{
	if (colorCount < 1) return;
	if (locationCount > 0 && locationCount != colorCount) return;
    
	int* p = (int*)data;
    
	for (int y = 0; y < h; y++)
        for (int x = 0; x < w; x++) {
            float dirX = x - center.x;
            float dirY = y - center.y;
            float angle = -atan2f(dirY, dirX);
            angle += startAngle;
            if (angle < 0) angle += 2 * M_PI;
            angle /= 2 * M_PI;
            
            int index = 0, nextIndex = 0;
            float t = 0;
            
            if (locationCount > 0) {
                for (index = locationCount - 1; index >= 0; index--) {
                    if (angle >= locations[index]) {
                        break;
                    }
                }
                if (index >= locationCount) index = locationCount - 1;
                nextIndex = index + 1;
                if (nextIndex >= locationCount) nextIndex = locationCount - 1;
                float ld = (locations[nextIndex] - locations[index]);
                t = ld <= 0 ? 0 : (angle - locations[index]) / ld;
            }
            else {
                t = angle * (colorCount - 1);
                index = t;
                t -= index;
                nextIndex = index + 1;
                if (nextIndex >= colorCount) nextIndex = colorCount - 1;
            }
            
            int lc = colors[index];
            int rc = colors[nextIndex];
            int color = lerp(lc, rc, t);
            *p++ = color;
        }
}

+(TiGradient *)gradientFromObject:(id)value proxy:(TiProxy *)proxy
{
	if ([value isKindOfClass:[NSDictionary class]])
	{
        id<TiEvaluator> context = ([proxy executionContext] == nil) ? [proxy pageContext] : [proxy executionContext];
		TiGradient * newGradient = [[[TiGradient alloc] _initWithPageContext:context] autorelease];
		[newGradient _initWithProperties:value];
		return newGradient;
	}
	else if([value isKindOfClass:[TiGradient class]])
	{
		return value;
	}
	return nil;
}

@end
