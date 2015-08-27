package com.affectiva.affdexme;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Process;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.affectiva.android.affdex.sdk.detector.Face;

import org.w3c.dom.Attr;

import java.lang.reflect.Type;


/**
 * This class contains a SurfaceView and its own thread that draws to it.
 * It is used to display the facial tracking dots over a user's face.
 */
public class DrawingView extends SurfaceView implements SurfaceHolder.Callback {

    //Inner Thread class
    class DrawingThread extends Thread{
        private SurfaceHolder mSurfaceHolder;
        private Paint circlePaint;
        private Paint boxPaint;
        private boolean stopFlag = false; //boolean to indicate when thread has been told to stop
        private PointF[] nextPointsToDraw = null; //holds a reference to the most recent set of points returned by CameraDetector, passed in by main thread
        private DrawingViewConfig config;
        private final long drawPeriod = 33; //draw at 30 fps

        private final int TEXT_RAISE = 10;

        String roll = "";
        String yaw = "";
        String pitch = "";
        String interOcDis = "";
        String gender = "";

        public DrawingThread(SurfaceHolder surfaceHolder, DrawingViewConfig con) {
            mSurfaceHolder = surfaceHolder;

            circlePaint = new Paint();
            circlePaint.setColor(Color.WHITE);
            boxPaint = new Paint();
            boxPaint.setColor(Color.WHITE);
            boxPaint.setStyle(Paint.Style.STROKE);

            config = con;

            setThickness(config.drawThickness);
        }

        void setMetrics(float roll, float yaw, float pitch, float interOcDis, float valence, Face.Gender gender) {
            this.roll = String.format("%.2f",roll);
            this.yaw = String.format("%.2f",yaw);
            this.pitch = String.format("%.2f",pitch);
            this.interOcDis = String.format("%.2f",interOcDis);
            switch (gender) {
                case Male:
                    this.gender = "MALE";
                    break;
                case Female:
                        this.gender = "FEMALE";
                    break;
                default:
                    this.gender = "UNKNOWN";

            }

            if (valence > 0) {
                float colorScore = ((100f-valence)/100f)*255;
                boxPaint.setColor(Color.rgb((int)colorScore,255,(int)colorScore));
            } else {
                float colorScore = ((100f+valence)/100f)*255;
                boxPaint.setColor(Color.rgb(255, (int) colorScore, (int) colorScore));
            }

        }

        public void stopThread() {
            stopFlag = true;
        }

        public boolean isStopped() {
            return stopFlag;
        }

        //Updates thread with latest points returned by the onImageResults() event.
        public void updatePoints(PointF[] pointList) {
            nextPointsToDraw = pointList;
        }

        void setThickness(int thickness) {
            boxPaint.setStrokeWidth(thickness);
        }

        //Inform thread face detection has stopped, so array of points is no longer valid.
        public void invalidatePoints() {
            nextPointsToDraw = null;
        }

        @Override
        public void run() {
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            while(!stopFlag) {
                long startTime = SystemClock.elapsedRealtime(); //get time at the start of thread loop

                /**
                 * We use SurfaceHolder.lockCanvas() to get the canvas that draws to the SurfaceView.
                 * After we are done drawing, we let go of the canvas using SurfaceHolder.unlockCanvasAndPost()
                 * **/
                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas();

                    if (c!= null) {
                        synchronized (mSurfaceHolder) {
                            c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); //clear previous dots
                            if ((nextPointsToDraw != null) ) {
                                draw(c);
                            }
                        }
                    }
                }
                finally {
                    if (c!= null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }

                //send thread to sleep so we don't draw faster than the requested 'drawPeriod'.
                long sleepTime = drawPeriod - (SystemClock.elapsedRealtime() - startTime);
                try {
                    if(sleepTime>0){
                        this.sleep(sleepTime);
                    }
                } catch (InterruptedException ex) {
                    Log.e(LOG_TAG,ex.getMessage());
                }
            }

            config = null; //nullify object to avoid memory leak
        }

