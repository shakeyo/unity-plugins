package com.UGS.NativePlugins;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.UGS.NativePluins.R;
import com.tencent.mm.sdk.modelbase.BaseResp;
import com.tencent.mm.sdk.modelmsg.SendAuth;
import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.tencent.mm.sdk.modelmsg.WXImageObject;
import com.tencent.mm.sdk.modelmsg.WXMediaMessage;
import com.tencent.mm.sdk.modelmsg.WXTextObject;
import com.tencent.mm.sdk.modelmsg.WXWebpageObject;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.unity3d.player.UnityPlayer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class WXAuth extends BroadcastReceiver {

	private IWXAPI mWeixinAPI;
	private String mCallbackObj;
	private String mCallback;
	// private String mAppID;
	private Context mContext;

	public static String WX_ACCESS_TOKEN = "access_token";
	public static String WX_OPEN_ID = "openid";
	public static String WX_REFRESH_TOKEN = "refresh_token";
	public static String WX_UNION_ID = "unionid";
	public static String WX_SCOPE_STR = "snsapi_userinfo";
	public static String WX_STATE_STR = "wx_login";

	public static String AppID = "";

	private static final int THUMB_SIZE = 150;

	public WXAuth(Context context, String appID) {

		mContext = context;
		AppID = appID;

	}

	public Boolean Init() {
		Log.w(DefineConst.LOG_TAG, "WxAuth init. appid:" + AppID);
		mWeixinAPI = WXAPIFactory.createWXAPI(mContext, AppID, false);
		if (!IsSupported()) {
			Toast.makeText(mContext, "您尚未安装微信或者安装的微信版本不支持", Toast.LENGTH_LONG).show();
			return false;
		}

		mWeixinAPI.registerApp(AppID);
		return true;
	}

	public void UnInit() {
		Log.w(DefineConst.LOG_TAG, "WxAuth UnInit");
		if (mWeixinAPI != null) {
			mWeixinAPI.detach();
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.w(DefineConst.LOG_TAG, "WxAuth onReceive:" + intent.getAction());

		if (intent.getAction().equals(DefineConst.BROADCAST_ACTION_WEIXIN_AUTH_RESP)) {
			int errCode = intent.getExtras().getInt("errCode");
			String code = intent.getExtras().getString("code");
			String result = String.format("%d:%s", errCode, code == null ? "用户取消" : code);
			UnityPlayer.UnitySendMessage(mCallbackObj, mCallback, result);
		} else if (intent.getAction().equals(DefineConst.BROADCAST_ACTION_WEIXIN_SHARE_RESP)) {
			int errCode = intent.getExtras().getInt("errCode");
			UnityPlayer.UnitySendMessage(mCallbackObj, mCallback, errCode + "");
		}

		// loadingDialog.dismiss();

		Activity act = (Activity) mContext;
		act.finish();
	}

	public void SendAuth(String cbObj, String cb) {

		Log.w(DefineConst.LOG_TAG, "WxAuth SendRequest");

		if (!IsSupported()) {
			Toast.makeText(mContext, "没有安装微信或者微信版本不支持", Toast.LENGTH_LONG).show();
			return;
		}

		mCallbackObj = cbObj;
		mCallback = cb;

		SendAuth.Req req = new SendAuth.Req();
		req.scope = WX_SCOPE_STR;
		req.state = WX_STATE_STR;
		mWeixinAPI.registerApp(AppID);
		mWeixinAPI.sendReq(req);
	}
	
	public Bitmap GetAppIcon(){
		PackageManager mgr = mContext.getApplicationContext().getPackageManager();
		try {
			Drawable icon = mgr.getApplicationIcon(mContext.getPackageName());
			BitmapDrawable bd = (BitmapDrawable)icon;
			return bd.getBitmap();
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public void SendShareUrl(String title, String text, String url, int scene) {

		Log.w(DefineConst.LOG_TAG, "WxAuth SendRequest:" + title + " text:" + text+" scene:"+scene);

		WXWebpageObject webObj = new WXWebpageObject();
		webObj.webpageUrl = url;

		WXMediaMessage msg = new WXMediaMessage(webObj);
		msg.mediaObject = webObj;
		msg.description = text;
		msg.title = title;
		
		Bitmap thumb = GetAppIcon();
		if(thumb == null){
			Log.w(DefineConst.LOG_TAG, "null image");
		}else{
			Log.w(DefineConst.LOG_TAG, "share thumb:"+thumb.getRowBytes());
			msg.thumbData = bmpToByteArray(thumb, false);
		}

		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.transaction = String.valueOf(System.currentTimeMillis());
		req.message = msg;
		req.scene = scene;
		mWeixinAPI.sendReq(req);
	}

	private byte[] bmpToByteArray(Bitmap bmp, boolean needRecycle) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		bmp.compress(CompressFormat.JPEG, 100, output);
		if (needRecycle) {
			bmp.recycle();
		}

		byte[] result = output.toByteArray();
		try {
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	protected static char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
			'f' };
	protected static MessageDigest messagedigest = null;

	static {
		try {
			messagedigest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			Log.w(DefineConst.LOG_TAG, "MD5FileUtil messagedigest初始化失败:" + e.getMessage());
		}
	}

	public static String getFileMD5String(File file) throws IOException {
		FileInputStream in = new FileInputStream(file);
		FileChannel ch = in.getChannel();
		MappedByteBuffer byteBuffer = ch.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
		messagedigest.update(byteBuffer);
		return bufferToHex(messagedigest.digest());
	}

	public static String getMD5String(String s) {
		return getMD5String(s.getBytes());
	}

	public static String getMD5String(byte[] bytes) {
		messagedigest.update(bytes);
		return bufferToHex(messagedigest.digest());
	}

	private static String bufferToHex(byte bytes[]) {
		return bufferToHex(bytes, 0, bytes.length);
	}

	private static String bufferToHex(byte bytes[], int m, int n) {
		StringBuffer stringbuffer = new StringBuffer(2 * n);
		int k = m + n;
		for (int l = m; l < k; l++) {
			appendHexPair(bytes[l], stringbuffer);
		}
		return stringbuffer.toString();
	}

	private static void appendHexPair(byte bt, StringBuffer stringbuffer) {
		char c0 = hexDigits[(bt & 0xf0) >> 4];
		char c1 = hexDigits[bt & 0xf];
		stringbuffer.append(c0);
		stringbuffer.append(c1);
	}

	public static byte[] BitmapToByteArray(Bitmap bmp, int maxSize) {

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		bmp.compress(Bitmap.CompressFormat.JPEG, 100, output);
		float zoom = (float) Math.sqrt(maxSize / (float) output.toByteArray().length); // 获取缩放比例

		// 设置矩阵数据
		Matrix matrix = new Matrix();
		matrix.setScale(zoom, zoom);

		// 根据矩阵数据进行新bitmap的创建
		Bitmap resultBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);

		output.reset();

		resultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);

		// 如果进行了上面的压缩后，依旧大于32K，就进行小范围的微调压缩
		while (output.toByteArray().length > maxSize) {
			matrix.setScale(0.7f, 0.7f);// 每次缩小 1/10
			resultBitmap = Bitmap.createBitmap(resultBitmap, 0, 0, resultBitmap.getWidth(), resultBitmap.getHeight(),
					matrix, true);

			output.reset();
			resultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
		}

		return output.toByteArray();
	}

	public void SendShareImageUrl(String img, int maxSize, int scene) {

		File file = new File(img);
		if (!file.exists()) {
			Log.e(DefineConst.LOG_TAG, img + " file no found!");
			return;
		}

		Log.e(DefineConst.LOG_TAG, "ShareImage. Img:" + img + " Size:" + (file.length() / 1024)+" scene:"+scene);

		Bitmap toShareBMP = BitmapFactory.decodeFile(img);
		WXImageObject imgObj = new WXImageObject(toShareBMP);
		WXMediaMessage msg = new WXMediaMessage();
		msg.mediaObject = imgObj;
		
		Bitmap thumbBmp = Bitmap.createScaledBitmap(toShareBMP, THUMB_SIZE, THUMB_SIZE, true);
		if(thumbBmp.getByteCount()>1024*32){
			byte[] bytes = BitmapToByteArray(thumbBmp, maxSize);
			thumbBmp.recycle();
			thumbBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
		}
		
		msg.thumbData = bmpToByteArray(thumbBmp, true);

		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.transaction = String.valueOf(System.currentTimeMillis());
		req.scene = scene;
		req.message = msg;
		mWeixinAPI.sendReq(req);
	}

	public Boolean IsSupported() {
		return mWeixinAPI.isWXAppInstalled() && mWeixinAPI.isWXAppSupportAPI();
	}

}
