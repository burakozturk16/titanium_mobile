/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UILISTVIEW

#import "TiUIListItem.h"
#import "TiUtils.h"
#import "TiViewProxy.h"
#import "ImageLoader.h"
#import "Webcolor.h"
#import "TiSelectableBackgroundLayer.h"

#define GROUP_ROUND_RADIUS 8

@implementation TiUIListItem {
	TiUIListItemProxy *_proxy;
	NSInteger _templateStyle;
	NSMutableDictionary *_initialValues;
	NSMutableDictionary *_currentValues;
	NSMutableSet *_resetKeys;
	NSDictionary *_dataItem;
	NSDictionary *_bindings;
    int _positionMask;
    BOOL _grouped;
    TiSelectableBackgroundLayer* _bgLayer;
}

@synthesize templateStyle = _templateStyle;
@synthesize proxy = _proxy;
@synthesize dataItem = _dataItem;

- (id)initWithStyle:(UITableViewCellStyle)style position:(int)position grouped:(BOOL)grouped reuseIdentifier:(NSString *)reuseIdentifier proxy:(TiUIListItemProxy *)proxy
{
    self = [super initWithStyle:style reuseIdentifier:reuseIdentifier];
    if (self) {
		_templateStyle = style;
		_initialValues = [[NSMutableDictionary alloc] initWithCapacity:5];
		_currentValues = [[NSMutableDictionary alloc] initWithCapacity:5];
		_resetKeys = [[NSMutableSet alloc] initWithCapacity:5];
		_proxy = [proxy retain];
        self.contentView.backgroundColor = [UIColor grayColor];
		_proxy.listItem = self;
        _grouped = grouped;
        _positionMask = position;
    }
    return self;
}

- (id)initWithProxy:(TiUIListItemProxy *)proxy position:(int)position grouped:(BOOL)grouped reuseIdentifier:(NSString *)reuseIdentifier
{
    self = [super initWithStyle:UITableViewCellStyleDefault reuseIdentifier:reuseIdentifier];
    if (self) {
		_templateStyle = TiUIListItemTemplateStyleCustom;
		_initialValues = [[NSMutableDictionary alloc] initWithCapacity:10];
		_currentValues = [[NSMutableDictionary alloc] initWithCapacity:10];
		_resetKeys = [[NSMutableSet alloc] initWithCapacity:10];
		_proxy = [proxy retain];
		_proxy.listItem = self;
        _grouped = grouped;
        _positionMask = position;
        [self applyCellProps:[_proxy allProperties]];
    }
    return self;
}

- (void)dealloc
{
	_proxy.listItem = nil;
	[_initialValues release];
	[_currentValues release];
	[_resetKeys release];
	[_dataItem release];
	[_proxy release];
	[_bindings release];
    [_bgLayer removeFromSuperlayer];
    [_bgLayer release];
	[super dealloc];
}

- (NSDictionary *)bindings
{
	if (_bindings == nil) {
		NSMutableDictionary *dict = [[NSMutableDictionary alloc] initWithCapacity:10];
		[[self class] buildBindingsForViewProxy:_proxy intoDictionary:dict];
		_bindings = [dict copy];
		[dict release];
	}
	return _bindings;
}

- (void)prepareForReuse
{
	RELEASE_TO_NIL(_dataItem);
	[super prepareForReuse];
}

- (void)layoutSubviews
{
	[super layoutSubviews];
    if (_bgLayer != nil) {
        if (!CGRectEqualToRect(_bgLayer.bounds, self.contentView.frame)) {
            _bgLayer.frame = self.contentView.bounds;
        }
        [self.contentView.layer insertSublayer:_bgLayer atIndex:0];
    }
    
	if (_templateStyle == TiUIListItemTemplateStyleCustom) {
		// prevent any crashes that could be caused by unsupported layouts
		_proxy.layoutProperties->layoutStyle = TiLayoutRuleAbsolute;
		[_proxy layoutChildren:NO];
	}
}

