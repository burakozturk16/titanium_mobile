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
	// orientationModes : [Ti.UI.UPSIDE_PORTRAIT]
});

var START_HOUR = 18*60;
var view = Charts.createLineChart({
	top:0,
	left:0,
	width:Ti.UI.FILL,
	height:"100%",
	backgroundColor : 'transparent',
	plotArea : {
		  borderColor : 'black',
		  borderWidth : 0.0,

		 padding : {
			 left : 40.0,
			 bottom : 40.0
		 },
		 backgroundColor : 'transparent'
	 },
	 legend:{
		 visible:false
	 },

	gridArea : {
		padding:{
			 // right : 20.0,
			 top : 20.0
		 }
	},
	xAxis : {
		origin:0,
		lineColor : 'white',
		lineWidth : 1.0,
		title : {
			text : 'Time (minutes)',
			color : 'white',
			font : {
				fontSize : 14
			}
		},
		majorTicks : {
			width : 1.0,
			interval:60,
			gridLines : {
				width : 2.0,
				color : 'white',
				opacity : 0.5
			},
			labels : {
				offset : 0.0,
				angle : 0.0,
				color : 'white',
				font : {
					fontSize : 20
				},
				textAlign : Charts.ALIGNMENT_CENTER,
				formatCallback:function(_number){
					var date = new Date((_number + START_HOUR)*60*1000);
					return date.getHours() + ":" + date.getMinutes();
				}
			}
		},
		minorTicks:{
			count:6,
			gridLines : {

				width : 1.0,
				color : 'white',
				opacity : 0.3
			}
		}
	},
	yAxis : {
		 origin:0,
		 // align:Charts.ALIGNMENT_RIGHT,
		 lineColor : 'red',
		 lineWidth : 2.0,
		 title : {
			 text : 'Glycemia',
			 color : 'red',
			 font : {
				 fontSize : 20
			 }
		 },
		majorTicks : {
			width : 1.0,
			// interval : 100,
			labels : {
				offset : 0.0,
				angle : 45.0,
				color : 'red',
				font : {
					fontSize : 12
				},
				textAlign : Charts.ALIGNMENT_MIDDLE,
				numberFormat : '#'
			}
		}
	},
	
	barStyle:Charts.BAR_STYLE_SIDE_BY_SIDE,
	barWidthStyle:Charts.BAR_WIDTH_FIXED,
	barWidth:20,
	barGap:50,
// 
	plotSpace : {
		// scaleToFit : true,
		yRange : {
			min : 0,
			 max : 450
		},
		 xRange : {
			 min : 0,
			 max : 240
		 }
	},
	userInteraction : true,
	clampInteraction : false
});


var plotBar = Charts.createPlotBar({
	name : 'step plot',
	lineColor : 'white',
	lineWidth : 1.0,
	fillOpacity:0.2,
	lineOpacity:0.6,
	fillColor:'green',
	labels:{
		color:'green',
		opacity:0.6,
		angle:-30,
		offset:{y:-20},
		font:{
			fontSize:16
		}
	},
	data:[[490, 764, 340, 975, 1085],[45, 45, 405, 50, 135]]
});

view.add(plotBar);

var plotBar2 = Charts.createPlotBar({
	name : 'step plot',
	lineColor : 'white',
	barRadius:10,
	lineWidth : 1.0,
	fillOpacity:0.6,
	lineOpacity:0.6,
	lineDash:{
		pattern:[10,10],
		phase:0.5
	},
	shadow:{
			offset:{x:1, y:1},
			radius:4,
			color:'black'
	},
	fillColor:'blue',
	labels:{
		color:'blue',
		offset:{y:0},
		font:{
			fontSize:16
		},
		formatCallback:function(_y, _x, _index){
			return _y/10;
		}
	},
	data:[[490, 764, 860, 975, 1085],[30, 32, 10, 30, 110]]
});

view.add(plotBar2);

