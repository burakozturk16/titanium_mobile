/*
 * Google account authentfication library / module
 * by Miroslav Magda, blog.ejci.net,
 *
 *
 * Copyright 2012 Miroslav Magda
 *
 * All code is open source and dual licensed under GPL and MIT. Check the individual licenses for more information.
 */

/*
 * Google authentification for Titanium
 * based on: https://developers.google.com/accounts/docs/OAuth2InstalledApp#formingtheurl
 * Check also https://code.google.com/apis/console/
 */
Ti.include('/lib/EventTarget.js');
var GoogleAuth = function(o) {
	var _version = '0.3.2';
	var _this = new EventTarget({
		isAuthorized: isAuthorized,
		deAuthorize: deAuthorize,
		getAccessToken: getAccessToken,
		refreshToken: refreshToken,
		authorize: authorize,
		version: _version
	});
	o = (o) ? o : {};
	var _opt = {
		clientId: (o.clientId) ? o.clientId : null,
		clientSecret: (o.clientSecret) ? o.clientSecret : null,
		propertyName: (o.propertyName) ? o.propertyName : 'googleToken',
		url: 'https://accounts.google.com/o/oauth2/auth',
		scope: (o.scope) ? o.scope : ['https://www.googleapis.com/auth/tasks'],
		closeTitle: (o.closeTitle) ? o.closeTitle : 'Close',
		winTitle: (o.winTitle) ? o.winTitle : 'Google Account',
		errorText: (o.errorText) ? o.errorText : 'Can not authorize user!',
		winColor: (o.winColor) ? o.winColor : '#000',
		quiet: (typeof(o.quiet) === 'undefined') ? false : o.quiet
	};
	var log = function() {};
	log.error = function(t) {
		if (!_opt.quiet) {
			Ti.API.error('' + t);
		}
	}
	log.info = function(t) {
		if (!_opt.quiet) {
			Ti.API.info(t);
		}
	}
	log.debug = function(t) {
		if (!_opt.quiet) {
			Ti.API.debug('' + t);
		}
	}
	log.trace = function(t) {
		if (!_opt.quiet) {
			Ti.API.trace('' + t);
		}
	} //UTILS

	log.info('-------------------------------------');
	log.info('| Google Account Authentification   |');
	log.info('| Titanium Module (v.:' + _version + ')        |');
	log.info('| by Miroslav Magda                 |');
	log.info('-------------------------------------');

	var win;

	var _prop = {}
	_prop.accessToken = null;
	_prop.refreshToken = null;
	_prop.tokenType = null;
	_prop.expiresIn = 0;

	var prop = {};
	prop = getProps();

	if (prop.expiresIn >= (new Date()).getTime()) {
		log.info('GoogleAuth: Access code valid');
		_prop = prop;
	}
	/*else {
	 log.info('GoogleAuth: Access code not valid. Refreshing...');
	 _prop = prop;
	 refreshToken();
	 }*/
	function getProps() {
		var p = {};
		p.accessToken = Ti.App.Properties.getString(_opt.propertyName + '.accessToken');
		p.refreshToken = Ti.App.Properties.getString(_opt.propertyName + '.refreshToken');
		p.tokenType = Ti.App.Properties.getString(_opt.propertyName + '.tokenType');
		p.expiresIn = Ti.App.Properties.getString(_opt.propertyName + '.expiresIn');
		return p;
	}

	var authorizeCounter = 0;
	var doSomethingOnLoad = true;

	function webviewAuthorizeOnLoad(e) {
		if (doSomethingOnLoad === false) return;
		log.info('webviewAuthorizeOnLoad');
		authorizeCounter++;
		var accessDenied = e.source.evalJS('document.getElementById("access_denied").value;');
		if (accessDenied != '') {
			Ti.App.Properties.setString(_opt.propertyName + '.accessToken', '');
			Ti.App.Properties.setString(_opt.propertyName + '.refreshToken', '');
			Ti.App.Properties.setString(_opt.propertyName + '.tokenType', '');
			Ti.App.Properties.setString(_opt.propertyName + '.expiresIn', 0);
			_prop.accessToken = null;
			_prop.refreshToken = null;
			_prop.tokenType = null;
			_prop.expiresIn = 0;
			_this.fireEvent('error', {
				error: 'access_denied'
			});
		}
		var title = e.source.evalJS("document.title");
		var code = title.split("=")[1];
		// var code = e.source.evalJS('document.getElementById("code").value;');
		log.info('webviewAuthorizeOnLoad code ' + code);
		if (code !== '') {
			log.debug('GoogleAuth: Access granted!');
			getToken(code);
		}
		if (authorizeCounter > 10) {
			//some error (to many requests :) )
			_this.fireEvent('error', {
				error: 'GoogleAuth: To many redirects...'
			});
		}
	}

	function authorize(webview) {
		webview.url = prepareUrl();
		authorizeCounter = 0;
		webview.removeEventListener('load', webviewAuthorizeOnLoad);
		webview.addEventListener('load', webviewAuthorizeOnLoad);
	}

	function webviewDeauthorizeOnLoad(e) {
		var t = setTimeout(function() {
			_this.fireEvent('logout');
			//w.remove(webview);
		}, 500);
		Ti.App.Properties.setString(_opt.propertyName + '.accessToken', '');
		Ti.App.Properties.setString(_opt.propertyName + '.refreshToken', '');
		Ti.App.Properties.setString(_opt.propertyName + '.tokenType', '');
		Ti.App.Properties.setString(_opt.propertyName + '.expiresIn', 0);
		_prop.accessToken = null;
		_prop.refreshToken = null;
		_prop.tokenType = null;
		_prop.expiresIn = 0;
	}

	function deAuthorize(webview) {
		if (isAuthorized()) {
			webview.url = 'https://accounts.google.com/Logout';
			webview.removeEventListener('load', webviewDeauthorizeOnLoad);
			webview.addEventListener('load', webviewDeauthorizeOnLoad);
		}
	}

	/**
	 * Refresh token
	 */
	function refreshToken(cbSuccess, cbError) {
		log.info('GoogleAuth: Access code not valid. Refreshing...');
		cbSuccess = (cbSuccess) ? cbSuccess : function() {};
		cbError = (cbError) ? cbError : function() {};
		var xhr = Ti.Network.createHTTPClient({
			// function called when the response data is available
			onload: function(e) {
				//log.info("Received text: " + this.responseText);
				var resp = JSON.parse(this.responseText);
				resp.expires_in = parseFloat(resp.expires_in, 10) * 1000 + (new Date()).getTime();
				Ti.App.Properties.setString(_opt.propertyName + '.accessToken', resp.access_token);
				Ti.App.Properties.setString(_opt.propertyName + '.tokenType', resp.token_type);
				Ti.App.Properties.setString(_opt.propertyName + '.expiresIn', resp.expires_in);
				_prop.accessToken = resp.access_token;
				_prop.tokenType = resp.token_type;
				_prop.expiresIn = resp.expires_in;
				log.debug(_prop);
				//win.close();
				cbSuccess();
			},
			// function called when an error occurs, including a timeout
			onerror: function(e) {
				log.info(e.error);
				log.info(e.responseText);
				cbError();
				//ERROR
				Titanium.UI.createAlertDialog({
					title: 'Error',
					message: _opt.errorText
				});
				cbError();
				//authorize();

			},
			timeout: 5000 /* in milliseconds */
		});
		// Prepare the connection.
		xhr.open("POST", _opt.url);
		xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
		var d = {
			client_id: _opt.clientId,
			client_secret: _opt.clientSecret,
			refresh_token: _prop.refreshToken,
			grant_type: 'refresh_token'
		}
		// Send the request.
		xhr.send(d);
	}

	/**
	 * Get TOKEN
	 */
	function getToken(code) {
		var xhr = Ti.Network.createHTTPClient({
			// function called when the response data is available
			onload: function(e) {
				//log.info("Received text: " + this.responseText);
				var resp = JSON.parse(this.responseText);
				log.info(resp.expires_in);
				resp.expires_in = parseFloat(resp.expires_in, 10) * 1000 + (new Date()).getTime();
				log.info(resp.expires_in);
				Ti.App.Properties.setString(_opt.propertyName + '.accessToken', resp.access_token);
				Ti.App.Properties.setString(_opt.propertyName + '.refreshToken', resp.refresh_token);
				Ti.App.Properties.setString(_opt.propertyName + '.tokenType', resp.token_type);
				Ti.App.Properties.setString(_opt.propertyName + '.expiresIn', resp.expires_in);
				_prop.accessToken = resp.access_token;
				_prop.refreshToken = resp.refresh_token;
				_prop.tokenType = resp.token_type;
				_prop.expiresIn = resp.expires_in;
				log.debug(_prop);
				//alert('success');
				_this.fireEvent('login', {
					token: resp.access_token
				});
			},
			// function called when an error occurs, including a timeout
			onerror: function(e) {
				_this.fireEvent('error', {
					error: _opt.errorText
				});
			},
			timeout: 5000 /* in milliseconds */
		});
		// Prepare the connection.
		xhr.open("POST", 'https://accounts.google.com/o/oauth2/token');
		xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=utf-8');
		var d = {
			code: code,
			client_id: _opt.clientId,
			client_secret: _opt.clientSecret,
			redirect_uri: 'urn:ietf:wg:oauth:2.0:oob',
			grant_type: 'authorization_code'
		}
		// Send the request.
		xhr.send(d);
	}

	/**
	 * Prepare url from options
	 */
	function prepareUrl() {
		//encodeURIComponent(_opt.scope.join('+'))
		var scope = [];
		for (var i = 0; i < _opt.scope.length; i++) {
			scope[i] = encodeURIComponent(_opt.scope[i])
		}
		var url = _opt.url + '?' + 'approval_prompt=force&scope=' + scope.join('+') + '&' + 'redirect_uri=urn:ietf:wg:oauth:2.0:oob' + '&' + 'response_type=code' + '&' + 'client_id=' + _opt.clientId + '&' + 'btmpl=mobile' + '';
		log.debug(url);
		return url;
	}

	function isAuthorized(cbSuccess, cbError) {
		cbSuccess = (cbSuccess) ? cbSuccess : function() {};
		cbError = (cbError) ? cbError : function() {};
		_prop = getProps();
		log.debug('Properties: ' + JSON.stringify(_prop));
		if (_prop.accessToken != null && _prop.accessToken != '') {
			if (_prop.expiresIn < (new Date()).getTime()) {
				refreshToken(cbSuccess, cbError);
			} else {
				cbSuccess();
			}
			return true;
		} else {
			cbError();
		}

		return false;
	}

	function getAccessToken() {
		return _prop.accessToken;
	}

	return _this;
};

module.exports = GoogleAuth;