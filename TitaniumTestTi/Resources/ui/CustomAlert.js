ak.ti.constructors.createCustomAlert = function(_args) {
	_args = _args || {};
	var constructorName = _args.constructorName || 'CustomAlert';
	var rclass = constructorName + 'View';
	var buttonNames = _args.buttonNames; delete _args.buttonNames;
	var cancel = _args.cancel; delete _args.cancel;
	var title = _args.title; delete _args.title;
	var message = _args.message; delete _args.message;
	var persistent = !!_args.persistent; delete _args.persistent;
	var self = new AnimatedWindow(_args);

	if (_args.viewClass) rclass += ' ' + _args.viewClass;
	self.alertView = new View({rclass:rclass});
	self.customView = _args.customView; delete _args.customView;
	
	self.titleView = new Label({rclass:constructorName + 'Title', html:title});
	self.centerHolder = new View({rclass:constructorName + 'CenterHolder'});
	self.messageView = self.customView || new Label({rclass:constructorName + 'Message', html:message});
	self.messageView.alert = self;
	var buttons = [];
	var buttonHolder;
	if (buttonNames) {
		buttonHolder = new View({rclass:constructorName + 'BHolder'});
		_(buttonNames).each( function( value, i, list ) {
			var holder  = new View({rclass:'FillWidth SizeHeight'});
			var button = new Label(_.isObject(value)?value:{rclass:constructorName + 'Button', title:value.toUpperCase()});
			button.index = i;
			buttons.push(button);
			holder.add(button);
			buttonHolder.add(holder);
		})

		app.onDebounce(buttonHolder, 'click', function(_evt) {
			if (_evt.source.index === undefined) return;
			var index = _evt.source.index;
			self.fireEvent('click', {
				index:index,
				persistent:persistent,
				cancel:(index === cancel),
				source:_evt.source
			});
			if(!persistent) self.hideMe();
		});
	}
	else {
		self.addEventListener('touchstart', function(e){
			if (!self.alertView.containsView(e.source))
				self.hideMe();
		});
	}

	var untrans = Ti.UI.create2DMatrix();
	var trans = untrans.scale(1.3, 1.3);
	var trans2 = untrans.scale(0.7, 0.7);
	self.beforeShow = function(){
		self.alertView.transform = trans;
	}
	self.onShow = function(){
		self.alertView.animate({transform:untrans, duration:300});
	}
	self.onHide = function(){
		self.alertView.animate({transform:trans2, duration:300});
	};
	self.buttons = function(){return buttons}

	self.hideButton = function(_index) {
		if (buttonHolder) {
			var button = buttons[_index];
			button.parent.hide();
		}
	}

	self.hideButton = function(_index) {
		if (buttonHolder) {
			var button = buttons[_index];
			buttonHolder.remove(button.parent);
			buttons.splice(_index, 1);
		}
	}

	self.alertView.add(self.titleView);
	self.centerHolder.add(self.messageView);
	self.alertView.add(self.centerHolder);
	if (buttonHolder) self.alertView.add(buttonHolder);
	self.add(self.alertView);

	//END OF CLASS. NOW GC 
	var _parentGC = self.GC;
	self.GC = function(){
		if (_parentGC) _parentGC();
		self  = null;
		buttons = null;
	}
	return self;
};