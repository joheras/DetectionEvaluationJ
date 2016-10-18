package workspace.roicomparison;

import ij.ImagePlus;
import ij.gui.Plot;
import static ij.gui.PlotWindow.CROSS;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.text.TextWindow;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author joheras
 */
public class RoiCompareSeveralPointsHorizontal {
    
    
    private ArrayList<Roi> truerois; 
    private ArrayList<Roi> truepointrois = new ArrayList<Roi>(); 
    private ArrayList<Roi> pointrois = new ArrayList<Roi>(); 
    private ArrayList<Integer> closerup = new ArrayList<Integer>();
    private ArrayList<Integer> border = new ArrayList<Integer>();
    
    private ImagePlus imp;
    

    public RoiCompareSeveralPointsHorizontal(ArrayList<Roi> truerois, ArrayList<Roi> points,ImagePlus imp) {
        this.truerois=truerois;
        for(Roi p :points){
        
            if(p instanceof PointRoi){
                pointrois.add(p);
            }else{
                pointrois.add(new PointRoi(p.getBounds().x + p.getBounds().width/2,
                        p.getBounds().y + p.getBounds().height/2));
            }        
        }
        this.imp=imp;
        
        border.add(0);
        for(Roi r : truerois){
            border.add(r.getBounds().x + r.getBounds().width);
        }
        border.add(imp.getWidth());
        
        Collections.sort(border);
        
        
        
    }
    
    
    
    
    public void comparerois(){
        double total = truerois.size();
        double points = pointrois.size();
        double truepositive = contains();
        double falsenegative = total - truepositive;
        
        double falsepositive = points - truepositive;
        
        double truenegative =  total + 1 - containscomplementary();
        
        /// The Roc Space
        double[] x = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};
        double[] y = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};

        Plot plot = new Plot("ROC space", "FPR or (1-specifity)", "TPR or sensitivity", x, y);
        plot.setColor(Color.BLUE);
        plot.draw();
        
        
        ArrayList list = new ArrayList();
        String headings = "Index\tTrue positive\tFalse positive\tTrue negative\tFalse negative\tPositive\tNegative\tAccuracy\tPrecision\tRecall\tFallout\tSensitivity\tSpecifity\tNegative predictive value\tFalse discovery rate\tFalse negative rate\tLR+\tLR-\tF-measure alpha=0.5\tF-measure alpha=1\tF-measure alpha=2";

        
        double positive = truepositive + falsenegative;
        double negative = falsepositive + truenegative;
        double accuracy = (truepositive + truenegative) / (positive + negative);
        double precision = (truepositive) / (truepositive + falsepositive);
        double recall = truepositive / (positive);
        double fallout = falsepositive / negative;
        double sensitivity = truepositive / (truepositive + falsenegative);
        double specifity = truenegative / (falsepositive + truenegative);
        double negativepredictivevalue = truenegative / (falsenegative + truenegative);
        double falsediscoveryrate = 1 - precision;
        double falsenegativerate = falsenegative / positive;
        double lrplus = sensitivity / (1 - specifity);
        double lrminus = (1 - sensitivity) / specifity;
        double alpha05 = ((1 + 0.5) * precision * recall) / (0.5 * precision + recall);
        double alpha1 = ((1 + 1) * precision * recall) / (1 * precision + recall);
        double alpha2 = ((1 + 2) * precision * recall) / (2 * precision + recall);
        list.add(1 + "\t" + truepositive + "\t" + falsepositive + "\t"
                + truenegative + "\t" + falsenegative + "\t" + positive + "\t" + negative + "\t" + accuracy
                + "\t" + precision + "\t" + recall + "\t" + fallout + "\t" + sensitivity + "\t" + specifity
                + "\t" + negativepredictivevalue + "\t" + falsediscoveryrate + "\t" + falsenegativerate + "\t"
                + lrplus + "\t" + lrminus + "\t" + alpha05 + "\t" + alpha1 + "\t" + alpha2);
        
        
        TextWindow textWindow = new TextWindow("Measurements", headings, list, 600, 400);

