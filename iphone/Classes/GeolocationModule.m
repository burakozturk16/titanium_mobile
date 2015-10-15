/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2014 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_GEOLOCATION

#import "GeolocationModule.h"
#import "TiApp.h"
#import "TiEvaluator.h"
#import "SBJSON.h"
#import <sys/utsname.h>
#import "NSData+Additions.h"
#import "APSAnalytics.h"
#import "APSHTTPRequest.h"
#import "APSHTTPResponse.h"
#import "NetworkModule.h"

extern NSString * const TI_APPLICATION_GUID;
extern BOOL const TI_APPLICATION_ANALYTICS;

@interface GeolocationCallback : NSObject<APSHTTPRequestDelegate>
{
    id<TiEvaluator> context;
    KrollCallback *callback;
}
-(id)initWithCallback:(KrollCallback*)callback context:(id<TiEvaluator>)context;
@end

@implementation GeolocationCallback

-(id)initWithCallback:(KrollCallback*)callback_ context:(id<TiEvaluator>)context_
{
    //Ignore analyzer warning here. Delegate will call autorelease onLoad or onError.
    if (self = [super init])
    {
        callback = [callback_ retain];
        context = [context_ retain];
    }
    return self;
}

-(void)dealloc
{
    RELEASE_TO_NIL(callback);
    RELEASE_TO_NIL(context);
    [super dealloc];
}

-(void)start:(NSDictionary*)params
{
	// https://api.appcelerator.net/p/v1/geo
	NSString *kGeolocationURL = stringWithHexString(@"68747470733a2f2f6170692e61707063656c657261746f722e6e65742f702f76312f67656f");
	
	NSMutableString *url = [[[NSMutableString alloc] init] autorelease];
	[url appendString:kGeolocationURL];
	[url appendString:@"?"];
	for (id key in params)
	{
		NSString *value = [TiUtils stringValue:[params objectForKey:key]];
		[url appendFormat:@"%@=%@&",key,[value stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]];
	}

    APSHTTPRequest *req = [[APSHTTPRequest alloc] init];
    [req setShowActivity:YES];
    [req addRequestHeader:@"User-Agent" value:[[TiApp app] userAgent]];
    [req setUrl:[NSURL URLWithString:url]];
    [req setDelegate:self];
    [req setMethod:@"GET"];
    [req setSynchronous:NO];
    NSOperationQueue *operationQueue = [NetworkModule operationQueue];
    [req setTheQueue:operationQueue];
    // Place it in the main thread since we're not using a queue and yet we need the
    // delegate methods to be called...
    //    TiThreadPerformOnMainThread(^{
    [self retain]; //retain ourself for now (because req delegate doesn't
    [req send];
    [req autorelease];
    //    }, NO);
}

-(void)requestSuccess:(NSString*)data
{
}

-(void)requestError:(NSError*)error
{
    NSDictionary *event = [TiUtils dictionaryWithCode:[error code] message:[TiUtils messageFromError:error]];
    [context fireEvent:callback withObject:event remove:NO thisObject:nil];
}

-(void)request:(APSHTTPRequest*)request onLoad:(APSHTTPResponse*)response
{
    if (request!=nil && [response error]==nil)
    {
        NSString *data = [response responseString];
        [self requestSuccess:data];
    }
    else
    {
        [self requestError:[response error]];
    }
    
    [self autorelease];
}

-(void)request:(APSHTTPRequest *)request onError:(APSHTTPResponse *)response
{
    [self requestError:[response error]];
    [self autorelease];
}

@end


@interface ForwardGeoCallback : GeolocationCallback
@end

@interface ReverseGeoCallback : GeolocationCallback
@end

@implementation ForwardGeoCallback

-(void)requestSuccess:(NSString*)locationString
{
    NSMutableDictionary *event = nil;
    
    NSArray *listItems = [locationString componentsSeparatedByString:@","];
    if([listItems count] == 4 && [[listItems objectAtIndex:0] isEqualToString:@"200"])
    {
        id accuracy = [listItems objectAtIndex:1];
        id latitude = [listItems objectAtIndex:2];
        id longitude = [listItems objectAtIndex:3];
        event = [TiUtils dictionaryWithCode:0 message:nil];
        [event setObject:accuracy forKey:@"accuracy"];
        [event setObject:latitude forKey:@"latitude"];
        [event setObject:longitude forKey:@"longitude"];
    }
    else
    {
        //TODO: better error handling
        event = [TiUtils dictionaryWithCode:-1 message:@"error obtaining geolocation"];
    }
    
    [context fireEvent:callback withObject:event remove:NO thisObject:nil];
}

@end

@implementation ReverseGeoCallback

-(void)requestSuccess:(NSString*)locationString
{
    SBJSON *json = [[SBJSON alloc] init];
    NSError * error = nil;
    id event = [json fragmentWithString:locationString error:&error];
    [json release];
    if (error != nil) {
        [self requestError:error];
    }
    else {
        BOOL success = [TiUtils boolValue:@"success" properties:event def:YES];
        NSMutableDictionary * revisedEvent = [TiUtils dictionaryWithCode:success?0:-1 message:success?nil:@"error reverse geocoding"];
        [revisedEvent setValuesForKeysWithDictionary:event];
        [context fireEvent:callback withObject:revisedEvent remove:NO thisObject:nil];
    }
}

