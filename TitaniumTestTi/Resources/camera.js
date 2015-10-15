function create(_context) {
    var module = {};
    var Camera = app.modules.camera = require('akylas.camera');

    module.exs = function(_args) {
        var win = createWin(_args);
        var listview = createListView();
        listview.sections = [{
            items: [{
                properties: {
                    title: 'Camera'
                },
                callback: cameraEx1
            }]
        }];
        win.add(listview);
        openWin(win);
    }

    function cameraEx1(_args) {

        var win = createWin({
                childTemplates: [{
                    type: 'Akylas.Camera.View',
                    bindId: 'camera',
                    properties: {
                        // torch: true
                    },
                    childTemplates: [{
                        type: 'Ti.UI.ImageView',
                        bindId: 'image',
                        properties: {
                            width: 100,
                            height: 100,
                            bottom: 10,
                            left: 10,
                            scaleType: Ti.UI.SCALE_TYPE_ASPECT_FIT
                        }
                    }],
                    events: {
                        singletap: function(e) {
                            e.source.focus({
                                x: e.x,
                                y: e.y
                            });
                        },
                        swipe: function(e) {
                            sdebug('swipe', e.direction);
                            if (e.direction === 'left' || e.direction === 'right')
                                e.source.swapCamera();
                            else if (e.direction === 'up')
                                e.source.torch = true;
                            else if (e.direction === 'down')
                                e.source.torch = false;
                        },
                        doubletap: function(e) {
                            var rect = e.source.rect;
                            var center = {
                                x: (rect.x + rect.width / 2),
                                y: (rect.y + rect.height / 2)
                            };
                            e.source.autoFocus(center);
                        },
                        longpress: function(e) {
                            e.source.takePicture(function(e) {
                                win.image.image = e.image;
                            });
                        }
                    },

                }]
            },
            _args);

        openWin(win);
    }
    return module;

}

exports.load = function(_context) {
    return create(_context);
};