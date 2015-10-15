/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UIIMAGEVIEW

#import "TiBase.h"
#import "TiUIImageView.h"
#import "TiUtils.h"
#import "ImageLoader.h"
#import "OperationQueue.h"
#import "TiViewProxy.h"
#import "TiProxy.h"
#import "TiBlob.h"
#import "TiFile.h"
#import "UIImage+Resize.h"
#import "TiUIImageViewProxy.h"
#import "TiSVGImage.h"
#import "TiTransitionHelper.h"
#import "TiTransition.h"
#import "TiImageHelper.h"
#import <CommonCrypto/CommonDigest.h>

#define IMAGEVIEW_DEBUG 0

#define IMAGEVIEW_MIN_INTERVAL 30

@interface TiUIImageView()
{
    TiAnimatedImage* _animatedImage;
    NSMutableArray *_images;
    UIImage* _currentImage;
    id _currentImageSource;
    
    NSTimer *timer;
    NSTimeInterval interval;
    NSInteger repeatCount;
    NSInteger index;
    NSInteger iterations;
    UIView *previous;
    UIView *container;
    BOOL ready;
    BOOL stopped;
    BOOL reverse;
    BOOL placeholderLoading;
    TiDimension width;
    TiDimension height;
    CGFloat autoHeight;
    CGFloat autoWidth;
    CGFloat autoScale;
    NSInteger loadCount;
    NSInteger readyCount;
    NSInteger loadTotal;
    UIImageView * imageView;
    UIViewContentMode scaleType;
    BOOL localLoadSync;
    BOOL shouldTransition;
    BOOL onlyTransitionIfRemote;
    BOOL _reusing;
    
    NSDictionary* _filterOptions;
    
    CGFloat animationDuration;
    TiSVGImage* _svg;
    BOOL animationPaused;
    BOOL autoreverse;
    BOOL usingNewMethod;
    NSURL *_defaultImageUrl;
    BOOL _preventDefaultImage;
    BOOL _needsSetImage;
}
@property(nonatomic,retain) NSDictionary *transition;
-(void)startTimerWithEvent:(NSString *)eventName;
-(void)stopTimerWithEvent:(NSString *)eventName;
@end

@implementation TiUIImageView

#pragma mark Internal

DEFINE_EXCEPTIONS

-(id)init
{
    if (self = [super init]) {
        localLoadSync = NO;
        _needsSetImage= NO;
        scaleType = UIViewContentModeScaleAspectFit;
        animationDuration = 0.5;
        self.transition = nil;
        animationPaused=  YES;
        autoreverse = NO;
        usingNewMethod = NO;
        stopped = YES;
        autoScale = 1;
        _reusing = NO;
        _preventDefaultImage = NO;
        _filterOptions = nil;
        onlyTransitionIfRemote = NO;
    }
    return self;
}

-(void)dealloc
{
    if (timer!=nil)
    {
        [timer invalidate];
    }
    RELEASE_TO_NIL(timer);
    RELEASE_TO_NIL(_images);
    RELEASE_TO_NIL(container);
    RELEASE_TO_NIL(previous);
    RELEASE_TO_NIL(imageView);
    RELEASE_TO_NIL(_svg);
    RELEASE_TO_NIL(_transition);
    RELEASE_TO_NIL(_currentImageSource)
    RELEASE_TO_NIL(_defaultImageUrl)
    if (_animatedImage) {
        _animatedImage.delegate = nil;
        RELEASE_TO_NIL(_animatedImage);
    }
    
    RELEASE_TO_NIL(_currentImage);
    RELEASE_TO_NIL(_filterOptions);
    [super dealloc];
}

