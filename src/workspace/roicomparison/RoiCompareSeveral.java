package workspace.roicomparison;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Plot;
import static ij.gui.Plot.CIRCLE;
import static ij.gui.PlotWindow.CROSS;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.text.TextWindow;
import java.awt.Color;
import java.util.ArrayList;

/**
 *
 * @author joheras
 */
public class RoiCompareSeveral {

    public static void compareseveralrois(ArrayList<Roi> rois1, ArrayList<Roi> roisb, ImagePlus imp, ResultsTable rt) {

        /// Variables to store the total values 
        double truepositive = 0;
        double falsepositive = 0;
        double truenegative = 0;
        double falsenegative = 0;

        /// The Roc Space
        double[] x = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};
        double[] y = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};

        Plot plot = new Plot("ROC space", "FPR or (1-specificity)", "TPR or sensitivity", x, y);
        plot.setColor(Color.BLUE);
        plot.draw();

        double[] tprs = new double[rois1.size()];
        double[] fprs = new double[rois1.size()];

        /// The table
        rt.reset();

        if (rt != null) {
            rt.reset();
        } else {
            rt = new ResultsTable();
        }

        for (int i = 0; i < rois1.size(); i++) {
            RoiCompare rc = new RoiCompare(rois1.get(i), roisb.get(i), IJ.getImage());
            rt.incrementCounter();
            rt.addValue("True ROI", rois1.get(i).getName());
            rt.addValue("Hypothesised ROI", roisb.get(i).getName());
            double tp = rc.truepositive();
            truepositive = truepositive + tp;
            double fp = rc.falsepositive();
            falsepositive = falsepositive + fp;
            double tn = rc.truenegative();
//            truenegative = truenegative + tn;
            double fn = rc.falsenegative();
            falsenegative = falsenegative + fn;
            rt.addValue("True positive", tp);
            rt.addValue("False positive", fp);
            rt.addValue("True negative", tn);
            rt.addValue("False negative", fn);
            rt.addValue("Positive", rc.positive());
            rt.addValue("Negative", rc.negative());
            rt.addValue("Accuracy", rc.accuracy());
            rt.addValue("Precision", rc.precision());
            rt.addValue("Recall", rc.recall());
            rt.addValue("Fallout", rc.fallout());
            double sens = rc.sensitivity();
            rt.addValue("Sensitivity", sens);
            double spec = rc.specifity();
            rt.addValue("Specifity", spec);
            rt.addValue("Negative predictive value", rc.negativepredictivevalue());
            rt.addValue("False discovery rate", rc.falsediscoveryrate());
            rt.addValue("False negative rate", rc.falsenegativerate());
            rt.addValue("LR+", rc.lrplus());
            rt.addValue("LR-", rc.lrminus());
            rt.addValue("F-measure alpha=0.5", rc.fmeasure(0.5));
            rt.addValue("F-measure alpha=1", rc.fmeasure(1));
            rt.addValue("F-measure alpha=2", rc.fmeasure(2));
            rt.addValue("Intersection over Union", rc.intersectionoverunion());
            rt.addValue("Fowlkes-Mallows index", rc.fowlkesmallows());
            rt.addValue("Matthews correlation coefficient", rc.matthewscorrelation());
            rt.addValue("Youden's J statistic", rc.youdenjstatistic());
            rt.addValue("Markedness", rc.markedness());
            rt.addValue("Diagnostic odds ratio", rc.diagnosticoddsratio());
            rt.addValue("Balanced accuracy", rc.balancedaccuracy());
            rt.addValue("Error rate", rc.errorrate());

            tprs[i] = sens;
            fprs[i] = 1 - spec;

        }

        truenegative = imp.getWidth() * imp.getHeight() - (truepositive + falsepositive + falsenegative);

        if (truenegative < 0) {
            truenegative = 0;
        }

        /// Drawing the points in the roc 
        plot.setColor(Color.RED);
        plot.addPoints(fprs, tprs, CIRCLE);
        for (int i = 0; i < fprs.length; i++) {
            plot.addLabel(fprs[i], 1 - tprs[i], "" + (i + 1));
        }

        /// Totals
        rt.incrementCounter();
        rt.setLabel("Total", rois1.size());
        rt.addValue("True ROI", "");
        rt.addValue("Hypothesised ROI", "");
        rt.addValue("True positive", truepositive);
        rt.addValue("False positive", falsepositive);
        rt.addValue("True negative", truenegative);
        rt.addValue("False negative", falsenegative);
        double positive = truepositive + falsenegative;
        rt.addValue("Positive", positive);
        double negative = falsepositive + truenegative;
        rt.addValue("Negative", negative);
        double accuracy = (truepositive + truenegative) / (positive + negative);
        rt.addValue("Accuracy", accuracy);
        double precision = (truepositive) / (truepositive + falsepositive);
        rt.addValue("Precision", precision);
        double recall = truepositive / (positive);
        rt.addValue("Recall", recall);
        double fallout = falsepositive / negative;
        rt.addValue("Fallout", fallout);
        double sensitivity = truepositive / (truepositive + falsenegative);
        rt.addValue("Sensitivity", sensitivity);
        double specificity = truenegative / (falsepositive + truenegative);
        rt.addValue("Specifity", specificity);
        double negativepredictivevalue = truenegative / (falsenegative + truenegative);
        rt.addValue("Negative predictive value", negativepredictivevalue);
        double falsediscoveryrate = 1 - precision;
        rt.addValue("False discovery rate", falsediscoveryrate);
        double falsenegativerate = falsenegative / positive;
        rt.addValue("False negative rate", falsenegativerate);
        double lrplus = sensitivity / (1 - specificity);
        rt.addValue("LR+", lrplus);
        double lrminus = (1 - sensitivity) / specificity;
        rt.addValue("LR-", lrminus);
        rt.addValue("F-measure alpha=0.5", ((1 + 0.5) * precision * recall) / (0.5 * precision + recall));
        rt.addValue("F-measure alpha=1", ((1 + 1) * precision * recall) / (1 * precision + recall));
        rt.addValue("F-measure alpha=2", ((1 + 2) * precision * recall) / (2 * precision + recall));
        double intersectionoverunion = truepositive / (truepositive + falsenegative + falsepositive);
        rt.addValue("Intersection over Union", intersectionoverunion);
        double fowlkesmallows = Math.sqrt(precision * recall);
        rt.addValue("Fowlkes-Mallows index", fowlkesmallows);
        double matthewscorrelation = ((truepositive * truenegative) - (falsepositive * falsenegative)) / Math.sqrt(positive * negative * (truepositive + falsepositive) * (falsenegative + truenegative));
        rt.addValue("Matthews correlation coefficient", matthewscorrelation);
        double youdenjstatistic = sensitivity + specificity - 1;
        rt.addValue("Youden's J statistic", youdenjstatistic);
        double markedness = precision + negativepredictivevalue - 1;
        rt.addValue("Markedness", markedness);
        double diagnosticoddsratio = lrplus / lrminus;
        rt.addValue("Diagnostic odds ratio", diagnosticoddsratio);
        double balancedaccuracy = (sensitivity + specificity) / 2;
        rt.addValue("Balanced accuracy", balancedaccuracy);
        double errorrate = (falsepositive + falsenegative) / (positive + negative);
        rt.addValue("Error rate", errorrate);

        rt.show("Measurements");

