var Map = app.modules.map = require('akylas.map');

function mapExs(_args) {
    var win = createWin(_args);
    var listview = createListView();
    listview.sections = [{
        items: [{
            properties: {
                title: 'MapBox'
            },
            callback: mapboxEx
        }, {
            properties: {
                title: 'Map'
            },
            callback: mapEx
        }]
    }];
    win.add(listview);
    initGeoSettings();
    openWin(win);
}

function mapboxEx() {
    var win = Ti.UI.createWindow({
        backgroundColor: 'white'
    });
    // var win = createWin({
    // layout: 'vertical'
    // });
    var rows = [{
        properties: {
            title: 'Go Mt. View'
        },
        run: function() {
            map.region = {
                latitude: 37.3689,
                longitude: -122.0353,
                latitudeDelta: 0.1,
                longitudeDelta: 0.1
            }; //Mountain View
        }
    }, {
        properties: {
            title: 'add anno3'
        },
        run: function() {
            map.addAnnotation(anno3);
        }
    }, {
        properties: {
            title: 'rm anno3'
        },
        run: function() {
            map.removeAnnotation(anno3);
        }
    }, {
        properties: {
            title: 'add anno1, 2, 4'
        },
        run: function() {
            map.annotations = [anno, anno2, anno4];
        }
    }, {
        properties: {
            title: 'rm all'
        },
        run: function() {
            map.removeAllAnnotations();
        }
    }, {
        properties: {
            title: 'remove annos: Sydney, anno2'
        },
        run: function() {
            Ti.API.info(anno.getTitle());
            map.removeAnnotations(["Sydney", anno2]);
        }
    }, {
        properties: {
            title: 'select anno2'
        },
        run: function() {
            map.selectAnnotation(anno2);
        }
    }, {
        properties: {
            title: 'desel anno2'
        },
        run: function() {
            map.deselectAnnotation(anno2);
        }
    }, {
        properties: {
            title: 'modify anno2'
        },
        run: function() {
            anno2.title = "Hello";
            anno2.subtitle = "Hi there.";
            anno2.longitude = 151.27689;
        }
    }];

    var tableView = Ti.UI.createListView({
        height: '30%',
        sections: [{
            items: rows
        }]
    });
    // win.add(tableView);
    tableView.addEventListener('itemclick', function(_event) {
        if (_event.hasOwnProperty('itemIndex')) {
            var item = _event.section.getItemAt(_event.itemIndex);
            rows[_event.itemIndex].run && rows[_event.itemIndex].run();
        }
    });

    var anno = Map.createAnnotation({
        latitude: -33.87365,
        longitude: 151.20689,
        title: "Sydney",
        subtitle: "Sydney is quite chill",
        draggable: true
    });
    var anno2 = Map.createAnnotation({
        latitude: -33.86365,
        pincolor: 'green',
        longitude: 151.21689,
        title: "Anno2",
        subtitle: "This is anno2",
        draggable: true
    });
    var anno3 = Map.createAnnotation({
        latitude: -33.85365,
        longitude: 151.20689,
        title: "Anno3",
        subtitle: "This is anno3",
        draggable: false
    });
    var anno4 = Map.createAnnotation({
        latitude: -33.86365,
        longitude: 151.22689,
        title: "Anno4",
        subtitle: "This is anno4",
        draggable: true
    });

    Ti.API.info("Latitude:" + anno.latitude);
    Ti.API.info("Title:" + anno.title);

    var map = Map.createMapboxView({
        userLocationEnabled: true,
        userTrackingMode: 1,
        backgroundColor: 'gray',
        tileSource: 'mapquest',
        diskCache:true,
        // userLocationRequiredZoom:5,
        animate: true,
        // regionFit: true,
        disableHW: true,
        zoom: 6,
        mapType: Map.NORMAL_TYPE,
        centerCoordinate: [46, 2],
        // defaultPinImage: 'map_pin.png',
        // animate: true,
        // annotations: [anno, anno2, anno4],

        // region: {
        // 	latitude: -33.87365,
        // 	longitude: 151.20689,
        // 	latitudeDelta: 0.1,
        // 	longitudeDelta: 0.1
        // }, //Sydney
        height: 'FILL',
        width: 'FILL',
    });
    win.add(map);

    function onEventInfo(e) {
        info(e.type);
    }
    var tileSource;
    var token;

    function queryString(params, _start) {
        return _.reduce(params, function(memo, value, key) {
            if (memo !== '') return memo + '&' + key + '=' + value;
            else return '?' + key + '=' + value;
        }, _start || '');
    };

    function runMethod(_method, _params, _callback) {
        if (token) {
            _params.token = token;
        }
        var url = queryString(_params, 'https://api.quantified-people.com/api.php?fct=' + _method);

        var xhr = Titanium.Network.createHTTPClient({
            onload: _callback
        });
        info(url);
        xhr.open('GET', url);
        xhr.send();
    }

    function onError(e) {
        alert('error' + e);
    }

    function loggin(_username, _password, _callback) {
        runMethod('auth', {
            method: 0,
            login: _username,
            passwd: _password
        }, function(e) {
            try {
                token = JSON.parse(this.responseText).token;
                _callback();
            } catch (e) {
                onError();
            }
        });
    }

    function getTrailsList(_callback) {
        runMethod('getOfflineList', {}, function(e) {
            try {
                var json = JSON.parse(this.responseText);
                var optionTitles = [];
                var optionValues = [];
                _.each(json.list, function(value, key, list) {
                    optionTitles.push(value.TrackName);
                    optionValues.push(value);
                });
                var opts = {
                    cancel: optionTitles.length,
                    options: optionTitles.concat(['cancel']),
                    title: 'Select Trail'
                };
                var dialog = Ti.UI.createOptionDialog(opts);
                dialog.addEventListener('click', function(e) {
                    if (e.cancel == false) {
                        _callback(optionValues[e.index]);
                    }
                });
                dialog.show();
            } catch (e) {
                onError();
            }
        });
    }

    function onDownloadedTrail(_trailId, _file) {
        tileSource = Map.createTileSource({
            source: _file.nativePath
        });
        // tileSource.addEventListener('cachebegin', onEventInfo);
        // tileSource.addEventListener('cachefinished', onEventInfo);
        // tileSource.beginBackgroundCache();
        map.addTileSource(tileSource, 0);
        var region = tileSource.region;
        info(stringify(region));
        if (online === false) {
            map.scrollableAreaLimit = region;
        } else {
            map.scrollableAreaLimit = null;
        }
    }

    var currentRoute;
    // var recordingRoute = Map.createRoute({
    // color: 'red'
    // });
    // map.addRoute(recordingRoute);

    function loadRoute(points) {
        info(stringify(points));
        currentRoute = Map.createRoute({
            points: points,
            lineJoin: 'round',
            color: 'blue',
            width: 5.0
        });
        map.addRoute(currentRoute);
        map.userTrackingMode = 0;
        followUser = false;
        map.region = currentRoute.region;
    }

    function downloadTrail(_trailId) {

        runMethod('getTrailInfo', {
            trailId: _trailId
        }, function(e) {
            try {
                var json = JSON.parse(this.responseText);
                info(_trailId);

                var points = [];
                var gpx = json.GPX;
                for (var i = 0; i < gpx.length; i++) {
                    var dict = gpx[i];
                    points.push({
                        latitude: dict.lat,
                        longitude: dict.lon,
                        altitude: dict.ele,
                    });
                };
                Ti.App.Properties.setString('routeSaved', JSON.stringify(points));

                var f = Ti.Filesystem.getFile(Ti.Filesystem.applicationDataDirectory, 'trail' + _trailId +
                    '.MBTiles');
                if (f.exists()) {
                    onDownloadedTrail(_trailId, f);
                    loadRoute(points);
                } else {
                    info('no file already downloaded');
                    runMethod('getTrailInfo', {
                        trailId: _trailId,
                        mbTiles: 1
                    }, function(e) {
                        try {
                            var json = JSON.parse(this.responseText);
                            var xhr2 = Titanium.Network.createHTTPClient({
                                onload: function() {
                                    f.write(this.responseData);
                                    Ti.App.Properties.setString('mapSaved', _trailId);
                                    onDownloadedTrail(_trailId, f);
                                    loadRoute(points);
                                }
                            });
                            xhr2.open('GET', json.MBTiles.tiles);
                            xhr2.send();
                        } catch (e) {
                            onError(e);
                        }
                    });
                }
            } catch (e) {
                onError(e);
            }
        });

        // var xhr = Titanium.Network.createHTTPClient({
        // 	onload: function() {
        // 		var json = JSON.parse(this.responseText);
        // 		info(json);
        // 		if (json && json.tiles) {
        // 			var xhr2 = Titanium.Network.createHTTPClient({
        // 				onload: function() {
        // 					var f = Ti.Filesystem.getFile(Ti.Filesystem.applicationDataDirectory, 'trail' + _id + '.MBTiles');
        // 					f.write(this.responseData);
        // 					onDownloadedTile(_id, f);
        // 				},
        // 				// timeout: 10000
        // 			});
        // 			xhr2.open('GET', json.tiles);
        // 			xhr2.send();
        // 		}

        // 	},
        // 	// timeout: 10000
        // });
        // xhr.open('GET', 'https://api.quantified-people.com/getmbTrail.php?idTrail=' + _id);
        // xhr.send();
    }
    var online = Ti.Network.online
    info('Ti.Network.online ' + online);
    if (online === false) {
        // loadRoute(JSON.parse(Ti.App.Properties.getString('routeSaved')));
        // var trailId = Ti.App.Properties.getString('mapSaved');
        // var f = Ti.Filesystem.getFile(Ti.Filesystem.applicationDataDirectory, 'trail' + trailId + '.MBTiles');
        // if (f.exists()) {
        // info('loading trail ' + trailId);
        // onDownloadedTrail(trailId, f);
        // }
    } else {
        // loggin('pjallon@quantified-people.com', 'Hello001', function() {
        //     getTrailsList(function(_trail) {
        //         downloadTrail(_trail.id);
        //     });
        // });
    }
    var test = Ti.Database.open('test.MBTiles');
    var file = Ti.Filesystem.getFile(Ti.Filesystem.resourcesDirectory,
        "fWD3wU1eWWqugvvIcw0FV9sMNyO0ewrGsWtNndDQVh8%3D.mbtiles");
    var source = Map.createTileSource({
        source: "fWD3wU1eWWqugvvIcw0FV9sMNyO0ewrGsWtNndDQVh8%3D.mbtiles"
    });

    // map.addTileSource(source);
    var route = Map.createRoute({
        width: 4,
        points: JSON.parse(
            "[[45.264896392822266, 5.840264320373535, 457],[45.265533447265625, 5.840564727783203, 464],[45.26759719848633, 5.841236591339111, 484],[45.2693977355957, 5.841233253479004, 500],[45.270694732666016, 5.841258049011231, 508],[45.271202087402344, 5.841386795043945, 526],[45.27193832397461, 5.843213558197022, 535],[45.272422790527344, 5.843866348266602, 529],[45.272945404052734, 5.844753742218018, 540],[45.274009704589844, 5.845562934875488, 552],[45.27434158325195, 5.846032619476318, 550],[45.2757453918457, 5.846815586090088, 564],[45.27626419067383, 5.847389698028565, 573],[45.27651596069336, 5.84733772277832, 578],[45.276084899902344, 5.846659183502197, 597],[45.27491760253906, 5.8450927734375, 603],[45.274269104003906, 5.844414234161377, 622],[45.27383804321289, 5.844179630279541, 598],[45.27322769165039, 5.843239784240723, 611],[45.2729606628418, 5.841543197631836, 629],[45.27324676513672, 5.840394973754883, 633],[45.273193359375, 5.839663982391357, 633],[45.27387619018555, 5.837837219238281, 663],[45.274322509765625, 5.836975574493408, 695],[45.274322509765625, 5.835879325866699, 679],[45.27444839477539, 5.835983753204346, 687],[45.274593353271484, 5.836871147155762, 704],[45.274234771728516, 5.837784767150879, 704],[45.27405548095703, 5.83812427520752, 674],[45.273963928222656, 5.839037895202637, 682],[45.27412796020508, 5.839507579803467, 681],[45.274898529052734, 5.839742183685303, 714],[45.27493667602539, 5.840786457061768, 708],[45.2760124206543, 5.842169761657715, 736],[45.27561950683594, 5.840603828430176, 735],[45.27558135986328, 5.839925289154053, 744],[45.27499008178711, 5.838071823120117, 729],[45.27513122558594, 5.83781099319458, 759],[45.275672912597656, 5.838881015777588, 760],[45.27632141113281, 5.839272499084473, 781],[45.276824951171875, 5.840212345123291, 794],[45.27720260620117, 5.841282367706299, 788],[45.278011322021484, 5.841987133026123, 803],[45.27842712402344, 5.843239784240723, 785],[45.278568267822266, 5.8438401222229, 771],[45.278926849365234, 5.844336032867432, 759],[45.27896499633789, 5.845954418182373, 720],[45.27934265136719, 5.846841812133789, 758],[45.27988052368164, 5.847311496734619, 791],[45.28129196166992, 5.847233295440674, 920],[45.28209686279297, 5.848172664642334, 914],[45.28287124633789, 5.848642826080322, 894],[45.28378677368164, 5.84859037399292, 916],[45.28445053100586, 5.848616600036621, 907],[45.284793853759766, 5.848329544067383, 927],[45.28514099121094, 5.849003791809082, 902],[45.286041259765625, 5.849212646484375, 919],[45.286956787109375, 5.850413322448731, 920],[45.287837982177734, 5.851039409637451, 910],[45.28826904296875, 5.850987434387207, 918],[45.28855514526367, 5.849969387054443, 946],[45.28830337524414, 5.849734783172607, 949],[45.28803634643555, 5.850387096405029, 933],[45.287837982177734, 5.85054349899292, 930],[45.2872428894043, 5.850256443023682, 925],[45.287479400634766, 5.849525928497315, 949],[45.28767395019531, 5.848768711090088, 968],[45.28807067871094, 5.84816837310791, 979],[45.289058685302734, 5.847803115844727, 979],[45.28982162475586, 5.84722900390625, 991],[45.29051971435547, 5.847855567932129, 990],[45.290809631347656, 5.847933769226074, 992],[45.29098892211914, 5.847724914550781, 1002],[45.290771484375, 5.847307205200195, 999],[45.290340423583984, 5.846733093261719, 1002],[45.289676666259766, 5.846184730529785, 1009],[45.28958511352539, 5.845715045928955, 1023],[45.289981842041016, 5.845950126647949, 1015],[45.290592193603516, 5.845975875854492, 1021],[45.29109573364258, 5.846237182617188, 1025],[45.29167175292969, 5.845663070678711, 1041],[45.290863037109375, 5.845245361328125, 1039],[45.290592193603516, 5.844696998596191, 1046],[45.28933334350586, 5.843626976013184, 1061],[45.28874206542969, 5.843679428100586, 1063],[45.28791427612305, 5.843209266662598, 1082],[45.2889404296875, 5.842556953430176, 1082],[45.28953170776367, 5.842269897460938, 1094],[45.28883361816406, 5.842061042785645, 1096],[45.28927993774414, 5.841852188110352, 1099],[45.29104232788086, 5.84119987487793, 1123],[45.29163360595703, 5.84119987487793, 1126],[45.292301177978516, 5.841434478759766, 1135],[45.29334259033203, 5.841304302215576, 1155],[45.2938346862793, 5.841304302215576, 1157],[45.29433822631836, 5.840991020202637, 1171],[45.2947883605957, 5.84135627746582, 1165],[45.29570388793945, 5.84135627746582, 1182],[45.29640579223633, 5.842400550842285, 1182],[45.296836853027344, 5.842661380767822, 1194],[45.29759216308594, 5.842844009399414, 1221],[45.29689025878906, 5.843914031982422, 1209],[45.296783447265625, 5.844253540039063, 1215],[45.29631423950195, 5.844827651977539, 1207],[45.29613494873047, 5.845192909240723, 1209],[45.29629898071289, 5.845558643341065, 1212],[45.296836853027344, 5.844801425933838, 1226],[45.29793167114258, 5.843705177307129, 1243],[45.29890060424805, 5.842948436737061, 1259],[45.299278259277344, 5.842400550842285, 1264],[45.29994583129883, 5.841878414154053, 1273],[45.30012512207031, 5.841460704803467, 1286],[45.299442291259766, 5.840442657470703, 1298],[45.300140380859375, 5.840860366821289, 1291],[45.301815032958984, 5.840495109558106, 1327],[45.30223083496094, 5.841042995452881, 1333],[45.30224609375, 5.840755939483643, 1333],[45.30188751220703, 5.839816570281982, 1345],[45.301170349121094, 5.839398860931397, 1350],[45.30204772949219, 5.83929443359375, 1357],[45.30287551879883, 5.839999198913574, 1362],[45.30345153808594, 5.840442657470703, 1373],[45.30332565307617, 5.839999198913574, 1368],[45.30312728881836, 5.839138031005859, 1389],[45.30345153808594, 5.839425086975098, 1386],[45.303955078125, 5.840103626251221, 1381],[45.30466842651367, 5.840312480926514, 1396],[45.304935455322266, 5.840677738189697, 1399],[45.3051872253418, 5.840755939483643, 1408],[45.305171966552734, 5.839972972869873, 1415],[45.30500793457031, 5.839503288269043, 1424],[45.30452346801758, 5.839138031005859, 1419],[45.30378723144531, 5.8378586769104, 1436],[45.30326461791992, 5.83770227432251, 1430],[45.30231475830078, 5.836841106414795, 1444],[45.3017463684082, 5.837028503417969, 1445],[45.30122375488281, 5.836897850036621, 1456],[45.3005256652832, 5.837132930755615, 1461],[45.2998046875, 5.837237358093262, 1458],[45.29885482788086, 5.836793422698975, 1461],[45.29800796508789, 5.836480140686035, 1480],[45.297489166259766, 5.835984230041504, 1477],[45.2967529296875, 5.834757804870606, 1505],[45.29638671875, 5.834392070770264, 1491],[45.29582977294922, 5.83449649810791, 1466],[45.29520034790039, 5.834131240844727, 1459],[45.29465866088867, 5.834757804870606, 1429],[45.294517517089844, 5.835175514221191, 1421],[45.29435348510742, 5.835436344146729, 1419],[45.29464340209961, 5.835540771484375, 1419],[45.29492950439453, 5.835488319396973, 1420],[45.29381561279297, 5.836558818817139, 1390],[45.29340362548828, 5.836688995361328, 1375],[45.293060302734375, 5.836480140686035, 1366],[45.29255676269531, 5.836610794067383, 1344],[45.292259216308594, 5.836349964141846, 1333],[45.291542053222656, 5.836454391479492, 1310],[45.290443420410156, 5.83608865737915, 1280],[45.2894401550293, 5.836532592773438, 1249],[45.28926086425781, 5.836793422698975, 1243],[45.288631439208984, 5.837106704711914, 1224],[45.288307189941406, 5.837628841400147, 1211],[45.287837982177734, 5.837733268737793, 1207],[45.287445068359375, 5.8375244140625, 1207],[45.28744125366211, 5.83705472946167, 1193],[45.28799819946289, 5.836558818817139, 1196],[45.28833770751953, 5.835827827453613, 1194],[45.28893280029297, 5.83546257019043, 1198],[45.28916549682617, 5.834757804870606, 1166],[45.288734436035156, 5.835227489471436, 1188],[45.28832244873047, 5.835305690765381, 1157],[45.288841247558594, 5.834209442138672, 1145],[45.288211822509766, 5.834862232208252, 1130],[45.28841018676758, 5.834288120269775, 1127],[45.28876876831055, 5.83387041091919, 1105],[45.28797912597656, 5.834183692932129, 1103],[45.2880859375, 5.833817958831787, 1070],[45.2872428894043, 5.834418296813965, 1062],[45.28736877441406, 5.83387041091919, 1033],[45.287906646728516, 5.833243846893311, 1026],[45.287261962890625, 5.833687782287598, 1024],[45.28656005859375, 5.834079265594482, 1001],[45.28657913208008, 5.833583354949951, 983],[45.28691864013672, 5.833348274230957, 1006],[45.28727722167969, 5.832695960998535, 982],[45.28776550292969, 5.832278251647949, 977],[45.28797912597656, 5.831860542297363, 969],[45.287261962890625, 5.832460880279541, 957],[45.286163330078125, 5.833426475524902, 972],[45.28657913208008, 5.832826137542725, 946],[45.28677749633789, 5.83243465423584, 941],[45.28742218017578, 5.831860542297363, 942],[45.28754806518555, 5.831442832946777, 935],[45.286956787109375, 5.83196496963501, 935],[45.28575134277344, 5.832591533660889, 906],[45.2862548828125, 5.831886768341065, 902],[45.286720275878906, 5.831442832946777, 902],[45.286380767822266, 5.831051349639893, 873],[45.286991119384766, 5.831025123596191, 898],[45.286991119384766, 5.830555438995361, 890],[45.286128997802734, 5.830294609069824, 865],[45.28476333618164, 5.82990312576294, 837],[45.28364944458008, 5.829641819000244, 812],[45.282325744628906, 5.829484462738037, 774],[45.28153610229492, 5.829196929931641, 767],[45.28018569946289, 5.829745292663574, 735],[45.279415130615234, 5.828909873962402, 729],[45.278568267822266, 5.828909873962402, 709],[45.27799606323242, 5.828669548034668, 699],[45.277347564697266, 5.828826427459717, 689],[45.27659606933594, 5.829452514648438, 668],[45.27602005004883, 5.829661369323731, 661],[45.27470779418945, 5.829296112060547, 641],[45.27436828613281, 5.829243659973145, 638],[45.273719787597656, 5.829556941986084, 632],[45.27317428588867, 5.830105304718018, 621],[45.27286911010742, 5.830183506011963, 620],[45.27263641357422, 5.830235481262207, 621],[45.27229690551758, 5.831175327301025, 600],[45.27200698852539, 5.831827640533447, 588],[45.27155685424805, 5.832480430603027, 576],[45.271541595458984, 5.833289623260498, 574],[45.272438049316406, 5.833263397216797, 588],[45.27339172363281, 5.833315372467041, 605],[45.27283477783203, 5.833863735198975, 595],[45.2713623046875, 5.834503173828125, 569],[45.27050018310547, 5.835351467132568, 554],[45.269832611083984, 5.835729598999023, 544],[45.269500732421875, 5.83591890335083, 537],[45.26832962036133, 5.837162017822266, 513],[45.26729965209961, 5.838170051574707, 497],[45.26627731323242, 5.83884859085083, 483],[45.265892028808594, 5.839488029479981, 477],[45.265445709228516, 5.83988618850708, 468]]"
        )
    });
    // map.addRoute(route);
    // map.scrollableAreaLimit = source.region;
    // map.region = route.region;

    // downloadTile(1325);

    map.addEventListener('click', function(e) {
        Ti.API.info(stringify(e));
    });

    map.addEventListener('doubletap', function(e) {
        if (currentRoute) {
            map.region = currentRoute.region;
        }
    });

    map.addEventListener('longpress', function(e) {
        Ti.API.info(stringify(e));
        map.zoomOut(e);
    });

    var followUser = true;

    function onLocation(e) {
        info(stringify(e));
        if (!e.coords || e.success === false || e.error) {
            return;
        }
        // if (following) {
        if (followUser) {
            followUser = false;
            // map.applyProperties({
            // zoom: 17,
            // userTrackingMode: 1
            // });
        }
        // if (recordingRoute) {
        // recordingRoute.addPoint(e.coords);
        // }
        // }

    }
    Titanium.Geolocation.addEventListener('location', onLocation);
    // Titanium.Geolocation.removeEventListener('location', onLocation);
    // win.addEventListener('close', function(){
    // info('close');
    // Titanium.Geolocation.removeEventListener('location', onLocation);
    // });

    openWin(win);
}

