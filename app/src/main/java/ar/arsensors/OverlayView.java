package ar.arsensors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.hardware.Camera;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ar.arsensors.model.ModelObject;

import static android.view.MotionEvent.INVALID_POINTER_ID;


//http://www.techrepublic.com/article/pro-tip-create-your-own-magnetic-compass-using-androids-internal-sensors/

public class OverlayView extends View implements SensorEventListener,
        LocationListener {

    public static final String DEBUG_TAG = "OverlayView Log";

    private final Context context;
    private Handler handler;

    // Mount Washington, NH: 44.27179, -71.3039, 6288 ft (highest peak
    private final static Location mountWashington = new Location("manual");

    static {
        mountWashington.setLatitude(44.27179d);
        mountWashington.setLongitude(-71.3039d);
        mountWashington.setAltitude(1916.5d);
    }

    String accelData = "Accelerometer Data";
    String compassData =""; // "Compass Data";
    String gyroData =""; // "Gyro Data";

    private LocationManager locationManager = null;
    private SensorManager sensors = null;

    private Location lastLocation;
    private float[] lastAccelerometer;
    private float[] lastCompass;

    private float verticalFOV;
    private float horizontalFOV;

    private boolean isAccelAvailable;
    private boolean isCompassAvailable;
    private boolean isGyroAvailable;
    private Sensor accelSensor;
    private Sensor compassSensor;
    private Sensor gyroSensor;

    private TextPaint contentPaint;
    private Paint targetPaint, transparentPaint, textPaint, textWhitePaint, bluePaint;
    Paint paintColor, paintBorder, paintAlpha;
    private Bitmap bmpPointer, bmpSign, toDrawOn;
    private Canvas offScreen;
    private ShapeDrawable mDrawable;
    private Matrix matrix;
    //private StaticLayout staticLayout;
    private double bearing = 0;
    private GeomagneticField geomagneticField;


    private int viewWidth = 0, viewHeight = 0;
    private float mPosX;
    private float mPosY;
    private float mLastTouchX, mPrevTouchX;
    private float mLastTouchY, mPrevTouchY;
    private int mActivePointerId = INVALID_POINTER_ID;
    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.f;
    private List<ModelObject> list = new ArrayList<>();
    private Boolean objectAdded = false;
    private OnCanvasObjectClicked mListener;

    Date now; // = new Date();
    Calendar calendar;
    StringBuilder text = new StringBuilder();

    //AnimatedSprite mSprites = new AnimatedSprite();
    //List<AnimatedSprite> mSprites = new ArrayList<>();

    //ElaineAnimated elaine;


    public OverlayView(Context context, OnCanvasObjectClicked listener) {
        super(context);
        this.context = context;
        this.handler = new Handler();
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelSensor = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        compassSensor = sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroSensor = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mListener = listener;

        calendar = Calendar.getInstance();
        //calendar.setTime(now);

        bmpSign = BitmapFactory.decodeResource(this.getResources(), R.drawable.sign90);
        bmpPointer = BitmapFactory.decodeResource(this.getResources(), R.drawable.pointer50);
        matrix = new Matrix();

        startSensors();
        startGPS();


        Camera camera = Camera.open();
        Camera.Parameters params = camera.getParameters();
        verticalFOV = params.getVerticalViewAngle();
        horizontalFOV = params.getHorizontalViewAngle();
        camera.release();

        // paint for text
        contentPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        contentPaint.setStrokeWidth(0);
        contentPaint.setTextAlign(Align.LEFT);
        contentPaint.setTextSize(35);
        contentPaint.setStyle(Paint.Style.STROKE);
        contentPaint.setColor(Color.WHITE);

        //RadialGradient gradient = new RadialGradient(200, 200, 200, 0xFFFFFFFF, 0xFF000000, android.graphics.Shader.TileMode.CLAMP);

        // paint for target
        targetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        targetPaint.setARGB(128, 255, 255, 255);
        targetPaint.setColor(Color.LTGRAY);
        //targetPaint.setStyle(Paint.Style.FILL);
        //targetPaint.setColor(getResources().getColor(android.R.color.transparent));
        //targetPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR)); //not visible at all
        targetPaint.setAntiAlias(true);
        targetPaint.setFilterBitmap(true);
        targetPaint.setDither(true);
        //targetPaint.setShader(gradient);


        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.DKGRAY);
        textPaint.setAntiAlias(true);
        textPaint.setFilterBitmap(true);
        textPaint.setDither(true);
        textPaint.setTextAlign(Align.LEFT);
        textPaint.setTextSize(40);

        //http://realtruth.tech/news/augmented-realityadvertising/images/
        int blue_color = ContextCompat.getColor(context, R.color.colorBlue);
        bluePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bluePaint.setARGB(128, 255, 255, 255);
        bluePaint.setColor(blue_color);
        bluePaint.setAntiAlias(true);
        bluePaint.setFilterBitmap(true);
        bluePaint.setDither(true);

        textWhitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textWhitePaint.setColor(Color.WHITE);
        textWhitePaint.setAntiAlias(true);
        textWhitePaint.setFilterBitmap(true);
        textWhitePaint.setDither(true);
        textWhitePaint.setTextAlign(Align.LEFT);
        textWhitePaint.setTextSize(40);

        paintColor = new Paint();
        paintColor.setAntiAlias(true);
        paintColor.setColor(Color.RED);

        paintBorder = new Paint();
        paintBorder.setAntiAlias(true);
        paintBorder.setColor(Color.BLACK);
        paintBorder.setStyle(Paint.Style.STROKE);
        paintBorder.setStrokeWidth(10);

        paintAlpha = new Paint();
        paintAlpha.setColor(Color.parseColor("#90FFFFFF"));
        paintAlpha.setAntiAlias(true);

    }

    private void startSensors() {
        isAccelAvailable = sensors.registerListener(this, accelSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        isCompassAvailable = sensors.registerListener(this, compassSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        isGyroAvailable = sensors.registerListener(this, gyroSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        super.onSizeChanged(xNew, yNew, xOld, yOld);
        viewWidth = xNew;
        viewHeight = yNew;
    }

    private void initObjects() {
        if (viewWidth != 0) {
            objectAdded = true;
            Integer rectX, rectY;
            rectX = viewWidth / 2;
            rectY = viewHeight / 2;
            ModelObject model = new ModelObject(1, rectX, rectY, rectX + 400, rectY + 200, "fish here", "Lorem ipsum info", 0, 0L);
            list.add(model);


            rectX = 200;
            rectY = 200;
            //rectX = offScreen.getWidth() / 2;
            model = new ModelObject(2, rectX - 70, rectY - 70, rectX + 70, rectY + 70, "Sign placed here", "Lorem ipsum info2", 0, 0L);
            list.add(model);


            rectX = viewWidth - 400;
            rectY = viewHeight - 200;
            model = new ModelObject(3, rectX, rectY, rectX + 400, rectY + 200, "Blue adv placed here", "Lorem ipsum info3", 0, 0L);
            list.add(model);
        }
    }

    private void startGPS() {
        Criteria criteria = new Criteria();
        // criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAccuracy(Criteria.NO_REQUIREMENT);
        criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);

        String best = locationManager.getBestProvider(criteria, true);

        Log.v(DEBUG_TAG, "Best provider: " + best);
        //locationManager.requestLocationUpdates(best, 50, 0, this);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        //Log.d(DEBUG_TAG, "onDraw");
        super.onDraw(canvas);
        Integer rectX = 0, rectY = 0, count = 0;
        String txt = "", nameDir = "";

        // Draw something fixed (for now) over the camera view
        if (!objectAdded) initObjects();

        now = new Date();
        calendar.setTime(now);
        long longTs = new Timestamp(calendar.getTime().getTime()).getTime();
        long diffSeconds = 0;

        float curBearingToMW = 0.0f;

        text.setLength(0);

        // compute rotation matrix
        //double rotationInDegrees, coordX, coordY;
        float mCurrentDegree = 0f;
        float azimuthInDegress = 0f;
        float[] mOrientation = new float[3];
        float rotation[] = new float[9];
        float identity[] = new float[9];
        if (lastAccelerometer != null && lastCompass != null) {
            boolean gotRotation = SensorManager.getRotationMatrix(rotation,
                    identity, lastAccelerometer, lastCompass);
            if (gotRotation) {
                float cameraRotation[] = new float[9];
                // remap such that the camera is pointing straight down the Y axis
                SensorManager.remapCoordinateSystem(rotation,
                        SensorManager.AXIS_X, SensorManager.AXIS_Z,
                        cameraRotation);

                // orientation vector
                float orientationMatrix[] = new float[3];
                SensorManager.getOrientation(cameraRotation, orientationMatrix);


                //https://www.ssaurel.com/blog/learn-how-to-make-a-compass-application-for-android/
                SensorManager.getRotationMatrix(rotation, null, lastAccelerometer, lastCompass);
                SensorManager.getOrientation(cameraRotation, mOrientation);


                bearing = mOrientation[0];
                bearing = Math.toDegrees(bearing);
                // fix difference between true North and magnetical North
                if (geomagneticField != null) {
                    bearing += geomagneticField.getDeclination();
                }
                if (bearing < 0) {
                    bearing += 360;
                }
                nameDir = updateTextDirection(bearing);
                text.append("Pointer: " + nameDir + ", " + ((char) 176) + "" + bearing + "\n");

                txt = "" + nameDir + ", " + ((char) 176) + "" + bearing + "\n";

/*
                if (mSprites.size() > 0) {
                    for (AnimatedSprite a : mSprites) {
                        a.draw(canvas);
                        a.Update(longTs);
                    }
                }
*/
                //we identify objects before show/hide them on View
                for (ModelObject item : list) {
                    if (nameDir.equals("S") || nameDir.equals("SE")) {
                        if (item.getId() == 1) {
                            item.setVisible(1);
                            item.setTsVisible(longTs);
                        }
                    }
                    if (nameDir.equals("N") || nameDir.equals("NE") || nameDir.equals("E")) {
                        if (item.getId() == 2) {
                            item.setVisible(1);
                            item.setTsVisible(longTs);
                        }
                    }
                    if (nameDir.equals("W") || nameDir.equals("SW") || nameDir.equals("NW")) {
                        if (item.getId() == 3) {
                            item.setVisible(1);
                            item.setTsVisible(longTs);
                        }
                    }
                    if (item.getVisibility() == 1 && item.getTsVisibility() != 0L) {
                        diffSeconds = (longTs - item.getTsVisibility()) / 1000 % 60;
                        //Log.wtf(DEBUG_TAG, "id: " + item.getId() + ", diffSeconds: " + diffSeconds);
                        if (diffSeconds >= 7) {
                            item.setVisible(0);
                            item.setTsVisible(0L);
                        }
                    }
                }

                toDrawOn = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
                offScreen = new Canvas(toDrawOn);

                for (ModelObject item : list) {
                    count += 1;
                    rectX = item.getX1();
                    rectY = item.getY1();
                    RectF rectF = new RectF(item.getX1(), item.getY1(), item.getX2(), item.getY2());

                    if (count == 1 && item.getVisibility() == 1) {
                        //paintColor.setColor(Color.RED);
                        offScreen.drawCircle(getWidth() / 4, getHeight() / 2, getWidth() / 5 - 50, paintColor);
                        offScreen.drawCircle(getWidth() / 4, getHeight() / 2, getWidth() / 5 - 45, paintBorder);

                        offScreen.drawRoundRect(rectF, 10, 10, targetPaint);
                        offScreen.drawBitmap(bmpPointer, rectX + 5, rectY + 5, targetPaint);

                        offScreen.drawText(txt, rectX + 160, rectY + 55, textPaint);
                        offScreen.drawText(item.getDescription(), rectX + 160, rectY + 105, textPaint);

                        // Draw bitmap 'toDrawOn' to canvas using 'paintAlpha'
                        canvas.drawBitmap(toDrawOn, 0, 0, paintAlpha);
                    }

                    if (count == 2 && item.getVisibility() == 1) {
                        offScreen.drawBitmap(bmpSign, rectX + 5, rectY + 5, targetPaint);
                        canvas.drawBitmap(toDrawOn, 0, 0, paintAlpha);
                    }

                    if (count == 3 && item.getVisibility() == 1) {
                        offScreen.drawRoundRect(rectF, 10, 10, bluePaint);

                        offScreen.drawBitmap(bmpPointer, rectX + 5, rectY + 5, bluePaint);

                        offScreen.drawText(item.getCaption(), rectX + 160, rectY + 55, textWhitePaint);
                        offScreen.drawText(item.getDescription(), rectX + 160, rectY + 105, textWhitePaint);
                        canvas.drawBitmap(toDrawOn, 0, 0, paintAlpha);

                        mDrawable = new ShapeDrawable(new OvalShape());
                        mDrawable.getPaint().setColor(0xff74AC23);
                        mDrawable.setBounds(rectX, rectY, rectX + 160, rectY + 55);
                        mDrawable.draw(canvas);
                    }
                }
            }
        }

        canvas.translate(15.0f, 15.0f); //Preconcat the current matrix with the specified translation
        StaticLayout staticLayout = new StaticLayout(text.toString(), contentPaint,
                580, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true); //mSpacingMult, mSpacingAdd
        staticLayout.draw(canvas);
    }


    public void onAccuracyChanged(Sensor arg0, int arg1) {
        Log.d(DEBUG_TAG, "onAccuracyChanged");
    }

    public void onSensorChanged(SensorEvent event) {
        Log.d(DEBUG_TAG, "onSensorChanged");

        StringBuilder msg = new StringBuilder(event.sensor.getName()).append(" ");
        for (float value : event.values) {
            msg.append("[").append(String.format("%.3f", value)).append("]");
        }

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                lastAccelerometer = event.values.clone();
                accelData = msg.toString();
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyroData = msg.toString();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                lastCompass = event.values.clone();
                //smoothed = LowPassFilter.filter(event.values, geomagnetic);
                compassData = msg.toString();
                break;
        }
        if (compassData.equals("")) mListener.sensorOutputResult("Compass data not found. May be sensor absent on device.");
        this.invalidate();
    }

    //store data
    public void onLocationChanged(Location location) {
        lastLocation = location;
    }

    public void onProviderDisabled(String provider) {
    }

    public void onProviderEnabled(String provider) {
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public void onPause() {
        //locationManager.removeUpdates(this);
        sensors.unregisterListener(this);
    }

    public void onResume() {
        startSensors();
        startGPS();
    }

    public Bitmap rotateBitmap(Bitmap source, float angle) {
        matrix.reset();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private String updateTextDirection(double bearing) {
        int range = (int) (bearing / (360f / 16f));
        String dirTxt = "";

        if (range == 15 || range == 0)
            dirTxt = "N";
        if (range == 1 || range == 2)
            dirTxt = "NE";
        if (range == 3 || range == 4)
            dirTxt = "E";
        if (range == 5 || range == 6)
            dirTxt = "SE";
        if (range == 7 || range == 8)
            dirTxt = "S";
        if (range == 9 || range == 10)
            dirTxt = "SW";
        if (range == 11 || range == 12)
            dirTxt = "W";
        if (range == 13 || range == 14)
            dirTxt = "NW";
        return dirTxt;
    }


    //https://stackoverflow.com/questions/5743328/image-in-canvas-with-touch-events/5747233#5747233
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Let the ScaleGestureDetector inspect all events.
        mScaleDetector.onTouchEvent(ev);

        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                mLastTouchX = x;
                mLastTouchY = y;
                mActivePointerId = ev.getPointerId(0);
/*
                Bitmap b;
                AnimatedSprite a = new AnimatedSprite();
                b = BitmapFactory.decodeResource(this.getResources(), R.drawable.basket);

                a.Initialize(b, 200, 200, 24, 20, true);
                a.setXPos((int) x);
                a.setYPos((int) y);

                synchronized (mSprites) {
                    mSprites.add(a);
                }
                */
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                final float x = ev.getX(pointerIndex);
                final float y = ev.getY(pointerIndex);

                // Only move if the ScaleGestureDetector isn't processing a gesture.
                if (!mScaleDetector.isInProgress()) {
                    final float dx = x - mLastTouchX;
                    final float dy = y - mLastTouchY;

                    mPosX += dx;
                    mPosY += dy;

                    invalidate();
                }
                mLastTouchX = x;
                mLastTouchY = y;

                checkForClickedCanvasObject();
                break;
            }

            case MotionEvent.ACTION_UP: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mLastTouchX = ev.getX(newPointerIndex);
                    mLastTouchY = ev.getY(newPointerIndex);
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                }
                break;
            }
        }
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 10.0f));
            invalidate();
            return true;
        }
    }

    private void checkForClickedCanvasObject() {
        Log.wtf(DEBUG_TAG, "mLastTouchX: " + mLastTouchX);

        if (mPrevTouchX != mLastTouchX && mPrevTouchY != mLastTouchY) {
            for (ModelObject item : list) {
                if (item.getX1() <= mLastTouchX && mLastTouchX <= item.getX2()
                        && item.getY1() <= mLastTouchY && mLastTouchY <= item.getY2()
                        && item.getVisibility() == 1) {

                    Log.wtf(DEBUG_TAG, "mLastTouchX: " + mLastTouchX + ", mLastTouchY: " + mLastTouchY);
                    mListener.clickCanvasObject(item);
                }
            }
        }
        mPrevTouchX = mLastTouchX;
        mPrevTouchY = mLastTouchY;
    }


}