package au.edu.usyd.corona.gui.dnd;


import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

@SuppressWarnings("serial")
class InstructionsPanel extends JPanel {
	public static String INSTRUCTIONS = "";
	
	private JEditorPane instructions;
	private final JScrollPane instructionsScroll;
	private String arrowImgPath = "";
	private String treeImgPath = "";
	
	InstructionsPanel(int width, int height) {
		Dimension size = new Dimension(width, height);
		setSize(size);
		setPreferredSize(size);
		setMaximumSize(size);
		setMinimumSize(size);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		arrowImgPath = getClass().getClassLoader().getResource("arrow1.png").getPath();
		treeImgPath = getClass().getClassLoader().getResource("tree.png").getPath();
		INSTRUCTIONS = "<html><h1 id=\"HowToBuildAQuery\">How To Build A Query</h1> <p> You can use this query builder to build a query to get the data you want from the Sunspot network. These instructions will guide you through the process. Note that this may be a quick or lengthy process depending on what data you require. </p> <h2 id=\"Step1:PlanYourQuery\">Step 1: Plan Your Query</h2> <p> The most important step is to determine what data you want to retrieve from the network. Determine what data items you want as well as the conditions for the retrieval of the data.</p> <h3 id=\"Step1a:Whatdatadoyouwant\">Step 1a: What data do you want?</h3> <p> Determine what data you want retrieved - the available data items are node id, parent node, time, light, temperature, switch 1, switch 2, memory, CPU and battery. You can also aggregate data (count, get minimum, maximum, sum and average).</p> <p> e.g. \"I want node ids and temperatures from nodes\".</p> <h3 id=\"Step1b:Whendoyouwantitretrieved\">Step 1b: When do you want it retrieved?</h3> <p> Determine what conditions you want to apply for data retrieval. These can be quite complex, so make sure you clearly determine what you require.</p> <p> e.g. \"Where temperature &gt; 10 and light &lt; 50\".</p> <h3 id=\"Step1c:Doyouwantdatatobegrouped\">Step 1c: Do you want data to be grouped?</h3> <p> If you want data to be returned in grouped by data types, then define data types to group on. </p> <p> e.g. \"I want data returned grouped by each parent\". </p> <h3 id=\"Step1d:Whatconditionsdoyouwantdatatobereturnedaftergrouping\">Step 1d: What conditions do you want data to be returned after grouping?</h3> <p> If you have defined data types to group on then you can set additional conditions on data after grouping. This is done in the same fashion as Step 1b. </p> <p> e.g. \"Where parent &gt; node\".</p> <h3 id=\"Step1e:Howdoyouwanttorunthequery\">Step 1e: How do you want to run the query?</h3> <p> Determine how many times you want to run the query, the time between each run, and the time before the query starts running.</p> <h2 id=\"Step2:BuildYourQuery\">Step 2: Build Your Query</h2> <p> Now that you have determined what you want to retrieve, you can use the query builder to build the query.</p> <p> The above steps each correspond to a tab on this query builder, in order from left to right (1a: \"Select Data, 1b: \"Set Conditions For Data\", 1c: \"Group Data\", 1d: \"Set Conditions For Groups\", 1e: \"Query Run Properties\"). You can build the components of the query that you require as determined above.</p> <p> For each tab that you require: </p> <ul><li>Drag items from the side bar onto the main panel. </li></ul><p> <img src=\"file://" + arrowImgPath + "\" alt=\"Drag and drop arrow.\" title=\"Drag and drop arrow.\" /> </p> <ul><li>Drag and drop items on top of each other in order to 'tree' them together. Double clicking on an item will delete it. </li></ul><p><img src=\"file://" + treeImgPath + "\" alt=\"Tree structure.\" title=\"Tree structure.\" /> </p> <ul><li>Right clicking on an item will reverse the item's children.</li></ul><p> For 1a and 1c you only need to drag elements into the main panel that you require. For 1b and 1d you need to build a structure that encapsulates all the conditions that you require.</p> <h2 id=\"Step3:RunTheQuery\">Step 3: Run The Query</h2> <p> After you have built the query, close the window and your query will appear in the SQL entry area. You can now click \"Run Query\" to run your query.</p></html>";
		
		instructions = new JEditorPane("text/html", INSTRUCTIONS);
		instructions.setEditable(false);
		
		instructions.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		instructionsScroll = new JScrollPane(instructions);
		
		instructionsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		add(instructionsScroll, BorderLayout.PAGE_START);
	}
}
