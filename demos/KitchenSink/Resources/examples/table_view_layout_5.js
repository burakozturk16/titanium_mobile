var win = Ti.UI.currentWindow;
win.backgroundColor = 'white';
var tableView = Ti.UI.createTableView();
var toolbar = createToolbar();
var data = [];
var selectedRows = {};
var nbSelected = 0;

function selectRow(_row)
{
	_row.selected = _row.selectButton.selected = true;
        // selectButton.backgroundImage = '/images/select_on.png';
        _row.backgroundColor = '#D9E7FD';
        _row.separatorColor = '#B9CFF5';
}

function unselectRow(_row)
{
	_row.selected = _row.selectButton.selected = false;
	_row.backgroundColor = _row.unread?'white':'#efefef';
	_row.separatorColor = '#777';
}

function starRow(_row)
{
	_row.starred = _row.starButton.selected = true;
}

function unstarRow(_row)
{
	_row.starred = _row.starButton.selected = false;
}

function createRow(c) {
    var row = Ti.UI.createTableViewRow({
        selectionStyle:'none'
    });
    row.selected = false;
    row.unread = !! Math.round(Math.random() * 1);
    row.starred = !! Math.round(Math.random() * 1);
    row.attachment = !! Math.round(Math.random() * 1);
    var important=Math.floor(Math.random()*5);
    var label=Math.floor(Math.random()*3);
    row.selectedBackgroundColor = 'transparent';
    row.height = 80;
    row.className = 'datarow';
    row.clickName = 'row';
    row.index = c;
    
 
    row.selectButton = Ti.UI.createButton({
        backgroundImage:'/images/select_off.png',
        backgroundSelectedImage:'/images/select_on.png',
        left:10,
        width:20,
        height:20,
        clickName:'select'
    });
    
    var viewCenter = Ti.UI.createView({
        layout:'vertical', 
        left:40, 
        right:40
    });

    var viewSenderDate = Ti.UI.createView({
        height:30,
        bottom:5
    });
 
    var senderView = Ti.UI.createLabel({
        color:'black',
        font:{fontSize:16,fontWeight:(row.unread?'bold':'normal')},
        left:0,
        top:0,
        height:Ti.UI.FILL,
        right:50,
        clickName:'user',
        text:'Fred Smith '+c
    });
    
    var attachmentView = Ti.UI.createView({
        backgroundImage:'/images/button_attach.png',
        right:38,
        top:5,
        width:24,
        height:16,
        clickName:'attachment'
    });
    attachmentView.visible = row.attachment;
    
    var dateView = Ti.UI.createLabel({
        color:'#444',
        font:{fontSize:14,fontWeight:'normal'},
        width:35,
        top:0,
        right:0,
        height:20,
        clickName:'date',
        text:'7/'+ (c % 32)
    });
    
    var viewimportanceSubject = Ti.UI.createView({
        height:20
    });
    
    var importanceView = Ti.UI.createView({
        left:0,
        width:16,
        height:10,
        clickName:'importance'
    });
    
        
    var subjectView = Ti.UI.createLabel({
        color:(row.unread?'black':'#444'),
        font:{fontSize:14,fontWeight:(row.unread?'bold':'normal')},
        left:20,
        top:0,
        height:Ti.UI.FILL,
        right:0,
        wordWrap:false,
        clickName:'subject',
        text:'This is a sample subject'+c
    });
    
    if (important === 0)
    {
        subjectView.left = 0;
        importanceView.visible = false;
    }
    else {
        importanceView.backgroundImage = '/images/importance' + important + '.png';
    }
   
     var viewmessageLabels = Ti.UI.createView({
        height:20
    });
    
    var messageView = Ti.UI.createLabel({
        color:'#444',
        font:{fontSize:14,fontWeight:'normal'},
        left:0,
        top:0,
        height:Ti.UI.FILL,
        wordWrap:false,
        clickName:'message',
        text:'This is a sample core message text which actually needs to be pretty long so that we see what we want to see'+c
    });
    
    row.starButton = Ti.UI.createButton({
        right:10,
        width:20,
        height:20,
        selected:row.starred,
        backgroundImage:'/images/star_off.png',
        backgroundSelectedImage:'/images/star_on.png',
        clickName:'star'
    });
 
    viewSenderDate.add(senderView);
    viewSenderDate.add(attachmentView);
    viewSenderDate.add(dateView);
    
    viewimportanceSubject.add(importanceView);
    viewimportanceSubject.add(subjectView);
    
    viewmessageLabels.add(messageView);
    // viewmessageLabels.add(labelsView);
    
    viewCenter.add(viewSenderDate);
    viewCenter.add(viewimportanceSubject);
    viewCenter.add(viewmessageLabels);
    
    row.add(row.selectButton);
    row.add(viewCenter);
    row.add(row.starButton);
    return row;
}
 
