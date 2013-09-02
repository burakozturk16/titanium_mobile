var Shape = Ti.Shape;
var isAndroid = Ti.Platform.osname == "android";
var backColor = isAndroid?'black':'white';
var navGroup;

function createWin() {
	return Ti.UI.createWindow({
		backgroundColor : backColor,
		// backgroundGradient : {
		// type : 'linear',
		// colors : ['#52DA5A', '#3AA04C', '#52DA5A'],
		// startPoint : {
		// x : 0,
		// y : 0
		// },
		// endPoint : {
		// x : 0,
		// y : "100%"
		// }
		// },
		orientationModes : [Ti.UI.UPSIDE_PORTRAIT, Ti.UI.PORTRAIT, Ti.UI.LANDSCAPE_RIGHT, Ti.UI.LANDSCAPE_LEFT]
	});
}

function varSwitch(_var, _val1, _val2) {
	return (_var === _val1) ? _val2 : _val1;
}

var androidActivitysSettings = {
		actionBar : {
			displayHomeAsUp:true,
			onHomeIconItemSelected : function(e) {e.window.close();}
		}
};
function openWin(_win, _withoutActionBar) {
	if (isAndroid) {
		if (_withoutActionBar != true) _win.activity = androidActivitysSettings;
		_win.open();
	}
	else {
		if (!navGroup) {
			navGroup = Titanium.UI.iPhone.createNavigationGroup({
				window: _win
			});
			var winHolder = createWin();
			winHolder.add(navGroup);
			winHolder.open();
		}
		else
			navGroup.open(_win);
	}
}

function shapeEx() {
	var win = createWin();
	var view = Shape.createView({
		bubbleParent : false,
		width : '90%',
		height : 100,
		backgroundColor : 'white'
	});

	var shape = Shape.createArc({
		anchor : Shape.TOP_MIDDLE,
		lineColor : '#777',
		center : {
			x : 0,
			y : '-20%'
		},
		radius : '100%',
		lineWidth : 10,
		startAngle : -45,
		sweepAngle : 95,
		lineCap : Shape.CAP_ROUND,
		lineShadow : {
			radius : 12,
			color : 'black',
			offset : {
				x : 3,
				y : 4
			}
		},
		lineEmboss : {}
		// lineGradient : {
		// type : 'linear',
		// colors : ['#52DA5A', '#3AA04C', '#52DA5A'],
		// startPoint : {
		// x : 0,
		// y : 0
		// },
		// endPoint : {
		// x : 0,
		// y : "100%"
		// }
		// }
	});
	shape.add({
		fillColor : 'red',
		operations : [{
			anchor : Shape.CENTER,
			type : 'circle',
			radius : 10
		}]
	});
	view.add(shape);
	view.addEventListener('click', function(e) {
		shape.animate({
			duration : 600,
			autoreverse : true,
			lineWidth : 30,
			lineColor : 'yellow',
			sweepAngle : 40
		});
	});
	win.add(view);
	openWin(win);
}

function transformEx() {
	var gone = false;
	var win = createWin();
	var t0 = Ti.UI.create2DMatrix({anchorPoint:{x:0,y:"100%"}});
	var t1 = t0.rotate(30);
	var t2 = t0.rotate(135);
	var t3 = t0.rotate(125);
	var t4 = t0.translate(0, "100%").rotate(125);
	var view = Ti.UI.createView({
		transform:t0,
		bubbleParent:false,
		backgroundColor : 'orange',
		top : 30,
		width : 100,
		height : 100
	});
	if (false) {
		var set = Ti.UI.createAnimationSet({
			playMode : 1
		});
		set.addEventListener('complete', function(e) {
			gone = true;
		});
		
		set.add({duration : 800, transform : t1}, view);
		set.add({duration : 800, transform : t2}, view);
		set.add({duration : 500, transform : t3}, view);
		set.add({duration : 500, transform : t4}, view);
		
		view.addEventListener('click', function(e) {
			if (gone === true) {
				view.animate({duration : 300, transform : t0}, function(){gone = false});
			}
			else
				set.start();
		});
		win.addEventListener('click', function(e) {
			if (gone === true) {
				view.animate({duration : 300, transform : t0}, function(){gone = false});
			}
		});
	}
	else {
		var anim1 = Ti.UI.createAnimation({duration : 800, transform : t1});
		anim1.addEventListener('complete', function(){view.animate(anim2);});
		var anim2 = Ti.UI.createAnimation({duration : 800, transform : t2});
		anim2.addEventListener('complete', function(){view.animate(anim3);});
		var anim3 = Ti.UI.createAnimation({duration : 500, transform : t3});
		anim3.addEventListener('complete', function(){view.animate(anim4);});
		var anim4 = Ti.UI.createAnimation({duration : 500, transform : t4});
		anim4.addEventListener('complete', function(){gone = true;});
		
		view.addEventListener('click', function(e) {
			if (gone === true) {
				view.animate({duration : 300, transform : t0}, function(){gone = false;});
			}
			else
				view.animate(anim1);
		});
		win.addEventListener('click', function(e) {
			if (gone === true) {
				view.animate({duration : 300, transform : t0}, function(){gone = false;});
			}
		});
	}
	
	win.add(view);
	openWin(win);
}

