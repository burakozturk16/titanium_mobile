ak.ti.constructors.createDataPlanView = function(_args) {
	var self = new ScrollView(_args);
	var data = {
		type:0,
		expiration:30,
		size:500,
		view:1
	}
	var datePicker;
	var selectedProps =  {}, unselectedProps = {};

	redux.fn.applyStyle(selectedProps, undefined, {rclass:'DPButtonSelected'});
	redux.fn.applyStyle(unselectedProps, undefined, {rclass:'DPButton'});
	function selectButton(_button) {
		_button.applyProperties(selectedProps);
	}
	function unselectButton(_button) {
		_button.applyProperties(unselectedProps);
	}

	function createButtonPicker(_buttonNames, _prop) {
		var startIndex = data[_prop];
		var view = new View({rclass:'AutoHeight HorizontalLayout', width:'200%'});
		var buttons = [];
		_(_buttonNames).each( function( value, i, list ) {
			var rclass = (i === startIndex)?'DPButtonSelected':'DPButton';
			var button = new Button({rclass:'FillWidth DPWidget ' + rclass, title:value, index:i});
			buttons.push(button);
			// view.add(button);
		});
		view.add(buttons);
		view.addEventListener('click', function(e){
			if (e.source.index !== undefined ) {
				var oldindex = data[_prop];
				if (oldindex === e.source.index) return;
				data[_prop] = e.source.index;
				selectButton(e.source);
				unselectButton(buttons[oldindex]);
			}
		});
		return view;
	}

	var expTF = new TextField({rclass:'DPWidget', rid:'dpExpTF', value:data.expiration});
	expTF.addEventListener('blur', function(e){info(stringify(e));data.expiration = parseInt(e.value)});

	var now = moment();
	var startTF = new DatePickerTextField({rclass:'DPWidget', rid:'dpStartTF', minDate:now,selectedDate:now});
	startTF.addEventListener('click', function() {
		startTF.showPicker(self.window);
	});
	var oldOffset = 0;
	var wasOffset = false;
	var screenScale = 1;
	startTF.addEventListener('willshow', function(e) {
		oldOffset = self.contentOffset.y;
		var value = e.windowRect.height - e.rect.height - (e.textfieldRect.y + e.textfieldRect.height);
		if (value < 0) {
			wasOffset = true;
			self.setContentOffset({x:0,y:oldOffset - value*screenScale}, {animated:true});
		}
		else wasOffset = false;
	});
	startTF.addEventListener('willhide', function(e) {
		if (wasOffset)  {
			self.setContentOffset({x:0,y:oldOffset}, {animated:true});
		}
	});
	var sizeTF = new TextField({rclass:'DPWidget', rid:'dpSizeTF', value:data.size});
	sizeTF.addEventListener('blur', function(e){info(stringify(e));data.size = parseInt(e.value)});

	var focusedTf;
	ak.ti.addEventListener('focus', function(e){
		focusedTf = e.source;
		startTF.hidePicker();
	}, [expTF, sizeTF]);

	self.addEventListener('touchstart', function(_evt){
		if (!_evt.source.keyboardType) {
			self.blur();
			self.hideKeyboard();
		}
		if (_evt.source !== startTF) startTF.hidePicker();
	});
	self.add([
		new Label({rclass:'DPInfoLabel', rid:'dpTypeDesc'}),
		createButtonPicker(['Button1', 'Button2'], 'type'),
		new Label({rclass:'DPInfoLabel', rid:'dpExpDesc'}),
		expTF,
		new Label({rclass:'DPInfoLabel', rid:'dpStartDesc'}),
		startTF,
		new Label({rclass:'DPInfoLabel', rid:'dpSizeDesc'}),
		sizeTF,
		createButtonPicker(['Button1', 'Button2'], 'view'),
		new View({height:15})]);
	return self;
};