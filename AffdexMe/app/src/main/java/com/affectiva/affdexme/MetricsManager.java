package com.affectiva.affdexme;

import java.util.HashMap;

/**
 * Created by Alan on 7/14/2015.
 */
public class MetricsManager {
    //Emotions
    static final int ANGER = 0;
    static final int CONTEMPT = 1;
    static final int DISGUST = 2;
    static final int ENGAGEMENT = 3;
    static final int FEAR = 4;
    static final int JOY = 5;
    static final int SADNESS = 6;
    static final int SURPRISE = 7;
    static final int VALENCE = 8;

    static final int EXPRESSIONS_START_INDEX = 9;

    //Expressions
    static final int ATTENTION = 9;
    static final int BROW_FURROW = 10;
    static final int BROW_RAISE = 11;
    static final int CHIN_RAISER = 12;
    static final int EYE_CLOSURE = 13;
    static final int INNER_BROW_RAISER = 14;
    static final int LIP_DEPRESSOR = 15;
    static final int LIP_PRESS = 16;
    static final int LIP_PUCKER = 17;
    static final int LIP_SUCK = 18;
    static final int MOUTH_OPEN = 19;
    static final int NOSE_WRINKLER = 20;
    static final int SMILE = 21;
    static final int SMIRK = 22;
    static final int UPPER_LIP_RAISER = 23;



    private static HashMap<Integer,String> metricNames;

    static {
        metricNames = new HashMap<Integer,String>();

        metricNames.put(ANGER,"anger");
        metricNames.put(CONTEMPT,"contempt");
        metricNames.put(DISGUST,"disgust");
        metricNames.put(ENGAGEMENT,"engagement");
        metricNames.put(FEAR,"fear");
        metricNames.put(JOY,"joy");
        metricNames.put(SADNESS,"sadness");
        metricNames.put(SURPRISE,"surprise");
        metricNames.put(VALENCE,"valence");

        metricNames.put(ATTENTION,"attention");
        metricNames.put(BROW_FURROW,"brow_furrow");
        metricNames.put(BROW_RAISE,"brow_raise");
        metricNames.put(CHIN_RAISER,"chin_raise");
        metricNames.put(EYE_CLOSURE,"eye_closure");
        metricNames.put(INNER_BROW_RAISER,"inner_brow_raise");
        metricNames.put(LIP_DEPRESSOR,"lip_depressor");
        metricNames.put(LIP_PRESS,"lip_press");
        metricNames.put(LIP_PUCKER,"lip_pucker");
        metricNames.put(LIP_SUCK,"lip_suck");
        metricNames.put(MOUTH_OPEN,"mouth_open");
        metricNames.put(NOSE_WRINKLER,"nose_wrinkler");
        metricNames.put(SMILE,"smile");
        metricNames.put(SMIRK,"smirk");
        metricNames.put(UPPER_LIP_RAISER,"upper_lip_raise");

    }

    static String getMetricName(int index){
        String toReturn = metricNames.get(index);
        if (toReturn != null) {
            return toReturn;
        } else {
            return "";
        }
    }

    static int getTotalNumEmotions() {
        return EXPRESSIONS_START_INDEX;
    }

    static  int getTotalNumExpressions() {
        return metricNames.size() - EXPRESSIONS_START_INDEX;
    }


}