function transform2Ex() {
	var win = createWin();
	var view = Shape.createView({
		top : 150,
		borderRadius : 10,
		borderColor : 'red',
		borderWidth : 5,
		bubbleParent : false,
		width : 300,
		height : 100,
		backgroundColor : 'white',
		transform : Ti.UI.create2DMatrix().scale(2.0, 2.0),
		viewMask : '/images/body-mask.png'
	});

	var button = Ti.UI.createButton({
		top : 10,
		width : 100,
		transform : Ti.UI.create2DMatrix().scale(2.0, 2.0).translate(0, 40),
		bubbleParent : false,
		title : 'test buutton'
	});
	button.addEventListener('click', function(e) {
		view.top -=1;
	});
	button.add(Ti.UI.createView({
		backgroundColor : 'red',
		bottom : 10,
		width : 5,
		height : 5
	}));

	var shape = Shape.createCircle({
		fillColor : '#bbb',
		lineColor : '#777',
		lineWidth : 1,
		lineShadow : {
			radius : 2,
			color : 'black'
		},
		radius : '45%'
	});
	view.add(shape);
	view.add(Ti.UI.createView({
		backgroundColor : 'red',
		bottom : 10,
		width : 30,
		height : 30
	}));
	view.addEventListener('click', function(e) {
		if (isAndroid)
			set.cancel();
		shape.animate({
			duration : 400,
			lineWidth : 20,
			autoreverse : true,
			lineColor : 'yellow',
			fillColor : 'blue'
		});
	});
	win.add(view);
	win.add(button);

	if (isAndroid) {
		var set = Ti.UI.createAnimationSet({
			playMode : 2
		});
		set.add({
			duration : 300,
			autoreverse : true,
			height : 300
		}, view);
		set.add({
			duration : 1000,
			lineWidth : 20,
			autoreverse : true,
			lineColor : 'yellow',
			fillColor : 'blue'
		}, shape);
		win.addEventListener('click', function(e) {
			shape.cancelAllAnimations();
			set.start();
		});
	}
	else {
		win.addEventListener('click', function(e) {
			view.animate({
				duration : 300,
				autoreverse : true,
				height : 300
			});
		});
	}

	openWin(win);
}

function buttonAndLabelEx() {
	var win = createWin();
	var button = Ti.UI.createButton({
		top : 20,
		titlePadding : {
			left : 30,
			top : 30,
			bottom : 30,
			right : 30
		},
		bubbleParent : false,
		title : 'test buutton'
	});

	button.add(Ti.UI.createView({
		backgroundColor : 'red',
		left : 10,
		width : 15,
		height : 15
	}));
	button.add(Ti.UI.createView({
		backgroundColor : 'green',
		bottom : 10,
		width : 15,
		height : 15
	}));
	button.add(Ti.UI.createView({
		backgroundColor : 'yellow',
		top : 10,
		width : 15,
		height : 15
	}));
	button.add(Ti.UI.createView({
		backgroundColor : 'orange',
		right : 10,
		width : 15,
		height : 15
	}));
	var t1 = Ti.UI.create2DMatrix();
	var t2 = Ti.UI.create2DMatrix().scale(2.0, 2.0).translate(0, 40).rotate(90);
	button.addEventListener('longpress', function(e) {
		button.animate({
			duration : 500,
			transform : varSwitch(button.transform, t1, t2),
		});
	});
	win.add(button);

	var label = Ti.UI.createLabel({
		bottom : 20,
		backgroundColor : 'gray',
		textPadding : {
			left : 30,
			top : 30,
			bottom : 30,
			right : 30
		},
		bubbleParent : false,
		text : 'This is a sample\n text for a label'
	});

	label.add(Ti.UI.createView({
		backgroundColor : 'red',
		left : 10,
		width : 15,
		height : 15
	}));
	label.add(Ti.UI.createView({
		backgroundColor : 'green',
		bottom : 10,
		width : 15,
		height : 15
	}));
	label.add(Ti.UI.createView({
		backgroundColor : 'yellow',
		top : 10,
		width : 15,
		height : 15
	}));
	label.add(Ti.UI.createView({
		backgroundColor : 'orange',
		right : 10,
		width : 15,
		height : 15
	}));
	var t3 = Ti.UI.create2DMatrix().scale(2.0, 2.0).translate(0, -40).rotate(90);
	label.addEventListener('longpress', function(e) {
		label.animate({
			duration : 500,
			transform : varSwitch(label.transform, t1, t3),
		});
	});
	win.add(label);
	openWin(win);
}

