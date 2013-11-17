package cs3246.a2.test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;

import cs3246.a2.ColorCoherenceVector;
import cs3246.a2.ColorHist;
import cs3246.a2.Constant;
import cs3246.a2.SobelOperator;
import cs3246.a2.Util;
import cs3246.a2.model.ImageIndex;
import cs3246.a2.model.ImageIndex;

public class GenerateImageIndex {

	public static void main(String[] args) {
		List<ImageIndex> images = new ArrayList<ImageIndex>();
		String IMAGE_FOLDER = args[0];
		try {
			for (int i = 1; i <= 400; i++) {
				try{
					String filename = i + ".jpg";
					System.out.println("Indexing " + filename);
					
					File file;
					BufferedImage bi;
					try{
						file = new File(IMAGE_FOLDER+filename);
						bi = ImageIO.read(file);
						bi = Util.convertColorspace(bi, BufferedImage.TYPE_INT_RGB);
					} catch (IIOException e){
						filename = i + ".png";
						System.out.println("File cannot found. Looking for: " + filename);
						file = new File(IMAGE_FOLDER+filename);
						bi = ImageIO.read(file);
					}
	
					ColorHist hist = new ColorHist();
					double[] histogramResult = hist.getFeature(bi);
	
					SobelOperator edge = new SobelOperator();
					double[] edgeDirectionResult = edge.getFeature(bi);
	
					ColorCoherenceVector ccv = new ColorCoherenceVector();
					double[] ccvResult = ccv.getFeature(bi);
	
					ImageIndex image = new ImageIndex(filename, ccvResult, histogramResult,
							edgeDirectionResult, "");
					images.add(image);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			ImageDBWriter writer = new ImageDBWriter(images,
					Constant.DB_FILE_NAME);
			writer.write();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
