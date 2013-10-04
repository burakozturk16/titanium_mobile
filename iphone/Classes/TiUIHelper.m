//
//  TiUIHelper.m
//  Titanium
//
//  Created by Martin Guillon on 23/08/13.
//
//

#import "TiUIHelper.h"
#import "TiUtils.h"

@implementation TiUIHelper

+(void)applyShadow:(NSDictionary*)args toLayer:(CALayer *)layer
{
    if (args == nil) {
        layer.masksToBounds = YES;
        layer.shadowOpacity = 0.0f;
        layer.shouldRasterize = NO;
        return;
    }
    layer.masksToBounds = NO;
    layer.shouldRasterize = YES;
    layer.rasterizationScale = [[UIScreen mainScreen] scale];
    ShadowDef data = [TiUIHelper getShadow:args];
    layer.shadowOffset = data.offset;
    layer.shadowOpacity = data.opacity;
    layer.shadowColor = data.color;
    layer.shadowRadius = data.radius;
}

+(ShadowDef)getShadow:(NSDictionary*)args;
{
    ShadowDef result;
    if ([args objectForKey:@"offset"]) {
        NSDictionary* offsetDict  = [args objectForKey:@"offset"];
        CGSize offset = CGSizeZero;
        if ([offsetDict objectForKey:@"x"]) {
            offset.width = [TiUtils floatValue:[offsetDict objectForKey:@"x"]];
        }
        if ([offsetDict objectForKey:@"y"]) {
            offset.height = [TiUtils floatValue:[offsetDict objectForKey:@"y"]];
        }
        result.offset = offset;
    }
    else {
        result.offset = CGSizeZero;
    }
    
    if ([args objectForKey:@"opacity"]) {
        result.opacity = [TiUtils floatValue:[args objectForKey:@"opacity"]];
    }
    else {
        result.opacity = 1.0f;
    }
    
    if ([args objectForKey:@"color"]) {
        result.color = [[TiUtils colorValue:[args objectForKey:@"color"]] _color].CGColor;
    }
    else {
        result.color = [UIColor blackColor].CGColor;
    }
    if ([args objectForKey:@"radius"]) {
        result.radius = [TiUtils floatValue:[args objectForKey:@"radius"]];
    }
    else {
        result.radius = 3.0f; //same as Android
    }
    return result;
}

-(UIImage*) imageFromView:(UIView*)originalView
{
    //Creating a fakeView which is initialized as our original view.
    //Actually i render images on a fakeView and take screenshot of this fakeView.
    UIView *fakeView = [[UIView alloc] initWithFrame:originalView.frame];
    [fakeView setBackgroundColor:originalView.backgroundColor];
    [fakeView.layer setMasksToBounds:originalView.layer.masksToBounds];
    [fakeView.layer setCornerRadius:originalView.layer.cornerRadius];
    [fakeView.layer setBorderColor:originalView.layer.borderColor];
    [fakeView.layer setBorderWidth:originalView.layer.borderWidth];
    
    //Getting subviews of originalView.
    for (UIView *view in originalView.subviews)
    {
        //Getting screenshot of layer of view.
        UIGraphicsBeginImageContext(view.bounds.size);
        [view.layer renderInContext:UIGraphicsGetCurrentContext()];
        UIImage *imageLayer = UIGraphicsGetImageFromCurrentImageContext();
        UIGraphicsEndImageContext();
        
        //If it is masked view, then get masked image.
        if (view.layer.mask)
        {
            //getting screenshot of masked layer.
            UIGraphicsBeginImageContext(view.bounds.size);
            [view.layer.mask renderInContext:UIGraphicsGetCurrentContext()];
            
            //PNG Representation is most important. otherwise this masked image will not work.
            UIImage *maskedLayerImage=[UIImage imageWithData:UIImagePNGRepresentation(UIGraphicsGetImageFromCurrentImageContext())];
            UIGraphicsEndImageContext();
            
            //getting image by masking original image.
            imageLayer = [self maskImage:imageLayer withMask:maskedLayerImage];
        }
        
        //If imageLayer is pointing to a valid object, then setting this image to UIImageView, our UIImageView frame having exactly as view.frame.
        if (imageLayer)
        {
            UIImageView *imageView = [[UIImageView alloc] initWithFrame:view.frame];
            [imageView setImage:imageLayer];
            [fakeView addSubview:imageView];
        }
    }
    
    //At the end, taking screenshot of fakeView. This will get your Original Image.
    UIGraphicsBeginImageContext(fakeView.bounds.size);
    [fakeView.layer renderInContext:UIGraphicsGetCurrentContext()];
    UIImage *previewCapturedImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return previewCapturedImage;
}

//Method is used to get masked image.
- (UIImage*) maskImage:(UIImage *)image withMask:(UIImage *)maskImage
{
    CGImageRef maskRef = maskImage.CGImage;
    CGImageRef mask = CGImageMaskCreate(CGImageGetWidth(maskRef),
                                        CGImageGetHeight(maskRef),
                                        CGImageGetBitsPerComponent(maskRef),
                                        CGImageGetBitsPerPixel(maskRef),
                                        CGImageGetBytesPerRow(maskRef),
                                        CGImageGetDataProvider(maskRef), NULL, false);
    
    CGImageRef masked = CGImageCreateWithMask([image CGImage], mask);
    
    return [UIImage imageWithCGImage:masked];
}
@end
