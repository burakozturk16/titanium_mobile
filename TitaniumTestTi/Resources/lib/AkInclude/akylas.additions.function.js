Function.prototype.extend = function(parent) {
	var child = this;
	child.prototype = parent;
	child.prototype.$super = parent;
	child.prototype = new child(Array.prototype.slice.call(1, arguments));
	child.prototype.constructor = child;
}
Function.prototype.inheritsFrom = function(parentClassOrObject) {
	if (parentClassOrObject.constructor == Function) {
		//Normal Inheritance
		this.prototype = new parentClassOrObject;
		this.prototype.constructor = this;
		this.prototype.$super = parentClassOrObject.prototype;
	} else {
		//Pure Virtual Inheritance
		this.prototype = parentClassOrObject;
		this.prototype.constructor = this;
		this.prototype.$super = parentClassOrObject;
	}
	return this;
}