//        Frame f = WindowManager.getFrame("Measurements");
//        f.setAlwaysOnTop(true);
        double[] totaltprs = {sensitivity};
        double[] totalfprs = {1 - specificity};
        plot.setColor(Color.BLACK);
        plot.addPoints(totalfprs, totaltprs, CROSS);

        plot.show();
//        plot.getImagePlus().getWindow().toFront();
    }

    public static void compareseveralroisagainstone(ArrayList<Roi> rois1, ArrayList<Roi> roisb, ImagePlus imp, ResultsTable rt) {

        /// Variables to store the total values 
        double truepositive = 0;
        double falsepositive = 0;
        double truenegative = 0;
        double falsenegative = 0;

        /// The Roc Space
        double[] x = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};
        double[] y = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};

        Plot plot = new Plot("ROC space", "FPR or (1-specificity)", "TPR or sensitivity", x, y);
        plot.setColor(Color.BLUE);
        plot.draw();

        double[] tprs = new double[rois1.size()];
        double[] fprs = new double[rois1.size()];

        /// The table
        rt.reset();

        if (rt != null) {
            rt.reset();
        } else {
            rt = new ResultsTable();
        }

        for (int i = 0; i < rois1.size(); i++) {
            RoiCompare rc = new RoiCompare(rois1.get(i), roisb.get(i), IJ.getImage());
            rt.incrementCounter();
            rt.addValue("True ROI", rois1.get(i).getName());
            rt.addValue("Hypothesised ROI", roisb.get(i).getName());
            double tp = rc.truepositive();
            truepositive = truepositive + tp;
            double fp = rc.falsepositive();
            falsepositive = falsepositive + fp;
            double tn = rc.truenegative();
            truenegative = truenegative + tn;
            double fn = rc.falsenegative();
            falsenegative = falsenegative + fn;
            rt.addValue("True positive", tp);
            rt.addValue("False positive", fp);
            rt.addValue("True negative", tn);
            rt.addValue("False negative", fn);
            rt.addValue("Positive", rc.positive());
            rt.addValue("Negative", rc.negative());
            rt.addValue("Accuracy", rc.accuracy());
            rt.addValue("Precision", rc.precision());
            rt.addValue("Recall", rc.recall());
            rt.addValue("Fallout", rc.fallout());
            double sens = rc.sensitivity();
            rt.addValue("Sensitivity", sens);
            double spec = rc.specifity();
            rt.addValue("Specifity", spec);
            rt.addValue("Negative predictive value", rc.negativepredictivevalue());
            rt.addValue("False discovery rate", rc.falsediscoveryrate());
            rt.addValue("False negative rate", rc.falsenegativerate());
            rt.addValue("LR+", rc.lrplus());
            rt.addValue("LR-", rc.lrminus());
            rt.addValue("F-measure alpha=0.5", rc.fmeasure(0.5));
            rt.addValue("F-measure alpha=1", rc.fmeasure(1));
            rt.addValue("F-measure alpha=2", rc.fmeasure(2));
            rt.addValue("Intersection over Union", rc.intersectionoverunion());
            rt.addValue("Fowlkes-Mallows index", rc.fowlkesmallows());
            rt.addValue("Matthews correlation coefficient", rc.matthewscorrelation());
            rt.addValue("Youden's J statistic", rc.youdenjstatistic());
            rt.addValue("Markedness", rc.markedness());
            rt.addValue("Diagnostic odds ratio", rc.diagnosticoddsratio());
            rt.addValue("Balanced accuracy", rc.balancedaccuracy());
            rt.addValue("Error rate", rc.errorrate());

            tprs[i] = sens;
            fprs[i] = 1 - spec;

        }

        /// Drawing the points in the roc 
        plot.setColor(Color.RED);
        plot.addPoints(fprs, tprs, CIRCLE);

        for (int i = 0; i < fprs.length; i++) {
            plot.addLabel(fprs[i], 1 - tprs[i], "" + (i + 1));
        }

        rt.show("Measurements");
