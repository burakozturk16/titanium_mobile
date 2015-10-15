//
//  TiUIiOSActivityViewProxy.h
//  Titanium
//
//  Created by Martin Guillon on 16/02/14.
//
//
#import "TiProxy.h"

#import "CMDActivityViewController.h"
@interface TiUIiOSActivityViewProxy : TiProxy<CMDActivityViewControllerDelegate>

@property(retain, nonatomic) NSArray* excluded;
@property(retain, nonatomic) NSArray* items;
@property(retain, nonatomic) NSArray* activities;
@property(retain, nonatomic) KrollCallback* itemForActivityType;
@property(retain, nonatomic) NSString* subject;
@property(retain, nonatomic) KrollCallback* subjectForActivityType;
@property(retain, nonatomic) UIImage* thumbnail;
@property(retain, nonatomic) KrollCallback* thumbnailForActivityType;

@end
