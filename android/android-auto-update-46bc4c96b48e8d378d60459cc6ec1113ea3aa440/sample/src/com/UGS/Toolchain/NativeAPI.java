package com.UGS.Toolchain;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.DecimalFormat;

import com.unity3d.player.UnityPlayer;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.telephony.TelephonyManager;

public class NativeAPI {

    /** 
     * 安装apk 
     * @param url 
     */  
    public static void InstallApk(String apkFile, Activity act){  

        File apkfile = new File(apkFile);  
        if (!apkfile.exists()) {  
            return;  
        }

        Intent it = new Intent(Intent.ACTION_VIEW);  
        it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        it.setDataAndType(Uri.parse("file://" + apkfile.toString()), "application/vnd.android.package-archive");   
        act.startActivity(it);
    }
    
    public static void UninstallAPK(String packageName, Activity act) {
		// 通过程序的报名创建URI
		Uri packageURI = Uri.parse("package:" + packageName);
		// 创建Intent意图
		Intent intent = new Intent(Intent.ACTION_DELETE);
		intent.setData(packageURI);
		// 执行卸载程序
		act.startActivity(intent);
	}
    
    public static Boolean IsApkInstalled(String apkFile){
    	return false;
    }
    
    public static String GetPhoneNumber(){
    	TelephonyManager tMgr = (TelephonyManager)UnityPlayer.currentActivity.getSystemService(Context.TELEPHONY_SERVICE);
    	String mPhoneNumber = tMgr.getLine1Number();
    	return mPhoneNumber;
    }
    
	public static void ShowNotification(String title, String content, Activity act) {
		
		NotificationManager notificationManager = (NotificationManager) UnityPlayer.currentActivity
				.getSystemService(android.content.Context.NOTIFICATION_SERVICE);

		// 定义Notification的各种属性
		Notification notification = new Notification(0, title, System.currentTimeMillis());
		
		// FLAG_AUTO_CANCEL 该通知能被状态栏的清除按钮给清除掉
		// FLAG_NO_CLEAR 该通知不能被状态栏的清除按钮给清除掉
		// FLAG_ONGOING_EVENT 通知放置在正在运行
		// FLAG_INSISTENT 是否一直进行，比如音乐一直播放，知道用户响应
		notification.flags |= Notification.FLAG_NO_CLEAR; // 将此通知放到通知栏的"Ongoing"即"正在运行"组中
		//notification.flags |= Notification.FLAG_NO_CLEAR; // 表明在点击了通知栏中的"清除通知"后，此通知不清除，经常与FLAG_ONGOING_EVENT一起使用
		//notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		// DEFAULT_ALL 使用所有默认值，比如声音，震动，闪屏等等
		// DEFAULT_LIGHTS 使用默认闪光提示
		// DEFAULT_SOUNDS 使用默认提示声音
		// DEFAULT_VIBRATE 使用默认手机震动，需加上权限
		//notification.defaults = Notification.DEFAULT_LIGHTS;
		// 叠加效果常量
		// notification.defaults=Notification.DEFAULT_LIGHTS|Notification.DEFAULT_SOUND;
		//notification.ledARGB = Color.BLUE;
		//notification.ledOnMS = 5000; // 闪光时间，毫秒

		// 设置通知的事件消息
		CharSequence contentTitle = title; // 通知栏标题
		CharSequence contentText = content; // 通知栏内容
		Intent notificationIntent = new Intent(act, act.getClass()); // 点击该通知后要跳转的Activity
		PendingIntent contentItent = PendingIntent.getActivity(act, 0, notificationIntent, PendingIntent.FLAG_NO_CREATE);
		notification.setLatestEventInfo(act, contentTitle, contentText, contentItent);

		// 把Notification传递给NotificationManager
		notificationManager.notify(0, notification);
	}
	
	public static String Size2String(long size){  
		  DecimalFormat df = new DecimalFormat("0.00");  
		  String mysize = "";  
		  if( size > 1024*1024){  
		    mysize = df.format( size / 1024f / 1024f ) +"M";  
		  }else if( size > 1024 ){  
		    mysize = df.format( size / 1024f ) +"K";  
		  }else{  
		    mysize = size + "B";  
		  }  
		  return mysize;  
	} 
	
	
	
    public static void PlayAMR(final String url){
    	new Thread(new Runnable() {

            @Override
            public void run() {

                MediaPlayer mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mp.start();
                    }
                });

                try {
                    mMediaPlayer.setDataSource(UnityPlayer.currentActivity, Uri.parse(url));
                    mMediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    
}