//              Frame f = WindowManager.getFrame("Measurements");
//        f.setAlwaysOnTop(true);

        plot.show();
//        plot.getImagePlus().getWindow().toFront();

    }

    private static double roiarea(Roi r, ImagePlus imp) {
        double res = 0;
        for (int i = 0; i < imp.getWidth(); i++) {
            for (int j = 0; j < imp.getHeight(); j++) {

                if (r.contains(i, j)) {
                    res = res + 1;
                }

            }
        }
        return res;

    }

    public static void compareseveralrois(ArrayList<Roi> rois1, ArrayList<Roi> roisb, ArrayList<Roi> notincluded, ImagePlus imp) {

        /// Variables to store the total values 
        double truepositive = 0;
        double falsepositive = 0;
        double truenegative = 0;
        double falsenegative = 0;

        /// The Roc Space
        double[] x = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};
        double[] y = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};

        Plot plot = new Plot("ROC space", "FPR or (1-specificity)", "TPR or sensitivity", x, y);
        plot.setColor(Color.BLUE);
        plot.draw();

        double[] tprs = new double[rois1.size()];
        double[] fprs = new double[rois1.size()];

        /// The table
        ArrayList list = new ArrayList();
        String headings = "Index\tLabel\tTrue ROI\tHypothesised ROI\tTrue positive\tFalse Positive\tTrue negative\tFalse negative\tPositive\tNegative\tAccuracy\tPrecision\tRecall\tFallout\tSensitivity\tSpecifity\tNegative predictive value\tFalse discovery rate\tFalse negative rate\tLR+\tLR-\tF-measure alpha=0.5\tF-measure alpha=1\tF-measure alpha=2\tIntersection over Union\tFowlkes-Mallows index\tMatthews correlation coefficient\tYouden's J statistic\tMarkedness\tDiagnostic odds ratio\tBalanced accuracy\tError rate";

        for (int i = 0; i < rois1.size(); i++) {
            RoiCompare rc = new RoiCompare(rois1.get(i), roisb.get(i), IJ.getImage());

            double tp = rc.truepositive();
            truepositive = truepositive + tp;
            double fp = rc.falsepositive();
            falsepositive = falsepositive + fp;
            double tn = rc.truenegative();
            truenegative = truenegative + tn;
            double fn = rc.falsenegative();
            falsenegative = falsenegative + fn;
            double sens = rc.sensitivity();
            double spec = rc.specifity();
            list.add((i + 1) + "\t \t" + rois1.get(i).getName() + "\t" + roisb.get(i).getName()
                    + "\t" + tp + "\t" + fp + "\t" + tn + "\t" + fn + "\t"
                    + rc.positive() + "\t" + rc.negative() + "\t" + rc.accuracy() + "\t"
                    + rc.precision() + "\t" + rc.recall() + "\t" + rc.fallout() + "\t"
                    + sens + "\t" + spec + "\t" + rc.negativepredictivevalue() + "\t"
                    + rc.falsediscoveryrate() + "\t" + rc.falsenegativerate() + "\t"
                    + rc.lrplus() + "\t" + rc.lrminus() + "\t" + rc.fmeasure(0.5) + "\t"
                    + rc.fmeasure(1) + "\t" + rc.fmeasure(2) + "\t" + rc.intersectionoverunion() + "\t" + 
                    rc.fowlkesmallows()+ "\t"+rc.matthewscorrelation()+ "\t"+
                    rc.youdenjstatistic()+ "\t"+rc.markedness()+ "\t"+rc.diagnosticoddsratio()+ "\t"+
                    rc.balancedaccuracy()+ "\t"+rc.errorrate());

            tprs[i] = sens;
            fprs[i] = 1 - spec;

        }

        /// Drawing the points in the roc 
        plot.setColor(Color.RED);
        plot.addPoints(fprs, tprs, CIRCLE);
        for (int i = 0; i < fprs.length; i++) {
            plot.addLabel(fprs[i], 1 - tprs[i], "" + (i + 1));
        }

        /// Adding true rois that were not detected as false negative
        for (Roi r : notincluded) {
            falsenegative = falsenegative + roiarea(r, imp);
        }

        truenegative = imp.getWidth() * imp.getHeight() - (truepositive + falsepositive + falsenegative);

        if (truenegative < 0) {
            truenegative = 0;
        }
        /// Totals
        double positive = truepositive + falsenegative;
        double negative = falsepositive + truenegative;
        double accuracy = (truepositive + truenegative) / (positive + negative);
        double precision = (truepositive) / (truepositive + falsepositive);
        double recall = truepositive / (positive);
        double fallout = falsepositive / negative;
        double sensitivity = truepositive / (truepositive + falsenegative);
        double specificity = truenegative / (falsepositive + truenegative);
        double negativepredictivevalue = truenegative / (falsenegative + truenegative);
        double falsediscoveryrate = 1 - precision;
        double falsenegativerate = falsenegative / positive;
        double lrplus = sensitivity / (1 - specificity);
        double lrminus = (1 - sensitivity) / specificity;
        double alpha05 = ((1 + 0.5) * precision * recall) / (0.5 * precision + recall);
        double alpha1 = ((1 + 1) * precision * recall) / (1 * precision + recall);
        double alpha2 = ((1 + 2) * precision * recall) / (2 * precision + recall);
        double intersectionoverunion = truepositive / (truepositive + falsenegative + falsepositive);
        double fowlkesmallows = Math.sqrt(precision * recall);
        double matthewscorrelation = ((truepositive * truenegative) - (falsepositive * falsenegative)) / Math.sqrt(positive * negative * (truepositive + falsepositive) * (falsenegative + truenegative));
        double youdenjstatistic = sensitivity + specificity - 1;
        double markedness = precision + negativepredictivevalue - 1;
        double diagnosticoddsratio = lrplus / lrminus;
        double balancedaccuracy = (sensitivity + specificity) / 2;
        double errorrate = (falsepositive + falsenegative) / (positive + negative);
        list.add(rois1.size() + 1 + "\t Total \t \t \t" + truepositive + "\t" + falsepositive + "\t"
                + truenegative + "\t" + falsenegative + "\t" + positive + "\t" + negative + "\t" + accuracy
                + "\t" + precision + "\t" + recall + "\t" + fallout + "\t" + sensitivity + "\t" + specificity
                + "\t" + negativepredictivevalue + "\t" + falsediscoveryrate + "\t" + falsenegativerate + "\t"
                + lrplus + "\t" + lrminus + "\t" + alpha05 + "\t" + alpha1 + "\t" + alpha2+ "\t" +
                intersectionoverunion+ "\t" +fowlkesmallows+ "\t" +matthewscorrelation+ "\t" +
                youdenjstatistic+ "\t" +markedness+ "\t" +diagnosticoddsratio+ "\t" +
                balancedaccuracy+ "\t" +errorrate);

        TextWindow textWindow = new TextWindow("Measurements", headings, list, 600, 400);

