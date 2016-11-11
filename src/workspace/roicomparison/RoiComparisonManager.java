package workspace.roicomparison;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Macro;
import ij.Prefs;
import ij.Undo;
import ij.WindowManager;
import ij.gui.ColorChooser;
import ij.gui.EllipseRoi;
import ij.gui.GUI;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.ImageRoi;
import ij.gui.MessageDialog;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.RoiProperties;
import ij.gui.ShapeRoi;
import ij.gui.TextRoi;
import ij.gui.Toolbar;
import ij.gui.YesNoCancelDialog;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.io.RoiDecoder;
import ij.io.RoiEncoder;
import ij.io.SaveDialog;
import ij.macro.Interpreter;
import ij.macro.MacroRunner;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.Colors;
import ij.plugin.OverlayCommands;
import ij.plugin.OverlayLabels;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.Filler;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.frame.Recorder;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.util.StringSorter;
import ij.util.Tools;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.List;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This plugin implements the Analyze/Tools/ROI Manager command.
 */
public class RoiComparisonManager extends PlugInFrame implements ActionListener, ItemListener, MouseListener, MouseWheelListener, ListSelectionListener {

    public static final String LOC_KEY = "manager.loc";
    private static final int BUTTONS = 11;
    private static final int DRAW = 0, FILL = 1, LABEL = 2;
    private static final int SHOW_ALL = 0, SHOW_NONE = 1, LABELS = 2, NO_LABELS = 3;
    private static final int MENU = 0, COMMAND = 1;
    private static final int IGNORE_POSITION = -999;
    private static final int CHANNEL = 0, SLICE = 1, FRAME = 2, SHOW_DIALOG = 3;
    private static int rows = 15;
    private static int lastNonShiftClick = -1;
    private static boolean allowMultipleSelections = true;
    private static String moreButtonLabel = "More " + '\u00bb';
    private Panel panel;
    private static Frame instance;
    private static int colorIndex = 4;
    private JList list;
    private DefaultListModel listModel;
    private DefaultListModel listModel2;
    private Hashtable rois = new Hashtable();
    private Hashtable rois2 = new Hashtable();
    private boolean canceled;
    private boolean macro;
    private boolean ignoreInterrupts;
    private PopupMenu pm;
    private Button moreButton, colorButton;
    private Checkbox showAllCheckbox = new Checkbox("Show All", true);
    private Checkbox labelsCheckbox = new Checkbox("Labels", false);

    private static boolean measureAll = true;
    private static boolean onePerSlice = true;
    private static boolean restoreCentered;
    private int prevID;
    private boolean noUpdateMode;
    private int defaultLineWidth = 1;
    private Color defaultColor;
    private boolean firstTime = true;
    private int[] selectedIndexes;
    private boolean appendResults;
    private ResultsTable mmResults;
    private int imageID;
    private JList list2;
    private ResultsTable rt = new ResultsTable();

