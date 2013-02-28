//
//  TiUISlideFakeWindowProxy.m
//  Titanium
//
//  Created by Martin Guillon on 2/28/13.
//
//

#import "TiUISlideFakeWindowProxy.h"

@interface TiUISlideFakeWindowProxy ()

@end

@implementation TiUISlideFakeWindowProxy

-(id)initWithViewProxy:(TiViewProxy *)argproxy
{
	if (self = [super init])
	{
		viewProxy = [argproxy retain];
	}
	return self;
}

-(TiUIView*)view
{
    return [viewProxy getOrCreateView];
}

-(void)dealloc
{
	RELEASE_TO_NIL(viewProxy);
	[super dealloc];
}

-(id)_proxy:(TiProxyBridgeType)type
{
	return viewProxy;
}

- (UIViewController *)childViewController
{
	return nil;
}


@end
