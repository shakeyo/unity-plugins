package com.UGS.Toolchain;

import org.json.JSONException;
import org.json.JSONObject;

import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;
import com.unity3d.player.UnityPlayer;

public class GeoLocation  implements TencentLocationListener
{
	protected String _locationCallback = "LocationListener";
    
    public int InitLocationService(String callback){
    	
    	_locationCallback = callback;
    	TencentLocationRequest request = TencentLocationRequest.create();
    	TencentLocationManager locationManager = TencentLocationManager.getInstance(UnityPlayer.currentActivity);
    	return locationManager.requestLocationUpdates(request, this);
    }
    
    public void UninitLocationService(){
    	TencentLocationManager locationManager = TencentLocationManager.getInstance(UnityPlayer.currentActivity);
    	locationManager.removeUpdates(this);
    }
    
    private static String prepareLocationJSONObject(TencentLocation location, int error, String reason){  
        JSONObject studentJSONObject = new JSONObject();  
        try {  
            studentJSONObject.put("latitude", location.getLatitude());
            studentJSONObject.put("longitude", location.getLongitude());
            studentJSONObject.put("altitude", location.getAltitude());
            studentJSONObject.put("accuracy", location.getAccuracy());
            studentJSONObject.put("name", location.getName());
            studentJSONObject.put("address", location.getAddress());
            studentJSONObject.put("error", error);  
            studentJSONObject.put("reason", reason);  
        } catch (JSONException e) {  
            e.printStackTrace();  
        }  
          
        return studentJSONObject.toString();  
    }
    
	@Override
	public void onLocationChanged(TencentLocation arg0, int arg1, String arg2) {
		String args = prepareLocationJSONObject(arg0, arg1, arg2);
		UnityPlayer.UnitySendMessage(_locationCallback, "LocationChanged", args);
	}

    private static String prepareStatusJSONObject(String name, int status, String desc){  
        JSONObject studentJSONObject = new JSONObject();  
        try {  
            studentJSONObject.put("name", name);  
            studentJSONObject.put("status", status);  
            studentJSONObject.put("desc", desc);  
        } catch (JSONException e) {  
            e.printStackTrace();  
        }  
          
        return studentJSONObject.toString();  
    }  
    
	@Override
	public void onStatusUpdate(String arg0, int arg1, String arg2) {
		String arg = prepareStatusJSONObject(arg0, arg1, arg2);
		UnityPlayer.UnitySendMessage(_locationCallback, "StatusChanged", arg);
	}
}
