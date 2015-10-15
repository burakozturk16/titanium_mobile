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

var movies = JSON.parse(
    '[{"backdrop_path ":"http: //image.tmdb.org/t/p/w500/cKw3HY835PMp6bzse3LMivIY5Nl.jpg","id":1884,"original_title":"The Ewok Adventure","release_date":"1984-11-25","poster_path":"https://image.tmdb.org/t/p/w154/x2nKP0FCJwNLHgCyUI1cL8bF6nL.jpg","popularity":0.72905031478,"title":"The Ewok Adventure","vote_average":10,"vote_count":4},{"backdrop_path":"http://image.tmdb.org/t/p/w500/s0LjxgmzKdTny2CiuHNBjV8urmf.jpg","id":1537,"original_title":"Changing Lanes","release_date":"2002-04-07","poster_path":"https://image.tmdb.org/t/p/w154/bbINOtOFyQFAtaKo0fdMKXB1Og5.jpg","popularity":1.1399252536936,"title":"Changing Lanes","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/39R1ItA8srBv07z3ZG6MUA3uD2p.jpg","id":1672,"original_title":"Le Professionnel","release_date":"1981-10-21","poster_path":"https://image.tmdb.org/t/p/w154/A7Yjc5a1DTvijtaQG08h8ufDQKA.jpg","popularity":1.55987,"title":"The Professional","vote_average":10,"vote_count":2},{"backdrop_path":"http://image.tmdb.org/t/p/w500/7dJLUyCSlYbz822I7FGRrql7gpB.jpg","id":8427,"original_title":"I Spy","release_date":"2002-10-31","poster_path":"https://image.tmdb.org/t/p/w154/22nUxAkL42Snd8azTlUrpDDtBSh.jpg","popularity":1.06413962734445,"title":"I Spy","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/rUPLEQWvCsYYj8z4vPx1QnezDX5.jpg","id":16364,"original_title":"Robotech: The Shadow Chronicles","release_date":"2007-09-19","poster_path":"https://image.tmdb.org/t/p/w154/eFRVAKXc31AbsbIJ6JL0QuWTgPe.jpg","popularity":1.152820462,"title":"Robotech: The Shadow Chronicles","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/oiix1SXcy7i3MitqVJor1o9ZSUv.jpg","id":17264,"original_title":"The Black Stallion","release_date":"1979-10-17","poster_path":"https://image.tmdb.org/t/p/w154/hznqiRVJqwfg0rSMulyRSpXuvGb.jpg","popularity":0.582087308,"title":"The Black Stallion","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/q6CLnjffLrUkfjOuIv3vbhYGAwN.jpg","id":11176,"original_title":"The Muppet Movie","release_date":"1979-05-31","poster_path":"https://image.tmdb.org/t/p/w154/48Ve7uLDcPJFGaDnmYYdcV3Ve1M.jpg","popularity":0.834,"title":"The Muppet Movie","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/oY5ujxy7jJ8hiHXO8FA39JAKjiy.jpg","id":10339,"original_title":"Moby Dick","release_date":"1956-06-27","poster_path":"https://image.tmdb.org/t/p/w154/i8u9Gw4EDESLiC902bDQ9MuWg6O.jpg","popularity":0.58,"title":"Moby Dick","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/nQx2Ks3Sl82BygE71BgyaHFlo1a.jpg","id":10109,"original_title":"Fei hap siu baak lung","release_date":"2004-10-28","poster_path":"https://image.tmdb.org/t/p/w154/JPI3wpmRqYaqv7ey3QnjaVb0ZW.jpg","popularity":0.44,"title":"The White Dragon","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/fWh5gHBl6e9W0bIGEcEaAqgsj5.jpg","id":11570,"original_title":"The Crimson Pirate","release_date":"1952-09-27","poster_path":"https://image.tmdb.org/t/p/w154/aysnvyXlpqiEPkCn1bbiBqZIgCg.jpg","popularity":0.4322,"title":"The Crimson Pirate","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/nwhHlNlvYj589KpOiX0pzVbbkf6.jpg","id":11616,"original_title":"Nati con la camicia","release_date":"1982-12-31","poster_path":"https://image.tmdb.org/t/p/w154/gmb1nA58ZH12cGUFMBoR6UDDb0j.jpg","popularity":1.001322002,"title":"Go for It","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/nBz9PJerPhqghjqObFRW0PnaOyA.jpg","id":12097,"original_title":"Pippi Långstrump","release_date":"1969-05-09","poster_path":"https://image.tmdb.org/t/p/w154/dQwrHaxbM3eAoQQgZakzHKmj8Xh.jpg","popularity":0.545992,"title":"Pippi Longstocking","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/7iQoI61oUOr8tiphzuE4dvUZLCE.jpg","id":11385,"original_title":"Hatari!","release_date":"1962-06-19","poster_path":"https://image.tmdb.org/t/p/w154/mJwf9AvwjJaopgK8AkQt6dW6Nh.jpg","popularity":0.98,"title":"Hatari!","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/ivm5T3xOGgG3Tvb8m4ExLBb7wsW.jpg","id":11818,"original_title":"Kurtlar vadisi - Irak","release_date":"2006-02-03","poster_path":"https://image.tmdb.org/t/p/w154/o1q1Q1R2Bf21NlfzTJiP7Su127X.jpg","popularity":0.92,"title":"Valley of the Wolves: Iraq","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/sqIDSVlWjupKDPm37vC5mURWETw.jpg","id":13274,"original_title":"ラストオーダー -ファイナルファンタジーVII-","release_date":"2005-09-13","poster_path":"https://image.tmdb.org/t/p/w154/s45WPYo7K19CdtjcMiNFORT4oRN.jpg","popularity":0.2,"title":"Last Order: Final Fantasy VII","vote_average":10,"vote_count":2},{"backdrop_path":"http://image.tmdb.org/t/p/w500/fKj7papXZq1dxnvfnxrydlsbbzt.jpg","id":13938,"original_title":"The Last Dragon","release_date":"1985-03-22","poster_path":"https://image.tmdb.org/t/p/w154/RZQF0DGwlC0XTYNKjNSjpkSlQn.jpg","popularity":0.2,"title":"The Last Dragon","vote_average":10,"vote_count":2},{"backdrop_path":"http://image.tmdb.org/t/p/w500/j05QW2sgHG5y3Kru1gxSxAyHNoM.jpg","id":14597,"original_title":"Lassie","release_date":"2005-12-16","poster_path":"https://image.tmdb.org/t/p/w154/zJOqLMWKKUxWVjfqNnuyO6BRu4r.jpg","popularity":1.332,"title":"Lassie","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/c0i7MxnHADv9MAiEtl7mHCqcNd4.jpg","id":15012,"original_title":"Linewatch","release_date":"2008-01-01","poster_path":"https://image.tmdb.org/t/p/w154/nV99lWYgqyyGn61dIHt4LrZbXWY.jpg","popularity":0.711408006785931,"title":"Linewatch","vote_average":10,"vote_count":1},{"backdrop_path":null,"id":18497,"original_title":"Exils","release_date":"2004-05-19","poster_path":"https://image.tmdb.org/t/p/w154/cyBz3PzzmkUnjOq8XbIq0NxlX9i.jpg","popularity":0.2,"title":"Exiles","vote_average":10,"vote_count":1},{"backdrop_path":null,"id":23191,"original_title":"The 9 Ball Diaries","release_date":"2008-10-07","poster_path":"https://image.tmdb.org/t/p/w154/wyDPW7A7l31xH7FVMNQRRFVT1aa.jpg","popularity":0.4,"title":"The 9 Ball Diaries","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/cKw3HY835PMp6bzse3LMivIY5Nl.jpg","id":1884,"original_title":"The Ewok Adventure","release_date":"1984-11-25","poster_path":"https://image.tmdb.org/t/p/w154/x2nKP0FCJwNLHgCyUI1cL8bF6nL.jpg","popularity":0.72905031478,"title":"The Ewok Adventure","vote_average":10,"vote_count":4},{"backdrop_path":"http://image.tmdb.org/t/p/w500/s0LjxgmzKdTny2CiuHNBjV8urmf.jpg","id":1537,"original_title":"Changing Lanes","release_date":"2002-04-07","poster_path":"https://image.tmdb.org/t/p/w154/bbINOtOFyQFAtaKo0fdMKXB1Og5.jpg","popularity":1.1399252536936,"title":"Changing Lanes","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/39R1ItA8srBv07z3ZG6MUA3uD2p.jpg","id":1672,"original_title":"Le Professionnel","release_date":"1981-10-21","poster_path":"https://image.tmdb.org/t/p/w154/A7Yjc5a1DTvijtaQG08h8ufDQKA.jpg","popularity":1.55987,"title":"The Professional","vote_average":10,"vote_count":2},{"backdrop_path":"http://image.tmdb.org/t/p/w500/7dJLUyCSlYbz822I7FGRrql7gpB.jpg","id":8427,"original_title":"I Spy","release_date":"2002-10-31","poster_path":"https://image.tmdb.org/t/p/w154/22nUxAkL42Snd8azTlUrpDDtBSh.jpg","popularity":1.06413962734445,"title":"I Spy","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/rUPLEQWvCsYYj8z4vPx1QnezDX5.jpg","id":16364,"original_title":"Robotech: The Shadow Chronicles","release_date":"2007-09-19","poster_path":"https://image.tmdb.org/t/p/w154/eFRVAKXc31AbsbIJ6JL0QuWTgPe.jpg","popularity":1.152820462,"title":"Robotech: The Shadow Chronicles","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/oiix1SXcy7i3MitqVJor1o9ZSUv.jpg","id":17264,"original_title":"The Black Stallion","release_date":"1979-10-17","poster_path":"https://image.tmdb.org/t/p/w154/hznqiRVJqwfg0rSMulyRSpXuvGb.jpg","popularity":0.582087308,"title":"The Black Stallion","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/q6CLnjffLrUkfjOuIv3vbhYGAwN.jpg","id":11176,"original_title":"The Muppet Movie","release_date":"1979-05-31","poster_path":"https://image.tmdb.org/t/p/w154/48Ve7uLDcPJFGaDnmYYdcV3Ve1M.jpg","popularity":0.834,"title":"The Muppet Movie","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/oY5ujxy7jJ8hiHXO8FA39JAKjiy.jpg","id":10339,"original_title":"Moby Dick","release_date":"1956-06-27","poster_path":"https://image.tmdb.org/t/p/w154/i8u9Gw4EDESLiC902bDQ9MuWg6O.jpg","popularity":0.58,"title":"Moby Dick","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/nQx2Ks3Sl82BygE71BgyaHFlo1a.jpg","id":10109,"original_title":"Fei hap siu baak lung","release_date":"2004-10-28","poster_path":"https://image.tmdb.org/t/p/w154/JPI3wpmRqYaqv7ey3QnjaVb0ZW.jpg","popularity":0.44,"title":"The White Dragon","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/fWh5gHBl6e9W0bIGEcEaAqgsj5.jpg","id":11570,"original_title":"The Crimson Pirate","release_date":"1952-09-27","poster_path":"https://image.tmdb.org/t/p/w154/aysnvyXlpqiEPkCn1bbiBqZIgCg.jpg","popularity":0.4322,"title":"The Crimson Pirate","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/nwhHlNlvYj589KpOiX0pzVbbkf6.jpg","id":11616,"original_title":"Nati con la camicia","release_date":"1982-12-31","poster_path":"https://image.tmdb.org/t/p/w154/gmb1nA58ZH12cGUFMBoR6UDDb0j.jpg","popularity":1.001322002,"title":"Go for It","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/nBz9PJerPhqghjqObFRW0PnaOyA.jpg","id":12097,"original_title":"Pippi Långstrump","release_date":"1969-05-09","poster_path":"https://image.tmdb.org/t/p/w154/dQwrHaxbM3eAoQQgZakzHKmj8Xh.jpg","popularity":0.545992,"title":"Pippi Longstocking","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/7iQoI61oUOr8tiphzuE4dvUZLCE.jpg","id":11385,"original_title":"Hatari!","release_date":"1962-06-19","poster_path":"https://image.tmdb.org/t/p/w154/mJwf9AvwjJaopgK8AkQt6dW6Nh.jpg","popularity":0.98,"title":"Hatari!","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/ivm5T3xOGgG3Tvb8m4ExLBb7wsW.jpg","id":11818,"original_title":"Kurtlar vadisi - Irak","release_date":"2006-02-03","poster_path":"https://image.tmdb.org/t/p/w154/o1q1Q1R2Bf21NlfzTJiP7Su127X.jpg","popularity":0.92,"title":"Valley of the Wolves: Iraq","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/sqIDSVlWjupKDPm37vC5mURWETw.jpg","id":13274,"original_title":"ラストオーダー -ファイナルファンタジーVII-","release_date":"2005-09-13","poster_path":"https://image.tmdb.org/t/p/w154/s45WPYo7K19CdtjcMiNFORT4oRN.jpg","popularity":0.2,"title":"Last Order: Final Fantasy VII","vote_average":10,"vote_count":2},{"backdrop_path":"http://image.tmdb.org/t/p/w500/fKj7papXZq1dxnvfnxrydlsbbzt.jpg","id":13938,"original_title":"The Last Dragon","release_date":"1985-03-22","poster_path":"https://image.tmdb.org/t/p/w154/RZQF0DGwlC0XTYNKjNSjpkSlQn.jpg","popularity":0.2,"title":"The Last Dragon","vote_average":10,"vote_count":2},{"backdrop_path":"http://image.tmdb.org/t/p/w500/j05QW2sgHG5y3Kru1gxSxAyHNoM.jpg","id":14597,"original_title":"Lassie","release_date":"2005-12-16","poster_path":"https://image.tmdb.org/t/p/w154/zJOqLMWKKUxWVjfqNnuyO6BRu4r.jpg","popularity":1.332,"title":"Lassie","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/c0i7MxnHADv9MAiEtl7mHCqcNd4.jpg","id":15012,"original_title":"Linewatch","release_date":"2008-01-01","poster_path":"https://image.tmdb.org/t/p/w154/nV99lWYgqyyGn61dIHt4LrZbXWY.jpg","popularity":0.711408006785931,"title":"Linewatch","vote_average":10,"vote_count":1},{"backdrop_path":null,"id":18497,"original_title":"Exils","release_date":"2004-05-19","poster_path":"https://image.tmdb.org/t/p/w154/cyBz3PzzmkUnjOq8XbIq0NxlX9i.jpg","popularity":0.2,"title":"Exiles","vote_average":10,"vote_count":1},{"backdrop_path":null,"id":23191,"original_title":"The 9 Ball Diaries","release_date":"2008-10-07","poster_path":"https://image.tmdb.org/t/p/w154/wyDPW7A7l31xH7FVMNQRRFVT1aa.jpg","popularity":0.4,"title":"The 9 Ball Diaries","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/cKw3HY835PMp6bzse3LMivIY5Nl.jpg","id":1884,"original_title":"The Ewok Adventure","release_date":"1984-11-25","poster_path":"https://image.tmdb.org/t/p/w154/x2nKP0FCJwNLHgCyUI1cL8bF6nL.jpg","popularity":0.72905031478,"title":"The Ewok Adventure","vote_average":10,"vote_count":4},{"backdrop_path":"http://image.tmdb.org/t/p/w500/s0LjxgmzKdTny2CiuHNBjV8urmf.jpg","id":1537,"original_title":"Changing Lanes","release_date":"2002-04-07","poster_path":"https://image.tmdb.org/t/p/w154/bbINOtOFyQFAtaKo0fdMKXB1Og5.jpg","popularity":1.1399252536936,"title":"Changing Lanes","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/39R1ItA8srBv07z3ZG6MUA3uD2p.jpg","id":1672,"original_title":"Le Professionnel","release_date":"1981-10-21","poster_path":"https://image.tmdb.org/t/p/w154/A7Yjc5a1DTvijtaQG08h8ufDQKA.jpg","popularity":1.55987,"title":"The Professional","vote_average":10,"vote_count":2},{"backdrop_path":"http://image.tmdb.org/t/p/w500/7dJLUyCSlYbz822I7FGRrql7gpB.jpg","id":8427,"original_title":"I Spy","release_date":"2002-10-31","poster_path":"https://image.tmdb.org/t/p/w154/22nUxAkL42Snd8azTlUrpDDtBSh.jpg","popularity":1.06413962734445,"title":"I Spy","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/rUPLEQWvCsYYj8z4vPx1QnezDX5.jpg","id":16364,"original_title":"Robotech: The Shadow Chronicles","release_date":"2007-09-19","poster_path":"https://image.tmdb.org/t/p/w154/eFRVAKXc31AbsbIJ6JL0QuWTgPe.jpg","popularity":1.152820462,"title":"Robotech: The Shadow Chronicles","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/oiix1SXcy7i3MitqVJor1o9ZSUv.jpg","id":17264,"original_title":"The Black Stallion","release_date":"1979-10-17","poster_path":"https://image.tmdb.org/t/p/w154/hznqiRVJqwfg0rSMulyRSpXuvGb.jpg","popularity":0.582087308,"title":"The Black Stallion","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/q6CLnjffLrUkfjOuIv3vbhYGAwN.jpg","id":11176,"original_title":"The Muppet Movie","release_date":"1979-05-31","poster_path":"https://image.tmdb.org/t/p/w154/48Ve7uLDcPJFGaDnmYYdcV3Ve1M.jpg","popularity":0.834,"title":"The Muppet Movie","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/oY5ujxy7jJ8hiHXO8FA39JAKjiy.jpg","id":10339,"original_title":"Moby Dick","release_date":"1956-06-27","poster_path":"https://image.tmdb.org/t/p/w154/i8u9Gw4EDESLiC902bDQ9MuWg6O.jpg","popularity":0.58,"title":"Moby Dick","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/nQx2Ks3Sl82BygE71BgyaHFlo1a.jpg","id":10109,"original_title":"Fei hap siu baak lung","release_date":"2004-10-28","poster_path":"https://image.tmdb.org/t/p/w154/JPI3wpmRqYaqv7ey3QnjaVb0ZW.jpg","popularity":0.44,"title":"The White Dragon","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/fWh5gHBl6e9W0bIGEcEaAqgsj5.jpg","id":11570,"original_title":"The Crimson Pirate","release_date":"1952-09-27","poster_path":"https://image.tmdb.org/t/p/w154/aysnvyXlpqiEPkCn1bbiBqZIgCg.jpg","popularity":0.4322,"title":"The Crimson Pirate","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/nwhHlNlvYj589KpOiX0pzVbbkf6.jpg","id":11616,"original_title":"Nati con la camicia","release_date":"1982-12-31","poster_path":"https://image.tmdb.org/t/p/w154/gmb1nA58ZH12cGUFMBoR6UDDb0j.jpg","popularity":1.001322002,"title":"Go for It","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/nBz9PJerPhqghjqObFRW0PnaOyA.jpg","id":12097,"original_title":"Pippi Långstrump","release_date":"1969-05-09","poster_path":"https://image.tmdb.org/t/p/w154/dQwrHaxbM3eAoQQgZakzHKmj8Xh.jpg","popularity":0.545992,"title":"Pippi Longstocking","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/7iQoI61oUOr8tiphzuE4dvUZLCE.jpg","id":11385,"original_title":"Hatari!","release_date":"1962-06-19","poster_path":"https://image.tmdb.org/t/p/w154/mJwf9AvwjJaopgK8AkQt6dW6Nh.jpg","popularity":0.98,"title":"Hatari!","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/ivm5T3xOGgG3Tvb8m4ExLBb7wsW.jpg","id":11818,"original_title":"Kurtlar vadisi - Irak","release_date":"2006-02-03","poster_path":"https://image.tmdb.org/t/p/w154/o1q1Q1R2Bf21NlfzTJiP7Su127X.jpg","popularity":0.92,"title":"Valley of the Wolves: Iraq","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/sqIDSVlWjupKDPm37vC5mURWETw.jpg","id":13274,"original_title":"ラストオーダー -ファイナルファンタジーVII-","release_date":"2005-09-13","poster_path":"https://image.tmdb.org/t/p/w154/s45WPYo7K19CdtjcMiNFORT4oRN.jpg","popularity":0.2,"title":"Last Order: Final Fantasy VII","vote_average":10,"vote_count":2},{"backdrop_path":"http://image.tmdb.org/t/p/w500/fKj7papXZq1dxnvfnxrydlsbbzt.jpg","id":13938,"original_title":"The Last Dragon","release_date":"1985-03-22","poster_path":"https://image.tmdb.org/t/p/w154/RZQF0DGwlC0XTYNKjNSjpkSlQn.jpg","popularity":0.2,"title":"The Last Dragon","vote_average":10,"vote_count":2},{"backdrop_path":"http://image.tmdb.org/t/p/w500/j05QW2sgHG5y3Kru1gxSxAyHNoM.jpg","id":14597,"original_title":"Lassie","release_date":"2005-12-16","poster_path":"https://image.tmdb.org/t/p/w154/zJOqLMWKKUxWVjfqNnuyO6BRu4r.jpg","popularity":1.332,"title":"Lassie","vote_average":10,"vote_count":1},{"backdrop_path":"http://image.tmdb.org/t/p/w500/c0i7MxnHADv9MAiEtl7mHCqcNd4.jpg","id":15012,"original_title":"Linewatch","release_date":"2008-01-01","poster_path":"https://image.tmdb.org/t/p/w154/nV99lWYgqyyGn61dIHt4LrZbXWY.jpg","popularity":0.711408006785931,"title":"Linewatch","vote_average":10,"vote_count":1},{"backdrop_path":null,"id":18497,"original_title":"Exils","release_date":"2004-05-19","poster_path":"https://image.tmdb.org/t/p/w154/cyBz3PzzmkUnjOq8XbIq0NxlX9i.jpg","popularity":0.2,"title":"Exiles","vote_average":10,"vote_count":1},{"backdrop_path":null,"id":23191,"original_title":"The 9 Ball Diaries","release_date":"2008-10-07","poster_path":"https://image.tmdb.org/t/p/w154/wyDPW7A7l31xH7FVMNQRRFVT1aa.jpg","popularity":0.4,"title":"The 9 Ball Diaries","vote_average":10,"vote_count":1}]'
);

