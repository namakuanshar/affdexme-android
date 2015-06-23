package com.affectiva.affdexme;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
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
 *  you will find the calls relevant to the use of the SDK in the startCamera() and stopCamera() methods, as well as the onImageResults() method.
 *
 * This class implements the Detector.ImageListener interface, allowing it to receive the onImageResults() event.
 * This class implements the Detector.FaceListener interface, allowing it to receive the onFaceDetectionStarted() and
 * onFaceDetectionStopped() events.
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
        implements Detector.FaceListener, Detector.ImageListener, TextView.OnEditorActionListener, View.OnTouchListener{

    private static final String LOG_TAG = "AffdexMe";

    //Affectiva SDK Object
    private CameraDetector detector = null;

    //Metrics View UI Objects
    private RelativeLayout metricViewLayout;
    private LinearLayout leftMetricsLayout;
    private LinearLayout rightMetricsLayout;
    private MetricView smilePct;
    private MetricView browRaisePct;
    private MetricView browFurrowPct;
    private MetricView frownPct;
    private MetricView valencePct;
    private MetricView engagementPct;
    private TextView fpsName;
    private TextView fpsPct;
    private TextView smileName;
    private TextView browRaiseName;
    private TextView browFurrowName;
    private TextView frownName;
    private TextView valenceName;
    private TextView engagementName;

    //Menu UI Objects
    private RelativeLayout menuLayout;
    private EditText fpsEditText;
    private CheckBox fpsCheckbox;
    private CheckBox trackingCheckbox;
    private TextView fpsEditTextName;

    //Other UI objects
    private ViewGroup activityLayout; //top-most ViewGroup in which all content resides
    private RelativeLayout mainLayout; //layout, to be resized, containing all UI elements
    private RelativeLayout progressBarLayout; //layout used to show progress circle while camera is starting
    private SurfaceView cameraView; //SurfaceView used to display camera images
    private DrawingView drawingView; //SurfaceView containing its own thread, used to draw facial tracking dots

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
    }

    void initializeUI() {

        //Get handles to UI objects
        activityLayout = (ViewGroup) findViewById(android.R.id.content);
        progressBarLayout = (RelativeLayout) findViewById(R.id.progress_bar_cover);
        metricViewLayout = (RelativeLayout) findViewById(R.id.metric_view_group);
        leftMetricsLayout = (LinearLayout) findViewById(R.id.left_metrics);
        rightMetricsLayout = (LinearLayout) findViewById(R.id.right_metrics);
        mainLayout = (RelativeLayout) findViewById(R.id.main_layout);
        menuLayout = (RelativeLayout) findViewById(R.id.affdexme_menu);
        smilePct = (MetricView) findViewById(R.id.smile_pct);
        browRaisePct = (MetricView) findViewById(R.id.brow_raise_pct);
        browFurrowPct = (MetricView) findViewById(R.id.brow_furrow_pct);
        frownPct = (MetricView) findViewById(R.id.frown_pct);
        valencePct = (MetricView) findViewById(R.id.valence_pct);
        engagementPct = (MetricView) findViewById(R.id.engagement_pct);
        fpsPct = (TextView) findViewById(R.id.fps_value);
        smileName = (TextView) findViewById(R.id.smile_name);
        browRaiseName = (TextView) findViewById(R.id.brow_raise_name);
        browFurrowName = (TextView) findViewById(R.id.brow_furrow_name);
        frownName = (TextView) findViewById(R.id.frown_name);
        valenceName = (TextView) findViewById(R.id.valence_name);
        engagementName = (TextView) findViewById(R.id.engagement_name);
        fpsName = (TextView) findViewById(R.id.fps_name);
        fpsEditText = (EditText) findViewById(R.id.fps_edittext);
        fpsEditTextName = (TextView) findViewById(R.id.fps_edittext_name);
        fpsCheckbox = (CheckBox) findViewById(R.id.fps_checkbox);
        trackingCheckbox = (CheckBox) findViewById(R.id.tracking_checkbox);
        cameraView = (SurfaceView) findViewById(R.id.camera_preview);
        drawingView = (DrawingView) findViewById(R.id.drawing_view);

        //Load Application Font and set UI Elements to use it
        Typeface face = Typeface.createFromAsset(getAssets(), "fonts/Square.ttf");
        smilePct.setTypeface(face);
        browRaisePct.setTypeface(face);
        browFurrowPct.setTypeface(face);
        frownPct.setTypeface(face);
        valencePct.setTypeface(face);
        engagementPct.setTypeface(face);
        smileName.setTypeface(face);
        browRaiseName.setTypeface(face);
        browFurrowName.setTypeface(face);
        frownName.setTypeface(face);
        valenceName.setTypeface(face);
        engagementName.setTypeface(face);
        fpsPct.setTypeface(face);
        fpsName.setTypeface(face);
        fpsEditTextName.setTypeface(face);
        fpsCheckbox.setTypeface(face);
        trackingCheckbox.setTypeface(face);

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
        menuLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                /**
                 * This method effectively blocks the mainLayout from receiving a touch event
                 * when the menu is pressed. This is to prevent the menu from closing if the user accidentally touches it
                 * when aiming for a checkbox or edit box.
                 */
                return true;
            }
        });
        fpsEditText.setOnEditorActionListener(this);

        /**
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

    /**
     * We use onResume() to restore application settings using the SharedPreferences object and
     * to indicate that dimensions should be recalculated.
     */
    @Override
    public void onResume() {
        super.onResume();
        restoreApplicationSettings();
        drawingView.invalidateDimensions(); //set flag to have screen dimensions resized (usage appears in onImageResults())
        setMenuVisible(false); //always make the menu invisible by default
    }

    /**
     * We use the Shared Preferences object to restore application settings.
     * **/
    public void restoreApplicationSettings() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, 0);

        detectorProcessRate = sharedPreferences.getInt("rate", detectorProcessRate);   //restore camera processing rate
        fpsEditText.setText(String.valueOf(detectorProcessRate));

        if (sharedPreferences.getBoolean("fps",isFPSVisible)) {    //restore isFPSMetricVisible
            fpsCheckbox.setChecked(true);
            setFPSVisible(true);
        } else {
            fpsCheckbox.setChecked(false);
            setFPSVisible(false);
        }

        if (sharedPreferences.getBoolean("track",drawingView.getDrawPointsEnabled())) {  //restore isTrackingDotsVisible
            setTrackPoints(true);
            trackingCheckbox.setChecked(true);
        } else {
            setTrackPoints(false);
            trackingCheckbox.setChecked(false);
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
     * the camera will not be reinitialized because the detector object will not have been set to null during onPause().
     * We also reset variables used to calculate the Processed Frames Per Second.
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus && isFrontFacingCameraDetected) {
            startCamera();
            resetFPSCalculations();
        }
    }

    void startCamera() {
        if (detector == null) {
            /** Put the SDK in camera mode by using this constructor. The SDK will be in control of
             * the camera. If a SurfaceView is passed in as the last argument to the constructor,
             * that view will be painted with what the camera sees.
             */
            detector = new CameraDetector(this, CameraDetector.CameraType.CAMERA_FRONT, cameraView);

            // NOTE: uncomment the line below and replace "YourLicenseFile" with your license file, which should be stored in /assets/Affdex/
            //detector.setLicensePath("YourLicenseFile");

            // We want to detect all expressions, so turn on all classifiers.
            detector.setDetectSmile(true);
            detector.setDetectBrowFurrow(true);
            detector.setDetectBrowRaise(true);
            detector.setDetectEngagement(true);
            detector.setDetectValence(true);
            detector.setDetectLipCornerDepressor(true);

            detector.setMaxProcessRate(detectorProcessRate);

            detector.setImageListener(this);
            detector.setFaceListener(this);

            //now that the CameraDetector object has been set up, start the camera
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
         * If the flag indicating that we need to size our layout is set, call calculateDimensions().
         * The flag is a boolean stored in our drawingView object, retrieved through DrawingView.isDimensionsNeeded().
         */
        if (drawingView.isDimensionsNeeded() ) {
            calculateDimensions(image);
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
        smilePct.setScore(face.getSmileScore());
        browRaisePct.setScore(face.getBrowRaiseScore());
        browFurrowPct.setScore(face.getBrowFurrowScore());
        engagementPct.setScore(face.getEngagementScore());
        frownPct.setScore(face.getLipCornerDepressorScore());
        float valenceScore = face.getValenceScore();
        valencePct.setScore(valenceScore);

        /**
         * If the user has selected to have facial tracking dots drawn, we use face.getFacePoints() to send those points
         * to our drawing thread and also inform the thread what the valence score was, as that will determine the color
         * of the bounding box.
        */
        if (drawingView.getDrawPointsEnabled()) {
            drawingView.setScore(valenceScore);
            drawingView.updatePoints(face.getFacePoints());
        }
    }

    /**
     * This method serves two purposes:
     * -It informs the drawing thread of the size of the frames passed by the CameraDetector object.
     * -It corrects the dimensions of our mainLayout object to conform to the aspect ratio of the frames passed by the CameraDetector object.
     */
    void calculateDimensions(Frame image){
        //Log.i(LOG_TAG,"Dimensions being re-calculated");
        float screenWidth = activityLayout.getWidth();
        float screenHeight = activityLayout.getHeight();
        float referenceDimension = screenHeight; //referenceDimension will be used to determine the size of the facial tracking dots

        //get size of frames being passed by camera
        float imageWidth = image.getWidth();
        float imageHeight = image.getHeight();

        /**
         * If device is rotated vertically, reverse the width and height returned by the Frame object,
         * and switch the dimension we consider to be the reference dimension.
        */
        if ((ROTATE.BY_90_CW == image.getTargetRotation()) || (ROTATE.BY_90_CCW == image.getTargetRotation())) {
            float temp = imageWidth;
            imageWidth = imageHeight;
            imageHeight = temp;

            referenceDimension = screenWidth;
        }

        /**
         * In this section, we resize our layouts so that the SurfaceView displaying the camera images to will have the same
         * aspect ratio as the frames we are receiving from the camera.
         * Since all elements in our app are inside 'mainLayout', we just have to adjust the height and width of this layout.
         */

        //calculate aspect ratios of camera frames and screen
        float imageAspectRatio = imageWidth/imageHeight;
        float screenAspectRatio = screenWidth/screenHeight;
        float screenToImageRatio = 0;
        int newLayoutHeight = 0;
        int newLayoutWidth = 0;

        if (screenAspectRatio < imageAspectRatio) {
            newLayoutHeight = (int) (screenWidth / imageAspectRatio);
            screenToImageRatio = screenWidth / imageWidth;
            newLayoutWidth = (int)screenWidth;
        } else {
            newLayoutWidth =  (int) (screenHeight * imageAspectRatio);
            screenToImageRatio = screenHeight/imageHeight;
            newLayoutHeight = (int)screenHeight;
        }


        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mainLayout.getLayoutParams();
        params.height = newLayoutHeight;
        params.width = newLayoutWidth;
        mainLayout.setLayoutParams(params);

        /**
         * Send necessary dimensions to the drawing thread.
         * The dimensions are: width of frame, height of frame, ratio of screen to frame size, and thickness of facial tracking dots.
         * This method will clear the flag that indicates whether we need to calculate dimensions, so this calculateDimensions()
         * will not be continuously called.
         */
        drawingView.setDimensions((int) imageWidth, (int) imageHeight, screenToImageRatio, referenceDimension / 160);

        //Now that the aspect ratio has been corrected, remove the progress bar from obscuring the screen
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
        saveApplicationSettings();
        progressBarLayout.setVisibility(View.VISIBLE);
        stopCamera();
    }

    private void stopCamera() {
        performFaceDetectionStoppedTasks();

        if (null != detector) {
            try {
                detector.stop();
            } catch (Exception e) {
                Log.e("AffdexMe", e.getMessage());
            }
        }
        detector = null; //setting detector to null will allow startCamera() to recreate the detector object when the application is reopened.
    }

    /**
     * We use the SharedPreferences object to save application settings.
    **/
    public void saveApplicationSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("fps", isFPSVisible);
        editor.putBoolean("track", drawingView.getDrawPointsEnabled());
        editor.putInt("rate", detectorProcessRate);
        editor.commit();
    }

    public void fps_checkbox_click(View view) {
        setFPSVisible(((CheckBox) view).isChecked());
    }

    public void tracking_checkbox_click(View view) {
        setTrackPoints(((CheckBox) view).isChecked());
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        /**
         * When a user has selected  the Edit box to change the number of frames the detector processes per second
         * and presses the 'DONE' button, the below block will be executed.
         * */
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            int parsedInt = 0;
            try {
                parsedInt = Integer.parseInt(v.getText().toString());
            } catch (Exception e) {
                v.setText(String.valueOf(detectorProcessRate));
                return false;
            }
            if (parsedInt > 0) {
                detectorProcessRate = parsedInt;
                detector.setMaxProcessRate(detectorProcessRate);
                resetFPSCalculations(); //reset FPS variables, since changing the process rate should change the FPS.
            } else {
                v.setText(String.valueOf(detectorProcessRate));
            }
        }
        return false; //return false regardless, so Android closes the keyboard when user presses 'DONE'
    }

    /**
     * When the user taps the screen, hide the menu if it is visible and show it if it is hidden.
     * **/
    void setMenuVisible(boolean b){
        isMenuVisible = b;
        if (b) {
            menuLayout.setVisibility(View.VISIBLE);

            //We display the navigation bar again
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
        else {
            InputMethodManager imm = (InputMethodManager)getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(fpsEditText.getWindowToken(), 0);

            //We hide the navigation bar
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);


            menuLayout.setVisibility(View.INVISIBLE);
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
}


