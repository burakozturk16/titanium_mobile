ak.ti.constructors.createNotificationWindow = function(_args) {
	var trVisible = Ti.UI.create2DMatrix({
		ownFrameCoord: true
	}),
		trHidden = trVisible.translate(0, '-100%'),
		trHidden2 = trVisible.translate('100%', 0),
		animIn = {
			transform: trVisible,
			duration: 300
		}, animOut = {
			opacity: 0,
			transform: trHidden2,
			duration: 200
		},
		self = new Window(_args),
		hidden = true,
		currentMessageView = null,
		hideTimer = null;

	function resetHideTimer(_duration) {
		if (hideTimer !== null) {
			clearTimeout(hideTimer);
			hideTimer = null;
		}
		if (_duration > 0) hideTimer = setTimeout(hideMe, _duration);
	}

	function clearMessage() {
		self.removeAllChildren();
		currentMessageView = null;
		self.close({
			animated: false
		});
	}

	self.showMessage = function(_options) {
		var newMessageView;
		if (_.isString(_options.message)) {
			info('showMessage ' + _options.message);
			newMessageView = new Label({
				rclass: 'MsgView ' + 'MsgLevel' + _.capitalize(_options.level),
				text: _options.message
			});
		} else newMessageView = _options.view;
		if (!newMessageView) return;

		newMessageView.transform = trHidden;
		self.add(newMessageView);
		if (hidden === true) {
			hidden = false;
			showMe();
		}
		newMessageView.animate(animIn);
		hideCurrentMessage();
		currentMessageView = newMessageView;
		resetHideTimer((_options.timeout !== undefined) ? _options.timeout : $notificationDuration);
	};

	function hideCurrentMessage(_clear) {
		if (currentMessageView !== null) {
			var oldMessageView = currentMessageView;
			oldMessageView.animate(animOut, function() {
				self.remove(oldMessageView);
				if (_clear === true) clearMessage();
			});
		}
		else if (_clear === true) clearMessage();
	}

	function showMe() {
		info('showMe ');
		self.open({
			animated: false
		});

	}

	function hideMe() {
		info('hideMe ');
		hidden = true;
		hideCurrentMessage(true);
	}

	// self.addEventListener('touchstart',hideMe);
	self.addEventListener('close',function(){hidden =true;});

	return self;
};