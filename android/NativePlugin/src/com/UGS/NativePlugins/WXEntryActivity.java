package com.UGS.NativePlugins;

import com.tencent.mm.sdk.constants.ConstantsAPI;
import com.tencent.mm.sdk.modelbase.BaseReq;
import com.tencent.mm.sdk.modelbase.BaseResp;
import com.tencent.mm.sdk.modelmsg.SendAuth;
import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;
import com.unity3d.player.UnityPlayerNativeActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

public class WXEntryActivity extends Activity implements IWXAPIEventHandler {

	private IWXAPI mWeixinAPI;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.w(DefineConst.LOG_TAG, "WxAuthActivity onCreate");
		
		mWeixinAPI = WXAPIFactory.createWXAPI(this, WXAuth.AppID, false);
		mWeixinAPI.registerApp(WXAuth.AppID);
		mWeixinAPI.handleIntent(getIntent(), this);
	}
	
	
	@Override
    public void onResume() {
        super.onResume();
        
        Log.d(DefineConst.LOG_TAG, "WxAuthActivity onResume");
    }
	
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        Log.w(DefineConst.LOG_TAG, "WxAuthActivity onDestroy");

        if (mWeixinAPI != null)
        	mWeixinAPI.detach();
    }

	@Override
	protected void onNewIntent(Intent intent) {
		Log.w(DefineConst.LOG_TAG, "WxAuthActivity onNewIntent");
		super.onNewIntent(intent);
		setIntent(intent);
		mWeixinAPI.handleIntent(intent, this);
		finish();
	}

	@Override
	public void onReq(BaseReq arg0) {
		Log.w(DefineConst.LOG_TAG, "WxAuthActivity onReq");
	}

	@Override
	public void onResp(BaseResp resp) {
		
		Log.w(DefineConst.LOG_TAG, "WxAuthActivity onResp errCode:" + resp.errCode+" errStr:" + resp.errStr+ " type:"+resp.getType());
		
		Intent intent = new Intent();
		
		switch(resp.getType()){
		case ConstantsAPI.COMMAND_SENDAUTH:
		{
			SendAuth.Resp authResp = ((SendAuth.Resp) resp);
			intent.putExtra("errCode", authResp.errCode);
			intent.putExtra("code", authResp.code);
			intent.setAction(DefineConst.BROADCAST_ACTION_WEIXIN_AUTH_RESP);
			break;
		}
		case ConstantsAPI.COMMAND_SENDMESSAGE_TO_WX:{
			SendMessageToWX.Resp shareResp = ((SendMessageToWX.Resp)resp);
			intent.putExtra("errCode", shareResp.errCode);
			intent.setAction(DefineConst.BROADCAST_ACTION_WEIXIN_SHARE_RESP);
			break;
		}
		}

		if (android.os.Build.VERSION.SDK_INT >= 12) {
			intent.setFlags(32);// 3.1以后的版本需要设置Intent.FLAG_INCLUDE_STOPPED_PACKAGES
		}

		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
		lbm.sendBroadcast(intent);
		
		this.finish();
		
		Log.w(DefineConst.LOG_TAG, "WxAuthActivity finish");
	}

}
