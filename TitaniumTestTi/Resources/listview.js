function listViewExs(_args) {
    var win = createWin(_args);
    var listview = createListView();
    listview.sections = [{
        items: [{
            properties: {
                title: 'Click'
            },
            callback: listViewEx1
        }, {
            properties: {
                title: 'Complex layout'
            },
            callback: listViewEx2
        }, {
            properties: {
                title: 'Gradients'
            },
            callback: listViewEx3
        }, {
            properties: {
                title: 'Sections'
            },
            callback: listViewEx4
        }]
    }];
    win.add(listview);
    initGeoSettings();
    openWin(win);
}

function listViewEx1(_args) {
    var titleTest = ' Article title';
    var descriptionTest = ' This is a description text hopping it s going to hold on at least 2 lines';
    var win = createWin();
    var listview = Ti.UI.createListView({
        selectedBackgroundColor:'gray',
        defaultItemTemplate: 'default',
        allowsSelection:false,
        templates: {
            "default": {
                "properties": {
                    "rclass": "NewsRow",
                    "height": 85,
                    "dispatchPressed": true,
                    "borderColor": "black",
                    "borderPadding": {
                        "left": -1,
                        "right": -1,
                        "top": -1
                    }
                },
                "childTemplates": [{
                    "type": "Ti.UI.View",
                    "properties": {
                        "rclass": "NewsRowHolder"
                    },
                    "childTemplates": [{
                        "type": "Ti.UI.ImageView",
                        "bindId": "imageView",
                        "properties": {
                            "rclass": "NewsRowImage",
                            preventListViewSelection:true,
                            "dispatchPressed": true,
                            "top": 8,
                            "scaleType": 2,
                            "width": 50,
                            "height": 50,
                            "image": "/images/news_default.png",
                            "backgroundColor": "#C5C5C5",
                            "backgroundSelectedColor": "red",

                            "left": 8,
                            "retina": false,
                            "localLoadSync": true,
                            "preventDefaultImage": true
                        },
                        "childTemplates": [{
                            "type": "Ti.UI.View",
                            "properties": {
                                "rclass": "NewsRowImageHover",
                                "backgroundSelectedColor": "#88dddddd",
                                "dispatchPressed": true,
                                "touchPassThrough": true
                            },
                            "childTemplates": [{
                                "type": "Ti.UI.View",
                                "properties": {
                                    "rclass": "NewsRowImageHoverTicker",
                                    "touchPassThrough": true,
                                    "backgroundColor": "#2096D7",
                                    "width": 20,
                                    "height": 20,
                                    "right": -10,
                                    "bottom": -10,
                                    "transform": "r45",
                                    "backgroundSelectedColor": "#B0C113"
                                }
                            }]
                        }]
                    }, {
                        "type": "Ti.UI.Label",
                        "bindId": "source",
                        "properties": {
                            "rclass": "NewsRowSource",
                            "font": {
                                "size": 11
                            },
                            "left": 5,
                            "height": 13,
                            "bottom": 5,
                            "width": 50,
                            "verticalAlign": "top",
                            "color": "white",
                            "padding": {
                                "left": 5,
                                "right": 5
                            },
                            "ellipsize": "END"
                        }
                    }, {
                        "type": "Ti.UI.View",
                        "properties": {
                            "rclass": "NewsRowLabelHolder",
                            "touchEnabled": false,
                            "layout": "vertical",
                            "left": 66,
                            "top": 5,
                            "bottom": 5
                        },
                        "childTemplates": [{
                            "type": "Ti.UI.Label",
                            "bindId": "title",
                            "properties": {
                                "rclass": "NewsRowTitle",
                                "padding": {
                                    "right": 75,
                                    "left": 5
                                },
                                "color": "white",
                                "verticalAlign": "top",
                                "width": "FILL",
                                "maxLines": 2,
                                "height": "SIZE",
                                "ellipsize": "END",
                                "font": {
                                    "size": 16,
                                    "weight": "bold"
                                }
                            },
                            "childTemplates": [{
                                "type": "Ti.UI.Label",
                                "bindId": "date",
                                "properties": {
                                    "rclass": "NewsRowDate",
                                    "font": {
                                        "size": 10
                                    },
                                    "padding": {
                                        "top": 4,
                                        "left": 5,
                                        "right": 5
                                    },
                                    "textAlign": "right",
                                    "right": 0,
                                    "width": 75,
                                    "verticalAlign": "top",
                                    "color": "white",
                                    "height": "FILL",
                                    "ellipsize": "END"
                                }
                            }]
                        }, {
                            "type": "Ti.UI.Label",
                            "bindId": "description",
                            "properties": {
                                "rclass": "NewsRowSubtitle",
                                "verticalAlign": "top",
                                "color": "white",
                                "width": "FILL",
                                "padding": {
                                    "left": 5,
                                    "right": 5
                                },
                                "height": "FILL",
                                "ellipsize": "END",
                                "font": {
                                    "size": 13
                                }
                            }
                        }]
                    }]
                }]
            }
        },
    });
    var items = [];
    for (var i = 0; i < 10; i++) {
        items.push({
            source: {
                text: 'rss'
            },
            title: {
                text: titleTest
            },
            description: {
                text: descriptionTest
            },
            date: {
                text: (new Date()).toString()
            }
        });
    }
    listview.sections = [{
        items: items
    }];
    listview.addEventListener('itemclick', function(_event) {
        info('click ');
        if (_event.hasOwnProperty('section') && _event.hasOwnProperty('itemIndex')) {
            var item = _event.section.getItemAt(_event.itemIndex);
            info('click ' + _event.itemIndex + ":" + _event.bindId);
        }
    });
    win.add(listview);
    openWin(win);
}

