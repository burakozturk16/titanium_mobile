/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

var bootstrap = require("bootstrap");

// Objects retained by persistent handles.
// Each element in this array acts as a storage "cell"
// keeping the object reachable and alive until it is removed.
persistentObjects = [];

// Keeps an object alive until dispose() is called.
// This is currently used to keep "top level" objects
// (ex: windows, tab groups) alive until their lifecycle ends.
function PersistentHandle(object) {
	this.cell = persistentObjects.length;
	persistentObjects.push(object);
}

PersistentHandle.prototype.dispose = function() {
	if (this.cell == -1) {
		// This handle has already been disposed.
		return;
	}

	persistentObjects.splice(this.cell, 1);
	this.cell = -1;
}

exports.PersistentHandle = PersistentHandle;

exports.bootstrap = function(Titanium) {
	require("window").bootstrap(Titanium);
	require("tabgroup").bootstrap(Titanium);
	require("tab").bootstrap(Titanium);
	require("listview").bootstrap(Titanium);
	require("webview").bootstrap(Titanium);

	Titanium.invocationAPIs.push({namespace: "UI", api: "createWindow"});
	Titanium.invocationAPIs.push({namespace: "UI", api: "createTabGroup"});
	Titanium.invocationAPIs.push({namespace: "UI", api: "createTab"});

	function iPhoneConstant(name) {
		Titanium.API.error("!!!");
		Titanium.API.error("!!! WARNING : Use of unsupported constant Ti.UI.iPhone." + name + " !!!");
		Titanium.API.error("!!!");
		return 0;
	}

	// TODO: Remove me. Only for temporary compatibility
	Titanium.UI.iPhone = {
		AnimationStyle: {
			get FLIP_FROM_LEFT() { return iPhoneConstant("AnimationStyle.FLIP_FROM_LEFT"); }
		},
		ProgressBarStyle: {
			get SIMPLE() { return iPhoneConstant("ProgressBarStyle.SIMPLE"); }
		},
		SystemButton: {
			get FLEXIBLE_SPACE() { return iPhoneConstant("SystemButton.FLEXIBLE_SPACE"); },
			get DISCLOSURE() { return iPhoneConstant("SystemButton.DISCLOSURE"); }
		},
		SystemButtonStyle: {
			get BAR() { return iPhoneConstant("SystemButtonStyle.BAR"); }
		},
		TableViewCellSelectionStyle: {
			get NONE() { return iPhoneConstant("TableViewCellSelectionStyle.NONE"); }
		},
		RowAnimationStyle: {
			get NONE() { return iPhoneConstant("RowAnimationStyle.NONE"); }
		},
		TableViewScrollPosition: {
			get MIDDLE() { return iPhoneConstant("TableViewScrollPosition.MIDDLE"); }
		},
		TableViewStyle: {
			get GROUPED() { return iPhoneConstant("TableViewStyle.GROUPED"); }
		}
	};

	var TiView = Titanium.TiView;
	TiView.prototype.toJSON = function() {
		var json = {};
		var keys = Object.keys(this);
		var len = keys.length;

		for (var i = 0; i < len; i++) {
			var key = keys[i];
			if (key == "parent") {
				continue;
			}
			json[key] = this[key];
		}
		return json;
	}

	// Define constants for ActivityIndicator here for now.
	Titanium.UI.ActivityIndicator.STATUS_BAR = 0;
	Titanium.UI.ActivityIndicator.DIALOG = 1;
	Titanium.UI.ActivityIndicator.INDETERMINANT = 0;
	Titanium.UI.ActivityIndicator.DETERMINANT = 1;

	//Create ListItemProxy, add events, then store it in 'tiProxy' property
	function processTemplate(properties) {
		var proxyType = (properties.type  || "Ti.UI.View");
		proxyType = proxyType.slice(proxyType.indexOf(".") + 1);
		properties.tiClass = Titanium.proxyBindings[proxyType];
		if (!properties.hasOwnProperty('childTemplates')) return;
		var childProperties = properties.childTemplates;
		if (childProperties === void 0 || childProperties === null) return;
		
		for (var i = 0; i < childProperties.length; i++) {
			var child = childProperties[i];
			processTemplate(child);
		}
	}

	var realAddViewTemplates = Titanium.UI.addViewTemplates;
	function addViewTemplates(templates) {
		if (templates) {
			for (var binding in templates) {
				var currentTemplate = templates[binding];
				//process template
				processTemplate(currentTemplate);
			}
		}
		realAddViewTemplates.call(this, templates);
	}

	function setBindings(_proxy , _level) {
		var bindings = _proxy.getBindings();
		for (var binding in bindings) {
			var _bproxy = bindings[binding];
			setBindings(_bproxy, (_level + 1) );
			_proxy[binding] = _bproxy;
		}
	}

	var realCreateViewFromTemplate = Titanium.UI.createViewFromTemplate;
	function createViewFromTemplate(options) {
		var proxy  = realCreateViewFromTemplate.call(this, options);
		if (proxy !== null) {
			setBindings(proxy, 0);
		}
		return proxy;
	}


	//overwrite createViewFromTemplate function with our own.
	Titanium.UI.addViewTemplates = addViewTemplates;
	Titanium.UI.createViewFromTemplate = createViewFromTemplate;
}

