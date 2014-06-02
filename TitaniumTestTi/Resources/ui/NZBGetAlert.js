ak.ti.constructors.createNZBGetAlert = function(_args, _constructorName) {
	_args = _args || {};
	_args.constructorName = 'NZBGetAlert';
	if (_args.buttonNames)
	{
		_.each(_args.buttonNames, function(value, key, list){
		
			if(_.isString(value))
			{
				_args.buttonNames[key] = app.utils.createNZBButton(value);
			}
		});
	}
	var self = new CustomAlert(_args);

	//END OF CLASS. NOW GC 
	var _parentGC = self.GC;
	self.GC = function(){
		if (_parentGC) _parentGC();
		self  = null;
	}
	return self;
}