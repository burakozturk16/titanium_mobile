//
//  TiCache.m
//
// Copyright (c) 2008-2009, Route-Me Contributors
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// * Redistributions of source code must retain the above copyright notice, this
//   list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright notice,
//   this list of conditions and the following disclaimer in the documentation
//   and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

#import <sys/utsname.h>

#import "TiCache.h"
#import "TiMemoryCache.h"
#import "TiDatabaseCache.h"
#import "TiBase.h"

@interface TiCache (Configuration)

- (id <TiCache>)memoryCacheWithConfig:(NSDictionary *)cfg;
- (id <TiCache>)databaseCacheWithConfig:(NSDictionary *)cfg;

@end

@implementation TiCache
{
    NSMutableArray *_caches;
    
    // The memory cache, if we have one
    // This one has its own variable because we want to propagate cache hits down in
    // the cache hierarchy up to the memory cache
    TiMemoryCache *_memoryCache;
    NSTimeInterval _expiryPeriod;
    
    dispatch_queue_t _cacheQueue;
}

- (id)initWithConfig:(NSArray*)cacheCfg expiryPeriod:(NSTimeInterval)period
{
    if (!(self = [super init]))
        return nil;
    
    _caches = [NSMutableArray new];
    _cacheQueue = dispatch_queue_create("ti.tileCacheQueue", DISPATCH_QUEUE_CONCURRENT);
    
    _memoryCache = nil;
    _expiryPeriod = period;
    
    
    if (!cacheCfg)
        cacheCfg = [NSArray arrayWithObjects:
                    [NSDictionary dictionaryWithObject: @"memory-cache" forKey: @"type"],
                    [NSDictionary dictionaryWithObject: @"db-cache"     forKey: @"type"],
                    nil];
    
    for (id cfg in cacheCfg)
    {
        id <TiCache> newCache = nil;
        
        @try {
            
            NSString *type = [cfg valueForKey:@"type"];
            
            if ([@"memory-cache" isEqualToString:type])
            {
                _memoryCache = [[self memoryCacheWithConfig:cfg] retain];
                continue;
            }
            
            if ([@"db-cache" isEqualToString:type])
                newCache = [self databaseCacheWithConfig:cfg];
            
            if (newCache)
                [_caches addObject:newCache];
            else
                DebugLog(@"failed to create cache of type %@", type);
            
        }
        @catch (NSException * e) {
            DebugLog(@"*** configuration error: %@", [e reason]);
        }
    }
    
    return self;
}

- (id)init
{
    if (!(self = [self initWithConfig:nil expiryPeriod:0]))
        return nil;
    
    return self;
}

- (void)dealloc
{
    
    dispatch_barrier_sync(_cacheQueue, ^{
        [_memoryCache release];
        _memoryCache = nil;
        [_caches release];
        _caches = nil;
    });
    
#if ! OS_OBJECT_USE_OBJC
    dispatch_release(_cacheQueue);
#endif
    [super dealloc];
}

- (void)addCache:(id <TiCache>)cache
{
    dispatch_barrier_async(_cacheQueue, ^{
        [_caches addObject:cache];
    });
}

- (void)insertCache:(id <TiCache>)cache atIndex:(NSUInteger)index
{
    dispatch_barrier_async(_cacheQueue, ^{
        if (index >= [_caches count])
            [_caches addObject:cache];
        else
            [_caches insertObject:cache atIndex:index];
    });
}

- (NSArray *)caches
{
    return [NSArray arrayWithArray:_caches];
}

- (UIImage *)cachedImage:(NSNumber*)aKey withCacheKey:(NSString *)aCacheKey
{
    return [self cachedImage:aKey withCacheKey:aCacheKey bypassingMemoryCache:NO];
}

- (UIImage *)cachedImage:(NSNumber*)aKey withCacheKey:(NSString *)aCacheKey bypassingMemoryCache:(BOOL)shouldBypassMemoryCache
{
    __block UIImage *image = nil;
    
    if (!shouldBypassMemoryCache)
        image = [_memoryCache cachedImage:aKey withCacheKey:aCacheKey];
    
    if (image)
        return image;
    
    dispatch_sync(_cacheQueue, ^{
        
        for (id <TiCache> cache in _caches)
        {
            image = [cache cachedImage:aKey withCacheKey:aCacheKey];
            
            if (image != nil && !shouldBypassMemoryCache)
            {
                [_memoryCache addImage:image forKey:aKey withCacheKey:aCacheKey];
                break;
            }
        }
        
    });
    
    return image;
}

- (void)addImage:(UIImage *)image forKey:(NSNumber*)aKey withCacheKey:(NSString *)aCacheKey
{
    if (!image || !aCacheKey)
        return;
    
    [_memoryCache addImage:image forKey:aKey withCacheKey:aCacheKey];
    
    dispatch_sync(_cacheQueue, ^{
        
        for (id <TiCache> cache in _caches)
        {
            if ([cache respondsToSelector:@selector(addImage:forKey:withCacheKey:)])
                [cache addImage:image forKey:aKey withCacheKey:aCacheKey];
        }
        
    });
}