-(CGSize)contentSizeForSize:(CGSize)size
{
    CGSize result = size;
    if (CGSizeEqualToSize(size, CGSizeZero)) {
        result = CGSizeMake(autoWidth, autoHeight);
    }
    else if(size.width == 0 && autoHeight>0) {
        result.width = (size.height*autoWidth/autoHeight);
    }
    else if(size.height == 0 && autoWidth > 0) {
        result.height = (size.width*autoHeight/autoWidth);
    }
    else if(autoHeight == 0 || autoWidth == 0) {
        result = container.bounds.size;
    }
    else {
        BOOL autoSizeWidth = [(TiViewProxy*)self.proxy widthIsAutoSize];
        BOOL autoSizeHeight = [(TiViewProxy*)self.proxy heightIsAutoSize];
        if (!autoSizeWidth && ! autoSizeHeight) {
            result = size;
        }
        else if(autoSizeWidth && autoSizeHeight) {
            float ratio = autoWidth/autoHeight;
            float viewratio = size.width/size.height;
            if(viewratio > ratio) {
                result.height = MIN(size.height, autoHeight);
                result.width = (result.height*autoWidth/autoHeight);
            }
            else {
                result.width = MIN(size.width, autoWidth);
                result.height = (result.width*autoHeight/autoWidth);
            }        }
        else if(autoSizeHeight) {
            result.width = size.width;
            result.height = result.width*autoHeight/autoWidth;
        }
        else {
            result.height = size.height;
            result.width = (result.height*autoWidth/autoHeight);
        }
    }
    result.width = ceilf(result.width);
    result.height = ceilf(result.height);
    return result;
}

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
{
    imageView.frame = bounds;
    if (imageView.layer.mask != nil) {
        [imageView.layer.mask setFrame:bounds];
    }
    
    if (_svg != nil) {
        imageView.image = [_svg imageForSize:bounds.size];
    }
    
    if (container!=nil)
    {
        for (UIView *child in [container subviews])
        {
            [TiUtils setView:child positionRect:bounds];
        }
    }
    [super frameSizeChanged:frame bounds:bounds];
}

-(void)timerFired:(id)arg
{
    if (stopped) {
        return;
    }
    
    // don't let the placeholder stomp on our new images
    placeholderLoading = NO;
    
    NSInteger position = index % loadTotal;
    
    if (position<0)
    {
        position=loadTotal-1;
        index=position-1;
    }
    UIView *view = [[container subviews] objectAtIndex:position];
    
    // see if we have an activity indicator... if we do, that means the image hasn't yet loaded
    // and we want to start the spinner to let the user know that we're still loading. we
    // don't initially start the spinner when added since we don't want to prematurely show
    // the spinner (usually for the first image) and then immediately remove it with a flash
    UIView *spinner = [[view subviews] count] > 0 ? [[view subviews] objectAtIndex:0] : nil;
    if (spinner!=nil && [spinner isKindOfClass:[UIActivityIndicatorView class]])
    {
        [(UIActivityIndicatorView*)spinner startAnimating];
        [view bringSubviewToFront:spinner];
    }
    
    // the container sits on top of the image in case the first frame (via setUrl) is first
    [self bringSubviewToFront:container];
    
    if (previous!=nil)
    {
        previous.hidden = YES;
        RELEASE_TO_NIL(previous);
    }
    
    previous = [view retain];
    previous.hidden = NO;
    
    if ([[self viewProxy] _hasListeners:@"change" checkParent:NO])
    {
        NSDictionary *evt = [NSDictionary dictionaryWithObject:NUMINTEGER(position) forKey:@"index"];
        [self.proxy fireEvent:@"change" withObject:evt propagate:NO checkForListener:NO];
    }
    
    if (repeatCount > 0 && ((reverse==NO && position == (loadTotal-1)) || (reverse && position==0)))
    {
        iterations++;
        if (iterations == repeatCount) {
            stopped = YES;
            [self.proxy replaceValue:NUMBOOL(NO) forKey:@"animating" notification:NO];
            [self.proxy replaceValue:NUMBOOL(YES) forKey:@"stopped" notification:NO];
            [self.proxy replaceValue:NUMBOOL(NO) forKey:@"paused" notification:NO];
            [self stopTimerWithEvent:@"stop"];
        }
    }
    index = (reverse? --index : ++index);
}

-(void)queueImage:(id)img index:(NSUInteger)index_
{
    UIView *view = [[UIView alloc] initWithFrame:self.bounds];
    UIActivityIndicatorView *spinner = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleWhiteLarge];
    
    spinner.center = view.center;
    spinner.autoresizingMask = UIViewAutoresizingFlexibleLeftMargin | UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleRightMargin | UIViewAutoresizingFlexibleBottomMargin;
    
    [view addSubview:spinner];
    [container addSubview:view];
    [view release];
    [spinner release];
    
    [_images addObject:img];
    [[OperationQueue sharedQueue] queue:@selector(loadImageInBackground:) target:self arg:NUMUINTEGER(index_) after:nil on:nil ui:NO];
}

-(void)startTimerWithEvent:(NSString *)eventName
{
    RELEASE_TO_NIL(timer);
    if (!stopped) {
        if ([[self viewProxy] _hasListeners:eventName checkParent:NO])
        {
            [self.proxy fireEvent:eventName withObject:nil propagate:NO checkForListener:NO];
        }
        
        if ([eventName isEqualToString:@"start"] && previous == nil) {
            //TIMOB-18830. Load the first image immediately
            [self timerFired:nil];
        }
        
        timer = [[NSTimer scheduledTimerWithTimeInterval:interval target:self selector:@selector(timerFired:) userInfo:nil repeats:YES] retain];
    }
}

