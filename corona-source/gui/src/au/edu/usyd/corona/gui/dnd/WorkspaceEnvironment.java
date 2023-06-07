package au.edu.usyd.corona.gui.dnd;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import au.edu.usyd.corona.gui.GUIManager;
import au.edu.usyd.corona.gui.dnd.shapes.AggregateShape;
import au.edu.usyd.corona.gui.dnd.shapes.AttributeShape;
import au.edu.usyd.corona.gui.dnd.shapes.BooleanShape;
import au.edu.usyd.corona.gui.dnd.shapes.ComparatorShape;
import au.edu.usyd.corona.gui.dnd.shapes.DataTypeShape;
import au.edu.usyd.corona.gui.dnd.shapes.GroupingShape;
import au.edu.usyd.corona.gui.dnd.shapes.OperatorShape;
import au.edu.usyd.corona.gui.dnd.shapes.Shape;
import au.edu.usyd.corona.gui.util.DrawingConstants;

@SuppressWarnings("serial")
abstract class WorkspaceEnvironment extends JPanel implements SQLFragmentGenerator, DrawingConstants {
	protected static final String INSTRUCTIONS = "Drag items from the side bar onto the main panel. Drag and drop items ontop of each other in order to 'tree' them together. Double clicking on an item will delete it. Right clicking on an item will reverse the item's children.";
	
	protected String[] columnNames;
	
	protected final SidePanel sidePanel;
	protected final MainPanel mainPanel;
	protected final InstructionsPanel instructionsPanel;
	
	private final DragAndDropHandler dnd;
	
	private final Collection<SQLFragmentListener> listeners = new ArrayList<SQLFragmentListener>();
	
	protected WorkspaceEnvironment(int width, int height) {
		// Get the column names
		try {
			columnNames = GUIManager.getInstance().getRemoteSessionInterface().getAttributeNames();
		}
		catch (RemoteException e) {
			GUIManager.getInstance().getRemoteExceptionNotifier().notifyHandler(e);
		}
		
		// set up properties
		Dimension size = new Dimension(width, height);
		setPreferredSize(size);
		setMinimumSize(size);
		setMaximumSize(size);
		setSize(size);
		
		// sets up the inner panels
		setBackground(Color.WHITE);
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		// places the three panels in their correct positions 
		final int LEFT_WIDTH = 256;
		final int RIGHT_WIDTH = width - LEFT_WIDTH;
		final int INSTRUCTION_HEIGHT = 60;
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = c.gridy = 0;
		c.gridheight = 2;
		add(sidePanel = getSidePanel(LEFT_WIDTH, height), c);
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 0;
		c.gridheight = 1;
		add(instructionsPanel = new InstructionsPanel(RIGHT_WIDTH, INSTRUCTION_HEIGHT), c);
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 1;
		c.gridheight = 1;
		add(mainPanel = getMainPanel(RIGHT_WIDTH, height - INSTRUCTION_HEIGHT), c);
		
		// sets up the drag and drop listener framework
		dnd = new DragAndDropHandler(this);
		sidePanel.addMouseListener(dnd.sideListener);
		sidePanel.addMouseMotionListener(dnd.sideListener);
		mainPanel.addMouseListener(dnd.mainListener);
		mainPanel.addMouseMotionListener(dnd.mainListener);
	}
	
	public void subscribeSQLFragmentGeneratorListener(SQLFragmentListener listener) {
		listeners.add(listener);
	}
	
	protected abstract MainPanel getMainPanel(int width, int height);
	
	protected abstract SidePanel getSidePanel(int width, int height);
	
	protected abstract String getInstructions();
	
	protected class InstructionsPanel extends JPanel implements DrawingConstants {
		private final JTextArea textArea;
		
		public InstructionsPanel(int width, int height) {
			Dimension size = new Dimension(width, height);
			setPreferredSize(size);
			setMinimumSize(size);
			setMaximumSize(size);
			setSize(size);
			
			//setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			textArea = new JTextArea();
			textArea.setEditable(false);
			textArea.setFont(VSMALL_FONT);
			textArea.setLineWrap(true);
			textArea.setWrapStyleWord(true);
			textArea.setText(getInstructions());
			textArea.setSize(size);
			
			add(textArea);
			setBackground(WHITE);
		}
	}
	
