/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2014 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_PLATFORM

#import "PlatformModule.h"
#import "TiApp.h"

#import <sys/sysctl.h>  
#import <mach/mach.h>
#import <sys/utsname.h>

@implementation PlatformModule

@synthesize name, model, version, architecture, processorCount, username, ostype, availableMemory, SDKVersion;

#pragma mark Internal

-(id)init
{
	if (self = [super init])
	{
		UIDevice *theDevice = [UIDevice currentDevice];
		name = [[theDevice systemName] retain];
        version = [[theDevice systemVersion] retain];
        SDKVersion = [@([[version substringToIndex:1] integerValue]) retain];
		processorCount = [[NSNumber numberWithInt:1] retain];
		username = [[theDevice name] retain];
#ifdef __LP64__
		ostype = [@"64bit" retain];
#else
		ostype = [@"32bit" retain];
#endif
		
		if ([TiUtils isIPad])
		{
			// ipad is a constant for Ti.Platform.osname
			[self replaceValue:@"ipad" forKey:@"osname" notification:NO];
		}
		else 
		{
			// iphone is a constant for Ti.Platform.osname
			[self replaceValue:@"iphone" forKey:@"osname" notification:NO]; 
		}
        model = [[self deviceName:[theDevice model]] retain];
		architecture = [[TiUtils currentArchitecture] retain];

		// needed for platform displayCaps orientation to be correct
		[[UIDevice currentDevice] beginGeneratingDeviceOrientationNotifications];
	}
	return self;
}

-(NSString*)deviceId {
    struct utsname systemInfo;
    
    uname(&systemInfo);
    
    return [NSString stringWithCString:systemInfo.machine
                              encoding:NSUTF8StringEncoding];
}

- (NSString*) deviceName:(NSString*)defaultValue
{
    NSString* code = [self deviceId];
    
    static NSDictionary* deviceNamesByCode = nil;
    
    if (!deviceNamesByCode) {
        
        deviceNamesByCode = @{
                              @"iPod1,1"   :@"iPod Touch",      // (Original)
                              @"iPod2,1"   :@"iPod Touch",      // (Second Generation)
                              @"iPod3,1"   :@"iPod Touch",      // (Third Generation)
                              @"iPod4,1"   :@"iPod Touch",      // (Fourth Generation)
                              @"iPhone1,1" :@"iPhone",          // (Original)
                              @"iPhone1,2" :@"iPhone",          // (3G)
                              @"iPhone2,1" :@"iPhone",          // (3GS)
                              @"iPad1,1"   :@"iPad",            // (Original)
                              @"iPad2,1"   :@"iPad 2",          //
                              @"iPad3,1"   :@"iPad",            // (3rd Generation)
                              @"iPhone3,1" :@"iPhone 4",        // (GSM)
                              @"iPhone3,3" :@"iPhone 4",        // (CDMA/Verizon/Sprint)
                              @"iPhone4,1" :@"iPhone 4S",       //
                              @"iPhone5,1" :@"iPhone 5",        // (model A1428, AT&T/Canada)
                              @"iPhone5,2" :@"iPhone 5",        // (model A1429, everything else)
                              @"iPad3,4"   :@"iPad",            // (4th Generation)
                              @"iPad2,5"   :@"iPad Mini",       // (Original)
                              @"iPhone5,3" :@"iPhone 5c",       // (model A1456, A1532 | GSM)
                              @"iPhone5,4" :@"iPhone 5c",       // (model A1507, A1516, A1526 (China), A1529 | Global)
                              @"iPhone6,1" :@"iPhone 5s",       // (model A1433, A1533 | GSM)
                              @"iPhone6,2" :@"iPhone 5s",       // (model A1457, A1518, A1528 (China), A1530 | Global)
                              @"iPhone7,1" :@"iPhone 6 Plus",   //
                              @"iPhone7,2" :@"iPhone 6",        //
                              @"iPad4,1"   :@"iPad Air",        // 5th Generation iPad (iPad Air) - Wifi
                              @"iPad4,2"   :@"iPad Air",        // 5th Generation iPad (iPad Air) - Cellular
                              @"iPad4,4"   :@"iPad Mini",       // (2nd Generation iPad Mini - Wifi)
                              @"iPad4,5"   :@"iPad Mini"        // (2nd Generation iPad Mini - Cellular)
                              };
    }
    
    NSString* deviceName = [deviceNamesByCode objectForKey:code];
    
    if (!deviceName) {
        // Not found on database. At least guess main device type from string contents:
        
        return defaultValue;
    }
    
    return deviceName;
}

