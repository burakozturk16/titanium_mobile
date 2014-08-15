//
//  EsOyatsuAvcApplicationActivityProxy.h
//  ActivityViewController
//
//  Created by Alberto Gonzalez on 9/19/13.
//
//

#import "TiProxy.h"
#import "ActivityProxy.h"

@class TiActivity;

@interface TiUIiOSActivityProxy : TiProxy <TiActivityProxy>

@property(retain, nonatomic) NSNumber* category;
@property(retain, nonatomic) NSString* type;
@property(retain, nonatomic) NSString* title;
@property(retain, nonatomic) id        image;
@property(retain, nonatomic) KrollCallback* onPerformActivity;

+(TiUIiOSActivityProxy*)activityFromArg:(id)args context:(id<TiEvaluator>)context;
-(UIImage*)imageOrDefault;
-(BOOL)performActivity:(TiActivity*)activity withItems:(NSArray*)items;

@end

@interface TiActivity : UIActivity {
    NSArray* _activityItems;
    TiUIiOSActivityProxy* _proxy;
}

-(TiUIiOSActivityProxy*)proxy;
- (instancetype) initWithProxy:(TiUIiOSActivityProxy *)proxy;

+ (TiActivity*) activityWithProxy:(TiUIiOSActivityProxy *)proxy ofCategory:(UIActivityCategory)category;

@end

@interface ApplicationShareActivity : TiActivity
@end

@interface ApplicationActionActivity : TiActivity
@end
