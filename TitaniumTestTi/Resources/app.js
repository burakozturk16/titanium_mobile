
__ANDROID__ = false;
__APPLE__ = true;
var backColor = 'white';
var textColor = 'black';
var navGroup;
var openWinArgs;
var html =
	'  SUCCESS  <big><font face="mapme">' + String.fromCharCode(0xe6b0) +
	'</font></big>  <font color="red">musique</font> électronique <b><span style="background-color:green;border-color:black;border-radius:20px;border-width:1px">est un type de </span><big><big>musique</big></big> qui a <font color="green">été <a href="test">conçu</a> à</font></b> partir des années<br> 1950 avec des générateurs de signaux<br> et de sons synthétiques. Avant de pouvoir être utilisée en temps réel, elle a été primitivement enregistrée sur bande magnétique, ce qui permettait aux compositeurs de manier aisément les sons, par exemple dans l\'utilisation de boucles répétitives superposées. Ses précurseurs ont pu bénéficier de studios spécialement équipés ou faisaient partie d\'institutions musicales pré-existantes. La musique pour bande de Pierre Schaeffer, également appelée musique concrète, se distingue de ce type de musique dans la mesure où son matériau primitif était constitué des sons de la vie courante. La particularité de la musique électronique de l\'époque est de n\'utiliser que des sons générés par des appareils électroniques.';
// html = '<span
// style="border-style:solid;background-color:green;border-color:red;border-radius:20px;border-width:3px;padding-top:3px;padding-bottom:3px;line-height:2em;">
// SUCCESS </span><br><span
// style="border-style:solid;background-color:green;border-color:red;border-radius:20px;border-width:3px;padding-top:0px;padding-bottom:0px;line-height:1em;">
// SUCCESS </span>'
if (__ANDROID__) {
	backColor = 'black';
	textColor = 'gray';
}
sdebug('__APPLE__', __APPLE__);
sdebug('__ANDROID__', __ANDROID__);
sdebug('isiOS7', isiOS7);

function merge_options(obj1, obj2, _new) {
	if (_new === true) {
		return _.assign(_.clone(obj1), obj2);
	}
	return _.assign(obj1, obj2);
}
var initWindowArgs = {
	// backgroundColor: backColor,
	orientationModes: [Ti.UI.UPSIDE_PORTRAIT, Ti.UI.PORTRAIT,
		Ti.UI.LANDSCAPE_RIGHT, Ti.UI.LANDSCAPE_LEFT
	]
};
if (isiOS7) {
	// initWindowArgs = merge_options(initWindowArgs, {
	// autoAdjustScrollViewInsets: true,
	// extendEdges: [Ti.UI.EXTEND_EDGE_ALL],
	// translucent: true
	// });
} else if (__ANDROID__) {
	initWindowArgs = merge_options(initWindowArgs, {
		top: Ti.App.defaultBarHeight
	});
}

function createWin(_args, _customTitleView) {
	_args = _args || {};
	if (__ANDROID__) {
		var buttons = [];
		_.each(['rightNavButton', 'rightNavButtons'], function(prop, index, list) {
			if (_args[prop]) {
				var data = _args[prop];
				buttons = _.union(buttons, _.isArray(data) ? data : [data]);
				delete _args[prop];
			}
		});
		if (buttons.length > 0) {
			_args.activity = _args.activity || {};
			_args.activity.onCreateOptionsMenu = function(e) {
				var menu = e.menu;
				_.each(buttons, function(view, index, list) {
					var args = {
						actionView: view,
						showAsAction: Ti.Android.SHOW_AS_ACTION_IF_ROOM
					};
					menu.add(args);
				});
			};
		}
	}
	if (!!_customTitleView) {
		var titleView = {
			type: 'Ti.UI.Label',
			properties: {
				backgroundColor: '#44ffff00',
				// left:0,
				color: 'black',
				textAlign: 'center',
				width: 'SIZE',
				height: 'FILL',
				text: _args.title
			}
		};
		if (_args.properties) {
			if (!_args.properties.titleView) {
				_args.properties.titleView = titleView;
			}
		} else if (!_args.titleView) {
			_args.titleView = titleView;
		}
	}

	return Ti.UI.createWindow(merge_options(initWindowArgs, _args, true));
}

function listViewClickHandle(_event) {
	if (_event.hasOwnProperty('item')) {
		var item = _event.item;
		sinfo('itemclick ', _event.itemIndex, item);
		if (item.callback) {
			item.callback(_.omit(item.properties, 'height', 'backgroundColor'));
		}
	}
}