-(void)dealloc
{
	RELEASE_TO_NIL(name);
	RELEASE_TO_NIL(model);
	RELEASE_TO_NIL(version);
	RELEASE_TO_NIL(architecture);
	RELEASE_TO_NIL(processorCount);
	RELEASE_TO_NIL(username);
	RELEASE_TO_NIL(ostype);
	RELEASE_TO_NIL(availableMemory);
	RELEASE_TO_NIL(capabilities);
	[super dealloc];
}

-(NSString*)apiName
{
    return @"Ti.Platform";
}

-(void)registerListeners:(id)unused
{
    UIDevice *device = [UIDevice currentDevice];
    // set a flag to temporarily turn on battery enablement
    if (batteryEnabled==NO && device.batteryMonitoringEnabled==NO)
    {
        batteryEnabled = YES;
        [device setBatteryMonitoringEnabled:YES];
    }
    
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(batteryStateChanged:) name:UIDeviceBatteryStateDidChangeNotification object:device];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(batteryStateChanged:) name:UIDeviceBatteryLevelDidChangeNotification object:device];
}

-(void)unregisterListeners:(id)unused
{
    UIDevice *device = [UIDevice currentDevice];
    if (batteryEnabled)
    {
        [device setBatteryMonitoringEnabled:NO];
        batteryEnabled = NO;
    }
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIDeviceBatteryStateDidChangeNotification object:device];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIDeviceBatteryLevelDidChangeNotification object:device];
}

-(void)_listenerAdded:(NSString *)type count:(NSInteger)count
{
	if (count == 1 && [type isEqualToString:@"battery"])
	{
        TiThreadPerformOnMainThread(^{
            [self registerListeners:nil];
        }, YES);
	}
}

-(void)_listenerRemoved:(NSString *)type count:(NSInteger)count
{
	if (count == 0 && [type isEqualToString:@"battery"])
	{
        TiThreadPerformOnMainThread(^{
            [self unregisterListeners:nil];
        }, YES);
	}
}

#pragma mark Public APIs

-(NSString*)runtime
{
	return @"javascriptcore";
}

-(NSString*)manufacturer
{
    return @"apple";
}


-(id)fullInfo
{
    return @{
        @"dpi": [[self displayCaps] dpi],
        @"osname": [self valueForKey:@"osname"],
        @"density": [[self displayCaps] density],
        @"retinaSuffix": [[self displayCaps] retinaSuffix],
        @"version": [self version],
        @"SDKVersion": [self SDKVersion],
        @"name": [self name],
        @"ostype": [self ostype],
        @"model": [self model],
        @"modelId": [self deviceId],
        @"locale": [self locale],
        @"id": [self id],
        @"densityFactor": [[self displayCaps] logicalDensityFactor],
        @"pixelWidth": [[self displayCaps] platformWidth],
        @"pixelHeight": [[self displayCaps] platformHeight]
    };
}

-(NSString*)locale
{
	// this will return the locale that the user has set the phone in
	// not the region where the phone is
	NSUserDefaults* defs = [NSUserDefaults standardUserDefaults];
	NSArray* languages = [defs objectForKey:@"AppleLanguages"];
	return [languages count] > 0 ? [languages objectAtIndex:0] : @"en";
}

-(NSString*)macaddress
{
    return [TiUtils appIdentifier];
}

-(id)id
{
    return [TiUtils appIdentifier];
}

- (NSString *)createUUID:(id)args
{
	return [TiUtils createUUID];
}

-(NSNumber*) is24HourTimeFormat: (id) unused
{
	NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
	[dateFormatter setLocale:[NSLocale currentLocale]];
	[dateFormatter setTimeStyle:NSDateFormatterShortStyle];
	NSString *dateInStringForm = [dateFormatter stringFromDate:[NSDate date]];
	NSRange amRange = [dateInStringForm rangeOfString:[dateFormatter AMSymbol]];
	NSRange pmRange = [dateInStringForm rangeOfString:[dateFormatter PMSymbol]];
	[dateFormatter release];
	return NUMBOOL(amRange.location == NSNotFound && pmRange.location == NSNotFound);
	
}


- (NSNumber*)availableMemory
{
	vm_statistics_data_t vmStats;
	mach_msg_type_number_t infoCount = HOST_VM_INFO_COUNT;
	kern_return_t kernReturn = host_statistics(mach_host_self(), HOST_VM_INFO, (host_info_t)&vmStats, &infoCount);
	
	if (kernReturn != KERN_SUCCESS) {
		return [NSNumber numberWithDouble:-1];
	}
	
	return [NSNumber numberWithDouble:((vm_page_size * vmStats.free_count) / 1024.0) / 1024.0];
}

