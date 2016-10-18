/**
 * Confusion matrix: | Pos | Neg | ------------------- Yes | TP | FP |
 * ------------------- No | FN | TN | -------------------
 *
 * TP = True positive FP = False positive FN = False negative TN = True negative
 * P = TP + FN N = FP + TN
 */
package workspace.roicomparison;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Plot;
import static ij.gui.Plot.CIRCLE;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.measure.ResultsTable;
import java.awt.Color;
import java.util.ArrayList;

/**
 *
 * @author joheras
 */
public class RoiCompare {

    private Roi trueroi;
    private Roi hypothesisroi;
    private ImagePlus imp;

    double truepositivepixels = -1;

    private ArrayList<point> insidetrueroi = new ArrayList<point>();
    private ArrayList<point> outsidetrueroi = new ArrayList<point>();
    private ArrayList<point> insidehypothesisroi = new ArrayList<point>();
    private ArrayList<point> outsidehypothesisroi = new ArrayList<point>();

    public RoiCompare(Roi trueroi, Roi hypothesisroi, ImagePlus imp) {
        this.trueroi = trueroi;
        this.hypothesisroi = hypothesisroi;
        this.imp = imp;

        if (!trueroi.isArea()) {
            IJ.error("True rois must have an area.");
            return;
        }

        for (int i = 0; i < imp.getWidth(); i++) {
            for (int j = 0; j < imp.getHeight(); j++) {
                if (trueroi.contains(i, j)) {
                    insidetrueroi.add(new point(i, j));
                } else {
                    outsidetrueroi.add(new point(i, j));
                }

                if (hypothesisroi.contains(i, j)) {
                    insidehypothesisroi.add(new point(i, j));
                } else {
                    outsidehypothesisroi.add(new point(i, j));
                }
            }
        }

    }

    /// In all these functions, the first roi is the true result, and
    /// the second roi is the hypothesised roi.
    public double truepositive() {

        if (truepositivepixels == -1) {

            ShapeRoi s1 = null, s2 = null;
            if (trueroi instanceof ShapeRoi) {
                s1 = (ShapeRoi) trueroi.clone();
            } else {
                s1 = new ShapeRoi(trueroi);
            }
            if (hypothesisroi instanceof ShapeRoi) {
                s2 = (ShapeRoi) hypothesisroi.clone();
            } else {
                s2 = new ShapeRoi(hypothesisroi);
            }
            ShapeRoi and = s1.and(s2);

            imp.deleteRoi();
            imp.setRoi(and);

            ImagePlus imp2 = imp.duplicate();
            and.setLocation(0, 0);
            imp2.setRoi(and);

            double k = 0;
            for (int i = 0; i < imp2.getWidth(); i++) {
                for (int j = 0; j < imp2.getHeight(); j++) {
                    if (and.contains(i, j)) {
                        k = k + 1;
                    }
                }
            }
            truepositivepixels = k;
            imp2.close();
            imp.deleteRoi();
            return k;

        } else {
            return truepositivepixels;
        }
    }

    public double falsepositive() {
        if (truepositivepixels == -1) {
            truepositive();
        }
        double i = insidehypothesisroi.size() - truepositivepixels;
        return i;
    }

    public double truenegative() {
        if (truepositivepixels == -1) {
            truepositive();
        }
        double i = outsidetrueroi.size() - insidehypothesisroi.size() + truepositivepixels;
        return i;
    }

    public double falsenegative() {
        if (truepositivepixels == -1) {
            truepositive();
        }
        double i = insidetrueroi.size() - truepositivepixels;
        return i;
    }

    public double positive() {
        return truepositive() + falsenegative();
    }

    public double negative() {
        return falsepositive() + truenegative();
    }

    /* Multi-class focus measures. */
    // Accuracy = (TP+TN)/(P+N)
    public double accuracy() {
        return (truepositive() + truenegative()) / (positive() + negative());
    }

    /* single-class focus measures. */
    // Precision = TP/(TP+FP) --- also known as positive predictive value (PPV)
    public double precision() {
        return (truepositive()) / (truepositive() + falsepositive());
    }

