#import "TiProperties.h"
#import "TiHost.h"
#import "TiUtils.h"
#import "TiApp.h"

@implementation TiProperties
{
    NSData *_defaultsNull;
}
@synthesize changedProperty;

+ (TiProperties*)sharedInstance
{
    static TiProperties* sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedInstance = [[self alloc] init];
    });
    return sharedInstance;
}

-(void)dealloc
{
	RELEASE_TO_NIL(_defaultsNull);
	RELEASE_TO_NIL(changedProperty);
    
	[super dealloc];
}

-(NSUserDefaults*)userDefaults
{
    static NSUserDefaults* userDefaults;
    
    if(userDefaults == nil) {
        userDefaults = [[TiApp app] userDefaults];
    }
    return userDefaults;
}

-(id)init
{
    if (self = [super init]) {
        _defaultsNull = [[NSData alloc] initWithBytes:"NULL" length:4];
    }
	return self;
}

-(BOOL)propertyExists: (NSString *)key
{
	if (![key isKindOfClass:[NSString class]]) return NO;
	[[self userDefaults] synchronize];
	return ([[self userDefaults] objectForKey:key] != nil);
}

#define GETPROP \
id appProp = [[TiApp tiAppProperties] objectForKey:key]; \
if(appProp) { \
return appProp; \
} \
if (![self propertyExists:key]) return defaultValue; \

-(id)getBool:(NSString*)key defaultValue:(id)defaultValue
{
	GETPROP
	return [NSNumber numberWithBool:[[self userDefaults] boolForKey:key]];
}

-(id)getDouble:(NSString*)key defaultValue:(id)defaultValue
{
	GETPROP
	return [NSNumber numberWithDouble:[[self userDefaults] doubleForKey:key]];
}

-(id)getInt:(NSString*)key defaultValue:(id)defaultValue
{
	GETPROP
	return [NSNumber numberWithInteger:[[self userDefaults] integerForKey:key]];
}

-(id)getString:(NSString*)key defaultValue:(id)defaultValue
{
	GETPROP
	return [[self userDefaults] stringForKey:key];
}

-(id)getList:(NSString*)key defaultValue:(id)defaultValue
{
	GETPROP
	NSArray *value = [[self userDefaults] arrayForKey:key];
	NSMutableArray *array = [[[NSMutableArray alloc] initWithCapacity:[value count]] autorelease];
	[(NSArray *)value enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
		if ([obj isKindOfClass:[NSData class]] && [_defaultsNull isEqualToData:obj]) {
			obj = [NSNull null];
		}
		[array addObject:obj];
	}];
	return array;
}

-(id)getObject:(NSString*)key defaultValue:(id)defaultValue
{
    GETPROP
    id theObject = [[self userDefaults] objectForKey:key];
    if ([theObject isKindOfClass:[NSData class]]) {
        return [NSKeyedUnarchiver unarchiveObjectWithData:theObject];
    }
    else {
        return theObject;
    }
    
}

#define SETPROP \
id appProp = [[TiApp tiAppProperties] objectForKey:key]; \
if(appProp) { \
DebugLog(@"[ERROR] Property \"%@\" already exist and cannot be overwritten", key); \
return; \
} \
if (value==nil || value==[NSNull null]) {\
[[self userDefaults] removeObjectForKey:key];\
[[self userDefaults] synchronize]; \
return;\
}\
if ([self propertyExists:key] && [ [[self userDefaults] objectForKey:key] isEqual:value]) {\
return;\
}\

-(void)setBool:(id)value forKey:(NSString*)key
{
	SETPROP
    self.changedProperty = key;
	[[self userDefaults] setBool:[TiUtils boolValue:value] forKey:key];
	[[self userDefaults] synchronize];
}

-(void)setDouble:(id)value forKey:(NSString*)key
{
	SETPROP
    self.changedProperty = key;
	[[self userDefaults] setDouble:[TiUtils doubleValue:value] forKey:key];
	[[self userDefaults] synchronize];
}

-(void)setInt:(id)value forKey:(NSString*)key
{
	SETPROP
    self.changedProperty = key;
	[[self userDefaults] setInteger:[TiUtils intValue:value] forKey:key];
	[[self userDefaults] synchronize];
}

-(void)setString:(id)value forKey:(NSString*)key
{
	SETPROP
    self.changedProperty = key;
	[[self userDefaults] setObject:[TiUtils stringValue:value] forKey:key];
	[[self userDefaults] synchronize];
}

