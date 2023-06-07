package au.edu.usyd.corona.gui.results;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.io.IOException;
import java.io.Writer;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;

import au.edu.usyd.corona.gui.GUIManager;
import au.edu.usyd.corona.gui.cache.CacheManager;
import au.edu.usyd.corona.gui.cache.ResultCache;
import au.edu.usyd.corona.gui.util.DrawingConstants;
import au.edu.usyd.corona.gui.util.FormattingUtils;
import au.edu.usyd.corona.server.grammar.Query;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.visualization.DefaultVisualizationModel;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationModel;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.AbstractModalGraphMouse;
import edu.uci.ics.jung.visualization.control.LayoutScalingControl;
import edu.uci.ics.jung.visualization.control.PickingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.ScalingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.TranslatingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse.ModeKeyAdapter;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.BasicVertexLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;

/**
 * A network tree view of the results for a query using <a
 * href="http://jung.sourceforge.net/" target="_blank">JUNG</a> to do the
 * visualisation.
 * 
 * @author Tim Dawborn
 * @author Glen Pink
 */
@SuppressWarnings("serial")
class NetworkTreeViewType extends ViewType implements DrawingConstants {
	NetworkTreeViewType(int queryId) {
		super("Network Tree", "images/networktree.png", queryId);
	}
	
	@Override
	public AbstractResultsView createNewView() {
		return new TreeView();
	}
	
	private class TreeView extends AbstractResultsView {
		private final FrontSide front;
		private final Map<Long, Long> childToParent = new HashMap<Long, Long>();
		
		TreeView() {
			super(NetworkTreeViewType.this.name);
			front = new MyFrontSide();
		}
		
		public BackSide getBackSide() {
			return null;
		}
		
		public FrontSide getFrontSide() {
			return front;
		}
		
		@Override
		protected void _dispose() {
			childToParent.clear();
		}
		
		@Override
		protected void exportData(Writer out) throws IOException {
			// not implemented
		}
		
		/**
		 * Class for the front side of network tree view
		 * 
		 * @author Tim Dawborn
		 */
		private class MyFrontSide extends FrontSide {
			MyFrontSide() {
				final ResultCache resultCache = CacheManager.getInstance().getResultCache();
				final Query query = getQuery();
				
				// grab a copy of the columns from the result context as we want to sort them. not taking a copy causes reference based woes
				String[] origColumnNames = resultCache.getColumnNames(query, "SELECT * FROM TABLE_" + query.getQueryID());
				String[] columns = new String[origColumnNames.length];
				System.arraycopy(origColumnNames, 0, columns, 0, columns.length);
				Arrays.sort(columns);
				
				// checks to see if we have the required columns to display this view
				int nodeIndex = Arrays.binarySearch(columns, "NODE");
				int parentIndex = Arrays.binarySearch(columns, "PARENT");
				
				if (nodeIndex >= 0 && parentIndex >= 0) {
					List<Object[]> rows = resultCache.getAllResults(query, "SELECT NODE, PARENT FROM TABLE_" + query.getQueryID());
					for (Object[] row : rows) {
						Long child = (Long) row[0];
						Long parent = (Long) row[1];
						if (!childToParent.containsKey(child))
							childToParent.put(child, parent);
					}
					add(new JUNGView<Integer>(new IntegerFactory(), new IEEENodeTransformer()));
				}
				else {
					JLabel label = new JLabel("You need at least the 'node' and 'parent' column to use this view");
					label.setForeground(RED);
					add(label, BorderLayout.CENTER);
				}
			}
			
			@Override
			public void update() {
			}
			
			@Override
			public boolean isImageExportable() {
				return false;
			}
			
			@Override
			public boolean isEditable() {
				return false;
			}
			
			@Override
			public boolean isDataExportable() {
				return false;
			}
			
			@Override
			protected void _dispose() {
				// do nothing
			}
		}
		
		private class JUNGView<E> extends JPanel {
			private final Forest<Long, E> graph = new DelegateForest<Long, E>();
			private final VisualizationViewer<Long, E> viewer;
			
			@SuppressWarnings("unchecked")
			public JUNGView(Factory<E> edgeFactory, Transformer<Long, String> vertexLabelTransformer) {
				// finds the root of the tree
				Set<Long> allParents = new HashSet<Long>(childToParent.values());
				for (Long child : childToParent.keySet())
					allParents.remove(child);
				
				// add the roots to the tree
				for (Long parent : allParents)
					graph.addVertex(parent);
				
				// bfs inserts the nodes
				Queue<Long> nodes = new LinkedList<Long>(allParents);
				while (!nodes.isEmpty()) {
					Long node = nodes.poll();
					for (Entry<Long, Long> e : childToParent.entrySet()) {
						if (e.getValue().equals(node)) {
							graph.addEdge(edgeFactory.create(), node, e.getKey());
							nodes.add(e.getKey());
						}
					}
				}
				
				// sets up the graph renderer
				final Layout<Long, E> layout = new TreeLayout<Long, E>(graph);
				final VisualizationModel<Long, E> model = new DefaultVisualizationModel<Long, E>(layout, new Dimension(400, 400));
				viewer = new VisualizationViewer<Long, E>(model);
				viewer.setBackground(WHITE);
				viewer.setGraphMouse(new MyGraphMouse());
				viewer.setVertexToolTipTransformer(new ToStringLabeller<Long>());
				viewer.getRenderer().setVertexLabelRenderer(new BasicVertexLabelRenderer<Long, E>(Position.CNTR));
				viewer.getRenderContext().setArrowFillPaintTransformer(new ConstantTransformer(Color.lightGray));
				viewer.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line<Long, E>());
				viewer.getRenderContext().setVertexLabelTransformer(vertexLabelTransformer);
				
				// adds to swing component
				GraphZoomScrollPane pane = new GraphZoomScrollPane(viewer);
				setLayout(new BorderLayout());
				add(pane, BorderLayout.CENTER);
			}
			
			private class MyGraphMouse extends AbstractModalGraphMouse {
				protected MyGraphMouse() {
					this(1.1f, 1 / 1.1f);
				}
				
				protected MyGraphMouse(float in, float out) {
					super(in, out);
					loadPlugins();
					setModeKeyListener(new ModeKeyAdapter(this));
				}
				
				@Override
				protected void loadPlugins() {
					add(new PickingGraphMousePlugin<String, E>()); // pick with left mouse button
					add(new TranslatingGraphMousePlugin(InputEvent.BUTTON3_MASK)); // make translation the right mouse button
					add(new ScalingGraphMousePlugin(new LayoutScalingControl(), 0, in, out)); // add zoom control via the scroll ball
				}
			}
		}
	}
	
	private static class IntegerFactory implements Factory<Integer> {
		private int i = 0;
		
		public Integer create() {
			return i++;
		}
	}
	
	private static class IEEENodeTransformer implements Transformer<Long, String> {
		private long basestation;
		
		public IEEENodeTransformer() {
			try {
				basestation = GUIManager.getInstance().getRemoteSessionInterface().getBasestationIEEEAddress();
			}
			catch (RemoteException e) {
				basestation = 0;
			}
		}
		
		public String transform(Long val) {
			String label = FormattingUtils.DOTTED_HEX_RENDERER.convert(val);
			if (val.longValue() == basestation)
				label += " (basestation)";
			return label;
		}
	}
}
