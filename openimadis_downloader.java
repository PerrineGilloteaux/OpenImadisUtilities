package plugins.perrine.openimadisutilities;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.strandgenomics.imaging.iclient.ImageSpace;
import com.strandgenomics.imaging.iclient.ImageSpaceObject;
import com.strandgenomics.imaging.iclient.PixelMetaData;
import com.strandgenomics.imaging.iclient.RecordBuilder;
import com.strandgenomics.imaging.icore.Dimension;
import com.strandgenomics.imaging.icore.IPixelData;
import com.strandgenomics.imaging.icore.ImageType;
import com.strandgenomics.imaging.icore.Site;
import com.strandgenomics.imaging.icore.SourceFormat;
import com.strandgenomics.imaging.icore.image.PixelArray;
import com.strandgenomics.imaging.icore.image.PixelDepth;
import com.strandgenomics.imaging.iclient.Project;
import com.strandgenomics.imaging.iclient.impl.ws.ispace.Record;
import com.strandgenomics.imaging.icore.Channel;


import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.system.thread.ThreadUtil;
import icy.type.DataType;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVar;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarListener;
import plugins.adufour.vars.lang.VarSequence;




public class openimadis_downloader extends EzPlug implements Block{



	/**
	 * TODO put an option and query(several GUID selection) OR GUID. For now one guid to be compaticble with cluster processing.
	 * @author Perrine
	 *
	 */


	EzVarInteger guid=new EzVarInteger( "GUIDtoprocess");

	EzVarInteger cropx1=new EzVarInteger( "Crop in X from ");
	EzVarInteger cropx2=new EzVarInteger( "To:  ");
	EzVarInteger cropy1=new EzVarInteger( "Crop in Y from ");
	EzVarInteger cropy2=new EzVarInteger( "To: ");

	VarSequence outputsequence=new VarSequence("Downloaded Sequence", null);
	@Override
	public void clean() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void execute() {
		// TODO Auto-generated method stub
		if (isHeadLess()){
			//nothing set in block-> get full size
			if (cropx2.getValue()==0||cropy2.getValue()==0){
				ImageSpace ispace = ImageSpaceObject.getConnectionManager();
				if (ispace==null){

				}
				else{
					com.strandgenomics.imaging.iclient.Record record = ispace.findRecordForGUID(guid.getValue());
					if (record==null){
						cropx2.setValue(0);
						cropy2.setValue(0);
					}
					else{
						cropx2.setValue(record.getImageWidth()-1);
						cropy2.setValue(record.getImageHeight()-1);
					}
				}}}
		outputsequence.setValue(downloadSequence(guid.getValue(), cropx1.getValue(), cropy1.getValue(), cropx2.getValue()-cropx1.getValue()+1, cropy2.getValue()-cropy1.getValue()+1));
		if (isHeadLess()){

		}else

			addSequence(outputsequence.getValue());

	}