function initGeoSettings() {
    app.location = {};
    Titanium.Geolocation.purpose = 'suggest_a_move';
    if (__APPLE__) {
        Titanium.Geolocation.showCalibration = false;
        Titanium.Geolocation.accuracy = Titanium.Geolocation.ACCURACY_LOW;
        Titanium.Geolocation.distanceFilter = 100;
    } else if (__ANDROID__) {
        try {
            Titanium.Geolocation.Android.manualMode = true;

            var networkRule = Ti.Geolocation.Android.createLocationRule({
                provider: Ti.Geolocation.Android.PROVIDER_NETWORK,
                // Updates should be accurate to 100m
                // accuracy: 100,
                minDistance: 100,
                // Updates should be no older than 5m
                maxAge: 300000,
                // But  no more frequent than once per 10 seconds
                minAge: 10000
            });
            Ti.Geolocation.Android.addLocationRule(networkRule);

            app.location.gpsSFullRule = Ti.Geolocation.Android.createLocationRule({
                provider: Ti.Geolocation.Android.PROVIDER_GPS,
                // Updates should be accurate to 1m
                minDistance: 1,
                // Updates should be no older than 5m
                maxAge: 300000
            });
            app.location.gpsSSlowRule = Ti.Geolocation.Android.createLocationRule({
                provider: Ti.Geolocation.Android.PROVIDER_GPS,
                // Updates should be accurate to 1m
                minDistance: 100,
                // Updates should be no older than 5m
                maxAge: 300000,
                // But  no more frequent than once per 10 seconds
                minAge: 10000
            });
            Ti.Geolocation.Android.addLocationRule(app.location.gpsSSlowRule);

            Ti.Geolocation.Android.addLocationProvider(Ti.Geolocation.Android.createLocationProvider({
                name: Ti.Geolocation.Android.PROVIDER_PASSIVE
            }));
            Ti.Geolocation.Android.addLocationProvider(Ti.Geolocation.Android.createLocationProvider({
                name: Ti.Geolocation.Android.PROVIDER_GPS
            }));
            Ti.Geolocation.Android.addLocationProvider(Ti.Geolocation.Android.createLocationProvider({
                name: Ti.Geolocation.Android.PROVIDER_NETWORK
            }));
        } catch (e) {}

    }
    app.location.setFullGPS = function() {
        if (__APPLE__) {
            Titanium.Geolocation.accuracy = Titanium.Geolocation.ACCURACY_HIGH;
            Titanium.Geolocation.distanceFilter = 1;
        } else if (__ANDROID__) {
            Ti.Geolocation.Android.removeLocationRule(app.location.gpsSSlowRule);
            Ti.Geolocation.Android.addLocationRule(app.location.gpsSFullRule);
        }
    };
    app.location.setSlowGPS = function() {
        if (__APPLE__) {
            Titanium.Geolocation.accuracy = Titanium.Geolocation.ACCURACY_LOW;
            Titanium.Geolocation.distanceFilter = 100;
        } else if (__ANDROID__) {
            Ti.Geolocation.Android.removeLocationRule(app.location.gpsSFullRule);
            Ti.Geolocation.Android.addLocationRule(app.location.gpsSSlowRule);
        }
    };
}

