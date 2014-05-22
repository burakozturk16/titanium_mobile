/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2014 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_NETWORK

#import "TiNetworkCookieProxy.h"
#import "NetworkModule.h"
#import "Reachability.h"
#import "TiApp.h"
#import "SBJSON.h"
#import "TiBlob.h"
#import "TiNetworkSocketProxy.h"
#import "TiUtils.h"

#import <CoreTelephony/CTTelephonyNetworkInfo.h>
#import <SystemConfiguration/CaptiveNetwork.h>

#include <mach/mach_time.h>
#import <ifaddrs.h>
//#import <netinet/in.h>
#import <net/if.h>
#import <arpa/inet.h>
#import "Reachability.h"

NSString* const WIFI_IFACE = @"en0";
NSString* const DATA_IFACE = @"pdp_ip0";

NSString* const INADDR_ANY_token = @"INADDR_ANY";
static NSOperationQueue *_operationQueue = nil;
@implementation NetworkModule
{
    dispatch_semaphore_t _startingSema;
    BOOL _firstUpdateDone;
	NSString *address;
}

-(NSString*)apiName
{
    return @"Ti.Network";
}

-(NSString*)getINADDR_ANY
{
    return INADDR_ANY_token;
}

-(NSNumber*)READ_MODE
{
    return [NSNumber numberWithInt:READ_MODE];
}

-(NSNumber*)WRITE_MODE
{
    return [NSNumber numberWithInt:WRITE_MODE];
}

-(NSNumber*)READ_WRITE_MODE
{
    return [NSNumber numberWithInt:READ_WRITE_MODE];
}

-(void)shutdown:(id)sender
{
    RELEASE_TO_NIL(_operationQueue);
    [super shutdown:sender];
}
-(void)startReachability
{
	NSAssert([NSThread currentThread],@"not on the main thread for startReachability");
	// reachability runs on the current run loop so we need to make sure we're
	// on the main UI thread
	reachability = [[Reachability reachabilityForInternetConnection] retain];
    [reachability startNotifier];
	[self updateReachabilityStatus];
}

-(void)stopReachability
{
	NSAssert([NSThread currentThread],@"not on the main thread for stopReachability");
	[reachability stopNotifier];
	RELEASE_TO_NIL(reachability);
}

-(void)_configure
{
	[super _configure];
    _firstUpdateDone = NO;
	// default to unknown network type on startup until reachability has figured it out
	state = TiNetworkConnectionStateUnknown; 
	WARN_IF_BACKGROUND_THREAD_OBJ;	//NSNotificationCenter is not threadsafe!
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(reachabilityChanged:) name:kReachabilityChangedNotification object:nil];
	// wait until done is important to get the right state
	TiThreadPerformOnMainThread(^{[self startReachability];}, NO);
}

-(void)_destroy
{
	TiThreadPerformOnMainThread(^{[self stopReachability];}, YES);
	WARN_IF_BACKGROUND_THREAD_OBJ;	//NSNotificationCenter is not threadsafe!
	[[NSNotificationCenter defaultCenter] removeObserver:self name:kReachabilityChangedNotification object:nil];
	RELEASE_TO_NIL(pushNotificationCallback);
	RELEASE_TO_NIL(pushNotificationError);
	RELEASE_TO_NIL(pushNotificationSuccess);
	RELEASE_TO_NIL(address);
    [self forgetProxy:socketProxy];
    RELEASE_TO_NIL(socketProxy);
	[super _destroy];
}

-(void)updateReachabilityStatus
{
    
	NetworkStatus status = [reachability currentReachabilityStatus];
	switch(status)
	{
		case NotReachable:
		{
			state = TiNetworkConnectionStateNone;
			break;
		}
		case ReachableViaWiFi:
		{
			state = TiNetworkConnectionStateWifi;
			break;
		}
		case ReachableViaWWAN:
		{
			state = TiNetworkConnectionStateMobile;
			break;
		}
		default:
		{
			state = TiNetworkConnectionStateUnknown;
			break;
		}
	}
    _firstUpdateDone = YES;
    if (_startingSema) {
        dispatch_semaphore_signal(_startingSema);
        
    }
	if ([self _hasListeners:@"change"])
	{
		NSDictionary *event = [NSDictionary dictionaryWithObjectsAndKeys:
							   [self networkType], @"networkType",
							   [self online], @"online",
							   [self networkTypeName], @"networkTypeName",
							   nil];
		[self fireEvent:@"change" withObject:event];
	}
}

