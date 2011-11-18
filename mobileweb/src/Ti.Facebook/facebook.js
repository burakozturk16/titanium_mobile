(function(api){
	// Interfaces
	Ti._5.EventDriven(api);

	// Properties
	var _accessToken = null;
	Object.defineProperty(api, 'accessToken', {
		get: function(){return _accessToken;},
		set: function(val){return _accessToken = val;}
	});

	var _appid = null;
	Object.defineProperty(api, 'appid', {
		get: function(){return _appid;},
		set: function(val){return _appid = val;}
	});

	var _expirationDate = null;
	Object.defineProperty(api, 'expirationDate', {
		get: function(){return _expirationDate;},
		set: function(val){return _expirationDate = val;}
	});

	Object.defineProperty(api, 'forceDialogAuth', {
		get: function(){return true;},
		set: function(val){return true;}
	});

	var _loggedIn = false;
	Object.defineProperty(api, 'loggedIn', {
		get: function(){return _loggedIn;},
		set: function(val){return _loggedIn = val;}
	});

	var _permissions = null;
	Object.defineProperty(api, 'permissions', {
		get: function(){return _permissions;},
		set: function(val){return _permissions = val;}
	});

	var _uid = null;
	Object.defineProperty(api, 'uid', {
		get: function(){return _uid;},
		set: function(val){return _uid = val;}
	});

	// Setup the Facebook initialization callback
	var _facebookInitialized = false;
	var _authAfterInitialized = false;
	window.fbAsyncInit = function() {
		FB.init({
			appId  : _appid, // App ID
			status : true, // check login status
			cookie : true, // enable cookies to allow the server to access the session
			oauth  : true, // enable OAuth 2.0
			xfbml  : true  // parse XFBML
		});
		FB.getLoginStatus(function(response){
			if (response.status == "connected" && response.authResponse) {
				_loggedIn = true;
			} else if (_authAfterInitialized) {
				api.authorize();
			}
			_facebookInitialized = true;
		});
	};
	
	// Create the div required by Facebook
	fbDiv = document.createElement('div');
	fbDiv.id = 'fb-root';
	document.getElementsByTagName('body')[0].appendChild(fbDiv);
	
	// Load the Facebook SDK Asynchronously.
	
	var fbScriptTag, id = 'facebook-jssdk'; 
	if (!document.getElementById(id)) {
		fbScriptTag = document.createElement('script');
		fbScriptTag.id = id; 
		fbScriptTag.async = true;
		fbScriptTag.src = "//connect.facebook.net/en_US/all.js";
		document.getElementsByTagName('head')[0].appendChild(fbScriptTag);
	}

	// Methods
	api.authorize = function(){
		
		// Sanity check
		if (_appid == null) {
			throw new Error('App ID not set. Facebook authorization cancelled.');
		}
		
		// Check if facebook is still initializing, and if so queue the auth request
		if (!_facebookInitialized) {
			_authAfterInitialized = true;
			return;
		}
		
		// Authorize
		FB.login(function(response) {
			var undef;
			var oEvent = {
				cancelled	: false,
				data		: response,
				error		: undef,
				source		: api,
				success		: false,
				type		: undef,
				uid			: response.id
			};
			if (response.authResponse) {
				_expirationDate = new Date((new Date()).getTime() + response.authResponse.expiresIn * 1000);
				FB.api('/me', function(response) {
					if (!response) {
						oEvent.error = "An unknown error occured.";
					} else if (response.error) {
						oEvent.error = response.error;
					} else {
						_loggedIn = true;
						_uid = response.id;
						oEvent.success = true;
						oEvent.uid = _uid;
					}
					api.fireEvent('login', oEvent);
				});
			} else {
				oEvent.cancelled = true
				oEvent.error = "The user cancelled or an internal error occured."
				api.fireEvent('login', oEvent);
			}
		}, {'scope':_permissions.join()});
	};
	api.createLoginButton = function(parameters){
		console.debug('Method "Titanium.Facebook.createLoginButton" is not implemented yet.');
	};
	api.dialog = function(action,params,callback){
		params.method = action;
		FB.ui(params,function(response){
			if (!response) {
				var undef;
				callback({'success':false,'error':undef,'action':action,'source':api});
			} else if (response.error) {
				callback({'success':false,'error':response.error,'action':action,'source':api});
			} else {
				callback({'success':true,'result':response,'action':action,'source':api});
			}
		});
	};
	api.logout = function(){
		FB.logout(function(response) {
			_loggedIn = false;
			var undef;
			var oEvent = {
				source		: api,
				type		: undef
			};
			api.fireEvent('logout', oEvent);
		});
	};
	api.request = function(method,params,callback){
		params.method = method;
		params.urls = 'facebook.com,developers.facebook.com';
		FB.api(params,function(response){
			if (!response) {
				var undef;
				callback({'success':false,'error':undef,'method':method,'source':api});
			} else if (response.error) {
				callback({'success':false,'error':response.error,'method':method,'source':api});
			} else {
				callback({'success':true,'result':JSON.stringify(response),'method':method,'source':api});
			}
		});
	};
	api.requestWithGraphPath = function(path,params,httpMethod,callback){
		FB.api(path,httpMethod,params,function(response){
			if (!response) {
				var undef;
				callback({'success':false,'error':undef,'path':path,'source':api});
			} else if (response.error) {
				callback({'success':false,'error':response.error,'path':path,'source':api});
			} else {
				callback({'success':true,'result':JSON.stringify(response),'path':path,'source':api});
			}
		});
	};
})(Ti._5.createClass('Titanium.Facebook'));