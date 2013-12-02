ak.ti.constructors.createDataPlanAlert = function(_args) {
	_args = _args || {};
	var dataPlanView;
	var welcomeFirst = _args.welcomeFirst || false; delete _args.welcomeFirst;
	if (welcomeFirst) {
		info('welcomeFirst');
		_args.rid = 'welcomeAlert';
	}
	else {
		_args.rid = 'cellularPlanAlert';
		// _args.viewClass = 'DataPlanAlertView';
		_args.customView = dataPlanView = new DataPlanView();
	}
	var self = new DAlert(_args);
	if (!welcomeFirst) {
		dataPlanView.window = self;
		// self.alertView.top = 20;
	}
	self.addEventListener('click', function(e) {
		if (!e.hasOwnProperty('index')) return;
		if (e.cancel === false) {
			if (welcomeFirst) {
				welcomeFirst = false;
				var transArgs = {duration:200};
				// redux.fn.applyStyle(transArgs, undefined, {rclass:'DataPlanAlertView'});
				redux.fn.applyStyle(self, undefined, {rid:'cellularPlanAlert'}, undefined, true);
				// self.centerHolder.removeAllChildren();
				dataPlanView = new DataPlanView({window:self, opacity:0, height:0});
				self.centerHolder.add(dataPlanView);
				self.messageView.opacity = 0;
				self.titleView.html = self.title;
				_(self.buttons()).each( function( value, i, list ) {
					value.title = self.buttonNames[i].toUpperCase();
				});

				// self.alertView.animate({top:20, duration:200});
				dataPlanView.animate({opacity:1, height:Ti.UI.SIZE, duration:200});
			}
			else info("needs saving settings");
		}
		else self.hideMe();
	});
	return self;
};