    // Recall = TP/P --- also known as sensitivity, true positive rate (TPR)
    public double recall() {
        return truepositive() / (positive());
    }

    // Fallout = FP/N --- also known as false positive rate (FPR)
    public double fallout() {
        return falsepositive() / negative();
    }

    // Sensitivity = TP/(TP+FN)
    public double sensitivity() {
        return truepositive() / (truepositive() + falsenegative());
    }

    // Specificity (SPC) = TN/(FP+TN) --- also known as true negative rate
    public double specifity() {
        return truenegative() / (falsepositive() + truenegative());
    }

    // Negative predictive value (NPV) = TN/(FN+TN)
    public double negativepredictivevalue() {
        return truenegative() / (falsenegative() + truenegative());
    }

    // False discovery rate (FDR) = 1-PPV
    public double falsediscoveryrate() {
        return 1 - precision();
    }

    // Miss rate or false negative rate (FNR) = FN/P
    public double falsenegativerate() {
        return falsenegative() / positive();
    }


    /* Likelihoods ratios */
    // lr+ = sensitivity/(1-specifity)
    public double lrplus() {
        return sensitivity() / (1 - specifity());
    }

    // lr- = (1-sensitivity)/specifity
    public double lrminus() {
        return (1 - sensitivity()) / specifity();
    }

    /* Compouded measures */
    // F-measure, alpha = 1, 2, 0.5
    public double fmeasure(double alpha) {
        return ((1 + alpha) * precision() * recall()) / (alpha * precision() + recall());
    }

    /* Summary */
    public void measurements() {
        ResultsTable rt = new ResultsTable();
        rt.incrementCounter();
        rt.addValue("True positive", truepositive());
        rt.addValue("False positive", falsepositive());
        rt.addValue("True negative", truenegative());
        rt.addValue("False negative", falsenegative());
        rt.addValue("Positive", positive());
        rt.addValue("Negative", negative());
        rt.addValue("Accuracy", accuracy());
        rt.addValue("Precision", precision());
        rt.addValue("Recall", recall());
        rt.addValue("Fallout", fallout());
        rt.addValue("Sensitivity", sensitivity());
        rt.addValue("Specifity", specifity());
        rt.addValue("Negative predictive value", negativepredictivevalue());
        rt.addValue("False discovery rate", falsediscoveryrate());
        rt.addValue("False negative rate", falsenegativerate());
        rt.addValue("LR+", lrplus());
        rt.addValue("LR-", lrminus());
        rt.addValue("F-measure alpha=0.5", fmeasure(0.5));
        rt.addValue("F-measure alpha=1", fmeasure(1));
        rt.addValue("F-measure alpha=2", fmeasure(2));
        rt.show("Measurements");

        ArrayList<Roi> rois1 = new ArrayList<>();
        ArrayList<Roi> rois2 = new ArrayList<>();
        rois1.add(trueroi);
        rois2.add(hypothesisroi);
        rocspace(rois1, rois2, imp);
    }



    public static void rocspace(ArrayList<Roi> rois1, ArrayList<Roi> rois2, ImagePlus imp) {

        if (rois1.size() != rois2.size()) {
            IJ.error("The two lists of rois must have the same number of elements");
            return;
        }

        double[] x = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};
        double[] y = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};

        Plot plot = new Plot("ROC space", "FPR or (1-specifity)", "TPR or sensitivity", x, y);
        plot.setColor(Color.BLUE);
        plot.draw();

        double[] tprs = new double[rois1.size()];
        double[] fprs = new double[rois1.size()];
        for (int i = 0; i < rois1.size(); i++) {
            RoiCompare rc = new RoiCompare(rois1.get(i), rois2.get(i), imp);
            tprs[i] = rc.sensitivity();
            fprs[i] = 1 - rc.specifity();
        }
        plot.setColor(Color.RED);
        plot.addPoints(fprs, tprs, CIRCLE);
        plot.show();
    }

    class point {

        private int x;
        private int y;

        public point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final point other = (point) obj;
            if (this.x != other.x) {
                return false;
            }
            if (this.y != other.y) {
                return false;
            }
            return true;
        }

    }

}
