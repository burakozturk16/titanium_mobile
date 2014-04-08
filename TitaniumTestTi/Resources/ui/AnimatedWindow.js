ak.ti.constructors.createAnimatedWindow = function(_args) {
	_args = _args || {};
	var self = new Window(_args);
	self.openArgs = self.openArgs || {
		opacity: 1,
		duration: 200
	};
	self.closeArgs = self.closeArgs || {
		opacity: 0,
		duration: 200
	};
	// self.winManager = self.winManager || app.ui;
	self.handleOpen = self.handleClose = true;
	self._opened = false;
	self._closing = false;

	function onWinOpen() {
		self.animate(self.openArgs, function() {
			app.ui.windowSignalsOpened(self);
		});
		if (self.onShow) {
			self.onShow();
		}
	}
	self.showMe = function(_force) {
		if (self._opened) return;
		self._closing = false;
		self._opened = true; 
		self._closing = false;
		if (self.beforeShow) {
			self.beforeShow();
		}
		self.applyProperties(self.closeArgs);
		app.ui.openWindow(self);
		
	};
	self.hideMe = function() {
		if (!self._opened || self._closing) return;
		self._closing = true;
		self._opened = false;



		if (self.onHide) {
			self.onHide();
		}
		if (self.closeInOnHide !== true) {
			self.animate(self.closeArgs, function() {
				self._closing = false;
				app.ui.closeWindow(self);
			});
		} else {
			self.animate(self.closeArgs);
		}
		
	};
	self.addEventListener('open', onWinOpen);

	//END OF CLASS. NOW GC 
	var _parentGC = self.GC;
	self.GC = function() {
		if (_parentGC) _parentGC();
		self = null;
	};
	return self;
};