        void draw(Canvas c) {
            //Save our own reference to the list of points, in case the previous reference is overwritten by the main thread.
            PointF[] rawPoints = nextPointsToDraw;

            //Coordinates around which to draw bounding box.
            float leftBx = config.surfaceViewWidth;
            float rightBx = 0;
            float topBx = config.surfaceViewHeight;
            float botBx = 0;

            for (int i = 0; i < rawPoints.length; i++) {

                float x = (config.imageWidth - rawPoints[i].x - 1) * config.screenToImageRatio;
                float y = (rawPoints[i].y)* config.screenToImageRatio;

                //We determine the left-most, top-most, right-most, and bottom-most points to draw the bounding box around.
                if (x < leftBx)
                    leftBx = x;
                if (x > rightBx)
                    rightBx = x;
                if (y < topBx)
                    topBx = y;
                if (y > botBx)
                    botBx = y;

                //Draw facial tracking dots.
                //The camera preview is displayed as a mirror, so X pts have to be reversed
                if (config.isDrawPointsEnabled) {
                    c.drawCircle(x, y, config.drawThickness, circlePaint);
                }
            }

            //Draw the bounding box.
            if (config.isDrawPointsEnabled) {
                c.drawRect(leftBx, topBx, rightBx, botBx, boxPaint);
            }

            if (config.isDrawMeasurementsEnabled) {
                float centerBx = (leftBx + rightBx) / 2;

                float upperText = topBx - TEXT_RAISE;
                c.drawText("PITCH", centerBx, upperText - config.textSize,config.dropShadowPaint);
                c.drawText("PITCH", centerBx, upperText - config.textSize, config.textPaint);
                c.drawText(pitch,centerBx ,upperText ,config.dropShadowPaint);
                c.drawText(pitch, centerBx, upperText, config.textPaint);

                float upperLeft = centerBx - config.upperTextSpacing;

                c.drawText("YAW", upperLeft , upperText - config.textSize , config.dropShadowPaint);
                c.drawText("YAW", upperLeft, upperText - config.textSize, config.textPaint);
                c.drawText(yaw, upperLeft , upperText , config.dropShadowPaint);
                c.drawText(yaw, upperLeft, upperText, config.textPaint);

                float upperRight = centerBx + config.upperTextSpacing;
                c.drawText("ROLL", upperRight , upperText - config.textSize , config.dropShadowPaint);
                c.drawText("ROLL", upperRight, upperText - config.textSize, config.textPaint);
                c.drawText(roll, upperRight , upperText , config.dropShadowPaint);
                c.drawText(roll, upperRight, upperText, config.textPaint);

                float lowerLeft = centerBx - config.lowerTextSpacing;

                c.drawText("GENDER", lowerLeft , botBx + config.textSize , config.dropShadowPaint);
                c.drawText("GENDER", lowerLeft, botBx + config.textSize, config.textPaint);
                c.drawText(gender, lowerLeft , botBx + config.textSize*2 ,config.dropShadowPaint);
                c.drawText(gender, lowerLeft, botBx + config.textSize * 2, config.textPaint);

                float lowerRight = centerBx + config.lowerTextSpacing;

                c.drawText("INTEROCULAR DISTANCE", lowerRight , botBx + config.textSize , config.dropShadowPaint);
                c.drawText("INTEROCULAR DISTANCE", lowerRight, botBx + config.textSize, config.textPaint);
                c.drawText(interOcDis,lowerRight , botBx + config.textSize*2 , config.dropShadowPaint);
                c.drawText(interOcDis, lowerRight, botBx + config.textSize * 2, config.textPaint);
            }


        }
    }

    class DrawingViewConfig {
        private int imageHeight = 1;
        private int imageWidth = 1;
        private int surfaceViewWidth = 0;
        private int surfaceViewHeight = 0;
        private float screenToImageRatio = 0;
        private int drawThickness = 0;
        private boolean isImageDimensionsNeeded = true;
        private boolean isSurfaceViewDimensionsNeeded = true;
        private boolean isDrawPointsEnabled = true; //by default, have the drawing thread draw tracking dots
        private boolean isDrawMeasurementsEnabled = false;

        private Paint textPaint;
        private int textSize;
        private Paint dropShadowPaint;
        private int upperTextSpacing;
        private int lowerTextSpacing;

        public void setMeasurementMetricConfigs(Paint textPaint, Paint dropShadowPaint, int textSize, int upperTextSpacing, int lowerTextSpacing) {
            this.textPaint = textPaint;
            this.textSize = textSize;
            this.dropShadowPaint = dropShadowPaint;
            this.upperTextSpacing = upperTextSpacing;
            this.lowerTextSpacing = lowerTextSpacing;
        }

        public void updateImageDimensions(int w, int h) {

            if ( (w <= 0) || (h <= 0)) {
                throw new IllegalArgumentException("Image Dimensions must be positive.");
            }

            imageWidth = w;
            imageHeight = h;
            if (!isSurfaceViewDimensionsNeeded) {
                screenToImageRatio = (float)surfaceViewWidth / (float)imageWidth;
            }
            isImageDimensionsNeeded = false;
        }

        public void updateSurfaceViewDimensions(int w, int h) {

            if ( (w <= 0) || (h <= 0)) {
                throw new IllegalArgumentException("SurfaceView Dimensions must be positive.");
            }

            surfaceViewWidth = w;
            surfaceViewHeight = h;
            if (!isImageDimensionsNeeded) {
                screenToImageRatio = (float)surfaceViewWidth / (float)imageWidth;
            }
            isSurfaceViewDimensionsNeeded = false;
        }

        public void setDrawThickness(int t) {

            if ( t <= 0) {
                throw new IllegalArgumentException("Thickness must be positive.");
            }

            drawThickness = t;
        }
    }

    //Class variables of DrawingView class
    private SurfaceHolder surfaceHolder;
    private DrawingThread drawingThread; //DrawingThread object
    private Typeface typeface;
    private DrawingViewConfig drawingViewConfig;
    private static String LOG_TAG = "AffdexMe";


    //three constructors required of any custom view
    public DrawingView(Context context) {
        super(context);
        initView(context, null);
    }
    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }
    public DrawingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context, attrs);
    }

    void initView(Context context, AttributeSet attrs){
        surfaceHolder = getHolder(); //The SurfaceHolder object will be used by the thread to request canvas to draw on SurfaceView
        surfaceHolder.setFormat(PixelFormat.TRANSPARENT); //set to Transparent so this surfaceView does not obscure the one it is overlaying (the one displaying the camera).
        surfaceHolder.addCallback(this); //become a Listener to the three events below that SurfaceView throws

        drawingViewConfig = new DrawingViewConfig();

        //default values
        int upperTextSpacing = 15;
        int lowerTextSpacing = 15;
        int textSize = 15;

        Paint measurementTextPaint = new Paint();
        measurementTextPaint.setStyle(Paint.Style.FILL);
        measurementTextPaint.setTextAlign(Paint.Align.CENTER);

        Paint dropShadow = new Paint();
        dropShadow.setColor(Color.BLACK);
        dropShadow.setStyle(Paint.Style.STROKE);
        dropShadow.setTextAlign(Paint.Align.CENTER);

        //load and parse XML attributes
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs,R.styleable.drawing_view_attributes,0,0);
            upperTextSpacing = a.getDimensionPixelSize(R.styleable.drawing_view_attributes_measurements_upper_spacing,upperTextSpacing);
            lowerTextSpacing = a.getDimensionPixelSize(R.styleable.drawing_view_attributes_measurements_lower_spacing,lowerTextSpacing);
            measurementTextPaint.setColor(a.getColor(R.styleable.drawing_view_attributes_measurements_color,Color.WHITE));
            dropShadow.setColor(a.getColor(R.styleable.drawing_view_attributes_measurements_text_border_color,Color.BLACK));
            dropShadow.setStrokeWidth(a.getInteger(R.styleable.drawing_view_attributes_measurements_text_border_thickness,5));
            textSize = a.getDimensionPixelSize(R.styleable.drawing_view_attributes_measurements_text_size,textSize);
            measurementTextPaint.setTextSize(textSize);
            dropShadow.setTextSize(textSize);
            a.recycle();
        }

        drawingViewConfig.setMeasurementMetricConfigs(measurementTextPaint, dropShadow, textSize, upperTextSpacing, lowerTextSpacing);

        drawingThread = new DrawingThread(surfaceHolder, drawingViewConfig);
    }
    
    public void setTypeface(Typeface face) {
        drawingViewConfig.textPaint.setTypeface(face);
        drawingViewConfig.dropShadowPaint.setTypeface(face);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (drawingThread.isStopped()) {
            drawingThread = new DrawingThread(surfaceHolder, drawingViewConfig);
        }
        drawingThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //command thread to stop, and wait until it stops
        boolean retry = true;
        drawingThread.stopThread();
        while (retry) {
            try {
                drawingThread.join();
                retry = false;
            } catch (InterruptedException e) {
                Log.e(LOG_TAG,e.getMessage());
            }
        }
    }

    public boolean isImageDimensionsNeeded() {
        return drawingViewConfig.isImageDimensionsNeeded;
    }

    public boolean isSurfaceDimensionsNeeded() {
        return drawingViewConfig.isSurfaceViewDimensionsNeeded;
    }

    public void invalidateImageDimensions() {
        drawingViewConfig.isImageDimensionsNeeded = true;
    }

    public void updateImageDimensions(int w, int h) {
        try {
            drawingViewConfig.updateImageDimensions(w, h);
        } catch (Exception e) {
            Log.e(LOG_TAG,e.getMessage());
        }
    }

    public void updateSurfaceViewDimensions(int w, int h) {
        try {
            drawingViewConfig.updateSurfaceViewDimensions(w, h);
        } catch (Exception e) {
            Log.e(LOG_TAG,e.getMessage());
        }
    }

    public void setThickness(int t) {
        drawingViewConfig.setDrawThickness(t);
        try {
            drawingThread.setThickness(t);
        } catch(Exception e) {
            Log.e(LOG_TAG,e.getMessage());
        }
    }

    public void setDrawPointsEnabled(boolean b){
        drawingViewConfig.isDrawPointsEnabled = b;
    }

    public boolean getDrawPointsEnabled() {
        return drawingViewConfig.isDrawPointsEnabled;
    }

    public  void setDrawMeasurementsEnabled(boolean b) {
        drawingViewConfig.isDrawMeasurementsEnabled = b;
    }

    public boolean getDrawMeasurementsEnabled() {
        return drawingViewConfig.isDrawMeasurementsEnabled;
    }

    public void setMetrics(float roll, float yaw, float pitch, float interOcDis, float valence, Face.Gender gender) {
        drawingThread.setMetrics(roll,yaw,pitch,interOcDis,valence,gender);
    }

    public void updatePoints(PointF[] points) {
        drawingThread.updatePoints(points);
    }

    public void invalidatePoints(){
        drawingThread.invalidatePoints();
    }




}
