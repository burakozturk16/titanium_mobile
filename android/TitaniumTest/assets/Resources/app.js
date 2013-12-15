var shape = ti.shape;
var isios7 = false;
var isandroid = ti.platform.osname == "android";
var isapple = ti.platform.osname === 'ipad' || ti.platform.osname === 'iphone';
if (isapple) {
	isios7 = parseint(titanium.platform.version.split(".")[0]) >= 7;
}
var backcolor = 'white';
var textcolor = 'black';
var navgroup;
var openwinargs;
var html = '  success     <font color="red">musique</font> électronique <b><span style="background-color:green;border-color:black;border-radius:20px;border-width:4px">est un type de </span><big><big>musique</big></big> qui a <font color="green">été conçu à</font></b> partir des années<br> 1950 avec des générateurs de signaux<br> et de sons synthétiques. avant de pouvoir être utilisée en temps réel, elle a été primitivement enregistrée sur bande magnétique, ce qui permettait aux compositeurs de manier aisément les sons, par exemple dans l\'utilisation de boucles répétitives superposées. ses précurseurs ont pu bénéficier de studios spécialement équipés ou faisaient partie d\'institutions musicales pré-existantes. la musique pour bande de pierre schaeffer, également appelée musique concrète, se distingue de ce type de musique dans la mesure où son matériau primitif était constitué des sons de la vie courante. la particularité de la musique électronique de l\'époque est de n\'utiliser que des sons générés par des appareils électroniques.';
// html = '<span style="border-style:solid;background-color:green;border-color:red;border-radius:20px;border-width:3px;padding-top:3px;padding-bottom:3px;line-height:2em;"> success </span><br><span style="border-style:solid;background-color:green;border-color:red;border-radius:20px;border-width:3px;padding-top:0px;padding-bottom:0px;line-height:1em;"> success </span>'
if (isandroid) {
	backcolor = 'black';
	textcolor = 'gray';
}

function merge_options(obj1, obj2, _new) {
	_new = _new === true;
	var newobject = obj1;
	if (_new === true) {
		newobject = json.parse(json.stringify(obj1));
	}
	for (var attrname in obj2) {
		newobject[attrname] = obj2[attrname];
	}
	return newobject;
}
var initwindowargs = {
	backgroundcolor: backcolor,
	orientationmodes: [ti.ui.upside_portrait,
		ti.ui.portrait,
		ti.ui.landscape_right,
		ti.ui.landscape_left
	]
};
if (isios7) {
	initwindowargs = merge_options(initwindowargs, {
		// autoadjustscrollviewinsets: true,
		// extendedges: [ti.ui.extend_edge_all],
		// translucent: false
	});
}

function createwin(_args) {
	return ti.ui.createwindow(merge_options(initwindowargs, _args, true));
}

function createlistview(_args) {
	var realargs = merge_options({
		allowsselection: false,
		rowheight: 50,
		selectedbackgroundgradient: {
			type: 'linear',
			colors: [{
				color: '#1e232c',
				offset: 0.0
			}, {
				color: '#3f4a58',
				offset: 0.2
			}, {
				color: '#3f4a58',
				offset: 0.8
			}, {
				color: '#1e232c',
				offset: 1
			}],
			startpoint: {
				x: 0,
				y: 0
			},
			endpoint: {
				x: 0,
				y: "100%"
			}
		}
	}, _args);
	var listview = ti.ui.createlistview(realargs);
	listview.addeventlistener('itemclick', function(_event) {
		if (_event.hasownproperty('section') && _event.hasownproperty('itemindex')) {
			var item = _event.section.getitemat(_event.itemindex);
			if (item.callback) {
				item.callback();
			}
		}
	});
	return listview;
}

function varswitch(_var, _val1, _val2) {
	return (_var === _val1) ? _val2 : _val1;
}
var androidactivityssettings = {
	actionbar: {
		displayhomeasup: true,
		onhomeiconitemselected: function(e) {
			e.window.close();
		}
	}
};

function openwin(_win, _withoutactionbar) {
	if (isandroid) {
		if (_withoutactionbar != true) _win.activity = androidactivityssettings;
	}
	mainwin.openwindow(_win);
}

function layoutexs() {
	var win = createwin();
	var listview = createlistview();
	listview.sections = [{
		items: [{
			properties: {
				title: 'animated horizontal'
			},
			callback: layout1ex
		}]
	}];
	win.add(listview);
	openwin(win);
}

function layout1ex() {
	var win = createwin();
	var view = ti.ui.createview({
		backgroundcolor: 'green',
		width: 200,
		height: ti.ui.size,
		layout: 'horizontal'
	});
	var view1 = ti.ui.createview({
		backgroundcolor: 'red',
		width: 60,
		height: 80,
		left: 0
	});
	var view2 = ti.ui.createview({
		backgroundcolor: 'blue',
		width: 20,
		bordercolor: 'red',
		borderwidth: 2,
		// top:10,
		height: 80,
		left: 10,
		right: 4
	});
	var view3 = ti.ui.createview({
		backgroundcolor: 'orange',
		width: ti.ui.fill,
		height: ti.ui.fill,
		bottom: 6,
		right: 4
	});
	view.add(view1);
	view.add(view2);
	view.add({
		backgroundcolor: 'purple',
		width: ti.ui.fill,
		height: ti.ui.fill,
		bottom: 6,
		right: 4
	});
	view.add(view3);
	win.add(view);
	win.addeventlistener('click', function(e) {
		view2.animate({
			cancelrunninganimations: true,
			// restartfrombeginning:true,
			duration: 300,
			autoreverse: true,
			repeat: 4,
			width: ti.ui.fill,
			height: 100,
			top: null,
			left: 0,
			right: 30
		});
	});
	openwin(win);
}

function shapeexs() {
	var win = createwin();
	var listview = createlistview();
	listview.sections = [{
		items: [{
			properties: {
				title: 'arc'
			},
			callback: shape1ex
		}, {
			properties: {
				title: 'circle'
			},
			callback: shape2ex
		}, {
			properties: {
				title: 'line'
			},
			callback: shape3ex
		}, {
			properties: {
				title: 'inversed'
			},
			callback: shape4ex
		}, {
			properties: {
				title: 'shutter'
			},
			callback: shape5ex
		}, {
			properties: {
				title: 'inner shadow'
			},
			callback: shape6ex
		}, {
			properties: {
				title: 'pieslice'
			},
			callback: shape7ex
		}]
	}];
	win.add(listview);
	openwin(win);
}

function shape1ex() {
	var win = createwin();
	var view = shape.createview({
		bubbleparent: false,
		width: 200,
		height: 200
	});
	view.add({
		linecolor: '#777',
		linewidth: 10,
		linecap: shape.cap_round,
		transform: ti.ui.create2dmatrix().rotate(5),
		lineshadow: {
			color: 'white'
		},
		operations: [{
			type: 'arc',
			radius: '45%',
			startangle: -160,
			sweepangle: 320
		}]
	});
	var shape = shape.createarc({
		radius: '45%',
		startangle: -160,
		sweepangle: 190,
		linewidth: 10,
		linecap: shape.cap_round,
		linegradient: {
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
		}
	});
	view.add(shape);
	var anim = ti.ui.createanimation({
		duration: 600,
		autoreverse: true,
		sweepangle: 320
	});
	view.addeventlistener('click', function(e) {
		// shape.cancelallanimations();
		// shape.sweepangle = 320;
		shape.animate(anim);
	});
	win.add(view);
	openwin(win);
}

function shape2ex() {
	var win = createwin();
	var view = shape.createview({
		top: 150,
		borderradius: 10,
		bordercolor: 'red',
		borderwidth: 5,
		bubbleparent: false,
		width: 300,
		height: 100,
		backgroundcolor: 'white',
		transform: ti.ui.create2dmatrix().scale(1.5, 1.5),
		viewmask: '/images/body-mask.png'
	});
	var shape = shape.createcircle({
		fillcolor: '#bbb',
		linecolor: '#777',
		linewidth: 1,
		fillimage: '/images/pattern.png',
		transform: ti.ui.create2dmatrix().scale(0.5, 1),
		lineshadow: {
			radius: 2,
			color: 'black'
		},
		radius: '40%'
	});
	view.add(shape);
	view.add(ti.ui.createview({
		backgroundcolor: 'red',
		bottom: 10,
		width: 30,
		height: 30
	}));
	var anim = ti.ui.createanimation({
		duration: 400,
		linewidth: 20,
		autoreverse: true,
		// restartfrombeginning:true,
		repeat: 2,
		linecolor: 'yellow',
		fillcolor: 'blue'
	});
	shape.addeventlistener('click', function(e) {
		// e.source.cancelallanimations();
		e.source.animate(anim);
	});
	win.add(view);
	openwin(win);
}

