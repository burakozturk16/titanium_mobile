var ak = {animation:require('akylas.animation').init(this)};
var iOS7 = false;
var isAndroid = Ti.Platform.osname == "android";
var isApple = Ti.Platform.osname === 'ipad' || Ti.Platform.osname === 'iphone';
if (isApple) {
	isiOS7 = parseInt(Titanium.Platform.version.split(".")[0]) >= 7;
}
var backColor = 'white';
var textColor = 'black';
var navGroup;
var openWinArgs;
var html = '  SUCCESS     <font color="red">musique</font> électronique <b><span style="background-color:green;border-color:black;border-radius:20px;border-width:1px">est un type de </span><big><big>musique</big></big> qui a <font color="green">été conçu à</font></b> partir des années<br> 1950 avec des générateurs de signaux<br> et de sons synthétiques. Avant de pouvoir être utilisée en temps réel, elle a été primitivement enregistrée sur bande magnétique, ce qui permettait aux compositeurs de manier aisément les sons, par exemple dans l\'utilisation de boucles répétitives superposées. Ses précurseurs ont pu bénéficier de studios spécialement équipés ou faisaient partie d\'institutions musicales pré-existantes. La musique pour bande de Pierre Schaeffer, également appelée musique concrète, se distingue de ce type de musique dans la mesure où son matériau primitif était constitué des sons de la vie courante. La particularité de la musique électronique de l\'époque est de n\'utiliser que des sons générés par des appareils électroniques.';
// html = '<span style="border-style:solid;background-color:green;border-color:red;border-radius:20px;border-width:3px;padding-top:3px;padding-bottom:3px;line-height:2em;"> SUCCESS </span><br><span style="border-style:solid;background-color:green;border-color:red;border-radius:20px;border-width:3px;padding-top:0px;padding-bottom:0px;line-height:1em;"> SUCCESS </span>'
if (isAndroid) {
	backColor = 'black';
	textColor = 'gray';
}

function merge_options(obj1, obj2, _new) {
	_new = _new === true;
	var newObject = obj1;
	if (_new === true) {
		newObject = JSON.parse(JSON.stringify(obj1));
	}
	for (var attrname in obj2) {
		newObject[attrname] = obj2[attrname];
	}
	return newObject;
}
var initWindowArgs = {
	backgroundColor: backColor,
	orientationModes: [Ti.UI.UPSIDE_PORTRAIT,
		Ti.UI.PORTRAIT,
		Ti.UI.LANDSCAPE_RIGHT,
		Ti.UI.LANDSCAPE_LEFT
	]
};
if (isiOS7) {
	initWindowArgs = merge_options(initWindowArgs, {
		autoAdjustScrollViewInsets: true,
		extendEdges: [Ti.UI.EXTEND_EDGE_ALL],
		translucent: true
	});
}

function createWin(_args) {
	return Ti.UI.createWindow(merge_options(initWindowArgs, _args, true));
}

function createListView(_args) {
	var realArgs = merge_options({
		allowsSelection: false,
		unHighlightOnSelect: false,
		rowHeight: 50,
		selectedBackgroundGradient: {
			type: 'linear',
			colors: [{
				color: '#1E232C',
				offset: 0.0
			}, {
				color: '#3F4A58',
				offset: 0.2
			}, {
				color: '#3F4A58',
				offset: 0.8
			}, {
				color: '#1E232C',
				offset: 1
			}],
			startPoint: {
				x: 0,
				y: 0
			},
			endPoint: {
				x: 0,
				y: "100%"
			}
		}
	}, _args);
	var listview = Ti.UI.createListView(realArgs);
	listview.addEventListener('itemclick', function(_event) {
		info('itemclick ' + _event.itemIndex);
		if (_event.hasOwnProperty('section') && _event.hasOwnProperty('itemIndex')) {
			var item = _event.section.getItemAt(_event.itemIndex);
			if (item.callback) {
				item.callback();
			}
		}
	});
	return listview;
}

function varSwitch(_var, _val1, _val2) {
	return (_var === _val1) ? _val2 : _val1;
}
var androidActivitysSettings = {
	actionBar: {
		displayHomeAsUp: true,
		onHomeIconItemSelected: function(e) {
			e.window.close();
		}
	}
};

function openWin(_win, _withoutActionBar) {
	if (isAndroid) {
		if (_withoutActionBar !== true) _win.activity = androidActivitysSettings;
	}
	mainWin.openWindow(_win);
}

function transformExs() {
	var win = createWin();
	var listview = createListView();
	listview.sections = [{
		items: [{
			properties: {
				title: 'Transform',
				backgroundColor: cellColor(1)
			},
			callback: transform1Ex
		}, {
			properties: {
				title: 'TransformAnimated'
			},
			callback: transform2Ex
		}, {
			properties: {
				title: 'PopIn'
			},
			callback: transform3Ex
		}, {
			properties: {
				title: 'SlideIn'
			},
			callback: transform4Ex
		}, {
			properties: {
				title: 'ListView'
			},
			callback: transform5Ex
		}, {
			properties: {
				title: 'AnchorPoint'
			},
			callback: transform6Ex
		}]
	}];
	win.add(listview);
	openWin(win);
}

function transform1Ex() {
	var win = createWin();
	var button = Ti.UI.createButton({
		bottom: 20,
		bubbleParent: false,
		title: 'test buutton'
	});
	var t1 = Ti.UI.create2DMatrix();
	var t2 = t1.scale(2.0, 2.0).translate(0, 40).rotate(90);
	button.addEventListener('longpress', function(e) {
		button.animate({
			duration: 500,
			transform: varSwitch(button.transform, t2, t1),
		});
	});
	win.add(button);
	var label = Ti.UI.createLabel({
		bottom: 20,
		backgroundColor: 'gray',
		backgroundSelectedColor: '#ddd',
		bubbleParent: false,
		text: 'This is a sample\n text for a label'
	});
	var t3 = t1.scale(2.0, 2.0).translate(0, -40).rotate(90);
	label.addEventListener('longpress', function(e) {
		label.animate({
			duration: 500,
			transform: varSwitch(label.transform, t3, t1),
		});
	});
	win.add(label);
	openWin(win);
}

function transform2Ex() {
	var gone = false;
	var win = createWin();
	var t0 = Ti.UI.create2DMatrix({
		anchorPoint: {
			x: 0,
			y: "100%"
		}
	});
	var t1 = t0.rotate(30);
	var t2 = t0.rotate(145);
	var t3 = t0.rotate(135);
	var t4 = t0.translate(0, "100%").rotate(125);
	var t5 = Ti.UI.create2DMatrix().translate(0, ((Math.sqrt(2)) * 100)).rotate(180);
	var view = Ti.UI.createView({
		transform: t0,
		borderRadius: 20,
		borderColor: 'orange',
		borderWidth: 2,
		// backgroundPadding: {
		//	left: 10,
		//	right: 10,
		//	bottom: -5
		// },
		clipChildren: false,
		backgroundColor: 'yellow',
		backgroundGradient: {
			type: 'radial',
			colors: ['orange', 'yellow']
		},
		top: 30,
		width: 200,
		height: 100
	});
	var anim1 = Ti.UI.createAnimation({
		id: 1,
		cancelRunningAnimations: true,
		duration: 800,
		transform: t1
	});
	var animToRun = anim1;
	anim1.addEventListener('complete', function() {
		animToRun = anim2;
		view.animate(anim2);
	});
	var anim2 = Ti.UI.createAnimation({
		id: 2,
		cancelRunningAnimations: true,
		duration: 800,
		transform: t2
	});
	anim2.addEventListener('complete', function() {
		animToRun = anim3;
		view.animate(anim3);
	});
	var anim3 = Ti.UI.createAnimation({
		id: 3,
		cancelRunningAnimations: true,
		duration: 500,
		transform: t3
	});
	anim3.addEventListener('complete', function() {
		animToRun = anim5;
		view.animate(anim5);
	});
	var anim4 = Ti.UI.createAnimation({
		id: 4,
		cancelRunningAnimations: true,
		duration: 500,
		transform: t4
	});
	anim4.addEventListener('complete', function() {
		gone = true;
	});
	var anim5 = Ti.UI.createAnimation({
		id: 5,
		cancelRunningAnimations: true,
		duration: 4000,
		bottom: 145,
		width: 200,
		top: null
	});
	anim5.addEventListener('complete', function() {
		animToRun = anim6;
		view.animate(anim6);
	});
	var anim6 = Ti.UI.createAnimation({
		id: 6,
		cancelRunningAnimations: true,
		duration: 400,
		transform: t5
	});
	anim6.addEventListener('complete', function() {
		animToRun = anim1;
		gone = true;
	});

	function onclick() {
		if (gone === true) {
			view.animate({
				duration: 6000,
				transform: t0,
				top: 30,
				width: 100,
				bottom: null
			}, function() {
				gone = false;
			});
		} else view.animate(animToRun);
	}
	win.addEventListener('click', onclick);
	win.add(view);
	openWin(win);
}

function transform3Ex() {
	var win = createWin();
	var t = Ti.UI.create2DMatrix().scale(0.3, 0.6);
	var view = Ti.UI.createView({
		backgroundColor: 'red',
		borderRadius: [12, 4, 0, 40],
		disableHW: true,
		borderColor: 'green',
		borderPadding: {
			left: 10,
			right: 10,
			bottom: -5
		},
		borderWidth: 2,
		opacity: 0,
		width: 200,
		height: 200
	});
	view.add(Ti.UI.createView({
		borderColor: 'yellow',
		borderWidth: 2,
		backgroundColor: 'blue',
		bottom: 0,
		width: Ti.UI.FILL,
		height: 50
	}));
	var showMe = function() {
		view.opacity = 0;
		view.transform = t;
		win.add(view);
		ak.animation.fadeIn(view, 100);
		ak.animation.popIn(view);
	};
	var hideMe = function(_callback) {
		ak.animation.fadeOut(view, 200, function() {
			win.remove(view);
		});
	};
	var button = Ti.UI.createButton({
		bottom: 10,
		width: 100,
		bubbleParent: false,
		title: 'test buutton'
	});
	button.addEventListener('click', function(e) {
		if (view.opacity === 0) showMe();
		else hideMe();
	});
	win.add(button);
	openWin(win);
}

function transform4Ex() {
	var win = createWin();
	var t0 = Ti.UI.create2DMatrix();
	var t1 = t0.translate("-100%", 0);
	var t2 = t0.translate("100%", 0);
	var view = Ti.UI.createView({
		backgroundColor: 'red',
		opacity: 0,
		transform: t1,
		width: 200,
		height: 200
	});
	view.add(Ti.UI.createView({
		backgroundColor: 'blue',
		bottom: 10,
		width: 50,
		height: 50
	}));
	var showMe = function() {
		view.transform = t1;
		win.add(view);
		view.animate({
			duration: 3000,
			transform: t0,
			opacity: 1
		});
	};
	var hideMe = function(_callback) {
		view.animate({
			duration: 3000,
			transform: t2,
			opacity: 0
		}, function() {
			win.remove(view);
		});
	};
	var button = Ti.UI.createButton({
		bottom: 10,
		width: 100,
		bubbleParent: false,
		title: 'test buutton'
	});
	button.addEventListener('click', function(e) {
		if (view.opacity === 1) hideMe();
		else showMe();
	});
	win.add(button);
	openWin(win);
}

