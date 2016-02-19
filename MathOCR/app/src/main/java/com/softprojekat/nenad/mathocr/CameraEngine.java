package com.softprojekat.nenad.mathocr;

import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;

/**
 * Created by Nenad on 2/16/2016.
 */
public class CameraEngine {

    static final String TAG = "DBG_" + CameraUtils.class.getName();

    boolean on;
    Camera camera;
    SurfaceHolder surfaceHolder;

    Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {

        }
    };

    public boolean isOn() {
        return on;
    }

    private CameraEngine(SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
    }

    public static CameraEngine New(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "Creating camera engine");
        return new CameraEngine(surfaceHolder);
    }

    public void requestFocus(){
        if(camera == null)
            return;

        if(isOn()) {
            camera.autoFocus(autoFocusCallback);
        }
    }

    public void start() {

        Log.d(TAG, "Entered com.softprojekat.nenad.mathocr.CameraEngine - start()");
        this.camera = CameraUtils.getCamera();

        if(camera == null)
            return;

        Log.d(TAG, "Got camera hardware");

        try {

            this.camera.setPreviewDisplay(this.surfaceHolder);
            this.camera.setDisplayOrientation(90);
            this.camera.startPreview();

            on = true;

            Log.d(TAG, "com.softprojekat.nenad.mathocr.CameraEngine preview started");

        } catch (IOException e) {
            Log.e(TAG, "Error in setPreviewDisplay");
        }

    }

    public void stop() {
        if(camera != null) {
            camera.release();
            camera = null;
        }

        on = false;

        Log.d(TAG, "com.softprojekat.nenad.mathocr.CameraEngine Stopped");
    }

    public void takeShot(Camera.ShutterCallback shutterCallback,
                         Camera.PictureCallback rawPictureCallback,
                         Camera.PictureCallback jpegPictureCallBack) {

        if(isOn()) {
            camera.takePicture(shutterCallback, rawPictureCallback, jpegPictureCallBack);
        }
    }

}
