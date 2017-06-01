package com.UGS.NativePlugins;

import java.io.File;
import java.io.IOException;

import com.unity3d.player.UnityPlayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.util.Log;

public class VoiceCaptureProxy  implements VoiceCaptureCallback
{
    protected static VoiceDealer _voiceDealer;
    protected static String _voiceCaptureCBObj;

    @Override
    public void onVoiceCaptureFinished(boolean flag, String s, long l){
    	UnityPlayer.UnitySendMessage(_voiceCaptureCBObj, "onVoiceCaptureFinished", 
    			String.format("%d:%s:%l", flag, s, l));
	}

    @Override
	public void onVoiceVolume(float f){
    	UnityPlayer.UnitySendMessage(_voiceCaptureCBObj, "onVoiceVolume", f+"");
	}

    @Override
	public void onVoiceCaptureError(int i){
    	UnityPlayer.UnitySendMessage(_voiceCaptureCBObj, "onVoiceCaptureError", i+"");
	}
    
    public static void startRecVoice(String cacheDir, String cbObj){
    	
    	Log.d(AndroidPlugin.LOG_TAG, "startRecVoice: " + cacheDir+" "+cbObj);
    	
    	_voiceCaptureCBObj = cbObj;
    	
    	if(_voiceDealer == null){
    		_voiceDealer = new VoiceDealer(new VoiceCaptureProxy());
    	}
    	
    	_voiceDealer.startRec(cacheDir);
    }
    
    public static void stopRecVoid(){
    	_voiceDealer.stopRec();
    }
}