function createModule(context) {
    var module = {};
    module.FADEIN = 0;
    module.FADEOUT = 1;
    module.SLIDEIN = 2;
    module.SLIDEOUT = 3;
    var identity = Ti.UI.create2DMatrix();
    module.animateView = function(_view, _args) {
        if (_view === null)
            return;
        //that shouldnt happen!
        // _view.visible = _view.visible || false;
        //just to make sure
 
        _args = _args || {};
        var complete = _args.complete;
        _args.type = _args.type || module.FADEIN;
 
        //do we really need to do it? (already at state)
        if (!_view.currentAnimation || _view.currentAnimation === null) {
            if ((_args.type === module.SLIDEIN && _view.visible === true) || (_args.type === module.SLIDEOUT && _view.visible === false)) {
                complete && complete();
                return;
            }
        }
 
        if (_view.currentAnimation && _view.currentAnimation !== null) {
            _view.currentAnimation.addEventListener('complete', function() {
                ak.module.animateView(_view, _args);
            });
            // _view.cancelAllanimations();
            return;
        }
 
        _args.direction = _args.direction || 'bottom';
        var decale = _args.decale || 0;
 
        var tx = 0, ty = 0;
        if (_args.type === module.SLIDEIN || _args.type === module.SLIDEOUT) {
            var width = app.deviceinfo.width;
            if (_view.getParent() && _view.getParent().rect && _view.getParent().rect.width > 0)
                width = _view.getParent().rect.width;
            var height = app.deviceinfo.height;
            if (_view.getParent() && _view.getParent().rect && _view.getParent().rect.height > 0)
                height = _view.getParent().rect.height;
 
            if (_args.direction === 'bottom')
                ty = height;
            else if (_args.direction === 'top')
                ty = -height;
            else if (_args.direction === 'right')
                tx = width;
            else if (_args.direction === 'left')
                tx = -width;
        } else if (decale != 0) {
            if (_args.direction === 'bottom')
                ty = decale;
            else if (_args.direction === 'top')
                ty = -decale;
            else if (_args.direction === 'right')
                tx = -decale;
            else if (_args.direction === 'left')
                tx = decale;
        }
 
        var oldMatrix;
        if (_args.transform && _args.transform != null)
            oldMatrix = _args.transform;
        //i cant save the old matrix, because when going too fast you can end up being
        // stuck with that matrix
        // Ti.API.debug(_args.type + ":" + tx + "," + ty);
        var matrix = (oldMatrix || identity).translate(tx, ty);
 
        var anim = new Animation();
        anim.duration = (_args.duration || 400);
        anim.delay = (_args.delay || 0);
        anim.curve = (_args.curve || Titanium.UI.module_CURVE_EASE_IN_OUT);
 
        _view.currentAnimation = anim;
        if (_.isFunction(complete)) {
            var customCompleteHandler = function() {
                anim.removeEventListener('complete', customCompleteHandler);
                complete();
            };
            anim.addEventListener('complete', customCompleteHandler);
        }
 
        switch (_args.type) {
            case module.FADEIN: {
                _view.opacity = 0;
                _view.visible = true;
                _view.transform = matrix;
                if (oldMatrix)
                    anim.transform = oldMatrix;
                anim.opacity = 1;
 
                var moduleCompleteHandler = function() {
                    anim.removeEventListener('complete', moduleCompleteHandler);
                    // _view.transform = oldMatrix;
                    delete _view.currentAnimation;
                };
                anim.addEventListener('complete', moduleCompleteHandler);
 
                _view.animate(anim);
                break;
            }
            case module.FADEOUT: {
                anim.transform = matrix;
                anim.opacity = 0;
 
                var moduleCompleteHandler = function() {
                    anim.removeEventListener('complete', moduleCompleteHandler);
                    _view.visible = false;
                    delete _view.currentAnimation;
                };
                anim.addEventListener('complete', moduleCompleteHandler);
                _view.animate(anim);
                break;
            }
            case module.SLIDEIN: {
                _view.transform = matrix;
                _view.visible = true;
                if (oldMatrix)
                    anim.transform = oldMatrix;
                if (_args.opacity)
                    anim.opacity = _args.opacity;
 
                var moduleCompleteHandler = function() {
                    anim.removeEventListener('complete', moduleCompleteHandler);
                    // _view.transform = oldMatrix;
                    delete _view.currentAnimation;
                };
                anim.addEventListener('complete', moduleCompleteHandler);
                _view.animate(anim);
                break;
            }
            case module.SLIDEOUT: {
                anim.transform = matrix;
                if (_args.opacity)
                    anim.opacity = _args.opacity;
 
                var moduleCompleteHandler = function() {
                    anim.removeEventListener('complete', moduleCompleteHandler);
                    _view.visible = false;
                    delete _view.currentAnimation;
                };
                anim.addEventListener('complete', moduleCompleteHandler);
                _view.animate(anim);
                break;
            }
        }
    };
 
    module.crossFade = function(from, to, duration, finishCallback) {
        module.fadeOut(from, duration);
        module.fadeIn(to, duration);
        finishCallback && setTimeout(finishCallback, duration + 300);
        to = from = duration = null;
    };
 
    module.fadeAndRemove = function(from, duration, container, finishCallback) {
        module.fadeOut(from, duration, function() {
            container.remove(from);
            container = from = duration = null;
            finishCallback && finishCallback();
        });
    };
 
    module.fadeIn = function(view, duration, finishCallback) {
        view.animate({
            duration : duration || 200,
            opacity:1.0
        }, function(){
            finishCallback && finishCallback();
        });
    };
 
    module.fadeOut = function(view, duration, finishCallback) {
        view.animate({
            duration : duration || 200,
            opacity:0.0
        }, function(){
            finishCallback && finishCallback();
        });
    };
 
    module.slideOut = function(to, direction, duration, finishCallback) {
        to && module.animateView(to, {
            type : module.SLIDEOUT,
            direction : direction,
            duration : duration || 200,
            complete : finishCallback
        });
        to = null;
    };
 
    module.slideIn = function(to, direction, duration, finishCallback) {
        to && module.animateView(to, {
            type : module.SLIDEIN,
            direction : direction,
            duration : duration || 200,
            complete : finishCallback
        });
        to = null;
    };
 
    module.popIn = function(view, finishCallback) {
        var identity = Ti.UI.create2DMatrix();
        // view.transform = identity.scale(0.6, 0.6);
        // view.opacity = 0.60;
 
        module.chainAnimate(view, [{
            transform : identity.scale(1.05, 1.05),
            duration : 200
        }, {
            transform : identity.scale(0.9, 0.9),
            duration : 30
        }, {
            transform : identity,
            duration : 120
        }], finishCallback);
        view = null;
    };
 
    module.popOut = function(view, finishCallback) {
        view.transform = identity.scale(0.9, 0.9);
        var module = new Animation({
            transform : identity,
            duration : 100
        });
        if (finishCallback) {
            module.addEventListener('complete', function() {
                finishCallback();
            });
        }
        view.animate(module);
        view = null;
    };
 
    module.shake = function(view, delay, finishCallback) {
        var shake1 = new Animation({
            transform : identity.translate(5, 0),
            duration : 100
        }), shake2 = new Animation({
            transform : identity.translate(-5, 0),
            duration : 100
        }), shake3 = new Animation({
            transform : identity.translate(5, 0),
            duration : 100
        }), shake4 = new Animation({
            transform : identity.translate(-5, 0),
            duration : 100
        }), shake5 = new Animation({
            transform : identity,
            duration : 100
        });
        delay ? setTimeout(function() {
            module.chainAnimate(view, [shake1, shake2, shake3, shake4, shake5], finishCallback);
            view = shake1 = shake2 = shake3 = shake4 = shake5 = null;
        }, delay) : module.chainAnimate(view, [shake1, shake2, shake3, shake4, shake5], finishCallback);
    };
 
    module.flash = function(view, delay, finishCallback) {
        var flash1 = new Animation({
            opacity : 0.7,
            duration : 100
        }), flash2 = new Animation({
            opacity : 1,
            duration : 100
        }), flash3 = new Animation({
            opacity : 0.7,
            duration : 100
        }), flash4 = new Animation({
            opacity : 1,
            duration : 100
        });
        delay ? setTimeout(function() {
            module.chainAnimate(view, [flash1, flash2, flash3, flash4], finishCallback);
            view = flash1 = flash2 = flash3 = flash4 = null;
        }, delay) : module.chainAnimate(view, [flash1, flash2, flash3, flash4], finishCallback);
    };
 
    module.chainAnimate = function(view, animations, finishCallback) {
        var animations = animations;
        function step() {
            if (animations.length === 0) {
                view = animations = null;
                if (finishCallback)
                    finishCallback();
                return;
            }
            view.animate(animations.shift(), step);
        };
 
        step();
    };
 
    return module;
}
 
if ( typeof exports === 'undefined') {
    var animation = createModule(this);
} else {
    exports.init = createModule;
}