function shape3ex() {
	var win = createwin();
	var view = shape.createview({
		bubbleparent: false,
		width: ti.ui.fill,
		height: 200
	});
	var shape = shape.createline({
		linecolor: 'blue',
		linewidth: 6,
		retina: false,
		antialiasing: true,
		linecap: shape.cap_butt,
		linejoin: shape.join_round,
		lineshadow: {
			radius: 3,
			color: 'blue'
		},
		lineimage: '/images/pattern.png',
		// linedash:{
		// 	phase:0,
		// 	pattern:[10,2,10]
		// },
		points: [
			['0%', 0],
			['20%', 20, '20%', 10, '10%', 30],
			['40%', -5],
			['60%', 8],
			['80%', 16],
			['100%', 0]
		]
	});
	view.add(shape);
	view.addeventlistener('click', function(e) {
		shape.animate({
			duration: 400,
			linewidth: 20,
			autoreverse: true,
			linecolor: 'yellow',
			points: [
				['0%', 30],
				['10%', 40, '20%', 10, '10%', 30],
				['40%', 25],
				['60%', -38],
				['80%', 56],
				['100%', 0]
			]
		});
	});
	win.add(view);
	openwin(win);
}

function shape4ex() {
	var win = createwin();
	win.add(ti.ui.createlabel({
		width: ti.ui.fill,
		height: ti.ui.fill,
		bottom: 20,
		html: html
	}));
	var view = shape.createview({
		width: ti.ui.fill,
		height: ti.ui.fill,
		bubbleparent: false
	});
	var shape = shape.createcircle({
		fillcolor: 'transparent',
		linecolor: '#777',
		linewidth: 1,
		retina: false,
		antialiasing: false,
		fillgradient: {
			type: 'radial',
			colors: ['transparent', 'gray'],
			// startpoint:{x:0, y:0},
			// endpoint:{x:0, y:"100%"}
		},
		fillinversed: true,
		fillcolor: 'blue',
		fillshadow: {
			radius: 5,
			color: 'black'
		},
		radius: '20%'
	});
	view.add(shape);
	shape.addeventlistener('click', function(e) {
		e.source.cancelallanimations();
		e.source.animate({
			duration: 400,
			linewidth: 20,
			radius: '40%',
			fillopacity: 0.7,
			autoreverse: true,
			linecolor: 'yellow',
			fillcolor: 'blue'
		});
	});
	win.add(view);
	openwin(win);
}

function shape5ex() {
	var win = createwin();
	win.add(ti.ui.createlabel({
		width: ti.ui.fill,
		height: ti.ui.fill,
		bottom: 20,
		html: html
	}));
	var view = shape.createview({
		width: ti.ui.fill,
		height: ti.ui.fill,
		bubbleparent: false
	});
	var shape = shape.createroundedrect({
		cornerradius: 10,
		// linecolor:'#777',
		// linewidth:4,
		retina: false,
		antialiasing: false,
		fillgradient: {
			type: 'radial',
			colors: ['white', 'gray']
		},
		fillinversed: true,
		fillcolor: 'blue',
		fillshadow: {
			radius: 5,
			color: 'black'
		},
		transform: ti.ui.create2dmatrix().scale(0.0003)
	});
	view.add(shape);
	view.addeventlistener('click', function(e) {
		shape.animate({
			duration: 3000,
			restartfrombeginning: true,
			transform: ti.ui.create2dmatrix().scale(2)
		});
	});
	win.add(view);
	openwin(win);
}

function shape6ex() {
	var win = createwin();
	win.backgroundcolor = 'gray';
	var view = shape.createview({
		width: 200,
		height: 200,
		bubbleparent: false
	});
	view.add(shape.createroundedrect({
		linewidth: 1,
		fillcolor: 'white',
		linecolor: 'gray',
		cornerradius: 10,
		lineclipped: true,
		radius: '43%',
		lineshadow: {
			radius: 4,
			color: 'black',
			offset: {
				x: 0,
				y: -3
			}
		}
	}));
	// view.add({
	// 	linewidth:4,
	// 	fillcolor:'white',
	// 	linecolor:'black',
	// 	cornerradius:10,
	// 	radius:'43%',
	// 	lineshadow:{radius:4, color:'black', offset:{x:0,y:-4}},
	// 	type:'roundedrect'
	// });
	view.add(ti.ui.createlabel({
		left: 14,
		right: 14,
		top: 14,
		bottom: 14,
		width: ti.ui.fill,
		height: ti.ui.fill,
		bottom: 20,
		html: html
	}));
	win.add(view);
	openwin(win);
}

function shape7ex() {
	var win = createwin({
		backgroundcolor: 'gray'
	});
	var view = shape.createview({
		width: 200,
		height: 200,
		bubbleparent: false
	});
	var slice1 = shape.createpieslice({
		fillcolor: '#aa00ffff',
		innerradius: 30,
		startangle: 0,
		radius: '40%',
		sweepangle: 40
	});
	var slice2 = shape.createpieslice({
		fillcolor: '#aaff00ff',
		innerradius: 30,
		startangle: 30,
		sweepangle: 100
	});
	var slice3 = shape.createpieslice({
		fillcolor: '#aaffff00',
		innerradius: 30,
		startangle: -60,
		radius: '20%',
		sweepangle: 10
	});
	view.add({
		type: 'circle',
		radius: 30,
		fillcolor: 'blue'
	});
	view.add(slice1);
	view.add(slice2);
	view.add(slice3);
	win.add(view);
	var anim1 = ti.ui.createanimation({});
	slice1.animate({
		duration: 10000,
		startangle: 360,
		repeat: ti.ui.infinite
	});
	slice2.animate({
		duration: 5000,
		startangle: 200,
		autoreverse: true,
		repeat: ti.ui.infinite
	});
	slice3.animate({
		duration: 4000,
		startangle: -420,
		repeat: ti.ui.infinite
	});
	var anim1 = ti.ui.createanimation({
		duration: 400,
		radius: '50%',
		restartfrombeginning: true,
		autoreverse: true
	});
	var anim2 = ti.ui.createanimation({
		duration: 700,
		radius: '30%',
		repeat: 3,
		autoreverse: true
	});
	var anim3 = ti.ui.createanimation({
		duration: 300,
		radius: '30%'
	});

	view.addeventlistener('click', function(e) {
		slice1.animate(anim1);
		slice2.animate(anim2);
		// anim3.cancel();
		slice3.animate(anim3);
	});
	openwin(win);
}

function buttonandlabelex() {
	var win = createwin({
		navbarhidden: true,
		dispatchpressed: true,
		backgroundselectedcolor: 'green'
	});
	var button = ti.ui.createbutton({
		top: 0,
		padding: {
			left: 30,
			top: 30,
			bottom: 30,
			right: 30
		},
		height: 50,
		bubbleparent: false,
		backgroundcolor: 'gray',
		touchpassthrough: false,
		backgroundselectedgradient: {
			type: 'linear',
			colors: ['#333', 'transparent'],
			startpoint: {
				x: 0,
				y: 0
			},
			endpoint: {
				x: 0,
				y: "100%"
			}
		},
		title: 'test buutton'
	});
	button.add(ti.ui.createview({
		enabled: false,
		backgroundcolor: 'purple',
		backgroundselectedcolor: 'white',
		left: 10,
		width: 15,
		height: 15
	}));
	button.add(ti.ui.createview({
		backgroundcolor: 'green',
		bottom: 10,
		width: 15,
		height: 15
	}));
	button.add(ti.ui.createview({
		backgroundcolor: 'yellow',
		top: 10,
		width: 15,
		height: 15
	}));
	button.add(ti.ui.createview({
		touchpassthrough: true,
		backgroundcolor: 'orange',
		right: 0,
		width: 35,
		height: ti.ui.fill
	}));
	var t1 = ti.ui.create2dmatrix();
	var t2 = ti.ui.create2dmatrix().scale(2.0, 2.0).translate(0, 40).rotate(90);
	button.addeventlistener('longpress', function(e) {
		button.animate({
			duration: 500,
			transform: varswitch(button.transform, t2, t1),
		});
	});
	button.addeventlistener('touchstart', function(e) {
		alert('stste');
	});
	win.add(button);
	var label = ti.ui.createlabel({
		bottom: 20,
		// dispatchpressed: true,
		backgroundcolor: 'gray',
		backgroundselectedcolor: '#a46',
		padding: {
			left: 30,
			top: 30,
			bottom: 30,
			right: 30
		},
		bubbleparent: false,
		selectedcolor: 'green',
		backgroundselectedgradient: {
			type: 'linear',
			colors: ['#333', 'transparent'],
			startpoint: {
				x: 0,
				y: 0
			},
			endpoint: {
				x: 0,
				y: "100%"
			}
		},
		text: 'this is a sample\n text for a label'
	});
	label.add(ti.ui.createview({
		touchenabled: false,
		backgroundcolor: 'red',
		backgroundselectedcolor: 'white',
		left: 10,
		width: 15,
		height: 15
	}));
	label.add(ti.ui.createview({
		backgroundcolor: 'green',
		bottom: 10,
		width: 15,
		height: 15
	}));
	label.add(ti.ui.createview({
		backgroundcolor: 'yellow',
		top: 10,
		width: 15,
		height: 15
	}));
	label.add(ti.ui.createview({
		backgroundcolor: 'orange',
		right: 10,
		width: 15,
		height: 15
	}));
	var t3 = ti.ui.create2dmatrix().scale(2.0, 2.0).translate(0, -40).rotate(90);
	label.addeventlistener('longpress', function(e) {
		label.animate({
			duration: 500,
			transform: varswitch(label.transform, t3, t1),
		});
	});
	win.add(label);
	var button2 = ti.ui.createbutton({
		padding: {
			left: 80
		},
		bubbleparent: false,
		backgroundcolor: 'gray',
		dispatchpressed: true,
		selectedcolor: 'red',
		backgroundselectedgradient: {
			type: 'linear',
			colors: ['#333', 'transparent'],
			startpoint: {
				x: 0,
				y: 0
			},
			endpoint: {
				x: 0,
				y: "100%"
			}
		},
		title: 'test buutton'
	});
	button2.add(ti.ui.createbutton({
		left: 0,
		backgroundcolor: 'green',
		selectedcolor: 'red',
		backgroundselectedgradient: {
			type: 'linear',
			colors: ['#333', 'transparent'],
			startpoint: {
				x: 0,
				y: 0
			},
			endpoint: {
				x: 0,
				y: "100%"
			}
		},
		title: 'osd'
	}));
	win.add(button2);
	openwin(win);
}

