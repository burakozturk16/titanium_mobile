/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
var url = require("url"),
	Script = kroll.binding('evals').Script;

var TAG = "js_NavigationWindow";

exports.bootstrap = function(Titanium) {
	var NavigationWindow = Titanium.UI.NavigationWindow;

	// Set constants for representing states for the tab group
	NavigationWindow.prototype.state = {closed: 0, opening: 1, opened: 2};

	function createNavigationWindow(scopeVars, options) {
		var nav = new NavigationWindow(options);
		nav._windows = [];
		if (options) {
			var window = options.window;
			nav._windows.push(window);
		}

		// Keeps track of the current navigationwindow state
		nav.currentState = nav.state.closed;

		// Set the activity property here since we bind it to _internalActivity for window proxies by default
		Object.defineProperty(NavigationWindow.prototype, "activity", { get: nav.getActivity});

		return nav;
	}
	Titanium.UI.createNavigationWindow = createNavigationWindow;

	var _open = NavigationWindow.prototype.open;
	NavigationWindow.prototype.open = function(options) {

		if (this.currentState == this.state.opened) {
			return;
		}

		this.currentState = this.state.opening;
		_open.call(this, options);
		this.currentState = this.state.opened;
	}

	NavigationWindow.prototype.onWindowClosed  = function(e) {
		if (e._closeFromActivityForcedToDestroy) {
			if (kroll.DBG) {
				kroll.log(TAG, "Window is closed because the activity is forced to destroy by Android OS.");
			}
			return;
		}
		var window = e.source;
		var index = this._windows.indexOf(window);
		if (index > -1) {
			
			this._windows.splice(index, 1);
			if (kroll.DBG) {
				kroll.log(TAG, "removing window from NavigationWindow at index " + index);
			}
		}
		// Dispose the URL context if the window's activity is destroyed.
		if (window._urlContext) {
			Script.disposeContext(window._urlContext);
			window._urlContext = null;
		}
 
		if (kroll.DBG) {
			kroll.log(TAG, "Window is closed normally from NavigationWindow.");
		}
		window = null;
	}

	var _openWindow = NavigationWindow.prototype.openWindow;
	NavigationWindow.prototype.openWindow = function(window, options) {
		if (!window) {
			return;
		}

		if (!options) {
			options = {};
		}
		var index = this._windows.indexOf(window);
		if (index == -1) { //already opened window
			// var handle = new PersistentHandle(window);
			this._windows.push(window);
			// Retain the window until it has closed.
			window.once("close", NavigationWindow.prototype.onWindowClosed.bind(this));
		}
//		window.open(options);

		_openWindow.call(this, window, options);
	}

	var _setWindow = NavigationWindow.prototype.setWindow;
	NavigationWindow.prototype.setWindow = function(window) {
		if (this.currentState != this.state.opened) {
			this._windows = [window];
		} else {
			kroll.log(TAG, "Cannot set window after navigationwindow opens");
		}
	}

	// NavigationWindow.prototype.getWindow = function() {
	// 	return this._windows[0];
	// }

	// Object.defineProperty(NavigationWindow.prototype, 'window', {
	// 	enumerable: true,
	// 	set: NavigationWindow.prototype.setWindow,
	// 	get: NavigationWindow.prototype.getWindow
	// });
}

