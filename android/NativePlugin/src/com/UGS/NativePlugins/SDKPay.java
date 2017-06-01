/**
 * ShoppingCartActivity.java
 *
 * Created by xuanzhui on 2015/7/29.
 * Copyright (c) 2015 BeeCloud. All rights reserved.
 */
package com.UGS.NativePlugins;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

//import com.unionpay.UPPayAssistEx;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.unity3d.player.UnityPlayer;

import cn.beecloud.BCPay;
import cn.beecloud.BCQuery;
import cn.beecloud.BeeCloud;
import cn.beecloud.async.BCCallback;
import cn.beecloud.async.BCResult;
//import cn.beecloud.demo.util.BillUtils;
import cn.beecloud.entity.BCBillOrder;
import cn.beecloud.entity.BCPayResult;
import cn.beecloud.entity.BCQueryBillResult;
import cn.beecloud.entity.BCReqParams;

//todo 继承自activity
public class SDKPay extends Activity {
	
	public static final int PAY_MODE_WECHAT = 0x0001;
	public static final int PAY_MODE_ALIPAY = 0x0002;
	public static final int PAY_MODE_TEST = 0x0004;
	
	private ProgressDialog loadingDialog;
	private int mMode;
	private String mCallback;
	private String mCallbackObj;
	private Stack<String> mBillStack;
	private Context mContext;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		try {

			String callbackObj = this.getIntent().getStringExtra("CallbackObj");
			String callback = this.getIntent().getStringExtra("Callback");
			
			String wxAppID = this.getIntent().getStringExtra("WXAppID");
			String bcAppID = this.getIntent().getStringExtra("BeeCloudAppID");
			String bcSecret = this.getIntent().getStringExtra("BeeCloudAppSecret");
			
			this.mMode = PAY_MODE_WECHAT | PAY_MODE_ALIPAY;
			this.mBillStack = new Stack<String>();
			this.mCallback = callback;
			this.mCallbackObj = callbackObj;
			this.mContext = this;
			
			BeeCloud.setSandbox(false);
			BeeCloud.setAppIdAndSecret(bcAppID, bcSecret);

			String initInfo = BCPay.initWechatPay(this, wxAppID);
			if (initInfo != null) {
				Log.w(DefineConst.LOG_TAG, "微信初始化失败：" + initInfo);
			}
			
			Log.w(DefineConst.LOG_TAG, 
					String.format("InitSDKPay：%s %s %s", bcAppID, bcSecret, wxAppID));
			
			String title = this.getIntent().getStringExtra("Title");
			String bill = this.getIntent().getStringExtra("Bill");
			int amount = this.getIntent().getIntExtra("Amount", 0);
			int channel = this.getIntent().getIntExtra("Channel", 0);
			String optional = this.getIntent().getStringExtra("Optional");
			
			this.Pay(title, bill, amount, channel, optional);
			
		} catch (Exception e) {
			e.printStackTrace();
			Log.d(DefineConst.LOG_TAG, "init pay sdk failed: " + e.getMessage());
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		Log.d(DefineConst.LOG_TAG, "pay onDestroy");
		
		if(loadingDialog.isShowing()){
			loadingDialog.dismiss();
		}
		
		// 清理当前的activity引用
		BCPay.clear();
		// 使用微信的，在initWechatPay的activity结束时detach
		BCPay.detachWechat();
	}
	

	@Override
    public void onResume() {
        super.onResume();
        
        Log.d(DefineConst.LOG_TAG, "SDKPay Activity onResume");
        
        if(loadingDialog != null && loadingDialog.isShowing()){
        	loadingDialog.dismiss();
        }
        //this.finish();
    }
	
	public static void PayNow(final String bcAppID, final String bcSecret, final String wxAppID, 
			final String callbackObj, final String callback, 
			final String title, final String bill, final int amount, final int channel, final String optional) {

		Log.w(DefineConst.LOG_TAG, "PayNow");

		Intent intent = new Intent(UnityPlayer.currentActivity, SDKPay.class);
		intent.putExtra("BeeCloudAppID", bcAppID);
		intent.putExtra("BeeCloudAppSecret", bcSecret);
		intent.putExtra("WXAppID", wxAppID);
		
    	intent.putExtra("CallbackObj", callbackObj);
    	intent.putExtra("Callback", callback);
    	
    	intent.putExtra("Title", title);
    	intent.putExtra("Bill", bill);
    	intent.putExtra("Amount", amount);
    	intent.putExtra("Channel", channel);
    	intent.putExtra("Optional", optional);
    	
    	UnityPlayer.currentActivity.startActivity(intent);
	}
	