function transform5Ex() {
	var showItemIndex = -1;
	var showItemSection = null;

	function hideMenu() {
		if (showItemIndex != -1 && showItemSection !== null) {
			var hideItem = showItemSection.getItemAt(showItemIndex);
			hideItem.menu.transform = t1;
			hideItem.menu.opacity = 0;
			showItemSection.updateItemAt(showItemIndex, hideItem);
			showItemIndex = -1;
			showItemSection = null;
		}
	}
	var win = createWin();
	var t0 = Ti.UI.create2DMatrix();
	var t1 = t0.translate(0, "100%");
	var myTemplate = {
		childTemplates: [{
			type: 'Ti.UI.View',
			bindId: 'holder',
			properties: {
				width: Ti.UI.FILL,
				height: Ti.UI.FILL,
				touchEnabled: false,
				layout: 'horizontal',
				horizontalWrap: false
			},
			childTemplates: [{
				type: 'Ti.UI.ImageView',
				bindId: 'pic',
				properties: {
					touchEnabled: false,
					width: 50,
					height: 50
				}
			}, {
				type: 'Ti.UI.Label',
				bindId: 'info',
				properties: {
					color: textColor,
					touchEnabled: false,
					font: {
						size: 20,
						weight: 'bold'
					},
					width: Ti.UI.FILL,
					left: 10
				}
			}, {
				type: 'Ti.UI.Button',
				bindId: 'button',
				properties: {
					title: 'menu',
					left: 10
				},
				events: {
					'click': function(_event) {
						if (_event.hasOwnProperty('section') && _event.hasOwnProperty('itemIndex')) {
							hideMenu();
							var item = _event.section.getItemAt(_event.itemIndex);
							item.menu = {
								transform: t0,
								opacity: 1
							};
							showItemIndex = _event.itemIndex;
							showItemSection = _event.section;
							_event.section.updateItemAt(_event.itemIndex, item);
						}
					}
				}
			}]
		}, {
			type: 'Ti.UI.Label',
			bindId: 'menu',
			properties: {
				color: 'white',
				text: 'I am the menu',
				backgroundColor: '#444',
				width: Ti.UI.FILL,
				height: Ti.UI.FILL,
				opacity: 0,
				transform: t1
			},
			events: {
				'click': hideMenu
			}
		}]
	};
	var listView = createListView({
		templates: {
			'template': myTemplate
		},
		defaultItemTemplate: 'template'
	});
	var sections = [{
		headerTitle: 'Fruits / Frutas',
		items: [{
			info: {
				text: 'Apple'
			}
		}, {
			properties: {
				backgroundColor: 'red'
			},
			info: {
				text: 'Banana'
			},
			pic: {
				image: 'banana.png'
			}
		}]
	}, {
		headerTitle: 'Vegetables / Verduras',
		items: [{
			info: {
				text: 'Carrot'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}]
	}, {
		headerTitle: 'Grains / Granos',
		items: [{
			info: {
				text: 'Corn'
			}
		}, {
			info: {
				text: 'Rice'
			}
		}]
	}];
	listView.setSections(sections);
	win.add(listView);
	openWin(win);
}

function transform6Ex() {
	var win = createWin();
	var t = Ti.UI.create2DMatrix().rotate(90);
	var view = Ti.UI.createView({
		transform: null,
		backgroundColor: 'red',
		borderRadius: 12,
		borderColor: 'green',
		borderWidth: 2,
		width: 200,
		height: 200
	});
	win.add(view);
	var bid = -1;

	function createBtn(_title) {
		bid++;
		return {
			type: 'Ti.UI.Button',
			left: 0,
			bid: bid,
			title: _title
		}
	}

	win.add({
		properties: {
			width: 'SIZE',
			height: 'SIZE',
			left: 0,
			layout: 'vertical'
		},
		childTemplates: [
			createBtn('topright'),
			createBtn('bottomright'),
			createBtn('bottomleft'),
			createBtn('topleft'),
			createBtn('center'),
		]
	});
	win.addEventListener('click', function(e) {
		if (e.source.bid !== undefined) {
			var anchorPoint = {
				x: 0,
				y: 0
			};
			switch (e.source.bid) {
				case 0:
					anchorPoint.x = 1;
					break;
				case 1:
					anchorPoint.x = 1;
					anchorPoint.y = 1;
					break;
				case 2:
					anchorPoint.y = 1;
					break;
				case 3:
					break;
				case 4:
					anchorPoint.x = 0.5;
					anchorPoint.y = 0.5;
					break;
			}
			view.anchorPoint = anchorPoint;
		} else {
			// view.transform = (view.transform === null)?t:null;
			view.animate({
				transform: (view.transform === null) ? t : null,
				duration: 500
			});
		}
	});
	openWin(win);
}

function layoutExs() {
	var win = createWin();
	var listview = createListView();
	listview.sections = [{
		items: [{
			properties: {
				title: 'Animated Horizontal'
			},
			callback: layout1Ex
		}]
	}];
	win.add(listview);
	openWin(win);
}

function layout1Ex() {
	var win = createWin();
	var view = Ti.UI.createView({
		backgroundColor: 'green',
		width: 200,
		height: Ti.UI.SIZE,
		layout: 'horizontal'
	});
	var view1 = Ti.UI.createView({
		backgroundColor: 'red',
		width: 60,
		height: 80,
		left: 0
	});
	var view2 = Ti.UI.createView({
		backgroundColor: 'blue',
		width: 20,
		borderColor: 'red',
		borderWidth: 2,
		borderRadius: [2, 10, 0, 20],
		// top:10,
		height: 80,
		left: 10,
		right: 4
	});
	view2.add(Ti.UI.createView({
		backgroundColor: 'orange',
		width: 10,
		height: 20,
		top: 0
	}));
	var view3 = Ti.UI.createView({
		backgroundColor: 'orange',
		width: Ti.UI.FILL,
		height: Ti.UI.FILL,
		bottom: 6,
		right: 4
	});
	view.add(view1);
	view.add(view2);
	view.add({
		type: 'Ti.UI.View',
		properties: {
			backgroundColor: 'purple',
			width: Ti.UI.FILL,
			height: Ti.UI.FILL,
			bottom: 6,
			right: 4
		},
		childTemplates: [{
			type: 'Ti.UI.View',
			properties: {
				backgroundColor: 'orange',
				width: 50,
				height: 20,
				bottom: 0
			}
		}]

	});
	view.add(view3);
	win.add(view);
	win.addEventListener('click', function(e) {
		view2.animate({
			cancelRunningAnimations: true,
			// restartFromBeginning:true,
			duration: 3000,
			autoreverse: true,
			// repeat: 4,
			width: Ti.UI.FILL,
			height: 100,
			top: null,
			left: 0,
			right: 30
		});
	});
	openWin(win);
}

function buttonAndLabelEx() {
	var win = createWin({
		dispatchPressed: true,
		backgroundSelectedColor: 'green'
	});
	var button = Ti.UI.createButton({
		top: 0,
		padding: {
			left: 30,
			top: 30,
			bottom: 30,
			right: 30
		},
		height: 50,
		disableHW: true,
		bubbleParent: false,
		borderRadius: 10,
		borderColor: 'red',
		backgroundColor: 'gray',
		touchPassThrough: false,
		backgroundSelectedGradient: {
			type: 'linear',
			colors: ['#333', 'transparent'],
			startPoint: {
				x: 0,
				y: 0
			},
			endPoint: {
				x: 0,
				y: "100%"
			}
		},
		title: 'test buutton'
	});
	button.add(Ti.UI.createView({
		enabled: false,
		backgroundColor: 'purple',
		backgroundSelectedColor: 'white',
		left: 10,
		width: 15,
		height: 15
	}));
	button.add(Ti.UI.createView({
		backgroundColor: 'green',
		bottom: 10,
		width: 15,
		height: 15
	}));
	button.add(Ti.UI.createView({
		backgroundColor: 'yellow',
		top: 10,
		width: 15,
		height: 15
	}));
	button.add(Ti.UI.createView({
		touchPassThrough: true,
		backgroundColor: 'orange',
		borderRadius: 1,
		right: 0,
		width: 35,
		height: Ti.UI.FILL
	}));
	var t1 = Ti.UI.create2DMatrix();
	var t2 = Ti.UI.create2DMatrix().scale(2.0, 2.0).translate(0, 40).rotate(90);
	button.addEventListener('longpress', function(e) {
		button.animate({
			duration: 500,
			transform: varSwitch(button.transform, t2, t1),
		});
	});

	win.add(button);
	var label = Ti.UI.createLabel({
		bottom: 20,
		dispatchPressed: true,
		backgroundColor: 'gray',
		borderColor: 'blue',
		borderSelectedColor: 'red',
		backgroundSelectedColor: '#a46',
		padding: {
			left: 30,
			top: 30,
			bottom: 30,
			right: 30
		},
		// borderRadius:2,
		bubbleParent: false,
		selectedColor: 'green',
		backgroundSelectedGradient: {
			type: 'linear',
			colors: ['#333', 'transparent'],
			startPoint: {
				x: 0,
				y: 0
			},
			endPoint: {
				x: 0,
				y: "100%"
			}
		},
		text: 'This is a sample\n text for a label'
	});
	label.add(Ti.UI.createView({
		touchEnabled: false,
		backgroundColor: 'red',
		backgroundSelectedColor: 'white',
		left: 10,
		width: 15,
		height: 15
	}));
	label.add(Ti.UI.createView({
		backgroundColor: 'green',
		bottom: 10,
		width: 15,
		height: 15
	}));
	label.add(Ti.UI.createView({
		backgroundColor: 'yellow',
		top: 10,
		width: 15,
		height: 15
	}));
	label.add(Ti.UI.createView({
		backgroundColor: 'orange',
		right: 10,
		width: 15,
		height: 15
	}));
	var t3 = Ti.UI.create2DMatrix().scale(2.0, 2.0).translate(0, -40).rotate(90);
	label.addEventListener('longpress', function(e) {
		label.animate({
			duration: 500,
			transform: varSwitch(label.transform, t3, t1),
		});
	});
	win.add(label);
	var button2 = Ti.UI.createButton({
		padding: {
			left: 80
		},
		bubbleParent: false,
		backgroundColor: 'gray',
		dispatchPressed: true,
		selectedColor: 'red',
		backgroundSelectedGradient: {
			type: 'linear',
			colors: ['#333', 'transparent'],
			startPoint: {
				x: 0,
				y: 0
			},
			endPoint: {
				x: 0,
				y: "100%"
			}
		},
		title: 'test buutton'
	});
	button2.add(Ti.UI.createButton({
		left: 0,
		backgroundColor: 'green',
		selectedColor: 'red',
		backgroundSelectedGradient: {
			type: 'linear',
			colors: ['#333', 'transparent'],
			startPoint: {
				x: 0,
				y: 0
			},
			endPoint: {
				x: 0,
				y: "100%"
			}
		},
		title: 'Osd'
	}));
	win.add(button2);
	openWin(win);
}

function maskEx() {
	var win = createWin();
	win.backgroundGradient = {
		type: 'linear',
		colors: ['gray', 'white'],
		startPoint: {
			x: 0,
			y: 0
		},
		endPoint: {
			x: 0,
			y: "100%"
		}
	};
	var view = Ti.UI.createView({
		top: 20,
		borderRadius: 10,
		borderColor: 'red',
		borderWidth: 5,
		bubbleParent: false,
		width: 300,
		height: 100,
		backgroundColor: 'green',
		viewMask: '/images/body-mask.png',
		backgroundGradient: {
			type: 'linear',
			colors: ['red', 'green', 'orange'],
			startPoint: {
				x: 0,
				y: 0
			},
			endPoint: {
				x: 0,
				y: "100%"
			}
		}
	});
	var imageView = Ti.UI.createImageView({
		bottom: 20,
		// borderRadius : 10,
		// borderColor:'red',
		// borderWidth:5,
		bubbleParent: false,
		width: 300,
		height: 100,
		backgroundColor: 'yellow',
		scaleType: Ti.UI.SCALE_TYPE_ASPECT_FIT,
		image: '/images/slightlylargerimage.png',
		imageMask: '/images/body-mask.png',
		// viewMask : '/images/mask.png',
	});
	view.add(Ti.UI.createView({
		backgroundColor: 'red',
		bottom: 10,
		width: 30,
		height: 30
	}));
	win.add(view);
	win.add(imageView);
	win.add(Ti.UI.createButton({
		borderRadius: 20,
		padding: {
			left: 30,
			top: 30,
			bottom: 30,
			right: 30
		},
		bubbleParent: false,
		title: 'test buutton',
		viewMask: '/images/body-mask.png'
	}));
	openWin(win);
}

function ImageViewEx() {
	var win = createWin();
	var view = Ti.UI.createImageView({
		bubbleParent: false,
		width: 300,
		localLoadSync: true,
		height: Ti.UI.SIZE,
		borderColor: 'red',
		borderWidth: 2,
		backgroundColor: 'green',
		backgroundGradient: {
			type: 'linear',
			colors: ['red', 'green', 'orange'],
			startPoint: {
				x: 0,
				y: 0
			},
			endPoint: {
				x: 0,
				y: "100%"
			}
		},
		image: 'http://cf2.imgobject.com/t/p/w154/vjDUeQvczSdL8nzcMVwZtlVSXYe.jpg',
	});
	view.add(Ti.UI.createView({
		backgroundColor: 'yellow',
		top: 10,
		width: 15,
		height: 15
	}));
	view.addEventListener('click', function() {
		//		view.image = varSwitch(view.image, '/images/slightlylargerimage.png', '/images/poster.jpg');
		view.animate({
			height: 400,
			duration: 1000,
			autoreverse: true
		});
	});
	win.add(view);
	openWin(win);
}

function random(min, max) {
	if (max == null) {
		max = min;
		min = 0;
	}
	return min + Math.floor(Math.random() * (max - min + 1));
};

function listView2Ex() {
	var win = createWin();
	// Create a custom template that displays an image on the left,
	// then a title next to it with a subtitle below it.
	var myTemplate = {
		childTemplates: [{ // Image justified left
			type: 'Ti.UI.ImageView', // Use an image view for the image
			bindId: 'pic', // Maps to a custom pic property of the item data
			properties: { // Sets the image view  properties
				width: '50dp',
				height: '50dp',
				left: 0
			}
		}, { // Title
			type: 'Ti.UI.Label', // Use a label for the title
			bindId: 'info', // Maps to a custom info property of the item data
			properties: { // Sets the label properties
				color: 'black',
				font: {
					fontFamily: 'Arial',
					size: '20dp',
					weight: 'bold'
				},
				left: '60dp',
				top: 0,
			}
		}, { // Subtitle
			type: 'Ti.UI.Label', // Use a label for the subtitle
			bindId: 'es_info', // Maps to a custom es_info property of the item data
			properties: { // Sets the label properties
				color: 'gray',
				font: {
					fontFamily: 'Arial',
					size: '14dp'
				},
				left: '60dp',
				top: '25dp',
			}
		}, { // Subtitle
			type: 'Ti.UI.Label', // Use a label for the subtitle
			properties: { // Sets the label properties
				color: 'red',
				selectedColor: 'green',
				backgroundColor: 'blue',
				backgroundSelectedColor: 'orange',
				text: 'test',
				right: '0dp'
			},
			events: {
				'click': function() {}
			}
		}]
	};
	var listView = Ti.UI.createListView({
		delaysContentTouches: false,
		// Maps myTemplate dictionary to 'template' string
		templates: {
			'template': myTemplate
		},
		// Use 'template', that is, the myTemplate dict created earlier
		// for all items as long as the template property is not defined for an item.
		defaultItemTemplate: 'template',
		selectedBackgroundGradient: {
			type: 'linear',
			colors: ['blue', 'green'],
			startPoint: {
				x: 0,
				y: 0
			},
			endPoint: {
				x: 0,
				y: "100%"
			}
		}
	});
	if (isApple) listView.style = Titanium.UI.iPhone.ListViewStyle.GROUPED;
	var sections = [];
	var fruitSection = Ti.UI.createListSection({
		headerTitle: 'Fruits / Frutas'
	});
	var fruitDataSet = [
		// the text property of info maps to the text property of the title label
		// the text property of es_info maps to text property of the subtitle label
		// the image property of pic maps to the image property of the image view
		{
			info: {
				text: 'Apple'
			},
			es_info: {
				text: 'Manzana'
			},
			pic: {
				image: 'apple.png'
			}
		}, {
			properties: {
				backgroundColor: 'red'
			},
			info: {
				text: 'Banana'
			},
			es_info: {
				text: 'Banana'
			},
			pic: {
				image: 'banana.png'
			}
		}
	];
	fruitSection.setItems(fruitDataSet);
	sections.push(fruitSection);
	var vegSection = Ti.UI.createListSection({
		headerTitle: 'Vegetables / Verduras'
	});
	var vegDataSet = [{
		info: {
			text: 'Carrot'
		},
		es_info: {
			text: 'Zanahoria'
		},
		pic: {
			image: 'carrot.png'
		}
	}, {
		info: {
			text: 'Potato'
		},
		es_info: {
			text: 'Patata'
		},
		pic: {
			image: 'potato.png'
		}
	}];
	vegSection.setItems(vegDataSet);
	sections.push(vegSection);
	var grainSection = Ti.UI.createListSection({
		headerTitle: 'Grains / Granos'
	});
	var grainDataSet = [{
		info: {
			text: 'Corn'
		},
		es_info: {
			text: 'Maiz'
		},
		pic: {
			image: 'corn.png'
		}
	}, {
		info: {
			text: 'Rice'
		},
		es_info: {
			text: 'Arroz'
		},
		pic: {
			image: 'rice.png'
		}
	}];
	grainSection.setItems(grainDataSet);
	sections.push(grainSection);
	listView.setSections(sections);
	win.add(listView);
	openWin(win);
}

var sweepGradient = {
	type: 'sweep',
	colors: [{
		color: 'orange',
		offset: 0
	}, {
		color: 'red',
		offset: 0.19
	}, {
		color: 'red',
		offset: 0.25
	}, {
		color: 'blue',
		offset: 0.25
	}, {
		color: 'blue',
		offset: 0.31
	}, {
		color: 'green',
		offset: 0.55
	}, {
		color: 'yellow',
		offset: 0.75
	}, {
		color: 'orange',
		offset: 1
	}]
};

function listViewEx() {
	var win = createWin();
	var listview = Ti.UI
		.createListView({
			allowsSelection: false,
			rowHeight: 50,
			selectedBackgroundGradient: sweepGradient,
			sections: [{
				items: [{
					properties: {
						backgroundColor: 'blue',
						title: 'Shape'
					}
				}, {
					properties: {
						backgroundColor: 'red',
						title: 'ButtonsAndLabels'
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Transform',
						accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						backgroundColor: 'red',
						title: 'Shape',
						accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Transform',
						accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						backgroundColor: 'red',
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Transform',
						accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Transform',
						accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						backgroundColor: 'red',
						title: 'Shape',
						accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Transform',
						accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						backgroundColor: 'red',
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Transform',
						accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Transform',
						accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						backgroundColor: 'red',
						title: 'Shape',
						accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Transform',
						accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						backgroundColor: 'red',
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Transform',
						accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Transform',
						accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						backgroundColor: 'red',
						title: 'Shape',
						accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Transform',
						accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						backgroundColor: 'red',
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Transform',
						accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Transform',
						accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						backgroundColor: 'red',
						title: 'Shape',
						accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Transform',
						accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						backgroundColor: 'red',
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Transform',
						accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
					}
				}, {
					properties: {
						title: 'Shape'
					}
				}, {
					properties: {
						title: 'Transform',
						accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
					}
				}]
			}]
		});
	if (isApple) listview.style = Titanium.UI.iPhone.ListViewStyle.GROUPED;
	win.add(listview);
	openWin(win);
}

function fadeInEx() {
	var win = createWin();
	var view = Ti.UI.createView({
		backgroundColor: 'red',
		opacity: 0,
		width: 200,
		height: 200
	});
	view.add(Ti.UI.createView({
		backgroundColor: 'blue',
		bottom: 10,
		width: 50,
		height: 50
	}));
	var showMe = function() {
		view.opacity = 0;
		view.transform = Ti.UI.create2DMatrix().scale(0.6, 0.6);
		win.add(view);
		view.animate({
			opacity: 1,
			duration: 300,
			transform: Ti.UI.create2DMatrix()
		});
	};
	var hideMe = function(_callback) {
		view.animate({
			opacity: 0,
			duration: 200
		}, function() {
			win.remove(view);
		});
	};
	var button = Ti.UI.createButton({
		top: 10,
		width: 100,
		bubbleParent: false,
		title: 'test buutton'
	});
	button.addEventListener('click', function(e) {
		if (view.opacity === 0) showMe();
		else hideMe();
	});
	win.add(button);
	openWin(win);
}
// if (false) {
// 	var set = Ti.UI.createAnimationSet({
// 		playMode : 1
// 	});
// 	set.addEventListener('complete', function(e) {
// 		gone = true;
// 	});
// 	set.add({
// 		duration : 800,
// 		transform : t1
// 	}, view);
// 	set.add({
// 		duration : 800,
// 		transform : t2
// 	}, view);
// 	set.add({
// 		duration : 500,
// 		transform : t3
// 	}, view);
// 	set.add({
// 		duration : 500,
// 		transform : t4
// 	}, view);
// 	view.addEventListener('click', function(e) {
// 		if (gone === true) {
// 			view.animate({
// 				duration : 300,
// 				transform : t0
// 			}, function() {
// 				gone = false
// 			});
// 		} else
// 			set.start();
// 	});
// 	win.addEventListener('click', function(e) {
// 		if (gone === true) {
// 			view.animate({
// 				duration : 300,
// 				transform : t0
// 			}, function() {
// 				gone = false
// 			});
// 		}
// 	});
// } else {
// function transform2Ex() {
// 	var win = createWin();
// 	var view = Shape.createView({
// 		top : 150,
// 		borderRadius : 10,
// 		borderColor : 'red',
// 		borderWidth : 5,
// 		bubbleParent : false,
// 		width : 300,
// 		height : 100,
// 		backgroundColor : 'white',
// 		transform : Ti.UI.create2DMatrix().scale(2.0, 2.0),
// 		viewMask : '/images/body-mask.png'
// 	});
// 	var button = Ti.UI.createButton({
// 		top : 10,
// 		width : 100,
// 		transform : Ti.UI.create2DMatrix().scale(2.0, 2.0).translate(0, 40),
// 		bubbleParent : false,
// 		title : 'test buutton'
// 	});
// 	button.addEventListener('click', function(e) {
// 		view.top -=1;
// 	});
// 	button.add(Ti.UI.createView({
// 		backgroundColor : 'red',
// 		bottom : 10,
// 		width : 5,
// 		height : 5
// 	}));
// 	var shape = Shape.createCircle({
// 		fillColor : '#bbb',
// 		lineColor : '#777',
// 		lineWidth : 1,
// 		lineShadow : {
// 			radius : 2,
// 			color : 'black'
// 		},
// 		radius : '45%'
// 	});
// 	view.add(shape);
// 	view.add(Ti.UI.createView({
// 		backgroundColor : 'red',
// 		bottom : 10,
// 		width : 30,
// 		height : 30
// 	}));
// 	view.addEventListener('click', function(e) {
// 		if (isAndroid)
// 			set.cancel();
// 		shape.animate({
// 			duration : 400,
// 			lineWidth : 20,
// 			autoreverse : true,
// 			lineColor : 'yellow',
// 			fillColor : 'blue'
// 		});
// 	});
// 	win.add(view);
// 	win.add(button);
// 	if (isAndroid) {
// 		var set = Ti.UI.createAnimationSet({
// 			playMode : 2
// 		});
// 		set.add({
// 			duration : 300,
// 			autoreverse : true,
// 			height : 300
// 		}, view);
// 		set.add({
// 			duration : 1000,
// 			lineWidth : 20,
// 			autoreverse : true,
// 			lineColor : 'yellow',
// 			fillColor : 'blue'
// 		}, shape);
// 		win.addEventListener('click', function(e) {
// 			shape.cancelAllAnimations();
// 			set.start();
// 		});
// 	}
// 	else {
// 		win.addEventListener('click', function(e) {
// 			view.animate({
// 				duration : 300,
// 				autoreverse : true,
// 				height : 300
// 			});
// 		});
// 	}
// 	openWin(win);
// }

function htmlLabelEx() {
	var win = createWin();
	var scrollView = Ti.UI.createScrollView({
		layout: 'vertical',
		contentWidth: 'FILL',
		contentHeight: Ti.UI.SIZE
	});
	scrollView.add(Ti.UI.createLabel({
		width: Ti.UI.FILL,
		padding: {
			left: 20,
			right: 20,
			top: 20,
			bottom: 20
		},
		height: Ti.UI.SIZE,
		bottom: 20,
		html: html
	}));
	scrollView.add(Ti.UI.createLabel({
		multiLineEllipsize: Ti.UI.TEXT_ELLIPSIZE_HEAD,
		truncationString: '_ _',
		// verticalAlign:'top',
		bottom: 20,
		html: html
	}));
	scrollView.add(Ti.UI.createLabel({
		multiLineEllipsize: Ti.UI.TEXT_ELLIPSIZE_MIDDLE,
		bottom: 20,
		html: html
	}));
	scrollView.add(Ti.UI.createLabel({
		width: Ti.UI.FILL,
		height: Ti.UI.SIZE,
		// verticalAlign:'bottom',
		bottom: 20,
		multiLineEllipsize: Ti.UI.TEXT_ELLIPSIZE_TAIL,
		html: html
	}));
	scrollView.add(Ti.UI.createLabel({
		width: 200,
		height: Ti.UI.SIZE,
		backgorundColor: 'green',
		bottom: 20,
		multiLineEllipsize: Ti.UI.TEXT_ELLIPSIZE_TAIL,
		html: html
	}));
	scrollView.add(Ti.UI.createLabel({
		width: 200,
		height: Ti.UI.SIZE,
		backgorundColor: 'blue',
		bottom: 20,
		ellipsize: Ti.UI.TEXT_ELLIPSIZE_TAIL,
		html: html
	}));
	scrollView.add(Ti.UI.createLabel({
		height: 200,
		bottom: 20,
		ellipsize: Ti.UI.TEXT_ELLIPSIZE_TAIL,
		html: html
	}));
	win.add(scrollView);
	scrollView.addEventListener('click', function(e) {
		Ti.API.info(e.link);
	})

	openWin(win);
}

function svgExs() {
	var win = createWin();
	var listview = createListView();
	listview.sections = [{
		items: [{
			properties: {
				title: 'View'
			},
			callback: svg1Ex
		}, {
			properties: {
				title: 'Button'
			},
			callback: svg2Ex
		}, {
			properties: {
				title: 'ImageView'
			},
			callback: svg3Ex
		}, {
			properties: {
				title: 'ListView'
			},
			callback: svg4Ex
		}]
	}];
	win.add(listview);
	openWin(win);
}

function svg1Ex() {
	var win = createWin();
	var view = Ti.UI.createView({
		bubbleParent: false,
		width: 100,
		height: 100,
		backgroundColor: 'yellow',
		scaleType: Ti.UI.SCALE_TYPE_ASPECT_FIT,
		preventDefaultImage: true,
		backgroundImage: '/images/Notepad_icon_small.svg'
	});
	win.add(view);
	var button = Ti.UI.createButton({
		top: 20,
		bubbleParent: false,
		title: 'change svg'
	});
	button.addEventListener('click', function() {
		view.backgroundImage = varSwitch(view.backgroundImage, '/images/gradients.svg', '/images/Logo.svg');
	});
	win.add(button);
	var button2 = Ti.UI.createButton({
		bottom: 20,
		bubbleParent: false,
		title: 'animate'
	});
	button2.addEventListener('click', function() {
		view.animate({
			height: Ti.UI.FILL,
			width: Ti.UI.FILL,
			duration: 2000,
			autoreverse: true
		});
	});
	win.add(button2);
	openWin(win);
}

function svg2Ex() {
	var win = createWin();
	var button = Ti.UI.createButton({
		top: 20,
		padding: {
			left: 30,
			top: 30,
			bottom: 30,
			right: 30
		},
		bubbleParent: false,
		image: '/images/Logo.svg',
		title: 'test buutton'
	});
	win.add(button);
	openWin(win);
}

function svg3Ex() {
	var win = createWin({
		backgroundImage: '/images/Notepad_icon_small.svg'
	});
	var imageView = Ti.UI.createImageView({
		bubbleParent: false,
		width: 300,
		height: 100,
		backgroundColor: 'yellow',
		scaleType: Ti.UI.SCALE_TYPE_ASPECT_FIT,
		preventDefaultImage: true,
		image: '/images/Notepad_icon_small.svg'
	});
	imageView.addEventListener('click', function() {
		imageView.scaleType = (imageView.scaleType + 1) % 6;
	});
	win.add(imageView);
	var button = Ti.UI.createButton({
		top: 20,
		bubbleParent: false,
		title: 'change svg'
	});
	button.addEventListener('click', function() {
		imageView.image = varSwitch(imageView.image, '/images/gradients.svg', '/images/Logo.svg');
	});
	win.add(button);
	var button2 = Ti.UI.createButton({
		bottom: 20,
		bubbleParent: false,
		title: 'animate'
	});
	button2.addEventListener('click', function() {
		imageView.animate({
			height: 400,
			duration: 1000,
			autoreverse: true
		});
	});
	win.add(button2);
	openWin(win);
}

function svg4Ex() {
	var win = createWin();
	var myTemplate = {
		childTemplates: [{
			type: 'Ti.UI.View',
			bindId: 'holder',
			properties: {
				width: Ti.UI.FILL,
				height: Ti.UI.FILL,
				touchEnabled: false,
				layout: 'horizontal',
				horizontalWrap: false
			},
			childTemplates: [{
				type: 'Ti.UI.ImageView',
				bindId: 'pic',
				properties: {
					touchEnabled: false,
					// localLoadSync:true,
					transition: {
						style: Ti.UI.TransitionStyle.FADE
					},
					height: 'FILL',
					image: '/images/gradients.svg'
				}
			}, {
				type: 'Ti.UI.Label',
				bindId: 'info',
				properties: {
					color: textColor,
					touchEnabled: false,
					font: {
						size: 20,
						weight: 'bold'
					},
					width: Ti.UI.FILL,
					left: 10
				}
			}, {
				type: 'Ti.UI.Button',
				bindId: 'button',
				properties: {
					title: 'menu',
					left: 10
				}
			}]
		}, {
			type: 'Ti.UI.Label',
			bindId: 'menu',
			properties: {
				color: 'white',
				text: 'I am the menu',
				backgroundColor: '#444',
				width: Ti.UI.FILL,
				height: Ti.UI.FILL,
				opacity: 0
			},
		}]
	};
	var listView = createListView({
		templates: {
			'template': myTemplate
		},
		defaultItemTemplate: 'template'
	});
	var sections = [{
		headerTitle: 'Fruits / Frutas',
		items: [{
			info: {
				text: 'Apple'
			}
		}, {
			properties: {
				backgroundColor: 'red'
			},
			info: {
				text: 'Banana'
			},
			pic: {
				image: 'banana.png'
			}
		}]
	}, {
		headerTitle: 'Vegetables / Verduras',
		items: [{
			info: {
				text: 'Carrot'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			},
			pic: {
				image: '/images/opacity.svg'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			},
			pic: {
				image: '/images/opacity.svg'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			},
			pic: {
				image: '/images/Logo.svg'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}, {
			info: {
				text: 'Potato'
			}
		}]
	}, {
		headerTitle: 'Grains / Granos',
		items: [{
			info: {
				text: 'Corn'
			}
		}, {
			info: {
				text: 'Rice'
			}
		}]
	}];
	listView.setSections(sections);
	win.add(listView);
	openWin(win);
}

function float2color(pr, pg, pb) {
	var color_part_dec = 255 * pr;
	var r = Number(parseInt(color_part_dec, 10)).toString(16);
	color_part_dec = 255 * pg;
	var g = Number(parseInt(color_part_dec, 10)).toString(16);
	color_part_dec = 255 * pb;
	var b = Number(parseInt(color_part_dec, 10)).toString(16);
	return "#" + r + g + b;
}

function cellColor(_index) {
	switch (_index % 4) {
		case 0: // Green
			return float2color(0.196, 0.651, 0.573);
		case 1: // Orange
			return float2color(1, 0.569, 0.349);
		case 2: // Red
			return float2color(0.949, 0.427, 0.427);
			break;
		case 3: // Blue
			return float2color(0.322, 0.639, 0.800);
			break;
		default:
			break;
	}
}
var transitionsMap = [{
	title: 'SwipFade',
	id: Ti.UI.TransitionStyle.SWIPE_FADE
}, {
	title: 'Flip',
	id: Ti.UI.TransitionStyle.FLIP
}, {
	title: 'Cube',
	id: Ti.UI.TransitionStyle.CUBE
}, {
	title: 'Fold',
	id: Ti.UI.TransitionStyle.FOLD
}, {
	title: 'Fade',
	id: Ti.UI.TransitionStyle.FADE
}, {
	title: 'Back Fade',
	id: Ti.UI.TransitionStyle.BACK_FADE
}, {
	title: 'Scale',
	id: Ti.UI.TransitionStyle.SCALE
}, {
	title: 'Push Rotate',
	id: Ti.UI.TransitionStyle.PUSH_ROTATE
}, {
	title: 'Slide',
	id: Ti.UI.TransitionStyle.SLIDE
}, {
	title: 'Modern Push',
	id: Ti.UI.TransitionStyle.MODERN_PUSH
}, {
	title: 'Ghost',
	id: Ti.UI.TransitionStyle.GHOST
}, {
	title: 'Zoom',
	id: Ti.UI.TransitionStyle.ZOOM
}, {
	title: 'SWAP',
	id: Ti.UI.TransitionStyle.SWAP
}, {
	title: 'CAROUSEL',
	id: Ti.UI.TransitionStyle.CAROUSEL
}, {
	title: 'CROSS',
	id: Ti.UI.TransitionStyle.CROSS
}, {
	title: 'GLUE',
	id: Ti.UI.TransitionStyle.GLUE
}];

function choseTransition(_view, _property) {
	var optionTitles = [];
	for (var i = 0; i < transitionsMap.length; i++) {
		optionTitles.push(transitionsMap[i].title);
	};
	optionTitles.push('Cancel');
	var opts = {
		cancel: optionTitles.length - 1,
		options: optionTitles,
		selectedIndex: transitionsMap.indexOf(_.findWhere(transitionsMap, {
			id: _view[_property].style
		})),
		title: 'Transition Style'
	};

	var dialog = Ti.UI.createOptionDialog(opts);
	dialog.addEventListener('click', function(e) {
		if (e.cancel == false) {
			_view[_property] = {
				style: transitionsMap[e.index].id
			};
		}
	});
	dialog.show();
}

function test2() {
	var win = createWin({
		modal: true
	});
	var view = Ti.UI.createView({
		top: 0,
		width: Ti.UI.FILL,
		backgroundColor: 'purple',
		height: 60
	});
	var view1 = Ti.UI.createScrollView({
		top: 0,
		backgroundColor: 'yellow',
		layout: 'vertical',
		height: Ti.UI.SIZE,
		left: 5,
		right: 5
	});
	var view2 = Ti.UI.createView({
		height: 'SIZE',
		backgroundColor: 'blue',
		layout: 'horizontal',
		width: Ti.UI.FILL
	});
	var view3 = Ti.UI.createTextField({
		value: 'This is my tutle test',
		backgroundColor: 'red',
		ellipsize: Ti.UI.TEXT_ELLIPSIZE_TAIL,
		font: {
			size: 14
		},
		width: 75,
		right: 20
	});
	var view4 = Ti.UI.createView({
		height: 'SIZE',
		backgroundColor: 'green',
		layout: 'horizontal',
		width: Ti.UI.FILL
	});
	var view5 = Ti.UI.createLabel({
		height: Ti.UI.FILL,
		text: 'button1',
		width: Ti.UI.FILL,
		left: 0,
		height: 35,
		top: 5,
		enabled: false,
		backgroundColor: 'red',
		borderColor: '#006598',
		selectedColor: 'white',
		disabledColor: 'white',
		color: '#006598',
		backgroundDisabledColor: '#006598',
		backgroundSelectedColor: '#006598'
	});
	var view6 = Ti.UI.createLabel({
		height: Ti.UI.FILL,
		text: 'button2',
		width: Ti.UI.FILL,
		left: 0,
		height: 35,
		top: 5,
		backgroundColor: 'transparent',
		borderColor: '#006598',
		selectedColor: 'white',
		disabledColor: 'white',
		color: '#006598',
		backgroundDisabledColor: '#006598',
		backgroundSelectedColor: '#006598'
	});

	view4.add([view5, view6]);
	view2.add([view3, view4]);
	view1.add(view2);
	view.add(view1);
	win.add(view);
	win.open();
}

function listViewLayout() {
	var win = createWin();
	var template = {
		properties: {
			layout: 'horizontal',
			backgroundColor: 'orange',
			dispatchPressed: true,
			height: 40,
			borderColor: 'blue'
		},
		childTemplates: [{
			type: 'Ti.UI.ImageView',
			bindId: 'button',
			properties: {
				width: 41,
				height: 'FILL',
				padding: {
					top: 10,
					bottom: 10,
					left: 10,
					right: 10
				},
				left: 4,
				right: 4,
				font: {
					size: 18,
					weight: 'bold'
				},
				// transition: {
				// style: Ti.UI.TransitionStyle.FADE,
				// substyle:Ti.UI.TransitionStyle.TOP_TO_BOTTOM
				// },
				localLoadSync: true,
				// backgroundColor:'blue',
				borderColor: 'gray',
				borderSelectedColor: 'red',
				backgroundGradient: {
					type: 'linear',
					colors: [{
						color: 'blue',
						offset: 0.0
					}, {
						color: 'transparent',
						offset: 0.2
					}, {
						color: 'transparent',
						offset: 0.8
					}, {
						color: 'blue',
						offset: 1
					}],
					startPoint: {
						x: 0,
						y: 0
					},
					endPoint: {
						x: 0,
						y: "100%"
					}
				},
				// borderRadius: 10,
				// clipChildren:false,
				color: 'white',
				selectedColor: 'black'
			}
		}, {
			type: 'Ti.UI.View',
			properties: {
				dispatchPressed: true,
				layout: 'vertical'
			},
			childTemplates: [{
				type: 'Ti.UI.View',
				properties: {
					dispatchPressed: true,
					layout: 'horizontal',
					height: 'FILL'
				},
				childTemplates: [{
					type: 'Ti.UI.Label',
					bindId: 'tlabel',
					properties: {
						top: 2,
						// backgroundGradient: {
						// type: 'linear',
						// colors: [{
						// color: 'yellow',
						// offset: 0.0
						// }, {
						// // 	color: 'yellow',
						// // 	offset: 0.2
						// // }, {
						// // 	color: 'yellow',
						// // 	offset: 0.8
						// // }, {
						// color: 'blue',
						// offset: 1
						// }],
						// startPoint: {
						// x: 0,
						// y: 0
						// },
						// endPoint: {
						// x: "100%",
						// y: 0,
						// }
						// },
						maxLines: 2,
						ellipsize: Ti.UI.TEXT_ELLIPSIZE_TAIL,
						font: {
							size: 14
						},
						height: 'FILL',
						width: 'FILL',
						// bottom: -9
					}
				}, {
					type: 'Ti.UI.Label',
					bindId: 'plabel',
					properties: {
						color: 'white',
						padding: {
							left: 14,
							right: 4,
							bottom: 2
						},
						shadowColor: '#55000000',
						selectedColor: 'green',
						shadowRadius: 2,
						borderRadius: 4,
						clipChildren: false,
						font: {
							size: 12,
							weight: 'bold'
						},
						backgroundSelectedGradient: sweepGradient,
						backgroundColor: 'red',
						right: 10,
						width: 100,
						height: 20
					}
				}]
			}, {
				type: 'Ti.UI.View',
				properties: {
					layout: 'horizontal',
					height: 20
				},
				childTemplates: [{
					type: 'Ti.UI.View',
					properties: {
						width: Ti.UI.FILL,
						backgroundColor: '#e9e9e9',
						borderRadius: 4,
						clipChildren: false,
						bottom: 0,
						height: 16
					},
					childTemplates: [{
						type: 'Ti.UI.View',
						bindId: 'progressbar',
						properties: {
							borderRadius: 4,
							clipChildren: false,
							left: 0,
							height: Ti.UI.FILL,
							backgroundColor: 'green'
						}
					}, {
						type: 'Ti.UI.Label',
						bindId: 'sizelabel',
						properties: {
							color: 'black',
							shadowColor: '#55ffffff',
							// width: 'FILL',
							// height: 'FILL',
							shadowRadius: 2,
							text: 'size',

							font: {
								size: 12
							}
						}
					}]
				}, {
					type: 'Ti.UI.Label',
					bindId: 'timelabel',
					properties: {
						font: {
							size: 12
						},
						color: 'black',
						textAlign: 'right',
						right: 4,
						height: 20,
						bottom: 2,
						width: 80
					}
				}]
			}]
		}]
	};

	var names = ['Carolyn Humbert',
		'David Michaels',
		'Rebecca Thorning',
		'Joe B',
		'Phillip Craig',
		'Michelle Werner',
		'Philippe Christophe',
		'Marcus Crane',
		'Esteban Valdez',
		'Sarah Mullock'
	];
	var priorities = ['downloading',
		'success',
		'failure',
		'',
		'test',
		'processing'
	];
	var images = ['http://cf2.imgobject.com/t/p/w154/vjDUeQvczSdL8nzcMVwZtlVSXYe.jpg',
	'http://zapp.trakt.us/images/posters_movies/192263-138.jpg',
	'http://zapp.trakt.us/images/posters_movies/210231-138.jpg',
	'http://zapp.trakt.us/images/posters_movies/176347-138.jpg',
	'http://zapp.trakt.us/images/posters_movies/210596-138.jpg'];
	var listView = createListView({
		rowHeight: 60,
		// minRowHeight: 40,
		defaultItemTemplate: 'template2'
	});

	listView.templates = {
		'template' : template,
		'template2' : {
			"properties" : {
				"rclass" : "NZBGetRowBordered",
				"height" : 75,
				"borderPadding" : {
					"left" : -1,
					"right" : -1,
					"top" : -1
				},
				"borderColor" : "#DDDDDD",
				"backgroundColor" : "white"
			},
			"childTemplates" : [{
				"type" : "Ti.UI.View",
				"properties" : {
					"rclass" : "NZBGetDRCheckHolder",
					"width" : 40,
					"height" : 40,
					"left" : 4
				},
				"childTemplates" : [{
					"type" : "Ti.UI.Label",
					"bindId" : "check",
					"properties" : {
						"rclass" : "NZBGetDRCheck",
						"color" : "transparent",
						"textAlign" : "center",
						"clipChildren" : false,
						"borderSelectedColor" : "#0088CC",
						"font" : {
							"family" : "LigatureSymbols"
						},
						"text" : "",
						"width" : 20,
						"height" : 20,
						"borderRadius" : 2,
						"borderColor" : "#DDDDDD"
					}
				}]
			}, {
				"type" : "Ti.UI.Button",
				"bindId" : "button",
				"properties" : {
					"rclass" : "NZBGetDRButton",
					"width" : 40,
					"height" : 40,
					"left" : 4,
					"font" : {
						"family" : "Simple-Line-Icons",
						"size" : 18,
						"weight" : "bold"
					},
					"borderRadius" : 10,
					"borderWidth" : 1,
					"color" : "white",
					"selectedColor" : "gray",
					"backgroundColor" : "transparent"
				},
				"events" : {}
			}, {
				"type" : "Ti.UI.View",
				"properties" : {
					"rclass" : "NZBGetDRVHolder",
					"layout" : "vertical",
					"left" : 44,
					"height" : "FILL",
					"width" : "FILL"
				},
				"childTemplates" : [{
					"type" : "Ti.UI.Label",
					"bindId" : "tlabel",
					"properties" : {
						"rclass" : "NZBGetRTitle",
						"padding" : {
							"left" : 5,
							"right" : 5
						},
						"ellipsize" : 'END',
						"maxLines" : 2,
						"height" : "48%",
						"width" : "FILL",
						"verticalAlign" : "top",
						"font" : {
							"size" : 14
						},
						"color" : "black"
					}
				}, {
					"type" : "Ti.UI.View",
					"properties" : {
						"rclass" : "Fill HHolder",
						"layout" : "horizontal",
						"width" : "FILL",
						"height" : "FILL"
					},
					"childTemplates" : [{
						"type" : "Ti.UI.Label",
						"bindId" : "category",
						"properties" : {
							"rclass" : "NZBGetLabelLeft NZBGetRTags",
							"backgroundColor" : "#999999",
							"color" : "white",
							"padding" : {
								"left" : 2,
								"right" : 2,
								"top" : 0
							},
							"shadowColor" : "#55000000",
							"shadowRadius" : 1,
							"borderRadius" : 2,
							"height" : "SIZE",
							"width" : "SIZE",
							"maxLines" : 1,
							"clipChildren" : false,
							"font" : {
								"size" : 12,
								"weight" : "bold"
							},
							"textAlign" : "left",
							"left" : 5
						}
					}, {
						"type" : "Ti.UI.Label",
						"bindId" : "health",
						"properties" : {
							"visible" : false,
							"rclass" : "NZBGetLabelLeft NZBGetRTags",
							"backgroundColor" : "#999999",
							"color" : "white",
							"padding" : {
								"left" : 2,
								"right" : 2,
								"top" : 0
							},
							"shadowColor" : "#55000000",
							"shadowRadius" : 1,
							"borderRadius" : 2,
							"height" : "SIZE",
							"width" : "SIZE",
							"maxLines" : 1,
							"clipChildren" : false,
							"font" : {
								"size" : 12,
								"weight" : "bold"
							},
							"textAlign" : "left",
							"left" : 5
						}
					}, {
						"type" : "Ti.UI.View",
						"properties" : {
							"rclass" : "FILL"
						}
					}, {
						"type" : "Ti.UI.Label",
						"bindId" : "priority",
						"properties" : {
							"rclass" : "NZBGetLabelRight NZBGetRPriority",
							"color" : "white",
							"padding" : {
								"left" : 2,
								"right" : 2,
								"top" : 0
							},
							"shadowColor" : "#55000000",
							"shadowRadius" : 1,
							"borderRadius" : 2,
							"height" : "SIZE",
							"width" : "SIZE",
							"maxLines" : 1,
							"clipChildren" : false,
							"font" : {
								"size" : 12,
								"weight" : "bold"
							},
							"backgroundColor" : "#b94a48",
							"textAlign" : "right",
							"right" : 5
						}
					}]
				}, {
					"type" : "Ti.UI.View",
					"properties" : {
						"rclass" : "Fill",
						"width" : "FILL",
						"height" : "FILL"
					},
					"childTemplates" : [{
						"type" : "Ti.UI.View",
						"properties" : {
							"rclass" : "NZBGetDRPPBHolder",
							"disableHW" : true,
							"left" : 3,
							"top" : 1,
							"height" : 16,
							"right" : 60,
							"bottom" : 2
						},
						"childTemplates" : [{
							"type" : "Ti.UI.View",
							"properties" : {
								"rclass" : "NZBGetDRPPBack",
								"backgroundColor" : "#e9e9e9",
								"borderPadding" : {
									"bottom" : -1
								},
								"borderColor" : "#E1E1E1",
								"borderRadius" : 4
							}
						}, {
							"type" : "Ti.UI.View",
							"bindId" : "progressbar",
							"properties" : {
								"rclass" : "NZBGetDRPPB",
								"borderPadding" : {
									"top" : -1,
									"left" : -1,
									"right" : -1
								},
								"left" : 0,
								"height" : "FILL",
								"borderRadius" : 4,
								"backgroundGradient" : {
									"type" : "linear",
									"tileMode" : "repeat",
									"rect" : {
										"x" : 0,
										"y" : 0,
										"width" : 40,
										"height" : 40
									},
									"colors" : [{
										"offset" : 0,
										"color" : "#26ffffff"
									}, {
										"offset" : 0.25,
										"color" : "#26ffffff"
									}, {
										"offset" : 0.25,
										"color" : "transparent"
									}, {
										"offset" : 0.5,
										"color" : "transparent"
									}, {
										"offset" : 0.5,
										"color" : "#26ffffff"
									}, {
										"offset" : 0.75,
										"color" : "#26ffffff"
									}, {
										"offset" : 0.75,
										"color" : "transparent"
									}, {
										"offset" : 1,
										"color" : "transparent"
									}],
									"startPoint" : {
										"x" : 0,
										"y" : 0
									},
									"endPoint" : {
										"x" : "100%",
										"y" : "100%"
									}
								}
							}
						}, {
							"type" : "Ti.UI.Label",
							"bindId" : "sizelabel",
							"properties" : {
								"rclass" : "NZBGetDRSize",
								"maxLines" : 1,
								"textAlign" : "center",
								"pading" : {
									"left" : 2,
									"right" : 2
								},
								"ellipsize" : 'END',
								"font" : {
									"size" : 12
								},
								"height" : "FILL",
								"width" : "FILL",
								"color" : "black"
							}
						}]
					}, {
						"type" : "Ti.UI.Label",
						"bindId" : "timelabel",
						"properties" : {
							"rclass" : "NZBGetDRTime",
							"width" : 60,
							"height" : 16,
							"textAlign" : "right",
							"right" : 5,
							"font" : {
								"size" : 12
							},
							"color" : "black"
						}
					}]
				}]
			}]
		}
	};
	var items = [];

	for (var i = 0; i < 300; i++) {
		var cat = priorities[Math.floor(Math.random() * priorities.length)];
		var priority = priorities[Math.floor(Math.random() * priorities.length)];
		items.push({
			properties: {
				// 	// height: 60
			},
			button: {
				callbackId: i,
				visible: true,
				backgroundColor: '#fbb450',
				borderColor: '#f89405',
				selectedColor: 'gray',
				title: '||'
			},
			tlabel: {
				text: names[Math.floor(Math.random() * names.length)]
			},
			priority: {
				visible:priority.length > 0,
				html: priority
			},
			sizelabel: {
				text:  (new Date()).toString()
			},
			timelabel: {
				html: '<strike>' + (new Date()).toString() + '</strike>'
			},
			category: {
				visible:cat.length > 0,
				text: cat
			},
			progressbar: {
				backgroundColor: '#fbb450',
				borderColor: '#f89405',
				width: Math.floor(Math.random() * 100) + '%'
			},
			check: {
				color: 'transparent'
			}
		});
	}
	listView.setSections([{
		items: items
	}]);
	win.add(listView);
	listView.addEventListener('click', function(_event) {
		info('click');
		if (_event.bindId && _event.hasOwnProperty('section') && _event.hasOwnProperty('itemIndex')) {
			var item = _event.section.getItemAt(_event.itemIndex);
			if (_event.bindId === 'button') {
				item.button.image = images[Math.floor(Math.random() * images.length)];
				item.properties.backgroundColor = 'blue';
				item.priority.text = 'my test';
				item.priority.backgroundColor = 'green';
				_event.section.updateItemAt(_event.itemIndex, item);
			}
		}
	});
	openWin(win);
}

function keyboardTest() {
	var textfield = Ti.UI.createTextField();
	var dialog = Ti.UI.createAlertDialog({
		title: 'test',
		buttonNames: ['cancel', 'ok'],
		persistent: true,
		cancel: 0,
		androidView: textfield
	});
	textfield.addEventListener('change', function(e) {
		textfield.blur();
	});
	dialog.addEventListener('open', function(e) {
		textfield.focus();
	});
	dialog.addEventListener('click', function(e) {
		if (e.cancel)
			return;
	});
	dialog.addEventListener('return', function(e) {});
	dialog.show();
}

function transitionTest() {
	var win = createWin();

	var holderHolder = Ti.UI.createView({
		// clipChildren:false,
		height: 100,
		borderColor: 'green',
		width: 220,
		backgroundColor: 'green'
	});
	var transitionViewHolder = Ti.UI.createView({
		clipChildren: false,
		height: 80,
		width: 200,
		// borderRadius: 10,
		// borderColor: 'green',
		backgroundColor: 'yellow'
	});
	var tr1 = Ti.UI.createLabel({
		text: 'I am a text!',
		color: '#fff',
		textAlign: 'center',
		backgroundColor: 'green',
		// borderRadius: 10,
		width: 50,
		height: 40,
	});
	tr1.addEventListener('click', function(e) {
		Ti.API.info('click');
		transitionViewHolder.transitionViews(tr1, tr2, {
			style: Ti.UI.TransitionStyle.CUBE,
			duration: 3000,
			reverse: true
		});
	});
	var tr2 = Ti.UI.createButton({
		title: 'I am a button!',
		color: '#000',
		// borderColor:'orange',
		// borderRadius: 20,
		height: 40,
		backgroundColor: 'white'
	});
	tr2.addEventListener('click', function(e) {
		transitionViewHolder.transitionViews(tr2, tr1, {
			style: Ti.UI.TransitionStyle.SWIPE_DUAL_FADE,
		});
	});
	transitionViewHolder.add(tr1);
	holderHolder.add(transitionViewHolder);
	win.add(holderHolder);
	openWin(win);
}

function opacityTest() {
	var win = createWin({
		dispatchPressed: true,
		backgroundSelectedColor: 'green'
	});

	var image1 = Ti.UI.createImageView({
		backgroundColor: 'yellow',
		image: "animation/win_1.png"
	});
	image1.addEventListener('longpress', function() {
		image1.animate({
			// opacity: 0,
			backgroundColor: 'transparent',
			autoreverse: true,
			duration: 2000,
		});
	});

	var button = Ti.UI.createButton({
		top: 0,
		padding: {
			left: 30,
			top: 30,
			bottom: 30,
			right: 30
		},
		height: 70,
		bubbleParent: false,
		backgroundColor: 'gray',
		touchPassThrough: false,
		dispatchPressed: true,
		backgroundSelectedGradient: {
			type: 'linear',
			colors: ['#333', 'transparent'],
			startPoint: {
				x: 0,
				y: 0
			},
			endPoint: {
				x: 0,
				y: "100%"
			}
		},
		title: 'test buutton'
	});
	button.add(Ti.UI.createView({
		enabled: true,
		backgroundColor: 'purple',
		backgroundSelectedColor: 'white',
		left: 10,
		width: 15,
		height: 15
	}));
	button.add(Ti.UI.createView({
		backgroundColor: 'green',
		bottom: 10,
		width: 15,
		height: 15
	}));
	button.add(Ti.UI.createView({
		backgroundColor: 'yellow',
		top: 10,
		width: 15,
		height: 15
	}));
	button.add(Ti.UI.createView({
		touchPassThrough: true,
		backgroundColor: 'orange',
		right: 0,
		width: 35,
		height: Ti.UI.FILL
	}));
	var t1 = Ti.UI.create2DMatrix();
	var t2 = Ti.UI.create2DMatrix().scale(2.0, 2.0).translate(0, 40).rotate(90);
	button.addEventListener('longpress', function(e) {
		button.animate({
			opacity: 0,
			// autoreverse: true,
			duration: 2000,
		});
	});
	win.add(button);
	var label = Ti.UI.createLabel({
		bottom: 20,
		height: 120,
		width: 170,
		// dispatchPressed: true,
		backgroundColor: 'gray',
		backgroundSelectedColor: '#a46',
		padding: {
			left: 30,
			top: 30,
			bottom: 30,
			right: 30
		},
		bubbleParent: false,
		selectedColor: 'green',
		backgroundSelectedGradient: {
			type: 'linear',
			colors: ['#333', 'transparent'],
			startPoint: {
				x: 0,
				y: 0
			},
			endPoint: {
				x: 0,
				y: "100%"
			}
		},
		text: 'This is a sample\n text for a label'
	});
	label.add(Ti.UI.createView({
		touchEnabled: false,
		backgroundColor: 'red',
		backgroundSelectedColor: 'white',
		left: 10,
		width: 15,
		height: 15
	}));
	label.add(Ti.UI.createView({
		backgroundColor: 'green',
		bottom: 10,
		width: 15,
		height: 15
	}));
	label.add(Ti.UI.createView({
		backgroundColor: 'yellow',
		top: 10,
		width: 15,
		height: 15
	}));
	label.add(Ti.UI.createView({
		backgroundColor: 'orange',
		right: 10,
		width: 15,
		height: 15
	}));
	var t3 = Ti.UI.create2DMatrix().scale(2.0, 2.0).translate(0, -40).rotate(90);
	label.addEventListener('longpress', function(e) {
		label.animate({
			opacity: 0,
			autoreverse: true,
			duration: 2000,
		});
	});
	win.add(label);
	var button2 = Ti.UI.createButton({
		padding: {
			left: 80
		},
		bubbleParent: false,
		backgroundColor: 'gray',
		dispatchPressed: true,
		selectedColor: 'red',
		backgroundSelectedGradient: {
			type: 'linear',
			colors: ['#333', 'transparent'],
			startPoint: {
				x: 0,
				y: 0
			},
			endPoint: {
				x: 0,
				y: "100%"
			}
		},
		title: 'test buutton'
	});
	button2.add(Ti.UI.createButton({
		left: 0,
		backgroundColor: 'green',
		selectedColor: 'red',
		backgroundSelectedGradient: {
			type: 'linear',
			colors: ['#333', 'transparent'],
			startPoint: {
				x: 0,
				y: 0
			},
			endPoint: {
				x: 0,
				y: "100%"
			}
		},
		title: 'Osd'
	}));
	win.add(button2);
	win.add(image1);
	openWin(win);
}

function imageViewTests() {
	var win = createWin();
	var listview = createListView();
	listview.sections = [{
		items: [{
			properties: {
				title: 'AnimationTest'
			},
			callback: imageViewAnimationTest
		}, {
			properties: {
				title: 'TransitionTest'
			},
			callback: imageViewTransitionTest
		}]
	}];
	win.add(listview);
	openWin(win);
}

function imageViewTransitionTest() {
	var win = createWin();

	var image1 = Ti.UI.createImageView({
		backgroundColor: 'yellow',
		image: "animation/win_1.png",
		backgroundGradient: {
			type: 'linear',
			colors: ['#333', 'transparent'],
			startPoint: {
				x: 0,
				y: 0
			},
			endPoint: {
				x: 0,
				y: "100%"
			}
		},
		localLoadSync: true,

		width: 100,
		transition: {
			style: Ti.UI.TransitionStyle.FLIP,
			// substyle:Ti.UI.TransitionStyle.TOP_TO_BOTTOM
		}
	});
	image1.add(Ti.UI.createView({
		enabled: false,
		backgroundColor: 'purple',
		backgroundSelectedColor: 'white',
		left: 10,
		width: 15,
		height: 15
	}));
	win.add(image1);
	image1.addEventListener('click', function() {
		image1.image = "animation/win_" + Math.floor(Math.random() * 16 + 1) + ".png";
	});
	var button = Ti.UI.createButton({
		bottom: 0,
		bubbleParent: false,
		title: 'Transition'
	});
	button.addEventListener('click', function() {
		choseTransition(image1, 'transition');
	});
	win.add(button);
	openWin(win);
}

function imageViewAnimationTest() {
	var win = createWin();

	var image1 = Ti.UI.createImageView({
		backgroundColor: 'yellow',
		width: 100,
		transition: {
			style: Ti.UI.TransitionStyle.FADE,
		},
		image: 'http://zapp.trakt.us/images/posters_movies/192263-138.jpg',
		animatedImages: ["animation/win_1.png", "animation/win_2.png", "animation/win_3.png", "animation/win_4.png",
						"animation/win_5.png", "animation/win_6.png", "animation/win_7.png", "animation/win_8.png",
						"animation/win_9.png", "animation/win_10.png", "animation/win_11.png", "animation/win_12.png",
						"animation/win_13.png", "animation/win_14.png", "animation/win_15.png", "animation/win_16.png"],
		duration: 100,
		viewMask: '/images/body-mask.png'
	});
	win.add(image1);
	var btnHolder = Ti.UI.createView({
		left: 0,
		layout: 'vertical',
		height: Ti.UI.SIZE,
		width: Ti.UI.SIZE,
		backgroundColor: 'green'
	});
	btnHolder.add([
		{
			type: 'Ti.UI.Button',
			left: 0,
			bid: 0,
			title: 'start'
		},
		{
			type: 'Ti.UI.Button',
			right: 0,
			bid: 1,
			title: 'pause'
		},
		{
			type: 'Ti.UI.Button',
			left: 0,
			bid: 2,
			title: 'resume'
		},
		{
			type: 'Ti.UI.Button',
			right: 0,
			bid: 3,
			title: 'playpause'
		},
		{
			type: 'Ti.UI.Button',
			left: 0,
			bid: 4,
			title: 'stop'
		},
		{
			type: 'Ti.UI.Button',
			right: 0,
			bid: 5,
			title: 'reverse'
		},
		{
			type: 'Ti.UI.Button',
			left: 0,
			bid: 6,
			title: 'autoreverse'
		},
		{
			type: 'Ti.UI.Button',
			right: 0,
			bid: 7,
			title: 'transition'
		}
	]);
	btnHolder.addEventListener('singletap', function(e) {
		switch (e.source.bid) {
			case 0:
				image1.start();
				break;
			case 1:
				image1.pause();
				break;
			case 2:
				image1.resume();
				break;
			case 3:
				image1.pauseOrResume();
				break;
			case 4:
				image1.stop();
				break;
			case 5:
				image1.reverse = !image1.reverse;
				break;
			case 6:
				image1.autoreverse = !image1.autoreverse;
				break;
			case 7:
				choseTransition(image1, 'transition');
				break;

		}
	});
	win.add(btnHolder);
	openWin(win);
}

function antiAliasTest() {
	var win = createWin();
	var html = '  SUCCESS     <font color="red">musique</font> électronique <b><span style="background-color:green;border-color:black;border-radius:20px;border-width:1px">est un type de </span><big><big>musique</big></big> qui a <font color="green">été conçu à</font></b> partir des années<br> 1950 avec des générateurs de signaux<br> et de sons synthétiques. Avant de pouvoir être utilisée en temps réel, elle a été primitivement enregistrée sur bande magnétique, ce qui permettait aux compositeurs de manier aisément les sons, par exemple dans l\'utilisation de boucles répétitives superposées. Ses précurseurs ont pu bénéficier de studios spécialement équipés ou faisaient partie d\'institutions musicales pré-existantes. La musique pour bande de Pierre Schaeffer, également appelée musique concrète, se distingue de ce type de musique dans la mesure où son matériau primitif était constitué des sons de la vie courante. La particularité de la musique électronique de l\'époque est de n\'utiliser que des sons générés par des appareils électroniques.';
	var view = Ti.UI.createLabel({
		backgroundColor: 'blue',
		borderWidth: 4,
		html: html,
		selectedColor: 'green',
		color: 'black',
		retina: true,
		disableHW: true,
		// borderColor: 'green',
		borderRadius: [150, 50, 0, 0],
		width: 300,
		height: 300,
		backgroundColor: 'white',
		backgroundSelectedColor: 'orange',
		backgroundInnerShadows: [{
			color: 'black',
			radius: 20
		}],
		backgroundSelectedInnerShadows: [{
			offset: {
				x: 0,
				y: 15
			},
			color: 'blue',
			radius: 20
		}],
		borderSelectedGradient: sweepGradient,
		borderColor: 'blue'
	});
	view.addEventListener('longpress', function() {
		view.animate({
			transform: Ti.UI.create2DMatrix().scale(0.3, 0.3),
			duration: 2000,
			autoreverse: true,
			curve: [0, 0, 1, -1.14]
		});
	});

	win.add(view);
	openWin(win);
}

var firstWindow = createWin({});
var listview = createListView({
	headerTitle: 'Testing Title'
	// minRowHeight:100,
	// maxRowHeight:140
});
var color = cellColor(0);
listview.sections = [{
	headerView: {
		type: 'Ti.UI.Label',
		properties: {
			backgroundColor: 'red',
			bottom: 20,
			text: 'HeaderView created from Dict'
		}
	},
	items: [{
		properties: {
			title: 'Transform',
			height: Ti.UI.FILL,
			backgroundColor: color
		},
		callback: transformExs
	}, {
		properties: {
			title: 'ImageView tests'
		},
		callback: imageViewTests
	}, {
		properties: {
			title: 'antiAliasTest',
			visible: true
		},
		callback: antiAliasTest
	}, {
		properties: {
			title: 'Opacity'
		},
		callback: opacityTest
	}, {
		properties: {
			title: 'Layout'
		},
		callback: layoutExs
	}, {
		properties: {
			title: 'listViewLayout'
		},
		callback: listViewLayout
	}, {
		properties: {
			title: 'transitionTest'
		},
		callback: transitionTest
	}, {
		properties: {
			title: 'ButtonsAndLabels'
		},
		callback: buttonAndLabelEx
	}, {
		properties: {
			title: 'Mask'
		},
		callback: maskEx
	}, {
		properties: {
			title: 'ImageView'
		},
		callback: ImageViewEx
	}, {
		properties: {
			title: 'AnimationSet'
		},
		callback: transform2Ex
	}, {
		properties: {
			title: 'HTML Label'
		},
		callback: htmlLabelEx
	}, {
		properties: {
			title: 'SVG'
		},
		callback: svgExs
	}, {
		properties: {
			title: 'ListView'
		},
		callback: listViewEx
	}, {
		properties: {
			title: 'ListView2'
		},
		callback: listView2Ex
	}, {
		properties: {
			title: 'webView'
		},
		callback: webviewTest
	}]
}];
firstWindow.add(listview);
var mainWin = Ti.UI.createNavigationWindow({
	backgroundColor: backColor,
	exitOnClose: true,
	window: firstWindow,
	// transition: {
	// 	style: Ti.UI.TransitionStyle.SLIDE
	// }
});
mainWin.addEventListener('openWindow', function(e) {
	Ti.API.info(e);
});
mainWin.addEventListener('closeWindow', function(e) {
	Ti.API.info(e);
});
mainWin.open();

function textFieldTest() {
	var win = createWin();
	win.add([{
		type: 'Ti.UI.TextField',
		bindId: 'textfield',
		properties: {
			top: 20,
			width: 'FILL',
			color: 'black',
			text: 'Border Padding',
			verticalAlign: 'bottom',
			borderWidth: 2,
			borderColor: 'black',
			borderSelectedColor: 'blue',
			backgroundColor: 'gray',
			returnKeyType: Ti.UI.RETURNKEY_NEXT,
			color: '#686868',
			font: {
				size: 14
			},
			top: 4,
			bottom: 4,
			padding: {
				left: 20,
				right: 20,
				bottom: 2,
				top: 2
			},
			verticalAlign: 'center',
			left: 4,
			width: 'FILL',
			height: 'SIZE',
			right: 4,
			textAlign: 'left',
			maxLines: 2,
			borderSelectedColor: '#74B9EF',
			height: 40,
			ellipsize: Ti.UI.TEXT_ELLIPSIZE_TAIL,
			rightButton: Ti.UI.createView({
				backgroundColor: 'yellow',
				top: 8,
				bottom: 8,
				right: 0,
				width: 40
			}),
			rightButtonMode: Ti.UI.INPUT_BUTTONMODE_ONFOCUS
		}
	}, {
		type: 'Ti.UI.TextField',
		properties: {
			top: 80,
			width: 'FILL',
			color: 'black',
			text: 'Border Padding',
			verticalAlign: 'bottom',
			borderWidth: 2,
			borderColor: 'black',
			borderSelectedColor: 'blue',
			backgroundColor: 'gray'
		}
	}]);
	win.addEventListener('click', function() {
		win.textfield.focus()
	})

	openWin(win);
}
// textFieldTest();

function test4() {
	var win = createWin({
		backgroundColor: 'orange',
		layout: 'vertical'
	});
	// var view = Ti.UI.createView({
	// 	height:0,
	// 	backgroundColor:'red'
	// });
	win.add(Ti.UI.createView({
		height: 100,
		backgroundColor: 'blue'
	}));
	var view1 = Ti.UI.createView({
		height: 'FILL',
		backgroundColor: 'yellow',
		backgroundGradient: {
			type: 'linear',
			colors: ['white', 'red'],
			startPoint: {
				x: 0,
				y: 0,
			},
			endPoint: {
				x: 0,
				y: "100%",
			}
		}
	});
	view1.add(Ti.UI.createView({
		height: 50,
		bottom: 0,
		backgroundColor: 'green'
	}));
	win.add(view1);

	var view2 = Ti.UI.createView({
		visible: false,
		height: 0,
		backgroundColor: 'purple'
	})
	var view3 = Ti.UI.createLabel({
		text: 'test',
		height: 60,
		backgroundColor: 'brown'
	})
	view2.add(view3);
	win.add(view2);

	win.addEventListener('click', function() {
		if (view2.visible) {
			view2.animate({
				height: 0,
				cancelRunningAnimations: true,
				duration: 4000
			}, function() {
				view2.visible = false;
			});
		} else {
			view2.visible = true;
			view2.animate({
				cancelRunningAnimations: true,
				height: 'SIZE',
				duration: 4000
			});
		}
	})
	openWin(win);
}

function borderPaddingEx() {
	var win = createWin({
		backgroundColor: 'white'
	});
	var view = new View({
		"type": "Ti.UI.Label",
		"bindId": "title",
		"properties": {
			"rclass": "NZBGetTVRTitle",
			text: 'downloadpath',
			"font": {
				"size": 14
			},
			"padding": {
				"left": 4,
				"right": 4
			},
			"textAlign": "right",
			"width": 110,
			"color": "black"
		}
	});

	win.add([{
		type: 'Ti.UI.View',
		properties: {
			top: 20,
			width: 100,
			height: 50,
			backgroundColor: 'yellow',
		},
		childTemplates: [{
			bindId: 'test',
			type: 'Ti.UI.View',
			properties: {
				width: '50%',
				color: 'black',
				hint: 'Border Padding',
				// borderWidth: 1,
				backgroundColor: 'green',
				borderSelectedColor: 'blue',
				// borderSelectedColor: 'blue',
				// borderSelectedGradient: {
				// 	type: 'radial',
				// 	colors: ['orange', 'yellow']
				// },
				// backgroundColor: '#282D34',
				// backgroundSelectedColor: '#3A4350',
				borderPadding: {
					top: -1,
					left: -2,
					right: -2
				},
				left: 0,
				height: 'FILL',
				borderRadius: 4,
				backgroundGradient: {
					type: 'linear',
					rect: {
						x: 0,
						y: 0,
						width: 40,
						height: 40
					},
					colors: [{
						offset: 0,
						color: '#26ffffff'
		}, {
						offset: 0.25,
						color: '#26ffffff'
		}, {
						offset: 0.25,
						color: 'transparent'
		}, {
						offset: 0.5,
						color: 'transparent'
		}, {
						offset: 0.5,
						color: '#26ffffff'
		}, {
						offset: 0.75,
						color: '#26ffffff'
		}, {
						offset: 0.75,
						color: 'transparent'
		}, {
						offset: 1,
						color: 'transparent'
		}],
					startPoint: {
						x: 0,
						y: 0
					},
					endPoint: {
						x: "100%",
						y: '100%'
					}
				}
				// backgroundSelectedInnerShadows:[{color:'black', radius:10}],
				// backgroundInnerShadows:[{color:'black', radius:10}]
			}
		}, {
			type: 'Ti.UI.View',
			properties: {
				height: 'FILL',
				color: 'white',
				width: 'SIZE',
				borderColor: '#667383',
				borderPadding: {
					right: -1,
					top: -1,
					bottom: -1
				},
			},
			childTemplates: [{
				type: 'Ti.UI.Label',
				bindId: 'test',
				properties: {
					borderWidth: 3,
					borderPadding: {
						right: -3,
						left: -3,
						top: -3
					},
					borderSelectedColor: '#047792',
					backgroundSelectedColor: '#667383',
					backgroundColor: 'gray',
					font: {
						size: 20,
						weight: 'bold'
					},
					padding: {
						left: 15,
						right: 15
					},
					color: 'white',
					disabledColor: 'white',
					height: 'FILL',
					callbackId: 'search',
					right: 0,
					text: 'Aaaa',
					clearIcon: 'X',
					icon: 'A',
					transition: {
						style: Ti.UI.TransitionStyle.FADE
					}
				}
				}]
			}],
		events: {
			'longpress': function(e) {
				info('test' + JSON.stringify(e));
				// if (e.bindId === 'test') {
				e.source.text = 'toto';
				// }
				e.source.borderColor = 'red';
			}
		}
	}]);

	// win.add({
	// "properties": {
	// "rclass": "GenericRow TVRow",
	// "layout": "horizontal",
	// "height": "SIZE"
	// },
	// "childTemplates": [{
	// "type": "Ti.UI.Label",
	// "bindId": "title",
	// "properties": {
	// "rclass": "NZBGetTVRTitle",
	// "font": {
	// "size": 14
	// },
	// "padding": {
	// "left": 4,
	// "right": 4,
	// "top": 10
	// },
	// text: 'downloadpath',
	// "textAlign": "right",
	// "width": 90,
	// "color": "black",
	// // "height": "FILL",
	// "verticalAlign": "top"
	// }
	// }, {
	// "type": "Ti.UI.Label",
	// "bindId": "value",
	// "properties": {
	// selectedColor:'green',
	// html: 'A new version is available <a href="https://github.com/RuudBurger/CouchPotatoServer/compare/b468048d95216474183daafaf46a4f2bd0d7ada7...master" target="_blank"><font color="red"><b><u>see what has changed</u></b></font></a> or <a href="update">just update, gogogo!</a>',
	// // "autoLink":Ti.UI.AUTOLINK_ALL,
	// "rclass": "NZBGetTVRValue",
	// "color": "#686868",
	// "font": {
	// "size": 14
	// },
	// // transition: {
	// // style: Ti.UI.TransitionStyle.SWIPE_FADE
	// // },
	// "top": 4,
	// "bottom": 4,
	// "padding": {
	// "left": 4,
	// "right": 4,
	// "bottom": 2,
	// "top": 2
	// },
	// "verticalAlign": "middle",
	// "left": 4,
	// "width": "FILL",
	// "height": "SIZE",
	// "right": 4,
	// "textAlign": "left",
	// "maxLines": 2,
	// "ellipsize": Ti.UI.TEXT_ELLIPSIZE_TAIL,
	// "borderColor": "#eeeeee",
	// "borderRadius": 2
	// }
	// }]
	// });
	// info(win.value.text);
	var first = true;
	// win.value.addEventListener('click',function(e){
	// info(stringify(e));
	// });
	// win.value.addEventListener('longpress',function(e){
	// info(stringify(e));
	// });

	// win.add({
	// type: 'Ti.UI.View',
	// properties: {
	// width: 200,
	// height: 20
	// },
	// events:{
	// 'click':function(e){info(stringify(e));}
	// },
	// childTemplates: [{
	// borderPadding: {
	// bottom: -1
	// },
	// borderColor: 'darkGray',
	// backgroundColor: 'gray',
	// borderRadius: 4
	// }, {
	// bindId: 'progress',
	// properties: {
	// borderPadding: {
	// top: -1,
	// left: -1,
	// right: -1
	// },
	// borderColor: '#66AC66',
	// backgroundColor: '#62C462',
	// borderRadius: 4,
	// left: 0,
	// width: '50%',
	// backgroundGradient: {
	// type: 'linear',
	// rect:{x:0, y:0, width:40, height:40},
	// colors: [{
	// offset: 0,
	// color: '#26ffffff'
	// }, {
	// offset: 0.25,
	// color: '#26ffffff'
	// }, {
	// offset: 0.25,
	// color: 'transparent'
	// }, {
	// offset: 0.5,
	// color: 'transparent'
	// }, {
	// offset: 0.5,
	// color: '#26ffffff'
	// }, {
	// offset: 0.75,
	// color: '#26ffffff'
	// }, {
	// offset: 0.75,
	// color: 'transparent'
	// }, {
	// offset: 1,
	// color: 'transparent'
	// }],
	// startPoint: {
	// x: 0,
	// y: 0
	// },
	// endPoint: {
	// x: "100%",
	// y: '100%'
	// }
	// }
	// }
	// }]
	// });

	openWin(win);
}

function testMCTS() {
	var win = createWin({
		backgroundColor: 'white'
	});

	var listview = Ti.UI.createListView({
		delaysContentTouches: false,
		templates: {
			"title": {
				"childTemplates": [{
					"properties": {
						"rclass": "BigTitleRowLabel",
						"font": {
							"size": 20,
							"weight": "bold"
						},
						"padding": {
							"left": 4,
							"top": 2,
							"bottom": 10,
							"right": 4
						},
						"color": "black"
					},
					"type": "Ti.UI.Label",
					"bindId": "label"
				}],
				"properties": {
					"height": "SIZE",
					"rclass": "GenericRow SizeHeight"
				}
			},
			"value": {
				"childTemplates": [{
					"properties": {
						"rclass": "NZBGetTVRTitle",
						"textAlign": "right",
						"width": 90,
						"font": {
							"size": 14
						},
						"padding": {
							"left": 4,
							"right": 4
						},
						"color": "black"
					},
					"type": "Ti.UI.Label",
					"bindId": "title"
				}, {
					"properties": {
						"color": "#686868",
						"padding": {
							"left": 4,
							"top": 2,
							"bottom": 2,
							"right": 4
						},
						"ellipsize": 'END',
						"maxLines": 2,
						"rclass": "NZBGetTVRValue",
						"bottom": 4,
						"font": {
							"size": 14
						},
						"verticalAlign": "middle",
						"borderColor": "#eeeeee",
						"textAlign": "left",
						"right": 4,
						"height": "SIZE",
						"borderRadius": 2,
						"left": 4,
						"width": "FILL",
						"top": 4
					},
					"type": "Ti.UI.Label",
					"bindId": "value"
				}],
				"properties": {
					"height": "SIZE",
					"layout": "horizontal",
					"rclass": "GenericRow TVRow"
				}
			},
			"button": {
				"childTemplates": [{
					// "properties" : {
					// "rclass" : "NZBGetTVRTitle",
					// "textAlign" : "right",
					// "width" : 90,
					// "font" : {
					// "size" : 14
					// },
					// "padding" : {
					// "left" : 4,
					// "right" : 4
					// },
					// "color" : "black"
					// },
					// "type" : "Ti.UI.Label",
					// "bindId" : "title"
					// }, {
					"properties": {
						"verticalAlign": "middle",
						"colors": ["#FEFEFE", "#E3E3E3"],
						"color": "#686868",
						"borderWidth": 1,
						"width": "SIZE",
						"top": 5,
						"font": {
							"size": 16
						},
						"selectedColors": ["#F1F1F1", "#E2E2E2"],
						"right": 4,
						"left": 4,
						"bottom": 5,
						"padding": {
							"left": 10,
							"top": 10,
							"bottom": 10,
							"right": 10
						},
						"bindId": "value",
						"borderColor": "#BFBFBF",
						"textAlign": "center",
						"backgroundSelectedGradient": {
							"startPoint": {
								"x": 0,
								"y": 0
							},
							"type": "linear",
							"colors": ["#F1F1F1", "#E2E2E2"],
							"endPoint": {
								"x": 0,
								"y": "100%"
							}
						},
						"backgroundGradient": {
							"startPoint": {
								"x": 0,
								"y": 0
							},
							"type": "linear",
							"colors": ["#FEFEFE", "#E3E3E3"],
							"endPoint": {
								"x": 0,
								"y": "100%"
							}
						},
						"borderRadius": 4,
						"backgroundSelectedInnerShadows": [{
							"color": "#88000000",
							"radius": 5,
							"offset": {
								"x": 0,
								"y": 5
							}
						}],
						"height": "SIZE"
					},
					"type": "Ti.UI.Label",
					"bindId": "value"
				}],
				"properties": {
					"height": "SIZE",
					"layout": "horizontal",
					"rclass": "GenericRow TVRow"
				}
			},
			"textfield": {
				"childTemplates": [{
					// "properties" : {
					// "rclass" : "NZBGetTVRTitle",
					// "textAlign" : "right",
					// "width" : 90,
					// "font" : {
					// "size" : 14
					// },
					// "padding" : {
					// "left" : 4,
					// "right" : 4
					// },
					// "color" : "black"
					// },
					// "type" : "Ti.UI.Label",
					// "bindId" : "title"
					// }, {
					"type": "Ti.UI.TextField",
					"bindId": "textfield",
					"events": {},
					"properties": {
						"color": "#686868",
						"ellipsize": 'END',
						"padding": {
							"left": 4,
							"top": 2,
							"bottom": 2,
							"right": 4
						},
						"backgroundColor": "white",
						"maxLines": 2,
						"rclass": "TVRValue NZBGetTVRTextField",
						"font": {
							"size": 14
						},
						"borderColor": "#eeeeee",
						"bottom": 4,
						"verticalAlign": "middle",
						"borderSelectedColor": "#74B9EF",
						"borderRadius": 2,
						"height": 40,
						"right": 4,
						"textAlign": "left",
						"left": 4,
						"width": "FILL",
						"top": 4
					}
				}],
				"properties": {
					"height": "SIZE",
					"layout": "horizontal",
					"rclass": "GenericRow TVRow"
				}
			}
		},
		sections: [{
			"items": [{
				"template": "textfield",
				"title": {
					"text": "name"
				},
				"textfield": {
					// "value": "The.Conjuring.2013.1080p.BluRay.DTS.x264-CyTSuNee",
					// "text" : "The.Conjuring.2013.1080p.BluRay.DTS.x264-CyTSuNee"
				}
			}, {
				"template": "button",
				"title": {
					"text": "priority"
				},
				"value": {
					"callbackId": "priority",
					"text": "normal"
				}
			}, {
				"template": "button",
				"title": {
					"text": "category"
				},
				"value": {
					"callbackId": "category",
					"text": "movies"
				}
			}, {
				"template": "button",
				"value": {
					"callbackId": "files",
					"text": "files"
				}
			}]
		}]
	});

	listview.addEventListener('click', function(_event) {
		if (_event.bindId && _event.hasOwnProperty('section') && _event.hasOwnProperty('itemIndex')) {
			var item = _event.section.getItemAt(_event.itemIndex);
			var callbackId = item[_event.bindId].callbackId;
			if (callbackId) {
				item[_event.bindId].text = 'test';
				_event.section.updateItemAt(_event.itemIndex, item);
			}
		}
	});
	win.addEventListener('click', function(_event) {
		info('click' + _event.bindId);
		if (_event.bindId !== 'textfield') {
			win.blur();
		}
	});
	win.add(listview);
	openWin(win);
}

function test3() {
	var win = createWin();
	var viewHolder = new View({
		width: '50%',
		height: '60',
		backgroundColor: 'yellow'
	});
	var test = Ti.UI.createScrollView({
		properties: {
			width: 'FILL',
			height: 'SIZE',
			layout: 'vertical'
		},
		childTemplates: [{
			properties: {
				width: 'FILL',
				height: 'SIZE',
				layout: 'horizontal'
			},
			childTemplates: [{
				type: 'Ti.UI.Label',
				properties: {
					width: 'FILL',
					text: 'test'
				}
				}, {
				properties: {
					layout: 'horizontal',
					height: 'SIZE',
					right: 5,
					top: 10,
					bottom: 10,
					width: 'FILL'
				},
				childTemplates: [{
					type: 'Ti.UI.TextField',
					bindId: 'textfield',
					properties: {
						keyboardType: Ti.UI.KEYBOARD_NUMBER_PAD,
						left: 3,
						width: 'FILL',
						hintText: 'none',
						height: 40,
						backgroundColor: 'white',
						font: {
							size: 14
						},
					}
					}, {
					type: 'Ti.UI.Label',
					properties: {
						right: 0,
						width: 'SIZE',
						verticalAlign: 'middle',
						height: 'FILL',
						padding: {
							left: 5,
							right: 5
						},
						backgroundColor: '#EEEEEE',
						text: 'KB/s',
						font: {
							size: 14
						}
					}
					}]
				}]
			}]
	});
	var visible = false;

	win.addEventListener('longpress', function() {
		if (visible) {
			viewHolder.transitionViews(test, null, {
				style: Ti.UI.TransitionStyle.FADE
			});
		} else {
			viewHolder.transitionViews(null, test, {
				style: Ti.UI.TransitionStyle.FADE
			});
		}
		visible = !visible;
	});
	// viewHolder.add(test);
	win.add(viewHolder);

	openWin(win);
}

function deepLayoutTest() {
	var win = createWin({
		dispatchPressed: true,
		layout: 'vertical'
	});
	var viewHolder = Ti.UI.createView({
		width: 'FILL',
		height: 60,
		backgroundColor: 'yellow'
	});
	var test = Ti.UI.createView({
		properties: {
			backgroundColor: 'green',
			right: 0,
			width: 'SIZE',
			height: 'FILL',
			layout: 'horizontal'
		},
		childTemplates: [{
			type: 'Ti.UI.View',
			properties: {
				height: 'FILL',
				width: 'SIZE',
				borderColor:'#667383',
				borderPadding:{right:-1,top:-1,bottom:-1},
			},
			childTemplates: [{
				type: 'Ti.UI.TextField',
				bindId: 'searchField',
				properties: {
					rclass: 'CPSearchField',
					color: 'black',
					hintColor: 'gray',
					right: 0,
					height: 'FILL',
					visible: false,
					backgroundColor: 'white',
					borderWidth:3,
					borderPadding:{right:-3,left:-3,top:-3},
					borderColor:'red',
					borderSelectedColor:'#04BCE6',
					width: 'FILL',
					hintText: 'cp.searchfieldHint',
					padding: {
						left: 5,
						right: 5
					},
				}
			}, {
				type: 'Ti.UI.Label',
				bindId: 'search',
				properties: {
					callbackId: 'search',
					borderWidth:3,
					borderPadding:{right:-3,left:-3,top:-3},
					borderSelectedColor:'#047792',
					backgroundSelectedColor: '#667383',
					backgroundColor: 'gray',
					font: {
						size: 20,
						weight: 'bold'
					},
					padding: {
						left: 15,
						right: 15
					},
					color: 'white',
					disabledColor: 'white',
					height: 'FILL',
					callbackId: 'search',
					right: 0,
					text: 'Aaaa',
					clearIcon: 'X',
					icon: 'A',
					transition: {
						style: Ti.UI.TransitionStyle.FADE
					}
				}
			}]
		}]
	});

	test.addEventListener('click', function(e) {
		if (e.source.callbackId === 'search') {
			if (test.searchField.visible) {
				var searchField = test.searchField;
				searchField.value = '';
				searchField.animate({
					width: 1,
					opacity: 0,
					duration: 200
				}, function() {
					searchField.visible = false;
				});
				searchField.blur();
				searchField.fireEvent('hidding');
			} else {
				var searchField = test.searchField;
				searchField.applyProperties({
					value: null,
					opacity: 0,
					width: 1,
					visible: true
				});
				searchField.animate({
					width: 'FILL',
					opacity: 1,
					duration: 300
				}, function() {
					searchField.focus();
				});
				searchField.fireEvent('showing');
			}
		}
	});

	viewHolder.add(test);
	var headerView = new Label({
		properties: {
			color: 'gray',
			font: {
				size: 12
			},
			backgroundColor: 'green',
			width: 'FILL',
			height: 22,
			// borderPadding:{left:-1,right:-1,top:-1},
			padding: {
				left: 40,
				top: 2,
				bottom: 2
			},
			text: 'test'
		},
		childTemplates: [{
			type: 'Ti.UI.Label',
			properties: {
				color: 'white',
				backgroundColor: '#3A87AD',
				font: {
					size: 12
				},
				left: 10,
				height: 16,
				// borderRadius:8,
				clipChildren: false,
				verticalAlign: 'center',
				padding: {
					left: 8,
					right: 8,
					top: -2
				},
				text: '2'
			}
		}, {
			type: 'Ti.UI.Switch',
			properties: {
				right: 0,
				value: false
			},
			events: {
				'change': function(e) {
					listView.sections[1].visible = e.value;
				}
			}
			}]
	});
	var section = Ti.UI.createListSection({
		headerView: headerView,
		visible: false,
		items: [{
			title: 'test1'
		}, {
			title: 'test2'
		}]
	});

	function createSoonRow(_number) {
		var template = redux.fn.style('ListItem', {
			properties: {
				layout: 'horizontal',
				horizontalWrap: true,
				height: 'SIZE'
			}
		});
		var childTemplates = [];
		var defProps = ak.ti.style({
			type: 'Ti.UI.Label',
			properties: {
				font: {
					size: 15,
					weight: 'normal'
				},
				padding: {
					left: 4,
					top: 4,
					right: 4
				},
				verticalAlign: 'top',
				width: 80,
				height: 120,
				visible: false,
				dispatchPressed: true,
				backgroundColor: '#55000000',
				color: 'white'
			},
			childTemplates: [{
				type: 'Ti.UI.ImageView',
				properties: {
					width: 'FILL',
					height: 'FILL',
					dispatchPressed: true,
					transition: {
						style: Ti.UI.TransitionStyle.FADE
					},
					scaleType: Ti.UI.SCALE_TYPE_ASPECT_FILL
				},
				childTemplates: [{
					type: 'Ti.UI.Label',
					properties: {
						width: 'FILL',
						font: {
							size: 15,
							weight: 'normal'
						},
						padding: {
							left: 4,
							top: 4,
							right: 4
						},
						verticalAlign: 'top',
						height: 'FILL',
						touchPassThrough: false,
						backgroundSelectedColor: '#99000000',
						color: 'transparent',
						selectedColor: 'white',
					}
					}]
				}]
		});
		for (var i = 0; i < _number; i++) {
			var props = redux.fn.clone(defProps);
			props.properties.imageId = i;
			props.bindId = 'soonBottomLabel' + i;
			props.childTemplates[0].bindId = 'soonImage' + i;
			props.childTemplates[0].childTemplates[0].bindId = 'soonLabel' + i;
			childTemplates.push(props);
		}
		template.childTemplates = childTemplates;
		return template;
	};
	var editMode = false;
	var listView = createListView({
		height: 'FILL',
		backgroundSelectedColor: 'blue',
		templates: {
			"textfield": {
				"childTemplates": [{
					type: 'Ti.UI.View',
					properties: {
						left: 0,
						width: 50,
						height: 'FILL',
					},
					childTemplates: [{
						"type": "Ti.UI.Label",
						"bindId": "check",
						properties: {
							visible: false,
							disableHW: true,
							borderRadius: 4,
							backgroundColor: '#2C333A',
							backgroundSelectedColor: '#414C59',
							backgroundInnerShadows: [{
								radius: 10,
								color: '#1a1e22'
							}],
							backgroundSelectedInnerShadows: [{
								radius: 10,
								color: '#252A31'
							}],
							color: 'transparent',
							textAlign: 'center',
							clipChildren: false,
							font: {
								size: 12
							},
							text: 's',
							width: 20,
							height: 20
						}
					}]
				}, {
					"type": "Ti.UI.TextField",
					"bindId": "textfield",
					"events": {},
					"properties": {
						"color": "#686868",
						"ellipsize": 'END',
						"padding": {
							"left": 4,
							"top": 2,
							"bottom": 2,
							"right": 4
						},
						"backgroundColor": "white",
						"maxLines": 2,
						"font": {
							"size": 14
						},
						"borderColor": "#eeeeee",
						"bottom": 4,
						"verticalAlign": "middle",
						"borderSelectedColor": "#74B9EF",
						"borderRadius": 2,
						"height": 40,
						"right": 4,
						"textAlign": "left",
						"left": 4,
						"width": "FILL",
						"top": 4
					}
				}],
				"properties": {
					"height": 60,
					"layout": "horizontal",
					backgroundColor: 'white',
					"rclass": "GenericRow TVRow"
				}
			},
			soonRow: createSoonRow(10),
			"release": {
				"childTemplates": [{
					"properties": {
						"rclass": "CPMovieReleaseRowProvider",
						"left": 3,
						"width": "FILL",
						"font": {
							"size": 11
						},
						"color": "#B9B9BA",
						"height": 15
					},
					"type": "Ti.UI.Label",
					"bindId": "provider"
				}, {
					"childTemplates": [{
						"properties": {
							"left": 3,
							"rclass": "CPMovieReleaseRowTitle",
							"color": "#ffffff",
							"verticalAlign": "top",
							"width": "FILL",
							"ellipsize": 'END',
							"height": "FILL",
							"font": {
								"size": 12
							}
						},
						"type": "Ti.UI.Label",
						"bindId": "title"
					}],
					"type": "Ti.UI.View",
					"properties": {
						"width": "FILL",
						"layout": "horizontal",
						"rclass": "Fill HHolder",
						"height": "FILL"
					}
				}, {
					"childTemplates": [{
						"childTemplates": [{
							"childTemplates": [{
								"type": "Ti.UI.Label",
								"properties": {
									"text": "",
									"left": 3,
									"rid": "cpSizeIcon",
									"rclass": "CPMovieReleaseRowIcon",
									"color": "#ffffff",
									"width": 12,
									"height": "FILL",
									"font": {
										"family": "webhostinghub",
										"size": 11
									}
								}
							}, {
								"bindId": "size",
								"type": "Ti.UI.Label",
								"properties": {
									"rclass": "CPMovieReleaseRowInfos",
									"font": {
										"size": 11
									},
									"color": "#B9B9BA",
									"height": "FILL"
								}
							}],
							"type": "Ti.UI.View",
							"properties": {
								"width": "SIZE",
								"layout": "horizontal",
								"rclass": "FillHeight SizeWidth HHolder",
								"height": "FILL"
							}
						}, {
							"childTemplates": [{
								"type": "Ti.UI.Label",
								"properties": {
									"text": "",
									"left": 3,
									"rid": "cpAgeIcon",
									"rclass": "CPMovieReleaseRowIcon",
									"color": "#ffffff",
									"width": 12,
									"height": "FILL",
									"font": {
										"family": "webhostinghub",
										"size": 11
									}
								}
							}, {
								"bindId": "age",
								"type": "Ti.UI.Label",
								"properties": {
									"rclass": "CPMovieReleaseRowInfos",
									"font": {
										"size": 11
									},
									"color": "#B9B9BA",
									"height": "FILL"
								}
							}],
							"type": "Ti.UI.View",
							"properties": {
								"width": "SIZE",
								"layout": "horizontal",
								"rclass": "FillHeight SizeWidth HHolder",
								"height": "FILL"
							}
						}, {
							"childTemplates": [{
								"type": "Ti.UI.Label",
								"properties": {
									"text": "",
									"left": 3,
									"rid": "cpScoreIcon",
									"rclass": "CPMovieReleaseRowIcon",
									"color": "#ffffff",
									"width": 12,
									"height": "FILL",
									"font": {
										"family": "webhostinghub",
										"size": 11
									}
								}
							}, {
								"bindId": "score",
								"type": "Ti.UI.Label",
								"properties": {
									"rclass": "CPMovieReleaseRowInfos",
									"font": {
										"size": 11
									},
									"color": "#B9B9BA",
									"height": "FILL"
								}
							}],
							"type": "Ti.UI.View",
							"properties": {
								"width": "SIZE",
								"layout": "horizontal",
								"rclass": "FillHeight SizeWidth HHolder",
								"height": "FILL"
							}
						}],
						"type": "Ti.UI.View",
					"bindId": "iconsHolder",
						"properties": {
							"layout": "horizontal",
							"rclass": "CPMovieReleaseRowInfosHolder"
						}
					}, {
						"type": "Ti.UI.View",
						"bindId": "statusHolder",
						"childTemplates": [{
							"type": "Ti.UI.View",
							"properties": {
								"width": "FILL",
								"height": "FILL",
								"rclass": "Fill"
							}
						}, {
							"bindId": "status0",
							"type": "Ti.UI.Label",
							"properties": {
								"visible": false,
								"backgroundColor": "#5082BC",
								"padding": {
									"left": 3,
									"right": 3
								},
								"textAlign": "center",
								"clipChildren": false,
								"rclass": "CPMRStatus",
								"color": "#ffffff",
								"right": 3,
								"borderRadius": 2,
								"imageId": 0,
								"font": {
									"size": 11
								}
							}
						}],
						"properties": {
							"width": "FILL",
							"rclass": "CPMRReleaseBottomLine",
							"layout": "horizontal",
							"visible": true,
							"height": 20
						}
					}],
					"type": "Ti.UI.View",
					"properties": {
						"rclass": "CPMovieReleaseBottomLine",
						"layout": "horizontal",
						"width": "FILL",
						"height": 20
					}
				}],
				"properties": {
					"borderColor": "#667383",
					"backgroundSelectedColor": "#110000",
					"layout": "vertical",
					"backgroundColor": "#77000000",
					"borderPadding": {
						"left": -1,
						"top": -1,
						"right": -1
					},
					"rclass": "CPMovieReleaseRow",
					"height": 62
				}
			}
		},
		sections: [{
				items: [{
					template: 'textfield',
					check: {
						visible: editMode
					},
					textfield: {
						value: ''
					}
				}, {
					template: 'textfield',
					textfield: {
						value: ''
					}
				}, {
					template: 'soonRow',
					soonBottomLabel0: {
						visible:true,
						text:'test'
					},
					soonLabel0: {
						text:'test'
					},
					soonImage0: {
						image:'http://zapp.trakt.us/images/posters_movies/192263-138.jpg'
					}
				}, {
					template:'release',
					title: {
						text: 'test release'
					},
					iconsHolder: {
						visible: false
					}
				}]
			},
			section]
	});

	var label = Ti.UI.createLabel({
		color: '#F2F3F3',
		disabledColor: '#F2F3F3',
		width: 'FILL',
		touchPassThrough:true,
		borderWidth:3,
		text: 'test',
		borderPadding:{right:-3,top:-3,bottom:-3},
		borderDisabledColor: 'red',
		borderSelectedColor: 'blue',
		padding: {
			left: 15,
			right: 15
		},
		font: {
			size: 20
		}
	});
	win.add(label);
	win.add(listView);
	win.add(viewHolder);
	// win.addEventListener('click', function(e){
	// // win.blur()
	// // label.enabled = !label.enabled;
	// });
	openWin(win);
}