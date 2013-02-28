//
//  TiUISlideFakeWindowProxy.h
//  Titanium
//
//  Created by Martin Guillon on 2/28/13.
//
//

#import "TiWindowProxy.h"

@interface TiUISlideFakeWindowProxy : TiWindowProxy<TiUIViewController> {
    TiViewProxy* viewProxy;
}
-(id)initWithViewProxy:(TiViewProxy *)argproxy;

@end