function pulltorefresh() {
	var win = createwin();
	var sections = [];

	var fruitsection = ti.ui.createlistsection({
		headertitle: 'fruits'
	});
	var fruitdataset = [
		{
			properties: {
				title: 'apple'
			}
		},
		{
			properties: {
				title: 'banana'
			}
		},
		{
			properties: {
				title: 'cantaloupe'
			}
		},
		{
			properties: {
				title: 'fig'
			}
		},
		{
			properties: {
				title: 'guava'
			}
		},
		{
			properties: {
				title: 'kiwi'
			}
		}
	];
	fruitsection.setitems(fruitdataset);
	sections.push(fruitsection);

	var vegsection = ti.ui.createlistsection({
		headertitle: 'vegetables'
	});
	var vegdataset = [
		{
			properties: {
				title: 'carrots'
			}
		},
		{
			properties: {
				title: 'potatoes'
			}
		},
		{
			properties: {
				title: 'corn'
			}
		},
		{
			properties: {
				title: 'beans'
			}
		},
		{
			properties: {
				title: 'tomato'
			}
		}
	];
	vegsection.setitems(vegdataset);

	var fishsection = ti.ui.createlistsection({
		headertitle: 'fish'
	});
	var fishdataset = [
		{
			properties: {
				title: 'cod'
			}
		},
		{
			properties: {
				title: 'haddock'
			}
		},
		{
			properties: {
				title: 'salmon'
			}
		},
		{
			properties: {
				title: 'tuna'
			}
		}
	];
	fishsection.setitems(fishdataset);

	var refreshcount = 0;

	function getformatteddate() {
		var date = new date();
		return date.getmonth() + '/' + date.getdate() + '/' + date.getfullyear() + ' ' + date.gethours() + ':' + date.getminutes();
	}

	function resetpullheader() {
		actind.hide();
		imagearrow.transform = ti.ui.create2dmatrix();
		if (refreshcount < 2) {
			imagearrow.show();
			labelstatus.text = 'pull down to refresh...';
			labellastupdated.text = 'last updated: ' + getformatteddate();
		} else {
			// labelstatus.text = 'nothing to refresh';
			// labellastupdated.text = 'go cook something';
			// listview.removeeventlistener('pull', pulllistener);
			// listview.removeeventlistener('pullend', pullendlistener);
			// eventstatus.text = 'removed event listeners.';
		}
		listview.closepullview();
	}

	function loadtabledata() {
		if (refreshcount == 0) {
			listview.appendsection(vegsection);
		} else if (refreshcount == 1) {
			listview.appendsection(fishsection);
		}
		refreshcount++;
		resetpullheader();
	}
	var currentactive;

	function pulllistener(e) {
		if (e.active === currentactive) return;
		currentactive = e.active;
		eventstatus.text = 'event pull fired. e.active = ' + e.active;
		if (e.active == false) {
			var unrotate = ti.ui.create2dmatrix();
			imagearrow.animate({
				transform: unrotate,
				duration: 180
			});
			labelstatus.text = 'pull down to refresh...';
		} else {
			var rotate = ti.ui.create2dmatrix().rotate(180);
			imagearrow.animate({
				transform: rotate,
				duration: 180
			});
			if (refreshcount == 0) {
				labelstatus.text = 'release to get vegetables...';
			} else {
				labelstatus.text = 'release to get fish...';
			}
		}
	}

	function pullendlistener(e) {
		eventstatus.text = 'event pullend fired.';

		if (refreshcount == 0) {
			labelstatus.text = 'loading vegetables...';
		} else {
			labelstatus.text = 'loading fish...';
		}
		imagearrow.hide();
		actind.show();
		listview.showpullview();
		settimeout(function() {
			loadtabledata();
		}, 2000);
	}

	var tableheader = ti.ui.createview({
		backgroundcolor: '#e2e7ed',
		width: ti.ui.fill,
		height: 80
	});

	var border = ti.ui.createview({
		backgroundcolor: '#576c89',
		bottom: 0,
		height: 2
	});
	tableheader.add(border);

	var imagearrow = ti.ui.createimageview({
		image: 'https://github.com/appcelerator/titanium_mobile/raw/master/demos/kitchensink/resources/images/whitearrow.png',
		left: 20,
		bottom: 10,
		width: 23,
		height: 60
	});
	tableheader.add(imagearrow);

	var labelstatus = ti.ui.createlabel({
		color: '#576c89',
		font: {
			fontsize: 13,
			fontweight: 'bold'
		},
		text: 'pull down to refresh...',
		textalign: 'center',
		left: 55,
		bottom: 30,
		width: 200
	});
	tableheader.add(labelstatus);

	var labellastupdated = ti.ui.createlabel({
		color: '#576c89',
		font: {
			fontsize: 12
		},
		text: 'last updated: ' + getformatteddate(),
		textalign: 'center',
		left: 55,
		bottom: 15,
		width: 200
	});
	tableheader.add(labellastupdated);

	var actind = ti.ui.createactivityindicator({
		left: 20,
		bottom: 13,
		width: 30,
		height: 30
	});
	tableheader.add(actind);

	var listview = ti.ui.createlistview({
		height: '90%',
		top: 0,
		rowheight: 50,
		sections: sections,
		pullview: tableheader
	});

	listview.addeventlistener('pull', pulllistener);

	listview.addeventlistener('pullend', pullendlistener);

	var eventstatus = ti.ui.createlabel({
		font: {
			fontsize: 13,
			fontweight: 'bold'
		},
		text: 'event data will show here',
		bottom: 0,
		height: '10%'
	})

	win.add(listview);
	win.add(eventstatus);
	openwin(win);
}

function maskex() {
	var win = createwin();
	win.backgroundgradient = {
		type: 'linear',
		colors: ['gray', 'white'],
		startpoint: {
			x: 0,
			y: 0
		},
		endpoint: {
			x: 0,
			y: "100%"
		}
	};
	var view = ti.ui.createview({
		top: 20,
		borderradius: 10,
		bordercolor: 'red',
		borderwidth: 5,
		bubbleparent: false,
		width: 300,
		height: 100,
		backgroundcolor: 'green',
		viewmask: '/images/body-mask.png',
		backgroundgradient: {
			type: 'linear',
			colors: ['red', 'green', 'orange'],
			startpoint: {
				x: 0,
				y: 0
			},
			endpoint: {
				x: 0,
				y: "100%"
			}
		}
	});
	var imageview = ti.ui.createimageview({
		bottom: 20,
		// borderradius : 10,
		// bordercolor:'red',
		// borderwidth:5,
		bubbleparent: false,
		width: 300,
		height: 100,
		backgroundcolor: 'yellow',
		scaletype: ti.ui.scale_type_aspect_fit,
		image: '/images/slightlylargerimage.png',
		imagemask: '/images/body-mask.png',
		// viewmask : '/images/mask.png',
	});
	view.add(ti.ui.createview({
		backgroundcolor: 'red',
		bottom: 10,
		width: 30,
		height: 30
	}));
	win.add(view);
	win.add(imageview);
	win.add(ti.ui.createbutton({
		borderradius: 20,
		padding: {
			left: 30,
			top: 30,
			bottom: 30,
			right: 30
		},
		bubbleparent: false,
		title: 'test buutton',
		viewmask: '/images/body-mask.png'
	}));
	openwin(win);
}