-(void)stopTimerWithEvent:(NSString *)eventName
{
    if (!stopped) {
        return;
    }
    if (timer != nil) {
        [timer invalidate];
        RELEASE_TO_NIL(timer);
        if ([[self viewProxy] _hasListeners:eventName checkParent:NO])
        {
            [self.proxy fireEvent:eventName withObject:nil propagate:NO checkForListener:NO];
        }
    }
}

-(void)updateTimer{
    if([timer isValid] && !stopped ){
        
        [timer invalidate];
        RELEASE_TO_NIL(timer)
        
        timer = [[NSTimer scheduledTimerWithTimeInterval:interval target:self selector:@selector(timerFired:) userInfo:nil repeats:YES] retain];
    }
}

-(UIImage*)rotatedImage:(UIImage*)originalImage
{
    //If autorotate is set to false and the image orientation is not UIImageOrientationUp create new image
    if (![TiUtils boolValue:[[self proxy] valueForUndefinedKey:@"autorotate"] def:YES] && (originalImage.imageOrientation != UIImageOrientationUp)) {
        UIImage* theImage = [UIImage imageWithCGImage:[originalImage CGImage] scale:[originalImage scale] orientation:UIImageOrientationUp];
        return theImage;
    }
    else {
        return originalImage;
    }
}

-(void)fireLoadEventWithState:(NSString *)stateString
{
    TiUIImageViewProxy* ourProxy = (TiUIImageViewProxy*)self.proxy;
    [ourProxy propagateLoadEvent:stateString];
}

-(void)animationCompleted:(NSString *)animationID finished:(NSNumber *)finished context:(void *)context
{
    for (UIView *view in [self subviews])
    {
        // look for our alpha view which is the placeholder layer
        if (view.alpha == 0)
        {
            [view removeFromSuperview];
            break;
        }
    }
}

-(UIViewContentMode)contentModeForImageView
{
    LayoutConstraint* constraints = [(TiViewProxy*)[self proxy] layoutProperties];
    if (((TiDimensionIsAuto(width) || TiDimensionIsAutoSize(width)) &&
         (TiDimensionIsUndefined(constraints->left) || TiDimensionIsUndefined(constraints->right))) &&
        ((TiDimensionIsAuto(height) || TiDimensionIsAutoSize(height)) &&
         (TiDimensionIsUndefined(constraints->top) || TiDimensionIsUndefined(constraints->bottom)))) {
            return UIViewContentModeScaleAspectFit;
        } else {
            return scaleType;
        }
}

-(void)updateContentMode
{
    UIViewContentMode curMode = [self contentModeForImageView];
    if (imageView != nil) {
        imageView.contentMode = curMode;
    }
    if (container != nil) {
        for (UIView *view in [container subviews]) {
            UIView *child = [[view subviews] count] > 0 ? [[view subviews] objectAtIndex:0] : nil;
            if (child!=nil && [child isKindOfClass:[UIImageView class]])
            {
                child.contentMode = curMode;
            }
        }
    }
}

-(UIImageView *)imageView
{
    if (imageView==nil)
    {
        imageView = [[UIImageView alloc] initWithFrame:[self bounds]];
        [imageView setAutoresizingMask:UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight];
        imageView.backgroundColor = [UIColor clearColor];
        [imageView setContentMode:[self contentModeForImageView]];
        TiUIView* topView = [[self childViews] firstObject];
        if (topView != nil)
            [self insertSubview:imageView belowSubview:topView];
        else {
            [self addSubview:imageView];
        }
    }
    return imageView;
}

//-(UIView*)viewForHitTest
//{
//    return imageView;
//}

- (id)accessibilityElement
{
    return [self imageView];
}

- (id) cloneView:(id)source {
    NSData *archivedViewData = [NSKeyedArchiver archivedDataWithRootObject: source];
    id clone = [NSKeyedUnarchiver unarchiveObjectWithData:archivedViewData];
    return clone;
}