-(void)reachabilityChanged:(NSNotification*)note
{
	[self updateReachabilityStatus];
}

-(id)encodeURIComponent:(id)args
{
	id arg = [args objectAtIndex:0];
	NSString *unencodedString = [TiUtils stringValue:arg];
	return [(NSString *)CFURLCreateStringByAddingPercentEscapes(NULL,
								(CFStringRef)unencodedString,
								NULL,
								(CFStringRef)@"!*'();:@+$,/?%#[]=&",
								kCFStringEncodingUTF8) autorelease];
}

-(id)decodeURIComponent:(id)args
{
	id arg = [args objectAtIndex:0];
	NSString *encodedString = [TiUtils stringValue:arg];
	return [(NSString *)CFURLCreateStringByReplacingPercentEscapesUsingEncoding(NULL, (CFStringRef)encodedString, CFSTR(""), kCFStringEncodingUTF8) autorelease];
}

// Socket submodule
#ifdef USE_TI_NETWORKSOCKET
-(TiProxy*)Socket
{
    if (socketProxy == nil) {
        socketProxy = [[TiNetworkSocketProxy alloc] _initWithPageContext:[self pageContext]];
        [self rememberProxy:socketProxy];
    }
    return socketProxy;
}
#endif

-(void)waitIfNotReady{
    if (_firstUpdateDone == NO) {
        _startingSema = dispatch_semaphore_create(0);
        dispatch_semaphore_wait(_startingSema, DISPATCH_TIME_FOREVER);
        dispatch_release(_startingSema);
        _startingSema = nil;
    }
}

- (NSNumber*)online
{
    [self waitIfNotReady];
	if (state!=TiNetworkConnectionStateNone && state!=TiNetworkConnectionStateUnknown)
	{
		return NUMBOOL(YES);
	}
	return NUMBOOL(NO);
}

- (NSString*)networkTypeName
{
    [self waitIfNotReady];
	switch(state)
	{
		case TiNetworkConnectionStateNone:
			return @"NONE";
		case TiNetworkConnectionStateWifi:
			return @"WIFI";
		case TiNetworkConnectionStateLan:
			return @"LAN";
		case TiNetworkConnectionStateMobile:
			return @"MOBILE";
		default: {
			break;
		}
	}
	return @"UNKNOWN";
}

- (NSString*)carrierName
{
	CTTelephonyNetworkInfo *netinfo = [[[CTTelephonyNetworkInfo alloc] init] autorelease];
    CTCarrier *carrier = [netinfo subscriberCellularProvider];
    if (carrier != nil)
        return [carrier carrierName];
    return @"";
}

-(NSNumber*)networkType
{
    [self waitIfNotReady];
    return NUMINT(state);
}


-(NSString*)address
{
#if TARGET_IPHONE_SIMULATOR
    // Assume classical ethernet and wifi interfaces
    NSArray* interfaces = [NSArray arrayWithObjects:@"en0", @"en1", nil];
    for (NSString* interface in interfaces) {
        NSString* iface = [self getIface:interface mask:NO];
        if (iface) {
            return iface;
        }
    }
    return @"";
#else
    return [self getIface:WIFI_IFACE mask:NO];
#endif
}

-(NSString*)dataAddress
{
#if TARGET_IPHONE_SIMULATOR
    return @""; // Handy shortcut
#else
    return [self getIface:DATA_IFACE mask:NO];
#endif
}

// Only available for the local wifi; why would you want it for the data network?
-(NSString*)netmask
{
#if TARGET_IPHONE_SIMULATOR
    // Assume classical ethernet and wifi interfaces
    NSArray* interfaces = [NSArray arrayWithObjects:@"en0", @"en1", nil];
    for (NSString* interface in interfaces) {
        NSString* iface = [self getIface:interface mask:YES];
        if (iface) {
            return iface;
        }
    }
    return @"";
#else
    return [self getIface:WIFI_IFACE mask:YES];
#endif
}

