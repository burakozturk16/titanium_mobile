#import "TiAnimatedImage.h"
#import "TiBase.h"

@implementation TiAnimatedImage
@synthesize index, paused = _paused, stopped = _stopped, reverse=_reverse;


-(id)initWithImages:(NSArray*)images andDurations:(NSArray*)durations
{
    if (self = [super init]) {
        _reverse = _actualreverse = NO;
        _paused = YES;
        _stopped = YES;
        index = -1;
        count = [images count];
        _images = [images retain];
        _durations = [durations retain];
    }
    return self;
}

-(void)dealloc
{
    RELEASE_TO_NIL(_images)
    RELEASE_TO_NIL(_durations)
    [super dealloc];
}

-(void)resetIndex
{
    index = _reverse?[_images count]-1:0;
}

-(void) reset {
    [self resetIndex];
    _actualreverse = _reverse;
    [self presentCurrentImage];
}


-(void) start {
    _stopped = NO;
    [self reset];
    [self resume];
}


-(void) resume {
    if (_stopped) {
        [self start];
        return;
    }
    if (_paused) {
        _paused = NO;
        [self stepThroughImages];
    }
}

-(void)pause
{
    _paused = YES;
}
-(void)presentCurrentImage
{
    if ([_delegate respondsToSelector:@selector(animatedImage:changedToImage:)]) {
        [_delegate animatedImage:self changedToImage:[_images objectAtIndex: index]];
    }
}

- (void) stepThroughImages {
    [self presentCurrentImage];
    if (_actualreverse ) {
        if (index == 0) {
            if (_autoreverse) {
                _actualreverse = !_actualreverse;
                index ++;
            }
            else {
                index = count -1;
            }
        }
        else {
            index --;
        }
    } else {
        if (index == count -1) {
            if (_autoreverse) {
                _actualreverse = !_actualreverse;
                index --;
            }
            else {
                index = 0;
            }
        }
        else {
            index ++;
        }
    }
    
    
    if (!_paused) {
        dispatch_time_t popTime = dispatch_time(DISPATCH_TIME_NOW, (int64_t)([[_durations objectAtIndex:index] doubleValue] * NSEC_PER_SEC));
        dispatch_after(popTime, dispatch_get_main_queue(), ^(void){
            if (!_paused) [self stepThroughImages];
        });
    }
}


-(void)setAnimatedImageAtIndex:(int)i
{
    if (_stopped) {
        _stopped = NO;
    }
    _paused = YES;
    index = _reverse?([_images count] - i):i;
    [self presentCurrentImage];
}


-(void)setProgress:(float)progress
{

    [self setAnimatedImageAtIndex:roundf((_reverse?(1 - progress):progress)* [_images count])];
}

-(float)progress
{
    float result = (float)index / [_images count];
    return _reverse?(1 - result):result;
}

-(void) stop {
    _stopped = _paused = YES;
    [self reset];
}

-(void)setReverse:(BOOL)reverse
{
    if (_reverse == reverse) return;
    _reverse = reverse;
    if (!_paused) {
        [self start];
    }
    else {
        index = count - index;
    }
}
@end
