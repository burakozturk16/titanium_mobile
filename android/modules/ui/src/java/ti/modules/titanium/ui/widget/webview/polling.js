function checkForJSCode() {
	var result = TiApp.hasResult();
	if ( result != -1) {
		if (result == 1) {
			var code = TiApp.getJSCode();
			if (code != undefined) {
				eval(code);
			}
			else {
				clearInterval(refreshIntervalId);
			}
			code = null;
		}
	}
	else {
		clearInterval(refreshIntervalId);
	}
	result = null;
}

var refreshIntervalId = setInterval(checkForJSCode, 250);