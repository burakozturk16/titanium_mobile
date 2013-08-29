var win = Titanium.UI.currentWindow;
var image = Titanium.UI.createImageView({
	backgroundColor:'yellow',
	image:'../images/body.png',
	imageMask:'../images/body-mask.png'
});
changed = false;
image.addEventListener('click', function()
{
	if (!changed)
	{
		image.animate({transform:Ti.UI.create2DMatrix().scale(0.5,0.5), duration:500});
		changed = true;
	}
	else
	{
		image.animate({transform:Ti.UI.create2DMatrix(), duration:500});
		changed=false;
	}
});
win.add(image);

