var locale = {
	defaultLanguage : 'en',
	currentLanguage : 'en',
	jsonLang : {},
	moment : undefined
};

locale.loadLanguage = function(_context, _lang) {
	try {
		var langFile = _lang || Titanium.Locale.currentLanguage;
		// grab language from device
		// Ti.API.debug('Current language is ' + langFile);
		var file = Ti.Filesystem.getFile(Ti.Filesystem.resourcesDirectory, 'lang', langFile + '.json');

		if (!file.exists()) {// file is found
			Ti.API.debug('Could not find ' + file.nativePath);
			langFile = locale.defaultLanguage;
			// default language.
			file = Ti.Filesystem.getFile(Ti.Filesystem.resourcesDirectory, 'lang', langFile + '.json');
		}
		if (file.exists()) {
			// Ti.API.debug('loading lang file :' + file.nativePath);
			locale.jsonLang = eval.call(_context || this, '(' + file.read() + ')');
			// Ti.API.debug('loaded lang file :' + JSON.stringify(locale.jsonLang));
		}
		// grab it & use it
		locale.currentLanguage = langFile;
		if (locale.moment) {
			try {
				locale.moment.loadLangs(locale.currentLanguage);
			} catch(e) {
				Ti.API.debug('loadLanguage moment error ' + JSON.stringify(e));
			}
		}
	} catch (e) {
		Ti.API.debug('loadLanguage error ' + JSON.stringify(e));
		locale.jsonLang = {};
	}
};

locale.appendLanguage = function(_context, _dir) {
	try {
		var file = Ti.Filesystem.getFile(Ti.Filesystem.resourcesDirectory, _dir,'lang', locale.currentLanguage + '.json');
		if (file.exists()) {
			_.extend(locale.jsonLang, eval.call(_context || this, '(' + file.read() + ')'));
		}
	} catch (e) {
		Ti.API.debug('appendLanguage error ' + JSON.stringify(e));
	}
};

locale.tr = function(_id, _default) {
	_default = _default || _id;
	if (locale.jsonLang.hasOwnProperty(_id))
		return locale.jsonLang[_id];
	else
		return _default;
};

exports = module.exports = locale;