function imageviewex() {
	var win = createwin();
	var view = ti.ui.createimageview({
		bubbleparent: false,
		width: 300,
		height: ti.ui.size,
		bordercolor: 'red',
		borderwidth: 2,
		backgroundcolor: 'green',
		backgroundgradient: {
			type: 'linear',
			colors: ['red', 'green', 'orange'],
			startpoint: {
				x: 0,
				y: 0
			},
			endpoint: {
				x: 0,
				y: "100%"
			}
		},
		image: '/images/slightlylargerimage.png',
	});
	view.add(ti.ui.createview({
		backgroundcolor: 'yellow',
		top: 10,
		width: 15,
		height: 15
	}));
	view.addeventlistener('click', function() {
		//		view.image = varswitch(view.image, '/images/slightlylargerimage.png', '/images/poster.jpg');
		view.animate({
			height: 400,
			duration: 1000,
			autoreverse: true
		});
	});
	win.add(view);
	openwin(win);
}

function random(min, max) {
	if (max == null) {
		max = min;
		min = 0;
	}
	return min + math.floor(math.random() * (max - min + 1));
};

function scrollableex() {
	var win = createwin();
	// create a custom template that displays an image on the left,
	// then a title next to it with a subtitle below it.
	var mytemplate = {
		properties: {
			height: 50
		},
		childtemplates: [{
			type: 'ti.ui.imageview',
			bindid: 'leftimageview',
			properties: {
				left: 0,
				width: 40,
				localloadsync: true,
				height: 40,
				transform: ti.ui.create2dmatrix().rotate(90),
				backgroundcolor: 'blue',
				// backgroundselectedcolor:'green',
				image: '/images/contacticon.png',
				// bordercolor:'red',
				// borderwidth:2
				viewmask: '/images/contactmask.png',
			}
		}, {
			type: 'ti.ui.label',
			bindid: 'label',
			properties: {
				multilineellipsize: ti.ui.text_ellipsize_tail,
				top: 2,
				bottom: 2,
				left: 45,
				padding: {
					bottom: 4
				},
				right: 55,
				touchenabled: false,
				height: ti.ui.fill,
				color: 'black',
				font: {
					fontsize: 16
				},
				width: ti.ui.fill
			}
		}, {
			type: 'ti.ui.imageview',
			bindid: 'rightimageview',
			properties: {
				right: 5,
				top: 8,
				localloadsync: true,
				bottom: 8,
				width: ti.ui.size,
				touchenabled: false
			}
		}, {
			type: 'ti.ui.imageview',
			bindid: 'networkindicator',
			properties: {
				right: 40,
				top: 4,
				localloadsync: true,
				height: 15,
				width: ti.ui.size,
				touchenabled: false
			}
		}, {
			type: 'ti.ui.view',
			properties: {
				backgroundcolor: '#999',
				left: 4,
				right: 4,
				bottom: 0,
				height: 1
			}
		}]
	};
	var contactaction;
	var blurimage;
	var listview = ti.ui.createlistview({
		// maps mytemplate dictionary to 'template' string
		templates: {
			'template': mytemplate
		},
		defaultitemtemplate: 'template',
		selectedbackgroundgradient: {
			type: 'linear',
			colors: ['blue', 'green'],
			startpoint: {
				x: 0,
				y: 0
			},
			endpoint: {
				x: 0,
				y: "100%"
			}
		}
	});
	listview.addeventlistener('itemclick', function(_event) {
		if (_event.hasownproperty('section') && _event.hasownproperty('itemindex')) {
			var item = _event.section.getitemat(_event.itemindex);
			if (!contactaction) {
				contactaction = ti.ui.createview({
					backgroundcolor: 'green'
				});
				blurimage = ti.ui.createimageview({
					scaletype: ti.ui.scale_type_aspect_fill,
					width: ti.ui.fill,
					height: ti.ui.fill
				});
				contactaction.add(blurimage);
				blurimage.addeventlistener('click', function() {
					animation.fadeout(contactaction, 200, function() {
						win.remove(contactaction);
					});
				});
			}
			contactaction.opacity = 0;
			win.add(contactaction);
			var image = ti.media.takescreenshot();
			// var image = ti.image.getfilteredviewtoimage(win, ti.image.filter_gaussian_blur, {scale:0.3});
			blurimage.image = image;
			animation.fadein(contactaction, 300);
		}
	});
	var names = ['carolyn humbert',
		'david michaels',
		'rebecca thorning',
		'joe b',
		'phillip craig',
		'michelle werner',
		'philippe christophe',
		'marcus crane',
		'esteban valdez',
		'sarah mullock'
	];

	function formattitle(_history) {
		return _history.fullname + '<br><small><small><b><font color="#5b5b5b">' + (new date()).tostring() + '</font> <font color="#3fac53"></font></b></small></small>';
	}

	function random(min, max) {
		if (max == null) {
			max = min;
			min = 0;
		}
		return min + math.floor(math.random() * (max - min + 1));
	};

	function update() {
		// var outgoingimage = ti.utils.imageblob('/images/outgoing.png');
		// var incomingimage = ti.utils.imageblob('/images/incoming.png');
		var dataset = [];
		for (var i = 0; i < 300; i++) {
			var callhistory = {
				fullname: names[math.floor(math.random() * names.length)],
				date: random(1293886884000, 1376053320000),
				kb: random(0, 100000),
				outgoing: !! random(0, 1),
				wifi: !! random(0, 1)
			};
			dataset.push({
				contactname: callhistory.fullname,
				label: {
					html: formattitle(callhistory)
				},
				rightimageview: {
					image: (callhistory.outgoing ? '/images/outgoing.png' : '/images/incoming.png')
				},
				networkindicator: {
					image: (callhistory.wifi ? '/images/wifi.png' : '/images/mobile.png')
				}
			});
		}
		var historysection = ti.ui.createlistsection();
		historysection.setitems(dataset);
		listview.sections = [historysection];
	}
	win.add(listview);
	win.addeventlistener('open', update);
	openwin(win);
}

function listview2ex() {
	var win = createwin();
	// create a custom template that displays an image on the left,
	// then a title next to it with a subtitle below it.
	var mytemplate = {
		childtemplates: [{ // image justified left
			type: 'ti.ui.imageview', // use an image view for the image
			bindid: 'pic', // maps to a custom pic property of the item data
			properties: { // sets the image view  properties
				width: '50dp',
				height: '50dp',
				left: 0
			}
		}, { // title
			type: 'ti.ui.label', // use a label for the title
			bindid: 'info', // maps to a custom info property of the item data
			properties: { // sets the label properties
				color: 'black',
				font: {
					fontfamily: 'arial',
					fontsize: '20dp',
					fontweight: 'bold'
				},
				left: '60dp',
				top: 0,
			}
		}, { // subtitle
			type: 'ti.ui.label', // use a label for the subtitle
			bindid: 'es_info', // maps to a custom es_info property of the item data
			properties: { // sets the label properties
				color: 'gray',
				font: {
					fontfamily: 'arial',
					fontsize: '14dp'
				},
				left: '60dp',
				top: '25dp',
			}
		}]
	};
	var listview = ti.ui.createlistview({
		// maps mytemplate dictionary to 'template' string
		templates: {
			'template': mytemplate
		},
		// use 'template', that is, the mytemplate dict created earlier
		// for all items as long as the template property is not defined for an item.
		defaultitemtemplate: 'template',
		selectedbackgroundgradient: {
			type: 'linear',
			colors: ['blue', 'green'],
			startpoint: {
				x: 0,
				y: 0
			},
			endpoint: {
				x: 0,
				y: "100%"
			}
		}
	});
	if (isapple) listview.style = titanium.ui.iphone.listviewstyle.grouped;
	var sections = [];
	var fruitsection = ti.ui.createlistsection({
		headertitle: 'fruits / frutas'
	});
	var fruitdataset = [
		// the text property of info maps to the text property of the title label
		// the text property of es_info maps to text property of the subtitle label
		// the image property of pic maps to the image property of the image view
		{
			info: {
				text: 'apple'
			},
			es_info: {
				text: 'manzana'
			},
			pic: {
				image: 'apple.png'
			}
		}, {
			properties: {
				backgroundcolor: 'red'
			},
			info: {
				text: 'banana'
			},
			es_info: {
				text: 'banana'
			},
			pic: {
				image: 'banana.png'
			}
		}
	];
	fruitsection.setitems(fruitdataset);
	sections.push(fruitsection);
	var vegsection = ti.ui.createlistsection({
		headertitle: 'vegetables / verduras'
	});
	var vegdataset = [{
		info: {
			text: 'carrot'
		},
		es_info: {
			text: 'zanahoria'
		},
		pic: {
			image: 'carrot.png'
		}
	}, {
		info: {
			text: 'potato'
		},
		es_info: {
			text: 'patata'
		},
		pic: {
			image: 'potato.png'
		}
	}];
	vegsection.setitems(vegdataset);
	sections.push(vegsection);
	var grainsection = ti.ui.createlistsection({
		headertitle: 'grains / granos'
	});
	var graindataset = [{
		info: {
			text: 'corn'
		},
		es_info: {
			text: 'maiz'
		},
		pic: {
			image: 'corn.png'
		}
	}, {
		info: {
			text: 'rice'
		},
		es_info: {
			text: 'arroz'
		},
		pic: {
			image: 'rice.png'
		}
	}];
	grainsection.setitems(graindataset);
	sections.push(grainsection);
	listview.setsections(sections);
	win.add(listview);
	openwin(win);
}