-(void) transitionToImage:(UIImage*)image
{
    ENSURE_UI_THREAD(transitionToImage,image);
    if (self.proxy==nil)
    {
        // this can happen after receiving an async callback for loading the image
        // but after we've detached our view.  In which case, we need to just ignore this
        return;
    }
    image = [self prepareImage:image];
    TiTransition* transition = [TiTransitionHelper transitionFromArg:self.transition containerView:self];
    [(TiViewProxy*)[self proxy] contentsWillChange];
    if (shouldTransition && transition != nil) {
        UIImageView *oldView = [[self imageView] retain];
        RELEASE_TO_NIL(imageView);
        imageView = [[self cloneView:oldView] retain];
        imageView.image = image;
        [self fireLoadEventWithState:@"image"];
        placeholderLoading = NO;
        [TiTransitionHelper transitionFromView:oldView toView:imageView insideView:self withTransition:transition prepareBlock:^{
        } completionBlock:^{
            [oldView release];
        }];
    }
    else {
        [[self imageView] setImage:image];
        [self fireLoadEventWithState:@"image"];
    }
}

-(id)prepareImage:(id)image
{
    UIImage* imageToUse = nil;
    if ([image isKindOfClass:[UIImage class]]) {
        imageToUse = [self rotatedImage:image];
    }
    else if([image isKindOfClass:[TiSVGImage class]]) {
        autoHeight = _svg.size.height;
        autoWidth = _svg.size.width;
        return [_svg imageForSize:[self imageSize]];
    }
    else {
        autoHeight = autoWidth = 0;
        return nil;
    }
    float factor = 1.0f;
    float screenScale = [UIScreen mainScreen].scale;
    if ([TiUtils boolValue:[[self proxy] valueForKey:@"hires"] def:[TiUtils isRetinaDisplay]])
    {
        factor /= screenScale;
    }
    //    CGFloat realWidth = imageToUse.size.width * factor;
    //    CGFloat realHeight = imageToUse.size.height * factor;
    autoWidth = imageToUse.size.width;
    autoHeight = imageToUse.size.height;
    if (_tintColorImage) {
        imageToUse = [imageToUse imageWithRenderingMode:UIImageRenderingModeAlwaysTemplate];
    }
    if (!TiCapIsUndefined(imageCap)) {
        return [TiUtils stretchedImage:imageToUse withCap:imageCap];
    }
    return imageToUse;
}

-(void)loadImageInBackground:(NSNumber*)pos
{
    NSInteger position = [TiUtils intValue:pos];
    NSURL *theurl = [TiUtils toURL:[_images objectAtIndex:position] proxy:self.proxy];
    id theimage = [[ImageLoader sharedLoader] loadImmediateImage:theurl];
    if (theimage==nil)
    {
        theimage = [[ImageLoader sharedLoader] loadRemote:theurl withOptions:[self.proxy valueForUndefinedKey:@"httpOptions"]];
    }
    if (theimage==nil)
    {
        NSLog(@"[ERROR] couldn't load imageview image: %@ at position: %d",theurl,position);
        return;
    }
    
    UIImage *imageToUse = [self prepareImage:[self convertToUIImage:theimage]];
    
    TiThreadPerformOnMainThread(^{
        UIView *view = [[container subviews] objectAtIndex:position];
        UIImageView *newImageView = [[UIImageView alloc] initWithFrame:[view bounds]];
        newImageView.image = imageToUse;
        newImageView.autoresizingMask = UIViewAutoresizingFlexibleHeight | UIViewAutoresizingFlexibleWidth;
        newImageView.contentMode = [self contentModeForImageView];
        
        // remove the spinner now that we've loaded our image
        UIView *spinner = [[view subviews] count] > 0 ? [[view subviews] objectAtIndex:0] : nil;
        if (spinner!=nil && [spinner isKindOfClass:[UIActivityIndicatorView class]])
        {
            [spinner removeFromSuperview];
        }
        [view addSubview:newImageView];
        [self sendSubviewToBack:newImageView];
        view.clipsToBounds = YES;
        [newImageView release];
        view.hidden = YES;
        
#if IMAGEVIEW_DEBUG	== 1
        UILabel *label = [[UILabel alloc] initWithFrame:CGRectMake(10, 10, 50, 20)];
        label.text = [NSString stringWithFormat:@"%d",position];
        label.font = [UIFont boldSystemFontOfSize:28];
        label.textColor = [UIColor redColor];
        label.backgroundColor = [UIColor clearColor];
        [view addSubview:label];
        [view bringSubviewToFront:label];
        [label release];
#endif
        
        loadCount++;
        if (loadCount==loadTotal)
        {
            [self fireLoadEventWithState:@"_images"];
        }
        
        if (ready)
        {
            //NOTE: for now i'm just making sure you have at least one frame loaded before starting the timer
            //but in the future we may want to be more sophisticated
            int min = 1;
            readyCount++;
            if (readyCount >= min)
            {
                readyCount = 0;
                ready = NO;
                
                [self startTimerWithEvent:@"start"];
            }
        }
    }, NO);
}