	protected abstract class InnerPanel extends JPanel implements DrawingConstants {
		public List<Shape> shapes = new ArrayList<Shape>();
		public Shape dragShape = null;
		
		protected InnerPanel(int width, int height) {
			Dimension size = new Dimension(width, height);
			setPreferredSize(size);
			setMinimumSize(size);
			setMaximumSize(size);
			setSize(size);
		}
		
		@Override
		public void paint(Graphics _g) {
			Graphics2D g = (Graphics2D) _g;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
			if (getBorder() != null)
				getBorder().paintBorder(this, g, 0, 0, getWidth(), getHeight());
			
			// draw each shape
			for (Shape s : shapes)
				s.draw(g);
			
			// draw the drag shape
			if (dragShape != null)
				dragShape.draw(g);
		}
	}
	
	/**
	 * The class for the side panel of the workspace context
	 */
	protected class SidePanel extends InnerPanel {
		public SidePanel(int width, int height) {
			super(width, height);
			
			setBackground(WHITE);
			//setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createLoweredBevelBorder()));
		}
		
		protected int initBooleans(int y, int padding) {
			final String[] NAMES = {"AND", "OR"}; //, "NOT"};
			for (int x = padding + BooleanShape.WIDTH / 2, i = 0; i < NAMES.length; i++, x += BooleanShape.WIDTH + padding)
				shapes.add(new BooleanShape(x, y, NAMES[i], null));
			return BooleanShape.HEIGHT;
		}
		
		protected int initOperators(int y, int padding) {
			final String[] NAMES = {"+", "-", "*", "/"};
			for (int x = padding, i = 0; i < NAMES.length; i++, x += OperatorShape.SIZE + padding)
				shapes.add(new OperatorShape(x, y, NAMES[i], null));
			return OperatorShape.SIZE;
		}
		
		protected int initComparators(int y, int padding) {
			final String[] NAMES = {"<", "==", "!=", ">", "<=", "=>"};
			for (int x = padding, i = 0; i < 4; i++, x += ComparatorShape.SIZE + padding)
				shapes.add(new ComparatorShape(x, y, NAMES[i], null));
			y += padding + ComparatorShape.SIZE;
			int x = padding;
			x += padding + ComparatorShape.SIZE;
			shapes.add(new ComparatorShape(x, y, NAMES[4], null));
			x += padding + ComparatorShape.SIZE;
			shapes.add(new ComparatorShape(x, y, NAMES[5], null));
			return 2 * ComparatorShape.SIZE + padding;
		}
		
		protected int initAttributes(int y, int padding) {
			int placed = 0;
			while (placed < columnNames.length) {
				for (int x = padding, i = 0; i < 3 && placed < columnNames.length; i++, x += AttributeShape.WIDTH + padding) {
					shapes.add(new AttributeShape(x, y, columnNames[placed], null));
					placed++;
				}
				y += AttributeShape.HEIGHT + padding;
			}
			
			return (int) Math.ceil(columnNames.length / 3) * (AttributeShape.HEIGHT + 3 * padding);
		}
		
		protected int initAggregates(int y, int padding) {
			final String[] NAMES = {"COUNT", "MIN", "MAX", "SUM", "AVG"};
			for (int row = 0; row < 2; row++) {
				for (int x = padding, i = 0; i < 3 && 3 * row + i < NAMES.length; i++, x += AggregateShape.WIDTH + padding) {
					shapes.add(new AggregateShape(x, y, NAMES[row * 3 + i], null));
				}
				y += AggregateShape.HEIGHT + padding;
			}
			return 2 * AttributeShape.HEIGHT + padding;
		}
		
		protected int initDataType(int y, int padding) {
			shapes.add(new DataTypeShape(padding, y, "{number}", null));
			return DataTypeShape.HEIGHT + padding;
		}
	}
	
	/**
	 * The class for the main workspace panel
	 */
	protected class MainPanel extends InnerPanel implements SQLFragmentGenerator {
		public MainPanel(int width, int height) {
			super(width, height);
			
			setBackground(LIGHT_GREY);
			setBorder(BorderFactory.createLineBorder(BLACK));
		}
		
		public String getSQLFragment() {
			switch (shapes.size()) {
			case 0:
				return "";
			case 1:
				return shapes.iterator().next().getSQLFragment();
			default:
				return "<Invalid WHERE clause; a forest found (" + shapes.size() + ")>";
			}
		}
		
