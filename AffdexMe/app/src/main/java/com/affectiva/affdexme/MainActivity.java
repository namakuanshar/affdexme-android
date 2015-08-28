package com.affectiva.affdexme;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.List;

import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.Frame.ROTATE;
import com.affectiva.android.affdex.sdk.detector.CameraDetector;
import com.affectiva.android.affdex.sdk.detector.Detector;
import com.affectiva.android.affdex.sdk.detector.Face;

/*
 * AffdexMe is an app that demonstrates the use of the Affectiva Android SDK.  It uses the
 * front-facing camera on your Android device to view, process and analyze live video of your face.
 * Start the app and you will see your own face on the screen and metrics describing your
 * expressions.
 *
 * Tapping the screen will bring up a menu with options to display the Processed Frames Per Second metric,
 * display facial tracking points, and control the rate at which frames are processed by the SDK.
 *
 * Most of the methods in this file control the application's UI. Therefore, if you are just interested in learning how the Affectiva SDK works,
 *  you will find the calls relevant to the use of the SDK in the initializeCameraDetector(), startCamera(), stopCamera(),
 *  and onImageResults() methods.
 *
 * This class implements the Detector.ImageListener interface, allowing it to receive the onImageResults() event.
 * This class implements the Detector.FaceListener interface, allowing it to receive the onFaceDetectionStarted() and
 * onFaceDetectionStopped() events.
 * This class implements the CameraDetector.CameraSurfaceViewListener interface, allowing it to receive
 * onSurfaceViewAspectRatioChanged() events.
 *
 * In order to use this project, you will need to:
 * - Obtain the SDK from Affectiva (visit http://www.affdex.com/mobile-sdk)
 * - Copy the SDK assets folder contents into this project's assets folder
 * - Copy the SDK libs folder contents into this project's libs folder
 * - Copy the armeabi-v7a folder (found in the SDK libs folder) into this project's jniLibs folder
 * - Add your license file to the /assets/Affdex folder and uncomment the line in the startCamera() method
 * to type in your license file name
 * - Build the project
 * - Run the app on an Android device with a front-facing camera
 *
 * Copyright (c) 2014 Affectiva. All rights reserved.
 */