-(NSString*)getIface:(NSString*)iname mask:(BOOL)mask
{
    struct ifaddrs* head = NULL;
    struct ifaddrs* ifaddr = NULL;
    getifaddrs(&head);
    
    NSString* str = nil;
    for (ifaddr = head; ifaddr != NULL; ifaddr = ifaddr->ifa_next) {
        if (ifaddr->ifa_addr->sa_family == AF_INET &&
            !strcmp(ifaddr->ifa_name, [iname UTF8String])) {
            
            char ipaddr[20];
            struct sockaddr_in* addr;
            if (mask) {
                addr = (struct sockaddr_in*)ifaddr->ifa_netmask;
            }
            else {
                addr = (struct sockaddr_in*)ifaddr->ifa_addr;
            }
            inet_ntop(addr->sin_family, &(addr->sin_addr), ipaddr, 20);
            str = [NSString stringWithUTF8String:ipaddr];
            break;
        }
    }
    
    freeifaddrs(head);
    return str;
}

- (id)networkInfo
{
    NSMutableDictionary* result = [NSMutableDictionary dictionary];
#if TARGET_IPHONE_SIMULATOR
    [result setObject: @{
                         @"ip":[self address],
                         @"netmask":[self netmask],
                         } forKey:@"wifi"] ;
#else
    NSArray *ifs = (id)CNCopySupportedInterfaces();
    CFDictionaryRef networkinfo = nil;
    for (NSString *ifnam in ifs) {
        networkinfo = CNCopyCurrentNetworkInfo((CFStringRef)ifnam);
        if (networkinfo) {
            [result setObject: @{
                                 @"ip":[self address],
                                 @"netmask":[self netmask],
                                 @"ssid": (NSString*)CFDictionaryGetValue(networkinfo, kCNNetworkInfoKeySSID),
                                 @"bssid":(NSString*)CFDictionaryGetValue(networkinfo, kCNNetworkInfoKeyBSSID)
                                 } forKey:@"wifi"] ;
            break;
        }
        CFRelease(networkinfo);
    }
    [ifs release];
    if (networkinfo) CFRelease(networkinfo);
#endif
    [result setObject: @{
                         @"carrierName":[self carrierName],
                         @"ip":[self dataAddress],
                         } forKey:@"wwan"] ;
    return result;
}

+ (float) secondsSinceLastReboot{
    static mach_timebase_info_data_t    sTimebaseInfo;
    // If this is the first time we've run, get the timebase.
    // We can use denom == 0 to indicate that sTimebaseInfo is
    // uninitialised because it makes no sense to have a zero
    // denominator is a fraction.
    
    if ( sTimebaseInfo.denom == 0 ) {
        (void) mach_timebase_info(&sTimebaseInfo);
    }
    return ((float)(mach_absolute_time())) * ((float)sTimebaseInfo.numer) / ((float)sTimebaseInfo.denom) / 1000000000.0f;
}

