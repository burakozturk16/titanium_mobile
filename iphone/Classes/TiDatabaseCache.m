//
//  TiDatabaseCache.m
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

#import "TiDatabaseCache.h"
#import "FMDB.h"
#import "TiBase.h"

#define kWriteQueueLimit 15

@interface TiDatabaseCache ()

- (NSUInteger)count;
- (NSUInteger)countDatabase;
- (void)touch:(NSNumber*)key withCacheKey:(NSString *)cacheKey;
- (void)purge:(NSUInteger)count;

@end

#pragma mark -

@implementation TiDatabaseCache
{
    // Database
    FMDatabaseQueue *_queue;
    
    NSUInteger _tileCount;
    NSOperationQueue *_writeQueue;
    NSRecursiveLock *_writeQueueLock;
    
    // Cache
    TiCachePurgeStrategy _purgeStrategy;
    NSUInteger _capacity;
    NSUInteger _minimalPurge;
    NSTimeInterval _expiryPeriod;
}

@synthesize databasePath = _databasePath;

+ (NSString *)dbPath:(NSString*)name usingCacheDir:(BOOL)useCacheDir
{
    NSArray *paths;
    
    if (useCacheDir)
        paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES);
    else
        paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    
    if ([paths count] > 0) // Should only be one...
    {
        NSString *cachePath = [paths objectAtIndex:0];
        
        // check for existence of cache directory
        if ( ![[NSFileManager defaultManager] fileExistsAtPath: cachePath])
        {
            // create a new cache directory
            [[NSFileManager defaultManager] createDirectoryAtPath:cachePath withIntermediateDirectories:NO attributes:nil error:nil];
        }
        if ([[name pathExtension] length] == 0) {
            return [cachePath stringByAppendingPathComponent:[NSString stringWithFormat:@"%@.db", name]];
        }
        return [cachePath stringByAppendingPathComponent:name];
    }
    
    return nil;
}

- (void)configureDBForFirstUse
{
    [_queue inDatabase:^(FMDatabase *db) {
        [[db executeQuery:@"PRAGMA synchronous=OFF"] close];
        [[db executeQuery:@"PRAGMA journal_mode=OFF"] close];
        [[db executeQuery:@"PRAGMA cache_size=100"] close];
        [[db executeQuery:@"PRAGMA count_changes=OFF"] close];
        [db executeUpdate:@"CREATE TABLE IF NOT EXISTS ZCACHE (key INTEGER NOT NULL, cache_key VARCHAR(25) NOT NULL, last_used DOUBLE NOT NULL, data BLOB NOT NULL)"];
        [db executeUpdate:@"CREATE UNIQUE INDEX IF NOT EXISTS main_index ON ZCACHE(key, cache_key)"];
        [db executeUpdate:@"CREATE INDEX IF NOT EXISTS last_used_index ON ZCACHE(last_used)"];
    }];
}

- (id)initWithDatabase:(NSString *)path
{
    if (!(self = [super init]))
        return nil;
    
    self.databasePath = path;
    
    _writeQueue = [NSOperationQueue new];
    [_writeQueue setMaxConcurrentOperationCount:1];
    _writeQueueLock = [NSRecursiveLock new];
    
    DebugLog(@"Opening database at %@", path);
    
    _queue = [[FMDatabaseQueue databaseQueueWithPath:path] retain];
    
    if (!_queue)
    {
        DebugLog(@"Could not connect to database");
        
        [[NSFileManager defaultManager] removeItemAtPath:path error:NULL];
        
        return nil;
    }
    
    [_queue inDatabase:^(FMDatabase *db) {
        [db setCrashOnErrors:NO];
        [db setShouldCacheStatements:TRUE];
    }];
    
    [self configureDBForFirstUse];
    
    _tileCount = [self countDatabase];
    
    return self;
}

- (id)initWithName:(NSString*)name usingCacheDir:(BOOL)useCacheDir
{
    return [self initWithDatabase:[TiDatabaseCache dbPath:name usingCacheDir:useCacheDir]];
}

