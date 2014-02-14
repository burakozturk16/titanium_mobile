var xcode = require('xcode'),
	wrench = require('wrench'),
	path = require('path'),
	fs = require('fs');

var args = process.argv.slice(2);
var projectPath = args[0];
var destPath = path.join(args[1], path.basename(projectPath));

var myProj = xcode.project(path.join(projectPath, 'project.pbxproj'));
	// parsing is async, in a different process

var onDone = (function (err) {
	if (err) {
    	console.error(err);
    	process.exit(1);
	}
	var group = myProj.removeGroupByName("_dependencies_");
	if (fs.existsSync(destPath)) {
		wrench.rmdirSyncRecursive(destPath);
	}
	wrench.mkdirSyncRecursive(destPath);
    fs.writeFileSync(path.join(destPath, 'project.pbxproj'), myProj.writeSync());
	process.exit(0);
}).bind(this);

myProj.parse(onDone);