function listViewEx2() {
    var win = createWin();
    var template = {
        properties: {
            layout: 'horizontal',
            backgroundColor: 'orange',
            dispatchPressed: true,
            height: 40,
            borderColor: 'blue'
        },
        childTemplates: [{
            type: 'Ti.UI.ImageView',
            bindId: 'button',
            properties: {
                width: 41,
                height: 'FILL',
                padding: {
                    top: 10,
                    bottom: 10,
                    left: 10,
                    right: 10
                },
                left: 4,
                right: 4,
                font: {
                    size: 18,
                    weight: 'bold'
                },
                // transition: {
                // style: Ti.UI.TransitionStyle.FADE,
                // substyle:Ti.UI.TransitionStyle.TOP_TO_BOTTOM
                // },
                localLoadSync: true,
                // backgroundColor:'blue',
                borderColor: 'gray',
                borderSelectedColor: 'red',
                backgroundGradient: {
                    type: 'linear',
                    colors: [{
                        color: 'blue',
                        offset: 0.0
                    }, {
                        color: 'transparent',
                        offset: 0.2
                    }, {
                        color: 'transparent',
                        offset: 0.8
                    }, {
                        color: 'blue',
                        offset: 1
                    }],
                    startPoint: {
                        x: 0,
                        y: 0
                    },
                    endPoint: {
                        x: 0,
                        y: "100%"
                    }
                },
                // borderRadius: 10,
                // clipChildren:false,
                color: 'white',
                selectedColor: 'black'
            }
        }, {
            type: 'Ti.UI.View',
            properties: {
                dispatchPressed: true,
                layout: 'vertical'
            },
            childTemplates: [{
                type: 'Ti.UI.View',
                properties: {
                    dispatchPressed: true,
                    layout: 'horizontal',
                    height: 'FILL'
                },
                childTemplates: [{
                    type: 'Ti.UI.Label',
                    bindId: 'tlabel',
                    properties: {
                        top: 2,
                        // backgroundGradient: {
                        // type: 'linear',
                        // colors: [{
                        // color: 'yellow',
                        // offset: 0.0
                        // }, {
                        // //   color: 'yellow',
                        // //   offset: 0.2
                        // // }, {
                        // //   color: 'yellow',
                        // //   offset: 0.8
                        // // }, {
                        // color: 'blue',
                        // offset: 1
                        // }],
                        // startPoint: {
                        // x: 0,
                        // y: 0
                        // },
                        // endPoint: {
                        // x: "100%",
                        // y: 0,
                        // }
                        // },
                        maxLines: 2,
                        ellipsize: Ti.UI.TEXT_ELLIPSIZE_TAIL,
                        font: {
                            size: 14
                        },
                        height: 'FILL',
                        width: 'FILL',
                        // bottom: -9
                    }
                }, {
                    type: 'Ti.UI.Label',
                    bindId: 'plabel',
                    properties: {
                        color: 'white',
                        padding: {
                            left: 14,
                            right: 4,
                            bottom: 2
                        },
                        shadowColor: '#55000000',
                        selectedColor: 'green',
                        shadowRadius: 2,
                        borderRadius: 4,
                        clipChildren: false,
                        font: {
                            size: 12,
                            weight: 'bold'
                        },
                        backgroundSelectedGradient: sweepGradient,
                        backgroundColor: 'red',
                        right: 10,
                        width: 100,
                        height: 20
                    }
                }]
            }, {
                type: 'Ti.UI.View',
                properties: {
                    layout: 'horizontal',
                    height: 20
                },
                childTemplates: [{
                    type: 'Ti.UI.View',
                    properties: {
                        width: Ti.UI.FILL,
                        backgroundColor: '#e9e9e9',
                        borderRadius: 4,
                        clipChildren: false,
                        bottom: 0,
                        height: 16
                    },
                    childTemplates: [{
                        type: 'Ti.UI.View',
                        bindId: 'progressbar',
                        properties: {
                            borderRadius: 4,
                            clipChildren: false,
                            left: 0,
                            height: Ti.UI.FILL,
                            backgroundColor: 'green'
                        }
                    }, {
                        type: 'Ti.UI.Label',
                        bindId: 'sizelabel',
                        properties: {
                            color: 'black',
                            shadowColor: '#55ffffff',
                            // width: 'FILL',
                            // height: 'FILL',
                            shadowRadius: 2,
                            text: 'size',

                            font: {
                                size: 12
                            }
                        }
                    }]
                }, {
                    type: 'Ti.UI.Label',
                    bindId: 'timelabel',
                    properties: {
                        font: {
                            size: 12
                        },
                        color: 'black',
                        textAlign: 'right',
                        right: 4,
                        height: 20,
                        bottom: 2,
                        width: 80
                    }
                }]
            }]
        }]
    };

    var names = ['Carolyn Humbert',
        'David Michaels',
        'Rebecca Thorning',
        'Joe B',
        'Phillip Craig',
        'Michelle Werner',
        'Philippe Christophe',
        'Marcus Crane',
        'Esteban Valdez',
        'Sarah Mullock'
    ];
    var priorities = ['downloading',
        'success',
        'failure',
        '',
        'test',
        'processing'
    ];
    var images = ['http://cf2.imgobject.com/t/p/w154/vjDUeQvczSdL8nzcMVwZtlVSXYe.jpg',
        'http://zapp.trakt.us/images/posters_movies/192263-138.jpg',
        'http://zapp.trakt.us/images/posters_movies/210231-138.jpg',
        'http://zapp.trakt.us/images/posters_movies/176347-138.jpg',
        'http://zapp.trakt.us/images/posters_movies/210596-138.jpg'
    ];
    var trId = Ti.UI.create2DMatrix({
        ownFrameCoord: true
    });
    var trDecaled = trId.translate(50, 0);
    var listView = createListView({
        rowHeight: 60,
        // minRowHeight: 40,
        onDisplayCell: function(_args) {
            _args.view.opacity = 0;
            _args.view.animate({
                opacity: 1,
                duration: 250
            });
        },
        defaultItemTemplate: 'template2'
    }, false);

    listView.templates = {
        'template': template,
        'template2': {
            "properties": {
                "rclass": "NZBGetRowBordered",
                "height": 75,
                "borderPadding": {
                    "left": -1,
                    "right": -1,
                    "top": -1
                },
                "borderColor": "#DDDDDD",
                "backgroundColor": "white"
            },
            "childTemplates": [{
                "type": "Ti.UI.View",
                "properties": {
                    "rclass": "NZBGetDRCheckHolder",
                    "width": 40,
                    "height": 40,
                    "left": 4
                },
                "childTemplates": [{
                    "type": "Ti.UI.Label",
                    "bindId": "check",
                    "properties": {
                        "rclass": "NZBGetDRCheck",
                        "color": "transparent",
                        "textAlign": "center",
                        "clipChildren": false,
                        "borderSelectedColor": "#0088CC",
                        "font": {
                            "family": "LigatureSymbols"
                        },
                        "text": "",
                        "width": 20,
                        "height": 20,
                        "borderRadius": 2,
                        "borderColor": "#DDDDDD"
                    }
                }]
            }, {
                "type": "Ti.UI.Button",
                "bindId": "button",
                "properties": {
                    "rclass": "NZBGetDRButton",
                    "width": 40,
                    "height": 40,
                    "left": 4,
                    "font": {
                        "family": "Simple-Line-Icons",
                        "size": 18,
                        "weight": "bold"
                    },
                    "borderRadius": 10,
                    "borderWidth": 1,
                    "color": "white",
                    "selectedColor": "gray",
                    "backgroundColor": "transparent"
                },
                "events": {}
            }, {
                "type": "Ti.UI.ActivityIndicator",
                "bindId": "loader",
                "properties": {
                    "width": 40,
                    "height": 40,
                    visible: false,
                    style: Ti.UI.ActivityIndicatorStyle.DARK

                }
            }, {
                "type": "Ti.UI.View",
                "properties": {
                    "rclass": "NZBGetDRVHolder",
                    "layout": "vertical",
                    "left": 44,
                    "height": "FILL",
                    "width": "FILL"
                },
                "childTemplates": [{
                    "type": "Ti.UI.Label",
                    "bindId": "tlabel",
                    "properties": {
                        "rclass": "NZBGetRTitle",
                        "padding": {
                            "left": 5,
                            "right": 5
                        },
                        "ellipsize": 'END',
                        "maxLines": 2,
                        "height": "48%",
                        "width": "FILL",
                        "verticalAlign": "top",
                        "font": {
                            "size": 14
                        },
                        "color": "black"
                    }
                }, {
                    "type": "Ti.UI.View",
                    "properties": {
                        "rclass": "Fill HHolder",
                        "layout": "horizontal",
                        "width": "FILL",
                        "height": "FILL"
                    },
                    "childTemplates": [{
                        "type": "Ti.UI.Label",
                        "bindId": "category",
                        "properties": {
                            "rclass": "NZBGetLabelLeft NZBGetRTags",
                            "backgroundColor": "#999999",
                            "color": "white",
                            "padding": {
                                "left": 2,
                                "right": 2,
                                "top": 0
                            },
                            "shadowColor": "#55000000",
                            "shadowRadius": 1,
                            "borderRadius": 2,
                            "height": "SIZE",
                            "width": "SIZE",
                            "maxLines": 1,
                            "clipChildren": false,
                            "font": {
                                "size": 12,
                                "weight": "bold"
                            },
                            "textAlign": "left",
                            "left": 5
                        }
                    }, {
                        "type": "Ti.UI.Label",
                        "bindId": "health",
                        "properties": {
                            "visible": false,
                            "rclass": "NZBGetLabelLeft NZBGetRTags",
                            "backgroundColor": "#999999",
                            "color": "white",
                            "padding": {
                                "left": 2,
                                "right": 2,
                                "top": 0
                            },
                            "shadowColor": "#55000000",
                            "shadowRadius": 1,
                            "borderRadius": 2,
                            "height": "SIZE",
                            "width": "SIZE",
                            "maxLines": 1,
                            "clipChildren": false,
                            "font": {
                                "size": 12,
                                "weight": "bold"
                            },
                            "textAlign": "left",
                            "left": 5
                        }
                    }, {
                        "type": "Ti.UI.View",
                        "properties": {
                            "rclass": "FILL"
                        }
                    }, {
                        "type": "Ti.UI.Label",
                        "bindId": "priority",
                        "properties": {
                            "rclass": "NZBGetLabelRight NZBGetRPriority",
                            "color": "white",
                            "padding": {
                                "left": 2,
                                "right": 2,
                                "top": 0
                            },
                            "shadowColor": "#55000000",
                            "shadowRadius": 1,
                            "borderRadius": 2,
                            "height": "SIZE",
                            "width": "SIZE",
                            "maxLines": 1,
                            "clipChildren": false,
                            "font": {
                                "size": 12,
                                "weight": "bold"
                            },
                            "backgroundColor": "#b94a48",
                            "textAlign": "right",
                            "right": 5
                        }
                    }]
                }, {
                    "type": "Ti.UI.View",
                    "properties": {
                        "rclass": "Fill",
                        "width": "FILL",
                        "height": "FILL"
                    },
                    "childTemplates": [{
                        "type": "Ti.UI.View",
                        "properties": {
                            "rclass": "NZBGetDRPPBHolder",
                            "disableHW": true,
                            "left": 3,
                            "top": 1,
                            "height": 16,
                            "right": 60,
                            "bottom": 2
                        },
                        "childTemplates": [{
                            "type": "Ti.UI.View",
                            "properties": {
                                "rclass": "NZBGetDRPPBack",
                                "backgroundColor": "#e9e9e9",
                                "borderPadding": {
                                    "bottom": -1
                                },
                                "borderColor": "#E1E1E1",
                                "borderRadius": 4
                            }
                        }, {
                            "type": "Ti.UI.View",
                            "bindId": "progressbar",
                            "properties": {
                                "rclass": "NZBGetDRPPB",
                                "borderPadding": {
                                    "top": -1,
                                    "left": -1,
                                    "right": -1
                                },
                                "left": 0,
                                "height": "FILL",
                                "borderRadius": 4,
                                "backgroundGradient": {
                                    "type": "linear",
                                    "tileMode": "repeat",
                                    "rect": {
                                        "x": 0,
                                        "y": 0,
                                        "width": 40,
                                        "height": 40
                                    },
                                    "colors": [{
                                        "offset": 0,
                                        "color": "#26ffffff"
                                    }, {
                                        "offset": 0.25,
                                        "color": "#26ffffff"
                                    }, {
                                        "offset": 0.25,
                                        "color": "transparent"
                                    }, {
                                        "offset": 0.5,
                                        "color": "transparent"
                                    }, {
                                        "offset": 0.5,
                                        "color": "#26ffffff"
                                    }, {
                                        "offset": 0.75,
                                        "color": "#26ffffff"
                                    }, {
                                        "offset": 0.75,
                                        "color": "transparent"
                                    }, {
                                        "offset": 1,
                                        "color": "transparent"
                                    }],
                                    "startPoint": {
                                        "x": 0,
                                        "y": 0
                                    },
                                    "endPoint": {
                                        "x": "100%",
                                        "y": "100%"
                                    }
                                }
                            }
                        }, {
                            "type": "Ti.UI.Label",
                            "bindId": "sizelabel",
                            "properties": {
                                "rclass": "NZBGetDRSize",
                                "maxLines": 1,
                                "textAlign": "center",
                                "pading": {
                                    "left": 2,
                                    "right": 2
                                },
                                "ellipsize": 'END',
                                "font": {
                                    "size": 12
                                },
                                "height": "FILL",
                                "width": "FILL",
                                "color": "black"
                            }
                        }]
                    }, {
                        "type": "Ti.UI.Label",
                        "bindId": "timelabel",
                        "properties": {
                            "rclass": "NZBGetDRTime",
                            "width": 60,
                            "height": 16,
                            "textAlign": "right",
                            "right": 5,
                            "font": {
                                "size": 12
                            },
                            "color": "black"
                        }
                    }]
                }]
            }]
        }
    };
    var items = [];

    for (var i = 0; i < 300; i++) {
        var cat = priorities[Math.floor(Math.random() * priorities.length)];
        var priority = priorities[Math.floor(Math.random() * priorities.length)];
        items.push({
            properties: {
                //  // height: 60
            },
            button: {
                callbackId: i,
                visible: true,
                backgroundColor: '#fbb450',
                borderColor: '#f89405',
                selectedColor: 'gray',
                title: '||'
            },
            tlabel: {
                text: names[Math.floor(Math.random() * names.length)]
            },
            priority: {
                visible: priority.length > 0,
                html: priority
            },
            sizelabel: {
                text: (new Date()).toString()
            },
            timelabel: {
                html: '<strike>' + (new Date()).toString() + '</strike>'
            },
            category: {
                visible: cat.length > 0,
                text: cat
            },
            progressbar: {
                backgroundColor: '#fbb450',
                borderColor: '#f89405',
                width: Math.floor(Math.random() * 100) + '%'
            },
            check: {
                color: 'transparent'
            }
        });
    }
    listView.setSections([{
        items: items
    }]);

    win.add(listView);
    win.addEventListener('click', function(_event) {
        listView.updateItemAt(0, 0, {
            tlabel: {
                text: 'toto',
                color: 'transparent'
            },
            button: {
                visible: false
            },
            loader: {
                visible: true
            }
        });
        info('click ');
        if (_event.bindId && _event.hasOwnProperty('section') && _event.hasOwnProperty('itemIndex')) {
            var item = _event.section.getItemAt(_event.itemIndex);
            if (_event.bindId === 'button') {
                item.button.image = images[Math.floor(Math.random() * images.length)];
                item.properties.backgroundColor = 'blue';
                item.priority.text = 'my test';
                item.priority.backgroundColor = 'green';
                info(item);
                _event.section.updateItemAt(_event.itemIndex, item);
            }
        }
    });
    openWin(win);
}