@end

@interface GeolocationModule()
@property(nonatomic,readwrite,retain) CLLocation* lastLocation;
@end

@implementation GeolocationModule {
    CLLocationManager *locationManager;
    CLLocationManager *locationPermissionManager; // used for just permissions requests
    
    CLLocationAccuracy accuracy;
    CLLocationDistance distance;
    CLLocationDegrees heading;
    BOOL calibration;
    NSMutableArray *singleHeading;
    NSMutableArray *singleLocation;
    NSString *purpose;
    BOOL trackingHeading;
    BOOL trackingLocation;
    BOOL trackSignificantLocationChange;
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_6_0
    CLActivityType activityType;
    BOOL pauseLocationUpdateAutomatically;
#endif
    NSDictionary * lastLocationDict;
    NSRecursiveLock* lock;
    BOOL canReportSameLocation;
    BOOL _locationManagerCreated;
	KrollCallback *authorizationCallback;
}

@synthesize lastLocation;

#pragma mark Internal

// TODO: Do we need to force this onto the main thread?
-(void)shutdownLocationManager
{
    [lock lock];
    if (locationManager == nil || !_locationManagerCreated)
    {
        [lock unlock];
        return;
    }
    _locationManagerCreated = NO;
    
    [self setHeadingUpdateState:NO];
    [self setLocationUpdateState:NO];
    
    [lock unlock];
}

-(void)_destroy
{
    [self shutdownLocationManager];
    RELEASE_TO_NIL(locationManager);
    RELEASE_TO_NIL(locationPermissionManager);
    RELEASE_TO_NIL(singleHeading);
    RELEASE_TO_NIL(singleLocation);
    RELEASE_TO_NIL(purpose);
    RELEASE_TO_NIL(lock);
    RELEASE_TO_NIL(lastLocationDict);
    [super _destroy];
}

-(NSString*)apiName
{
    return @"Ti.Geolocation";
}

-(void)contextWasShutdown:(KrollBridge*)bridge
{
    if (singleHeading!=nil)
    {
        for (KrollCallback *callback in [NSArray arrayWithArray:singleHeading])
        {
            KrollContext *ctx = (KrollContext*)[callback context];
            if ([bridge krollContext] == ctx)
            {
                [singleHeading removeObject:callback];
            }
        }
    }
    if (singleLocation!=nil)
    {
        for (KrollCallback *callback in [NSArray arrayWithArray:singleLocation])
        {
            KrollContext *ctx = (KrollContext*)[callback context];
            if ([bridge krollContext] == ctx)
            {
                [singleLocation removeObject:callback];
            }
        }
    }
    [self shutdownLocationManager];
}

-(void)_configure
{
    _locationManagerCreated = NO;
   // reasonable defaults:
    
    // accuracy by default
    accuracy = kCLLocationAccuracyThreeKilometers;
    
    // distance filter by default is notify of all movements
    distance = kCLDistanceFilterNone;
    
    // minimum heading filter by default
    heading = kCLHeadingFilterNone;
    
    // should we show heading calibration dialog? defaults to YES
    calibration = YES;
    
    // track all location changes by default
    trackSignificantLocationChange = NO;
    
    // activity Type by default
    activityType = CLActivityTypeOther;
    
    // pauseLocationupdateAutomatically by default NO
    pauseLocationUpdateAutomatically  = NO;
    
    lock = [[NSRecursiveLock alloc] init];
    
    canReportSameLocation = NO;
    
    [super _configure];
}

-(CLLocationManager*)locationManager
{
    [lock lock];
    if (locationManager==nil)
    {
        _locationManagerCreated = YES;
        locationManager = [[CLLocationManager alloc] init];
        locationManager.delegate = self;
        if (!trackSignificantLocationChange) {
            if (accuracy!=-1)
            {
                locationManager.desiredAccuracy = accuracy;
            }
            else
            {
                locationManager.desiredAccuracy = kCLLocationAccuracyThreeKilometers;
            }
            locationManager.distanceFilter = distance;
        }
        locationManager.headingFilter = heading;
        
        if ([TiUtils isIOS8OrGreater]) {
            if([[NSBundle mainBundle] objectForInfoDictionaryKey:@"NSLocationAlwaysUsageDescription"]){
                [locationManager requestAlwaysAuthorization];
            }else if ([[NSBundle mainBundle] objectForInfoDictionaryKey:@"NSLocationWhenInUseUsageDescription"]){
                [locationManager requestWhenInUseAuthorization];
            }else{
                NSLog(@"[ERROR] The keys NSLocationAlwaysUsageDescription or NSLocationWhenInUseUsageDescription are not defined in your tiapp.xml.  Starting with iOS8 this is required.");
            }
        }else{
            if (purpose!=nil)
            {
                DebugLog(@"[WARN] The Ti.Geolocation.purpose property is deprecated. On iOS6 and above include the NSLocationUsageDescription key in your Info.plist");
                if ([locationManager respondsToSelector:@selector(setPurpose:)]) {
                    [locationManager performSelector:@selector(setPurpose:) withObject:purpose];
                }
            }
        }
        
        locationManager.activityType = activityType;
        locationManager.pausesLocationUpdatesAutomatically = pauseLocationUpdateAutomatically;
        

    }
    [lock unlock];
    return locationManager;
}