- (NSNumber *)openURL:(NSArray*)args
{
	NSString *newUrlString = [args objectAtIndex:0];
	NSURL * newUrl = [TiUtils toURL:newUrlString proxy:self];
	BOOL result = NO;
	if (newUrl != nil)
	{
		[[UIApplication sharedApplication] openURL:newUrl];
	}
	
	return [NSNumber numberWithBool:result];
}

-(id)canOpenURL:(id)arg
{
	ENSURE_SINGLE_ARG(arg, NSObject);
    if ([arg isKindOfClass:[NSArray class]]) {
        for (int i =0; i < [arg count]; i++) {
            NSURL* url = [TiUtils toURL:[arg objectAtIndex:i] proxy:self];
            if ([[UIApplication sharedApplication] canOpenURL:url]) {
                return @(i);
            }
        }
        return @(-1);
    }
    else {
        NSURL* url = [TiUtils toURL:arg proxy:self];
        return NUMBOOL([[UIApplication sharedApplication] canOpenURL:url]);
    }
}

-(TiPlatformDisplayCaps*)displayCaps
{
	if (capabilities == nil)
	{
		return [[[TiPlatformDisplayCaps alloc] _initWithPageContext:[self executionContext]] autorelease];
	}
	return capabilities;
}

-(void)setBatteryMonitoring:(NSNumber *)yn
{
    if (![NSThread isMainThread]) {
        TiThreadPerformOnMainThread(^{
            [self setBatteryMonitoring:yn];
        }, YES);
    }
	[[UIDevice currentDevice] setBatteryMonitoringEnabled:[TiUtils boolValue:yn]];
}

-(NSNumber*)batteryMonitoring
{
    if (![NSThread isMainThread]) {
        __block NSNumber* result = nil;
        TiThreadPerformOnMainThread(^{
            result = [[self batteryMonitoring] retain];
        }, YES);
        return [result autorelease];
    }
	return NUMBOOL([UIDevice currentDevice].batteryMonitoringEnabled);
}

-(NSNumber*)batteryState
{
    if (![NSThread isMainThread]) {
        __block NSNumber* result = nil;
        TiThreadPerformOnMainThread(^{
            result = [[self batteryState] retain];
        }, YES);
        return [result autorelease];
    }
	return NUMINT([[UIDevice currentDevice] batteryState]);
}

-(NSNumber*)batteryLevel
{
    if (![NSThread isMainThread]) {
        __block NSNumber* result = nil;
        TiThreadPerformOnMainThread(^{
            result = [[self batteryLevel] retain];
        }, YES);
        return [result autorelease];
    }
	return NUMFLOAT([[UIDevice currentDevice] batteryLevel]);
}

-(TiBlob*)splashImageForCurrentOrientation {
    UIUserInterfaceIdiom deviceIdiom = [[UIDevice currentDevice] userInterfaceIdiom];
    UIUserInterfaceIdiom imageIdiom;
    UIDeviceOrientation imageOrientation;
    UIImage * defaultImage = [[[TiApp app] controller] defaultImageForOrientation:
                              [[UIDevice currentDevice] orientation]
                                         resultingOrientation:&imageOrientation idiom:&imageIdiom];
    return [[[TiBlob alloc] initWithImage:defaultImage] autorelease];
}


MAKE_SYSTEM_PROP(BATTERY_STATE_UNKNOWN,UIDeviceBatteryStateUnknown);
MAKE_SYSTEM_PROP(BATTERY_STATE_UNPLUGGED,UIDeviceBatteryStateUnplugged);
MAKE_SYSTEM_PROP(BATTERY_STATE_CHARGING,UIDeviceBatteryStateCharging);
MAKE_SYSTEM_PROP(BATTERY_STATE_FULL,UIDeviceBatteryStateFull);

#pragma mark Delegates

-(void)batteryStateChanged:(NSNotification*)note
{
	NSDictionary *event = [NSDictionary dictionaryWithObjectsAndKeys:[self batteryState],@"state",[self batteryLevel],@"level",nil];
	[self fireEvent:@"battery" withObject:event];
}

-(void)didReceiveMemoryWarning:(NSNotification*)notification
{
	RELEASE_TO_NIL(capabilities);
	[super didReceiveMemoryWarning:notification];
}


@end

#endif
