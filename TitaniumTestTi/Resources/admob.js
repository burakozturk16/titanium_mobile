var Admob = app.modules.admob = require('akylas.admob');

function admobExs(_args) {
    var win = createWin(_args);
    var listview = createListView();
    listview.sections = [{
        items: [{
            properties: {
                title: 'AdmobView'

            },
            callback: admobEx
        }, {
            properties: {
                title: 'Interstitial'
            },
            callback: interstitialEx
        }, {
            properties: {
                title: 'ListView'
            },
            callback: admobListViewEx
        }]
    }];
    win.add(listview);
    openWin(win);
}

function admobEx(_args) {
    var win = createWin(_args);

    function onEvent(e) {
        sdebug('admobEx', e.type);
    }

    win.add({
        type: 'AkylasAdmob.View',
        properties: {
            bottom: 0,
            adUnitId: 'ca-app-pub-4951262838901192/2865489868', // You can get your own at http: //www.admob.com/
            adBackgroundColor: 'black',
            backgroundColor: 'blue',
            // You can get your device's id for testDevices by looking in the console log after the app launched
            testDevices: ["EAC5B55CEE507B7D477F9C2BB2EBF375", Admob.SIMULATOR_ID],
            birthday: new Date(1985, 10, 1, 12, 1, 1),
            gender: 'male',
            keywords: ''
        },
        events: {
            'load': onEvent,
            'error': onEvent,
            'open': onEvent,
            'close': onEvent,
            'leftapp': onEvent
        }

    });
    openWin(win);
}
var interstitial = Admob.createInterstitial({
    adUnitId: 'ca-app-pub-3147314304971005/7624605172', // You can get your own at http: //www.admob.com/
    adBackgroundColor: 'black',
    backgroundColor: 'blue',
    // You can get your device's id for testDevices by looking in the console log after the app launched
    testDevices: ["EAC5B55CEE507B7D477F9C2BB2EBF375"],
    birthday: new Date(1985, 10, 1, 12, 1, 1),
    gender: 'male',
    //        showOnLoad:true
});
interstitial.addEventListener('load', function(e) {
    sdebug('interstitial', e.type);
    e.source.show();
});
interstitial.addEventListener('error', function(e) {
    sdebug('interstitial', e.type);
});
interstitial.addEventListener('close', function(e) {
    sdebug('interstitial', e.type);
});

function interstitialEx(_args) {
    interstitial.load();
}

function admobListViewEx(_args) {
    var win = createWin(_args);

    openWin(win);
}

function admobListViewEx(_args) {
    var win = createWin();
    var items = [];
    for (var i = 0; i < 300; i++) {
        if (i % 50 === 0) {
            items.push({
                template: 'admob'
            });
        } else {
            items.push({
                title: "this is a test title " + i
            });
        }

    }
    win.add({
        type: 'Ti.UI.ListView',
        properties: {
            selectedBackgroundColor: 'gray',
            allowsSelection: false,
            templates: {
                "admob": {
                    "properties": {
                        "height": 'SIZE',
                    },
                    "childTemplates": [{
                        type: 'AkylasAdmob.View',
                        properties: {
                            adUnitId: 'ca-app-pub-4951262838901192/2865489868', // You can get your own at http: //www.admob.com/
                            backgroundColor: 'blue',
                            testDevices: ["EAC5B55CEE507B7D477F9C2BB2EBF375", Admob.SIMULATOR_ID],
                            birthday: new Date(1985, 10, 1, 12, 1, 1),
                            gender: 'male',
                            keywords: ''
                        }
                    }]
                }
            },
            sections: [{
                items: items
            }]
        }
    });
    openWin(win);
}