-(void)removeAllImagesFromContainer
{
    // remove any existing _images
    if (container!=nil)
    {
        for (UIView *view in [container subviews])
        {
            [view removeFromSuperview];
        }
    }
    //	if (imageView!=nil)
    //	{
    //		imageView.image = nil;
    //	}
}

-(void)cancelPendingImageLoads
{
    // cancel a pending request if we have one pending
    //	[(TiUIImageViewProxy *)[self proxy] cancelPendingImageLoads];
    placeholderLoading = NO;
}

-(CGSize) imageSize {
    CGSize _imagesize = CGSizeMake(TiDimensionCalculateValue(width, 0.0),
                                   TiDimensionCalculateValue(height,0.0));
    if ([TiUtils boolValue:[[self proxy] valueForKey:@"hires"]])
    {
        CGFloat scale = [TiUtils screenScale];
        _imagesize.width *= scale;
        _imagesize.height *= scale;
    }
    return _imagesize;
}

-(void)loadDefaultImage
{
    if (!_preventDefaultImage && _defaultImageUrl!=nil)
    {
        UIImage *poster = [[ImageLoader sharedLoader] loadImmediateImage:_defaultImageUrl withSize:[self imageSize]];
        
        [self transitionToImage:poster];
    }
    else {
        [self transitionToImage:nil];
    }
}

-(void)setImageInternal:(id)img
{
    if (img!=nil)
    {
        NSURL* imageURL = [[self proxy] sanitizeURL:img];
        if (![imageURL isKindOfClass:[NSURL class]]) {
            NSLog(@"[ERROR] invalid image type: \"%@\" is not a TiBlob, URL, TiFile",imageURL);
            return;
        }
        NSURL *url_ = [TiUtils toURL:[imageURL absoluteString] proxy:self.proxy];
        
        __block UIImage *image = nil;
        if (localLoadSync)
        {
            image = [self convertToUIImage:[[ImageLoader sharedLoader] loadImmediateImage:url_]];
        }
        
        if (image==nil)
        {
            placeholderLoading = YES;
            shouldTransition = YES;
            [(TiUIImageViewProxy *)[self proxy] startImageLoad:url_];
            return;
        } else {
            [(TiUIImageViewProxy*)[self proxy] setImageURL:url_];
            
            if (_filterOptions) {
                shouldTransition = YES;
                dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^(void)
                               {
                                   RELEASE_TO_NIL(_currentImage);
                                   _currentImage = [[TiImageHelper imageFiltered:image withOptions:_filterOptions] retain];
                                   TiThreadPerformOnMainThread(^{
                                       [self transitionToImage:_currentImage];
                                   }, NO);
                               });
            }
            else {
                RELEASE_TO_NIL(_currentImage);
                _currentImage = [image retain];
                [self transitionToImage:image];
            }
        }
    }
}


-(UIView*)container
{
    if (container==nil)
    {
        // we use a separate container view so we can both have an image
        // and a set of _images
        container = [[UIView alloc] initWithFrame:self.bounds];
        container.userInteractionEnabled = NO;
        [self addSubview:container];
    }
    return container;
}

-(id)convertToUIImage:(id)arg
{
    id image = nil;
    UIImage* imageToUse = nil;
    
    if ([arg isKindOfClass:[TiBlob class]]) {
        TiBlob *blob = (TiBlob*)arg;
        image = [blob image];
    }
    else if ([arg isKindOfClass:[TiFile class]]) {
        TiFile *file = (TiFile*)arg;
        NSURL * fileUrl = [NSURL fileURLWithPath:[file path]];
        image = [[ImageLoader sharedLoader] loadImmediateImage:fileUrl];
    }
    else if ([arg isKindOfClass:[UIImage class]]) {
        // called within this class
        image = (UIImage*)arg;
    }
    else if ([arg isKindOfClass:[TiSVGImage class]]) {
        _svg = [arg retain];
        // called within this class
        image = (TiSVGImage*)arg;
    }
    return image;
}
#pragma mark Public APIs

-(void)animatedImage:(TiAnimatedImage*)animatedImage changedToImage:(UIImage*)image
{
    if (animatedImage.stopped){
        [self transitionToImage:_currentImage];
    }
    else {
        BOOL wasShowingCurrentImage = [self imageView].image == _currentImage;
        if (!animatedImage.paused && !wasShowingCurrentImage) {
            [[self imageView] setImage:[self prepareImage:image]];
        }
        else {
            [self transitionToImage:[self prepareImage:image]];
        }
    }
}