//        Frame f = WindowManager.getFrame("Measurements");
//        f.setAlwaysOnTop(true);
        double[] totaltprs = {sensitivity};
        double[] totalfprs = {1 - specificity};
        plot.setColor(Color.BLACK);
        plot.addPoints(totalfprs, totaltprs, CROSS);

        plot.show();
//        plot.getImagePlus().getWindow().toFront();
    }

    public static void compareseveralrois(ArrayList<Roi> rois1, ArrayList<Roi> roisb, ArrayList<Roi> notincluded, ImagePlus imp, Plot plot, ArrayList list) {

        /// Variables to store the total values 
        double truepositive = 0;
        double falsepositive = 0;
        double truenegative = 0;
        double falsenegative = 0;

        /// The Roc Space
        double[] x = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};
        double[] y = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};

        double[] tprs = new double[rois1.size()];
        double[] fprs = new double[rois1.size()];

        /// The table
        String headings = "Index\tLabel\tTrue ROI\tHypothesised ROI\tTrue positive\tFalse Positive\tTrue negative\tFalse negative\tPositive\tNegative\tAccuracy\tPrecision\tRecall\tFallout\tSensitivity\tSpecifity\tNegative predictive value\tFalse discovery rate\tFalse negative rate\tLR+\tLR-\tF-measure alpha=0.5\tF-measure alpha=1\tF-measure alpha=2\tIntersection over Union\tFowlkes-Mallows index\tMatthews correlation coefficient\tYouden's J statistic\tMarkedness\tDiagnostic odds ratio\tBalanced accuracy\tError rate";

        for (int i = 0; i < rois1.size(); i++) {
            RoiCompare rc = new RoiCompare(rois1.get(i), roisb.get(i), IJ.getImage());

            double tp = rc.truepositive();
            truepositive = truepositive + tp;
            double fp = rc.falsepositive();
            falsepositive = falsepositive + fp;
            double tn = rc.truenegative();
            truenegative = truenegative + tn;
            double fn = rc.falsenegative();
            falsenegative = falsenegative + fn;
            double sens = rc.sensitivity();
            double spec = rc.specifity();
            list.add((i + 1) + "\t \t" + rois1.get(i).getName() + "\t" + roisb.get(i).getName()
                    + "\t" + tp + "\t" + fp + "\t" + tn + "\t" + fn + "\t"
                    + rc.positive() + "\t" + rc.negative() + "\t" + rc.accuracy() + "\t"
                    + rc.precision() + "\t" + rc.recall() + "\t" + rc.fallout() + "\t"
                    + sens + "\t" + spec + "\t" + rc.negativepredictivevalue() + "\t"
                    + rc.falsediscoveryrate() + "\t" + rc.falsenegativerate() + "\t"
                    + rc.lrplus() + "\t" + rc.lrminus() + "\t" + rc.fmeasure(0.5) + "\t"
                    + rc.fmeasure(1) + "\t" + rc.fmeasure(2)+ "\t" + rc.intersectionoverunion() + "\t" + 
                    rc.fowlkesmallows()+ "\t"+rc.matthewscorrelation()+ "\t"+
                    rc.youdenjstatistic()+ "\t"+rc.markedness()+ "\t"+rc.diagnosticoddsratio()+ "\t"+
                    rc.balancedaccuracy()+ "\t"+rc.errorrate());

            tprs[i] = sens;
            fprs[i] = 1 - spec;

        }

        truenegative = imp.getWidth() * imp.getHeight() - (truepositive + falsepositive + falsenegative);

        if (truenegative < 0) {
            truenegative = 0;
        }
        /// Drawing the points in the roc 
        plot.setColor(Color.RED);
        plot.addPoints(fprs, tprs, CIRCLE);
