var win = Titanium.UI.currentWindow;
win.backgroundColor = '#ccc';
 
var tableview = Ti.UI.createTableView({
	separatorColor:'transparent',
	separatorStyle:Titanium.UI.TableViewSeparatorStyle.NONE,
	backgroundColor:'black'
});
// tableview.transform = Ti.UI.create2DMatrix({rotate:90})
win.add(tableview);

var isandroid = (Titanium.Platform.name == 'android') ;


var menuWidth = 200;
var currentRowMenu = null;
var currentMenu = null;
var oldMenu = null;
var oldRowMenu = null;
 
var animShow = Ti.UI.createAnimation({
    duration:200,
    left:0,
    right:-menuWidth
})
 
var animHide = Ti.UI.createAnimation({
    duration:200,
    left:-menuWidth,
    right:0
})
animHide.addEventListener('complete', function(){
    if (oldRowMenu === null) return;
    oldRowMenu.container.remove(oldMenu);
    oldMenu = null;
    oldRowMenu = null;
})
function hideMenu()
{
    if (currentRowMenu!=null)
    {
        oldMenu = currentMenu;
        oldRowMenu = currentRowMenu;
        oldRowMenu.container.animate(animHide);
        currentMenu = null;
        currentRowMenu = null;
        return true;
    }
    return false;
}
 
var fontname = 'Iconminia';
function showMenuForRow(_row)
{
	if (currentRowMenu == _row) {
		hideMenu();
		return;
	}
    hideMenu();
 
    currentRowMenu = _row;

    var menu = Ti.UI.createView({
        width:menuWidth,
        layout:'horizontal',
        horizontalWrap:false,
        left:0,
        top:0,
        bottom:0,
        backgroundImage:'/images/menubgd.png'
    })
    menu.add(Ti.UI.createView({
        width:Ti.UI.FILL
    }));

    var btn1 = Ti.UI.createButton({
    	style:'none',
		font:{fontSize:40, fontFamily:fontname},
		color:'white',
		shadowColor:'black',
		selectedColor:'yellow',
		shadowOffset:{x:0,y:1},
		shadowRadius:10,
        title:'1',
        right:15
    })

    function setFav(value){
    	currentRowMenu.favs = value;
    	btn1.title = (value?'2':'1');
    	btn1.color = (value?'yellow':'white');
    }
    setFav(currentRowMenu.favs);

    btn1.addEventListener('click', function(e){
    	setFav(!currentRowMenu.favs);
    })

    var btn2 = Ti.UI.createButton({
    	style:'none',
		font:{fontSize:40, fontFamily:fontname},
		shadowColor:'black',
		selectedColor:'red',
		shadowOffset:{x:0,y:1},
		shadowRadius:10,
        title:'3',
        right:15
    })
    function setLove(value){
    	currentRowMenu.love = value;
    	btn2.color = (value?'red':'white');
    }
    setLove(currentRowMenu.love);
    btn2.addEventListener('click', function(e){
    	setLove(!currentRowMenu.love);
    })

    var btn3 = Ti.UI.createButton({
    	style:'none',
		font:{fontSize:40, fontFamily:fontname},
		color:'white',
		selectedColor:'gray',
		shadowColor:'black',
		shadowOffset:{x:0,y:1},
		shadowRadius:10,
        title:'4',
        right:15
    })
    menu.add(btn1);
    menu.add(btn2);
    menu.add(btn3);

    _row.container.add(menu);
    _row.container.animate(animShow);
    currentMenu = menu;
    menu = null;
}
 
tableview.addEventListener('swipe', function(e)
{
    if(e.row)
    {
    	if (e.row.isUpdateRow) return;
    	if (e.direction == 'right') {
        	showMenuForRow(e.row);
    	}
    	else if (e.direction == 'left') {
        	if (currentRowMenu == e.row)
				hideMenu();
    	}
        
    }
});
 


