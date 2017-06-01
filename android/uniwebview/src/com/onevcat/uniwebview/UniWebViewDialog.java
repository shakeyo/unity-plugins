/**
 * Created by onevcat on 2013/12/02.
 * You can modify, rebuild and use this file if you purchased it.
 * But you can not redistribute it in any form.
 * Copyright and all rights reserved OneV's Den.
 */
package com.onevcat.uniwebview;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.text.Editable;
import android.util.Log;
import android.view.*;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.webkit.*;
import android.widget.EditText;
import android.widget.FrameLayout;
import com.unity3d.player.UnityPlayer;

import java.util.ArrayList;
import java.util.HashMap;

@SuppressLint("NewApi")
public class UniWebViewDialog extends Dialog {

    public ArrayList<String> schemes;
    private HashMap<String, String> headers;
    private ArrayList<String> trustSites;

    public static final int ANIMATION_EDGE_NONE = 0;
    public static final int ANIMATION_EDGE_TOP = 1;
    public static final int ANIMATION_EDGE_LEFT = 2;
    public static final int ANIMATION_EDGE_BOTTOM = 3;
    public static final int ANIMATION_EDGE_RIGHT = 4;

    private FrameLayout _content;
    private ProgressDialog _spinner;
    private UniWebView _uniWebView;
    private DialogListener _listener;
    private boolean _showSpinnerWhenLoading = true;
    private String _spinnerText = "Loading...";
    private boolean _isLoading;
    private boolean _loadingInterrupted;
    private int _top, _left, _bottom, _right;
    private AlertDialog _alertDialog;
    private String _currentUrl = "";

    private int _backgroundColor = Color.WHITE;

    private boolean _backButtonEnable = true;
    private boolean _manualHide;
    private boolean _animating = false;

    private boolean _canGoBack;
    private boolean _canGoForward;

    private String _currentUserAgent;
    private float alpha = 1.0f;

    private boolean _immersiveMode = true;
    private boolean _openLinksInExternalBrowser;


    public UniWebView getWebView() {
        return _uniWebView;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(AndroidPlugin.LOG_TAG, "onKeyDown " + event);
        this._listener.onDialogKeyDown(this, keyCode);
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!_backButtonEnable) {
                return true;
            } else if (!goBack()) {
                this._listener.onDialogShouldCloseByBackButton(this);
            }
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @SuppressLint("NewApi")
    public UniWebViewDialog(Context context, DialogListener listener) {
        super(context,android.R.style.Theme_Holo_NoActionBar);
        this._listener = listener;

        schemes = new ArrayList<String>();
        schemes.add("uniwebview");

        trustSites = new ArrayList<String>();
        headers = new HashMap<String, String>();

        Window window = this.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        if (Build.VERSION.SDK_INT < 16) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            HideSystemUI();
        }


        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        createContent();
        createWebView();
        createSpinner();