	// 支付结果返回入口
	BCCallback bcCallback = new BCCallback() {
		@Override
		public void done(final BCResult result) {
			
			BCPayResult bcPayResult = (BCPayResult) result;
			
			Log.w(DefineConst.LOG_TAG, "pay result:"+bcPayResult.getErrMsg());
			Log.w(DefineConst.LOG_TAG, "pay result:"+bcPayResult.getResult());
			Log.w(DefineConst.LOG_TAG, "pay result:"+bcPayResult.getErrCode());
			
			String resultMsg = bcPayResult.getErrMsg();
			if (resultMsg.equals(BCPayResult.RESULT_SUCCESS)) {
				resultMsg = "用户支付成功";
			} else if (resultMsg.equals(BCPayResult.RESULT_CANCEL)){
				Log.w(DefineConst.LOG_TAG, "RESULT_CANCEL");
				resultMsg = "用户取消支付";
			}
			else if (resultMsg.equals(BCPayResult.RESULT_UNKNOWN)) {
				// 可能出现在支付宝8000返回状态
				resultMsg = "订单状态未知";
			}else if (resultMsg.equals(BCPayResult.FAIL_NETWORK_ISSUE)) {
				resultMsg = "网络错误";
			}else{
				resultMsg = "支付失败, " + bcPayResult.getDetailInfo();
			} 

			if (bcPayResult.getId() != null && (mMode & PAY_MODE_TEST) != 0) {
				// 你可以把这个id存到你的订单中，下次直接通过这个id查询订单
				Log.w(DefineConst.LOG_TAG, "bill id retrieved : " + bcPayResult.getId());

				// 根据ID查询，此处只是演示如何通过id查询订单，并非支付必要部分
				getBillInfoByID(bcPayResult.getId());
			}
			
			
			/*Message msg = mHandler.obtainMessage();
			mHandler.sendMessage(msg);*/
			
			//Toast.makeText(mContext, resultMsg, Toast.LENGTH_SHORT).show();
			String bill = "";
			if(!mBillStack.empty()){
				bill = mBillStack.pop();
			}
			
			Log.w(DefineConst.LOG_TAG, "bill:"+bill);
			
			String toastMsg = String.format("%s:%d:%s:%s", 
					bill, bcPayResult.getErrCode(), resultMsg, bcPayResult.getResult());
			
			Log.w(DefineConst.LOG_TAG, "UnitySendMessage cbObj:" +mCallbackObj + " cb:"+mCallback+" msg:"+toastMsg);
			UnityPlayer.UnitySendMessage(mCallbackObj, mCallback, toastMsg);

			Activity activity = (Activity) mContext;
			activity.finish();
		}
	};
	
