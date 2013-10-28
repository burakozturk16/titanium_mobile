/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
var url = require("url"),
	Script = kroll.binding('evals').Script,
	PersistentHandle = require('ui').PersistentHandle;

var TAG = "SlideMenu";

exports.bootstrap = function(Titanium) {
	var SlideMenu = Titanium.UI.SlideMenu;

	// Set constants for representing states for the tab group
	SlideMenu.prototype.state = {closed: 0, opening: 1, opened: 2};

	function createSlideMenu(scopeVars, options) {
		kroll.log(TAG, "createSlideMenu");
		var slidemenu = new SlideMenu(options);
		slidemenu._windows = [];
		if (options) {
			slidemenu._windows.push(options.window);
		}

		// Keeps track of the current navigationwindow state
		slidemenu.currentState = slidemenu.state.closed;

		// Set the activity property here since we bind it to _internalActivity for window proxies by default
		Object.defineProperty(SlideMenu.prototype, "activity", { get: slidemenu.getActivity});

		return slidemenu;
	}
	Titanium.UI.createSlideMenu = createSlideMenu;

	var _open = SlideMenu.prototype.open;
	SlideMenu.prototype.open = function(options) {

		if (this.currentState == this.state.opened) {
			return;
		}
		
		this.currentState = this.state.opening;
		_open.call(this, options);
		this.currentState = this.state.opened;
	}

	// var _setWindow = SlideMenu.prototype.setWindow;
	// SlideMenu.prototype.setWindow = function(window) {
	// 	if (this.currentState != this.state.opened) {
	// 		this._windows = [window];
	// 	} else {
	// 		kroll.log(TAG, "Cannot set window after navigationwindow opens");
	// 	}
	// }

	// SlideMenu.prototype.getWindow = function() {
	// 	return this._windows[0];
	// }

	// Object.defineProperty(SlideMenu.prototype, 'window', {
	// 	enumerable: true,
	// 	set: SlideMenu.prototype.setWindow,
	// 	get: SlideMenu.prototype.getWindow
	// });
}

