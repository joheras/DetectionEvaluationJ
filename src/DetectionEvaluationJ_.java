

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.Opener;
import ij.plugin.PlugIn;
import workspace.roicomparison.RoiComparisonManager;

/**
 *
 * @author joheras
 */
public class DetectionEvaluationJ_ implements PlugIn {

    @Override
    public void run(String arg) {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {
            Opener o = new Opener();
            o.open();
            imp = IJ.getImage();
        }

        RoiComparisonManager rcm = new RoiComparisonManager();
        rcm.setVisible(true);

    }

}