- (void)addDiskCachedImageData:(NSData *)data forKey:(NSNumber*)aKey withCacheKey:(NSString *)aCacheKey
{
    if (!data || !aCacheKey)
        return;
    
    dispatch_sync(_cacheQueue, ^{
        
        for (id <TiCache> cache in _caches)
        {
            if ([cache respondsToSelector:@selector(addDiskCachedImageData:forKey:withCacheKey:)])
                [cache addDiskCachedImageData:data forKey:aKey withCacheKey:aCacheKey];
        }
        
    });
}

- (void)didReceiveMemoryWarning
{
    [_memoryCache didReceiveMemoryWarning];
    
    dispatch_sync(_cacheQueue, ^{
        
        for (id<TiCache> cache in _caches)
        {
            [cache didReceiveMemoryWarning];
        }
        
    });
}

- (void)removeAllCachedImages
{
    [_memoryCache removeAllCachedImages];
    
    dispatch_sync(_cacheQueue, ^{
        
        for (id<TiCache> cache in _caches)
        {
            [cache removeAllCachedImages];
        }
        
    });
}

- (void)removeAllCachedImagesForCacheKey:(NSString *)cacheKey
{
    [_memoryCache removeAllCachedImagesForCacheKey:cacheKey];
    
    dispatch_sync(_cacheQueue, ^{
        
        for (id<TiCache> cache in _caches)
        {
            [cache removeAllCachedImagesForCacheKey:cacheKey];
        }
    });
}

@end

#pragma mark -

@implementation TiCache (Configuration)

static NSMutableDictionary *predicateValues = nil;

- (NSDictionary *)predicateValues
{
    static dispatch_once_t predicateValuesOnceToken;
    
    dispatch_once(&predicateValuesOnceToken, ^{
        struct utsname systemInfo;
        uname(&systemInfo);
        
        NSString *machine = [NSString stringWithCString:systemInfo.machine encoding:NSASCIIStringEncoding];
        
        predicateValues = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
                           [[UIDevice currentDevice] model], @"model",
                           machine, @"machine",
                           [[UIDevice currentDevice] systemName], @"systemName",
                           [NSNumber numberWithFloat:[[[UIDevice currentDevice] systemVersion] floatValue]], @"systemVersion",
                           [NSNumber numberWithInt:[[UIDevice currentDevice] userInterfaceIdiom]], @"userInterfaceIdiom",
                           nil];
        
        if ( ! ([machine isEqualToString:@"i386"] || [machine isEqualToString:@"x86_64"]))
        {
            NSNumber *machineNumber = [NSNumber numberWithFloat:[[[machine stringByTrimmingCharactersInSet:[NSCharacterSet letterCharacterSet]] stringByReplacingOccurrencesOfString:@"," withString:@"."] floatValue]];
            
            if ( ! machineNumber)
                machineNumber = [NSNumber numberWithFloat:0.0];
            
            [predicateValues setObject:machineNumber forKey:@"machineNumber"];
        }
        else
        {
            [predicateValues setObject:[NSNumber numberWithFloat:0.0] forKey:@"machineNumber"];
        }
        
        // A predicate might be:
        // (self.model = 'iPad' and self.machineNumber >= 3) or (self.machine = 'x86_64')
        // See NSPredicate
        
        //        NSLog(@"Predicate values:\n%@", [predicateValues description]);
    });
    
    return predicateValues;
}

- (id <TiCache>)memoryCacheWithConfig:(NSDictionary *)cfg
{
    NSUInteger capacity = 32;
    
    NSNumber *capacityNumber = [cfg objectForKey:@"capacity"];
    if (capacityNumber != nil)
        capacity = [capacityNumber unsignedIntegerValue];
    
    NSArray *predicates = [cfg objectForKey:@"predicates"];
    
    if (predicates)
    {
        NSDictionary *predicateValues = [self predicateValues];
        
        for (NSDictionary *predicateDescription in predicates)
        {
            NSString *predicate = [predicateDescription objectForKey:@"predicate"];
            if ( ! predicate)
                continue;
            
            if ( ! [[NSPredicate predicateWithFormat:predicate] evaluateWithObject:predicateValues])
                continue;
            
            capacityNumber = [predicateDescription objectForKey:@"capacity"];
            if (capacityNumber != nil)
                capacity = [capacityNumber unsignedIntegerValue];
        }
    }
    
    DebugLog(@"Memory cache configuration: {capacity : %lu}", (unsigned long)capacity);
    
    return [[[TiMemoryCache alloc] initWithCapacity:capacity] autorelease];
}

