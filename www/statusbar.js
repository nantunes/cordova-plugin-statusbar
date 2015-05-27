/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/

var exec = require('cordova/exec');

var namedColors = {
    "black": "#000000",
    "darkGray": "#A9A9A9",
    "lightGray": "#D3D3D3",
    "white": "#FFFFFF",
    "gray": "#808080",
    "red": "#FF0000",
    "green": "#00FF00",
    "blue": "#0000FF",
    "cyan": "#00FFFF",
    "yellow": "#FFFF00",
    "magenta": "#FF00FF",
    "orange": "#FFA500",
    "purple": "#800080",
    "brown": "#A52A2A"
};

var StatusBar = {

    isVisible: true,
    statusBarHeight: 0,

    hasNavigationBar: false,
    navigationBarHeight: 0,
    navigationBarWidth: 0,

    overlaysWebView: function (doOverlay) {
        exec(null, null, "StatusBar", "overlaysWebView", [doOverlay]);
    },

    styleDefault: function () {
        // dark text ( to be used on a light background )
        exec(null, null, "StatusBar", "styleDefault", []);
    },

    styleLightContent: function () {
        // light text ( to be used on a dark background )
        exec(null, null, "StatusBar", "styleLightContent", []);
    },

    styleBlackTranslucent: function () {
        // #88000000 ? Apple says to use lightContent instead
        exec(null, null, "StatusBar", "styleBlackTranslucent", []);
    },

    styleBlackOpaque: function () {
        // #FF000000 ? Apple says to use lightContent instead
        exec(null, null, "StatusBar", "styleBlackOpaque", []);
    },

    backgroundColorByName: function (colorname) {
        return StatusBar.backgroundColorByHexString(namedColors[colorname]);
    },

    backgroundColorByHexString: function (hexString) {
        if (hexString.charAt(0) !== "#") {
            hexString = "#" + hexString;
        }

        if (hexString.length === 4) {
            var split = hexString.split("");
            hexString = "#" + split[1] + split[1] + split[2] + split[2] + split[3] + split[3];
        }

        exec(null, null, "StatusBar", "backgroundColorByHexString", [hexString]);
    },

    setStatusBarBackgroundColor: function (hexString) {
        if(namedColors[hexString]) {
            hexString = namedColors[colorname];
        }

        if (hexString.charAt(0) !== "#") {
            hexString = "#" + hexString;
        }

        if (hexString.length < 6) {
            var split = hexString.split("");
            hexString = "#";
            for (var i = 0, il = split.length; i != il; ++i) {
                hexString += split[i] + split[i];
            }
        }

        exec(null, null, "StatusBar", "setStatusBarBackgroundColor", [hexString]);
    },

    setNavigationBarBackgroundColor: function (hexString) {
        if(namedColors[hexString]) {
            hexString = namedColors[colorname];
        }

        if (hexString.charAt(0) !== "#") {
            hexString = "#" + hexString;
        }

        if (hexString.length < 6) {
            var split = hexString.split("");
            hexString = "#";
            for (var i = 0, il = split.length; i != il; ++i) {
                hexString += split[i] + split[i];
            }
        }

        exec(null, null, "StatusBar", "setNavigationBarBackgroundColor", [hexString]);
    },

    setSystemBarsBackgroundColor: function (hexString) {
        if(namedColors[hexString]) {
            hexString = namedColors[colorname];
        }

        if (hexString.charAt(0) !== "#") {
            hexString = "#" + hexString;
        }

        if (hexString.length < 6) {
            var split = hexString.split("");
            hexString = "#";
            for (var i = 0, il = split.length; i != il; ++i) {
                hexString += split[i] + split[i];
            }
        }

        exec(null, null, "StatusBar", "setNavigationBarBackgroundColor", [hexString]);
    },

    hide: function () {
        exec(null, null, "StatusBar", "hide", []);
        StatusBar.isVisible = false;
    },

    show: function () {
        exec(null, null, "StatusBar", "show", []);
        StatusBar.isVisible = true;
    },

    hideStatusBar: function () {
        exec(null, null, "StatusBar", "hideStatusBar", []);
        StatusBar.isVisible = false;
    },

    showStatusBar: function () {
        exec(null, null, "StatusBar", "showStatusBar", []);
        StatusBar.isVisible = true;
    },

    hideNavigationBar: function () {
        exec(null, null, "StatusBar", "hideNavigationBar", []);
        StatusBar.isNavigationBarVisible = false;
    },

    showNavigationBar: function () {
        exec(null, null, "StatusBar", "showNavigationBar", []);
        StatusBar.isNavigationBarVisible = true;
    },

    hideSystemBars: function () {
        exec(null, null, "StatusBar", "hideSystemBars", []);
        StatusBar.isVisible = false;
        StatusBar.isNavigationBarVisible = false;
    },

    showSystemBars: function () {
        exec(null, null, "StatusBar", "showSystemBars", []);
        StatusBar.isVisible = true;
        StatusBar.isNavigationBarVisible = true;
    }

};

// prime it
exec(function (res) {
    if (typeof res == 'object') {
        if (res.type == 'tap') {
            cordova.fireWindowEvent('statusTap');
        } else {
            var density = window.devicePixelRatio;

            StatusBar.isVisible = res.statusBarVisible;
            StatusBar.statusBarHeight = res.statusBarHeight / density;

            if(StatusBar.hasNavigationBar = res.hasNavigationBar) {
                StatusBar.isNavigationBarVisible = res.navigationBarVisible;
                StatusBar.navigationBarHeight = res.navigationBarHeight / density;
                StatusBar.navigationBarWidth = res.navigationBarWidth / density;
            }
        }
    } else {
        StatusBar.isVisible = res;
    }
}, null, "StatusBar", "_ready", []);

module.exports = StatusBar;
