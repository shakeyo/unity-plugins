package com.UGS.NativePlugins;

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
            	
            	Activity act = UnityPlayer.currentActivity;
            	NativeAPI.InstallApk(saveFileName, UnityPlayer.currentActivity);
                act.finish();
               
                break;  
            case DOWN_ERROR:
            	//downloadDialog.setCancelable(true);
            	//downloadDialog.setCanceledOnTouchOutside(true);
            	//downloadDialog.setMessage(downloadMsg);
            	downloadDialog.dismiss();
            	
                AlertDialog.Builder builder=new AlertDialog.Builder(UnityPlayer.currentActivity);  //先得到构造器  
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
                        
                        UnityPlayer.currentActivity.finish();
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

    
    //外部接口让主Activity调用  
    public void Download(final String url, final String storge){
    	
    	apkUrl = url;
    	savePath = storge;
    	
    	saveFileName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/"+storge;
    
    	downloadDialog = new ProgressDialog(UnityPlayer.currentActivity);
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
                    downloadMsg = String.format("正在下载:%s %s/%s", fName, NativeAPI.Size2String(count), NativeAPI.Size2String(length));
                    
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
    
}
    