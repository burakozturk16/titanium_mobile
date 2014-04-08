(function() {
	var global = this;

	/**
	 * Utilities namespace.
	 *
	 * @namespace
	 */

	var util = {};
	if ( typeof exports !== 'undefined') {
		if ( typeof module !== 'undefined' && module.exports) {
			exports = module.exports = util;
		}
		else
			exports.util = util;
	} else {
		if ( typeof ak !== 'undefined')
			ak.util = util;
		else
			context.util = util;
	}
	/**
	 * Transforms a querystring in to an object
	 *
	 * @param {String} qs
	 * @api public
	 */

	util.chunkQuery = function(qs) {
		var query = {}, params = qs.split('&'), i = 0, l = params.length, kv;

		for (; i < l; ++i) {
			kv = params[i].split('=');
			if (kv[0]) {
				query[kv[0]] = kv[1];
			}
		}

		return query;
	};

	util.urlParamsToString = function(_params) {
		var string = '';
		if (checkObj(_params)) {
			var i = 0;
			for (pkey in _params) {
				var sep = (i == 0) ? '?' : '&';
				string = string + sep + pkey + '=';
				if (is("boolean", _params[pkey]))
					string += _params[pkey] ? "true" : "false";
				else
					string += _params[pkey];
				i += 1;
			}
		}
		return string;
	};

	/**
	 * Merges two objects.
	 *
	 * @api public
	 */

	util.merge = function merge(target, additional, deep, lastseen) {
		var seen = lastseen || [], depth = typeof deep == 'undefined' ? 2 : deep, prop;

		for (prop in additional) {
			if (additional.hasOwnProperty(prop) && seen.indexOf(seen, prop) < 0) {
				if ( typeof target[prop] !== 'object' || !depth) {
					target[prop] = additional[prop];
					seen.push(additional[prop]);
				} else {
					util.merge(target[prop], additional[prop], depth - 1, seen);
				}
			}
		}

		return target;
	};

	util.insertAt = function(array, index) {
		var arrayToInsert = Array.prototype.splice.apply(arguments, [2]);
		return insertArrayAt(array, index, arrayToInsert);
	};

	util.insertArrayAt = function(array, index, arrayToInsert) {
		Array.prototype.splice.apply(array, [index, 0].concat(arrayToInsert));
		return array;
	};

	util.formatAddress = function(_address, _type) {
		var format = tr("headingAddress");
		if (_address.hasOwnProperty('Street'))
			format = format.replace('#s', _address.Street);
		if (_address.hasOwnProperty('City'))
			format = format.replace('#ci', _address.City);
		if (_address.hasOwnProperty('State'))
			format = format.replace('#st', _address.State);
		if (_address.hasOwnProperty('ZIP'))
			format = format.replace('#z', _address.ZIP);
		if (_address.hasOwnProperty('Country'))
			format = format.replace('#co', _address.Country);
		return format;
	};

}).call(this);