	private Sequence downloadSequence(final Integer myguid, final Integer offsetX,final Integer offsetY,final Integer sizeX,final Integer sizeY) {
		// TODO Auto-generated method stub
		Future<Sequence> seqresult=ThreadUtil.bgRun(new Callable<Sequence>(){
			@Override
			public Sequence call(){
				ImageSpace ispace = ImageSpaceObject.getConnectionManager();


				com.strandgenomics.imaging.iclient.Record record = ispace.findRecordForGUID(myguid);
				if (record==null)
				{
					String user=ispace.getUser();
					System.out.println("this record id does not exist or you do not have access with your account, currently: "+user);
					return null ;
				}
				System.out.println("This record contains "+record.getFrameCount()+" time points "+record.getSliceCount()+" z planes and comes from Project: "+record.getParentProject()+ "\n Total Size is "+(record.getSliceCount()*record.getFrameCount()*record.getChannelCount()*record.getImageHeight()*record.getImageWidth()*record.getPixelDepth().getByteSize())/1000000+" MBytes.");
				Sequence seq = new Sequence();
				double averagevalue=0.0;
				float[] values= new float[record.getImageWidth()*record.getImageHeight()];
				int nChannel=record.getChannelCount();
				int nFrames=record.getFrameCount();
				int nSlices=record.getSliceCount();
				Rectangle myRoi=new Rectangle(offsetX,offsetY,sizeX,sizeY);
				try{
					for(int time = 0;time<nFrames;time++)
					{
						double elapsedtime=0;
						for(int slice = 0;slice<nSlices;slice++)
						{
							IcyBufferedImage img = new IcyBufferedImage((int)myRoi.getWidth(), (int) myRoi.getHeight(), nChannel, getDataType(record.getPixelDepth()));
							for(int channel = 0;channel<nChannel;channel++)
							{
								if (!isHeadLess())
									getUI().setProgressBarValue((double)(channel+slice*nChannel+time*nSlices*nChannel)/(double)(nChannel*nFrames*nSlices));
								IPixelData pixelData = record.getPixelData(new Dimension(time, slice, channel, 0));
								System.out.println(String.format("% ,.1f",100*(double)(channel+slice*nChannel+time*nSlices*nChannel)/(double)(nChannel*nFrames*nSlices))+ " % downloaded");

								//Object values = pixelData.getRawData().getPixelArray(); //old
								PixelArray pa;

								pa = (PixelArray)  pixelData.getRawData(myRoi);

								for (int i = 0; i < myRoi.getHeight()*myRoi.getWidth(); i++) {//n
									values[i] = (float) pa.getPixelValue(i);//n
								}//n 
								img.setDataXY(channel, values);//old
								elapsedtime=pixelData.getElapsedTime();

							}

							seq.addImage(time, img);

						}
						averagevalue=elapsedtime;
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}//n

				if (record.getFrameCount()>1)
					averagevalue=averagevalue/(record.getFrameCount()-1); //in ICY metadata indicated are the time interval (duration of the movie)
				seq.setName(record.getSourceFilename()+"_guid_"+myguid);

				//Set Meta Data (minimum: pixel size and time increment, computed in average: to be checked: image based metadata in ICY)

				double physicalSizeX=record.getPixelSizeAlongXAxis(); //in um
				double physicalSizeY=record.getPixelSizeAlongYAxis();
				double physicalSizeZ=record.getPixelSizeAlongZAxis();
				if (averagevalue>0)
					seq.setTimeInterval(averagevalue); //in seconds
				if (physicalSizeX>0)
					seq.setPixelSizeX(physicalSizeX);
				if (physicalSizeY>0)
					seq.setPixelSizeY(physicalSizeY);
				if (physicalSizeZ>0)
					seq.setPixelSizeZ(physicalSizeZ);
				return seq;
			}});
		try {
			return seqresult.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}



	@Override
	protected void initialize() {
		addEzComponent(guid);
		addEzComponent(cropx1);
		addEzComponent(cropx2);
		addEzComponent(cropy1);
		addEzComponent(cropy2);
		guid.addVarChangeListener(new EzVarListener<Integer>()
		{
			@Override
			public void variableChanged(EzVar<Integer> source, Integer newValue)
			{
				if ((newValue!=null)&&(newValue!=0)){
					// we get the metadata of this file
					ImageSpace ispace = ImageSpaceObject.getConnectionManager();
					if (ispace==null){

					}
					else{
						com.strandgenomics.imaging.iclient.Record record = ispace.findRecordForGUID(newValue);
						if (record==null){
							cropx2.setValue(0);
							cropy2.setValue(0);
						}
						else{
							cropx2.setValue(record.getImageWidth()-1);
							cropy2.setValue(record.getImageHeight()-1);
						}
					}
				}
			}


		});

	}

	@Override
	public void declareInput(VarList inputMap) {
		// TODO Auto-generated method stub

		inputMap.add("STRANDGUID", guid.getVariable());
		inputMap.add("cropx1", cropx1.getVariable());
		inputMap.add("cropx2",cropx2.getVariable());
		inputMap.add("cropy1", cropy1.getVariable());
		inputMap.add("cropy2", cropy2.getVariable());
	}

	@Override
	public void declareOutput(VarList outputMap) {
		outputMap.add("SEQUENCEdownloaded", outputsequence);

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