//        for (int i = 0; i < fprs.length; i++) {
//            plot.addLabel(fprs[i], 1 - tprs[i], "" + (i + 1));
//        }

        /// Adding true rois that were not detected as false negative
        for (Roi r : notincluded) {
            falsenegative = falsenegative + roiarea(r, imp);
        }

        /// Totals
        double positive = truepositive + falsenegative;
        double negative = falsepositive + truenegative;
        double accuracy = (truepositive + truenegative) / (positive + negative);
        double precision = (truepositive) / (truepositive + falsepositive);
        double recall = truepositive / (positive);
        double fallout = falsepositive / negative;
        double sensitivity = truepositive / (truepositive + falsenegative);
        double specificity = truenegative / (falsepositive + truenegative);
        double negativepredictivevalue = truenegative / (falsenegative + truenegative);
        double falsediscoveryrate = 1 - precision;
        double falsenegativerate = falsenegative / positive;
        double lrplus = sensitivity / (1 - specificity);
        double lrminus = (1 - sensitivity) / specificity;
        double alpha05 = ((1 + 0.5) * precision * recall) / (0.5 * precision + recall);
        double alpha1 = ((1 + 1) * precision * recall) / (1 * precision + recall);
        double alpha2 = ((1 + 2) * precision * recall) / (2 * precision + recall);
        double intersectionoverunion = truepositive / (truepositive + falsenegative + falsepositive);
        double fowlkesmallows = Math.sqrt(precision * recall);
        double matthewscorrelation = ((truepositive * truenegative) - (falsepositive * falsenegative)) / Math.sqrt(positive * negative * (truepositive + falsepositive) * (falsenegative + truenegative));
        double youdenjstatistic = sensitivity + specificity - 1;
        double markedness = precision + negativepredictivevalue - 1;
        double diagnosticoddsratio = lrplus / lrminus;
        double balancedaccuracy = (sensitivity + specificity) / 2;
        double errorrate = (falsepositive + falsenegative) / (positive + negative);
        list.add(rois1.size() + 1 + "\t Total \t \t \t" + truepositive + "\t" + falsepositive + "\t"
                + truenegative + "\t" + falsenegative + "\t" + positive + "\t" + negative + "\t" + accuracy
                + "\t" + precision + "\t" + recall + "\t" + fallout + "\t" + sensitivity + "\t" + specificity
                + "\t" + negativepredictivevalue + "\t" + falsediscoveryrate + "\t" + falsenegativerate + "\t"
                + lrplus + "\t" + lrminus + "\t" + alpha05 + "\t" + alpha1 + "\t" + alpha2+ "\t" +
                intersectionoverunion+ "\t" +fowlkesmallows+ "\t" +matthewscorrelation+ "\t" +
                youdenjstatistic+ "\t" +markedness+ "\t" +diagnosticoddsratio+ "\t" +
                balancedaccuracy+ "\t" +errorrate);

        TextWindow textWindow = new TextWindow("Measurements", headings, list, 600, 400);

