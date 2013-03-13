
var wasStartedAtBoot = Ti.App.Android.wasStartedAtBoot;
Ti.API.info('test0: ' + wasStartedAtBoot);

var win = Ti.UI.createWindow({
	backgroundColor:'white',
	modal:false,
	exitOnClose:true,
	title:'TestAutoBoot'
});
Ti.API.info('test1');

var bc = Ti.Android.createBroadcastReceiver({
	onReceived: function(e) {
		Ti.API.info(JSON.stringify(e));
	}
});
Ti.Android.registerBroadcastReceiver(bc, [Ti.Android.ACTION_AIRPLANE_MODE_CHANGED,Ti.Android.ACTION_BOOT_COMPLETED]);

Ti.API.info('test2');
if (wasStartedAtBoot) {
	function onResume (){
		Ti.App.removeEventListener("resume", onResume);
		Ti.App.removeEventListener("resumed", onResume);
		win.open();
	}
	Ti.App.addEventListener("resume", onResume);
	Ti.App.addEventListener("resumed", onResume);
}
else
	win.open();
