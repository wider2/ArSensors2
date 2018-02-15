package ar.arsensors;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

/**
 * Created by Alexei on 8/22/2017.
 */

public class ArDisplayView extends SurfaceView
        implements SurfaceHolder.Callback
{
    public static final String DEBUG_TAG = "ArDisplayView Log";
    Camera mCamera;
    SurfaceHolder mHolder;
    Activity mActivity;

    public ArDisplayView(Context context, Activity activity) {
        super(context);
        mActivity = activity;

        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open();

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        mCamera.setDisplayOrientation((info.orientation - degrees + 360) % 360);

        try {
            mCamera.setPreviewDisplay(mHolder);
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "surfaceCreated exception: ", e);
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> prevSizes = params.getSupportedPreviewSizes();
        for (Camera.Size s : prevSizes)
        {
            if((s.height <= height) && (s.width <= width))
            {
                params.setPreviewSize(s.width, s.height);
                break;
            }
        }
        mCamera.setParameters(params);
        mCamera.startPreview();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mCamera.release();
    }

}