function mapEx(_args) {
    var win = createWin(_args);
    var opera = Map.createAnnotation({
        latitude: -33.8569,
        longitude: 151.2153,
        image: 'SydneyOperaHouse.jpg',
        title: 'Sydney Opera House',
        subtitle: 'Sydney, New South Wales, Australia'
    });

    var bridge = Map.createAnnotation({
        latitude: -33.852222,
        longitude: 151.210556,
        pincolor: Map.ANNOTATION_AZURE,
        // Even though we are creating a button, it does not respond to Button events or animates.
        // Use the Map View's click event and monitor the clicksource property for 'leftPane'.
        leftView: Ti.UI.createButton({
            title: 'Detail'
        }),
        // For eventing, use the Map View's click event
        // and monitor the clicksource property for 'rightPane'.
        rightButton: 'SydneyHarbourBridge.jpg',
        title: 'Sydney Harbour Bridge',
        subtitle: 'Port Jackson'
    });

    var random = Map.createAnnotation({
        latitude: -33.87365,
        longitude: 151.20689,
        pincolor: Map.ANNOTATION_VIOLET,
        // Even though we are creating a label, it does not respond to Label events.
        // Use the Map View's events instead.
        customView: Ti.UI.createLabel({
            text: 'MOVE ME!',
            opacity: '80%',
            color: 'red',
            backgroundColor: 'gray',
            font: {
                fontSize: '16dp',
                fontWeight: 'bold'
            }
        }),
        draggable: true
    });

    var mapview = Map.createMapView({
        mapType: Map.NORMAL_TYPE,
        region: {
            latitude: -33.87365,
            longitude: 151.20689,
            latitudeDelta: 0.1,
            longitudeDelta: 0.1
        },
        annotations: [bridge, opera] //< add these annotations upon creation
    });

    // Add this annotation after creation
    mapview.addAnnotation(random);
    win.add(mapview);
    openWin(win);
}