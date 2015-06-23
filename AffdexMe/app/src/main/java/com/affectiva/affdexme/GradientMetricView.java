package com.affectiva.affdexme;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;

/**
 * GradientMetricView is used to display the valence metric and adds functionality of allowing
 * the bar's shade of color to scale with the metric's value, rather than just being red or green.
 */
public class GradientMetricView extends MetricView {

    //Three Constructors required of any custom view:
    public GradientMetricView(Context context) {
        super(context);
    }
    public GradientMetricView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public GradientMetricView(Context context, AttributeSet attrs, int styleID){
        super(context, attrs, styleID);
    }

    /**
     * As in MetricView, we set our text to display the score and size the colored bar appropriately.
     * In this class, however, we let the score determine the color of the bar (shades of red for negative
     * and shades of green for positive).
     */
    @Override
    public void setScore(float s) {
        text = String.format("%d%%", (int)s);
        if (s > 0) {
            left = midX - (halfWidth * (s / 100));
            right = midX + (halfWidth * (s / 100));
        } else {
            left = midX - (halfWidth * (-s / 100));
            right = midX + (halfWidth * (-s / 100));
        }
        if (s > 0) {
            float colorScore = ((100f-s)/100f)*255;
            boxPaint.setColor(Color.rgb((int)colorScore,255,(int)colorScore));
        } else {
            float colorScore = ((100f+s)/100f)*255;
            boxPaint.setColor(Color.rgb(255,(int)colorScore,(int)colorScore));
        }
        invalidate(); //instruct Android to re-draw our view, now that the text has changed
    }
}
