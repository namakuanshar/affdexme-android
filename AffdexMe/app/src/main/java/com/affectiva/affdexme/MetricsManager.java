package com.affectiva.affdexme;

import java.lang.StringBuilder;

/**
 * A class containing:
 *  -enumerations representing the Emotion and Expressions featured in the Affectiva SDK.
 *  -a Metric interface to allow easy iteration through all Expressions and Emotions
 *  -utility methods for converting a Metric into several types of strings
 */
public class MetricsManager {

    private static Metrics[] allMetrics;

    static {
        Emotions[] emotions = Emotions.values();
        Expressions[] expressions = Expressions.values();
        allMetrics = new Metrics[emotions.length + expressions.length];
        System.arraycopy(emotions,0,allMetrics,0,emotions.length);
        System.arraycopy(expressions,0,allMetrics,emotions.length,expressions.length);
    }

    static Metrics[] getAllMetrics() {
        return allMetrics;
    }

    enum MetricType {Emotion, Expression};

    interface Metrics {
        MetricType getType();
    }

    enum Emotions implements Metrics {
        ANGER,
        DISGUST,
        FEAR,
        JOY,
        SADNESS,
        SURPRISE,
        CONTEMPT,
        ENGAGEMENT,
        VALENCE;

        @Override
        public MetricType getType() {
            return MetricType.Emotion;
        }

    }

    enum Expressions implements Metrics {
        ATTENTION,
        BROW_FURROW,
        BROW_RAISE,
        CHIN_RAISE,
        EYE_CLOSURE,
        INNER_BROW_RAISE,
        LIP_CORNER_DEPRESSOR,
        LIP_PRESS,
        LIP_PUCKER,
        LIP_SUCK,
        MOUTH_OPEN,
        NOSE_WRINKLE,
        SMILE,
        SMIRK,
        UPPER_LIP_RAISE;

        @Override
        public MetricType getType() {
            return MetricType.Expression;
        }

    }

    //Used for displays
    static String getUpperCaseName(Metrics metric) {
        if (metric == Expressions.LIP_CORNER_DEPRESSOR) {
            return "FROWN";
        } else {
            return metric.toString().replace("_", " ");
        }
    }

    //Used for MetricSelectionFragment
    //This method is optimized for strings of the form SOME_METRIC_NAME, which all metric names currently are
    static String getCapitalizedName(Metrics metric) {
        if (metric == Expressions.LIP_CORNER_DEPRESSOR) {
            return "Frown";
        }
        String original = metric.toString();
        StringBuilder builder = new StringBuilder();
        boolean canBeLowerCase = false;
        for (int n = 0; n < original.length(); n++) {
            char c = original.charAt(n);
            if (c == '_') {
                builder.append(' ');
                canBeLowerCase = false;
            } else {
                if (canBeLowerCase) {
                    builder.append(Character.toLowerCase(c));
                } else {
                    builder.append(c);
                    canBeLowerCase = true;
                }
            }
        }
        return builder.toString();
    }

    //Used to load resource files
    static String getLowerCaseName(Metrics metric) {
        return metric.toString().toLowerCase();
    }

    //Used to construct method names for reflection
    static String getCamelCase(Metrics metric) {
        String metricString = metric.toString();
        
        StringBuilder builder = new StringBuilder();
        builder.append(Character.toUpperCase(metricString.charAt(0)));

        if (metricString.length() > 1) {
            for (int n = 1; n < metricString.length(); n++ ){
                char c = metricString.charAt(n);
                if (c == '_') {
                    n += 1;
                    if (n < metricString.length()) {
                        builder.append(metricString.charAt(n));
                    }
                } else {
                    builder.append(Character.toLowerCase(metricString.charAt(n)));
                }
            }
        }

        return builder.toString();
    }


}
