var __myPath = Ti.resourcesRelativePath;
Ti.API.debug('__myPath ' + __myPath);
if (__myPath.length > 0 && __myPath[__myPath.length - 1] != '/')
	__myPath += '/';
function createModule(context, _config) {
	var module = {};
	var production = (Ti.App.deployType === 'production');
	if (production === true) {
		Ti.API.info = function() {
		};
		Ti.API.debug = function() {
		};
	}

	module.prepareAppObject = function(_app) {
		if (module.ti) {
			_app.deviceinfo = module.ti.getPlatformInfo();
			_app.info = module.ti.getAppInfo();
		} else
			Ti.API.debug('prepareAppObject: missing ak.ti module');
	};

	var modules = _config.modules || ['ti', 'moment', 'lang', 'animation'];

	for (var i = 0, j = modules.length; i < j; i++) {
		var moduleStr = modules[i];
		if (moduleStr === 'ti') {
			if (module.redux)
				continue;
			module.redux = context.redux = require(__myPath + 'redux').inject(context);
			module.ti = require(__myPath + 'akylas.ti').init(context);
		} else if (moduleStr === 'moment') {
			if (module.moment)
				continue;
			var path = _config.modules_dir + 'moment/';
			context.moment = require(path + 'moment');
			context.moment.loadLangs = function() {
				for (var i = 0, j = arguments.length; i < j; i++) {
					var lang = null;
					try {
						lang = require(path + 'lang/' + arguments[i]);
						if (lang === null)
							lang = require('/moment/lang/' + arguments[i]);
					} catch(e) {
					}

					if (lang !== null) {
						context.moment.lang(arguments[i], lang);
						context.moment.lang(arguments[i]);
					} else
						debug('could not load moment lang: ' + arguments[i]);
				}
			};
			module.moment = context.moment;
		} else if (moduleStr === 'lang') {
			if (module.locale)
				continue;
			module.locale = require(__myPath + 'akylas.lang');
			if ( typeof akdefaultlang !== 'undefined')
				module.locale.defaultLanguage = akdefaultlang;
			context.tr = module.locale.tr;
			context.loadLanguage = module.locale.loadLanguage;
			if (modules.indexOf('moment') !== -1) {
				module.locale.moment = module.moment;
				try {
					module.moment.loadLangs(module.locale.currentLanguage);
					// module.moment.lang(module.locale.currentLanguage);
				} catch(e) {
				}

			}
		} else {
			var name = moduleStr.toLowerCase();
			if (module[name])
				continue;
			module[name] = require(__myPath + 'akylas.' + name).init(context);
		}
	}

	var additions = _config.additions || [];

	(function() {
		for (var i = 0, j = additions.length; i < j; i++) {
			var addition = additions[i];
			if (addition.indexOf('.') === -1)
				addition = 'akylas.additions.' + additions[i].toLowerCase();
			Ti.include(__myPath + addition + '.js');
		}
	}).call(context);
	context.stringify = context.stringify || JSON.stringify;
	return module;
}

if ( typeof exports === 'undefined') {
	var ak = createModule(this, ak_config);
} else {
	exports.init = createModule;
}