- (NSDictionary*)networkStats
{
    BOOL   success;
    struct ifaddrs *addrs;
    const struct ifaddrs *cursor;
    const struct if_data *networkStatisc;
    
    long WiFiSent = 0;
    long WiFiReceived = 0;
    long WWANSent = 0;
    long WWANReceived = 0;
    
    NSString *name=[[[NSString alloc]init]autorelease];
    
    success = getifaddrs(&addrs) == 0;
    if (success)
    {
        cursor = addrs;
        while (cursor != NULL)
        {
            name=[NSString stringWithFormat:@"%s",cursor->ifa_name];
            // names of interfaces: en0 is WiFi ,pdp_ip0 is WWAN
            if (cursor->ifa_addr->sa_family == AF_LINK)
            {
                if ([name hasPrefix:@"en"])
                {
                    networkStatisc = (const struct if_data *) cursor->ifa_data;
                    WiFiSent+=networkStatisc->ifi_obytes;
                    WiFiReceived+=networkStatisc->ifi_ibytes;
                }
                
                if ([name hasPrefix:@"pdp_ip"])
                {
                    networkStatisc = (const struct if_data *) cursor->ifa_data;
                    WWANSent+=networkStatisc->ifi_obytes;
                    WWANReceived+=networkStatisc->ifi_ibytes;
                }
            }
            
            cursor = cursor->ifa_next;
        }
        
        freeifaddrs(addrs);
    }
    
    NSDate* now = [NSDate date];
    NSDate* boottime = [now dateByAddingTimeInterval:-[NetworkModule secondsSinceLastReboot]];
    return @{
             @"boottime":NUMLONGLONG([boottime timeIntervalSince1970]*1000.0),
             @"timestamp":NUMLONGLONG([now timeIntervalSince1970]*1000.0),
             @"wifi":@{@"sent_bytes": NUMLONG(abs(WiFiSent)), @"received_bytes": NUMLONG(abs(WiFiReceived))},
             @"wwan":@{@"sent_bytes": NUMLONG(abs(WWANSent)), @"received_bytes": NUMLONG(abs(WWANReceived))}
    };
}

MAKE_SYSTEM_PROP(NETWORK_NONE,TiNetworkConnectionStateNone);
MAKE_SYSTEM_PROP(NETWORK_WIFI,TiNetworkConnectionStateWifi);
MAKE_SYSTEM_PROP(NETWORK_MOBILE,TiNetworkConnectionStateMobile);
MAKE_SYSTEM_PROP(NETWORK_LAN,TiNetworkConnectionStateLan);
MAKE_SYSTEM_PROP(NETWORK_UNKNOWN,TiNetworkConnectionStateUnknown);

MAKE_SYSTEM_PROP(NOTIFICATION_TYPE_BADGE,1);
MAKE_SYSTEM_PROP(NOTIFICATION_TYPE_ALERT,2);
MAKE_SYSTEM_PROP(NOTIFICATION_TYPE_SOUND,3);
MAKE_SYSTEM_PROP(NOTIFICATION_TYPE_NEWSSTAND, 4);

MAKE_SYSTEM_PROP(TLS_VERSION_1_0, TLS_VERSION_1_0);
MAKE_SYSTEM_PROP(TLS_VERSION_1_1, TLS_VERSION_1_1);
MAKE_SYSTEM_PROP(TLS_VERSION_1_2, TLS_VERSION_1_2);

#pragma mark Push Notifications 

- (NSString*) remoteDeviceUUID
{
	return [[TiApp app] remoteDeviceUUID];
}

- (NSNumber*)remoteNotificationsEnabled
{
	UIRemoteNotificationType types = [[UIApplication sharedApplication] enabledRemoteNotificationTypes];
	return NUMBOOL(types != UIRemoteNotificationTypeNone);
}

- (NSArray*)remoteNotificationTypes
{
	UIRemoteNotificationType types = [[UIApplication sharedApplication] enabledRemoteNotificationTypes];
	NSMutableArray *result = [NSMutableArray array];
	if ((types & UIRemoteNotificationTypeBadge)!=0)
	{
		[result addObject:NUMINT(1)];
	}
	if ((types & UIRemoteNotificationTypeAlert)!=0)
	{
		[result addObject:NUMINT(2)];
	}
	if ((types & UIRemoteNotificationTypeSound)!=0)
	{
		[result addObject:NUMINT(3)];
	}
	if ((types & UIRemoteNotificationTypeNewsstandContentAvailability)!=0)
	{
		[result addObject:NUMINT(4)];
	}
	return result;
}

