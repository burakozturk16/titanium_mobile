//
//  SlideMenu.h
//  Titanium
//
//  Created by Martin Guillon on 2/27/13.
//
//

#import "TiBase.h"
#import "TiUIView.h"
#import "IIViewDeckController.h"

@interface TiUISlideMenu : TiUIView{
    
@private
	IIViewDeckController *controller;
}
-(IIViewDeckController*)controller;

//API
-(void)toggleLeftView:(id)args;
-(void)toggleRightView:(id)args;
-(void)bounceLeftView:(id)args;
-(void)bounceRightView:(id)args;
-(void)bounceTopView:(id)args;
-(void)bounceBottomView:(id)args;
-(void)toggleOpenView:(id)args;

@end
