/**
 * ShoppingCartActivity.java
 *
 * Created by xuanzhui on 2015/7/29.
 * Copyright (c) 2015 BeeCloud. All rights reserved.
 */
package com.QianNiao.ToolChain;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.os.Message;
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

public class NativePay {
	
	private static final String TAG = "UnityActivity";
	private static final int PAY_MODE_TEST = 0x0001;
	private static final int PAY_MODE_WECHAT = 0x0002;
	private static final int PAY_MODE_ALIPAY = 0x0004;

	private ProgressDialog loadingDialog;

	private Context mContext;
	
	private int mMode;
	
	private String mCallback;
	
	private String mCallbackObj;
	
	private Stack<String> mBillStack;
	
	// 支付结果返回入口
	BCCallback bcCallback = new BCCallback() {
		@Override
		public void done(final BCResult result) {
			
			BCPayResult bcPayResult = (BCPayResult) result;
			
			Log.w(TAG, "pay result:"+bcPayResult.getErrMsg());
			Log.w(TAG, "pay result:"+bcPayResult.getResult());
			Log.w(TAG, "pay result:"+bcPayResult.getErrCode());
			
			String resultMsg = bcPayResult.getErrMsg();
			if (resultMsg.equals(BCPayResult.RESULT_SUCCESS)) {
				resultMsg = "用户支付成功";
			} else if (resultMsg.equals(BCPayResult.RESULT_CANCEL)){
				Log.w(TAG, "RESULT_CANCEL");
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
				Log.w(TAG, "bill id retrieved : " + bcPayResult.getId());

				// 根据ID查询，此处只是演示如何通过id查询订单，并非支付必要部分
				getBillInfoByID(bcPayResult.getId());
			}
			
			
			/*Message msg = mHandler.obtainMessage();
			mHandler.sendMessage(msg);*/
			
			String bill = mBillStack.pop();
			String toastMsg = String.format("%s:%d:%s:%s", 
					bill, bcPayResult.getErrCode(), resultMsg, bcPayResult.getResult());
			
			Log.w(TAG, "UnitySendMessage cbObj:" +mCallbackObj + " cb:"+mCallback+" msg:"+toastMsg);
			UnityPlayer.UnitySendMessage(mCallbackObj, mCallback, toastMsg);

			loadingDialog.dismiss();
		}
	};

	// Defines a Handler object that's attached to the UI thread.
	// 通过Handler.Callback()可消除内存泄漏警告
	private Handler mHandler = new Handler(new Handler.Callback() {
		
		@Override
		public boolean handleMessage(Message msg) {
			//Toast.makeText(mContext, toastMsg, Toast.LENGTH_SHORT).show();
			// 此处关闭loading界面

			return true;
		}
	});

	  
	public void Init(Context context, int mod, String appID, String secret, String wechatID, String callbackObj, String callback) {

		this.mMode = mod;
		this.mContext = context;
		this.mCallback = callback;
		this.mCallbackObj = callbackObj;
		this.mBillStack = new Stack<String>();
		
		// 推荐在主Activity或application里的onCreate函数中初始化BeeCloud.
		BeeCloud.setSandbox((mod & PAY_MODE_TEST) != 0);
		BeeCloud.setAppIdAndSecret(appID, secret);
		
		// 如果用到微信支付，在用到微信支付的Activity的onCreate函数里调用以下函数.
		// 第二个参数需要换成你自己的微信AppID.
		if((mod & PAY_MODE_WECHAT) != 0){
			String initInfo = BCPay.initWechatPay(mContext, wechatID);
			if (initInfo != null) {
				Toast.makeText(mContext, "微信初始化失败：" + initInfo, Toast.LENGTH_LONG).show();
			}
		}
		
	}
	
	public void UnInit(){
		   //清理当前的activity引用
        BCPay.clear();

        //使用微信的，在initWechatPay的activity结束时detach
        BCPay.detachWechat();
	}
	
