
@class TiAnimatedImage;
@protocol AnimatedImageDelegate <NSObject>
-(void)animatedImage:(TiAnimatedImage*)animatedImage changedToImage:(UIImage*)image;
@end

@interface TiAnimatedImage : NSObject
{
    NSArray *_images;
    NSArray *_durations;
	NSInteger count;
	NSInteger index;
    BOOL _paused;
    BOOL _stopped;
    BOOL _actualreverse;
    BOOL _reverse;
}
@property (nonatomic,readonly) NSInteger index;
@property (nonatomic,readonly) BOOL paused;
@property (nonatomic,readonly) BOOL stopped;
@property (nonatomic,assign) BOOL autoreverse;
@property (nonatomic,assign) BOOL reverse;
@property (nonatomic,assign) id<AnimatedImageDelegate> delegate;
-(id)initWithImages:(NSArray*)images andDurations:(NSArray*)durations;
-(void) start;
-(void) stop;
-(void) resume;
-(void) pause;
-(void) reset;
-(void)setAnimatedImageAtIndex:(int)i;
@end