    double[] x = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};
    double[] y = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1};
    Plot plot = new Plot("ROC space", "FPR or (1-specifity)", "TPR or sensitivity", x, y);
    ArrayList listres = new ArrayList();
    String headings = "Index\tLabel\tGold Standard ROI\tHypothesised ROI\tTrue positive\tFalse Positive\tTrue negative\tFalse negative\tPositive\tNegative\tAccuracy\tPrecision\tRecall\tFallout\tSensitivity\tSpecifity\tNegative predictive value\tFalse discovery rate\tFalse negative rate\tLR+\tLR-\tF-measure alpha=0.5\tF-measure alpha=1\tF-measure alpha=2";

    public RoiComparisonManager() {
        super("Performance Evaluation Manager");
        if (instance != null) {
            WindowManager.toFront(instance);
            return;
        }
        instance = this;
        list = new JList();
        list2 = new JList();
        plot.setColor(Color.BLUE);
        plot.draw();
        showWindow();
    }

    public RoiComparisonManager(boolean hideWindow) {
        super("ROI Manager");
        list = new JList();
        listModel = new DefaultListModel();
        list.setModel(listModel);

        list2 = new JList();
        listModel2 = new DefaultListModel();
        list2.setModel(listModel2);
        plot.setColor(Color.BLUE);
        plot.draw();
    }

    void showWindow() {
        ImageJ ij = IJ.getInstance();
        addKeyListener(ij);
        addMouseListener(this);
        addMouseWheelListener(this);
        WindowManager.addWindow(this);
        //setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
        setLayout(new BorderLayout());
        listModel = new DefaultListModel();
        list.setModel(listModel);
        list.setPrototypeCellValue("0000-0000-0000 ");
        list.addListSelectionListener(this);
        list.addKeyListener(ij);
        list.addMouseListener(this);
        list.addMouseWheelListener(this);
        if (IJ.isLinux()) {
            list.setBackground(Color.white);
        }
        JScrollPane scrollPane = new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add("West", scrollPane);
        panel = new Panel();
        int nButtons = BUTTONS;
        panel.setLayout(new GridLayout(nButtons, 1, 5, 0));
        addButton("Add Gold Standard Roi");
        addButton("Add Hypothesised Roi");
        addButton("Rename Gold Standard Roi");
        addButton("Rename Hypothesised Roi");
        addButton("Delete Gold Standard Roi");
        addButton("Delete Hypothesised Roi");
        addButton("Load Gold Standard Roi");
        addButton("Load Hypothesised Roi");
        addButton("Save Gold Standard Roi");
        addButton("Save Hypothesised Roi");
        addButton("Open Gold Standard Roi from XML");
        addButton("Open Hypothesised Standard Roi from XML");

        showAllCheckbox.addItemListener(this);
        panel.add(showAllCheckbox);
        labelsCheckbox.addItemListener(this);
        panel.add(labelsCheckbox);
        add("Center", panel);
        listModel2 = new DefaultListModel();
        list2.setModel(listModel2);
        list2.setPrototypeCellValue("0000-0000-0000 ");
        list2.addListSelectionListener(this);
        list2.addKeyListener(ij);
        list2.addMouseListener(this);
        list2.addMouseWheelListener(this);
        if (IJ.isLinux()) {
            list2.setBackground(Color.white);
        }
        JScrollPane scrollPane2 = new JScrollPane(list2, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add("East", scrollPane2);
        Panel panel1 = new Panel();
        Button bdone = new Button("Performance Evaluation");
        bdone.addActionListener(this);
        bdone.addKeyListener(IJ.getInstance());
        bdone.addMouseListener(this);
        panel1.add(bdone);
        add("South", panel1);
        addPopupMenu();
        pack();
        Dimension size = getSize();
//		if (size.width>270)
//			setSize(size.width-40, size.height);
        list.remove(0);
        Point loc = Prefs.getLocation(LOC_KEY);
        if (loc != null) {
            setLocation(loc);
        } else {
            GUI.center(this);
        }
        show();
    }

    void addButton(String label) {
        Button b = new Button(label);
        b.addActionListener(this);
        b.addKeyListener(IJ.getInstance());
        b.addMouseListener(this);
        if (label.equals(moreButtonLabel)) {
            moreButton = b;
        }
        panel.add(b);
    }

    void addPopupMenu() {
        pm = new PopupMenu();
        //addPopupItem("Select All");
        addPopupItem("Open...");
        addPopupItem("Save...");
        addPopupItem("Fill");
        addPopupItem("Draw");
        addPopupItem("AND");
        addPopupItem("OR (Combine)");
        addPopupItem("XOR");
        addPopupItem("Split");
        addPopupItem("Add Particles");
        addPopupItem("Multi Measure");
        addPopupItem("Multi Plot");
        addPopupItem("Sort");
        addPopupItem("Specify...");
        addPopupItem("Remove Positions...");
        addPopupItem("Labels...");
        addPopupItem("List");
        addPopupItem("Interpolate ROIs");
        addPopupItem("Help");
        addPopupItem("Options...");
        add(pm);
    }

    void addPopupItem(String s) {
        MenuItem mi = new MenuItem(s);
        mi.addActionListener(this);
        pm.add(mi);
    }

    public void actionPerformed(ActionEvent e) {
        String label = e.getActionCommand();
        if (label == null) {
            return;
        }
        String command = label;
        if (command.equals("Add Gold Standard Roi")) {
//            if (rois.isEmpty()) {
            runCommand("add");
//            } else {
//                IJ.error("You can only add one true roi");
//            }
        } else if (command.equals("Add Hypothesised Roi")) {
//            if (rois2.isEmpty()) {
            runCommand("add2");
//                loadroi();
//            } else {
//                IJ.error("You can only add one hypothesised roi");
//            }
        } else if (command.equals("Rename Gold Standard Roi")) {
            rename(null);
        } else if (command.equals("Rename Hypothesised Roi")) {
            rename2(null);
        } else if (command.equals("Load Gold Standard Roi")) {
            loadtrueroi();
        } else if (command.equals("Save Gold Standard Roi")) {
            savetrueroi();
        } else if (command.equals("Save Hypothesised Roi")) {
            savehyproi();
        } else if (command.equals("Load Hypothesised Roi")) {
            loadroi();
        } else if (command.equals("Delete Gold Standard Roi")) {
            delete(false);
        } else if (command.equals("Delete Hypothesised Roi")) {
            delete2(false);
        } else if (command.equals("Performance Evaluation")) {
            performanceevaluation();
        } else if (command.equals("Open Gold Standard Roi from XML")) {
            openGoldXML();
        } else if (command.equals("Open Hypothesised Standard Roi from XML")) {
            openHypothesiedXML();
        }

    }

    //Manuel García
    private void openGoldXML() {
        String path = "";
        Macro.setOptions(null);
        String name = null;
        if (path == null || path.equals("")) {
            OpenDialog od = new OpenDialog("Open Selection(s)...", "");
            String directory = od.getDirectory();
            name = od.getFileName();
            if (name == null) {
                return;
            }
            path = directory + name;
        }

        Opener o = new Opener();
        if (name == null) {
            name = o.getName(path);
        }

        if (name.endsWith(".xml")) {
            name = name.substring(0, name.length() - 4);
        }
        name = getUniqueName(name);
        readXML(path,true);//El método que hemos creado para leer el XML y agregar las ROIs
        updateShowAll();
    }
    
    private void openHypothesiedXML(){
        String path = "";
        Macro.setOptions(null);
        String name = null;
        if (path == null || path.equals("")) {
            OpenDialog od = new OpenDialog("Open Selection(s)...", "");
            String directory = od.getDirectory();
            name = od.getFileName();
            if (name == null) {
                return;
            }
            path = directory + name;
        }

        Opener o = new Opener();
        if (name == null) {
            name = o.getName(path);
        }

        if (name.endsWith(".xml")) {
            name = name.substring(0, name.length() - 4);
        }
        name = getUniqueName(name);
        readXML(path,false);//El método que hemos creado para leer el XML y agregar las ROIs

        updateShowAll();
    }
    
    /*
    Método para leer el archivo XML. En este método también comprobamos que el XML es válido contra el schema que debe cumplir.
    Los parámetros de entrada son el path del XML que hemos capturado mediante el plugin y un booleano. Este booleano nos ayuda a 
    reutilizar el código, de esta manera si es true lo que leamos lo añadiremos a la parte de Gold Standard y si es false lo añadiremos
    a la parte de los hipotéticos.
    Dentro del método encontramos que se comprueba también qué estamos leyendo. En función de lo que se haya leído se creará una serie 
    de región u otra.
    */
    private void readXML(String path,Boolean isGoldStandard) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        File docXML = new File(path);
        File docXSD = new File("..\\ejemplo1.xsd");

        try {

            SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
            Schema schema = factory.newSchema(docXSD);
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(docXML));

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            //Ruta del archivo xml a leer
            Document doc = builder.parse(docXML);

            //Obtenemos el elemento raiz que tiene el arachivo XML que estamos leyendo
            Element root = doc.getDocumentElement();
            //Mostramos el nombre para ver que realmente es lo que queremos obtener
            //System.out.println("Nombre del elemento raíz: " + root.getNodeName());

            /*
            Recogemos todos los elementos image del XML. En nuestro caso solo tenemos uno por archivo. Una vez que sabemos eso, lo que hacemos
            es obtener solo el primer elemento que es el que tiene la información y queremos leer.            
             */
            NodeList images = root.getElementsByTagName("image");
            Node image = images.item(0);
            //Mostramos los datos de la imagen a la que se hace referencia en el documento XML
            System.out.println("Archivo: "+path +"\n\tLa imagen de esa ruta tiene \n\t\t ruta: " + image.getChildNodes().item(1).getTextContent()
                    + "\n\t\t alto: " + image.getChildNodes().item(3).getTextContent()
                    + "\n\t\t ancho: " + image.getChildNodes().item(5).getTextContent() + "\n\n");

            /*
            Ahora nos vamos a centrar en sacar todas las regiones de interés que tendrá ese documento. Hay que tener en cuenta que el documento
            puede estar formado por diferentes regiones como puntos, cuadrados... es por ello que deberemos controlar qué elemento es el que
            estamos leyendo en cada momento para poder trabajar con el correctamente.
            Una vez que comprobamos el tag de la región de interés ya sabremos que tipo de región es, por lo que podremos mostrar los datos que 
            almacena sin problema.
             */
            NodeList rois = root.getElementsByTagName("ROIs");
            Element regiones = (Element) rois.item(0);

            //Obtenemos la lista con todas las regiones
            NodeList hijos = regiones.getChildNodes();
            //Comprobamos el tipo del tag que hemos leído para saber con que tipo de región de interés trabajar para obtener los datos.
            switch (hijos.item(1).getNodeName()) {
                //Para el caso del punto
                case ("point"):
                    for (int i = 0; i < hijos.getLength(); i++) {
                        if (hijos.item(i).getNodeName() == "point") {
                            double x=Double.parseDouble(hijos.item(i).getChildNodes().item(1).getTextContent());
                            double y=Double.parseDouble(hijos.item(i).getChildNodes().item(3).getTextContent());
                            
                            PointRoi point = new PointRoi(x, y);
                            
                            if(isGoldStandard){addRoi(point);}
                            else{addRoi2(point);}
                            //Sacamos por pantalla el elemento 1 y el 3 porque en la 0 y la dos tenemos los saltos de línea.
                            /*
                            System.out.println("Punto :\n \t x: "
                                    + hijos.item(i).getChildNodes().item(1).getTextContent() + "\n \t y: "
                                    + hijos.item(i).getChildNodes().item(3).getTextContent());
                            */
                        }
                    }
                    break;

                //Para el caso del óvalo
                case ("oval"):
                    for (int i = 0; i < hijos.getLength(); i++) {
                        if (hijos.item(i).getNodeName() == "oval") {
                            double x=Double.parseDouble(hijos.item(i).getChildNodes().item(1).getChildNodes().item(1).getTextContent());
                            double y=Double.parseDouble(hijos.item(i).getChildNodes().item(1).getChildNodes().item(3).getTextContent());
                            double height=Double.parseDouble(hijos.item(i).getChildNodes().item(3).getTextContent());
                            double width=Double.parseDouble(hijos.item(i).getChildNodes().item(5).getTextContent());
                            
                            OvalRoi oval = new OvalRoi(x, y, width, height);
                            if(isGoldStandard){addRoi(oval);}
                            else{addRoi2(oval);}
                            //Sacamos por pantalla el elemento 1 y el 3 porque en la 0 y la dos tenemos los saltos de línea.
                            /*
                            System.out.println("Óvalo :\n \t x: "
                                    + hijos.item(i).getChildNodes().item(1).getChildNodes().item(1).getTextContent() + "\n \t y: "
                                    + hijos.item(i).getChildNodes().item(1).getChildNodes().item(3).getTextContent()
                                    + "\n \t altura: " + hijos.item(i).getChildNodes().item(3).getTextContent()
                                    + "\n \t anchura: " + hijos.item(i).getChildNodes().item(5).getTextContent());
                            */
                        }
                    }
                    break;

                //Para el caso de la elipse
                case ("ellipse"):
                    for (int i = 0; i < hijos.getLength(); i++) {
                        if (hijos.item(i).getNodeName() == "ellipse") {
                            double x1=Double.parseDouble(hijos.item(i).getChildNodes().item(1).getChildNodes().item(1).getTextContent());
                            double y1=Double.parseDouble(hijos.item(i).getChildNodes().item(1).getChildNodes().item(3).getTextContent());
                            double x2=Double.parseDouble(hijos.item(i).getChildNodes().item(3).getChildNodes().item(1).getTextContent());
                            double y2=Double.parseDouble(hijos.item(i).getChildNodes().item(3).getChildNodes().item(3).getTextContent());
                            double aspectRatio=Double.parseDouble(hijos.item(i).getChildNodes().item(5).getTextContent());
                            
                            EllipseRoi ellipse = new EllipseRoi(x1, y1, x2, y2, aspectRatio);
                            if(isGoldStandard){addRoi(ellipse);}
                            else{addRoi2(ellipse);}
                            //Sacamos por pantalla el elemento 1 y el 3 porque en la 0 y la dos tenemos los saltos de línea.
                            /*
                            System.out.println("Elipse :\n \t x: "
                                    + hijos.item(i).getChildNodes().item(1).getChildNodes().item(1).getTextContent() + "\n \t y: "
                                    + hijos.item(i).getChildNodes().item(1).getChildNodes().item(3).getTextContent()
                                    + "\n \t x: " + hijos.item(i).getChildNodes().item(3).getChildNodes().item(1).getTextContent()
                                    + "\n \t y: " + hijos.item(i).getChildNodes().item(3).getChildNodes().item(3).getTextContent()
                                    + "\n \t radio de aspecto: " + hijos.item(i).getChildNodes().item(5).getTextContent());
                            */
                        }
                    }
                    break;

                //Para el caso del círculo
                case ("circle"):
                    for (int i = 0; i < hijos.getLength(); i++) {
                        if (hijos.item(i).getNodeName() == "circle") {
                            double x = Double.parseDouble(hijos.item(i).getChildNodes().item(1).getChildNodes().item(1).getTextContent());
                            double y = Double.parseDouble(hijos.item(i).getChildNodes().item(1).getChildNodes().item(3).getTextContent());
                            double ratio = Double.parseDouble(hijos.item(i).getChildNodes().item(3).getTextContent());
                            
                            OvalRoi circle = new OvalRoi(x,y,ratio/*anchura*/,ratio/*altura*/);                           
                            if(isGoldStandard){addRoi(circle);}
                            else{addRoi2(circle);}
                            //Sacamos por pantalla el elemento 1 y el 3 porque en la 0 y la dos tenemos los saltos de línea.
                            /*
                            System.out.println("Círculo :\n \t x: "
                                    + hijos.item(i).getChildNodes().item(1).getChildNodes().item(1).getTextContent() + "\n \t y: "
                                    + hijos.item(i).getChildNodes().item(1).getChildNodes().item(3).getTextContent()
                                    + "\n \t radio: " + hijos.item(i).getChildNodes().item(3).getTextContent());
                            */
                        }
                    }
                    break;

                //Para el caso del rectángulo
                case ("rectangle"):
                    for (int i = 0; i < hijos.getLength(); i++) {
                        if (hijos.item(i).getNodeName() == "rectangle") {
                            double x=Double.parseDouble(hijos.item(i).getChildNodes().item(1).getChildNodes().item(1).getTextContent());
                            double y=Double.parseDouble(hijos.item(i).getChildNodes().item(1).getChildNodes().item(3).getTextContent());
                            double height=Double.parseDouble(hijos.item(i).getChildNodes().item(3).getTextContent());
                            double width=Double.parseDouble(hijos.item(i).getChildNodes().item(5).getTextContent());
                            
                            Roi rectangle = new Roi(x, y, width, height);
                            if(isGoldStandard){addRoi(rectangle);}
                            else{addRoi2(rectangle);}
                            //Sacamos por pantalla el elemento 1 y el 3 porque en la 0 y la dos tenemos los saltos de línea.
                            /*
                            System.out.println("Rectángulo :\n \t x: "
                                    + hijos.item(i).getChildNodes().item(1).getChildNodes().item(1).getTextContent() + "\n \t y: "
                                    + hijos.item(i).getChildNodes().item(1).getChildNodes().item(3).getTextContent()
                                    + "\n \t altura: " + hijos.item(i).getChildNodes().item(3).getTextContent()
                                    + "\n \t anchura: " + hijos.item(i).getChildNodes().item(5).getTextContent());
                            */
                        }
                    }
                    break;

                //Para el caso del cuadrado
                case ("square"):
                    for (int i = 0; i < hijos.getLength(); i++) {
                        if (hijos.item(i).getNodeName() == "square") {
                            double x=Double.parseDouble(hijos.item(i).getChildNodes().item(1).getChildNodes().item(1).getTextContent());
                            double y=Double.parseDouble(hijos.item(i).getChildNodes().item(1).getChildNodes().item(3).getTextContent());
                            double side=Double.parseDouble(hijos.item(i).getChildNodes().item(3).getTextContent());
                            
                            Roi square = new Roi(x, y, side, side);
                            if(isGoldStandard){addRoi(square);}
                            else{addRoi2(square);}
                            //Sacamos por pantalla el elemento 1 y el 3 porque en la 0 y la dos tenemos los saltos de línea.
                            /*
                            System.out.println("Cuadrado :\n \t x: "
                                    + hijos.item(i).getChildNodes().item(1).getChildNodes().item(1).getTextContent() + "\n \t y: "
                                    + hijos.item(i).getChildNodes().item(1).getChildNodes().item(3).getTextContent()
                                    + "\n \t lado: " + hijos.item(i).getChildNodes().item(3).getTextContent());
                            */
                        }
                    }
                    break;

                //Para el caso del polígono
                case ("polygon"):
                    for (int i = 0; i < hijos.getLength(); i++) {
                        if (hijos.item(i).getNodeName() == "polygon") {
                            //System.out.println("Polígono: ");
                            NodeList puntos = hijos.item(i).getChildNodes();
                            
                            ArrayList<Float> xs1 = new ArrayList<Float>();
                            ArrayList<Float> ys1 = new ArrayList<Float>();
                            for (int j = 0; j < puntos.getLength(); j++) {
                                Node punto = hijos.item(i).getChildNodes().item(j);
                                if ((punto.hasChildNodes())) {
                                    xs1.add(Float.parseFloat(punto.getChildNodes().item(1).getTextContent()));
                                    ys1.add(Float.parseFloat(punto.getChildNodes().item(3).getTextContent()));                                 
                                    /*
                                    System.out.println("\n\t Punto: \n \t\t x:" + punto.getChildNodes().item(1).getTextContent() + "\n \t\t y: "
                                            + punto.getChildNodes().item(3).getTextContent());
                                    */
                                }
                            }
                            /*
                            Para poder crear bien los vectores con las coordenadas hemos tenido que crear un par de ArrayList.
                            De esta forma se añaden siempre seguidos, con un vector normal al pasarle en este caso j
                            como no todos los puntos tienen childNodes(cuenta como punto también el tag de cierre) dejabamos un hueco
                            libre en el vector. Así con los arrayLis están todos seguidos. El problema es que luego los tenemos que 
                            recorrerlos para añadirlos a los vectores que hay que introducir en la clase polygon.
                            */
                            float[] xs= new float[xs1.size()];
                            float[] ys= new float[ys1.size()];
                            for(int k =0; k <xs1.size();k++){
                                xs[k]=xs1.get(k);
                                System.out.println("x:"+xs1.get(k));
                                ys[k]=ys1.get(k);
                                System.out.println("y:"+ys1.get(k));
                            }                            
                            PolygonRoi polygon = new PolygonRoi(xs, ys, xs.length, Roi.POLYGON);                     
                            
                            if(isGoldStandard){addRoi(polygon);}
                            else{addRoi2(polygon);}                         
                        }
                    }
                    break;
            }

        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (SAXException e) {
            System.out.println(new StreamSource(docXML).getSystemId() + " is NOT valid");
            System.out.println("Reason: " + e.getLocalizedMessage());
        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void performanceevaluation() {
        if (rois.isEmpty() || rois2.isEmpty()) {
            IJ.error("You must select at least a true roi and a hypothesised roi.");
        } else if (getCount() > 40 || getCount2() > 40) {
            IJ.error("The maximum number of rois is 40.");
        } else {
            ArrayList<Roi> rois1 = new ArrayList<>();
            ArrayList<Roi> roisb = new ArrayList<>();
            rois1.addAll(Arrays.asList(getRoisAsArray()));
            roisb.addAll(Arrays.asList(getRois2AsArray()));
            Roi combinetrueroi = combineTrueRois(IJ.getImage());
            combinetrueroi.setName("Gold Standard Roi Combination");
            Roi combinehyproi = combineHypRois(IJ.getImage());
            combinehyproi.setName("Hypothesised Roi Combination");

            RoiMatching rm = new RoiMatching(this, true, rois1, roisb, IJ.getImage(),
                    combinetrueroi, combinehyproi, plot, listres);
            rm.setVisible(true);

//            compareseveralrois(rois1, roisb, IJ.getImage(),rt);
//            
//            RoiCompare rc = new RoiCompare(getRoi(0), getRoi2(0), IJ.getImage());
//            rc.measurements();
        }

    }

    private void interpolateRois() {
        IJ.runPlugIn("ij.plugin.RoiInterpolator", "");
        if (record()) {
            Recorder.record("roiManager", "Interpolate ROIs");
        }
    }

    public void itemStateChanged(ItemEvent e) {
        Object source = e.getSource();
        if (source == showAllCheckbox) {
            if (firstTime) {
                labelsCheckbox.setState(true);
            }
            showAll(showAllCheckbox.getState() ? SHOW_ALL : SHOW_NONE);
            firstTime = false;
            return;
        }
        if (source == labelsCheckbox) {
            if (firstTime) {
                showAllCheckbox.setState(true);
            }
            boolean editState = labelsCheckbox.getState();
            boolean showAllState = showAllCheckbox.getState();
            if (!showAllState && !editState) {
                showAll(SHOW_NONE);
            } else {
                showAll(editState ? LABELS : NO_LABELS);
                if (editState) {
                    showAllCheckbox.setState(true);
                }
            }
            firstTime = false;
            return;
        }
    }

    void add(boolean shiftKeyDown, boolean altKeyDown) {
        if (shiftKeyDown) {
            addAndDraw(altKeyDown);
        } else if (altKeyDown) {
            addRoi(true);
        } else {
            addRoi(false);
        }
    }

    void add2(boolean shiftKeyDown, boolean altKeyDown) {
        if (shiftKeyDown) {
            addAndDraw2(altKeyDown);
        } else if (altKeyDown) {
            addRoi2(true);
        } else {
            addRoi2(false);
        }
    }

    /**
     * Adds the specified ROI.
     */
    public void addRoi(Roi roi) {
        addRoi(roi, false, null, -1);
    }

    boolean addRoi(boolean promptForName) {
        return addRoi(null, promptForName, null, IGNORE_POSITION);
    }

    boolean addRoi(Roi roi, boolean promptForName, Color color, int lineWidth) {
        ImagePlus imp = roi == null ? getImage() : WindowManager.getCurrentImage();
        if (roi == null) {
            if (imp == null) {
                return false;
            }
            roi = imp.getRoi();
            if (roi == null) {
                error("The active image does not have a selection.");
                return false;
            }
        }
        if ((roi instanceof PolygonRoi) && ((PolygonRoi) roi).getNCoordinates() == 0) {
            return false;
        }
        if (color == null && roi.getStrokeColor() != null) {
            color = roi.getStrokeColor();
        } else if (color == null && defaultColor != null) {
            color = defaultColor;
        }
        boolean ignorePosition = false;
        if (lineWidth == IGNORE_POSITION) {
            ignorePosition = true;
            lineWidth = -1;
        }
        if (lineWidth < 0) {
            int sw = (int) roi.getStrokeWidth();
            lineWidth = sw > 1 ? sw : defaultLineWidth;
        }
        if (lineWidth > 100) {
            lineWidth = 1;
        }
        int n = getCount();
        int position = imp != null && !ignorePosition ? roi.getPosition() : 0;
        int saveCurrentSlice = imp != null ? imp.getCurrentSlice() : 0;
        if (position > 0 && position != saveCurrentSlice) {
            imp.setSliceWithoutUpdate(position);
        } else {
            position = 0;
        }
        if (n > 0 && !IJ.isMacro() && imp != null) {
            // check for duplicate
            String label = (String) listModel.getElementAt(n - 1);
            Roi roi2 = (Roi) rois.get(label);
            if (roi2 != null) {
                int slice2 = getSliceNumber(roi2, label);
                if (roi.equals(roi2) && (slice2 == -1 || slice2 == imp.getCurrentSlice()) && imp.getID() == prevID && !Interpreter.isBatchMode()) {
                    if (position > 0) {
                        imp.setSliceWithoutUpdate(saveCurrentSlice);
                    }
                    return false;
                }
            }
        }
        prevID = imp != null ? imp.getID() : 0;
        String name = roi.getName();
        if (isStandardName(name)) {
            name = null;
        }
        String label = name != null ? name : getLabel(imp, roi, -1);
        if (promptForName) {
            label = promptForName(label);
        }
        if (label == null) {
            if (position > 0) {
                imp.setSliceWithoutUpdate(saveCurrentSlice);
            }
            return false;
        }
        label = getUniqueName(label);
        listModel.addElement(label);
        roi.setName(label);
        Roi roiCopy = (Roi) roi.clone();
        setRoiPosition(imp, roiCopy);
        if (lineWidth > 1) {
            roiCopy.setStrokeWidth(lineWidth);
        }
        if (color != null) {
            roiCopy.setStrokeColor(color);
        }
        rois.put(label, roiCopy);
        updateShowAll();
        if (record()) {
            recordAdd(defaultColor, defaultLineWidth);
        }
        if (position > 0) {
            imp.setSliceWithoutUpdate(saveCurrentSlice);
        }
        return true;
    }

    public void addRoi2(Roi roi) {
        addRoi2(roi, false, null, -1);
    }

    boolean addRoi2(boolean promptForName) {
        return addRoi2(null, promptForName, null, IGNORE_POSITION);
    }

    public boolean addRoi2(Roi roi, boolean promptForName, Color color, int lineWidth) {
        ImagePlus imp = roi == null ? getImage() : WindowManager.getCurrentImage();
        if (roi == null) {
            if (imp == null) {
                return false;
            }
            roi = imp.getRoi();
            if (roi == null) {
                error("The active image does not have a selection.");
                return false;
            }
        }
        if ((roi instanceof PolygonRoi) && ((PolygonRoi) roi).getNCoordinates() == 0) {
            return false;
        }
        if (color == null && roi.getStrokeColor() != null) {
            color = Color.red;
        } else if (color == null && defaultColor != null) {
            color = Color.red;
        }
        boolean ignorePosition = false;
        if (lineWidth == IGNORE_POSITION) {
            ignorePosition = true;
            lineWidth = -1;
        }
        if (lineWidth < 0) {
            int sw = (int) roi.getStrokeWidth();
            lineWidth = sw > 1 ? sw : defaultLineWidth;
        }
        if (lineWidth > 100) {
            lineWidth = 1;
        }
        int n = getCount();
        int position = imp != null && !ignorePosition ? roi.getPosition() : 0;
        int saveCurrentSlice = imp != null ? imp.getCurrentSlice() : 0;
        if (position > 0 && position != saveCurrentSlice) {
            imp.setSliceWithoutUpdate(position);
        } else {
            position = 0;
        }
        if (n > 0 && !IJ.isMacro() && imp != null) {
            // check for duplicate
            String label = (String) listModel.getElementAt(n - 1);
            Roi roi2 = (Roi) rois.get(label);
            if (roi2 != null) {
                int slice2 = getSliceNumber(roi2, label);
                if (roi.equals(roi2) && (slice2 == -1 || slice2 == imp.getCurrentSlice()) && imp.getID() == prevID && !Interpreter.isBatchMode()) {
                    if (position > 0) {
                        imp.setSliceWithoutUpdate(saveCurrentSlice);
                    }
                    return false;
                }
            }
        }
        prevID = imp != null ? imp.getID() : 0;
        String name = roi.getName();
        if (isStandardName(name)) {
            name = null;
        }
        String label = name != null ? name : getLabel(imp, roi, -1);
        if (promptForName) {
            label = promptForName(label);
        }
        if (label == null) {
            if (position > 0) {
                imp.setSliceWithoutUpdate(saveCurrentSlice);
            }
            return false;
        }
        label = getUniqueName(label);
        listModel2.addElement(label);
        roi.setName(label);
        Roi roiCopy = (Roi) roi.clone();
        setRoiPosition(imp, roiCopy);
        if (lineWidth > 1) {
            roiCopy.setStrokeWidth(lineWidth);
        }
        if (color != null) {
            roiCopy.setStrokeColor(color);
        }
        rois2.put(label, roiCopy);
        updateShowAll();
        if (record()) {
            recordAdd(defaultColor, defaultLineWidth);
        }
        if (position > 0) {
            imp.setSliceWithoutUpdate(saveCurrentSlice);
        }
        return true;
    }

    void recordAdd(Color color, int lineWidth) {
        if (Recorder.scriptMode()) {
            Recorder.recordCall("rm.addRoi(imp.getRoi());");
        } else if (color != null && lineWidth == 1) {
            Recorder.recordString("roiManager(\"Add\", \"" + getHex(color) + "\");\n");
        } else if (lineWidth > 1) {
            Recorder.recordString("roiManager(\"Add\", \"" + getHex(color) + "\", " + lineWidth + ");\n");
        } else {
            Recorder.record("roiManager", "Add");
        }
    }

    String getHex(Color color) {
        if (color == null) {
            color = ImageCanvas.getShowAllColor();
        }
        String hex = Integer.toHexString(color.getRGB());
        if (hex.length() == 8) {
            hex = hex.substring(2);
        }
        return hex;
    }

    /**
     * Adds the specified ROI to the list. The third argument ('n') will be used
     * to form the first part of the ROI label if it is >= 0.
     */
    public void add(ImagePlus imp, Roi roi, int n) {
        if (roi == null) {
            return;
        }
        String label = roi.getName();
        String label2 = label;
        if (label == null) {
            label = getLabel(imp, roi, n);
        } else {
            label = label + "-" + n;
        }
        if (label == null) {
            return;
        }
        listModel.addElement(label);
        if (label2 != null) {
            roi.setName(label2);
        } else {
            roi.setName(label);
        }
        rois.put(label, (Roi) roi.clone());
    }

    boolean isStandardName(String name) {
        if (name == null) {
            return false;
        }
        boolean isStandard = false;
        int len = name.length();
        if (len >= 14 && name.charAt(4) == '-' && name.charAt(9) == '-') {
            isStandard = true;
        } else if (len >= 17 && name.charAt(5) == '-' && name.charAt(11) == '-') {
            isStandard = true;
        } else if (len >= 9 && name.charAt(4) == '-') {
            isStandard = true;
        } else if (len >= 11 && name.charAt(5) == '-') {
            isStandard = true;
        }
        return isStandard;
    }

    String getLabel(ImagePlus imp, Roi roi, int n) {
        Rectangle r = roi.getBounds();
        int xc = r.x + r.width / 2;
        int yc = r.y + r.height / 2;
        if (n >= 0) {
            xc = yc;
            yc = n;
        }
        if (xc < 0) {
            xc = 0;
        }
        if (yc < 0) {
            yc = 0;
        }
        int digits = 4;
        String xs = "" + xc;
        if (xs.length() > digits) {
            digits = xs.length();
        }
        String ys = "" + yc;
        if (ys.length() > digits) {
            digits = ys.length();
        }
        if (digits == 4 && imp != null && imp.getStackSize() >= 10000) {
            digits = 5;
        }
        xs = "000000" + xc;
        ys = "000000" + yc;
        String label = ys.substring(ys.length() - digits) + "-" + xs.substring(xs.length() - digits);
        if (imp != null && imp.getStackSize() > 1) {
            int slice = imp.getCurrentSlice();
            String zs = "000000" + slice;
            label = zs.substring(zs.length() - digits) + "-" + label;
        }
        return label;
    }

    void addAndDraw(boolean altKeyDown) {
        if (altKeyDown) {
            if (!addRoi(true)) {
                return;
            }
        } else if (!addRoi(false)) {
            return;
        }
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp != null) {
            Undo.setup(Undo.COMPOUND_FILTER, imp);
            IJ.run(imp, "Draw", "slice");
            Undo.setup(Undo.COMPOUND_FILTER_DONE, imp);
        }
        if (record()) {
            Recorder.record("roiManager", "Add & Draw");
        }
    }

    void addAndDraw2(boolean altKeyDown) {
        if (altKeyDown) {
            if (!addRoi2(true)) {
                return;
            }
        } else if (!addRoi2(false)) {
            return;
        }
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp != null) {
            Undo.setup(Undo.COMPOUND_FILTER, imp);
            IJ.run(imp, "Draw", "slice");
            Undo.setup(Undo.COMPOUND_FILTER_DONE, imp);
        }
        if (record()) {
            Recorder.record("roiManager", "Add & Draw");
        }
    }

    public void delete2withoutasking() {
        rois2.clear();
        listModel2.removeAllElements();
    }

    public void deletewithoutasking() {
        rois.clear();
        listModel.removeAllElements();
    }

    public boolean delete2(boolean replacing) {
        int count = getCount2();
        if (count == 0) {
            return error("The list is empty.");
        }
        int index[] = getSelectedIndexes2();
        if (index.length == 0 || (replacing && count > 1)) {
            String msg = "Delete all items on the list?";
            if (replacing) {
                msg = "Replace items on the list?";
            }
            canceled = false;
            if (!IJ.isMacro() && !macro) {
                YesNoCancelDialog d = new YesNoCancelDialog(this, "ROI Manager", msg);
                if (d.cancelPressed()) {
                    canceled = true;
                    return false;
                }
                if (!d.yesPressed()) {
                    return false;
                }
            }
            index = getAllIndexes2();
        }
        if (count == index.length && !replacing) {
            rois2.clear();
            listModel2.removeAllElements();
        } else {
            for (int i = count - 1; i >= 0; i--) {
                boolean delete = false;
                for (int j = 0; j < index.length; j++) {
                    if (index[j] == i) {
                        delete = true;
                    }
                }
                if (delete) {
                    rois2.remove((String) listModel2.getElementAt(i));
                    listModel2.remove(i);
                }
            }
        }
        ImagePlus imp = WindowManager.getCurrentImage();
        if (count > 1 && index.length == 1 && imp != null) {
            imp.deleteRoi();
        }
        updateShowAll();
        if (record()) {
            Recorder.record("roiManager", "Delete");
        }
        return true;
    }

    boolean delete(boolean replacing) {
        int count = getCount();
        if (count == 0) {
            return error("The list is empty.");
        }
        int index[] = getSelectedIndexes();
        if (index.length == 0 || (replacing && count > 1)) {
            String msg = "Delete all items on the list?";
            if (replacing) {
                msg = "Replace items on the list?";
            }
            canceled = false;
            if (!IJ.isMacro() && !macro) {
                YesNoCancelDialog d = new YesNoCancelDialog(this, "ROI Manager", msg);
                if (d.cancelPressed()) {
                    canceled = true;
                    return false;
                }
                if (!d.yesPressed()) {
                    return false;
                }
            }
            index = getAllIndexes();
        }
        if (count == index.length && !replacing) {
            rois.clear();
            listModel.removeAllElements();
        } else {
            for (int i = count - 1; i >= 0; i--) {
                boolean delete = false;
                for (int j = 0; j < index.length; j++) {
                    if (index[j] == i) {
                        delete = true;
                    }
                }
                if (delete) {
                    rois.remove((String) listModel.getElementAt(i));
                    listModel.remove(i);
                }
            }
        }
        ImagePlus imp = WindowManager.getCurrentImage();
        if (count > 1 && index.length == 1 && imp != null) {
            imp.deleteRoi();
        }
        updateShowAll();
        if (record()) {
            Recorder.record("roiManager", "Delete");
        }
        return true;
    }

    boolean update(boolean clone) {
        ImagePlus imp = getImage();
        if (imp == null) {
            return false;
        }
        ImageCanvas ic = imp.getCanvas();
        boolean showingAll = ic != null && ic.getShowAllROIs();
        Roi roi = imp.getRoi();
        if (roi == null) {
            error("The active image does not have a selection.");
            return false;
        }
        int index = list.getSelectedIndex();
        if (index < 0 && !showingAll) {
            return error("Exactly one item in the list must be selected.");
        }
        if (index >= 0) {
            String name = (String) listModel.getElementAt(index);
            rois.remove(name);
            if (clone) {
                Roi roi2 = (Roi) roi.clone();
                setRoiPosition(imp, roi2);
                roi.setName(name);
                roi2.setName(name);
                rois.put(name, roi2);
            } else {
                rois.put(name, roi);
            }
        }
        if (record()) {
            Recorder.record("roiManager", "Update");
        }
        updateShowAll();
        return true;
    }

    boolean rename(String name2) {
        int index = list.getSelectedIndex();
        if (index < 0) {
            return error("Exactly one item in the list must be selected.");
        }
        String name = (String) listModel.getElementAt(index);
        if (name2 == null) {
            name2 = promptForName(name);
        }
        if (name2 == null) {
            return false;
        }
        if (name2.equals(name)) {
            return false;
        }
        name2 = getUniqueName(name2);
        Roi roi = (Roi) rois.get(name);
        if (roi == null) {
            return false;
        }
        rois.remove(name);
        roi.setName(name2);
        int position = getSliceNumber(name2);
        if (position > 0 && roi.getCPosition() == 0 && roi.getZPosition() == 0 && roi.getTPosition() == 0) {
            roi.setPosition(position);
        }
        rois.put(name2, roi);
        listModel.setElementAt(name2, index);
        list.setSelectedIndex(index);
        if (Prefs.useNamesAsLabels && labelsCheckbox.getState()) {
            ImagePlus imp = WindowManager.getCurrentImage();
            if (imp != null) {
                imp.draw();
            }
        }
        if (record()) {
            Recorder.record("roiManager", "Rename", name2);
        }
        return true;
    }

    boolean rename2(String name2) {
        int index = list2.getSelectedIndex();
        if (index < 0) {
            return error("Exactly one item in the list must be selected.");
        }
        String name = (String) listModel2.getElementAt(index);
        if (name2 == null) {
            name2 = promptForName(name);
        }
        if (name2 == null) {
            return false;
        }
        if (name2.equals(name)) {
            return false;
        }
        name2 = getUniqueName(name2);
        Roi roi = (Roi) rois2.get(name);
        if (roi == null) {
            return false;
        }
        rois2.remove(name);
        roi.setName(name2);
        int position = getSliceNumber(name2);
        if (position > 0 && roi.getCPosition() == 0 && roi.getZPosition() == 0 && roi.getTPosition() == 0) {
            roi.setPosition(position);
        }
        rois2.put(name2, roi);
        listModel2.setElementAt(name2, index);
        list2.setSelectedIndex(index);
        if (Prefs.useNamesAsLabels && labelsCheckbox.getState()) {
            ImagePlus imp = WindowManager.getCurrentImage();
            if (imp != null) {
                imp.draw();
            }
        }
        if (record()) {
            Recorder.record("roiManager", "Rename", name2);
        }
        return true;
    }

    String promptForName(String name) {
        GenericDialog gd = new GenericDialog("ROI Manager");
        gd.addStringField("Rename As:", name, 20);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return null;
        } else {
            return gd.getNextString();
        }
    }

    boolean restore(ImagePlus imp, int index, boolean setSlice) {
        String label = (String) listModel.getElementAt(index);
        Roi roi = (Roi) rois.get(label);
        if (imp == null || roi == null) {
            return false;
        }
        if (setSlice) {
            int c = roi.getCPosition();
            int z = roi.getZPosition();
            int t = roi.getTPosition();
            boolean hyperstack = imp.isHyperStack();
            //IJ.log("restore: "+hyperstack+" "+c+" "+z+" "+t);
            if (hyperstack && (c > 0 || z > 0 || t > 0)) {
                imp.setPosition(c, z, t);
            } else {
                int n = getSliceNumber(roi, label);
                if (n >= 1 && n <= imp.getStackSize()) {
                    if (hyperstack) {
                        if (imp.getNSlices() > 1 && n <= imp.getNSlices()) {
                            imp.setPosition(imp.getC(), n, imp.getT());
                        } else if (imp.getNFrames() > 1 && n <= imp.getNFrames()) {
                            imp.setPosition(imp.getC(), imp.getZ(), n);
                        } else {
                            imp.setPosition(n);
                        }
                    } else {
                        imp.setSlice(n);
                    }
                }
            }
        }
        if (showAllCheckbox.getState() && !restoreCentered && !noUpdateMode) {
            roi.setImage(null);
            imp.setRoi(roi);
            return true;
        }
        Roi roi2 = (Roi) roi.clone();
        Rectangle r = roi2.getBounds();
        int width = imp.getWidth(), height = imp.getHeight();
        if (restoreCentered) {
            ImageCanvas ic = imp.getCanvas();
            if (ic != null) {
                Rectangle r1 = ic.getSrcRect();
                Rectangle r2 = roi2.getBounds();
                roi2.setLocation(r1.x + r1.width / 2 - r2.width / 2, r1.y + r1.height / 2 - r2.height / 2);
            }
        }
        if (r.x >= width || r.y >= height || (r.x + r.width) < 0 || (r.y + r.height) < 0) {
            roi2.setLocation((width - r.width) / 2, (height - r.height) / 2);
        }
        if (noUpdateMode) {
            imp.setRoi(roi2, false);
            noUpdateMode = false;
        } else {
            imp.setRoi(roi2, true);
        }
        return true;
    }

    boolean restore2(ImagePlus imp, int index, boolean setSlice) {
        String label = (String) listModel2.getElementAt(index);
        Roi roi = (Roi) rois2.get(label);
        roi.setStrokeColor(Color.RED);
        if (imp == null || roi == null) {
            return false;
        }
        if (setSlice) {
            int c = roi.getCPosition();
            int z = roi.getZPosition();
            int t = roi.getTPosition();
            boolean hyperstack = imp.isHyperStack();
            //IJ.log("restore: "+hyperstack+" "+c+" "+z+" "+t);
            if (hyperstack && (c > 0 || z > 0 || t > 0)) {
                imp.setPosition(c, z, t);
            } else {
                int n = getSliceNumber(roi, label);
                if (n >= 1 && n <= imp.getStackSize()) {
                    if (hyperstack) {
                        if (imp.getNSlices() > 1 && n <= imp.getNSlices()) {
                            imp.setPosition(imp.getC(), n, imp.getT());
                        } else if (imp.getNFrames() > 1 && n <= imp.getNFrames()) {
                            imp.setPosition(imp.getC(), imp.getZ(), n);
                        } else {
                            imp.setPosition(n);
                        }
                    } else {
                        imp.setSlice(n);
                    }
                }
            }
        }
        if (showAllCheckbox.getState() && !restoreCentered && !noUpdateMode) {
            roi.setImage(null);
            imp.setRoi(roi);
            return true;
        }
        Roi roi2 = (Roi) roi.clone();
        roi2.setStrokeColor(Color.RED);
        Rectangle r = roi2.getBounds();
        int width = imp.getWidth(), height = imp.getHeight();
        if (restoreCentered) {
            ImageCanvas ic = imp.getCanvas();
            if (ic != null) {
                Rectangle r1 = ic.getSrcRect();
                Rectangle r2 = roi2.getBounds();
                roi2.setLocation(r1.x + r1.width / 2 - r2.width / 2, r1.y + r1.height / 2 - r2.height / 2);
            }
        }
        if (r.x >= width || r.y >= height || (r.x + r.width) < 0 || (r.y + r.height) < 0) {
            roi2.setLocation((width - r.width) / 2, (height - r.height) / 2);
        }
        if (noUpdateMode) {
            imp.setRoi(roi2, false);
            noUpdateMode = false;
        } else {
            imp.setRoi(roi2, true);
        }
        return true;
    }

    boolean restoreWithoutUpdate(int index) {
        noUpdateMode = true;
        return restore(getImage(), index, false);
    }

    /**
     * Returns the slice number associated with the specified name, or -1 if the
     * name does not include a slice number.
     */
    public int getSliceNumber(String label) {
        int slice = -1;
        if (label.length() >= 14 && label.charAt(4) == '-' && label.charAt(9) == '-') {
            slice = (int) Tools.parseDouble(label.substring(0, 4), -1);
        } else if (label.length() >= 17 && label.charAt(5) == '-' && label.charAt(11) == '-') {
            slice = (int) Tools.parseDouble(label.substring(0, 5), -1);
        } else if (label.length() >= 20 && label.charAt(6) == '-' && label.charAt(13) == '-') {
            slice = (int) Tools.parseDouble(label.substring(0, 6), -1);
        }
        return slice;
    }

    /**
     * Returns the slice number associated with the specified ROI or name, or -1
     * if the ROI or name does not include a slice number.
     */
    int getSliceNumber(Roi roi, String label) {
        int slice = roi != null ? roi.getPosition() : -1;
        if (slice == 0) {
            slice = -1;
        }
        if (slice == -1) {
            slice = getSliceNumber(label);
        }
        return slice;
    }

    void open(String path) {
        Macro.setOptions(null);
        String name = null;
        if (path == null || path.equals("")) {
            OpenDialog od = new OpenDialog("Open Selection(s)...", "");
            String directory = od.getDirectory();
            name = od.getFileName();
            if (name == null) {
                return;
            }
            path = directory + name;
        }
        if (Recorder.record && !Recorder.scriptMode()) {
            Recorder.record("roiManager", "Open", path);
        }
        if (path.endsWith(".zip")) {
            openZip(path);
            return;
        }
        Opener o = new Opener();
        if (name == null) {
            name = o.getName(path);
        }
        Roi roi = o.openRoi(path);
        if (roi != null) {
            if (name.endsWith(".roi")) {
                name = name.substring(0, name.length() - 4);
            }
            name = getUniqueName(name);
            listModel.addElement(name);
            rois.put(name, roi);
        }
        updateShowAll();
    }

    // Modified on 2005/11/15 by Ulrik Stervbo to only read .roi files and to not empty the current list
    void openZip(String path) {
        ZipInputStream in = null;
        ByteArrayOutputStream out;
        int nRois = 0;
        try {
            in = new ZipInputStream(new FileInputStream(path));
            byte[] buf = new byte[1024];
            int len;
            ZipEntry entry = in.getNextEntry();
            while (entry != null) {
                String name = entry.getName();
                if (name.endsWith(".roi")) {
                    out = new ByteArrayOutputStream();
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    out.close();
                    byte[] bytes = out.toByteArray();
                    RoiDecoder rd = new RoiDecoder(bytes, name);
                    Roi roi = rd.getRoi();
                    if (roi != null) {
                        name = name.substring(0, name.length() - 4);
                        name = getUniqueName(name);
                        listModel.addElement(name);
                        rois.put(name, roi);
                        nRois++;
                    }
                }
                entry = in.getNextEntry();
            }
            in.close();
        } catch (IOException e) {
            error(e.toString());
        }
        if (nRois == 0) {
            error("This ZIP archive does not appear to contain \".roi\" files");
        }
        updateShowAll();
    }

    // Modified on 2005/11/15 by Ulrik Stervbo to only read .roi files and to not empty the current list
    void openZip2(String path) {
        ZipInputStream in = null;
        ByteArrayOutputStream out;
        int nRois = 0;
        try {
            in = new ZipInputStream(new FileInputStream(path));
            byte[] buf = new byte[1024];
            int len;
            ZipEntry entry = in.getNextEntry();
            while (entry != null) {
                String name = entry.getName();
                if (name.endsWith(".roi")) {
                    out = new ByteArrayOutputStream();
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    out.close();
                    byte[] bytes = out.toByteArray();
                    RoiDecoder rd = new RoiDecoder(bytes, name);
                    Roi roi = rd.getRoi();
                    if (roi != null) {
                        name = name.substring(0, name.length() - 4);
                        name = getUniqueName(name);

                        listModel2.addElement(name);
                        rois2.put(name, roi);

                        nRois++;
                    }
                }
                entry = in.getNextEntry();
            }
            in.close();
        } catch (IOException e) {
            error(e.toString());
        }
        if (nRois == 0) {
            error("This ZIP archive does not appear to contain \".roi\" files");
        }
        updateShowAll();
    }

    String getUniqueName(String name) {
        String name2 = name;
        int n = 1;
        Roi roi2 = (Roi) rois.get(name2);
        while (roi2 != null) {
            roi2 = (Roi) rois.get(name2);
            if (roi2 != null) {
                int lastDash = name2.lastIndexOf("-");
                if (lastDash != -1 && name2.length() - lastDash < 5) {
                    name2 = name2.substring(0, lastDash);
                }
                name2 = name2 + "-" + n;
                n++;
            }
            roi2 = (Roi) rois.get(name2);
        }
        return name2;
    }

    boolean savetrueroi() {
        if (getCount() == 0) {
            return error("The selection list is empty.");
        }
        int[] indexes = getSelectedIndexes();
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }
        if (indexes.length > 1) {
            return saveMultiple(indexes, null);
        }
        String name = (String) listModel.getElementAt(indexes[0]);
        Macro.setOptions(null);
        SaveDialog sd = new SaveDialog("Save Selection...", name, ".roi");
        String name2 = sd.getFileName();
        if (name2 == null) {
            return false;
        }
        String dir = sd.getDirectory();
        Roi roi = (Roi) rois.get(name);
        rois.remove(name);
        if (!name2.endsWith(".roi")) {
            name2 = name2 + ".roi";
        }
        String newName = name2.substring(0, name2.length() - 4);
        rois.put(newName, roi);
        roi.setName(newName);
        listModel.setElementAt(newName, indexes[0]);
        RoiEncoder re = new RoiEncoder(dir + name2);
        try {
            re.write(roi);
        } catch (IOException e) {
            IJ.error("ROI Manager", e.getMessage());
        }
        if (record()) {
            String path = dir + name2;
            if (Recorder.scriptMode()) {
                Recorder.recordCall("IJ.saveAs(imp, \"Selection\", \"" + path + "\");");
            } else {
                Recorder.record("saveAs", "Selection", path);
            }
        }
        return true;
    }

    boolean savehyproi() {
        if (getCount2() == 0) {
            return error("The selection list is empty.");
        }
        int[] indexes = getSelectedIndexes2();
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }
        if (indexes.length > 1) {
            return saveMultiple2(indexes, null);
        }
        String name = (String) listModel2.getElementAt(indexes[0]);
        Macro.setOptions(null);
        SaveDialog sd = new SaveDialog("Save Selection...", name, ".roi");
        String name2 = sd.getFileName();
        if (name2 == null) {
            return false;
        }
        String dir = sd.getDirectory();
        Roi roi = (Roi) rois2.get(name);
        rois.remove(name);
        if (!name2.endsWith(".roi")) {
            name2 = name2 + ".roi";
        }
        String newName = name2.substring(0, name2.length() - 4);
        rois2.put(newName, roi);
        roi.setName(newName);
        listModel2.setElementAt(newName, indexes[0]);
        RoiEncoder re = new RoiEncoder(dir + name2);
        try {
            re.write(roi);
        } catch (IOException e) {
            IJ.error("ROI Manager", e.getMessage());
        }
        if (record()) {
            String path = dir + name2;
            if (Recorder.scriptMode()) {
                Recorder.recordCall("IJ.saveAs(imp, \"Selection\", \"" + path + "\");");
            } else {
                Recorder.record("saveAs", "Selection", path);
            }
        }
        return true;
    }

    boolean saveMultiple(int[] indexes, String path) {
        Macro.setOptions(null);
        if (path == null) {
            SaveDialog sd = new SaveDialog("Save ROIs...", "RoiSet", ".zip");
            String name = sd.getFileName();
            if (name == null) {
                return false;
            }
            if (!(name.endsWith(".zip") || name.endsWith(".ZIP"))) {
                name = name + ".zip";
            }
            String dir = sd.getDirectory();
            path = dir + name;
        }
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(path));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(zos));
            RoiEncoder re = new RoiEncoder(out);
            for (int i = 0; i < indexes.length; i++) {
                String label = (String) listModel.getElementAt(indexes[i]);
                Roi roi = (Roi) rois.get(label);
                if (IJ.debugMode) {
                    IJ.log("saveMultiple: " + i + "  " + label + "  " + roi);
                }
                if (roi == null) {
                    continue;
                }
                if (!label.endsWith(".roi")) {
                    label += ".roi";
                }
                zos.putNextEntry(new ZipEntry(label));
                re.write(roi);
                out.flush();
            }
            out.close();
        } catch (IOException e) {
            error("" + e);
            return false;
        }
        if (record()) {
            Recorder.record("roiManager", "Save", path);
        }
        return true;
    }

    public boolean saveMultiple2(int[] indexes, String path) {
        Macro.setOptions(null);
        if (path == null) {
            SaveDialog sd = new SaveDialog("Save ROIs...", "RoiSet", ".zip");
            String name = sd.getFileName();
            if (name == null) {
                return false;
            }
            if (!(name.endsWith(".zip") || name.endsWith(".ZIP"))) {
                name = name + ".zip";
            }
            String dir = sd.getDirectory();
            path = dir + name;
        }
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(path));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(zos));
            RoiEncoder re = new RoiEncoder(out);
            for (int i = 0; i < indexes.length; i++) {
                String label = (String) listModel2.getElementAt(indexes[i]);
                Roi roi = (Roi) rois2.get(label);
                if (IJ.debugMode) {
                    IJ.log("saveMultiple: " + i + "  " + label + "  " + roi);
                }
                if (roi == null) {
                    continue;
                }
                if (!label.endsWith(".roi")) {
                    label += ".roi";
                }
                zos.putNextEntry(new ZipEntry(label));
                re.write(roi);
                out.flush();
            }
            out.close();
        } catch (IOException e) {
            error("" + e);
            return false;
        }
        if (record()) {
            Recorder.record("roiManager", "Save", path);
        }
        return true;
    }

    private void listRois() {
        Roi[] rois = getRoisAsArray();
        OverlayCommands.listRois(rois);
        if (record()) {
            Recorder.record("roiManager", "List");
        }
    }

    boolean measure(int mode) {
        ImagePlus imp = getImage();
        if (imp == null) {
            return false;
        }
        int[] indexes = getSelectedIndexes();
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }
        if (indexes.length == 0) {
            return false;
        }
        boolean allSliceOne = true;
        for (int i = 0; i < indexes.length; i++) {
            String label = (String) listModel.getElementAt(indexes[i]);
            Roi roi = (Roi) rois.get(label);
            if (getSliceNumber(roi, label) > 1) {
                allSliceOne = false;
            }
        }
        int measurements = Analyzer.getMeasurements();
        if (imp.getStackSize() > 1) {
            Analyzer.setMeasurements(measurements | Measurements.SLICE);
        }
        int currentSlice = imp.getCurrentSlice();
        Analyzer.setMeasurements(measurements & (~Measurements.ADD_TO_OVERLAY));
        for (int i = 0; i < indexes.length; i++) {
            if (restore(getImage(), indexes[i], !allSliceOne)) {
                IJ.run("Measure");
            } else {
                break;
            }
        }
        Analyzer.setMeasurements(measurements);
        imp.setSlice(currentSlice);
        if (indexes.length > 1) {
            IJ.run("Select None");
        }
        if (record()) {
            Recorder.record("roiManager", "Measure");
        }
        return true;
    }

    /*
     void showIndexes(int[] indexes) {
     for (int i=0; i<indexes.length; i++) {
     String label = (String) listModel.getElementAt(indexes[i]);
     Roi roi = (Roi)rois.get(label);
     IJ.log(i+" "+roi.getName());
     }
     }
     */

 /* This method performs measurements for several ROI's in a stack
     and arranges the results with one line per slice.  By constast, the 
     measure() method produces several lines per slice.	The results 
     from multiMeasure() may be easier to import into a spreadsheet 
     program for plotting or additional analysis. Based on the multi() 
     method in Bob Dougherty's Multi_Measure plugin
     (http://www.optinav.com/Multi-Measure.htm).
     */
    boolean multiMeasure(String cmd) {
        ImagePlus imp = getImage();
        if (imp == null) {
            return false;
        }
        int[] indexes = getSelectedIndexes();
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }
        if (indexes.length == 0) {
            return false;
        }
        int measurements = Analyzer.getMeasurements();

        int nSlices = imp.getStackSize();
        if (cmd != null) {
            appendResults = cmd.contains("append") ? true : false;
        }
        if (IJ.isMacro()) {
            if (nSlices > 1) {
                measureAll = true;
            }
            onePerSlice = true;
        } else {
            GenericDialog gd = new GenericDialog("Multi Measure");
            if (nSlices > 1) {
                gd.addCheckbox("Measure all " + nSlices + " slices", measureAll);
            }
            gd.addCheckbox("One Row Per Slice", onePerSlice);
            gd.addCheckbox("Append results", appendResults);
            int columns = getColumnCount(imp, measurements) * indexes.length;
            String str = nSlices == 1 ? "this option" : "both options";
            gd.setInsets(10, 25, 0);
            gd.addMessage(
                    "Enabling " + str + " will result\n"
                    + "in a table with " + columns + " columns."
            );
            gd.showDialog();
            if (gd.wasCanceled()) {
                return false;
            }
            if (nSlices > 1) {
                measureAll = gd.getNextBoolean();
            }
            onePerSlice = gd.getNextBoolean();
            appendResults = gd.getNextBoolean();
        }
        if (!measureAll) {
            nSlices = 1;
        }
        int currentSlice = imp.getCurrentSlice();

        if (!onePerSlice) {
            int measurements2 = nSlices > 1 ? measurements | Measurements.SLICE : measurements;
            ResultsTable rt = new ResultsTable();
            Analyzer analyzer = new Analyzer(imp, measurements2, rt);
            for (int slice = 1; slice <= nSlices; slice++) {
                if (nSlices > 1) {
                    imp.setSliceWithoutUpdate(slice);
                }
                for (int i = 0; i < indexes.length; i++) {
                    if (restoreWithoutUpdate(indexes[i])) {
                        analyzer.measure();
                    } else {
                        break;
                    }
                }
            }
            rt.show("Results");
            if (nSlices > 1) {
                imp.setSlice(currentSlice);
            }
            return true;
        }

        Analyzer aSys = new Analyzer(imp); // System Analyzer
        ResultsTable rtSys = Analyzer.getResultsTable();
        ResultsTable rtMulti = new ResultsTable();
        if (appendResults && mmResults != null) {
            rtMulti = mmResults;
        }
        rtSys.reset();
        //Analyzer aMulti = new Analyzer(imp, measurements, rtMulti); //Private Analyzer

        for (int slice = 1; slice <= nSlices; slice++) {
            int sliceUse = slice;
            if (nSlices == 1) {
                sliceUse = currentSlice;
            }
            imp.setSliceWithoutUpdate(sliceUse);
            rtMulti.incrementCounter();
            if ((Analyzer.getMeasurements() & Measurements.LABELS) != 0) {
                rtMulti.addLabel("Label", imp.getTitle());
            }
            int roiIndex = 0;
            for (int i = 0; i < indexes.length; i++) {
                if (restoreWithoutUpdate(indexes[i])) {
                    roiIndex++;
                    aSys.measure();
                    for (int j = 0; j <= rtSys.getLastColumn(); j++) {
                        float[] col = rtSys.getColumn(j);
                        String head = rtSys.getColumnHeading(j);
                        String suffix = "" + roiIndex;
                        Roi roi = imp.getRoi();
                        if (roi != null) {
                            String name = roi.getName();
                            if (name != null && name.length() > 0 && (name.length() < 9 || !Character.isDigit(name.charAt(0)))) {
                                suffix = "(" + name + ")";
                            }
                        }
                        if (head != null && col != null && !head.equals("Slice")) {
                            rtMulti.addValue(head + suffix, rtSys.getValue(j, rtSys.getCounter() - 1));
                        }
                    }
                } else {
                    break;
                }
            }
        }
        mmResults = (ResultsTable) rtMulti.clone();
        rtMulti.show("Results");

        imp.setSlice(currentSlice);
        if (indexes.length > 1) {
            IJ.run("Select None");
        }
        if (record()) {
            String arg = appendResults ? " append" : "";
            Recorder.record("roiManager", "Multi Measure" + arg);
        }
        return true;
    }

    int getColumnCount(ImagePlus imp, int measurements) {
        ImageStatistics stats = imp.getStatistics(measurements);
        ResultsTable rt = new ResultsTable();
        Analyzer analyzer = new Analyzer(imp, measurements, rt);
        analyzer.saveResults(stats, null);
        int count = 0;
        for (int i = 0; i <= rt.getLastColumn(); i++) {
            float[] col = rt.getColumn(i);
            String head = rt.getColumnHeading(i);
            if (head != null && col != null) {
                count++;
            }
        }
        return count;
    }

    void multiPlot() {
        ImagePlus imp = getImage();
        if (imp == null) {
            return;
        }
        int[] indexes = getSelectedIndexes();
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }
        int n = indexes.length;
        if (n == 0) {
            return;
        }
        Color[] colors = {Color.blue, Color.green, Color.magenta, Color.red, Color.cyan, Color.yellow};
        if (n > colors.length) {
            colors = new Color[n];
            double c = 0;
            double inc = 150.0 / n;
            for (int i = 0; i < n; i++) {
                colors[i] = new Color((int) c, (int) c, (int) c);
                c += inc;
            }
        }
        int currentSlice = imp.getCurrentSlice();
        double[][] x = new double[n][];
        double[][] y = new double[n][];
        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double fixedMin = ProfilePlotAverage.getFixedMin();
        double fixedMax = ProfilePlotAverage.getFixedMax();
        boolean freeYScale = fixedMin == 0.0 && fixedMax == 0.0;
        if (!freeYScale) {
            minY = fixedMin;
            maxY = fixedMax;
        }
        int maxX = 0;
        Calibration cal = imp.getCalibration();
        double xinc = cal.pixelWidth;
        for (int i = 0; i < indexes.length; i++) {
            if (!restore(getImage(), indexes[i], true)) {
                break;
            }
            Roi roi = imp.getRoi();
            if (roi == null) {
                break;
            }
            if (roi.isArea() && roi.getType() != Roi.RECTANGLE) {
                IJ.run(imp, "Area to Line", "");
            }
            ProfilePlotAverage pp = new ProfilePlotAverage(imp, Prefs.verticalProfile || IJ.altKeyDown());
            y[i] = pp.getProfile();
            if (y[i] == null) {
                break;
            }
            if (y[i].length > maxX) {
                maxX = y[i].length;
            }
            if (freeYScale) {
                double[] a = Tools.getMinMax(y[i]);
                if (a[0] < minY) {
                    minY = a[0];
                }
                if (a[1] > maxY) {
                    maxY = a[1];
                }
            }
            double[] xx = new double[y[i].length];
            for (int j = 0; j < xx.length; j++) {
                xx[j] = j * xinc;
            }
            x[i] = xx;
        }
        String xlabel = "Distance (" + cal.getUnits() + ")";
        Plot plot = new Plot("Profiles", xlabel, "Value", x[0], y[0]);
        plot.setLimits(0, maxX * xinc, minY, maxY);
        for (int i = 1; i < indexes.length; i++) {
            plot.setColor(colors[i]);
            if (x[i] != null) {
                plot.addPoints(x[i], y[i], Plot.LINE);
            }
        }
        plot.setColor(colors[0]);
        if (x[0] != null) {
            plot.show();
        }
        imp.setSlice(currentSlice);
        if (indexes.length > 1) {
            IJ.run("Select None");
        }
        if (record()) {
            Recorder.record("roiManager", "Multi Plot");
        }
    }

    boolean drawOrFill(int mode) {
        int[] indexes = getSelectedIndexes();
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }
        ImagePlus imp = WindowManager.getCurrentImage();
        imp.deleteRoi();
        ImageProcessor ip = imp.getProcessor();
        ip.setColor(Toolbar.getForegroundColor());
        ip.snapshot();
        Undo.setup(Undo.FILTER, imp);
        Filler filler = mode == LABEL ? new Filler() : null;
        int slice = imp.getCurrentSlice();
        for (int i = 0; i < indexes.length; i++) {
            String name = (String) listModel.getElementAt(indexes[i]);
            Roi roi = (Roi) rois.get(name);
            int type = roi.getType();
            if (roi == null) {
                continue;
            }
            if (mode == FILL && (type == Roi.POLYLINE || type == Roi.FREELINE || type == Roi.ANGLE)) {
                mode = DRAW;
            }
            int slice2 = getSliceNumber(roi, name);
            if (slice2 >= 1 && slice2 <= imp.getStackSize()) {
                imp.setSlice(slice2);
                ip = imp.getProcessor();
                ip.setColor(Toolbar.getForegroundColor());
                if (slice2 != slice) {
                    Undo.reset();
                }
            }
            switch (mode) {
                case DRAW:
                    roi.drawPixels(ip);
                    break;
                case FILL:
                    ip.fill(roi);
                    break;
                case LABEL:
                    roi.drawPixels(ip);
                    filler.drawLabel(imp, ip, i + 1, roi.getBounds());
                    break;
            }
        }
        if (record() && (mode == DRAW || mode == FILL)) {
            Recorder.record("roiManager", mode == DRAW ? "Draw" : "Fill");
        }
        if (showAllCheckbox.getState()) {
            runCommand("show none");
        }
        imp.updateAndDraw();
        return true;
    }

    void setProperties(Color color, int lineWidth, Color fillColor) {
        boolean showDialog = color == null && lineWidth == -1 && fillColor == null;
        int[] indexes = getSelectedIndexes();
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }
        int n = indexes.length;
        if (n == 0) {
            return;
        }
        Roi rpRoi = null;
        String rpName = null;
        Font font = null;
        int justification = TextRoi.LEFT;
        double opacity = -1;
        int position = -1;
        int cpos = -1, zpos = -1, tpos = -1;
        if (showDialog) {
            String label = (String) listModel.getElementAt(indexes[0]);
            rpRoi = (Roi) rois.get(label);
            if (n == 1) {
                fillColor = rpRoi.getFillColor();
                rpName = rpRoi.getName();
            }
            if (rpRoi.getStrokeColor() == null) {
                rpRoi.setStrokeColor(Roi.getColor());
            }
            rpRoi = (Roi) rpRoi.clone();
            if (n > 1) {
                rpRoi.setName("range: " + (indexes[0] + 1) + "-" + (indexes[n - 1] + 1));
            }
            rpRoi.setFillColor(fillColor);
            RoiProperties rp = new RoiProperties("Properties", rpRoi);
            if (!rp.showDialog()) {
                return;
            }
            lineWidth = (int) rpRoi.getStrokeWidth();
            defaultLineWidth = lineWidth;
            color = rpRoi.getStrokeColor();
            fillColor = rpRoi.getFillColor();
            defaultColor = color;
            position = rpRoi.getPosition();
            cpos = rpRoi.getCPosition();
            zpos = rpRoi.getZPosition();
            tpos = rpRoi.getTPosition();
            if (rpRoi instanceof TextRoi) {
                font = ((TextRoi) rpRoi).getCurrentFont();
                justification = ((TextRoi) rpRoi).getJustification();
            }
            if (rpRoi instanceof ImageRoi) {
                opacity = ((ImageRoi) rpRoi).getOpacity();
            }
        }
        ImagePlus imp = WindowManager.getCurrentImage();
        if (n == getCount() && n > 1 && !IJ.isMacro()) {
            GenericDialog gd = new GenericDialog("ROI Manager");
            gd.addMessage("Apply changes to all " + n + " selections?");
            gd.showDialog();
            if (gd.wasCanceled()) {
                return;
            }
        }
        for (int i = 0; i < n; i++) {
            String label = (String) listModel.getElementAt(indexes[i]);
            Roi roi = (Roi) rois.get(label);
            if (roi == null) {
                continue;
            }
            //IJ.log("set "+color+"	 "+lineWidth+"	"+fillColor);
            if (color != null) {
                roi.setStrokeColor(color);
            }
            if (lineWidth >= 0) {
                roi.setStrokeWidth(lineWidth);
            }
            roi.setFillColor(fillColor);
            if (cpos > 0 || zpos > 0 || tpos > 0) {
                roi.setPosition(cpos, zpos, tpos);
            } else if (position != -1) {
                roi.setPosition(position);
            }
            if (roi instanceof TextRoi) {
                roi.setImage(imp);
                if (font != null) {
                    ((TextRoi) roi).setCurrentFont(font);
                }
                ((TextRoi) roi).setJustification(justification);
                roi.setImage(null);
            }
            if ((roi instanceof ImageRoi) && opacity != -1) {
                ((ImageRoi) roi).setOpacity(opacity);
            }
        }
        if (rpRoi != null && rpName != null && !rpRoi.getName().equals(rpName)) {
            rename(rpRoi.getName());
        }
        ImageCanvas ic = imp != null ? imp.getCanvas() : null;
        Roi roi = imp != null ? imp.getRoi() : null;
        boolean showingAll = ic != null && ic.getShowAllROIs();
        if (roi != null && (n == 1 || !showingAll)) {
            if (lineWidth >= 0) {
                roi.setStrokeWidth(lineWidth);
            }
            if (color != null) {
                roi.setStrokeColor(color);
            }
            if (fillColor != null) {
                roi.setFillColor(fillColor);
            }
            if (roi != null && (roi instanceof TextRoi)) {
                ((TextRoi) roi).setCurrentFont(font);
                ((TextRoi) roi).setJustification(justification);
            }
            if (roi != null && (roi instanceof ImageRoi) && opacity != -1) {
                ((ImageRoi) roi).setOpacity(opacity);
            }
        }
        if (lineWidth > 1 && !showingAll && roi == null) {
            showAll(SHOW_ALL);
            showingAll = true;
        }
        if (imp != null) {
            imp.draw();
        }
        if (record()) {
            if (fillColor != null) {
                Recorder.record("roiManager", "Set Fill Color", Colors.colorToString(fillColor));
            } else {
                Recorder.record("roiManager", "Set Color", Colors.colorToString(color != null ? color : Color.red));
                Recorder.record("roiManager", "Set Line Width", lineWidth);
            }
        }
    }

    void flatten() {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {
            IJ.noImage();
            return;
        }
        ImageCanvas ic = imp.getCanvas();
        if ((ic != null && ic.getShowAllList() == null) && imp.getOverlay() == null && imp.getRoi() == null) {
            error("Image does not have an overlay or ROI");
        } else {
            IJ.doCommand("Flatten"); // run Image>Flatten in separate thread
        }
    }

    public boolean getDrawLabels() {
        return labelsCheckbox.getState();
    }

    void combine() {
        ImagePlus imp = getImage();
        if (imp == null) {
            return;
        }
        int[] indexes = getSelectedIndexes();
        if (indexes.length == 1) {
            error("More than one item must be selected, or none");
            return;
        }
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }
        int nPointRois = 0;
        for (int i = 0; i < indexes.length; i++) {
            Roi roi = (Roi) rois.get((String) listModel.getElementAt(indexes[i]));
            if (roi.getType() == Roi.POINT) {
                nPointRois++;
            } else {
                break;
            }
        }
        if (nPointRois == indexes.length) {
            combinePoints(imp, indexes);
        } else {
            combineRois(imp, indexes);
        }
        if (record()) {
            Recorder.record("roiManager", "Combine");
        }
    }

    public Roi combineTrueRois(ImagePlus imp) {
        ShapeRoi s1 = null, s2 = null;
        ImageProcessor ip = null;
        int[] indexes = getAllIndexes();
        for (int i = 0; i < indexes.length; i++) {
            Roi roi = (Roi) rois.get((String) listModel.getElementAt(indexes[i]));
            if (!roi.isArea()) {
                if (ip == null) {
                    ip = new ByteProcessor(imp.getWidth(), imp.getHeight());
                }
                roi = convertLineToPolygon(roi, ip);
            }
            if (s1 == null) {
                if (roi instanceof ShapeRoi) {
                    s1 = (ShapeRoi) roi;
                } else {
                    s1 = new ShapeRoi(roi);
                }
                if (s1 == null) {
                    return null;
                }
            } else {
                if (roi instanceof ShapeRoi) {
                    s2 = (ShapeRoi) roi;
                } else {
                    s2 = new ShapeRoi(roi);
                }
                if (s2 == null) {
                    continue;
                }
                s1.or(s2);
            }
        }
        return s1;
    }

    public Roi combineHypRois(ImagePlus imp) {
        ShapeRoi s1 = null, s2 = null;
        ImageProcessor ip = null;
        int[] indexes = getAllIndexes2();
        for (int i = 0; i < indexes.length; i++) {
            Roi roi = (Roi) rois2.get((String) listModel2.getElementAt(indexes[i]));
            if (!roi.isArea()) {
                if (ip == null) {
                    ip = new ByteProcessor(imp.getWidth(), imp.getHeight());
                }
                roi = convertLineToPolygon(roi, ip);
            }
            if (s1 == null) {
                if (roi instanceof ShapeRoi) {
                    s1 = (ShapeRoi) roi;
                } else {
                    s1 = new ShapeRoi(roi);
                }
                if (s1 == null) {
                    return null;
                }
            } else {
                if (roi instanceof ShapeRoi) {
                    s2 = (ShapeRoi) roi;
                } else {
                    s2 = new ShapeRoi(roi);
                }
                if (s2 == null) {
                    continue;
                }
                s1.or(s2);
            }
        }
        return s1;
    }

    void combineRois(ImagePlus imp, int[] indexes) {
        ShapeRoi s1 = null, s2 = null;
        ImageProcessor ip = null;
        for (int i = 0; i < indexes.length; i++) {
            Roi roi = (Roi) rois.get((String) listModel.getElementAt(indexes[i]));
            if (!roi.isArea()) {
                if (ip == null) {
                    ip = new ByteProcessor(imp.getWidth(), imp.getHeight());
                }
                roi = convertLineToPolygon(roi, ip);
            }
            if (s1 == null) {
                if (roi instanceof ShapeRoi) {
                    s1 = (ShapeRoi) roi;
                } else {
                    s1 = new ShapeRoi(roi);
                }
                if (s1 == null) {
                    return;
                }
            } else {
                if (roi instanceof ShapeRoi) {
                    s2 = (ShapeRoi) roi;
                } else {
                    s2 = new ShapeRoi(roi);
                }
                if (s2 == null) {
                    continue;
                }
                s1.or(s2);
            }
        }
        if (s1 != null) {
            imp.setRoi(s1);
        }
    }

    Roi convertLineToPolygon(Roi roi, ImageProcessor ip) {
        if (roi == null) {
            return null;
        }
        ip.resetRoi();
        ip.setColor(0);
        ip.fill();
        ip.setColor(255);
        if (roi.getType() == Roi.LINE && roi.getStrokeWidth() > 1) {
            ip.fillPolygon(roi.getPolygon());
        } else {
            roi.drawPixels(ip);
        }
        //new ImagePlus("ip", ip.duplicate()).show();
        ip.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
        ThresholdToSelection tts = new ThresholdToSelection();
        return tts.convert(ip);
    }

    void combinePoints(ImagePlus imp, int[] indexes) {
        int n = indexes.length;
        Polygon[] p = new Polygon[n];
        int points = 0;
        for (int i = 0; i < n; i++) {
            Roi roi = (Roi) rois.get((String) listModel.getElementAt(indexes[i]));
            p[i] = roi.getPolygon();
            points += p[i].npoints;
        }
        if (points == 0) {
            return;
        }
        int[] xpoints = new int[points];
        int[] ypoints = new int[points];
        int index = 0;
        for (int i = 0; i < p.length; i++) {
            for (int j = 0; j < p[i].npoints; j++) {
                xpoints[index] = p[i].xpoints[j];
                ypoints[index] = p[i].ypoints[j];
                index++;
            }
        }
        imp.setRoi(new PointRoi(xpoints, ypoints, xpoints.length));
    }

    void and() {
        ImagePlus imp = getImage();
        if (imp == null) {
            return;
        }
        int[] indexes = getSelectedIndexes();
        if (indexes.length == 1) {
            error("More than one item must be selected, or none");
            return;
        }
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }
        ShapeRoi s1 = null, s2 = null;
        for (int i = 0; i < indexes.length; i++) {
            Roi roi = (Roi) rois.get((String) listModel.getElementAt(indexes[i]));
            if (roi == null || !roi.isArea()) {
                continue;
            }
            if (s1 == null) {
                if (roi instanceof ShapeRoi) {
                    s1 = (ShapeRoi) roi.clone();
                } else {
                    s1 = new ShapeRoi(roi);
                }
                if (s1 == null) {
                    return;
                }
            } else {
                if (roi instanceof ShapeRoi) {
                    s2 = (ShapeRoi) roi.clone();
                } else {
                    s2 = new ShapeRoi(roi);
                }
                if (s2 == null) {
                    continue;
                }
                s1.and(s2);
            }
        }
        if (s1 != null) {
            imp.setRoi(s1);
        }
        if (record()) {
            Recorder.record("roiManager", "AND");
        }
    }

    void xor() {
        ImagePlus imp = getImage();
        if (imp == null) {
            return;
        }
        int[] indexes = getSelectedIndexes();
        if (indexes.length == 1) {
            error("More than one item must be selected, or none");
            return;
        }
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }
        ShapeRoi s1 = null, s2 = null;
        for (int i = 0; i < indexes.length; i++) {
            Roi roi = (Roi) rois.get((String) listModel.getElementAt(indexes[i]));
            if (!roi.isArea()) {
                continue;
            }
            if (s1 == null) {
                if (roi instanceof ShapeRoi) {
                    s1 = (ShapeRoi) roi.clone();
                } else {
                    s1 = new ShapeRoi(roi);
                }
                if (s1 == null) {
                    return;
                }
            } else {
                if (roi instanceof ShapeRoi) {
                    s2 = (ShapeRoi) roi.clone();
                } else {
                    s2 = new ShapeRoi(roi);
                }
                if (s2 == null) {
                    continue;
                }
                s1.xor(s2);
            }
        }
        if (s1 != null) {
            imp.setRoi(s1);
        }
        if (record()) {
            Recorder.record("roiManager", "XOR");
        }
    }

    void addParticles() {
        String err = IJ.runMacroFile("ij.jar:AddParticles", null);
        if (err != null && err.length() > 0) {
            error(err);
        }
    }

    void sort() {
        int n = rois.size();
        if (n == 0) {
            return;
        }
        String[] labels = new String[n];
        int index = 0;
        for (Enumeration en = rois.keys(); en.hasMoreElements();) {
            labels[index++] = (String) en.nextElement();
        }
        listModel.clear();
        StringSorter.sort(labels);
        for (int i = 0; i < labels.length; i++) {
            listModel.addElement(labels[i]);
        }
        if (record()) {
            Recorder.record("roiManager", "Sort");
        }
    }

    void specify() {
        try {
            IJ.run("Specify...");
        } catch (Exception e) {
            return;
        }
        runCommand("add");
    }

    private static boolean channel = false, slice = true, frame = false;

    private void removePositions(int position) {
        int[] indexes = getSelectedIndexes();
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }
        if (indexes.length == 0) {
            return;
        }
        boolean removeChannels = position == CHANNEL;
        boolean removeFrames = position == FRAME;
        boolean removeSlices = !(removeChannels || removeFrames);
        if (position == SHOW_DIALOG) {
            ImagePlus imp = WindowManager.getCurrentImage();
            if (imp != null && !imp.isHyperStack()) {
                channel = false;
                slice = true;
                frame = false;
            }
            Font font = new Font("SansSerif", Font.BOLD, 12);
            GenericDialog gd = new GenericDialog("Remove");
            gd.setInsets(5, 15, 0);
            gd.addMessage("Remove positions for:      ", font);
            gd.setInsets(6, 25, 0);
            gd.addCheckbox("Channels:", channel);
            gd.setInsets(0, 25, 0);
            gd.addCheckbox("Slices:", slice);
            gd.setInsets(0, 25, 0);
            gd.addCheckbox("Frames:", frame);
            gd.showDialog();
            if (gd.wasCanceled()) {
                return;
            }
            removeChannels = gd.getNextBoolean();
            removeSlices = gd.getNextBoolean();
            removeFrames = gd.getNextBoolean();
            channel = removeChannels;
            slice = removeSlices;
            frame = removeFrames;
        }
        if (!removeChannels && !removeSlices && !removeFrames) {
            slice = true;
            return;
        }
        for (int i = 0; i < indexes.length; i++) {
            int index = indexes[i];
            String name = (String) listModel.getElementAt(index);
            Roi roi = (Roi) rois.get(name);
            int c = roi.getCPosition();
            int z = roi.getZPosition();
            int t = roi.getTPosition();
            if (c > 0 || t > 0) {
                if (removeChannels) {
                    c = 0;
                }
                if (removeSlices) {
                    z = 0;
                }
                if (removeFrames) {
                    t = 0;
                }
                roi.setPosition(c, z, t);
                continue;
            }
            int n = getSliceNumber(name);
            if (n == -1) {
                continue;
            }
            String name2 = name.substring(5, name.length());
            name2 = getUniqueName(name2);
            rois.remove(name);
            roi.setName(name2);
            roi.setPosition(0);
            rois.put(name2, roi);
            listModel.setElementAt(name2, index);
        }
        if (record()) {
            if (removeChannels) {
                Recorder.record("roiManager", "Remove Channel Info");
            }
            if (removeSlices) {
                Recorder.record("roiManager", "Remove Slice Info");
            }
            if (removeFrames) {
                Recorder.record("roiManager", "Remove Frame Info");
            }
        }
    }

    private void help() {
        String macro = "run('URL...', 'url=" + IJ.URL + "/docs/menus/analyze.html#manager');";
        new MacroRunner(macro);
    }

    private void labels() {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp != null) {
            showAllCheckbox.setState(true);
            labelsCheckbox.setState(true);
            showAll(LABELS);
        }
        IJ.doCommand("Labels...");
    }

    private void options() {
        Color c = ImageCanvas.getShowAllColor();
        GenericDialog gd = new GenericDialog("Options");
        //gd.addPanel(makeButtonPanel(gd), GridBagConstraints.CENTER, new Insets(5, 0, 0, 0));
        gd.addCheckbox("Associate \"Show All\" ROIs with slices", Prefs.showAllSliceOnly);
        gd.addCheckbox("Restore ROIs centered", restoreCentered);
        gd.addCheckbox("Use ROI names as labels", Prefs.useNamesAsLabels);
        gd.showDialog();
        if (gd.wasCanceled()) {
            if (c != ImageCanvas.getShowAllColor()) {
                ImageCanvas.setShowAllColor(c);
            }
            return;
        }
        Prefs.showAllSliceOnly = gd.getNextBoolean();
        restoreCentered = gd.getNextBoolean();
        Prefs.useNamesAsLabels = gd.getNextBoolean();
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp != null) {
            Overlay overlay = imp.getOverlay();
            if (overlay != null) {
                overlay.drawNames(Prefs.useNamesAsLabels);
            }
            imp.draw();
        }
        if (record()) {
            Recorder.record("roiManager", "Associate", Prefs.showAllSliceOnly ? "true" : "false");
            Recorder.record("roiManager", "Centered", restoreCentered ? "true" : "false");
            Recorder.record("roiManager", "UseNames", Prefs.useNamesAsLabels ? "true" : "false");
        }
    }

    Panel makeButtonPanel(GenericDialog gd) {
        Panel panel = new Panel();
        //buttons.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
        colorButton = new Button("\"Show All\" Color...");
        colorButton.addActionListener(this);
        panel.add(colorButton);
        return panel;
    }

    void setShowAllColor() {
        ColorChooser cc = new ColorChooser("\"Show All\" Color", ImageCanvas.getShowAllColor(), false);
        ImageCanvas.setShowAllColor(cc.getColor());
    }

    void split() {
        ImagePlus imp = getImage();
        if (imp == null) {
            return;
        }
        Roi roi = imp.getRoi();
        if (roi == null || roi.getType() != Roi.COMPOSITE) {
            error("Image with composite selection required");
            return;
        }
        boolean record = Recorder.record;
        Recorder.record = false;
        Roi[] rois = ((ShapeRoi) roi).getRois();
        for (int i = 0; i < rois.length; i++) {
            imp.setRoi(rois[i]);
            addRoi(false);
        }
        Recorder.record = record;
        if (record()) {
            Recorder.record("roiManager", "Split");
        }
    }

    public void showAll(int mode) {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {
            return;
        }
        boolean showAll = mode == SHOW_ALL;
        if (showAll) {
            imageID = imp.getID();
        }
        if (mode == LABELS) {
            showAll = true;
            if (record()) {
                Recorder.record("roiManager", "Show All with labels");
            }
        } else if (mode == NO_LABELS) {
            showAll = true;
            if (record()) {
                Recorder.record("roiManager", "Show All without labels");
            }
        }
        if (showAll) {
            imp.deleteRoi();
        }
        Roi[] rois = getRoisAsArray();
        Roi[] rois2 = getRois2AsArray();
        if (mode == SHOW_NONE) {
            removeOverlay(imp);
            imageID = 0;
        } else if (rois.length > 0 || rois2.length > 0) {
            Overlay overlay = newOverlay();
            for (int i = 0; i < rois.length; i++) {
                overlay.add(rois[i]);
            }
            for (int i = 0; i < rois2.length; i++) {
                rois2[i].setStrokeColor(Color.RED);
                overlay.add(rois2[i]);
            }
            setOverlay(imp, overlay);
        }
        if (record()) {
            Recorder.record("roiManager", showAll ? "Show All" : "Show None");
        }
    }

    void updateShowAll() {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {
            return;
        }
        if (showAllCheckbox.getState()) {
            Roi[] rois = getRoisAsArray();
            Roi[] rois2 = getRois2AsArray();
            if (rois.length > 0 || rois2.length > 0) {
                Overlay overlay = newOverlay();
                for (int i = 0; i < rois.length; i++) {
                    overlay.add(rois[i]);
                }
                for (int i = 0; i < rois2.length; i++) {
                    overlay.add(rois2[i]);
                }
                setOverlay(imp, overlay);
            } else {
                removeOverlay(imp);
            }

        } else {
            removeOverlay(imp);
        }
    }

    int[] getAllIndexes() {
        int count = getCount();
        int[] indexes = new int[count];
        for (int i = 0; i < count; i++) {
            indexes[i] = i;
        }
        return indexes;
    }

    public int[] getAllIndexes2() {
        int count = getCount2();
        int[] indexes = new int[count];
        for (int i = 0; i < count; i++) {
            indexes[i] = i;
        }
        return indexes;
    }

    ImagePlus getImage() {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {
            error("There are no images open.");
            return null;
        } else {
            return imp;
        }
    }

    boolean error(String msg) {
        new MessageDialog(this, "ROI Manager", msg);
        Macro.abort();
        return false;
    }

    public void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            instance = null;
        }
        if (!IJ.isMacro()) {
            ignoreInterrupts = false;
        }
    }

    /**
     * Returns a reference to the ROI Manager or null if it is not open.
     */
    public static RoiComparisonManager getInstance() {
        return (RoiComparisonManager) instance;
    }

    /**
     * Returns a reference to the ROI Manager window or to the macro batch mode
     * RoiManager, or null if neither exists.
     */
    public static RoiComparisonManager getInstance2() {
        RoiComparisonManager rm = getInstance();
        return rm;
    }

    /**
     * Obsolete
     *
     * @deprecated
     * @see #getCount
     * @see #getRoisAsArray
     */
    public Hashtable getROIs() {
        return rois;
    }

    /**
     * Obsolete
     *
     * @deprecated
     * @see #getCount
     * @see #getRoisAsArray
     * @see #getSelectedIndex
     */
    public List getList() {
        List awtList = new List();
        for (int i = 0; i < getCount(); i++) {
            awtList.add((String) listModel.getElementAt(i));
        }
        int index = getSelectedIndex();
        if (index >= 0) {
            awtList.select(index);
        }
        return awtList;
    }

    /**
     * Returns the ROI count.
     */
    public int getCount() {
        return listModel.getSize();
    }

    public int getCount2() {
        return listModel2.getSize();
    }

    /**
     * Returns the index of the specified Roi, or -1 if it is not found.
     */
    public int getRoiIndex(Roi roi) {
        int n = getCount();
        for (int i = 0; i < n; i++) {
            String label = (String) listModel.getElementAt(i);
            Roi roi2 = (Roi) rois.get(label);
            if (roi == roi2) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the index of the first selected ROI or -1 if no ROI is selected.
     */
    public int getSelectedIndex() {
        return list.getSelectedIndex();
    }

    /**
     * Returns a reference to the ROI at the specified index.
     */
    public Roi getRoi(int index) {
        if (index < 0 || index >= getCount()) {
            return null;
        }
        String label = (String) listModel.getElementAt(index);
        return (Roi) rois.get(label);
    }

    public Roi getRoi2(int index) {
        if (index < 0 || index >= getCount()) {
            return null;
        }
        String label = (String) listModel2.getElementAt(index);
        return (Roi) rois2.get(label);
    }

    /**
     * Returns the ROIs as an array.
     */
    public Roi[] getRoisAsArray() {
        int n = getCount();
        Roi[] array = new Roi[n];
        for (int i = 0; i < n; i++) {
            String label = (String) listModel.getElementAt(i);
            array[i] = (Roi) rois.get(label);
        }
        return array;
    }

    public Roi[] getRois2AsArray() {
        int n = getCount2();
        Roi[] array = new Roi[n];
        for (int i = 0; i < n; i++) {
            String label = (String) listModel2.getElementAt(i);
            array[i] = (Roi) rois2.get(label);
        }
        return array;
    }

    /**
     * Returns the selected ROIs as an array, or all the ROIs if none are
     * selected.
     */
    public Roi[] getSelectedRoisAsArray() {
        int[] indexes = getSelectedIndexes();
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }
        int n = indexes.length;
        Roi[] array = new Roi[n];
        for (int i = 0; i < n; i++) {
            String label = (String) listModel.getElementAt(indexes[i]);
            array[i] = (Roi) rois.get(label);
        }
        return array;
    }

    /**
     * Returns the name of the ROI with the specified index, or null if the
     * index is out of range.
     */
    public String getName(int index) {
        if (index >= 0 && index < getCount()) {
            return (String) listModel.getElementAt(index);
        } else {
            return null;
        }
    }

    /**
     * Returns the name of the ROI with the specified index. Can be called from
     * a macro using
     * <pre>call("ij.plugin.frame.RoiManager.getName", index)</pre> Returns
     * "null" if the Roi Manager is not open or index is out of range.
     */
    public static String getName(String index) {
        int i = (int) Tools.parseDouble(index, -1);
        RoiComparisonManager instance = getInstance2();
        if (instance != null && i >= 0 && i < instance.getCount()) {
            return (String) instance.listModel.getElementAt(i);
        } else {
            return "null";
        }
    }

    public void loadtrueroi() {
        String path = "";
        Macro.setOptions(null);
        String name = null;
        if (path == null || path.equals("")) {
            OpenDialog od = new OpenDialog("Open Selection(s)...", "");
            String directory = od.getDirectory();
            name = od.getFileName();
            if (name == null) {
                return;
            }
            path = directory + name;
        }
        if (Recorder.record && !Recorder.scriptMode()) {
            Recorder.record("roiManager", "Open", path);
        }
        if (path.endsWith(".zip")) {
            openZip(path);
            return;
        }
        Opener o = new Opener();
        if (name == null) {
            name = o.getName(path);
        }
        Roi roi = o.openRoi(path);
        if (roi != null) {
            if (name.endsWith(".roi")) {
                name = name.substring(0, name.length() - 4);
            }
            name = getUniqueName(name);
            listModel.addElement(name);
            rois.put(name, roi);
        }
        updateShowAll();

    }

    public void loadtrueroidir(String path, String name) {

        if (Recorder.record && !Recorder.scriptMode()) {
            Recorder.record("roiManager", "Open", path);
        }
        if (path.endsWith(".zip")) {
            openZip(path);
            return;
        }
        Opener o = new Opener();
        if (name == null) {
            name = o.getName(path);
        }
        Roi roi = o.openRoi(path);
        if (roi != null) {
            if (name.endsWith(".roi")) {
                name = name.substring(0, name.length() - 4);
            }
            name = getUniqueName(name);
            listModel.addElement(name);
            rois.put(name, roi);
        }
        updateShowAll();

    }

    public void loadroi() {
        String path = "";
        Macro.setOptions(null);
        String name = null;
        if (path == null || path.equals("")) {
            OpenDialog od = new OpenDialog("Open Selection(s)...", "");
            String directory = od.getDirectory();
            name = od.getFileName();
            if (name == null) {
                return;
            }
            path = directory + name;
        }
        if (Recorder.record && !Recorder.scriptMode()) {
            Recorder.record("roiManager", "Open", path);
        }
        if (path.endsWith(".zip")) {
            openZip2(path);
            return;
        }
        Opener o = new Opener();
        if (name == null) {
            name = o.getName(path);
        }
        Roi roi = o.openRoi(path);
        if (roi != null) {
            if (name.endsWith(".roi")) {
                name = name.substring(0, name.length() - 4);
            }
            name = getUniqueName(name);
            listModel2.addElement(name);
            rois2.put(name, roi);
        }
        updateShowAll();

    }

    public void loadroidir(String path, String name) {
        if (Recorder.record && !Recorder.scriptMode()) {
            Recorder.record("roiManager", "Open", path);
        }
        if (path.endsWith(".zip")) {
            openZip2(path);
            return;
        }
        Opener o = new Opener();
        if (name == null) {
            name = o.getName(path);
        }
        Roi roi = o.openRoi(path);
        if (roi != null) {
            if (name.endsWith(".roi")) {
                name = name.substring(0, name.length() - 4);
            }
            name = getUniqueName(name);
            listModel2.addElement(name);
            rois2.put(name, roi);
        }
        updateShowAll();

    }

    /**
     * Executes the ROI Manager "Add", "Add & Draw", "Update", "Delete",
     * "Measure", "Draw", "Show All", "Show None", "Fill", "Deselect", "Select
     * All", "Combine", "AND", "XOR", "Split", "Sort" or "Multi Measure"
     * command.	Returns false if <code>cmd</code> is not one of these strings.
     */
    public boolean runCommand(String cmd) {
        cmd = cmd.toLowerCase();
        macro = true;
        boolean ok = true;
        if (cmd.equals("add")) {
            boolean shift = IJ.shiftKeyDown();
            boolean alt = IJ.altKeyDown();
            if (Interpreter.isBatchMode()) {
                shift = false;
                alt = false;
            }
            //setRoiPosition();
            add(shift, alt);
        } else if (cmd.equals("add2")) {
            boolean shift2 = IJ.shiftKeyDown();
            boolean alt2 = IJ.altKeyDown();
            if (Interpreter.isBatchMode()) {
                shift2 = false;
                alt2 = false;
            }
            //setRoiPosition();
            add2(shift2, alt2);
        } else if (cmd.equals("add & draw")) {
            addAndDraw(false);
        } else if (cmd.equals("update")) {
            update(true);
        } else if (cmd.equals("update2")) {
            update(false);
        } else if (cmd.equals("delete")) {
            delete(false);
        } else if (cmd.equals("measure")) {
            measure(COMMAND);
        } else if (cmd.equals("draw")) {
            drawOrFill(DRAW);
        } else if (cmd.equals("fill")) {
            drawOrFill(FILL);
        } else if (cmd.equals("label")) {
            drawOrFill(LABEL);
        } else if (cmd.equals("and")) {
            and();
        } else if (cmd.equals("or") || cmd.equals("combine")) {
            combine();
        } else if (cmd.equals("xor")) {
            xor();
        } else if (cmd.equals("split")) {
            split();
        } else if (cmd.equals("sort")) {
            sort();
        } else if (cmd.startsWith("multi measure")) {
            multiMeasure(cmd);
        } else if (cmd.equals("multi plot")) {
            multiPlot();
        } else if (cmd.equals("show all")) {
            if (WindowManager.getCurrentImage() != null) {
                showAll(SHOW_ALL);
                showAllCheckbox.setState(true);
            }
        } else if (cmd.equals("show none")) {
            if (WindowManager.getCurrentImage() != null) {
                showAll(SHOW_NONE);
                showAllCheckbox.setState(false);
            }
        } else if (cmd.equals("show all with labels")) {
            labelsCheckbox.setState(true);
            showAll(LABELS);
            if (Interpreter.isBatchMode()) {
                IJ.wait(250);
            }
        } else if (cmd.equals("show all without labels")) {
            labelsCheckbox.setState(false);
            showAll(NO_LABELS);
            if (Interpreter.isBatchMode()) {
                IJ.wait(250);
            }
        } else if (cmd.equals("deselect") || cmd.indexOf("all") != -1) {
            if (IJ.isMacOSX()) {
                ignoreInterrupts = true;
            }
            select(-1);
            IJ.wait(50);
        } else if (cmd.equals("reset")) {
            if (IJ.isMacOSX() && IJ.isMacro()) {
                ignoreInterrupts = true;
            }
            listModel.clear();
            rois.clear();
            updateShowAll();
        } else if (cmd.equals("debug")) {
            //IJ.log("Debug: "+debugCount);
            //for (int i=0; i<debugCount; i++)
            //	IJ.log(debug[i]);
        } else if (cmd.equals("enable interrupts")) {
            ignoreInterrupts = false;
        } else if (cmd.equals("remove channel info")) {
            removePositions(CHANNEL);
        } else if (cmd.equals("remove slice info")) {
            removePositions(SLICE);
        } else if (cmd.equals("remove frame info")) {
            removePositions(FRAME);
        } else if (cmd.equals("list")) {
            listRois();
        } else if (cmd.equals("interpolate rois")) {
            interpolateRois();
        } else {
            ok = false;
        }
        macro = false;
        return ok;
    }

    /**
     * Executes the ROI Manager "Open", "Save" or "Rename" command. Returns
     * false if <code>cmd</code> is not "Open", "Save" or "Rename", or if an
     * error occurs.
     */
    public boolean runCommand(String cmd, String name) {
        cmd = cmd.toLowerCase();
        macro = true;
        if (cmd.equals("open")) {
            open(name);
            macro = false;
            return true;
        } else if (cmd.equals("save")) {
            save(name, false);
        } else if (cmd.equals("save selected")) {
            save(name, true);
        } else if (cmd.equals("rename")) {
            rename(name);
            macro = false;
            return true;
        } else if (cmd.equals("set color")) {
            Color color = Colors.decode(name, Color.cyan);
            setProperties(color, -1, null);
            macro = false;
            return true;
        } else if (cmd.equals("set fill color")) {
            Color fillColor = Colors.decode(name, Color.cyan);
            setProperties(null, -1, fillColor);
            macro = false;
            return true;
        } else if (cmd.equals("set line width")) {
            int lineWidth = (int) Tools.parseDouble(name, 0);
            if (lineWidth >= 0) {
                setProperties(null, lineWidth, null);
            }
            macro = false;
            return true;
        } else if (cmd.equals("associate")) {
            Prefs.showAllSliceOnly = name.equals("true") ? true : false;
            macro = false;
            return true;
        } else if (cmd.equals("centered")) {
            restoreCentered = name.equals("true") ? true : false;
            macro = false;
            return true;
        } else if (cmd.equals("usenames")) {
            Prefs.useNamesAsLabels = name.equals("true") ? true : false;
            macro = false;
            if (labelsCheckbox.getState()) {
                ImagePlus imp = WindowManager.getCurrentImage();
                if (imp != null) {
                    imp.draw();
                }
            }
            return true;
        }
        return false;
    }

    private boolean save(String name, boolean saveSelected) {
        if (!name.endsWith(".zip") && !name.equals("")) {
            return error("Name must end with '.zip'");
        }
        if (getCount() == 0) {
            return error("The selection list is empty.");
        }
        int[] indexes = null;
        if (saveSelected) {
            indexes = getSelectedIndexes();
            if (indexes.length == 0) {
                indexes = getAllIndexes();
            }
        } else {
            indexes = getAllIndexes();
        }
        boolean ok = false;
        if (name.equals("")) {
            ok = saveMultiple(indexes, null);
        } else {
            ok = saveMultiple(indexes, name);
        }
        macro = false;
        return ok;
    }

    /**
     * Adds the current selection to the ROI Manager, using the specified color
     * (a 6 digit hex string) and line width.
     */
    public boolean runCommand(String cmd, String hexColor, double lineWidth) {
        //setRoiPosition();
        if (hexColor == null && lineWidth == 1.0 && (IJ.altKeyDown() && !Interpreter.isBatchMode())) {
            addRoi(true);
        } else {
            Color color = hexColor != null ? Colors.decode(hexColor, Color.cyan) : null;
            addRoi(null, false, color, (int) Math.round(lineWidth));
        }
        return true;
    }

    private void setRoiPosition(ImagePlus imp, Roi roi) {
        if (imp == null || roi == null) {
            return;
        }
        if (imp.isHyperStack()) {
            roi.setPosition(imp.getChannel(), imp.getSlice(), imp.getFrame());
        } else if (imp.getStackSize() > 1) {
            roi.setPosition(imp.getCurrentSlice());
        }
    }

    /**
     * Assigns the ROI at the specified index to the current image.
     */
    public void select(int index) {
        select(null, index);
    }

    /**
     * Assigns the ROI at the specified index to 'imp'.
     */
    public void select(ImagePlus imp, int index) {
        selectedIndexes = null;
        int n = getCount();
        if (index < 0) {
            for (int i = 0; i < n; i++) {
                list.clearSelection();
            }
            if (record()) {
                Recorder.record("roiManager", "Deselect");
            }
            return;
        }
        if (index >= n) {
            return;
        }
        boolean mm = list.getSelectionMode() == ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
        if (mm) {
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }
        int delay = 1;
        long start = System.currentTimeMillis();
        while (true) {
            if (list.isSelectedIndex(index)) {
                break;
            }
            list.clearSelection();
            list.setSelectedIndex(index);
        }
        if (imp == null) {
            imp = WindowManager.getCurrentImage();
        }
        if (imp != null) {
            restore(imp, index, true);
        }
        if (mm) {
            list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        }
    }

    public void select(int index, boolean shiftKeyDown, boolean altKeyDown) {
        if (!(shiftKeyDown || altKeyDown)) {
            select(index);
        }
        ImagePlus imp = IJ.getImage();
        if (imp == null) {
            return;
        }
        Roi previousRoi = imp.getRoi();
        if (previousRoi == null) {
            select(index);
            return;
        }
        Roi.previousRoi = (Roi) previousRoi.clone();
        String label = (String) listModel.getElementAt(index);
        Roi roi = (Roi) rois.get(label);
        if (roi != null) {
            roi.setImage(imp);
            roi.update(shiftKeyDown, altKeyDown);
        }
    }

    public void setEditMode(ImagePlus imp, boolean editMode) {
        showAllCheckbox.setState(editMode);
        labelsCheckbox.setState(editMode);
        showAll(editMode ? LABELS : SHOW_NONE);
    }

    /*
     void selectAll() {
     boolean allSelected = true;
     int count = getCount();
     for (int i=0; i<count; i++) {
     if (!list.isIndexSelected(i))
     allSelected = false;
     }
     if (allSelected)
     select(-1);
     else {
     for (int i=0; i<count; i++)
     if (!list.isSelected(i)) list.select(i);
     }
     }
     */
    /**
     * Overrides PlugInFrame.close().
     */
    public void close() {
        super.close();
        instance = null;
        Prefs.saveLocation(LOC_KEY, getLocation());
        if (!showAllCheckbox.getState() || IJ.macroRunning()) {
            return;
        }
        int n = getCount();
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null || (imp.getCanvas() != null && imp.getCanvas().getShowAllList() == null)) {
            return;
        }
        if (n > 0) {
            GenericDialog gd = new GenericDialog("ROI Manager");
            gd.addMessage("Save the " + n + " displayed ROIs as an overlay?");
            gd.setOKLabel("Discard");
            gd.setCancelLabel("Save as Overlay");
            gd.showDialog();
            if (gd.wasCanceled()) {
                moveRoisToOverlay(imp);
            } else {
                removeOverlay(imp);
            }
        } else {
            imp.draw();
        }
    }

    /**
     * Moves all the ROIs to the specified image's overlay.
     */
    public void moveRoisToOverlay(ImagePlus imp) {
        if (imp == null) {
            return;
        }
        Roi[] rois = getRoisAsArray();
        int n = rois.length;
        Overlay overlay = imp.getOverlay();
        if (overlay == null) {
            overlay = newOverlay();
        }
        for (int i = 0; i < n; i++) {
            Roi roi = (Roi) rois[i].clone();
            if (!Prefs.showAllSliceOnly) {
                roi.setPosition(0);
            }
            if (roi.getStrokeWidth() == 1) {
                roi.setStrokeWidth(0);
            }
            overlay.add(roi);
        }
        imp.setOverlay(overlay);
        if (imp.getCanvas() != null) {
            setOverlay(imp, null);
        }
    }

    public void mousePressed(MouseEvent e) {
        int x = e.getX(), y = e.getY();
        if (e.isPopupTrigger() || e.isMetaDown()) {
            pm.show(e.getComponent(), x, y);
        }
    }

    public void mouseWheelMoved(MouseWheelEvent event) {
        synchronized (this) {
            int index = list.getSelectedIndex();
            int rot = event.getWheelRotation();
            if (rot < -1) {
                rot = -1;
            }
            if (rot > 1) {
                rot = 1;
            }
            index += rot;
            if (index < 0) {
                index = 0;
            }
            if (index >= getCount()) {
                index = getCount();
            }
            //IJ.log(index+"  "+rot);
            select(index);
            if (IJ.isWindows()) {
                list.requestFocusInWindow();
            }
        }
    }

    /**
     * Selects multiple ROIs, where 'indexes' is an array of integers, each
     * greater than or equal to 0 and less than the value returned by
     * getCount().
     */
    /**
     * Selects multiple ROIs, where 'indexes' is an array of integers, each
     * greater than or equal to 0 and less than the value returned by
     * getCount().
     *
     * @see #getSelectedIndexes
     * @see #getSelectedRoisAsArray
     * @see #getCount
     */
    public void setSelectedIndexes(int[] indexes) {
        int count = getCount();
        if (count == 0) {
            return;
        }
        for (int i = 0; i < indexes.length; i++) {
            if (indexes[i] < 0) {
                indexes[i] = 0;
            }
            if (indexes[i] >= count) {
                indexes[i] = count - 1;
            }
        }
        selectedIndexes = indexes;
        list.setSelectedIndices(indexes);
    }

    /**
     * Returns an array of all of the selected indexes.
     */
    public int[] getSelectedIndexes() {
        if (selectedIndexes != null) {
            int[] indexes = selectedIndexes;
            selectedIndexes = null;
            return indexes;
        } else {
            return list.getSelectedIndices();
        }
    }

    public int[] getSelectedIndexes2() {
        if (selectedIndexes != null) {
            int[] indexes = selectedIndexes;
            selectedIndexes = null;
            return indexes;
        } else {
            return list2.getSelectedIndices();
        }
    }

    private Overlay newOverlay() {
        Overlay overlay = OverlayLabels.createOverlay();
        overlay.drawLabels(labelsCheckbox.getState());
        if (overlay.getLabelFont() == null && overlay.getLabelColor() == null) {
            overlay.setLabelColor(Color.white);
            overlay.drawBackgrounds(true);
        }
        overlay.drawNames(Prefs.useNamesAsLabels);
        return overlay;
    }

    private void removeOverlay(ImagePlus imp) {
        if (imp != null && imp.getCanvas() != null) {
            setOverlay(imp, null);
        }
    }

    private void setOverlay(ImagePlus imp, Overlay overlay) {
        if (imp == null) {
            return;
        }
        ImageCanvas ic = imp.getCanvas();
        if (ic == null) {
            imp.setOverlay(overlay);
            return;
        }
        ic.setShowAllList(overlay);
        imp.draw();
    }

    private boolean record() {
        return Recorder.record && !IJ.isMacro();
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }
        if (getCount() == 0) {
            if (record()) {
                Recorder.record("roiManager", "Deselect");
            }
            return;
        }
        if (getCount2() == 0) {
            if (record()) {
                Recorder.record("roiManager", "Deselect");
            }
            return;
        }

        if (e.getSource().equals(list)) {
            int[] selected = list.getSelectedIndices();
            if (selected.length == 0) {
                return;
            }
            if (WindowManager.getCurrentImage() != null) {
                if (selected.length == 1) {
                    restore(getImage(), selected[0], true);
                }
                if (record()) {
                    String arg = Arrays.toString(selected);
                    if (!arg.startsWith("[") || !arg.endsWith("]")) {
                        return;
                    }
                    arg = arg.substring(1, arg.length() - 1);
                    arg = arg.replace(" ", "");
                    if (Recorder.scriptMode()) {
                        if (selected.length == 1) {
                            Recorder.recordCall("rm.select(" + arg + ");");
                        } else {
                            Recorder.recordCall("rm.setSelectedIndexes([" + arg + "]);");
                        }
                    } else {
                        if (selected.length == 1) {
                            Recorder.recordString("roiManager(\"Select\", " + arg + ");\n");
                        } else {
                            Recorder.recordString("roiManager(\"Select\", newArray(" + arg + "));\n");
                        }
                    }
                }
            }

//            list2.clearSelection();
//            list.setSelectedIndices(selected);
        } else if (e.getSource().equals(list2)) {

            int[] selected = list2.getSelectedIndices();
            if (selected.length == 0) {
                return;
            }
            if (WindowManager.getCurrentImage() != null) {
                if (selected.length == 1) {
                    restore2(getImage(), selected[0], true);
                }
                if (record()) {
                    String arg = Arrays.toString(selected);
                    if (!arg.startsWith("[") || !arg.endsWith("]")) {
                        return;
                    }
                    arg = arg.substring(1, arg.length() - 1);
                    arg = arg.replace(" ", "");
                    if (Recorder.scriptMode()) {
                        if (selected.length == 1) {
                            Recorder.recordCall("rm.select(" + arg + ");");
                        } else {
                            Recorder.recordCall("rm.setSelectedIndexes([" + arg + "]);");
                        }
                    } else {
                        if (selected.length == 1) {
                            Recorder.recordString("roiManager(\"Select\", " + arg + ");\n");
                        } else {
                            Recorder.recordString("roiManager(\"Select\", newArray(" + arg + "));\n");
                        }
                    }
                }
            }

//            list.clearSelection();
//            list2.setSelectedIndices(selected);
        }

    }

    public void windowActivated(WindowEvent e) {
        super.windowActivated(e);
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp != null) {
            if (imageID != 0 && imp.getID() != imageID) {
                showAll(SHOW_NONE);
                showAllCheckbox.setState(false);
            }
        }
    }

}
