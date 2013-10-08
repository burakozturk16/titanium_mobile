var Shape = Ti.Shape;
Ti.include('akylas.animation.js');
var isAndroid = Ti.Platform.osname == "android";
var backColor = 'white';
var textColor = 'black';
var navGroup;
var openWinArgs;
var html = 'La <font color="red">musique</font> électronique <b>est un type de <big><big>musique</big></big> qui a <font color="green">été conçu à</font></b> partir des années<br> 1950 avec des générateurs de signaux<br> et de sons synthétiques. Avant de pouvoir être utilisée en temps réel, elle a été primitivement enregistrée sur bande magnétique, ce qui permettait aux compositeurs de manier aisément les sons, par exemple dans l\'utilisation de boucles répétitives superposées. Ses précurseurs ont pu bénéficier de studios spécialement équipés ou faisaient partie d\'institutions musicales pré-existantes. La musique pour bande de Pierre Schaeffer, également appelée musique concrète, se distingue de ce type de musique dans la mesure où son matériau primitif était constitué des sons de la vie courante. La particularité de la musique électronique de l\'époque est de n\'utiliser que des sons générés par des appareils électroniques.';

if (isAndroid) {
	backColor = 'black';
	textColor = 'gray';
}

function merge_options(obj1, obj2) {
	for ( var attrname in obj2) {
		obj1[attrname] = obj2[attrname];
	}
	return obj1;
}

function createWin(_args) {
	return Ti.UI.createWindow(merge_options({backgroundColor:backColor,
			autoAdjustScrollViewInsets:true,
			extendEdges:[Ti.UI.EXTEND_EDGE_ALL],
		translucent:true,
		orientationModes:[Ti.UI.UPSIDE_PORTRAIT,
				Ti.UI.PORTRAIT,
				Ti.UI.LANDSCAPE_RIGHT,
				Ti.UI.LANDSCAPE_LEFT]}, _args));
}

function createListView(_args) {
	var realArgs = merge_options({allowsSelection:false,
		rowHeight:50,
		selectedBackgroundGradient:{type:'linear',
			colors:[{color:'#1E232C', offset:0.0},
					{color:'#3F4A58', offset:0.2},
					{color:'#3F4A58', offset:0.8},
					{color:'#1E232C', offset:1}],
			startPoint:{x:0, y:0},
			endPoint:{x:0, y:"100%"}}}, _args);

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
	return (_var === _val1)?_val2:_val1;
}

var androidActivitysSettings = {actionBar:{displayHomeAsUp:true,
	onHomeIconItemSelected:function(e) {
		e.window.close();
	}}};
function openWin(_win, _withoutActionBar) {
	if (isAndroid) {
		if (_withoutActionBar != true) _win.activity = androidActivitysSettings;
	} else {
		// if (!navGroup) {
		// 	navGroup = Titanium.UI.iPhone.createNavigationGroup({window:_win});
		// 	var winHolder = createWin();
		// 	winHolder.add(navGroup);
		// 	winHolder.open();
		// } else navGroup.open(_win);
	}
	mainWin.push(_win);
}

function transformExs() {
	var win = createWin();
	var listview = createListView();
	listview.sections = [{items:[{properties:{title:'Transform', backgroundColor:cellColor(1)}, callback:transform1Ex},
			{properties:{title:'TransformAnimated'}, callback:transform2Ex},
			{properties:{title:'PopIn'}, callback:transform3Ex},
			{properties:{title:'SlideIn'}, callback:transform4Ex},
			{properties:{title:'ListView'}, callback:transform5Ex},
			{properties:{title:'VerticalScrollView'}, callback:transform6Ex}]}];
	win.add(listview);
	openWin(win);
}

function transform1Ex() {
	var win = createWin();
	var button = Ti.UI.createButton({top:20, bubbleParent:false, title:'test buutton'});
	var t1 = Ti.UI.create2DMatrix();
	var t2 = t1.scale(2.0, 2.0).translate(0, 40).rotate(90);
	button.addEventListener('longpress', function(e) {
		button.animate({duration:500, transform:varSwitch(button.transform, t2, t1),});
	});
	win.add(button);
	var label = Ti.UI.createLabel({bottom:20,
		backgroundColor:'gray',
		backgroundSelectedColor:'#ddd',
		bubbleParent:false,
		text:'This is a sample\n text for a label'});
	var t3 = t1.scale(2.0, 2.0).translate(0, -40).rotate(90);
	label.addEventListener('longpress', function(e) {
		label.animate({duration:500, transform:varSwitch(label.transform, t3, t1),});
	});
	win.add(label);
	openWin(win);
}

