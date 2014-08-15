//
//  EsOyatsuAvcApplicationActivityProxy.m
//  ActivityViewController
//
//  Created by Alberto Gonzalez on 9/19/13.
//
//

#import "TiApp.h"
#import "TiUtils.h"
#import "TiUIiOSActivityProxy.h"

@implementation TiUIiOSActivityProxy

+(TiUIiOSActivityProxy*)activityFromArg:(id)args context:(id<TiEvaluator>)context
{
    id arg = nil;
	BOOL isArray = NO;
	
	if ([args isKindOfClass:[TiUIiOSActivityProxy class]])
	{
		return args;
	}
	return [[[TiUIiOSActivityProxy alloc] _initWithPageContext:context] autorelease];
}

-(UIImage*)imageOrDefault
{
    if (_image) {
        return [TiUtils image:_image proxy:self];
    } else {
        return nil;
    }
}


-(UIActivity*)asActivity
{
    return [TiActivity activityWithProxy:self ofCategory:_category];
}

-(BOOL)performActivity:(TiActivity*)activity withItems:(NSArray *)items
{
    if (self.onPerformActivity != nil) {
        NSLog(@"invoking onPerformActivity %@ %@", self.type, self.title);
        id ret = [self.onPerformActivity call:items thisObject:self];
        return [TiUtils boolValue:ret];
    } else {
        NSLog(@"ApplicationActivityProxy calling performActivity with no callback, will do nothing!");
        return YES;
    }
}
@end


@implementation TiActivity

- (NSString *) activityType
{
    return _proxy.type;
}

- (NSString *) activityTitle
{
    return _proxy.title;
}

- (UIImage *) activityImage
{
    return [_proxy imageOrDefault];
}
-(TiUIiOSActivityProxy*)proxy {
    return _proxy;
}
- (void) prepareWithActivityItems:(NSArray *) activityItems
{
    _activityItems = activityItems;
}

- (BOOL) canPerformWithActivityItems:(NSArray *)activityItems
{
    return YES;
}

- (void) performActivity
{
    [self activityDidFinish:[_proxy performActivity:self withItems:_activityItems]];
}

- (instancetype) initWithProxy:(TiUIiOSActivityProxy *)proxy;
{
    self = [super init];
    if (self) {
        _proxy = proxy;
    }
    return self;
}

+ (TiActivity*) activityWithProxy:(TiUIiOSActivityProxy *)proxy ofCategory:(UIActivityCategory)category
{
    if (category == UIActivityCategoryAction) {
        return [[ApplicationActionActivity alloc] initWithProxy:proxy];
    } else {
        return [[ApplicationShareActivity alloc] initWithProxy:proxy];
    }
}

@end

@implementation ApplicationShareActivity

+(UIActivityCategory)activityCategory
{
    return UIActivityCategoryShare;
}

@end

@implementation ApplicationActionActivity

+(UIActivityCategory)activityCategory
{
    return UIActivityCategoryAction;
}

@end

