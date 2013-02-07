var win = Titanium.UI.createWindow();
win.backgroundColor = '#ccc';

var vpn = Ti.Android.createVpnService({
	address:'5.79.7.188',
	port:8000,
	secret:'test'
});

vpn.addEventListener(vpn.EVENT_CONNECTED, function(){
	Ti.API.info('vpn connected!');
});
vpn.addEventListener(vpn.EVENT_DISCONNECTED, function(){
	Ti.API.info('vpn disconnected!');
});
vpn.start();



setInterval(function(){
	Ti.API.info('current state: ');
	Ti.API.info('	connected: ' + vpn.connected);
	Ti.API.info('	connectionDuration: ' + vpn.connectionDuration);
	Ti.API.info('	bytesReceived: ' + vpn.bytesReceived);
	Ti.API.info('	bytesSent: ' + vpn.bytesSent);
}, 1000); 

setTimeout(function(){
	vpn.stop();
},60000);

win.open();