-(void)setList:(id)value forKey:(NSString*)key
{
	SETPROP
	if ([value isKindOfClass:[NSArray class]]) {
		NSMutableArray *array = [[[NSMutableArray alloc] initWithCapacity:[value count]] autorelease];
		[(NSArray *)value enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
			if ([obj isKindOfClass:[NSNull class]]) {
				obj = _defaultsNull;
			}
			[array addObject:obj];
		}];
		value = array;
	}
    self.changedProperty = key;
	[[self userDefaults] setObject:value forKey:key];
	[[self userDefaults] synchronize];
}

-(void)setObject:(id)value forKey:(NSString*)key
{
	SETPROP
	NSData* encoded = [NSKeyedArchiver archivedDataWithRootObject:value];
    self.changedProperty = key;
	[[self userDefaults] setObject:encoded forKey:key];
	[[self userDefaults] synchronize];
}

-(void)removeProperty:(NSString*)key
{
    if([[TiApp tiAppProperties] objectForKey:key] != nil) {
        DebugLog(@"[ERROR] Cannot remove property \"%@\", it is read-only.", key);
        return;
    }
    self.changedProperty = key;
	[[self userDefaults] removeObjectForKey:key];
	[[self userDefaults] synchronize];
}

-(void)removeAllProperties {
	NSArray *keys = [[[self userDefaults] dictionaryRepresentation] allKeys];
	for(NSString *key in keys) {
		[[self userDefaults] removeObjectForKey:key];
	}
}

-(BOOL)hasProperty:(NSString*)key
{
    BOOL inUserDefaults = [self propertyExists:[TiUtils stringValue:key]];
    BOOL inTiAppProperties = [[TiApp tiAppProperties] objectForKey:key] != nil;
    return inUserDefaults || inTiAppProperties;
}

-(NSMutableArray*)listProperties
{
    NSMutableArray *array = [NSMutableArray array];
    [array addObjectsFromArray:[[[self userDefaults] dictionaryRepresentation] allKeys]];
    [array addObjectsFromArray:[[TiApp tiAppProperties] allKeys]];
    return array;
}

+(NSUserDefaults*)userDefaults
{
    return [[TiProperties sharedInstance] userDefaults];
}

+(BOOL)propertyExists: (NSString *)key
{
    return [[TiProperties sharedInstance] propertyExists:key];
}

+(id)getBool:(NSString*)key defaultValue:(id)defaultValue
{
    return [[TiProperties sharedInstance] getBool:key defaultValue:defaultValue];
}

+(id)getDouble:(NSString*)key defaultValue:(id)defaultValue
{
    return [[TiProperties sharedInstance] getDouble:key defaultValue:defaultValue];
}

+(id)getInt:(NSString*)key defaultValue:(id)defaultValue
{
    return [[TiProperties sharedInstance] getInt:key defaultValue:defaultValue];
}

+(id)getString:(NSString*)key defaultValue:(id)defaultValue
{
    return [[TiProperties sharedInstance] getString:key defaultValue:defaultValue];
}

+(id)getList:(NSString*)key defaultValue:(id)defaultValue
{
    return [[TiProperties sharedInstance] getList:key defaultValue:defaultValue];
}

+(id)getObject:(NSString*)key defaultValue:(id)defaultValue
{
    return [[TiProperties sharedInstance] getObject:key defaultValue:defaultValue];
}

+(void)setBool:(id)value forKey:(NSString*)key
{
    [[TiProperties sharedInstance] setBool:value forKey:key];
}

+(void)setDouble:(id)value forKey:(NSString*)key
{
    [[TiProperties sharedInstance] setDouble:value forKey:key];
}

+(void)setInt:(id)value forKey:(NSString*)key
{
    [[TiProperties sharedInstance] setInt:value forKey:key];
}

+(void)setString:(id)value forKey:(NSString*)key
{
    [[TiProperties sharedInstance] setString:value forKey:key];
}

+(void)setList:(id)value forKey:(NSString*)key
{
    [[TiProperties sharedInstance] setList:value forKey:key];
}

+(void)setObject:(id)value forKey:(NSString*)key
{
    [[TiProperties sharedInstance] setObject:value forKey:key];
}

+(void)removeProperty:(NSString*)key
{
    [[TiProperties sharedInstance] removeProperty:key];
}

+(void)removeAllProperties {
    return [[TiProperties sharedInstance] removeAllProperties];
}

+(BOOL)hasProperty:(NSString*)key
{
    return [[TiProperties sharedInstance] hasProperty:key];
}

+(NSMutableArray*)listProperties
{
    return [[TiProperties sharedInstance] listProperties];
}

@end
