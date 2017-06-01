//
//	AndroidPlugin.java
//  Created by Wang Wei(@onevcat) on 2013-11-7.
//
package com.onevcat.uniwebview;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UniWebChromeClient extends VideoEnabledWebChromeClient {

    public UniWebChromeClient(View activityNonVideoView, ViewGroup activityVideoView, View loadingView, VideoEnabledWebView webView) {
        super(activityNonVideoView, activityVideoView, loadingView, webView);
    }

    //The undocumented magic method override
    //IDE will swear at you if you try to put @Override here
    //For Android 3.0+
    public void openFileChooser(ValueCallback<Uri> uploadMsg) {
        if (AndroidPlugin._uploadMessages != null) {
            AndroidPlugin._uploadMessages.onReceiveValue(null);
        }

        AndroidPlugin.setUploadMessage(uploadMsg);
        startFileChooserActivity();
    }

    // For Android 3.0+
    public void openFileChooser( ValueCallback uploadMsg, String acceptType ) {
        if (AndroidPlugin._uploadMessages != null) {
            AndroidPlugin._uploadMessages.onReceiveValue(null);
        }

        AndroidPlugin.setUploadMessage(uploadMsg);
        startFileChooserActivity();
    }

    //For Android 4.1
    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture){
        if (AndroidPlugin._uploadMessages != null) {
            AndroidPlugin._uploadMessages.onReceiveValue(null);
        }

        AndroidPlugin.setUploadMessage(uploadMsg);
        startFileChooserActivity();
    }

    //For Android 5
    public boolean onShowFileChooser(
            WebView webView, ValueCallback<Uri[]> filePathCallback,
            WebChromeClient.FileChooserParams fileChooserParams) {

        // Double check that we don't have any existing callbacks
        if(AndroidPlugin._uploadCallback != null) {
            AndroidPlugin._uploadCallback.onReceiveValue(null);
        }

        AndroidPlugin.setUploadCallback(filePathCallback);
        startFileChooserActivity();

        return true;
    }

    private void startFileChooserActivity() {
        Activity activity = AndroidPlugin.getUnityActivity_();

        // Set up the take picture intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
                takePictureIntent.putExtra("PhotoPath", AndroidPlugin._cameraPhotoPath);
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e(AndroidPlugin.LOG_TAG, "Unable to create Image File", ex);
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                AndroidPlugin._cameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
            } else {
                takePictureIntent = null;
            }
        }

        // Set up the intent to get an existing image
        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("image/*");

        // Set up the intents for the Intent chooser
        Intent[] intentArray;
        if(takePictureIntent != null) {
            intentArray = new Intent[]{takePictureIntent};
        } else {
            intentArray = new Intent[0];
        }

        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

        activity.startActivityForResult(chooserIntent, AndroidPlugin.FILECHOOSER_RESULTCODE);

    }

    public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
        // callback.invoke(String origin, boolean allow, boolean remember);
        callback.invoke(origin, true, false);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }
}