/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UILISTVIEW

#import "TiUIListItemProxy.h"
#import "TiUtils.h"
#import "TiUIListItem.h"
#import "TiUIListViewProxy.h"
#import "ImageLoader.h"

#define CHILD_ACCESSORY_WIDTH 20.0
#define CHECK_ACCESSORY_WIDTH 20.0
#define DETAIL_ACCESSORY_WIDTH 33.0
#define IOS7_ACCESSORY_EXTRA_OFFSET 15.0

static void SetEventOverrideDelegateRecursive(NSArray *children, id<TiViewEventOverrideDelegate> eventOverrideDelegate);

@implementation TiUIListItemProxy {
	TiUIListViewProxy *_listViewProxy; // weak
	NSDictionary *_bindings;
    NSMutableDictionary *_initialValues;
	NSMutableDictionary *_currentValues;
	NSMutableSet *_resetKeys;
}

@synthesize listItem = _listItem;
@synthesize indexPath = _indexPath;

- (id)initWithListViewProxy:(TiUIListViewProxy *)listViewProxy inContext:(id<TiEvaluator>)context
{
    self = [self _initWithPageContext:context];
    if (self) {
        _initialValues = [[NSMutableDictionary alloc] initWithCapacity:10];
		_currentValues = [[NSMutableDictionary alloc] initWithCapacity:10];
		_resetKeys = [[NSMutableSet alloc] initWithCapacity:10];
		_listViewProxy = listViewProxy;
		[context.krollContext invokeBlockOnThread:^{
			[context registerProxy:self];
			[listViewProxy rememberProxy:self];
		}];
		self.modelDelegate = self;
        [self getCellPropsFromDict:[listViewProxy allProperties]];
      
        [self setDefaultReadyToCreateView:YES];
    }
    return self;
}

- (id)init
{
    self = [super init];
    if (self) {
    }
    return self;
}

-(void) setListItem:(TiUIListItem *)newListItem
{
    RELEASE_TO_NIL(_listItem);
    _listItem = [newListItem retain];
    viewInitialized = YES;
    [self setReadyToCreateView:YES];
    [self windowWillOpen];
    [self windowDidOpen];
    [self willShow];
}

-(void)dealloc
{
    [_initialValues release];
	[_currentValues release];
	[_resetKeys release];
	[_indexPath release];
	[_bindings release];
	[super dealloc];
}

- (TiUIView *)view
{
	return view = (TiUIView *)_listItem.contentView;
}

- (void)detachView
{
	view = nil;
	[super detachView];
}

-(void)_destroy
{
	view = nil;
	[super _destroy];
}

-(void) getCellPropsFromDict:(NSDictionary*)dict {
    NSArray* keys = [dict allKeys];
    if ([keys containsObject:@"accessoryType"]) {
        [self setValue:[dict valueForKey:@"accessoryType"] forKey:@"accessoryType"];
    }
    if ([keys containsObject:@"selectionStyle"]) {
        [self setValue:[dict valueForKey:@"selectionStyle"] forKey:@"selectionStyle"];
    }
    if ([keys containsObject:@"selectedBackgroundColor"]) {
        [self setValue:[dict valueForKey:@"selectedBackgroundColor"] forKey:@"selectedBackgroundColor"];
    }
    if ([keys containsObject:@"selectedBackgroundImage"]) {
        [self setValue:[dict valueForKey:@"selectedBackgroundImage"] forKey:@"selectedBackgroundImage"];
    }
    if ([keys containsObject:@"selectedBackgroundGradient"]) {
        [self setValue:[dict valueForKey:@"selectedBackgroundGradient"] forKey:@"selectedBackgroundGradient"];
    }
}

-(void)_initWithProperties:(NSDictionary*)properties
{
    [super _initWithProperties:properties];
    if (_listItem != nil) {
        [self applyCellProps:properties];
    }
    else {
        [self getCellPropsFromDict:properties];
    }
}

