//
//  TiCache.h
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
#import <Foundation/Foundation.h>
#import "TiCacheObject.h"

@class TiMemoryCache;

typedef enum : short {
    TiCachePurgeStrategyLRU,
    TiCachePurgeStrategyFIFO,
} TiCachePurgeStrategy;

#pragma mark -

/** The RMTileCache protocol describes behaviors that tile caches should implement. */
@protocol TiCache <NSObject>

/** @name Querying the Cache */

/** Returns an image from the cache if it exists.
 *   @param aKey the key.
 *   @param cacheKey The key representing a certain cache.
 *   @return An image of the tile that can be used to draw a portion of the map. */
- (UIImage *)cachedImage:(NSNumber*)aKey withCacheKey:(NSString *)cacheKey;


- (void)didReceiveMemoryWarning;

@optional

/** @name Adding to the Cache */

/** Adds an image to the specified cache.
 *   @param image An image to be cached.
 *   @param aKey The key
 *   @param cacheKey The key representing a certain cache. */
- (void)addImage:(UIImage *)image forKey:(NSNumber*)aKey withCacheKey:(NSString *)aCacheKey;

/** Adds image data to the specified cache, bypassing the memory cache and only writing to disk. This is useful for instances where many images are downloaded directly to disk for later use offline.
 *   @param data The image data to be cached.
 *   @param aKey The key
 *   @param cacheKey The key representing a certain cache. */
- (void)addDiskCachedImageData:(NSData *)data forKey:(NSNumber*)aKey withCacheKey:(NSString *)cacheKey;

/** @name Clearing the Cache */

/** Removes all images from a cache. */
- (void)removeAllCachedImages;
- (void)removeAllCachedImagesForCacheKey:(NSString *)cacheKey;

@end

#pragma mark -

/** An TiCache object manages memory-based and disk-based caches for images that have been retrieved from the network.
 *
 *   An TiCache is a key component of offline image access. All image requests pass through the cache and are served from cache if available, avoiding network operation.
 *
 *   @see [TiDatabaseCache initUsingCacheDir:] */
@interface TiCache : NSObject <TiCache>

/** @name Initializing a Cache Manager */

/** Initializes and returns a newly allocated cache object with specified expiry period.
 *
 *   If the `init` method is used to initialize a cache instead, a period of `0` is used. In that case, time-based expiration of images is not performed, but rather the cached count is used instead.
 *
 *   @param period A period of time after which images should be expunged from the cache.
 *   @return An initialized cache object or `nil` if the object couldn't be created. */
- (id)initWithConfig:(NSArray*)cacheCfg expiryPeriod:(NSTimeInterval)period;

/** @name Adding Caches to the Cache Manager */

/** Adds a given cache to the cache management system.
 *
 *   @param cache A memory-based or disk-based cache. */
- (void)addCache:(id <TiCache>)cache;
- (void)insertCache:(id <TiCache>)cache atIndex:(NSUInteger)index;

/** Returns an image from the cache if it exists.
 *   @param aKey the key.
 *   @param cacheKey The key representing a certain cache.
 *   @param shouldBypassMemoryCache Whether to only consult disk-based caches.
 *   @return An image of the tile that can be used to draw a portion of the map. */
- (UIImage *)cachedImage:(NSNumber*)aKey withCacheKey:(NSString *)cacheKey bypassingMemoryCache:(BOOL)shouldBypassMemoryCache;

/** The list of caches managed by a cache manager. This could include memory-based, disk-based, or other types of caches. */
@property (nonatomic, readonly, strong) NSArray *caches;

- (void)didReceiveMemoryWarning;

@end
