/**
 * Akylas
 * Copyright (c) 2009-2010 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiUIVolumeView.h"
#import <MediaPlayer/MPVolumeView.h>
#import "ImageLoader.h"
#import "UIControl+TiUIView.h"
#import "TiViewProxy.h"

@implementation TiUIVolumeView{
@private
    MPVolumeView *volumeView;
    UISlider *volumeSlider;
    UIControlState thumbImageState;
    UIControlState rightTrackImageState;
    UIControlState leftTrackImageState;
    TiCap leftTrackCap;
    TiCap rightTrackCap;
    
    NSDate* lastTouchUp;
    NSTimeInterval lastTimeInterval;
}

-(void)dealloc
{
    if (volumeSlider) {
        [volumeSlider removeTarget:self action:@selector(sliderChanged:) forControlEvents:UIControlEventValueChanged];
        [volumeSlider removeTarget:self action:@selector(sliderBegin:) forControlEvents:UIControlEventTouchDown];
        [volumeSlider removeTarget:self action:@selector(sliderEnd:) forControlEvents:(UIControlEventTouchUpInside | UIControlEventTouchUpOutside | UIControlEventTouchCancel)];
        volumeSlider = nil;
    }
    RELEASE_TO_NIL(volumeView);
    RELEASE_TO_NIL(lastTouchUp);
    [super dealloc];
}

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
{
    [[self volumeView] setFrame:[self bounds]];
    [super frameSizeChanged:frame bounds:bounds];
    [self setCenter:[self center]];
}

-(MPVolumeView*)volumeView
{
    if (volumeView==nil)
    {
        volumeView = [[MPVolumeView alloc] initWithFrame:[self bounds]];
        volumeView.showsRouteButton = YES;
        volumeView.showsVolumeSlider = YES;
        
        for (UIView *view in [volumeView subviews]) {
            if (IS_OF_CLASS(view, UISlider)) {
                volumeSlider = (UISlider*)view;
                break;
            }
        }
        if (volumeSlider) {
            [volumeSlider addTarget:self action:@selector(sliderChanged:) forControlEvents:UIControlEventValueChanged];
            [volumeSlider addTarget:self action:@selector(sliderBegin:) forControlEvents:UIControlEventTouchDown];
            [volumeSlider addTarget:self action:@selector(sliderEnd:) forControlEvents:(UIControlEventTouchUpInside | UIControlEventTouchUpOutside | UIControlEventTouchCancel)];
            [volumeSlider setTiUIView:self];
        }

        [self addSubview:volumeView];
        
        thumbImageState = UIControlStateNormal;
        leftTrackImageState = UIControlStateNormal;
        rightTrackImageState = UIControlStateNormal;
    }
    return volumeView;
}

- (void) initialize
{
    [super initialize];
    //by default do not mask to bounds to show the thumb shadow
    self.layer.masksToBounds = NO;
}


#pragma mark Accessors

- (id)accessibilityElement
{
    return [self volumeView];
}

-(void)setThumb:(id)value forState:(UIControlState)state
{
    [[self volumeView] setVolumeThumbImage:[TiUtils image:value proxy:[self proxy]] forState:state];
}

-(void)setRightTrack:(id)value forState:(UIControlState)state
{
    NSURL *url = [TiUtils toURL:value proxy:[self proxy]];
    if (url==nil)
    {
        DebugLog(@"[WARN] could not find image: %@",[url absoluteString]);
        return;
    }
    
    UIImage* ret = [[ImageLoader sharedLoader] loadImmediateStretchableImage:url withCap:rightTrackCap];
    
    [[self volumeView] setMaximumVolumeSliderImage:ret forState:state];
}

-(void)setLeftTrack:(id)value forState:(UIControlState)state
{
    NSURL *url = [TiUtils toURL:value proxy:[self proxy]];
    if (url==nil)
    {
        DebugLog(@"[WARN] could not find image: %@",[url absoluteString]);
        return;
    }
    
    UIImage* ret = [[ImageLoader sharedLoader] loadImmediateStretchableImage:url withCap:leftTrackCap];
    
    
    [[self volumeView] setMinimumVolumeSliderImage:ret forState:state];
}

#pragma mark View controller stuff

-(void)setShowsRouteButton_:(id)value
{
    [[self volumeView] setShowsRouteButton:[TiUtils boolValue:value def:YES]];
}

-(void)setShowsVolumeSlider_:(id)value
{
    [[self volumeView] setShowsVolumeSlider:[TiUtils boolValue:value def:YES]];
}

-(void)setThumbImage_:(id)value
{
    [self setThumb:value forState:UIControlStateNormal];
    
    if ((thumbImageState & UIControlStateSelected)==0) {
        [self setThumb:value forState:UIControlStateSelected];
    }
    if ((thumbImageState & UIControlStateHighlighted)==0) {
        [self setThumb:value forState:UIControlStateHighlighted];
    }
    if ((thumbImageState & UIControlStateDisabled)==0) {
        [self setThumb:value forState:UIControlStateDisabled];
    }
}

-(void)setSelectedThumbImage_:(id)value
{
    [self setThumb:value forState:UIControlStateSelected];
    thumbImageState = thumbImageState | UIControlStateSelected;
}

-(void)setHighlightedThumbImage_:(id)value
{
    [self setThumb:value forState:UIControlStateHighlighted];
    thumbImageState = thumbImageState | UIControlStateHighlighted;
}

-(void)setDisabledThumbImage_:(id)value
{
    [self setThumb:value forState:UIControlStateDisabled];
    thumbImageState = thumbImageState | UIControlStateSelected;
}


-(void)setLeftTrackImage_:(id)value
{
    [self setLeftTrack:value forState:UIControlStateNormal];
    
    if ((leftTrackImageState & UIControlStateSelected)==0) {
        [self setLeftTrack:value forState:UIControlStateSelected];
    }
    if ((leftTrackImageState & UIControlStateHighlighted)==0) {
        [self setLeftTrack:value forState:UIControlStateHighlighted];
    }
    if ((leftTrackImageState & UIControlStateDisabled)==0) {
        [self setLeftTrack:value forState:UIControlStateDisabled];
    }
}

-(void)setSelectedLeftTrackImage_:(id)value
{
    [self setLeftTrack:value forState:UIControlStateSelected];
    leftTrackImageState = leftTrackImageState | UIControlStateSelected;
}

-(void)setHighlightedLeftTrackImage_:(id)value
{
    [self setLeftTrack:value forState:UIControlStateHighlighted];
    leftTrackImageState = leftTrackImageState | UIControlStateHighlighted;
}

-(void)setDisabledLeftTrackImage_:(id)value
{
    [self setLeftTrack:value forState:UIControlStateDisabled];
    leftTrackImageState = leftTrackImageState | UIControlStateDisabled;
}


-(void)setRightTrackImage_:(id)value
{
    [self setRightTrack:value forState:UIControlStateNormal];
    
    if ((rightTrackImageState & UIControlStateSelected)==0) {
        [self setRightTrack:value forState:UIControlStateSelected];
    }
    if ((rightTrackImageState & UIControlStateHighlighted)==0) {
        [self setRightTrack:value forState:UIControlStateHighlighted];
    }
    if ((rightTrackImageState & UIControlStateDisabled)==0) {
        [self setRightTrack:value forState:UIControlStateDisabled];
    }
}

-(void)setSelectedRightTrackImage_:(id)value
{
    [self setRightTrack:value forState:UIControlStateSelected];
    rightTrackImageState = rightTrackImageState | UIControlStateSelected;
}

-(void)setHighlightedRightTrackImage_:(id)value
{
    [self setRightTrack:value forState:UIControlStateHighlighted];
    rightTrackImageState = rightTrackImageState | UIControlStateHighlighted;
}

-(void)setDisabledRightTrackImage_:(id)value
{
    [self setRightTrack:value forState:UIControlStateDisabled];
    rightTrackImageState = rightTrackImageState | UIControlStateDisabled;
}

-(void)setLeftTrackCap_:(id)arg
{
    leftTrackCap = [TiUtils capValue:arg def:TiCapUndefined];
}

-(void)setRightTrackCap_:(id)arg
{
    rightTrackCap = [TiUtils capValue:arg def:TiCapUndefined];
}

-(CGFloat)verifyHeight:(CGFloat)suggestedHeight
{
    if (suggestedHeight == 0) {
        CGFloat result = [[self volumeView] sizeThatFits:CGSizeZero].height;
        
        //IOS7 DP3 sizeThatFits always returns zero for regular slider
        if (result == 0) {
            result = 30.0;
        }
        return result;
    }
    return suggestedHeight;
}

USE_PROXY_FOR_VERIFY_AUTORESIZING

#pragma mark Delegates

- (IBAction)sliderChanged:(id)sender
{
    if ([(TiViewProxy*)self.proxy _hasListeners:@"change" checkParent:NO])
    {
        [self.proxy fireEvent:@"change" withObject:@{
                                                     @"value":@([(UISlider*)sender value]),
                                                     @"max":@(1.0)
                                                     } propagate:NO checkForListener:NO];
    }
}

-(IBAction)sliderBegin:(id)sender
{
    if ([[self viewProxy] _hasListeners:@"start" checkParent:NO])
    {
        [[self proxy] fireEvent:@"start" withObject:@{
                                                      @"value":@([(UISlider*)sender value]),
                                                      @"max":@(1.0)
                                                      } propagate:NO checkForListener:NO];
    }
}

-(IBAction)sliderEnd:(id)sender
{
    // APPLE BUG: Sometimes in a double-click our 'UIControlEventTouchUpInside' event is fired more than once.  This is
    // ALWAYS indicated by a sub-0.1s difference between the clicks, and results in an additional fire of the event.
    // We have to track the PREVIOUS (not current) inverval and prevent these ugly misfires!
    NSDate* now = [[NSDate alloc] init];
    NSTimeInterval currentTimeInterval = [now timeIntervalSinceDate:lastTouchUp];
    if (!(lastTimeInterval < 0.1 && currentTimeInterval < 0.1)) {
        if ([[self viewProxy] _hasListeners:@"stop" checkParent:NO])
        {
            [[self proxy] fireEvent:@"stop" withObject:@{
                                                         @"value":@([(UISlider*)sender value]),
                                                         @"max":@(1.0)
                                                         } propagate:NO checkForListener:NO];
        }
    }
    lastTimeInterval = currentTimeInterval;
    RELEASE_TO_NIL(lastTouchUp);
    lastTouchUp = now;
    
}


@end
