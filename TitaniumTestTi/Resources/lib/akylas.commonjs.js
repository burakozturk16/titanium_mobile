var __myPath = Ti.resourcesRelativePath;
if (__myPath.length > 0 && __myPath[__myPath.length - 1] != '/')
	__myPath += '/';
Ti.API.info('__myPath in commonjs ' + __myPath);

exports.load = function(_context, _config) {
	var modules_dir = _config.modules_dir || this.config.modules_dir;
	var underscore_file = _config.underscore ||  this.config.underscore;
	(function(){
		if (!this['_']) {
			try {
				_ = _context._ = require(modules_dir + underscore_file);
			}
			catch(e) {
				Ti.API.error('Could not load ' + modules_dir + underscore_file);
				return;
			}
		}
	}).call(_context);
	_context._.extend(this.config, _config);
	var config = this.config;
	_context.ak = require(__myPath  +'AkInclude/akylas.global').init(_context, this.config);
};

exports.config = {
	underscore: 'underscore',
	modules_dir:'lib/'
}

exports.loadExtraWidgets = function(_context) {
	if (_context.ak.ti){
		var isApple =  Ti.Platform.osname === 'ipad' || Ti.Platform.osname === 'iphone';
		if (isApple) {

			(function(){
				ak.ti.loadCreators(__myPath  +'AkWidgets/Notification.js');
			}).call(_context);
		}
	}
};