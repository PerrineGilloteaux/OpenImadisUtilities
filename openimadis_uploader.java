package plugins.perrine.openimadisutilities;

import com.strandgenomics.imaging.iclient.ImageSpace;
import com.strandgenomics.imaging.iclient.ImageSpaceObject;
import com.strandgenomics.imaging.iclient.PixelMetaData;
import com.strandgenomics.imaging.iclient.RecordBuilder;
import com.strandgenomics.imaging.icore.Dimension;

import com.strandgenomics.imaging.icore.ImageType;
import com.strandgenomics.imaging.icore.Site;
import com.strandgenomics.imaging.icore.SourceFormat;
import com.strandgenomics.imaging.icore.image.PixelArray;
import com.strandgenomics.imaging.icore.image.PixelDepth;
import com.strandgenomics.imaging.iclient.Project;

import com.strandgenomics.imaging.icore.Channel;


import icy.sequence.Sequence;
import icy.type.DataType;
import icy.system.thread.ThreadUtil;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
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




/**
 * TODO put an option and query(several GUID selection) OR GUID. For now one guid to be compaticble with cluster processing.
 * @author Perrine
 *
 */

public class openimadis_uploader extends EzPlug implements Block{
	EzVarInteger mamaguid=new EzVarInteger( "GUID from which the seqeunce was created");


	VarSequence inputsequence=new VarSequence("sequence to upload", null);
	EzVarSequence inputvarsequence=new EzVarSequence("input sequence");
	EzVarText choiceproject ;
	String[] listofprojectnames;
	private EzVar<Integer> prj;
	@Override
	public void clean() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void execute() {
		// TODO Auto-generated method stub
		if (isHeadLess()){
			uploadSequence(mamaguid.getValue(),inputsequence.getValue());
		}
		else {
			uploadSequence(choiceproject.getValue(),inputvarsequence.getValue());
		}



	}

