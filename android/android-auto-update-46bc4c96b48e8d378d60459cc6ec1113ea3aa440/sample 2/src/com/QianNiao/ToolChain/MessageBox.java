package com.QianNiao.ToolChain;

import com.unity3d.player.UnityPlayer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.app.Activity;

public class MessageBox {


	private final int MSG_STYLE_OK = 0;
	private final int MSG_STYLE_CANCEL = 1;
	private final int MSG_STYLE_OKCANCEL = 2;
	private final int MSG_STYLE_YESNO = 3;
	
	
    public void MsgBox(final String title, 
    		final String msg, 
    		final int mode, 
    		final String callbackObj, 
    		final String callback){
    	
    	UnityPlayer.currentActivity.runOnUiThread(new Runnable(){

			@Override
			public void run() {
				AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(UnityPlayer.currentActivity);                      
		        dlgAlert.setMessage(msg);
		        dlgAlert.setTitle(title);            
		        
		        switch(mode){
		        case MSG_STYLE_OK:{
		        	dlgAlert.setPositiveButton("确定", new DialogInterface.OnClickListener() { 
		                public void onClick(DialogInterface dialog, int whichButton) { 
		                    //setResult(Activity.RESULT_OK);//确定按钮事件 
		                    //finish(); 
		                    
		                    UnityPlayer.UnitySendMessage(callbackObj, callback, Activity.RESULT_OK+"");
		                }
		            }); 
		        	break;
		        }
		        case MSG_STYLE_CANCEL:{
		        	dlgAlert.setPositiveButton("取消", new DialogInterface.OnClickListener() { 
		                public void onClick(DialogInterface dialog, int whichButton) { 
		                    //setResult(Activity.RESULT_CANCELED);//确定按钮事件 
		                    //finish(); 
		                    
		                    UnityPlayer.UnitySendMessage(callbackObj, callback, Activity.RESULT_CANCELED+"");
		                }
		            }); 
		        	break;
		        }
		        case MSG_STYLE_OKCANCEL:{
		        	dlgAlert.setNegativeButton("确定", new DialogInterface.OnClickListener() { 
		                public void onClick(DialogInterface dialog, int whichButton) { 
		                    //setResult(Activity.RESULT_OK);//确定按钮事件 
		                    //finish(); 
		                    
		                    UnityPlayer.UnitySendMessage(callbackObj, callback, Activity.RESULT_OK+"");
		                }
		            }); 
		        	dlgAlert.setPositiveButton("取消", new DialogInterface.OnClickListener() { 
		                public void onClick(DialogInterface dialog, int whichButton) { 
		                    //setResult(Activity.RESULT_CANCELED);//确定按钮事件 
		                    //finish(); 
		                    
		                    UnityPlayer.UnitySendMessage(callbackObj, callback, Activity.RESULT_CANCELED+"");
		                }
		            }); 
		        	break;
		        }
		        }
		       
		        dlgAlert.setCancelable(true);
		        dlgAlert.create().show();	
			}
    	});
    }
}
