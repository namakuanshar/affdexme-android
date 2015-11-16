package com.affectiva.affdexme;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * A view representing a metric that can be selected by the user. Meant for use by MetricSelectionFragment.
 * This view not only changes color when selected, but also plays a video. To save resources, only one MetricSelector
 * object plays a video at a time, so playback is coordinated by the MetricSelectionFragment object.
 */
public class MetricSelector extends FrameLayout {

    private boolean isMetricSelected;
    private MetricsManager.Metrics metric;

    TextureView textureView;

    TextView gridItemTextView;
    ImageView imageView;
    ImageView imageViewBeneath;
    RelativeLayout backgroundLayout;

    int itemNotSelectedColor;
    int itemSelectedColor;
    int itemSelectedOverLimitColor;

    Uri[] videoResourceURIs;
    int videoResourceURIIndex;
    TextView videoOverlay;

    int picId;

    public MetricSelector(Activity hostActivity, LayoutInflater inflater, Resources res, String packageName, MetricsManager.Metrics metric) {
        super(hostActivity);

        this.metric = metric;
        this.isMetricSelected = false;

       initContent(inflater, res, packageName);
    }

    void initContent(LayoutInflater inflater, Resources res, String packageName) {
        View content = inflater.inflate(R.layout.grid_item, this, true);

        String resourceName = MetricsManager.getLowerCaseName(metric);

        videoOverlay = (TextView) content.findViewById(R.id.video_overlay);

        int videoId = res.getIdentifier(resourceName,"raw",packageName);
        if (metric == MetricsManager.Emotions.VALENCE) {
            videoResourceURIs = new Uri[2];
            videoResourceURIs[0] = Uri.parse(String.format("android.resource://%s/%d", packageName, videoId ));
            videoResourceURIs[1] = Uri.parse(String.format("android.resource://%s/%d", packageName, res.getIdentifier(resourceName+"0","raw",packageName)));
        } else {
            videoResourceURIs = new Uri[1];
            videoResourceURIs[0] = Uri.parse(String.format("android.resource://%s/%d", packageName, videoId));
        }

        videoResourceURIIndex = 0;

        //set up image
        picId = res.getIdentifier(resourceName, "drawable", packageName);
        imageView = (ImageView) content.findViewById(R.id.grid_item_image_view);
        imageViewBeneath = (ImageView) content.findViewById(R.id.grid_item_image_view_beneath);
        imageView.setImageResource(picId);
        imageViewBeneath.setImageResource(picId);
        imageViewBeneath.setVisibility(GONE);

        backgroundLayout = (RelativeLayout) content.findViewById(R.id.grid_item_background);

        gridItemTextView = (TextView) content.findViewById(R.id.grid_item_text);
        gridItemTextView.setText(MetricsManager.getCapitalizedName(metric));

        itemSelectedOverLimitColor = res.getColor(R.color.grid_item_chosen_over_limit);
        itemNotSelectedColor = res.getColor(R.color.grid_item_not_chosen);
        itemSelectedColor = res.getColor(R.color.grid_item_chosen);
    }

    boolean getIsSelected() {
        return this.isMetricSelected;
    }

    void setIsSelected(boolean isSelected) {
        this.isMetricSelected = isSelected;
    }

    void removeCover() {
        imageViewBeneath.setVisibility(VISIBLE);
        imageView.setVisibility(GONE);
    }

    void displayCover() {
        imageViewBeneath.setVisibility(GONE);
        imageView.setVisibility(VISIBLE);
    }

    void displayVideo(TextureView videoView) {
        textureView = videoView;
        backgroundLayout.addView(textureView, 1);
        textureView.setVisibility(VISIBLE);
        videoOverlay.setVisibility(VISIBLE);
    }

    void removeVideo() {
        if (textureView != null) {
            textureView.setVisibility(GONE);
            backgroundLayout.removeView(textureView);
            textureView = null;
        }
        videoOverlay.setVisibility(GONE);
    }

    MetricsManager.Metrics getMetric() {
        return metric;
    }

    void initIndex() {
        videoResourceURIIndex = 0;
    }

    Uri getNextVideoResourceURI() {
        if (metric == MetricsManager.Emotions.VALENCE) {
            if (videoResourceURIIndex == 0) {
                videoOverlay.setText("NEGATIVE");
                videoOverlay.setTextColor(Color.RED);
            } else {
                videoOverlay.setText("POSITIVE");
                videoOverlay.setTextColor(Color.GREEN);
            }
        }

        videoResourceURIIndex += 1;
        if (videoResourceURIIndex > videoResourceURIs.length) {
            return null;
        } else {
            return videoResourceURIs[videoResourceURIIndex - 1];
        }
    }

    /**
     * Changes the appearance of the grid item to indicate whether too many metrics have been selected
     */
    void setUnderOrOverLimit(boolean atOrUnderLimit) {
        if (isMetricSelected) {
            if (atOrUnderLimit) {
                gridItemTextView.setBackgroundColor(itemSelectedColor);
                backgroundLayout.setBackgroundColor(itemSelectedColor);
            } else {
                gridItemTextView.setBackgroundColor(itemSelectedOverLimitColor);
                backgroundLayout.setBackgroundColor(itemSelectedOverLimitColor);
            }
        } else {
            gridItemTextView.setBackgroundColor(itemNotSelectedColor);
            backgroundLayout.setBackgroundColor(itemNotSelectedColor);
        }
    }



}