- (void)dealloc
{
    [_writeQueueLock lock];
    [_writeQueue release];
    _writeQueue = nil;
    [_writeQueueLock unlock];
    _writeQueueLock = nil;
    [_queue release];
    _queue = nil;
    [super dealloc];
}

- (void)setPurgeStrategy:(TiCachePurgeStrategy)theStrategy
{
    _purgeStrategy = theStrategy;
}

- (void)setCapacity:(NSUInteger)theCapacity
{
    _capacity = theCapacity;
}

- (NSUInteger)capacity
{
    return _capacity;
}

- (void)setMinimalPurge:(NSUInteger)theMinimalPurge
{
    _minimalPurge = theMinimalPurge;
}

- (void)setExpiryPeriod:(NSTimeInterval)theExpiryPeriod
{
    _expiryPeriod = theExpiryPeriod;
    
    srand((unsigned int)time(NULL));
}

- (unsigned long long)fileSize
{
    return [[[NSFileManager defaultManager] attributesOfItemAtPath:self.databasePath error:nil] fileSize];
}

- (UIImage *)cachedImage:(NSNumber*)aKey withCacheKey:(NSString *)cacheKey
{
    //	DebugLog(@"DB cache check for tile %d %d %d", tile.x, tile.y, tile.zoom);
    
    __block UIImage *cachedImage = nil;
    
    [_writeQueueLock lock];
    
    [_queue inDatabase:^(FMDatabase *db)
     {
         FMResultSet *results = [db executeQuery:@"SELECT data FROM ZCACHE WHERE key = ? AND cache_key = ?", aKey, cacheKey];
         
         if ([db hadError])
         {
             DebugLog(@"DB error while fetching tile data: %@", [db lastErrorMessage]);
             return;
         }
         
         NSData *data = nil;
         
         if ([results next])
         {
             data = [results dataForColumnIndex:0];
             if (data) cachedImage = [UIImage imageWithData:data];
         }
         
         [results close];
     }];
    
    [_writeQueueLock unlock];
    
    if (_capacity != 0 && _purgeStrategy == TiCachePurgeStrategyLRU)
        [self touch:aKey withCacheKey:cacheKey];
    
    if (_expiryPeriod > 0)
    {
        if (rand() % 100 == 0)
        {
            [_writeQueueLock lock];
            
            [_queue inDatabase:^(FMDatabase *db)
             {
                 BOOL result = [db executeUpdate:@"DELETE FROM ZCACHE WHERE last_used < ?", [NSDate dateWithTimeIntervalSinceNow:-_expiryPeriod]];
                 
                 if (result)
                     result = [db executeUpdate:@"VACUUM"];
                 
                 if ( ! result)
                     DebugLog(@"Error expiring cache");
             }];
            
            [_writeQueueLock unlock];
            
            _tileCount = [self countDatabase];
        }
    }
    
    //    DebugLog(@"DB cache     hit    tile %d %d %d (%@)", tile.x, tile.y, tile.zoom, [RMTileCache tileHash:tile]);
    
    return cachedImage;
}

- (void)addImage:(UIImage *)image forKey:(NSNumber*)aKey withCacheKey:(NSString *)aCacheKey
{
    [self addDiskCachedImageData:UIImagePNGRepresentation(image) forKey:aKey withCacheKey:aCacheKey];
}

- (void)addDiskCachedImageData:(NSData *)data forKey:(NSString*)aKey withCacheKey:(NSString *)aCacheKey
{
    if (_capacity != 0)
    {
        NSUInteger tilesInDb = [self count];
        
        if (_capacity <= tilesInDb && _expiryPeriod == 0)
            [self purge:MAX(_minimalPurge, 1+tilesInDb-_capacity)];
        
        //        DebugLog(@"DB cache     insert tile %d %d %d (%@)", tile.x, tile.y, tile.zoom, [RMTileCache tileHash:tile]);
        
        // Don't add new images to the database while there are still more than kWriteQueueLimit
        // insert operations pending. This prevents some memory issues.
        
        BOOL skipThisTile = NO;
        
        [_writeQueueLock lock];
        
        if ([_writeQueue operationCount] > kWriteQueueLimit)
            skipThisTile = YES;
        
        [_writeQueueLock unlock];
        
        if (skipThisTile)
            return;
        
        [_writeQueue addOperationWithBlock:^{
            __block BOOL result = NO;
            
            [_writeQueueLock lock];
            
            [_queue inDatabase:^(FMDatabase *db)
             {
                 result = [db executeUpdate:@"INSERT OR IGNORE INTO ZCACHE (key, cache_key, last_used, data) VALUES (?, ?, ?, ?)", aKey, aCacheKey, [NSDate date], data];
             }];
            
            [_writeQueueLock unlock];
            
            if (result == NO) {
                DebugLog(@"Error occured adding data");
            }
            else {
                _tileCount++;
            }
        }];
    }
}

