package plugins.perrine.openimadisutilities;


import com.strandgenomics.imaging.iclient.ImageSpace;
import com.strandgenomics.imaging.iclient.ImageSpaceObject;
import com.strandgenomics.imaging.iclient.PixelMetaData;
import com.strandgenomics.imaging.iclient.RecordBuilder;
import com.strandgenomics.imaging.icore.Dimension;

import com.strandgenomics.imaging.icore.ImageType;
import com.strandgenomics.imaging.icore.Site;
import com.strandgenomics.imaging.icore.SourceFormat;
import com.strandgenomics.imaging.icore.VODimension;
import com.strandgenomics.imaging.icore.image.PixelArray;
import com.strandgenomics.imaging.icore.image.PixelDepth;
import com.strandgenomics.imaging.icore.vo.Ellipse;
import com.strandgenomics.imaging.icore.vo.VisualObject;
import com.strandgenomics.imaging.iclient.Project;

import com.strandgenomics.imaging.icore.Channel;

import icy.gui.frame.progress.AnnounceFrame;
import icy.roi.ROI;
import icy.sequence.Sequence;
import icy.type.DataType;
import icy.type.point.Point5D;
import icy.system.thread.ThreadUtil;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import loci.formats.ome.OMEXMLMetadataImpl;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVar;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.adufour.ezplug.EzVarText;
import plugins.adufour.vars.lang.VarSequence;
import plugins.kernel.roi.descriptor.measure.ROIMassCenterDescriptorsPlugin;




/**
 * From a sequence with Roi on it (like from spot detector with export to roi enabled):  
 * ask for an annotation name (number of roi will be called), ask for a parent guid, 
 * upload the number of roi as field /value annotations and add a circle around the position
 * of the gravity center of the counted stuff.
 * also in block
 * @author  Perrine
 *
 */

public class openimadis_annotate extends EzPlug implements Block{
	EzVarInteger mamaguid=new EzVarInteger( "GUID from which the sequence was created");
	EzVarText annotationfield=new EzVarText("Name for the count of Rois");

	VarSequence inputsequence=new VarSequence("sequence where to read ROI", null);
	// for block processing
	EzVarSequence inputvarsequence=new EzVarSequence("input sequence with Rois");



	@Override
	public void clean() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void execute() {
		// TODO Auto-generated method stub
		if (!isHeadLess()){
			uploadRoisandannotationstoSequence(mamaguid.getValue(),inputvarsequence.getValue());
		}
		else
		{
			uploadRoisandannotationstoSequence(mamaguid.getValue(),inputsequence.getValue());
		}

		new AnnounceFrame("Done.");

	}

	private void uploadRoisandannotationstoSequence(final Integer mamaguid, Sequence value2) {
		// TODO Auto-generated method stub

		// get list of Rois from sequence
		ArrayList<ROI> listrois = value2.getROIs();
		int count = listrois.size();
		ImageSpace ispace = ImageSpaceObject.getConnectionManager();

		com.strandgenomics.imaging.iclient.Record mamarecord = ispace.findRecordForGUID(mamaguid);
		// clean the previous one if any

		// we clean it first 
		Map<String, Object> annotations =mamarecord.getUserAnnotations();

		Iterator<String> it = annotations.keySet().iterator();

		
		while(it.hasNext())
		{
			String key = it.next();
			if (key.compareTo(annotationfield.getValue())==0){

				Object value = annotations.get(key);

				mamarecord.removeUserAnnotation(annotationfield.getValue());
				
				mamarecord.addCustomHistory("Annotation "+ annotationfield.getValue()+"=" + value+   "have been DELETED from the record by ICY client.");
			}
		}

		mamarecord.addUserAnnotation(annotationfield.getValue(), count);
		
		mamarecord.addCustomHistory("Automatic annotation "+ annotationfield.getValue()+"=" + count+   " have been added to the record by ICY client.");
		// bonus track -> add the protocol as an attachment when headless
		double width=10;
		double length=10;
		
		if (count>0) //at least one roi to add:
		{
			mamarecord.deleteVisualOverlays(0, annotationfield.getValue());
			 mamarecord.createVisualOverlays(0, annotationfield.getValue());
			List<VisualObject> vObjects = new ArrayList<VisualObject>();//red on the center
			for (int r=0;r<listrois.size(); r++) {
				Point5D p3D = ROIMassCenterDescriptorsPlugin.computeMassCenter(listrois.get(r));
			double upperleftx=p3D.getX()-width/2;
		        double upperlefty=p3D.getY()-length/2; 
			
			
			
			
			Ellipse position2 = new Ellipse(upperleftx,upperlefty,width,length);
			position2.setPenColor(Color.RED);
		        position2.setPenWidth(2.0f); 
		        vObjects.add(position2) ;
			
			
			
		}
			mamarecord.addVisualObjects(vObjects, annotationfield.getValue(), new VODimension(0,0,0));
		}

	}





	@Override
	protected void initialize() {

		addEzComponent(mamaguid);
		addEzComponent(inputvarsequence);
		addEzComponent(annotationfield);



	}



	@Override
	public void declareInput(VarList inputMap) {
		// TODO Auto-generated method stub

		inputMap.add("MAMAGUID", mamaguid.getVariable());
		inputMap.add("SequenceToUpload", inputsequence);
		inputMap.add("annotationname", annotationfield.getVariable());
	}

	@Override
	public void declareOutput(VarList outputMap) {


	}




}