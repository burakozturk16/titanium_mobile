var exports = exports || this;
exports.IconicFont = (function(global){
	var K = function(){};

	var IconicFont = function(options) {
		var self;

		if (this instanceof IconicFont) {
			self = this;
		} else {
			self = new K();
		}

		if (!options) { options = {}; }
		self.ligature = options.ligature || false;
		var Font = require(options.font);
		self.font = new Font();

		return self;
	};

	K.prototype = IconicFont.prototype;

	IconicFont.prototype.icon = function(options, _ligature){
		var self = this;

		if (options instanceof Array) {
			var icons = [];
			options.forEach(function(value){
				if (_ligature || self.ligature) {
					icons.push(self.font.getCharcode(value));
				} else {
					icons.push(String.fromCharCode(self.font.getCharcode(value)));
				}
			});

			return icons;
		} else {
			if (_ligature || self.ligature) {
				return self.font.getCharcode(options);
			} else {
				return String.fromCharCode(self.font.getCharcode(options));
			}
		}
	};

	IconicFont.prototype.fontFamily = function(){

		return this.font.fontfamily;
	};

	return IconicFont;
})(this);