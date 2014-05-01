/**
 * Appcelerator APSHTTPClient Library
 * Copyright (c) 2014 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import "APSHTTPClient.h"
#import <libkern/OSAtomic.h>
#import <UIKit/UIKit.h>

@implementation APSHTTPRequest
static int32_t networkActivityCount;
static BOOL _disableNetworkActivityIndicator;
@synthesize url = _url;
@synthesize method = _method;
@synthesize response = _response;
@synthesize filePath = _filePath;
@synthesize requestPassword = _requestPassword;
@synthesize requestUsername = _requestUsername;
@synthesize challengedCredential = _challengedCredential;
@synthesize authenticationChallenge = _authenticationChallenge;
@synthesize persistence = _persistence;
@synthesize authRetryCount = _authRetryCount;
@synthesize showActivity;



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

- (void)dealloc
{
    RELEASE_TO_NIL(_connection);
    RELEASE_TO_NIL(_request);
    RELEASE_TO_NIL(_response);
    RELEASE_TO_NIL(_url);
    RELEASE_TO_NIL(_method);
    RELEASE_TO_NIL(_filePath);
    RELEASE_TO_NIL(_requestUsername);
    RELEASE_TO_NIL(_requestPassword);
    RELEASE_TO_NIL(_postForm);
    RELEASE_TO_NIL(_operation);
    RELEASE_TO_NIL(_userInfo);
    RELEASE_TO_NIL(_headers);
    RELEASE_TO_NIL(_challengedCredential);
    RELEASE_TO_NIL(_authenticationChallenge);
    [super dealloc];
}
- (id)init
{
    self = [super init];
    if (self) {
        [self initialize];
    }
    return self;
}

-(void)initialize
{
    [self setSendDefaultCookies:YES];
    [self setRedirects:YES];
    [self setValidatesSecureCertificate: YES];
    
    showActivity = NO;
    _authRetryCount = 1;
    _persistence = NSURLCredentialPersistenceForSession;
    
    _request = [[NSMutableURLRequest alloc] init];
    [_request setCachePolicy:NSURLRequestReloadIgnoringLocalAndRemoteCacheData];
    _response = [[APSHTTPResponse alloc] init];
    [_response setReadyState: APSHTTPResponseStateUnsent];
}

-(void)abort
{
    [self setCancelled:YES];
    if(_operation != nil) {
        [_operation cancel];
    } else if(_connection != nil) {
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
    RELEASE_TO_NIL(_requestUsername)
    _requestUsername = [requestUsername retain];
    [self updateChallengeCredential];
}

-(void)setRequestPassword:(NSString *)requestPassword
{
    RELEASE_TO_NIL(_requestPassword)
    _requestPassword = [requestPassword retain];
    [self updateChallengeCredential];
}


-(void)send
{
    if (showActivity) {
        [APSHTTPRequest startNetwork];
    }
    if([self filePath]) {
        [_response setFilePath:[self filePath]];
    }
    if([self postForm] != nil) {
        NSData *data = [[self postForm] requestData];
        if([data length] > 0) {
            [_request setHTTPBody:data];
        }
#ifdef DEBUG
        NSLog(@"Data: %@", [NSString stringWithUTF8String: [data bytes]]);
#endif
        NSDictionary *headers = [[self postForm] requestHeaders];
        for (NSString* key in headers)
        {
            [_request setValue:[headers valueForKey:key] forHTTPHeaderField:key];
#ifdef DEBUG
            NSLog(@"Header: %@: %@", key, [headers valueForKey:key]);
#endif
        }
    }
    if(_headers != nil) {
        for (NSString* key in _headers)
        {
            [_request setValue:[_headers valueForKey:key] forHTTPHeaderField:key];
#ifdef DEBUG
            NSLog(@"Header: %@: %@", key, [_headers valueForKey:key]);
#endif
        }
    }
#ifdef DEBUG
    NSLog(@"URL: %@", [self url]);
#endif
    [_request setURL: [self url]];
    
    if([self timeout] > 0) {
        [_request setTimeoutInterval:[self timeout]];
    }
    if([self method] != nil) {
        [_request setHTTPMethod: [self method]];
#ifdef DEBUG
        NSLog(@"Method: %@", [self method]);
#endif
    }
    [_request setHTTPShouldHandleCookies:[self sendDefaultCookies]];
    
    if([self synchronous]) {
        if(!_challengedCredential && [self requestUsername] != nil && [self requestPassword] != nil && [_request valueForHTTPHeaderField:@"Authorization"] == nil) {
            NSString *authString = [APSHTTPHelper base64encode:[[NSString stringWithFormat:@"%@:%@",[self requestUsername], [self requestPassword]] dataUsingEncoding:NSUTF8StringEncoding]];
            [_request setValue:[NSString stringWithFormat:@"Basic %@", authString] forHTTPHeaderField:@"Authorization"];
        }
        NSURLResponse *response;
        NSError *error = nil;
        NSData *responseData = [NSURLConnection sendSynchronousRequest:_request returningResponse:&response error:&error];
        [_response appendData:responseData];
        [_response setResponse:response];
        [_response setError:error];
        [_response setRequest:_request];
        [_response setReadyState:APSHTTPResponseStateDone];
        [_response setConnected:NO];
    } else {
        [_response setRequest:_request];
        [_response setReadyState:APSHTTPResponseStateOpened];
        if([_delegate respondsToSelector:@selector(request:onReadyStateChage:)]) {
            [_delegate request:self onReadyStateChage:_response];
        }
        
        _connection = [[NSURLConnection alloc] initWithRequest: _request
                                                      delegate: self
                                              startImmediately: NO
                               ];
        
        if([self theQueue]) {
            RELEASE_TO_NIL(_operation);
            _operation = [[APSHTTPOperation alloc] initWithConnection: self];
            [_operation setIndex:[[self theQueue] operationCount]];
            [[self theQueue] addOperation: _operation];
           
        } else {
            [_connection start];
        }
    }
    
}

-(void)setCachePolicy:(NSURLRequestCachePolicy)cache
{
    [_request setCachePolicy:cache];
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

- (BOOL)connectionShouldUseCredentialStorage:(NSURLConnection *)connection{
	if([[self delegate] respondsToSelector:@selector(request:connectionShouldUseCredentialStorage:)])
    {
        return [[self delegate] request:self connectionShouldUseCredentialStorage:connection];
    }
	return YES;
}

-(void)connection:(NSURLConnection *)connection willSendRequestForAuthenticationChallenge:(NSURLAuthenticationChallenge *)challenge
{
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
    if(![self validatesSecureCertificate]) {
        if (
            [authMethod isEqualToString:NSURLAuthenticationMethodServerTrust] &&
            [challenge.protectionSpace.host isEqualToString:[[self url] host]]
            ) {
            [[challenge sender] useCredential:
             [NSURLCredential credentialForTrust: [[challenge protectionSpace] serverTrust]]
                   forAuthenticationChallenge: challenge];
        }
    }
    
    if ([authMethod isEqualToString:NSURLAuthenticationMethodDefault]) {
        self.authenticationChallenge = challenge;
        
        if(_challengedCredential) { //if password and username
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
            //        [[challenge sender] continueWithoutCredentialForAuthenticationChallenge:challenge];
        }
        else {
            [self removeCredentialsWithURLURLProtectionSpace:challenge.protectionSpace];
            if([[self delegate] respondsToSelector:@selector(request:onRequestForAuthenticationChallenge:)])
            {
                [[self delegate] request:self onRequestForAuthenticationChallenge:challenge];
            }
            else {
                [[challenge sender] continueWithoutCredentialForAuthenticationChallenge:challenge];
            }
        }
    }
    else {
        [[challenge sender] continueWithoutCredentialForAuthenticationChallenge:challenge];
    }
}

-(NSURLRequest*)connection:(NSURLConnection *)connection willSendRequest:(NSURLRequest *)request redirectResponse:(NSURLResponse *)response
{
#ifdef DEBUG
    NSLog(@"Code %li Redirecting from: %@ to: %@",(long)[(NSHTTPURLResponse*)response statusCode], [_request URL] ,[request URL]);
#endif
    [_response setConnected:YES];
    [_response setResponse: response];
    [_response setRequest:request];

    if([[self delegate] respondsToSelector:@selector(request:onRedirect:)])
    {
        [[self delegate] request:self onRedirect:_response];
    }
    if(![self redirects] && [_response status] != 0)
    {
        return nil;
    }
    
    //http://tewha.net/2012/05/handling-302303-redirects/
    if (response) {
        NSMutableURLRequest *r = [[_request mutableCopy] autorelease];
        [r setURL: [request URL]];
        RELEASE_TO_NIL(_request);
        _request = [r retain];
        return r;
    } else {
        return request;
    }
}

- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response
{
#ifdef DEBUG
    NSLog(@"%s", __PRETTY_FUNCTION__);
#endif
    [_response setReadyState:APSHTTPResponseStateHeaders];
    [_response setConnected:YES];
    [_response setResponse: response];
    if([_response status] == 0) {
        [self connection:connection
        didFailWithError:[NSError errorWithDomain: [_response location]
                                             code: [_response status]
                                         userInfo: @{NSLocalizedDescriptionKey: [NSHTTPURLResponse localizedStringForStatusCode:[(NSHTTPURLResponse*)response statusCode]]}
                          ]];
        return;
    }
    _expectedDownloadResponseLength = [response expectedContentLength];
    
    if([_delegate respondsToSelector:@selector(request:onReadyStateChage:)]) {
        [_delegate request:self onReadyStateChage:_response];
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
#ifdef DEBUG
    NSLog(@"%s", __PRETTY_FUNCTION__);
#endif
    if([_response readyState] != APSHTTPResponseStateLoading) {
        [_response setReadyState:APSHTTPResponseStateLoading];
        if([_delegate respondsToSelector:@selector(request:onReadyStateChage:)]) {
            [_delegate request:self onReadyStateChage:_response];
        }
    }
    [_response appendData:data];
    [_response setDownloadProgress: (float)[_response responseLength] / (float)_expectedDownloadResponseLength];
    if([_delegate respondsToSelector:@selector(request:onDataStream:)]) {
        [_delegate request:self onDataStream:_response];
    }
    
}

-(void)connection:(NSURLConnection *)connection didSendBodyData:(NSInteger)bytesWritten totalBytesWritten:(NSInteger)totalBytesWritten totalBytesExpectedToWrite:(NSInteger)totalBytesExpectedToWrite
{
    if([_response readyState] != APSHTTPResponseStateLoading) {
        [_response setReadyState:APSHTTPResponseStateLoading];
        if([_delegate respondsToSelector:@selector(request:onReadyStateChage:)]) {
            [_delegate request:self onReadyStateChage:_response];
        }
    }
    [_response setUploadProgress: (float)totalBytesWritten / (float)totalBytesExpectedToWrite];
    if([_delegate respondsToSelector:@selector(request:onSendStream:)]) {
        [_delegate request:self onSendStream:_response];
    }
}

- (void)connectionDidFinishLoading:(NSURLConnection *)connection
{
    if (showActivity) {
        [APSHTTPRequest stopNetwork];
    }
    if(_operation != nil) {
        [_operation setFinished:YES];
    }
#ifdef DEBUG
    NSLog(@"%s", __PRETTY_FUNCTION__);
#endif
    [_response setDownloadProgress:1.f];
    [_response setUploadProgress:1.f];
    [_response setReadyState:APSHTTPResponseStateDone];
    [_response setConnected:NO];
     
    if([_delegate respondsToSelector:@selector(request:onReadyStateChage:)]) {
        [_delegate request:self onReadyStateChage:_response];
    }
    if([_delegate respondsToSelector:@selector(request:onSendStream:)]) {
        [_delegate request:self onSendStream:_response];
    }
    if([_delegate respondsToSelector:@selector(request:onDataStream:)]) {
        [_delegate request:self onDataStream:_response];
    }
    if([_delegate respondsToSelector:@selector(request:onLoad:)]) {
        [_delegate request:self onLoad:_response];
    }
}

- (void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error
{
	if (showActivity) {
        [APSHTTPRequest stopNetwork];
    }
    if(_operation != nil) {
        [_operation setFinished:YES];
    }
#ifdef DEBUG
    NSLog(@"%s", __PRETTY_FUNCTION__);
#endif
    [_response setReadyState:APSHTTPResponseStateDone];
    if([_delegate respondsToSelector:@selector(request:onReadyStateChage:)]) {
        [_delegate request:self onReadyStateChage:_response];
    }
    [_response setConnected:NO];
    [_response setError:error];
    if([_delegate respondsToSelector:@selector(request:onError:)]) {
        [_delegate request:self onError:_response];
    }
}

@end