//        Frame f = WindowManager.getFrame("Measurements");
//        f.setAlwaysOnTop(true);
        double[] totaltprs = {sensitivity};
        double[] totalfprs = {1 - specificity};
        plot.setColor(Color.BLACK);
        plot.addPoints(totalfprs, totaltprs, CROSS);

        plot.show();
//        plot.getImagePlus().getWindow().toFront();
    }

    public static void compareseveralroisagainstone(ArrayList<Roi> rois1, ArrayList<Roi> roisb, ImagePlus imp) {

        /// Variables to store the total values 
        double truepositive = 0;
        double falsepositive = 0;
        double truenegative = 0;
        double falsenegative = 0;

        /// The Roc Space
        double[] x = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};
        double[] y = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};

        Plot plot = new Plot("ROC space", "FPR or (1-specificity)", "TPR or sensitivity", x, y);
        plot.setColor(Color.BLUE);
        plot.draw();

        double[] tprs = new double[rois1.size()];
        double[] fprs = new double[rois1.size()];

        ArrayList list = new ArrayList();
        String headings = "Index\tLabel\tTrue ROI\tHypothesised ROI\tTrue positive\tFalse Positive\tTrue negative\tFalse negative\tPositive\tNegative\tAccuracy\tPrecision\tRecall\tFallout\tSensitivity\tSpecifity\tNegative predictive value\tFalse discovery rate\tFalse negative rate\tLR+\tLR-\tF-measure alpha=0.5\tF-measure alpha=1\tF-measure alpha=2\tIntersection over Union\tFowlkes-Mallows index\tMatthews correlation coefficient\tYouden's J statistic\tMarkedness\tDiagnostic odds ratio\tBalanced accuracy\tError rate";

        for (int i = 0; i < rois1.size(); i++) {
            RoiCompare rc = new RoiCompare(rois1.get(i), roisb.get(i), IJ.getImage());

            double tp = rc.truepositive();
            truepositive = truepositive + tp;
            double fp = rc.falsepositive();
            falsepositive = falsepositive + fp;
            double tn = rc.truenegative();
            truenegative = truenegative + tn;
            double fn = rc.falsenegative();
            falsenegative = falsenegative + fn;
            double sens = rc.sensitivity();
            double spec = rc.specifity();
            list.add((i + 1) + "\t \t" + rois1.get(i).getName() + "\t" + roisb.get(i).getName()
                    + "\t" + tp + "\t" + fp + "\t" + tn + "\t" + fn + "\t"
                    + rc.positive() + "\t" + rc.negative() + "\t" + rc.accuracy() + "\t"
                    + rc.precision() + "\t" + rc.recall() + "\t" + rc.fallout() + "\t"
                    + sens + "\t" + spec + "\t" + rc.negativepredictivevalue() + "\t"
                    + rc.falsediscoveryrate() + "\t" + rc.falsenegativerate() + "\t"
                    + rc.lrplus() + "\t" + rc.lrminus() + "\t" + rc.fmeasure(0.5) + "\t"
                    + rc.fmeasure(1) + "\t" + rc.fmeasure(2)+ "\t" + rc.intersectionoverunion() + "\t" + 
                    rc.fowlkesmallows()+ "\t"+rc.matthewscorrelation()+ "\t"+
                    rc.youdenjstatistic()+ "\t"+rc.markedness()+ "\t"+rc.diagnosticoddsratio()+ "\t"+
                    rc.balancedaccuracy()+ "\t"+rc.errorrate());

            tprs[i] = sens;
            fprs[i] = 1 - spec;

        }

        /// Drawing the points in the roc 
        plot.setColor(Color.RED);
        plot.addPoints(fprs, tprs, CIRCLE);

        for (int i = 0; i < fprs.length; i++) {
            plot.addLabel(fprs[i], 1 - tprs[i], "" + (i + 1));
        }

        TextWindow textWindow = new TextWindow("Measurements", headings, list, 600, 400);