function listViewExs(_args) {
    var win = createWin(_.assign(_args, {
        title: 'listviews',
        fullscreen: true,
        // barColor:'blue',
        layout: 'vertical',
        backgroundImage: Ti.Image.getFilteredScreenshot({
            scale: 0.6,
            filters: [{
                radius: 8,
                type: Ti.Image.FILTER_IOS_BLUR
            }]
        })
    }));
    var listview = createListView({
        updateInsetWithKeyboard: true
    });
    listview.sections = [{
        headerView: {
            type: 'Ti.UI.TextField',
            properties: {
                backgroundColor: 'red',
                width: 'FILL',
                bottom: 20,
                hintText: 'HeaderView created from Dict'
            }
        },
        items: [{
            properties: {
                title: 'Long List'
            },
            callback: longListTest
        }, {
            properties: {
                title: 'Animation'
            },
            callback: listViewExAnim
        }, {
            properties: {
                title: 'CollectionInsideListView'
            },
            callback: collectionInsideListViewEx
        }, {
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
        }, {
            properties: {
                title: 'TextFields'
            },
            callback: listViewEx5
        }, {
            properties: {
                title: 'DeepLayout'
            },
            callback: deepLayoutTest
        }, {
            properties: {
                title: 'AutoSize'
            },
            callback: autoSizeEx
        }, {
            properties: {
                title: 'PullToRefresh'
            },
            callback: pullToRefresh
        }, {
            properties: {
                title: 'CollectionView'
            },
            callback: collectionViewEx
        }]
    }];
    win.add({
        type: 'Ti.UI.TextField',
        properties: {
            height: 40,
            width: 'FILL',
            backgroundColor: 'gray',
            bottom: 30
        }
    });
    win.add(listview);
    openWin(win);
}