function listViewEx3() {
    var win = createWin();
    var listview = Ti.UI
        .createListView({
            allowsSelection: false,
            rowHeight: 50,
            selectedBackgroundGradient: sweepGradient,
            sections: [{
                items: [{
                    properties: {
                        backgroundColor: 'blue',
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        backgroundColor: 'red',
                        title: 'ButtonsAndLabels'
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Transform',
                        accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        backgroundColor: 'red',
                        title: 'Shape',
                        accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Transform',
                        accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        backgroundColor: 'red',
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Transform',
                        accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Transform',
                        accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        backgroundColor: 'red',
                        title: 'Shape',
                        accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Transform',
                        accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        backgroundColor: 'red',
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Transform',
                        accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Transform',
                        accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        backgroundColor: 'red',
                        title: 'Shape',
                        accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Transform',
                        accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        backgroundColor: 'red',
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Transform',
                        accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Transform',
                        accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        backgroundColor: 'red',
                        title: 'Shape',
                        accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Transform',
                        accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        backgroundColor: 'red',
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Transform',
                        accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Transform',
                        accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        backgroundColor: 'red',
                        title: 'Shape',
                        accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Transform',
                        accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        backgroundColor: 'red',
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Transform',
                        accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
                    }
                }, {
                    properties: {
                        title: 'Shape'
                    }
                }, {
                    properties: {
                        title: 'Transform',
                        accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
                    }
                }]
            }]
        });
    if (__APPLE__) listview.style = Titanium.UI.iPhone.ListViewStyle.GROUPED;
    win.add(listview);
    openWin(win);
}