var length = 24*60;
var step = Math.floor(length / 5) + 1;
var data = [[],[]];
var data2 = [[],[]];
var lastvalue = 70;
var lastvalue2 = 70;
for (var i=0; i < step; i++) {
  data[0].push(i*5);
  data2[0].push(i*5);
  var newvalue = lastvalue + Math.floor(Math.random()*10 - 4.5);
  var newvalue2 = lastvalue2 + Math.floor(Math.random()*10 - 4.5);
  data[1].push(newvalue);
  lastvalue = newvalue;
  data2[1].push(newvalue2);
  lastvalue2 = newvalue2;
};

var linePlot = Charts.createPlotLine({
	name : 'line plot',
	lineColor : 'pink',
	lineWidth : 2.0,
	shadow:{
		offset:{x:0, y:1},
		radius:4,
		color:'black'
	},
	lineDash:{
		pattern:[10,10],
		phase:0.5
	},
	data:data
});

view.add(linePlot);

var linePlot2 = Charts.createPlotLine({
	name : 'line plot',
	lineColor : 'purple',
	lineCap:Charts.CAP_ROUND,
	lineJoin:Charts.JOIN_ROUND,
	lineGradient : {
		type : 'linear',
		colors : ['red', 'orange', 'yellow', 'green'],
		startPoint : {
			x : 0,
			y : 0
		},
		endPoint : {
			x : 0,
			y : "100%"
		},
		backFillStart : false
	},
	lineWidth : 10.0,
	data:data2
});

view.add(linePlot2);

view.addMarker({
	type:Charts.VERTICAL,
	title:'Hyper',
	value:200,
	lineColor : 'orange',
	lineWidth : 2.0,
	lineDash:{
		pattern:[10,10],
		phase:0.5
	},
	shadow:{
			offset:{x:1, y:1},
			radius:4,
			color:'red'
	},
	label:{
		color:'orange',
		offset:{x:36},
		font:{
			fontSize:16
		},
		shadow:{
			offset:{x:0, y:1},
			radius:4,
			color:'black'
		}
	}
});

view.addMarker({
	type:Charts.HORIZONTAL,
	title:'Hypo',
	value:60,
	lineColor : 'orange',
	lineWidth : 2.0,
	lineDash:{
		pattern:[10,10],
		phase:0.5
	},
	shadow:{
			offset:{x:1, y:1},
			radius:4,
			color:'blue'
	},
	label:{
		color:'orange',
		offset:{y:-16},
		font:{
			fontSize:16
		}
	}
});

var plotStep = Charts.createPlotStep({
	name : 'step plot',
	lineColor : 'blue',
	lineWidth : 2.0,
	labels:{
		color:'blue',
		offset:{
			x:16
		},
		font:{
			fontSize:16
		},
		formatCallback:function(_y, _x, _index){
			return _y/10;
		}
	},
	data:[[0, 240, 540, 1050, 1080, 1440],[6, 13.5, 5, 10, 9, 0.65]]
});

view.add(plotStep);

win.add(view);

///// SENSOR /////

// var HISTORY_SIZE = 100;
// var view = Charts.createLineChart({
	// fillColor : 'transparent',
	// backgroundColor : 'orange',
	// // Configure the plot area -- the area where the chart is drawn
	// // Configure the title for the chart
	// title : {
		// text : 'Line Chart',
		// color : '#900',
		// font : {
			// fontFamily : 'Times New Roman',
			// fontSize : 24,
			// fontWeight : 'bold',
			// fontStyle : 'italic'
		// },
		// location : Charts.LOCATION_TOP,
		// offset : {
			// x : 0.0,
			// y : 0.0
		// }
	// },
// 
	// // Configure the external padding -- the area between the view edge and the
	// // plot
	// // area frame
	// padding : {
		// top : 0,
		// left : 0,
		// right : 0,
		// bottom : 0
	// },
// 
	// // Configure the plot area -- the area where the chart is drawn
	// plotArea : {
		// // borderRadius : 5.0,
		// // borderOpacity : 0.7,
		// // borderColor : 'black',
		// // borderWidth : 2.0,
		// // Configure the inner padding -- the area between the plot area frame
		// // and the
		// // actual plot area
		// padding : {
			// top : 0.0,
			// left : 0.0,
			// right : 0.0,
			// bottom : 0.0
		// },
