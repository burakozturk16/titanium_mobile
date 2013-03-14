
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
		Titanium.Android.currentActivity.removeEventListener("resume", onResume);
		Titanium.Android.currentActivity.removeEventListener("resumed", onResume);
		win.open();
	}
	Titanium.Android.currentActivity.addEventListener("resume", onResume);
	Titanium.Android.currentActivity.addEventListener("resumed", onResume);
}
else
{
//	var activity = win.getActivity();
//	activity.invalidateOptionsMenu();
	function onResume (){
		Titanium.Android.currentActivity.removeEventListener("resume", onResume);
		Titanium.Android.currentActivity.removeEventListener("resumed", onResume);
		win.open();
	}
	Titanium.Android.currentActivity.addEventListener("resume", onResume);
	Titanium.Android.currentActivity.addEventListener("resumed", onResume)
	Titanium.Android.currentActivity.moveTaskToBack();
//	activity.moveTaskToBack();
//	win.open();
}