#pragma mark -

- (NSUInteger)count
{
    return _tileCount;
}

- (NSUInteger)countDatabase
{
    __block NSUInteger count = 0;
    
    [_writeQueueLock lock];
    
    [_queue inDatabase:^(FMDatabase *db)
     {
         FMResultSet *results = [db executeQuery:@"SELECT COUNT(*) FROM ZCACHE"];
         
         if ([results next])
             count = [results intForColumnIndex:0];
         else
             DebugLog(@"Unable to count columns");
         
         [results close];
     }];
    
    [_writeQueueLock unlock];
    
    return count;
}

- (void)purge:(NSUInteger)count
{
    DebugLog(@"purging %lu old tiles from the db cache", (unsigned long)count);
    
    [_writeQueueLock lock];
    
    [_queue inDatabase:^(FMDatabase *db)
     {
         BOOL result = [db executeUpdate:@"DELETE FROM ZCACHE WHERE key IN (SELECT key FROM ZCACHE ORDER BY last_used LIMIT ?)", [NSNumber numberWithUnsignedLongLong:count]];
         
         if (result)
             result = [db executeUpdate:@"VACUUM"];
         
         if ( ! result)
             DebugLog(@"Error purging cache");
     }];
    
    [_writeQueueLock unlock];
    
    _tileCount = [self countDatabase];
}

- (void)removeAllCachedImages
{
    DebugLog(@"removing all tiles from the db cache");
    
    [_writeQueue addOperationWithBlock:^{
        [_writeQueueLock lock];
        
        [_queue inDatabase:^(FMDatabase *db)
         {
             BOOL result = [db executeUpdate:@"DELETE FROM ZCACHE"];
             
             if (result)
                 result = [db executeUpdate:@"VACUUM"];
             
             if ( ! result)
                 DebugLog(@"Error purging cache");
         }];
        
        [_writeQueueLock unlock];
        
        _tileCount = [self countDatabase];
    }];
}

- (void)removeAllCachedImagesForCacheKey:(NSString *)cacheKey
{
    DebugLog(@"removing tiles for key '%@' from the db cache", cacheKey);
    
    [_writeQueue addOperationWithBlock:^{
        [_writeQueueLock lock];
        
        [_queue inDatabase:^(FMDatabase *db)
         {
             BOOL result = [db executeUpdate:@"DELETE FROM ZCACHE WHERE cache_key = ?", cacheKey];
             
             if (result)
                 result = [db executeUpdate:@"VACUUM"];
             
             if ( ! result)
                 DebugLog(@"Error purging cache");
         }];
        
        [_writeQueueLock unlock];
        
        _tileCount = [self countDatabase];
    }];
}

- (void)touch:(NSNumber*)key withCacheKey:(NSString *)cacheKey
{
    [_writeQueue addOperationWithBlock:^{
        [_writeQueueLock lock];
        
        [_queue inDatabase:^(FMDatabase *db)
         {
             BOOL result = [db executeUpdate:@"UPDATE ZCACHE SET last_used = ? WHERE key = ? AND cache_key = ?", [NSDate date], key, cacheKey];
             
             if (result == NO)
                 DebugLog(@"Error touching tile");
         }];
        
        [_writeQueueLock unlock];
    }];
}

- (void)didReceiveMemoryWarning
{
    DebugLog(@"Low memory in the database tilecache");
    
    [_writeQueueLock lock];
    [_writeQueue cancelAllOperations];
    [_writeQueueLock unlock];
}

@end