-(void)startStopLocationManagerIfNeeded
{
    BOOL startHeading = NO;
    BOOL startLocation = NO;
    
    if (singleHeading!=nil && [singleHeading count] > 0)
    {
        startHeading = YES;
    }
    if (singleLocation!=nil && [singleLocation count] > 0)
    {
        startLocation = YES;
    }
    if (!startHeading && [self _hasListeners:@"heading"])
    {
        startHeading = YES;
    }
    if (!startLocation && [self _hasListeners:@"location"])
    {
        startLocation = YES;
    }
    
    if (startHeading || startLocation)
    {
        CLLocationManager *lm = [self locationManager];
        if (!_locationManagerCreated) {
            _locationManagerCreated = YES;
            if ([CLLocationManager locationServicesEnabled]== NO)
            {
                //NOTE: this is from Apple example from LocateMe and it works well. the developer can still check for the
                //property and do this message themselves before calling geo. But if they don't, we at least do it for them.
                NSString *title = NSLocalizedString(@"Location Services Disabled",@"Location Services Disabled Alert Title");
                NSString *msg = NSLocalizedString(@"You currently have all location services for this device disabled. If you proceed, you will be asked to confirm whether location services should be reenabled.",@"Location Services Disabled Alert Message");
                NSString *ok = NSLocalizedString(@"OK",@"Location Services Disabled Alert OK Button");
                UIAlertView *servicesDisabledAlert = [[UIAlertView alloc] initWithTitle:title message:msg delegate:nil cancelButtonTitle:ok otherButtonTitles:nil];
                [servicesDisabledAlert show];
                [servicesDisabledAlert release];
            }
        }
        
    }
    [self setHeadingUpdateState:startHeading];
    [self setLocationUpdateState:startLocation];
    if (!trackingHeading && !trackingLocation)
    {
        [self shutdownLocationManager];
    }
}

-(void)_listenerAdded:(NSString *)type count:(NSInteger)count
{
    BOOL startStop = NO;
    
    if (count == 1 && [type isEqualToString:@"change"])
    {
        //in order to receive change state of the ios geolocation module,
        //we need to create the locationManager
        TiThreadPerformOnMainThread(^{[self locationManager];}, YES);
    } else
        if (count == 1 && [type isEqualToString:@"heading"])
    {
        startStop = YES;
    }
    else if (count == 1 && [type isEqualToString:@"location"])
    {
        startStop = YES;
    }
    
    if (startStop)
    {
        TiThreadPerformOnMainThread(^{[self startStopLocationManagerIfNeeded];}, NO);
    }
}

-(void)_listenerRemoved:(NSString *)type count:(NSInteger)count
{
    BOOL check = NO;
    if (count == 0 && [type isEqualToString:@"heading"])
    {
        check = YES;
        [self setHeadingUpdateState:NO];
    }
    else if (count == 0 && [type isEqualToString:@"location"])
    {
        check = YES;
        [self setLocationUpdateState:NO];
    }
    
    if (check && ![self _hasListeners:@"heading"] && ![self _hasListeners:@"location"])
    {
        TiThreadPerformBlockOnMainThread(^{[self startStopLocationManagerIfNeeded];}, YES);
        [self shutdownLocationManager];

        RELEASE_TO_NIL(singleHeading);
        RELEASE_TO_NIL(singleLocation);
    }
}

-(void)setHeadingUpdateState:(BOOL)state {
    if (trackingHeading != state)
    {
        trackingHeading = state;
        if (state) {
            [locationManager startUpdatingHeading];
        } else {
            [locationManager stopUpdatingHeading];
        }
        if ([self _hasListeners:@"state"]) {
            [self fireEvent:@"state" withObject:@{
                                                  @"monitor":@"heading",
                                                  @"state":@(state)
                                                  }checkForListener:NO];
        }
    }
}

-(void)setLocationUpdateState:(BOOL)state {
    if (trackingLocation != state)
    {
        trackingLocation = state;
        if (state) {
            if (trackSignificantLocationChange) {
                [locationManager startMonitoringSignificantLocationChanges];
            }
            else{
                [locationManager startUpdatingLocation];
            }
        } else {
            if (trackSignificantLocationChange){
                [locationManager stopMonitoringSignificantLocationChanges];
            }
            else{
                [locationManager stopUpdatingLocation];
            }
        }
        if ([self _hasListeners:@"state"]) {
            [self fireEvent:@"state" withObject:@{
                                                  @"monitor":@"location",
                                                  @"state":@(state)
                                                  }checkForListener:NO];
        }
    }
}

-(BOOL)headingAvailable
{
    return [CLLocationManager headingAvailable];
}

#pragma mark Public APIs

-(NSNumber*)hasCompass
{
    UIDevice * theDevice = [UIDevice currentDevice];
    NSString* version = [theDevice systemVersion];
    
    BOOL headingAvailableBool = [self headingAvailable];
    if (headingAvailableBool)
    {
        struct utsname u;
        uname(&u);
        if (!strcmp(u.machine, "i386"))
        {
            // 3.0 simulator headingAvailable will report YES but its not really available except post 3.0
            headingAvailableBool = [version hasPrefix:@"3.0"] ? NO : [CLLocationManager headingAvailable];
        }
    }
    return NUMBOOL(headingAvailableBool);
}

