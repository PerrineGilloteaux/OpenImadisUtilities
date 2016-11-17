package plugins.perrine.openimadisutilities;


import icy.gui.frame.progress.ToolTipFrame;
import plugins.adufour.ezplug.EzPlug;

public class openimadisutilities extends EzPlug{

	@Override
	public void clean() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void execute() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void initialize() {
		// TODO Auto-generated method stub
		new ToolTipFrame(    			
				"<html>"+
						"<br>Available functions :"
						+ "<br>Login (to do once for one icy session)"
						+ "<br>downloader, with or without crop, also callable from Protocols as a block "
						+ "<br>uploader, to an identified project, "
						+ "<br> or to the same project that a parent image when called as block"
						+ "<br> annotate , to upload visual annotation and quantified annotations"+
						"</html>"
				);
	}

}
