/** Converts numeric degrees to radians */
if ( typeof (Number.prototype.toRad) === "undefined") {
	Number.prototype.toRad = function() {
		return this * Math.PI / 180;
	}
}

/** Converts numeric radians to degrees */
if ( typeof (Number.prototype.toDeg) === "undefined") {
	Number.prototype.toDeg = function() {
		return this * 180 / Math.PI;
	}
}

if ( typeof (Number.prototype.roundDecimal) === "undefined") {
	Number.prototype.roundDecimal = function(_nbAfter) {
		var multiplier = Math.pow(10, _nbAfter);
		return Math.round(this * multiplier) / multiplier
	}
}