-(void)performGeo:(NSString*)direction address:(NSString*)address callback:(GeolocationCallback*)callback
{
    id aguid = TI_APPLICATION_GUID;
    id sid = [[TiApp app] sessionId];
    
    //for now we trick it and don't send identifiers ;)
    NSDictionary *params = [NSDictionary dictionaryWithObjectsAndKeys:
                            direction, @"d",
                            //							aguid,@"aguid",
                            //							[TiUtils appIdentifier],@"mid",
                            //							sid,@"sid",
                            address,@"q",
                            [[NSLocale currentLocale] objectForKey: NSLocaleCountryCode],@"c",
                            nil];
    
    [callback start:params];
}

-(void)reverseGeocoder:(id)args
{
	ENSURE_ARG_COUNT(args,3);
	KrollCallback *callback = [args objectAtIndex:2];
	ENSURE_TYPE(callback,KrollCallback);
#ifndef __clang_analyzer__ //ignore static analyzer error here, memory will be released
	CGFloat lat = [TiUtils floatValue:[args objectAtIndex:0]];
	CGFloat lon = [TiUtils floatValue:[args objectAtIndex:1]];
	ReverseGeoCallback *rcb = [[ReverseGeoCallback alloc] initWithCallback:callback context:[self executionContext]];
	[self performGeo:@"r" address:[NSString stringWithFormat:@"%f,%f",lat,lon] callback:rcb];
#endif
}

-(void)forwardGeocoder:(id)args
{
	ENSURE_ARG_COUNT(args,2);
	KrollCallback *callback = [args objectAtIndex:1];
	ENSURE_TYPE(callback,KrollCallback);
#ifndef __clang_analyzer__ //ignore static analyzer error here, memory will be released
	ForwardGeoCallback *fcb = [[ForwardGeoCallback alloc] initWithCallback:callback context:[self executionContext]];
	[self performGeo:@"f" address:[TiUtils stringValue:[args objectAtIndex:0]] callback:fcb];
#endif
}

-(void)getCurrentHeading:(id)callback
{
    ENSURE_SINGLE_ARG(callback,KrollCallback);
    ENSURE_UI_THREAD(getCurrentHeading,callback);
    if (singleHeading==nil)
    {
        singleHeading = [[NSMutableArray alloc] initWithCapacity:1];
    }
    [singleHeading addObject:callback];
    [self startStopLocationManagerIfNeeded];
}

-(void)getCurrentPosition:(id)callback
{
    ENSURE_SINGLE_ARG(callback,KrollCallback);
    ENSURE_UI_THREAD(getCurrentPosition,callback);
    
    // If the location updates are started, invoke the callback directly.
    if (locationManager != nil && locationManager.location != nil && trackingLocation == YES ) {
        CLLocation *currentLocation = locationManager.location;
        NSMutableDictionary *event = [TiUtils dictionaryWithCode:0 message:nil];
        [event setObject:[self locationDictionary:currentLocation] forKey:@"coords"];
        [self _fireEventToListener:@"location" withObject:event listener:callback thisObject:nil];
    }
    // Otherwise, start the location manager.
    else {
//        canReportSameLocation = YES;
        if (singleLocation==nil)
        {
            singleLocation = [[NSMutableArray alloc] initWithCapacity:1];
        }
        [singleLocation addObject:callback];
        [self startStopLocationManagerIfNeeded];
    }
}

-(NSDictionary*)lastGeolocation
{
    return lastLocationDict;
}

-(NSNumber*)highAccuracy
{
    return @(accuracy==kCLLocationAccuracyBest);
}

-(void)setHighAccuracy:(NSNumber *)value
{
    ENSURE_UI_THREAD(setHighAccuracy,value);
    accuracy = kCLLocationAccuracyBest;
    // don't prematurely start it
    if (locationManager!=nil)
    {
        [locationManager setDesiredAccuracy:kCLLocationAccuracyBest];
    }
}

-(NSNumber*)accuracy
{
    return @(accuracy);
}

-(void)setAccuracy:(NSNumber *)value
{
    ENSURE_UI_THREAD(setAccuracy,value);
    accuracy = [TiUtils doubleValue:value];
    // don't prematurely start it
    if (locationManager!=nil)
    {
        [locationManager setDesiredAccuracy:accuracy];
    }
}

-(NSNumber*)distanceFilter
{
    return @(distance);
}

-(void)setDistanceFilter:(NSNumber *)value
{
    ENSURE_UI_THREAD(setDistanceFilter,value);
    distance = [TiUtils doubleValue:value];
    // don't prematurely start it
    if (locationManager!=nil)
    {
        [locationManager setDistanceFilter:distance];
    }
}

-(NSNumber*)headingFilter
{
    return NUMDOUBLE(heading);
}

-(void)setHeadingFilter:(NSNumber *)value
{
    ENSURE_UI_THREAD(setHeadingFilter,value);
    heading = [TiUtils doubleValue:value];
    // don't prematurely start it
    if (locationManager!=nil)
    {
        [locationManager setHeadingFilter:heading];
    }
}

-(NSNumber*)showCalibration
{
    return @(calibration);
}

-(void)setShowCalibration:(NSNumber *)value
{
    calibration = [TiUtils boolValue:value];
}

