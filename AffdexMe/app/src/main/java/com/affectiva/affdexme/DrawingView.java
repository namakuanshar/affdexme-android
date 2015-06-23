package com.affectiva.affdexme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.os.Process;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


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
        boolean isDrawPointsEnabled = false; //saves whether user has selected dots to be drawn
        float imageWidth = 0;
        float imageHeight = 0;
        float screenToImageRatio = 0;
        float drawThickness = 0; //thickness with which dots and square will be drawn

        private final long drawPeriod = 33; //draw at 30 fps

        public DrawingThread(SurfaceHolder surfaceHolder, boolean drawPoints) {
            mSurfaceHolder = surfaceHolder;

            circlePaint = new Paint();
            circlePaint.setColor(Color.WHITE);

            boxPaint = new Paint();
            boxPaint.setColor(Color.WHITE);
            boxPaint.setStyle(Paint.Style.STROKE);

            isDrawPointsEnabled = drawPoints;
        }

        /**
         * Used to set the valence score, which determines the color of the bounding box.
         * **/
        void setScore(float s) {
            if (s > 0) {
                float colorScore = ((100f-s)/100f)*255;
                boxPaint.setColor(Color.rgb((int)colorScore,255,(int)colorScore));
            } else {
                float colorScore = ((100f+s)/100f)*255;
                boxPaint.setColor(Color.rgb(255,(int)colorScore,(int)colorScore));
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

        //Sets measurements thread will use to draw facial tracking dots.
        public void setDimen(int w, int h, float appToImageRatio, float thickness) {
            imageWidth = w;
            imageHeight = h;
            screenToImageRatio = appToImageRatio;
            drawThickness = thickness;
            boxPaint.setStrokeWidth(thickness);
        }

        private void setDrawPointsEnabled(boolean b) {
            isDrawPointsEnabled = b;
        }

        private boolean getDrawPointsEnabled() {
            return isDrawPointsEnabled;
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
                            if (isDrawPointsEnabled && (nextPointsToDraw != null) ) {
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
        }

        void draw(Canvas c) {
            //Save our own reference to the list of points, in case the previous reference is overwritten by the main thread.
            PointF[] points = nextPointsToDraw;

            //Coordinates around which to draw bounding box.
            float leftBx = imageWidth;
            float rightBx = 0;
            float topBx = imageHeight;
            float botBx = 0;

            //Iterate through all the points given to us by the CameraDetector object
            for (int i = 0; i < points.length; i++) {

                //We determine the left-most, top-most, right-most, and bottom-most points to draw the bounding box around.
                if (points[i].x < leftBx)
                    leftBx = points[i].x;
                if (points[i].x > rightBx)
                    rightBx = points[i].x;
                if (points[i].y < topBx)
                    topBx = points[i].y;
                if (points[i].y > botBx)
                    botBx = points[i].y;

                //Draw facial tracking dots.
                //The camera preview is displayed as a mirror, so X pts have to be reversed
                c.drawCircle((imageWidth - points[i].x - 1) * screenToImageRatio, (points[i].y)* screenToImageRatio, drawThickness, circlePaint);
            }

            //Draw the bounding box.
            c.drawRect((imageWidth - leftBx - 1) * screenToImageRatio, topBx * screenToImageRatio, (imageWidth - rightBx - 1) * screenToImageRatio, botBx * screenToImageRatio, boxPaint);
        }
    }

    //Class variables of DrawingView class
    private SurfaceHolder surfaceHolder;
    private DrawingThread drawingThread; //DrawingThread object
    private boolean isDimensionsNeeded = true;
    private boolean isDrawPointsEnabled = true; //by default, start drawing thread without drawing points
    private static String LOG_TAG = "AffdexMe";

    //three constructors required of any custom view
    public DrawingView(Context context) {
        super(context);
        initView();
    }
    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }
    public DrawingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    void initView(){
        surfaceHolder = getHolder(); //The SurfaceHolder object will be used by the thread to request canvas to draw on SurfaceView
        surfaceHolder.setFormat(PixelFormat.TRANSPARENT); //set to Transparent so this surfaceView does not obscure the one it is overlaying (the one displaying the camera).
        surfaceHolder.addCallback(this); //become a Listener to the three events below that SurfaceView throws
        drawingThread = new DrawingThread(surfaceHolder, isDrawPointsEnabled);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (drawingThread.isStopped()) {
            drawingThread = new DrawingThread(surfaceHolder, isDrawPointsEnabled);
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
        isDimensionsNeeded = true; //Now that thread has been destroyed, we'll need dimensions to be recalculated if a new thread is later created.
    }

    public void setDimensions(int width, int height, float appToImageRatio, float radius) {
        drawingThread.setDimen(width, height, appToImageRatio, radius);
        isDimensionsNeeded = false;
    }

    public boolean isDimensionsNeeded() {
        return isDimensionsNeeded;
    }

    public void setDrawPointsEnabled(boolean b){
        isDrawPointsEnabled = b;
        drawingThread.setDrawPointsEnabled(b);
    }

    public void invalidateDimensions() {
        isDimensionsNeeded = true;
    }

    public boolean getDrawPointsEnabled() {
        return isDrawPointsEnabled;
    }

    //The methods below simply delegate to the drawingThread object
    public void setScore(float s) {
        drawingThread.setScore(s);
    }

    public void updatePoints(PointF[] points) {
        drawingThread.updatePoints(points);
    }

    public void invalidatePoints(){
        drawingThread.invalidatePoints();
    }






}
