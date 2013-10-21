/*
 * package.js: Titanium iOS CLI package hook
 *
 * Copyright (c) 2012, Appcelerator, Inc.  All Rights Reserved.
 * See the LICENSE file for more information.
 */

var appc = require('node-appc'),
	i18n = appc.i18n(__dirname),
	__ = i18n.__,
	__n = i18n.__n,
	afs = appc.fs,
	fs = require('fs'),
	path = require('path'),
	async = require('async'),
	wrench = require('wrench'),
	exec = require('child_process').exec;

exports.cliVersion = '>=3.X';

exports.init = function (logger, config, cli) {
	
	cli.addHook('build.post.compile', {
		priority: 8000,
		post: function (build, finished) {
			if (/dist-playstore/.test(cli.argv.target)) return finished();
			
			if (cli.argv['build-only'] && cli.argv['output-dir']) {
				var apk = path.join(build.buildBinDir, 'app.apk')
				dest = path.join(cli.argv['output-dir'], build.tiapp.name + '.apk');
				afs.exists(dest) && fs.unlink(dest);
				afs.copyFileSync(apk, dest, { logger: logger.debug });

				return finished();
			}
		}
	});
	
};
