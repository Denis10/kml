import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.ogc.kml.KMLAbstractFeature;
import gov.nasa.worldwind.ogc.kml.KMLRoot;
import gov.nasa.worldwind.ogc.kml.impl.KMLController;
import gov.nasa.worldwind.render.Offset;
import gov.nasa.worldwind.retrieve.RetrievalService;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.util.layertree.KMLLayerTreeNode;
import gov.nasa.worldwind.util.layertree.KMLNetworkLinkTreeNode;
import gov.nasa.worldwind.util.layertree.LayerTree;
import gov.nasa.worldwindx.examples.ApplicationTemplate;
import gov.nasa.worldwindx.examples.kml.KMLApplicationController;
import gov.nasa.worldwindx.examples.util.BalloonController;
import gov.nasa.worldwindx.examples.util.HotSpotController;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author Denis
 */
public class KMLViewer extends ApplicationTemplate
{
    public static class AppFrame extends ApplicationTemplate.AppFrame
    {
        protected LayerTree layerTree;
        protected RenderableLayer hiddenLayer;

        protected HotSpotController hotSpotController;
        protected KMLApplicationController kmlAppController;
        protected BalloonController balloonController;
        
        protected JTree jTree;        
        protected DefaultTreeModel model;
        protected DefaultMutableTreeNode root;

        public AppFrame()
        {
            super(true, true, false); // Don't include the layer panel; we're using the on-screen layer tree.

            // Add the on-screen layer tree, refreshing model with the WorldWindow's current layer list. We
            // intentionally refresh the tree's model before adding the layer that contains the tree itself. This
            // prevents the tree's layer from being displayed in the tree itself.
            this.layerTree = new LayerTree(new Offset(20d, 160d, AVKey.PIXELS, AVKey.INSET_PIXELS));
           // this.layerTree.getModel().refresh(this.getWwd().getModel().getLayers());                                    
           
          //Create the nodes.
            DefaultMutableTreeNode top = new DefaultMutableTreeNode("KML");
            //createNodes(top);
     
            //Create a tree that allows one selection at a time.
            this.jTree = new JTree(top);
            jTree.getSelectionModel().setSelectionMode
                    (TreeSelectionModel.SINGLE_TREE_SELECTION);
            SelectableTreeCellRenderer renderer =
                    new SelectableTreeCellRenderer();
            jTree.setCellRenderer(renderer);
          //Create the scroll pane and add the tree to it.
            JScrollPane treeView = new JScrollPane(jTree);
            model = (DefaultTreeModel) jTree.getModel();
            root = (DefaultMutableTreeNode) model.getRoot();
            //model.insertNodeInto(new DefaultMutableTreeNode("another_child"), root, root.getChildCount());            
            this.layerPanel.removeAll();
            Dimension dim=new Dimension(200, getHeight());
            this.layerPanel.setPreferredSize(dim);            
            this.layerPanel.add(treeView);
            this.jTree.setShowsRootHandles(true);
                 
            

            // Set up a layer to display the on-screen layer tree in the WorldWindow. This layer is not displayed in
            // the layer tree's model. Doing so would enable the user to hide the layer tree display with no way of
            // bringing it back.
            this.hiddenLayer = new RenderableLayer();
            this.hiddenLayer.addRenderable(this.layerTree);
            //this.getWwd().getModel().getLayers().add(this.hiddenLayer);

            // Add a controller to handle input events on the layer selector and on browser balloons.
            this.hotSpotController = new HotSpotController(this.getWwd());

            // Add a controller to handle common KML application events.
            this.kmlAppController = new KMLApplicationController(this.getWwd());

            // Add a controller to display balloons when placemarks are clicked. We override the method addDocumentLayer
            // so that loading a KML document by clicking a KML balloon link displays an entry in the on-screen layer
            // tree.
            this.balloonController = new BalloonController(this.getWwd())
            {
                @Override
                protected void addDocumentLayer(KMLRoot document)
                {
                    addKMLLayer(document);
                }
            };

            // Give the KML app controller a reference to the BalloonController so that the app controller can open
            // KML feature balloons when feature's are selected in the on-screen layer tree.
            this.kmlAppController.setBalloonController(balloonController);

            // Size the World Window to take up the space typically used by the layer panel.
            Dimension size = new Dimension(1200, 700);
            this.setPreferredSize(size);
            this.pack();
            WWUtil.alignComponent(null, this, AVKey.CENTER);

            makeMenu(this);

            // Set up to receive SSLHandshakeExceptions that occur during resource retrieval.
            WorldWind.getRetrievalService().setSSLExceptionListener(new RetrievalService.SSLExceptionListener()
            {
                public void onException(Throwable e, String path)
                {
                    System.out.println(path);
                    System.out.println(e);
                }
            });
        }