-(NSNumber*)locationServicesEnabled
{
    return @([CLLocationManager locationServicesEnabled]);
}

-(NSNumber*)locationServicesAuthorization
{
    return @([CLLocationManager authorizationStatus]);
}

-(NSNumber*)trackSignificantLocationChange
{
    return @(trackSignificantLocationChange);
}

-(void)setTrackSignificantLocationChange:(id)value
{
    if ([CLLocationManager significantLocationChangeMonitoringAvailable]) {
        BOOL newval = [TiUtils boolValue:value def:YES];
        
        if (newval != trackSignificantLocationChange) {
            if ( trackingLocation && locationManager != nil ) {
                [lock lock];
                [self shutdownLocationManager];
                trackSignificantLocationChange = newval;
                [lock unlock];
                TiThreadPerformBlockOnMainThread(^{[self startStopLocationManagerIfNeeded];}, NO);
                return ;
            }
        }
        trackSignificantLocationChange = newval;
    }
    else{
        trackSignificantLocationChange = NO;
        DebugLog(@"[WARN] Ti.Geolocation.setTrackSignificantLocationChange is not supported on this device.");
    }
}

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_6_0
// Activity Type for CLlocationManager.
-(NSNumber*)activityType
{
    return NUMINT(activityType);
}

-(void)setActivityType:(NSNumber *)value
{
    activityType = [TiUtils intValue:value];
    TiThreadPerformBlockOnMainThread(^{[locationManager setActivityType:activityType];}, NO);
}

// Flag to decide whether or not the app should continue to send location updates while the app is in background.

-(NSNumber*)pauseLocationUpdateAutomatically
{
    return NUMBOOL(pauseLocationUpdateAutomatically);
}

-(void)setPauseLocationUpdateAutomatically:(id)value
{
    pauseLocationUpdateAutomatically = [TiUtils boolValue:value];
    TiThreadPerformBlockOnMainThread(^{[locationManager setPausesLocationUpdatesAutomatically:pauseLocationUpdateAutomatically];}, NO);
}
#endif


-(void)restart:(id)arg
{
    [lock lock];
    [self shutdownLocationManager];
    [lock unlock];
    // must be on UI thread
    TiThreadPerformBlockOnMainThread(^{[self startStopLocationManagerIfNeeded];}, NO);
}

MAKE_SYSTEM_PROP_DBL(ACCURACY_BEST,kCLLocationAccuracyBest);
MAKE_SYSTEM_PROP_DBL(ACCURACY_HIGH,kCLLocationAccuracyBest);
MAKE_SYSTEM_PROP_DBL(ACCURACY_NEAREST_TEN_METERS,kCLLocationAccuracyNearestTenMeters);
MAKE_SYSTEM_PROP_DBL(ACCURACY_HUNDRED_METERS,kCLLocationAccuracyHundredMeters);
MAKE_SYSTEM_PROP_DBL(ACCURACY_KILOMETER,kCLLocationAccuracyKilometer);
MAKE_SYSTEM_PROP_DBL(ACCURACY_THREE_KILOMETERS,kCLLocationAccuracyThreeKilometers);
MAKE_SYSTEM_PROP_DBL(ACCURACY_LOW, kCLLocationAccuracyThreeKilometers);
MAKE_SYSTEM_PROP(ACCURACY_BEST_FOR_NAVIGATION, kCLLocationAccuracyBestForNavigation);//Since 2.1.3

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_4_2
MAKE_SYSTEM_PROP(AUTHORIZATION_UNKNOWN, kCLAuthorizationStatusNotDetermined);
MAKE_SYSTEM_PROP(AUTHORIZATION_AUTHORIZED, kCLAuthorizationStatusAuthorized);
MAKE_SYSTEM_PROP(AUTHORIZATION_DENIED, kCLAuthorizationStatusDenied);
MAKE_SYSTEM_PROP(AUTHORIZATION_RESTRICTED, kCLAuthorizationStatusRestricted);
#else
// We only need auth unknown, because that's all the system will return.
MAKE_SYSTEM_PROP(AUTHORIZATION_UNKNOWN, 0);
#endif

MAKE_SYSTEM_PROP(ERROR_LOCATION_UNKNOWN, kCLErrorLocationUnknown);
MAKE_SYSTEM_PROP(ERROR_DENIED, kCLErrorDenied);
MAKE_SYSTEM_PROP(ERROR_NETWORK, kCLErrorNetwork);
MAKE_SYSTEM_PROP(ERROR_HEADING_FAILURE, kCLErrorHeadingFailure);

MAKE_SYSTEM_PROP(ERROR_REGION_MONITORING_DENIED, kCLErrorRegionMonitoringDenied);
MAKE_SYSTEM_PROP(ERROR_REGION_MONITORING_FAILURE, kCLErrorRegionMonitoringFailure);
MAKE_SYSTEM_PROP(ERROR_REGION_MONITORING_DELAYED, kCLErrorRegionMonitoringSetupDelayed);

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_6_0
MAKE_SYSTEM_PROP(ACTIVITYTYPE_OTHER, CLActivityTypeOther);
MAKE_SYSTEM_PROP(ACTIVITYTYPE_AUTOMOTIVE_NAVIGATION, CLActivityTypeAutomotiveNavigation);
MAKE_SYSTEM_PROP(ACTIVITYTYPE_FITNESS, CLActivityTypeFitness);
MAKE_SYSTEM_PROP(ACTIVITYTYPE_OTHER_NAVIGATION, CLActivityTypeOtherNavigation);
#endif

