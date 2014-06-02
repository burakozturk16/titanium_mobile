ak.ti.constructors.createPullToRefresh = function(_args) {
	var rclass = _args.rclass || '';
	var needsReset = true;
	var unrotate = Ti.UI.create2DMatrix();
	var rotate = unrotate.rotate(180);
	var listView;
	var pullMessage = _args.pullMessage || tr('Pull down to refresh...');
	var releaseMessage = _args.releaseMessage || tr('Release to reload...');
	var loadingMessage = _args.loadingMessage || tr('Loading ...');
	info(stringify(_args));
	var self = new View({
		properties:_args,
		childTemplates:[{
			type:'Ti.UI.View',
			properties:{
				rclass:'AutoSize Horizontal',
				backgroundColor:'red'
			},
			childTemplates:[{
				bindId:'arrow',
				type:'Ti.UI.Label',
				properties:{
					rclass:_args.arrowClass || 'PullToRefreshArrow',
					backgroundColor:'green'
				},
			}, {
				bindId:'label',
				type:'Ti.UI.Label',
				properties:{
					rclass:_args.labelClass || 'PullToRefreshLabel',
				text:'Pull down to refresh...',
					backgroundColor:'blue'
			},
			}]
		}]
	});

	self.pullchangedListener = function(e) {
		if (e.active === false) {
			self.arrow.animate({
				transform: unrotate,
				duration: 180
			});
			self.label.text = pullMessage;
		} else {
			self.arrow.animate({
				transform: rotate,
				duration: 180
			});
			self.label.text = releaseMessage;
		}
	};

	self.goToLoading = function()
	{
		info('goToLoading ' + loadingMessage);
		self.arrow.hide();
		self.label.text = loadingMessage;
	};

	self.pullendListener = function(e) {
		info('pullendListener');
		listView.addEventListener('pull', self.reset);
		if (e.active === false) return;
		self.goToLoading();
		self.fireEvent('pulled');
	};
	self.reset = function(e) {
		listView.removeEventListener('pull', self.reset);
		self.label.text = pullMessage;
		self.arrow.transform = unrotate;
		self.arrow.show();
	};

	self.setListView = function(_listview) {
		listView = _listview;
		_listview.addEventListener('pullchanged', self.pullchangedListener);
		_listview.addEventListener('pullend', self.pullendListener);
	};

	return self;
};