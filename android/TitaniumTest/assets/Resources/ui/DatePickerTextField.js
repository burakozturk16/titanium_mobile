ak.ti.constructors.createDatePickerTextField = function(_args) {
	function validateDate(date) {
		return date;
	};
	var minDate = validateDate(_args.minDate);
	delete _args.minDate;
	var maxDate = validateDate(_args.maxDate);
	delete _args.maxDate;
	var selectedDate = validateDate(_args.selectedDate);
	delete _args.selectedDate;
	var format = _args.format || 'L';
	delete _args.format;

	var untrans = Ti.UI.create2DMatrix();
	var trans = untrans.translate(0, '100%');


	_args.title = selectedDate.format(format);
	var self = new Button(_args);
	var cancel = new Label({rclass:'DatePickerButton', rid:'datePickerCancel'});
	var done = new Label({rclass:'DatePickerButton', rid:'datePickerDone'});

	var pickerView = new View({rclass:'DatePicker',transform: trans});
	var toolbar = new View({rclass:'DatePickerToolbar'});
	toolbar.add([cancel, {width:Ti.UI.FILL}, done]);
	var picker = Ti.UI.createPicker({
		type: Ti.UI.PICKER_TYPE_DATE
	});
	if (minDate) picker.minDate = minDate.toDate();
	if (maxDate) picker.maxDate = maxDate.toDate();
	if (selectedDate) {
		picker.value = selectedDate.toDate();
		self.date = selectedDate;
	}
	picker.selectionIndicator = true;
	pickerView.add([toolbar,picker]);

	
	pickerView.transform = trans;
	var slideIn = {
		transform: untrans,
		// height: Ti.UI.SIZE,
		duration: 300
	};
	var slideOut = {
		transform: trans,
		// height: 0,
		duration: 300
	};
	picker.addEventListener('change', function(e) {
		self.title = moment(picker.value).format(format);
	});

	var window = null;
	var visible = false;

	function onFirstAnimateLayout(e){
		var selfrect = self.absoluteRect;
		var pickerViewrect = pickerView.absoluteRect;
		var args = {
			textfieldRect:selfrect,
			rect:pickerViewrect,
			windowRect:window.absoluteRect
		}
		self.fireEvent('willshow',args);
		pickerView.removeEventListener('postlayout', onFirstAnimateLayout);
	}
	self.showPicker = function(_window) {
		if (visible)return;
		visible = true;
		pickerView.addEventListener('postlayout', onFirstAnimateLayout);
		info('showPicker')
		window = _window;
		window.add(pickerView);
		if (self.date) picker.value = self.date.toDate();
		pickerView.animate(slideIn); //a trick because of a bug on that sdk version on ios

	}
	self.hidePicker = function(_cancel) {
		if (!visible)return;
		visible = false;
		info('hidePicker')
		self.fireEvent('willhide');
		_cancel = _cancel === true;
		if (!_cancel) {
			self.date = moment(picker.value);
			self.title = self.date.format(format);
		}
		else self.title = selectedDate.format(format);
		pickerView.animate(slideOut, function() {
			window.remove(pickerView);
		});
	}
	cancel.addEventListener('click', function(){
		self.hidePicker(true);
	});
	done.addEventListener('click', function() {
		self.hidePicker();
	});
	return self;
};