		public void subscribeSQLFragmentGeneratorListener(SQLFragmentListener listener) {
			// not implemented
		}
	}
	
	/**
	 * All possible states that the drag and drop can be in
	 */
	private enum DragAndDropStates {
		NO_STATE, CLICKED_IN_SIDE, EXIT_SIDE_WHILE_DRAGGING, DRAGGED_FROM_SIDE_TO_MAIN, CLICKED_IN_MAIN, EXIT_MAIN_WHILE_DRAGGING, DRAGGED_FROM_MAIN_TO_SIDE, DRAGGED_FROM_MAIN_TO_SIDE_EXIT_SIDE,
	};
	
	protected class DragAndDropHandler {
		public final SideListener sideListener;
		public final MainListener mainListener;
		
		private Shape dragShape;
		private Shape realShape;
		private final int[] dragOffset = new int[2];
		private final int[] oldPos = new int[2];
		
		private DragAndDropStates state;
		
		private DragAndDropHandler(WorkspaceEnvironment workspacePanel) {
			sideListener = new SideListener();
			mainListener = new MainListener(workspacePanel);
			clearState();
		}
		
		/**
		 * Resets the state of the Drag and Drop
		 */
		private void clearState() {
			state = DragAndDropStates.NO_STATE;
			dragShape = null;
			realShape = null;
			mainPanel.dragShape = null;
			sidePanel.dragShape = null;
			mainPanel.repaint();
			sidePanel.repaint();
		}
		
		/**
		 * The mouse event listener for the side panel
		 * 
		 * @author Tim Dawborn
		 */
		public class SideListener implements MouseMotionListener, MouseListener {
			
			public void mouseDragged(MouseEvent e) {
				if (dragShape != null) {
					Point p = e.getPoint();
					
					if (state == DragAndDropStates.CLICKED_IN_SIDE || state == DragAndDropStates.DRAGGED_FROM_MAIN_TO_SIDE) {
						dragShape.setX((int) p.getX() + dragOffset[0]);
						dragShape.setY((int) p.getY() + dragOffset[1]);
						sidePanel.repaint();
					}
					else if (state == DragAndDropStates.DRAGGED_FROM_SIDE_TO_MAIN) {
						mainListener.mouseDragged(e);
					}
				}
			}
			
			public void mouseExited(MouseEvent e) {
				if (state == DragAndDropStates.CLICKED_IN_SIDE)
					state = DragAndDropStates.EXIT_SIDE_WHILE_DRAGGING;
				else if (state == DragAndDropStates.DRAGGED_FROM_MAIN_TO_SIDE)
					state = DragAndDropStates.DRAGGED_FROM_MAIN_TO_SIDE_EXIT_SIDE;
				else
					state = DragAndDropStates.NO_STATE;
			}
			
			public void mousePressed(MouseEvent e) {
				Point p = e.getPoint();
				for (Shape s : sidePanel.shapes) {
					if (s.contains(p)) {
						realShape = s;
						dragShape = sidePanel.dragShape = s.getDrag();
						state = DragAndDropStates.CLICKED_IN_SIDE;
						
						dragOffset[0] = (int) (s.getBounds().getX() - p.getX());
						dragOffset[1] = (int) (s.getBounds().getY() - p.getY());
						
						break;
					}
				}
			}
			
			public void mouseReleased(MouseEvent e) {
				if (state == DragAndDropStates.DRAGGED_FROM_SIDE_TO_MAIN)
					mainListener.mouseReleased(e);
				else
					clearState();
			}
			
			public void mouseMoved(MouseEvent e) {
			}
			
			public void mouseClicked(MouseEvent e) {
			}
			
			public void mouseEntered(MouseEvent e) {
				boolean changed = true, updateOffset = false;
				
				if (state == DragAndDropStates.DRAGGED_FROM_SIDE_TO_MAIN) {
					state = DragAndDropStates.CLICKED_IN_SIDE;
					updateOffset = true;
				}
				else if (state == DragAndDropStates.EXIT_SIDE_WHILE_DRAGGING) {
					state = DragAndDropStates.CLICKED_IN_SIDE;
				}
				else if (state == DragAndDropStates.EXIT_MAIN_WHILE_DRAGGING) {
					state = DragAndDropStates.DRAGGED_FROM_MAIN_TO_SIDE;
					updateOffset = true;
				}
				else if (state == DragAndDropStates.DRAGGED_FROM_MAIN_TO_SIDE_EXIT_SIDE) {
					state = DragAndDropStates.DRAGGED_FROM_MAIN_TO_SIDE;
				}
				else
					changed = false;
				
				if (changed) {
					sidePanel.dragShape = dragShape;
					sidePanel.repaint();
					mainPanel.dragShape = null;
					mainPanel.repaint();
					if (updateOffset) {
						dragOffset[0] += mainPanel.getX();
						dragOffset[1] += mainPanel.getY();
					}
				}
			}
		}
		