-(void)propertyChanged:(NSString*)key oldValue:(id)oldValue newValue:(id)newValue proxy:(TiProxy*)proxy_
{
	if ([key isEqualToString:@"accessoryType"]) {
		TiThreadPerformOnMainThread(^{
			_listItem.accessoryType = [TiUtils intValue:newValue def:UITableViewCellAccessoryNone];
		}, YES);
	} else if ([key isEqualToString:@"backgroundColor"]) {
		TiThreadPerformOnMainThread(^{
			_listItem.contentView.backgroundColor = [[TiUtils colorValue:newValue] _color];
		}, YES);
	} else if ([key isEqualToString:@"selectionStyle"]) {
		TiThreadPerformOnMainThread(^{
			_listItem.selectionStyle = [TiUtils intValue:newValue def:UITableViewCellSelectionStyleBlue];
		}, YES);
	}
}

- (void)unarchiveFromTemplate:(id)viewTemplate
{
	[super unarchiveFromTemplate:viewTemplate];
	SetEventOverrideDelegateRecursive(self.children, self);
}

- (NSDictionary *)bindings
{
	if (_bindings == nil) {
		NSMutableDictionary *dict = [[NSMutableDictionary alloc] initWithCapacity:10];
		[[self class] buildBindingsForViewProxy:self intoDictionary:dict];
		_bindings = [dict copy];
		[dict release];
	}
	return _bindings;
}


- (void) applyCellProps:(NSDictionary *)properties
{
    //Here we treat all properties that can come from the template or the listview
    NSArray* keys = [properties allKeys];
    NSDictionary* currentProps = [self allProperties];
    
    
    if ([keys containsObject:@"accessoryType"]) {
        id accessoryTypeValue = [properties objectForKey:@"accessoryType"];
        if ([self shouldUpdateValue:accessoryTypeValue forKeyPath:@"accessoryType"]) {
            if ([accessoryTypeValue isKindOfClass:[NSNumber class]]) {
                UITableViewCellAccessoryType accessoryType = [accessoryTypeValue unsignedIntegerValue];
                [self recordChangeValue:accessoryTypeValue forKeyPath:@"accessoryType" withBlock:^{
                    _listItem.accessoryType = accessoryType;
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
                    _listItem.selectionStyle = selectionStyle;
                }];
            }
        }
	}
    if ([keys containsObject:@"selectedBackgroundGradient"]) {
        id value = [properties objectForKey:@"selectedBackgroundGradient"];
        [_listItem setSelectedBackgroundGradient_:value];
    }
    if ([keys containsObject:@"selectedBackgroundColor"]) {
        id value = [properties objectForKey:@"selectedBackgroundColor"];
        [_listItem setSelectedBackgroundColor_:value];
    }
    if ([keys containsObject:@"selectedBackgroundImage"]) {
        id value = [properties objectForKey:@"selectedBackgroundImage"];
        [_listItem setSelectedBackgroundImage_:value];
    }
    
}

- (void) applyCellProps
{
    [self applyCellProps:[self allProperties]];
}

