/**
 * Created by onevcat on 2013/10/20.
 * You can modify, rebuild and use this file if you purchased it.
 * But you can not redistribute it in any form.
 * Copyright and all rights reserved OneV's Den.
 */
package com.UGS.NativePlugins;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.ValueCallback;
import cn.beecloud.async.BCCallback;
import cn.beecloud.async.BCResult;
import rong.unity.demo.RongUnityActivity;

import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;
import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerNativeActivity;
import com.zxinsight.MLink;
import com.zxinsight.MWConfiguration;
import com.zxinsight.MagicWindowSDK;
import com.zxinsight.Session;
import com.zxinsight.mlink.MLinkCallback;
import com.zxinsight.mlink.MLinkIntentBuilder;

import java.util.ArrayList;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class AndroidPlugin extends RongUnityActivity
{
    public final static int FILECHOOSER_RESULTCODE = 19238467;

    protected static ValueCallback<Uri> _uploadMessages;
    protected static ValueCallback<Uri[]> _uploadCallback;
    protected static String _cameraPhotoPath;
    
    protected static Boolean _initMW = false;
    protected static String _mwData = "";
      
    protected static final String LOG_TAG = "UGS";

    public static Activity getUnityActivity_() {
        return UnityPlayer.currentActivity;
    }
    
    public static String getVersion(){
    	return DefineConst.Version;
    }
     
	// 向剪贴板中添加文本
    public static void copyTextToClipboard(final String str) throws Exception {
    	
    	Log.d(AndroidPlugin.LOG_TAG, "copyTextToClipboard:"+str);
    
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
            	
            	Log.d(AndroidPlugin.LOG_TAG, "copyTextToClipboard");
            	
            	try {
					NativeAPI.copyTextToClipboard(getUnityActivity_(), str);
				} catch (Exception e) {
					e.printStackTrace();
				}
            }
		});
    }

    // 从剪贴板中获取文本
    public static String getTextFromClipboard() {
    	return NativeAPI.getTextFromClipboard(getUnityActivity_());
    }

    public static void setUploadMessage(ValueCallback<Uri> message) {
        _uploadMessages = message;
    }

    public static void setUploadCallback(ValueCallback<Uri[]> message) {
        _uploadCallback = message;
    }
    
    public static String GetMWData(){
    	String str = _mwData;
    	_mwData = "";
    	return str;
    }
    
    private static void RegisterHandlerMW(Context context)
    {
      Log.d("MLink", "RegisterHandlerMW");
      
      MLink.getInstance(context).registerDefault(new MLinkCallback()
      {
    	@Override
        public void execute(Map paramAnonymousMap, Uri paramAnonymousUri, Context paramAnonymousContext)
        {
        	Log.d("MLink", "default action uri:"+paramAnonymousUri.getQuery());
        	
        	_mwData = new JSONObject(paramAnonymousMap).toString();
        	
        }
      });
    }
    
    private void InitMW(){
    	Log.d("MLink", "init");
    	MWConfiguration config = new MWConfiguration(this);
    	//设置渠道，非必须（渠道推荐在AndroidManifest.xml内填写）
    	config.setDebugModel(true)
    	.setPageTrackWithFragment(true)
    	.setWebViewBroadcastOpen(true);
    			//.setChannel("test")
    			//开启Debug模式，显示Log，release时注意关闭
    			
    			//带有Fragment的页面。具体查看2.2.2
    			//. setPageTrackWithFragment(true);
    			//设置分享方式，如果之前有集成sharesdk，可在此开启
    			//. setSharePlatform (MWConfiguration. ORIGINAL);
    	MagicWindowSDK.initSDK(config);
    	
    	RegisterHandlerMW(this);
    }
    
    @Override
    protected void onStart() {
      super.onStart();
      
	   	Uri mLink = getIntent().getData();
	    if (mLink != null) {
	    	Log.d(AndroidPlugin.LOG_TAG, "onStart router:"+mLink.toString());
	        MagicWindowSDK.getMLink().router(mLink);
	    } else {
	        MLink.getInstance(this).checkYYB();
	    }
    }

    @Override
    public void onNewIntent(Intent paramIntent){
      super.onNewIntent(paramIntent);
      Uri mLink = paramIntent.getData();
      setIntent(paramIntent);
      if (mLink != null) {
    	  Log.d(AndroidPlugin.LOG_TAG, "onNewIntent router:"+mLink.toString());
          MagicWindowSDK.getMLink().router(mLink);
      } else {
          MLink.getInstance(this).checkYYB();
      }
      
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CookieSyncManager.createInstance(AndroidPlugin.getUnityActivity_());
        
        if(!_initMW){
        	_initMW = true;
        	InitMW();
        }
        
        Uri mLink = getIntent().getData();
        MLink.getInstance(this).deferredRouter();
        if (mLink != null) {
        	Log.d(AndroidPlugin.LOG_TAG, "onCreate router:"+mLink.toString());
            MLink.getInstance(this).router(mLink);
        } else {
            MLink.getInstance(this).checkYYB();
        }
    }
   
    //It is a black magic in onPause and onResume
    //to make the unity view not disappear when return from background
    //Something about inactive activity which might be not reload when resume from bg.
    @Override
    public void onPause() {
    	
    	Session.onPause(this);
        super.onPause();
        ShowAllWebViewDialogs(false);
        
        CookieSyncManager manager = CookieSyncManager.getInstance();
        if (manager != null) {
            manager.stopSync();
        }
    }

    @Override
    public void onResume() {
    	Session.onResume(this);
        super.onResume();
        ShowAllWebViewDialogs(false);
        //If you are suffering a black unity scene problem when switch back to the game,
        //try to increase the number 200.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ShowAllWebViewDialogs(true);
            }
        }, 200);

        CookieSyncManager manager = CookieSyncManager.getInstance();
        if (manager != null) {
            manager.startSync();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(AndroidPlugin.LOG_TAG, "Rotation: " + newConfig.orientation);
        for (UniWebViewDialog dialog : UniWebViewManager.Instance().allDialogs()) {
            dialog.updateContentSize();
            dialog.HideSystemUI();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,  Intent data) {
        if (requestCode != FILECHOOSER_RESULTCODE || (_uploadMessages == null) && (_uploadCallback == null)) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        Uri[] results = null;
        Uri result = null;

        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                if (_cameraPhotoPath != null) {
                    result = Uri.parse(_cameraPhotoPath);
                    results = new Uri[]{result};
                }
            } else {
                String dataString = data.getDataString();
                if (dataString != null) {
                    result = Uri.parse(dataString);
                    results = new Uri[]{result};
                }
            }
        }

        if (_uploadCallback != null) {
            _uploadCallback.onReceiveValue(results);
            _uploadCallback = null;
        }

        if (_uploadMessages != null) {
            _uploadMessages.onReceiveValue(result);
            _uploadMessages = null;
        }

        _cameraPhotoPath = null;

    }

    public static void _UniWebViewInit(final String name, final int top, final int left, final int bottom, final int right) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewInit");
                UniWebViewDialog.DialogListener listener = new UniWebViewDialog.DialogListener() {
                    public void onPageFinished(UniWebViewDialog dialog, String url) {
                        Log.d(LOG_TAG, "page load finished: " + url);
                        UnityPlayer.UnitySendMessage(name, "LoadComplete", "");

                        //what the hell!!!!!
                        if (url.startsWith("weixin://")) {
                        	Log.d(LOG_TAG, "start wechat activity");
                            Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
                            getUnityActivity_().startActivity(intent);
                        }
                    }

                    public void onPageStarted(UniWebViewDialog dialog, String url) {
                        Log.d(LOG_TAG, "page load started: " + url);
                        UnityPlayer.UnitySendMessage(name, "LoadBegin", url);
                    }

                    public void onReceivedError(UniWebViewDialog dialog, int errorCode, String description, String failingUrl) {
                        Log.d(LOG_TAG, "page load error: " + failingUrl + " Error: " + description+" Code:"+errorCode);
                        
                        UnityPlayer.UnitySendMessage(name, "LoadComplete", description);
                    }

                    public boolean shouldOverrideUrlLoading(UniWebViewDialog dialog, String url) {
                    	Log.d(LOG_TAG, "shouldOverrideUrlLoading:"+url);
                        boolean shouldOverride = false;
                        if (url.startsWith("mailto:")) {
                            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                            getUnityActivity_().startActivity(intent);
                            shouldOverride = true;
                        } else if (url.startsWith("tel:")) {
                            Intent intent = new Intent(Intent.ACTION_DIAL,Uri.parse(url));
                            getUnityActivity_().startActivity(intent);
                            shouldOverride = true;
                        } else if (url.startsWith("sms:")) {
                            try {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                intent.addCategory(Intent.CATEGORY_DEFAULT);
                                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                                getUnityActivity_().startActivity(intent);
                                shouldOverride = true;
                            } catch (Exception e) {
                                Log.d(AndroidPlugin.LOG_TAG, e.getMessage());
                            }
                        } 
                        else if (url.startsWith("weixin://")) {
                        	Log.d(LOG_TAG, "start wechat activity");
                            Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
                            getUnityActivity_().startActivity(intent);
                            shouldOverride = true;
                        }
                        else {
                            boolean canResponseScheme = false;
                            for (String scheme : dialog.schemes) {
                                if (url.startsWith(scheme + "://")) {
                                    canResponseScheme = true;
                                    break;
                                }
                            }
                            if (canResponseScheme) {
                                UnityPlayer.UnitySendMessage(name, "ReceivedMessage", url);
                                shouldOverride = true;
                            }
                        }
                        return shouldOverride;
                    }

                    public void onDialogShouldCloseByBackButton(UniWebViewDialog dialog) {
                        Log.d(LOG_TAG, "dialog should be closed");
                        UnityPlayer.UnitySendMessage(name, "WebViewDone", "");
                    }

                    public void onDialogKeyDown(UniWebViewDialog dialog, int keyCode) {
                        UnityPlayer.UnitySendMessage(name, "WebViewKeyDown", Integer.toString(keyCode));
                    }

                    public void onDialogClose(UniWebViewDialog dialog) {
                        UniWebViewManager.Instance().removeUniWebView(name);
                    }

                    public void onJavaScriptFinished(UniWebViewDialog dialog, String result) {
                        UnityPlayer.UnitySendMessage(name, "EvalJavaScriptFinished", result);
                    }

                    public void onAnimationFinished(UniWebViewDialog dialog, String identifier) {
                        UnityPlayer.UnitySendMessage(name, "AnimationFinished", identifier);
                    }

                    public void onShowTransitionFinished(UniWebViewDialog dialog) {
                        UnityPlayer.UnitySendMessage(name, "ShowTransitionFinished", "");
                    }

                    public void onHideTransitionFinished(UniWebViewDialog dialog) {
                        UnityPlayer.UnitySendMessage(name, "HideTransitionFinished", "");
                    }
                };
                UniWebViewDialog dialog = new UniWebViewDialog(getUnityActivity_(), listener);
                dialog.changeInsets(top, left, bottom, right);
                UniWebViewManager.Instance().setUniWebView(name, dialog);
            }
        });
	}

	public static void _UniWebViewLoad(final String name, final String url)
	{
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewLoad");
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                if (dialog != null) {
                    dialog.load(url);
                }
            }
        });
	}

    public static void _UniWebViewReload(final String name) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewReload");
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                if (dialog != null) {
                    dialog.reload();
                }
            }
        });
    }

    public static void _UniWebViewStop(final String name) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewStop");
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                if (dialog != null) {
                    dialog.stop();
                }
            }
        });
    }

	public static void _UniWebViewChangeInsets(final String name, final int top, final int left, final int bottom, final int right)
	{
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewChangeSize");
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                if (dialog != null) {
                    dialog.changeInsets(top, left, bottom, right);
                }
            }
        });
	}

    public static void _UniWebViewShow(final String name, final boolean fade, final int direction, final float duration) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewShow");
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                if (dialog != null) {
                    dialog.setShow(true, fade, direction, duration);
                }
            }
        });
    }

    public static void _UniWebViewHide(final String name, final boolean fade, final int direction, final float duration) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewHide");
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                if (dialog != null) {
                    dialog.setShow(false, fade, direction, duration);
                }
            }
        });
    }

	public static void _UniWebViewEvaluatingJavaScript(final String name, final String js) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewEvaluatingJavaScript");
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                if (dialog != null) {
                    dialog.loadJS(js);
                }
            }
        });
	}

    public static void _UniWebViewAddJavaScript(final String name, final String js) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewAddJavaScript");
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                if (dialog != null) {
                    dialog.addJs(js);
                }
            }
        });
    }

	public static void _UniWebViewCleanCache(final String name) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewCleanCache");
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                if (dialog != null) {
                    dialog.cleanCache();
                }
            }
        });
	}

    public static void _UniWebViewCleanCookie(final String name, final String key) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewCleanCookie");

                CookieManager cm = CookieManager.getInstance();
                if (key == null || key.length() == 0) {
                    Log.d(LOG_TAG, "Cleaning all cookies");
                    cm.removeAllCookie();
                } else {
                    Log.d(LOG_TAG, "Setting an empty cookie for: " + key);
                    cm.setCookie(key,"");
                }

                CookieSyncManager manager = CookieSyncManager.getInstance();
                if (manager != null) {
                    manager.sync();
                }
            }
        });
    }

    public static void _UniWebViewDestroy(final String name) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewDestroy");
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                if (dialog != null) {
                    dialog.destroy();
                }
            }
        });
    }

	public static void _UniWebViewTransparentBackground(final String name, final boolean transparent) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewTransparentBackground");
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                if (dialog != null) {
                    dialog.setTransparent(transparent);
                }
            }
        });
	}

    public static void _UniWebViewSetBackgroundColor(final String name, final float r, final float g, final float b, final float a) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewSetBackgroundColor");
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                if (dialog != null) {
                    dialog.setBackgroundColor(r, g, b, a);
                }
            }
        });
    }

    public static void _UniWebViewSetSpinnerShowWhenLoading(final String name, final boolean show) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewSetSpinnerShowWhenLoading: " + show);
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                if (dialog != null) {
                    dialog.setSpinnerShowWhenLoading(show);
                }
            }
        });
    }

    public static void _UniWebViewSetSpinnerText(final String name, final String text) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewSetSpinnerText: " + text);
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                if (dialog != null) {
                    dialog.setSpinnerText(text);
                }
            }
        });
    }

    public static boolean _UniWebViewCanGoBack(final String name) {
        UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
        if (dialog != null) {
            return dialog.canGoBack();
        } else {
            return false;
        }
    }

    public static boolean _UniWebViewCanGoForward(final String name) {
        UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
        if (dialog != null) {
            return dialog.canGoForward();
        } else {
            return false;
        }
    }

	public static void _UniWebViewGoBack(final String name) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewGoBack");
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                if (dialog != null) {
                    dialog.goBack();
                }
            }
        });
	}

	public static void _UniWebViewGoForward(final String name) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewGoForward");
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                if (dialog != null) {
                    dialog.goForward();
                }
            }
        });
	}

    public static void _UniWebViewLoadHTMLString(final String name, final String htmlString, final String baseURL) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewLoadHTMLString");
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                if (dialog != null) {
                    dialog.loadHTMLString(htmlString, baseURL);
                }
            }
        });
    }

    public static String _UniWebViewGetCurrentUrl(final String name) {
        UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
        if (dialog != null) {
            return dialog.getUrl();
        }
        return "";
    }

    public static void _UniWebViewSetBackButtonEnable(final String name, final boolean enable) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewSetBackButtonEnable:" + enable);
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                if (dialog != null) {
                    dialog.setBackButtonEnable(enable);
                }
            }
        });
    }

    public static void _UniWebViewSetBounces(final String name, final boolean enable) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewSetBounces:" + enable);
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                if (dialog != null) {
                    dialog.setBounces(enable);
                }
            }
        });
    }

    public static void _UniWebViewSetZoomEnable(final String name, final boolean enable) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewSetZoomEnable:" + enable);
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                if (dialog != null) {
                    dialog.setZoomEnable(enable);
                }
            }
        });
    }

    public static void _UniWebViewAddUrlScheme(final String name, final String scheme) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewAddUrlScheme:" + scheme);
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                if (dialog != null) {
                    dialog.addUrlScheme(scheme);
                }
            }
        });
    }

    public static void _UniWebViewRemoveUrlScheme(final String name, final String scheme) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewAddUrlScheme:" + scheme);
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                if (dialog != null) {
                    dialog.removeUrlScheme(scheme);
                }
            }
        });
    }

    public static void _UniWebViewUseWideViewPort(final String name, final boolean use) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewUseWideViewPort:" + use);
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                if (dialog != null) {
                    dialog.useWideViewPort(use);
                }
            }
        });
    }

    public static void _UniWebViewSetUserAgent(final String userAgent) {
        UniWebView.customUserAgent = userAgent;
    }

    public static String _UniWebViewGetUserAgent(final String name) {
        UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
        if (dialog != null) {
            return dialog.getUserAgent();
        }
        return "";
    }

    public static void _UniWebViewSetAlpha(final String name, final float alpha) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "_UniWebViewSetAlpha: " + alpha);
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                if (dialog != null) {
                    dialog.setAlpha(alpha);
                }
            }
        });
    }

    public static float _UniWebViewGetAlpha(final String name) {
        UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
        if (dialog != null) {
            return dialog.getAlpha();
        }
        return 0.0f;
    }

    public static void _UniWebViewSetImmersiveModeEnabled(final String name, final boolean enabled) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                if (dialog != null) {
                    dialog.setImmersiveModeEnabled(enabled);
                }
            }
        });
    }

    public static void _UniWebViewAddPermissionRequestTrustSite(final String name, final String site) {
        UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
        if (dialog != null) {
            dialog.AddPermissionRequestTrustSite(site);
        }
    }

    public static void _UniWebViewSetPosition(final String name, final int x, final int y) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                dialog.setPosition(x, y);
            }
        });
    }

    public static void _UniWebViewSetSize(final String name, final int width, final int height) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                dialog.setSize(width, height);
            }
        });
    }

    public static void _UniWebViewAnimateTo(final String name, final int x, final int y, final float duration, final float delay, final String identifier) {
        runSafelyOnUiThread(new Runnable() {
            @Override
            public void run() {
                UniWebViewDialog dialog = UniWebViewManager.Instance().getUniWebViewDialog(name);
                dialog.animateTo(x, y, duration, delay, identifier);
            }
        });
    }

    protected static void runSafelyOnUiThread(final Runnable r) {
        getUnityActivity_().runOnUiThread(new Runnable() {
            public void run() {
                try {
                    r.run();
                } catch (Exception e) {
                    Log.d(LOG_TAG, "UniWebView should run on UI thread: " + e.getMessage());
                }
            }
        });
    }

    protected void ShowAllWebViewDialogs(boolean show) {
        ArrayList<UniWebViewDialog> webViewDialogs = UniWebViewManager.Instance().getShowingWebViewDialogs();
        for (UniWebViewDialog webViewDialog : webViewDialogs) {
            if (show) {
                Log.d(LOG_TAG, webViewDialog + "goForeGround");
                webViewDialog.goForeGround();
                webViewDialog.HideSystemUI();
            } else {
                Log.d(LOG_TAG, webViewDialog + "goBackGround");
                webViewDialog.goBackGround();
                webViewDialog.HideSystemUI();
            }
        }
    }
}