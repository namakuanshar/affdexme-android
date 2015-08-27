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
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
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
        implements Detector.FaceListener, Detector.ImageListener, View.OnTouchListener, CameraDetector.CameraSurfaceViewListener {

    private static final String LOG_TAG = "Affectiva";
    //Affectiva SDK Object
    private CameraDetector detector = null;
    //TODO: License file in byte form. Should NOT be included in released sample code.
    private byte[] licenseBytes = {123,34,116,111,107,101,110,34,58,32,34,102,98,51,51,101,102,57,98,102,98,49,57,53,100,97,99,55,97,53,99,48,98,50,56,54,54,51,51,48,56,52,100,102,56,48,57,55,52,101,99,99,57,98,51,54,97,97,101,51,57,99,97,51,98,97,53,54,57,50,49,102,56,49,53,34,44,34,108,105,99,101,110,115,111,114,34,58,32,34,65,102,102,101,99,116,105,118,97,32,73,110,99,46,34,44,34,101,120,112,105,114,101,115,34,58,32,34,50,48,57,57,45,48,49,45,48,49,34,44,34,100,101,118,101,108,111,112,101,114,73,100,34,58,32,34,65,102,102,101,99,116,105,118,97,45,105,110,116,101,114,110,97,108,34,44,34,115,111,102,116,119,97,114,101,34,58,32,34,65,102,102,100,101,120,32,83,68,75,34,125};

    //Metrics View UI Objects
    private RelativeLayout metricViewLayout;
    private LinearLayout leftMetricsLayout;
    private LinearLayout rightMetricsLayout;
    private MetricView metricPct1;
    private MetricView metricPct2;
    private MetricView metricPct3;
    private MetricView metricPct4;
    private MetricView metricPct5;
    private MetricView metricPct6;
    private TextView fpsName;
    private TextView fpsPct;
    private TextView metricName1;
    private TextView metricName2;
    private TextView metricName3;
    private TextView metricName4;
    private TextView metricName5;
    private TextView metricName6;

    //Other UI objects
    private ViewGroup activityLayout; //top-most ViewGroup in which all content resides
    private RelativeLayout mainLayout; //layout, to be resized, containing all UI elements
    private RelativeLayout progressBarLayout; //layout used to show progress circle while camera is starting
    private SurfaceView cameraView; //SurfaceView used to display camera images
    private DrawingView drawingView; //SurfaceView containing its own thread, used to draw facial tracking dots
    private ImageButton settingsButton;

    //The Shared Preferences object is used to restore/save settings when activity is created/destroyed
    private final String PREFS_NAME = "AffdexMe";
    SharedPreferences sharedPreferences;

    //Application settings variables
    private int detectorProcessRate = 20;
    private boolean isMenuVisible = false;
    private boolean isFPSVisible = false;

    //Frames Per Second (FPS) variables
    private long firstSystemTime = 0;
    private float numberOfFrames = 0;
    private long timeToUpdate = 0;

    private boolean isFrontFacingCameraDetected = true;


    final static int NUM_METRICS_TO_SHOW = 6;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //To maximize UI space, we declare our app to be full-screen
        setContentView(R.layout.activity_main);

        /**
         * We check to make sure the device has a front-facing camera.
         * If it does not, we obscure the app with a notice informing the user they cannot
         * use the app.
         */
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
            isFrontFacingCameraDetected = false;
            TextView notFoundTextView = (TextView) findViewById(R.id.not_found_textview);
            notFoundTextView.setVisibility(View.VISIBLE);
        }

        initializeUI();

        initializeCameraDetector();
    }

    void initializeUI() {
        //TODO: what to do about valence?

        //Get handles to UI objects
        activityLayout = (ViewGroup) findViewById(android.R.id.content);
        progressBarLayout = (RelativeLayout) findViewById(R.id.progress_bar_cover);
        metricViewLayout = (RelativeLayout) findViewById(R.id.metric_view_group);
        leftMetricsLayout = (LinearLayout) findViewById(R.id.left_metrics);
        rightMetricsLayout = (LinearLayout) findViewById(R.id.right_metrics);
        mainLayout = (RelativeLayout) findViewById(R.id.main_layout);
        metricPct1 = (MetricView) findViewById(R.id.metric_pct_1);
        metricPct2 = (MetricView) findViewById(R.id.metric_pct_2);
        metricPct3 = (MetricView) findViewById(R.id.metric_pct_3);
        metricPct4 = (MetricView) findViewById(R.id.metric_pct_4);
        metricPct5 = (MetricView) findViewById(R.id.metric_pct_5);
        metricPct6 = (MetricView) findViewById(R.id.metric_pct_6);
        fpsPct = (TextView) findViewById(R.id.fps_value);
        metricName1 = (TextView) findViewById(R.id.metric_name_1);
        metricName2 = (TextView) findViewById(R.id.metric_name_2);
        metricName3 = (TextView) findViewById(R.id.metric_name_3);
        metricName4 = (TextView) findViewById(R.id.metric_name_4);
        metricName5 = (TextView) findViewById(R.id.metric_name_5);
        metricName6 = (TextView) findViewById(R.id.metric_name_6);
        fpsName = (TextView) findViewById(R.id.fps_name);
        cameraView = (SurfaceView) findViewById(R.id.camera_preview);
        drawingView = (DrawingView) findViewById(R.id.drawing_view);
        settingsButton = (ImageButton) findViewById(R.id.settings_button);

        //Load Application Font and set UI Elements to use it
        Typeface face = Typeface.createFromAsset(getAssets(), "fonts/Square.ttf");
        metricPct1.setTypeface(face);
        metricPct2.setTypeface(face);
        metricPct3.setTypeface(face);
        metricPct4.setTypeface(face);
        metricPct5.setTypeface(face);
        metricPct6.setTypeface(face);
        metricName1.setTypeface(face);
        metricName2.setTypeface(face);
        metricName3.setTypeface(face);
        metricName4.setTypeface(face);
        metricName5.setTypeface(face);
        metricName6.setTypeface(face);
        fpsPct.setTypeface(face);
        fpsName.setTypeface(face);
        drawingView.setTypeface(face);

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
        detector = new CameraDetector(this, CameraDetector.CameraType.CAMERA_FRONT, cameraView);
        //TODO: this method SHOULD NOT be included in sample code release (customer should enter their own license file).
        detector.setLicenseStream(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(licenseBytes))));

        // NOTE: uncomment the line below and replace "YourLicenseFile" with your license file, which should be stored in /assets/Affdex/
        //detector.setLicensePath("YourLicenseFile");

        detector.setMaxProcessRate(detectorProcessRate);

        //this app will always detect gender
        detector.setDetectGender(true);

        detector.setImageListener(this);
        detector.setFaceListener(this);
        detector.setCameraDetectorDimensionsListener(this);
    }

    /*
     * We use onResume() to restore application settings using the SharedPreferences object
     */
    @Override
    public void onResume() {
        super.onResume();
        restoreApplicationSettings();
        setMenuVisible(false); //always make the menu invisible by default

    }

    /*
     * We use the Shared Preferences object to restore application settings.
     */
    public void restoreApplicationSettings() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);


        String processRateString = sharedPreferences.getString("rate", "20");   //restore camera processing rate
        detectorProcessRate = parseDetectorRateString(processRateString);
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

        //restore metric names
        int metricCode = sharedPreferences.getInt("metric_1",0);
        activateMetricAndMetricName(metricName1, metricPct1, metricCode);

        metricCode = sharedPreferences.getInt("metric_2",1);
        activateMetricAndMetricName(metricName2, metricPct2, metricCode);

        metricCode = sharedPreferences.getInt("metric_3",2);
        activateMetricAndMetricName(metricName3, metricPct3, metricCode);

        metricCode = sharedPreferences.getInt("metric_4",3);
        activateMetricAndMetricName(metricName4, metricPct4, metricCode);

        metricCode = sharedPreferences.getInt("metric_5", 4);
        activateMetricAndMetricName(metricName5, metricPct5, metricCode);

        metricCode = sharedPreferences.getInt("metric_6",5);
        activateMetricAndMetricName(metricName6, metricPct6, metricCode);

        //TODO: remove this
        detector.setDetectValence(true);

    }

    int parseDetectorRateString(String rateString) {
        int toReturn;
        try {
            toReturn = Integer.parseInt(rateString);

        } catch (Exception e) {
            return 20;
        }
        if (toReturn > 0) {
            return toReturn;
        } else return 20;
    }

    void activateMetricAndMetricName(TextView metricName, MetricView metricView, int metricCode) {

        metricView.setMetricToDisplay(metricCode);
        metricName.setText(MetricsManager.getMetricName(metricCode));

        if (metricCode == MetricsManager.VALENCE) {
            metricView.setIsShadedMetricView(true);
        } else {
            metricView.setIsShadedMetricView(false);
        }
        
        switch (metricCode) {
            case MetricsManager.ANGER:
                detector.setDetectAnger(true);
                break;
            case MetricsManager.CONTEMPT:
                detector.setDetectContempt(true);
                break;
            case MetricsManager.DISGUST:
                detector.setDetectDisgust(true);
                break;
            case MetricsManager.FEAR:
                detector.setDetectFear(true);
                break;
            case MetricsManager.JOY:
                detector.setDetectJoy(true);
                break;
            case MetricsManager.SADNESS:
                detector.setDetectSadness(true);
                break;
            case MetricsManager.SURPRISE:
                detector.setDetectSurprise(true);
                break;
            case MetricsManager.ATTENTION:
                detector.setDetectAttention(true);
                break;
            case MetricsManager.BROW_FURROW:
                detector.setDetectBrowFurrow(true);
                break;
            case MetricsManager.BROW_RAISE:
                detector.setDetectBrowRaise(true);
                break;
            case MetricsManager.CHIN_RAISER:
                detector.setDetectChinRaiser(true);
                break;
            case MetricsManager.ENGAGEMENT:
                detector.setDetectEngagement(true);
                break;
            case MetricsManager.EYE_CLOSURE:
                detector.setDetectEyeClosure(true);
                break;
            case MetricsManager.INNER_BROW_RAISER:
                detector.setDetectInnerBrowRaiser(true);
                break;
            case MetricsManager.LIP_DEPRESSOR:
                detector.setDetectLipDepressor(true);
                break;
            case MetricsManager.LIP_PRESS:
                detector.setDetectLipPress(true);
                break;
            case MetricsManager.LIP_PUCKER:
                detector.setDetectLipPucker(true);
                break;
            case MetricsManager.LIP_SUCK:
                detector.setDetectLipSuck(true);
                break;
            case MetricsManager.MOUTH_OPEN:
                detector.setDetectMouthOpen(true);
                break;
            case MetricsManager.NOSE_WRINKLER:
                detector.setDetectNoseWrinkler(true);
                break;
            case MetricsManager.SMILE:
                detector.setDetectSmile(true);
                break;
            case MetricsManager.SMIRK:
                detector.setDetectSmirk(true);
                break;
            case MetricsManager.UPPER_LIP_RAISER:
                detector.setDetectUpperLipRaiser(true);
                break;
            case MetricsManager.VALENCE:
                detector.setDetectValence(true);
                break;
            default:
                break;


        }
    }

    /**
     * Reset the variables used to calculate processed frames per second.
     * **/
    public void resetFPSCalculations() {
        firstSystemTime = SystemClock.elapsedRealtime();
        timeToUpdate = firstSystemTime + 1000L;
        numberOfFrames = 0;
    }

    /**
     * We start the camera as soon as the application has been given focus, which occurs as soon as the application has
     * been opened or reopened. Although this can also occur when the application regains focus after a dialog box has been closed,
     * the startCamera() method will not start the camera if it is already running.
     * We also reset variables used to calculate the Processed Frames Per Second.
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {

        if (hasFocus && isFrontFacingCameraDetected) {

            startCamera();

            if (!drawingView.isSurfaceDimensionsNeeded()) {
                progressBarLayout.setVisibility(View.GONE);
            }
            resetFPSCalculations();
        }
    }

    void startCamera() {

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

        drawingView.invalidatePoints(); //inform the drawing thread that the latest facial tracking points are now invalid
        resetFPSCalculations(); //Since the FPS may be different whether a face is being tracked or not, reset variables.
    }

    /**
     * This event is received every time the SDK processes a frame.
     */
    @Override
    public void onImageResults(List<Face> faces, Frame image, float timeStamp) {
        /**
         * If the flag indicating that we still need to know the size of the camera frames, call calculateImageDimensions().
         * The flag is a boolean stored in our drawingView object, retrieved through DrawingView.isImageDimensionsNeeded().
         */
        if (drawingView.isImageDimensionsNeeded() ) {
            calculateImageDimensions(image);
        }

        //If the faces object is null, we received an unprocessed frame
        if (faces == null) {
            return;
        }

        //At this point, we know the frame received was processed, so we perform our processed frames per second calculations
        performFPSCalculations();

        //If faces.size() is 0, we received a frame in which no face was detected
        if (faces.size() == 0) {
            return;
        }

        //The SDK currently detects one face at a time, so we recover it using .get(0).
        //'0' indicates we are recovering the first face.
        Face face = faces.get(0);

        //update metrics with latest face information. The metrics are displayed on a MetricView, a custom view with a .setScore() method.
        updateMetricScore(metricPct1,face);
        updateMetricScore(metricPct2,face);
        updateMetricScore(metricPct3,face);
        updateMetricScore(metricPct4,face);
        updateMetricScore(metricPct5,face);
        updateMetricScore(metricPct6,face);

        /**
         * If the user has selected to have facial tracking dots drawn, we use face.getFacePoints() to send those points
         * to our drawing thread and also inform the thread what the valence score was, as that will determine the color
         * of the bounding box.
        */
        //TODO: what are you gonna do about valence?
        if (drawingView.getDrawPointsEnabled() || drawingView.getDrawMeasurementsEnabled()) {
            //TODO: get gender working
            drawingView.setMetrics(face.measurements.getRoll(), face.measurements.getYaw(), face.measurements.getPitch(), face.measurements.getInterOcularDistance(), face.emotions.getValence(), face.appearence.getGender());
            drawingView.updatePoints(face.getFacePoints());
        }
    }

    void updateMetricScore(MetricView metricView, Face face) {

        int metricCode = metricView.getMetricToDisplay();

        switch (metricCode) {
            case MetricsManager.ANGER:
                metricView.setScore(face.emotions.getAnger());
                break;
            case MetricsManager.CONTEMPT:
                metricView.setScore(face.emotions.getContempt());
                break;
            case MetricsManager.DISGUST:
                metricView.setScore(face.emotions.getDisgust());
                break;
            case MetricsManager.FEAR:
                metricView.setScore(face.emotions.getFear());
                break;
            case MetricsManager.JOY:
                metricView.setScore(face.emotions.getJoy());
                break;
            case MetricsManager.SADNESS:
                metricView.setScore(face.emotions.getSadness());
                break;
            case MetricsManager.SURPRISE:
                metricView.setScore(face.emotions.getSurprise());
                break;
            case MetricsManager.ATTENTION:
                metricView.setScore(face.expressions.getAttention());
                break;
            case MetricsManager.BROW_FURROW:
                metricView.setScore(face.expressions.getBrowFurrow());
                break;
            case MetricsManager.BROW_RAISE:
                metricView.setScore(face.expressions.getBrowRaise());
                break;
            case MetricsManager.CHIN_RAISER:
                metricView.setScore(face.expressions.getChinRaiser());
                break;
            case MetricsManager.ENGAGEMENT:
                metricView.setScore(face.emotions.getEngagement());
                break;
            case MetricsManager.EYE_CLOSURE:
                metricView.setScore(face.expressions.getEyeClosure());
                break;
            case MetricsManager.INNER_BROW_RAISER:
                metricView.setScore(face.expressions.getInnerBrowRaiser());
                break;
            case MetricsManager.LIP_DEPRESSOR:
                metricView.setScore(face.expressions.getLipDepressor());
                break;
            case MetricsManager.LIP_PRESS:
                metricView.setScore(face.expressions.getLipPress());
                break;
            case MetricsManager.LIP_PUCKER:
                metricView.setScore(face.expressions.getLipPucker());
                break;
            case MetricsManager.LIP_SUCK:
                metricView.setScore(face.expressions.getLipSuck());
                break;
            case MetricsManager.MOUTH_OPEN:
                metricView.setScore(face.expressions.getMouthOpen());
                break;
            case MetricsManager.NOSE_WRINKLER:
                metricView.setScore(face.expressions.getNoseWrinkler());
                break;
            case MetricsManager.SMILE:
                metricView.setScore(face.expressions.getSmile());
                break;
            case MetricsManager.SMIRK:
                metricView.setScore(face.expressions.getSmirk());
                break;
            case MetricsManager.UPPER_LIP_RAISER:
                metricView.setScore(face.expressions.getUpperLipRaiser());
                break;
            case MetricsManager.VALENCE:
                metricView.setScore(face.emotions.getValence());
                break;
            default:
                metricView.setScore(0f);
                break;
        }
    }

    /**
     * In this method, we update our drawingView to contain the dimensions of the frames coming from the camera so that drawingView
     * can correctly draw the tracking dots. We also call drawingView.setThickness(), which sets the size of the tracking dots and the
     * thickness of the bounding box.
     */
    void calculateImageDimensions(Frame image){
        ///referenceDimension will be used to determine the size of the facial tracking dots
        float referenceDimension = activityLayout.getHeight();

        //get size of frames being passed by camera
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        /**
         * If device is rotated vertically, reverse the width and height returned by the Frame object,
         * and switch the dimension we consider to be the reference dimension.
        */
        if ((ROTATE.BY_90_CW == image.getTargetRotation()) || (ROTATE.BY_90_CCW == image.getTargetRotation())) {
            int temp = imageWidth;
            imageWidth = imageHeight;
            imageHeight = temp;

            referenceDimension = activityLayout.getWidth();
        }

        drawingView.updateImageDimensions(imageWidth, imageHeight);
        drawingView.setThickness((int) (referenceDimension / 160f));
    }


    /**
     * This method is called when the SDK has corrected the aspect ratio of the SurfaceView. We use this information to resize
     * our mainLayout ViewGroup so the UI fits snugly around the SurfaceView. We also update our drawingView object, so the tracking dots
     * are drawn in the correct coordinates.
     */
    @Override
    public void onSurfaceViewAspectRatioChanged(int width, int height) {
        drawingView.updateSurfaceViewDimensions(width,height);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mainLayout.getLayoutParams();
        params.height = height;
        params.width = width;
        mainLayout.setLayoutParams(params);

        //Now that our main layout has been resized, we can remove the progress bar that was obscuring the screen (its purpose was to obscure the resizing of the SurfaceView)
        progressBarLayout.setVisibility(View.GONE);
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
        detector.setAllEmotions(false);
        detector.setAllExpressions(false);
        stopCamera();
    }

    private void stopCamera() {
        performFaceDetectionStoppedTasks();
        try {
            detector.stop();
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }


    /**
     * When the user taps the screen, hide the menu if it is visible and show it if it is hidden.
     * **/
    void setMenuVisible(boolean b){
        isMenuVisible = b;
        if (b) {
            settingsButton.setVisibility(View.VISIBLE);

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
        startActivity(new Intent(this,EditPreferences.class));
    }
}


