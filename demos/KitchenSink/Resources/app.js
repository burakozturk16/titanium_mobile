var Shape = Ti.Shape;
Ti.include('akylas.animation.js');
var isiOS7 = false;
var isAndroid = Ti.Platform.osname == "android";
var isApple = Ti.Platform.osname === 'ipad' || Ti.Platform.osname === 'iphone';
if (isApple) {
	isiOS7 = parseInt(Titanium.Platform.version.split(".")[0]) >= 7;
}
var backColor = 'white';
var textColor = 'black';
var navGroup;
var openWinArgs;
var html = 'La <font color="red">musique</font> électronique <b>est un type de <big><big>musique</big></big> qui a <font color="green">été conçu à</font></b> partir des années<br> 1950 avec des générateurs de signaux<br> et de sons synthétiques. Avant de pouvoir être utilisée en temps réel, elle a été primitivement enregistrée sur bande magnétique, ce qui permettait aux compositeurs de manier aisément les sons, par exemple dans l\'utilisation de boucles répétitives superposées. Ses précurseurs ont pu bénéficier de studios spécialement équipés ou faisaient partie d\'institutions musicales pré-existantes. La musique pour bande de Pierre Schaeffer, également appelée musique concrète, se distingue de ce type de musique dans la mesure où son matériau primitif était constitué des sons de la vie courante. La particularité de la musique électronique de l\'époque est de n\'utiliser que des sons générés par des appareils électroniques.';
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
		if (_withoutActionBar != true) _win.activity = androidActivitysSettings;
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
				title: 'VerticalScrollView'
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
		borderRadius: 6,
		borderColor: 'orange',
		borderWidth: 2,
		backgroundGradient: {
			type: 'radial',
			colors: ['orange', 'yellow']
		},
		top: 30,
		width: 100,
		height: 100
	});
	var anim1 = Ti.UI.createAnimation({
		duration: 800,
		transform: t1
	});
	anim1.addEventListener('complete', function() {
		view.animate(anim2);
	});
	var anim2 = Ti.UI.createAnimation({
		duration: 800,
		transform: t2
	});
	anim2.addEventListener('complete', function() {
		view.animate(anim3);
	});
	var anim3 = Ti.UI.createAnimation({
		duration: 500,
		transform: t3
	});
	anim3.addEventListener('complete', function() {
		view.animate(anim5);
	});
	var anim4 = Ti.UI.createAnimation({
		duration: 500,
		transform: t4
	});
	anim4.addEventListener('complete', function() {
		gone = true;
	});
	var anim5 = Ti.UI.createAnimation({
		duration: 200,
		bottom: 145,
		top: null
	});
	anim5.addEventListener('complete', function() {
		view.animate(anim6);
	});
	var anim6 = Ti.UI.createAnimation({
		duration: 400,
		transform: t5
	});
	anim6.addEventListener('complete', function() {
		gone = true;
	});

	function onclick() {
		if (gone === true) {
			view.animate({
				duration: 300,
				transform: t0,
				top: 30,
				bottom: null
			}, function() {
				gone = false;
			});
		} else view.animate(anim1);
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
		borderRadius: 12,
		borderColor: 'green',
		borderWidth: 2,
		opacity: 0,
		width: 200,
		height: 200
	});
	view.add(Ti.UI.createView({
		backgroundColor: 'blue',
		bottom: 0,
		width: Ti.UI.FILL,
		height: 50
	}));
	var showMe = function() {
		view.opacity = 0;
		view.transform = t;
		win.add(view);
		animation.fadeIn(view, 100);
		animation.popIn(view);
	};
	var hideMe = function(_callback) {
		animation.fadeOut(view, 200, function() {
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
			duration: 300,
			transform: t0,
			opacity: 1
		});
	};
	var hideMe = function(_callback) {
		view.animate({
			duration: 300,
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
		if (showItemIndex != -1 && showItemSection != null) {
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
						fontSize: 20,
						fontWeight: 'bold'
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
	var rotate = Ti.UI.create2DMatrix().rotate(90);
	var counterRotate = rotate.rotate(-180);
	var scrollView = Titanium.UI.createScrollableView({
		views: [Titanium.UI
			.createImageView({
				image: 'default_app_logo.png',
				transform: counterRotate
			}),
			Titanium.UI.createImageView({
				image: 'KS_nav_ui.png',
				transform: counterRotate
			}),
			Titanium.UI.createImageView({
				image: 'KS_nav_views.png',
				transform: counterRotate
			})
		],
		showPagingControl: true,
		transform: rotate
	});
	win.add(scrollView);
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
		height: Ti.UI.FILL,
		layout: 'horizontal',
		horizontalWrap: false
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
		// top:10,
		height: 80,
		left: 10,
		right: 4
	});
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
		backgroundColor: 'purple',
		width: Ti.UI.FILL,
		height: Ti.UI.FILL,
		bottom: 6,
		right: 4
	});
	view.add(view3);
	win.add(view);
	win.addEventListener('click', function(e) {
		view2.animate({
			duration: 600,
			autoreverse: true,
			width: Ti.UI.FILL,
			height: 100,
			// top:null,
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
		top: 50,
		titlePadding: {
			left: 30,
			top: 30,
			bottom: 30,
			right: 30
		},
		height: 50,
		bubbleParent: false,
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
	button.addEventListener('touchstart', function(e) {
		alert('stste');
	});
	win.add(button);
	var label = Ti.UI.createLabel({
		bottom: 20,
		// dispatchPressed: true,
		backgroundColor: 'gray',
		backgroundSelectedColor: '#a46',
		textPadding: {
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
			duration: 500,
			transform: varSwitch(label.transform, t3, t1),
		});
	});
	win.add(label);
	var button2 = Ti.UI.createButton({
		titlePadding: {
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

function pullToRefresh() {
	var win = createWin();
	var sections = [];

	var fruitSection = Ti.UI.createListSection({
		headerTitle: 'Fruits'
	});
	var fruitDataSet = [
		{
			properties: {
				title: 'Apple'
			}
		},
		{
			properties: {
				title: 'Banana'
			}
		},
		{
			properties: {
				title: 'Cantaloupe'
			}
		},
		{
			properties: {
				title: 'Fig'
			}
		},
		{
			properties: {
				title: 'Guava'
			}
		},
		{
			properties: {
				title: 'Kiwi'
			}
		}
	];
	fruitSection.setItems(fruitDataSet);
	sections.push(fruitSection);

	var vegSection = Ti.UI.createListSection({
		headerTitle: 'Vegetables'
	});
	var vegDataSet = [
		{
			properties: {
				title: 'Carrots'
			}
		},
		{
			properties: {
				title: 'Potatoes'
			}
		},
		{
			properties: {
				title: 'Corn'
			}
		},
		{
			properties: {
				title: 'Beans'
			}
		},
		{
			properties: {
				title: 'Tomato'
			}
		}
	];
	vegSection.setItems(vegDataSet);

	var fishSection = Ti.UI.createListSection({
		headerTitle: 'Fish'
	});
	var fishDataSet = [
		{
			properties: {
				title: 'Cod'
			}
		},
		{
			properties: {
				title: 'Haddock'
			}
		},
		{
			properties: {
				title: 'Salmon'
			}
		},
		{
			properties: {
				title: 'Tuna'
			}
		}
	];
	fishSection.setItems(fishDataSet);

	var refreshCount = 0;

	function getFormattedDate() {
		var date = new Date();
		return date.getMonth() + '/' + date.getDate() + '/' + date.getFullYear() + ' ' + date.getHours() + ':' + date.getMinutes();
	}

	function resetPullHeader() {
		actInd.hide();
		imageArrow.transform = Ti.UI.create2DMatrix();
		if (refreshCount < 2) {
			imageArrow.show();
			labelStatus.text = 'Pull down to refresh...';
			labelLastUpdated.text = 'Last Updated: ' + getFormattedDate();
		} else {
			// labelStatus.text = 'Nothing To Refresh';
			// labelLastUpdated.text = 'Go Cook Something';
			// listView.removeEventListener('pull', pullListener);
			// listView.removeEventListener('pullend', pullendListener);
			// eventStatus.text = 'Removed event listeners.';
		}
		listView.closePullView();
	}

	function loadTableData() {
		if (refreshCount == 0) {
			listView.appendSection(vegSection);
		} else if (refreshCount == 1) {
			listView.appendSection(fishSection);
		}
		refreshCount ++;
		resetPullHeader();
	}
	var currentActive;

	function pullListener(e) {
		if (e.active === currentActive) return;
		currentActive = e.active;
		eventStatus.text = 'EVENT pull FIRED. e.active = ' + e.active;
		if (e.active == false) {
			var unrotate = Ti.UI.create2DMatrix();
			imageArrow.animate({
				transform: unrotate,
				duration: 180
			});
			labelStatus.text = 'Pull down to refresh...';
		} else {
			var rotate = Ti.UI.create2DMatrix().rotate(180);
			imageArrow.animate({
				transform: rotate,
				duration: 180
			});
			if (refreshCount == 0) {
				labelStatus.text = 'Release to get Vegetables...';
			} else {
				labelStatus.text = 'Release to get Fish...';
			}
		}
	}

	function pullendListener(e) {
		if (e.active == false) {
			resetPullHeader();
			return;
		}
		eventStatus.text = 'EVENT pullend FIRED.';

		if (refreshCount == 0) {
			labelStatus.text = 'Loading Vegetables...';
		} else {
			labelStatus.text = 'Loading Fish...';
		}
		imageArrow.hide();
		actInd.show();
		listView.showPullView();
		setTimeout(function() {
			loadTableData();
		}, 2000);
	}

	var tableHeader = Ti.UI.createView({
		backgroundColor: '#e2e7ed',
		width: Ti.UI.FILL,
		height: 80
	});

	var border = Ti.UI.createView({
		backgroundColor: '#576c89',
		bottom: 0,
		height: 2
	});
	tableHeader.add(border);

	var imageArrow = Ti.UI.createImageView({
		image: 'https://github.com/appcelerator/titanium_mobile/raw/master/demos/KitchenSink/Resources/images/whiteArrow.png',
		left: 20,
		bottom: 10,
		width: 23,
		height: 60
	});
	tableHeader.add(imageArrow);

	var labelStatus = Ti.UI.createLabel({
		color: '#576c89',
		font: {
			fontSize: 13,
			fontWeight: 'bold'
		},
		text: 'Pull down to refresh...',
		textAlign: 'center',
		left: 55,
		bottom: 30,
		width: 200
	});
	tableHeader.add(labelStatus);

	var labelLastUpdated = Ti.UI.createLabel({
		color: '#576c89',
		font: {
			fontSize: 12
		},
		text: 'Last Updated: ' + getFormattedDate(),
		textAlign: 'center',
		left: 55,
		bottom: 15,
		width: 200
	});
	tableHeader.add(labelLastUpdated);

	var actInd = Ti.UI.createActivityIndicator({
		left: 20,
		bottom: 13,
		width: 30,
		height: 30
	});
	tableHeader.add(actInd);

	var listView = Ti.UI.createListView({
		height: '90%',
		top: 0,
		rowHeight: 50,
		sections: sections,
		pullView: tableHeader
	});

	listView.addEventListener('pull', pullListener);

	listView.addEventListener('pullend', pullendListener);

	var eventStatus = Ti.UI.createLabel({
		font: {
			fontSize: 13,
			fontWeight: 'bold'
		},
		text: 'Event data will show here',
		bottom: 0,
		height: '10%'
	})

	win.add(listView);
	win.add(eventStatus);
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
		titlePadding: {
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
		image: '/images/slightlylargerimage.png',
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
				textPadding: {
					bottom: 4
				},
				right: 55,
				touchEnabled: false,
				height: Ti.UI.FILL,
				color: 'black',
				font: {
					fontSize: 16
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
			// var image = Ti.Image.getFilteredViewToImage(win, Ti.Image.FILTER_GAUSSIAN_BLUR, {scale:0.3});
			blurImage.image = image;
			animation.fadeIn(contactAction, 300);
		}
	});
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

	function formatTitle(_history) {
		return _history.fullName + '<br><small><small><b><font color="#5B5B5B">' + (new Date()).toString() + '</font> <font color="#3FAC53"></font></b></small></small>';
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
				outgoing: !! random(0, 1),
				wifi: !! random(0, 1)
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
					fontSize: '20dp',
					fontWeight: 'bold'
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
					fontSize: '14dp'
				},
				left: '60dp',
				top: '25dp',
			}
		}]
	};
	var listView = Ti.UI.createListView({
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

function listViewEx() {
	var win = createWin();
	var listview = Ti.UI
		.createListView({
			allowsSelection: false,
			rowHeight: 50,
			selectedBackgroundGradient: {
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
			},
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
		contentHeight: Ti.UI.SIZE
	});
	scrollView.add(Ti.UI.createLabel({
		width: Ti.UI.FILL,
		height: Ti.UI.SIZE,
		bottom: 20,
		html: html
	}));
	scrollView.add(Ti.UI.createLabel({
		multiLineEllipsize: Ti.UI.TEXT_ELLIPSIZE_HEAD,
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
		titlePadding: {
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
					height: 50,
					image: '/images/gradients.svg'
				}
			}, {
				type: 'Ti.UI.Label',
				bindId: 'info',
				properties: {
					color: textColor,
					touchEnabled: false,
					font: {
						fontSize: 20,
						fontWeight: 'bold'
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
		cancel: optionTitles.length-1,
		options: optionTitles,
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
		width: Ti.UI.FILL,
		height: 60
	});
	var view1 = Ti.UI.createView({
		layout: 'vertical'
	});
	var view2 = Ti.UI.createView({
		height: '65%',
		layout: 'horizontal',
		width: Ti.UI.FILL
	});
	var view3 = Ti.UI.createLabel({
		text: 'This is my tutle test',
		top: 2,
		ellipsize: Ti.UI.TEXT_ELLIPSIZE_TAIL,
		font: {
			fontSize: 14
		},
		width: Ti.UI.FILL
	});
	var view4 = Ti.UI.createLabel({
		color: 'white',
		text: 'test',
		textPadding: {
			left: 4,
			right: 4,
			bottom: 2
		},
		shadowColor: '#55000000',
		shadowRadius: 2,
		font: {
			fontSize: 12,
			fontWeight: 'bold'
		},
		backgroundColor: 'red',
		borderRadius: 4,
		right: 4
	});
	var view5 = Ti.UI.createView({
		height: Ti.UI.FILL,
		layout: 'horizontal',
		width: Ti.UI.FILL
	});
	var view6 = Ti.UI.createLabel({
		font: {
			fontSize: 12
		},
		color: 'black',
		bottom: 2
	});
	var view7 = Ti.UI.createLabel({
		font: {
			fontSize: 12
		},
		text: 'date',
		color: 'black',
		bottom: 2,
		textAlign: 'right',
		right: 4
	});
	view5.add(view6);
	view5.add(view7);
	view2.add(view3);
	view2.add(view4);
	view1.add(view2);
	view1.add(view5);
	view.add(view1);
	win.add(view);
	win.open();
}

function listViewLayout() {
	var win = createWin();
	var template = {
		properties : {
			layout : 'horizontal',
			height : 60
		},
		childTemplates : [{
			type : 'Ti.UI.Button',
			bindId : 'button',
			properties : {
				width : 40,
				height : 40,
				left : 4,
				right : 4,
				font : {
					fontSize : 18,
					fontWeight : 'bold'
				},
				borderRadius : 10,
				color : 'white',
				selectedColor : 'black'
			}
		}, {
			type : 'Ti.UI.View',
			properties : {
				width : Ti.UI.FILL,
				height : Ti.UI.FILL,
				layout : 'vertical'
			},
			childTemplates : [{
				type : 'Ti.UI.View',
				properties : {
					layout : 'horizontal',
					backgroundColor:'blue',
					width : Ti.UI.FILL,
					height : Ti.UI.FILL
				},
				childTemplates : [{
					type : 'Ti.UI.Label',
					bindId : 'tlabel',
					properties : {
						top : 2,
						ellipsize : Ti.UI.TEXT_ELLIPSIZE_TAIL,
						font : {
							fontSize : 14
						},
						width : Ti.UI.FILL
					}
				}, {
					type : 'Ti.UI.Label',
					bindId : 'plabel',
					properties : {
						color : 'white',
						textPadding : {
							left : 4,
							right : 4,
							bottom : 2
						},
						shadowColor : '#55000000',
						selectedColor : 'green',
						shadowRadius : 2,
						borderRadius : 4,
						font : {
							fontSize : 12,
							fontWeight : 'bold'
						},
						backgroundColor : 'red',
						right : 4
					}
				}]
			}, {
				type : 'Ti.UI.View',
				properties : {
					layout : 'horizontal',
					width : Ti.UI.FILL,
					backgroundColor:'yellow',
					height : 16,
					top : 2,
					bottom : 6

				},
				childTemplates : [{
					type : 'Ti.UI.View',
					properties : {
						width : Ti.UI.FILL,
						backgroundColor : '#e9e9e9',
						borderRadius : 4
					},
					childTemplates : [{
						type : 'Ti.UI.View',
						bindId : 'progressbar',
						properties : {
							left : 0,
							height : Ti.UI.FILL,
							backgroundColor:'green'
						}
					}, {
						type : 'Ti.UI.Label',
						bindId : 'sizelabel',
						properties : {
							color : 'black',
							shadowColor : '#55ffffff',
							shadowRadius : 2,
							font : {
								fontSize : 12
							}
						}
					}]
				}, {
					type : 'Ti.UI.Label',
					bindId : 'timelabel',
					properties : {
						font : {
							fontSize : 12
						},
						color : 'black',
						textAlign : 'right',
						right : 4,
						width : 80
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
		'test',
		'processing'
	];
	var listView = createListView({
		templates: {
			'template': template
		},
		defaultItemTemplate: 'template'
	});
	var items = [];
	for (var i = 0; i < 100; i++) {
		items.push({
			tlabel: {
				text: names[Math.floor(Math.random() * names.length)]
			},
			plabel: {
				text: priorities[Math.floor(Math.random() * priorities.length)]
			},
			sizelabel: {
				text: 'size'
			},
			timelabel: {
				text: (new Date()).toString()
			},
			progressbar: {
				width: Math.floor(Math.random() * 100) +'%'
			}
		});
	}
	listView.setSections([{
		items: items
	}]);
	win.add(listView);
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

	var holderHolder  = Ti.UI.createView({
		// clipChildren:false,
		height: 100,
		borderColor:'green',
		width: 220,
		backgroundColor:'green'
	});
	var transitionViewHolder = Ti.UI.createView({
		clipChildren:false,
		height: 80,
		width: 200,
		borderColor:'green',
		// borderRadius: 10,
		backgroundColor: 'red'
	});
	var tr1 = Ti.UI.createLabel({
		text: 'I am a text!',
		color: '#fff',
		textAlign: 'center',
		backgroundColor: 'green',
		width: 50,
		height: 40,
	});
	tr1.addEventListener('click', function(e) {
		Ti.API.info('click');
		transitionViewHolder.transitionViews(tr1, tr2, {
			style: Ti.UI.TransitionStyle.CUBE,
			duration: 3000,
			reverse:true
		});
	});
	var tr2 = Ti.UI.createButton({
		title: 'I am a button!',
		color: '#000',
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
		backgroundColor:'yellow',
		image: "animation/win_1.png"
	});
	image1.addEventListener('longpress', function(){
		image1.animate({
			opacity:0,
			autoreverse:true,
			duration:2000,
		});
	});

	var button = Ti.UI.createButton({
		top: 50,
		titlePadding: {
			left: 30,
			top: 30,
			bottom: 30,
			right: 30
		},
		height: 50,
		bubbleParent: false,
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
		right: 0,
		width: 35,
		height: Ti.UI.FILL
	}));
	var t1 = Ti.UI.create2DMatrix();
	var t2 = Ti.UI.create2DMatrix().scale(2.0, 2.0).translate(0, 40).rotate(90);
	button.addEventListener('longpress', function(e) {
		button.animate({
			opacity:0,
			autoreverse:true,
			duration:2000,
		});
	});
	win.add(button);
	var label = Ti.UI.createLabel({
		bottom: 20,
		// dispatchPressed: true,
		backgroundColor: 'gray',
		backgroundSelectedColor: '#a46',
		textPadding: {
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
			opacity:0,
			autoreverse:true,
			duration:2000,
		});
	});
	win.add(label);
	var button2 = Ti.UI.createButton({
		titlePadding: {
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


function imageViewTests(){
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
		backgroundColor:'yellow',
		image: "animation/win_1.png",
		width:100,
		transition:{
			style: Ti.UI.TransitionStyle.FLIP,
			// substyle:Ti.UI.TransitionStyle.TOP_TO_BOTTOM
		}
	});
	win.add(image1);
	image1.addEventListener('click', function(){
			image1.image = "animation/win_5.png";
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
		backgroundColor:'yellow',
		width:100,
		transition:{
			style: Ti.UI.TransitionStyle.FADE,
		},
		image:'http://zapp.trakt.us/images/posters_movies/192263-138.jpg',
		animatedImages: ["animation/win_1.png", "animation/win_2.png", "animation/win_3.png", "animation/win_4.png",
						"animation/win_5.png", "animation/win_6.png", "animation/win_7.png", "animation/win_8.png",
						"animation/win_9.png", "animation/win_10.png", "animation/win_11.png", "animation/win_12.png",
						"animation/win_13.png", "animation/win_14.png", "animation/win_15.png", "animation/win_16.png"],
		duration:100,
		viewMask: '/images/body-mask.png'
	});
	win.add(image1);
	var bthHolder = Ti.UI.createView({left:0, layout:'vertical', height:Ti.UI.SIZE, width:Ti.UI.SIZE, backgroundColor:'green'});
	var btn = Ti.UI.createButton({title:'start'});
	btn.addEventListener('singletap', function(){image1.start();});
	bthHolder.add(btn);
	btn = Ti.UI.createButton({title:'pause'});
	btn.addEventListener('singletap', function(){image1.pause();});
	bthHolder.add(btn);
	btn = Ti.UI.createButton({title:'resume'});
	btn.addEventListener('singletap', function(){image1.resume();});
	bthHolder.add(btn);
	btn = Ti.UI.createButton({title:'playpause'});
	btn.addEventListener('singletap', function(){image1.pauseOrResume();});
	bthHolder.add(btn);
	btn = Ti.UI.createButton({title:'stop'});
	btn.addEventListener('singletap', function(){image1.stop();});
	bthHolder.add(btn);
	btn = Ti.UI.createButton({title:'reverse'});
	btn.addEventListener('singletap', function(){image1.reverse = !image1.reverse;});
	bthHolder.add(btn);
	btn = Ti.UI.createButton({title:'autoreverse'});
	btn.addEventListener('singletap', function(){image1.autoreverse = !image1.autoreverse;});
	bthHolder.add(btn);
	win.add(bthHolder);
	openWin(win);
}

var firstWindow = createWin({});
var listview = createListView();
var color = cellColor(0);
listview.sections = [{
	items: [{
		properties: {
			title: 'Transform',
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
				title: 'PullToRefresh'
			},
			callback: pullToRefresh
		},{
		properties: {
			title: 'ListView'
		},
		callback: listViewEx
	}, {
		properties: {
			title: 'ListView2'
		},
		callback: listView2Ex
	}]
}];
firstWindow.add(listview);
var mainWin = Ti.UI.createNavigationWindow({
	backgroundColor: backColor,
	exitOnClose: true,
	window: firstWindow,
	transition:{
		style: Ti.UI.TransitionStyle.CUBE
	}
});
mainWin.addEventListener('openWindow', function(e) {
	Ti.API.info(e)
});
mainWin.addEventListener('closeWindow', function(e) {
	Ti.API.info(e)
});
mainWin.open();