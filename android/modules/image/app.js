// This is a test harness for your Charts
// You should do something interesting in this harness
// to test out the Charts and to provide instructions
// to users on how to use it by example.

var Shape = Ti.Shape;

var Charts = Ti.Charts;
// open a single window
var win = Ti.UI.createWindow({
	backgroundColor : 'black',
	exitOnClose : true,
	fullscreen : true,
	orientationModes : [Ti.UI.UPSIDE_PORTRAIT, Ti.UI.PORTRAIT, Ti.UI.LANDSCAPE_RIGHT, Ti.UI.LANDSCAPE_LEFT]
});

var view = Shape.createView({
	width:300,
	height:100,
	backgroundColor:'white'
});
var lineWidth = 40;
var lineShadow = 4;
var totalSweepAngle = 90;
var startAngle = -45;
var arc  = Shape.createArc({
	// fillColor:'black',
	anchor:Shape.TOP_MIDDLE,
	point:{x:'50%',y:25},
	widthRadius:'50%',
	startAngle:startAngle,
	sweepAngle:totalSweepAngle,
	lineShadow:{
			offset:{x:1, y:1},
			radius:lineShadow,
			color:'black'
	},
	lineEmboss:{

	},
	lineColor:'gray',
	lineCap:Shape.CAP_ROUND,
	lineWidth:lineWidth
});

var arc2  = Shape.createArc({
	// fillColor:'black',
	point:{x:'50%',y:25},
	anchor:Shape.TOP_MIDDLE,
	widthRadius:'50%',
	startAngle:startAngle,

	lineColor:'blue',
	lineCap:Shape.CAP_ROUND,
	lineWidth:lineWidth
});

var radius = lineWidth/2;
var shape2 = Shape.createCircle({
	fillColor:'#bbb',
	lineColor:'#777',
	lineWidth:1,
	anchor:Shape.RIGHT_TOP,
	fillShadow:{
			radius:2,
			color:'black'
	},
	radius:radius
});
var shape3 = Shape.createCircle({
	fillColor:'blue',
	radius:10
});
shape2.add(shape3);

view.add(arc);
view.add(arc2);
arc2.add(shape2);
win.add(view);

var percentage = 0;
function setPercent(_percent) {
	if (_percent < 0 || _percent > 1) return;
	percentage = _percent;
	var rect = arc.rect;
	var myangle = percentage*totalSweepAngle;
	var y  = 0;
	if (percentage > 0.5 && rect != null){
		y = (rect.height - 2*radius)*(1 - (Math.cos(Math.PI/180*(myangle + startAngle)*2)));
	}
	arc2.sweepAngle = myangle;
	shape2.point = {x:0, y:y};
	arc2.update();
	view.redraw();
}
setPercent(0.8);

view.addEventListener('touchstart', function(e){
	setPercent(e.x / view.rect.width);
});
view.addEventListener('touchmove', function(e){
	Ti.API.info("touchstart " + e.x);
	setPercent(e.x / view.rect.width);
});

view.addEventListener('postlayout', function(e){
	setPercent(percentage);
});



win.open();