function listviewex() {
	var win = createwin();
	var listview = ti.ui
		.createlistview({
			allowsselection: false,
			rowheight: 50,
			selectedbackgroundgradient: {
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
						backgroundcolor: 'blue',
						title: 'shape'
					}
				}, {
					properties: {
						backgroundcolor: 'red',
						title: 'buttonsandlabels'
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						title: 'transform',
						accessorytype: ti.ui.list_accessory_type_checkmark
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						backgroundcolor: 'red',
						title: 'shape',
						accessorytype: ti.ui.list_accessory_type_checkmark
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						title: 'transform',
						accessorytype: ti.ui.list_accessory_type_checkmark
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						backgroundcolor: 'red',
						title: 'shape'
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						title: 'transform',
						accessorytype: ti.ui.list_accessory_type_checkmark
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						title: 'transform',
						accessorytype: ti.ui.list_accessory_type_checkmark
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						backgroundcolor: 'red',
						title: 'shape',
						accessorytype: ti.ui.list_accessory_type_checkmark
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						title: 'transform',
						accessorytype: ti.ui.list_accessory_type_checkmark
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						backgroundcolor: 'red',
						title: 'shape'
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						title: 'transform',
						accessorytype: ti.ui.list_accessory_type_checkmark
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						title: 'transform',
						accessorytype: ti.ui.list_accessory_type_checkmark
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						backgroundcolor: 'red',
						title: 'shape',
						accessorytype: ti.ui.list_accessory_type_checkmark
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						title: 'transform',
						accessorytype: ti.ui.list_accessory_type_checkmark
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						backgroundcolor: 'red',
						title: 'shape'
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						title: 'transform',
						accessorytype: ti.ui.list_accessory_type_checkmark
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						title: 'transform',
						accessorytype: ti.ui.list_accessory_type_checkmark
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						backgroundcolor: 'red',
						title: 'shape',
						accessorytype: ti.ui.list_accessory_type_checkmark
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						title: 'transform',
						accessorytype: ti.ui.list_accessory_type_checkmark
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						backgroundcolor: 'red',
						title: 'shape'
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						title: 'transform',
						accessorytype: ti.ui.list_accessory_type_checkmark
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						title: 'transform',
						accessorytype: ti.ui.list_accessory_type_checkmark
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						backgroundcolor: 'red',
						title: 'shape',
						accessorytype: ti.ui.list_accessory_type_checkmark
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						title: 'transform',
						accessorytype: ti.ui.list_accessory_type_checkmark
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						backgroundcolor: 'red',
						title: 'shape'
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						title: 'transform',
						accessorytype: ti.ui.list_accessory_type_checkmark
					}
				}, {
					properties: {
						title: 'shape'
					}
				}, {
					properties: {
						title: 'transform',
						accessorytype: ti.ui.list_accessory_type_checkmark
					}
				}]
			}]
		});
	if (isapple) listview.style = titanium.ui.iphone.listviewstyle.grouped;
	win.add(listview);
	openwin(win);
}

function fadeinex() {
	var win = createwin();
	var view = ti.ui.createview({
		backgroundcolor: 'red',
		opacity: 0,
		width: 200,
		height: 200
	});
	view.add(ti.ui.createview({
		backgroundcolor: 'blue',
		bottom: 10,
		width: 50,
		height: 50
	}));
	var showme = function() {
		view.opacity = 0;
		view.transform = ti.ui.create2dmatrix().scale(0.6, 0.6);
		win.add(view);
		view.animate({
			opacity: 1,
			duration: 300,
			transform: ti.ui.create2dmatrix()
		});
	};
	var hideme = function(_callback) {
		view.animate({
			opacity: 0,
			duration: 200
		}, function() {
			win.remove(view);
		});
	};
	var button = ti.ui.createbutton({
		top: 10,
		width: 100,
		bubbleparent: false,
		title: 'test buutton'
	});
	button.addeventlistener('click', function(e) {
		if (view.opacity === 0) showme();
		else hideme();
	});
	win.add(button);
	openwin(win);
}
// if (false) {
// 	var set = ti.ui.createanimationset({
// 		playmode : 1
// 	});
// 	set.addeventlistener('complete', function(e) {
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
// 	view.addeventlistener('click', function(e) {
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
// 	win.addeventlistener('click', function(e) {
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
// function transform2ex() {
// 	var win = createwin();
// 	var view = shape.createview({
// 		top : 150,
// 		borderradius : 10,
// 		bordercolor : 'red',
// 		borderwidth : 5,
// 		bubbleparent : false,
// 		width : 300,
// 		height : 100,
// 		backgroundcolor : 'white',
// 		transform : ti.ui.create2dmatrix().scale(2.0, 2.0),
// 		viewmask : '/images/body-mask.png'
// 	});
// 	var button = ti.ui.createbutton({
// 		top : 10,
// 		width : 100,
// 		transform : ti.ui.create2dmatrix().scale(2.0, 2.0).translate(0, 40),
// 		bubbleparent : false,
// 		title : 'test buutton'
// 	});
// 	button.addeventlistener('click', function(e) {
// 		view.top -=1;
// 	});
// 	button.add(ti.ui.createview({
// 		backgroundcolor : 'red',
// 		bottom : 10,
// 		width : 5,
// 		height : 5
// 	}));
// 	var shape = shape.createcircle({
// 		fillcolor : '#bbb',
// 		linecolor : '#777',
// 		linewidth : 1,
// 		lineshadow : {
// 			radius : 2,
// 			color : 'black'
// 		},
// 		radius : '45%'
// 	});
// 	view.add(shape);
// 	view.add(ti.ui.createview({
// 		backgroundcolor : 'red',
// 		bottom : 10,
// 		width : 30,
// 		height : 30
// 	}));
// 	view.addeventlistener('click', function(e) {
// 		if (isandroid)
// 			set.cancel();
// 		shape.animate({
// 			duration : 400,
// 			linewidth : 20,
// 			autoreverse : true,
// 			linecolor : 'yellow',
// 			fillcolor : 'blue'
// 		});
// 	});
// 	win.add(view);
// 	win.add(button);
// 	if (isandroid) {
// 		var set = ti.ui.createanimationset({
// 			playmode : 2
// 		});
// 		set.add({
// 			duration : 300,
// 			autoreverse : true,
// 			height : 300
// 		}, view);
// 		set.add({
// 			duration : 1000,
// 			linewidth : 20,
// 			autoreverse : true,
// 			linecolor : 'yellow',
// 			fillcolor : 'blue'
// 		}, shape);
// 		win.addeventlistener('click', function(e) {
// 			shape.cancelallanimations();
// 			set.start();
// 		});
// 	}
// 	else {
// 		win.addeventlistener('click', function(e) {
// 			view.animate({
// 				duration : 300,
// 				autoreverse : true,
// 				height : 300
// 			});
// 		});
// 	}
// 	openwin(win);
// }

function htmllabelex() {
	var win = createwin();
	var scrollview = ti.ui.createscrollview({
		layout: 'vertical',
		contentheight: ti.ui.size
	});
	scrollview.add(ti.ui.createlabel({
		width: ti.ui.fill,
		padding: {
			left: 20,
			right: 20,
			top: 20,
			bottom: 20
		},
		height: ti.ui.size,
		bottom: 20,
		html: html
	}));
	scrollview.add(ti.ui.createlabel({
		multilineellipsize: ti.ui.text_ellipsize_head,
		truncationstring: '_ _',
		bottom: 20,
		html: html
	}));
	scrollview.add(ti.ui.createlabel({
		multilineellipsize: ti.ui.text_ellipsize_middle,
		bottom: 20,
		html: html
	}));
	scrollview.add(ti.ui.createlabel({
		width: ti.ui.fill,
		height: ti.ui.size,
		bottom: 20,
		multilineellipsize: ti.ui.text_ellipsize_tail,
		html: html
	}));
	scrollview.add(ti.ui.createlabel({
		width: 200,
		height: ti.ui.size,
		backgorundcolor: 'green',
		bottom: 20,
		multilineellipsize: ti.ui.text_ellipsize_tail,
		html: html
	}));
	scrollview.add(ti.ui.createlabel({
		width: 200,
		height: ti.ui.size,
		backgorundcolor: 'blue',
		bottom: 20,
		ellipsize: ti.ui.text_ellipsize_tail,
		html: html
	}));
	scrollview.add(ti.ui.createlabel({
		height: 200,
		bottom: 20,
		ellipsize: ti.ui.text_ellipsize_tail,
		html: html
	}));
	win.add(scrollview);

	openwin(win);
}

function svgexs() {
	var win = createwin();
	var listview = createlistview();
	listview.sections = [{
		items: [{
			properties: {
				title: 'view'
			},
			callback: svg1ex
		}, {
			properties: {
				title: 'button'
			},
			callback: svg2ex
		}, {
			properties: {
				title: 'imageview'
			},
			callback: svg3ex
		}, {
			properties: {
				title: 'listview'
			},
			callback: svg4ex
		}]
	}];
	win.add(listview);
	openwin(win);
}

