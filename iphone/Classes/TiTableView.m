//
//  TiTableView.m
//  Titanium
//
//  Created by Martin Guillon on 20/12/13.
//
//

#import "TiTableView.h"
#import "TiBase.h"

@implementation TiTableView
{
}

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
    }
    return self;
}

- (void)setContentOffset:(CGPoint)contentOffset animated:(BOOL)animated
{
    if (IOS_7) {
        //we have to delay it on ios7 :s
        double delayInSeconds = 0.01;
        dispatch_time_t popTime = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delayInSeconds * NSEC_PER_SEC));
        dispatch_after(popTime, dispatch_get_main_queue(), ^(void){
            [super setContentOffset:contentOffset animated:animated];
        });
    }
    else {
        [super setContentOffset:contentOffset animated:animated];
    }
}
@end
