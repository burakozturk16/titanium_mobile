var rootWin;
var isAndroid = Ti.Platform.osname == 'android';

function createAndroidNavGroup() {
 return {
 open: function(openWin, props) {
 	openWin.fullscreen = true;
 	openWin.backgroundColor = 'blue';
 	openWin.open();
 	}
 }
}

var navGroup = isAndroid ? createAndroidNavGroup() : Ti.UI.iPhone.createNavigationGroup({
   window: rootWin = Ti.UI.createWindow()
});
var button = Ti.UI.createButton({
    title: 'Open ListView Tests'
});
button.addEventListener('click', function() {
	openTestsWindow();
});
var label = Ti.UI.createLabel({
	text: "Return to this screen to check for memory leaks.\nEnsure garbage collection by doing 'Simulate Memory Warning'",
	left: 5,
	color: 'red',
	bottom: 20
});
if (isAndroid) {
	var navWin = Ti.UI.createWindow({ backgroundColor: 'white' });

	navWin.add(button);
	navWin.add(label);
} else {
	rootWin.add(button);
	rootWin.add(label);

	var navWin = Ti.UI.createWindow();
	navWin.add(navGroup);
}
navWin.open();


// ----------------------

var tests = {};

function openTestsWindow() {
	var win = Ti.UI.createWindow({ title: 'ListView Tests'});
	var listView = Ti.UI.createListView();

	var section = Ti.UI.createListSection({ headerTitle: 'Items'});
	section.setItems([
		{ properties: { title: 'Basic', itemId: 'basic', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
		{ properties: { title: 'Set Items', itemId: 'setItems', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
		{ properties: { title: 'Append Items', itemId: 'appendItems', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
		{ properties: { title: 'Insert Items', itemId: 'insertItems', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
		{ properties: { title: 'Replace Items', itemId: 'replaceItems', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
		{ properties: { title: 'Delete Items', itemId: 'deleteItems', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
		{ properties: { title: 'Get Item', itemId: 'getItem', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
		{ properties: { title: 'Scroll To Item', itemId: 'scrollToItem', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
	]);
	listView.appendSection(section);

	section = Ti.UI.createListSection({ headerTitle: 'Sections'});
	section.setItems([
		{ properties: { title: 'Basic With Sections', itemId: 'basicWithSections', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
		{ properties: { title: 'Append Section', itemId: 'appendSection', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
		{ properties: { title: 'Delete Section', itemId: 'deleteSection', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
		{ properties: { title: 'Insert Section', itemId: 'insertSection', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
		{ properties: { title: 'Replace Section', itemId: 'replaceSection', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
	]);
	listView.appendSection(section);

	section = Ti.UI.createListSection({ headerTitle: 'Other'});
	section.setItems([
		{ properties: { title: 'Headers & Footers', itemId: 'headersNfooters', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
		{ properties: { title: 'Accessory Types', itemId: 'accessoryTypes', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
		{ properties: { title: 'Background Color', itemId: 'backgroundColor', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
		{ properties: { title: 'Scroll Indicator Style (iOS only)', itemId: 'scrollIndicatorStyle', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
		{ properties: { title: 'Not Scroll On Status Tap (iOS only)', itemId: 'notScrollOnStatusTap', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
		{ properties: { title: 'Hide Scroll Indicator', itemId: 'hideScrollIndicator', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
		{ properties: { title: 'Grouped Style (iOS only)', itemId: 'groupedStyle', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
		{ properties: { title: 'Not Allows Selection (iOS only)', itemId: 'notAllowsSelection', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
		{ properties: { title: 'Animations (iOS only)', itemId: 'animations', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
		{ properties: { title: 'Row Height', itemId: 'rowHeight', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
	]);
	listView.appendSection(section);

	section = Ti.UI.createListSection({ headerTitle: 'Templates'});
	section.setItems([
		{ properties: { title: 'Default Item Template', itemId: 'defaultItemTemplate', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
		{ properties: { title: 'Builtin Templates', itemId: 'builtinTemplates', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
		{ properties: { title: 'Complex Templates', itemId: 'complexTemplates', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
		{ properties: { title: 'Variable Height', itemId: 'variableHeight', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
		{ properties: { title: 'Control Events', itemId: 'controlEvents', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
		{ properties: { title: 'Twitter Feed', itemId: 'twitterFeed', accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE} },
	]);
	listView.appendSection(section);

	listView.addEventListener('itemclick', function(e) {
		var f = tests[e.itemId];
		if (f) f();
	});
	win.add(listView);
    navGroup.open(win, { animated: true });
}

tests.basic = function() {
	var win = Ti.UI.createWindow({ title: 'Basic' });
	var section = Ti.UI.createListSection();
	var items = [];
	for (var i = 0; i < 1000; ++i) {
		items.push({ properties: { title: 'Item '+i} });
	}
	section.setItems(items);
	var listView = Ti.UI.createListView({
		sections: [section]
	});
	win.add(listView);
    navGroup.open(win, { animated: true });
}

tests.basicWithSections = function() {
	var win = Ti.UI.createWindow({ title: 'Basic With Sections' });
	var items = [];
	for (var i = 0; i < 20; ++i) {
		items.push({ properties: { title: 'Item '+i} });
	}
	var sections = [];
	var sectionCount = 20;
	for (var i = 0; i < sectionCount; ++i) {
		var section = Ti.UI.createListSection({ headerTitle: 'Section '+i});
		section.setItems(items);
		sections.push(section);
	}
	var listView = Ti.UI.createListView({
		sections: sections
	});
	win.add(listView);
    navGroup.open(win, { animated: true });
	win.addEventListener('open', function() {
		if (listView.sectionCount != sectionCount) {
			alert('Section count does not match. Expected '+sectionCount+', actual '+listView.sectionCount);
		}
	});
}

tests.setItems = function() {
	var win = Ti.UI.createWindow({ title: 'Set Items' });
	var section = Ti.UI.createListSection();
	section.setItems([{ properties: { title: 'Initial Placeholder' } }]);
	var listView = Ti.UI.createListView({
		sections: [section]
	});
	win.add(listView);
    navGroup.open(win, { animated: true });
	var loop = 0;
	var interval = setInterval(function() {
		var items = [];
		for (var i = 0; i < 50; ++i) {
			items.push({ properties: { title: 'Item '+i+' Loop '+loop} });
		}
		section.setItems(items);
		++loop;
	}, 1000);
	win.addEventListener('close', function() { clearInterval(interval); });
}

tests.appendItems = function() {
	var win = Ti.UI.createWindow({ title: 'Append Items' });
	var section = Ti.UI.createListSection();
	var listView = Ti.UI.createListView({
		sections: [section]
	});
	win.add(listView);
    navGroup.open(win, { animated: true });
	var i = 1;
	var interval = setInterval(function() {
		section.appendItems([{ properties: { title: 'Item '+i} }]);
		++i;
	}, 100);
	win.addEventListener('close', function() { clearInterval(interval); });
}

tests.insertItems = function() {
	var win = Ti.UI.createWindow({ title: 'Insert Items' });
	var section = Ti.UI.createListSection();
	var listView = Ti.UI.createListView({
		sections: [section]
	});
	win.add(listView);
    navGroup.open(win, { animated: true });
	var i = 1;
	var interval = setInterval(function() {
		section.insertItemsAt(0, [{ properties: { title: 'Item '+i} }]);
		++i;
	}, 1000);
	win.addEventListener('close', function() { clearInterval(interval); });
}

tests.replaceItems = function() {
	var win = Ti.UI.createWindow({ title: 'Replace Items' });
	var section = Ti.UI.createListSection();
	var items = [];
	for (var i = 0; i < 1000; ++i) {
		items.push({ properties: { title: 'Item '+i} });
	}
	section.setItems(items);
	var listView = Ti.UI.createListView({
		sections: [section]
	});
	win.add(listView);
    navGroup.open(win, { animated: true });
	var i = 0;
	var interval = setInterval(function() {
		section.replaceItemsAt(i, 2, [{ properties: { title: 'Item Replaced'} }]);
		++i;
	}, 1000);
	win.addEventListener('close', function() { clearInterval(interval); });
}

tests.deleteItems = function() {
	var win = Ti.UI.createWindow({ title: 'Delete Items' });
	var section = Ti.UI.createListSection();
	var items = [];
	for (var i = 0; i < 1000; ++i) {
		items.push({ properties: { title: 'Item '+i} });
	}
	section.setItems(items);
	var listView = Ti.UI.createListView({
		sections: [section]
	});
	win.add(listView);
    navGroup.open(win, { animated: true });
	var i = 0;
	var interval = setInterval(function() {
		section.deleteItemsAt(i, 1);
		++i;
	}, 1000);
	win.addEventListener('close', function() { clearInterval(interval); });
}

tests.getItem = function() {
	var win = Ti.UI.createWindow({ title: 'Get Item' });
	var section = Ti.UI.createListSection();
	var items = [];
	for (var i = 0; i < 10; ++i) {
		items.push({ properties: { title: 'Click on item '+i, itemId: "id"+i }, otherData: "Hello from "+i });
	}
	section.setItems(items);
	var listView = Ti.UI.createListView({
		sections: [section]
	});
	listView.addEventListener('itemclick', function(e) {
		alert(e.section.getItemAt(e.itemIndex).otherData);
	});
	win.add(listView);
    navGroup.open(win, { animated: true });
}

tests.scrollToItem = function() {
	var scrollSectionIndex = 10, scrollItemIndex = 6;
	var win = Ti.UI.createWindow({ title: 'Scroll To Item' });
	var items = [];
	for (var i = 0; i < 20; ++i) {
		items.push({ properties: { title: 'Item '+i} });
	}
	var sections = [];
	for (var i = 0; i < 20; ++i) {
		var section = Ti.UI.createListSection({ headerTitle: 'Section '+i});
		if (i == scrollSectionIndex) {
			items[scrollItemIndex].properties.title = 'Should scroll to this item';
		}
		section.setItems(items);
		sections.push(section);
	}
	var listView = Ti.UI.createListView({
		sections: sections
	});
	win.add(listView);
    navGroup.open(win, { animated: true });
	setTimeout(function() {
		if (isAndroid) {
			listView.scrollToItem(scrollSectionIndex, scrollItemIndex);
		} else {
			listView.scrollToItem(scrollSectionIndex, scrollItemIndex,  { position: Ti.UI.iPhone.ListViewScrollPosition.MIDDLE });
		}
	}, 1000);
}

tests.appendSection = function() {
	var win = Ti.UI.createWindow({ title: 'Append Section' });
	var section = Ti.UI.createListSection({ headerTitle: 'Section 0'});
	section.setItems([{ properties: { title: 'Item' } }]);
	var listView = Ti.UI.createListView({
		sections: [section]
	});
	win.add(listView);
    navGroup.open(win, { animated: true });
	var i = 1;
	var interval = setInterval(function() {
		// single
		var s1 = Ti.UI.createListSection({ headerTitle: 'Section '+i});
		s1.setItems([{ properties: { title: 'Item' }}]);
		listView.appendSection(s1);
		++i;
		// array
		var s2 = Ti.UI.createListSection({ headerTitle: 'Section '+i});
		s2.setItems([{ properties: { title: 'Item' }}]);
		listView.appendSection([s2]);
		++i;
	}, 2000);
	win.addEventListener('close', function() { clearInterval(interval); });
}

tests.deleteSection = function() {
	var win = Ti.UI.createWindow({ title: 'Delete Section' });
	var sections = [];
	for (var i = 0; i < 10; ++i) {
		var s = Ti.UI.createListSection({ headerTitle: 'Section '+i});
		s.setItems([{ properties: { title: 'Item' }}]);
		sections.push(s);
	}
	var listView = Ti.UI.createListView({
		sections: sections
	});
	win.add(listView);
    navGroup.open(win, { animated: true });
	var interval = setInterval(function() {
		listView.deleteSectionAt(1);
	}, 2000);
	win.addEventListener('close', function() { clearInterval(interval); });
}

tests.insertSection = function() {
	var win = Ti.UI.createWindow({ title: 'Insert Section' });
	var section = Ti.UI.createListSection({ headerTitle: 'Section '+i});
	section.setItems([{ properties: { title: 'Item' }}]);
	var listView = Ti.UI.createListView({
		sections: [section]
	});
	win.add(listView);
    navGroup.open(win, { animated: true });
	var i = 1;
	var interval = setInterval(function() {
		// single
		var s1 = Ti.UI.createListSection({ headerTitle: 'Section '+i});
		s1.setItems([{ properties: { title: 'Item' }}]);
		listView.insertSectionAt(0, s1);
		++i;
		// array
		var s2 = Ti.UI.createListSection({ headerTitle: 'Section '+i});
		s2.setItems([{ properties: { title: 'Item' }}]);
		listView.insertSectionAt(1, [s2]);
		++i;
	}, 2000);
	win.addEventListener('close', function() { clearInterval(interval); });
}

tests.replaceSection = function() {
	var win = Ti.UI.createWindow({ title: 'Replace Section' });
	var sections = [];
	for (var i = 0; i < 10; ++i) {
		var s = Ti.UI.createListSection({ headerTitle: 'Section '+i});
		s.setItems([{ properties: { title: 'Item' }}]);
		sections.push(s);
	}
	var listView = Ti.UI.createListView({
		sections: sections
	});
	win.add(listView);
    navGroup.open(win, { animated: true });
	var i = 0;
	var interval = setInterval(function() {
		var s = Ti.UI.createListSection({ headerTitle: 'Replaced Section '+i});
		s.setItems([{ properties: { title: 'Item' }}]);
		listView.replaceSectionAt(i, s);
		++i
	}, 2000);
	win.addEventListener('close', function() { clearInterval(interval); });
}

tests.headersNfooters = function() {
	var win = Ti.UI.createWindow({ title: 'Headers & Footers' });
	var items = [];
	for (var i = 0; i < 5; ++i) {
		items.push({ properties: { title: 'Item '+i} });
	}
	var sections = [];
	for (var i = 0; i < 10; ++i) {
		var section = Ti.UI.createListSection({ headerTitle: 'Section '+i+' Header', footerTitle: 'Section '+i+' Footer'});
		section.setItems(items);
		sections.push(section);
	}
	var listView = Ti.UI.createListView({
		sections: sections,
		headerTitle: 'Table Header',
		footerTitle: 'Table Footer',
	});
	win.add(listView);
    navGroup.open(win, { animated: true });
}

tests.accessoryTypes = function() {
	var win = Ti.UI.createWindow({ title: 'Accessory Types' });
	var section = Ti.UI.createListSection();
	var items = [];
	var accessoryTypes = {
		'None': Ti.UI.LIST_ACCESSORY_TYPE_NONE,
		'Checkmark': Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK,
		'Detail': Ti.UI.LIST_ACCESSORY_TYPE_DETAIL,
		'Disclosure': Ti.UI.LIST_ACCESSORY_TYPE_DISCLOSURE
	};
	for (var i = 0; i < 10; ++i) {
		for (var type in accessoryTypes) {
			items.push({ properties: { title: type, accessoryType: accessoryTypes[type] } });
		}
	}
	section.setItems(items);
	var listView = Ti.UI.createListView({
		sections: [section]
	});
	win.add(listView);
    navGroup.open(win, { animated: true });
}

tests.backgroundColor = function() {
	var win = Ti.UI.createWindow({ title: 'Background Color' });
	var section = Ti.UI.createListSection();
	var items = [];
	for (var i = 0; i < 100; ++i) {
		items.push({ properties: { title: 'Item '+i} });
	}
	section.setItems(items);
	var listView = Ti.UI.createListView({
		sections: [section],
		backgroundColor: 'yellow'
	});
	win.add(listView);
    navGroup.open(win, { animated: true });
}

tests.scrollIndicatorStyle = function() {
	if (isAndroid) {
		return;
	}
	var win = Ti.UI.createWindow({ title: 'Scroll Indicator Style' });
	var section = Ti.UI.createListSection();
	var items = [];
	for (var i = 0; i < 100; ++i) {
		items.push({ properties: { title: 'Item '+i, color: 'white' } });
	}
	section.setItems(items);
	var listView = Ti.UI.createListView({
		sections: [section],
		backgroundColor: 'black',
		scrollIndicatorStyle: Ti.UI.iPhone.ScrollIndicatorStyle.WHITE
	});
	win.add(listView);
    navGroup.open(win, { animated: true });
}

tests.notScrollOnStatusTap = function() {
	if (isAndroid) {
		return;
	}
	var win = Ti.UI.createWindow({ title: 'Not Scroll On Status Tap' });
	var section = Ti.UI.createListSection();
	var items = [];
	for (var i = 0; i < 100; ++i) {
		items.push({ properties: { title: 'Item '+i} });
	}
	section.setItems(items);
	var listView = Ti.UI.createListView({
		sections: [section],
		willScrollOnStatusTap: false
	});
	win.add(listView);
    navGroup.open(win, { animated: true });
}

tests.hideScrollIndicator = function() {
	var win = Ti.UI.createWindow({ title: 'Hide Scroll Indicator' });
	var section = Ti.UI.createListSection();
	var items = [];
	for (var i = 0; i < 100; ++i) {
		items.push({ properties: { title: 'Item '+i} });
	}
	section.setItems(items);
	var listView = Ti.UI.createListView({
		sections: [section],
		showVerticalScrollIndicator: false
	});
	win.add(listView);
    navGroup.open(win, { animated: true });
}

tests.groupedStyle = function() {
	if (isAndroid) {
		return;
	}
	var win = Ti.UI.createWindow({ title: 'Grouped Style' });
	var items = [];
	for (var i = 0; i < 5; ++i) {
		items.push({ properties: { title: 'Item '+i} });
	}
	var sections = [];
	for (var i = 0; i < 10; ++i) {
		var section = Ti.UI.createListSection({ headerTitle: 'Section '+i+' Header', footerTitle: 'Section '+i+' Footer'});
		section.setItems(items);
		sections.push(section);
	}
	var listView = Ti.UI.createListView({
		sections: sections,
		style: Ti.UI.iPhone.ListViewStyle.GROUPED
	});
	win.add(listView);
    navGroup.open(win, { animated: true });
}

tests.notAllowsSelection = function() {
	if (isAndroid) {
		return;
	}
	var win = Ti.UI.createWindow({ title: 'Not Allows Selection' });
	var section = Ti.UI.createListSection();
	var items = [];
	for (var i = 0; i < 20; ++i) {
		items.push({ properties: { title: 'Selection Not Allowed' } });
	}
	section.setItems(items);
	var listView = Ti.UI.createListView({
		sections: [section],
		allowsSelection: false
	});
	win.add(listView);
    navGroup.open(win, { animated: true });
}

tests.animations = function() {
//	if (isAndroid) {
//		return;
//	}
	var win = Ti.UI.createWindow({ title: 'Animations' });
	var section = Ti.UI.createListSection();
	section.setItems([{ properties: { title: 'Item' } }, { properties: { title: 'Item' } }]);
	var listView = Ti.UI.createListView({
		sections: [section]
	});
	win.add(listView);
    navGroup.open(win, { animated: true });
	var animationStyles = {
		'None': Ti.UI.iPhone.RowAnimationStyle.NONE,
		'Left': Ti.UI.iPhone.RowAnimationStyle.LEFT,
		'Right': Ti.UI.iPhone.RowAnimationStyle.RIGHT,
		'Top': Ti.UI.iPhone.RowAnimationStyle.TOP,
		'Bottom': Ti.UI.iPhone.RowAnimationStyle.BOTTOM,
		'Fade': Ti.UI.iPhone.RowAnimationStyle.FADE,
	};
	var animationsArray = ['None', 'Left', 'Right', 'Top', 'Bottom', 'Fade'];
	var i = 0;
	var interval = setInterval(function() {
		var type = animationsArray[i];
		if (isAndroid) {
			section.insertItemsAt(1, [{ properties: { title: type } }]);
		} else {
			section.insertItemsAt(1, [{ properties: { title: type } }], { animationStyle: animationStyles[type] });
		}
		i = (i + 1) % animationsArray.length;
	}, 1000);
	win.addEventListener('close', function() { clearInterval(interval); });
}

tests.rowHeight = function() {
	var win = Ti.UI.createWindow({ title: 'Row Height' });
	var section = Ti.UI.createListSection();
	var items = [];
	for (var i = 0; i < 50; ++i) {
		items.push({ properties: { title: 'Item '+i} });
	}
	section.setItems(items);
	var listView = Ti.UI.createListView({
		sections: [section],
		rowHeight: 80
	});
	win.add(listView);
    navGroup.open(win, { animated: true });
}

tests.defaultItemTemplate = function() {
	var win = Ti.UI.createWindow({ title: 'Default Item Template' });
	var section = Ti.UI.createListSection();
	var items = [];
	items.push({ properties: { title: 'Item with no subtitle', subtitle:'This subtitle will not show' }, template: Ti.UI.LIST_ITEM_TEMPLATE_DEFAULT });
	for (var i = 0; i < 20; ++i) {
		items.push({ properties: { title: 'Item '+i, subtitle:'Subtitle text for '+i } });
	}
	section.setItems(items);
	var listView = Ti.UI.createListView({
		sections: [section],
		defaultItemTemplate: Ti.UI.LIST_ITEM_TEMPLATE_SUBTITLE
	});
	win.add(listView);
    navGroup.open(win, { animated: true });
}

tests.builtinTemplates = function() {
	var win = Ti.UI.createWindow({ title: 'Builtin Templates' });
	var listView = Ti.UI.createListView();
	var section = Ti.UI.createListSection({ headerTitle: 'Default' });
	section.setItems([
		{ properties: { title: 'Title' }, template: Ti.UI.LIST_ITEM_TEMPLATE_DEFAULT },
		{ properties: { title: 'Text Color', color: 'red' }, template: Ti.UI.LIST_ITEM_TEMPLATE_DEFAULT },
		{ properties: { title: 'Font', font: { fontSize: 24, fontFamily: 'Chantelli Antiqua' } }, template: Ti.UI.LIST_ITEM_TEMPLATE_DEFAULT },
		{ properties: { title: 'Image', image: 'KS_nav_views.png' }, template: Ti.UI.LIST_ITEM_TEMPLATE_DEFAULT },
	]);
	listView.appendSection(section);

	section = Ti.UI.createListSection({ headerTitle: 'Subtitle' });
	section.setItems([
		{ properties: { title: 'Title', subtitle: 'Subtitle' }, template: Ti.UI.LIST_ITEM_TEMPLATE_SUBTITLE },
		{ properties: { title: 'Text Color', subtitle: 'Subtitle', color: 'red' }, template: Ti.UI.LIST_ITEM_TEMPLATE_SUBTITLE },
		{ properties: { title: 'Font', subtitle: 'Subtitle', font: { fontSize: 24, fontFamily: 'Chantelli Antiqua' } }, template: Ti.UI.LIST_ITEM_TEMPLATE_SUBTITLE },
		{ properties: { title: 'Image', subtitle: 'Subtitle', image: 'KS_nav_views.png' }, template: Ti.UI.LIST_ITEM_TEMPLATE_SUBTITLE },
	]);
	listView.appendSection(section);

	section = Ti.UI.createListSection({ headerTitle: 'Settings' });
	section.setItems([
		{ properties: { title: 'Title', subtitle: 'Subtitle' }, template: Ti.UI.LIST_ITEM_TEMPLATE_SETTINGS },
		{ properties: { title: 'Text Color', subtitle: 'Subtitle', color: 'red' }, template: Ti.UI.LIST_ITEM_TEMPLATE_SETTINGS },
		{ properties: { title: 'Font', subtitle: 'Subtitle', font: { fontSize: 24, fontFamily: 'Chantelli Antiqua' } }, template: Ti.UI.LIST_ITEM_TEMPLATE_SETTINGS },
		{ properties: { title: 'Image', subtitle: 'Subtitle', image: 'KS_nav_views.png' }, template: Ti.UI.LIST_ITEM_TEMPLATE_SETTINGS },
	]);
	listView.appendSection(section);

	section = Ti.UI.createListSection({ headerTitle: 'Contacts' });
	section.setItems([
		{ properties: { title: 'Title', subtitle: 'Subtitle' }, template: Ti.UI.LIST_ITEM_TEMPLATE_CONTACTS },
		{ properties: { title: 'Text Color', subtitle: 'Subtitle', color: 'red' }, template: Ti.UI.LIST_ITEM_TEMPLATE_CONTACTS },
		{ properties: { title: 'Font', subtitle: 'Subtitle', font: { fontSize: 24, fontFamily: 'Chantelli Antiqua' } }, template: Ti.UI.LIST_ITEM_TEMPLATE_CONTACTS },
		{ properties: { title: 'Image', subtitle: 'Subtitle', image: 'KS_nav_views.png' }, template: Ti.UI.LIST_ITEM_TEMPLATE_CONTACTS },
	]);
	listView.appendSection(section);
	
	win.add(listView);
    navGroup.open(win, { animated: true });
}

tests.complexTemplates = function() {
	var template1 = {
		properties: {
			accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DETAIL,
		},
		childTemplates: [
		{
			type: 'Ti.UI.Label',
			bindId: 'bindLabel',
			properties: {
				color: 'red',
				font: { fontFamily:'Arial', fontSize: 13, fontWeight:'bold' },
				left: 10,
				width: 200, height: 30
			},
		},
		{
			type: 'Ti.UI.ImageView',
			bindId: 'bindImage',
			properties: {
				image: 'KS_nav_ui.png',
				left: 220,
				width: 40, height: 40
			},
		}
		]
	};
	var template2 = {
		properties: {
		},
		childTemplates: [
		{
			type: 'Ti.UI.ImageView',
			bindId: 'bindImage',
			properties: {
				image: 'KS_nav_views.png',
				left: 10,
				width: 40, height: 40
			},
		},
		{
			type: 'Ti.UI.Label',
			bindId: 'bindLabel',
			properties: {
				left: 60,
				width: 200, height: 30
			},
		}
		]
	};
	
	var win = Ti.UI.createWindow({ title: 'Complex Templates' });
	var section = Ti.UI.createListSection();
	var listView = Ti.UI.createListView({
		sections: [section],
		templates: { 'template1': template1, 'template2': template2 },
		defaultItemTemplate: 'template2'
	});
	var items = [];
	for (var i = 0; i < 10; ++i) {
		items.push({ template: 'template1', bindLabel: { text: 'Template 1 Item '+i+' green', color: 'green' } });
		items.push({ template: 'template1', bindLabel: { text: 'Template 1 Item '+i } });
	}
	for (var i = 0; i < 10; ++i) {
		items.push({ bindLabel: { text: 'Template2 Item '+i+' yellow', color: 'yellow' } });
		items.push({ bindLabel: { text: 'Template2 Item '+i }, bindImage: { left: 0, image: 'KS_nav_ui.png' }, properties: { accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK } });
	}
	section.setItems(items);
	win.add(listView);
    navGroup.open(win, { animated: true });
}

tests.variableHeight = function() {
	var template1 = {
		properties: {
			accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DETAIL,
			height: 60
		},
		childTemplates: [
		{
			type: 'Ti.UI.Label',
			bindId: 'bindLabel',
			properties: {
				color: 'red',
				font: { fontFamily:'Arial', fontSize: 13, fontWeight:'bold' },
				left: 10,
				width: 200, height: 30
			},
		},
		{
			type: 'Ti.UI.ImageView',
			bindId: 'bindImage',
			properties: {
				image: 'KS_nav_ui.png',
				left: 220,
				width: 40, height: 40
			},
		}
		]
	};
	var template2 = {
		properties: {
			height: 50
		},
		childTemplates: [
		{
			type: 'Ti.UI.ImageView',
			bindId: 'bindImage',
			properties: {
				image: 'KS_nav_views.png',
				left: 10,
				width: 40, height: 40
			},
		},
		{
			type: 'Ti.UI.Label',
			bindId: 'bindLabel',
			properties: {
				left: 60,
				width: 200, height: 30
			},
		}
		]
	};
	
	var win = Ti.UI.createWindow({ title: 'Variable Height' });
	var section = Ti.UI.createListSection();
	var listView = Ti.UI.createListView({
		sections: [section],
		templates: { 'template1': template1, 'template2': template2 },
		defaultItemTemplate: 'template2'
	});
	var items = [];
	for (var i = 0; i < 10; ++i) {
		items.push({ template: 'template1', bindLabel: { text: 'Template 1 Item '+i+' green', color: 'green' } });
		items.push({ template: 'template1', bindLabel: { text: 'Template 1 Item '+i } });
	}
	for (var i = 0; i < 10; ++i) {
		items.push({ bindLabel: { text: 'Template2 Item '+i+' yellow', color: 'yellow' } });
		items.push({ bindLabel: { text: 'Template2 Item '+i }, bindImage: { left: 0, image: 'KS_nav_ui.png' }, properties: { accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK, height: 80 } });
	}
	section.setItems(items);
	win.add(listView);
    navGroup.open(win, { animated: true });
}

tests.controlEvents = function() {
	var template = {
		properties: {
		},
		childTemplates: [
		{
			type: 'Ti.UI.Label',
			bindId: 'bindLabel',
			properties: {
				color: 'red',
				font: { fontFamily:'Arial', fontSize: 13, fontWeight:'bold' },
				left: 10,
				width: 200, height: 30
			},
		},
		{
			type: 'Ti.UI.Switch',
			bindId: 'bindSwitch',
			properties: {
				value: false,
				right: 10,
				height: 40
			},
			events: {
				'change': function(e) {
					var data = e.section.getItemAt(e.itemIndex);
					data.bindLabel.text = 'Item is '+(e.value ? 'ON' : 'OFF');
					data.bindLabel.color = e.value ? 'green' : 'red';
					data.bindSwitch.value = e.value;
					e.section.replaceItemsAt(e.itemIndex, 1, [data]);						
				}
			}
		}
		]
	};
		
	var win = Ti.UI.createWindow({ title: 'Control Events' });
	var section = Ti.UI.createListSection();
	var listView = Ti.UI.createListView({
		sections: [section],
		templates: { 'template': template },
		defaultItemTemplate: 'template'
	});
	var items = [];
	for (var i = 0; i < 50; ++i) {
		items.push({ bindLabel: { text: 'Item is OFF', color: 'red' }, bindSwitch: { value: false } });
	}
	section.setItems(items);
	win.add(listView);
    navGroup.open(win, { animated: true });
}

tests.twitterFeed = function() {
	var template = {
		properties: {
		},
		childTemplates: [
		{
			type: 'Ti.UI.ImageView',
			bindId: 'avatar',
			properties: {
				left: 10,
				width: 40, height: 40
			},
		},
		{
			type: 'Ti.UI.Label',
			bindId: 'username',
			properties: {
				color: 'white',
				font: { fontFamily:'Arial', fontSize: 13, fontWeight:'bold' },
				left: 60, top: 5,
				width: 200, height: 10
			},
		},
		{
			type: 'Ti.UI.Label',
			bindId: 'message',
			properties: {
				color: 'white',
				font: { fontFamily:'Arial', fontSize: 11 },
				left: 60, top: 20,
				width: 200, bottom: 5
			},
		}
		]
	};
	
	var win = Ti.UI.createWindow({ title: 'Twitter Feed' });
	var section = Ti.UI.createListSection();
	var listView = Ti.UI.createListView({
		sections: [section],
		templates: { 'template': template },
		defaultItemTemplate: 'template',
		backgroundColor: 'black',
		rowHeight: 80
	});
	win.add(listView);
    navGroup.open(win, { animated: true });

	var url = "http://api.twitter.com/1/lists/statuses.json?slug=mobile-award-winners&owner_screen_name=appcelerator&include_rts=1&per_page=5";
	var loader = Titanium.Network.createHTTPClient();
	var page = 1;
	loader.onload = function() {
		var tweets = eval('('+this.responseText+')');
		var items = [];
		for (var i = 0; i < tweets.length; ++i) {
			items.push({ username: { text: tweets[i].user.screen_name }, message: { text: tweets[i].text }, avatar: { image: tweets[i].user.profile_image_url } });
			if (items.length == 4) {
				section.appendItems(items);
				items = [];
			}
		}
		section.appendItems(items);
		++page;
		if (page < 400) {
			setTimeout(function(){
				loader.open("GET", url+'&page='+page);
				loader.send();							
			}, 10);
		}
	};
	loader.open("GET", url+'&page='+page);
	loader.send();
	win.addEventListener('close', function() { loader.abort(); });
	
}