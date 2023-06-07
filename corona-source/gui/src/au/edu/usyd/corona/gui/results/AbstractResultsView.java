package au.edu.usyd.corona.gui.results;


import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * Abstract base class implementation of {@link ResultsView} for all views to
 * extend off
 * 
 * @author Tim Dawborn
 */
abstract class AbstractResultsView implements ResultsView {
	protected final String name;
	
	protected AbstractResultsView(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	protected abstract void _dispose();
	
	public void dispose() {
		_dispose();
		if (getBackSide() != null)
			getBackSide().dispose();
		if (getFrontSide() != null)
			getFrontSide().dispose();
	}
	
	protected abstract void exportData(Writer out) throws IOException;
	
	public void exportData(File file) throws IOException {
		Writer writer = new PrintWriter(file);
		try {
			exportData(writer);
		}
		finally {
			writer.flush();
			writer.close();
		}
	}
}