public class MainActivity extends Activity
        implements Detector.FaceListener, Detector.ImageListener, View.OnTouchListener, CameraDetector.CameraEventListener {

    private static final String LOG_TAG = "Affectiva";
    public static final int NUM_METRICS_DISPLAYED = 6;

    //Affectiva SDK Object
    private CameraDetector detector = null;
    //TODO: License file in byte form. Should NOT be included in released sample code.
    private byte[] licenseBytes = {123,34,116,111,107,101,110,34,58,32,34,102,98,51,51,101,102,57,98,102,98,49,57,53,100,97,99,55,97,53,99,48,98,50,56,54,54,51,51,48,56,52,100,102,56,48,57,55,52,101,99,99,57,98,51,54,97,97,101,51,57,99,97,51,98,97,53,54,57,50,49,102,56,49,53,34,44,34,108,105,99,101,110,115,111,114,34,58,32,34,65,102,102,101,99,116,105,118,97,32,73,110,99,46,34,44,34,101,120,112,105,114,101,115,34,58,32,34,50,48,57,57,45,48,49,45,48,49,34,44,34,100,101,118,101,108,111,112,101,114,73,100,34,58,32,34,65,102,102,101,99,116,105,118,97,45,105,110,116,101,114,110,97,108,34,44,34,115,111,102,116,119,97,114,101,34,58,32,34,65,102,102,100,101,120,32,83,68,75,34,125};

    //MetricsManager View UI Objects
    private RelativeLayout metricViewLayout;
    private LinearLayout leftMetricsLayout;
    private LinearLayout rightMetricsLayout;
    private MetricDisplay[] metricDisplays;
    private TextView[] metricNames;
    private TextView fpsName;
    private TextView fpsPct;
    private TextView pleaseWaitTextView;
    private ProgressBar progressBar;

    //Other UI objects
    private ViewGroup activityLayout; //top-most ViewGroup in which all content resides
    private RelativeLayout mainLayout; //layout, to be resized, containing all UI elements
    private RelativeLayout progressBarLayout; //layout used to show progress circle while camera is starting
    private SurfaceView cameraView; //SurfaceView used to display camera images
    private DrawingView drawingView; //SurfaceView containing its own thread, used to draw facial tracking dots
    private ImageButton settingsButton;
    private ImageButton cameraButton;

    //Application settings variables
    private int detectorProcessRate;
    private boolean isMenuVisible = false;
    private boolean isFPSVisible = false;
    private boolean isMenuShowingForFirstTime = true;

    //Frames Per Second (FPS) variables
    private long firstSystemTime = 0;
    private float numberOfFrames = 0;
    private long timeToUpdate = 0;

    //Camera-related variables
    private boolean isFrontFacingCameraDetected = true;
    private boolean isBackFacingCameraDetected = true;
    int cameraPreviewWidth = 0;
    int cameraPreviewHeight = 0;
    CameraDetector.CameraType cameraType;
    boolean mirrorPoints = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //To maximize UI space, we declare our app to be full-screen
        setContentView(R.layout.activity_main);

        initializeUI();

        determineCameraAvailability();

        initializeCameraDetector();
    }

    /**
     * We check to make sure the device has a front-facing camera.
     * If it does not, we obscure the app with a notice informing the user they cannot
     * use the app.
     */
    void determineCameraAvailability() {
        PackageManager manager = getPackageManager();
        isFrontFacingCameraDetected = manager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
        isBackFacingCameraDetected = manager.hasSystemFeature(PackageManager.FEATURE_CAMERA);

        if (!isFrontFacingCameraDetected && !isBackFacingCameraDetected) {
            progressBar.setVisibility(View.INVISIBLE);
            pleaseWaitTextView.setVisibility(View.INVISIBLE);
            TextView notFoundTextView = (TextView) findViewById(R.id.not_found_textview);
            notFoundTextView.setVisibility(View.VISIBLE);
        }

        //TODO: change this to be taken from settings
        if (isBackFacingCameraDetected) {
            cameraType = CameraDetector.CameraType.CAMERA_BACK;
            mirrorPoints = false;
        }
        if (isFrontFacingCameraDetected) {
            cameraType = CameraDetector.CameraType.CAMERA_FRONT;
            mirrorPoints = true;
        }
    }

    void initializeUI() {

        //Get handles to UI objects
        activityLayout = (ViewGroup) findViewById(android.R.id.content);
        progressBarLayout = (RelativeLayout) findViewById(R.id.progress_bar_cover);
        metricViewLayout = (RelativeLayout) findViewById(R.id.metric_view_group);
        leftMetricsLayout = (LinearLayout) findViewById(R.id.left_metrics);
        rightMetricsLayout = (LinearLayout) findViewById(R.id.right_metrics);
        mainLayout = (RelativeLayout) findViewById(R.id.main_layout);
        fpsPct = (TextView) findViewById(R.id.fps_value);
        fpsName = (TextView) findViewById(R.id.fps_name);
        cameraView = (SurfaceView) findViewById(R.id.camera_preview);
        drawingView = (DrawingView) findViewById(R.id.drawing_view);
        settingsButton = (ImageButton) findViewById(R.id.settings_button);
        cameraButton = (ImageButton) findViewById(R.id.camera_button);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        pleaseWaitTextView = (TextView) findViewById(R.id.please_wait_textview);

        //Initialize views to display metrics
        metricNames = new TextView[NUM_METRICS_DISPLAYED];
        metricNames[0] = (TextView) findViewById(R.id.metric_name_0);
        metricNames[1] = (TextView) findViewById(R.id.metric_name_1);
        metricNames[2] = (TextView) findViewById(R.id.metric_name_2);
        metricNames[3] = (TextView) findViewById(R.id.metric_name_3);
        metricNames[4] = (TextView) findViewById(R.id.metric_name_4);
        metricNames[5] = (TextView) findViewById(R.id.metric_name_5);
        metricDisplays = new MetricDisplay[NUM_METRICS_DISPLAYED];
        metricDisplays[0] = (MetricDisplay) findViewById(R.id.metric_pct_0);
        metricDisplays[1] = (MetricDisplay) findViewById(R.id.metric_pct_1);
        metricDisplays[2] = (MetricDisplay) findViewById(R.id.metric_pct_2);
        metricDisplays[3] = (MetricDisplay) findViewById(R.id.metric_pct_3);
        metricDisplays[4] = (MetricDisplay) findViewById(R.id.metric_pct_4);
        metricDisplays[5] = (MetricDisplay) findViewById(R.id.metric_pct_5);

        //Load Application Font and set UI Elements to use it
        Typeface face = Typeface.createFromAsset(getAssets(), "fonts/Square.ttf");
        for (TextView textView : metricNames) {
            textView.setTypeface(face);
        }
        for (MetricDisplay metricDisplay : metricDisplays) {
            metricDisplay.setTypeface(face);
        }
        fpsPct.setTypeface(face);
        fpsName.setTypeface(face);
        drawingView.setTypeface(face);
        pleaseWaitTextView.setTypeface(face);

        //Hide left and right metrics by default (will be made visible when face detection starts)
        leftMetricsLayout.setAlpha(0);
        rightMetricsLayout.setAlpha(0);

        /**
         * This app uses two SurfaceView objects: one to display the camera image and the other to draw facial tracking dots.
         * Since we want the tracking dots to appear over the camera image, we use SurfaceView.setZOrderMediaOverlay() to indicate that
         * cameraView represents our 'media', and drawingView represents our 'overlay', so that Android will render them in the
         * correct order.
         */
        drawingView.setZOrderMediaOverlay(true);
        cameraView.setZOrderMediaOverlay(false);

        //Attach event listeners to the menu and edit box
        activityLayout.setOnTouchListener(this);

        /*
         * This app sets the View.SYSTEM_UI_FLAG_HIDE_NAVIGATION flag. Unfortunately, this flag causes
         * Android to steal the first touch event after the navigation bar has been hidden, a touch event
         * which should be used to make our menu visible again. Therefore, we attach a listener to be notified
         * when the navigation bar has been made visible again, which corresponds to the touch event that Android
         * steals. If the menu bar was not visible, we make it visible.
         */
        activityLayout.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int uiCode) {
                if ((uiCode == 0) && (isMenuVisible == false)) {
                    setMenuVisible(true);
                }

            }
        });
    }

    void initializeCameraDetector() {
        /* Put the SDK in camera mode by using this constructor. The SDK will be in control of
         * the camera. If a SurfaceView is passed in as the last argument to the constructor,
         * that view will be painted with what the camera sees.
         */
        detector = new CameraDetector(this, cameraType, cameraView);
        //TODO: this method SHOULD NOT be included in sample code release (customer should enter their own license file).
        detector.setLicenseStream(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(licenseBytes))));
        // NOTE: uncomment the line below and replace "YourLicenseFile" with your license file, which should be stored in /assets/Affdex/
        //detector.setLicensePath("YourLicenseFile");
        detector.setImageListener(this);
        detector.setFaceListener(this);
        detector.setOnCameraEventListener(this);
    }

    /*
     * We use onResume() to restore application settings using the SharedPreferences object
     */
    @Override
    public void onResume() {
        super.onResume();
        restoreApplicationSettings();
        setMenuVisible(true);
        isMenuShowingForFirstTime = true;
        mainWindowResumedTasks();
    }

    /*
     * We use the Shared Preferences object to restore application settings.
     */
    public void restoreApplicationSettings() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //restore camera processing rate
        detectorProcessRate = PreferencesUtils.getFrameProcessingRate(sharedPreferences);
        detector.setMaxProcessRate(detectorProcessRate);

        if (sharedPreferences.getBoolean("fps",isFPSVisible)) {    //restore isFPSMetricVisible
            setFPSVisible(true);
        } else {
            setFPSVisible(false);
        }

        if (sharedPreferences.getBoolean("track",drawingView.getDrawPointsEnabled())) {  //restore isTrackingDotsVisible
            setTrackPoints(true);
        } else {
            setTrackPoints(false);
        }

        if (sharedPreferences.getBoolean("measurements",drawingView.getDrawMeasurementsEnabled())) { //restore show measurements
            setShowMeasurements(true);
        } else {
            setShowMeasurements(false);
        }

        //populate metric displays
        for (int n = 0; n < NUM_METRICS_DISPLAYED; n++) {
            activateMetric(n,PreferencesUtils.getMetricFromPrefs(sharedPreferences, n));
        }
    }

    /**
     * Populates a TextView to display a metric name and readies a MetricDisplay to display the value.
     * Uses reflection to:
     *  -enable the corresponding metric in the Detector object by calling Detector.setDetect<MetricName>()
     *  -save the Method object that will be invoked on the Face object received in onImageResults() to get the metric score
     */
    void activateMetric(int index, MetricsManager.Metrics metric) {
        metricNames[index].setText(MetricsManager.getUpperCaseName(metric));

        Method getFaceScoreMethod = null; //The method that will be used to get a metric score
        try {
            //Enable metric detection
            Detector.class.getMethod("setDetect" + MetricsManager.getCamelCase(metric), boolean.class).invoke(detector, true);

            if (metric.getType() == MetricsManager.MetricType.Emotion) {
                getFaceScoreMethod = Face.Emotions.class.getMethod("get" + MetricsManager.getCamelCase(metric), null);

                //The MetricDisplay for Valence is unique; it shades it color depending on the metric value
                if (metric == MetricsManager.Emotions.VALENCE) {
                    metricDisplays[index].setIsShadedMetricView(true);
                } else {
                    metricDisplays[index].setIsShadedMetricView(false);
                }
            } else if (metric.getType() == MetricsManager.MetricType.Expression) {
                getFaceScoreMethod = Face.Expressions.class.getMethod("get" + MetricsManager.getCamelCase(metric),null);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG,String.format("Error using reflection to generate methods for %s",metric.toString()));
        }

        metricDisplays[index].setMetricToDisplay(metric, getFaceScoreMethod);
    }

    /**
     * Reset the variables used to calculate processed frames per second.
     * **/
    public void resetFPSCalculations() {
        firstSystemTime = SystemClock.elapsedRealtime();
        timeToUpdate = firstSystemTime + 1000L;
        numberOfFrames = 0;
    }

    void mainWindowResumedTasks() {

        startDetector();

        if (!drawingView.isDimensionsNeeded()) {
            progressBarLayout.setVisibility(View.GONE);
        }
        resetFPSCalculations();

        cameraView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isMenuShowingForFirstTime) {
                    setMenuVisible(false);
                }
            }
        }, 5000);
    }

    void startDetector() {
        if (!isBackFacingCameraDetected && !isFrontFacingCameraDetected)
            return; //without any cameras detected, we cannot proceed

        detector.setDetectValence(true); //this app will always detect valence
        if (!detector.isRunning()) {
            try {
                detector.start();
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }
    }



    @Override
    public void onFaceDetectionStarted() {
        leftMetricsLayout.animate().alpha(1); //make left and right metrics appear
        rightMetricsLayout.animate().alpha(1);

        resetFPSCalculations(); //Since the FPS may be different whether a face is being tracked or not, reset variables.
    }

    @Override
    public void onFaceDetectionStopped() {
        performFaceDetectionStoppedTasks();
    }

    void performFaceDetectionStoppedTasks() {
        leftMetricsLayout.animate().alpha(0); //make left and right metrics disappear
        rightMetricsLayout.animate().alpha(0);
        resetFPSCalculations(); //Since the FPS may be different whether a face is being tracked or not, reset variables.
    }

    /**
     * This event is received every time the SDK processes a frame.
     */
    @Override
    public void onImageResults(List<Face> faces, Frame image, float timeStamp) {
        //If the faces object is null, we received an unprocessed frame
        if (faces == null) {
            return;
        }

        //At this point, we know the frame received was processed, so we perform our processed frames per second calculations
        performFPSCalculations();

        //If faces.size() is 0, we received a frame in which no face was detected
        if (faces.size() == 0) {
            drawingView.updatePoints(null, mirrorPoints); //the drawingView takes null points to mean it doesn't have to draw anything
            return;
        }

        //The SDK currently detects one face at a time, so we recover it using .get(0).
        //'0' indicates we are recovering the first face.
        Face face = faces.get(0);

        //update metrics with latest face information. The metrics are displayed on a MetricView, a custom view with a .setScore() method.
        for (MetricDisplay metricDisplay : metricDisplays) {
            updateMetricScore(metricDisplay,face);
        }

        /**
         * If the user has selected to have facial tracking dots or measurements drawn, we use face.getFacePoints() to send those points
         * to our drawing thread and also inform the thread what the valence score was, as that will determine the color
         * of the bounding box.
        */
        if (drawingView.getDrawPointsEnabled() || drawingView.getDrawMeasurementsEnabled()) {
            drawingView.setMetrics(face.measurements.orientation.getRoll(), face.measurements.orientation.getYaw(), face.measurements.orientation.getPitch(), face.measurements.getInterocularDistance(), face.emotions.getValence());
            drawingView.updatePoints(face.getFacePoints(),mirrorPoints);
        }
    }


    /**
     * Use the method that we saved in activateMetric() to get the metric score and display it
     */
    void updateMetricScore(MetricDisplay metricDisplay, Face face) {

        MetricsManager.Metrics metric = metricDisplay.getMetricToDisplay();
        float score = Float.NaN;

        try {
            if (metric.getType() == MetricsManager.MetricType.Emotion) {
                score = (Float) metricDisplay.getFaceScoreMethod().invoke(face.emotions,null);
                metricDisplay.setScore(score);
            } else if (metric.getType() == MetricsManager.MetricType.Expression) {
                score = (Float) metricDisplay.getFaceScoreMethod().invoke(face.expressions,null);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG,String.format("Error using reflecting to get %s score from face.",metric.toString()));
        }
        metricDisplay.setScore(score);
    }

    /**
     * FPS measurement simply uses SystemClock to measure how many frames were processed since
     * the FPS variables were last reset.
     * The constants 1000L and 1000f appear because .elapsedRealtime() measures time in milliseconds.
     * Note that if 20 frames per second are processed, this method could run for 1.5 years without being reset
     * before numberOfFrames overflows.
     */
    void performFPSCalculations() {
        numberOfFrames += 1;
        long currentTime = SystemClock.elapsedRealtime();
        if (currentTime > timeToUpdate) {
            float framesPerSecond = (numberOfFrames/(float)(currentTime - firstSystemTime))*1000f;
            fpsPct.setText(String.format(" %.1f",framesPerSecond));
            timeToUpdate = currentTime + 1000L;
        }
    }

    /**
     * Although we start the camera in onWindowFocusChanged(), we stop it in onPause(), and set detector to be null so that when onWindowFocusChanged()
     * is called it restarts the camera. We also set the Progress Bar to be visible, so the camera (which may need resizing when the app
     * is resumed) is obscured.
     */
    @Override
    public void onPause() {
        super.onPause();
        progressBarLayout.setVisibility(View.VISIBLE);

        performFaceDetectionStoppedTasks();

        stopDetector();
    }

    void stopDetector() {
        if (detector.isRunning()) {
            try {
                detector.stop();
            } catch (Exception e) {
                Log.e(LOG_TAG,e.getMessage());
            }
        }

        detector.setDetectAllEmotions(false);
        detector.setDetectAllExpressions(false);
    }



    /**
     * When the user taps the screen, hide the menu if it is visible and show it if it is hidden.
     * **/
    void setMenuVisible(boolean b){
        isMenuShowingForFirstTime = false;
        isMenuVisible = b;
        if (b) {
            settingsButton.setVisibility(View.VISIBLE);
            cameraButton.setVisibility(View.VISIBLE);

            //We display the navigation bar again
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
        else {

            //We hide the navigation bar
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);


            settingsButton.setVisibility(View.INVISIBLE);
            cameraButton.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * If a user has a phone with a physical menu button, they may expect it to toggle
     * the menu, so we add that functionality.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            setMenuVisible(!isMenuVisible);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //If the user selects to have facial tracking dots drawn, inform our drawing thread.
    void setTrackPoints(boolean b) {
        drawingView.setDrawPointsEnabled(b);
    }

    void setShowMeasurements(boolean b) {
        drawingView.setDrawMeasurementsEnabled(b);
    }

    void setFPSVisible(boolean b) {
        isFPSVisible = b;
        if (b) {
            fpsName.setVisibility(View.VISIBLE);
            fpsPct.setVisibility(View.VISIBLE);
        } else {
            fpsName.setVisibility(View.INVISIBLE);
            fpsPct.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            setMenuVisible(!isMenuVisible);
        }
        return false;
    }

    public void settings_button_click(View view) {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    public void onCameraStarted(boolean b, Throwable throwable) {
        if (throwable != null) {
            Toast.makeText(this,"Failed to start camera.",Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCameraSizeSelected(int cameraWidth, int cameraHeight, ROTATE rotation) {
        if (rotation == ROTATE.BY_90_CCW || rotation == ROTATE.BY_90_CW) {
            cameraPreviewWidth = cameraHeight;
            cameraPreviewHeight = cameraWidth;
        } else {
            cameraPreviewWidth = cameraWidth;
            cameraPreviewHeight = cameraHeight;
        }
        drawingView.setThickness((int)(cameraWidth/100f));

        mainLayout.post(new Runnable() {
            @Override
            public void run() {
                int layoutWidth = mainLayout.getWidth();
                int layoutHeight = mainLayout.getHeight();

                if (cameraPreviewWidth == 0 || cameraPreviewHeight == 0 || layoutWidth == 0 || layoutHeight == 0)
                    return;

                float layoutAspectRatio = (float)layoutWidth/layoutHeight;
                float cameraPreviewAspectRatio = (float)cameraPreviewWidth/cameraPreviewHeight;

                int newWidth;
                int newHeight;

                if (cameraPreviewAspectRatio > layoutAspectRatio) {
                    newWidth = layoutWidth;
                    newHeight =(int) (layoutWidth / cameraPreviewAspectRatio);
                } else {
                    newWidth = (int) (layoutHeight * cameraPreviewAspectRatio);
                    newHeight = layoutHeight;
                }

                drawingView.updateViewDimensions(newWidth,newHeight,cameraPreviewWidth,cameraPreviewHeight);

                ViewGroup.LayoutParams params = mainLayout.getLayoutParams();
                params.height = newHeight;
                params.width = newWidth;
                mainLayout.setLayoutParams(params);

                //Now that our main layout has been resized, we can remove the progress bar that was obscuring the screen (its purpose was to obscure the resizing of the SurfaceView)
                progressBarLayout.setVisibility(View.GONE);
            }
        });

    }


    public void camera_button_click(View view) {
        if (cameraType == CameraDetector.CameraType.CAMERA_FRONT) {
            if (isBackFacingCameraDetected) {
                cameraType = CameraDetector.CameraType.CAMERA_BACK;
                mirrorPoints = false;
            } else {
                Toast.makeText(this,"No back-facing camera found",Toast.LENGTH_LONG).show();
            }
        } else if (cameraType == CameraDetector.CameraType.CAMERA_BACK) {
            if (isFrontFacingCameraDetected) {
                cameraType = CameraDetector.CameraType.CAMERA_FRONT;
                mirrorPoints = true;
            } else {
                Toast.makeText(this,"No front-facing camera found",Toast.LENGTH_LONG).show();
            }
        }

        performFaceDetectionStoppedTasks();

        try {
            detector.setCameraType(cameraType);
        } catch (Exception e) {
            Log.e(LOG_TAG,e.getMessage());
        }
    }
}


