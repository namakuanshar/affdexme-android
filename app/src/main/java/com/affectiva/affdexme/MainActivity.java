package com.affectiva.affdexme;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.Frame.ROTATE;
import com.affectiva.android.affdex.sdk.detector.CameraDetector;
import com.affectiva.android.affdex.sdk.detector.Detector;
import com.affectiva.android.affdex.sdk.detector.Face;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
 * - Copy the contents of the SDK's libs folder into this project's libs folder under AffdexMe/app/lib
 * - Copy the armeabi-v7a folder (found in the SDK libs folder) into this project's jniLibs folder under AffdexMe/app/src/main/jniLibs
 * - Add your license file to the /assets/Affdex folder and rename to license.txt. 
 * (Note: if you name the license file something else you will need to update the licensePath in the initializeCameraDetector() method in MainActivity)
 * - Build the project
 * - Run the app on an Android device with a front-facing camera
 *
 * Copyright (c) 2014 Affectiva. All rights reserved.
 */

public class MainActivity extends AppCompatActivity
        implements Detector.FaceListener, Detector.ImageListener, CameraDetector.CameraEventListener,
        View.OnTouchListener, ActivityCompat.OnRequestPermissionsResultCallback {

    public static final int MAX_SUPPORTED_FACES = 4;
    public static final int NUM_METRICS_DISPLAYED = 6;
    private static final String LOG_TAG = "AffdexMe";
    private static final int AFFDEXME_PERMISSIONS_REQUEST = 42;  //value is arbitrary (between 0 and 255)
    int cameraPreviewWidth = 0;
    int cameraPreviewHeight = 0;
    CameraDetector.CameraType cameraType;
    boolean mirrorPoints = false;
    private boolean cameraPermissionsAvailable = false;
    private CameraDetector detector = null;
    private RelativeLayout metricViewLayout;
    private LinearLayout leftMetricsLayout;
    private LinearLayout rightMetricsLayout;
    private MetricDisplay[] metricDisplays;
    private TextView[] metricNames;
    private TextView fpsName;
    private TextView fpsPct;
    private TextView pleaseWaitTextView;
    private ProgressBar progressBar;
    private RelativeLayout mainLayout; //layout, to be resized, containing all UI elements
    private RelativeLayout progressBarLayout; //layout used to show progress circle while camera is starting
    private LinearLayout permissionsUnavailableLayout; //layout used to notify the user that not enough permissions have been granted to use the app
    private SurfaceView cameraView; //SurfaceView used to display camera images
    private DrawingView drawingView; //SurfaceView containing its own thread, used to draw facial tracking dots
    private ImageButton settingsButton;
    private ImageButton cameraButton;
    private boolean isMenuVisible = false;
    private boolean isFPSVisible = false;
    private boolean isMenuShowingForFirstTime = true;
    private long firstSystemTime = 0;
    private float numberOfFrames = 0;
    private long timeToUpdate = 0;
    private boolean isFrontFacingCameraDetected = true;
    private boolean isBackFacingCameraDetected = true;
    private boolean multiFaceModeEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //To maximize UI space, we declare our app to be full-screen
        preproccessMetricImages();
        setContentView(R.layout.activity_main);
        initializeUI();
        checkForDangerousPermissions();
        determineCameraAvailability();
        initializeCameraDetector();
    }

    /**
     * Only load the files onto disk the first time the app opens
     */
    private void preproccessMetricImages() {
        Context context = getBaseContext();

        for (Face.EMOJI emoji : Face.EMOJI.values()) {
            if (emoji.equals(Face.EMOJI.UNKNOWN)) {
                continue;
            }
            String emojiResourceName = emoji.name().trim().replace(' ', '_').toLowerCase(Locale.US).concat("_emoji");
            String emojiFileName = emojiResourceName + ".png";
            ImageHelper.preproccessImageIfNecessary(context, emojiFileName, emojiResourceName);
        }

        ImageHelper.preproccessImageIfNecessary(context, "female_glasses.png", "female_glasses");
        ImageHelper.preproccessImageIfNecessary(context, "female_noglasses.png", "female_noglasses");
        ImageHelper.preproccessImageIfNecessary(context, "male_glasses.png", "male_glasses");
        ImageHelper.preproccessImageIfNecessary(context, "male_noglasses.png", "male_noglasses");
        ImageHelper.preproccessImageIfNecessary(context, "unknown_glasses.png", "unknown_glasses");
        ImageHelper.preproccessImageIfNecessary(context, "unknown_noglasses.png", "unknown_noglasses");
    }


    private void checkForDangerousPermissions() {
        cameraPermissionsAvailable =
                ContextCompat.checkSelfPermission(
                        getApplicationContext(),
                        Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        if (!cameraPermissionsAvailable) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                showPermissionExplanationDialog();
            } else {
                // No explanation needed, we can request the permission.
                requestNeededPermissions();
            }
        }
    }

    private void requestNeededPermissions() {
        List<String> neededPermissions = new ArrayList<>();

        if (!cameraPermissionsAvailable) {
            neededPermissions.add(Manifest.permission.CAMERA);
        }

        ActivityCompat.requestPermissions(
                this,
                neededPermissions.toArray(new String[neededPermissions.size()]),
                AFFDEXME_PERMISSIONS_REQUEST);

        // AFFDEXME_PERMISSIONS_REQUEST is an app-defined int constant that must be between 0 and 255.
        // The callback method gets the result of the request.
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == AFFDEXME_PERMISSIONS_REQUEST) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.CAMERA)) {
                    cameraPermissionsAvailable = (grantResult == PackageManager.PERMISSION_GRANTED);
                }
            }
        }

        if (!cameraPermissionsAvailable) {
            permissionsUnavailableLayout.setVisibility(View.VISIBLE);
        } else {
            permissionsUnavailableLayout.setVisibility(View.GONE);
        }
    }

    private void showPermissionExplanationDialog() {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                getApplicationContext());

        // set title
        alertDialogBuilder.setTitle(getResources().getString(R.string.insufficient_permissions));

        // set dialog message
        alertDialogBuilder
                .setMessage(getResources().getString(R.string.permissions_needed_explanation))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.understood), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        requestNeededPermissions();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
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

        //set default camera settings
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //restore the camera type settings
        String cameraTypeName = sharedPreferences.getString("cameraType", CameraDetector.CameraType.CAMERA_FRONT.name());
        if (cameraTypeName.equals(CameraDetector.CameraType.CAMERA_FRONT.name())) {
            cameraType = CameraDetector.CameraType.CAMERA_FRONT;
            mirrorPoints = true;
        } else {
            cameraType = CameraDetector.CameraType.CAMERA_BACK;
            mirrorPoints = false;
        }
    }

    void initializeUI() {

        //Get handles to UI objects
        ViewGroup activityLayout = (ViewGroup) findViewById(android.R.id.content);
        progressBarLayout = (RelativeLayout) findViewById(R.id.progress_bar_cover);
        permissionsUnavailableLayout = (LinearLayout) findViewById(R.id.permissionsUnavialableLayout);
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
        Button retryPermissionsButton = (Button) findViewById(R.id.retryPermissionsButton);

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
                if ((uiCode == 0) && (!isMenuVisible)) {
                    setMenuVisible(true);
                }

            }
        });

        retryPermissionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestNeededPermissions();
            }
        });
    }

    void initializeCameraDetector() {
        /* Put the SDK in camera mode by using this constructor. The SDK will be in control of
         * the camera. If a SurfaceView is passed in as the last argument to the constructor,
         * that view will be painted with what the camera sees.
         */
        detector = new CameraDetector(this, cameraType, cameraView, (multiFaceModeEnabled ? MAX_SUPPORTED_FACES : 1), Detector.FaceDetectorMode.LARGE_FACES);

        // update the license path here if you name your file something else
        detector.setLicensePath("license.txt");
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
        checkForDangerousPermissions();
        restoreApplicationSettings();
        setMenuVisible(true);
        isMenuShowingForFirstTime = true;
    }

    private void setMultiFaceModeEnabled(boolean isEnabled) {

        //if setting change is necessary
        if (isEnabled != multiFaceModeEnabled) {
            // change the setting, stop the detector, and reinitialize it to change the setting
            multiFaceModeEnabled = isEnabled;
            stopDetector();
            initializeCameraDetector();
        }
    }

    /*
     * We use the Shared Preferences object to restore application settings.
     */
    public void restoreApplicationSettings() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //restore the camera type settings
        String cameraTypeName = sharedPreferences.getString("cameraType", CameraDetector.CameraType.CAMERA_FRONT.name());
        if (cameraTypeName.equals(CameraDetector.CameraType.CAMERA_FRONT.name())) {
            setCameraType(CameraDetector.CameraType.CAMERA_FRONT);
        } else {
            setCameraType(CameraDetector.CameraType.CAMERA_BACK);
        }

        //restore the multiface mode setting to reset the detector if necessary
        if (sharedPreferences.getBoolean("multiface", false)) { // default to false
            setMultiFaceModeEnabled(true);
        } else {
            setMultiFaceModeEnabled(false);
        }

        //restore camera processing rate
        int detectorProcessRate = PreferencesUtils.getFrameProcessingRate(sharedPreferences);
        detector.setMaxProcessRate(detectorProcessRate);
        drawingView.invalidateDimensions();

        if (sharedPreferences.getBoolean("fps", isFPSVisible)) {    //restore isFPSMetricVisible
            setFPSVisible(true);
        } else {
            setFPSVisible(false);
        }

        if (sharedPreferences.getBoolean("track", drawingView.getDrawPointsEnabled())) {  //restore isTrackingDotsVisible
            setTrackPoints(true);
        } else {
            setTrackPoints(false);
        }

        if (sharedPreferences.getBoolean("appearance", drawingView.getDrawAppearanceMarkersEnabled())) {
            detector.setDetectAllAppearance(true);
            setShowAppearance(true);
        } else {
            detector.setDetectAllAppearance(false);
            setShowAppearance(false);
        }

        if (sharedPreferences.getBoolean("emoji", drawingView.getDrawEmojiMarkersEnabled())) {
            detector.setDetectAllEmojis(true);
            setShowEmoji(true);
        } else {
            detector.setDetectAllEmojis(false);
            setShowEmoji(false);
        }

        //populate metric displays
        for (int n = 0; n < NUM_METRICS_DISPLAYED; n++) {
            activateMetric(n, PreferencesUtils.getMetricFromPrefs(sharedPreferences, n));
        }

        //if we are in multiface mode, we need to enable the detection of all emotions
        if (multiFaceModeEnabled) {
            detector.setDetectAllEmotions(true);
        }
    }

    /**
     * Populates a TextView to display a metric name and readies a MetricDisplay to display the value.
     * Uses reflection to:
     * -enable the corresponding metric in the Detector object by calling Detector.setDetect<MetricName>()
     * -save the Method object that will be invoked on the Face object received in onImageResults() to get the metric score
     */
    void activateMetric(int index, MetricsManager.Metrics metric) {

        Method getFaceScoreMethod = null; //The method that will be used to get a metric score

        try {
            switch (metric.getType()) {
                case Emotion:
                    Detector.class.getMethod("setDetect" + MetricsManager.getCamelCase(metric), boolean.class).invoke(detector, true);
                    metricNames[index].setText(MetricsManager.getUpperCaseName(metric));
                    getFaceScoreMethod = Face.Emotions.class.getMethod("get" + MetricsManager.getCamelCase(metric));

                    //The MetricDisplay for Valence is unique; it shades it color depending on the metric value
                    if (metric == MetricsManager.Emotions.VALENCE) {
                        metricDisplays[index].setIsShadedMetricView(true);
                    } else {
                        metricDisplays[index].setIsShadedMetricView(false);
                    }
                    break;
                case Expression:
                    Detector.class.getMethod("setDetect" + MetricsManager.getCamelCase(metric), boolean.class).invoke(detector, true);
                    metricNames[index].setText(MetricsManager.getUpperCaseName(metric));
                    getFaceScoreMethod = Face.Expressions.class.getMethod("get" + MetricsManager.getCamelCase(metric));
                    break;
                case Emoji:
                    detector.setDetectAllEmojis(true);
                    MetricsManager.Emojis emoji = ((MetricsManager.Emojis) metric);
                    String metricTitle = emoji.getDisplayName(); // + " " + emoji.getUnicodeForEmoji();
                    metricNames[index].setText(metricTitle);
                    Log.d(LOG_TAG, "Getter Method: " + "get" + MetricsManager.getCamelCase(metric));
                    getFaceScoreMethod = Face.Emojis.class.getMethod("get" + MetricsManager.getCamelCase(metric));
                    break;
            }
        } catch (NoSuchMethodException e) {
            Log.e(LOG_TAG, String.format("No such method while using reflection to generate methods for %s", metric.toString()), e);
        } catch (InvocationTargetException e) {
            Log.e(LOG_TAG, String.format("Invocation error while using reflection to generate methods for %s", metric.toString()), e);
        } catch (IllegalAccessException e) {
            Log.e(LOG_TAG, String.format("Illegal access error while using reflection to generate methods for %s", metric.toString()), e);
        }

        metricDisplays[index].setMetricToDisplay(metric, getFaceScoreMethod);
    }

    /**
     * Reset the variables used to calculate processed frames per second.
     **/
    public void resetFPSCalculations() {
        firstSystemTime = SystemClock.elapsedRealtime();
        timeToUpdate = firstSystemTime + 1000L;
        numberOfFrames = 0;
    }

    /**
     * We want to start the camera as late as possible, so it does not freeze the application before it has been visually resumed.
     * We thus post a runnable that will take care of starting the camera.
     * We also reset variables used to calculate the Processed Frames Per Second.
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus && isFrontFacingCameraDetected) {
            cameraView.post(new Runnable() {
                @Override
                public void run() {
                    mainWindowResumedTasks();
                }
            });
        }
    }

    void mainWindowResumedTasks() {

        //Notify the user that they can't use the app without authorizing these permissions.
        if (!cameraPermissionsAvailable) {
            permissionsUnavailableLayout.setVisibility(View.VISIBLE);
            return;
        }

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
        if (faces.size() <= 0) {
            drawingView.invalidatePoints();
        } else if (faces.size() == 1) {
            metricViewLayout.setVisibility(View.VISIBLE);

            //update metrics with latest face information. The metrics are displayed on a MetricView, a custom view with a .setScore() method.
            for (MetricDisplay metricDisplay : metricDisplays) {
                updateMetricScore(metricDisplay, faces.get(0));
            }

            /**
             * If the user has selected to have any facial attributes drawn, we use face.getFacePoints() to send those points
             * to our drawing thread and also inform the thread what the valence score was, as that will determine the color
             * of the bounding box.
             */
            if (drawingView.getDrawPointsEnabled() || drawingView.getDrawAppearanceMarkersEnabled() || drawingView.getDrawEmojiMarkersEnabled()) {
                drawingView.updatePoints(faces, mirrorPoints);
            }

        } else {
            // metrics overlay is hidden in multi face mode
            metricViewLayout.setVisibility(View.GONE);

            // always update points in multi face mode
            drawingView.updatePoints(faces, mirrorPoints);
        }
    }

    /**
     * Use the method that we saved in activateMetric() to get the metric score and display it
     */
    void updateMetricScore(MetricDisplay metricDisplay, Face face) {

        MetricsManager.Metrics metric = metricDisplay.getMetricToDisplay();
        float score = Float.NaN;

        try {
            switch (metric.getType()) {
                case Emotion:
                    score = (Float) metricDisplay.getFaceScoreMethod().invoke(face.emotions);
                    break;
                case Expression:
                    score = (Float) metricDisplay.getFaceScoreMethod().invoke(face.expressions);
                    break;
                case Emoji:
                    score = (Float) metricDisplay.getFaceScoreMethod().invoke(face.emojis);
                    break;
                default:
                    throw new Exception("Unknown Metric Type: " + metric.getType().toString());
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, String.format("Error using reflecting to get %s score from face.", metric.toString()));
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
            float framesPerSecond = (numberOfFrames / (float) (currentTime - firstSystemTime)) * 1000f;
            fpsPct.setText(String.format(" %.1f", framesPerSecond));
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
                Log.e(LOG_TAG, e.getMessage());
            }
        }

        detector.setDetectAllEmotions(false);
        detector.setDetectAllExpressions(false);
        detector.setDetectAllAppearance(false);
        detector.setDetectAllEmojis(false);
    }


    /**
     * When the user taps the screen, hide the menu if it is visible and show it if it is hidden.
     **/
    void setMenuVisible(boolean b) {
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
        } else {

            //We hide the navigation bar
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
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

    void setShowAppearance(boolean b) {
        drawingView.setDrawAppearanceMarkersEnabled(b);
    }

    void setShowEmoji(boolean b) {
        drawingView.setDrawEmojiMarkersEnabled(b);
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

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void onCameraSizeSelected(int cameraWidth, int cameraHeight, ROTATE rotation) {
        if (rotation == ROTATE.BY_90_CCW || rotation == ROTATE.BY_90_CW) {
            cameraPreviewWidth = cameraHeight;
            cameraPreviewHeight = cameraWidth;
        } else {
            cameraPreviewWidth = cameraWidth;
            cameraPreviewHeight = cameraHeight;
        }
        drawingView.setThickness((int) (cameraPreviewWidth / 100f));

        mainLayout.post(new Runnable() {
            @Override
            public void run() {
                //Get the screen width and height, and calculate the new app width/height based on the surfaceview aspect ratio.
                DisplayMetrics displaymetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
                int layoutWidth = displaymetrics.widthPixels;
                int layoutHeight = displaymetrics.heightPixels;

                if (cameraPreviewWidth == 0 || cameraPreviewHeight == 0 || layoutWidth == 0 || layoutHeight == 0)
                    return;

                float layoutAspectRatio = (float) layoutWidth / layoutHeight;
                float cameraPreviewAspectRatio = (float) cameraPreviewWidth / cameraPreviewHeight;

                int newWidth;
                int newHeight;

                if (cameraPreviewAspectRatio > layoutAspectRatio) {
                    newWidth = layoutWidth;
                    newHeight = (int) (layoutWidth / cameraPreviewAspectRatio);
                } else {
                    newWidth = (int) (layoutHeight * cameraPreviewAspectRatio);
                    newHeight = layoutHeight;
                }

                drawingView.updateViewDimensions(newWidth, newHeight, cameraPreviewWidth, cameraPreviewHeight);

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
        //Toggle the camera setting
        setCameraType(cameraType == CameraDetector.CameraType.CAMERA_FRONT ? CameraDetector.CameraType.CAMERA_BACK : CameraDetector.CameraType.CAMERA_FRONT);
    }

    private void setCameraType(CameraDetector.CameraType type) {
        SharedPreferences.Editor preferencesEditor = PreferenceManager.getDefaultSharedPreferences(this).edit();

        //If a settings change is necessary
        if (cameraType != type) {
            switch (type) {
                case CAMERA_BACK:
                    if (isBackFacingCameraDetected) {
                        cameraType = CameraDetector.CameraType.CAMERA_BACK;
                        mirrorPoints = false;
                    } else {
                        Toast.makeText(this, "No back-facing camera found", Toast.LENGTH_LONG).show();
                        return;
                    }
                    break;
                case CAMERA_FRONT:
                    if (isFrontFacingCameraDetected) {
                        cameraType = CameraDetector.CameraType.CAMERA_FRONT;
                        mirrorPoints = true;
                    } else {
                        Toast.makeText(this, "No front-facing camera found", Toast.LENGTH_LONG).show();
                        return;
                    }
                    break;
                default:
                    Log.e(LOG_TAG, "Unknown camera type selected");
            }

            performFaceDetectionStoppedTasks();

            detector.setCameraType(cameraType);
            preferencesEditor.putString("cameraType", cameraType.name());
            preferencesEditor.apply();
        }
    }
}