        /**
         * Adds the specified <code>kmlRoot</code> to this app frame's <code>WorldWindow</code> as a new
         * <code>Layer</code>, and adds a new <code>KMLLayerTreeNode</code> for the <code>kmlRoot</code> to this app
         * frame's on-screen layer tree.
         * <p/>
         * This expects the <code>kmlRoot</code>'s <code>AVKey.DISPLAY_NAME</code> field to contain a display name
         * suitable for use as a layer name.
         *
         * @param kmlRoot the KMLRoot to add a new layer for.
         */
        protected void addKMLLayer(KMLRoot kmlRoot)
        {
            // Create a KMLController to adapt the KMLRoot to the World Wind renderable interface.
            KMLController kmlController = new KMLController(kmlRoot);

            // Adds a new layer containing the KMLRoot to the end of the WorldWindow's layer list. This
            // retrieves the layer name from the KMLRoot's DISPLAY_NAME field.
            RenderableLayer layer = new RenderableLayer();
            layer.setName((String) kmlRoot.getField(AVKey.DISPLAY_NAME));
            layer.addRenderable(kmlController);
            this.getWwd().getModel().getLayers().add(layer);
            
            //
            root.add(new DefaultMutableTreeNode((String) kmlRoot.getField(AVKey.DISPLAY_NAME)));
            model.reload(root);                            
            jTree.expandRow(0);
                                   
            // Adds a new layer tree node for the KMLRoot to the on-screen layer tree, and makes the new node visible
            // in the tree. This also expands any tree paths that represent open KML containers or open KML network
            // links.
            KMLLayerTreeNode layerNode = new KMLLayerTreeNode(layer, kmlRoot);
            this.layerTree.getModel().addLayer(layerNode);
            this.layerTree.makeVisible(layerNode.getPath());
            layerNode.expandOpenContainers(this.layerTree);

            // Listens to refresh property change events from KML network link nodes. Upon receiving such an event this
            // expands any tree paths that represent open KML containers. When a KML network link refreshes, its tree
            // node replaces its children with new nodes created from the refreshed content, then sends a refresh
            // property change event through the layer tree. By expanding open containers after a network link refresh,
            // we ensure that the network link tree view appearance is consistent with the KML specification.
            layerNode.addPropertyChangeListener(AVKey.RETRIEVAL_STATE_SUCCESSFUL, new PropertyChangeListener()
            {
                public void propertyChange(final PropertyChangeEvent event)
                {
                    if (event.getSource() instanceof KMLNetworkLinkTreeNode)
                    {
                        // Manipulate the tree on the EDT.
                        SwingUtilities.invokeLater(new Runnable()
                        {
                            public void run()
                            {
                                ((KMLNetworkLinkTreeNode) event.getSource()).expandOpenContainers(layerTree);
                                getWwd().redraw();
                            }
                        });
                    }
                }
            });
        }       
    }

    /** A <code>Thread</code> that loads a KML file and displays it in an <code>AppFrame</code>. */
    public static class WorkerThread extends Thread
    {
        /** Indicates the source of the KML file loaded by this thread. Initialized during construction. */
        protected Object kmlSource;
        /** Indicates the <code>AppFrame</code> the KML file content is displayed in. Initialized during construction. */
        protected AppFrame appFrame;

