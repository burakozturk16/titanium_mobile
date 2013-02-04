var win = Titanium.UI.createWindow();
win.backgroundColor = '#ccc';
 
var tableview = Ti.UI.createTableView({
	backgroundColor:'black'
});
// tableview.transform = Ti.UI.create2DMatrix({rotate:90})
win.add(tableview);

var isandroid = (Titanium.Platform.name == 'android') ;

function createMenu(){
	var menuRow = Ti.UI.createView({
		layout:'horizontal',
		// className:'menu',
		height:60,
		width:200,
		bottom:0,
		right:0,
		backgroundColor:'black'
	})
	// menuRow.container = Ti.UI.createView({
		// layout:'horizontal'
	// })
	var buttons = [];
	for (var i=0; i < 6; i++) {
	  var button = Ti.UI.createButton({
	  	title: i,
	  	color:'white',
	  	backgroundImage:'none',
	  	backgroundColor:'black',
	  	style:'none',
	  	width:60,
	  	height:60,
	  	font:{fontSize:20, fontWeight:'bold'}
	  });
	  menuRow.add(button);
	  // menuRow.container.add(button);
	  buttons.push(button);
	};
	// menuRow.add(menuRow.container);
	return menuRow;
}

var currentMenuIndex = -1;
var currentRow = null;
var currentMenu = null;
function showMenuForRow(e){
	if (e.row === currentMenu) return;
	if (e.hasOwnProperty('index')){
		var index = e.index;
		if (currentRow != null){
			// if (currentMenuIndex >=0 && currentMenuIndex < index)
				// index -= 1;
			// if (isandroid)
			// {
				// var oldMenu = currentMenu;
				// oldMenu.container.animate({height:0,duration:100}, function(){
					// tableview.deleteRow(oldMenu);
				// });
			// }
			// else{
				currentRow.className = currentRow.realClassName;
				delete currentRow.realClassName;
				currentRow.container.remove(currentMenu);
				// tableview.deleteRow(currentMenu);
			// }
			currentRow = null;
			currentMenu = null;
		}
		if (index != currentMenuIndex){
			currentMenu = createMenu();
			// tableview.insertRowAfter(index, currentMenu);
			// if (isandroid)
				// setTimeout(function(){
					// currentMenu.container.animate({height:200,duration:200});
				// },10)
			currentMenuIndex = index;
			e.row.container.add(currentMenu);
			
			currentRow = e.row;
			currentRow.realClassName = currentRow.className;
			currentRow.className = 'rowWithMenu';
			// tableview.scrollToIndex(index + 1);
			tableview.scrollToIndex(index);
		}
		else{
			currentMenuIndex = -1;
		}
	}
}

function hideMenu(){
	if (currentRow != null){
			// if (currentMenuIndex >=0 && currentMenuIndex < index)
				// index -= 1;
			// if (isandroid)
			// {
				// var oldMenu = currentMenu;
				// oldMenu.container.animate({height:0,duration:100}, function(){
					// tableview.deleteRow(oldMenu);
				// });
			// }
			// else{
				currentRow.className = currentRow.realClassName;
				delete currentRow.realClassName;
				currentRow.container.remove(currentMenu);
				// tableview.deleteRow(currentMenu);
			// }
			currentRow = null;
			currentMenu = null;
		}
}
	tableview.addEventListener('click', showMenuForRow);
	// tableview.addEventListener('scroll', hideMenu);

function createCustomRow(obj){
	var row = Ti.UI.createTableViewRow({
		className:'rowClassic',
		backgroundImage:'/images/tabbgd1.png',
		selectionStyle:'none',
		height:120
	})
	row.container = Ti.UI.createView();
	
	
	var posterContainer = Ti.UI.createImageView({
		left:10,
		width:70,
		height:70
	});

	var imgpath = 'http://cf2.imgobject.com/t/p/w300'+obj.poster_path;
	row.imageView = Ti.UI.createImageView({
		width:70,
		height:70,
		scaleType:Ti.UI.SCALE_TYPE_ASPECT_FILL,
		image:imgpath
	});
	
	posterContainer.add(row.imageView);
	row.imageView.add(Ti.UI.createView({
		backgroundImage:'/images/posterbgd.png'
	}))
	
	var textcontainer = Ti.UI.createView({
		top:0,
		bottom:0,
		left:70,
		right:70,
		layout:'vertical'
	})
	row.titleLabel = Ti.UI.createLabel({
		textAlign:'left',
		left:0,
		text:obj.title,
		color:'white',
		shadowColor:'black',
		shadowOffset:{x:0,y:2},
		shadowRadius:4,
		font:{fontSize:20, fontWeight:'bold'}
	});
	row.subtitleLabel = Ti.UI.createLabel({
		textAlign:'left',
		left:0,
		top:10,
		text:obj.original_title,
		font:{fontSize:12},
		color:'gray'
	});
	
	var rightcontainer = Ti.UI.createView({
		width:70,
		right:0,
		top:0,
		bottom:0,
		layout:'vertical'
	})
	
	row.yearLabel = Ti.UI.createLabel({
		textAlign:'right',
		top:4,
		text:obj.release_date,
		font:{fontSize:10, fontWeight:'bold'},
		shadowColor:'black',
		shadowOffset:{x:0,y:2},
		shadowRadius:4,
		color:'white'
	});
	posterContainer.add(row.yearLabel);
	
	row.voteLabel = Ti.UI.createLabel({
		textAlign:'right',
		right:0,
		text:obj.vote_average,
		font:{fontSize:14},
		color:'red',
		height:Ti.UI.FILL,
		verticalAlign:Ti.UI.TEXT_VERTICAL_ALIGNMENT_BOTTOM
	});
	
	row.container.add(posterContainer)
	textcontainer.add(row.titleLabel)
	textcontainer.add(row.subtitleLabel)
	row.container.add(textcontainer)
	rightcontainer.add(row.voteLabel)
	row.container.add(rightcontainer)
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

function processData(e){
	var rowData = [];
	var objs = JSON.parse(e).results;
	for(var i=0,j=objs.length; i<j; i++){
		rowData.push(createCustomRow(objs[i]));
	};
	tableview.data = rowData;
}

var privateurl = 'http://private-e9ba-themoviedb.apiary.io/3/';
var apikey='2d06dfd032252c2f28640c29b6f0b067';
function getData(){
	var url = privateurl + "movie/popular?api_key=" + apikey;
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
 
getData();
win.open();