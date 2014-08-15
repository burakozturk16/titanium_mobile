@interface TiProperties : NSObject

@property(nonatomic,retain) NSString *changedProperty;

-(NSUserDefaults*)userDefaults;
-(BOOL)propertyExists: (NSString *)key;
-(id)getBool:(NSString*)key defaultValue:(id)defaultValue;
-(id)getDouble:(NSString*)key defaultValue:(id)defaultValue;
-(id)getInt:(NSString*)key defaultValue:(id)defaultValue;
-(id)getString:(NSString*)key defaultValue:(id)defaultValue;
-(id)getList:(NSString*)key defaultValue:(id)defaultValue;
-(id)getObject:(NSString*)key defaultValue:(id)defaultValue;
-(void)setBool:(id)value forKey:(NSString*)key;
-(void)setDouble:(id)value forKey:(NSString*)key;
-(void)setInt:(id)value forKey:(NSString*)key;
-(void)setString:(id)value forKey:(NSString*)key;
-(void)setList:(id)value forKey:(NSString*)key;
-(void)setObject:(id)value forKey:(NSString*)key;
-(void)removeProperty:(NSString*)key;
-(void)removeAllProperties;
-(BOOL)hasProperty:(NSString*)key;
-(NSMutableArray*)listProperties;

+ (TiProperties*)sharedInstance;
+(NSUserDefaults*)userDefaults;
+(BOOL)propertyExists: (NSString *)key;
+(id)getBool:(NSString*)key defaultValue:(id)defaultValue;
+(id)getDouble:(NSString*)key defaultValue:(id)defaultValue;
+(id)getInt:(NSString*)key defaultValue:(id)defaultValue;
+(id)getString:(NSString*)key defaultValue:(id)defaultValue;
+(id)getList:(NSString*)key defaultValue:(id)defaultValue;
+(id)getObject:(NSString*)key defaultValue:(id)defaultValue;
+(void)setBool:(id)value forKey:(NSString*)key;
+(void)setDouble:(id)value forKey:(NSString*)key;
+(void)setInt:(id)value forKey:(NSString*)key;
+(void)setString:(id)value forKey:(NSString*)key;
+(void)setList:(id)value forKey:(NSString*)key;
+(void)setObject:(id)value forKey:(NSString*)key;
+(void)removeProperty:(NSString*)key;
+(void)removeAllProperties;
+(BOOL)hasProperty:(NSString*)key;
+(NSMutableArray*)listProperties;
@end