function listViewEx4() {
    var win = createWin();
    // Create a custom template that displays an image on the left,
    // then a title next to it with a subtitle below it.
    var myTemplate = {
        childTemplates: [{ // Image justified left
            type: 'Ti.UI.ImageView', // Use an image view for the image
            bindId: 'pic', // Maps to a custom pic property of the item data
            properties: { // Sets the image view  properties
                width: '50dp',
                height: '50dp',
                left: 0
            }
        }, { // Title
            type: 'Ti.UI.Label', // Use a label for the title
            bindId: 'info', // Maps to a custom info property of the item data
            properties: { // Sets the label properties
                color: 'black',
                font: {
                    fontFamily: 'Arial',
                    size: '20dp',
                    weight: 'bold'
                },
                left: '60dp',
                top: 0,
            }
        }, { // Subtitle
            type: 'Ti.UI.Label', // Use a label for the subtitle
            bindId: 'es_info', // Maps to a custom es_info property of the item data
            properties: { // Sets the label properties
                color: 'gray',
                font: {
                    fontFamily: 'Arial',
                    size: '14dp'
                },
                left: '60dp',
                top: '25dp',
            }
        }, { // Subtitle
            type: 'Ti.UI.Label', // Use a label for the subtitle
            properties: { // Sets the label properties
                color: 'red',
                selectedColor: 'green',
                backgroundColor: 'blue',
                backgroundSelectedColor: 'orange',
                text: 'test',
                right: '0dp'
            },
            events: {
                'click': function() {}
            }
        }]
    };
    var listView = Ti.UI.createListView({
        delaysContentTouches: false,
        // Maps myTemplate dictionary to 'template' string
        templates: {
            'template': myTemplate
        },
        // Use 'template', that is, the myTemplate dict created earlier
        // for all items as long as the template property is not defined for an item.
        defaultItemTemplate: 'template',
        selectedBackgroundGradient: {
            type: 'linear',
            colors: ['blue', 'green'],
            startPoint: {
                x: 0,
                y: 0
            },
            endPoint: {
                x: 0,
                y: "100%"
            }
        }
    });
    if (__APPLE__) listView.style = Titanium.UI.iPhone.ListViewStyle.GROUPED;
    var sections = [];
    var fruitSection = Ti.UI.createListSection({
        headerTitle: 'Fruits / Frutas'
    });
    var fruitDataSet = [
        // the text property of info maps to the text property of the title label
        // the text property of es_info maps to text property of the subtitle label
        // the image property of pic maps to the image property of the image view
        {
            info: {
                text: 'Apple'
            },
            es_info: {
                text: 'Manzana'
            },
            pic: {
                image: 'apple.png'
            }
        }, {
            properties: {
                backgroundColor: 'red'
            },
            info: {
                text: 'Banana'
            },
            es_info: {
                text: 'Banana'
            },
            pic: {
                image: 'banana.png'
            }
        }
    ];
    fruitSection.setItems(fruitDataSet);
    sections.push(fruitSection);
    var vegSection = Ti.UI.createListSection({
        headerTitle: 'Vegetables / Verduras'
    });
    var vegDataSet = [{
        info: {
            text: 'Carrot'
        },
        es_info: {
            text: 'Zanahoria'
        },
        pic: {
            image: 'carrot.png'
        }
    }, {
        info: {
            text: 'Potato'
        },
        es_info: {
            text: 'Patata'
        },
        pic: {
            image: 'potato.png'
        }
    }];
    vegSection.setItems(vegDataSet);
    sections.push(vegSection);
    var grainSection = Ti.UI.createListSection({
        headerTitle: 'Grains / Granos'
    });
    var grainDataSet = [{
        info: {
            text: 'Corn'
        },
        es_info: {
            text: 'Maiz'
        },
        pic: {
            image: 'corn.png'
        }
    }, {
        info: {
            text: 'Rice'
        },
        es_info: {
            text: 'Arroz'
        },
        pic: {
            image: 'rice.png'
        }
    }];
    grainSection.setItems(grainDataSet);
    sections.push(grainSection);
    listView.setSections(sections);
    win.add(listView);
    openWin(win);
}

var sweepGradient = {
    type: 'sweep',
    colors: [{
        color: 'orange',
        offset: 0
    }, {
        color: 'red',
        offset: 0.19
    }, {
        color: 'red',
        offset: 0.25
    }, {
        color: 'blue',
        offset: 0.25
    }, {
        color: 'blue',
        offset: 0.31
    }, {
        color: 'green',
        offset: 0.55
    }, {
        color: 'yellow',
        offset: 0.75
    }, {
        color: 'orange',
        offset: 1
    }]
};