// 
		// backgroundColor : 'black',
		// // backgroundGradient : {
			// // type : 'linear',
			// // colors : ['#00F', '#004'],
			// // startPoint : {
				// // x : 0,
				// // y : 0
			// // },
			// // endPoint : {
				// // x : "100%",
				// // y : "100%"
			// // },
			// // backFillStart : false
		// // }
	// },
// 
	// gridArea : {
	// },
// 
	// // Configure the xAxis
	// xAxis : {
		// // origin defines where it intercepts the orthogonal axis (the y-axis)
		// origin : 0,
		// lineColor : 'yellow',
		// lineWidth : 1.0,
		// title : {
			// text : 'X Axis',
			// offset : 18.0,
			// color : '#0f0',
			// font : {
				// fontFamily : 'Helvetica',
				// fontSize : 14
			// }
		// },
// 
		// interval_px : 60,
		// majorTicks : {
			// color : 'red',
			// width : 1.0,
			// length : 5.0,
			// gridLines : {
// 
				// width : 4.0,
				// color : 'blue',
				// opacity : 0.5,
				// range : {
					// location : 0.0,
					// length : 100.0
				// }
			// },
			// labels : {
				// offset : 0.0,
				// angle : 0.0,
				// color : 'red',
				// font : {
					// fontFamily : 'Helvetica',
					// fontSize : 20
				// },
				// textAlign : Charts.ALIGNMENT_CENTER,
				// numberFormat : '##',
			// }
		// },
		// minorTicks : {
			// color : 'purple',
			// width : 1.0,
			// length : 3.0,
			// gridLines : {
				// width : 1.0,
				// color : 'white',
				// opacity : 0.1
			// }
		// },
		// visibleRange : {
			// location : 0.0,
			// length : 100.0
		// }
	// },
	// // Configure the yAxis
	// yAxis : {
		// // origin defines where it intercepts the orthogonal axis (the x-axis)
		// origin : 0,
		// lineColor : 'yellow',
		// lineWidth : 2.0,
		// title : {
			// text : 'Y Axis',
			// offset : 24.0,
			// color : '#0f0',
			// font : {
				// fontFamily : 'Helvetica',
				// fontSize : 14
			// }
		// },
		// majorTicks : {
			// color : 'white',
			// width : 1.0,
			// length : 5.0,
			// gridLines : {
				// width : 1.0,
				// color : 'white',
				// opacity : 0.4,
				// range : {
					// location : 0.0,
					// length : 100.0
				// }
			// },
			// labels : {
				// offset : 0.0,
				// angle : 45.0,
				// color : 'white',
				// font : {
					// fontFamily : 'Helvetica',
					// fontSize : 12
				// },
				// textAlign : Charts.ALIGNMENT_MIDDLE,
				// numberFormat : '#'
			// }
		// },
		// visibleRange : {
			// location : 0.0,
			// length : 100.0
		// }
	// },
// 
	// plotSpace : {
		// scaleToFit : false,
		// yRange : {
			// min : -80,
			// max : 80
		// },
		// xRange : {
			// min : 0,
			// max : HISTORY_SIZE
		// }
	// },
	// // Enable user interaction -- defaults to true
	// userInteraction : false,
	// zoomEnabled : false,
	// clampInteraction : true
// });
// 
// var linePlotx = Charts.createPlotLine({
	// name : 'line plot',
	// lineColor : 'green',
	// lineWidth : 2.0,
// 
// });
// var linePloty = Charts.createPlotLine({
	// name : 'line plot2',
	// lineColor : 'blue',
	// lineWidth : 2.0
// });
// var linePlotz = Charts.createPlotLine({
	// name : 'line plot3',
	// lineColor : 'yellow',
	// lineWidth : 2.0
// });
// view.add(linePlotx);
// view.add(linePloty);
// view.add(linePlotz);
// var accelerometerCallback = function(e) {
	// if (linePlotx.size() > HISTORY_SIZE) {
		// view.removeDataFirst();
	// }
	// view.addDataLast([e.x, e.y, e.z]);
	// view.refresh();
