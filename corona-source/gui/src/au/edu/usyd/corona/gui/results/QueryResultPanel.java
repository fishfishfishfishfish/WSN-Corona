package au.edu.usyd.corona.gui.results;


import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import au.edu.usyd.corona.gui.results.ResultsView.BackSide;
import au.edu.usyd.corona.gui.results.ResultsView.FrontSide;
import au.edu.usyd.corona.gui.util.DialogUtils;

/**
 * Each result frame contains one of these panels, which provides the facility
 * to "flip" between the front side and back side of each result view
 * 
 * @author Tim Dawborn
 * @author Glen Pink
 */
@SuppressWarnings("serial")
class QueryResultPanel extends JPanel {
	private final JTabbedPane tabs;
	private final List<ResultsView> views = new ArrayList<ResultsView>();
	
	QueryResultPanel() {
		// creates tabs
		tabs = new JTabbedPane(JTabbedPane.TOP);
		
		// adds tabs to panel
		setLayout(new BorderLayout());
		add(tabs, BorderLayout.CENTER);
	}
	
	/**
	 * Adds a new view instance to the tabs
	 * 
	 * @param view the new view instance to add
	 */
	void addNewView(ResultsView view) {
		synchronized (views) {
			int index = views.size();
			views.add(view);
			tabs.addTab(view.getName(), new TabContents(view));
			tabs.setSelectedIndex(index);
		}
	}
	
	void dispose() {
		tabs.removeAll();
		for (ResultsView view : views)
			view.dispose();
		views.clear();
	}
	
	/**
	 * A wrapper class around what is actually in each of the tabs, so that the
	 * "flipping" can be done easily / is encapsulated nicely
	 * 
	 * @author Tim Dawborn
	 */
	private class TabContents extends JPanel {
		public final ResultsView view;
		
		private final FrontButtonPanel frontButtons;
		private final BackButtonPanel backButtons;
		private final FrontSide frontContent;
		private final BackSide backContent;
		
		private boolean showBackSide;
		
		private JPanel content;
		private JPanel buttons;
		
		TabContents(ResultsView view) {
			this.view = view;
			
			backButtons = new BackButtonPanel();
			frontButtons = new FrontButtonPanel();
			
			frontContent = view.getFrontSide();
			backContent = view.getBackSide();
			
			// initial side to show
			showBackSide = frontContent.isEditable();
			
			updateComponents();
		}
		
		private void closeAction() {
			synchronized (views) {
				int index = views.indexOf(view);
				tabs.remove(index);
				views.remove(index);
			}
		}
		
		/**
		 * Updates the visual representation of the results panel
		 */
		private void updateComponents() {
			if (buttons != null) {
				remove(buttons);
				remove(content);
			}
			
			setLayout(new BorderLayout());
			if (showBackSide) {
				add(content = backContent, BorderLayout.CENTER);
				add(buttons = backButtons, BorderLayout.PAGE_END);
			}
			else {
				add(content = frontContent, BorderLayout.CENTER);
				add(buttons = frontButtons, BorderLayout.PAGE_END);
				
				// set the enabled/disabled'ness of the buttons
				frontButtons.exportData.setEnabled(frontContent.isDataExportable());
				frontButtons.exportImage.setEnabled(frontContent.isImageExportable());
				frontButtons.edit.setEnabled(frontContent.isEditable());
			}
		}
		
		private class FrontButtonPanel extends JPanel {
			final JButton edit = new JButton("Edit");
			final JButton exportData = new JButton("Export Data");
			final JButton exportImage = new JButton("Export Image");
			final JButton close = new JButton("Close");
			final JFileChooser fileChooser = new JFileChooser();
			
			public FrontButtonPanel() {
				setLayout(new FlowLayout(FlowLayout.RIGHT));
				add(exportImage);
				add(exportData);
				add(edit);
				add(close);
				
				exportImage.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						frontContent.doExportImage();
					}
				});
				exportData.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						int retVal = fileChooser.showSaveDialog(FrontButtonPanel.this);
						if (retVal == JFileChooser.APPROVE_OPTION) {
							try {
								File f = fileChooser.getSelectedFile();
								view.exportData(f);
								DialogUtils.showMultilineInformation(FrontButtonPanel.this, "Export Data", "Data successfully exported to '" + f.getAbsolutePath() + "'");
							}
							catch (IOException e) {
								DialogUtils.showMultilineError(FrontButtonPanel.this, "Export Data", "Error while exporting data:\n" + e.getMessage());
							}
						}
					}
				});
				edit.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						showBackSide = true;
						updateComponents();
						QueryResultPanel.this.repaint();
					}
				});
				close.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						closeAction();
					}
				});
			}
		}
		
		private class BackButtonPanel extends JPanel {
			final JButton ok = new JButton("Ok");
			final JButton close = new JButton("Close");
			
			public BackButtonPanel() {
				setLayout(new FlowLayout(FlowLayout.RIGHT));
				add(ok);
				add(close);
				
				ok.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						if (backContent.okClicked()) {
							showBackSide = false;
							updateComponents();
							frontContent.update();
							QueryResultPanel.this.repaint();
						}
					}
				});
				
				close.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						closeAction();
					}
				});
			}
		}
	}
}
