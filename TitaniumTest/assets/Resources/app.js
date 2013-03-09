
// var win = Ti.UI.createWindow({
// 	backgroundColor:'pink'
// });
// var button = Ti.UI.createButton({
// 	title:'open Menu'
// });
// win.add(button);
// button.addEventListener('click', function()
// {
// 	slideMenu.open();
// });
var isAndroid = Ti.Platform.osname == "android";

var leftTableView = Ti.UI.createTableView({
	data:[{title:'Basic'},{title:'Basic2'}, {title:'Basic3'} ],
	color:'white',
	backgroundColor: 'blue',
	rowHeight: 44,
	allowsSelection: true
});

leftTableView.add(Ti.UI.createView({
	backgroundColor:'red',
	width:20,
	height:20,
	right:5
}));


var actionBar;
var openedWindows = [];
var navController;

function closeTopWindow() {
	if (openedWindows.length === 0) return;
	if (isAndroid)
	{
		var lastWin = openedWindows.pop();
		lastWin.close();
		lastWin = null;
		if (openedWindows.length === 0) {
			actionBar.title = '';
			actionBar.displayHomeAsUp = false;
			actionBar.onHomeIconItemSelected = null;
		}
		else
		{
			lastWin = openedWindows[openedWindows.length - 1];
			actionBar.title = lastWin.title;
			actionBar.displayHomeAsUp = true;
			actionBar.onHomeIconItemSelected = function() {
				closeTopWindow();
			};
		}
	}
	else {
		var lastWin = openedWindows.pop();
		navController.close(lastWin);
		lastWin = null;
	}
}

function openNavWindow(_win)
{
	openedWindows.push(_win);
	if (isAndroid)
	{
		actionBar.title = _win.title;
		actionBar.onHomeIconItemSelected = function() {
			closeTopWindow();
		};
		actionBar.displayHomeAsUp = true;
		_win.open();
	}
	else
	{
		navController.open(_win);
	}
}

leftTableView.addEventListener('click', function(){
	var win2 = Ti.UI.createWindow({
		backgroundColor:'blue',
		title:'test'
	});
	var button = Ti.UI.createButton({
		title:'close'
	});
	win2.add(button);
	button.addEventListener('click', closeTopWindow);
	openNavWindow(win2);
	slideMenu.closeLeftView();
})


var rightView = Ti.UI.createView({
	backgroundColor:'orange'
})

var button2 = Ti.UI.createButton({
	title:'test2',
	top:30
})
button2.addEventListener('click', function()
{
	slideMenu.centerView = winCenter2;
});
rightView.add(button2);

var winCenter1 = Ti.UI.createWindow({
	backgroundColor:'yellow',
	barColor:"#000"
});

//nav buttons
var leftBtn = Ti.UI.createButton({title:"left"});
leftBtn.addEventListener("click", function(){
	slideMenu.toggleLeftView();
});

var rightBtn = Ti.UI.createButton({title:"right"});
rightBtn.addEventListener("click", function(){
	slideMenu.toggleRightView();
});

var button = Ti.UI.createButton({
	title:"test",
	bottom:20
});
winCenter1.add(button);

if (!isAndroid) {
	winCenter1.leftNavButton = leftBtn;
	winCenter1.rightNavButton = rightBtn;
	navController = Ti.UI.iPhone.createNavigationGroup({
		window : winCenter1
	});
}

var winCenter2 = Ti.UI.createView({
	backgroundColor:'pink',
	barColor:"#000"
});

var slideMenu = Ti.UI.createSlideMenu({
	// navBarHidden:false,
	backgroundColor: '#333',
	orientationModes:[Ti.UI.UPSIDE_PORTRAIT, Ti.UI.PORTRAIT, Ti.UI.LANDSCAPE_RIGHT, Ti.UI.LANDSCAPE_LEFT],
	centerView:(isAndroid?winCenter2:navController),
	leftView:leftTableView,
	rightView:rightView,
	leftViewWidth:200,
	shadowWidth:10,
	fading:0.75,
	menuScrollScale:0.5,
	panningMode:Ti.UI.MENU_PANNING_CENTER_VIEW
});

var leftSideWidth= 0;

slideMenu.addEventListener('scrollstart', function(e){
	leftSideWidth = slideMenu.getRealLeftViewWidth();
	Ti.API.info("leftSideWidth:" + leftSideWidth);
});
slideMenu.addEventListener('scroll', function(e){
	Ti.API.info(e.type + ":" + e.offset);
	var offset = e.offset;
	if (!isAndroid && offset > 0) //left
	{
		var width = leftSideWidth;
		var ratio = offset/width;
		Ti.API.info("width:" + width);
		Ti.API.info("ratio:" + ratio);
		var transform = Ti.UI.create2DMatrix();
			transform = transform.translate((ratio-1)*width, 0);
		if (ratio > 1)
			transform = transform.scale(ratio, 1);
		leftTableView.transform = transform;
		leftTableView.opacity = ratio;
	}
});

slideMenu.addEventListener('closemenu', function(e){
	Ti.API.info(JSON.stringify(e));
	if (!isAndroid && e.animated && e.side === Ti.UI.LEFT_VIEW)
	{
		var width = slideMenu.getRealLeftViewWidth()
		// leftTableView.transform = Ti.UI.create2DMatrix();
		leftTableView.animate({
			duration:(e.duration*1000 - 150), //in sec
			transform:Ti.UI.create2DMatrix().translate(-width/2, 0),
			opacity:0
		});
	}
});

slideMenu.addEventListener('openmenu', function(e){
	Ti.API.info(JSON.stringify(e));
	if (!isAndroid && e.animated && e.side === Ti.UI.LEFT_VIEW)
	{
	
		// leftTableView.transform = Ti.UI.create2DMatrix().translate(-slideMenu.leftViewWidth/2, 0);
		// leftTableView.opacity = 0;
		leftTableView.animate({
			duration:(e.duration*1000 - 150), //in sec
			transform:Ti.UI.create2DMatrix(),
			opacity:1
		});
	}
});

if (isAndroid)
{
	// slideMenu.addEventListener("open", function() {
	// 	var activity = slideMenu.activity;
	// 	actionBar = activity.actionBar;
	// });
}
slideMenu.open();