-(void)registerForPushNotifications:(id)args
{
	ENSURE_SINGLE_ARG(args,NSDictionary);
	
	UIApplication * app = [UIApplication sharedApplication];
	UIRemoteNotificationType ourNotifications = [app enabledRemoteNotificationTypes];
	
	NSArray *typesRequested = [args objectForKey:@"types"];
	
	RELEASE_TO_NIL(pushNotificationCallback);
	RELEASE_TO_NIL(pushNotificationError);
	RELEASE_TO_NIL(pushNotificationSuccess);
	
	pushNotificationSuccess = [[args objectForKey:@"success"] retain];
	pushNotificationError = [[args objectForKey:@"error"] retain];
	pushNotificationCallback = [[args objectForKey:@"callback"] retain];
	
	if (typesRequested!=nil)
	{
		for (id thisTypeRequested in typesRequested) 
		{
			NSInteger value = [TiUtils intValue:thisTypeRequested];
			switch(value)
			{
				case 1: //NOTIFICATION_TYPE_BADGE
				{
					ourNotifications |= UIRemoteNotificationTypeBadge;
					break;
				}
				case 2: //NOTIFICATION_TYPE_ALERT
				{
					ourNotifications |= UIRemoteNotificationTypeAlert;
					break;
				}
				case 3: //NOTIFICATION_TYPE_SOUND
				{
					ourNotifications |= UIRemoteNotificationTypeSound;
					break;
				}
				case 4: // NOTIFICATION_TYPE_NEWSSTAND
				{
					ourNotifications |= UIRemoteNotificationTypeNewsstandContentAvailability;
					break;
				}
			}
		}
	}
	
	[[TiApp app] setRemoteNotificationDelegate:self];
	[app registerForRemoteNotificationTypes:ourNotifications];
	
	// check to see upon registration if we were started with a push 
	// notification and if so, go ahead and trigger our callback
	id currentNotification = [[TiApp app] remoteNotification];
	if (currentNotification!=nil && pushNotificationCallback!=nil)
	{
		NSMutableDictionary * event = [TiUtils dictionaryWithCode:0 message:nil];
		[event setObject:currentNotification forKey:@"data"];
		[event setObject:NUMBOOL(YES) forKey:@"inBackground"];
		[self _fireEventToListener:@"remote" withObject:event listener:pushNotificationCallback thisObject:nil];
	}
}

-(void)unregisterForPushNotifications:(id)args
{
	UIApplication * app = [UIApplication sharedApplication];
	[app unregisterForRemoteNotifications];
}

#pragma mark Push Notification Delegates

#ifdef USE_TI_NETWORKREGISTERFORPUSHNOTIFICATIONS

-(void)application:(UIApplication *)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken
{
	// called by TiApp
	if (pushNotificationSuccess!=nil)
	{
		NSString *token = [[[[deviceToken description] stringByReplacingOccurrencesOfString:@"<"withString:@""]
							stringByReplacingOccurrencesOfString:@">" withString:@""] 
						   stringByReplacingOccurrencesOfString: @" " withString: @""];
		NSMutableDictionary * event = [TiUtils dictionaryWithCode:0 message:nil];
		[event setObject:token forKey:@"deviceToken"];
		[self _fireEventToListener:@"remote" withObject:event listener:pushNotificationSuccess thisObject:nil];
	}
}