	private void uploadSequence(final Integer mamaguid, Sequence sequencetoupload) {
		// TODO Auto-generated method stub
		ImageSpace ispace = ImageSpaceObject.getConnectionManager();

		com.strandgenomics.imaging.iclient.Record mamarecord = ispace.findRecordForGUID(mamaguid);

		Project prj = mamarecord.getParentProject();
		List<Channel> channels = new ArrayList<Channel>();
		for(int i=0;i<sequencetoupload.getSizeC();i++)
		{
			String name = sequencetoupload.getChannelName(i);
			Channel channel = new Channel(name);

			channels.add(channel);
		}

		List<Site> sites = new ArrayList<Site>();
		sites.add(new Site(0, "Site 0"));


		RecordBuilder rb = prj.createRecordBuilder(sequencetoupload.getName(), sequencetoupload.getSizeT(), sequencetoupload.getSizeZ(), channels, sites , sequencetoupload.getWidth(), sequencetoupload.getHeight(), getPixelDepth(sequencetoupload.getDataType_()), 1.0, 1.0, 1.0, ImageType.GRAYSCALE, new SourceFormat("IMG"), "", "/tmp", System.currentTimeMillis(), System.currentTimeMillis(), System.currentTimeMillis());
		double interval = sequencetoupload.getTimeInterval();
		double elapsedtime=interval;
		if (sequencetoupload.getSizeT()>1)
			elapsedtime=interval /(sequencetoupload.getSizeT()-1);
		OMEXMLMetadataImpl omeMetadata = sequencetoupload.getMetadata(); 
		Double exposureTime=1.0;
		try{
			exposureTime = (Double) omeMetadata.getPlaneExposureTime(0, 0).value();
		}
		catch (Exception e)  {
			exposureTime=1.0;
		}

		for(int time = 0; time<sequencetoupload.getSizeT();time++)
		{
			for(int slice = 0;slice<sequencetoupload.getSizeZ();slice++)
			{
				for(int channel = 0;channel<sequencetoupload.getSizeC();channel++)
				{

					PixelArray rawData = null;
					if(getPixelDepth(sequencetoupload.getDataType_()) == PixelDepth.BYTE)
						rawData = new PixelArray.Byte(((byte[])sequencetoupload.getDataCopyXY(time, slice, channel)), sequencetoupload.getWidth(), sequencetoupload.getHeight());
					else if(getPixelDepth(sequencetoupload.getDataType_()) == PixelDepth.SHORT)
						rawData = new PixelArray.Short(((short[])sequencetoupload.getDataCopyXY(time, slice, channel)), sequencetoupload.getWidth(), sequencetoupload.getHeight());
					else if(getPixelDepth(sequencetoupload.getDataType_()) == PixelDepth.INT)
						rawData = new PixelArray.Integer(((int[])sequencetoupload.getDataCopyXY(time, slice, channel)), sequencetoupload.getWidth(), sequencetoupload.getHeight());
					else
					{
						System.out.println("unknown type");
						return;
					}

					PixelMetaData pixelData = new PixelMetaData(new Dimension(time, slice, channel, 0), sequencetoupload.getPixelSizeX(),sequencetoupload.getPixelSizeY(), sequencetoupload.getPixelSizeZ(), elapsedtime,exposureTime, new Date());

					rb.addImageData(new Dimension(time, slice, channel, 0), rawData, pixelData );
				}
			}
		}

		com.strandgenomics.imaging.iclient.Record record = rb.commit();
		record.addCustomHistory("Record was uploaded using Icy Block from guid:"+mamaguid);
		mamarecord.addCustomHistory("A new record using Icy Block has been created guid:"+record.getGUID());
		System.out.println("Record correctly uploaded: new ID is : "+record.getGUID());
	}
	private void uploadSequence(String prjname, Sequence sequencetoupload) {
		// TODO Auto-generated method stub

		ImageSpace ispace = ImageSpaceObject.getConnectionManager();
		Project prj=ispace.findProject(prjname);



		List<Channel> channels = new ArrayList<Channel>();
		for(int i=0;i<sequencetoupload.getSizeC();i++)
		{
			String name = sequencetoupload.getChannelName(i);
			Channel channel = new Channel(name);

			channels.add(channel);
		}

		List<Site> sites = new ArrayList<Site>();
		sites.add(new Site(0, "Site 0"));

		
		RecordBuilder rb = prj.createRecordBuilder(sequencetoupload.getName(), sequencetoupload.getSizeT(), sequencetoupload.getSizeZ(), channels, sites , sequencetoupload.getWidth(), sequencetoupload.getHeight(), getPixelDepth(sequencetoupload.getDataType_()), 1.0, 1.0, 1.0, ImageType.GRAYSCALE, new SourceFormat("IMG"), "", "/tmp", System.currentTimeMillis(), System.currentTimeMillis(), System.currentTimeMillis());
		double interval = sequencetoupload.getTimeInterval();
		double elapsedtime=interval;
		if (sequencetoupload.getSizeT()>1)
			elapsedtime=interval /(sequencetoupload.getSizeT()-1);
		OMEXMLMetadataImpl omeMetadata = sequencetoupload.getMetadata(); 
		Double exposureTime=1.0;
		try{
			exposureTime = (Double) omeMetadata.getPlaneExposureTime(0, 0).value();
		}
		catch (Exception e)  {
			exposureTime=1.0;
		}

		for(int time = 0; time<sequencetoupload.getSizeT();time++)
		{
			for(int slice = 0;slice<sequencetoupload.getSizeZ();slice++)
			{
				for(int channel = 0;channel<sequencetoupload.getSizeC();channel++)
				{

					PixelArray rawData = null;
					if(getPixelDepth(sequencetoupload.getDataType_()) == PixelDepth.BYTE)
						rawData = new PixelArray.Byte(((byte[])sequencetoupload.getDataCopyXY(time, slice, channel)), sequencetoupload.getWidth(), sequencetoupload.getHeight());
					else if(getPixelDepth(sequencetoupload.getDataType_()) == PixelDepth.SHORT)
						rawData = new PixelArray.Short(((short[])sequencetoupload.getDataCopyXY(time, slice, channel)), sequencetoupload.getWidth(), sequencetoupload.getHeight());
					else if(getPixelDepth(sequencetoupload.getDataType_()) == PixelDepth.INT)
						rawData = new PixelArray.Integer(((int[])sequencetoupload.getDataCopyXY(time, slice, channel)), sequencetoupload.getWidth(), sequencetoupload.getHeight());
					else
					{
						System.out.println("unknown type");
						return;
					}

					PixelMetaData pixelData = new PixelMetaData(new Dimension(time, slice, channel, 0), sequencetoupload.getPixelSizeX(),sequencetoupload.getPixelSizeY(), sequencetoupload.getPixelSizeZ(), elapsedtime,exposureTime, new Date());

					rb.addImageData(new Dimension(time, slice, channel, 0), rawData, pixelData );
				}
			}
		}

		com.strandgenomics.imaging.iclient.Record record = rb.commit();
		record.addCustomHistory("Record was uploaded using Icy Block");

		System.out.println("Record correctly uploaded: new ID is : "+record.getGUID());
	}
	private PixelDepth getPixelDepth(DataType dataType_)
	{
		if(dataType_ == DataType.BYTE || dataType_ == DataType.UBYTE)
			return PixelDepth.BYTE;
		if(dataType_ == DataType.SHORT || dataType_ == DataType.USHORT)
			return PixelDepth.SHORT;
		if(dataType_ == DataType.INT || dataType_ == DataType.UINT)
			return PixelDepth.INT;
		throw new IllegalArgumentException("unknown data type");
	}



	@Override
	protected void initialize() {
		if (isHeadLess()){
		addEzComponent(mamaguid);
		}
		addEzComponent(inputvarsequence);

		if (!isHeadLess()){
			// get the list of project to choose from
			ImageSpace ispace = ImageSpaceObject.getConnectionManager();
			List<Project> listofproj = ispace.getActiveProjects();
			listofprojectnames=new String[listofproj.size()];
			for (int i=0; i<listofproj.size();i++){

				listofprojectnames[i]=listofproj.get(i).getName();
			}
			choiceproject= new EzVarText("Upload in project: ", listofprojectnames , 0, false);
			addEzComponent(choiceproject);
		}


	}



	@Override
	public void declareInput(VarList inputMap) {
		// TODO Auto-generated method stub

		inputMap.add("MAMAGUID", mamaguid.getVariable());
		inputMap.add("SequenceToUpload", inputsequence);

	}

	@Override
	public void declareOutput(VarList outputMap) {


	}
	private DataType getDataType(PixelDepth pixelDepth)
	{
		if(pixelDepth == PixelDepth.BYTE)
			return DataType.UBYTE;
		if(pixelDepth == PixelDepth.SHORT)
			return DataType.USHORT;
		if(pixelDepth == PixelDepth.INT)
			return DataType.UINT;
		throw new IllegalArgumentException("unknown data type");
	}





}
