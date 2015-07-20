package com.affectiva.affdexme;

import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;


/**
 * Created by Alan on 7/14/2015.
 */
public class MetricChooserActivity extends Activity {

    GridView metricsGridView;
    //MetricChooserAdapter mAdapter;
    SharedPreferences sharedPreferences;
    MetricAdapterObject[] metricAdapterObjects;
    int numberOfSelectedItems = 0;
    final int MAX_NUMBER_TO_SELECT = 6;
    TextView metricChooserTextView;
    int atOrUnderLimitColorCode;
    int redColorCode;
    int transparentBlackColorCode;
    int chosenColorCode;
    int chosenOverLimitColorCode;

    GridLayout gridLayout;

    ViewTreeObserver obs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.metric_chooser);

        gridLayout = (GridLayout) findViewById(R.id.metric_chooser_gridlayout);

        gridLayout.post(new Runnable() {
            @Override
            public void run() {
                LayoutInflater inflater = MetricChooserActivity.this.getLayoutInflater();

                //calculate number of columns
                int gridWidth = gridLayout.getWidth();
                int minColumnWidth = 100;
                int numColumns = gridWidth / minColumnWidth; //intentional integer division

                int currentRow = 0;
                /*
                //calculate number of rows
                int emotionRows = MetricsManager.getTotalNumEmotions() / numColumns; //intentional integer division
                if (emotionRows == 0)
                    emotionRows = 1;
                else if (MetricsManager.getTotalNumEmotions() % emotionRows != 0)
                    emotionRows += 1;
                int expressionRows = MetricsManager.getTotalNumExpressions() / numColumns; //intentional integer division
                if (expressionRows == 0)
                    expressionRows = 1;
                else if (MetricsManager.getTotalNumExpressions() % expressionRows != 0)
                    expressionRows += 1;
                int totalRows = emotionRows + expressionRows + 2; //2 extra rows for the two headers*/


                //Add first header

                float columnWidth = (float) gridWidth / numColumns;

                View expressionsHeader = inflater.inflate(R.layout.grid_header, null);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(GridLayout.spec(currentRow, 1), GridLayout.spec(0, numColumns));
                params.width = gridLayout.getWidth();
                expressionsHeader.setLayoutParams(params);
                ((TextView) expressionsHeader.findViewById(R.id.header_text)).setText("Expressions");
                gridLayout.addView(expressionsHeader);
                currentRow += 1;

                //add emotions
                int currentColumn = 0;
                for (int i = 0; i < MetricsManager.EXPRESSIONS_START_INDEX; i++) {
                    View gridItem = inflater.inflate(R.layout.grid_item, null);


                    GridLayout.LayoutParams params2 = new GridLayout.LayoutParams();
                    params2.width = 100;
                    params2.height = 100;
                    params2.columnSpec = GridLayout.spec(currentColumn);
                    params2.rowSpec = GridLayout.spec(currentRow);
                    gridItem.setLayoutParams(params2);

                    gridLayout.addView(gridItem);

                    currentColumn += 1;
                    if (currentColumn >= numColumns) {
                        currentColumn = 0;
                        currentRow += 1;
                    }

                }

                currentRow += 1;
                gridLayout.setColumnCount(numColumns);
                gridLayout.setRowCount(currentRow);
            }


        });

        //metricsGridView = (GridView) findViewById(R.id.metrics_grid_view);

        /*
        metricAdapterObjects = generateMetricObjectArray();

        mAdapter = new MetricChooserAdapter(metricAdapterObjects);
        metricsGridView.setAdapter(mAdapter);
        metricsGridView.setOnItemClickListener(this);

        metricChooserTextView = (TextView) findViewById(R.id.metrics_chooser_textview);

        atOrUnderLimitColorCode = Color.rgb(255,255,255);
        redColorCode = Color.rgb(255,0,0);
        chosenOverLimitColorCode = Color.argb(130,180,0,0);
        transparentBlackColorCode = Color.argb(130, 0, 0, 0);
        chosenColorCode = Color.rgb(0,150,0);*/

    }



    /*
    MetricAdapterObject[] generateMetricObjectArray() {
        MetricAdapterObject[] toReturn = new MetricAdapterObject[MetricsManager.metricsArray.length];
        for (int n = 0; n < MetricsManager.metricsArray.length; n++) {
            toReturn[n] = new MetricAdapterObject(MetricsManager.metricsArray[n]);
        }


        return toReturn;
    }

    @Override
    protected void onResume() {
        super.onResume();
       restoreChosenMetrics();




    }


    void restoreChosenMetrics() {

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        selectItem(sharedPreferences.getInt("metric_1", 0), true);
        selectItem(sharedPreferences.getInt("metric_2", 1), true);
        selectItem(sharedPreferences.getInt("metric_3", 2), true);
        selectItem(sharedPreferences.getInt("metric_4", 3), true);
        selectItem(sharedPreferences.getInt("metric_5", 4), true);
        selectItem(sharedPreferences.getInt("metric_6", 5), true);
    }



    @Override
    protected void onPause() {
        super.onPause();

        saveChosenMetrics();
    }

    void saveChosenMetrics() {
        ArrayList<Integer> chosenMetricPositions = new ArrayList<>(MAX_NUMBER_TO_SELECT);

        //Add all chosen metrics
        for (int n = 0; n < metricAdapterObjects.length; n++) {
            if (metricAdapterObjects[n].isSelected) {
                chosenMetricPositions.add(n);
                if (chosenMetricPositions.size() >= MAX_NUMBER_TO_SELECT) {
                    break;
                }
            }
        }

        //fill remaining slots
        if (chosenMetricPositions.size() < MAX_NUMBER_TO_SELECT) {
            for (int n = 0; n < metricAdapterObjects.length; n++) {
                if (!chosenMetricPositions.contains(n)) {
                    chosenMetricPositions.add(n);
                    if (chosenMetricPositions.size() >= MAX_NUMBER_TO_SELECT) {
                        break;
                    }
                }
            }
        }

        //save metrics
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (int n = 0; n < chosenMetricPositions.size(); n++) {
            editor.putInt( String.format("metric_%d",n+1) , chosenMetricPositions.get(n)  );
        }

        //now save changes
        editor.commit();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        selectItem(position, !metricAdapterObjects[position].isSelected );

        mAdapter.notifyDataSetChanged();

    }

    void selectItem(int position, boolean isSelected) {
        boolean wasSelected = metricAdapterObjects[position].isSelected;
        metricAdapterObjects[position].isSelected = isSelected;

        if (!wasSelected && isSelected) {
            numberOfSelectedItems += 1;
        } else if (wasSelected && !isSelected) {
            numberOfSelectedItems -= 1;
        }

        if (numberOfSelectedItems < MAX_NUMBER_TO_SELECT) {
            metricChooserTextView.setTextColor(atOrUnderLimitColorCode);
            metricChooserTextView.setText(String.format("%d metrics chosen. Choose %d more.", numberOfSelectedItems,MAX_NUMBER_TO_SELECT - numberOfSelectedItems));
        } else if (numberOfSelectedItems == MAX_NUMBER_TO_SELECT) {
            metricChooserTextView.setTextColor(atOrUnderLimitColorCode);
            metricChooserTextView.setText(String.format("%d metrics chosen.", numberOfSelectedItems));
        } else {
            metricChooserTextView.setTextColor(redColorCode);
            metricChooserTextView.setText(String.format("%d metrics chosen. Please de-select %d", numberOfSelectedItems, numberOfSelectedItems - MAX_NUMBER_TO_SELECT));
        }

    }*/




    class MetricAdapterObject {
        private String name;
        private boolean isSelected;

        MetricAdapterObject(String name) {
            this.name = name;
            this.isSelected = false;
        }

        @Override
        public String toString() {
            return name;
        }
    }