//        Frame f = WindowManager.getFrame("Measurements");
//        f.setAlwaysOnTop(true);
        double[] totaltprs = {sensitivity};
        double[] totalfprs = {1 - specifity};
        plot.setColor(Color.RED);
        plot.addPoints(totalfprs, totaltprs, CROSS);

        plot.show();
        
        
    
    
    
    
    
    }
    
    
    
    public Point comparerois(Plot plot, ArrayList list,Point p, int i, Color c){
        double total = truerois.size();
        double points = pointrois.size();
        double truepositive = contains();
        double falsenegative = total - truepositive;
        double truenegative =  total + 1 - containscomplementary();
        double falsepositive = points - truepositive;
        
        /// The Roc Space
        double[] x = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};
        double[] y = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};

       
        
        double positive = truepositive + falsenegative;
        double negative = falsepositive + truenegative;
        double accuracy = (truepositive + truenegative) / (positive + negative);
        double precision = (truepositive) / (truepositive + falsepositive);
        double recall = truepositive / (positive);
        double fallout = falsepositive / negative;
        double sensitivity = truepositive / (truepositive + falsenegative);
        double specifity = truenegative / (falsepositive + truenegative);
        double negativepredictivevalue = truenegative / (falsenegative + truenegative);
        double falsediscoveryrate = 1 - precision;
        double falsenegativerate = falsenegative / positive;
        double lrplus = sensitivity / (1 - specifity);
        double lrminus = (1 - sensitivity) / specifity;
        double alpha05 = ((1 + 0.5) * precision * recall) / (0.5 * precision + recall);
        double alpha1 = ((1 + 1) * precision * recall) / (1 * precision + recall);
        double alpha2 = ((1 + 2) * precision * recall) / (2 * precision + recall);
        list.add(i + "\t" + truepositive + "\t" + falsepositive + "\t"
                + truenegative + "\t" + falsenegative + "\t" + positive + "\t" + negative + "\t" + accuracy
                + "\t" + precision + "\t" + recall + "\t" + fallout + "\t" + sensitivity + "\t" + specifity
                + "\t" + negativepredictivevalue + "\t" + falsediscoveryrate + "\t" + falsenegativerate + "\t"
                + lrplus + "\t" + lrminus + "\t" + alpha05 + "\t" + alpha1 + "\t" + alpha2);
        
        
       

//        Frame f = WindowManager.getFrame("Measurements");
//        f.setAlwaysOnTop(true);
        double[] totaltprs = {sensitivity};
        double[] totalfprs = {1 - specifity};
        plot.setColor(c);
        plot.addPoints(totalfprs, totaltprs, CROSS);
        
        plot.drawLine(p.getX(), p.getY(), totalfprs[0], totaltprs[0]);
        
        Point p1  = new Point(totalfprs[0],totaltprs[0]);

        return p1;
        
        
    
    
    
    
    
    }
    
    
    public Point comparerois(Plot plot, ArrayList list,Point p, int i, Color c, String m){
        double total = truerois.size();
        double points = pointrois.size();
        double truepositive = contains();
        double falsenegative = total - truepositive;
        double truenegative =  total + 1 - containscomplementary();
        double falsepositive = points - truepositive;
        
        /// The Roc Space
        double[] x = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};
        double[] y = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};

       
        
        double positive = truepositive + falsenegative;
        double negative = falsepositive + truenegative;
        double accuracy = (truepositive + truenegative) / (positive + negative);
        double precision = (truepositive) / (truepositive + falsepositive);
        double recall = truepositive / (positive);
        double fallout = falsepositive / negative;
        double sensitivity = truepositive / (truepositive + falsenegative);
        double specifity = truenegative / (falsepositive + truenegative);
        double negativepredictivevalue = truenegative / (falsenegative + truenegative);
        double falsediscoveryrate = 1 - precision;
        double falsenegativerate = falsenegative / positive;
        double lrplus = sensitivity / (1 - specifity);
        double lrminus = (1 - sensitivity) / specifity;
        double alpha05 = ((1 + 0.5) * precision * recall) / (0.5 * precision + recall);
        double alpha1 = ((1 + 1) * precision * recall) / (1 * precision + recall);
        double alpha2 = ((1 + 2) * precision * recall) / (2 * precision + recall);
        list.add(m +"\t" +i + "\t" + truepositive + "\t" + falsepositive + "\t"
                + truenegative + "\t" + falsenegative + "\t" + positive + "\t" + negative + "\t" + accuracy
                + "\t" + precision + "\t" + recall + "\t" + fallout + "\t" + sensitivity + "\t" + specifity
                + "\t" + negativepredictivevalue + "\t" + falsediscoveryrate + "\t" + falsenegativerate + "\t"
                + lrplus + "\t" + lrminus + "\t" + alpha05 + "\t" + alpha1 + "\t" + alpha2);
        
        
       