-(void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
    if ( ([[event touchesForView:self.contentView] count] > 0) || ([[event touchesForView:self.accessoryView] count] > 0)
        || ([[event touchesForView:self.imageView] count] > 0) || ([[event touchesForView:self.proxy.view] count]> 0 )) {
        if ([_proxy _hasListeners:@"touchstart"])
        {
        	UITouch *touch = [touches anyObject];
            NSDictionary *evt = [TiUtils dictionaryFromTouch:touch inView:self];
            [_proxy fireEvent:@"touchstart" withObject:evt propagate:YES];
        }
    }
    
    [super touchesBegan:touches withEvent:event];
}

- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event
{
    if ([[event touchesForView:self.contentView] count] > 0 || ([[event touchesForView:self.accessoryView] count] > 0)
        || ([[event touchesForView:self.imageView] count] > 0) || ([[event touchesForView:self.proxy.view] count]> 0 )) {
        if ([_proxy _hasListeners:@"touchmove"])
        {
            UITouch *touch = [touches anyObject];
            NSDictionary *evt = [TiUtils dictionaryFromTouch:touch inView:self];
            [_proxy fireEvent:@"touchmove" withObject:evt propagate:YES];
        }
    }
    [super touchesMoved:touches withEvent:event];
}

-(void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
    if ( ([[event touchesForView:self.contentView] count] > 0) || ([[event touchesForView:self.accessoryView] count] > 0)
        || ([[event touchesForView:self.imageView] count] > 0) || ([[event touchesForView:self.proxy.view] count]> 0 )) {
        if ([_proxy _hasListeners:@"touchend"])
        {
            UITouch *touch = [touches anyObject];
            NSDictionary *evt = [TiUtils dictionaryFromTouch:touch inView:self];
            [_proxy fireEvent:@"touchend" withObject:evt propagate:YES];
        }
    }
    [super touchesEnded:touches withEvent:event];
}