function createListView(_args, _addEvents) {
	var realArgs = merge_options({
		allowsSelection: false,
		unHighlightOnSelect: false,
		rowHeight: 50,
		// headerView:__ANDROID__?{
		// type:'Ti.UI.View',
		// properties:
		// {
		// height:50
		// }
		// }:undefined,
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
	if (_addEvents !== false) {
		listview.addEventListener('itemclick', listViewClickHandle);
	}

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
	// if (__ANDROID__) {
	// if (_withoutActionBar !== true)
	// _win.activity = androidActivitysSettings;
	// }
	mainWin.openWindow(_win);
	// _win.open();
}

function transformExs() {
	var win = createWin({
		swipeToClose: true,

		title: "transform"
	});
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

function transform1Ex(_args) {
	var win = createWin(_args, true);
	var button = Ti.UI.createButton({
		top: 50,
		bubbleParent: false,
		title: 'test buutton'
	});
	var t1 = '';
	info(t1.toString());
	var t2 = 'os2t40,100%r90';
	info(t2.toString());
	button.addEventListener('longpress', function(e) {
		button.animate({
			duration: 500,
			color: varSwitch(button.color, 'red', 'black'),
			transform: varSwitch(button.transform, t2, t1),
		});
	});
	button.addEventListener('singletap', keyboardTest);
	win.add(button);
	var label = Ti.UI.createLabel({
		bottom: 20,
		backgroundColor: 'gray',
		// backgroundSelectedColor: '#ddd',
		bubbleParent: false,
		text: 'This is a sample\n text for a label'
	});
	var t3 = 's2t0,-40r90';
	label.addEventListener('longpress', function(e) {
		label.animate({
			duration: 5000,
			from: {
				color: 'red',
				backgroundColor: 'gray',
				transform: t1
			},
			to: {
				color: 'green',
				backgroundColor: 'blue',
				transform: t3
			}
		});
	});
	win.add(label);
	openWin(win);
}

function transform2Ex(_args) {
	var gone = false;
	var win = createWin(_args);
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
	var t5 = Ti.UI.create2DMatrix().translate(0, ((Math.sqrt(2)) * 100))
		.rotate(180);
	var view = Ti.UI.createView({
		transform: t0,
		borderRadius: 20,
		borderColor: 'orange',
		borderWidth: 2,
		// backgroundPadding: {
		// left: 10,
		// right: 10,
		// bottom: -5
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
		} else
			view.animate(animToRun);
	}
	win.addEventListener('click', onclick);
	win.add(view);
	openWin(win);
}

function transform3Ex(_args) {
	var win = createWin(_args);
	var t = 's0.3';
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
		view.animate({
			duration: 200,
			transform: '',
			// autoreverse: true,
			opacity: 1,
			curve: [0.17, 0.67, 0.86, 1.57]
		});
	};
	var hideMe = function(_callback) {
		view.animate({
			duration: 200,
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
		if (view.opacity === 0)
			showMe();
		else
			hideMe();
	});
	win.add(button);
	openWin(win);
}

function transform4Ex(_args) {
	var win = createWin(_args);
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
		if (view.opacity === 1)
			hideMe();
		else
			showMe();
	});
	win.add(button);
	openWin(win);
}

function transform5Ex(_args) {
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
	var win = createWin(_args);
	var t0 = Ti.UI.create2DMatrix();
	var t1 = t0.translate(0, "100%");
	var myTemplate = {
		childTemplates: [{
			type: 'Ti.UI.View',
			bindId: 'holder',
			properties: {
				width: Ti.UI.FILL,
				height: Ti.UI.FILL,
				// touchEnabled : false,
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
						if (_event.hasOwnProperty('section') && _event
							.hasOwnProperty('itemIndex')) {
							hideMenu();
							var item = _event.section
								.getItemAt(_event.itemIndex);
							item.menu = {
								transform: t0,
								opacity: 1
							};
							showItemIndex = _event.itemIndex;
							showItemSection = _event.section;
							_event.section.updateItemAt(
								_event.itemIndex, item);
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

function transform6Ex(_args) {
	var win = createWin(_args);
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
		childTemplates: [createBtn('topright'), createBtn('bottomright'),
			createBtn('bottomleft'), createBtn('topleft'),
			createBtn('center'),
		]
	});
	win.addEventListener('click', function(e) {
		if (e.source.bid !== undefined) {
			info(e.source.bid);
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

function layoutExs(_args) {
	var win = createWin(_args);
	var listview = createListView();
	listview.sections = [{
		items: [{
			properties: {
				title: 'Animated Horizontal'
			},
			callback: layout1Ex
		}, {
			properties: {
				title: 'Horizontal size'
			},
			callback: layout2Ex
		}, {
			properties: {
				title: 'Match & Weight'
			},
			callback: matchWeightEx
		}]
	}];
	win.add(listview);
	openWin(win);
}

function layout1Ex(_args) {
	var win = createWin(_args);
	var view = Ti.UI.createView({
		backgroundColor: 'green',
		width: 200,
		top: 0,
		height: 300,
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
		top: 20,
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
		maxHeight: 100,
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
			bottom: 4,
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
	win.add({
		type: 'Ti.UI.View',
		properties: {
			backgroundColor: 'yellow',
			width: 200,
			bottom: 0,
			height: Ti.UI.SIZE,
			layout: 'horizontal',
			horizontalWrap: true
		},
		childTemplates: [{
			type: 'Ti.UI.View',
			properties: {
				backgroundColor: 'red',
				width: 60,
				height: 80,
				left: 0
			}
		}, {
			type: 'Ti.UI.View',
			properties: {
				backgroundColor: 'blue',
				width: 20,
				borderColor: 'red',
				borderWidth: 2,
				borderRadius: [2, 10, 0, 20],
				// top:10,
				height: 80,
				left: 10,
				right: 4
			}
		}, {
			type: 'Ti.UI.View',
			properties: {
				backgroundColor: 'purple',
				width: Ti.UI.FILL,
				height: 100,
				bottom: 4,
				right: 4
			}
		}, {
			type: 'Ti.UI.View',
			properties: {
				backgroundColor: 'orange',
				width: 10,
				height: 50,
				maxHeight: 100,
				bottom: 6,
				right: 4
			}
		}]

	});
	win.addEventListener('click', function(e) {
		view2.animate({
			cancelRunningAnimations: true,
			// restartFromBeginning:true,
			duration: 3000,
			autoreverse: true,
			layoutFullscreen: !view2.layoutFullscreen
				// repeat: 4,
				// width: Ti.UI.FILL,
				// height: 100,
				// top: null,
				// left: 0,
				// right: 30
		});
	});
	openWin(win);
}

function layout2Ex(_args) {
	var win = createWin(_args);
	win.add({
		type: 'Ti.UI.View',
		properties: {
			backgroundColor: 'blue',
			right: 0,
			top: 100,
			width: 'SIZE',
			height: 44,
		},
		childTemplates: [{
			"type": "Ti.UI.View",
			"properties": {
				"height": "FILL",
				"layout": "horizontal",
				"backgroundColor": "red",
				"width": "SIZE"
			},
			"childTemplates": [{
				"type": "Ti.UI.View",
				"properties": {
					"rclass": "CPNBButton",
					"height": "FILL",
					"color": "#ffffff",
					"width": "SIZE",
					"backgroundColor": "yellow",
					"borderColor": "#667383",
					"borderPadding": {
						"right": -1,
						"top": -1,
						"bottom": -1
					}
				},
				"childTemplates": [{
					"type": "Ti.UI.TextField",
					"bindId": "searchField",
					"properties": {
						"callbackId": "searchSearchField",
						"rclass": "CPSearchField",
						"color": "#ffffff",
						"hintColor": "#B9B9BA",
						"right": 0,
						"height": "FILL",
						"visible": false,
						"backgroundColor": "#4E5969",
						"borderColor": "#04BCE6",
						"borderSelectedColor": "#04BCE6",
						"width": "FILL",
						"hintText": "search & add a movie",
						"padding": {
							"left": 5,
							"right": 5
						},
						"borderWidth": 3,
						"borderPadding": {
							"right": -3,
							"left": -3,
							"top": -3
						},
						"autocapitalization": 0,
						"autocorrect": false
					}
				}, {
					"bindId": "search",
					"type": "Ti.UI.Label",
					"properties": {
						"callbackId": "search",
						"rid": "cpsearchIcon",
						"right": 0,
						"text": "¶",
						"clearIcon": "Ç",
						"icon": "¶",
						"transition": {
							"style": 17
						},
						"backgroundSelectedColor": "#4E5969",
						"font": {
							"family": "elusive",
							"size": 20
						},
						"padding": {
							"left": 15,
							"right": 15
						},
						"color": "#ffffff",
						"disabledColor": "#B9B9BA",
						"height": "FILL",
						"borderWidth": 3,
						"borderPadding": {
							"right": -3,
							"left": -3,
							"top": -3
						},
						"borderSelectedColor": "#047792"
					}
				}]
			}, {
				"type": "Ti.UI.View",
				"properties": {
					"rclass": "CPNBButton",
					"height": "FILL",
					"color": "#ffffff",
					"width": "SIZE",
					"backgroundColor": "yellow",
					"borderColor": "#667383",
					"borderPadding": {
						"right": -1,
						"top": -1,
						"bottom": -1
					}
				},
				"childTemplates": [{
					"bindId": "notifs",
					"type": "Ti.UI.Label",
					"properties": {
						"callbackId": "notifs",
						"rid": "cpnotifsIcon",
						"text": "e",
						"backgroundSelectedColor": "#4E5969",
						"font": {
							"family": "elusive",
							"size": 20
						},
						"padding": {
							"left": 15,
							"right": 15
						},
						"color": "#ffffff",
						"disabledColor": "#B9B9BA",
						"height": "FILL",
						"borderWidth": 3,
						"borderPadding": {
							"right": -3,
							"left": -3,
							"top": -3
						},
						"borderSelectedColor": "#047792"
					}
				}, {
					"type": "Ti.UI.Label",
					"bindId": "notifsBadge",
					"properties": {
						"rclass": "CPNotifBadge",
						"text": 8,
						"top": 0,
						"right": 0,
						"font": {
							"size": 12
						},
						"color": "#ffffff",
						"padding": {
							"left": 4,
							"right": 3,
							"top": 2,
							"bottom": 2
						},
						"backgroundColor": "#5B9BD1"
					}
				}]
			}]
		}]
	});
	var buttons = win;
	var searchbutton = win.search || win.filter;

	win.showSearchField = function() {
		if (buttons.searchField.visible)
			return;
		var searchField = buttons.searchField;
		searchField.applyProperties({
			value: null,
			//			opacity: 0,
			width: 1,
			visible: true
		});
		searchField.animate({
			width: 'FILL',
			//			opacity: 1,
			duration: 3000
		}, function() {
			searchField.focus();
		});
		searchField.fireEvent('showing');
	};

	win.blurSearchField = function() {
		buttons.searchField.blur();
	}

	win.hideSearchField = function() {
		if (buttons.searchField.visible === false)
			return;
		var searchField = buttons.searchField;
		searchbutton.text = searchbutton.icon;
		searchField.value = '';
		searchField.animate({
			width: 1,
			//			opacity: 0,
			duration: 3000
		}, function() {
			searchField.visible = false;
		});
		searchField.blur();
		searchField.fireEvent('hidding');
	};

	win.showHideSearchField = function() {
		if (buttons.searchField.visible) {
			win.hideSearchField();
		} else {
			win.showSearchField();
		}
	};
	win.searchField.addEventListener('change', function(e) {
		if (e.value.length > 0) {
			searchbutton.text = searchbutton.clearIcon;
		} else {
			searchbutton.text = searchbutton.icon;
		}
	});
	var onButtonClick = function(e) {
		if (e.source.callbackId === 'search') {
			win.showHideSearchField();
		}
	};
	openWin(win);
}

function matchWeightEx(_args) {
	var win = createWin(_args);

	win.add({
		// type:'Ti.UI.View',
		// properties:{
		// width:200,
		// backgroundColor:'red',
		// height:'MATCH'
		// }

		type: 'Ti.UI.View',
		properties: {
			width: 'FILL',
			height: 'FILL',
			layout: 'vertical',
			backgroundColor: 'yellow'
		},
		childTemplates: [{
			type: 'Ti.UI.View',
			properties: {
				width: '44%',
				left: '2%',
				height: 40,
				backgroundColor: 'red'
			}
		}, {
			type: 'Ti.UI.View',
			properties: {
				width: '44%',
				left: '2%',
				right: '2%',
				height: 40,
				backgroundColor: 'blue'
			}
		}, {
			type: 'Ti.UI.View',
			properties: {
				backgroundColor: 'white',
				top: 10,
				bottom: 10,
				weight: 10,
				layout: 'vertical',
				height: 'FILL',
				maxWidth: '85%',
				width: 'SIZE',
			},
			childTemplates: [{
				type: 'Ti.UI.ImageView',
				properties: {
					squared: true,
					width: 'FILL',
					height: 'FILL',
					backgroundColor: 'brown'
				}
			}, {
				type: 'Ti.UI.View',
				properties: {
					width: 40,
					right: 0,
					height: 20,
					backgroundColor: 'pink'
				}
			}]
		}, {
			type: 'Ti.UI.View',
			properties: {
				width: '44%',
				left: '2%',
				bottom: 5,
				right: '2%',
				height: 'FILL',
				backgroundColor: 'purple'
			}
		}, {
			type: 'Ti.UI.View',
			properties: {
				width: '44%',
				left: '2%',
				bottom: 5,
				right: '2%',
				height: 'FILL',
				backgroundColor: 'purple'
			}
		}]
	});

	openWin(win);
	win = null;
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
		height: 130,
		// disableHW : true,
		bubbleParent: false,
		borderRadius: 10,
		borderColor: 'red',
		// backgroundColor : 'gray',
		// touchEnabled : false,
		// backgroundSelectedGradient : {
		// type : 'linear',
		// colors : [ '#333', 'transparent' ],
		// startPoint : {
		// x : 0,
		// y : 0
		// },
		// endPoint : {
		// x : 0,
		// y : "100%"
		// }
		// },
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

	var t3 = Ti.UI.create2DMatrix().scale(2.0, 2.0).translate(0, -40)
		.rotate(90);
	win.add({
		type: 'Ti.UI.Label',
		properties: {
			textAlign: 'center',
			backgroundColor: 'red',
			verticalAlign: 'bottom',
			font: {
				weight: 'bold',
				size: 16
			},
			bottom: 20,
			height: 34,
			width: 140,
			borderRadius: 2,
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
			verticalAlign: 'bottom',
			text: 'This is a sample\n text for a label'
		},
		childTemplates: [{
			touchEnabled: false,
			backgroundColor: 'red',
			backgroundSelectedColor: 'white',
			left: 10,
			width: 15,
			height: 15
		}, {
			backgroundColor: 'green',
			bottom: 10,
			width: 15,
			height: 15
		}, {
			backgroundColor: 'yellow',
			top: 10,
			width: 15,
			height: 15
		}, {
			backgroundColor: 'orange',
			right: 10,
			width: 15,
			height: 15
		}],
		events: {
			'longpress': function(e) {
				e.source.animate({
					duration: 5000,
					// width:'FILL',
					// height:'FILL',
					// bottom:0,
					autoreverse: true,
					layoutFullscreen: !e.source.layoutFullscreen
						// transform: varSwitch(label.transform, t3, t1),
				});
			},
			'swipe': function(e) {
				e.source.backgroundColor = 'green';
			}
		}
	});
	win.add({
		type: 'Ti.UI.Button',
		properties: {
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
		},
		childTemplates: [{
			type: 'Ti.UI.Button',
			properties: {
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
			}
		}]

	});

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
	win.add({
		type: 'Ti.UI.ScrollView',
		properties: {
			layout: 'vertical',
			backgroundColor: 'green',
			width: 'FILL',
			height: 'FILL',
		},
		childTemplates: [{
			type: 'Ti.UI.ImageView',
			properties: {
				defaultImage: '/images/poster.jpg',
				width: 'FILL',
				backgroundColor: 'red',
				scaleType: Ti.UI.SCALE_TYPE_ASPECT_FIT,
				top: -20,
				height: 'SIZE',
				httpOptions: {
					headers: {
						'X-Api-Key': 'c0a8929c4f1443e48f8d939d9084df17',
						'Accept': '*/*',
						'Connection': 'keep-alive',
						'Content-Type': 'application/xml'
					},
					method: 'GET',
					autoRedirect: true
				},
				image: 'http://192.168.1.12:2108/api/MediaCover/96/poster.jpg'
			}
		}]
	});
	// var view = Ti.UI.createImageView({
	// width:'FILL',
	// backgroundColor:'red',
	// scaleType:Ti.UI.SCALE_TYPE_ASPECT_FILL,
	// top:-20,
	// height:'SIZE',
	// image:'/images/login_logo.png'
	// });
	// view.add(Ti.UI.createView({
	// backgroundColor: 'yellow',
	// top: 10,
	// width: 15,
	// height: 15
	// }));
	// view.addEventListener('click', function() {
	// // view.image = varSwitch(view.image, '/images/slightlylargerimage.png',
	// '/images/poster.jpg');
	// view.animate({
	// width: 'FILL',
	// height: 'FILL',
	// duration: 1000,
	// autoreverse: true
	// });
	// });
	// win.add(view);
	openWin(win);
}

function random(min, max) {
	if (max == null) {
		max = min;
		min = 0;
	}
	return min + Math.floor(Math.random() * (max - min + 1));
};

function scrollableEx() {
	var win = createWin();
	// Create a custom template that displays an image on the left,
	// then a title next to it with a subtitle below it.
	var myTemplate = {
		properties: {
			height: 50
		},
		childTemplates: [{
			type: 'Ti.UI.ImageView',
			bindId: 'leftImageView',
			properties: {
				left: 0,
				width: 40,
				localLoadSync: true,
				height: 40,
				transform: Ti.UI.create2DMatrix().rotate(90),
				backgroundColor: 'blue',
				// backgroundSelectedColor:'green',
				image: '/images/contactIcon.png',
				// borderColor:'red',
				// borderWidth:2
				viewMask: '/images/contactMask.png',
			}
		}, {
			type: 'Ti.UI.Label',
			bindId: 'label',
			properties: {
				multiLineEllipsize: Ti.UI.TEXT_ELLIPSIZE_TAIL,
				top: 2,
				bottom: 2,
				left: 45,
				padding: {
					bottom: 4
				},
				right: 55,
				touchEnabled: false,
				height: Ti.UI.FILL,
				color: 'black',
				font: {
					size: 16
				},
				width: Ti.UI.FILL
			}
		}, {
			type: 'Ti.UI.ImageView',
			bindId: 'rightImageView',
			properties: {
				right: 5,
				top: 8,
				localLoadSync: true,
				bottom: 8,
				width: Ti.UI.SIZE,
				touchEnabled: false
			}
		}, {
			type: 'Ti.UI.ImageView',
			bindId: 'networkIndicator',
			properties: {
				right: 40,
				top: 4,
				localLoadSync: true,
				height: 15,
				width: Ti.UI.SIZE,
				touchEnabled: false
			}
		}, {
			type: 'Ti.UI.View',
			properties: {
				backgroundColor: '#999',
				left: 4,
				right: 4,
				bottom: 0,
				height: 1
			}
		}]
	};
	var contactAction;
	var blurImage;
	var listView = Ti.UI.createListView({
		// Maps myTemplate dictionary to 'template' string
		templates: {
			'template': myTemplate
		},
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
	listView.addEventListener('itemclick', function(_event) {
		if (_event.hasOwnProperty('section') && _event.hasOwnProperty('itemIndex')) {
			var item = _event.section.getItemAt(_event.itemIndex);
			if (!contactAction) {
				contactAction = Ti.UI.createView({
					backgroundColor: 'green'
				});
				blurImage = Ti.UI.createImageView({
					scaleType: Ti.UI.SCALE_TYPE_ASPECT_FILL,
					width: Ti.UI.FILL,
					height: Ti.UI.FILL
				});
				contactAction.add(blurImage);
				blurImage.addEventListener('click', function() {
					animation.fadeOut(contactAction, 200, function() {
						win.remove(contactAction);
					});
				});
			}
			contactAction.opacity = 0;
			win.add(contactAction);
			var image = Ti.Media.takeScreenshot();
			// var image = Ti.Image.getFilteredViewToImage(win,
			// Ti.Image.FILTER_GAUSSIAN_BLUR, {scale:0.3});
			blurImage.image = image;
			animation.fadeIn(contactAction, 300);
		}
	});
	var names = ['Carolyn Humbert', 'David Michaels', 'Rebecca Thorning',
		'Joe B', 'Phillip Craig', 'Michelle Werner', 'Philippe Christophe',
		'Marcus Crane', 'Esteban Valdez', 'Sarah Mullock'
	];

	function formatTitle(_history) {
		return _history.fullName + '<br><small><small><b><font color="#5B5B5B">' + (new Date()).toString() +
			'</font> <font color="#3FAC53"></font></b></small></small>';
	}

	function random(min, max) {
		if (max == null) {
			max = min;
			min = 0;
		}
		return min + Math.floor(Math.random() * (max - min + 1));
	};

	function update() {
		// var outgoingImage = Ti.Utils.imageBlob('/images/outgoing.png');
		// var incomingImage = Ti.Utils.imageBlob('/images/incoming.png');
		var dataSet = [];
		for (var i = 0; i < 300; i++) {
			var callhistory = {
				fullName: names[Math.floor(Math.random() * names.length)],
				date: random(1293886884000, 1376053320000),
				kb: random(0, 100000),
				outgoing: !!random(0, 1),
				wifi: !!random(0, 1)
			};
			dataSet.push({
				contactName: callhistory.fullName,
				label: {
					html: formatTitle(callhistory)
				},
				rightImageView: {
					image: (callhistory.outgoing ? '/images/outgoing.png' : '/images/incoming.png')
				},
				networkIndicator: {
					image: (callhistory.wifi ? '/images/wifi.png' : '/images/mobile.png')
				}
			});
		}
		var historySection = Ti.UI.createListSection();
		historySection.setItems(dataSet);
		listView.sections = [historySection];
	}
	win.add(listView);
	win.addEventListener('open', update);
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
		if (view.opacity === 0)
			showMe();
		else
			hideMe();
	});
	win.add(button);
	openWin(win);
}

function htmlLabelEx() {
	var win = createWin({
		layout: 'vertical'
	});
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
		ellipsize: Ti.UI.TEXT_ELLIPSIZE_TAIL,
		maxHeight: 100,
		textIsSelectable: true,
		bottom: 20,
		html: html
	}));
	scrollView.add(Ti.UI.createLabel({
		disableLinkStyle: true,
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
		backgroundColor: 'green',
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
	win.add({
		type: 'Ti.UI.TextField',
		properties: {
			height: 40,
			width: 'FILL',
			backgroundColor: 'gray',
			bottom: 30
		}
	});
	win.add(scrollView);
	scrollView.addEventListener('click', function(e) {
		sinfo(e.link);
		// var index = e.source.characterIndexAtPoint({x:e.x,y:e.y});
		// Ti.API.info(index);
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
		view.backgroundImage = varSwitch(view.backgroundImage,
			'/images/gradients.svg', '/images/Logo.svg');
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
		imageView.image = varSwitch(imageView.image, '/images/gradients.svg',
			'/images/Logo.svg');
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
var transitionsMap = _.map(Ti.UI.TransitionStyle, function(value, key) {
	return {
		title: _.startCase(key),
		id: value
	};
});


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

	win.addEventListener('click', function() {
		info('click');
		// notificationView.showMessage({
		// 	message: 'test',
		// 	level: 'error'
		// });
	})
	win.open();
}

function keyboardTest() {
	var textfield = Ti.UI.createTextField({
		hintText: 'hint',
		// hintColor:'blue',
		ellipsize: true,
		maxLength: 4
	});
	var dialog = Ti.UI.createAlertDialog({
		title: 'test',
		buttonNames: ['ok', 'cancel'],
		persistent: true,
		cancel: 1,
		customView: textfield
	});
	// textfield.addEventListener('change', function(e) {
	// textfield.blur();
	// });
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
		height: 'SIZE',
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
		height: 80,
	});

	var params = {
		transition: {
			style: Ti.UI.TransitionStyle.CUBE,
			duration: 3000
		}
	};

	var button = Ti.UI.createButton({
		bottom: 0,
		bubbleParent: false,
		title: 'Transition'
	});
	button.addEventListener('click', function() {
		choseTransition(params, 'transition');
	});
	win.add(button);

	var onOff = true;
	win.add({
		type: 'Ti.UI.Button',
		properties: {
			top: 0,
			title: 'Switch'
		},
		events: {
			'click': function() {
				holderHolder.transitionViews(onOff ? null : transitionViewHolder, onOff ? transitionViewHolder : null, params.transition);
				onOff = !onOff;
			}
		}
	});

	tr1.addEventListener('click', function(e) {
		Ti.API.info('click');
		transitionViewHolder.transitionViews(tr1, tr2, _.assign({
			reverse: true
		}, params.transition));
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
		transitionViewHolder.transitionViews(tr2, tr1, params.transition);
	});
	transitionViewHolder.add(tr1);
	holderHolder.add(transitionViewHolder);
	win.add(holderHolder);
	openWin(win);
}

function opacityTest(_args) {
	var win = createWin(_.assign({
		dispatchPressed: true,
		backgroundSelectedColor: 'green'
	}, _args));

	var image1 = Ti.UI.createImageView({
		touchEnabled: false,
		backgroundColor: 'yellow',
		image: "animation/win_1.png"
	});
	// image1.addEventListener('longpress', function() {
	// image1.animate({
	// // opacity: 0,
	// backgroundColor: 'transparent',
	// autoreverse: true,
	// duration: 2000,
	// });
	// });

	var button = Ti.UI.createButton({
		top: 0,
		padding: {
			left: 30,
			top: 30,
			bottom: 30,
			right: 30
		},
		width: 'SIZE',
		height: 100,
		bubbleParent: false,
		backgroundColor: 'gray',
		touchPassThrough: true,
		dispatchPressed: false,
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
	// button.add(Ti.UI.createView({
	// touchPassThrough: true,
	// backgroundColor: 'orange',
	// right: 0,
	// width: 35,
	// height: Ti.UI.FILL
	// }));
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
	var t3 = Ti.UI.create2DMatrix().scale(2.0, 2.0).translate(0, -40)
		.rotate(90);
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
		width: 'SIZE',
		height: 'SIZE',
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
	// win.add(button2);
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
		}, {
			properties: {
				title: 'ColorArt'
			},
			callback: imageFilterTest
		}]
	}];
	win.add(listview);
	openWin(win);
}

function imageViewTransitionTest(_args) {
	var win = createWin(_args);

	var image1 = Ti.UI.createImageView({
		backgroundColor: 'yellow',
		tintColor: 'red',
		tintColorImage: true,
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

function imageViewAnimationTest(_args) {
	_args.tintColor = 'red';

	var win = createWin(_args);

	var image1 = Ti.UI.createImageView({
		backgroundColor: 'yellow',
		// tintColorImage: true,
		tintColorImage: true,
		width: 100,
		transition: {
			style: Ti.UI.TransitionStyle.FADE,
		},
		image: 'http://zapp.trakt.us/images/posters_movies/192263-138.jpg',
		animatedImages: ["animation/win_1.png", "animation/win_2.png",
			"animation/win_3.png", "animation/win_4.png",
			"animation/win_5.png", "animation/win_6.png",
			"animation/win_7.png", "animation/win_8.png",
			"animation/win_9.png", "animation/win_10.png",
			"animation/win_11.png", "animation/win_12.png",
			"animation/win_13.png", "animation/win_14.png",
			"animation/win_15.png", "animation/win_16.png"
		],
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
	btnHolder.add([{
		type: 'Ti.UI.Button',
		left: 0,
		bid: 0,
		title: 'start'
	}, {
		type: 'Ti.UI.Button',
		right: 0,
		bid: 1,
		title: 'pause'
	}, {
		type: 'Ti.UI.Button',
		left: 0,
		bid: 2,
		title: 'resume'
	}, {
		type: 'Ti.UI.Button',
		right: 0,
		bid: 3,
		title: 'playpause'
	}, {
		type: 'Ti.UI.Button',
		left: 0,
		bid: 4,
		title: 'stop'
	}, {
		type: 'Ti.UI.Button',
		right: 0,
		bid: 5,
		title: 'reverse'
	}, {
		type: 'Ti.UI.Button',
		left: 0,
		bid: 6,
		title: 'autoreverse'
	}, {
		type: 'Ti.UI.Button',
		right: 0,
		bid: 7,
		title: 'transition'
	}]);
	btnHolder.addEventListener('singletap', function(e) {
		info(stringify(e));
		switch (e.source.bid) {
			case 0:
				// image1.start();
				sdebug(image1.progress);
				sdebug(image1.touchPassThrough);
				// image1.touchPassThrough = 1;
				image1.progress = 0.8;
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

function antiAliasTest(_args) {
	var win = createWin(_args);
	var html =
		'  SUCCESS     <font color="red">musique</font> électronique <b><span style="background-color:green;border-color:black;border-radius:20px;border-width:1px">est un type de </span><big><big>musique</big></big> qui a <font color="green">été conçu à</font></b> partir des années<br> 1950 avec des générateurs de signaux<br> et de sons synthétiques. Avant de pouvoir être utilisée en temps réel, elle a été primitivement enregistrée sur bande magnétique, ce qui permettait aux compositeurs de manier aisément les sons, par exemple dans l\'utilisation de boucles répétitives superposées. Ses précurseurs ont pu bénéficier de studios spécialement équipés ou faisaient partie d\'institutions musicales pré-existantes. La musique pour bande de Pierre Schaeffer, également appelée musique concrète, se distingue de ce type de musique dans la mesure où son matériau primitif était constitué des sons de la vie courante. La particularité de la musique électronique de l\'époque est de n\'utiliser que des sons générés par des appareils électroniques.';
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
			transform: 's0.3',
			duration: 2000,
			autoreverse: true,
			curve: [0, 0, 1, -1.14]
		});
	});

	win.add(view);
	openWin(win);
}
var modules = ['shapes', 'bluetooth', 'charts'];
var moduleItems = [];
for (var i = 0; i < modules.length; i++) {
	var module = require(modules[i]).load(this);
	moduleItems.push({
		properties: {
			title: _.capitalize(modules[i])
		},
		callback: module.exs
	});
};

function modulesExs(_args) {
	var button = Titanium.UI.createButton({
		title: 'Close'
	});
	button.addEventListener('click', function() {
		var current = win.fullscreen;
		sdebug(current);
		// win.fullscreen = [1-current, {
		// animationStyle:Ti.UI.iPhone.StatusBar.ANIMATION_STYLE_SLIDE
		// }];
		win.setFullscreen(1 - current, {
			animationStyle: 2
		});
	})
	var win = createWin(_.assign({
		tintColor: 'purple',
		statusBarStyle: 1,
		rightNavButton: button,
	}, _args), true);
	var listview = createListView();
	listview.sections = [{
		items: moduleItems
	}];
	win.add(listview);
	openWin(win);
}

Ti.include('listview.js');
var color = cellColor(0);
var headerView = new ImageView({
	scaleType: Ti.UI.SCALE_TYPE_ASPECT_FILL,
	image: 'http://phandroid.s3.amazonaws.com/wp-content/uploads/2012/02/Android-At-Home-banner.jpg',
	height: 200
});
var firstWindow = createWin({
	// properties: {
	// tintColor : 'red',
	top: 0,
	// barColor:'rgba(255,255,255,0)',
	title: 'main',
	extendEdges: [Ti.UI.EXTEND_EDGE_ALL],
	// hideShadow:true,
	autoAdjustScrollViewInsets: true,
	translucent: true,
	startBarDeltaY: 0,
	barOpacity: 0,
	startToolbarDeltaY: 0,
	navBarHidden: true,
	barColor: 'transparent',
	toolbar: [Ti.UI.createButton({
			properties: {
				title: 'test',
			},
			events: {
				click: function() {
					firstWindow.listView.editing = !firstWindow.listView.editing;
				}
			}
		})]
		// }
});
var headerView2 = new View({
	properties: {
		layout: 'vertical',
		height: 'SIZE'
	},
	childTemplates: [{
		bindId: 'label',
		type: 'Ti.UI.Label',
		properties: {
			backgroundColor: 'red',
			height: 20,
			font: {
				weight: 'thin'
			},
			text: 'HeaderView created from Dict'
		}
	}]
});

firstWindow
	.add({
		bindId: 'listView',
		type: 'Ti.UI.ListView',
		properties: {

			// headerTitle: 'DEVICE',
			headerView: headerView,
			footerTitle: 'This is a footer textfor the list view',
			overScrollMode: 2,
			searchHidden: true,
			// style:1,
			allowsSelection: false,
			allowsMultipleSelectionDuringEditing: true,
			canEdit: true,
			// searchView: {
			// type: 'Ti.UI.SearchBar',
			// properties: {
			// // hideNavBarWithSearch:false,
			// // barColor: '#000',
			// showCancel: true,
			// height: 84,
			// value: 'test',
			// top: 0,
			// }
			// },
			sections: [{
					// footerTitle: 'This is a footer text',
					// headerTitle: 'WIRELESSS & NETWORK',
					headerView: headerView2,

					items: [{
						properties: {
							title: 'Modules',
							height: 100
						},
						callback: modulesExs
					}, {
						properties: {
							title: 'ListView'
						},
						callback: listViewExs
					}, {
						properties: {
							title: 'TextViews',
							subtitle: 'this is a test'
						},
						callback: textViewTests
					}, {
						properties: {
							title: 'Transform',
							height: Ti.UI.FILL,
							backgroundColor: color
						},
						callback: transformExs
					}, {
						title: 'Pickers',
						callback: pickerTests
					}, {
						properties: {
							height: 200,
							title: 'SlideMenu'
						},
						callback: slideMenuEx
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
							title: 'webView'
						},
						callback: videoOverlayTest
					}, {
						properties: {
							title: 'GCTest'
						},
						callback: GCTest
					}, {
						properties: {
							title: 'splash'
						},
						callback: testSplash
					}, {
						properties: {
							title: 'testi'
						},
						callback: testi
					}]
				}]
				// minRowHeight:100,
				// maxRowHeight:140
		},
		events: {
			'scrollend': function(e) {
				firstWindow.startBarDeltaY = firstWindow.barDeltaY;
				firstWindow.startToolbarDeltaY = firstWindow.toolbarDeltaY;
				firstWindow.startScrollY = e.contentOffset.y;

			},
			'scroll': {
				variables: {
					offset: 'contentOffset.y'
				},
				expressions: {
					a: 'max(_offset/2,0)',
					b: 'max(_offset/200*30,0)',
				},
				targets: [{
						target: headerView,
						properties: {
							transform: 't0,_a',
							opacity: '1-_offset/200'
						}
					},
					// {
					// 	target:headerView2.label,
					// 	properties:{
					// 		height:'20  + _b'
					// 	}
					// },
					__APPLE__ ? {
						target: firstWindow,
						targetVariables: {
							startBarDeltaY: 'startBarDeltaY',
							startToolbarDeltaY: 'startToolbarDeltaY',
							startScrollY: 'startScrollY'
						},
						properties: {
							barDeltaY: 'min(max(floor((_offset -_startScrollY)/5 + _startBarDeltaY),0), 66)',
							toolbarDeltaY: 'min(max(floor((_offset -_startScrollY)/5 + _startToolbarDeltaY),0), 44)'
						}
					} : {
						target: firstWindow,
						properties: {
							barOpacity: 'min(max(_offset, 0), 45) / 45'
						}
					}
				]

			},
			'itemclick': listViewClickHandle
		}
	});
firstWindow.addEventListener('open', function() {
	info('open');
	// listview.appendSection({
	// headerTitle: 'test2',
	// items: [{
	// title: 'test item'
	// }]
	// });
});
var mainWin = Ti.UI.createNavigationWindow({
	// backgroundColor: backColor,

	// swipeToClose: false,
	exitOnClose: true,
	// theme: "Theme.Titanium.TranslucentActionBar.Overlay",
	title: 'AKYLAS_MAIN_WINDOW',
	window: firstWindow,
	// transition: {
	// style: Ti.UI.TransitionStyle.SWIPE,
	// curve: [0.68, -0.55, 0.265, 1.55]
	// }
});
sdebug('test', mainWin.currentWindow.title)
mainWin.addEventListener('openWindow', function(e) {
	Ti.API.info(e);
});
mainWin.addEventListener('closeWindow', function(e) {
	Ti.API.info(e);
});
// firstWindow.add({
// type: 'Ti.UI.View',
// properties: {
// bottom: 0,
// backgroundColor: 'green',
// backgroundSelectedColor: 'yellow',barColor
// height: 50,
// width: 'FILL'
// }
// });
// 
mainWin.open();

// var notificationView = new NotificationWindow();

function textFieldTest(_args) {
	var win = createWin(_args);
	win.add([{
		type: 'Ti.UI.TextField',
		bindId: 'textfield',
		properties: {
			top: 100,
			color: 'black',
			borderWidth: 2,
			borderColor: 'black',
			backgroundColor: 'gray',
			color: '#686868',
			font: {
				size: 14
			},
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
			right: 4,
			textAlign: 'left',
			maxLines: 2,
			borderSelectedColor: '#74B9EF',
			height: 60,
			ellipsize: Ti.UI.TEXT_ELLIPSIZE_TAIL,
			rightButton: Ti.UI.createView({
				backgroundColor: 'yellow',
				top: 8,
				bottom: 8,
				width: 40
			}),
			rightButtonMode: Ti.UI.INPUT_BUTTONMODE_ONFOCUS
		}
	}, {
		type: 'Ti.UI.TextField',
		properties: {
			bottom: 20,
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
	// win.addEventListener('click', function() {
	// win.textfield.focus()
	// })
	win.textfield.focus()
	openWin(win);
}

function test4(_args) {
	var win = createWin(_.assign({
		backgroundColor: 'orange',
		layout: 'vertical'
	}, _args));
	// var view = Ti.UI.createView({
	// height:0,
	// backgroundColor:'red'
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

function borderPaddingEx(_args) {
	var win = createWin(_.assign({
		backgroundColor: 'white'
	}, _args));
	// var view = new View({
	// "type": "Ti.UI.Label",
	// "bindId": "title",
	// "properties": {
	// "rclass": "NZBGetTVRTitle",
	// text: 'downloadpath',
	// "font": {
	// "size": 14
	// },
	// "padding": {
	// "left": 4,
	// "right": 4
	// },
	// "textAlign": "right",
	// "width": 110,
	// "color": "black"
	// }
	// });
	win.add(new Label({
		properties: {
			text: 'test',
			backgroundColor: 'red',
			bottom: 10,
		},
		events: {
			'click': function() {
				info('click');
			}
		}
	}));

	win.add([{
		type: 'Ti.UI.View',
		properties: {
			top: 20,
			width: 300,
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
				// type: 'radial',
				// colors: ['orange', 'yellow']
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
				width: 'FILL',
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
					width: 'FILL',
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
					textAlign: 'center',
					right: 0,
					text: 'Aaaa',
					clearIcon: 'X',
					icon: 'A',
					transition: {
						style: Ti.UI.TransitionStyle.FLIP
					}
				}
			}]
		}],
		events: {
			'longpress': function(e) {
				info('test' + JSON.stringify(e));
				// if (e.bindId === 'test') {
				e.source.text = (e.source.text === 'test') ? 'this is a test' : 'test';
				// }
				e.source.borderColor = 'red';
			}
		}
	}]);

	win
		.add({
			"properties": {
				"rclass": "GenericRow TVRow",
				"layout": "horizontal",
				"height": "SIZE"
			},
			"childTemplates": [{
				"type": "Ti.UI.Label",
				"bindId": "title",
				"properties": {
					"rclass": "NZBGetTVRTitle",
					"font": {
						"size": 14
					},
					"padding": {
						"left": 4,
						"right": 4,
						"top": 10
					},
					text: 'downloadpath',
					"textAlign": "right",
					"width": 90,
					"color": "black",
					// "height": "FILL",
					"verticalAlign": "top"
				}
			}, {
				"type": "Ti.UI.Label",
				"bindId": "value",
				"properties": {
					selectedColor: 'green',
					html: 'A new version is available <a href="https://github.com/RuudBurger/CouchPotatoServer/compare/b468048d95216474183daafaf46a4f2bd0d7ada7...master" target="_blank"><font color="red"><b><u>see what has changed</u></b></font></a> or <a href="update">just update, gogogo!</a>',
					// "autoLink":Ti.UI.AUTOLINK_ALL,
					"rclass": "NZBGetTVRValue",
					"color": "#686868",
					"font": {
						"size": 14
					},
					// transition: {
					// style: Ti.UI.TransitionStyle.SWIPE_FADE
					// },
					"top": 4,
					"bottom": 4,
					"padding": {
						"left": 4,
						"right": 4,
						"bottom": 2,
						"top": 2
					},
					"verticalAlign": "middle",
					"left": 4,
					"width": "FILL",
					"height": "SIZE",
					"right": 4,
					"textAlign": "left",
					"maxLines": 2,
					"ellipsize": Ti.UI.TEXT_ELLIPSIZE_TAIL,
					"borderColor": "#eeeeee",
					"borderRadius": 2
				}
			}]
		});

	info(win.value.text);
	var first = true;
	win.value.addEventListener('click', function(e) {
		info(stringify(e));
	});
	win.value.addEventListener('longpress', function(e) {
		info(stringify(e));
	});

	win.add({
		type: 'Ti.UI.View',
		properties: {
			width: 200,
			height: 20
		},
		events: {
			'click': function(e) {
				info(stringify(e));
			}
		},
		childTemplates: [{
			borderPadding: {
				bottom: -1
			},
			borderColor: 'darkGray',
			backgroundColor: 'gray',
			borderRadius: 4
		}, {
			bindId: 'progress',
			properties: {
				borderPadding: {
					top: -1,
					left: -1,
					right: -1
				},
				borderColor: '#66AC66',
				backgroundColor: '#62C462',
				borderRadius: 4,
				left: 0,
				width: '50%',
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
			}
		}]
	});

	openWin(win);
}

function testMCTS(_args) {
	var win = createWin(_args);

	var listview = Ti.UI.createListView({
		delaysContentTouches: false,
		templates: {
			"title": {
				"childTemplates": [{
					"properties": {
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
				}
			},
			"value": {
				"childTemplates": [{
					"properties": {
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
					"properties": {
						// exclusiveTouch:true,
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
				"textfield": {}
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

function test3(_args) {
	var win = createWin(_args);
	var viewHolder = new View({
		width: '50%',
		height: '60',
		backgroundColor: 'yellow'
	});
	var test = new ScrollView({
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

function testLabel(_args) {
	var win1 = Ti.UI.createWindow({
			backgroundColor: 'blue'
		}),
		win2, win3;
	var navWin1 = Ti.UI.createNavigationWindow({
		backgroundColor: 'transparent',
		title: 'NavWindow1',
		window: win1,
		transition: {
			style: Ti.UI.TransitionStyle.FADE
		}
	});
	var leftMenu = createListView({
		backgroundColor: 'transparent',
		sections: [{
			items: [{
				properties: {
					title: 'nav1',
					backgroundColor: 'transparent'
				},
				callback: function() {
					navWin1.openWindow(win1);
					slidingMenu.toggleLeftView();
				}
			}, {
				properties: {
					title: 'nav2',
					backgroundColor: 'transparent'
				},
				callback: function() {
					if (!win2) {
						win2 = Ti.UI.createWindow({
							backgroundColor: 'red'
						});
					}
					navWin1.openWindow(win2);
					slidingMenu.toggleLeftView();
				}
			}, {
				properties: {
					title: 'Transition',
					backgroundColor: 'transparent'
				},
				callback: function() {
					if (!win3) {
						win3 = Ti.UI.createWindow({
							backgroundColor: 'green'
						});
					}
					navWin1.openWindow(win3);
					slidingMenu.toggleLeftView();
				}
			}, {
				properties: {
					title: 'Close',
					backgroundColor: 'transparent'
				},
				callback: function() {
					slidingMenu.close();
				}
			}]
		}]
	});
	// slidingMenu
	var slidingMenu = new SlideMenu({
		orientationModes: [Ti.UI.UPSIDE_PORTRAIT, Ti.UI.PORTRAIT,
			Ti.UI.LANDSCAPE_RIGHT, Ti.UI.LANDSCAPE_LEFT
		],
		leftViewWidth: -60,
		leftViewDisplacement: 40,
		shadowWidth: 0,
		backgroundGradient: {
			type: 'linear',
			colors: ['#444154', '#a86e6a'],
			startPoint: {
				x: 0,
				y: 0
			},
			endPoint: {
				x: "100%",
				y: 0
			}
		},
		leftView: leftMenu,
		centerView: navWin1
	});
	slidingMenu.open();
}


function showSpeedLimit() {
	var viewArgs = {
		properties: {
			rclass: 'Fill SizeHeight VHolder'
		},
		childTemplates: [{
			properties: {
				rclass: 'Fill SizeHeight HHolder'
			},
			childTemplates: [{
				// type: 'Ti.UI.Label',
				// properties: {
				// rclass: 'FillWidth',
				// rid: 'nzbGetSpeedLimitDesc'
				// }
				// }, {
				properties: {
					rid: 'nzbGetSpeedLimitTFHolder'
				},
				childTemplates: [{
					type: 'Ti.UI.TextField',
					bindId: 'textfield',
					properties: {
						rid: 'nzbGetSpeedLimitTF'
					}
				}, {
					type: 'Ti.UI.Label',
					properties: {
						// rclass: 'NZBGetBorderView',
						rid: 'nzbGetSpeedLimitUnit'
					}
				}]
			}]
		}]
	};
	var speedLimit = 0;
	if (speedLimit > 0) {
		viewArgs.childTemplates.push({
			type: 'Ti.UI.Label',
			properties: {
				rid: 'nzbGetCurrentSpeedLimit',
				text: 0
			}
		});
	}

	var args = {
		cancel: 0,
		// title: tr('speed_limit'),
		customView: new ScrollView(viewArgs),
		// buttonNames: ['close', 'setlimit']
	}
	var alert = new NZBGetAlert(args);
	alert.addEventListener('click', function(e) {
		if (e.cancel === false) {
			var rate = parseInt(alert.customView.textfield.value);
			Status.setSpeedLimitClick(rate, function() {
				Ti.App.fireEvent('nzbget', {
					subtype: 'cmd',
					command: 'rate',
					value: rate
				});
			});

		}
	});
	alert.on('touchstart', function(e) {
		if (e.source !== alert.customView.textfield) {
			alert.customView.textfield.blur();
		}
	});

	alert.showMe();
}

function videoOverlayTest(_args) {
	var win = createWin(_.assign({
		backgroundColor: 'transparent',
		lightweight: true,
		touchPassThrough: true,
		zIndex: 10
	}, _args));
	var htmlFormat, readFile = Titanium.Filesystem.getFile('video.html');
	htmlFormat = readFile.read();

	var currentDelta = 0,
		fullscreen = false,
		videoWVWidth = 200,
		videoWVRight = 20,
		totalDelta = 0,
		closeDelta = 0.5 * (videoWVWidth + videoWVRight),
		previousX = 0,
		previousVelocity = 0,
		inAnim = {
			transform: null,
			opacity: 1
		},
		outAnim = {
			transform: 'ot' + 2 * closeDelta + ',0',
			opacity: 0
		};

	var webView = new WebView(_.assign({
		scrollingEnabled: false,
		borderColor: 'gray',
		borderWidth: 2,
		width: videoWVWidth,
		height: Math.ceil(videoWVWidth * 9 / 16),
		bottom: videoWVRight,
		right: videoWVRight,
		// willHandleTouches:true,
		backgroundColor: 'black',
		mediaPlaybackRequiresUserAction: false,
		allowsInlineMediaPlayback: true,
		pluginState: 1
	}, outAnim));

	webView.addEventListener('longclick', function(e) {
		// to prevent html5 menu
	});

	webView.addEventListener('touchstart', function(e) {
		currentDelta = 0;
		totalDelta = 0;
		previousX = e.globalPoint.x;
	});

	webView.addEventListener('touchmove', function(e) {
		var newX = previousVelocity = e.globalPoint.x - previousX;
		var newDelta = Math.max(currentDelta + newX, 0);
		var delta = newDelta - currentDelta;
		totalDelta += delta;
		currentDelta += delta;
		webView.applyProperties({
			opacity: 1 - currentDelta / (2 * closeDelta),
			transform: 'ot' + currentDelta + ',0'
		});
		previousX = e.globalPoint.x;
	});

	function onTouchEnd(e) {
		var newX = e.globalPoint.x - previousX;
		var newDelta = Math.max(currentDelta + newX, 0);
		var delta = newDelta - currentDelta;
		totalDelta += delta;
		currentDelta += delta;
		info(previousVelocity);
		if (totalDelta >= closeDelta || previousVelocity >= 7) {
			webView.animate(_.assign({
				duration: totalDelta
			}, outAnim), function() {
				if (win != null)
					win.hideMe();
			});

		} else {
			webView.animate(_.assign({
				duration: totalDelta
			}, inAnim));
		}
	}
	webView.addEventListener('touchend', onTouchEnd);
	webView.addEventListener('touchcancel', onTouchEnd);

	win.add(webView);

	win.setVideoId = function(_videoId) {
		if (!_videoId)
			return;
		webView.html = htmlFormat.text.assign({
			videoId: _videoId
		});
		info(webView.html);
	};

	win.setVideoId('23m3EfXT0FM');

	var hidden = true;
	win.showMe = function() {

		if (hidden === true) {
			hidden = false;
			win.open({
				animated: false
			});
		}

		webView.animate(_.assign({
			duration: 2 * closeDelta
		}, inAnim));
	};

	win.hideMe = function() {
		if (hidden === false) {
			hidden = true;
			win.close({
				animated: false
			});
		}
	};
	win.addEventListener('close', function() {
		win = null;
	})
	win.showMe();
}

function zIndexTest() {
	var win1 = Titanium.UI.createWindow({
		title: 'Tab 1',
		backgroundColor: '#fff'
	});

	var g_backgroundGradient = {
		startPoint: {
			x: 0,
			y: 0
		},
		endPoint: {
			x: 400,
			y: 600
		},
		colors: ['blue', 'orange'],
		type: 'linear'
	};

	var backview = Titanium.UI.createView({
		backgroundGradient: g_backgroundGradient
			// <<< COMMENT OUT THIS LINE
			// TO FIX PROBLEM
	});

	var view1 = Titanium.UI.createView({
		backgroundColor: 'red',
		width: '300',
		height: '400',
		left: 15,
		top: 15,
		zIndex: 10,
	});

	var view2 = Titanium.UI.createView({
		width: '100',
		height: '70',
		left: 5,
		top: 5,
		backgroundColor: 'green',
		zIndex: 1
	});
	backview.add(view1);
	backview.add(view2);

	win1.add(backview);

	win1.open();
}

function horizontalLayout(_args) {
	var win = createWin({
		properties: _args,
		childTemplates: [{
			type: 'Ti.UI.ScrollView',
			properties: {
				width: 'FILL',
				height: 'FILL',
				layout: 'horizontal',
				horizontalWrap: true,
				backgroundColor: 'yellow'
			},
			childTemplates: [{
				// type: 'Ti.UI.Label',
				// properties: {
				// rid: 'dTitle'
				// }
				// }, {
				// type: 'Ti.UI.Label',
				// properties: {
				// rid: 'dDesc'
				// }
				// }, {
				type: 'Ti.UI.View',
				properties: {
					width: '44%',
					left: '2%',
					right: '2%',
					height: 40,
					backgroundColor: 'red'
				}
			}, {
				type: 'Ti.UI.View',
				properties: {
					width: '44%',
					left: '2%',
					right: '2%',
					height: 40,
					backgroundColor: 'blue'
				}
			}, {
				type: 'Ti.UI.View',
				properties: {
					width: '44%',
					left: '2%',
					right: '2%',
					height: 40,
					backgroundColor: 'orange'
				}
			}, {
				type: 'Ti.UI.View',
				properties: {
					width: '44%',
					left: '2%',
					right: '2%',
					height: 40,
					backgroundColor: 'purple'
				}
			}, {
				type: 'Ti.UI.TextField',
				bindId: 'userNameTF',
				properties: {
					borderRadius: 12,
					font: {
						size: 22
					},
					backgroundColor: 'white',
					hintColor: '#8C8C8C',
					padding: {
						left: 55,
						right: 5
					},
					height: 50,
					width: '90%',
					bottom: 20,
					returnKeyType: Ti.UI.RETURNKEY_NEXT,
					hintText: tr('username'),
				},
				childTemplates: [{
					type: 'Ti.UI.Label',
					properties: {
						left: 10,
						font: {
							size: 26
						},
						color: '#8C8C8C',
						selectedColor: 'black',
						text: 'b',
						// focusable:false
					}
				}]
			}, {
				type: 'Ti.UI.TextField',
				bindId: 'passwordTF',
				properties: {
					borderRadius: 12,
					font: {
						size: 22
					},
					backgroundColor: 'white',
					hintColor: '#8C8C8C',
					padding: {
						left: 55,
						right: 5
					},
					height: 50,
					width: '90%',
					bottom: 20,
					passwordMask: true,
					returnKeyType: Ti.UI.RETURNKEY_DONE,
					hintText: tr('password'),
				},
				childTemplates: [{
					type: 'Ti.UI.Label',
					properties: {
						left: 10,
						font: {
							size: 26
						},
						color: '#8C8C8C',
						selectedColor: 'black',
						text: 'b',
						// focusable:false
					}
				}]
			}]
		}]
	});
	openWin(win);
}

function showDummyNotification() {
	if (__ANDROID__) {
		// Intent object to launch the application
		var intent = Ti.Android.createIntent({
			className: Ti.Android.appActivityClassName,
			action: Ti.Android.ACTION_MAIN
		});
		intent.addCategory(Ti.Android.CATEGORY_LAUNCHER);

		// Create a PendingIntent to tie together the Activity and Intent
		var pending = Ti.Android.createPendingIntent({
			intent: intent,
			flags: Ti.Android.FLAG_UPDATE_CURRENT
		});

		// Create the notification
		var notification = Ti.Android.createNotification({
			flags: Ti.Android.FLAG_SHOW_LIGHTS | Ti.Android.FLAG_AUTO_CANCEL,
			contentTitle: tr('notif_title'),
			tickerText: tr('notif_title'),
			contentText: tr('notif_desc'),
			contentIntent: pending,
			ledOnMS: 3000,
			ledARGB: 0xFFff0000
		});
		// Send the notification.
		Ti.Android.NotificationManager.notify(1234, notification);
	}
}

Ti.App.addEventListener('pause', function() {
	info('pause');
	setTimeout(showDummyNotification, 10);
});

Ti.App.addEventListener('resume', function() {
	info('resume');
});

function tabGroupExample() {
	// create tab group
	var tabGroup = Titanium.UI.createTabGroup();

	//
	// create base UI tab and root window
	//
	var win1 = Titanium.UI.createWindow({
		title: 'Tab 1',
		backgroundColor: 'blue'
	});
	var tab1 = Titanium.UI.createTab({
		icon: 'KS_nav_views.png',
		title: 'Tab 1',
		window: win1
	});

	var button = Titanium.UI.createButton({
		color: '#999',
		title: 'Show Modal Window',
		width: 180,
		height: 35
	});

	win1.add(button);
	button.addEventListener('click', function(e) {

		var tabWin = Titanium.UI.createWindow({
			title: 'Modal Window',
			backgroundColor: '#f0f',
			width: '100%',
			height: '100%',
			tabBarHidden: true
		});

		var tabGroup = Titanium.UI.createTabGroup({
			bottom: -500,
			width: '100%',
			height: '100%'
		});
		var tab1 = Titanium.UI.createTab({
			icon: 'KS_nav_views.png',
			width: '100%',
			height: '100%',
			title: 'tabWin',
			window: tabWin
		});
		tabGroup.addTab(tab1);
		tabGroup.open();

		var closeBtn = Titanium.UI.createButton({
			title: 'Close'
		});
		tabWin.leftNavButton = closeBtn;
		closeBtn.addEventListener('click', function(e) {
			tabGroup.animate({
				duration: 400,
				bottom: -500
			}, function() {
				tabGroup.close()
			});
		});

		var tBtn = Titanium.UI.createButton({
			title: 'Click For Nav Group',
			width: 180,
			height: 35
		});
		tabWin.add(tBtn);
		tBtn.addEventListener('click', function(e) {
			var navWin = Titanium.UI.createWindow({
				title: 'Nav Window',
				backgroundColor: '#f00',
				width: '100%',
				height: '100%'
			});
			tab1.open(navWin);
		});

		tabGroup.animate({
			duration: 400,
			bottom: 0
		});
	});

	//
	// create controls tab and root window
	//
	var win2 = Titanium.UI.createWindow({
		title: 'Tab 2',
		backgroundColor: 'red'
	});

	var tBtn = Titanium.UI.createButton({
		title: 'Click For Nav Group',
		width: 180,
		height: 35
	});
	win2.add(tBtn);
	tBtn.addEventListener('click', function(e) {
		var navWin = Titanium.UI.createWindow({
			title: 'Nav Window',
			backgroundColor: '#f00',
			width: '100%',
			height: '100%'
		});
		tab1.open(navWin);
	});
	var tab2 = Titanium.UI.createTab({
		icon: 'KS_nav_ui.png',
		title: 'Tab 2',
		window: win2
	});

	//
	// add tabs
	//
	tabGroup.addTab(tab1);
	tabGroup.addTab(tab2);

	// open tab group
	tabGroup.open();
}

function navWindow2Ex(_args) {
	var win = createWin(_args)
	var navWin = Ti.UI.createNavigationWindow({
		swipeToClose: false,
		backgroundColor: 'green',
		title: 'NavWindow1',
		window: createWin({
			backgroundColor: 'red'
		})
	});
	win.add(navWin);
	win.open();
}

function textViewTests(_args) {
	var win = createWin(_args);
	var listview = createListView();
	listview.sections = [{
		items: [{
			properties: {
				title: 'TextArea'
			},
			callback: textAreaTest
		}]
	}];
	win.add(listview);
	openWin(win);
}

function textAreaTest(_args) {
	var win = createWin(_
		.assign({
			childTemplates: [{
				// type: 'Ti.UI.TextArea',
				// properties: {
				// backgroundColor: 'red',
				// color: 'white',
				// top: 0,
				// height: 'SIZE',
				// width: '80%',
				// padding: {
				// top: 4,
				// bottom: 4,
				// left: 4,
				// right: 4
				// },
				// font: {
				// size: 12
				// },
				// maxHeight: 100,
				// maxLines: 3,
				// suppressReturn: false,
				// hintText: 'Comment',
				// hintColor: 'yellow'
				// }
				// }, {
				// type: 'Ti.UI.TextArea',
				// properties: {
				// backgroundColor: 'blue',
				// callbackId: 'textfield',
				// color: 'white',
				// height: '100',
				// width: '80%',
				// padding: {
				// top: 4,
				// bottom: 4,
				// left: 4,
				// right: 4
				// },
				// font: {
				// size: 12
				// },
				// minHeight: 140,
				// // maxHeight: 90,
				// suppressReturn: false,
				// value: "dalvikvm: method
				// Lti/modules/titanium/ui/widget/TiUILabel$EllipsizingTextView;.getLineAtCoordinate
				// incorrectly overrides package-private method with
				// same name in
				// Landroid/widget/TextView;"
				// }
				// }, {
				type: 'Ti.UI.View',
				properties: {
					backgroundColor: 'yellow',
					// layout: 'vertical',
					height: 'FILL',
					width: 'FILL',
				},
				childTemplates: [{
					type: 'Ti.UI.TextArea',
					properties: {
						// backgroundColor : 'green',
						callbackId: 'textfield',
						color: 'white',
						height: 150,
						width: '80%',
						padding: {
							top: 4,
							bottom: 4,
							left: 4,
							right: 4
						},
						font: {
							size: 12
						},
						keyboardToolbar: {
							type: 'Ti.UI.iOS.Toolbar',
							properties: {
								items: [{
									type: 'Ti.UI.Button',
									systemButton: Ti.UI.iPhone.SystemButton.FLEXIBLE_SPACE
								}, {
									type: 'Ti.UI.Button',
									systemButton: Ti.UI.iPhone.SystemButton.DONE,
									callbackId: 'done',
								}]
							}
						},
						// top:0,
						// top:150,
						// maxLines: 3,
						suppressReturn: false,
						value: "dalvikvm: method Lti/modules/titanium/ui/widget/TiUILabel$EllipsizingTextView;.getLineAtCoordinate incorrectly overrides package-private method with same name in Landroid/widget/TextView;"
					}
				}]
			}]
		}, _args));
	win.addEventListener('click', function(e) {
		if (!e.source.callbackId) {
			e.source.blur();
		}
	})
	openWin(win);
}


function navWindowActionBarTest() {
	var win = Ti.UI.createWindow({
		backgroundColor: 'black',
		opacity: 0,
		childTemplates: [{
			type: 'Ti.UI.ImageView',
			properties: {
				defaultImage: '/images/poster.jpg',
				width: 'FILL',
				scaleType: Ti.UI.SCALE_TYPE_ASPECT_FIT,
				height: 'FILL'
			}
		}]
	});

	win.addEventListener('click', function(e) {
		win.close({
			opacity: 0,
			duration: 2000
		});
	});
	win.open({
		// navBarHidden:true,
		fullscreen: true,
		statusBarStyle: Titanium.UI.iPhone.StatusBar.LIGHT_CONTENT,
		opacity: 1,
		duration: 2000
	});
}

function shareTest(_data, _callbackSuccess, _callbackError) {
	var chars = (_data.text && _data.text != null) ? _data.text.length : 0;

	var classId = _.capitalize(this.moduleId);
	var win = new Window({
		properties: {
			rclass: 'SocialShareWindow ' + classId + 'SocialShareWindow'
		},
		childTemplates: [{
			bindId: 'holder',
			properties: {
				rclass: 'SocialShareWindowHolder ' + classId + 'SocialShareWindowHolder'
			},
			childTemplates: [{
				type: 'Ti.UI.Label',
				properties: {
					rclass: 'SocialShareWindowTitle ' + classId + 'SocialShareWindowTitle'

				},
				childTemplates: [{
					type: 'Ti.UI.Label',
					properties: {
						callbackId: 'cancel',
						rclass: 'SocialShareWindowCancel ' + classId + 'SocialShareWindowCancel'

					}
				}, {
					type: 'Ti.UI.Label',
					properties: {
						callbackId: 'send',
						rclass: 'SocialShareWindowSend ' + classId + 'SocialShareWindowSend'
					}
				}]
			}, {
				properties: {
					rclass: 'SocialShareWindowHolder2 ' + classId + 'SocialShareWindowHolder2'

				},
				childTemplates: [{
					type: 'Ti.UI.TextArea',
					bindId: 'textField',
					properties: {
						rclass: 'SocialShareWindowTextArea ' + classId + 'SocialShareWindowTextArea',
						value: _data.text,
						maxLength: 140
					}
				}, {
					type: 'Ti.UI.ImageView',
					properties: {
						rclass: 'SocialShareWindowImage ' + classId + 'SocialShareWindowImage',
						visible: (_data.image !== undefined || _data.imageBlob !== undefined),
						image: (_data.image || _data.imageBlob),

					}
				}]
			}]
		}]
	});
	win.addEventListener('open', function() {
		win.animate(win.openAnim);
		win.holder.animate(win.holder.openAnim);
		win.textField.focus();
	});
	win.open({
		animated: false
	});

	function closeWindow(_cancel) {
		win.textField.blur();
		win.animate(win.closeAnim);
		var animParams = (_cancel === true) ? win.holder.closeCancelAnim : win.holder.closeAnim;
		win.holder.animate(animParams, function() {
			win.close();
		});
	}

	win.addEventListener('click', function(_event) {
		if (_event.source.callbackId === 'cancel') {
			closeWindow(true);
		} else if (_event.source.callbackId === 'send') {
			_this.internalShare(_data, function(_result) {
				closeWindow(false);
				if (_callbackSuccess)
					_callbackSuccess(_result);
			}, function(_result) {
				closeWindow(false);
				if (_callbackError)
					_callbackError(_result);
			});
		} else {
			sdebug('focusing text view');
			win.textField.focus();
		}

	})
}

function shareExample(_data) {
	if (__APPLE__) {
		var activity = Ti.UI.iOS.createActivity({
			category: Ti.UI.iOS.ACTIVITY_CATEGORY_SHARE,
			type: 'es.oyatsu.custom1',
			title: 'Custom',
			image: '/images/activity.png',
			onPerformActivity: function() {
				Ti.API.info("Perform, baby!", arguments.length);
				for (var i = 0; i < arguments.length; i++) {
					Ti.API.info(arguments[i], typeof arguments[i]);
				}
				return true;
			}
		});
		var activityView = Ti.UI.iOS.createActivityView({
			activities: [activity],
			// excluded: [Ti.UI.iOS.ACTIVITY_TYPE_MAIL],
			items: [_data.subject, _data.text, _data.image],
			// itemForActivityType: function(_type, _items) {
			// Ti.API.info("itemForActivityType: ", _type);

			// return _items;
			// }
		});
		activityView.show();
	}
}

// Ti.App.clearImageCache();

Ti.Network.registerForPushNotifications({
	senderId: '724423202625',
	success: function(e) {
		sdebug('registerForPushNotifications', 'success', e);
	},
	error: function(e) {
		sdebug('registerForPushNotifications', 'error', e);
	},
	callback: function(e) {
		sdebug('registerForPushNotifications', 'callback', e);

	}
});

function randomColor() {
	var letters = '0123456789ABCDEF'.split('');
	var color = '#';
	for (var i = 0; i < 6; i++) {
		color += letters[Math.floor(Math.random() * 16)];
	}
	return color;
}

function scrollableViewTest(_args) {
	var tabs = [];
	for (var i = 0; i < 10; i++) {
		tabs.push({
			type: 'Ti.UI.Label',
			properties: {
				width: 'FILL',
				height: 'FILL',
				backgroundColor: randomColor(),
				color: 'white',
				textAlign: 'center',
				text: "not loaded tab " + i,
				bubbleParent: true,
				title: i
			}

		});
	}
	var win = createWin(_.assign({
		layout: 'vertical',
		childTemplates: [{
			type: 'Ti.UI.View',
			properties: {
				backgroundColor: 'blue',
				// layout: 'vertical',
				height: 30
			},
			events: {
				'click': function() {
					win.scrollable.scrollToView(win.scrollable.currentPage - 2, true);
				}
			}
		}, {
			type: 'Ti.UI.ScrollableView',
			bindId: 'scrollable',
			properties: {
				backgroundColor: 'yellow',
				// layout: 'vertical',
				height: 'FILL',
				width: 'FILL',
				views: tabs,
				transition: {
					// style: Ti.UI.TransitionStyle.FLIP
				}
			},
			events: {
				'first_load': function(e) {
					sdebug('first_load', e.source);
					e.source.text = 'loaded!'
				}
			}
		}, {
			type: 'Ti.UI.View',
			properties: {
				backgroundColor: 'red',
				// layout: 'vertical',
				height: 30
			},
			events: {
				'click': function() {
					win.scrollable.scrollToView(win.scrollable.currentPage + 2, true);
				}
			}
		}]
	}, _args));
	// win.add(new AppTabView({
	// tabsControllerClass: 'TabsControllerClassTest',
	// nativeControls: true,
	// tabs: tabs
	// }));
	openWin(win);
}

// shareExample ({
// subject:'<html><body><ul><li>one</li><li>two</li></ul></body></html>',
// text:"this is the text coming with the shared test",
// image:Ti.Media.takeScreenshot()
// });

// var location = require('akylas.millenoki.location');
// Ti.App.Properties.setString(location.SETTINGS_URL,
// 'http://report.datasquasher.com/s/l.php');
// Ti.App.Properties.setString(location.SETTINGS_ID, '12345');
// Ti.App.Properties.setString(location.SETTINGS_PASSPHRASE, 'tester');
// Ti.App.Properties.setString(location.SETTINGS_x1, '1');
// location.start();

// Ti.Geolocation.reverseGeocoder(45.1930902, 5.7166627, function(_result) {
// sdebug('reverseGeocoder', _result);
// });

function imageFilterTest(_args) {
	sdebug(Ti.Image.FILTER_IOS_BLUR);
	// Ti.Filesystem.getFile(Ti.Filesystem.resourcesDirectory,
	// 'colorArt.jpg').read().imageAsFiltered({
	// colorArt: {
	// width: 300,
	// height: 300
	// }
	// });

	var win = createWin(_
		.assign({
			properties: {
				layout: 'vertical'
			},
			childTemplates: [{
				type: 'Ti.UI.Label',
				bindId: 'primaryColor',
				properties: {
					font: {
						size: 20
					},
					text: 'primaryColor'
				}
			}, {
				type: 'Ti.UI.Label',
				bindId: 'secondaryColor',
				properties: {
					font: {
						size: 18
					},
					text: 'secondaryColor'
				}
			}, {
				type: 'Ti.UI.Label',
				bindId: 'detailColor',
				properties: {
					font: {
						size: 14
					},
					text: 'detailColor'
				}
			}, {
				type: 'Ti.UI.ImageView',
				properties: {
					preventDefaultImage: true,
					localLoadSync: true,
					width: Ti.UI.FILL,
					height: Ti.UI.FILL,
					image: 'http://image.tmdb.org/t/p/original/vhyyNGLOwhFEChGd5EjBTR1iydw.jpg',
					// image:'https://www.nzb.su/covers/tv/small/43773.jpg',
					filterOptions: {
						// tint:'#4E5969',
						scale: 0.3,
						colorArt: {
							// width:300,
							// height:300
						},
						filters: [{
							// saturation:0.6,
							type: Ti.Image.FILTER_IOS_BLUR
						}]
					},
					transition: {
						style: Ti.UI.TransitionStyle.FADE
					}
				},
				events: {
					'load': function(e) {
						sdebug(e);
						if (__ANDROID__) {
							var actionBar = win._internalActivity.actionBar;
							sdebug(actionBar);
							actionBar.displayHomeAsUp = false;
							actionBar.backgroundColor = e.colorArt.backgroundColor;
						} else {
							win.barColor = e.colorArt.backgroundColor;
						}
						win.backgroundColor = e.colorArt.backgroundColor;
						win.primaryColor.color = e.colorArt.primaryColor;
						win.secondaryColor.color = e.colorArt.secondaryColor;
						win.detailColor.color = e.colorArt.detailColor;
						// Ti.Image.getFilteredImage(e.image,
						// {
						// filters: [{
						// type: Ti.Image.FILTER_IOS_BLUR
						// }],
						// callback: function(result) {
						// win.blurImageView.image =
						// result.image;
						// }
						// });
					}
				}
				// }, {
				// type: 'Ti.UI.ImageView',
				// bindId: 'blurImageView',
				// properties: {
				// preventDefaultImage: true,
				// width: Ti.UI.FILL,
				// height: Ti.UI.FILL,
				// transition: {
				// style: Ti.UI.TransitionStyle.FADE
				// }
				// }
			}]
		}, _args));
	openWin(win);
}

function optionDialogTest() {
	var dialog = Ti.UI.createOptionDialog({
		title: 'birthdate',
		buttonNames: [],
		persistent: true,
		tapOutDismiss: true,
		customView: {
			type: 'Ti.UI.Picker',
			bindId: 'picker',
			properties: {
				type: Titanium.UI.PICKER_TYPE_DATE,
				maxDate: new Date(),
			}
		}

	});
	dialog.addEventListener('click', function(e) {});
	dialog.show();
}

function scrollBlurTest(_args) {
	var fullHeight;
	var win = createWin(_
		.assign({
			properties: {},
			childTemplates: [{
				type: 'Ti.UI.ImageView',
				bindId: 'mainImage',
				properties: {
					preventDefaultImage: true,
					localLoadSync: true,
					width: Ti.UI.FILL,
					height: Ti.UI.FILL,
					scaleType: Ti.UI.SCALE_TYPE_ASPECT_FILL,
					image: 'https://image.tmdb.org/t/p/w396/tPnw9oqIvIMoYjVozkDkxm3xR8r.jpg'
				}
			}, {
				type: 'Ti.UI.View',
				bindId: 'blurImageHolder',
				properties: {
					width: Ti.UI.FILL,
					height: 50,
					instantUpdates: true,
					bottom: 0
				},
				childTemplates: [{
					type: 'Ti.UI.ImageView',
					bindId: 'blurImage',
					properties: {
						preventDefaultImage: true,
						localLoadSync: true,
						bottom: 0,
						width: Ti.UI.FILL,
						height: Ti.UI.FILL,
						image: 'https://image.tmdb.org/t/p/w396/tPnw9oqIvIMoYjVozkDkxm3xR8r.jpg',
						scaleType: Ti.UI.SCALE_TYPE_ASPECT_FILL,
						filterOptions: {
							scale: 0.3,
							colorArt: true,
							filters: [{
								radius: 10,
								type: Ti.Image.FILTER_IOS_BLUR
							}]
						},
						transition: {
							style: Ti.UI.TransitionStyle.FADE
						}
					},
					events: {
						'load': function(e) {
							sdebug(e);
							// e.source.height =
							// win.mainImage.rect.height;
							if (__ANDROID__) {
								var actionBar = win.activity.actionBar;
								actionBar.displayHomeAsUp = false;
								actionBar.backgroundColor = e.colorArt.backgroundColor;
							} else {
								win.barColor = e.colorArt.backgroundColor;
							}
						}
					}
				}]
			}, {
				type: 'Ti.UI.ScrollView',
				// bindId: 'scrollView',
				properties: {
					layout: 'vertical',
					touchPassThrough: true,
					disableBounce: true,
					width: Ti.UI.FILL,
					contentWidth: Ti.UI.FILL,
					height: Ti.UI.FILL,
					contentHeight: Ti.UI.SIZE,
					// syncevents:'scroll',
				},
				childTemplates: [{
					type: 'Ti.UI.View',
					bindId: 'topview',
					properties: {
						touchPassThrough: true,
						height: 0
					}
				}, {
					type: 'Ti.UI.ImageView',
					properties: {
						height: 22,
						width: 24,
						image: '/images/up-arrow.png'
					},
					// events: {
					// 'click': function() {
					// win.blurImage.blurmask.animate({
					// duration: 400,
					// height: '50%',
					// autoreverse: true,
					// // restartFromBeginning:true,
					// // repeat: 2,
					// // lineColor: 'yellow',
					// // fillColor: 'blue',
					// curve: [0.68, -0.55, 0.265, 1.55]
					// });
					// }
					// }
				}, {
					type: 'Ti.UI.Label',
					properties: {
						text: 'Photo information',
						color: '#88000000',
						textAlign: 'center',
						bottom: 10,
						font: {
							weight: 'light',
							size: 18
						}
					}
				}, {
					type: 'Ti.UI.Label',
					bindId: 'descriptionLabel',
					properties: {
						text: 'Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
						color: '#99000000',
						font: {
							weight: 'light',
							size: 16
						}
					}
				}],
				events: {
					'scroll': function(_event) {
						win.blurImageHolder.height = 50 + _event.y;
						// win.blurImage.blurmask.applyProperties({
						// height: 50 + _event.y
						// });
					}
				}
			}],
			events: {
				postlayout: function() {
					var newHeight = win.mainImage.rect.height;
					if (newHeight !== fullHeight) {
						fullHeight = newHeight;
						sdebug('fullHeight', fullHeight);
						win.blurImage.height = fullHeight;
						win.topview.height = fullHeight - 54;
						win.descriptionLabel.height = fullHeight - 54;
					}
				}
			}
		}, _args));
	openWin(win);
}



function switchTest() {
	var win = Ti.UI.createWindow({
		layout: 'vertical'
	});

	win.add(Ti.UI.createLabel({
		text: "Default",
		color: 'black',
		textAlign: "left",
		height: 25,
		left: 7,
		top: 0
	}));

	var defaultSwitch = Ti.UI.createSwitch({
		top: 5,
		left: 20,
		value: true
	});
	win.add(defaultSwitch);

	win.add(Ti.UI.createLabel({
		text: "Sized Switch",
		color: 'black',
		textAlign: "left",
		height: 25,
		left: 7,
		top: 15
	}));

	var bigSwitch = Ti.UI.createSwitch({
		top: 5,
		height: 50,
		width: 250,
		left: 20,
		value: true
	});
	win.add(bigSwitch);

	win.add(Ti.UI.createLabel({
		text: "Knob Color",
		textAlign: "left",
		color: 'black',
		height: 25,
		left: 7,
		top: 15
	}));
	var colorKnobSwitch = Ti.UI.createSwitch({
		top: 5,
		left: 20,
		value: true,
		thumbTintColor: "orange"
	});
	win.add(colorKnobSwitch);

	win.add(Ti.UI.createLabel({
		text: "Active/Pressed Color",
		textAlign: "left",
		color: 'black',
		height: 25,
		left: 7,
		top: 15
	}));
	var activeColorSwitch = Ti.UI.createSwitch({
		top: 5,
		left: 20,
		value: true,
		activeColor: "orange",
	});
	win.add(activeColorSwitch);

	win.add(Ti.UI.createLabel({
		text: "On Color",
		textAlign: "left",
		color: 'black',
		height: 25,
		left: 7,
		top: 15
	}));
	var onColorSwitch = Ti.UI.createSwitch({
		top: 5,
		left: 20,
		height: 30,
		value: false,
		isRounded: false,
		title: 'test',
		backgroundCheckedColor: 'yellow',
		onTintColor: "yellow"
	});
	win.add(onColorSwitch);

	win.add(Ti.UI.createLabel({
		text: "Inactive Color",
		textAlign: "left",
		color: 'black',
		height: 25,
		left: 7,
		top: 15
	}));
	var inactiveColorSwitch = Ti.UI.createSwitch({
		top: 5,
		left: 20,
		height: 30,
		value: false,
		inactiveColor: "blue"
	});
	win.add(inactiveColorSwitch);
	inactiveColorSwitch.addEventListener('change', function() {
		inactiveColorSwitch.thumbTintColor = 'red';
	})

	openWin(win);
}

function GCTest() {
	createWin({
		backgroundColor: 'yellow',
		title: 'GCTest'
	}).open();
}

function testSplash() {
	createWin({
		backgroundColor: 'yellow',
		navBarHidden: true,
		backgroundImage: Ti.Platform.splashImageForCurrentOrientation,
		top: 0,
		title: 'GCTest',
		// childTemplates:[{
		// type: 'Ti.UI.View',
		// properties: {
		// backgroundImage: Ti.Platform.splashImageForCurrentOrientation,
		// width:'FILL',
		// height:'FILL',
		// scaleType: Ti.UI.SCALE_TYPE_ASPECT_FILL,
		// backgroundColor: 'red'
		// }
		// }]
	}).open();
}

function testi() {
	createWin({
		backgroundColor: 'green',
		navBarHidden: true,
		title: 'testi',
		childTemplates: [{
			type: 'Ti.UI.View',
			properties: {
				top: $navBarTop + $navBarHeight,
				bottom: 0,
				layout: 'vertical',
				backgroundColor: 'yellow'
			},
			childTemplates: [{
				type: 'Ti.UI.View',
				properties: {
					top: 0,

					bottom: 0,
					backgroundColor: 'blue'
				},

			}, {
				type: 'Ti.UI.View',
				properties: {
					top: 0,
					height: 20,
					bottom: 5,
					backgroundColor: 'red'
				},

			}]
		}]
	}).open();
}

function pickerTests(_args) {
	openWin(createWin(_.assign({
		childTemplates: [createListView({
			sections: [{
				items: [{
					properties: {
						title: 'columns test'
					},
					callback: pickerColumnsTest
				}]
			}]
		})]
	}, _args), true));
}

function pickerColumnsTest(_args) {
	var win = createWin(_.assign({
		backgroundColor: 'transparent',
		navBarHidden: true
	}, _args));
	// var pickerCol = new PickerColumn({
	// width: 'FILL'
	// });
	//
	// var data = [];
	// for (i = 0; i <= 30; i++) {
	// data[i] = new PickerRow({
	// title: (i + 1).toString()
	// });
	// pickerCol.addRow(data[i]);
	// }
	// picker.selectionIndicator = true;
	// picker.add(pickerCol);

	var billingPicker = new View({
		properties: {
			bottom: 0,
			layout: 'vertical',
			height: 'SIZE',
			backgroundColor: __APPLE__ ? '#eeffffff' : '#ee000000',
			transform: 'ot0,100%',
		},
		childTemplates: [{
			type: 'Ti.UI.View',
			properties: {
				width: "FILL",
				height: 40
			},
			childTemplates: [{
				type: 'Ti.UI.Label',
				bindId: 'cancelButton',
				properties: {
					rclass: 'DatePickerButton',
					padding: {
						left: 10,
						right: 10
					},
					height: 'FILL',
					color: __APPLE__ ? 'black' : 'white',
					selectedColor: __APPLE__ ? 'white' : 'black',
					left: 0,
					text: 'Cancel'
				}
			}, {
				type: 'Ti.UI.Label',
				bindId: 'doneButton',
				properties: {
					padding: {
						left: 10,
						right: 10
					},
					height: 'FILL',
					color: __APPLE__ ? 'black' : 'white',
					selectedColor: __APPLE__ ? 'white' : 'black',
					right: 0,
					text: 'Done'
				}
			}]
		}, {
			type: 'Ti.UI.Picker',
			properties: {
				width: 'FILL',
				selectionIndicator: true,
				useSpinner: true,
				rows: _.times(31, function(n) {
					return '' + (n + 1);
				}),
				selectedRow: [0, 21]
			},
			// childTemplates: [{
			// childTemplates: _.times(31, function(n) {
			// return {
			// title: '' + (n + 1)
			// };
			// })
			// }],
			events: {
				'change': function(e) {
					sdebug(e.selectedValue[0]);
					// setBillinDayText(e.selectedValue[0]);
				}
			}
		}]

	});
	billingPicker.cancelButton.addEventListener('click', function() {
		win.close();
	});
	billingPicker.doneButton.addEventListener('click', function() {
		win.close();
	});
	// billingPicker.originalValue = billingView.button.text;
	// picker.setSelectedRow(0, 1);
	win.add(billingPicker);
	billingPicker.animate({
		transform: '',
		duration: 300
	});
	win.open();
}

function evaluatorsEx(_args) {
	var win = createWin(_.assign({}, _args)),
		sliderWidth = 0,
		$infoWidgetWidth = 200,
		$thumbHeight = 80,
		thumbWidth = 0,
		sliderOn = false,
		firstLayout = true,
		slider = new Label({
			properties: {
				right: 20,
				left: 10,
				width: 'FILL',
				height: $thumbHeight,
				borderRadius: $thumbHeight / 2,
				textAlign: 'center',
				backgroundColor: '#141419',
			},
			childTemplates: [{
				type: 'Ti.UI.Label',
				bindId: 'thumb',
				properties: {
					width: $thumbHeight,
					height: $thumbHeight,
					backgroundColor: 'blue',
					left: 0,
					text: 's',
					textAlign: 'center',
					borderRadius: $thumbHeight / 2
				},
				events: {
					// 'touchstart': function(e) {
					// e.source.maxX = slider.rect.width - e.source.rect.width;
					// e.source.globalStartX = e.globalPoint.x;
					// },
					'panstart': {
						variables: {
							offset: 'globalPoint.x',
							width: 'source.rect.width',
							parentWidth: 'source.parent.rect.width',
						},
						targets: [{
							properties: {
								globalStartX: '_offset',
								maxX: '_parentWidth - _width'
							}
						}]

					},
					'pan': {
						variables: {
							offset: 'globalPoint.x'
						},
						targets: [{
							targetVariables: {
								maxX: 'maxX',
								globalStartX: 'globalStartX'
							},
							properties: {
								left: 'min(max(_offset - _globalStartX,0),_maxX)'
							}
						}]

					},
					'postlayout': function(e) {
						// if (firstLayout) {
						// firstLayout = false;
						// //important for the java objet to receive the change
						// // e.source.setProperty('maxX', slider.rect.width -
						// e.source.rect.width);
						// e.source.maxX = slider.rect.width -
						// e.source.rect.width;
						// }

						var current = sliderOn;
						sliderOn = e.source.left > e.source.maxX - 10;
						sdebug('postlayout');
						if (current != sliderOn) {
							e.source.applyProperties({
								color: sliderOn ? 'green' : 'red',
							})
						}
					},
					'panend': function(e) {
						sdebug('panend', e.source);
						// if (sliderWidth === 0) {
						if (sliderOn) {
							e.source.left = 0;
							sliderOn = false;
						} else {
							e.source.animate({
								left: 0,
								duration: 200
							});
							sliderOn = false;
						}
						// }
					}
				}
			}]
		})

	var gdebug = _.flow(_.partialRight(_.pick, 'type'), sdebug);
	win.addEventListener('pinch', gdebug)
	win.addEventListener('pan', gdebug)
	win.addEventListener('rotate', gdebug)
	win.addEventListener('shove', gdebug)
	win.addEventListener('doubletap', gdebug)
	win.addEventListener('singletap', gdebug)
	win.addEventListener('longpress', gdebug)
	win.addEventListener('touchstart', gdebug)
	win.addEventListener('touchmove', gdebug)
	win.addEventListener('touchend', gdebug)
	win.addEventListener('twofingertap', gdebug)
	win.addEventListener('click', gdebug)
		// win.add({
		// backgroundColor: 'red',
		// width: 100,
		// height: 100,
		// borderRadius: 3,
		// draggable: {
		// axis: 'x',
		// minLeft: 0,
		// maxRight: '100%',
		// minTop: 0,
		// maxTop: Ti.Platform.displayCaps.platformHeight - 100,
		// }
		// });
	win.add(slider);
	openWin(win);
}

function testElevation(_args) {
	var win = createWin({
		backgroundColor: '#ADCFFF',
		statusBarColor: 'red',
		navigationBarColor: 'green',
		navigationHidden: true,
		fullscreen: true,
		immersive: 2,
		layout: 'vertical'
	}, _args);
	win.add([{
		type: 'Ti.UI.Button',
		top: 10,
		height: 40,
		bottom: 10,
		width: 200,
		title: 'normal'
	}, {
		type: 'Ti.UI.Button',
		top: 10,
		height: 40,
		bottom: 10,
		width: 200,
		elevation: 0,
		title: 'elevation 0'
	}, {
		type: 'Ti.UI.Button',
		top: 10,
		height: 40,
		bottom: 10,
		width: 200,
		backgroundColor: 'green',
		selector: true,
		title: 'custom Background color'
	}, {
		type: 'Ti.UI.Button',
		top: 10,
		height: 40,
		bottom: 10,
		width: 200,
		backgroundColor: 'green',
		borderRadius: 4,
		selector: true,
		title: 'selector'
	}, {
		type: 'Ti.UI.Button',
		top: 10,
		height: 40,
		bottom: 10,
		width: 200,
		backgroundColor: 'green',
		borderRadius: 4,
		clipChildren: false,
		selector: 'red',
		title: 'selector red'
	}, {
		type: 'Ti.UI.View',
		top: 10,
		height: 40,
		bottom: 10,
		width: 200,
		elevation: 2,
		text: 'test elevation',
		backgroundColor: 'white'
	}, {
		type: 'Ti.UI.Label',
		top: 10,
		height: 40,
		bottom: 10,
		width: 200,
		text: 'test no elevation',
		backgroundColor: 'green'
	}]);
	win.open();
}

function animationtest(_args) {
	var win = createWin({
		backgroundColor: 'white',
	}, _args);
	var testview = new View({
		width: 100,
		height: 100,
		backgroundColor: 'red',
		left: 10
	});
	//	win.add(testview);
	var testview2 = new View({
		width: 100,
		height: 100,
		backgroundColor: 'green',
		right: 10,
		childTemplates: [{
			width: 30,
			height: 30,
			right: 10,
			backgroundColor: 'red'
		}]
	});
	win.add(testview2);
	win.addEventListener('click', function() {
		testview2.animate({
			width: 300 - testview2.width,
			duration: 300
		});
		//		testview.animate({
		//			width:300-testview.width,
		//			duration:300
		//		});
	})

	win.open();
}

function gradientAnimationTest(_args) {
	var randomColor = require('randomColor').randomColor;
	var win = createWin({
		backgroundColor: '#E4483E',
		childTemplates: [{
			type: 'Ti.UI.ImageView',
			bindId: 'imageView',
			properties: {
				transition: {
					// duration:200,
					// from:{
					// 	opacity:0
					// },
					// to:{
					// 	opacity:0
					// }
					style: Ti.UI.TransitionStyle.FADE,
					duration: 3000,
					substyle: Ti.UI.TransitionSubStyle.BOTTOM_TO_TOP
				},
				width: '200%',
				height: '200%',
				scaleType: Ti.UI.SCALE_TYPE_ASPECT_FILL
			}
		}, {
			type: 'Ti.UI.ImageView',
			bindId: 'imageView2',
			properties: {
				transition: {
					style: Ti.UI.TransitionSubStyle.FADE,
					duration: 2000,
					substyle: Ti.UI.TransitionSubStyle.BOTTOM_TO_TOP
				},
				transform: 'or180',
				width: 'FILL',
				height: 'FILL',
				scaleType: Ti.UI.SCALE_TYPE_ASPECT_FILL
			}
		}]
	}, _args);

	setInterval(function() {
		Ti.Image.getFilteredImage('images/gradient.png', {
			tint: randomColor.randomColor(),
			callback: function(e) {
				win.imageView.image = e.image;
			}
		});
	}, 10000);
	setInterval(function() {
		Ti.Image.getFilteredImage('images/gradient.png', {
			tint: randomColor.randomColor(),
			callback: function(e) {
				win.imageView2.image = e.image;
			}
		});
	}, 7000);
	openWin(win);
}

// function blendModeTest(_args) {
// 	var childTemplates = [];
// 	_.each(Ti.UI.BlendMode, function(value, key, list){

// 		sdebug(key, value);
// 		childTemplates.push({
// 			type: 'Ti.UI.ImageView',
// 			properties: {
// 				blendMode: key,
// 				image: '/images/userLoc.png',
// 				filterOptions: {
// 					blend: value,
// 					tint: '#ffff0000'
// 				}
// 			}
// 		});
// 	});
// 	var win = createWin({
// 		layout: 'horizontal',
// 		horizontalWrap: true,
// 		childTemplates: childTemplates
// 	}, _args);

// 	openWin(win);
// }
// blendModeTest();
// 

function windowLevelTest() {
	var win = Ti.UI.createWindow({
		lightweight: true,
		touchPassThrough: true,
		backgroundColor: 'transparent',
		childTemplates: [{
			type: 'Ti.UI.View',
			properties: {
				backgroundColor: 'blue',
				bottom: 0,
				height: 30
			},
			events: {
				click: function() {
					win.close();
					win = null;
				}
			}
		}]
	});
	win.open();
}
Ti.App.on('significanttimechange', sdebug);
// setTimeout(windowLevelTest, 1000);
// scrollableViewTest();
var indexer = Ti.App.iOS.createSearchableIndex();
if (indexer.isSupported()) {
	Ti.App.iOS.on('continueactivity', function(e) {
		sdebug(e);
	});
	indexer.addToDefaultSearchableIndex([{
		identifier: "4",
		domainIdentifier: "akylas.sink",
		attributeSet: {
			contentType: 'test',
			title: "Test1",
			contentDescription: "description1",
			supportsPhoneCall: true,
			supportsNavigation: true,
			keywords: ["akylas"]
		}
	}, {
		identifier: "5",
		domainIdentifier: "akylas.sink",
		attributeSet: {
			contentType: 'test',
			title: "Test2",
			contentDescription: "description2",
			// keywords: ["Test2", "akylas"]
		}
	}], function(e) {
		if (e.success) {
			alert("Press the home button and now search for your keywords");
		} else {
			alert("Errored: " + JSON.stringify(e.error));
		}
	});
}