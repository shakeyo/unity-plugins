package com.UGS.NativePlugins;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.util.Log;

public class VoiceDealer extends Thread
{
    private final int INTERNAL = 150;
    private String mFilePath;
    private MediaRecorder mMediaRecorder;
    private long mStartTime;
    private boolean mIsRecording;
    private VoiceCaptureCallback mCallback;

    public VoiceDealer(VoiceCaptureCallback cb)
    {
    	this.mCallback = cb;
    }

    
    public void startRec(String cacheDir)
    {
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSamplingRate(300000);
        mMediaRecorder.setAudioEncodingBitRate(8000);
        mMediaRecorder.setAudioChannels(1);
        mMediaRecorder.setAudioSource(1);
        mMediaRecorder.setOutputFormat(3);
        mMediaRecorder.setAudioEncoder(1);
        
        File file = new File(cacheDir, (new StringBuilder(String.valueOf(System.currentTimeMillis()))).append("temp.voice").toString());
        mFilePath = file.getPath();
        mMediaRecorder.setOutputFile(mFilePath);
        try
        {
        	Log.d("VoiceDealer", "startRec:"+mFilePath);
            mMediaRecorder.prepare();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            
            this.mCallback.onVoiceCaptureError(-1);
        }
        mMediaRecorder.start();
        mStartTime = System.currentTimeMillis();
        start();
    }

    public void stopRec()
    {
        mIsRecording = false;
        mMediaRecorder.stop();
        mMediaRecorder.release();
        mMediaRecorder = null;
        long duration = (System.currentTimeMillis() - mStartTime) / 1000L;
        
        this.mCallback.onVoiceCaptureFinished(true, mFilePath, duration);
    }

    public void run()
    {
        mIsRecording = true;
        while(mIsRecording)
        {
            UpdateCurrentVolume();
            
            try
            {
                sleep(150L);
            }
            catch(InterruptedException e)
            {
            	mIsRecording = false;
                e.printStackTrace();
                this.mCallback.onVoiceCaptureError(-2);
                break;
            }
        }
    }

    private void UpdateCurrentVolume()
    {
        if(mMediaRecorder != null)
        {
            double ratio = mMediaRecorder.getMaxAmplitude();
            if(ratio > 1.0D)
            {
                float currentVolume = (float)(20D * Math.log10(ratio));
                this.mCallback.onVoiceVolume(currentVolume);
            }
        }
    }
}