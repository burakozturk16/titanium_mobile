/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#import "TiBase.h"
#import "TiEvaluator.h"
#import "KrollCallback.h"
#import "KrollObject.h"
#import <pthread.h>

@class KrollBridge;
@class KrollObject;

//Common exceptions to throw when the function call was improper
extern NSString * const TiExceptionInvalidType;
extern NSString * const TiExceptionNotEnoughArguments;
extern NSString * const TiExceptionRangeError;

extern NSString * const TiExceptionOSError;

//This is when a normally allowed command is not allowed (Say, adding a row to a table when it already is added elsewhere)
extern NSString * const TiExceptionInternalInconsistency;

//Should be rare, but also useful if arguments are used improperly.
extern NSString * const TiExceptionInternalInconsistency;

//Rare exceptions to indicate a bug in the titanium code (Eg, function that a subclass should have implemented)
extern NSString * const TiExceptionUnimplementedFunction;

@class TiHost;
@class TiProxy;

typedef enum {
	NativeBridge,
	WebBridge
} TiProxyBridgeType;


/**
 The proxy delegate protocol
 */
@protocol TiProxyDelegate<NSObject>

@required

/**
 Tells the delegate that the proxy property has changed.
 @param key The property name.
 @param oldValue An old value of the property.
 @param newValue A new value of the property.
 @param proxy The proxy where the property has changed.
 */
-(void)propertyChanged:(NSString*)key oldValue:(id)oldValue newValue:(id)newValue proxy:(TiProxy*)proxy;

@optional

/**
 Tells the delegate to read proxy values.
 @param keys The enumeration of keys to read.
 */
-(void)readProxyValuesWithKeys:(id<NSFastEnumeration>)keys;

/**
 Tells the delegate that a listener has been added to the proxy.
 @param type The listener type.
 @param count The current number of active listeners
 */
-(void)listenerAdded:(NSString*)type count:(int)count;

/**
 Tells the delegate that a listener has been removed to the proxy.
 @param type The listener type.
 @param count The current number of active listeners after the remove
 */
-(void)listenerRemoved:(NSString*)type count:(int)count;

/**
 Tells the delegate to detach from proxy.
 */
-(void)detachProxy;

@end

SEL SetterForKrollProperty(NSString * key);
SEL SetterWithObjectForKrollProperty(NSString * key);

void DoProxyDelegateChangedValuesWithProxy(UIView<TiProxyDelegate> * target, NSString * key, id oldValue, id newValue, TiProxy * proxy);
void DoProxyDelegateReadValuesWithKeysFromProxy(UIView<TiProxyDelegate> * target, id<NSFastEnumeration> keys, TiProxy * proxy);
//Why are these here? Because they can be commonly used between TiUIView and TiUITableViewCell.


/**
 The base class for Titanium proxies.
 */
@interface TiProxy : NSObject<KrollTargetable> {
@private
	NSMutableDictionary *listeners;
	BOOL destroyed;
	id<TiProxyDelegate> modelDelegate;
	NSURL *baseURL;
	NSString *krollDescription;
	pthread_rwlock_t listenerLock;
	BOOL reproxying;
@protected
	NSMutableDictionary *dynprops; 
	pthread_rwlock_t dynpropsLock; // NOTE: You must respect the dynprops lock when accessing dynprops elsewhere!

	int bridgeCount;
	KrollObject * pageKrollObject;
	id<TiEvaluator> pageContext;
	id<TiEvaluator> executionContext;
}

-(void)boundBridge:(id<TiEvaluator>)newBridge withKrollObject:(KrollObject *)newKrollObject;
-(void)unboundBridge:(id<TiEvaluator>)oldBridge;


@property(readonly,nonatomic)			id<TiEvaluator> pageContext;
@property(readonly,nonatomic)			id<TiEvaluator> executionContext;

/**
 Provides access to proxy delegate.
 */
@property(nonatomic,retain,readwrite)	id<TiProxyDelegate> modelDelegate;

+(BOOL)shouldRegisterOnInit;

