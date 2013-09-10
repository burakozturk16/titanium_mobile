var win = Titanium.UI.currentWindow;

var imageView = Titanium.UI.createImageView({
	image:'http://www.appcelerator.com/wp-content/uploads/2009/06/titanium_desk.png',
	width:261,
	height:78,
	top:20
});

imageView.addEventListener('load', function()
{
	Ti.API.info('LOAD CALLED');
});
win.add(imageView);

var l = Titanium.UI.createLabel({
	text:'Click Image',
	bottom:30,
	color:'#999',
	height:'auto',
	width:300,
	textAlign:'center'
});
win.add(l);

function clicker()
{
	Titanium.UI.createAlertDialog({title:'Image View', message:'You clicked me!'}).show();
	l.text = "Try again. You shouldn't get alert and the image should be different";
	imageView.image = '../images/cloud.png';
	imageView.removeEventListener('click',clicker);
}

imageView.addEventListener('click', clicker);
l.addEventListener('click', function(){
	if (imageView.scaleType === Ti.UI.SCALE_TYPE_SCALE_TO_FILL)
		imageView.scaleType = Ti.UI.SCALE_TYPE_ASPECT_FIT
	else if (imageView.scaleType === Ti.UI.SCALE_TYPE_ASPECT_FIT)
		imageView.scaleType = Ti.UI.SCALE_TYPE_ASPECT_FILL
	else if (imageView.scaleType === Ti.UI.SCALE_TYPE_ASPECT_FILL)
		imageView.scaleType = Ti.UI.SCALE_TYPE_CENTER
	else if (imageView.scaleType === Ti.UI.SCALE_TYPE_CENTER)
		imageView.scaleType = Ti.UI.SCALE_TYPE_TOPLEFT
	else if (imageView.scaleType === Ti.UI.SCALE_TYPE_TOPLEFT)
		imageView.scaleType = Ti.UI.SCALE_TYPE_BOTTOMRIGHT
	else
		imageView.scaleType = Ti.UI.SCALE_TYPE_SCALE_TO_FILL
});


