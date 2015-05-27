/*
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
package org.apache.cordova.statusbar;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewConfiguration;
import android.view.KeyCharacterMap;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;

public class StatusBar extends CordovaPlugin {
    private static final String TAG = "StatusBar";

    private int status_bar_height = 0;

    private int system_ui_visibility_flags = 0;

    private boolean has_navigation_bar = true;

    private boolean status_bar_overlays_webview = false;
    private boolean navigation_bar_overlays_webview = false;

    private int navigation_bar_height = 0;
    private int navigation_bar_width = 0;

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    @Override
    public void initialize(final CordovaInterface cordova, CordovaWebView webView) {
        Log.v(TAG, "StatusBar: initialization");
        super.initialize(cordova, webView);

        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Clear flag FLAG_FORCE_NOT_FULLSCREEN which is set initially
                // by the Cordova.
                Window window = cordova.getActivity().getWindow();
                window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

                // Method and constants not available on all SDKs but we want to be able to compile this code with any SDK
                if (Build.VERSION.SDK_INT >= 16) {
                    boolean system_bars_overlays_webview = preferences.getBoolean("SystemBarsOverlaysWebView", false);

                    status_bar_overlays_webview = preferences.getBoolean("StatusBarOverlaysWebView", system_bars_overlays_webview);
                    navigation_bar_overlays_webview = preferences.getBoolean("NavigationBarOverlaysWebView", system_bars_overlays_webview);

                    if (status_bar_overlays_webview || navigation_bar_overlays_webview) {
                        system_ui_visibility_flags = 0x00000100; // SDK 16: WindowManager.LayoutParams.SYSTEM_UI_FLAG_LAYOUT_STABLE

                        if (status_bar_overlays_webview) {
                            system_ui_visibility_flags |= 0x00000400; // SDK 16: WindowManager.LayoutParams.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        }

                        if (navigation_bar_overlays_webview) {
                            system_ui_visibility_flags |= 0x00000200; // SDK 16: WindowManager.LayoutParams.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        }

                        if(Build.VERSION.SDK_INT >= 19) {
                            system_ui_visibility_flags |= 0x00001000; // SDK 19: SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        }

                        window.getDecorView().setSystemUiVisibility(system_ui_visibility_flags);
                    }

                    if (Build.VERSION.SDK_INT >= 19) {
                        window.clearFlags(0x04000000); // SDK 19: WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

                        if (Build.VERSION.SDK_INT >= 21) {
                            window.addFlags(0x80000000); // SDK 21: WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                        }
                    }
                }

                updateScreenInfo(cordova.getActivity());

                // Read 'SystemBarsBackgroundColor' from config.xml, default is #000000.
                String systembars_color = preferences.getString("SystemBarsBackgroundColor", "#000000");

                // Read 'StatusBarBackgroundColor' from config.xml, default is system bars global color.
                setStatusBarBackgroundColor(preferences.getString("StatusBarBackgroundColor", systembars_color));

                // Read 'NavigationBarBackgroundColor' from config.xml, default is system bars global color.
                setNavigationBarBackgroundColor(preferences.getString("NavigationBarBackgroundColor", systembars_color));
            }
        });
    }

    private void updateScreenInfo(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            Resources res = activity.getResources();

            this.status_bar_height = 0;
            // if there is no overlay the status bar height is zero, for all that concerns the webview
            if (status_bar_overlays_webview) {
                int resourceId = res.getIdentifier("status_bar_height", "dimen", "android");
                if (resourceId > 0) {
                    this.status_bar_height = res.getDimensionPixelSize(resourceId);
                }
            }

            this.has_navigation_bar = this.hasNavigationBar(activity);

            this.navigation_bar_height = 0;
            this.navigation_bar_width = 0;
            // if there is no overlay the navigation bar dimensions are zero, for all that concerns the webview
            if (has_navigation_bar && navigation_bar_overlays_webview) {
                int resourceId = res.getIdentifier("navigation_bar_height", "dimen", "android");

                if (resourceId > 0) {
                    this.navigation_bar_height = res.getDimensionPixelSize(resourceId);
                }

                resourceId = res.getIdentifier("navigation_bar_width", "dimen", "android");

                if (resourceId > 0) {
                    this.navigation_bar_width = res.getDimensionPixelSize(resourceId);
                }
            }
        }
    }

    private boolean hasNavigationBar(Activity activity) {
        Resources res = activity.getResources();

        int resourceId = res.getIdentifier("config_showNavigationBar", "bool", "android");

        if (resourceId != 0) {
            boolean hasNav = res.getBoolean(resourceId);

            try {
                Class c = Class.forName("android.os.SystemProperties");
                Method m = c.getDeclaredMethod("get", String.class);
                m.setAccessible(true);
                String v = (String) m.invoke(null, "qemu.hw.mainkeys");

                return !v.equals("1") && (v.equals("0") || hasNav);
            } catch (Throwable ignored) {
            }

            return hasNav;
        } else {
            boolean has_menu_key = false;

            ViewConfiguration vc = ViewConfiguration.get(activity.getBaseContext());

            try {
                // Using reflection makes sure any 5.0+ device will work without having to compile with SDK level 14
                has_menu_key = (Boolean) vc.getClass().getDeclaredMethod("hasPermanentMenuKey").invoke(vc);
            } catch (Exception ignore) {
            }

            return !has_menu_key && !KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
        }
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  True if the action was valid, false otherwise.
     */
    @Override
    public boolean execute(final String action, final CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        Log.v(TAG, "Executing action: " + action);
        final Activity activity = this.cordova.getActivity();
        final Window window = activity.getWindow();
        if ("_ready".equals(action)) {
            boolean statusBarVisible = (window.getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) == 0;

            JSONObject r = new JSONObject();
            r.put("statusBarVisible", statusBarVisible);
            r.put("statusBarHeight", this.status_bar_height);
            r.put("hasNavigationBar", this.has_navigation_bar);
            r.put("navigationBarHeight", this.navigation_bar_height);
            r.put("navigationBarWidth", this.navigation_bar_width);

            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, r));
            return true;
        }

        if ("show".equals(action) || "showStatusBar".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
            });
            return true;
        }

        if ("hide".equals(action) || "hideStatusBar".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
            });
            return true;
        }

        if ("showNavigationBar".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= 14) {
                        system_ui_visibility_flags &= ~0x00000002; // SDK 14: View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

                        window.getDecorView().setSystemUiVisibility(system_ui_visibility_flags);
                    }
                }
            });
            return true;
        }

        if ("hideNavigationBar".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= 14) {
                        system_ui_visibility_flags |= 0x00000002; // SDK 14: View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

                        window.getDecorView().setSystemUiVisibility(system_ui_visibility_flags);
                    }
                }
            });
            return true;
        }

        if ("showSystemBars".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

                    if (Build.VERSION.SDK_INT >= 14) {
                        system_ui_visibility_flags &= ~0x00000002; // SDK 14: View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

                        window.getDecorView().setSystemUiVisibility(system_ui_visibility_flags);
                    }
                }
            });
            return true;
        }

        if ("hideSystemBars".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

                    if (Build.VERSION.SDK_INT >= 14) {
                        system_ui_visibility_flags |= 0x00000002; // SDK 14: View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

                        window.getDecorView().setSystemUiVisibility(system_ui_visibility_flags);
                    }
                }
            });
            return true;
        }

        if ("backgroundColorByHexString".equals(action) || "setStatusBarBackgroundColor".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        setStatusBarBackgroundColor(args.getString(0));
                    } catch (JSONException ignore) {
                        Log.e(TAG, "Invalid hexString argument, use f.i. '#777777'");
                    }
                }
            });
            return true;
        }

        if ("setNavigationBarBackgroundColor".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        setNavigationBarBackgroundColor(args.getString(0));
                    } catch (JSONException ignore) {
                        Log.e(TAG, "Invalid hexString argument, use f.i. '#777777'");
                    }
                }
            });
            return true;
        }

        if ("setSystemBarsBackgroundColor".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        setStatusBarBackgroundColor(args.getString(0));
                        setNavigationBarBackgroundColor(args.getString(0));
                    } catch (JSONException ignore) {
                        Log.e(TAG, "Invalid hexString argument, use f.i. '#777777'");
                    }
                }
            });
            return true;
        }

        return false;
    }

    private void setStatusBarBackgroundColor(final String colorPref) {
        if (Build.VERSION.SDK_INT >= 21) {
            if (colorPref != null && !colorPref.isEmpty()) {
                final Window window = cordova.getActivity().getWindow();

                try {
                    // Using reflection makes sure any 5.0+ device will work without having to compile with SDK level 21
                    window.getClass().getDeclaredMethod("setStatusBarColor", int.class).invoke(window, Color.parseColor(colorPref));
                } catch (IllegalArgumentException ignore) {
                    Log.e(TAG, "Invalid hexString argument, use f.i. '#999999'");
                } catch (Exception ignore) {
                    // this should not happen, only in case Android removes this method in a version > 21
                    Log.w(TAG, "Method window.setStatusBarColor not found for SDK level " + Build.VERSION.SDK_INT);
                }
            }
        }
    }

    private void setNavigationBarBackgroundColor(final String colorPref) {
        if (Build.VERSION.SDK_INT >= 21) {
            if (colorPref != null && !colorPref.isEmpty()) {
                final Window window = cordova.getActivity().getWindow();

                try {
                    // Using reflection makes sure any 5.0+ device will work without having to compile with SDK level 21
                    window.getClass().getDeclaredMethod("setNavigationBarColor", int.class).invoke(window, Color.parseColor(colorPref));
                } catch (IllegalArgumentException ignore) {
                    Log.e(TAG, "Invalid hexString argument, use f.i. '#999999'");
                } catch (Exception ignore) {
                    // this should not happen, only in case Android removes this method in a version > 21
                    Log.w(TAG, "Method window.setNavigationBarColor not found for SDK level " + Build.VERSION.SDK_INT);
                }
            }
        }
    }
}
