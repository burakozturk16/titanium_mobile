
//var win = Ti.UI.createWindow({
//	backgroundColor:'pink'
//});
//var button = Ti.UI.createButton({
//	title:'open Menu'
//});
//win.add(button);
//button.addEventListener('click', function()
//{
//	slideMenu.open();
//});


var leftTableView = Ti.UI.createTableView({
	data:[{title:'Basic'},{title:'Basic2'}, {title:'Basic3'} ],
	backgroundColor: 'green',
	rowHeight: 44,
	allowsSelection: true,
	top:0,
	left:0,
	right:0,
	bottom:0
});

leftTableView.addEventListener('click', function(){
	var win2 = Ti.UI.createWindow({
		backgroundColor:'blue'
	});
	var button = Ti.UI.createButton({
		title:'close'
	});
	win2.add(button);
	button.addEventListener('click', function()
	{
		win2.close();
	});
	win2.open();
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

var winCenter1 = Ti.UI.createView({
	backgroundColor:'yellow',
	barColor:"#000"
});

var button = Ti.UI.createButton({
	title:'test',
	top:30
})
winCenter1.add(button);

var winCenter2 = Ti.UI.createView({
	backgroundColor:'pink',
	barColor:"#000"
});

var slideMenu = Ti.UI.createSlideMenu({
//	navBarHidden:false,
//	backgroundColor:'blue',
	orientationModes:[Ti.UI.UPSIDE_PORTRAIT, Ti.UI.PORTRAIT, Ti.UI.LANDSCAPE_RIGHT, Ti.UI.LANDSCAPE_LEFT],
	centerView:winCenter1,
	leftView:leftTableView,
	rightView:rightView,
	leftViewWidth:200,
	shadowWidth:10,
	fading:0.5,
	menuScrollScale:0.25,
	panningMode:Ti.UI.MENU_PANNING_FULLSCREEN,
	menuPanningMode:Ti.UI.MENU_PANNING_BORDERS
});
slideMenu.addEventListener('scroll', function(e){
	Ti.API.info(e.type + ":" + e.offset);
});

slideMenu.addEventListener('closemenu', function(e){
	Ti.API.info(JSON.stringify(e));

});

slideMenu.addEventListener('openmenu', function(e){
	Ti.API.info(JSON.stringify(e));

});
slideMenu.open();
