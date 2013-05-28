
var win = Ti.UI.createWindow({
	backgroundColor:'white',
	exitOnClose:true,
	title:'TestAutoBoot'
});

var imgpath = '/images/poster.jpg';

var identity = Ti.UI.create2DMatrix();
var transformed = identity.translate(-180, -80).scale(0.2, 0.2);

var isTransformed = true;

var imageview = Ti.UI.createImageView({
	top:0,
	left:0,
	right:0,
	bottom:0,
	scaleType:Ti.UI.SCALE_TYPE_ASPECT_FILL,
	image:imgpath,
	transform: transformed
});
win.add(imageview);

function createFullscreenImageWindow(_args) {
	var self = Ti.UI.createWindow(_args);
	var initRect = null;
	var initTransform = null;


	var imageWidget = Ti.UI.createImageView({
        image: _args.image,
		scaleType:Ti.UI.SCALE_TYPE_ASPECT_FIT,
	    localLoadSync:true,
	    backgroundColor:'black',
	    opacity:0,
		top:0,
		left:0,
		right:0,
		bottom:0,
		hires:true
	});
	if (_args.initialRect) {
		initRect = _args.initialRect;
		Ti.API.info('we got an init rect')
	}

    self.add(imageWidget);
	self.showAnimated = function()
	{
		var args = {
			opacity:1,
			duration:200
		}
		if (initRect !== null) {
			var currentRect = imageview.rect;
			Ti.API.info('currentRect ' + JSON.stringify(currentRect))
			Ti.API.info('initRect ' + JSON.stringify(initRect))
			var newCenter = {
				x:(initRect.x + initRect.width/2),
				y:(initRect.y + initRect.height/2)
			};
			var center = {
				x:(currentRect.x + currentRect.width/2),
				y:(currentRect.y + currentRect.height/2)
			};
			var scale = initRect.width/currentRect.width;
			Ti.API.info('newCenter ' + JSON.stringify(newCenter))
			Ti.API.info('center ' + JSON.stringify(center))
			Ti.API.info('scale ' + scale)
			initTransform = imageWidget.transform = Ti.UI.create2DMatrix().translate((newCenter.x - center.x), (newCenter.y - center.y)).scale(initRect.width/currentRect.width, initRect.height/currentRect.height);
			args.transform = Ti.UI.create2DMatrix();
		}
//		setTimeout(function(){
			imageWidget.animate(args);
//		}, 10)
	};

	self.addEventListener('click', function()
	{
		if (initTransform !== null) {
			imageWidget.animate({transform:initTransform, opacity:0, duration:1000}, function(){
				self.close();
			});
		}
		else {
			self.close({opacity:0, duration:200});
		}

	});
	
	function onFirstLayout(){
		self.showAnimated();
		self.removeEventListener('postlayout', onFirstLayout);
	}

	self.addEventListener('postlayout', onFirstLayout);
    return self;
}


imageview.addEventListener('singletap', function(){
	if (isTransformed)
		imageview.animate({transform:identity, duration:200});
//		imageview.transform = identity;
	else
		imageview.animate({transform:transformed, duration:200});
	isTransformed = !isTransformed;
//		imageview.transform = transformed;
//	var initialRect = imageview.absoluteRect;
//    var win = createFullscreenImageWindow({
//    	navBarHidden:true,
//		image : imgpath,
//		initialRect:initialRect
//	});
//    win.open({animated:false});
});

win.open();