var win = Titanium.UI.currentWindow;
win.barColor = '#385292';
//
// CREATE SEARCH BAR
//
var search = Titanium.UI.createSearchBar({
	height:80,
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
var row = Ti.UI.createTableViewRow();
row.backgroundColor = '#576996';
row.selectedBackgroundColor = '#385292';
row.height = 40;
var clickLabel = Titanium.UI.createLabel({
	text:'Click different parts of the row',
	color:'#fff',
	textAlign:'center',
	font:{fontSize:14},
	width:'auto',
	height:'auto'
});
row.className = 'header';
row.add(clickLabel);
data.push(row);

// when you click the header, scroll to the bottom
row.addEventListener('click',function()
{
	tableView.scrollToIndex(40,{animated:true,position:Ti.UI.iPhone.TableViewScrollPosition.TOP});
});

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

var animClearTransform = Ti.UI.createAnimation({
    duration:200,
    transform:Ti.UI.create2DMatrix()
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
		selectedColor:'yellow',
		shadowColor:'black',
		shadowOffset:{x:0,y:1},
		shadowRadius:10,
        title:'1',
        right:15
    })
    btn1.addEventListener('click', function(e){
    	var oldselected = e.source.selected
    	e.source.selected = !oldselected;
    	e.source.title = (oldselected?'1':'2');
    })

    var btn2 = Ti.UI.createButton({
    	style:'none',
		font:{fontSize:40, fontFamily:fontname},
		color:'white',
		selectedColor:'red',
		shadowColor:'black',
		shadowOffset:{x:0,y:1},
		shadowRadius:10,
        title:'3',
        right:15
    })
    btn2.addEventListener('click', function(e){
    	e.source.selected = !e.source.selected;
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
    currentRowMenu = _row;
    currentMenu = menu;
    menu = null;
}


// create the rest of the rows
for (var c=1;c<200;c++)
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
		left:-menuWidth,
		right:0,
		top:0,
		bottom:0,
		backgroundColor:"#222"
	});

	var rowrealcontainer =  Ti.UI.createView({
		left:menuWidth,
		right:0,
		backgroundColor:"#666"
	});

	var photo = Ti.UI.createView({
		backgroundImage:'../images/custom_tableview/user.png',
		top:5,
		left:10,
		width:50,
		height:50,
		clickName:'photo'
	});
	rowrealcontainer.add(photo);


	var user = Ti.UI.createLabel({
		color:'white',
		font:{fontSize:16,fontWeight:'bold', fontFamily:'Arial'},
		left:70,
		top:2,
		height:30,
		width:200,
		clickName:'user',
		text:'Fred Smith '+c
	});

	row.filter = user.text;
	rowrealcontainer.add(user);

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
		text:'Got some fresh fruit, conducted some business, took a nap'+c
	});
	rowrealcontainer.add(comment);

	var calendar = Ti.UI.createView({
		backgroundImage:'../images/custom_tableview/eventsButton.png',
		bottom:2,
		left:70,
		width:32,
		clickName:'calendar',
		height:32
	});
	rowrealcontainer.add(calendar);

	var button = Ti.UI.createView({
		backgroundImage:'../images/custom_tableview/commentButton.png',
		top:35,
		right:5,
		width:36,
		clickName:'button',
		height:34
	});
	rowrealcontainer.add(button);

	var date = Ti.UI.createLabel({
		color:'#999',
		font:{fontSize:13,fontWeight:'normal', fontFamily:'Arial'},
		left:105,
		bottom:5,
		height:20,
		width:100,
		clickName:'date',
		text:'posted on 3/11'+c
	});
	rowrealcontainer.add(date);
	row.container.add(rowrealcontainer);
	row.add(row.container);
	data.push(row);
}


//
// create table view (
//
tableView = Titanium.UI.createTableView({
	data:data,
	search:search,
	filterAttribute:'filter',
	backgroundColor:'white'
});


// tableView.addEventListener('swipe', function(e)
// {
//     if(e.row)
//     {
//     	if (e.row.isUpdateRow) return;
//     	if (e.direction == 'right') {
//         	showMenuForRow(e.row);
//     	}
//     	else if (e.direction == 'left') {
//         	if (currentRowMenu == e.row)
// 				hideMenu();
//     	}
        
//     }
// });

var percentColors = [
    { pct: 0.0, color: { r: 0x22, g: 0x22, b: 0x22 } },
    { pct: 1.0, color: { r: 0xff, g: 0x00, b: 0 } } ];

var getColorForPercentage = function(pct) {
    for (var i = 0; i < percentColors.length; i++) {
        if (pct <= percentColors[i].pct) {

            var lower = (i>0)?percentColors[i - 1]:percentColors[i];
            var upper = percentColors[i];
            var range = upper.pct - lower.pct;
            var rangePct = (pct - lower.pct) / range;
            var pctLower = 1 - rangePct;
            var pctUpper = rangePct;
            var color = {
                r: Math.floor(lower.color.r * pctLower + upper.color.r * pctUpper),
                g: Math.floor(lower.color.g * pctLower + upper.color.g * pctUpper),
                b: Math.floor(lower.color.b * pctLower + upper.color.b * pctUpper)
            };
            var rgb = color.b | (color.g << 8) | (color.r << 16);
    		return '#' + rgb.toString(16);
            // or output as hex if preferred
        }
    }
}

var touchDecaleMax = 80;
var currentDelta = 0;

var touchRow = null;
var touchRowPoint = null;
function handleTouchEnd(e)
{
    if(touchRow)
    {
    	if (e.type === 'touchend' && currentDelta >= (touchDecaleMax*0.92))
    		alert('action success');
    	touchRow.container.animate(animClearTransform);
    	touchRow = null;
    	currentDelta = 0;
    }
}
tableView.addEventListener('touchstart', function(e)
{
    if(e.row && currentRowMenu != e.row)
    {
    	touchRow = e.row;
    	touchRowPoint = e.globalPoint;
    }
});
tableView.addEventListener('touchend', handleTouchEnd);
tableView.addEventListener('touchcancel', handleTouchEnd);
tableView.addEventListener('touchmove', function(e)
{
    if(touchRow)
    {
    	var delta = Math.max(0, Math.min(Math.floor(e.globalPoint.x - touchRowPoint.x), touchDecaleMax));
    	if (delta != currentDelta) 
    	{
    		currentDelta = delta;
    		touchRow.container.backgroundColor = getColorForPercentage(currentDelta/touchDecaleMax);
        	touchRow.container.transform  = Ti.UI.create2DMatrix().translate(currentDelta, 0);
    	}
    }
});


tableView.addEventListener('singletap', function(e)
{
	if (e.source.toString().indexOf('Button') != -1 ) return;
	if (currentRowMenu == e.row)
		hideMenu();
	else if (e.row)
		showMenuForRow(e.row);
});

win.add(tableView);