function transform2Ex() {
	var gone = false;
	var win = createWin();
	var t0 = Ti.UI.create2DMatrix({anchorPoint:{x:0, y:"100%"}});
	var t1 = t0.rotate(30);
	var t2 = t0.rotate(145);
	var t3 = t0.rotate(135);
	var t4 = t0.translate(0, "100%").rotate(125);
	var t5 = Ti.UI.create2DMatrix().translate(0, ((Math.sqrt(2)) * 100)).rotate(180);
	var view = Ti.UI.createView({transform:t0,
		borderRadius:6,
		borderColor:'orange',
		borderWidth:2,
		backgroundGradient:{type:'radial', colors:['orange', 'yellow']},
		top:30,
		width:100,
		height:100});
	var anim1 = Ti.UI.createAnimation({duration:800, transform:t1});
	anim1.addEventListener('complete', function() {
		view.animate(anim2);
	});
	var anim2 = Ti.UI.createAnimation({duration:800, transform:t2});
	anim2.addEventListener('complete', function() {
		view.animate(anim3);
	});
	var anim3 = Ti.UI.createAnimation({duration:500, transform:t3});
	anim3.addEventListener('complete', function() {
		view.animate(anim5);
	});
	var anim4 = Ti.UI.createAnimation({duration:500, transform:t4});
	anim4.addEventListener('complete', function() {
		gone = true;
	});
	var anim5 = Ti.UI.createAnimation({duration:200, bottom:145, top:null});
	anim5.addEventListener('complete', function() {
		view.animate(anim6);
	});
	var anim6 = Ti.UI.createAnimation({duration:400, transform:t5});
	anim6.addEventListener('complete', function() {
		gone = true;
	});
	function onclick() {
		if (gone === true) {
			view.animate({duration:300, transform:t0, top:30, bottom:null}, function() {
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
	var view = Ti.UI.createView({backgroundColor:'red',
		borderRadius:12,
		borderColor:'green',
		borderWidth:2,
		opacity:0,
		width:200,
		height:200});
	view.add(Ti.UI.createView({backgroundColor:'blue', bottom:0, width:Ti.UI.FILL, height:50}));
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
	var button = Ti.UI.createButton({top:10, width:100, bubbleParent:false, title:'test buutton'});
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
	var view = Ti.UI.createView({backgroundColor:'red',
		opacity:0,
		transform:t1,
		width:200,
		height:200});
	view.add(Ti.UI.createView({backgroundColor:'blue', bottom:10, width:50, height:50}));
	var showMe = function() {
		view.transform = t1;
		win.add(view);
		view.animate({duration:300, transform:t0, opacity:1});
	};
	var hideMe = function(_callback) {
		view.animate({duration:300, transform:t2, opacity:0}, function() {
			win.remove(view);
		});
	};
	var button = Ti.UI.createButton({top:10, width:100, bubbleParent:false, title:'test buutton'});
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
	var myTemplate = {childTemplates:[{type:'Ti.UI.View',
		bindId:'holder',
		properties:{width:Ti.UI.FILL,
			height:Ti.UI.FILL,
			touchEnabled:false,
			layout:'horizontal',
			horizontalWrap:false},
		childTemplates:[{type:'Ti.UI.ImageView',
			bindId:'pic',
			properties:{touchEnabled:false, width:50, height:50}},
				{type:'Ti.UI.Label',
					bindId:'info',
					properties:{color:textColor,
						touchEnabled:false,
						font:{fontSize:20, fontWeight:'bold'},
						width:Ti.UI.FILL,
						left:10}},
				{type:'Ti.UI.Button',
					bindId:'button',
					properties:{title:'menu', left:10},
					events:{'click':function(_event) {
						if (_event.hasOwnProperty('section') && _event.hasOwnProperty('itemIndex')) {
							hideMenu();
							var item = _event.section.getItemAt(_event.itemIndex);
							item.menu = {transform:t0, opacity:1};
							showItemIndex = _event.itemIndex;
							showItemSection = _event.section;
							_event.section.updateItemAt(_event.itemIndex, item);
						}
					}}}]},
			{type:'Ti.UI.Label',
				bindId:'menu',
				properties:{color:'white',
					text:'I am the menu',
					backgroundColor:'#444',
					width:Ti.UI.FILL,
					height:Ti.UI.FILL,
					opacity:0,
					transform:t1},
				events:{'click':hideMenu}}]};

	var listView = createListView({templates:{'template':myTemplate},
		defaultItemTemplate:'template'});

	var sections = [{headerTitle:'Fruits / Frutas',
		items:[{info:{text:'Apple'}},
				{properties:{backgroundColor:'red'}, info:{text:'Banana'}, pic:{image:'banana.png'}}]},
			{headerTitle:'Vegetables / Verduras',
				items:[{info:{text:'Carrot'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}}]},
			{headerTitle:'Grains / Granos', items:[{info:{text:'Corn'}}, {info:{text:'Rice'}}]}];
	listView.setSections(sections);
	win.add(listView);
	openWin(win);
}

function transform6Ex() {
	var win = createWin();
	var rotate = Ti.UI.create2DMatrix().rotate(90);
	var counterRotate = rotate.rotate(-180);

	var scrollView = Titanium.UI.createScrollableView({views:[Titanium.UI
			.createImageView({image:'default_app_logo.png', transform:counterRotate}),
			Titanium.UI.createImageView({image:'KS_nav_ui.png', transform:counterRotate}),
			Titanium.UI.createImageView({image:'KS_nav_views.png', transform:counterRotate})],
		showPagingControl:true,
		transform:rotate});

	win.add(scrollView);
	openWin(win);
}

function layoutExs() {
	var win = createWin();
	var listview = createListView();
	listview.sections = [{items:[{properties:{title:'Animated Horizontal'}, callback:layout1Ex}]}];
	win.add(listview);
	openWin(win);
}

function layout1Ex() {
	var win = createWin();
	var view = Ti.UI.createView({backgroundColor:'green',
		width:Ti.UI.FILL,
		height:Ti.UI.SIZE,
		layout:'horizontal',
		horizontalWrap:false});
	var view1 = Ti.UI.createView({backgroundColor:'red', width:60, height:80, left:0});
	var view2 = Ti.UI.createView({backgroundColor:'blue', width:20,
		borderColor:'red',
		borderWidth:2,
	// top:10,
	height:80, left:10, right:4});
	var view3 = Ti.UI.createView({backgroundColor:'orange', width:Ti.UI.FILL, height:60, right:4});

	view.add(view1);
	view.add(view2);
	view.add(view3);
	win.add(view);
	win.addEventListener('click', function(e) {
		view2.animate({duration:600, autoreverse:true, width:Ti.UI.FILL, height:100,
		// top:null,
		left:0, right:30});
	});
	openWin(win);
}

function shapeExs() {
	var win = createWin();
	var listview = createListView();
	listview.sections = [{items:[{properties:{title:'Arc'}, callback:shape1Ex},
			{properties:{title:'Circle'}, callback:shape2Ex},
			{properties:{title:'Line'}, callback:shape3Ex},
			{properties:{title:'Inversed'}, callback:shape4Ex},
			{properties:{title:'Shutter'}, callback:shape5Ex},
			{properties:{title:'Inner Shadow'}, callback:shape6Ex},
			{properties:{title:'PieSlice'}, callback:shape7Ex}]}];
	win.add(listview);
	openWin(win);
}

function shape1Ex() {
	var win = createWin();
	var view = Shape.createView({bubbleParent:false, width:200, height:200});
	view.add({lineColor:'#777',
		lineWidth:10,
		lineCap:Shape.CAP_ROUND,
		transform:Ti.UI.create2DMatrix().rotate(5),
		lineShadow:{color:'white'},
		operations:[{type:'arc', radius:'45%', startAngle:-160, sweepAngle:320}]});
	var shape = Shape.createArc({radius:'45%',
		startAngle:-160,
		sweepAngle:190,
		lineWidth:10,
		lineCap:Shape.CAP_ROUND,
		lineGradient:{type:'sweep',
			colors:[{color:'orange', offset:0},
					{color:'red', offset:0.19},
					{color:'red', offset:0.25},
					{color:'blue', offset:0.25},
					{color:'blue', offset:0.31},
					{color:'green', offset:0.55},
					{color:'yellow', offset:0.75},
					{color:'orange', offset:1}]}});

	view.add(shape);
	view.addEventListener('click', function(e) {
		shape.cancelAllAnimations();
		// shape.sweepAngle = 320;
		shape.animate({duration:600, autoreverse:true, sweepAngle:320});
	});
	win.add(view);
	openWin(win);
}

function shape2Ex() {
	var win = createWin();
	var view = Shape.createView({top:150,
		borderRadius:10,
		borderColor:'red',
		borderWidth:5,
		bubbleParent:false,
		width:300,
		height:100,
		backgroundColor:'white',
		transform:Ti.UI.create2DMatrix().scale(1.5, 1.5),
		viewMask:'/images/body-mask.png'});
	var shape = Shape.createCircle({fillColor:'#bbb',
		lineColor:'#777',
		lineWidth:1,
		fillImage:'/images/pattern.png',
		transform:Ti.UI.create2DMatrix().scale(0.5, 1),
		lineShadow:{radius:2, color:'black'},
		radius:'40%'});
	view.add(shape);
	view.add(Ti.UI.createView({backgroundColor:'red', bottom:10, width:30, height:30}));
	shape.addEventListener('click', function(e) {
		e.source.cancelAllAnimations();
		e.source.animate({duration:400,
			lineWidth:20,
			autoreverse:true,
			lineColor:'yellow',
			fillColor:'blue'});
	});
	win.add(view);
	openWin(win);
}

function shape3Ex() {
	var win = createWin();
	var view = Shape.createView({bubbleParent:false, width:Ti.UI.FILL, height:200});
	var shape = Shape.createLine({lineColor:'blue',
		lineWidth:6,
		retina:false,
		antialiasing:true,
		lineCap:Shape.CAP_BUTT,
		lineJoin:Shape.JOIN_ROUND,
		lineShadow:{radius:3, color:'blue'},
		lineImage:'/images/pattern.png',
		// lineDash:{
		// 	phase:0,
		// 	pattern:[10,2,10]
		// },
		points:[['0%', 0],
				['20%', 20, '20%', 10, '10%', 30],
				['40%', -5],
				['60%', 8],
				['80%', 16],
				['100%', 0]]});
	view.add(shape);
	view.addEventListener('click', function(e) {
		shape.animate({duration:400,
			lineWidth:20,
			autoreverse:true,
			lineColor:'yellow',
			points:[['0%', 30],
					['10%', 40, '20%', 10, '10%', 30],
					['40%', 25],
					['60%', -38],
					['80%', 56],
					['100%', 0]]});
	});
	win.add(view);
	openWin(win);
}

function shape4Ex() {
	var win = createWin();
	win.add(Ti.UI.createLabel({width:Ti.UI.FILL, height:Ti.UI.FILL, bottom:20, html:html}));
	var view = Shape.createView({width:Ti.UI.FILL, height:Ti.UI.FILL, bubbleParent:false});
	var shape = Shape.createCircle({fillColor:'transparent',
		lineColor:'#777',
		lineWidth:1,
		retina:false,
		antialiasing:false,
		fillGradient:{type:'radial', colors:['transparent', 'gray'],
		// startPoint:{x:0, y:0},
		// endPoint:{x:0, y:"100%"}
		},
		fillInversed:true,
		fillColor:'blue',
		fillShadow:{radius:5, color:'black'},
		radius:'20%'});
	view.add(shape);
	shape.addEventListener('click', function(e) {
		e.source.cancelAllAnimations();
		e.source.animate({duration:400,
			lineWidth:20,
			radius:'40%',
			fillOpacity:0.7,
			autoreverse:true,
			lineColor:'yellow',
			fillColor:'blue'});
	});
	win.add(view);
	openWin(win);
}

function shape5Ex() {
	var win = createWin();
	win.add(Ti.UI.createLabel({width:Ti.UI.FILL, height:Ti.UI.FILL, bottom:20, html:html}));
	var view = Shape.createView({width:Ti.UI.FILL, height:Ti.UI.FILL, bubbleParent:false});
	var shape = Shape.createRoundedRect({cornerRadius:10,
		// lineColor:'#777',
		// lineWidth:4,
		retina:false,
		antialiasing:false,
		fillGradient:{type:'radial', colors:['white', 'gray']},
		fillInversed:true,
		fillColor:'blue',
		fillShadow:{radius:5, color:'black'},
		transform:Ti.UI.create2DMatrix().scale(0.0003)});
	view.add(shape);
	view.addEventListener('click', function(e) {
		shape.animate({duration:3000, transform:Ti.UI.create2DMatrix().scale(2)});
	});
	win.add(view);
	openWin(win);
}

function shape6Ex() {
	var win = createWin();
	win.backgroundColor = 'gray';
	var view = Shape.createView({width:200, height:200, bubbleParent:false});
	view.add(Shape.createRoundedRect({lineWidth:1,
		fillColor:'white',
		lineColor:'gray',
		cornerRadius:10,
		lineClipped:true,
		radius:'43%',
		lineShadow:{radius:4, color:'black', offset:{x:0, y:-3}}}));
	// view.add({
	// 	lineWidth:4,
	// 	fillColor:'white',
	// 	lineColor:'black',
	// 	cornerRadius:10,
	// 	radius:'43%',
	// 	lineShadow:{radius:4, color:'black', offset:{x:0,y:-4}},
	// 	type:'roundedrect'
	// });
	view.add(Ti.UI.createLabel({left:14,
		right:14,
		top:14,
		bottom:14,
		width:Ti.UI.FILL,
		height:Ti.UI.FILL,
		bottom:20,
		html:html}));
	win.add(view);
	openWin(win);
}

function shape7Ex() {
	var win = createWin();
	var view = Shape.createView({width:200, height:200, bubbleParent:false});

	var slice1 = Shape.createPieSlice({fillColor:'#aaffffff',
		innerRadius:30,
		startAngle:0,
		radius:'40%',
		sweepAngle:40});
	var slice2 = Shape.createPieSlice({fillColor:'#aaffffff',
		innerRadius:30,
		startAngle:30,
		sweepAngle:100});
	var slice3 = Shape.createPieSlice({fillColor:'#aaffffff',
		innerRadius:30,
		startAngle:-60,
		radius:'20%',
		sweepAngle:10});
	view.add({type:'circle', radius:30, fillColor:'blue'});
	view.add(slice1);
	view.add(slice2);
	view.add(slice3);
	win.add(view);
	slice1.animate({duration:10000, startAngle:360, repeat:Ti.UI.INFINITE});
	slice2.animate({duration:5000, startAngle:200, autoreverse:true, repeat:Ti.UI.INFINITE});
	slice3.animate({duration:4000, startAngle:-420, repeat:Ti.UI.INFINITE});

	view.addEventListener('click', function(e) {
		slice1.animate({duration:400, radius:'50%', autoreverse:true});
		slice2.animate({duration:700, radius:'30%', autoreverse:true});
		slice3.animate({duration:300, radius:'30%'});
	});
	openWin(win);
}

function buttonAndLabelEx() {
	var win = createWin();
	var button = Ti.UI.createButton({top:0,
		titlePadding:{left:30, top:30, bottom:30, right:30},
		bubbleParent:false,
		backgroundColor:'gray',
		backgroundSelectedGradient:{type:'linear',
			colors:['#333', 'transparent'],
			startPoint:{x:0, y:0},
			endPoint:{x:0, y:"100%"}},
		title:'test buutton'});
	button.add(Ti.UI.createView({backgroundColor:'purple', backgroundSelectedColor:'white', left:10, width:15, height:15}));
	button.add(Ti.UI.createView({backgroundColor:'green', bottom:10, width:15, height:15}));
	button.add(Ti.UI.createView({backgroundColor:'yellow', top:10, width:15, height:15}));
	button.add(Ti.UI.createView({backgroundColor:'orange', right:10, width:15, height:15}));
	var t1 = Ti.UI.create2DMatrix();
	var t2 = Ti.UI.create2DMatrix().scale(2.0, 2.0).translate(0, 40).rotate(90);
	button.addEventListener('longpress', function(e) {
		button.animate({duration:500, transform:varSwitch(button.transform, t2, t1),});
	});
	win.add(button);
	var label = Ti.UI.createLabel({bottom:20,
		backgroundColor:'gray',
		backgroundSelectedColor:'#a46',
		textPadding:{left:30, top:30, bottom:30, right:30},
		bubbleParent:false,
		backgroundSelectedGradient:{type:'linear',
			colors:['#333', 'transparent'],
			startPoint:{x:0, y:0},
			endPoint:{x:0, y:"100%"}},
		text:'This is a sample\n text for a label'});
	label.add(Ti.UI.createView({touchEnabled:false,backgroundColor:'red', backgroundSelectedColor:'white', left:10, width:15, height:15}));
	label.add(Ti.UI.createView({backgroundColor:'green', bottom:10, width:15, height:15}));
	label.add(Ti.UI.createView({backgroundColor:'yellow', top:10, width:15, height:15}));
	label.add(Ti.UI.createView({backgroundColor:'orange', right:10, width:15, height:15}));
	var t3 = Ti.UI.create2DMatrix().scale(2.0, 2.0).translate(0, -40).rotate(90);
	label.addEventListener('longpress', function(e) {
		label.animate({duration:500, transform:varSwitch(label.transform, t3, t1),});
	});
	win.add(label);
	win.open();
}

function maskEx() {
	var win = createWin();
	win.backgroundGradient = {type:'linear',
		colors:['gray', 'white'],
		startPoint:{x:0, y:0},
		endPoint:{x:0, y:"100%"}};
	var view = Ti.UI.createView({top:20,
		borderRadius:10,
		borderColor:'red',
		borderWidth:5,
		bubbleParent:false,
		width:300,
		height:100,
		backgroundColor:'green',
		viewMask:'/images/body-mask.png',
		backgroundGradient:{type:'linear',
			colors:['red', 'green', 'orange'],
			startPoint:{x:0, y:0},
			endPoint:{x:0, y:"100%"}}});
	var imageView = Ti.UI.createImageView({bottom:20,
		// borderRadius : 10,
		// borderColor:'red',
		// borderWidth:5,
		bubbleParent:false,
		width:300,
		height:100,
		backgroundColor:'yellow',
		scaleType:Ti.UI.SCALE_TYPE_ASPECT_FIT,
		image:'/images/slightlylargerimage.png',
		imageMask:'/images/body-mask.png',
	// viewMask : '/images/mask.png',
	});
	view.add(Ti.UI.createView({backgroundColor:'red', bottom:10, width:30, height:30}));
	win.add(view);
	win.add(imageView);
	win.add(Ti.UI.createButton({borderRadius:20,
		titlePadding:{left:30, top:30, bottom:30, right:30},
		bubbleParent:false,
		title:'test buutton',
		viewMask:'/images/body-mask.png'}));
	openWin(win);
}

function ImageViewEx() {
	var win = createWin();
	var view = Ti.UI.createImageView({
		bubbleParent:false,
		width:300,
		height:Ti.UI.SIZE,
		borderColor:'red',
		borderWidth:2,
		backgroundColor:'green',
		image:'/images/slightlylargerimage.png',});
	view.add(Ti.UI.createView({backgroundColor:'yellow', top:10, width:15, height:15}));
	view.addEventListener('click', function(){
//		view.image = varSwitch(view.image, '/images/slightlylargerimage.png', '/images/poster.jpg');
		view.animate({height:400, duration:1000, autoreverse:true});
	});
	win.add(view);
	openWin(win);
}


function scrollableEx() {
	var win = createWin();
	// Create a custom template that displays an image on the left,
	// then a title next to it with a subtitle below it.
	var myTemplate = {
			properties : {
				height:50
			},
			childTemplates : [{
				type : 'Ti.UI.ImageView',
				bindId : 'leftImageView',
				properties : {
					left:0,
					width:40,
					localLoadSync:true,
					height:40,
					transform:Ti.UI.create2DMatrix().rotate(90),
					backgroundColor:'blue',
					// backgroundSelectedColor:'green',
					image:'/images/contactIcon.png',
					// borderColor:'red',
					// borderWidth:2
					viewMask:'/images/contactMask.png',
				}
			}, {
				type : 'Ti.UI.Label',
				bindId : 'label',
				properties : {
					multiLineEllipsize:Ti.UI.TEXT_ELLIPSIZE_TAIL,
					top:2,
					bottom:2,
					left:45,
					textPadding:{bottom:4},
					right:55,
					touchEnabled:false,
					height:Ti.UI.FILL,
					color :'black',
					font: {
						fontSize: 16
					},
					width:Ti.UI.FILL
				}
			}, {
				type : 'Ti.UI.ImageView',
				bindId : 'rightImageView',
				properties : {
					right:5,
					top:8,
					localLoadSync:true,
					bottom:8,
					width:Ti.UI.SIZE,
					touchEnabled:false
				}
			}, {
				type : 'Ti.UI.ImageView',
				bindId : 'networkIndicator',
				properties : {
					right:40,
					top:4,
					localLoadSync:true,
					height:15,
					width:Ti.UI.SIZE,
					touchEnabled:false
				}
			}, {
				type : 'Ti.UI.View',
				properties : {
					backgroundColor: '#999',
					left:4, right:4,
					bottom:0,
					height:1
				}
			}]
	};
	var contactAction;
	var blurImage;
	var listView = Ti.UI.createListView({
	// Maps myTemplate dictionary to 'template' string
	templates:{'template':myTemplate},
		defaultItemTemplate:'template',
		selectedBackgroundGradient:{type:'linear',
			colors:['blue', 'green'],
			startPoint:{x:0, y:0},
			endPoint:{x:0, y:"100%"}}});
	listView.addEventListener('itemclick', function(_event) {
		if (_event.hasOwnProperty('section') && _event.hasOwnProperty('itemIndex')) {
			var item  = _event.section.getItemAt(_event.itemIndex);
			if (!contactAction) {
				contactAction = Ti.UI.createView({backgroundColor:'green'});
				blurImage = Ti.UI.createImageView({
					scaleType:Ti.UI.SCALE_TYPE_ASPECT_FILL,
					width:Ti.UI.FILL,
					height:Ti.UI.FILL
				});
				contactAction.add(blurImage);
				blurImage.addEventListener('click', function(){
					animation.fadeOut(contactAction, 200, function(){
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
	'Sarah Mullock'];

	function formatTitle (_history) {
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
				fullName:names[Math.floor(Math.random()*names.length)],
				date:random(1293886884000, 1376053320000),
				kb: random(0, 100000),
				outgoing:!!random(0, 1),
				wifi:!!random(0, 1)
			};
			dataSet.push({
				contactName : callhistory.fullName,
				label : {
					html : formatTitle(callhistory)
				},
				rightImageView:{
					image:(callhistory.outgoing?'/images/outgoing.png':'/images/incoming.png')
				},
				networkIndicator:{
					image:(callhistory.wifi?'/images/wifi.png':'/images/mobile.png')
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



function transitionEx() {
	var win = createWin();
	var funView = Titanium.UI.createView({
		height:40,
		width:200,
		borderRadius:10,
		top:260,
		backgroundColor:'#fff'
	});
	win.add(funView);

	// our first view - button
	var b = Titanium.UI.createButton({
		title:'Click Me',
		height:40,
		width:200
	});

	b.addEventListener('click', function()
	{
		funView.animate({view:b2, transition:Ti.UI.iPhone.AnimationStyle.CURL_UP});	
	});

	// view with label (our second view)
	var b2 = Titanium.UI.createView({
		height:40,
		width:200,
//		borderRadius:10,
		backgroundColor:'red'
	});

	b2.addEventListener('click', function()
	{
		funView.animate({view:b, transition:Ti.UI.iPhone.AnimationStyle.CURL_DOWN});	
	});
	var bViewLabel = Titanium.UI.createLabel({
		text:'Ouch!',
		color:'#fff',
		width:'auto',
		height:'auto'
	});
	b2.add(bViewLabel);


	funView.add(b);
	openWin(win);
}

function listView2Ex() {
	var win = createWin();
	// Create a custom template that displays an image on the left,
	// then a title next to it with a subtitle below it.
	var myTemplate = {childTemplates:[{// Image justified left
	type:'Ti.UI.ImageView', // Use an image view for the image
	bindId:'pic', // Maps to a custom pic property of the item data
	properties:{// Sets the image view  properties
	width:'50dp', height:'50dp', left:0}},
			{// Title
			type:'Ti.UI.Label', // Use a label for the title
				bindId:'info', // Maps to a custom info property of the item data
				properties:{// Sets the label properties
				color:'black',
					font:{fontFamily:'Arial', fontSize:'20dp', fontWeight:'bold'},
					left:'60dp',
					top:0,}},
			{// Subtitle
			type:'Ti.UI.Label', // Use a label for the subtitle
			bindId:'es_info', // Maps to a custom es_info property of the item data
			properties:{// Sets the label properties
			color:'gray', font:{fontFamily:'Arial', fontSize:'14dp'}, left:'60dp', top:'25dp',}}]};
	var listView = Ti.UI.createListView({
	// Maps myTemplate dictionary to 'template' string
	templates:{'template':myTemplate},
		// Use 'template', that is, the myTemplate dict created earlier
		// for all items as long as the template property is not defined for an item.
		style:Titanium.UI.iPhone.ListViewStyle.GROUPED,
		defaultItemTemplate:'template',
		selectedBackgroundGradient:{type:'linear',
			colors:['blue', 'green'],
			startPoint:{x:0, y:0},
			endPoint:{x:0, y:"100%"}}});
	var sections = [];
	var fruitSection = Ti.UI.createListSection({headerTitle:'Fruits / Frutas'});
	var fruitDataSet = [
	// the text property of info maps to the text property of the title label
	// the text property of es_info maps to text property of the subtitle label
	// the image property of pic maps to the image property of the image view
	{info:{text:'Apple'}, es_info:{text:'Manzana'}, pic:{image:'apple.png'}},
			{properties:{backgroundColor:'red'},
				info:{text:'Banana'},
				es_info:{text:'Banana'},
				pic:{image:'banana.png'}}];
	fruitSection.setItems(fruitDataSet);
	sections.push(fruitSection);
	var vegSection = Ti.UI.createListSection({headerTitle:'Vegetables / Verduras'});
	var vegDataSet = [{info:{text:'Carrot'}, es_info:{text:'Zanahoria'}, pic:{image:'carrot.png'}},
			{info:{text:'Potato'}, es_info:{text:'Patata'}, pic:{image:'potato.png'}}];
	vegSection.setItems(vegDataSet);
	sections.push(vegSection);
	var grainSection = Ti.UI.createListSection({headerTitle:'Grains / Granos'});
	var grainDataSet = [{info:{text:'Corn'}, es_info:{text:'Maiz'}, pic:{image:'corn.png'}},
			{info:{text:'Rice'}, es_info:{text:'Arroz'}, pic:{image:'rice.png'}}];
	grainSection.setItems(grainDataSet);
	sections.push(grainSection);
	listView.setSections(sections);
	win.add(listView);
	openWin(win);
}

function listViewEx() {
	var win = createWin();
	var listview = Ti.UI
			.createListView({allowsSelection:false,
				rowHeight:50,
				style:Titanium.UI.iPhone.ListViewStyle.GROUPED,
				selectedBackgroundGradient:{type:'linear',
					colors:['blue', 'green'],
					startPoint:{x:0, y:0},
					endPoint:{x:0, y:"100%"}},
				sections:[{items:[{properties:{backgroundColor:'blue', title:'Shape'}},
						{properties:{backgroundColor:'red', title:'ButtonsAndLabels'}},
						{properties:{title:'Shape'}},
						{properties:{title:'Shape'}},
						{properties:{title:'Transform',
							accessoryType:Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK}},
						{properties:{title:'Shape'}},
						{properties:{backgroundColor:'red',
							title:'Shape',
							accessoryType:Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK}},
						{properties:{title:'Shape'}},
						{properties:{title:'Transform',
							accessoryType:Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK}},
						{properties:{title:'Shape'}},
						{properties:{backgroundColor:'red', title:'Shape'}},
						{properties:{title:'Shape'}},
						{properties:{title:'Transform',
							accessoryType:Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK}},
						{properties:{title:'Shape'}},
						{properties:{title:'Shape'}},
						{properties:{title:'Shape'}},
						{properties:{title:'Transform',
							accessoryType:Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK}},
						{properties:{title:'Shape'}},
						{properties:{backgroundColor:'red',
							title:'Shape',
							accessoryType:Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK}},
						{properties:{title:'Shape'}},
						{properties:{title:'Transform',
							accessoryType:Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK}},
						{properties:{title:'Shape'}},
						{properties:{backgroundColor:'red', title:'Shape'}},
						{properties:{title:'Shape'}},
						{properties:{title:'Transform',
							accessoryType:Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK}},
						{properties:{title:'Shape'}},
						{properties:{title:'Shape'}},
						{properties:{title:'Shape'}},
						{properties:{title:'Transform',
							accessoryType:Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK}},
						{properties:{title:'Shape'}},
						{properties:{backgroundColor:'red',
							title:'Shape',
							accessoryType:Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK}},
						{properties:{title:'Shape'}},
						{properties:{title:'Transform',
							accessoryType:Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK}},
						{properties:{title:'Shape'}},
						{properties:{backgroundColor:'red', title:'Shape'}},
						{properties:{title:'Shape'}},
						{properties:{title:'Transform',
							accessoryType:Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK}},
						{properties:{title:'Shape'}},
						{properties:{title:'Shape'}},
						{properties:{title:'Shape'}},
						{properties:{title:'Transform',
							accessoryType:Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK}},
						{properties:{title:'Shape'}},
						{properties:{backgroundColor:'red',
							title:'Shape',
							accessoryType:Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK}},
						{properties:{title:'Shape'}},
						{properties:{title:'Transform',
							accessoryType:Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK}},
						{properties:{title:'Shape'}},
						{properties:{backgroundColor:'red', title:'Shape'}},
						{properties:{title:'Shape'}},
						{properties:{title:'Transform',
							accessoryType:Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK}},
						{properties:{title:'Shape'}},
						{properties:{title:'Shape'}},
						{properties:{title:'Shape'}},
						{properties:{title:'Transform',
							accessoryType:Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK}},
						{properties:{title:'Shape'}},
						{properties:{backgroundColor:'red',
							title:'Shape',
							accessoryType:Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK}},
						{properties:{title:'Shape'}},
						{properties:{title:'Transform',
							accessoryType:Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK}},
						{properties:{title:'Shape'}},
						{properties:{backgroundColor:'red', title:'Shape'}},
						{properties:{title:'Shape'}},
						{properties:{title:'Transform',
							accessoryType:Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK}},
						{properties:{title:'Shape'}},
						{properties:{title:'Transform',
							accessoryType:Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK}}]}]});
	win.add(listview);
	openWin(win);
}

function fadeInEx() {
	var win = createWin();
	var view = Ti.UI.createView({backgroundColor:'red', opacity:0, width:200, height:200})
	view.add(Ti.UI.createView({backgroundColor:'blue', bottom:10, width:50, height:50}));
	var showMe = function() {
		view.opacity = 0;
		view.transform = Ti.UI.create2DMatrix().scale(0.6, 0.6);
		win.add(view);
		view.animate({opacity:1, duration:300, transform:Ti.UI.create2DMatrix()});
	};
	var hideMe = function(_callback) {
		view.animate({opacity:0, duration:200}, function() {
			win.remove(view);
		});
	};
	var button = Ti.UI.createButton({top:10, width:100, bubbleParent:false, title:'test buutton'});
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
	var scrollView = Ti.UI.createScrollView({layout:'vertical', contentHeight:Ti.UI.SIZE});
	scrollView.add(Ti.UI.createLabel({width:Ti.UI.FILL, height:Ti.UI.SIZE, bottom:20, html:html}));
	scrollView.add(Ti.UI.createLabel({multiLineEllipsize:Ti.UI.TEXT_ELLIPSIZE_HEAD,
		bottom:20,
		html:html}));
	scrollView.add(Ti.UI.createLabel({multiLineEllipsize:Ti.UI.TEXT_ELLIPSIZE_MIDDLE,
		bottom:20,
		html:html}));
	scrollView.add(Ti.UI.createLabel({width:Ti.UI.FILL,
		height:Ti.UI.SIZE,
		bottom:20,
		multiLineEllipsize:Ti.UI.TEXT_ELLIPSIZE_TAIL,
		html:html}));
	scrollView.add(Ti.UI.createLabel({width:200,
		height:Ti.UI.SIZE,
		bottom:20,
		multiLineEllipsize:Ti.UI.TEXT_ELLIPSIZE_TAIL,
		html:html}));
	scrollView.add(Ti.UI.createLabel({width:200,
		height:Ti.UI.SIZE,
		bottom:20,
		ellipsize:Ti.UI.TEXT_ELLIPSIZE_TAIL,
		html:html}));
	scrollView.add(Ti.UI.createLabel({height:200,
		bottom:20,
		ellipsize:Ti.UI.TEXT_ELLIPSIZE_TAIL,
		html:html}));
	win.add(scrollView);
	openWin(win);
}

function svgExs() {
	var win = createWin();
	var listview = createListView();
	listview.sections = [{items:[{properties:{title:'View'}, callback:svg1Ex},
			{properties:{title:'Button'}, callback:svg2Ex},
			{properties:{title:'ImageView'}, callback:svg3Ex},
			{properties:{title:'ListView'}, callback:svg4Ex}]}];
	win.add(listview);
	openWin(win);
}
function svg1Ex() {
	var win = createWin();
	var view = Ti.UI.createView({
		bubbleParent:false,
		width:100,
		height:100,
		backgroundColor:'yellow',
		scaleType:Ti.UI.SCALE_TYPE_ASPECT_FIT,
		preventDefaultImage:true,
		backgroundImage:'/images/Notepad_icon_small.svg'
	});
	win.add(view);
	var button = Ti.UI.createButton({top:20,
		bubbleParent:false,
		title:'change svg'});
	button.addEventListener('click', function(){
		view.backgroundImage = varSwitch(view.backgroundImage, '/images/gradients.svg', '/images/Logo.svg');
	});
	win.add(button);
	var button2 = Ti.UI.createButton({bottom:20,
		bubbleParent:false,
		title:'animate'});
	button2.addEventListener('click', function(){
		view.animate({height:Ti.UI.FILL, width:Ti.UI.FILL, duration:2000, autoreverse:true});
	});
	win.add(button2);
	openWin(win);
}

function svg2Ex() {
	var win = createWin();
	var button = Ti.UI.createButton({top:20,
		titlePadding:{left:30, top:30, bottom:30, right:30},
		bubbleParent:false,
		image:'/images/Logo.svg',
		title:'test buutton'});
	win.add(button);
	openWin(win);
}

function svg3Ex() {
	var win = createWin({backgroundImage:'/images/Notepad_icon_small.svg'});
	var imageView = Ti.UI.createImageView({
		bubbleParent:false,
		width:300,
		height:100,
		backgroundColor:'yellow',
		scaleType:Ti.UI.SCALE_TYPE_ASPECT_FIT,
		preventDefaultImage:true,
		image:'/images/Notepad_icon_small.svg'
	});
	imageView.addEventListener('click', function(){
		imageView.scaleType = (imageView.scaleType + 1 )% 6;
	});
	win.add(imageView);
	var button = Ti.UI.createButton({top:20,
		bubbleParent:false,
		title:'change svg'});
	button.addEventListener('click', function(){
		imageView.image = varSwitch(imageView.image, '/images/gradients.svg', '/images/Logo.svg');
	});
	win.add(button);
	var button2 = Ti.UI.createButton({bottom:20,
		bubbleParent:false,
		title:'animate'});
	button2.addEventListener('click', function(){
		imageView.animate({height:400, duration:1000, autoreverse:true});
	});
	win.add(button2);
	openWin(win);
}

function svg4Ex() {
	var win = createWin();
	var myTemplate = {childTemplates:[{type:'Ti.UI.View',
		bindId:'holder',
		properties:{width:Ti.UI.FILL,
			height:Ti.UI.FILL,
			touchEnabled:false,
			layout:'horizontal',
			horizontalWrap:false},
		childTemplates:[{type:'Ti.UI.ImageView',
			bindId:'pic',
			properties:{touchEnabled:false, height:50, image:'/images/gradients.svg'}},
				{type:'Ti.UI.Label',
					bindId:'info',
					properties:{color:textColor,
						touchEnabled:false,
						font:{fontSize:20, fontWeight:'bold'},
						width:Ti.UI.FILL,
						left:10}},
				{type:'Ti.UI.Button',
					bindId:'button',
					properties:{title:'menu', left:10}
				}]},
			{type:'Ti.UI.Label',
				bindId:'menu',
				properties:{color:'white',
					text:'I am the menu',
					backgroundColor:'#444',
					width:Ti.UI.FILL,
					height:Ti.UI.FILL,
					opacity:0},
				}]};

	var listView = createListView({templates:{'template':myTemplate},
		defaultItemTemplate:'template'});

	var sections = [{headerTitle:'Fruits / Frutas',
		items:[{info:{text:'Apple'}},
				{properties:{backgroundColor:'red'}, info:{text:'Banana'}, pic:{image:'banana.png'}}]},
			{headerTitle:'Vegetables / Verduras',
				items:[{info:{text:'Carrot'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}, pic:{image:'/images/opacity.svg'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}, pic:{image:'/images/opacity.svg'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}, pic:{image:'/images/Logo.svg'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}},
						{info:{text:'Potato'}}]},
			{headerTitle:'Grains / Granos', items:[{info:{text:'Corn'}}, {info:{text:'Rice'}}]}];
	listView.setSections(sections);
	win.add(listView);
	openWin(win);
}

function float2color( pr,pg,pb ) {
	var color_part_dec = 255 * pr;
	var r = Number(parseInt( color_part_dec , 10)).toString(16);
	color_part_dec = 255 * pg;
	var g = Number(parseInt( color_part_dec , 10)).toString(16);
	color_part_dec = 255 * pb;
	var b = Number(parseInt( color_part_dec , 10)).toString(16);
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


function navWindowEx() {
	
	
	function createSimulateWindow(_navWin) {
		var index = _navWin.stackSize;
		var color = cellColor(index);
		var args = {
			autoAdjustScrollViewInsets:true,
			extendEdges:[Ti.UI.EXTEND_EDGE_ALL],
			translucent:true,
			title:(_navWin.title + ' / win' + (_navWin.stackSize)),
			backgroundColor:'black',
			navBarHidden:false
		};
		if (isAndroid) {
			args.activity = {
				onCreateOptionsMenu:function(e) {
					var menu = e.menu;
					 
					var closeMenuItem = menu.add({
						title : "Close",
						showAsAction : Ti.Android.SHOW_AS_ACTION_IF_ROOM
					});
					closeMenuItem.addEventListener("click", function(e) {
						newWin.close({transitionDuration:300});
					});
				}
			}
		}
		var newWin = Ti.UI.createWindow(args);

		function openMe(_params) {
			Ti.API.info('openMe');
			_params.transitionDuration = 1000;
			_navWin.push(createSimulateWindow(_navWin), _params);
		}

		

		var listView = createListView({sections:[{items:[{properties:{color:'black',title:'Swipe',
			backgroundColor:color},
			params:{transitionStyle:Ti.UI.TransitionStyle.SWIPE}},
				{properties:{color:'black',title:'SwipFade', backgroundColor:color, opacity:0.8},
					params:{transitionStyle:Ti.UI.TransitionStyle.SWIPE_FADE}},
				{properties:{color:'black',title:'Flip', backgroundColor:color},
					params:{transitionStyle:Ti.UI.TransitionStyle.FLIP}},
				{properties:{color:'black',title:'Cube', backgroundColor:color, opacity:0.8},
					params:{transitionStyle:Ti.UI.TransitionStyle.CUBE}},
				{properties:{color:'black',title:'SwipFade FromTop', backgroundColor:color},
					params:{transitionStyle:Ti.UI.TransitionStyle.SWIPE_FADE,
						transitionSubStyle:Ti.UI.TransitionStyle.TOP_TO_BOTTOM}},
				{properties:{color:'black',title:'Flip FromBottom', backgroundColor:color, opacity:0.8},
					params:{transitionStyle:Ti.UI.TransitionStyle.FLIP,
						transitionSubStyle:Ti.UI.TransitionStyle.BOTTOM_TO_TOP}},
				{properties:{color:'black',title:'Fold', backgroundColor:color},
					params:{transitionStyle:Ti.UI.TransitionStyle.FOLD}},
				{properties:{color:'black',title:'Fade', backgroundColor:color, opacity:0.8},
					params:{transitionStyle:Ti.UI.TransitionStyle.FADE}},
				{properties:{color:'black',title:'Back Fade', backgroundColor:color},
					params:{transitionStyle:Ti.UI.TransitionStyle.BACK_FADE}},
				{properties:{color:'black',title:'Scale', backgroundColor:color, opacity:0.8},
					params:{transitionStyle:Ti.UI.TransitionStyle.SCALE}},
				{properties:{color:'black',title:'Push Rotate', backgroundColor:color},
					params:{transitionStyle:Ti.UI.TransitionStyle.PUSH_ROTATE}},
		]}]});
		
		listView.addEventListener('itemclick', function(_event) {
			if (_event.hasOwnProperty('section') && _event.hasOwnProperty('itemIndex')) {
				var item = _event.section.getItemAt(_event.itemIndex);
				Ti.API.info('item ' + JSON.stringify(item));
				openMe(item.params);
			}
		});
		newWin.add(listView);
		return newWin;
	}
	
	var navWin1 = Ti.UI.createNavigationWindow({
		backgroundColor:'black',
		title:'NavWindow1'
	});
	navWin1.window = createSimulateWindow(navWin1);
	
	var navWin2 = Ti.UI.createNavigationWindow({
		backgroundColor:'black',
		title:'NavWindow2'
	});
	navWin2.window = createSimulateWindow(navWin2);

	var args = {
		backgroundColor:backColor,
		title:'TransitionWindow'
	};
	if (isAndroid) {
		args.activity = {actionBar:{icon:Ti.Android.R.drawable.ic_menu_preferences,
			backgroundColor:'red',
			onHomeIconItemSelected:function(e) {
				slidingMenu.toggleLeftView();
		}}};
	}

	var transitionWindow = Ti.UI.createWindow(args);
	
	var transitionViewHolder = Ti.UI.createButton({
		height:40,
		width:200,
		borderRadius:10,
		backgroundColor:'red'
	});
	
	var tr1 =Ti.UI.createLabel({
		text:'Ouch!',
		color:'#fff',
		textAlign:'center',
		backgroundColor:'green',
		width:Ti.UI.FILL,
		height:Ti.UI.FILL
	});
	tr1.addEventListener('click', function(e) {
		transitionViewHolder.transitionViews(tr1, tr2, {transitionStyle:Ti.UI.TransitionStyle.FOLD, transitionDuration:3000});
	});
	var tr2 =Ti.UI.createButton({
		title:'Ouch!',
		color:'#000',
		height:40,
		width:200,
		backgroundColor:'white'
	});
	tr2.addEventListener('click', function(e) {
		transitionViewHolder.transitionViews(tr2, tr1, null);
	});
	transitionViewHolder.add(tr1);
	transitionWindow.add(transitionViewHolder);
	
	//LeftMenu
	var leftMenu = createListView({backgroundColor:'gray',
		sections:[{items:[{properties:{title:'nav1'}, callback:function() {slidingMenu.centerView = navWin1;slidingMenu.closeLeftView()}},
				{properties:{title:'nav2'}, callback:function() {slidingMenu.centerView = navWin2;slidingMenu.closeLeftView()}},
				{properties:{title:'Transition'}, callback:function() {slidingMenu.centerView = transitionWindow;slidingMenu.closeLeftView()}},
				{properties:{title:'Close'}, callback:function() {
					slidingMenu.close();
				}}]}]});
	
	
	//slidingMenu
	var slidingMenu = Ti.UI.createSlideMenu({
		backgroundColor:'black',
		leftViewWidth:-60,
		fading:1,
		leftViewDisplacement:40,
		// panningMode:Ti.UI.MENU_PANNING_BORDERS,
		orientationModes:[Ti.UI.UPSIDE_PORTRAIT,
				Ti.UI.PORTRAIT,
				Ti.UI.LANDSCAPE_RIGHT,
				Ti.UI.LANDSCAPE_LEFT],
				leftView:leftMenu,
				centerView:transitionWindow
	});
	
	slidingMenu.open();
}
function slideMenuEx() {
	var rootWindows = [];
	var otherWindows = [];

	function closeWindow(_win) {
		var lastWin = otherWindows[otherWindows.length - 1];
		if (_win === lastWin) {
			otherWindows.pop();
			var lastWin = otherWindows[otherWindows.length - 1];
			_win.animate({transform:Ti.UI.create2DMatrix().translate('100%', 0), duration:400},
					function() {
						_win.close();
					});
			lastWin.animate({transform:null, opacity:1, duration:400});
		} else {
			_win.close();
		}
	};

	function openMovieWindow(_imgUrl) {
		var win = createWin({navBarHidden:true,
			transform:Ti.UI.create2DMatrix().translate('100%', 0)});
		var closeButton = Ti.UI.createLabel({text:'<-', color:'white', textAlign:'center', backgroundColor:'#77000000', width:50, height:40, top:0, left:10, font:{fontSize:24, fontWeight:'bold'}});
		closeButton.addEventListener('click', function(e) {
			closeWindow(win);
		});
		var verticalScrollView = Ti.UI.createScrollableView({disableBounce:true,layout:'vertical'});
		var topView = Ti.UI.createView({backgroundColor:'black'});
		var glassView = Ti.UI.createView({bottom:0, height:100});
		var textView = Ti.UI.createView({backgroundColor:'#55ffffff'});
		var blurImageView = Ti.UI.createImageView({opacity:0,
			width:Ti.UI.FILL,
			height:Ti.UI.FILL});
//		var blurImageView2 = Ti.UI.createImageView({width:Ti.UI.FILL,
//			height:Ti.UI.FILL,
//			viewMask:'/images/bottommask.png'});
		var imageView = Ti.UI.createImageView({animationDuration:0, 
			scaleType:Ti.UI.SCALE_TYPE_ASPECT_FILL,
			opacity:0, width:Ti.UI.FILL, height:Ti.UI.FILL});
		imageView.addEventListener('load', function(e) {
			imageView.animate({opacity:1, duration:500}, function() {
				Ti.Image.getFilteredViewToImage(imageView, Ti.Image.FILTER_GAUSSIAN_BLUR, {scale:0.3,
					callback:function(result) {
						Ti.API.info('imageView.rect.width ' + imageView.rect.width);
						Ti.API.info('imageView.rect.height ' + imageView.rect.height);
						Ti.API.info('imagewidth ' + result.image.width);
						Ti.API.info('imageheight ' + result.image.height);
						blurImageView.image = result.image;
						glassView.backgroundImage = result.image.imageAsCropped(glassView.rect, {scale:0.3});
					}
				});
			});

			scrollView.addEventListener('scroll', function(e) {
				blurImageView.opacity = Math.max(0, e.currentPageAsFloat);
			});
			
		})
		var scrollView = Ti.UI.createScrollableView({disableBounce:true});

//		verticalScrollView.addEventListener('swipe', function(e) {
//			if (e.direction === 'up') {
//				verticalScrollView.movePrevious();
//			} else if (e.direction === 'down') {
//				verticalScrollView.moveNext();
//			}
//		});
		topView.add(imageView);
		topView.add(blurImageView);
		glassView.add(textView);
		topView.add(glassView);
		topView.add(scrollView);
		var leftView = Ti.UI.createView({backgroundColor:'#55000000'});
		leftView.add(Ti.UI.createLabel({text:'Test Movie', color:backColor,
			textPadding:{left:20, right:20, top:10, bottom:10},
			backgroundColor:'#55000000',
			borderColor:'gray',
			borderRadius:10,
			borderWidth:2}));
		var rightView = Ti.UI.createView({backgroundColor:'#55000000'});
		rightView.add(Ti.UI.createLabel({text:'Test Movie', color:backColor,
			textPadding:{left:20, right:20, top:10, bottom:10},
			backgroundColor:'#55000000',
			borderColor:'gray',
			borderRadius:10,
			borderWidth:2}));
		scrollView.views = [leftView, rightView];
		var view2 = Ti.UI.createView({backgroundColor:'gray'});
		view2.add(createListView({top:100,
			backgroundColor:'#666',
			pageMargin:10,
			exclusiveTouch:true,
			bottom:50,
			sections:[{items:[{properties:{title:'test1'}, callback:function() {
				scrollView.movePrevious(false)
			}},
					{properties:{title:'test2'}},
					{properties:{title:'test2'}},
					{properties:{title:'test2'}},
					{properties:{title:'test2'}},
					{properties:{title:'test2'}},
					{properties:{title:'test2'}},
					{properties:{title:'test2'}},
					{properties:{title:'test2'}},
					{properties:{title:'test2'}},
					{properties:{title:'test2'}},
					{properties:{title:'test2'}},
					{properties:{title:'test2'}},
					{properties:{title:'test2'}},
					{properties:{title:'test2'}},
					{properties:{title:'Close'}, callback:function() {
						slidingMenu.close();
					}}]}]}));
		view2.add({backgroundGradient:{type:'linear',
			colors:['#333', 'transparent'],
			startPoint:{x:0, y:0},
			endPoint:{x:0, y:"100%"}},
			height:10,
			width:Ti.UI.FILL,
			top:100});
		view2.add({backgroundGradient:{type:'linear',
			colors:['transparent', '#333'],
			startPoint:{x:0, y:0},
			endPoint:{x:0, y:"100%"}},
			height:10,
			width:Ti.UI.FILL,
			bottom:50});
		verticalScrollView.views = [topView, view2];
		win.add(verticalScrollView);
		win.add(closeButton);

		var lastWin = otherWindows[otherWindows.length - 1];
		win.addEventListener('open', function(e) {
			lastWin.animate({transform:Ti.UI.create2DMatrix().scale(0.9),
						opacity:0.5,
						duration:200});
			e.source.animate({transform:null, duration:400}, function() {
				Ti.API.info('test');
				imageView.image = _imgUrl.replace('-138', '');
			});
		});
		win.addEventListener('androidback', function(e) {
			closeWindow(e.source);
		});
		win.open({animated:false});
		otherWindows.push(win);
	}

	var rootWindow1 = Ti.UI.createWindow({navBarHidden:true, backgroundColor:backColor});
	function getScrollViewPage(_imgUrl) {
		var imageView = Ti.UI.createImageView({
			opacity:0,
			scaleType:Ti.UI.SCALE_TYPE_SCALE_TO_FILL,
			height:Ti.UI.FILL,
			width:Ti.UI.FILL,
			left:'15%',
			right:'15%',
			image:_imgUrl});
		var glassView = Ti.UI.createView({bottom:0, height:100,viewMask:'/images/masktest.png'});
		var textView = Ti.UI.createView({backgroundColor:'#55000000'});
		imageView.addEventListener('load', function(e) {
			Ti.Image.getFilteredViewToImage(imageView, Ti.Image.FILTER_GAUSSIAN_BLUR, {scale:0.3,
				callback:function(result) {
					var rect = glassView.rect;
					Ti.API.info('imageView.rect.width ' + imageView.rect.width);
					Ti.API.info('imageView.rect.height ' + imageView.rect.height);
					Ti.API.info('imagewidth ' + result.image.width);
					Ti.API.info('imageheight ' + result.image.height);
					Ti.API.info('rect.width ' + rect.width);
					if (rect.width != 0)
						glassView.backgroundImage = result.image.imageAsCropped(rect, {scale:0.3});
					imageView.animate({opacity:1, duration:500});
					}
			});
			
		})
		imageView.addEventListener('click', function(e) {
			openMovieWindow(_imgUrl);
		});
		glassView.add(textView);
		imageView.add(glassView);
		return imageView;
	}
	var scrollView = Ti.UI.createScrollableView({backgroundColor:'blue',
		height:200,
		width:'90%',
		disableBounce:false,
		pageWidth:'60%',
		cacheSize:5,
		views:[getScrollViewPage('http://zapp.trakt.us/images/posters_movies/192263-138.jpg'),
				getScrollViewPage('http://zapp.trakt.us/images/posters_movies/208623-138.jpg'),
				getScrollViewPage('http://zapp.trakt.us/images/posters_movies/210231-138.jpg'),
				getScrollViewPage('http://zapp.trakt.us/images/posters_movies/176347-138.jpg'),
				getScrollViewPage('http://zapp.trakt.us/images/posters_movies/210596-138.jpg')]});
	rootWindow1.add(scrollView);
	rootWindows.push(rootWindow1);
	function openRootWindow(_win) {
		if (slidingMenu.centerView !== _win) {
			slidingMenu.centerView = _win;
		}
		for ( var i = 1; i < otherWindows.length; i++) {
			otherWindows[i].close();
		};
		slidingMenu.closeLeftView();
	}

	var slidingMenu = Ti.UI.createSlideMenu({backgroundColor:backColor,
		navBarHidden:true,
		// panningMode:Ti.UI.MENU_PANNING_NON_SCROLLVIEW,
		orientationModes:[Ti.UI.UPSIDE_PORTRAIT,
				Ti.UI.PORTRAIT,
				Ti.UI.LANDSCAPE_RIGHT,
				Ti.UI.LANDSCAPE_LEFT]});
	var listview = createListView({backgroundColor:'gray',
		sections:[{items:[{properties:{title:'test1'}, callback:function() {
			openRootWindow(rootWindow1)
		}},
				{properties:{title:'test2'}, callback:transform2Ex},
				{properties:{title:'PopIn'}, callback:transform3Ex},
				{properties:{title:'SlideIn'}, callback:transform4Ex},
				{properties:{title:'ListView'}, callback:transform5Ex},
				{properties:{title:'Close'}, callback:function() {
					slidingMenu.close();
				}}]}]});
	slidingMenu.leftView = listview;
	slidingMenu.centerView = Ti.UI.createWindow({backgroundColor:backColor});
	otherWindows.push(slidingMenu);
	slidingMenu.open();
}


var firstWindow = createWin();
var listview = createListView();
var color = cellColor(0);
listview.sections = [{items:[{properties:{title:'Transform', backgroundColor:color}, callback:transformExs},
							{properties:{title:'SlideMenu'}, callback:slideMenuEx},
							{properties:{title:'NavigationWindow'}, callback:navWindowEx},
		{properties:{title:'Layout'}, callback:layoutExs},
		{properties:{title:'listviewEx'}, callback:scrollableEx},
		{properties:{title:'Shapes'}, callback:shapeExs},
		{properties:{title:'ButtonsAndLabels'}, callback:buttonAndLabelEx},
		{properties:{title:'Mask'}, callback:maskEx},
		{properties:{title:'ImageView'}, callback:ImageViewEx},
		{properties:{title:'Transition'}, callback:transitionEx},
		{properties:{title:'AnimationSet'}, callback:transform2Ex},
		{properties:{title:'HTML Label'}, callback:htmlLabelEx},
		{properties:{title:'SVG'}, callback:svgExs},
		{properties:{title:'ListView'}, callback:listViewEx},
		{properties:{title:'ListView2'}, callback:listView2Ex}]}];
firstWindow.add(listview);
var mainWin = Ti.UI.createNavigationWindow({backgroundColor:backColor,
	exitOnClose:true,
	orientationModes:[Ti.UI.UPSIDE_PORTRAIT,
			Ti.UI.PORTRAIT,
			Ti.UI.LANDSCAPE_RIGHT,
			Ti.UI.LANDSCAPE_LEFT],
	window:firstWindow});

mainWin.open();