-(NSNumber*)AUTHORIZATION_ALWAYS
{
    if ([TiUtils isIOS8OrGreater]) {
        return NUMINT(kCLAuthorizationStatusAuthorizedAlways);
    }
    return NUMINT(0);
}

-(NSNumber*)AUTHORIZATION_WHEN_IN_USE
{
    if ([TiUtils isIOS8OrGreater]) {
        return NUMINT(kCLAuthorizationStatusAuthorizedWhenInUse);
    }
    return NUMINT(0);
}

-(CLLocationManager*)locationPermissionManager
{
    // if we don't have an instance, create it
    if (locationPermissionManager == nil) {
        locationPermissionManager = [[CLLocationManager alloc] init];
        locationPermissionManager.delegate = self;
    }
    return locationPermissionManager;
}

-(NSNumber*)hasLocationPermissions:(id)args
{
    id value = [args objectAtIndex:0];
    
    ENSURE_TYPE(value, NSNumber);

    CLAuthorizationStatus currentPermissionLevel = [CLLocationManager authorizationStatus];
    CLAuthorizationStatus requestedPermissionLevel = [TiUtils intValue: value];
    BOOL locationServicesEnabled = [CLLocationManager locationServicesEnabled];
    
    return NUMBOOL(locationServicesEnabled && currentPermissionLevel == requestedPermissionLevel);
}

-(void)requestAuthorization:(id)value
{
    DEPRECATED_REPLACED(@"Geolocation.requestAuthorization", @"5.1.0", @"Geolocation.requestLocationPermissions");
    [self requestLocationPermissions:@[value, [NSNull null]]];
}

-(void)requestLocationPermissions:(id)args
{
    if (![TiUtils isIOS8OrGreater]) {
        return;
    }
    
    id value = [args objectAtIndex:0];
    ENSURE_TYPE(value, NSNumber);
    
    // Store the authorization callback for later usage
    if([args count] == 2) {
        RELEASE_TO_NIL(authorizationCallback);
        ENSURE_TYPE([args objectAtIndex:1], KrollCallback);
        authorizationCallback = [[args objectAtIndex:1] retain];
    }
    
    CLAuthorizationStatus requested = [TiUtils intValue: value];
    CLAuthorizationStatus currentPermissionLevel = [CLLocationManager authorizationStatus];
    
    if(requested == kCLAuthorizationStatusAuthorizedWhenInUse){
        if ([[NSBundle mainBundle] objectForInfoDictionaryKey:@"NSLocationWhenInUseUsageDescription"]) {
            if((currentPermissionLevel == kCLAuthorizationStatusAuthorizedAlways) ||
               (currentPermissionLevel == kCLAuthorizationStatusAuthorized)) {
                NSLog(@"[WARN] cannot change already granted permission from AUTHORIZATION_ALWAYS to AUTHORIZATION_WHEN_IN_USE");
            }else{
                [[self locationPermissionManager] requestWhenInUseAuthorization];
            }
        }else{
            NSLog(@"[ERROR] the NSLocationWhenInUseUsageDescription key must be defined in your tiapp.xml in order to request this permission");
        }
    }
    if ((requested == kCLAuthorizationStatusAuthorizedAlways) ||
        (requested == kCLAuthorizationStatusAuthorized)) {
        if ([[NSBundle mainBundle] objectForInfoDictionaryKey:@"NSLocationAlwaysUsageDescription"]) {
            if (currentPermissionLevel == kCLAuthorizationStatusAuthorizedWhenInUse) {
                NSLog(@"[ERROR] cannot change already granted permission from AUTHORIZATION_WHEN_IN_USE to AUTHORIZATION_ALWAYS");
            } else {
                [[self locationPermissionManager] requestAlwaysAuthorization];
            }
            [[self locationPermissionManager] requestAlwaysAuthorization];
        }else{
            NSLog(@"[ERROR] the NSLocationAlwaysUsageDescription key must be defined in your tiapp.xml in order to request this permission");
        }
    }
}

#pragma mark Internal

-(NSDictionary*)locationDictionary:(CLLocation*)newLocation;
{
    if ([newLocation timestamp] == 0)
    {
        // this happens when the location object is essentially null (as in no location)
        return nil;
    }
    
    CLLocationCoordinate2D latlon = [newLocation coordinate];
    
    NSMutableDictionary * data = [NSMutableDictionary dictionaryWithObjectsAndKeys:
                                  @(latlon.latitude),@"latitude",
                                  @(latlon.longitude),@"longitude",
                                  @([newLocation horizontalAccuracy]),@"accuracy",
                                  @((long long)([[newLocation timestamp] timeIntervalSince1970] * 1000)),@"timestamp",
                                  nil];
    if ([newLocation verticalAccuracy] > 0) {
        [data setObject:@([newLocation verticalAccuracy]) forKey:@"altitudeAccuracy"];
        [data setObject:@([newLocation altitude]) forKey:@"altitude"];
    }
    if ([newLocation course] >= 0) {
        [data setObject:@([newLocation course]) forKey:@"heading"];
    }
    if ([newLocation speed] >= 0) {
        [data setObject:@([newLocation speed]) forKey:@"speed"];
    }
    if ([TiUtils isIOS8OrGreater]) {
        [data setObject:@{
                          @"level": @([[newLocation floor] level])
                          } forKey:@"floor"];
    }
    
    return data;
}