// };
// Ti.Accelerometer.addEventListener('update', accelerometerCallback);
// win.addEventListener('close', function() {
	// Ti.Accelerometer.removeEventListener('update', accelerometerCallback);
// });
// win.add(view);

///// !!!SENSOR!!! /////
///// PIECHART /////

// var view = Ti.UI.createView({
// backgroundColor:'red',
// borderColor:'black',
// borderWidth:2,
// transform:Ti.UI.create2DMatrix(),
// borderRadius:4,
// width:200,
// height:300
// });
// var view2 = Ti.UI.createView({
// borderColor:'black',
// borderWidth:2,
// transform:Ti.UI.create2DMatrix().rotate(90),
// width:100,
// height:100,
// backgroundColor:'blue'
// })
// view.add(view2)
// win.addEventListener('click', function(){
// if (set.animating == true) {
// set.cancel();
// }
// else {
// anim1.transform = view.transform.rotate(45);
// anim2.transform = view2.transform.rotate(-45);
// set.startSequentially();
// setTimeout(function(){
// Ti.API.info('animating: ' + set.animating);
// Ti.API.info('animating2: ' + anim1.animating);
// }, 500)
// }

// });

// var anim1 = Ti.UI.createAnimation({
// transform:view.transform.rotate(45),
// duration:2000
// });

// var anim2 = Ti.UI.createAnimation({
// transform:view2.transform.rotate(-45),
// duration:2000
// });

// var set = Ti.UI.createAnimationSet({duration:1000});
// set.addEventListener('complete', function(){
// 	alert('test2');
// 	Ti.API.info('animating: ' + set.animating);
// 	Ti.API.info('animating2: ' + anim1.animating);
// });
// set.add(anim1, view);
// set.add(anim2, view2);

// win.add(view);


// var view = Charts.createPieChart({
// 	plotArea : {
// 		  borderColor : 'black',
// 		  borderWidth : 2.0,

// 		 padding : {
// 			 left : 20.0,
// 			 bottom : 20.0
// 		 },
// 		 backgroundColor : 'transparent'
// 	 },
// 	 padding : {
// 			 left : 20.0,
// 			 bottom : 20.0
// 		 },
// 	title : {
// 		text : 'Pie Chart',
// 		color : '#900',
// 		font : {
// 			fontSize : 24,
// 			fontWeight : 'bold',
// 			fontStyle : 'italic'
// 		}
// 	},
// 	startAngle:0,
// 	donutSize : 30
// });
// view.add(Charts.createPieSegment({
// 	name : 'plot',
// 	value : 12,
// 	fillColor:'red',
// 	fillGradient : {
// 		type : 'radial',
// 		colors : [{
// 			color : 'red',
// 			offset : 0.8
// 		}, {
// 			color : '#000000',
// 			offset : 1.0
// 		}]
// 	},
// 	lineColor : 'blue',
// 	lineWidth : 2.0
// 	// lineColor : 'black'
// 	// lineWidth : 5.0
// }));

// var segment = Charts.createPieSegment({

// 	name : 'plot2',
// 	value : 30,
// 	fillColor : 'blue',
// 	fillGradient : {
// 		type : 'radial',
// 		colors : [{
// 			color : 'blue',
// 			offset : 0.8
// 		}, {
// 			color : '#000000',
// 			offset : 1.0
// 		}]
// 	},
// 	lineColor : 'black',
// 	lineWidth : 2.0
// });
// view.add(segment);
// win.add(view);

// var anim1 = Ti.UI.createAnimation({
// 	value : (segment.value == 30) ? 10 : 30,
// 	duration : 100
// });

// //anim1.addEventListener('complete', function(){alert('test')});

// win.addEventListener('click', function() {
// 	Ti.API.info('value ' + segment.value);
// 	Ti.API.info('title ' + segment.name);
// 	// segment.animate({
// 	// 	value : (segment.value == 30) ? 10 : 30,
// 	// 	duration : 1000
// 	// }, function() {
// 	// 	Ti.API.info('test');
// 	// 	segment.name = segment.value;
// 	// });
// });

win.open();