function svg1ex() {
	var win = createwin();
	var view = ti.ui.createview({
		bubbleparent: false,
		width: 100,
		height: 100,
		backgroundcolor: 'yellow',
		scaletype: ti.ui.scale_type_aspect_fit,
		preventdefaultimage: true,
		backgroundimage: '/images/notepad_icon_small.svg'
	});
	win.add(view);
	var button = ti.ui.createbutton({
		top: 20,
		bubbleparent: false,
		title: 'change svg'
	});
	button.addeventlistener('click', function() {
		view.backgroundimage = varswitch(view.backgroundimage, '/images/gradients.svg', '/images/logo.svg');
	});
	win.add(button);
	var button2 = ti.ui.createbutton({
		bottom: 20,
		bubbleparent: false,
		title: 'animate'
	});
	button2.addeventlistener('click', function() {
		view.animate({
			height: ti.ui.fill,
			width: ti.ui.fill,
			duration: 2000,
			autoreverse: true
		});
	});
	win.add(button2);
	openwin(win);
}

function svg2ex() {
	var win = createwin();
	var button = ti.ui.createbutton({
		top: 20,
		padding: {
			left: 30,
			top: 30,
			bottom: 30,
			right: 30
		},
		bubbleparent: false,
		image: '/images/logo.svg',
		title: 'test buutton'
	});
	win.add(button);
	openwin(win);
}

function svg3ex() {
	var win = createwin({
		backgroundimage: '/images/notepad_icon_small.svg'
	});
	var imageview = ti.ui.createimageview({
		bubbleparent: false,
		width: 300,
		height: 100,
		backgroundcolor: 'yellow',
		scaletype: ti.ui.scale_type_aspect_fit,
		preventdefaultimage: true,
		image: '/images/notepad_icon_small.svg'
	});
	imageview.addeventlistener('click', function() {
		imageview.scaletype = (imageview.scaletype + 1) % 6;
	});
	win.add(imageview);
	var button = ti.ui.createbutton({
		top: 20,
		bubbleparent: false,
		title: 'change svg'
	});
	button.addeventlistener('click', function() {
		imageview.image = varswitch(imageview.image, '/images/gradients.svg', '/images/logo.svg');
	});
	win.add(button);
	var button2 = ti.ui.createbutton({
		bottom: 20,
		bubbleparent: false,
		title: 'animate'
	});
	button2.addeventlistener('click', function() {
		imageview.animate({
			height: 400,
			duration: 1000,
			autoreverse: true
		});
	});
	win.add(button2);
	openwin(win);
}

function svg4ex() {
	var win = createwin();
	var mytemplate = {
		childtemplates: [{
			type: 'ti.ui.view',
			bindid: 'holder',
			properties: {
				width: ti.ui.fill,
				height: ti.ui.fill,
				touchenabled: false,
				layout: 'horizontal',
				horizontalwrap: false
			},
			childtemplates: [{
				type: 'ti.ui.imageview',
				bindid: 'pic',
				properties: {
					touchenabled: false,
					height: 50,
					image: '/images/gradients.svg'
				}
			}, {
				type: 'ti.ui.label',
				bindid: 'info',
				properties: {
					color: textcolor,
					touchenabled: false,
					font: {
						fontsize: 20,
						fontweight: 'bold'
					},
					width: ti.ui.fill,
					left: 10
				}
			}, {
				type: 'ti.ui.button',
				bindid: 'button',
				properties: {
					title: 'menu',
					left: 10
				}
			}]
		}, {
			type: 'ti.ui.label',
			bindid: 'menu',
			properties: {
				color: 'white',
				text: 'i am the menu',
				backgroundcolor: '#444',
				width: ti.ui.fill,
				height: ti.ui.fill,
				opacity: 0
			},
		}]
	};
	var listview = createlistview({
		templates: {
			'template': mytemplate
		},
		defaultitemtemplate: 'template'
	});
	var sections = [{
		headertitle: 'fruits / frutas',
		items: [{
			info: {
				text: 'apple'
			}
		}, {
			properties: {
				backgroundcolor: 'red'
			},
			info: {
				text: 'banana'
			},
			pic: {
				image: 'banana.png'
			}
		}]
	}, {
		headertitle: 'vegetables / verduras',
		items: [{
			info: {
				text: 'carrot'
			}
		}, {
			info: {
				text: 'potato'
			}
		}, {
			info: {
				text: 'potato'
			},
			pic: {
				image: '/images/opacity.svg'
			}
		}, {
			info: {
				text: 'potato'
			}
		}, {
			info: {
				text: 'potato'
			}
		}, {
			info: {
				text: 'potato'
			}
		}, {
			info: {
				text: 'potato'
			}
		}, {
			info: {
				text: 'potato'
			}
		}, {
			info: {
				text: 'potato'
			}
		}, {
			info: {
				text: 'potato'
			}
		}, {
			info: {
				text: 'potato'
			},
			pic: {
				image: '/images/opacity.svg'
			}
		}, {
			info: {
				text: 'potato'
			}
		}, {
			info: {
				text: 'potato'
			}
		}, {
			info: {
				text: 'potato'
			}
		}, {
			info: {
				text: 'potato'
			}
		}, {
			info: {
				text: 'potato'
			}
		}, {
			info: {
				text: 'potato'
			}
		}, {
			info: {
				text: 'potato'
			}
		}, {
			info: {
				text: 'potato'
			},
			pic: {
				image: '/images/logo.svg'
			}
		}, {
			info: {
				text: 'potato'
			}
		}, {
			info: {
				text: 'potato'
			}
		}, {
			info: {
				text: 'potato'
			}
		}]
	}, {
		headertitle: 'grains / granos',
		items: [{
			info: {
				text: 'corn'
			}
		}, {
			info: {
				text: 'rice'
			}
		}]
	}];
	listview.setsections(sections);
	win.add(listview);
	openwin(win);
}

function float2color(pr, pg, pb) {
	var color_part_dec = 255 * pr;
	var r = number(parseint(color_part_dec, 10)).tostring(16);
	color_part_dec = 255 * pg;
	var g = number(parseint(color_part_dec, 10)).tostring(16);
	color_part_dec = 255 * pb;
	var b = number(parseint(color_part_dec, 10)).tostring(16);
	return "#" + r + g + b;
}

function cellcolor(_index) {
	switch (_index % 4) {
		case 0: // green
			return float2color(0.196, 0.651, 0.573);
		case 1: // orange
			return float2color(1, 0.569, 0.349);
		case 2: // red
			return float2color(0.949, 0.427, 0.427);
			break;
		case 3: // blue
			return float2color(0.322, 0.639, 0.800);
			break;
		default:
			break;
	}
}
var transitionsmap = [{
	title: 'swipfade',
	id: ti.ui.transitionstyle.swipe_fade
}, {
	title: 'flip',
	id: ti.ui.transitionstyle.flip
}, {
	title: 'cube',
	id: ti.ui.transitionstyle.cube
}, {
	title: 'fold',
	id: ti.ui.transitionstyle.fold
}, {
	title: 'fade',
	id: ti.ui.transitionstyle.fade
}, {
	title: 'back fade',
	id: ti.ui.transitionstyle.back_fade
}, {
	title: 'scale',
	id: ti.ui.transitionstyle.scale
}, {
	title: 'push rotate',
	id: ti.ui.transitionstyle.push_rotate
}, {
	title: 'slide',
	id: ti.ui.transitionstyle.slide
}, {
	title: 'modern push',
	id: ti.ui.transitionstyle.modern_push
}, {
	title: 'ghost',
	id: ti.ui.transitionstyle.ghost
}, {
	title: 'zoom',
	id: ti.ui.transitionstyle.zoom
}, {
	title: 'swap',
	id: ti.ui.transitionstyle.swap
}, {
	title: 'carousel',
	id: ti.ui.transitionstyle.carousel
}, {
	title: 'cross',
	id: ti.ui.transitionstyle.cross
}, {
	title: 'glue',
	id: ti.ui.transitionstyle.glue
}];

function chosetransition(_view, _property) {
	var optiontitles = ['cancel'];
	for (var i = 0; i < transitionsmap.length; i++) {
		optiontitles.push(transitionsmap[i].title);
	};
	var opts = {
		cancel: 0,
		options: optiontitles,
		selectedindex: 0,
		destructive: 0,
		title: 'transition style'
	};

	var dialog = ti.ui.createoptiondialog(opts);
	dialog.addeventlistener('click', function(e) {
		if (e.cancel == false) {
			_view[_property] = {
				style: transitionsmap[e.index - 1].id
			};
		}
	});
	dialog.show();
}