	public void Pay(final String title, final String bill, final int amount, final int channel, final String optional) {

		Log.w(DefineConst.LOG_TAG, "start pay:"+channel+" "+bill+" "+amount);
		
		loadingDialog = new ProgressDialog(this);
		loadingDialog.setMessage("处理中，请稍候...");
		loadingDialog.setIndeterminate(true);
		loadingDialog.setCancelable(false);
		loadingDialog.show();
		
		Map<String, String> mapOptional = new HashMap<String, String>();
		if(optional != null && optional.length() >0){
			String[] pair = optional.split("#,#");
			for(int i=0; i<pair.length; i++){
				String[] value = pair[i].split("#.#");
				Log.w(DefineConst.LOG_TAG, "add optional key:" + value[0] + " value:"+value[1]);
				mapOptional.put(value[0], value[1]);
			}
		}
				
		/*mapOptional.put("Platform", "2");
		mapOptional.put("ProductName", appInfo.packageName);
		mapOptional.put("AppID", mContext.getPackageManager().getApplicationLabel(appInfo).toString());*/
		if((mMode & channel) == 0){
			Toast.makeText(this, "当前配置不支持此支付方式", Toast.LENGTH_LONG).show();
			//loadingDialog.dismiss();
			this.finish();
			return;
		}
		
		switch (channel) {
		case PAY_MODE_WECHAT: // 微信
			{
				if (!BCPay.isWXAppInstalledAndSupported() || !BCPay.isWXPaySupported()) {
					Log.w(DefineConst.LOG_TAG, "isWXAppInstalledAndSupported:"+BCPay.isWXAppInstalledAndSupported()+" isWXPaySupported:"+BCPay.isWXPaySupported());
					Toast.makeText(this, "您尚未安装微信或者安装的微信版本不支持", Toast.LENGTH_LONG).show();
					//loadingDialog.dismiss();
					this.finish();
					break;
				}
				
				Log.w(DefineConst.LOG_TAG, "wechat pay");
				mBillStack.push(bill);
				BCPay.getInstance(this).reqWXPaymentAsync(
						title, // 订单标题
						amount, // 订单金额(分)
						bill, // 订单流水号
						mapOptional, // 扩展参数(可以null)
						bcCallback); // 支付完成后回调入口
				break;
			}
		case PAY_MODE_ALIPAY: // 支付宝支付
			{
				Log.w(DefineConst.LOG_TAG, "ali pay");
				mBillStack.push(bill);
				BCPay.getInstance(this).reqAliPaymentAsync(
						title, 
						amount, 
						bill, 
						mapOptional,
						bcCallback);
	
				break;
			}
		}
	}

	void getBillInfoByID(String id) {

		BCQuery.getInstance().queryBillByIDAsync(id, new BCCallback() {
			@Override
			public void done(BCResult result) {
				BCQueryBillResult billResult = (BCQueryBillResult) result;

				Log.d(DefineConst.LOG_TAG, "------ response info ------");
				Log.d(DefineConst.LOG_TAG, "------getResultCode------" + billResult.getResultCode());
				Log.d(DefineConst.LOG_TAG, "------getResultMsg------" + billResult.getResultMsg());
				Log.d(DefineConst.LOG_TAG, "------getErrDetail------" + billResult.getErrDetail());

				if (billResult.getResultCode() != 0)
					return;

				Log.d(DefineConst.LOG_TAG, "------- bill info ------");
				BCBillOrder billOrder = billResult.getBill();
				Log.d(DefineConst.LOG_TAG, "订单唯一标识符：" + billOrder.getId());
				Log.d(DefineConst.LOG_TAG, "订单号:" + billOrder.getBillNum());
				Log.d(DefineConst.LOG_TAG, "订单金额, 单位为分:" + billOrder.getTotalFee());
				Log.d(DefineConst.LOG_TAG, "渠道类型:" + BCReqParams.BCChannelTypes.getTranslatedChannelName(billOrder.getChannel()));
				Log.d(DefineConst.LOG_TAG, "子渠道类型:" + BCReqParams.BCChannelTypes.getTranslatedChannelName(billOrder.getSubChannel()));
				Log.d(DefineConst.LOG_TAG, "订单是否成功:" + billOrder.getPayResult());

				if (billOrder.getPayResult())
					Log.d(DefineConst.LOG_TAG, "渠道返回的交易号，未支付成功时，是不含该参数的:" + billOrder.getTradeNum());
				else
					Log.d(DefineConst.LOG_TAG, "订单是否被撤销，该参数仅在线下产品（例如二维码和扫码支付）有效:" + billOrder.getRevertResult());

				Log.d(DefineConst.LOG_TAG, "订单创建时间:" + new Date(billOrder.getCreatedTime()));
				Log.d(DefineConst.LOG_TAG, "扩展参数:" + billOrder.getOptional());
				Log.w(DefineConst.LOG_TAG, "订单是否已经退款成功(用于后期查询): " + billOrder.getRefundResult());
				Log.w(DefineConst.LOG_TAG, "渠道返回的详细信息，按需处理: " + billOrder.getMessageDetail());

			}
		});
	}
}
