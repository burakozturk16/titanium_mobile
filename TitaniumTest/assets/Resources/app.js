// This is a test harness for your module
// You should do something interesting in this harness
// to test out the module and to provide instructions
// to users on how to use it by example.


// open a single window
var win = Ti.UI.createWindow({
    backgroundColor:'gray'
});
win.barColor = '#385292';

//
// CREATE SEARCH BAR
//
var search = Titanium.UI.createSearchBar({
	barColor:'#385292',
	showCancel:false
});
search.addEventListener('change', function(e)
{
	e.value; // search string as user types
});
search.addEventListener('return', function(e)
{
	search.blur();
});
search.addEventListener('cancel', function(e)
{
	search.blur();
});

var tableView;
var data = [];
var menuWidth = 200;

// create first row
//var row = Ti.UI.createTableViewRow();
//row.backgroundColor = '#576996';
//row.selectedBackgroundColor = '#385292';
//row.height = 40;
//var clickLabel = Titanium.UI.createLabel({
//	text:'Click different parts of the row',
//	color:'#fff',
//	textAlign:'center',
//	font:{fontSize:14},
//	width:'auto',
//	height:'auto'
//});
//row.className = 'header';
//row.add(clickLabel);
//data.push(row);

// when you click the header, scroll to the bottom
//row.addEventListener('click',function()
//{
//	tableView.scrollToIndex(40,{animated:true,position:Ti.UI.iPhone.TableViewScrollPosition.TOP});
//});

// create update row (used when the user clicks on the row)
function createUpdateRow(text)
{
	var updateRow = Ti.UI.createTableViewRow();
	updateRow.backgroundColor = '#13386c';
	updateRow.selectedBackgroundColor = 'transparent';

	// add custom property to identify this row
	updateRow.isUpdateRow = true;
	var updateRowText = Ti.UI.createLabel({
		color:'#fff',
		font:{fontSize:20, fontWeight:'bold'},
		text:text,
		width:'auto',
		height:'auto'
	});
	updateRow.className = 'updated_row';
	updateRow.add(updateRowText);
	return updateRow;
}
// create a var to track the active row
var currentRow = null;
var currentRowIndex = null;

// create the rest of the rows
for (var c=1;c<50;c++)
{
	var row = Ti.UI.createTableViewRow({
		selectionStyle: Titanium.UI.iPhone.TableViewCellSelectionStyle.NONE,
		backgroundColor:"#222"
	});

	row.selectedBackgroundColor = '#fff';
	row.height = 100;
	row.className = 'datarow';
	row.clickName = 'row';

	row.container = Ti.UI.createView({
		left:0,
		right:0,
		top:0,
		bottom:0,
		backgroundColor:"#666"
	})

	var photo = Ti.UI.createView({
		backgroundImage:'/images/custom_tableview/user.png',
		top:5,
		left:10,
		width:50,
		height:50,
		clickName:'photo'
	});
	row.container.add(photo);

	var text = 'Fred Smith '+c;
	var user = Ti.UI.createLabel({
		color:'white',
		font:{fontSize:16,fontWeight:'bold', fontFamily:'Arial'},
		left:70,
		top:2,
		height:30,
		width:200,
		clickName:'user',
		text:text
	});

	row.name = text;
	row.container.add(user);

	var fontSize = 16;
	if (Titanium.Platform.name == 'android') {
		fontSize = 14;
	}
	var comment = Ti.UI.createLabel({
		color:'silver',
		font:{fontSize:fontSize,fontWeight:'normal', fontFamily:'Arial'},
		left:70,
		top:21,
		height:50,
		width:200,
		clickName:'comment',
		text:'Got some fresh fruit, conducted some business, took a nap'
	});
	row.container.add(comment);

	var calendar = Ti.UI.createView({
		backgroundImage:'/images/custom_tableview/eventsButton.png',
		bottom:2,
		left:70,
		width:32,
		clickName:'calendar',
		height:32
	});
	row.container.add(calendar);

	var button = Ti.UI.createView({
		backgroundImage:'/images/custom_tableview/commentButton.png',
		top:35,
		right:5,
		width:36,
		clickName:'button',
		height:34
	});
	row.container.add(button);

	var date = Ti.UI.createLabel({
		color:'#999',
		font:{fontSize:13,fontWeight:'normal', fontFamily:'Arial'},
		left:105,
		bottom:5,
		height:20,
		width:100,
		clickName:'date',
		text:'posted on 3/11'
	});
	row.container.add(date);

	row.add(row.container);
	data.push(row);
}


//
// create table view (
//
tableView = Titanium.UI.createTableView({
	data:data,
	search:search,
	filterAttribute:'name',
	backgroundColor:'white'
});

var currentRowMenu = null;
var currentMenu = null;

function hideMenu()
{
	if (currentRowMenu!=null)
	{
		var oldMenu = currentMenu;
		var oldRowMenu = currentRowMenu;
		oldRowMenu.container.animate({left:0, duration:200});
		oldMenu.animate({left:-menuWidth, duration:200}, function(){
			oldRowMenu.container.left = 0;
			oldRowMenu.remove(oldMenu);
			oldMenu = null;
			oldRowMenu = null;
		});
	}
}

function showMenuForRow(_row)
{
	hideMenu();

	var menu = Ti.UI.createView({
		width:menuWidth,
		layout:'horizontal',
		horizontalWrap:false,
		left:-menuWidth,
		top:0,
		bottom:0,
		height:Ti.UI.FILL,
		backgroundColor:"#3ff"
	})
	menu.add(Ti.UI.createView({
		width:Ti.UI.FILL
	}));
	menu.add(Ti.UI.createButton({
		title:'1',
		right:10
	}));
	menu.add(Ti.UI.createButton({
		title:'2',
		right:10
	}));
	menu.add(Ti.UI.createButton({
		title:'3',
		right:10
	}));
	_row.add(menu);
	menu.animate({left:0, duration:200});
	_row.container.animate({left:menuWidth, duration:200});
	currentRowMenu = _row;
	currentMenu = menu;
	menu = null;
}

tableView.addEventListener('swipe', function(e)
{
	if(e.row && e.direction == 'right')
	{
		if (e.row.isUpdateRow) return;
		showMenuForRow(e.row);
	}
});


//tableView.addEventListener('singletap', function(e)
//{
//	if (!e.row) return;
//	if (e.source.toString() == '[object TiUIButton]') return;
//	Ti.API.info('table view row clicked - source ' + e.source);
//	if (e.row.isUpdateRow) return;
//	// use rowNum property on object to get row number
//	var rowNum = e.index;
//	var updateRow = createUpdateRow('You clicked on the '+e.source.clickName);
//	tableView.updateRow(rowNum,updateRow,{animationStyle:Titanium.UI.iPhone.RowAnimationStyle.LEFT});
//});

win.add(tableView);

win.open();