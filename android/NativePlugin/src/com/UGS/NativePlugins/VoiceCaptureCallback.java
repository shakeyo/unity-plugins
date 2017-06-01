package com.UGS.NativePlugins;

public abstract interface VoiceCaptureCallback {

	public abstract void onVoiceCaptureFinished(boolean flag, String s, long l);

	public abstract void onVoiceVolume(float f);

	public abstract void onVoiceCaptureError(int i);
}
