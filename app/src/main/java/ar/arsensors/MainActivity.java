package ar.arsensors;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import ar.arsensors.model.ModelObject;
import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 255;
    private static final int IGNORE_OPTIMIZATION_REQUEST = 254;
    private OverlayView arContent = null;
    String CAMERA = Manifest.permission.CAMERA;
    String STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    @BindView(R.id.ar_view_pane)
    FrameLayout arViewPane;

    @BindView(R.id.tv_output)
    TextView tvOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        //request whitelist this application
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                //startActivity(intent);
                startActivityForResult(intent, IGNORE_OPTIMIZATION_REQUEST);
            }
        }


        if (Build.VERSION.SDK_INT < 23) {
            //Do not need to check the permission on old Android
            initCamera();
        } else {
            if (checkPermission()) {
                //Toast.makeText(MainActivity.this, "All Permissions Granted Successfully", Toast.LENGTH_LONG).show();
                initCamera();
            } else {
                requestPermission();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IGNORE_OPTIMIZATION_REQUEST) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            boolean isIgnoringBatteryOptimizations = pm.isIgnoringBatteryOptimizations(getPackageName());
            if (isIgnoringBatteryOptimizations) {
                Toast.makeText(MainActivity.this, "Ignoring battery optimization", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "App use battery optimization", Toast.LENGTH_LONG).show();
            }
        }
    }


    private void initCamera() {
        ArDisplayView arDisplay = new ArDisplayView(getBaseContext(), this);
        arViewPane.addView(arDisplay);

        arContent = new OverlayView(getApplicationContext(), mCanvasListener);
        arViewPane.addView(arContent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (arContent != null) arContent.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (arContent != null) arContent.onResume();
    }

    //listener to react on click of canvas item
    OnCanvasObjectClicked mCanvasListener = new OnCanvasObjectClicked() {
        @Override
        public void clickCanvasObject(ModelObject model) {
            Toast.makeText(getBaseContext(), model.getCaption() + "\n" + model.getDescription(), Toast.LENGTH_SHORT).show();
        }

        public void sensorOutputResult(String result) {
            tvOutput.setText(result);
        }
    };


    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA);

        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{STORAGE, CAMERA}, PERMISSION_REQUEST_CODE);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {

                    boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean cameraAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (locationAccepted && cameraAccepted) {
                        Snackbar.make(tvOutput, "Permission Granted, Now you can work properly.", Snackbar.LENGTH_LONG).show();
                        initCamera();
                    } else {
                        Snackbar.make(tvOutput, "Permission Denied, You cannot access camera.", Snackbar.LENGTH_LONG).show();

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(STORAGE)) {
                                showMessageOKCancel("You need to allow access to both the permissions",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    requestPermissions(new String[]{STORAGE, CAMERA},
                                                            PERMISSION_REQUEST_CODE);
                                                }
                                            }
                                        });
                                return;
                            }
                        }
                    }
                }
                break;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

}