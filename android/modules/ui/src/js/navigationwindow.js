/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

exports.bootstrap = function(Titanium) {
	var NavigationWindow = Titanium.UI.NavigationWindow;

	function createNavigationWindow(scopeVars, options) {
		var nav = new NavigationWindow(options);
		nav._windows = [];
		if (options) {
			nav._windows.push(options.window);
		}

		// Keeps track of the current navigationwindow state
		nav.currentState = nav.state.closed;

		// Set the activity property here since we bind it to _internalActivity for window proxies by default
		Object.defineProperty(TabGroup.prototype, "activity", { get: nav.getActivity});

		return nav;
	}

	Titanium.UI.createNavigationWindow = createNavigationWindow;

	var _open = NavigationWindow.prototype.open;
	NavigationWindow.prototype.open = function(options) {

		if (this.currentState == this.state.opened) {
			return;
		}
		
		this.currentState = this.state.opening;

		// Retain the tab group until is has closed.
		var handle = new PersistentHandle(this);

		var self = this;
		this.on("close", function(e) {
			if (e._closeFromActivityForcedToDestroy) {
				if (kroll.DBG) {
					kroll.log(TAG, "NavigationWindow is closed because the activity is forced to destroy by Android OS.");
				}
				return;
			}

			self.currentState = self.state.closed;
			handle.dispose();

			if (kroll.DBG) {
				kroll.log(TAG, "NavigationWindow is closed normally.");
			}
		});

		_open.call(this, options);

		this.currentState = this.state.opened;
	}

	var _openWindow = NavigationWindow.prototype.openWindow;
	NavigationWindow.prototype.openWindow = function(window, options) {
		if (!window) {
			return;
		}

		if (!options) {
			options = {};
		}
		_windows.push(window);
		_openWindow.call(this, window, options);
	}

	var _closeWindow = NavigationWindow.prototype.closeWindow;
	NavigationWindow.prototype.closeWindow = function(window, options) {
		if (!window) {
			return;
		}

		if (!options) {
			options = {};
		}
		_windows.remove(window);
		_closeWindow.call(this, window, options);
	}

	var _setWindow = NavigationWindow.prototype.setWindow;
	NavigationWindow.prototype.setWindow = function(window) {
		if (this.currentState != this.state.opened) {
			this._windows = [window];
		} else {
			kroll.log(TAG, "Cannot set window after navigationwindow opens");
		}
	}

	NavigationWindow.prototype.getWindow = function() {
		return this._windows[0];
	}

	Object.defineProperty(NavigationWindow.prototype, 'window', {
		enumerable: true,
		set: NavigationWindow.prototype.setWindow,
		get: NavigationWindow.prototype.getWindow
	});
}