var updating = false;
var loadingRow = Ti.UI.createTableViewRow({
    height:80,
    title:"Show more messages...",
    clickName: 'loadingRow',
    color:'blue'
});

function addRows()
{
    updating = true;
    tableView.deleteRow(loadingRow);
    var lastCurrentRow = data.length -1;
    var lastRow = 50;
    for (var c=0;c<lastRow;c++)
    {
        var row = createRow(c);
        data.push(row);
    }
    Ti.API.info('adding ' + data.length + ' rows');
    tableView.data = data;
    tableView.appendRow(loadingRow);
    updating = false;
}

tableView.addEventListener('click', function(e){
    if (e.source.clickName === 'loadingRow')
        addRows();
    else if (e.source.clickName === 'select')
    {
        if (e.row.selected)
        {
            delete selectedRows[e.index];
            unselectRow(e.row);
            nbSelected -= 1;
            toolbar.setNbSelected(nbSelected);
            if (nbSelected === 0)
                toolbar.hideMe();
        }
        else
        {
            selectedRows[e.index] = e.row;
            selectRow(e.row);
            nbSelected += 1;
            toolbar.setNbSelected(nbSelected);
            if (nbSelected === 1) //first selected
                toolbar.showMe();
        }
    }
    else if (e.source.clickName === 'star')
    {
        if (e.row.starred)
        	unstarRow(e.row);
        else
            starRow(e.row);
    }
    
})

addRows();
win.add(tableView);
win.add(toolbar);

function createToolbar()
{
    var toolbar = Ti.UI.createView({
        left:-1,
        right:-1,
        height:38,
        bottom:-1,
        borderColor:'#6E6E6F',
        borderWidth:1,
        backgroundGradient: {
            type: 'linear',
            startPoint: { x: '50%', y: 0 },
            endPoint: { x: '50%', y:'100%' },
            colors: [ { color: '#424245', offset: 0.0}, { color: '#181819', offset: 1.0 } ],
            backfillStart:true
        }
    });
    toolbar.transform = Ti.UI.create2DMatrix().translate(0,toolbar.height);

    
    var labelNbSelected = Ti.UI.createLabel({
        color:'white',
        textAlign:'center',
        font:{fontSize:14,fontWeight:'normal'},
        left:10,
        width:25,
        height:25,
        wordWrap:false,
        backgroundColor:'#48484A',
        borderRadius:2
    });
    
    toolbar.setNbSelected = function(_nb)
    {
        labelNbSelected.text = _nb;
    }
    
     var buttonUnselect = Ti.UI.createButton({
        image:'/images/button_close.png',
        style:'plain',
        font:{fontSize:15,fontWeight:'normal'},
        right:5,
        width:30,
        height:30
    });
    
    buttonUnselect.addEventListener('click', function(_event)
    {
        if (nbSelected === 0) return;
        for (var index in selectedRows)
        {
            unselectRow(selectedRows[index]);
        }
        selectedRows = {};
        nbSelected = 0;
        toolbar.setNbSelected(0);
        toolbar.hideMe();
    });
    
    
    toolbar.showMe = function()
    {
        var animation = Ti.UI.createAnimation();
        animation.transform = Ti.UI.create2DMatrix();
        toolbar.animate(animation);
    }
    
    toolbar.hideMe = function()
    {
        var animation = Ti.UI.createAnimation();
        animation.transform = Ti.UI.create2DMatrix().translate(0,toolbar.height);
        toolbar.animate(animation);
    }
    
    toolbar.add(labelNbSelected);
    toolbar.add(buttonUnselect);
    return toolbar;
}
win.add(toolbar);