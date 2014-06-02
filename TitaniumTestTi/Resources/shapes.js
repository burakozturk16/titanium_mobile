var Shapes = app.modules.shapes = require('akylas.shapes');
function shapeExs(_args) {
    var win = createWin(_args);
    var listview = createListView();
    listview.sections = [{
        items: [{
            properties: {
                title: 'Arc'
            },
            callback: shape1Ex
        }, {
            properties: {
                title: 'Circle'
            },
            callback: shape2Ex
        }, {
            properties: {
                title: 'Line'
            },
            callback: shape3Ex
        }, {
            properties: {
                title: 'Inversed'
            },
            callback: shape4Ex
        }, {
            properties: {
                title: 'Shutter'
            },
            callback: shape5Ex
        }, {
            properties: {
                title: 'Inner Shadow'
            },
            callback: shape6Ex
        }, {
            properties: {
                title: 'PieSlice'
            },
            callback: shape7Ex
        }]
    }];
    win.add(listview);
    openWin(win);
}

function shape1Ex() {
    var win = createWin();
    var view = Shapes.createView({
        bubbleParent: false,
        width: 200,
        height: 200
    });
    view.add({
        lineColor: '#777',
        lineWidth: 10,
        lineCap: Shapes.CAP_ROUND,
        transform: Ti.UI.create2DMatrix().rotate(5),
        lineShadow: {
            color: 'white'
        },
        operations: [{
            type: 'arc',
            radius: '45%',
            startAngle: -160,
            sweepAngle: 320
        }]
    });
    var shape = Shapes.createArc({
        radius: '45%',
        // startAngle: -160,
        sweepAngle: 90,
        lineWidth: 10,
        lineCap: Shapes.CAP_ROUND,
        lineGradient: {
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
        }
    });
    view.add(shape);
    var anim = Ti.UI.createAnimation({
        duration: 600,
        autoreverse: true,
        height: 100
    });
    view.addEventListener('click', function(e) {
        // Shapes.cancelAllAnimations();
        // Shapes.sweepAngle = 320;
        view.animate(anim);
    });
    win.add(view);
    openWin(win);
}

function shape2Ex() {
    var win = createWin();
    var view = Shapes.createView({
        top: 150,
        borderRadius: 10,
        borderColor: 'red',
        borderWidth: 5,
        bubbleParent: false,
        width: 300,
        height: 100,
        backgroundColor: 'white',
        transform: Ti.UI.create2DMatrix().scale(1.5, 1.5),
        viewMask: '/images/body-mask.png'
    });
    var shape = Shapes.createCircle({
        fillColor: '#bbb',
        lineColor: '#777',
        lineWidth: 1,
        fillImage: '/images/pattern.png',
        transform: Ti.UI.create2DMatrix().scale(0.5, 1),
        lineShadow: {
            radius: 2,
            color: 'black'
        },
        radius: '40%'
    });
    view.add(shape);
    view.add(Ti.UI.createView({
        backgroundColor: 'red',
        bottom: 10,
        width: 30,
        height: 30
    }));
    var anim = Ti.UI.createAnimation({
        duration: 400,
        lineWidth: 20,
        // autoreverse: true,
        // restartFromBeginning:true,
        // repeat: 2,
        lineColor: 'yellow',
        fillColor: 'blue',
        curve: [0.68, -0.55, 0.265, 1.55]
    });
    shape.addEventListener('click', function(e) {
        // e.source.cancelAllAnimations();
        e.source.animate(anim);
    });
    win.add(view);
    openWin(win);
}

function shape3Ex() {
    var win = createWin();
    var view = Shapes.createView({
        bubbleParent: false,
        width: Ti.UI.FILL,
        height: 200
    });
    var shape = Shapes.createLine({
        lineColor: 'blue',
        lineWidth: 6,
        retina: false,
        antialiasing: true,
        lineCap: Shapes.CAP_BUTT,
        lineJoin: Shapes.JOIN_ROUND,
        lineShadow: {
            radius: 3,
            color: 'blue'
        },
        lineImage: '/images/pattern.png',
        // lineDash:{
        // 	phase:0,
        // 	pattern:[10,2,10]
        // },
        points: [
            ['0%', 0],
            ['20%', 20, '20%', 10, '10%', 30],
            ['40%', -5],
            ['60%', 8],
            ['80%', 16],
            ['100%', 0]
        ]
    });
    view.add(shape);
    view.addEventListener('click', function(e) {
        shape.animate({
            duration: 400,
            lineWidth: 20,
            autoreverse: true,
            lineColor: 'yellow',
            points: [
                ['0%', 30],
                ['10%', 40, '20%', 10, '10%', 30],
                ['40%', 25],
                ['60%', -38],
                ['80%', 56],
                ['100%', 0]
            ],
            curve: [0.68, -0.55, 0.265, 1.55]
        });
    });
    win.add(view);
    openWin(win);
}