- (id <TiCache>)databaseCacheWithConfig:(NSDictionary *)cfg
{
    BOOL useCacheDir = NO;
    TiCachePurgeStrategy strategy = TiCachePurgeStrategyFIFO;
    
    NSUInteger capacity = 1000;
    NSUInteger minimalPurge = capacity / 10;
    
    // Defaults
    
    NSNumber *capacityNumber = [cfg objectForKey:@"capacity"];
    
    if ([UIDevice currentDevice].userInterfaceIdiom == UIUserInterfaceIdiomPad && [cfg objectForKey:@"capacity-ipad"])
    {
        NSLog(@"***** WARNING: deprecated config option capacity-ipad, use a predicate instead: -[%@ %@] (line %d)", self, NSStringFromSelector(_cmd), __LINE__);
        capacityNumber = [cfg objectForKey:@"capacity-ipad"];
    }
    
    NSString *strategyStr = [cfg objectForKey:@"strategy"];
    NSNumber *useCacheDirNumber = [cfg objectForKey:@"useCachesDirectory"];
    NSNumber *minimalPurgeNumber = [cfg objectForKey:@"minimalPurge"];
    NSNumber *expiryPeriodNumber = [cfg objectForKey:@"expiryPeriod"];
    NSString * name = [cfg objectForKey:@"name"];
    NSArray *predicates = [cfg objectForKey:@"predicates"];
    
    if (predicates)
    {
        NSDictionary *predicateValues = [self predicateValues];
        
        for (NSDictionary *predicateDescription in predicates)
        {
            NSString *predicate = [predicateDescription objectForKey:@"predicate"];
            if ( ! predicate)
                continue;
            
            if ( ! [[NSPredicate predicateWithFormat:predicate] evaluateWithObject:predicateValues])
                continue;
            
            if ([predicateDescription objectForKey:@"capacity"])
                capacityNumber = [predicateDescription objectForKey:@"capacity"];
            if ([predicateDescription objectForKey:@"strategy"])
                strategyStr = [predicateDescription objectForKey:@"strategy"];
            if ([predicateDescription objectForKey:@"useCachesDirectory"])
                useCacheDirNumber = [predicateDescription objectForKey:@"useCachesDirectory"];
            if ([predicateDescription objectForKey:@"minimalPurge"])
                minimalPurgeNumber = [predicateDescription objectForKey:@"minimalPurge"];
            if ([predicateDescription objectForKey:@"expiryPeriod"])
                expiryPeriodNumber = [predicateDescription objectForKey:@"expiryPeriod"];
            if ([predicateDescription objectForKey:@"name"])
                name = [predicateDescription objectForKey:@"name"];
        }
    }
    
    // Check the values
    
    if (capacityNumber != nil)
    {
        NSInteger value = [capacityNumber intValue];
        
        // 0 is valid: it means no capacity limit
        if (value >= 0)
        {
            capacity =  value;
            minimalPurge = MAX(1,capacity / 10);
        }
        else
        {
            DebugLog(@"illegal value for capacity: %ld", (long)value);
        }
    }
    
    if (strategyStr != nil)
    {
        if ([strategyStr caseInsensitiveCompare:@"FIFO"] == NSOrderedSame) strategy = TiCachePurgeStrategyFIFO;
        if ([strategyStr caseInsensitiveCompare:@"LRU"] == NSOrderedSame) strategy = TiCachePurgeStrategyLRU;
    }
    else
    {
        strategyStr = @"FIFO";
    }
    
    if (useCacheDirNumber != nil)
        useCacheDir = [useCacheDirNumber boolValue];
    
    if (minimalPurgeNumber != nil && capacity != 0)
    {
        NSUInteger value = [minimalPurgeNumber unsignedIntValue];
        
        if (value > 0 && value<=capacity)
            minimalPurge = value;
        else
            DebugLog(@"minimalPurge must be at least one and at most the cache capacity");
    }
    
    if (expiryPeriodNumber != nil)
        _expiryPeriod = [expiryPeriodNumber doubleValue];
    
    if (!name) {
        name = @"TiCache";
    }
    DebugLog(@"Database cache configuration: {capacity : %lu, strategy : %@, minimalPurge : %lu, expiryPeriod: %.0f, useCacheDir : %@}", (unsigned long)capacity, strategyStr, (unsigned long)minimalPurge, _expiryPeriod, useCacheDir ? @"YES" : @"NO");
    
    
    TiDatabaseCache *dbCache = [[TiDatabaseCache alloc] initWithName:name usingCacheDir:useCacheDir];
    [dbCache setCapacity:capacity];
    [dbCache setPurgeStrategy:strategy];
    [dbCache setMinimalPurge:minimalPurge];
    [dbCache setExpiryPeriod:_expiryPeriod];
    
    return [dbCache autorelease];
}

@end
