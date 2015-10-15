//
//  TiModalNavViewController.m
//  Push
//
//  Created by Martin Guillon on 18/08/2014.
//
//

#import "TiModalNavViewController.h"

@interface TiModalNavViewController ()

@end

@implementation TiModalNavViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    // Do any additional setup after loading the view.
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (BOOL)automaticallyForwardAppearanceAndRotationMethodsToChildViewControllers
{
    return YES;
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation
{
    return [[self topViewController] shouldAutorotateToInterfaceOrientation:toInterfaceOrientation];
}

- (BOOL)shouldAutomaticallyForwardRotationMethods
{
    return [[self topViewController] shouldAutomaticallyForwardRotationMethods];
}

- (BOOL)shouldAutomaticallyForwardAppearanceMethods
{
    return [[self topViewController] shouldAutomaticallyForwardAppearanceMethods];
}

- (BOOL)shouldAutorotate{
    return [[self topViewController] shouldAutorotate];
}

- (NSUInteger)supportedInterfaceOrientations {
    return [[self topViewController] supportedInterfaceOrientations];
}

- (UIInterfaceOrientation)preferredInterfaceOrientationForPresentation
{
    return [[self topViewController] preferredInterfaceOrientationForPresentation];
}

- (BOOL)prefersStatusBarHidden
{
    return [[self topViewController] prefersStatusBarHidden];
}

- (UIStatusBarStyle)preferredStatusBarStyle
{
    return [[self topViewController] preferredStatusBarStyle];
}

-(BOOL) modalPresentationCapturesStatusBarAppearance
{
    return [[self topViewController] modalPresentationCapturesStatusBarAppearance];
}

- (UIStatusBarAnimation)preferredStatusBarUpdateAnimation
{
    return [[self topViewController] preferredStatusBarUpdateAnimation];
}

@end
