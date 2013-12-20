#import "TTTAttributedLabel.h"
@protocol TouchDelegate <NSObject>
@optional
- (void)processTouchesBegan:(NSSet *)touches withEvent:(UIEvent *)event;
- (void)processTouchesMoved:(NSSet *)touches withEvent:(UIEvent *)event;
- (void)processTouchesEnded:(NSSet *)touches withEvent:(UIEvent *)event;
- (void)processTouchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event;

@end
@interface TDUITableView : UITableView

@property (nonatomic, unsafe_unretained) id <TouchDelegate> touchDelegate;

@end

@interface TDTTTAttributedLabel : TTTAttributedLabel

@property (nonatomic, unsafe_unretained) id <TouchDelegate> touchDelegate;

@end

@interface TDUIView : UIView

@property (nonatomic, unsafe_unretained) id <TouchDelegate> touchDelegate;

@end
