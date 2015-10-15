//
//  TiMemoryCache.m
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

#import "TiMemoryCache.h"
#import "TiBase.h"

@implementation TiMemoryCache
{
    NSMutableDictionary *_memoryCache;
    dispatch_queue_t _memoryCacheQueue;
    NSUInteger _memoryCacheCapacity;
}

- (id)initWithCapacity:(NSUInteger)aCapacity
{
    return [self initWithCapacity:aCapacity queue:@"ti.memoryCacheQueue"];
}

- (id)initWithCapacity:(NSUInteger)aCapacity queue:(NSString*)queue
{
    if (!(self = [super init]))
        return nil;
    
    DebugLog(@"initializing memory cache %@ with capacity %lu", self, (unsigned long)aCapacity);
    
    _memoryCache = [[NSMutableDictionary alloc] initWithCapacity:aCapacity];
    _memoryCacheQueue = dispatch_queue_create([queue UTF8String], DISPATCH_QUEUE_CONCURRENT);
    
    if (aCapacity < 1)
        aCapacity = 1;
    
    _memoryCacheCapacity = aCapacity;
    
    return self;
}

- (id)init
{
    return [self initWithCapacity:32];
}

- (void)dealloc
{
    dispatch_barrier_sync(_memoryCacheQueue, ^{
        [_memoryCache removeAllObjects];
        [_memoryCache release];
        _memoryCache=  nil;
    });
    
#if ! OS_OBJECT_USE_OBJC
    dispatch_release(_memoryCacheQueue);
#endif
    [super dealloc];
}

- (void)didReceiveMemoryWarning
{
    dispatch_barrier_async(_memoryCacheQueue, ^{
        [_memoryCache removeAllObjects];
    });
}

- (void)remove:(NSString*)key
{
    dispatch_barrier_async(_memoryCacheQueue, ^{
        [_memoryCache removeObjectForKey:key];
    });
}


- (UIImage *)cachedImage:(NSNumber*)key withCacheKey:(NSString *)aCacheKey
{
    __block TiCacheObject *cachedObject = nil;
    
    dispatch_sync(_memoryCacheQueue, ^{
        
        cachedObject = [_memoryCache objectForKey:key];
        
        if (cachedObject)
        {
            if ([[cachedObject cacheKey] isEqualToString:aCacheKey])
            {
                [cachedObject touch];
            }
            else
            {
                dispatch_barrier_async(_memoryCacheQueue, ^{
                    [_memoryCache removeObjectForKey:key];
                });
                
                cachedObject = nil;
            }
        }
        
    });
    
    //    RMLog(@"Memory cache hit    tile %d %d %d (%@)", tile.x, tile.y, tile.zoom, [RMTileCache tileHash:tile]);
    
    return [cachedObject cachedObject];
}

- (NSUInteger)capacity
{
    return _memoryCacheCapacity;
}

/// Remove the least-recently used image from cache, if cache is at or over capacity. Removes only 1 image.
- (void)makeSpaceInCache
{
    dispatch_barrier_async(_memoryCacheQueue, ^{
        
        while ([_memoryCache count] >= _memoryCacheCapacity)
        {
            // Rather than scanning I would really like to be using a priority queue
            // backed by a heap here.
            
            // Maybe deleting one random element would work as well.
            
            NSEnumerator *enumerator = [_memoryCache objectEnumerator];
            TiCacheObject *image;
            
            NSDate *oldestDate = nil;
            TiCacheObject *oldestImage = nil;
            
            while ((image = (TiCacheObject *)[enumerator nextObject]))
            {
                if (oldestDate == nil || ([oldestDate timeIntervalSinceReferenceDate] > [[image timestamp] timeIntervalSinceReferenceDate]))
                {
                    oldestDate = [image timestamp];
                    oldestImage = image;
                }
            }
            
            if (oldestImage)
            {
                [_memoryCache removeObjectForKey:oldestImage.key];
            }
        }
        
    });
}

- (void)addImage:(UIImage *)image forKey:(NSNumber*)aKey withCacheKey:(NSString *)aCacheKey
{
    [self makeSpaceInCache];
    
    dispatch_barrier_async(_memoryCacheQueue, ^{
        [_memoryCache setObject:[TiCacheObject cacheObject:image forKey:aKey withCacheKey:aCacheKey] forKey:aKey];
    });
}

- (void)removeAllCachedImages
{
    dispatch_barrier_async(_memoryCacheQueue, ^{
        [_memoryCache removeAllObjects];
    });
}

- (void)removeAllCachedImagesForCacheKey:(NSString *)cacheKey
{
    dispatch_barrier_async(_memoryCacheQueue, ^{
        
        NSMutableArray *keysToRemove = [NSMutableArray array];
        
        [_memoryCache enumerateKeysAndObjectsUsingBlock:^(id key, TiCacheObject *cachedObject, BOOL *stop) {
            if ([[cachedObject cacheKey] isEqualToString:cacheKey])
                [keysToRemove addObject:key];
        }];
        
        [_memoryCache removeObjectsForKeys:keysToRemove];
        
    });
}

@end
