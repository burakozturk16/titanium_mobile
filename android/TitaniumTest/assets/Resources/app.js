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
		_win.open();
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
	listview.sections = [{items:[{properties:{title:'Transform'}, callback:transform1Ex},
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

var firstWindow = createWin();
var listview = createListView();
listview.sections = [{items:[{properties:{title:'Transform'}, callback:transformExs},
		{properties:{title:'Layout'}, callback:layoutExs},
		{properties:{title:'listviewEx'}, callback:scrollableEx},
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
