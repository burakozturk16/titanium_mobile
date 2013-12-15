var appc = require('node-appc'),
	fs = require('fs'),
	path = require('path'),
	__ = appc.i18n(__dirname).__;

exports.cliVersion = '>=3.2';
			
exports.init = function(logger, config, cli) {

	cli.on('build.android.aapt', {
		pre: function(data, next) {
			var args = data.args[1];
			args.push('--auto-add-overlay');

			var externalLibraries = [{
				javaClass:'com.actionbarsherlock',
				resPath:path.join(this.platformPath, 'externals', 'actionbarsherlock/res')
			}];

			externalLibraries.forEach(function(lib) {
				args.push('--extra-packages', lib.javaClass, '-S', lib.resPath);
			});

			next(data);
		}
	});
};