		/**
		 * The mouse event listener for the main panel
		 * 
		 * @author Tim Dawborn
		 */
		public class MainListener implements MouseMotionListener, MouseListener {
			private final WorkspaceEnvironment workspacePanel;
			
			public MainListener(WorkspaceEnvironment workspacePanel) {
				this.workspacePanel = workspacePanel;
			}
			
			public void mouseDragged(MouseEvent e) {
				if (dragShape != null) {
					Point p = e.getPoint();
					
					if (state == DragAndDropStates.CLICKED_IN_MAIN || state == DragAndDropStates.DRAGGED_FROM_SIDE_TO_MAIN) {
						dragShape.setX((int) p.getX() + dragOffset[0]);
						dragShape.setY((int) p.getY() + dragOffset[1]);
						handleMouseovers(e.getPoint());
						mainPanel.repaint();
					}
					else if (state == DragAndDropStates.DRAGGED_FROM_MAIN_TO_SIDE) {
						sideListener.mouseDragged(e);
					}
				}
			}
			
			public void mouseExited(MouseEvent e) {
				if (state == DragAndDropStates.CLICKED_IN_MAIN)
					state = DragAndDropStates.EXIT_MAIN_WHILE_DRAGGING;
				else if (state == DragAndDropStates.DRAGGED_FROM_SIDE_TO_MAIN)
					state = DragAndDropStates.DRAGGED_FROM_SIDE_TO_MAIN;
				else
					state = DragAndDropStates.NO_STATE;
			}
			
			public void mousePressed(MouseEvent e) {
				Point p = e.getPoint();
				for (Shape s : mainPanel.shapes) {
					if (s.contains(p)) {
						realShape = s;
						dragShape = mainPanel.dragShape = s.getDrag();
						state = DragAndDropStates.CLICKED_IN_MAIN;
						
						dragOffset[0] = (int) (s.getBounds().getX() - p.getX());
						dragOffset[1] = (int) (s.getBounds().getY() - p.getY());
						oldPos[0] = (int) p.getX() + dragOffset[0];
						oldPos[1] = (int) p.getY() + dragOffset[1];
						
						break;
					}
				}
			}
			
			private Shape getDuplicate() {
				Shape theShape = realShape.duplicate();
				boolean good = true;
				if (theShape instanceof DataTypeShape) {
					String value = JOptionPane.showInputDialog(null, "Enter data type value", "DataTypeShape", JOptionPane.PLAIN_MESSAGE);
					if (value == null)
						good = false;
					else
						((DataTypeShape) theShape).setValue(value);
				}
				return (good) ? theShape : null;
			}
			
			public void mouseReleased(MouseEvent e) {
				// if we are in a state to do something
				if (state == DragAndDropStates.DRAGGED_FROM_SIDE_TO_MAIN || state == DragAndDropStates.CLICKED_IN_MAIN) {
					// goes through each shape and checks if it intersects, and can have a valid drag and drop operation performed
					Shape found = null;
					boolean intersected = false;
					for (Shape root : mainPanel.shapes) {
						for (Shape s : root) {
							if (s != realShape && s.intersects(dragShape.getBounds2D())) {
								if (s.canHaveDroppedOn(realShape))
									found = s;
								intersected = true;
								break;
							}
						}
						if (found != null)
							break;
					}
					
					// move the shape to the drag location, or perform the drag and drop action
					if (intersected) {
						if (found != null) {
							Shape theShape = realShape;
							if (state == DragAndDropStates.DRAGGED_FROM_SIDE_TO_MAIN) {
								theShape = getDuplicate();
							}
							else {
								mainPanel.shapes.remove(theShape);
							}
							if (theShape != null)
								found.doDropOn(theShape);
						}
					}
					else if (state == DragAndDropStates.CLICKED_IN_MAIN) {
						int[] deltaPos = {dragShape.getX() - oldPos[0], dragShape.getY() - oldPos[1]};
						realShape.updatePoint(deltaPos[0], deltaPos[1]);
						realShape.endMovement();
					}
					else if (state == DragAndDropStates.DRAGGED_FROM_SIDE_TO_MAIN) {
						Shape theShape = getDuplicate();
						if (theShape != null) {
							mainPanel.shapes.add(theShape);
							theShape.setPoint(dragShape.getX(), dragShape.getY());
							theShape.endMovement();
						}
					}
				}
				else if (state == DragAndDropStates.DRAGGED_FROM_MAIN_TO_SIDE) {
					if (realShape.isRoot())
						mainPanel.shapes.remove(realShape);
					else
						realShape.getParent().removeChild(realShape);
					mainPanel.repaint();
				}
				
				// update the listeners
				for (SQLFragmentListener l : listeners)
					l.actUponChange(workspacePanel);
				
				// resets the DnD state
				clearState();
			}
			
