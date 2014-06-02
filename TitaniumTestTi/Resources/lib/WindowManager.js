var exports = exports || this;

function isFunction(functionToCheck) {
	var getType = {};
	return functionToCheck && getType.toString.call(functionToCheck) === '[object Function]';
}
exports.WindowManager = (function(global) {
	var isAndroid = Ti.Platform.osname === 'android';
	var K = function(_args) {
		return {
			_winId: 0,
			_managers: {},
			openedWindows: [],
			handlingOpening: false,
			defaultWinOpeningArgs: _args.winOpeningArgs || {}
		}
	};

	var WindowManager = function(_args) {
		var self;

		if (this instanceof WindowManager) {
			self = this;

		} else {
			self = new K(_args);
		}

		for (var key in self) {
			var obj = self[key];
			if (isFunction(obj))
			{
				self[key] = obj.bind(self);
			}
			
		}
		self._onWindowOpenedDelayed = _.debounce(self._onWindowOpened, 1000);
		self._winId = 0;
		self._managers = {};
		self.openedWindows = [];
		self.shouldDelayOpening = _args.shouldDelayOpening !== false;
		self.handlingOpening = false;
		self.defaultWinOpeningArgs = _args.winOpeningArgs || {};
		return self;
	};

	K.prototype = WindowManager.prototype;

	WindowManager.prototype._onBack = function(_win, e) {
		if (_win.handleClose === true && _win.hideMe) {
			_win.hideMe();
		}
		else this.closeWindow(_win);
	};

	WindowManager.prototype._onWindowOpened = function(e) {
		//if the window handles its open animation let's it set the variable
		//true later as it might be too soon now
		if (e.source.handleOpen !== true) {
			this.windowSignalsOpened(e.source);
		}
		e.source.removeEventListener('focus', this._onWindowOpenedDelayed);
		e.source.removeEventListener('open', this._onWindowOpened);
	};

	function prepareModalOpeningArgs(_args)
	{
		_args = _args || {};
		if (!_args.hasOwnProperty('activityEnterAnimation'))
				_args.activityEnterAnimation = Ti.App.Android.R.anim.push_up_in;
		if (!_args.hasOwnProperty('activityExitAnimation'))
			_args.activityExitAnimation = Ti.App.Android.R.anim.push_down_out;
		return _args;
	}

	WindowManager.prototype._openWindow = function(_win, _args) {
		var winManager = _win.winManager || this;

		_win.winId = this._winId++;
		this._managers[_win.winId] = winManager;
		winManager.openedWindows = [_win].concat(winManager.openedWindows);
		
		if (_win.cannotBeTopWindow !== true && winManager === this) {
			this.topWindow = _win;
		}
		if (isAndroid) {
			if (_win.modal === true) {
				_win.winOpeningArgs = prepareModalOpeningArgs(_win.winOpeningArgs);
			}
			if (!_win.onBack) {
				_win.onBack = _.partial(this._onBack, _win);
				_win.addEventListener('androidback', _win.onBack);
			}
		}
		var realArgs = _.size(_args) > 0 ? _args : _win.winOpeningArgs ? _win.winOpeningArgs : ((_win.handleOpen === true) ? {
			animated: false
		} : undefined);
		if (_win.winManager) {
			_win.winManager.openWindow(_win, realArgs);
		} else {
			_win.open(realArgs || this.defaultWinOpeningArgs);
		}
		if (_win.toDoAfterOpening) {
			_win.toDoAfterOpening();
		}
	};

	WindowManager.prototype.createAndOpenWindow = function(_constructor, _args, _dontCheckOpening) {
		var winManager = _args.winManager || this;
		_dontCheckOpening = _dontCheckOpening || this.shouldDelayOpening === false;
		if (_dontCheckOpening !== true) {
			if (winManager.handlingOpening === true) {
				error('Can\'t open window ' + _args.title);
				return;
			}
			winManager.handlingOpening = true;
		}
		
		var win = ak.ti.createFromConstructor(_constructor, _args);
		this.openWindow(win, {
			winManager: _args.winManager 
		}, _dontCheckOpening);
	};

	WindowManager.prototype.openWindow = function(_win, _args, _dontCheckOpening) {
		_args = _args || {};
		_dontCheckOpening = _dontCheckOpening || this.shouldDelayOpening === false;
		if (_args.winManager) {
			_win.winManager = _args.winManager;
			delete _args.winManager;
		}
		var winManager = _win.winManager || this;
		if (_.first(winManager.openedWindows) === _win) {
			if (_args.callback) {
				_args.callback(_win);
			}
			if (_win.toDoAfterOpening) {
				_win.toDoAfterOpening();
			}
			return;
		}

		if (_dontCheckOpening !== true) {
			winManager.handlingOpening = true;
			//focus is sent before window animation starts and so a double click could try to open 2 windows.
			//delaying the focus event prevent that
			_win.addEventListener('focus', this._onWindowOpenedDelayed); 
			_win.addEventListener('open', this._onWindowOpened);
		}
		
		if (_args.callback) {
			_args.callback(_win);
		}
		this._openWindow(_win, _args);
	};

	WindowManager.prototype.closeWindow = function(_win, _args, _callGC) {
		if (!_win || _win === null) return;
		_args = _args || {};
		var winManager = _win.winManager || this;
		_win.winManager = null;
		if (_win.manager) {
			if (_win.manager.canGCWindow) {
				_callGC = _win.manager.canGCWindow(_win);
			}
		}
		delete this._managers[_win.winId];
		winManager.openedWindows = _.difference(winManager.openedWindows, [_win]);
		if (_win.cannotBeTopWindow !== true && winManager === this) {
			this.topWindow = _.first(winManager.openedWindows);
		}

		var realArgs = _.size(_args) > 0 ? _args : _win.winOpeningArgs ? _win.winOpeningArgs : ((_win.handleClose === true) ? {
			animated: false
		} : this.defaultWinOpeningArgs);
		_win.close(_win.winOpeningArgs || this.defaultWinOpeningArgs);
		if (_callGC !== false && _win.GC) {
			debug('GC Window:' + _win.title);
			_win.manager = null;
			_win.onBack = null;
			_win.GC();
		}
	};

	WindowManager.prototype.windowSignalsOpened = function(_win) {
		var manager = this.getWindowManager(_win);
		if (manager) manager.handlingOpening = false;
	};
	WindowManager.prototype.getWindowManager = function(_win) {
		return this._managers[_win.winId];
	};

	return WindowManager;
})(this);