//              Frame f = WindowManager.getFrame("Measurements");
//        f.setAlwaysOnTop(true);

        plot.show();
//        plot.getImagePlus().getWindow().toFront();

    }

    public static void compareseveralroisagainstone(ArrayList<Roi> rois1, ArrayList<Roi> roisb, ImagePlus imp, Plot plot, ArrayList list) {

        /// Variables to store the total values 
        double truepositive = 0;
        double falsepositive = 0;
        double truenegative = 0;
        double falsenegative = 0;

        /// The Roc Space
        double[] x = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};
        double[] y = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};

        double[] tprs = new double[rois1.size()];
        double[] fprs = new double[rois1.size()];

        String headings = "Index\tLabel\tTrue ROI\tHypothesised ROI\tTrue positive\tFalse Positive\tTrue negative\tFalse negative\tPositive\tNegative\tAccuracy\tPrecision\tRecall\tFallout\tSensitivity\tSpecifity\tNegative predictive value\tFalse discovery rate\tFalse negative rate\tLR+\tLR-\tF-measure alpha=0.5\tF-measure alpha=1\tF-measure alpha=2\tIntersection over Union\tFowlkes-Mallows index\tMatthews correlation coefficient\tYouden's J statistic\tMarkedness\tDiagnostic odds ratio\tBalanced accuracy\tError rate";

        for (int i = 0; i < rois1.size(); i++) {
            RoiCompare rc = new RoiCompare(rois1.get(i), roisb.get(i), IJ.getImage());

            double tp = rc.truepositive();
            truepositive = truepositive + tp;
            double fp = rc.falsepositive();
            falsepositive = falsepositive + fp;
            double tn = rc.truenegative();
            truenegative = truenegative + tn;
            double fn = rc.falsenegative();
            falsenegative = falsenegative + fn;
            double sens = rc.sensitivity();
            double spec = rc.specifity();
            list.add((i + 1) + "\t \t" + rois1.get(i).getName() + "\t" + roisb.get(i).getName()
                    + "\t" + tp + "\t" + fp + "\t" + tn + "\t" + fn + "\t"
                    + rc.positive() + "\t" + rc.negative() + "\t" + rc.accuracy() + "\t"
                    + rc.precision() + "\t" + rc.recall() + "\t" + rc.fallout() + "\t"
                    + sens + "\t" + spec + "\t" + rc.negativepredictivevalue() + "\t"
                    + rc.falsediscoveryrate() + "\t" + rc.falsenegativerate() + "\t"
                    + rc.lrplus() + "\t" + rc.lrminus() + "\t" + rc.fmeasure(0.5) + "\t"
                    + rc.fmeasure(1) + "\t" + rc.fmeasure(2)+ "\t" + rc.intersectionoverunion() + "\t" + 
                    rc.fowlkesmallows()+ "\t"+rc.matthewscorrelation()+ "\t"+
                    rc.youdenjstatistic()+ "\t"+rc.markedness()+ "\t"+rc.diagnosticoddsratio()+ "\t"+
                    rc.balancedaccuracy()+ "\t"+rc.errorrate());

            tprs[i] = sens;
            fprs[i] = 1 - spec;

        }

        /// Drawing the points in the roc 
        plot.setColor(Color.RED);
        plot.addPoints(fprs, tprs, CIRCLE);

