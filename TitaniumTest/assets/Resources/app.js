var win = Titanium.UI.createWindow({
	orientationModes:[Ti.UI.UPSIDE_PORTRAIT, Ti.UI.PORTRAIT, Ti.UI.LANDSCAPE_RIGHT, Ti.UI.LANDSCAPE_LEFT],
	backgroundColor:'#385292'
});

var view = Ti.UI.createView({
	left:0,
	right:0,
	height:110,
	backgroundColor:'blue'
})
view.addEventListener('click',function(){
	var win2 = Ti.UI.createWindow({
		orientationModes:[Ti.UI.UPSIDE_PORTRAIT, Ti.UI.PORTRAIT, Ti.UI.LANDSCAPE_RIGHT, Ti.UI.LANDSCAPE_LEFT],
		modal:true,
		backgroundColor:'#bbcccccc'
	})
	win2.addEventListener('open',function(){
		win2.title = 'test';
	})
	win2.open({animated:false});
});

win.add(view);
win.open();