/*
    class MetricChooserAdapter extends ArrayAdapter<MetricAdapterObject> {

        MetricChooserAdapter(MetricAdapterObject[] array) {
            super(MetricChooserActivity.this, R.layout.grid_item, R.id.label, array);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View gridItem = super.getView(position,convertView,parent);

            Resources res = getResources();
            String classifierName = MetricsManager.getMetricName(position).toLowerCase().replace(" ","_");
            int id = res.getIdentifier(classifierName, "drawable", getPackageName());
            ImageView image = (ImageView) gridItem.findViewById(R.id.grid_item_image_view);
            image.setImageResource(id);


            /*
            AnimationDrawable animationDrawable = new AnimationDrawable();
            animationDrawable.setOneShot(false);

            for (int n = 0; n < 5; n++) {
                int id = res.getIdentifier(String.format("smile_%d",n), "drawable", getPackageName());
                Drawable myDrawable;
                if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
                    myDrawable = getResources().getDrawable(id, getTheme());
                } else {
                    myDrawable = getResources().getDrawable(id);
                }

                animationDrawable.addFrame(myDrawable,150);
            }*/



            //image.setBackground(animationDrawable);
            //animationDrawable.start();
            /*

            TextView label = (TextView) gridItem.findViewById(R.id.label);
            ImageView checkmark = (ImageView) gridItem.findViewById(R.id.grid_item_chooser_checkbox);

            if (metricAdapterObjects[position].isSelected) {
                //RelativeLayout selectedCover = (RelativeLayout) gridItem.findViewById(R.id.metrics_chooser_selected_cover);
                checkmark.setVisibility(View.VISIBLE);

                if (numberOfSelectedItems > MAX_NUMBER_TO_SELECT) {
                    label.setBackgroundColor(chosenOverLimitColorCode);
                } else {
                    label.setBackgroundColor(chosenColorCode);
                }


            } else {
                //RelativeLayout selectedCover = (RelativeLayout) gridItem.findViewById(R.id.metrics_chooser_selected_cover);
                checkmark.setVisibility(View.INVISIBLE);

                label.setBackgroundColor(transparentBlackColorCode);
            }



            return gridItem;

        }

    }
            */

}