function test2() {
	var win = createwin({
		modal: true
	});
	var view = ti.ui.createview({
		width: ti.ui.fill,
		height: 60
	});
	var view1 = ti.ui.createview({
		layout: 'vertical'
	});
	var view2 = ti.ui.createview({
		height: '65%',
		layout: 'horizontal',
		width: ti.ui.fill
	});
	var view3 = ti.ui.createlabel({
		text: 'this is my tutle test',
		top: 2,
		ellipsize: ti.ui.text_ellipsize_tail,
		font: {
			fontsize: 14
		},
		width: ti.ui.fill
	});
	var view4 = ti.ui.createlabel({
		color: 'white',
		text: 'test',
		padding: {
			left: 4,
			right: 4,
			bottom: 2
		},
		shadowcolor: '#55000000',
		shadowradius: 2,
		font: {
			fontsize: 12,
			fontweight: 'bold'
		},
		backgroundcolor: 'red',
		borderradius: 4,
		right: 4
	});
	var view5 = ti.ui.createview({
		height: ti.ui.fill,
		layout: 'horizontal',
		width: ti.ui.fill
	});
	var view6 = ti.ui.createlabel({
		font: {
			fontsize: 12
		},
		color: 'black',
		bottom: 2
	});
	var view7 = ti.ui.createlabel({
		font: {
			fontsize: 12
		},
		text: 'date',
		color: 'black',
		bottom: 2,
		textalign: 'right',
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

function listviewlayout() {
	var win = createwin();
	var template = {
		properties: {
			layout: 'horizontal',
			backgroundcolor: 'orange'
		},
		childtemplates: [{
			type: 'ti.ui.button',
			bindid: 'button',
			properties: {
				width: 40,
				height: 40,
				backgroundcolor: 'purple',
				left: 4,
				right: 4,
				font: {
					fontsize: 18,
					fontweight: 'bold'
				},
				borderradius: 10,
				color: 'white',
				selectedcolor: 'black'
			}
		}, {
			type: 'ti.ui.view',
			properties: {
				width: ti.ui.fill,
				height: ti.ui.fill,
				layout: 'vertical'
			},
			childtemplates: [{
				type: 'ti.ui.view',
				properties: {
					layout: 'horizontal',
					backgroundcolor: 'blue',
					width: ti.ui.fill,
					height: ti.ui.fill
				},
				childtemplates: [{
					type: 'ti.ui.label',
					bindid: 'tlabel',
					properties: {
						top: 2,
						backgroundcolor: 'gray',
						ellipsize: ti.ui.text_ellipsize_tail,
						font: {
							fontsize: 14
						},
						width: ti.ui.fill
					}
				}, {
					type: 'ti.ui.label',
					bindid: 'plabel',
					properties: {
						color: 'white',
						padding: {
							left: 4,
							right: 14,
							bottom: 2
						},
						shadowcolor: '#55000000',
						selectedcolor: 'green',
						shadowradius: 2,
						borderradius: 4,
						font: {
							fontsize: 12,
							fontweight: 'bold'
						},
						backgroundcolor: 'red',
						right: 4
					}
				}]
			}, {
				type: 'ti.ui.view',
				properties: {
					layout: 'horizontal',
					width: ti.ui.fill,
					backgroundcolor: 'yellow',
					height: 16,
					top: 2,
					bottom: 6

				},
				childtemplates: [{
					type: 'ti.ui.view',
					properties: {
						width: ti.ui.fill,
						backgroundcolor: '#e9e9e9',
						borderradius: 4
					},
					childtemplates: [{
						type: 'ti.ui.view',
						bindid: 'progressbar',
						properties: {
							left: 0,
							height: ti.ui.fill,
							backgroundcolor: 'green'
						}
					}, {
						type: 'ti.ui.label',
						bindid: 'sizelabel',
						properties: {
							color: 'black',
							shadowcolor: '#55ffffff',
							shadowradius: 2,
							font: {
								fontsize: 12
							}
						}
					}]
				}, {
					type: 'ti.ui.label',
					bindid: 'timelabel',
					properties: {
						font: {
							fontsize: 12
						},
						backgroundcolor: 'green',
						color: 'black',
						textalign: 'right',
						right: 4,
						width: 80
					}
				}]
			}]
		}]
	};

	var names = ['carolyn humbert',
		'david michaels',
		'rebecca thorning',
		'joe b',
		'phillip craig',
		'michelle werner',
		'philippe christophe',
		'marcus crane',
		'esteban valdez',
		'sarah mullock'
	];
	var priorities = ['downloading',
		'success',
		'failure',
		'test',
		'processing'
	];
	var listview = createlistview({
		minrowheight: 40,
		templates: {
			'template': template
		},
		defaultitemtemplate: 'template'
	});
	var items = [];
	for (var i = 0; i < 100; i++) {
		items.push({
			properties: {
				height: ti.ui.size
			},
			tlabel: {
				text: names[math.floor(math.random() * names.length)]
			},
			plabel: {
				text: priorities[math.floor(math.random() * priorities.length)]
			},
			sizelabel: {
				text: 'size'
			},
			timelabel: {
				text: (new date()).tostring()
			},
			progressbar: {
				width: math.floor(math.random() * 100) + '%'
			}
		});
	}
	listview.setsections([{
		items: items
	}]);
	win.add(listview);
	openwin(win);
}

function keyboardtest() {
	var textfield = ti.ui.createtextfield();
	var dialog = ti.ui.createalertdialog({
		title: 'test',
		buttonnames: ['cancel', 'ok'],
		persistent: true,
		cancel: 0,
		androidview: textfield
	});
	textfield.addeventlistener('change', function(e) {
		textfield.blur();
	});
	dialog.addeventlistener('open', function(e) {
		textfield.focus();
	});
	dialog.addeventlistener('click', function(e) {
		if (e.cancel)
			return;
	});
	dialog.addeventlistener('return', function(e) {});
	dialog.show();
}

function transitiontest() {
	var win = createwin();

	var holderholder = ti.ui.createview({
		// clipchildren:false,
		height: 100,
		bordercolor: 'green',
		width: 220,
		backgroundcolor: 'green'
	});
	var transitionviewholder = ti.ui.createview({
		clipchildren: false,
		height: 80,
		width: 200,
		bordercolor: 'green',
		// borderradius: 10,
		backgroundcolor: 'red'
	});
	var tr1 = ti.ui.createlabel({
		text: 'i am a text!',
		color: '#fff',
		textalign: 'center',
		backgroundcolor: 'green',
		width: 50,
		height: 40,
	});
	tr1.addeventlistener('click', function(e) {
		ti.api.info('click');
		transitionviewholder.transitionviews(tr1, tr2, {
			style: ti.ui.transitionstyle.cube,
			duration: 3000,
			reverse: true
		});
	});
	var tr2 = ti.ui.createbutton({
		title: 'i am a button!',
		color: '#000',
		height: 40,
		backgroundcolor: 'white'
	});
	tr2.addeventlistener('click', function(e) {
		transitionviewholder.transitionviews(tr2, tr1, {
			style: ti.ui.transitionstyle.swipe_dual_fade,
		});
	});
	transitionviewholder.add(tr1);
	holderholder.add(transitionviewholder);
	win.add(holderholder);
	openwin(win);
}

function opacitytest() {
	var win = createwin({
		dispatchpressed: true,
		backgroundselectedcolor: 'green'
	});

	var image1 = ti.ui.createimageview({
		backgroundcolor: 'yellow',
		image: "animation/win_1.png"
	});
	image1.addeventlistener('longpress', function() {
		image1.animate({
			opacity: 0,
			autoreverse: true,
			duration: 2000,
		});
	});

	var button = ti.ui.createbutton({
		top: 0,
		padding: {
			left: 30,
			top: 30,
			bottom: 30,
			right: 30
		},
		height: 50,
		bubbleparent: false,
		backgroundcolor: 'gray',
		touchpassthrough: false,
		dispatchpressed: true,
		backgroundselectedgradient: {
			type: 'linear',
			colors: ['#333', 'transparent'],
			startpoint: {
				x: 0,
				y: 0
			},
			endpoint: {
				x: 0,
				y: "100%"
			}
		},
		title: 'test buutton'
	});
	button.add(ti.ui.createview({
		enabled: true,
		backgroundcolor: 'purple',
		backgroundselectedcolor: 'white',
		left: 10,
		width: 15,
		height: 15
	}));
	button.add(ti.ui.createview({
		backgroundcolor: 'green',
		bottom: 10,
		width: 15,
		height: 15
	}));
	button.add(ti.ui.createview({
		backgroundcolor: 'yellow',
		top: 10,
		width: 15,
		height: 15
	}));
	button.add(ti.ui.createview({
		touchpassthrough: true,
		backgroundcolor: 'orange',
		right: 0,
		width: 35,
		height: ti.ui.fill
	}));
	var t1 = ti.ui.create2dmatrix();
	var t2 = ti.ui.create2dmatrix().scale(2.0, 2.0).translate(0, 40).rotate(90);
	button.addeventlistener('longpress', function(e) {
		button.animate({
			opacity: 0,
			autoreverse: true,
			duration: 2000,
		});
	});
	win.add(button);
	var label = ti.ui.createlabel({
		bottom: 20,
		// dispatchpressed: true,
		backgroundcolor: 'gray',
		backgroundselectedcolor: '#a46',
		padding: {
			left: 30,
			top: 30,
			bottom: 30,
			right: 30
		},
		bubbleparent: false,
		selectedcolor: 'green',
		backgroundselectedgradient: {
			type: 'linear',
			colors: ['#333', 'transparent'],
			startpoint: {
				x: 0,
				y: 0
			},
			endpoint: {
				x: 0,
				y: "100%"
			}
		},
		text: 'this is a sample\n text for a label'
	});
	label.add(ti.ui.createview({
		touchenabled: false,
		backgroundcolor: 'red',
		backgroundselectedcolor: 'white',
		left: 10,
		width: 15,
		height: 15
	}));
	label.add(ti.ui.createview({
		backgroundcolor: 'green',
		bottom: 10,
		width: 15,
		height: 15
	}));
	label.add(ti.ui.createview({
		backgroundcolor: 'yellow',
		top: 10,
		width: 15,
		height: 15
	}));
	label.add(ti.ui.createview({
		backgroundcolor: 'orange',
		right: 10,
		width: 15,
		height: 15
	}));
	var t3 = ti.ui.create2dmatrix().scale(2.0, 2.0).translate(0, -40).rotate(90);
	label.addeventlistener('longpress', function(e) {
		label.animate({
			opacity: 0,
			autoreverse: true,
			duration: 2000,
		});
	});
	win.add(label);
	var button2 = ti.ui.createbutton({
		padding: {
			left: 80
		},
		bubbleparent: false,
		backgroundcolor: 'gray',
		dispatchpressed: true,
		selectedcolor: 'red',
		backgroundselectedgradient: {
			type: 'linear',
			colors: ['#333', 'transparent'],
			startpoint: {
				x: 0,
				y: 0
			},
			endpoint: {
				x: 0,
				y: "100%"
			}
		},
		title: 'test buutton'
	});
	button2.add(ti.ui.createbutton({
		left: 0,
		backgroundcolor: 'green',
		selectedcolor: 'red',
		backgroundselectedgradient: {
			type: 'linear',
			colors: ['#333', 'transparent'],
			startpoint: {
				x: 0,
				y: 0
			},
			endpoint: {
				x: 0,
				y: "100%"
			}
		},
		title: 'osd'
	}));
	win.add(button2);
	win.add(image1);
	openwin(win);
}

function imageviewtests() {
	var win = createwin();
	var listview = createlistview();
	listview.sections = [{
		items: [{
			properties: {
				title: 'animationtest'
			},
			callback: imageviewanimationtest
		}, {
			properties: {
				title: 'transitiontest'
			},
			callback: imageviewtransitiontest
		}]
	}];
	win.add(listview);
	openwin(win);
}

function imageviewtransitiontest() {
	var win = createwin();

	var image1 = ti.ui.createimageview({
		backgroundcolor: 'yellow',
		image: "animation/win_1.png",
		width: 100,
		transition: {
			style: ti.ui.transitionstyle.flip,
			// substyle:ti.ui.transitionstyle.top_to_bottom
		}
	});
	win.add(image1);
	image1.addeventlistener('click', function() {
		image1.image = "animation/win_" + math.floor(math.random() * 16 + 1) + ".png";
	});
	var button = ti.ui.createbutton({
		bottom: 0,
		bubbleparent: false,
		title: 'transition'
	});
	button.addeventlistener('click', function() {
		chosetransition(image1, 'transition');
	});
	win.add(button);
	openwin(win);
}

function imageviewanimationtest() {
	var win = createwin();

	var image1 = ti.ui.createimageview({
		backgroundcolor: 'yellow',
		width: 100,
		transition: {
			style: ti.ui.transitionstyle.fade,
		},
		image: 'http://zapp.trakt.us/images/posters_movies/192263-138.jpg',
		animatedimages: ["animation/win_1.png", "animation/win_2.png", "animation/win_3.png", "animation/win_4.png",
						"animation/win_5.png", "animation/win_6.png", "animation/win_7.png", "animation/win_8.png",
						"animation/win_9.png", "animation/win_10.png", "animation/win_11.png", "animation/win_12.png",
						"animation/win_13.png", "animation/win_14.png", "animation/win_15.png", "animation/win_16.png"],
		duration: 100,
		viewmask: '/images/body-mask.png'
	});
	win.add(image1);
	var btnholder = ti.ui.createview({
		left: 0,
		layout: 'vertical',
		height: ti.ui.size,
		width: ti.ui.size,
		backgroundcolor: 'green'
	});
	btnholder.add([
		{
			type: 'ti.ui.button',
			left: 0,
			bid: 0,
			title: 'start'
		},
		{
			type: 'ti.ui.button',
			right: 0,
			bid: 1,
			title: 'pause'
		},
		{
			type: 'ti.ui.button',
			left: 0,
			bid: 2,
			title: 'resume'
		},
		{
			type: 'ti.ui.button',
			right: 0,
			bid: 3,
			title: 'playpause'
		},
		{
			type: 'ti.ui.button',
			left: 0,
			bid: 4,
			title: 'stop'
		},
		{
			type: 'ti.ui.button',
			right: 0,
			bid: 5,
			title: 'reverse'
		},
		{
			type: 'ti.ui.button',
			left: 0,
			bid: 6,
			title: 'autoreverse'
		},
		{
			type: 'ti.ui.button',
			right: 0,
			bid: 7,
			title: 'transition'
		}
	]);
	btnholder.addeventlistener('singletap', function(e) {
		info(stringify(e));
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
				image1.pauseorresume();
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
				chosetransition(image1, 'transition');
				break;

		}
	});
	win.add(btnholder);
	openwin(win);
}

function antialiastest() {
	var win = createwin();
	var view = ti.ui.createview({
		backgroundcolor: 'blue',
		borderwidth: 4,
		bordercolor: 'green',
		borderradius: 50,
		width: 300,
		height: 300
	});
	view.add({
		backgroundcolor: 'red',
		left: 0,
		top: 0,
		width: 100,
		height: 100
	});
	view.addeventlistener('singletap', function() {
		view.animate({
			top: 0,
			duration: 200,
			autoreverse: true
		});
	});
	view.addeventlistener('longpress', function() {
		view.animate({
			transform: ti.ui.create2dmatrix().scale(0.3, 0.3),
			duration: 200,
			autoreverse: true
		});
	});

	win.add(view);
	openwin(win);
}

var firstwindow = createwin({});
var listview = createlistview({
	// minrowheight:100,
	// maxrowheight:140
});
var color = cellcolor(0);
listview.sections = [{
	items: [{
		properties: {
			title: 'imageview tests'
		},
		callback: imageviewtests
	}, {
		properties: {
			title: 'antialiastest'
		},
		callback: antialiastest
	}, {
		properties: {
			title: 'opacity'
		},
		callback: opacitytest
	}, {
		properties: {
			title: 'layout'
		},
		callback: layoutexs
	}, {
		properties: {
			title: 'listviewlayout'
		},
		callback: listviewlayout
	}, {
		properties: {
			title: 'transitiontest'
		},
		callback: transitiontest
	}, {
		properties: {
			title: 'shapes'
		},
		callback: shapeexs
	}, {
		properties: {
			title: 'buttonsandlabels'
		},
		callback: buttonandlabelex
	}, {
		properties: {
			title: 'mask'
		},
		callback: maskex
	}, {
		properties: {
			title: 'imageview'
		},
		callback: imageviewex
	}, {
		properties: {
			title: 'animationset'
		},
		callback: transform2ex
	}, {
		properties: {
			title: 'html label'
		},
		callback: htmllabelex
	}, {
		properties: {
			title: 'svg'
		},
		callback: svgexs
	}, {
		properties: {
			title: 'pulltorefresh'
		},
		callback: pulltorefresh
		}, {
		properties: {
			title: 'listview'
		},
		callback: listviewex
	}, {
		properties: {
			title: 'listview2'
		},
		callback: listview2ex
	}]
}];
firstwindow.add(listview);
var mainwin = ti.ui.createnavigationwindow({
	backgroundcolor: backcolor,
	exitonclose: true,
	window: firstwindow,
	transition: {
		style: ti.ui.transitionstyle.cube
	}
});
mainwin.addeventlistener('openwindow', function(e) {
	ti.api.info(e);
});
mainwin.addeventlistener('closewindow', function(e) {
	ti.api.info(e);
});
mainwin.open();


function test2() {
	var win = createwin();
	var holder = ti.ui.createview({
		width: 200,
		height: '80%',
		backgroundcolor: 'red',
		layout: 'vertical'
	});
	var view1 = ti.ui.createview({
		width: ti.ui.fill,
		height: ti.ui.size,
		backgroundcolor: 'yellow'
	});
	view1.add({
		width: ti.ui.fill,
		height: ti.ui.fill,
		backgroundcolor: 'blue'
	});
	holder.add([
		view1,
		{
			width: ti.ui.fill,
			height: 60,
			backgroundcolor: 'orange'
		}
	]);
	win.add(holder);
	openwin(win);
}
