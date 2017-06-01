package com.QianNiao.ToolChain;

import java.io.File;  
import java.io.FileOutputStream;  
import java.io.IOException;  
import java.io.InputStream;

import java.net.HttpURLConnection;  
import java.net.MalformedURLException;  
import java.net.URL;
import java.text.DecimalFormat;

import com.unity3d.player.UnityPlayer;

import android.app.Activity;
import android.app.AlertDialog;  
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.net.Uri;

import android.os.Environment;
import android.os.Handler;  
import android.os.Message;
import android.text.format.Formatter;
  
  
public class Updater {

	private Context mContext;  
    
    //返回的安装包url  
    private String apkUrl;  
    
    // 下载包安装路径 
    private String savePath;
      
    private String saveFileName; 
    
    private static final int DOWN_BEGIN = 0;  
    
    private static final int DOWN_UPDATE = 1;  
      
    private static final int DOWN_OVER = 2;
    
    private static final int DOWN_ERROR = 3;
    
    private String downloadMsg;
      
    private int progress;  
      
    private ProgressDialog downloadDialog;  
    
    private Thread downLoadThread;  
      
    private boolean interceptFlag = false;  
    
    private Notification notification = null;
    private NotificationManager notificationManager = null;
    
 
    private Handler mHandler = new Handler(){  
        public void handleMessage(Message msg) {  
            switch (msg.what) {  
            case DOWN_BEGIN:
            	downloadDialog.setMessage(downloadMsg);
            	break;
            case DOWN_UPDATE:  
            	downloadDialog.setMessage(downloadMsg);
            	downloadDialog.setProgress(progress); 
                break;  
            case DOWN_OVER:  
            	downloadDialog.dismiss();
            	
            	//uninstallAPK(mContext.getPackageName());
            	
            	installApk();
            	
            	Activity act = (Activity)mContext;
                act.finish();
               
                break;  
            case DOWN_ERROR:
            	//downloadDialog.setCancelable(true);
            	//downloadDialog.setCanceledOnTouchOutside(true);
            	//downloadDialog.setMessage(downloadMsg);
            	downloadDialog.dismiss();
            	
                AlertDialog.Builder builder=new AlertDialog.Builder(mContext);  //先得到构造器  
                builder.setTitle("下载失败"); //设置标题  
                builder.setMessage("Err:"+downloadMsg+",是否重试?"); //设置内容  
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() { //设置确定按钮  
                    @Override  
                    public void onClick(DialogInterface dialog, int which) {  
                        dialog.dismiss();
                        Download(apkUrl, savePath);
                    }  
                });  
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() { //设置取消按钮  
                    @Override  
                    public void onClick(DialogInterface dialog, int which) {  
                        dialog.dismiss();
                        
                        Activity act = (Activity)mContext;
                        act.finish();
                    }  
                });    
                //参数都设置完成了，创建并显示出来  
                builder.create().show();  
                
            	/*AlertDialog.Builder builder = new AlertDialog.Builder(mContext);  
                
                builder.setTitle("下载完毕")
                    .setMessage(downloadMsg)
                    .setCancelable(false)
                    .setPositiveButton("OK", null);
     
                //TODO 关闭程序或者重启
                builder.show();  */
                
            	break;
            default:  
                break;  
            }  
        };  
    };

    public Updater(Context context){
    	mContext = context;
    }
    
    //外部接口让主Activity调用  
    public void Download(final String url, final String storge){
    	
    	apkUrl = url;
    	savePath = storge;
    	
    	saveFileName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/"+storge;
    
    	downloadDialog = new ProgressDialog(mContext);
		downloadDialog.setTitle("游戏更新");
		downloadDialog.setMessage("请稍后。。。");
		downloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		downloadDialog.setCancelable(false);
		downloadDialog.setCanceledOnTouchOutside(false);
		// downloadDialog.setIcon(R.drawable.ic_launcher);
		downloadDialog.setMax(100);
		downloadDialog.show();

		downloadApk();
    }  
    
    private static final int NOTICE_ID = 1222;
    
	public void showNotification(String title, String content) {
		
		notificationManager = (NotificationManager) mContext
				.getSystemService(android.content.Context.NOTIFICATION_SERVICE);

		// 定义Notification的各种属性
		notification = new Notification(R.drawable.ic_launcher, title, System.currentTimeMillis());
		
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
		Intent notificationIntent = new Intent(mContext, mContext.getClass()); // 点击该通知后要跳转的Activity
		PendingIntent contentItent = PendingIntent.getActivity(mContext, 0, notificationIntent, PendingIntent.FLAG_NO_CREATE);
		notification.setLatestEventInfo(mContext, contentTitle, contentText, contentItent);

		// 把Notification传递给NotificationManager
		notificationManager.notify(0, notification);
	}
    
	private String Size2String(long size){  
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
	
    private Runnable mdownApkRunnable = new Runnable() {      
        @Override  
        public void run() {  
            try {  
                URL url = new URL(apkUrl);  
              
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();  
                conn.connect();  
                int length = conn.getContentLength();  
                InputStream is = conn.getInputStream();  
   
                File file = new File(saveFileName);
                if(file.isFile()){
                	file = new File(file.getParent());
                }
                
                if(file.isDirectory() && !file.exists()){
                	file.mkdir();
                }

                String apkFile = saveFileName;
                File ApkFile = new File(apkFile);  
                FileOutputStream fos = new FileOutputStream(ApkFile);  
                  
                String fName = apkUrl.trim();  
                fName = fName.substring(fName.lastIndexOf("/")+1);  

                downloadMsg = "正在下载文件:"+fName;
                mHandler.sendEmptyMessage(DOWN_BEGIN);  
                
                int count = 0;  
                byte buf[] = new byte[1024];  
                  
                do{                   
                    int numread = is.read(buf);  
                    count += numread;  
                    progress =(int)(((float)count / length) * 100);  
                    downloadMsg = String.format("正在下载:%s %s/%s", Size2String(count), Size2String(length));
                    
                    //更新进度  
                    mHandler.sendEmptyMessage(DOWN_UPDATE);  
                    if(numread <= 0){      
                        break;  
                    }
                    fos.write(buf,0,numread);  
                }while(!interceptFlag);//点击取消就停止下载.  
                  
                fos.close();  
                is.close();  
                
              //下载完成通知安装  
            	mHandler.sendEmptyMessage(DOWN_OVER);  
            	
            } catch (MalformedURLException e) {  
                e.printStackTrace();
                downloadMsg = "下载失败，出现异常:"+e.getMessage();
                mHandler.sendEmptyMessage(DOWN_ERROR); 
            } catch(IOException e){  
                e.printStackTrace();
                downloadMsg = "下载失败，出现异常:"+e.getMessage();
                mHandler.sendEmptyMessage(DOWN_ERROR); 
            }finally{
            	 
            }
        }  
    };  
      
     /** 
     * 下载apk 
     * @param url 
     */  
      
    private void downloadApk(){  
        downLoadThread = new Thread(mdownApkRunnable);  
        downLoadThread.start();  
    }
     /** 
     * 安装apk 
     * @param url 
     */  
    private void installApk(){  

        File apkfile = new File(saveFileName);  
        if (!apkfile.exists()) {  
            return;  
        }

        Intent it = new Intent(Intent.ACTION_VIEW);  
        it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        it.setDataAndType(Uri.parse("file://" + apkfile.toString()), "application/vnd.android.package-archive");   
        mContext.startActivity(it);
    }
    
    public void uninstallAPK(String packageName) {
		// 通过程序的报名创建URI
		Uri packageURI = Uri.parse("package:" + packageName);
		// 创建Intent意图
		Intent intent = new Intent(Intent.ACTION_DELETE);
		intent.setData(packageURI);
		// 执行卸载程序
		mContext.startActivity(intent);
	}
}
    