-(void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event
{
    [super touchesCancelled:touches withEvent:event];
}

#pragma mark - Background Support
-(BOOL) selectedOrHighlighted
{
	return [self isSelected] || [self isHighlighted];
}

-(void)unHighlight:(NSArray*)views
{
    for (UIView *subview in views) {
        if ([subview isKindOfClass:[UIButton class] ])
        {
            [(UIButton*)subview setHighlighted:NO];
        }
        // Get the subviews of the view
        NSArray *subviews = [subview subviews];
        if ([subviews count] > 0)
            [self unHighlight:subviews];
    }
}

-(void)setHighlighted:(BOOL)yn
{
    [super setHighlighted:yn];
    [self unHighlight:[self subviews]];
}

-(void)setSelected:(BOOL)yn
{
    [super setSelected:yn];
    [self unHighlight:[self subviews]];
}


-(void)setSelected:(BOOL)yn animated:(BOOL)animated
{
    [super setSelected:yn animated:animated];
    [self unHighlight:[self subviews]];
    if (_bgLayer != nil) {
        BOOL realSelected = yn|[self isHighlighted];
        UIControlState state = realSelected?UIControlStateSelected:UIControlStateNormal;
        [_bgLayer setState:state];
    }
}

-(void)setHighlighted:(BOOL)yn animated:(BOOL)animated
{
    [super setHighlighted:yn animated:animated];
    [self unHighlight:[self subviews]];
    if (_bgLayer != nil) {
        BOOL realSelected = yn|[self isHighlighted];
        UIControlState state = realSelected?UIControlStateSelected:UIControlStateNormal;
        [_bgLayer setState:state];
    }
}

-(void)setBackgroundOpacity_:(id)opacity
{
    CGFloat backgroundOpacity = [TiUtils floatValue:opacity def:1.0f];
    if (_bgLayer!=nil) {
        _bgLayer.opacity = backgroundOpacity;
    }
}

-(void)setBorderRadius_:(id)radius
{
	self.layer.cornerRadius = [TiUtils floatValue:radius];
    if (_bgLayer!=nil) {
        _bgLayer.cornerRadius= self.layer.cornerRadius;
    }
}

-(void) setBackgroundGradient_:(id)newGradientDict
{
    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
    [[self getOrCreateCustomBackgroundLayer] setGradient:newGradient forState:UIControlStateNormal];
}

-(void) setSelectedBackgroundGradient_:(id)newGradientDict
{
    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
    [[self getOrCreateCustomBackgroundLayer] setGradient:newGradient forState:UIControlStateSelected];
    self.selectionStyle = UITableViewCellSelectionStyleNone;
}

-(void) setBackgroundColor_:(id)color
{
    self.backgroundColor = [TiUtils colorValue:color].color;
}

-(void) setSelectedBackgroundColor_:(id)color
{
    UIColor* uiColor = [TiUtils colorValue:color].color;
    if (uiColor == nil) {
        switch (self.selectionStyle) {
            case UITableViewCellSelectionStyleBlue:uiColor = [Webcolor webColorNamed:@"#0272ed"];break;
            case UITableViewCellSelectionStyleGray:uiColor = [Webcolor webColorNamed:@"#bbb"];break;
            case UITableViewCellSelectionStyleNone:uiColor = [UIColor clearColor];break;
        }
    }
    [[self getOrCreateCustomBackgroundLayer] setColor:uiColor forState:UIControlStateSelected];
    self.selectionStyle = UITableViewCellSelectionStyleNone;
}


-(void) setBackgroundImage_:(id)image
{
    UIImage* bgImage = [TiUtils loadBackgroundImage:image forProxy:_proxy];
    [[self getOrCreateCustomBackgroundLayer] setImage:bgImage forState:UIControlStateNormal];
}

-(void) setSelectedBackgroundImage_:(id)image
{
    UIImage* bgImage = [TiUtils loadBackgroundImage:image forProxy:_proxy];
    [[self getOrCreateCustomBackgroundLayer] setImage:bgImage forState:UIControlStateSelected];
    self.selectionStyle = UITableViewCellSelectionStyleNone;
}

-(void)setPosition:(int)position isGrouped:(BOOL)grouped
{
    _positionMask = position;
    _grouped = grouped;
    [self updateBackgroundLayerCorners];
    [self setNeedsDisplay];
}

-(void) updateBackgroundLayerCorners {
    if (_bgLayer == nil) {
        return;
    }
    if (_grouped) {
        UIRectCorner corners = -1;
        switch (_positionMask) {
            case TiGroupedListItemPositionBottom:
                corners = UIRectCornerBottomRight | UIRectCornerBottomLeft;
                break;
            case TiGroupedListItemPositionTop:
                corners = UIRectCornerTopRight | UIRectCornerTopLeft;
                break;
            case TiGroupedListItemPositionSingleLine:
                corners = UIRectCornerAllCorners;
                break;
            default:
                break;
        }
        if (corners != -1) {
            [_bgLayer setRoundedRadius:GROUP_ROUND_RADIUS inCorners:corners];
        }
    }
}

-(TiSelectableBackgroundLayer*)getOrCreateCustomBackgroundLayer
{
    if (_bgLayer != nil) {
        return _bgLayer;
    }
    
    _bgLayer = [[TiSelectableBackgroundLayer alloc] init];
    _bgLayer.frame = self.contentView.bounds;
    [self.contentView.layer insertSublayer:_bgLayer atIndex:0];
    
    [self updateBackgroundLayerCorners];

    CGFloat backgroundOpacity = [TiUtils floatValue:[_proxy valueForKey:@"backgroundOpacity"] def:1.0f];
    _bgLayer.opacity = backgroundOpacity;
    return _bgLayer;
}

- (BOOL)canApplyDataItem:(NSDictionary *)otherItem;
{
	id template = [_dataItem objectForKey:@"template"];
	id otherTemplate = [otherItem objectForKey:@"template"];
	BOOL same = (template == otherTemplate) || [template isEqual:otherTemplate];
	if (same) {
		id propertiesValue = [_dataItem objectForKey:@"properties"];
		NSDictionary *properties = ([propertiesValue isKindOfClass:[NSDictionary class]]) ? propertiesValue : nil;
		id heightValue = [properties objectForKey:@"height"];
		
		propertiesValue = [otherItem objectForKey:@"properties"];
		properties = ([propertiesValue isKindOfClass:[NSDictionary class]]) ? propertiesValue : nil;
		id otherHeightValue = [properties objectForKey:@"height"];
		same = (heightValue == otherHeightValue) || [heightValue isEqual:otherHeightValue];
	}
	return same;
}

- (void) applyCellProps:(NSDictionary *)properties
{
    //Here we treat all properties that can come from the template or the listview
    NSArray* keys = [properties allKeys];
    NSDictionary* currentProps = [_proxy allProperties];
    
    
    if ([keys containsObject:@"accessoryType"]) {
        id accessoryTypeValue = [properties objectForKey:@"accessoryType"];
        if ([self shouldUpdateValue:accessoryTypeValue forKeyPath:@"accessoryType"]) {
            if ([accessoryTypeValue isKindOfClass:[NSNumber class]]) {
                UITableViewCellAccessoryType accessoryType = [accessoryTypeValue unsignedIntegerValue];
                [self recordChangeValue:accessoryTypeValue forKeyPath:@"accessoryType" withBlock:^{
                    self.accessoryType = accessoryType;
                }];
            }
        }
    }
    if ([keys containsObject:@"selectionStyle"]) {
        id selectionStyleValue = [properties objectForKey:@"selectionStyle"];
        if ([self shouldUpdateValue:selectionStyleValue forKeyPath:@"selectionStyle"]) {
            if ([selectionStyleValue isKindOfClass:[NSNumber class]]) {
                UITableViewCellSelectionStyle selectionStyle = [selectionStyleValue unsignedIntegerValue];
                [self recordChangeValue:selectionStyleValue forKeyPath:@"selectionStyle" withBlock:^{
                    self.selectionStyle = selectionStyle;
                }];
            }
        }
	}
    if ([keys containsObject:@"selectedBackgroundGradient"]) {
        id value = [properties objectForKey:@"selectedBackgroundGradient"];
        [self setSelectedBackgroundGradient_:value];
    }
    if ([keys containsObject:@"selectedBackgroundColor"]) {
        id value = [properties objectForKey:@"selectedBackgroundColor"];
        [self setSelectedBackgroundColor_:value];
    }
    if ([keys containsObject:@"selectedBackgroundImage"]) {
        id value = [properties objectForKey:@"selectedBackgroundImage"];
        [self setSelectedBackgroundImage_:value];
    }
    
}

- (void)setDataItem:(NSDictionary *)dataItem
{
	_dataItem = [dataItem retain];
	[_resetKeys addObjectsFromArray:[_currentValues allKeys]];
	id propertiesValue = [dataItem objectForKey:@"properties"];
	NSDictionary *properties = ([propertiesValue isKindOfClass:[NSDictionary class]]) ? propertiesValue : nil;
	switch (_templateStyle) {
		case UITableViewCellStyleSubtitle:
		case UITableViewCellStyleValue1:
		case UITableViewCellStyleValue2:
			self.detailTextLabel.text = [[properties objectForKey:@"subtitle"] description];
            self.detailTextLabel.backgroundColor = [UIColor clearColor];
			// pass through
		case UITableViewCellStyleDefault:
			self.textLabel.text = [[properties objectForKey:@"title"] description];
            self.textLabel.backgroundColor = [UIColor clearColor];
			if (_templateStyle != UITableViewCellStyleValue2) {
				id imageValue = [properties objectForKey:@"image"];
				if ([self shouldUpdateValue:imageValue forKeyPath:@"imageView.image"]) {
					NSURL *imageUrl = [TiUtils toURL:imageValue proxy:_proxy];
					UIImage *image = [[ImageLoader sharedLoader] loadImmediateImage:imageUrl];
					if (image != nil) {
						[self recordChangeValue:imageValue forKeyPath:@"imageView.image" withBlock:^{
							self.imageView.image = image;
						}];
					}
				}
			}

			id fontValue = [properties objectForKey:@"font"];
			if ([self shouldUpdateValue:fontValue forKeyPath:@"textLabel.font"]) {
				UIFont *font = (fontValue != nil) ? [[TiUtils fontValue:fontValue] font] : nil;
				if (font != nil) {
					[self recordChangeValue:fontValue forKeyPath:@"textLabel.font" withBlock:^{
						[self.textLabel setFont:font];
					}];
				}
			}

			id colorValue = [properties objectForKey:@"color"];
			if ([self shouldUpdateValue:colorValue forKeyPath:@"textLabel.color"]) {
				UIColor *color = colorValue != nil ? [[TiUtils colorValue:colorValue] _color] : nil;
				if (color != nil) {
					[self recordChangeValue:colorValue forKeyPath:@"textLabel.color" withBlock:^{
						[self.textLabel setTextColor:color];
					}];
				}
			}
			break;
			
		default:
			[dataItem enumerateKeysAndObjectsUsingBlock:^(NSString *bindId, id dict, BOOL *stop) {
				if (![dict isKindOfClass:[NSDictionary class]] || [bindId isEqualToString:@"properties"]) {
					return;
				}
				id bindObject = [self valueForUndefinedKey:bindId];
				if (bindObject != nil) {
					BOOL reproxying = NO;
					if ([bindObject isKindOfClass:[TiProxy class]]) {
						[bindObject setReproxying:YES];
						reproxying = YES;
					}
					[(NSDictionary *)dict enumerateKeysAndObjectsUsingBlock:^(NSString *key, id value, BOOL *stop) {
						NSString *keyPath = [NSString stringWithFormat:@"%@.%@", bindId, key];
						if ([self shouldUpdateValue:value forKeyPath:keyPath]) {
							[self recordChangeValue:value forKeyPath:keyPath withBlock:^{
								[bindObject setValue:value forKey:key];
							}];
						}
					}];
					if (reproxying) {
						[bindObject setReproxying:NO];
					}
				}
			}];
			break;
	}
    
    [self applyCellProps:properties];
    
	[_resetKeys enumerateObjectsUsingBlock:^(NSString *keyPath, BOOL *stop) {
		id value = [_initialValues objectForKey:keyPath];
		[self setValue:(value != [NSNull null] ? value : nil) forKeyPath:keyPath];
		[_currentValues removeObjectForKey:keyPath];
	}];
	[_resetKeys removeAllObjects];
}

- (id)valueForUndefinedKey:(NSString *)key
{
	return [self.bindings objectForKey:key];
}

- (void)recordChangeValue:(id)value forKeyPath:(NSString *)keyPath withBlock:(void(^)(void))block
{
	if ([_initialValues objectForKey:keyPath] == nil) {
		id initialValue = [self valueForKeyPath:keyPath];
		[_initialValues setObject:(initialValue != nil ? initialValue : [NSNull null]) forKey:keyPath];
	}
	block();
	if (value != nil) {
		[_currentValues setObject:value forKey:keyPath];
	} else {
		[_currentValues removeObjectForKey:keyPath];
	}
	[_resetKeys removeObject:keyPath];
}

- (BOOL)shouldUpdateValue:(id)value forKeyPath:(NSString *)keyPath
{
	id current = [_currentValues objectForKey:keyPath];
	BOOL sameValue = ((current == value) || [current isEqual:value]);
	if (sameValue) {
		[_resetKeys removeObject:keyPath];
	}
	return !sameValue;
}

#pragma mark - Static 

+ (void)buildBindingsForViewProxy:(TiViewProxy *)viewProxy intoDictionary:(NSMutableDictionary *)dict
{
	[viewProxy.children enumerateObjectsUsingBlock:^(TiViewProxy *childViewProxy, NSUInteger idx, BOOL *stop) {
		[[self class] buildBindingsForViewProxy:childViewProxy intoDictionary:dict];
	}];
	id bindId = [viewProxy valueForKey:@"bindId"];
	if (bindId != nil) {
		[dict setObject:viewProxy forKey:bindId];
	}
}

@end

#endif