- (void)setDataItem:(NSDictionary *)dataItem
{
	[_resetKeys addObjectsFromArray:[_currentValues allKeys]];
	id propertiesValue = [dataItem objectForKey:@"properties"];
	NSDictionary *properties = ([propertiesValue isKindOfClass:[NSDictionary class]]) ? propertiesValue : nil;
    NSInteger templateStyle = (_listItem != nil)?_listItem.templateStyle:TiUIListItemTemplateStyleCustom;
	switch (templateStyle) {
		case UITableViewCellStyleSubtitle:
		case UITableViewCellStyleValue1:
		case UITableViewCellStyleValue2:
            if (_listItem != nil) {
                _listItem.detailTextLabel.text = [[properties objectForKey:@"subtitle"] description];
                _listItem.detailTextLabel.backgroundColor = [UIColor clearColor];
            }
			// pass through
		case UITableViewCellStyleDefault:
            if (_listItem != nil) {
                _listItem.textLabel.text = [[properties objectForKey:@"title"] description];
                _listItem.textLabel.backgroundColor = [UIColor clearColor];
                if (templateStyle != UITableViewCellStyleValue2) {
                    id imageValue = [properties objectForKey:@"image"];
                    if ([self shouldUpdateValue:imageValue forKeyPath:@"imageView.image"]) {
                        NSURL *imageUrl = [TiUtils toURL:imageValue proxy:self];
                        UIImage *image = [[ImageLoader sharedLoader] loadImmediateImage:imageUrl];
                        if (image != nil) {
                            [self recordChangeValue:imageValue forKeyPath:@"imageView.image" withBlock:^{
                                _listItem.imageView.image = image;
                            }];
                        }
                    }
                }
                id fontValue = [properties objectForKey:@"font"];
                if ([self shouldUpdateValue:fontValue forKeyPath:@"textLabel.font"]) {
                    UIFont *font = (fontValue != nil) ? [[TiUtils fontValue:fontValue] font] : nil;
                    if (font != nil) {
                        [self recordChangeValue:fontValue forKeyPath:@"textLabel.font" withBlock:^{
                            [_listItem.textLabel setFont:font];
                        }];
                    }
                }
                
                id colorValue = [properties objectForKey:@"color"];
                if ([self shouldUpdateValue:colorValue forKeyPath:@"textLabel.color"]) {
                    UIColor *color = colorValue != nil ? [[TiUtils colorValue:colorValue] _color] : nil;
                    if (color != nil) {
                        [self recordChangeValue:colorValue forKeyPath:@"textLabel.color" withBlock:^{
                            [_listItem.textLabel setTextColor:color];
                        }];
                    }
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
    
    if (_listItem != nil) [self applyCellProps:properties];
    
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
    NSArray* children = [viewProxy children];
	[children enumerateObjectsUsingBlock:^(TiViewProxy *childViewProxy, NSUInteger idx, BOOL *stop) {
		[[self class] buildBindingsForViewProxy:childViewProxy intoDictionary:dict];
	}];
    if (![viewProxy isKindOfClass:[self class]]) {
        id bindId = [viewProxy valueForKey:@"bindId"];
        if (bindId != nil) {
            [dict setObject:viewProxy forKey:bindId];
        }
    }
}


#pragma mark - TiViewEventOverrideDelegate

- (NSDictionary *)overrideEventObject:(NSDictionary *)eventObject forEvent:(NSString *)eventType fromViewProxy:(TiViewProxy *)viewProxy
{
	NSMutableDictionary *updatedEventObject = [eventObject mutableCopy];
	[updatedEventObject setObject:NUMINT(_indexPath.section) forKey:@"sectionIndex"];
	[updatedEventObject setObject:NUMINT(_indexPath.row) forKey:@"itemIndex"];
	[updatedEventObject setObject:[_listViewProxy sectionForIndex:_indexPath.section] forKey:@"section"];
	id propertiesValue = [_listItem.dataItem objectForKey:@"properties"];
	NSDictionary *properties = ([propertiesValue isKindOfClass:[NSDictionary class]]) ? propertiesValue : nil;
	id itemId = [properties objectForKey:@"itemId"];
	if (itemId != nil) {
		[updatedEventObject setObject:itemId forKey:@"itemId"];
	}
	id bindId = [viewProxy valueForKey:@"bindId"];
	if (bindId != nil) {
		[updatedEventObject setObject:bindId forKey:@"bindId"];
	}
	return [updatedEventObject autorelease];
}

-(CGFloat)sizeWidthForDecorations:(CGFloat)oldWidth forceResizing:(BOOL)force
{
    CGFloat width = oldWidth;
    BOOL updateForiOS7 = NO;
    if (force) {
        if ([TiUtils boolValue:[self valueForKey:@"hasChild"] def:NO]) {
            width -= CHILD_ACCESSORY_WIDTH;
            updateForiOS7 = YES;
        }
        else if ([TiUtils boolValue:[self valueForKey:@"hasDetail"] def:NO]) {
            width -= DETAIL_ACCESSORY_WIDTH;
            updateForiOS7 = YES;
        }
        else if ([TiUtils boolValue:[self valueForKey:@"hasCheck"] def:NO]) {
            width -= CHECK_ACCESSORY_WIDTH;
            updateForiOS7 = YES;
        }
    }
    
    if (updateForiOS7 && [TiUtils isIOS7OrGreater]) {
        width -= IOS7_ACCESSORY_EXTRA_OFFSET;
    }
	
    return width;
}

@end

static void SetEventOverrideDelegateRecursive(NSArray *children, id<TiViewEventOverrideDelegate> eventOverrideDelegate)
{
	[children enumerateObjectsUsingBlock:^(TiViewProxy *child, NSUInteger idx, BOOL *stop) {
		child.eventOverrideDelegate = eventOverrideDelegate;
		SetEventOverrideDelegateRecursive(child.children, eventOverrideDelegate);
	}];
}

#endif