-(void)stop
{
    stopped = YES;
    [self stopTimerWithEvent:@"stop"];
    ready = NO;
    index = -1;
    iterations = -1;
    if (_animatedImage) {
        [_animatedImage stop];
    }
    [self.proxy replaceValue:NUMBOOL(NO) forKey:@"animating" notification:NO];
    [self.proxy replaceValue:NUMBOOL(YES) forKey:@"stopped" notification:NO];
    [self.proxy replaceValue:NUMBOOL(YES) forKey:@"paused" notification:NO];
}

-(void)start
{
    stopped = NO;
    BOOL paused = [TiUtils boolValue:[self.proxy valueForKey:@"paused"] def:NO];
    [self.proxy replaceValue:NUMBOOL(NO) forKey:@"paused" notification:NO];
    [self.proxy replaceValue:NUMBOOL(NO) forKey:@"stopped" notification:NO];
    if (_animatedImage) {
        [self.proxy replaceValue:NUMBOOL(YES) forKey:@"animating" notification:NO];
        [_animatedImage start];
        return;
    }
    if (iterations<0 || !paused)
    {
        iterations = 0;
    }
    
    if (index<0 || !paused)
    {
        if (reverse)
        {
            index = loadTotal-1;
        }
        else
        {
            index = 0;
        }
    }
    
    
    // refuse to start animation if you don't have any images
    if (loadTotal > 0)
    {
        ready = YES;
        [self.proxy replaceValue:NUMBOOL(YES) forKey:@"animating" notification:NO];
        
        if (timer==nil)
        {
            readyCount = 0;
            ready = NO;
            [self startTimerWithEvent:@"start"];
        }
    }
}

-(void)playpause {
    if (_animatedImage) {
        BOOL paused = [_animatedImage paused];
        [self.proxy replaceValue:NUMBOOL(!paused) forKey:@"paused" notification:NO];
        [self.proxy replaceValue:NUMBOOL(paused) forKey:@"animating" notification:NO];
        if (_animatedImage.paused) {
            [_animatedImage resume];
        }
        else {
            [_animatedImage pause];
        }
    }
}

-(void)pause
{
    stopped = YES;
    [self.proxy replaceValue:NUMBOOL(YES) forKey:@"paused" notification:NO];
    [self.proxy replaceValue:NUMBOOL(NO) forKey:@"animating" notification:NO];
    if (_animatedImage) {
        [_animatedImage pause];
        return;
    }
    [self stopTimerWithEvent:@"pause"];
}

-(void)resume
{
    stopped = NO;
    [self.proxy replaceValue:NUMBOOL(NO) forKey:@"paused" notification:NO];
    [self.proxy replaceValue:NUMBOOL(YES) forKey:@"animating" notification:NO];
    if (_animatedImage) {
        [_animatedImage resume];
        return;
    }
    [self startTimerWithEvent:@"resume"];
}

-(id)index_
{
    if (_animatedImage) {
        return @([_animatedImage index]);
    }
    return @(0);
}

-(void)setIndex_:(id)arg
{
    ENSURE_SINGLE_ARG(arg, NSNumber)
    if (_animatedImage) {
        [self.proxy replaceValue:NUMBOOL(NO) forKey:@"animating" notification:NO];
        [self.proxy replaceValue:NUMBOOL(NO) forKey:@"paused" notification:NO];
        [_animatedImage setAnimatedImageAtIndex:[arg intValue]];
        return;
    }
}

-(id)progress_ {
    if (_animatedImage) {
        return @([_animatedImage progress]);
    }
    return @(1);
}


-(void)setProgress_:(id)arg
{
    ENSURE_SINGLE_ARG(arg, NSNumber)
    if (_animatedImage) {
        [self.proxy replaceValue:NUMBOOL(NO) forKey:@"animating" notification:NO];
        [self.proxy replaceValue:NUMBOOL(NO) forKey:@"paused" notification:NO];
        [_animatedImage setProgress:[arg floatValue]];
    }
}

-(void)setWidth_:(id)width_
{
    width = TiDimensionFromObject(width_);
    [self updateContentMode];
}

-(void)setHeight_:(id)height_
{
    height = TiDimensionFromObject(height_);
    [self updateContentMode];
}

-(void)setScaleType_:(id)arg
{
    scaleType = [TiUtils intValue:arg];
    [self updateContentMode];
}

-(void)setLocalLoadSync_:(id)arg
{
    localLoadSync = [TiUtils boolValue:arg];
}

