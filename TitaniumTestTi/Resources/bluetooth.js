function create(_context) {
    var module = {};

    function itemFromDevice(_device) {
        return {
            label: {
                text: _device.name + ' : ' + _device.identifier
            },
            button: {
                title: _device.paired ? 'Unpair' : 'Pair'
            },
            device: _device
        };
    }

    function fillSection(_section, _devices, _noItemText) {
        if (!_devices || _devices.length === 0) {
            _section.items = [{
                template: 'noitem',
                label: {
                    text: _noItemText
                }
            }];
        }
        sdebug('fillSection', _devices, _noItemText);
        _section.items = _.reduce(_devices, function(memo, value, key, list) {
            memo.push(itemFromDevice(value));
            return memo;
        }, []);
    }
    var Bluetooth = app.modules.bluetooth = require('akylas.bluetooth');

    module.exs = function(_args) {
        var win = createWin(_args);
        var listview = createListView();
        listview.sections = [{
            items: [{
                properties: {
                    title: 'Bluetooth'
                },
                callback: btEx1
            }, {
                properties: {
                    title: 'Bluetooth LTE'
                },
                callback: btEx2
            }]
        }];
        win.add(listview);
        openWin(win);
    }

    function btEx1(_args) {
        if (!Bluetooth.supported) {
            alert('bluetooth not supported');
            return;
        }
        var btDevice = null;
        var pairedDevices = [];
        var availableDevices = [];
        var availableSection = new ListSection({
            headerView: {
                type: 'Ti.UI.Label',
                bindId: 'listHeader',
                properties: {
                    color: 'green',
                    padding: {
                        left: 20
                    },
                    width: 'FILL',
                    height: 20,
                    font: {
                        size: 14,
                        weight: 'medium'
                    },
                    text: 'Available devices'
                },
                childTemplates: [{
                    type: 'Ti.UI.ActivityIndicator',
                    bindId: 'indicator',
                    properties: {
                        visible: false,
                        right: 10
                    }
                }]
            }
        });

        var connectedSection = new ListSection({
            headerView: {
                type: 'Ti.UI.Label',
                bindId: 'listHeader',
                properties: {
                    color: 'blue',
                    padding: {
                        left: 20
                    },
                    width: 'FILL',
                    height: 20,
                    font: {
                        size: 14,
                        weight: 'medium'
                    },
                    text: 'Paired devices'
                },
                childTemplates: [{
                    type: 'Ti.UI.ActivityIndicator',
                    bindId: 'indicator',
                    properties: {
                        visible: false,
                        right: 10
                    }
                }]
            }
        });

        var win = createWin({
                backgroundColor: 'white',
                layout: 'vertical',
                childTemplates: [{
                    type: 'Ti.UI.Label',
                    bindId: 'btStatus',
                    properties: {
                        color: 'white',
                        padding: {
                            left: 20
                        },
                        backgroundColor: '#333',
                        width: 'FILL',
                        height: 50,
                        font: {
                            size: 20,
                            // weight: 'bold'
                        }
                    },
                    childTemplates: [{
                        type: 'Ti.UI.Switch',
                        bindId: 'switch',
                        properties: {
                            right: 10
                        },
                        events: {
                            'change': function(e) {
                                Bluetooth.enabled = e.value;
                            }
                        }
                    }]

                }, {
                    type: 'Ti.UI.View',
                    properties: {
                        top: 10,
                        layout: 'horizontal',
                        height: 35
                    },
                    childTemplates: [{
                        type: 'Ti.UI.Button',
                        bindId: 'discoverButton',
                        properties: {
                            left: 10,
                            color: 'black',
                            title: 'Discover',
                        },
                        events: {
                            'click': function(_event) {
                                availableSection.indicator.visible = true;
                                availableSection.items = [];
                                availableDevices = [];
                                Bluetooth.discover(function(_devices) {
                                    availableSection.indicator.visible = false;
                                    if (availableDevices.length === 0) {
                                        availableSection.items = [{
                                            template: 'noitem',
                                            label: {
                                                text: 'No nearby Bluetooth devices were found.'
                                            }
                                        }];
                                    }
                                    // fillSection(availableSection, availableDevices, 'No nearby Bluetooth devices were found.');
                                });
                            }
                        }
                    }, {
                        type: 'Ti.UI.View'
                    }, {
                        type: 'Ti.UI.Button',
                        bindId: 'pairButton',
                        properties: {
                            right: 10,
                            title: 'Pair Device',
                        },
                        events: {
                            'click': function(_event) {
                                // Ti.Bluetooth.pairDevice({
                                //  success: function(e) {
                                //      bluetoothDevice.connect();
                                //      // win.pairButton.visible = false;
                                //  },
                                //  error: function(e) {
                                //      alert(e.error);
                                //  }
                                // });
                            }
                        }
                    }]
                }, {
                    type: 'Ti.UI.ListView',
                    bindId: 'listView',
                    properties: {
                        height: 'FILL',
                        defaultItemTemplate: 'device',
                        templates: {
                            'device': {
                                properties: {
                                    height: 45,
                                    layout: 'horizontal'
                                },
                                childTemplates: [{
                                    type: 'Ti.UI.Label',
                                    bindId: 'label',
                                    font: {
                                        size: 15
                                    },
                                    left: 25,
                                    width: 'FILL',
                                    color: 'black'
                                }, {
                                    type: 'Ti.UI.Button',
                                    color: 'black',
                                    bindId: 'button'
                                }]
                            },
                            'noitem': {
                                properties: {
                                    height: 45
                                },
                                childTemplates: [{
                                    type: 'Ti.UI.Label',
                                    bindId: 'label',
                                    font: {
                                        size: 15
                                    },
                                    textAlign: 'center',
                                    width: 'FILL',
                                    color: 'black'
                                }]
                            }

                        },
                        sections: [availableSection, connectedSection]
                    },
                    events: {
                        itemclick: app.debounce(function(e) {
                            if (e.hasOwnProperty('item')) {
                                var device = e.item.device;
                                sdebug('itemclick', device);
                                if (e.bindId === 'button') {
                                    if (device.paired) {
                                        Bluetooth.unpairDevice(device);
                                    } else {
                                        Bluetooth.pairDevice(device);
                                    }
                                } else {
                                    if (btDevice !== null && btDevice.address === device.address) {
                                        //this is our connected device
                                        if (btDevice.connected) {
                                            btDevice.disconnect();
                                        } else {
                                            btDevice.connect();
                                        }
                                    } else {
                                        if (btDevice !== null) {
                                            btDevice.disconnect();
                                        }
                                        if (device.paired) {
                                            btDevice = Bluetooth.createDevice(device)
                                            btDevice.on('read', function(e) {
                                                sdebug('received', e.data.text);
                                            }).on('connected', function(e) {
                                                btDevice.send('test');
                                            });
                                            btDevice.connect();
                                        }
                                    }
                                }
                            }
                        })
                    }
                }]
            },
            _args);
        var enabled = Bluetooth.enabled;
        win.applyProperties({
            btStatus: {
                text: enabled ? 'On' : 'Off'
            },
            switch: {
                value: enabled
            },
            listHeader: {
                visible: enabled
            }
        });
        Bluetooth.on('change', function(e) {
            sdebug('bluetooth state change', e.enabled);
            win.applyProperties({
                btStatus: {
                    text: e.enabled ? 'On' : 'Off'
                },
                switch: {
                    value: e.enabled
                },
                listHeader: {
                    visible: e.enabled
                }
            });
        }).on('pairing', function(e) {
            var paired = e.paired;
            var device = e.device;
            var index = _.findIndex(pairedDevices, {
                address: e.device.address
            });
            if (paired) {
                if (index < 0) {
                    connectedSection.appendItems([itemFromDevice(device)]);
                }
            } else {
                if (index >= 0) {
                    connectedSection.deleteItemsAt(index, 1);
                }
            }
            index = _.findIndex(availableDevices, {
                address: e.device.address
            });
            sdebug('pairing', device, index);
            if (index >= 0) {
                availableSection.updateItemAt(index, {
                    button: {
                        text: paired ? 'Unpair' : 'Pair'
                    },
                    device: e.device
                });
            }
        }).on('found', function(e) {
            if (e.discovering) {
                var device = e.device;
                var index = _.findIndex(availableDevices, {
                    address: e.device.address
                });
                if (index < 0) {
                    availableDevices.push(device);
                    availableSection.appendItems([itemFromDevice(device)]);
                }

            }
        });
        if (enabled) {
            win.discoverButton.fireEvent('click');
            pairedDevices = Bluetooth.connectedDevices || [];
            fillSection(connectedSection, pairedDevices, 'No Connected device');
        } else {
            Bluetooth.enabled = true;
        }
        openWin(win);
    }

    function btEx2(_args) {
        if (!Bluetooth.supported) {
            alert('bluetooth not supported');
            return;
        }
        var currentIdentifier = Ti.App.Properties.getString('bluetooth.identifier');
        sdebug('bluetooth.identifier', currentIdentifier);
        var btDevice = currentIdentifier? Bluetooth.createBLEDevice({'identifier':currentIdentifier}).on('read', function(e) {
                                            var text = e.data.hexString;
                                            sdebug('received', e.length, text);
                                        }):null;
        var pairedDevices = [];
        var availableDevices = [];
        var availableSection = new ListSection({
            headerView: {
                type: 'Ti.UI.Label',
                bindId: 'listHeader',
                properties: {
                    color: 'green',
                    padding: {
                        left: 20
                    },
                    width: 'FILL',
                    height: 20,
                    font: {
                        size: 14,
                        weight: 'medium'
                    },
                    text: 'Available devices',
                },
                childTemplates: [{
                    type: 'Ti.UI.ActivityIndicator',
                    bindId: 'indicator',
                    properties: {
                        visible: false,
                        right: 10
                    }
                }]
            }
        });

        var connectedSection = new ListSection({
            headerView: {
                type: 'Ti.UI.Label',
                bindId: 'listHeader',
                properties: {
                    color: 'blue',
                    padding: {
                        left: 20
                    },
                    width: 'FILL',
                    height: 20,
                    font: {
                        size: 14,
                        weight: 'medium'
                    },
                    text: 'Paired devices'
                },
                childTemplates: [{
                    type: 'Ti.UI.ActivityIndicator',
                    bindId: 'indicator',
                    properties: {
                        visible: false,
                        right: 10
                    }
                }]
            }
        });

        var win = createWin({
                backgroundColor: 'white',
                layout: 'vertical',
                childTemplates: [{
                    type: 'Ti.UI.Label',
                    bindId: 'btStatus',
                    properties: {
                        color: 'white',
                        padding: {
                            left: 20
                        },
                        backgroundColor: '#333',
                        width: 'FILL',
                        height: 50,
                        font: {
                            size: 20,
                            // weight: 'bold'
                        }
                    },
                    childTemplates: [{
                        type: 'Ti.UI.Switch',
                        bindId: 'switch',
                        properties: {
                            right: 10
                        },
                        events: {
                            'change': function(e) {
                                Bluetooth.enabled = e.value;
                            }
                        }
                    }]

                }, {
                    type: 'Ti.UI.View',
                    properties: {
                        top: 10,
                        layout: 'horizontal',
                        height: 35
                    },
                    childTemplates: [{
                        type: 'Ti.UI.Button',
                        bindId: 'discoverButton',
                        properties: {
                            left: 10,
                            color: 'black',
                            title: 'Discover',
                        },
                        events: {
                            'click': function(_event) {
                                availableSection.indicator.visible = true;
                                availableSection.items = [];
                                availableDevices = [];
                                Bluetooth.discoverBLE(function(_devices) {
                                    availableSection.indicator.visible = false;
                                    if (availableDevices.length === 0) {
                                        availableSection.items = [{
                                            template: 'noitem',
                                            label: {
                                                text: 'No nearby Bluetooth devices were found.'
                                            }
                                        }];
                                    }
                                    // fillSection(availableSection, availableDevices, 'No nearby Bluetooth devices were found.');
                                });
                            }
                        }
                    }, {
                        type: 'Ti.UI.View'
                    }, {
                        type: 'Ti.UI.Button',
                        bindId: 'pairButton',
                        properties: {
                            right: 10,
                            title: 'Pair Device',
                        },
                        events: {
                            'click': function(_event) {
                                Ti.Bluetooth.pairDevice({
                                    success: function(e) {
                                        bluetoothDevice.connect();
                                        // win.pairButton.visible = false;
                                    },
                                    error: function(e) {
                                        alert(e.error);
                                    }
                                });
                            }
                        }
                    }]
                }, {
                    type: 'Ti.UI.ListView',
                    bindId: 'listView',
                    properties: {
                        height: 'FILL',
                        defaultItemTemplate: 'device',
                        templates: {
                            'device': {
                                properties: {
                                    height: 45,
                                    layout: 'horizontal'
                                },
                                childTemplates: [{
                                    type: 'Ti.UI.Label',
                                    bindId: 'label',
                                    properties:{
                                        font: {
                                            size: 15
                                        },
                                        left: 25,
                                        width: 'FILL',
                                        color: 'black'
                                    }
                                    
                                }, {
                                    type: 'Ti.UI.Button',
                                    properties:{
                                        color: 'black',
                                    },
                                    bindId: 'button'
                                }]
                            },
                            'noitem': {
                                properties: {
                                    height: 45
                                },
                                childTemplates: [{
                                    type: 'Ti.UI.Label',
                                    bindId: 'label',
                                    properties:{
                                        font: {
                                            size: 15
                                        },
                                        textAlign: 'center',
                                        width: 'FILL',
                                        color: 'black'
                                    }
                                }]
                            }

                        },
                        sections: [availableSection, connectedSection]
                    },
                    events: {
                        itemclick: app.debounce(function(e) {
                            if (e.hasOwnProperty('item')) {
                                var device = e.item.device;
                                sdebug('itemclick', device);
                                if (e.bindId === 'button') {
                                    if (device.paired) {
                                        Bluetooth.unpairDevice(device);
                                    } else {
                                        Bluetooth.pairDevice(device);
                                    }
                                } else {
                                    if (btDevice !== null && btDevice.identifier === device.identifier) {
                                        //this is our connected device
                                        if (btDevice.connected) {
                                            btDevice.disconnect();
                                        } else {
                                            btDevice.connect();
                                        }
                                    } else {
                                        if (btDevice !== null) {
                                            btDevice.disconnect();
                                        }
                                        // if (device.paired) {
                                        Ti.App.Properties.setString('bluetooth.identifier', device.identifier);
                                        btDevice = Bluetooth.createBLEDevice(device).on('read', function(e) {
                                            var text = e.data.hexString;
                                            sdebug('received', e.length, text);
                                        }).on('connected', function(e) {
                                            btDevice.send('test');
                                        });
                                        btDevice.connect();
                                        // }
                                    }
                                }
                            }
                        })
                    }
                }]
            },
            _args);
        var enabled = Bluetooth.enabled;
        sdebug('bluetooth state', enabled);
        win.applyProperties({
            btStatus: {
                text: enabled ? 'On' : 'Off'
            },
            switch: {
                value: enabled
            },
            listHeader: {
                visible: enabled
            }
        });
        Bluetooth.on('change', function(e) {
            sdebug('bluetooth state change', e.enabled);
            win.applyProperties({
                btStatus: {
                    text: e.enabled ? 'On' : 'Off'
                },
                switch: {
                    value: e.enabled
                },
                listHeader: {
                    visible: e.enabled
                }
            });
            pairedDevices = Bluetooth.pairedDevices || [];
            fillSection(connectedSection, pairedDevices, 'No Connected device');
        }).on('pairing', function(e) {
            var paired = e.paired;
            var device = e.device;
            var index = _.findIndex(pairedDevices, {
                address: e.device.address
            });
            if (paired) {
                if (index < 0) {
                    connectedSection.appendItems([itemFromDevice(device)]);
                }
            } else {
                if (index >= 0) {
                    connectedSection.deleteItemsAt(index, 1);
                }
            }
            index = _.findIndex(availableDevices, {
                address: e.device.address
            });
            sdebug('pairing', device, index);
            if (index >= 0) {
                availableSection.updateItemAt(index, {
                    button: {
                        text: paired ? 'Unpair' : 'Pair'
                    },
                    device: e.device
                });
            }
        }).on('found', function(e) {
            sdebug('found', e.device);
            if (e.discovering) {
                var device = e.device;
                var index = _.findIndex(availableDevices, {
                    address: e.device.address
                });
                if (index < 0) {
                    availableDevices.push(device);
                    availableSection.appendItems([itemFromDevice(device)]);
                }

            }
        });
        win.on('close', function() {
            if (btDevice !== null) {
                btDevice.disconnect();
                btDevice = null;
            }
        })
        if (enabled) {
            if (btDevice !== null) {
                btDevice.connect();
            } else {
                win.discoverButton.fireEvent('click');
            }
        }
        pairedDevices = Bluetooth.pairedDevices || [];
        fillSection(connectedSection, pairedDevices, 'No Connected device');
        openWin(win);
    }
    return module;

}

exports.load = function(_context) {
    return create(_context);
};