//        Frame f = WindowManager.getFrame("Measurements");
//        f.setAlwaysOnTop(true);
        double[] totaltprs = {sensitivity};
        double[] totalfprs = {1 - specifity};
        plot.setColor(c);
        plot.addPoints(totalfprs, totaltprs, CROSS);
        
        plot.drawLine(p.getX(), p.getY(), totalfprs[0], totaltprs[0]);
        
        Point p1  = new Point(totalfprs[0],totaltprs[0]);

        return p1;
        
        
    
    
    
    
    
    }
    
    
    
    
    public void compareroismethod(ArrayList list,Plot plot,Color c,String method, int i, String author){
        double total = truerois.size();
        double points = pointrois.size();
        double truepositive = contains();
        double falsenegative = total - truepositive;
        double truenegative =  total + 1 - containscomplementary();
        double falsepositive = points - truepositive;
        
        /// The Roc Space
//        double[] x = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};
//        double[] y = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};

//        Plot plot = new Plot("ROC space", "FPR or (1-specifity)", "TPR or sensitivity", x, y);
//        plot.setColor(Color.BLUE);
//        plot.draw();
        
//        ArrayList list = new ArrayList();
//        String headings = "Index\tTrue positive\tFalse positive\tTrue negative\tFalse negative\tPositive\tNegative\tAccuracy\tPrecision\tRecall\tFallout\tSensitivity\tSpecifity\tNegative predictive value\tFalse discovery rate\tFalse negative rate\tLR+\tLR-\tF-measure alpha=0.5\tF-measure alpha=1\tF-measure alpha=2";

        
         double positive = truepositive + falsenegative;
        double negative = falsepositive + truenegative;
        double accuracy = (truepositive + truenegative) / (positive + negative);
        double precision = (truepositive) / (truepositive + falsepositive);
        double recall = truepositive / (positive);
        double fallout = falsepositive / negative;
        double sensitivity = truepositive / (truepositive + falsenegative);
        double specifity = truenegative / (falsepositive + truenegative);
        double negativepredictivevalue = truenegative / (falsenegative + truenegative);
        double falsediscoveryrate = 1 - precision;
        double falsenegativerate = falsenegative / positive;
        double lrplus = sensitivity / (1 - specifity);
        double lrminus = (1 - sensitivity) / specifity;
        double alpha05 = ((1 + 0.5) * precision * recall) / (0.5 * precision + recall);
        double alpha1 = ((1 + 1) * precision * recall) / (1 * precision + recall);
        double alpha2 = ((1 + 2) * precision * recall) / (2 * precision + recall);
        list.add(author + "\t" +method + "\t" + truepositive + "\t" + falsepositive + "\t"
                + truenegative + "\t" + falsenegative + "\t" + positive + "\t" + negative + "\t" + accuracy
                + "\t" + precision + "\t" + recall + "\t" + fallout + "\t" + sensitivity + "\t" + specifity
                + "\t" + negativepredictivevalue + "\t" + falsediscoveryrate + "\t" + falsenegativerate + "\t"
                + lrplus + "\t" + lrminus + "\t" + alpha05 + "\t" + alpha1 + "\t" + alpha2);
        
        
//        TextWindow textWindow = new TextWindow("Measurements", headings, list, 600, 400);

//        Frame f = WindowManager.getFrame("Measurements");
//        f.setAlwaysOnTop(true);
        double[] totaltprs = {sensitivity};
        double[] totalfprs = {1 - specifity};
        plot.setColor(c);
        plot.addPoints(totalfprs, totaltprs, CROSS);
        

//        plot.show();
        
        
    
    
    
    
    }
    
    
    public void compareroislane(ArrayList list,Plot plot,Color c, int i, String author){
        double total = truerois.size();
        double points = pointrois.size();
        double truepositive = contains();
        double falsenegative = total - truepositive;
        double truenegative = total + 1 - containscomplementary();
        double falsepositive = points - truepositive;
        
        /// The Roc Space
//        double[] x = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};
//        double[] y = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};

//        Plot plot = new Plot("ROC space", "FPR or (1-specifity)", "TPR or sensitivity", x, y);
//        plot.setColor(Color.BLUE);
//        plot.draw();
        
//        ArrayList list = new ArrayList();
//        String headings = "Index\tTrue positive\tFalse positive\tTrue negative\tFalse negative\tPositive\tNegative\tAccuracy\tPrecision\tRecall\tFallout\tSensitivity\tSpecifity\tNegative predictive value\tFalse discovery rate\tFalse negative rate\tLR+\tLR-\tF-measure alpha=0.5\tF-measure alpha=1\tF-measure alpha=2";

        
         double positive = truepositive + falsenegative;
        double negative = falsepositive + truenegative;
        double accuracy = (truepositive + truenegative) / (positive + negative);
        double precision = (truepositive) / (truepositive + falsepositive);
        double recall = truepositive / (positive);
        double fallout = falsepositive / negative;
        double sensitivity = truepositive / (truepositive + falsenegative);
        double specifity = truenegative / (falsepositive + truenegative);
        double negativepredictivevalue = truenegative / (falsenegative + truenegative);
        double falsediscoveryrate = 1 - precision;
        double falsenegativerate = falsenegative / positive;
        double lrplus = sensitivity / (1 - specifity);
        double lrminus = (1 - sensitivity) / specifity;
        double alpha05 = ((1 + 0.5) * precision * recall) / (0.5 * precision + recall);
        double alpha1 = ((1 + 1) * precision * recall) / (1 * precision + recall);
        double alpha2 = ((1 + 2) * precision * recall) / (2 * precision + recall);
        list.add(author + "\t Lane" + (i-1) + "\t" + truepositive + "\t" + falsepositive + "\t"
                + truenegative + "\t" + falsenegative + "\t" + positive + "\t" + negative + "\t" + accuracy
                + "\t" + precision + "\t" + recall + "\t" + fallout + "\t" + sensitivity + "\t" + specifity
                + "\t" + negativepredictivevalue + "\t" + falsediscoveryrate + "\t" + falsenegativerate + "\t"
                + lrplus + "\t" + lrminus + "\t" + alpha05 + "\t" + alpha1 + "\t" + alpha2);
        
        
//        TextWindow textWindow = new TextWindow("Measurements", headings, list, 600, 400);

//        Frame f = WindowManager.getFrame("Measurements");
//        f.setAlwaysOnTop(true);
        double[] totaltprs = {sensitivity};
        double[] totalfprs = {1 - specifity};
        plot.setColor(c);
        plot.addPoints(totalfprs, totaltprs, CROSS);

       
//        plot.show();
        
        
    
    
    
    
    }
    
    
    
    
    
    public int containscomplementary(){
        int num = 0;
        
        for(Roi p : pointrois){
            int c = closerpoint(p);
            
            if(!closerup.contains(c) && !truepointrois.contains(p)){
                num++;
                closerup.add(c);
            }
        }
        
        return num;
    }
    
    
    public int closerpoint(Roi p){
        int res = 0;
        for(int i : border){
            if(i>p.getBounds().x){
                return res;
            }else{
                res = i;
            }
        }
        return res;
    
    
    }
    
    
    public int contains(){
        int num = 0;
        
        for(Roi p : pointrois){
            if(pointintrueroi(p)){
                num++;
            }
        }
        
        return num;
    }
    
    public boolean pointintrueroi (Roi p){
    
      
        
        for(int i=0;i<truerois.size();i++){
            if(truerois.get(i).contains(p.getPolygon().xpoints[0], 
                    p.getPolygon().ypoints[0])){
                truepointrois.add(p);
                truerois.remove(i);
                return true;            
            }
        }
    
        return false;
    }
    
    
    
    
    
    
    
    
    

}