        addContentView(this._content,
                       new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        this._content.addView(this._uniWebView);
        Log.d(AndroidPlugin.LOG_TAG, "Create a new UniWebView Dialog");

        // Fix input method showing causes ui show issue.
        this._content.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                _content.getWindowVisibleDisplayFrame(r);
                boolean visible = (Math.abs(r.height() - _content.getHeight()) > 128);
                if (visible) {
                    HideSystemUI();
                }
            }
        });
    }

    @SuppressLint("NewApi")
    public void HideSystemUI() {

            if (Build.VERSION.SDK_INT >= 16) {
                final View decorView = getWindow().getDecorView();

                int uiOptions = 0;
                if (Build.VERSION.SDK_INT >= 19 && _immersiveMode) {
                    uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE;
                } else {
                    uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
                }
                decorView.setSystemUiVisibility(uiOptions);

                int updatedUIOptions = 0;
                // Fix input method showing causes ui show issue when slide up for navigation bar.
                if (Build.VERSION.SDK_INT >= 19 && _immersiveMode) {
                    updatedUIOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                } else {
                    updatedUIOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
                }

                final int finalUiOptions = updatedUIOptions;
                decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int i) {
                        decorView.setOnSystemUiVisibilityChangeListener(null);
                        decorView.setSystemUiVisibility(finalUiOptions);
                    }
                });
            }

    }

    public void changeInsets(int top, int left, int bottom, int right) {
        _top = top;
        _left = left;
        _bottom = bottom;
        _right = right;
        updateContentSize();
    }

    public void load(String url) {
        Log.d(AndroidPlugin.LOG_TAG, url);
        _uniWebView.loadUrl(url, headers);
    }

    public void addJs(String js) {
        if (js == null) {
            Log.d(AndroidPlugin.LOG_TAG, "Trying to add a null js. Abort.");
            return;
        }

        String requestString = String.format("javascript:%s",js);
        load(requestString);
    }

    public void loadJS(String js) {
        if (js == null) {
            Log.d(AndroidPlugin.LOG_TAG, "Trying to eval a null js. Abort.");
            return;
        }

        String jsReformat = js.trim();

        while (jsReformat.endsWith(";") && jsReformat.length() != 0) {
            jsReformat = jsReformat.substring(0, jsReformat.length()-1);
        }

        String requestString = String.format("javascript:android.onData(%s)", jsReformat);
        load(requestString);
    }

    public void loadHTMLString(String html, String baseURL) {
        _uniWebView.loadDataWithBaseURL(baseURL, html, "text/html", "UTF-8", null);
    }

    public void cleanCache() {
        _uniWebView.clearCache(true);
    }

    public boolean goBack() {
        if (_uniWebView.canGoBack()) {
            _uniWebView.goBack();
            return true;
        } else {
            return false;
        }
    }

    public boolean goForward() {
        if (_uniWebView.canGoForward()) {
            _uniWebView.goForward();
            return true;
        } else {
            return false;
        }
    }

    public void destroy() {
        _uniWebView.loadUrl("about:blank");
        UniWebViewManager.Instance().removeShowingWebViewDialog(this);
        this.dismiss();
    }

    protected void onStop() {
        this._listener.onDialogClose(this);
    }

    private void showDialog() {
        if (Build.VERSION.SDK_INT >= 19 && _immersiveMode) {
            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            this.show();
            this.getWindow().getDecorView().setSystemUiVisibility(
                    this.getWindow().getDecorView().getSystemUiVisibility());
            this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        } else {
            this.show();
        }
    }

    public void setShow(final boolean show, final boolean fade, int direction, float duration) {

        if (_animating) {
            Log.d(AndroidPlugin.LOG_TAG, "Trying to animate but another transition animation is not finished yet. Ignore this one.");
            return;
        }

        if (show) {
            showDialog();
            if (this._showSpinnerWhenLoading && this._isLoading) {
                showSpinner();
            }

            UniWebViewManager.Instance().addShowingWebViewDialog(this);
            _manualHide = false;
        } else {
            InputMethodManager imm = (InputMethodManager)UnityPlayer.currentActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(_uniWebView.getWindowToken(), 0);
            this._spinner.hide();
            _manualHide = true;
        }

        if (fade || direction != ANIMATION_EDGE_NONE) {
            _animating = true;

            final View v = ((ViewGroup)this.getWindow().getDecorView())
                    .getChildAt(0);
            AnimationSet set = new AnimationSet(false);

            int durationMills = (int)(duration * 1000);

            if (fade) {
                float startAlpha = show ? 0.0f : 1.0f;
                float endAlpha = show ? 1.0f : 0.0f;
                Animation a = new AlphaAnimation(startAlpha, endAlpha);
                a.setFillAfter(true);
                a.setDuration(durationMills);
                set.addAnimation(a);
            }

            int xValue, yValue;
            Point size = displaySize();
            if (direction == ANIMATION_EDGE_TOP) {
                xValue = 0; yValue = -size.y;
            } else if (direction == ANIMATION_EDGE_LEFT) {
                xValue = -size.x; yValue = 0;
            } else if (direction == ANIMATION_EDGE_BOTTOM) {
                xValue = 0; yValue = size.y;
            } else if (direction == ANIMATION_EDGE_RIGHT) {
                xValue = size.x; yValue = 0;
            } else if (direction == ANIMATION_EDGE_NONE) {
                xValue = 0; yValue = 0;
            } else {
                Log.d(AndroidPlugin.LOG_TAG, "Unknown direction. You should send 0~5");
                return;
            }

            if (direction != ANIMATION_EDGE_NONE) {
                Animation a = new TranslateAnimation(show ? xValue : 0, show ? 0 : xValue, show ? yValue : 0, show ? 0 : yValue);
                a.setFillAfter(true);
                a.setDuration(durationMills);
                set.addAnimation(a);
            }

            v.startAnimation(set);

            Handler h = new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    _animating = false;
                    v.clearAnimation();

                    if (show) {
                        _listener.onShowTransitionFinished(UniWebViewDialog.this);
                    } else {
                        UniWebViewDialog.this.hide();
                        _listener.onHideTransitionFinished(UniWebViewDialog.this);
                    }

                }
            }, durationMills);
        } else {
            if (show) {
                _listener.onShowTransitionFinished(UniWebViewDialog.this);
            } else {
                this.hide();
                _listener.onHideTransitionFinished(UniWebViewDialog.this);
            }

        }
    }

    Point displaySize() {
        Window window = this.getWindow();
        Display display = window.getWindowManager().getDefaultDisplay();

        if (Build.VERSION.SDK_INT >= 19 && _immersiveMode) { // For immersive mode
            Point size = new Point();
            display.getRealSize(size);
            return size;
        } else if (android.os.Build.VERSION.SDK_INT >= 13) {
            Point size = new Point();
            display.getSize(size);
            return size;
        } else {
            return new Point(display.getWidth(), display.getHeight());
        }
    }

    public void updateContentSize() {
        Window window = this.getWindow();

        Point size = displaySize();
        int width = Math.max(0, size.x - _left - _right);
        int height = Math.max(0, size.y - _top - _bottom);

        if (width == 0 || height == 0) {
            Log.d(AndroidPlugin.LOG_TAG, "The inset is lager then screen size. Webview will not show. Please check your insets setting.");
            return;
        }

        window.setLayout(width, height);

        WindowManager.LayoutParams layoutParam = window.getAttributes();
        layoutParam.gravity = Gravity.TOP | Gravity.LEFT;

        layoutParam.x = _left;
        layoutParam.y = _top;

        window.setAttributes(layoutParam);
    }

    public void setSpinnerShowWhenLoading(boolean showSpinnerWhenLoading) {
        this._showSpinnerWhenLoading = showSpinnerWhenLoading;
    }

    public void setSpinnerText(String text) {
        if (text != null) {
            this._spinnerText = text;
        } else {
            this._spinnerText = "";
        }
        this._spinner.setMessage(text);
    }

    private void showSpinner() {
        if (Build.VERSION.SDK_INT >= 19 && _immersiveMode) {
            this._spinner.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

            this._spinner.show();

            this._spinner.getWindow().getDecorView().setSystemUiVisibility(
                    this.getWindow().getDecorView().getSystemUiVisibility());
            this._spinner.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        } else {
            this._spinner.show();
        }
    }

    private void createContent() {
        this._content = new FrameLayout(getContext());
        this._content.setVisibility(View.VISIBLE);
    }

    private void createSpinner() {
        this._spinner = new ProgressDialog(getContext());
        this._spinner.setCanceledOnTouchOutside(true);
        this._spinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this._spinner.setMessage(this._spinnerText);
    }

    private void createWebView() {
        _uniWebView = new UniWebView(getContext());

        WebViewClient webClient = new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                UniWebViewDialog.this._canGoBack = view.canGoBack();
                UniWebViewDialog.this._canGoForward = view.canGoForward();

                Log.d(AndroidPlugin.LOG_TAG, "Start Loading URL: " + url);
                super.onPageStarted(view, url, favicon);
                if (UniWebViewDialog.this._showSpinnerWhenLoading && UniWebViewDialog.this.isShowing()) {
                    UniWebViewDialog.this.showSpinner();
                }
                UniWebViewDialog.this._isLoading = true;
                UniWebViewDialog.this._listener.onPageStarted(UniWebViewDialog.this,url);
                HideSystemUI();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                UniWebViewDialog.this._canGoBack = view.canGoBack();
                UniWebViewDialog.this._canGoForward = view.canGoForward();

                UniWebViewDialog.this._spinner.hide();
                _currentUrl = url;
                _currentUserAgent = _uniWebView.getSettings().getUserAgentString();
                UniWebViewDialog.this._listener.onPageFinished(UniWebViewDialog.this, url);
                UniWebViewDialog.this._isLoading = false;
                _uniWebView.setWebViewBackgroundColor(_backgroundColor);
                HideSystemUI();
            }

            @Override
            public void onReceivedError (WebView view, int errorCode, String description, String failingUrl) {
                UniWebViewDialog.this._canGoBack = view.canGoBack();
                UniWebViewDialog.this._canGoForward = view.canGoForward();
                
                HideSystemUI();
                UniWebViewDialog.this._spinner.hide();
                _currentUrl = failingUrl;
                _currentUserAgent = _uniWebView.getSettings().getUserAgentString();
                UniWebViewDialog.this._listener.onReceivedError(UniWebViewDialog.this,errorCode,description,failingUrl);
                UniWebViewDialog.this._isLoading = false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(AndroidPlugin.LOG_TAG,"shouldOverrideUrlLoading: " + url);
                return UniWebViewDialog.this._listener.shouldOverrideUrlLoading(UniWebViewDialog.this, url);
            }
        };

        _uniWebView.setWebViewClient(webClient);

        FrameLayout customViewContainer = new FrameLayout(getContext());
        
        customViewContainer.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        addContentView(customViewContainer, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));


        UniWebChromeClient chromeClient = new UniWebChromeClient(_content, customViewContainer, null, _uniWebView) {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                Log.d(AndroidPlugin.LOG_TAG, "onPermissionRequest");
                AndroidPlugin.getUnityActivity_().runOnUiThread(new Runnable() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void run() {
                        String url = request.getOrigin().toString();
                        Log.d(AndroidPlugin.LOG_TAG, "Request from url: " + url);
                        if(trustSites.contains(url)) {
                            request.grant(request.getResources());
                        } else {
                            request.deny();
                        }
                    }
                });
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(UniWebViewDialog.this.getContext());
                _alertDialog = alertDialogBuilder
                        .setTitle(url)
                        .setMessage(message)
                        .setCancelable(false)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.ok, new OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                result.confirm();
                                _alertDialog = null;
                            }
                        }).create();

                if (Build.VERSION.SDK_INT >= 19 && _immersiveMode) {
                    _alertDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                    _alertDialog.show();
                    _alertDialog.getWindow().getDecorView().setSystemUiVisibility(
                            UniWebViewDialog.this.getWindow().getDecorView().getSystemUiVisibility());
                    _alertDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                } else {
                    _alertDialog.show();
                }

                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(UniWebViewDialog.this.getContext());
                _alertDialog = alertDialogBuilder
                        .setTitle(url)
                        .setMessage(message)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes, new OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                                result.confirm();
                                _alertDialog = null;
                            }
                        })
                        .setNegativeButton(android.R.string.no, new OnClickListener() {
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.dismiss();
                                result.cancel();
                                _alertDialog = null;
                            }
                        }).show();
                return true;
            }

            @Override
            public boolean onJsPrompt (WebView view, String url, String message, String defaultValue, final JsPromptResult result) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(UniWebViewDialog.this.getContext());
                alertDialogBuilder
                        .setTitle(url)
                        .setMessage(message)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setCancelable(false);

                final EditText input = new EditText(UniWebViewDialog.this.getContext());
                input.setSingleLine();
                alertDialogBuilder.setView(input);

                alertDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Editable editable = input.getText();
                        String value = "";
                        if (editable != null) {
                            value = editable.toString();
                        }
                        dialog.dismiss();
                        result.confirm(value);
                        _alertDialog = null;
                    }
                });

                alertDialogBuilder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                        result.cancel();
                        _alertDialog = null;
                    }
                });
                _alertDialog = alertDialogBuilder.show();

                return true;
            }
        };

        _uniWebView.setWebChromeClient(chromeClient);

        _uniWebView.setDownloadListener(new DownloadListener() {
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype,
                                        long contentLength) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                AndroidPlugin.getUnityActivity_().startActivity(i);
            }
        });

        this._uniWebView.setVisibility(View.VISIBLE);

        _uniWebView.addJavascriptInterface(this, "android");

        setBounces(false);
    }

    @JavascriptInterface
    public void onData(String value) {
        Log.d(AndroidPlugin.LOG_TAG, "receive a call back from js: " + value);
        this._listener.onJavaScriptFinished(this, value);
    }

    public void goBackGround() {
        if (_isLoading) {
            _loadingInterrupted = true;
            this._uniWebView.stopLoading();
        }
        if (this._alertDialog != null) {
            this._alertDialog.hide();
        }
        this.hide();

        if (Build.VERSION.SDK_INT >= 11) {
            _uniWebView.onPause();
        }
    }

    public void goForeGround() {
        if (!_manualHide) {
            if (_loadingInterrupted) {
                //this._uniWebView.reload();
                _loadingInterrupted = false;
            }
            this.show();
            if (this._alertDialog != null) {
                this._alertDialog.show();
            }
        }
        if (Build.VERSION.SDK_INT >= 11) {
            _uniWebView.onResume();
        }
    }

    public void setTransparent(boolean transparent) {

        Log.d(AndroidPlugin.LOG_TAG, "SetTransparentBackground is already deprecated and there is no guarantee it will work in later versions. You should use SetBackgroundColor instead.");

        if (transparent) {
            _backgroundColor = Color.argb(0, 0, 0, 0);
        } else {
            _backgroundColor = Color.argb(255, 255, 255, 255);
        }

        _uniWebView.setWebViewBackgroundColor(_backgroundColor);
    }

    public void setBackgroundColor(float r, float g, float b, float a) {

        int redInt = (int)(r * 255);
        int greenInt = (int)(g * 255);
        int blueInt = (int)(b * 255);
        int alphaInt = (int)(a * 255);

        int color = Color.argb(alphaInt, redInt, greenInt, blueInt);

        _backgroundColor = color;
        _uniWebView.setWebViewBackgroundColor(_backgroundColor);
    }

    public String getUrl() {
        return _currentUrl;
    }

    public void setBackButtonEnable(boolean enable) {
        _backButtonEnable = enable;
    }

    public void setBounces(boolean enable) {
        if (android.os.Build.VERSION.SDK_INT <= 8) {
            Log.d(AndroidPlugin.LOG_TAG, "WebView over scroll effect supports after API 9");
        } else {
            if (enable) {
                _uniWebView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
            } else {
                _uniWebView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            }
        }
    }

    public void setZoomEnable(boolean enable) {
        _uniWebView.getSettings().setBuiltInZoomControls(enable);
    }

    public void reload() {
        _uniWebView.reload();
    }

    public void addUrlScheme(String scheme) {
        if (!schemes.contains(scheme)) {
            schemes.add(scheme);
        }
    }

    public void removeUrlScheme(String scheme) {
        if (schemes.contains(scheme)) {
            schemes.remove(scheme);
        }
    }

    public void stop() {
        _uniWebView.stopLoading();
    }

    public void useWideViewPort(boolean use) {
        _uniWebView.getSettings().setUseWideViewPort(use);
    }

    public String getUserAgent() {
        return _currentUserAgent;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
        if (Build.VERSION.SDK_INT < 11) {
            final AlphaAnimation animation = new AlphaAnimation(this.alpha, this.alpha);
            animation.setDuration(0);
            animation.setFillAfter(true);
            _uniWebView.startAnimation(animation);
        } else {
            _uniWebView.setAlpha(this.alpha);
        }
    }

    public float getAlpha() {
        return alpha;
    }

    public void setImmersiveModeEnabled(boolean immersiveModeEnabled) {
        _immersiveMode = immersiveModeEnabled;
    }

    public void AddPermissionRequestTrustSite(String site) {
        if (site != null && site.length() != 0) {
            trustSites.add(site);
        }
    }

    public boolean canGoBack() {
        return _canGoBack;
    }

    public boolean canGoForward() {
        return _canGoForward;
    }

    public void setPosition(int x, int y) {
        Window window = this.getWindow();
        WindowManager.LayoutParams layoutParam = window.getAttributes();
        layoutParam.x = x;
        layoutParam.y = y;
        window.setAttributes(layoutParam);
    }

    public void setSize(int width, int height) {
        if (width < 0 || height < 0) {
            Log.d(AndroidPlugin.LOG_TAG, "The width or height of size is less than 0. Webview will not show. Please check your setting. Input width: " + width + ", input height: " + height);
            return;
        }

        Window window = this.getWindow();
        window.setLayout(width, height);
    }

    public void animateTo(int deltaX, int deltaY, float duration, float delay, final String identifier) {
        final View v = ((ViewGroup)this.getWindow().getDecorView())
                .getChildAt(0);

        int durationInMills = (int)(duration * 1000);
        int delayInMills = (int)(delay * 1000);
        Animation a = new TranslateAnimation(0, deltaX, 0, deltaY);
        a.setFillAfter(true);
        a.setDuration(durationInMills);
        a.setStartOffset(delayInMills);
        v.startAnimation(a);

        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                _listener.onAnimationFinished(UniWebViewDialog.this, identifier);
            }
        }, durationInMills + delayInMills);
    }

    public void setHeaderField(String key, String value) {
        if (value == null || value.length() == 0) {
            headers.remove(key);
        } else {
            if (key != null && key.length() != 0) {
                headers.put(key, value);
            }
        }
    }

    public void setVerticalScrollBarShow(boolean show) {
        _uniWebView.setVerticalScrollBarEnabled(show);
    }

    public void setHorizontalScrollBarShow(boolean show) {
        _uniWebView.setHorizontalScrollBarEnabled(show);
    }

    public boolean getOpenLinksInExternalBrowser() {
        return _openLinksInExternalBrowser;
    }

    public void setOpenLinksInExternalBrowser(boolean value) {
        _openLinksInExternalBrowser = value;
    }

    public static abstract interface DialogListener {
        public abstract void onPageFinished(UniWebViewDialog dialog, String url);
        public abstract void onPageStarted(UniWebViewDialog dialog, String url);
        public abstract void onReceivedError(UniWebViewDialog dialog, int errorCode, String description, String failingUrl);
        public abstract boolean shouldOverrideUrlLoading(UniWebViewDialog dialog, String url);
        public abstract void onDialogShouldCloseByBackButton(UniWebViewDialog dialog);
        public abstract void onDialogKeyDown(UniWebViewDialog dialog, int keyCode);
        public abstract void onDialogClose(UniWebViewDialog dialog);
        public abstract void onJavaScriptFinished(UniWebViewDialog dialog, String result);
        public abstract void onAnimationFinished(UniWebViewDialog dialog, String identifier);
        public abstract void onShowTransitionFinished(UniWebViewDialog dialog);
        public abstract void onHideTransitionFinished(UniWebViewDialog dialog);
    }

}