function shape4Ex() {
    var win = createWin();
    win.add(Ti.UI.createLabel({
        width: Ti.UI.FILL,
        height: Ti.UI.FILL,
        bottom: 20,
        html: html
    }));
    var view = Shapes.createView({
        width: Ti.UI.FILL,
        height: Ti.UI.FILL,
        bubbleParent: false
    });
    var shape = Shapes.createCircle({
        fillColor: 'transparent',
        lineColor: '#777',
        lineWidth: 1,
        retina: false,
        antialiasing: false,
        fillGradient: {
            type: 'radial',
            colors: ['transparent', 'gray'],
            // startPoint:{x:0, y:0},
            // endPoint:{x:0, y:"100%"}
        },
        fillInversed: true,
        fillColor: 'blue',
        fillShadow: {
            radius: 5,
            color: 'black'
        },
        radius: '20%'
    });
    view.add(shape);
    shape.addEventListener('click', function(e) {
        e.source.cancelAllAnimations();
        e.source.animate({
            duration: 400,
            // lineWidth: 20,
            radius: '40%',
            // fillOpacity: 0.7,
            autoreverse: true,
            // lineColor: 'yellow',
            // fillColor: 'blue',
            curve: [0.68, -0.0, 0.265, 1.55]
        });
    });
    win.add(view);
    openWin(win);
}

function shape5Ex() {
    var win = createWin();
    win.add(Ti.UI.createLabel({
        width: Ti.UI.FILL,
        height: Ti.UI.FILL,
        bottom: 20,
        html: html
    }));
    var view = Shapes.createView({
        width: Ti.UI.FILL,
        height: Ti.UI.FILL,
        bubbleParent: false
    });
    var shape = Shapes.createRoundedRect({
        cornerRadius: 10,
        // lineColor:'#777',
        // lineWidth:4,
        retina: false,
        antialiasing: false,
        fillGradient: {
            type: 'radial',
            colors: ['white', 'gray']
        },
        fillInversed: true,
        fillColor: 'blue',
        fillShadow: {
            radius: 5,
            color: 'black'
        },
        transform: Ti.UI.create2DMatrix().scale(0.0003)
    });
    view.add(shape);
    view.addEventListener('click', function(e) {
        shape.animate({
            duration: 3000,
            restartFromBeginning: true,
            transform: Ti.UI.create2DMatrix().scale(2)
        });
    });
    win.add(view);
    openWin(win);
}

function shape6Ex() {
    var win = createWin();
    win.backgroundColor = 'gray';
    var view = Shapes.createView({
        width: 200,
        height: 200,
        bubbleParent: false
    });
    view.add(Shapes.createRoundedRect({
        lineWidth: 1,
        fillColor: 'white',
        lineColor: 'gray',
        cornerRadius: 10,
        lineClipped: true,
        radius: '43%',
        lineShadow: {
            radius: 4,
            color: 'black',
            offset: {
                x: 0,
                y: -3
            }
        }
    }));
    // view.add({
    // 	lineWidth:4,
    // 	fillColor:'white',
    // 	lineColor:'black',
    // 	cornerRadius:10,
    // 	radius:'43%',
    // 	lineShadow:{radius:4, color:'black', offset:{x:0,y:-4}},
    // 	type:'roundedrect'
    // });
    view.add(Ti.UI.createLabel({
        left: 14,
        right: 14,
        top: 14,
        bottom: 14,
        width: Ti.UI.FILL,
        height: Ti.UI.FILL,
        bottom: 20,
        html: html
    }));
    win.add(view);
    openWin(win);
}

function shape7Ex() {
    var win = createWin({
        backgroundColor: 'gray'
    });
    var view = Shapes.createView({
        width: 200,
        height: 200,
        bubbleParent: false
    });
    var slice1 = Shapes.createPieSlice({
        fillColor: '#aa00ffff',
        innerRadius: 30,
        startAngle: 0,
        radius: '40%',
        sweepAngle: 40
    });
    var slice2 = Shapes.createPieSlice({
        fillColor: '#aaff00ff',
        innerRadius: 30,
        startAngle: 30,
        sweepAngle: 100
    });
    var slice3 = Shapes.createPieSlice({
        fillColor: '#aaffff00',
        innerRadius: 30,
        startAngle: -60,
        radius: '20%',
        sweepAngle: 10
    });
    view.add({
        type: 'circle',
        radius: 30,
        fillColor: 'blue'
    });
    view.add(slice1);
    view.add(slice2);
    view.add(slice3);
    win.add(view);
    var anim1 = Ti.UI.createAnimation({});
    slice1.animate({
        duration: 10000,
        startAngle: 360,
        repeat: Ti.UI.INFINITE
    });
    slice2.animate({
        duration: 5000,
        startAngle: 200,
        autoreverse: true,
        repeat: Ti.UI.INFINITE
    });
    slice3.animate({
        duration: 4000,
        startAngle: -420,
        repeat: Ti.UI.INFINITE
    });
    var anim1 = Ti.UI.createAnimation({
        duration: 400,
        radius: '50%',
        restartFromBeginning: true,
        autoreverse: true
    });
    var anim2 = Ti.UI.createAnimation({
        duration: 700,
        radius: '30%',
        repeat: 3,
        autoreverse: true
    });
    var anim3 = Ti.UI.createAnimation({
        duration: 300,
        radius: '30%'
    });

    view.addEventListener('click', function(e) {
        slice1.animate(anim1);
        slice2.animate(anim2);
        // anim3.cancel();
        slice3.animate(anim3);
    });
    openWin(win);
}