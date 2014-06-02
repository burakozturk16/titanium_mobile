if ( typeof (String.prototype.assign) === "undefined") {
	String.prototype.assign = function() {
		var assign = {};
		_.each(arguments, function(element, index, list) {
			if (_.isObject(element)) {
				_.extend(assign, element);
			} else assign[index + 1] = element;
		});
		return this.replace(/\{([^{]+?)\}/g, function(m, key) {
			return _.has(assign, key) ? assign[key] : m;
		});
	}
}