function htmlLabelEx() {
	var win = createWin();
	var scrollView = Ti.UI.createScrollView({
		layout:'vertical',
		contentHeight:Ti.UI.SIZE
	});
	var html = 'La <font color="red">musique</font> électronique <b>est un type de <big><big>musique</big></big> qui a <font color="green">été conçu à</font></b> partir des années<br> 1950 avec des générateurs de signaux<br> et de sons synthétiques. Avant de pouvoir être utilisée en temps réel, elle a été primitivement enregistrée sur bande magnétique, ce qui permettait aux compositeurs de manier aisément les sons, par exemple dans l\'utilisation de boucles répétitives superposées. Ses précurseurs ont pu bénéficier de studios spécialement équipés ou faisaient partie d\'institutions musicales pré-existantes. La musique pour bande de Pierre Schaeffer, également appelée musique concrète, se distingue de ce type de musique dans la mesure où son matériau primitif était constitué des sons de la vie courante. La particularité de la musique électronique de l\'époque est de n\'utiliser que des sons générés par des appareils électroniques.';


	scrollView.add(Ti.UI.createLabel({
		width:Ti.UI.FILL,
		height:Ti.UI.SIZE,
		bottom:20,
		html:html	
	}));
	scrollView.add(Ti.UI.createLabel({
		multiLineEllipsize:Ti.UI.TEXT_ELLIPSIZE_HEAD,
		bottom:20,
		html:html	
	}));
	scrollView.add(Ti.UI.createLabel({
		multiLineEllipsize:Ti.UI.TEXT_ELLIPSIZE_MIDDLE,
		bottom:20,
		html:html	
	}));
	scrollView.add(Ti.UI.createLabel({
		width:Ti.UI.FILL,
		height:Ti.UI.SIZE,
		bottom:20,
		multiLineEllipsize:Ti.UI.TEXT_ELLIPSIZE_TAIL,
		html:html	
	}));
	
	scrollView.add(Ti.UI.createLabel({
		width:200,
		height:Ti.UI.SIZE,
		bottom:20,
		multiLineEllipsize:Ti.UI.TEXT_ELLIPSIZE_TAIL,
		html:html	
	}));
	scrollView.add(Ti.UI.createLabel({
		width:200,
		height:Ti.UI.SIZE,
		bottom:20,
		ellipsize:Ti.UI.TEXT_ELLIPSIZE_TAIL,
		html:html	
	}));
	scrollView.add(Ti.UI.createLabel({
		height:200,
		bottom:20,
		ellipsize:Ti.UI.TEXT_ELLIPSIZE_TAIL,
		html:html	
	}));
	win.add(scrollView);
	openWin(win);
}

var mainWin = Ti.UI.createWindow({
	backgroundColor : backColor,
	exitOnClose : true,
	orientationModes : [Ti.UI.UPSIDE_PORTRAIT, Ti.UI.PORTRAIT, Ti.UI.LANDSCAPE_RIGHT, Ti.UI.LANDSCAPE_LEFT]
});

var listview = Ti.UI.createListView({
	allowsSelection : false,
	separatorStyle : Titanium.UI.ListViewSeparatorStyle.NONE,
	rowHeight : 50,
	selectedBackgroundGradient : {
		type : 'linear',
		colors : ['blue', 'white'],
		startPoint : {
			x : 0,
			y : 0
		},
		endPoint : {
			x : 0,
			y : "100%"
		}
	},
	sections : [{
		items : [{
			properties : {
				title : 'Shape'
			},
			callback : shapeEx
		}, {
			properties : {
				title : 'ButtonsAndLabels'
			},
			callback : buttonAndLabelEx
		}, {
			properties : {
				title : 'Transform'
			},
			callback : transform2Ex
		}, {
			properties : {
				title : 'AnimationSet'
			},
			callback : transformEx
		}, {
			properties : {
				title : 'HTML Label'
			},
			callback : htmlLabelEx
		}]
	}]
});
listview.addEventListener('itemclick', function(_event) {
	if (_event.hasOwnProperty('section') && _event.hasOwnProperty('itemIndex')) {
		var item = _event.section.getItemAt(_event.itemIndex);
		if (item.callback) {
			item.callback();
		}
	}
});
mainWin.add(listview);
openWin(mainWin, true);
