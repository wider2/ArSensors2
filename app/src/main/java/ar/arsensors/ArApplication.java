package ar.arsensors;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;

/**
 * Created by Alexei on 8/22/2017.
 */

public class ArApplication extends Application {

    private final static String TAG = ArApplication.class.toString();

    @NonNull
    private static ArApplication instance;

    public ArApplication() {
        instance = this;
    }

    @NonNull
    public static ArApplication get() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        try {



        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}