- (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo
{
	// called by TiApp
	if (pushNotificationCallback!=nil)
	{
		NSMutableDictionary * event = [TiUtils dictionaryWithCode:0 message:nil];
		[event setObject:userInfo forKey:@"data"];
		BOOL inBackground = (application.applicationState != UIApplicationStateActive);
		[event setObject:NUMBOOL(inBackground) forKey:@"inBackground"];
		[self _fireEventToListener:@"remote" withObject:event listener:pushNotificationCallback thisObject:nil];
	}
}

- (void)application:(UIApplication *)application didFailToRegisterForRemoteNotificationsWithError:(NSError *)error
{
	// called by TiApp
	if (pushNotificationError!=nil)
	{
		NSString * message = [TiUtils messageFromError:error];
		NSMutableDictionary * event = [TiUtils dictionaryWithCode:[error code] message:message];
		[self _fireEventToListener:@"remote" withObject:event listener:pushNotificationError thisObject:nil];
	}
}

#endif

#pragma mark Cookies

-(id<TiEvaluator>)evaluationContext
{
	id<TiEvaluator> context = [self executionContext];
	if(context == nil) {
		context = [self pageContext];
	}
	return context;
}

-(NSArray*)getHTTPCookiesForDomain:(id)args
{
    ENSURE_SINGLE_ARG(args, NSString);
    NSHTTPCookieStorage *storage = [NSHTTPCookieStorage sharedHTTPCookieStorage];
    NSMutableArray *allCookies = [NSMutableArray array];
    for(NSHTTPCookie* cookie in [storage cookies])
    {
        if([[cookie domain] isEqualToString: args])
        {
            [allCookies addObject:cookie];
        }
    }
    NSMutableArray *returnArray = [NSMutableArray array];
    for(NSHTTPCookie *cookie in allCookies)
    {
        [returnArray addObject:[[[TiNetworkCookieProxy alloc] initWithCookie:cookie andPageContext:[self evaluationContext]] autorelease]];
    }
    return returnArray;
}

-(void)addHTTPCookie:(id)args;
{
    ENSURE_SINGLE_ARG(args, TiNetworkCookieProxy);
    NSHTTPCookieStorage *storage = [NSHTTPCookieStorage sharedHTTPCookieStorage];
    NSHTTPCookie* cookie = [args newCookie];
    if(cookie != nil)
    {
        [storage setCookie:cookie];
    }
}

-(NSArray*)getHTTPCookies:(id)args
{
    NSString* domain = [TiUtils stringValue:[args objectAtIndex:0]];
    NSString*   path = [TiUtils stringValue:[args objectAtIndex:1]];
    NSString*   name = [TiUtils stringValue:[args objectAtIndex:2]];
    if (path == nil || [path isEqual:@""]) {
        path = @"/";
    }
    NSHTTPCookieStorage *storage = [NSHTTPCookieStorage sharedHTTPCookieStorage];
    
    NSArray *allCookies = [storage cookies];
    NSMutableArray *returnArray = [NSMutableArray array];
    NSHTTPCookie *c = [[NSHTTPCookie alloc] initWithProperties:@{}];
    for(NSHTTPCookie *cookie in allCookies)
    {
        if([[cookie domain] isEqualToString:domain] &&
           [[cookie path] isEqualToString:path] &&
           ([[cookie name] isEqualToString:name] || name == nil)) {
            [returnArray addObject:[[[TiNetworkCookieProxy alloc] initWithCookie:cookie andPageContext:[self evaluationContext]] autorelease]];
        }
    }
    return returnArray;
}

-(void)removeAllHTTPCookies:(id)args
{
    NSHTTPCookieStorage *storage = [NSHTTPCookieStorage sharedHTTPCookieStorage];
    while ([[storage cookies] count] > 0) {
        [storage deleteCookie: [[storage cookies] objectAtIndex:0]];
    }
}

-(void)removeHTTPCookie:(id)args
{
    NSArray* cookies = [self getHTTPCookies:args];
    NSHTTPCookieStorage *storage = [NSHTTPCookieStorage sharedHTTPCookieStorage];
    for(TiNetworkCookieProxy* cookie in cookies) {
        [storage deleteCookie: [cookie newCookie]];
    }
}

-(void)removeHTTPCookiesForDomain:(id)args
{
    NSArray* cookies = [self getHTTPCookiesForDomain:args];
    NSHTTPCookieStorage *storage = [NSHTTPCookieStorage sharedHTTPCookieStorage];
    for(TiNetworkCookieProxy* cookie in cookies) {
        [storage deleteCookie: [cookie newCookie]];
    }
}

-(NSArray*)allHTTPCookies
{
    NSHTTPCookieStorage *storage = [NSHTTPCookieStorage sharedHTTPCookieStorage];
    NSMutableArray *array = [NSMutableArray array];
    for(NSHTTPCookie* cookie in [storage cookies])
    {
        [array addObject:[[[TiNetworkCookieProxy alloc] initWithCookie:cookie andPageContext:[self evaluationContext]] autorelease]];
    }
    return array;
}

+(NSOperationQueue*)operationQueue;
{
    if(_operationQueue == nil) {
        _operationQueue = [[NSOperationQueue alloc] init];
        [_operationQueue setMaxConcurrentOperationCount:4];
    }
    return _operationQueue;
}
@end


#endif