-(BOOL)locationFarEnough:(CLLocation*) loc1 fromLocation:(CLLocation*) loc2{
    if (!loc2) return true;
    float dist = [loc2 distanceFromLocation:loc1];
//    if (dist == 0 && loc1.altitude == loc2.altitude && loc1.course == loc2.course) {
//        //same exact coord
//        return false;
//    }
    return dist > distance;
}

-(NSMutableArray*)locationsDictionary:(NSArray*)newLocations;
{
    NSMutableArray* result = [[NSMutableArray alloc] initWithCapacity:[newLocations count]];
    for (CLLocation* loc in newLocations) {
//        if ([self locationFarEnough:loc fromLocation:self.lastLocation]) {
            self.lastLocation = loc;
            NSDictionary* dict = [self locationDictionary:loc];
            if (dict) {
                [result addObject:dict];
            }
//        }
        
    }
    return [result autorelease];
}

-(NSDictionary*)headingDictionary:(CLHeading*)newHeading
{
    long long ts = (long long)[[newHeading timestamp] timeIntervalSince1970] * 1000;
    
    NSDictionary *data = [NSDictionary dictionaryWithObjectsAndKeys:
                          [NSNumber numberWithDouble:[newHeading magneticHeading]],@"magneticHeading",
                          [NSNumber numberWithDouble:[newHeading trueHeading]],@"trueHeading",
                          [NSNumber numberWithDouble:[newHeading headingAccuracy]],@"accuracy",
                          [NSNumber numberWithLongLong:ts],@"timestamp",
                          [NSNumber numberWithDouble:[newHeading x]],@"x",
                          [NSNumber numberWithDouble:[newHeading y]],@"y",
                          [NSNumber numberWithDouble:[newHeading z]],@"z",
                          nil];
    return data;
}

#pragma mark Single Shot Handling

-(BOOL)fireSingleShotLocationIfNeeded:(NSDictionary*)event stopIfNeeded:(BOOL)stopIfNeeded
{
    // check to see if we have any single shot location callbacks
    if (singleLocation!=nil)
    {
        for (KrollCallback *callback in singleLocation)
        {
            [self _fireEventToListener:@"location" withObject:event listener:callback thisObject:nil];
        }
        
        // after firing, we remove them
        RELEASE_TO_NIL(singleLocation);
        canReportSameLocation = NO;
        
        // check to make sure we don't need to stop after the single shot
        if (stopIfNeeded)
        {
            [self startStopLocationManagerIfNeeded];
        }
        return YES;
    }
    return NO;
}

-(BOOL)fireSingleShotHeadingIfNeeded:(NSDictionary*)event stopIfNeeded:(BOOL)stopIfNeeded
{
    // check to see if we have any single shot heading callbacks
    if (singleHeading!=nil)
    {
        for (KrollCallback *callback in singleHeading)
        {
            [self _fireEventToListener:@"heading" withObject:event listener:callback thisObject:nil];
        }
        
        // after firing, we remove them
        RELEASE_TO_NIL(singleHeading);
        
        // check to make sure we don't need to stop after the single shot
        if (stopIfNeeded)
        {
            [self startStopLocationManagerIfNeeded];
        }
        return YES;
    }
    return NO;
}

-(NSString*)purpose
{
    return purpose;
}

-(void)setPurpose:(NSString *)reason
{
    ENSURE_UI_THREAD(setPurpose,reason);
    RELEASE_TO_NIL(purpose);
    purpose = [reason retain];
    DebugLog(@"[WARN] The Ti.Geolocation.purpose property is deprecated. On iOS6 and above include the NSLocationUsageDescription key in your Info.plist");
    
    if (locationManager!=nil)
    {
        if ([locationManager respondsToSelector:@selector(setPurpose:)]) {
            [locationManager performSelector:@selector(setPurpose:) withObject:purpose];
        }
    }
    
}

#pragma mark Geolacation Analytics

-(void)fireApplicationAnalyticsIfNeeded:(NSArray *)locations{
    if (!TI_APPLICATION_ANALYTICS) return;
    static BOOL analyticsSend = NO;
    if (!analyticsSend)
    {
        analyticsSend = YES;
        [[APSAnalytics sharedInstance] sendAppGeoEvent:[locations lastObject]];
    }
}

#pragma mark Delegates

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_6_0

- (void)locationManagerDidPauseLocationUpdates:(CLLocationManager *)manager
{
    if ([self _hasListeners:@"locationupdatepaused"])
    {
        [self fireEvent:@"locationupdatepaused" withObject:nil];
    }
}

- (void)locationManagerDidResumeLocationUpdates:(CLLocationManager *)manager
{
    if ([self _hasListeners:@"locationupdateresumed"])
    {
        [self fireEvent:@"locationupdateresumed" withObject:nil];
    }
}

#endif

