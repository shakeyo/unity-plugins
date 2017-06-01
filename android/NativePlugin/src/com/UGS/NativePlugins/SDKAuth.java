package com.UGS.NativePlugins;

import java.util.List;

import com.tencent.mm.sdk.modelbase.BaseResp;
import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.unity3d.player.UnityPlayer;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;


public class SDKAuth extends Activity {
	
	public static final int WX_OP_AUTH = 0x0001;
	public static final int WX_OP_SHARE = 0x0002;
	public static final int WX_OP_SHARE_IMG = 0x0003;
	
	private WXAuth mWXAuth;
	//private ProgressDialog loadingDialog;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.d(DefineConst.LOG_TAG, "Create SDKAuth Activity!!!!!!!!!!");
		
		try {
			
			String wxAppID = this.getIntent().getStringExtra("WXAppID");
			int opCode = this.getIntent().getIntExtra("Operate", 0);

			Log.d(DefineConst.LOG_TAG, "init SDKAuth:"+wxAppID);

			mWXAuth = new WXAuth(this, wxAppID);
			if(!mWXAuth.Init()){
				this.finish();
				return;
			}
			
			LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this); 
			IntentFilter filter = new IntentFilter(); 
			filter.addAction(DefineConst.BROADCAST_ACTION_WEIXIN_AUTH_RESP);
			filter.addAction(DefineConst.BROADCAST_ACTION_WEIXIN_SHARE_RESP); 
			lbm.registerReceiver(mWXAuth,filter);
			
			switch(opCode)
			{
			case WX_OP_AUTH:
			{
				String callbackObj = this.getIntent().getStringExtra("CallbackObj");
				String callback = this.getIntent().getStringExtra("Callback");
				mWXAuth.SendAuth(callbackObj, callback);
				break;
			}
			case WX_OP_SHARE:
			{
				Log.w(DefineConst.LOG_TAG, "WX_OP_SHARE");
				String msg = this.getIntent().getStringExtra("Message");
				String title = this.getIntent().getStringExtra("Title");
				String url = this.getIntent().getStringExtra("Url");
				int scene = this.getIntent().getIntExtra("Scene", 0);
				Log.w(DefineConst.LOG_TAG, "WX_OP_SHARE scene:"+scene);

				mWXAuth.SendShareUrl(title, msg, url, scene);
				break;
			}
			case WX_OP_SHARE_IMG:
			{
				Log.w(DefineConst.LOG_TAG, "WX_OP_SHARE_IMG");
				String imgUrl = this.getIntent().getStringExtra("ImageUrl");
				int maxSize = this.getIntent().getIntExtra("MaxSize", 30*1024);
				int scene = this.getIntent().getIntExtra("Scene", 0);
				mWXAuth.SendShareImageUrl(imgUrl, maxSize, scene);
				break;
			}
			}
			
		} catch (Exception e) {
			Log.d(DefineConst.LOG_TAG, "init auth sdk failed: " + e.getMessage());
			this.finish();
		}
	}
	    
	@Override
    public void onResume() {
        super.onResume();
        
        Log.d(DefineConst.LOG_TAG, "SDKAuth Activity onResume");
        
        /*Log.d(DefineConst.LOG_TAG, "SDKAuth Activity onResume");
        if(this.getIntent().hasExtra("CallbackObj")){
            String callbackObj = this.getIntent().getStringExtra("CallbackObj");
    		String callback = this.getIntent().getStringExtra("Callback");
    		String result = String.format("%d:%s", -1, "取消");
    		UnityPlayer.UnitySendMessage(callbackObj, callback, result);
    		
    		Toast.makeText(this, "用户取消", Toast.LENGTH_LONG).show();
		}*/

        //this.finish();
    }
	

	@Override
	protected void onNewIntent(Intent intent) {
		Log.w(DefineConst.LOG_TAG, "SDKAuth Activity onNewIntent");
		super.onNewIntent(intent);
	}
	
	@Override
	public void onPause() {
	    super.onPause();
	    Log.w(DefineConst.LOG_TAG, "SDKAuth Activity onPause");
	}
    
	@Override
	protected void onDestroy() {
		super.onDestroy();
	
		//loadingDialog.dismiss();
		if(mWXAuth != null){
			LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);  
			lbm.unregisterReceiver(mWXAuth);
			mWXAuth.UnInit();
		}
		
		Log.d(DefineConst.LOG_TAG, "SDKAuth Activity onDestroy");
	}
	
	public static void WXAuth(String appID, String callbackObj, String callback){
		
		Log.d(DefineConst.LOG_TAG, "WXLogin");
		
		Intent intent = new Intent(UnityPlayer.currentActivity,SDKAuth.class);
		intent.putExtra("WXAppID", appID);
    	intent.putExtra("CallbackObj", callbackObj);
    	intent.putExtra("Callback", callback);
    	intent.putExtra("Operate", WX_OP_AUTH);
    	UnityPlayer.currentActivity.startActivity(intent);
	}
	
	public static void WXShare(String title, String msg, String url, String appID, int scene){
		Log.d(DefineConst.LOG_TAG, "WXShare"+scene);
		
		Intent intent = new Intent(UnityPlayer.currentActivity,SDKAuth.class);
		intent.putExtra("WXAppID", appID);
    	intent.putExtra("Operate", WX_OP_SHARE);
    	intent.putExtra("Title", title);
    	intent.putExtra("Message", msg);
    	intent.putExtra("Url", url);
    	intent.putExtra("Image", false);
    	intent.putExtra("Scene", scene);
    	UnityPlayer.currentActivity.startActivity(intent);
	}
	
	public static void WXShareImg(String img, int maxSize, String appID, int scene){
		Log.d(DefineConst.LOG_TAG, "WXShareImg");
		
		Intent intent = new Intent(UnityPlayer.currentActivity,SDKAuth.class);
		intent.putExtra("WXAppID", appID);
    	intent.putExtra("Operate", WX_OP_SHARE_IMG);
    	intent.putExtra("MaxSize", maxSize);
    	intent.putExtra("Image", true);
    	intent.putExtra("ImageUrl", img);
    	intent.putExtra("Scene", scene);
    	UnityPlayer.currentActivity.startActivity(intent);
	}
}