        /**
         * Creates a new worker thread from a specified <code>kmlSource</code> and <code>appFrame</code>.
         *
         * @param kmlSource the source of the KML file to load. May be a {@link File}, a {@link URL}, or an {@link
         *                  java.io.InputStream}, or a {@link String} identifying a file path or URL.
         * @param appFrame  the <code>AppFrame</code> in which to display the KML source.
         */
        public WorkerThread(Object kmlSource, AppFrame appFrame)
        {
            this.kmlSource = kmlSource;
            this.appFrame = appFrame;
        }

        /**
         * Loads this worker thread's KML source into a new <code>{@link gov.nasa.worldwind.ogc.kml.KMLRoot}</code>,
         * then adds the new <code>KMLRoot</code> to this worker thread's <code>AppFrame</code>. The
         * <code>KMLRoot</code>'s <code>AVKey.DISPLAY_NAME</code> field contains a display name created from either the
         * KML source or the KML root feature name.
         * <p/>
         * If loading the KML source fails, this prints the exception and its stack trace to the standard error stream,
         * but otherwise does nothing.
         */
        public void run()
        {
            try
            {
                KMLRoot kmlRoot = this.parse();

                // Set the document's display name
                kmlRoot.setField(AVKey.DISPLAY_NAME, formName(this.kmlSource, kmlRoot));

                // Schedule a task on the EDT to add the parsed document to a layer
                final KMLRoot finalKMLRoot = kmlRoot;
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        appFrame.addKMLLayer(finalKMLRoot);
                    }
                });
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        /**
         * Parse the KML document.
         *
         * @return The parsed document.
         *
         * @throws IOException        if the document cannot be read.
         * @throws XMLStreamException if document cannot be parsed.
         */
        protected KMLRoot parse() throws IOException, XMLStreamException
        {
            // KMLRoot.createAndParse will attempt to parse the document using a namespace aware parser, but if that
            // fails due to a parsing error it will try again using a namespace unaware parser. Note that this second
            // step may require the document to be read from the network again if the kmlSource is a stream.
            return KMLRoot.createAndParse(this.kmlSource);
        }
    }

    protected static String formName(Object kmlSource, KMLRoot kmlRoot)
    {
        KMLAbstractFeature rootFeature = kmlRoot.getFeature();

        if (rootFeature != null && !WWUtil.isEmpty(rootFeature.getName()))
            return rootFeature.getName();

        if (kmlSource instanceof File)
            return ((File) kmlSource).getName();

        if (kmlSource instanceof URL)
            return ((URL) kmlSource).getPath();

        if (kmlSource instanceof String && WWIO.makeURL((String) kmlSource) != null)
            return WWIO.makeURL((String) kmlSource).getPath();

        return "KML Layer";
    }

    protected static void makeMenu(final AppFrame appFrame)
    {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        //fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("KML/KMZ File", "kml", "kmz"));
        fileChooser.setFileFilter(new MyCustomFilter());

        JMenuBar menuBar = new JMenuBar();
        appFrame.setJMenuBar(menuBar);
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        JMenuItem openFileMenuItem = new JMenuItem(new AbstractAction("Open File...")
        {
            public void actionPerformed(ActionEvent actionEvent)
            {
                try
                {
                    int status = fileChooser.showOpenDialog(appFrame);
                    if (status == JFileChooser.APPROVE_OPTION)
                    {
                        for (File file : fileChooser.getSelectedFiles())
                        {
                            new WorkerThread(file, appFrame).start();                                                                                                              
                            /*appFrame.root.add(new DefaultMutableTreeNode(file.getName()));
                            appFrame.model.reload(appFrame.root);                            
                            appFrame.jTree.expandRow(0);*/                                                                    
                        }
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });

        fileMenu.add(openFileMenuItem);

        JMenuItem openURLMenuItem = new JMenuItem(new AbstractAction("Open URL...")
        {
            public void actionPerformed(ActionEvent actionEvent)
            {
                try
                {
                    String status = JOptionPane.showInputDialog(appFrame, "URL");
                    if (!WWUtil.isEmpty(status))
                    {
                        new WorkerThread(status.trim(), appFrame).start();                        
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });

        fileMenu.add(openURLMenuItem);
    }
    
   

    public static void main(String[] args)
    {
        //noinspection UnusedDeclaration
        final AppFrame af = (AppFrame) start("World Wind KML Viewer", AppFrame.class);
    }
    
}
