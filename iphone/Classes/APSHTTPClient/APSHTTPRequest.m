/**
 * Appcelerator APSHTTPClient Library
 * Copyright (c) 2014 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import "APSHTTPClient.h"
#import <libkern/OSAtomic.h>
#import <UIKit/UIKit.h>

@interface APSHTTPRequest () <NSURLConnectionDataDelegate>
@end


@implementation APSHTTPRequest {
    long long _expectedDownloadResponseLength;
    NSURLConnection *_connection;
    NSMutableDictionary *_headers;
    NSMutableArray* _trustedHosts;
}
static int32_t networkActivityCount;
static BOOL _disableNetworkActivityIndicator;



+(void)startNetwork
{
	if (OSAtomicIncrement32(&networkActivityCount) == 1)
	{
        dispatch_async(dispatch_get_main_queue(), ^{
            [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:!_disableNetworkActivityIndicator];
        });
	}
}

+(void)stopNetwork
{
	if (OSAtomicDecrement32(&networkActivityCount) == 0)
	{
        dispatch_async(dispatch_get_main_queue(), ^{
            [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:NO];
        });
	}
}

+ (void)setDisableNetworkActivityIndicator:(BOOL)value
{
	_disableNetworkActivityIndicator = value;
	[[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:(!_disableNetworkActivityIndicator && (networkActivityCount > 0))];
}

+ (BOOL)disableNetworkActivityIndicator
{
	return _disableNetworkActivityIndicator;
}

- (id)init
{
    self = [super init];
    if (self) {
		_showActivity = NO;
        [self setSendDefaultCookies:YES];
        [self setRedirects:YES];
        [self setValidatesSecureCertificate: YES];
        
        _request = [[NSMutableURLRequest alloc] init];
        [_request setCachePolicy:NSURLRequestReloadIgnoringLocalAndRemoteCacheData];
        _response = [[APSHTTPResponse alloc] init];
        [_response setReadyState: APSHTTPResponseStateUnsent];
    }
    return self;
}

-(void)abort
{
    [self setCancelled:YES];
    if(_connection != nil) {
        [_connection cancel];
        [self connection:_connection didFailWithError:
         [NSError errorWithDomain:@"APSHTTPErrorDomain"
                             code:APSRequestErrorCancel
                         userInfo:[NSDictionary dictionaryWithObjectsAndKeys:@"The request was cancelled",NSLocalizedDescriptionKey,nil]]
         ];
    }
}

-(NSURLConnection*)connection
{
    return _connection;
}

-(void)addTrustedHost:(NSString*)host
{
    if (host == nil) return;
    if (_trustedHosts == nil) {
        _trustedHosts = [[NSMutableArray alloc] init];
    }
    [_trustedHosts addObject:host];
}

-(void)removeTrustedHost:(NSString*)host
{
    if (host == nil || _trustedHosts == nil) return;
    [_trustedHosts removeObject:host];
}

-(void)updateChallengeCredential
{
    if ([_requestUsername length] > 0 && [_requestPassword length] >0) {
        self.challengedCredential = [NSURLCredential credentialWithUser:_requestUsername password:_requestPassword persistence:_persistence];
    }
    else {
        self.challengedCredential = nil;
    }
}

-(void)setRequestUsername:(NSString *)requestUsername
{
    _requestUsername = requestUsername;
    [self updateChallengeCredential];
}

-(void)setRequestPassword:(NSString *)requestPassword
{
    _requestPassword = requestPassword;
    [self updateChallengeCredential];
}


-(void)send
{
    if (_showActivity) {
        [APSHTTPRequest startNetwork];
    }
    if([self filePath]) {
        [self.response setFilePath:[self filePath]];
    }
    if([self postForm] != nil) {
        NSData *data = [[self postForm] requestData];
        if([data length] > 0) {
            [self.request setHTTPBody:data];
        }
#ifdef APSHTTP_DEBUG
        DebugLog(@"Data: %@", [NSString stringWithUTF8String: [data bytes]]);
#endif
        NSDictionary *headers = [[self postForm] requestHeaders];
        for (NSString* key in headers)
        {
            [self.request setValue:[headers valueForKey:key] forHTTPHeaderField:key];
#ifdef APSHTTP_DEBUG
            NSLog(@"Header: '%@': %@", key, [headers valueForKey:key]);
#endif
        }
    }
    if(_headers != nil) {
        for (NSString* key in _headers)
        {
            [self.request setValue:[_headers valueForKey:key] forHTTPHeaderField:key];
#ifdef APSHTTP_DEBUG
            NSLog(@"Header: %@: %@", key, [_headers valueForKey:key]);
#endif
        }
    }
#ifdef APSHTTP_DEBUG
    NSLog(@"URL: %@", [self url]);
#endif
    [self.request setURL: [self url]];
    
    if([self timeout] > 0) {
        [self.request setTimeoutInterval:[self timeout]];
    }
    if([self method] != nil) {
        [self.request setHTTPMethod: [self method]];
#ifdef APSHTTP_DEBUG
        NSLog(@"Method: %@", [self method]);
#endif
    }
    [self.request setHTTPShouldHandleCookies:[self sendDefaultCookies]];
    [self.request setCachePolicy:self.cachePolicy];
    
    if([self synchronous]) {
        if(!_challengedCredential && [self requestUsername] != nil && [self requestPassword] != nil && [self.request valueForHTTPHeaderField:@"Authorization"] == nil) {
            NSString *authString = [APSHTTPHelper base64encode:[[NSString stringWithFormat:@"%@:%@",[self requestUsername], [self requestPassword]] dataUsingEncoding:NSUTF8StringEncoding]];
            [self.request setValue:[NSString stringWithFormat:@"Basic %@", authString] forHTTPHeaderField:@"Authorization"];
        }
        NSURLResponse *response;
        NSError *error = nil;
        NSData *responseData = [NSURLConnection sendSynchronousRequest:self.request returningResponse:&response error:&error];
        [self.response appendData:responseData];
        [self.response updateResponseParamaters:response];
        [self.response setError:error];
        [self.response updateRequestParamaters:self.request];
        [self.response setReadyState:APSHTTPResponseStateDone];
        [self.response setConnected:NO];
    } else {
        [self.response updateRequestParamaters:self.request];
        [self.response setReadyState:APSHTTPResponseStateOpened];
        if([_delegate respondsToSelector:@selector(request:onReadyStateChage:)]) {
            [_delegate request:self onReadyStateChage:self.response];
        }
        
        _connection = [[NSURLConnection alloc] initWithRequest: self.request
                                                      delegate: self
                                              startImmediately: NO
                               ];
        
        if([self theQueue]) {
            [_connection setDelegateQueue:[self theQueue]];
        } else {
            if (![NSThread isMainThread]) {
                NSRunLoop *runLoop = [NSRunLoop currentRunLoop];
                NSMutableSet *runLoopModes = [NSMutableSet setWithObjects:NSDefaultRunLoopMode, NSRunLoopCommonModes, nil];
                if ([runLoop currentMode]) {
                    if (![runLoopModes containsObject:[runLoop currentMode]]) {
                        [runLoopModes addObject:[runLoop currentMode]];
                    }
                } else {
#ifdef APSHTTP_DEBUG
                    DebugLog(@"%s [Line %@] [WARN] [[NSRunLoop currentRunLoop] currentMode] is nil", __PRETTY_FUNCTION__, @(__LINE__));
#endif
                }
                for (NSString *runLoopMode in runLoopModes) {
#ifdef APSHTTP_DEBUG
                    NSLog(@"MDL: runLoopMode = %@", runLoopMode);
#endif
                    [_connection scheduleInRunLoop:runLoop forMode:runLoopMode];
                }
            }
        }
        [_connection start];
    }
    
}

-(void)addRequestHeader:(NSString *)key value:(NSString *)value
{
    if(_headers == nil) {
        _headers = [[NSMutableDictionary alloc] init];
    }
    [_headers setValue:value forKey:key];
}

- (void)removeCredentialsWithURLURLProtectionSpace:(NSURLProtectionSpace *)space
{
    NSURLCredentialStorage* credentialStorage = [NSURLCredentialStorage sharedCredentialStorage];
    NSDictionary* credentials = [credentialStorage credentialsForProtectionSpace:space];
    for (NSString* key in [credentials allKeys]) {
        NSURLCredential* cred = [credentials objectForKey:key];
        [credentialStorage removeCredential:cred forProtectionSpace:space];
    }
}


- (BOOL)connection:(NSURLConnection *)connection canAuthenticateAgainstProtectionSpace:(NSURLProtectionSpace *)protectionSpace{
    if([[self delegate] respondsToSelector:@selector(request:canAuthenticateAgainstProtectionSpace:)])
    {
        return [[self delegate] request:self canAuthenticateAgainstProtectionSpace:protectionSpace];
    }
	if (([protectionSpace authenticationMethod] == NSURLAuthenticationMethodClientCertificate)
        ||  ([protectionSpace authenticationMethod] == NSURLAuthenticationMethodServerTrust))
	{
		return NO;
	}
	else
	{
		return YES;
	}
}

- (void)connection:(NSURLConnection *)connection didReceiveAuthenticationChallenge:(NSURLAuthenticationChallenge *)challenge {
    if ([challenge.protectionSpace.authenticationMethod isEqualToString:NSURLAuthenticationMethodServerTrust])
        if (![self validatesSecureCertificate] || [_trustedHosts containsObject:challenge.protectionSpace.host])
            [challenge.sender useCredential:[NSURLCredential credentialForTrust:challenge.protectionSpace.serverTrust] forAuthenticationChallenge:challenge];
    
    [challenge.sender continueWithoutCredentialForAuthenticationChallenge:challenge];
}

- (BOOL)connectionShouldUseCredentialStorage:(NSURLConnection *)connection{
	if([[self delegate] respondsToSelector:@selector(request:connectionShouldUseCredentialStorage:)])
    {
        return [[self delegate] request:self connectionShouldUseCredentialStorage:connection];
    }
	return YES;
}

-(void)connection:(NSURLConnection *)connection willSendRequestForAuthenticationChallenge:(NSURLAuthenticationChallenge *)challenge
{
    BOOL useSubDelegate = ([self connectionDelegate] != nil && [[self connectionDelegate] respondsToSelector:@selector(connection:willSendRequestForAuthenticationChallenge:)]);
    if(useSubDelegate && [[self connectionDelegate] respondsToSelector:@selector(willHandleChallenge:forConnection:)]) {
        useSubDelegate = [[self connectionDelegate] willHandleChallenge:challenge forConnection:connection];
    }
    
    if(useSubDelegate) {
        [[self connectionDelegate] connection:connection willSendRequestForAuthenticationChallenge:challenge];
        return;
    }
    
    NSString* authMethod = [[challenge protectionSpace] authenticationMethod];
    if ([challenge previousFailureCount]) {
        NSURLCredential* credential = [[NSURLCredentialStorage sharedCredentialStorage] defaultCredentialForProtectionSpace:challenge.protectionSpace];
        if(credential && [challenge previousFailureCount]  == 1) {
            [challenge.sender useCredential:credential forAuthenticationChallenge:challenge];
            return;
        }
        else if ([challenge previousFailureCount] > _authRetryCount) {
            [[challenge sender] cancelAuthenticationChallenge:challenge];
        }
    }
    if ([challenge.protectionSpace.authenticationMethod isEqualToString:NSURLAuthenticationMethodServerTrust] && [challenge.protectionSpace.host isEqualToString:[[self url] host]])
        if (![self validatesSecureCertificate] || [_trustedHosts containsObject:challenge.protectionSpace.host])
            [challenge.sender useCredential:[NSURLCredential credentialForTrust:challenge.protectionSpace.serverTrust] forAuthenticationChallenge:challenge];
    
    BOOL handled = NO;

    if ([authMethod isEqualToString:NSURLAuthenticationMethodServerTrust]) {
        if ( ([challenge.protectionSpace.host isEqualToString:[[self url] host]]) && (![self validatesSecureCertificate]) ){
            handled = YES;
            [[challenge sender] useCredential:
             [NSURLCredential credentialForTrust: [[challenge protectionSpace] serverTrust]]
                   forAuthenticationChallenge: challenge];
        }
	} else if ([authMethod isEqualToString:NSURLAuthenticationMethodDefault] && _challengedCredential) {
		handled = YES;
        self.authenticationChallenge = challenge;
        [challenge.sender useCredential:_challengedCredential forAuthenticationChallenge:challenge];
            //        [[challenge sender] useCredential:
            //         [NSURLCredential credentialWithUser:_requestUsername
            //                                    password:_requestPassword
            //                                 persistence:_persistence]
            //               forAuthenticationChallenge:challenge];
            if([[self delegate] respondsToSelector:@selector(request:onUseAuthenticationChallenge:)])
            {
                [[self delegate] request:self onUseAuthenticationChallenge:challenge];
            }
    } else if ( [authMethod isEqualToString:NSURLAuthenticationMethodDefault] || [authMethod isEqualToString:NSURLAuthenticationMethodHTTPBasic]
               || [authMethod isEqualToString:NSURLAuthenticationMethodNTLM] || [authMethod isEqualToString:NSURLAuthenticationMethodHTTPDigest]) {
        if([self requestPassword] != nil && [self requestUsername] != nil) {
            handled = YES;
            [[challenge sender] useCredential:
             [NSURLCredential credentialWithUser:[self requestUsername]
                                        password:[self requestPassword]
                                     persistence:NSURLCredentialPersistenceForSession]
                   forAuthenticationChallenge:challenge];
        }
    }
    
    if (!handled) {
        if ([[challenge sender] respondsToSelector:@selector(performDefaultHandlingForAuthenticationChallenge:)]) {
            [[challenge sender] performDefaultHandlingForAuthenticationChallenge:challenge];
        } else {
            [[challenge sender] continueWithoutCredentialForAuthenticationChallenge:challenge];
        }
    }
}

-(NSURLRequest*)connection:(NSURLConnection *)connection willSendRequest:(NSURLRequest *)request redirectResponse:(NSURLResponse *)response
{
#ifdef APSHTTP_DEBUG
    NSLog(@"Code %li Redirecting from: %@ to: %@",(long)[(NSHTTPURLResponse*)response statusCode], [self.request URL] ,[request URL]);
#endif
    [self.response setConnected:YES];
    [self.response updateResponseParamaters:response];
    [self.response updateRequestParamaters:self.request];

    if([[self delegate] respondsToSelector:@selector(request:onRedirect:)])
    {
        [[self delegate] request:self onRedirect:self.response];
    }
    if(![self redirects] && [self.response status] != 0)
    {
        return nil;
    }
    
    //http://tewha.net/2012/05/handling-302303-redirects/
    if (response) {
        self.request.URL = request.URL;
        return self.request;
    } else {
        return request;
    }
}

- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response
{
#ifdef APSHTTP_DEBUG
    NSLog(@"%s", __PRETTY_FUNCTION__);
#endif
    [self.response setReadyState:APSHTTPResponseStateHeaders];
    [self.response setConnected:YES];
    [self.response updateResponseParamaters:response];
    if([self.response status] == 0) {
        [self connection:connection
        didFailWithError:[NSError errorWithDomain: [self.response location]
                                             code: [self.response status]
                                         userInfo: @{NSLocalizedDescriptionKey: [NSHTTPURLResponse localizedStringForStatusCode:[(NSHTTPURLResponse*)response statusCode]]}
                          ]];
        return;
    }
    _expectedDownloadResponseLength = [response expectedContentLength];
    
    if([_delegate respondsToSelector:@selector(request:onReadyStateChage:)]) {
        [_delegate request:self onReadyStateChage:self.response];
    }
    if(_authenticationChallenge && [_authenticationChallenge.protectionSpace.protocol isEqualToString:response.URL.scheme] &&
       [_authenticationChallenge.protectionSpace.host isEqualToString:response.URL.host]){
        
        if([self requestPassword] != nil && [self requestUsername] != nil) {
            if(_challengedCredential && _authenticationChallenge && [(NSHTTPURLResponse *)response statusCode] == 200){
                [[NSURLCredentialStorage sharedCredentialStorage] setCredential:_challengedCredential forProtectionSpace:_authenticationChallenge.protectionSpace];
            }
        }
    }
}

- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data
{
#ifdef APSHTTP_DEBUG
    NSLog(@"%s", __PRETTY_FUNCTION__);
#endif
    if([self.response readyState] != APSHTTPResponseStateLoading) {
        [self.response setReadyState:APSHTTPResponseStateLoading];
        if([_delegate respondsToSelector:@selector(request:onReadyStateChage:)]) {
            [_delegate request:self onReadyStateChage:self.response];
        }
    }
    [self.response appendData:data];
    [self.response setDownloadProgress: (float)[self.response responseLength] / (float)_expectedDownloadResponseLength];
    if([_delegate respondsToSelector:@selector(request:onDataStream:)]) {
        [_delegate request:self onDataStream:self.response];
    }
    
}

-(void)connection:(NSURLConnection *)connection didSendBodyData:(NSInteger)bytesWritten totalBytesWritten:(NSInteger)totalBytesWritten totalBytesExpectedToWrite:(NSInteger)totalBytesExpectedToWrite
{
    if([self.response readyState] != APSHTTPResponseStateLoading) {
        [self.response setReadyState:APSHTTPResponseStateLoading];
        if([_delegate respondsToSelector:@selector(request:onReadyStateChage:)]) {
            [_delegate request:self onReadyStateChage:self.response];
        }
    }
    [self.response setUploadProgress: (float)totalBytesWritten / (float)totalBytesExpectedToWrite];
    if([_delegate respondsToSelector:@selector(request:onSendStream:)]) {
        [_delegate request:self onSendStream:self.response];
    }
}

- (void)connectionDidFinishLoading:(NSURLConnection *)connection
{
#ifdef APSHTTP_DEBUG
    NSLog(@"%s", __PRETTY_FUNCTION__);
#endif
	if (_showActivity) {
        [APSHTTPRequest stopNetwork];
    }
    [self.response setDownloadProgress:1.f];
    [self.response setUploadProgress:1.f];
    [self.response setReadyState:APSHTTPResponseStateDone];
    [self.response setConnected:NO];
     
    if([_delegate respondsToSelector:@selector(request:onReadyStateChage:)]) {
        [_delegate request:self onReadyStateChage:self.response];
    }
    if([_delegate respondsToSelector:@selector(request:onSendStream:)]) {
        [_delegate request:self onSendStream:self.response];
    }
    if([_delegate respondsToSelector:@selector(request:onDataStream:)]) {
        [_delegate request:self onDataStream:self.response];
    }
    if([_delegate respondsToSelector:@selector(request:onLoad:)]) {
        [_delegate request:self onLoad:self.response];
    }
}

- (void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error
{
	if (_showActivity) {
        [APSHTTPRequest stopNetwork];
    }
	if([self connectionDelegate] != nil && [[self connectionDelegate] respondsToSelector:@selector(connection:didFailWithError:)]) {
		[[self connectionDelegate] connection:connection didFailWithError:error];
	}
#ifdef APSHTTP_DEBUG
    NSLog(@"%s", __PRETTY_FUNCTION__);
#endif
    [self.response setReadyState:APSHTTPResponseStateDone];
    if([_delegate respondsToSelector:@selector(request:onReadyStateChage:)]) {
        [_delegate request:self onReadyStateChage:self.response];
    }
    [self.response setConnected:NO];
    [self.response setError:error];
    if([_delegate respondsToSelector:@selector(request:onError:)]) {
        [_delegate request:self onError:self.response];
    }
}

@end