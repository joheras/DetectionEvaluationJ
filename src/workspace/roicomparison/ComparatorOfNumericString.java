package workspace.roicomparison;

import java.util.Comparator;

/**
 *
 * @author joheras
 */
public class ComparatorOfNumericString  implements Comparator<String> {

        public int compare(String string1, String string2) {
            // TODO Auto-generated method stub
            String a = string1.substring(string1.lastIndexOf("_")+1, string1.lastIndexOf(".zip"));
            String b = string2.substring(string2.lastIndexOf("_")+1, string2.lastIndexOf(".zip"));
            return Integer.parseInt(a) - Integer.parseInt(b);
        }
    
}