function listViewEx1(_args) {
    var titleTest = ' Article title';
    var descriptionTest = ' This is a description text hopping it s going to hold on at least 2 lines';
    var win = createWin();
    var listview = Ti.UI.createListView({
        selectedBackgroundColor: 'gray',
        defaultItemTemplate: 'default',
        allowsSelection: false,
        templates: {
            "default": {
                "properties": {
                    "height": 85,
                    "dispatchPressed": true,
                    // "borderColor": "black",
                    // "borderPadding": {
                    // "left": -1,
                    // "right": -1,
                    // "top": -1
                    // }
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
                            preventListViewSelection: true,
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
    var win = createWin({
        layout: 'vertical'
    });
    var template = {
        properties: {
            layout: 'horizontal',
            backgroundColor: 'orange',
            dispatchPressed: true,
            height: 40,
            // borderColor: 'blue'
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
                // borderColor: 'gray',
                // borderSelectedColor: 'red',
                // backgroundGradient: {
                // type: 'linear',
                // colors: [{
                // color: 'blue',
                // offset: 0.0
                // }, {
                // color: 'transparent',
                // offset: 0.2
                // }, {
                // color: 'transparent',
                // offset: 0.8
                // }, {
                // color: 'blue',
                // offset: 1
                // }],
                // startPoint: {
                // x: 0,
                // y: 0
                // },
                // endPoint: {
                // x: 0,
                // y: "100%"
                // }
                // },
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
                        // borderRadius: 4,
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
                        // borderRadius: 4,
                        clipChildren: false,
                        bottom: 0,
                        height: 16
                    },
                    childTemplates: [{
                        type: 'Ti.UI.View',
                        bindId: 'progressbar',
                        properties: {
                            // borderRadius: 4,
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

    var names = ['Carolyn Humbert', 'David Michaels', 'Rebecca Thorning', 'Joe B', 'Phillip Craig',
        'Michelle Werner',
        'Philippe Christophe', 'Marcus Crane', 'Esteban Valdez', 'Sarah Mullock'
    ];
    var priorities = ['downloading', 'success', 'failure', '', 'test', 'processing'];
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
        height: 'FILL',
        // minRowHeight: 40,
        // onDisplayCell: function(_args) {
        //     _args.view.opacity = 0;
        //     _args.view.animate({
        //         opacity: 1,
        //         duration: 250
        //     });
        // },
        defaultItemTemplate: 'template2'
    }, false);

    listView.templates = {
        'template': template,
        'template2': {
            "properties": {
                // "height": 75,
                // "borderPadding": {
                // "left": -1,
                // "right": -1,
                // "top": -1
                // },
                // "borderColor": "#DDDDDD",
                "backgroundColor": "white"
            },
            "childTemplates": [{
                "type": "Ti.UI.View",
                "properties": {
                    "width": 40,
                    "height": 40,
                    "left": 4
                },
                "childTemplates": [{
                    "type": "Ti.UI.Label",
                    "bindId": "check",
                    "properties": {
                        "color": "transparent",
                        "textAlign": "center",
                        "clipChildren": false,
                        "borderSelectedColor": "#0088CC",
                        "font": {
                            "family": "LigatureSymbols"
                        },
                        "text": "",
                        "width": 20,
                        "height": 20
                            // "borderRadius": 2,
                            // "borderColor": "#DDDDDD"
                    }
                }]
            }, {
                "type": "Ti.UI.Button",
                "bindId": "button",
                "properties": {
                    "width": 40,
                    "height": 40,
                    "left": 4,
                    "font": {
                        "family": "Simple-Line-Icons",
                        "size": 18,
                        "weight": "bold"
                    },
                    // "borderRadius": 10,
                    // "borderWidth": 1,
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
                    "layout": "vertical",
                    "left": 44,
                    "height": "FILL",
                    "width": "FILL"
                },
                "childTemplates": [{
                    "type": "Ti.UI.Label",
                    "bindId": "tlabel",
                    "properties": {
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
                        "layout": "horizontal",
                        "width": "FILL",
                        "height": "FILL"
                    },
                    "childTemplates": [{
                        "type": "Ti.UI.Label",
                        "bindId": "category",
                        "properties": {
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
                        "properties": {}
                    }, {
                        "type": "Ti.UI.Label",
                        "bindId": "priority",
                        "properties": {
                            "color": "white",
                            "padding": {
                                "left": 2,
                                "right": 2,
                                "top": 0
                            },
                            "shadowColor": "#55000000",
                            "shadowRadius": 1,
                            // "borderRadius": 2,
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
                        "width": "FILL",
                        "height": "FILL"
                    },
                    "childTemplates": [{
                        "type": "Ti.UI.View",
                        "properties": {
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
                                "backgroundColor": "#e9e9e9",
                                // "borderPadding": {
                                // "bottom": -1
                                // },
                                // "borderColor": "#E1E1E1",
                                // "borderRadius": 4
                            }
                        }, {
                            "type": "Ti.UI.View",
                            "bindId": "progressbar",
                            "properties": {
                                "borderPadding": {
                                    "top": -1,
                                    "left": -1,
                                    "right": -1
                                },
                                "left": 0,
                                "height": "FILL",
                                // "borderRadius": 4,
                                // "backgroundGradient": {
                                // "type": "linear",
                                // "tileMode": "repeat",
                                // "rect": {
                                // "x": 0,
                                // "y": 0,
                                // "width": 40,
                                // "height": 40
                                // },
                                // "colors": [{
                                // "offset": 0,
                                // "color": "#26ffffff"
                                // }, {
                                // "offset": 0.25,
                                // "color": "#26ffffff"
                                // }, {
                                // "offset": 0.25,
                                // "color": "transparent"
                                // }, {
                                // "offset": 0.5,
                                // "color": "transparent"
                                // }, {
                                // "offset": 0.5,
                                // "color": "#26ffffff"
                                // }, {
                                // "offset": 0.75,
                                // "color": "#26ffffff"
                                // }, {
                                // "offset": 0.75,
                                // "color": "transparent"
                                // }, {
                                // "offset": 1,
                                // "color": "transparent"
                                // }],
                                // "startPoint": {
                                // "x": 0,
                                // "y": 0
                                // },
                                // "endPoint": {
                                // "x": "100%",
                                // "y": "100%"
                                // }
                                // }
                            }
                        }, {
                            "type": "Ti.UI.Label",
                            "bindId": "sizelabel",
                            "properties": {
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

    var loadingView = Ti.UI.createActivityIndicator({
        backgroundColor: 'black',
        width: 60,
        height: 60
    });

    function update() {
        var items = [];
        for (var i = 0; i < 300; i++) {
            var cat = priorities[Math.floor(Math.random() * priorities.length)];
            var priority = priorities[Math.floor(Math.random() * priorities.length)];
            var name = names[Math.floor(Math.random() * names.length)];
            items.push({
                properties: {
                    searchableText: name
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
                    text: name
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
        win.remove(loadingView);
    }

    var searchBar = Ti.UI.createSearchBar({
        hideNavBarWithSearch: false,
        backgroundImage: null,
        backgroundColor: 'red',
        showCancel: true,
        height: 44
    });

    listView.searchViewExternal = searchBar;
    // searchBar.addEventListener('change',function(e) {
    //     listView.searchText = e.value;
    // });
    win.add(searchBar);
    win.add(listView);
    win.addEventListener('click', function(_event) {
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
    update();
    listView.addEventListener('longpress', function(_event) {
        //    	alert('longpress');
        sinfo('longpress');
        win.add(loadingView);
        update();
    });
    openWin(win);
}

function listViewEx3() {
    var win = createWin();
    var listview = Ti.UI.createListView({
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
    if (__APPLE__)
        listview.style = Titanium.UI.iPhone.ListViewStyle.GROUPED;
    win.add(listview);
    openWin(win);
}

function listViewEx4() {
    var win = createWin();
    // Create a custom template that displays an image on the left,
    // then a title next to it with a subtitle below it.
    var myTemplate = {
        properties: {
            leftSwipeButtons: {
                type: 'Ti.UI.Button',
                bindId: 'delete',
                properties: {
                    backgroundColor: 'red',
                    height: 'FILL',
                    title: 'delete'
                }
            }
        },
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
                color: __APPLE__ ? 'black' : 'white',
                font: {
                    fontFamily: 'Arial',
                    size: 20,
                    weight: 'bold'
                },
                left: 60,
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
        canEdit: true,
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

    var sections = [];

    for (var i = 0; i < 26; i++) {
        sections.push({
            headerView: {
                type: 'Ti.UI.Label',
                properties: {
                    backgroundColor: 'red',
                    left: 0,
                    text: 'HeaderView ' + String.fromCharCode(65 + i)
                }
            },
            items: [{
                info: {
                    text: 'Apple'
                },
                properties: {
                    accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_CHECKMARK
                }
            }, {
                info: {
                    text: 'Banana'
                },
                properties: {
                    accessoryType: Ti.UI.LIST_ACCESSORY_TYPE_DETAIL
                }
            }, {
                info: {
                    text: 'Cantaloupe'
                }
            }, {
                info: {
                    text: 'Fig'
                },
                'delete': {
                    title: 'fig delete'
                }
            }, {
                info: {
                    text: 'Guava'
                }
            }, {
                info: {
                    text: 'Kiwi'
                }
            }]
        });
    };
    listView.setSections(sections);
    var currentHeader;
    listView.addEventListener('headerchange', function(_event) {
        if (currentHeader) {
            currentHeader.backgroundColor = 'red';
        }
        currentHeader = _event.headerView;
        currentHeader.backgroundColor = 'blue';
    });

    listView.addEventListener('click', function(_event) {
        if (_event.bindId === 'delete') {
            _event.section.deleteItemsAt(_event.itemIndex, 1, {
                animated: true
            });
        }
        sdebug(_event);
    });
    listView.addEventListener('singletap', function(_event) {
        if (!_event.hasOwnProperty('itemIndex')) return;
        _event.section.deleteItemsAt(_event.itemIndex, 1, {
            animated: true
        });
    });
    listView.addEventListener('longpress', function(_event) {
        if (!_event.hasOwnProperty('itemIndex')) return;
        _event.section.insertItemsAt(_event.itemIndex, [{
            info: {
                text: 'inserted item at ' + (_event.itemIndex + 1)
            }
        }], {
            animated: true
        });
    });
    win.add(listView);
    openWin(win);
}

function getRandomColor() {
    var letters = '0123456789ABCDEF'.split('');
    var color = '#';
    for (var i = 0; i < 6; i++) {
        color += letters[Math.floor(Math.random() * 16)];
    }
    return color;
}

function listViewEx5() {
    var win = createWin();
    var myTemplate = {
        "properties": {
            "rclass": "THCGeneralRow",
            "layout": "horizontal",
            "height": 50,
            "dispatchPressed": true,
            "borderColor": "black",
            "borderPadding": {
                "left": -1,
                "right": -1,
                "top": -1
            }
        },
        "childTemplates": [{
            "type": "Ti.UI.Label",
            "bindId": "icon",
            "properties": {
                "rclass": "THCRowIcon",
                "touchPassThrough": true,
                "left": 10,
                "right": 5,
                "width": 30,
                "textAlign": "center",
                "font": {
                    "family": "fontawesome",
                    "size": 26
                },
                "padding": {
                    "bottom": 3
                },
                "color": "#454545"
            }
        }, {
            "type": "Ti.UI.TextField",
            "bindId": "textfield",
            "properties": {
                "rclass": "THCRowTextField",
                "width": "FILL",
                "height": "FILL",
                "font": {
                    "size": 16
                },
                "color": "blue",
                "backgroundColor": "transparent",
                "hintColor": "#6C6C6C",
                "returnKeyType": 3,
                "autocapitalization": 0,
                "autocorrect": false,

            },
            "events": {}
        }, {
            "type": "Ti.UI.Label",
            "bindId": "optional",
            "properties": {
                "rclass": "THCRowOptionalLabel",
                "visible": false,
                "color": "white",
                "height": "FILL",
                "font": {
                    "size": 8,
                    "weight": "bold"
                },
                "text": "OPTIONAL"
            }
        }, {
            "type": "Ti.UI.Label",
            "bindId": "accessory",
            "properties": {
                "rclass": "THCRowAccessory",
                "visible": false,
                "left": 10,
                "right": 10,
                "textAlign": "center",
                "font": {
                    "family": "fontawesome",
                    "size": 26
                },
                "color": "#454545",
                "selectedColor": "#B0C113"
            }
        }]
    };
    myTemplate.childTemplates[1].events = {
        change: function(_event) {
            if (_event.hasOwnProperty('section') && _event.hasOwnProperty('itemIndex')) {
                var item = _event.section.getItemAt(_event.itemIndex);
                sdebug('change', item.textfield.value);
                item.textfield.color = _.isEmpty(item.textfield.value) ? 'red' : 'green';
                _event.section.updateItemAt(_event.itemIndex, item);
            }
        }
    };
    var listView = Ti.UI.createListView({
        rowHeight: 50,
        // updateInsetWithKeyboard:true,
        templates: {
            'template': myTemplate
        },
        defaultItemTemplate: 'template',
    });
    var items = [];
    var enabled, editable;
    for (var i = 0; i < 10; i++) {
        enabled = Math.random() < 0.5;
        editable = Math.random() < 0.5;
        passwordMask = Math.random() < 0.5;

        items.push({
            textfield: {
                hintText: 'ed: ' + editable + ', pw:' + passwordMask,
                // enabled : enabled,
                // editable: editable,
                passwordMask: passwordMask,
                keyboardToolbar: {
                    type: 'Ti.UI.iOS.Toolbar',
                    properties: {
                        tintColor: getRandomColor(),
                        items: [{
                            type: 'Ti.UI.Button',
                            systemButton: Ti.UI.iPhone.SystemButton.FLEXIBLE_SPACE
                        }, {
                            type: 'Ti.UI.Button',
                            systemButton: Ti.UI.iPhone.SystemButton.DONE,
                            callbackId: 'done',
                        }]
                    }
                }
            }
        });
    }

    listView.addEventListener('click', function(_event) {
        if (_event.source.callbackId === 'done') {
            listView.blur();
        } else if (_event.hasOwnProperty('section') && _event.hasOwnProperty('itemIndex')) {
            var item = _event.section.getItemAt(_event.itemIndex);
            info('click ' + JSON.stringify(item));
        }
    });

    listView.setSections([{
        headerView: {
            type: 'Ti.UI.Label',
            properties: {
                backgroundColor: 'red',
                bottom: 20,
                text: 'HeaderView created from Dict'
            }
        },
        items: items
            // }, {
            //     headerView: {
            //         type: 'Ti.UI.Label',
            //         properties: {
            //             backgroundColor: 'red',
            //             bottom: 20,
            //             text: 'HeaderView created from Dict'
            //         }
            //     },
            //     items: items
    }]);
    win.add(listView);
    openWin(win);
}

function longListTest() {
    var win = createWin({
        toolbar: [Ti.UI.createButton({
            properties: {
                title: 'test',
            },
            events: {
                click: function() {
                    listView.editing = !listView.editing;
                }
            }
        })]
    });
    var myTemplate = {
        "properties": {
            "height": 74,
            "borderColor": "#667383",
            "borderPadding": {
                "left": -1,
                "right": -1,
                "top": -1
            },
            leftSwipeButtons: {
                type: 'Ti.UI.Label',
                bindId: 'mark',
                properties: {
                    callbackId: 'mark',
                    backgroundColor: 'blue',
                    height: 'FILL',
                    text: 'mark as watched'
                }
            },
            rightSwipeButtons: [{
                type: 'Ti.UI.Label',
                bindId: 'delete',
                properties: {
                    backgroundColor: 'red',
                    height: 'FILL',
                    text: 'delete'
                }
            }, {
                type: 'Ti.UI.Label',
                bindId: 'more',
                properties: {
                    backgroundColor: 'gray',
                    height: 'FILL',
                    text: 'more'
                }
            }]
        },
        "childTemplates": [{
            "type": "Ti.UI.View",
            "properties": {
                "rclass": "CPMRCheckHolder",
                "left": 0,
                "width": 50,
                "height": "FILL"
            },
            "childTemplates": [{
                "type": "Ti.UI.Label",
                "bindId": "check",
                "properties": {
                    "rclass": "CPMRCheck",
                    "visible": false,
                    "color": "transparent",
                    "textAlign": "center",
                    "clipChildren": false,
                    "font": {
                        "size": 12,
                        "family": "elusive"
                    },
                    "text": "é",
                    "width": 20,
                    "height": 20,
                    "borderRadius": 4,
                    "backgroundColor": "#2C333A",
                    "backgroundSelectedColor": "#414C59",
                    "disableHW": true,
                    "backgroundInnerShadows": [{
                        "radius": 10,
                        "color": "#1a1e22"
                    }],
                    "backgroundSelectedInnerShadows": [{
                        "radius": 10,
                        "color": "#252A31"
                    }]
                }
            }]
        }, {
            "type": "Ti.UI.ImageView",
            "bindId": "image",
            "properties": {
                "rclass": "CPMovieRowImage",
                "height": "FILL",
                "width": 50,
                "top": 0,
                "bottom": 0,
                "left": 0,
                "scaleType": 2,
                "retina": true,
                "localLoadSync": true,
                "preventDefaultImage": true,
                onlyTransitionIfRemote: true,
                transition: {
                    style: Ti.UI.TransitionStyle.FADE
                }
            },
            "childTemplates": [{
                "type": "Ti.UI.ActivityIndicator",
                "bindId": "loader",
                "properties": {
                    "rclass": "CPMovieRowLoader",
                    "width": "FILL",
                    "height": "FILL",
                    "backgroundColor": "#77000000",
                    "visible": false
                }
            }]
        }, {
            "type": "Ti.UI.View",
            "properties": {
                "layout": "vertical",
                "width": "FILL",
                "height": "FILL",
                "left": 53
            },
            "childTemplates": [{
                "type": "Ti.UI.Label",
                "bindId": "title",
                "properties": {
                    "rclass": "CPMovieRowTitle",
                    "height": 22,
                    "font": {
                        "size": 17,
                        "weight": "normal"
                    },
                    "color": "#ffffff",
                    "top": 0,
                    "ellipsize": Ti.UI.TEXT_ELLIPSIS_END,
                    "width": "FILL",
                    "maxLines": 1
                }
            }, {
                "type": "Ti.UI.Label",
                "bindId": "subtitle",
                "properties": {
                    "rclass": "CPMovieRowSubtitle",
                    "color": "#B9B9BA",
                    "maxLines": 2,
                    "font": {
                        "size": 13
                    },
                    "padding": {
                        "right": 3
                    },
                    "verticalAlign": "bottom",
                    "height": "FILL",
                    "width": "FILL",
                    "ellipsize": Ti.UI.TEXT_ELLIPSIS_END
                }
            }, {
                "type": "Ti.UI.View",
                "bindId": "statusHolder",
                "properties": {
                    "rclass": "CPMRBottomLine",
                    "visible": true,
                    "height": 20,
                    "width": "FILL",
                    "layout": "horizontal"
                },
                "childTemplates": [{
                    "type": "Ti.UI.View",
                    "properties": {
                        "rclass": "Fill",
                        "width": "FILL",
                        "height": "FILL"
                    }
                }, {
                    "type": "Ti.UI.Label",
                    "properties": {
                        "rclass": "CPMRStatus",
                        "clipChildren": false,
                        "right": 3,
                        "visible": false,
                        "backgroundColor": "#5082BC",
                        "textAlign": "center",
                        "padding": {
                            "left": 3,
                            "right": 3
                        },
                        "color": "#ffffff",
                        "font": {
                            "size": 11
                        },
                        "borderRadius": 2,
                        "imageId": 2
                    },
                    "bindId": "status2"
                }, {
                    "type": "Ti.UI.Label",
                    "properties": {
                        "rclass": "CPMRStatus",
                        "clipChildren": false,
                        "right": 3,
                        "visible": false,
                        "backgroundColor": "#5082BC",
                        "textAlign": "center",
                        "padding": {
                            "left": 3,
                            "right": 3
                        },
                        "color": "#ffffff",
                        "font": {
                            "size": 11
                        },
                        "borderRadius": 2,
                        "imageId": 1
                    },
                    "bindId": "status1"
                }, {
                    "type": "Ti.UI.Label",
                    "properties": {
                        "rclass": "CPMRStatus",
                        "clipChildren": false,
                        "right": 3,
                        "visible": false,
                        "backgroundColor": "#5082BC",
                        "textAlign": "center",
                        "padding": {
                            "left": 3,
                            "right": 3
                        },
                        "color": "#ffffff",
                        "font": {
                            "size": 11
                        },
                        "borderRadius": 2,
                        "imageId": 0
                    },
                    "bindId": "status0"
                }, {
                    "type": "Ti.UI.Label",
                    "bindId": "year",
                    "properties": {
                        "rclass": "CPMRYear",
                        "width": 42,
                        "height": "FILL",
                        "font": {
                            "size": 18
                        },
                        "color": "#B9B9BA"
                    }
                }]
            }]
        }]
    };

    var listView = Ti.UI.createListView({
        rowHeight: 50,
        allowsMultipleSelectionDuringEditing: true,
        selectedBackgroundColor: 'red',
        canEdit: true,
        // updateInsetWithKeyboard:true,
        templates: {
            'template': myTemplate
        },
        defaultItemTemplate: 'template',
    });

    var items = [],
        color, movie, nbMovies = movies.length;
    for (var i = 0; i < 500; i++) {
        color = getRandomColor();
        movie = movies[Math.floor(Math.random() * nbMovies)];

        items.push({
            template: 'template',
            properties: {
                searchableText: movie.title
            },
            image: {
                backgroundColor: color,
                image: movie.poster_path
            },
            title: {
                text: movie.title
            },
            year: {
                text: movie.release_date.split('-')[0]
            },
            status0: {
                visible: true,
                color: 'black',
                html: movie.popularity
            }
        });

    }

    listView.addEventListener('click', function(_event) {
        if (_event.source.callbackId === 'done') {
            listView.blur();
        } else if (_event.source.callbackId === 'mark') {
            listView.closeSwipeMenu();
            _event.section.updateItemAt(_event.itemIndex, {
                mark: {
                    text: 'mark as unwatched'
                }
            });
        } else if (_event.hasOwnProperty('section') && _event.hasOwnProperty('itemIndex')) {
            var item = _event.section.getItemAt(_event.itemIndex);
            info('click ' + JSON.stringify(item));
        }
    });

    listView.setSections([{
        headerView: {
            type: 'Ti.UI.Label',
            properties: {
                backgroundColor: 'red',
                bottom: 20,
                text: 'HeaderView created from Dict'
            }
        },
        items: items
    }]);
    win.add(listView);
    openWin(win);
}

function deepLayoutTest() {

    var win = createWin({
        dispatchPressed: true,
        layout: 'vertical'
    });
    var viewHolder = new View({
        width: 'FILL',
        height: 60,
        backgroundColor: 'yellow'
    });
    var test = new View({
        properties: {
            backgroundColor: 'green',
            right: 0,
            width: 'SIZE',
            height: 'FILL',
            layout: 'horizontal'
        },
        childTemplates: [{
            type: 'Ti.UI.View',
            properties: {
                height: 'FILL',
                width: 'SIZE',
                borderColor: '#667383',
                borderPadding: {
                    right: -1,
                    top: -1,
                    bottom: -1
                },
            },
            childTemplates: [{
                type: 'Ti.UI.TextField',
                bindId: 'searchField',
                properties: {
                    rclass: 'CPSearchField',
                    color: 'black',
                    hintColor: 'gray',
                    right: 0,
                    height: 'FILL',
                    visible: false,
                    backgroundColor: 'white',
                    borderWidth: 3,
                    borderPadding: {
                        right: -3,
                        left: -3,
                        top: -3
                    },
                    borderColor: 'red',
                    borderSelectedColor: '#04BCE6',
                    width: 'FILL',
                    hintText: 'cp.searchfieldHint',
                    padding: {
                        left: 5,
                        right: 5
                    },
                }
            }, {
                type: 'Ti.UI.Label',
                bindId: 'search',
                properties: {
                    callbackId: 'search',
                    borderWidth: 3,
                    borderPadding: {
                        right: -3,
                        left: -3,
                        top: -3
                    },
                    borderSelectedColor: '#047792',
                    backgroundSelectedColor: '#667383',
                    backgroundColor: 'gray',
                    font: {
                        size: 20,
                        weight: 'bold'
                    },
                    padding: {
                        left: 15,
                        right: 15
                    },
                    color: 'white',
                    disabledColor: 'white',
                    height: 'FILL',
                    callbackId: 'search',
                    right: 0,
                    text: 'Aaaa',
                    clearIcon: 'X',
                    icon: 'A',
                    transition: {
                        style: Ti.UI.TransitionStyle.FADE
                    }
                }
            }]
        }]
    });

    test.addEventListener('click', function(e) {
        info('test click ' + JSON.stringify(e.source));
        if (e.source.callbackId === 'search') {
            if (test.searchField.visible) {
                var searchField = test.searchField;
                searchField.value = '';
                searchField.animate({
                    width: 1,
                    opacity: 0,
                    duration: 200
                }, function() {
                    searchField.visible = false;
                });
                searchField.blur();
                searchField.fireEvent('hidding');
                test.search.text = test.search.icon;
            } else {
                var searchField = test.searchField;
                info('showSearchField ' + searchField.callbackId);
                searchField.applyProperties({
                    value: null,
                    opacity: 0,
                    width: 1,
                    visible: true
                });
                searchField.animate({
                    width: 'FILL',
                    opacity: 1,
                    duration: 300
                }, function() {
                    searchField.focus();
                });
                searchField.fireEvent('showing');
                test.search.text = test.search.clearIcon;
            }
        }
    });

    viewHolder.add(test);
    var headerView = new Label({
        properties: {
            color: 'gray',
            font: {
                size: 12
            },
            backgroundColor: 'green',
            width: 'FILL',
            height: 22,
            // borderPadding:{left:-1,right:-1,top:-1},
            padding: {
                left: 40,
                top: 2,
                bottom: 2
            },
            text: 'test'
        },
        childTemplates: [{
            type: 'Ti.UI.Label',
            properties: {
                color: 'white',
                backgroundColor: '#3A87AD',
                font: {
                    size: 12
                },
                left: 10,
                height: 16,
                // borderRadius:8,
                clipChildren: false,
                verticalAlign: 'center',
                padding: {
                    left: 8,
                    right: 8,
                    top: -2
                },
                text: '2'
            }
        }, {
            type: 'Ti.UI.Switch',
            properties: {
                right: 0,
                value: false
            },
            events: {
                'change': function(e) {
                    info(stringify(e));
                    listView.sections[1].visible = e.value;
                }
            }
        }]
    });
    var section = Ti.UI.createListSection({
        headerView: headerView,
        visible: false,
        items: [{
            title: 'test1'
        }, {
            title: 'test2'
        }]
    });

    function createSoonRow(_number) {
        var template = redux.fn.style('ListItem', {
            properties: {
                layout: 'horizontal',
                horizontalWrap: true,
                height: 'SIZE'
            }
        });
        var childTemplates = [];
        var defProps = ak.ti.style({
            type: 'Ti.UI.Label',
            properties: {
                font: {
                    size: 15,
                    weight: 'normal'
                },
                padding: {
                    left: 4,
                    top: 4,
                    right: 4
                },
                verticalAlign: 'top',
                width: 80,
                height: 120,
                visible: false,
                dispatchPressed: true,
                backgroundColor: '#55000000',
                color: 'white'
            },
            childTemplates: [{
                type: 'Ti.UI.ImageView',
                properties: {
                    width: 'FILL',
                    height: 'FILL',
                    dispatchPressed: true,
                    transition: {
                        style: Ti.UI.TransitionStyle.FADE
                    },
                    scaleType: Ti.UI.SCALE_TYPE_ASPECT_FILL
                },
                childTemplates: [{
                    type: 'Ti.UI.Label',
                    properties: {
                        width: 'FILL',
                        font: {
                            size: 15,
                            weight: 'normal'
                        },
                        padding: {
                            left: 4,
                            top: 4,
                            right: 4
                        },
                        verticalAlign: 'top',
                        height: 'FILL',
                        touchPassThrough: false,
                        backgroundSelectedColor: '#99000000',
                        color: 'transparent',
                        selectedColor: 'white',
                    }
                }]
            }]
        });
        for (var i = 0; i < _number; i++) {
            var props = redux.fn.clone(defProps);
            props.properties.imageId = i;
            props.bindId = 'soonBottomLabel' + i;
            props.childTemplates[0].bindId = 'soonImage' + i;
            props.childTemplates[0].childTemplates[0].bindId = 'soonLabel' + i;
            childTemplates.push(props);
        }
        template.childTemplates = childTemplates;
        return template;
    };
    var editMode = false;

    var listView = createListView({
        height: 'FILL',
        backgroundSelectedColor: 'blue',
        templates: {
            "titlevalue": {
                "properties": {
                    "rclass": "GenericRow TVRow",
                    "layout": "horizontal",
                    "height": "SIZE"
                },
                "childTemplates": [{
                    "type": "Ti.UI.Label",
                    "bindId": "title",
                    "properties": {
                        "rclass": "NZBGetTVRTitle",
                        "font": {
                            "size": 14
                        },
                        "padding": {
                            "left": 4,
                            "right": 4,
                            "top": 5
                        },
                        "textAlign": "right",
                        "width": 90,
                        "color": "black",
                        "verticalAlign": "top",
                        top: 0
                            // "height" : "FILL"
                    }
                }, {
                    "type": "Ti.UI.Label",
                    "bindId": "value",
                    "properties": {
                        "rclass": "NZBGetTVRValue",
                        "color": "#686868",
                        "font": {
                            "size": 14
                        },
                        "top": 4,
                        "bottom": 4,
                        "padding": {
                            "left": 4,
                            "right": 4,
                            "bottom": 2,
                            "top": 2
                        },
                        "verticalAlign": "middle",
                        "left": 4,
                        "width": "FILL",
                        "height": "SIZE",
                        "right": 4,
                        "textAlign": "left",
                        "maxLines": 2,
                        "ellipsize": "END",
                        "borderColor": "#eeeeee",
                        "borderRadius": 2
                    }
                }]
            },
            "textfield": {
                "childTemplates": [{
                    type: 'Ti.UI.View',
                    properties: {
                        left: 0,
                        width: 50,
                        height: 'FILL',
                    },
                    childTemplates: [{
                        "type": "Ti.UI.Label",
                        "bindId": "check",
                        properties: {
                            visible: false,
                            disableHW: true,
                            borderRadius: 4,
                            backgroundColor: '#2C333A',
                            backgroundSelectedColor: '#414C59',
                            backgroundInnerShadows: [{
                                radius: 10,
                                color: '#1a1e22'
                            }],
                            backgroundSelectedInnerShadows: [{
                                radius: 10,
                                color: '#252A31'
                            }],
                            color: 'transparent',
                            textAlign: 'center',
                            clipChildren: false,
                            font: {
                                size: 12
                            },
                            text: 's',
                            width: 20,
                            height: 20
                        }
                    }]
                }, {
                    "type": "Ti.UI.TextField",
                    "bindId": "textfield",
                    "events": {},
                    "properties": {
                        "color": "#686868",
                        "ellipsize": 'END',
                        "padding": {
                            "left": 4,
                            "top": 2,
                            "bottom": 2,
                            "right": 4
                        },
                        "backgroundColor": "white",
                        "maxLines": 2,
                        "font": {
                            "size": 14
                        },
                        "borderColor": "#eeeeee",
                        "bottom": 4,
                        "verticalAlign": "middle",
                        "borderSelectedColor": "#74B9EF",
                        // returnKeyType: Ti.UI.RETURNKEY_NEXT,
                        "borderRadius": 2,
                        "height": 40,
                        "right": 4,
                        "textAlign": "left",
                        "left": 4,
                        "width": "FILL",
                        "top": 4
                    }
                }],
                "properties": {
                    "height": 60,
                    "layout": "horizontal",
                    backgroundColor: 'white',
                    "rclass": "GenericRow TVRow"
                }
            },
            soonRow: createSoonRow(10),
            "release": {
                "childTemplates": [{
                    "properties": {
                        "rclass": "CPMovieReleaseRowProvider",
                        "left": 3,
                        "width": "FILL",
                        "font": {
                            "size": 11
                        },
                        "color": "#B9B9BA",
                        "height": 15
                    },
                    "type": "Ti.UI.Label",
                    "bindId": "provider"
                }, {
                    "childTemplates": [{
                        "properties": {
                            "left": 3,
                            "rclass": "CPMovieReleaseRowTitle",
                            "color": "#ffffff",
                            "verticalAlign": "top",
                            "width": "FILL",
                            "ellipsize": 'END',
                            "height": "FILL",
                            "font": {
                                "size": 12
                            }
                        },
                        "type": "Ti.UI.Label",
                        "bindId": "title"
                    }],
                    "type": "Ti.UI.View",
                    "properties": {
                        "width": "FILL",
                        "layout": "horizontal",
                        "rclass": "Fill HHolder",
                        "height": "FILL"
                    }
                }, {
                    "childTemplates": [{
                        "childTemplates": [{
                            "childTemplates": [{
                                "type": "Ti.UI.Label",
                                "properties": {
                                    "text": "",
                                    "left": 3,
                                    "rid": "cpSizeIcon",
                                    "rclass": "CPMovieReleaseRowIcon",
                                    "color": "#ffffff",
                                    "width": 12,
                                    "height": "FILL",
                                    "font": {
                                        "family": "webhostinghub",
                                        "size": 11
                                    }
                                }
                            }, {
                                "bindId": "size",
                                "type": "Ti.UI.Label",
                                "properties": {
                                    "rclass": "CPMovieReleaseRowInfos",
                                    "font": {
                                        "size": 11
                                    },
                                    "color": "#B9B9BA",
                                    "height": "FILL"
                                }
                            }],
                            "type": "Ti.UI.View",
                            "properties": {
                                "width": "SIZE",
                                "layout": "horizontal",
                                "rclass": "FillHeight SizeWidth HHolder",
                                "height": "FILL"
                            }
                        }, {
                            "childTemplates": [{
                                "type": "Ti.UI.Label",
                                "properties": {
                                    "text": "",
                                    "left": 3,
                                    "rid": "cpAgeIcon",
                                    "rclass": "CPMovieReleaseRowIcon",
                                    "color": "#ffffff",
                                    "width": 12,
                                    "height": "FILL",
                                    "font": {
                                        "family": "webhostinghub",
                                        "size": 11
                                    }
                                }
                            }, {
                                "bindId": "age",
                                "type": "Ti.UI.Label",
                                "properties": {
                                    "rclass": "CPMovieReleaseRowInfos",
                                    "font": {
                                        "size": 11
                                    },
                                    "color": "#B9B9BA",
                                    "height": "FILL"
                                }
                            }],
                            "type": "Ti.UI.View",
                            "properties": {
                                "width": "SIZE",
                                "layout": "horizontal",
                                "rclass": "FillHeight SizeWidth HHolder",
                                "height": "FILL"
                            }
                        }, {
                            "childTemplates": [{
                                "type": "Ti.UI.Label",
                                "properties": {
                                    "text": "",
                                    "left": 3,
                                    "rid": "cpScoreIcon",
                                    "rclass": "CPMovieReleaseRowIcon",
                                    "color": "#ffffff",
                                    "width": 12,
                                    "height": "FILL",
                                    "font": {
                                        "family": "webhostinghub",
                                        "size": 11
                                    }
                                }
                            }, {
                                "bindId": "score",
                                "type": "Ti.UI.Label",
                                "properties": {
                                    "rclass": "CPMovieReleaseRowInfos",
                                    "font": {
                                        "size": 11
                                    },
                                    "color": "#B9B9BA",
                                    "height": "FILL"
                                }
                            }],
                            "type": "Ti.UI.View",
                            "properties": {
                                "width": "SIZE",
                                "layout": "horizontal",
                                "rclass": "FillHeight SizeWidth HHolder",
                                "height": "FILL"
                            }
                        }],
                        "type": "Ti.UI.View",
                        "bindId": "iconsHolder",
                        "properties": {
                            "layout": "horizontal",
                            "rclass": "CPMovieReleaseRowInfosHolder"
                        }
                    }, {
                        "type": "Ti.UI.View",
                        "bindId": "statusHolder",
                        "childTemplates": [{
                            "type": "Ti.UI.View",
                            "properties": {
                                "width": "FILL",
                                "height": "FILL",
                                "rclass": "Fill"
                            }
                        }, {
                            "bindId": "status0",
                            "type": "Ti.UI.Label",
                            "properties": {
                                "visible": false,
                                "backgroundColor": "#5082BC",
                                "padding": {
                                    "left": 3,
                                    "right": 3
                                },
                                "textAlign": "center",
                                "clipChildren": false,
                                "rclass": "CPMRStatus",
                                "color": "#ffffff",
                                "right": 3,
                                "borderRadius": 2,
                                "imageId": 0,
                                "font": {
                                    "size": 11
                                }
                            }
                        }],
                        "properties": {
                            "width": "FILL",
                            "rclass": "CPMRReleaseBottomLine",
                            "layout": "horizontal",
                            "visible": true,
                            "height": 20
                        }
                    }],
                    "type": "Ti.UI.View",
                    "properties": {
                        "rclass": "CPMovieReleaseBottomLine",
                        "layout": "horizontal",
                        "width": "FILL",
                        "height": 20
                    }
                }],
                "properties": {
                    "borderColor": "#667383",
                    "backgroundSelectedColor": "#110000",
                    "layout": "vertical",
                    "backgroundColor": "#77000000",
                    "borderPadding": {
                        "left": -1,
                        "top": -1,
                        "right": -1
                    },
                    "rclass": "CPMovieReleaseRow",
                    "height": 62
                }
            },

            titleTest: {
                "properties": {
                    "rclass": "NewsDetailsRow",
                    "height": "SIZE",
                    "backgroundGradient": {
                        "type": "linear",
                        "colors": ["#F7F7F7", "white"],
                        "startPoint": {
                            "x": 0,
                            "y": 0
                        },
                        "endPoint": {
                            "x": 0,
                            "y": "100%"
                        }
                    }
                },
                "childTemplates": [{
                    "type": "Ti.UI.ImageView",
                    "bindId": "image",
                    "properties": {
                        "rclass": "NewsDetailsRowImage",
                        "top": 8,
                        "backgroundColor": "#C5C5C5",
                        "left": 8,
                        "width": 60,
                        "height": 60,
                        "retina": false,
                        "localLoadSync": true,
                        "preventDefaultImage": true
                    }
                }, {
                    "type": "Ti.UI.View",
                    "properties": {
                        "rclass": "NewsDetailsRowLabelHolder",
                        "height": "SIZE",
                        "layout": "vertical",
                        "left": 76,
                        "top": 10,
                        "bottom": 10
                    },
                    "childTemplates": [{
                        "type": "Ti.UI.Label",
                        "bindId": "title",
                        "properties": {
                            "rclass": "NewsDetailsRowTitle",
                            "height": "SIZE",
                            "maxLines": 0,
                            "color": "#6B6B6B",
                            "width": "FILL",
                            "ellipsize": "END",
                            "font": {
                                "size": 14,
                                "weight": "bold"
                            }
                        }
                    }, {
                        "type": "Ti.UI.Label",
                        "bindId": "description",
                        "properties": {
                            "rclass": "NewsDetailsRowSubtitle",
                            "height": "SIZE",
                            "color": "#3F3F3F",
                            "width": "FILL",
                            "ellipsize": "END",
                            "font": {
                                "size": 12
                            }
                        }
                    }]
                }]
            }
        },
        sections: [{
                items: [{
                    template: 'textfield',
                    check: {
                        visible: editMode
                    },
                    textfield: {
                        value: ''
                    }
                }, {
                    template: 'textfield',
                    textfield: {
                        value: ''
                    }
                }, {
                    template: 'soonRow',
                    soonBottomLabel0: {
                        visible: true,
                        text: 'test'
                    },
                    soonLabel0: {
                        text: 'test'
                    },
                    soonImage0: {
                        image: 'http://zapp.trakt.us/images/posters_movies/192263-138.jpg'
                    }
                }, {
                    template: 'release',
                    title: {
                        text: 'test release'
                    },
                    iconsHolder: {
                        visible: false
                    }
                }, {
                    template: 'titleTest',
                    title: {
                        text: 'test release'
                    },
                    description: {
                        html: "<p style=\"text-align: center;\"><img src=\"https://www.yaliberty.org/sites/default/files/imagecache/fullsize/images/Bonnie_Kristian/susq.jpg\" alt=\"Susquehanna\" title=\"Susquehanna\" class=\"imagecache imagecache-fullsize\" /></p><p>Susquehanna Young Americans for Liberty had our first&nbsp;<span style=\"letter-spacing: 0px; line-height: 1.3em; word-spacing: normal;\">meeting on February 19. We introduced ourselves and discussed why we believe liberty is important, along with some&nbsp;</span><span style=\"letter-spacing: 0px; line-height: 1.3em; word-spacing: normal;\">potential recruiting options.&nbsp;</span><span style=\"letter-spacing: 0px; line-height: 1.3em; word-spacing: normal;\">We then discussed current events and how they affect liberty and how to move ahead in expanding this chapter on campus!</span></p>"
                    }
                }, {
                    template: 'titlevalue',
                    title: {
                        text: tr('category')
                    },
                    value: {
                        text: tr('nzbget.catEmpty')
                    }
                }, {
                    template: 'titleTest',
                    title: {
                        text: 'test release'
                    },
                    description: {
                        html: "<p style=\"text-align: center;\"><img src=\"https://www.yaliberty.org/sites/default/files/imagecache/fullsize/images/Bonnie_Kristian/susq.jpg\" alt=\"Susquehanna\" title=\"Susquehanna\" class=\"imagecache imagecache-fullsize\" /></p><p>Susquehanna Young Americans for Liberty had our first&nbsp;<span style=\"letter-spacing: 0px; line-height: 1.3em; word-spacing: normal;\">meeting on February 19. We introduced ourselves and discussed why we believe liberty is important, along with some&nbsp;</span><span style=\"letter-spacing: 0px; line-height: 1.3em; word-spacing: normal;\">potential recruiting options.&nbsp;</span><span style=\"letter-spacing: 0px; line-height: 1.3em; word-spacing: normal;\">We then discussed current events and how they affect liberty and how to move ahead in expanding this chapter on campus!</span></p>"
                    }
                }, {
                    template: 'titleTest',
                    title: {
                        text: 'test release'
                    },
                    description: {
                        html: "<p style=\"text-align: center;\"><img src=\"https://www.yaliberty.org/sites/default/files/imagecache/fullsize/images/Bonnie_Kristian/susq.jpg\" alt=\"Susquehanna\" title=\"Susquehanna\" class=\"imagecache imagecache-fullsize\" /></p><p>Susquehanna Young Americans for Liberty had our first&nbsp;<span style=\"letter-spacing: 0px; line-height: 1.3em; word-spacing: normal;\">meeting on February 19. We introduced ourselves and discussed why we believe liberty is important, along with some&nbsp;</span><span style=\"letter-spacing: 0px; line-height: 1.3em; word-spacing: normal;\">potential recruiting options.&nbsp;</span><span style=\"letter-spacing: 0px; line-height: 1.3em; word-spacing: normal;\">We then discussed current events and how they affect liberty and how to move ahead in expanding this chapter on campus!</span></p>"
                    }
                }, {
                    template: 'titleTest',
                    title: {
                        text: 'test release'
                    },
                    description: {
                        html: "<p style=\"text-align: center;\"><img src=\"https://www.yaliberty.org/sites/default/files/imagecache/fullsize/images/Bonnie_Kristian/susq.jpg\" alt=\"Susquehanna\" title=\"Susquehanna\" class=\"imagecache imagecache-fullsize\" /></p><p>Susquehanna Young Americans for Liberty had our first&nbsp;<span style=\"letter-spacing: 0px; line-height: 1.3em; word-spacing: normal;\">meeting on February 19. We introduced ourselves and discussed why we believe liberty is important, along with some&nbsp;</span><span style=\"letter-spacing: 0px; line-height: 1.3em; word-spacing: normal;\">potential recruiting options.&nbsp;</span><span style=\"letter-spacing: 0px; line-height: 1.3em; word-spacing: normal;\">We then discussed current events and how they affect liberty and how to move ahead in expanding this chapter on campus!</span></p>"
                    }
                }, {
                    template: 'titleTest',
                    title: {
                        text: 'test release'
                    },
                    description: {
                        html: "<p style=\"text-align: center;\"><img src=\"https://www.yaliberty.org/sites/default/files/imagecache/fullsize/images/Bonnie_Kristian/susq.jpg\" alt=\"Susquehanna\" title=\"Susquehanna\" class=\"imagecache imagecache-fullsize\" /></p><p>Susquehanna Young Americans for Liberty had our first&nbsp;<span style=\"letter-spacing: 0px; line-height: 1.3em; word-spacing: normal;\">meeting on February 19. We introduced ourselves and discussed why we believe liberty is important, along with some&nbsp;</span><span style=\"letter-spacing: 0px; line-height: 1.3em; word-spacing: normal;\">potential recruiting options.&nbsp;</span><span style=\"letter-spacing: 0px; line-height: 1.3em; word-spacing: normal;\">We then discussed current events and how they affect liberty and how to move ahead in expanding this chapter on campus!</span></p>"
                    }
                }, {
                    template: 'titleTest',
                    title: {
                        text: 'test release'
                    },
                    description: {
                        html: "<p style=\"text-align: center;\"><img src=\"https://www.yaliberty.org/sites/default/files/imagecache/fullsize/images/Bonnie_Kristian/susq.jpg\" alt=\"Susquehanna\" title=\"Susquehanna\" class=\"imagecache imagecache-fullsize\" /></p><p>Susquehanna Young Americans for Liberty had our first&nbsp;<span style=\"letter-spacing: 0px; line-height: 1.3em; word-spacing: normal;\">meeting on February 19. We introduced ourselves and discussed why we believe liberty is important, along with some&nbsp;</span><span style=\"letter-spacing: 0px; line-height: 1.3em; word-spacing: normal;\">potential recruiting options.&nbsp;</span><span style=\"letter-spacing: 0px; line-height: 1.3em; word-spacing: normal;\">We then discussed current events and how they affect liberty and how to move ahead in expanding this chapter on campus!</span></p>"
                    }
                }, {
                    template: 'titleTest',
                    title: {
                        text: 'test release'
                    },
                    description: {
                        html: "<p style=\"text-align: center;\"><img src=\"https://www.yaliberty.org/sites/default/files/imagecache/fullsize/images/Bonnie_Kristian/susq.jpg\" alt=\"Susquehanna\" title=\"Susquehanna\" class=\"imagecache imagecache-fullsize\" /></p><p>Susquehanna Young Americans for Liberty had our first&nbsp;<span style=\"letter-spacing: 0px; line-height: 1.3em; word-spacing: normal;\">meeting on February 19. We introduced ourselves and discussed why we believe liberty is important, along with some&nbsp;</span><span style=\"letter-spacing: 0px; line-height: 1.3em; word-spacing: normal;\">potential recruiting options.&nbsp;</span><span style=\"letter-spacing: 0px; line-height: 1.3em; word-spacing: normal;\">We then discussed current events and how they affect liberty and how to move ahead in expanding this chapter on campus!</span></p>"
                    }
                }, {
                    template: 'titleTest',
                    title: {
                        text: 'test release'
                    },
                    description: {
                        html: "<p style=\"text-align: center;\"><img src=\"https://www.yaliberty.org/sites/default/files/imagecache/fullsize/images/Bonnie_Kristian/susq.jpg\" alt=\"Susquehanna\" title=\"Susquehanna\" class=\"imagecache imagecache-fullsize\" /></p><p>Susquehanna Young Americans for Liberty had our first&nbsp;<span style=\"letter-spacing: 0px; line-height: 1.3em; word-spacing: normal;\">meeting on February 19. We introduced ourselves and discussed why we believe liberty is important, along with some&nbsp;</span><span style=\"letter-spacing: 0px; line-height: 1.3em; word-spacing: normal;\">potential recruiting options.&nbsp;</span><span style=\"letter-spacing: 0px; line-height: 1.3em; word-spacing: normal;\">We then discussed current events and how they affect liberty and how to move ahead in expanding this chapter on campus!</span></p>"
                    }
                }]
            },
            section
        ]
    });

    var label = new Label({
        color: '#F2F3F3',
        disabledColor: '#F2F3F3',
        width: 'FILL',
        touchPassThrough: true,
        borderWidth: 3,
        text: 'test',
        borderPadding: {
            right: -3,
            top: -3,
            bottom: -3
        },
        borderDisabledColor: 'red',
        borderSelectedColor: 'blue',
        padding: {
            left: 15,
            right: 15
        },
        font: {
            size: 20
        }
    });
    // win.add(label);
    win.add(listView);
    win.add(viewHolder);
    // win.addEventListener('click', function(e){
    // // win.blur()
    // // label.enabled = !label.enabled;
    // });
    openWin(win);
}

function pullToRefresh(_args) {
    var win = createWin(_args);
    var sections = [];

    var fruitSection = Ti.UI.createListSection({
        headerTitle: 'Fruits'
    });
    var fruitDataSet = [{
        properties: {
            title: 'Apple'
        }
    }, {
        properties: {
            title: 'Banana'
        }
    }, {
        properties: {
            title: 'Cantaloupe'
        }
    }, {
        properties: {
            title: 'Fig'
        }
    }, {
        properties: {
            title: 'Guava'
        }
    }, {
        properties: {
            title: 'Kiwi'
        }
    }];
    fruitSection.setItems(fruitDataSet);
    sections.push(fruitSection);

    var header = new Label({
        properties: {
            width: 'FILL',
            textAlign: 'left',
            text: 'Vegetables'
        },
        childTemplates: [{
            type: 'Ti.UI.Switch',
            bindId: 'switch',
            properties: {
                right: 0
            },
            events: {
                'change': function() {
                    vegSection.visible = !vegSection.visible;
                }
            }
        }]
    });
    var vegSection = Ti.UI.createListSection({
        headerView: header
    });
    var vegDataSet = [{
        properties: {
            title: 'Carrots'
        }
    }, {
        properties: {
            title: 'Potatoes'
        }
    }, {
        properties: {
            title: 'Corn'
        }
    }, {
        properties: {
            title: 'Beans'
        }
    }, {
        properties: {
            title: 'Tomato'
        }
    }];
    vegSection.setItems(vegDataSet);
    sections.push(vegSection);

    var fishSection = Ti.UI.createListSection({
        headerTitle: 'Fish'
    });
    var fishDataSet = [{
        properties: {
            title: 'Cod'
        }
    }, {
        properties: {
            title: 'Haddock'
        }
    }, {
        properties: {
            title: 'Salmon'
        }
    }, {
        properties: {
            title: 'Tuna'
        }
    }];
    fishSection.setItems(fishDataSet);

    var refreshCount = 0;

    function loadTableData() {
        if (refreshCount == 0) {
            // listView.appendSection(vegSection);
            // } else if (refreshCount == 1) {
            listView.appendSection(fishSection);
        }
        refreshCount++;
        listView.closePullView();
    }
    var pullToRefresh = ak.ti.createFromConstructor('PullToRefresh', {
        rclass: 'NZBPTR'
    });
    var listView = Ti.UI.createListView({
        height: '90%',
        top: 0,
        rowHeight: 50,
        sections: sections,
        pullBottomView: pullToRefresh
    });
    // listView.add({
    //     type: 'Ti.UI.ActivityIndicator',
    //     // properties: {
    //     backgroundColor: 'purple',
    //     width: 60,
    //     height: 60
    //         // }
    // });

    // listView.add({
    // bindId: 'testLabel',
    // properties: {
    // height: 50
    // },
    // childTemplates: [{
    // type: 'Ti.UI.View',
    // properties: {
    // width: 'SIZE',
    // height: 'SIZE',
    // layout: 'horizontal',
    // backgroundColor: 'red'
    // },
    // childTemplates: [{
    // bindId: 'arrow',
    // type: 'Ti.UI.Label',
    // properties: {
    // backgroundColor: 'green',
    // font: {
    // size: 14,
    // weight: 'bold'
    // },
    // shadowColor: 'white',
    // shadowRadius: 2,
    // shadowOffset: {
    // x: -10,
    // y: 1
    // },
    // textAlign: 'center',
    // color: '#3A87AD',
    // text: 'a'
    // },
    // }, {
    // bindId: 'label',
    // type: 'Ti.UI.Label',
    // properties: {
    // font: {
    // size: 14,
    // weight: 'bold'
    // },
    // shadowColor: 'white',
    // shadowRadius: 2,
    // shadowOffset: {
    // x: -10,
    // y: 1
    // },
    // textAlign: 'center',
    // color: '#3A87AD',
    // text: 'Pull down to refresh...',
    // backgroundColor: 'blue'
    // },
    // }]
    // }]
    // });
    // listView.testLabel.addEventListener('click', function() {
    // listView.arrow.hide();
    // listView.label.text = 'Loading ...';
    // });
    pullToRefresh.setListView(listView);
    pullToRefresh.addEventListener('pulled', function() {
        listView.showPullView();
        setTimeout(loadTableData, 4000);
    });
    win.add(listView);
    openWin(win);
}

function autoSizeEx() {
    var win = createWin();
    var listView = Ti.UI.createListView({
        templates: JSON.parse(
            '{"default":{"properties":{"rclass":"PushRow","backgroundImage":"/images/cell_background.png","backgroundSelectedImage":"/images/cell_background_on.png","imageCap":{"left":10,"right":9,"top":37,"bottom":9},"left":6,"right":6,"top":3,"bottom":3,"height":"SIZE"},"childTemplates":[{"type":"Ti.UI.View","properties":{"rclass":"PushRowHolder","left":15,"right":10,"bottom":5,"top":16,"layout":"vertical","height":"SIZE"},"childTemplates":[{"type":"Ti.UI.View","properties":{"rclass":"PushRowTopHolder","top":0,"height":95},"childTemplates":[{"type":"Ti.UI.View","properties":{"rclass":"PushRowTitleHolder","height":24,"top":0,"width":"FILL","layout":"horizontal"},"childTemplates":[{"type":"Ti.UI.Label","bindId":"title","properties":{"rclass":"PushRowTitle","borderRadius":4,"backgroundColor":"#e6eaec","height":"FILL","width":"SIZE","maxWidth":"FILL","color":"#686868","padding":{"left":17,"right":5,"bottom":-1},"maxLines":1,"font":{"family":"Roboto Condensed","size":15,"weight":"bold"}},"childTemplates":[{"type":"Ti.UI.Label","properties":{"rclass":"PushRowTitleIcon","font":{"family":"push","size":12,"weight":"normal"},"touchEnabled":false,"left":4,"color":"#00acb4","text":"c"}}]},{"type":"Ti.UI.Label","properties":{"rclass":"PushRowTitleSentIcon","font":{"size":26,"family":"push","weight":"normal"},"left":5,"text":"","touchEnabled":false,"color":"#00acb4"}}]},{"type":"Ti.UI.ImageView","bindId":"avatar","properties":{"rclass":"PushRowAvatar","top":34,"left":0,"width":50,"height":50,"borderRadius":2,"scaleType":2,"retina":true,"localLoadSync":true,"preventDefaultImage":true}},{"type":"Ti.UI.Label","bindId":"description","properties":{"rclass":"PushRowDescription","left":55,"top":34,"height":"FILL","verticalAlign":"top","maxLines":3,"font":{"size":15,"family":"Roboto","weight":"normal"},"padding":{"left":5,"right":5},"color":"#2d2d2d","width":"FILL"}}]},{"type":"Ti.UI.ImageView","bindId":"image","properties":{"callbackId":"image","rclass":"PushRowImage","left":60,"width":"FILL","height":"SIZE","bottom":5,"bubbleParent":true,"borderRadius":2,"preventListViewSelection":true,"scaleType":2,"visible":false,"retina":true,"localLoadSync":true,"preventDefaultImage":true}},{"type":"Ti.UI.View","properties":{"rclass":"PushRowBottomHolder","borderColor":"#EBEEEF","borderWidth":1,"borderPadding":{"left":-1,"bottom":-1,"right":-1},"height":20},"childTemplates":[{"type":"Ti.UI.View","properties":{"rclass":"PushRowTitleHolder","height":24,"top":0,"width":"FILL","layout":"horizontal"}},{"type":"Ti.UI.Label","bindId":"date","properties":{"rclass":"PushRowDate","left":0,"font":{"size":14,"family":"Roboto","weight":"normal"},"height":"FILL","padding":{"top":4},"color":"#acafaf"}},{"type":"Ti.UI.Label","bindId":"from","properties":{"rclass":"PushRowFrom","right":0,"height":"FILL","padding":{"top":4},"font":{"size":14,"family":"Roboto","weight":"normal"},"color":"#acafaf"}}]}]}]}}'
        ),
        defaultItemTemplate: 'default',
        sections: JSON.parse(
            '[{"items":[{"title":{"text":"PERSO É@€%"},"description":{"text":" yoplé!!@#���"},"from":{"html":"<i>from</i> doudounette"},"push":{"id":"1047","content":" yoplé!!@#€","creation":"2014-05-19 19:37:41","creation_timestamp":"1400521061","image":"1","circle":{"id":"1024","name":"perso é@€%","public":"0"},"user":{"id":"2","username":"doudounette","avatar":"1"}},"image":{"image":"http://push-network.com/Api/Push/image?token=932014080706184973aad1da3f407924b3aadd6f6b3309ea&push=1047","visible":true},"avatar":{"image":"http://push-network.com/Api/User/photo?token=932014080706184973aad1da3f407924b3aadd6f6b3309ea&user=2"}},{"title":{"text":"PRV CIRCLE"},"description":{"text":" coucou"},"from":{"html":"<i>from</i> "},"push":{"id":"1030","content":" coucou","creation":"2014-05-18 09:36:52","creation_timestamp":"1400398612","image":"1","circle":{"id":"1026","name":"prv circle","public":"0"},"user":{"id":"","username":"","avatar":""}},"image":{"image":"http://push-network.com/Api/Push/image?token=932014080706184973aad1da3f407924b3aadd6f6b3309ea&push=1030","visible":true}}]}] '
        )
    });

    win.add(listView);
    openWin(win);
}

function collectionViewEx(_args) {
    var win = createWin(_.assign({
        backgroundColor: 'white'
    }, _args));

    var myTemplate = {
        properties: {
            height: 180,

            // unHighlightOnSelect:false,
            backgroundColor: 'green',
            // backgroundSelectedColor: 'yellow'
        },
        childTemplates: [{

            type: 'Ti.UI.ImageView',
            bindId: 'image',
            properties: {
                width: 'FILL',
                height: 'FILL',
                left: 5,
                right: 5,
                top: 5,
                bottom: 5,
                dispatchPressed: true,
                "retina": true,
                "localLoadSync": true,
                "preventDefaultImage": true,
                onlyTransitionIfRemote: true,
                transition: {
                    style: Ti.UI.TransitionStyle.FADE
                },
                scaleType: Ti.UI.SCALE_TYPE_ASPECT_FILL

            },
            childTemplates: [{
                type: 'Ti.UI.Label',
                bindId: 'title',
                properties: {
                    backgroundColor: '#aa000000',
                    width: 'FILL',
                    textAlign: 'center',
                    color: 'white',
                    font: {
                        fontSize: 20,
                        fontWeight: 'bold'
                    },
                    height: 50,
                    maxLines: 2,
                    ellipsize: Ti.UI.TEXT_ELLIPSIZE_TAIL,
                    bottom: 0
                }
            }]
        }]
    };

    var listView = Ti.UI.createCollectionView({
        backgroundColor: "white",
        columnWidth: 130,
        allowsSelection: false,
        selectedBackgroundColor: 'transparent',
        // stickyHeaders: false,
        templates: {
            'template': myTemplate,
            // 'header': {
            //     properties: {
            //         backgroundColor: 'red',
            //         height: 20
            //     },
            //     childTemplates: [{
            //         type: 'Ti.UI.Label',
            //         bindId: 'label'
            //     }]
            // }
        },
        pullView: {
            type: 'Ti.UI.View',
            backgroundColor: 'blue',
            height: 60
        },
        defaultItemTemplate: 'template'
    });

    var sections = [];

    var items, color, movie, nbMovies = movies.length;
    for (var i = 0; i < 10; i++) {
        items = [];
        for (var j = 0; j < 10; j++) {
            color = getRandomColor();
            movie = movies[Math.floor(Math.random() * nbMovies)];
            items.push({
                properties: {
                    searchableText: movie.title
                },
                image: {
                    backgroundColor: color,
                    image: movie.poster_path
                },
                title: {
                    text: movie.title
                },
                year: {
                    text: movie.release_date.split('-')[0]
                },
                status0: {
                    visible: true,
                    color: 'black',
                    html: movie.popularity
                }
            });
        }
        sections.push({
            headerView: {
                properties: {
                    backgroundColor: '#31B7ED',
                    height: 20
                },
                childTemplates: [{
                    type: 'Ti.UI.Label',
                    text: 'Section ' + i
                }]
            },
            items: items
        });
    };
    listView.setSections(sections);
    listView.addEventListener('itemclick', sinfo);
    win.add(listView);
    openWin(win);
}

function listViewExAnim(_args) {
    var win = createWin();
    var listview = Ti.UI.createListView({
        defaultItemTemplate: 'default',
        allowsSelection: false,
        separatorStyle: Ti.UI.ListViewSeparatorStyle.NONE,
        clipChildren: false,
        selectedBackgroundColor: 'transparent',
        useAppearAnimation: true,
        backgroundGradient: {
            type: 'linear',
            colors: ['#3393B8', '#4F7C9C'],
            startPoint: {
                x: 0,
                y: 0,
            },
            endPoint: {
                x: "100%",
                y: "100%",
            }
        },

        templates: {
            "default": {
                "properties": {
                    height: 100
                },
                "childTemplates": [{
                    "type": "Ti.UI.View",
                    "properties": {
                        clipChildren: false,
                        top: 16,
                        bottom: 16,
                        right: 16,
                        left: 16,
                        borderRadius: 6,
                        elevation: 2,
                        translationZ: 2,
                        backgroundColor: 'white'
                    }
                }]
            }
        },
    });
    var items = [];
    for (var i = 0; i < 100; i++) {
        items.push({
            appearAnimation: {
                duration: 300,
                from: {

                    transform: (i % 2 === 0) ? 'ot100%,0' : 'ot-100%,0',
                    opacity: 0
                }
            }
        });
    }
    listview.sections = [{
        items: items
    }];
    win.add(listview);
    openWin(win);
}

function collectionInsideListViewEx(_args) {
    var win = createWin();
    var listview = Ti.UI.createListView({
        defaultItemTemplate: 'default',
        templates: {
            "default": {
                "properties": {
                    height: 200,
                    backgroundColor: 'red'
                },
                "childTemplates": [{
                    bindId: 'collectionView',
                    "type": "Ti.UI.CollectionView",
                    "properties": {
                        scrollDirection: 'horizontal',
                        backgroundColor: 'blue',
                        height: 'FILL',
                        defaultItemTemplate: 'default',
                        templates: {
                            default: {
                                properties: {
                                    width: 100,
                                    height: 'FILL',
                                    backgroundColor: getRandomColor(),
                                },
                                childTemplates: [{

                                    type: 'Ti.UI.ImageView',
                                    bindId: 'image',
                                    properties: {
                                        width: 'FILL',
                                        height: 'FILL',
                                        left: 5,
                                        right: 5,
                                        top: 5,
                                        bottom: 5,
                                        dispatchPressed: true,
                                        "retina": true,
                                        "localLoadSync": true,
                                        "preventDefaultImage": true,
                                        onlyTransitionIfRemote: true,
                                        transition: {
                                            style: Ti.UI.TransitionStyle.FADE
                                        },
                                        scaleType: Ti.UI.SCALE_TYPE_ASPECT_FILL

                                    },
                                    childTemplates: [{
                                        type: 'Ti.UI.Label',
                                        bindId: 'title',
                                        properties: {
                                            backgroundColor: '#aa000000',
                                            width: 'FILL',
                                            textAlign: 'center',
                                            color: 'white',
                                            font: {
                                                fontSize: 20,
                                                fontWeight: 'bold'
                                            },
                                            height: 50,
                                            maxLines: 2,
                                            ellipsize: Ti.UI.TEXT_ELLIPSIZE_TAIL,
                                            bottom: 0
                                        }
                                    }]
                                }]
                            }
                        }
                    }
                }]
            }
        }
    });

    listview.sections = [{
        items: [{
            collectionView: {
                sections: [{
                    items: _.times(100, function(i) {
                        return {
                            properties: {
                                backgroundColor: getRandomColor()
                            },
                            appearAnimation: {
                                duration: 2000,
                                from: {

                                    transform: (i % 2 == 0) ? 'ot100%,0' : 'ot-100%,0',
                                    opacity: 0
                                }
                            }
                        };
                    })
                }]
            }
        }]
    }];
    win.add(listview);
    openWin(win);
}