//        for (int i = 0; i < fprs.length; i++) {
//            plot.addLabel(fprs[i], 1 - tprs[i], "" + (i + 1));
//        }
//        TextWindow textWindow = new TextWindow("Measurements", headings, list, 600, 400);
//              Frame f = WindowManager.getFrame("Measurements");
//        f.setAlwaysOnTop(true);
//        plot.show();
//        plot.getImagePlus().getWindow().toFront();
    }

    public static void compareseveralroisagainstone(ArrayList<Roi> rois1, ArrayList<Roi> roisb, ImagePlus imp, Plot plot, ArrayList list, String method, String author, Color c) {

        /// Variables to store the total values 
        double truepositive = 0;
        double falsepositive = 0;
        double truenegative = 0;
        double falsenegative = 0;

        /// The Roc Space
        double[] x = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};
        double[] y = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};

        double[] tprs = new double[rois1.size()];
        double[] fprs = new double[rois1.size()];

        for (int i = 0; i < rois1.size(); i++) {
            RoiCompare rc = new RoiCompare(rois1.get(i), roisb.get(i), IJ.getImage());

            double tp = rc.truepositive();
            truepositive = truepositive + tp;
            double fp = rc.falsepositive();
            falsepositive = falsepositive + fp;
            double tn = rc.truenegative();
            truenegative = truenegative + tn;
            double fn = rc.falsenegative();
            falsenegative = falsenegative + fn;
            double sens = rc.sensitivity();
            double spec = rc.specifity();
            list.add(author + "\t" + method
                    + "\t" + tp + "\t" + fp + "\t" + tn + "\t" + fn + "\t"
                    + rc.positive() + "\t" + rc.negative() + "\t" + rc.accuracy() + "\t"
                    + rc.precision() + "\t" + rc.recall() + "\t" + rc.fallout() + "\t"
                    + sens + "\t" + spec + "\t" + rc.negativepredictivevalue() + "\t"
                    + rc.falsediscoveryrate() + "\t" + rc.falsenegativerate() + "\t"
                    + rc.lrplus() + "\t" + rc.lrminus() + "\t" + rc.fmeasure(0.5) + "\t"
                    + rc.fmeasure(1) + "\t" + rc.fmeasure(2)+ "\t" + rc.intersectionoverunion() + "\t" + 
                    rc.fowlkesmallows()+ "\t"+rc.matthewscorrelation()+ "\t"+
                    rc.youdenjstatistic()+ "\t"+rc.markedness()+ "\t"+rc.diagnosticoddsratio()+ "\t"+
                    rc.balancedaccuracy()+ "\t"+rc.errorrate());

            tprs[i] = sens;
            fprs[i] = 1 - spec;

        }

        /// Drawing the points in the roc 
        plot.setColor(c);
        plot.addPoints(fprs, tprs, CIRCLE);

//        for (int i = 0; i < fprs.length; i++) {
//            plot.addLabel(fprs[i], 1 - tprs[i], "" + (i + 1));
//        }
//        TextWindow textWindow = new TextWindow("Measurements", headings, list, 600, 400);
//              Frame f = WindowManager.getFrame("Measurements");
//        f.setAlwaysOnTop(true);
//        plot.show();
//        plot.getImagePlus().getWindow().toFront();
    }

}