- (void)locationManager:(CLLocationManager *)manager didChangeAuthorizationStatus:(CLAuthorizationStatus)status {
    NSInteger state = [CLLocationManager authorizationStatus];
	BOOL enabled = state == kCLAuthorizationStatusAuthorizedAlways || state == kCLAuthorizationStatusAuthorized;
	NSDictionary* event = @{
                                                      @"enabled":@(enabled),
                                                      @"authorizationStatus": NUMINTEGER(state)
                                                      };
    if ([self _hasListeners:@"authorization"])
    {
        [self fireEvent:@"authorization" withObject: event];
    }
    
    if ([self _hasListeners:@"change"])
    {
        [self fireEvent:@"change" withObject: event];
    }
    
    // The new callback for android parity used inside Ti.Geolocation.requestLocationPermissions()
    if (authorizationCallback != nil && status != kCLAuthorizationStatusNotDetermined) {
        
        int code = 0;
        NSString* errorStr = @"";
        
        switch (status) {
            case kCLAuthorizationStatusAuthorizedAlways:
            case kCLAuthorizationStatusAuthorizedWhenInUse:
                break;
            default:
                code = 1;
                errorStr = @"The user is unable to allow access to location.";
        }
        
        NSMutableDictionary * propertiesDict = [TiUtils dictionaryWithCode:code message:errorStr];
        [propertiesDict setObject:@(state) forKey:@"authorizationStatus"];
        [propertiesDict setObject:@(enabled) forKey:@"enabled"];
        
        NSArray * invocationArray = [[NSArray alloc] initWithObjects:&propertiesDict count:1];
        
        [authorizationCallback call:invocationArray thisObject:self];
        [invocationArray release];
        RELEASE_TO_NIL(errorStr);
        RELEASE_TO_NIL(authorizationCallback);
    }
}


//Using new delegate instead of the old deprecated method - (void)locationManager:didUpdateToLocation:fromLocation:

-(void)locationManager:(CLLocationManager *)manager didUpdateLocations:(NSArray *)locations {
    NSMutableArray* coords = [self locationsDictionary:locations];
    BOOL hasNewPosition = coords && [coords count] > 0;
    BOOL hasPosition = hasNewPosition || (canReportSameLocation && lastLocationDict);
    if (!hasPosition)
    {
        [self startStopLocationManagerIfNeeded];
        return;
    }
    if (hasNewPosition) {
        RELEASE_TO_NIL(lastLocationDict)
        lastLocationDict = [[coords lastObject] retain];
        [coords removeLastObject];
    }
    
    //Must use dictionary because of singleshot.
    NSMutableDictionary *event = [TiUtils dictionaryWithCode:0 message:nil];
    [event setObject:lastLocationDict forKey:@"coords"];
    if ([coords count] > 0) {
        [event setObject:coords forKey:@"olderCoords"];
    }
    if ([self _hasListeners:@"location"])
    {
        [self fireEvent:@"location" withObject:event];
    }
    
    [self fireApplicationAnalyticsIfNeeded:locations];
    [self fireSingleShotLocationIfNeeded:event stopIfNeeded:YES];
}



- (void)locationManager:(CLLocationManager *)manager didUpdateToLocation:(CLLocation *)newLocation fromLocation:(CLLocation *)oldLocation
{
    if (newLocation != nil) {
        if (oldLocation == nil) {
            [self locationManager:manager didUpdateLocations:[NSArray arrayWithObject:newLocation]];
        }
        else{
            [self locationManager:manager didUpdateLocations:[NSArray arrayWithObjects:oldLocation,newLocation,nil]];
        }
    }
    
}


- (void)locationManager:(CLLocationManager *)manager didFailWithError:(NSError *)error
{
    if ([self _hasListeners:@"location"])
    {
        [self fireEvent:@"location" withObject:nil errorCode:[error code] message:[TiUtils messageFromError:error]];
    }
    
    NSMutableDictionary * event = [TiUtils dictionaryWithCode:[error code] message:[TiUtils messageFromError:error]];
    BOOL recheck = [self fireSingleShotLocationIfNeeded:event stopIfNeeded:NO];
    recheck = recheck || [self fireSingleShotHeadingIfNeeded:event stopIfNeeded:NO];
    
    if (recheck)
    {
        // check to make sure we don't need to stop after the single shot
        [self startStopLocationManagerIfNeeded];
    }
}

- (void)locationManager:(CLLocationManager *)manager didUpdateHeading:(CLHeading *)newHeading
{
    //Unfortunately, because of the single shot overloaded here, we can't use the faster eventing.
    NSMutableDictionary * event = [TiUtils dictionaryWithCode:0 message:nil];
    [event setObject:[self headingDictionary:newHeading] forKey:@"heading"];
    
    [self fireEvent:@"heading" withObject:event];
    
    [self fireSingleShotHeadingIfNeeded:event stopIfNeeded:YES];
}

- (BOOL)locationManagerShouldDisplayHeadingCalibration:(CLLocationManager *)manager
{
    if (calibration)
    {
        // fire an event in case the dev wants to hide it
        if ([self _hasListeners:@"calibration"])
        {
            [self fireEvent:@"calibration" withObject:nil];
        }
    }
    return calibration;
}

- (void)dismissHeadingCalibrationDisplay:(id)args
{
    ENSURE_UI_THREAD(dismissHeadingCalibrationDisplay,args);
    [[self locationManager] dismissHeadingCalibrationDisplay];
}

@end

#endif
