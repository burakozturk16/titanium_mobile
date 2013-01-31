var win = Ti.UI.createWindow({backgroundColor:'gray'});
var button1 = Titanium.UI.createButton({
	  left:10,
	  top:10,
	  height:50,
	  width:50,
	  enabled:true,
	  focusable:true,
	  title:'B1',
	  backgroundColor: 'white',
	  backgroundSelectedColor: 'yellow',
	  backgroundImage:'../images/slightlylargerimage.png'
	});

	var button2 = Titanium.UI.createButton({
	  left:70,
	  top:10,
	  height:50,
	  width:50,
	  focusable:true,
	  enabled:true,
	  title:'B2',
	  backgroundColor: 'white',
	  backgroundDisabledColor:'orange',
	  backgroundDisabledImage:'../images/slightlylargerimage.png'
	});

	var button3 = Titanium.UI.createButton({
	  left:130,
	  top:10,
	  height:50,
	  width:50,
	  enabled:true,
	  focusable:true,
	  title:'B3',
	  backgroundColor: 'red',
	  backgroundSelectedColor: 'blue',
	  backgroundFocusedImage:'../images/slightlylargerimage.png'
	});

	var button4 = Titanium.UI.createButton({
	  left:190,
	  top:10,
	  height:50,
	  width:50,
	  focusable:true,
	  enabled:true,
	  title:'B4',
	  backgroundColor: 'white',
	  backgroundSelectedImage:'../images/slightlylargerimage.png'
	});

	var button5 = Titanium.UI.createButton({
	  left:10,
	  top:200,
	  height:60,
	  width:100,
	  focusable:true,
	  enabled:true,
	  title:'click me'
	});

	var state=1;
	button5.addEventListener('click',function()
	{
		switch(state)
		{
			case 0:
				button2.focusable=true;
				button2.enabled=true;
				state=1;
				break;
			case 1:
				button2.focusable=false;
				button2.enabled=false;
				state=0;
				break;
			
		}
	})


	win.add(button1);
	win.add(button2);
	win.add(button3);
	win.add(button4);
	win.add(button5);
	
	var view1 = Titanium.UI.createView({
		  height:200,
		  width:200,
		  borderRadius:30,
		  borderWidth:10,
		  backgroundColor: 'orange',
		  backgroundSelectedColor:'yellow'
		})
	
		var view2 = Titanium.UI.createView({
			left:-50,
			top:-50,
		  height:100,
		  width:100,
		  backgroundColor: 'blue'
		})
	view1.add(view2);
	win.add(view1);
	
	win.add(Titanium.UI.createView({
		  left:10,
		  bottom:10,
		  height:50,
		  width:50,
		  borderColor:'white',
		  backgroundColor: 'silver',
		  backgroundSelectedColor:'white'
		}));
	
	win.add(Titanium.UI.createView({
		  left:70,
		  bottom:10,
		  height:50,
		  width:50,
		  borderRadius:8,
		  backgroundColor: 'silver',
		  backgroundSelectedColor:'yellow'
		}));
	
	win.add(Titanium.UI.createView({
		  left:130,
		  bottom:10,
		  height:50,
		  width:50,
		  borderColor:'blue',
		  borderRadius:20,
		  borderWidth:3,
		  backgroundColor: 'silver',
		  backgroundSelectedColor:'yellow'
		}));
	
	win.add(Titanium.UI.createView({
		  left:190,
		  bottom:10,
		  height:50,
		  width:50,
		  borderColor:'blue',
		  borderRadius:20,
		  backgroundColor: 'silver',
		  backgroundGradient: {
		        type: 'linear',
		        startPoint: { x: '0%', y: '50%' },
		        endPoint: { x: '100%', y: '50%' },
		        colors: [ { color: 'red', offset: 0.0}, { color: 'blue', offset: 0.25 }, { color: 'red', offset: 1.0 } ],
		    }
		}));

win.open();