-(void)setOnlyTransitionIfRemote_:(id)arg
{
    onlyTransitionIfRemote = [TiUtils boolValue:arg];
}

-(id)getImage {
    return [[self imageView] image];
}

-(void)setReusing:(BOOL)value
{
    _reusing = value;
}

-(void)setImage_:(id)arg
{
    if (!configurationSet) {
        _needsSetImage = YES;
        //        if (_reusing) {
        //            [self loadDefaultImage];
        //        }
        return;
    }
    _needsSetImage = NO;
    if (_currentImageSource && [_currentImageSource isEqual:arg] && _currentImage) return;
    if ([[self viewProxy] _hasListeners:@"startload" checkParent:NO])
    {
        [self.proxy fireEvent:@"startload" withObject:@{
                                                        @"image":arg
                                                        } propagate:NO checkForListener:NO];
    }
    RELEASE_TO_NIL(_currentImageSource)
    _currentImageSource = [arg retain];
    
    [self removeAllImagesFromContainer];
    [self cancelPendingImageLoads];
    if (_animatedImage) {
        if (_animatedImage.paused)
        {
            [self.proxy replaceValue:NUMBOOL(NO) forKey:@"animating" notification:NO];
            [self.proxy replaceValue:NUMBOOL(NO) forKey:@"paused" notification:NO];
        }
        [_animatedImage stop];
        
    }
    
    shouldTransition = !onlyTransitionIfRemote && !_reusing;
    if (arg==nil || [arg isEqual:@""] || [arg isKindOfClass:[NSNull class]])
    {
        [self loadDefaultImage];
        return;
    }
    
    if (_reusing) {
        [self loadDefaultImage];
    }
    shouldTransition = !onlyTransitionIfRemote;
    
    id image = nil;
    NSURL* imageURL = nil;
    RELEASE_TO_NIL(_svg);
    
    if (localLoadSync || ![arg isKindOfClass:[NSString class]]) {
        image = [self convertToUIImage:arg];
    }
    
    if (image == nil)
    {
        [self setImageInternal:arg];
        return;
    }
    if (_filterOptions) {
        shouldTransition = YES;
        __block id imageSource = _currentImageSource;
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^(void)
                       {
                           if (imageSource != _currentImageSource) return;
                           RELEASE_TO_NIL(_currentImage);
                           _currentImage = [[TiImageHelper imageFiltered:image withOptions:_filterOptions] retain];
                           TiThreadPerformOnMainThread(^{
                               if (imageSource != _currentImageSource) return;
                               [self transitionToImage:_currentImage];
                           }, NO);
                       });
    }
    else {
        RELEASE_TO_NIL(_currentImage);
        _currentImage = [image retain];
        [self transitionToImage:image];
    }
}

-(void)setImages_:(id)args
{
    if (!configurationSet){
        _needsSetImage = YES;
        return;
    }
    BOOL running = (timer!=nil);
    usingNewMethod = NO;
    
    [self stop];
    
    if (imageView!=nil)
    {
        [imageView setImage:nil];
    }
    
    // remove any existing _images
    [self removeAllImagesFromContainer];
    
    RELEASE_TO_NIL(_images);
    ENSURE_TYPE_OR_NIL(args,NSArray);
    
    if (args!=nil)
    {
        [self container];
        _images = [[NSMutableArray alloc] initWithCapacity:[args count]];
        loadTotal = [args count];
        for (NSUInteger c = 0; c < [args count]; c++)
        {
            [self queueImage:[args objectAtIndex:c] index:c];
        }
    }
    else
    {
        RELEASE_TO_NIL(container);
    }
    
    // if we were running, re-start it
    if (running)
    {
        [self start];
    }
}

-(void)setDuration_:(id)duration
{
    float dur = [TiUtils floatValue:duration];
    dur =  MAX(IMAGEVIEW_MIN_INTERVAL,dur);
    
    interval = dur/1000;
    
    [self updateTimer];
}

-(void)setRepeatCount_:(id)count
{
    repeatCount = [TiUtils intValue:count];
    [self imageView].animationRepeatCount = [TiUtils intValue:count];
}


-(void)setReverse_:(id)value
{
    reverse = [TiUtils boolValue:value def:reverse];
    if (_animatedImage) {
        _animatedImage.reverse = reverse;
    }
}

-(void)setAutoreverse_:(id)value
{
    autoreverse = [TiUtils boolValue:value];
    if (_animatedImage) {
        _animatedImage.autoreverse = autoreverse;
    }
}