function createCustomRow(obj){
	var row = Ti.UI.createTableViewRow({
		className:'rowClassic',
		height:80,
		favs:false,
		love:false
	})
	row.container = Ti.UI.createView({
		left:-menuWidth,
		right:0,
		top:0,
		bottom:0
	});

	var rowrealcontainer =  Ti.UI.createView({
		left:menuWidth,
		right:0,
		backgroundImage:'/images/tabbgd1.png'
	});
	
	
	// var posterContainer = Ti.UI.createImageView({
	// 	left:6,
		
	// 	width:70,
	// 	height:70
	// });

	var imgpath = 'http://cf2.imgobject.com/t/p/w300'+obj.poster_path;
	row.imageView = Ti.UI.createImageView({
		preventDefaultImage:true,
		// alwaysLoadAsync:false,
		left:6,
		width:70,
		height:70,
		scaleType:Ti.UI.SCALE_TYPE_ASPECT_FILL,
		image:imgpath
	});
	
	var posterHover = Ti.UI.createView({
		left:6,
		width:70,
		height:70,
		backgroundImage:'/images/posterbgd.png'
	})
	
	var textcontainer = Ti.UI.createView({
		top:10,
		bottom:10,
		left:80,
		right:80,
		layout:'vertical'
	})
	row.titleLabel = Ti.UI.createLabel({
		textAlign:'left',
		left:0,
		text:obj.title,
		color:'white',
		shadowColor:'black',
		maxLines:1,
		shadowOffset:{x:0,y:1},
		shadowRadius:10,
		font:{fontSize:20, fontWeight:'bold'}
	});
	row.subtitleLabel = Ti.UI.createLabel({
		textAlign:'left',
		left:0,
		top:10,
		text:obj.release_date,
		shadowColor:'black',
		color:'white',
		maxLines:1,
		shadowOffset:{x:0,y:1},
		shadowRadius:10,
		font:{fontSize:12, fontWeight:'bold'}
	});
	
	// var rightcontainer = Ti.UI.createView({
	// 	width:70,
	// 	right:0,
	// 	top:0,
	// 	bottom:0,
	// 	layout:'vertical'
	// })
	
	row.yearLabel = Ti.UI.createLabel({
		textAlign:'right',
		top:4,
		text:obj.vote_average,
		font:{fontSize:10, fontWeight:'bold'},
		shadowColor:'black',
		shadowOffset:{x:0,y:1},
		shadowRadius:10,
		color:'white'
	});
	
	// row.voteLabel = Ti.UI.createLabel({
	// 	textAlign:'right',
	// 	right:0,
	// 	text:obj.vote_average,
	// 	font:{fontSize:14},
	// 	color:'red',
	// 	height:Ti.UI.FILL,
	// 	verticalAlign:Ti.UI.TEXT_VERTICAL_ALIGNMENT_BOTTOM
	// });
	
	rowrealcontainer.add(row.imageView)
	rowrealcontainer.add(posterHover)
	posterHover.add(row.yearLabel);
	textcontainer.add(row.titleLabel)
	textcontainer.add(row.subtitleLabel)
	rowrealcontainer.add(textcontainer)
	// rightcontainer.add(row.voteLabel)
	// rowrealcontainer.add(rightcontainer)
	row.container.add(rowrealcontainer);
	row.add(row.container);
	return row;
}

function createRow(obj){
	var imgpath = 'http://cf2.imgobject.com/t/p/w300'+obj.poster_path;
	Ti.API.info('processing ' + imgpath)
	return {
		height:120,
		title:obj.title,
		leftImage:imgpath
	};
}

function loadNextPage(){
	page += 1;
	getData();
}

var page = 1;
function processData(e){
	var objs = JSON.parse(e).results;
	if (page == 1) {
		var rowData = [];
		for(var i=0,j=objs.length; i<j; i++){
		rowData.push(createCustomRow(objs[i]));
		};
		tableview.data = rowData;
	}
	else {
		for(var i=0,j=objs.length; i<j; i++){
			tableview.appendRow(createCustomRow(objs[i]));
		};
	}
	
}

var privateurl = 'http://private-e9ba-themoviedb.apiary.io/3/';
var apikey='2d06dfd032252c2f28640c29b6f0b067';
function getData(){
	var url = privateurl + "movie/popular?api_key=" + apikey + '&page=' + page;
	 var client = Ti.Network.createHTTPClient({
	     // function called when the response data is available
	     onload : function(e) {
	         Ti.API.info("Received text: " + this.responseText);
	         processData(this.responseText);
	     },
	     // function called when an error occurs, including a timeout
	     onerror : function(e) {
	         Ti.API.debug(e.error);
	         // alert('error');
	     },
	     timeout : 5000  // in milliseconds
	 });
	 // Prepare the connection.
	 client.open("GET", url);
	 client.setRequestHeader("Accept", "application/json")
	 // Send the request.
	 client.send(); 
}

if (isandroid === false) {
var more = Titanium.UI.createButton({
	systemButton:Titanium.UI.iPhone.SystemButton.ADD
});

more.addEventListener('click', loadNextPage);


win.setRightNavButton(more);
}
 
getData();