	public void Pay(String title, String bill, int amount, int channel, String optional) {

		Log.w(TAG, "StartPay channel:" + channel+" mod:"+mMode);
		Log.w(TAG, "v1:" + (mMode & PAY_MODE_TEST));
		Log.w(TAG, "v2:" + (mMode & PAY_MODE_WECHAT));
		Log.w(TAG, "v3:" + (mMode & PAY_MODE_ALIPAY));

		loadingDialog = new ProgressDialog(mContext);
		loadingDialog.setMessage("处理中，请稍候...");
		loadingDialog.setIndeterminate(true);
		loadingDialog.setCancelable(true);
		loadingDialog.show();
		
		Map<String, String> mapOptional = new HashMap<String, String>();
		if(optional != null && optional.length() >0){
			String[] pair = optional.split("#,#");
			for(int i=0; i<pair.length; i++){
				String[] value = pair[i].split("#.#");
				Log.w(TAG, "add optional key:" + value[0] + " value:"+value[1]);
				mapOptional.put(value[0], value[1]);
			}
		}

		/*mapOptional.put("Platform", "2");
		mapOptional.put("ProductName", appInfo.packageName);
		mapOptional.put("AppID", mContext.getPackageManager().getApplicationLabel(appInfo).toString());*/
		
		switch (channel) {
		case 0: // 微信
			
			// 对于微信支付, 手机内存太小会有OutOfResourcesException造成的卡顿, 以致无法完成支付
			// 这个是微信自身存在的问题
			
			if((mMode & PAY_MODE_WECHAT) == 0){
				Toast.makeText(mContext, "当前配置不支持此支付方式", Toast.LENGTH_LONG).show();
				loadingDialog.dismiss();
				break;
			}

			if (!BCPay.isWXAppInstalledAndSupported() || !BCPay.isWXPaySupported()) {
				Toast.makeText(mContext, "您尚未安装微信或者安装的微信版本不支持", Toast.LENGTH_LONG).show();
				loadingDialog.dismiss();
				break;
			}
			
			mBillStack.push(bill);
			BCPay.getInstance(mContext).reqWXPaymentAsync(
					title, // 订单标题
					amount, // 订单金额(分)
					bill, // 订单流水号
					mapOptional, // 扩展参数(可以null)
					bcCallback); // 支付完成后回调入口
			break;

		case 1: // 支付宝支付
			
			if((mMode & PAY_MODE_ALIPAY) == 0){
				Toast.makeText(mContext, "当前配置不支持此支付方式", Toast.LENGTH_LONG).show();
				loadingDialog.dismiss();
				break;
			}
			
			mBillStack.push(bill);
			BCPay.getInstance(mContext).reqAliPaymentAsync(
					title, 
					amount, 
					bill, 
					mapOptional,
					bcCallback);

			break;
		}
	}
	
	public int GetSupportPayMode(){
		int mod = 0;
		
		if (BCPay.isWXAppInstalledAndSupported() && BCPay.isWXPaySupported()) {
			mod |= PAY_MODE_WECHAT;
		}
		
		//支付宝不用安装app，如果没有安装会跳转到网页支付
		mod |= PAY_MODE_ALIPAY;
		return mod;
	}

	void getBillInfoByID(String id) {

		BCQuery.getInstance().queryBillByIDAsync(id, new BCCallback() {
			@Override
			public void done(BCResult result) {
				BCQueryBillResult billResult = (BCQueryBillResult) result;

				Log.d(TAG, "------ response info ------");
				Log.d(TAG, "------getResultCode------" + billResult.getResultCode());
				Log.d(TAG, "------getResultMsg------" + billResult.getResultMsg());
				Log.d(TAG, "------getErrDetail------" + billResult.getErrDetail());

				if (billResult.getResultCode() != 0)
					return;

				Log.d(TAG, "------- bill info ------");
				BCBillOrder billOrder = billResult.getBill();
				Log.d(TAG, "订单唯一标识符：" + billOrder.getId());
				Log.d(TAG, "订单号:" + billOrder.getBillNum());
				Log.d(TAG, "订单金额, 单位为分:" + billOrder.getTotalFee());
				Log.d(TAG, "渠道类型:" + BCReqParams.BCChannelTypes.getTranslatedChannelName(billOrder.getChannel()));
				Log.d(TAG, "子渠道类型:" + BCReqParams.BCChannelTypes.getTranslatedChannelName(billOrder.getSubChannel()));
				Log.d(TAG, "订单是否成功:" + billOrder.getPayResult());

				if (billOrder.getPayResult())
					Log.d(TAG, "渠道返回的交易号，未支付成功时，是不含该参数的:" + billOrder.getTradeNum());
				else
					Log.d(TAG, "订单是否被撤销，该参数仅在线下产品（例如二维码和扫码支付）有效:" + billOrder.getRevertResult());

				Log.d(TAG, "订单创建时间:" + new Date(billOrder.getCreatedTime()));
				Log.d(TAG, "扩展参数:" + billOrder.getOptional());
				Log.w(TAG, "订单是否已经退款成功(用于后期查询): " + billOrder.getRefundResult());
				Log.w(TAG, "渠道返回的详细信息，按需处理: " + billOrder.getMessageDetail());

			}
		});
	}
}