-(void)setAnimatedImages_:(id)args
{
    if (!configurationSet){
        _needsSetImage = YES;
        return;
    }
    ENSURE_TYPE_OR_NIL(args,NSArray);
    if (args == nil) {
        RELEASE_TO_NIL(_animatedImage);
        [self transitionToImage:_currentImage];
        return;
    }
    
    NSMutableArray* images = [[NSMutableArray alloc] initWithCapacity:[args count]];
    NSMutableArray* durations = [[NSMutableArray alloc] initWithCapacity:[args count]];
    id anObject;
    NSEnumerator *enumerator = [args objectEnumerator];
    while (anObject = [enumerator nextObject]) {
        [images addObject:[self loadImage:anObject]];
        [durations addObject:NUMFLOAT(interval)];
    }
    _animatedImage = [[TiAnimatedImage alloc] initWithImages:images andDurations:durations];
    [durations release];
    [images release];
    _animatedImage.reverse = reverse;
    _animatedImage.autoreverse = autoreverse;
    _animatedImage.delegate = self;
    [_animatedImage reset];
    if ([TiUtils boolValue:[[self proxy] valueForKey:@"animating"]]) {
        [_animatedImage resume];
    }
}

-(void)setImageMask_:(id)arg
{
    UIImage* image = [self loadImage:arg];
    UIImageView *imageview = [self imageView];
    if (image == nil) {
        imageview.layer.mask = nil;
    }
    else {
        if (imageview.layer.mask == nil) {
            imageview.layer.mask = [CALayer layer];
            imageview.layer.mask.frame = self.layer.bounds;
        }
        imageview.layer.opaque = NO;
        imageview.layer.mask.contentsScale = [image scale];
        imageview.layer.mask.magnificationFilter = @"nearest";
        imageview.layer.mask.contents = (id)image.CGImage;
    }
    
    [imageview.layer setNeedsDisplay];
}

-(void)setTransition_:(id)arg
{
    ENSURE_SINGLE_ARG_OR_NIL(arg, NSDictionary)
    self.transition = arg;
}

-(void)configurationSet
{
    [super configurationSet];
    if (_needsSetImage) {
        [self setImage_:[self.proxy valueForKey:@"image"]];
        if ([self.proxy valueForKey:@"images"]) {
            [self setImages_:[self.proxy valueForKey:@"images"]];
        } else if ([self.proxy valueForKey:@"animatedImages"]) {
            [self setAnimatedImages_:[self.proxy valueForKey:@"animatedImages"]];
        }
    }
}

-(void)setPreventDefaultImage_:(id)value
{
    _preventDefaultImage = [TiUtils boolValue:value];
    if (configurationSet) [self setImage_:[self.proxy valueForKey:@"image"]];
}

-(void)setDefaultImage_:(id)value
{
    RELEASE_TO_NIL(_defaultImageUrl)
    _defaultImageUrl = [[TiUtils toURL:value proxy:self.proxy] retain];
    if (configurationSet) {
        [self setImage_:[self.proxy valueForKey:@"image"]];
    } else if (_defaultImageUrl) {
        _needsSetImage = YES;
    }
}

-(void)setFilterOptions_:(id)value
{
    RELEASE_TO_NIL(_filterOptions)
    _filterOptions = [value retain];
    if (configurationSet) [self setImage_:[self.proxy valueForKey:@"image"]];
}
#pragma mark ImageLoader delegates

-(void)imageLoadSuccess:(ImageLoaderRequest*)request image:(id)image
{
    RELEASE_TO_NIL(_currentImage);
    shouldTransition = YES;
    _currentImage = [image retain];
    if (_filterOptions) {
        __block id imageSource = _currentImageSource;
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^(void)
                       {
                           if (imageSource != _currentImageSource) return;
                           RELEASE_TO_NIL(_currentImage);
                           _currentImage = [[TiImageHelper imageFiltered:[self convertToUIImage:image] withOptions:_filterOptions] retain];
                           TiThreadPerformOnMainThread(^{
                               if (imageSource != _currentImageSource) return;
                               [self transitionToImage:_currentImage];
                           }, NO);
                       });
        
    }
    else {
        TiThreadPerformOnMainThread(^{
            [self transitionToImage:[self convertToUIImage:image]];
        }, NO);
    }
    
}

-(void)imageLoadFailed:(ImageLoaderRequest*)request error:(NSError*)error
{
    NSLog(@"[ERROR] Failed to load image: %@, Error: %@",[request url], error);
    // NOTE: Loading from URL means we can't pre-determine any % value.
    [self loadDefaultImage];
}

@end

#endif