#pragma mark Private 

-(id)_initWithPageContext:(id<TiEvaluator>)context;
-(id)_initWithPageContext:(id<TiEvaluator>)context args:(NSArray*)args;
-(void)_initWithProperties:(NSDictionary*)properties;
-(BOOL)_hasListeners:(NSString*)type;
-(void)_fireEventToListener:(NSString*)type withObject:(id)obj listener:(KrollCallback*)listener thisObject:(TiProxy*)thisObject_;
-(id)_proxy:(TiProxyBridgeType)type;
-(void)contextWasShutdown:(id<TiEvaluator>)context;
-(TiHost*)_host;
-(NSURL*)_baseURL;
-(void)_setBaseURL:(NSURL*)url;
-(void)_destroy;
-(void)_configure;
-(void)_dispatchWithObjectOnUIThread:(NSArray*)args;
-(void)didReceiveMemoryWarning:(NSNotification*)notification;
-(TiProxy*)currentWindow;
-(void)contextShutdown:(id)sender;
-(id)toString:(id)args;

/**
 Returns if the proxy was destroyed.
 @return _YES_ if destroyed, _NO_ otherwise.
 */
-(BOOL)destroyed;

/**
 Tells the proxy that it is in reproxying stage.
 @param yn _YES_ if the proxy is in reproxying stage, _NO_ otherwise.
 */
-(void)setReproxying:(BOOL)yn;

/**
 Returns if the proxy is in reproxying stage.
 @return _YES_ if the proxy is in reproxying stage, _NO_ otherwise.
 */
-(BOOL)inReproxy;

#pragma mark Utility
-(KrollObject *)krollObjectForContext:(KrollContext *)context;

-(BOOL)retainsJsObjectForKey:(NSString *)key;

//TODO: Find everywhere were we retain a proxy in a non-assignment way, and do remember/forget properly.

/**
 Tells the proxy to associate another proxy with it.
 
 The associated proxy will be retained.
 @param rememberedProxy The proxy to remember.
 */
-(void)rememberProxy:(TiProxy *)rememberedProxy;

/**
 Tells the proxy to deassociate another proxy with it.
 
 The deassociated proxy will be released.
 @param forgottenProxy The proxy to forget.
 */
-(void)forgetProxy:(TiProxy *)forgottenProxy;

//These are when, say, a window is opened, so you want to do tiValueProtect to make SURE it doesn't go away.

/**
 Tells the proxy to retain associated JS object.
 */
-(void)rememberSelf;

/**
 Tells the proxy to release associated JS object.
 */
-(void)forgetSelf;

//SetCallback is done internally by setValue:forUndefinedKey:
-(void)fireCallback:(NSString*)type withArg:(NSDictionary *)argDict withSource:(id)source;

#pragma mark Public 

/**
 Returns keys of all properties associated with the proxy.
 */
-(id<NSFastEnumeration>)allKeys;

-(NSArray *)keySequence;

+(void)throwException:(NSString *) reason subreason:(NSString*)subreason location:(NSString *)location;
-(void)throwException:(NSString *) reason subreason:(NSString*)subreason location:(NSString *)location;
-(void)addEventListener:(NSArray*)args;
-(void)removeEventListener:(NSArray*)args;

-(void)fireEvent:(id)args;
-(void)fireEvent:(NSString*)type withObject:(id)obj;
-(void)fireEvent:(NSString*)type withObject:(id)obj withSource:(id)source;
-(void)fireEvent:(NSString*)type withObject:(id)obj withSource:(id)source propagate:(BOOL)yn;
-(void)fireEvent:(NSString*)type withObject:(id)obj propagate:(BOOL)yn;

-(NSDictionary*)allProperties;
-(void)initializeProperty:(NSString*)name defaultValue:(id)value;
-(void)replaceValue:(id)value forKey:(NSString*)key notification:(BOOL)notify;
-(void)deleteKey:(NSString*)key;

-(id)sanitizeURL:(id)value;

-(void)setExecutionContext:(id<TiEvaluator>)context;

@end