			public void mouseEntered(MouseEvent e) {
				boolean changed = true;
				DragAndDropStates oldState = state;
				
				if (state == DragAndDropStates.DRAGGED_FROM_MAIN_TO_SIDE || state == DragAndDropStates.EXIT_MAIN_WHILE_DRAGGING) {
					state = DragAndDropStates.CLICKED_IN_MAIN;
				}
				else if (state == DragAndDropStates.EXIT_SIDE_WHILE_DRAGGING) {
					state = DragAndDropStates.DRAGGED_FROM_SIDE_TO_MAIN;
				}
				else if (state == DragAndDropStates.DRAGGED_FROM_MAIN_TO_SIDE_EXIT_SIDE) {
					state = DragAndDropStates.CLICKED_IN_MAIN;
				}
				else
					changed = false;
				
				if (changed) {
					mainPanel.dragShape = dragShape;
					mainPanel.repaint();
					sidePanel.dragShape = null;
					sidePanel.repaint();
					if (oldState != DragAndDropStates.EXIT_MAIN_WHILE_DRAGGING) {
						dragOffset[0] -= mainPanel.getX();
						dragOffset[1] -= mainPanel.getY();
					}
				}
			}
			
			public void mouseMoved(MouseEvent e) {
				handleMouseovers(e.getPoint());
				mainPanel.repaint();
			}
			
			public void mouseClicked(MouseEvent e) {
				int button = e.getButton();
				int clicks = e.getClickCount();
				Point p = e.getPoint();
				
				// check to see if the click requires a special event
				boolean remove = button == MouseEvent.BUTTON1 && clicks == 2;
				boolean flip = button == MouseEvent.BUTTON3 && clicks == 1;
				
				// if we might be removing or flipping a tree node
				if (remove || flip) {
					// check for intersection of the cursor with a tree node
					Shape shape = null;
					for (Shape root : mainPanel.shapes) {
						for (Shape s : root) {
							if (s.contains(p)) {
								shape = s;
								break;
							}
						}
					}
					if (shape != null) {
						if (flip) {
							// swap the children around
							Shape[] children = shape.getChildren();
							Shape tmp = children[0];
							children[0] = children[1];
							children[1] = tmp;
							shape.setChildren(children);
						}
						else {
							// remove the node either from the forest or from the subtree
							if (shape.isRoot())
								mainPanel.shapes.remove(shape);
							else
								shape.getParent().removeChild(shape);
						}
						
						// update the UI and the listeners
						mainPanel.repaint();
						for (SQLFragmentListener l : listeners)
							l.actUponChange(workspacePanel);
					}
				}
			}
			
			private void handleMouseovers(Point p) {
				if (state == DragAndDropStates.DRAGGED_FROM_SIDE_TO_MAIN) {
					p.setLocation(p.getX() - mainPanel.getX(), p.getY() - mainPanel.getY());
				}
				
				for (Shape root : mainPanel.shapes) {
					Shape innerMost = null;
					for (Shape s : root) {
						GroupingShape grouper = s.getGrouper();
						if (grouper == null)
							continue;
						else if (grouper.contains(p))
							innerMost = s;
						s.setGrouperVisible(false);
					}
					
					if (innerMost != null)
						innerMost.setGrouperVisible(true);
				}
			}
		}
	}
	
}
