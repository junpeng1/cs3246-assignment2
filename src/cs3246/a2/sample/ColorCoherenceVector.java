package cs3246.a2.sample;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.*;

public class ColorCoherenceVector {

	public static BufferedImage img;
	public static int[] originalImage;
	public static int[] currentImage;
	public static int[][]  colorTagged;
	public static int numOfDifferentAreas;
	public static double[] alpha;
	public static double[] beta;
	private static final int NUM_OF_FILES = 30;
	private static final String FILE_PATH = "./../image/";
	
	/**
	 * We first blur the image slightly by replacing pixel values with the average value in a small local neighborhood
	 * currently including the 8 adjacent pixels). This eliminates small vari- ations between neighboring pixels. 
	 * @param w
	 * @param h
	 */
	private static void applyAverageFilter(int w, int h){
		for (int i = 1; i < h - 1; i++){
			for (int j = 1; j < w - 1; j++){
				
				//System.out.println(i + " " + j + " " + " " + w + " " + h);
                int addr = j + i * w;
                
                
				for (int k = 0; k < 3; k++){
					int shift = 8 * k;
	                int color = + originalImage[addr - 1] >> shift
							+ originalImage[addr - w] >> shift
							+ originalImage[addr - 1 - w] >> shift
									
							+ originalImage[addr + 1] >> shift
							+ originalImage[addr + w] >> shift
							+ originalImage[addr + 1 + w] >> shift
									
							+ originalImage[addr - 1 + w] >> shift
							+ originalImage[addr - w + 1] >> shift;
					color /= 8;
					currentImage[addr] += color << shift;
				}						
			}
		}
		
		img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, w, h, currentImage, 0, w);  
	}
	
	/**
	 * We first blur the image slightly by Gaussian Filter.
	 * This eliminates small variations between neighboring pixels.
	 * Algorithm from: http://en.wikipedia.org/wiki/Gaussian_blur and 
	 * http://d.hatena.ne.jp/nowokay/touch/20081007
	 * @param w
	 * @param h
	 */
	private static void applyGaussianFilter(int w, int h){		
		
		// The pre-defined Gaussian Filter
		int[][] GaussianFilter = {
            {1, 2, 1},
            {2, 4, 2},
            {1, 2, 1}};
		
		for(int x = 0; x < w; x++){
        	for(int y = 0; y < h; y++){
                
        		int tr = 0;
                int tg = 0;
                int tb = 0;
                int t = 0;
                
                for(int i = -1; i < 2; i++){
                    for(int j = -1; j < 2; j++){
                        
                    	if(y + i < 0 || x + j < 0 || y + i >= h || x + j >= w) continue;
                        
                        t += GaussianFilter[i + 1][j + 1];
                        int currentAddress = (x + j) + (y + i) * w;
                        int currentGFilter = GaussianFilter[i + 1][j + 1];
                        tr += currentGFilter * ((originalImage[currentAddress] >> 16) & 255);
                        tg += currentGFilter * ((originalImage[currentAddress] >> 8)  & 255);
                        tb += currentGFilter * ( originalImage[currentAddress]        & 255);
                    }
                }
                currentImage[x + y * w] = ((tr / t) << 16) + ((tg / t) << 8) + tb / t;
            }
        }
        img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, w, h, currentImage, 0, w);  
	}
	
	private static void reduceColor(){
		// 192 in binary: 11000000
		int flag = 192;
		
		for(int i = 0; i < currentImage.length; ++i){
            int r = (currentImage[i] >> 16) & flag;
            int g = (currentImage[i] >> 8) & flag;
            int b = currentImage[i] & flag;
            currentImage[i] = (r << 16) + (g << 8) + b;
        }
	}
	
	private static void tagColor(int w, int h){

        colorTagged = new int[w][h];
        numOfDifferentAreas = 0;
        for(int y = 0; y < h; ++y){
            for(int x = 0; x < w; ++x){
                int col = currentImage[y * w + x];
                if(y > 0){
                    if(x > 0){
                        if(currentImage[(y - 1) * w + x - 1] == col){
                            // With the upper-left
                            colorTagged[x][y] = colorTagged[x - 1][y - 1];
                            continue;
                        }
                    }
                    if(currentImage[(y - 1) * w + x] == col){
                        // With the above
                        colorTagged[x][y] = colorTagged[x][y - 1];
                        continue;
                    }
                    if(x < w - 1){
                        if(currentImage[(y - 1) * w + x + 1] == col){
                            // With the right
                            colorTagged[x][y] = colorTagged[x + 1][y - 1];
                            continue;
                        }
                    }
                }
                if(x > 0){
                    if(currentImage[y * w + x - 1] == col){
                        // With the left
                        colorTagged[x][y] = colorTagged[x - 1][y];
                        continue;
                    }
                }
                colorTagged[x][y] = numOfDifferentAreas;
                numOfDifferentAreas++;
            }
        }
	}
	
	private static void computeCoherence(int w, int h){
		int[] count = new int[numOfDifferentAreas];
        int[] color = new int[numOfDifferentAreas];
        for(int x = 0; x < w; ++x){
            for(int y = 0; y < h; ++y){
                count[colorTagged[x][y]]++;
                color[colorTagged[x][y]] = currentImage[y * w + x];
            }
        }
        
        alpha = new double[64];
        beta = new double[64];
        
        for(int i = 0; i < numOfDifferentAreas; ++i){
            int d = color[i];
            color[i] = (((d >> 22) & 3) << 4) + (((d >> 14) & 3) << 2) + ((d >> 6) & 3);
            if(count[i] < 20){
                beta[color[i]] ++;
            }else{
                alpha[color[i]] ++;
            }
        }
	}
	
	public static void computeCCV(String fileName) throws IOException{

        BufferedImage imgsrc = ImageIO.read(new File(fileName));
        int w = imgsrc.getWidth();
        int h = imgsrc.getHeight();
        
        img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D grp = (Graphics2D) img.getGraphics();
        grp.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        grp.drawImage(imgsrc, 0, 0, w, h, null);
        grp.dispose();
        
		originalImage = img.getRGB(0, 0, w, h, null, 0, w);
        currentImage = new int[originalImage.length];
        
        // Gaussian Filter
        applyGaussianFilter(w, h);
        //applyAverageFilter(w, h);
        
        // Color Reduction
        reduceColor();
        
        // Tagging
        tagColor(w, h);
        
        // Aggregate
        computeCoherence(w, h);
        
        // Normalize
        for (int i = 0; i < alpha.length; ++i){
        	if(alpha[i] == 0 && beta[i] == 0) continue;
        	alpha[i] /= h * w / 10000;
        	beta[i] /= h * w / 10000;
        	//System.out.printf("%d (%3f, %3f)%n", i, alpha[i], beta[i]);
        }

	}
	
	private static void test(String fileName) throws IOException{
    	JFrame f = new JFrame("CCV");
      f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      f.setLayout(new GridLayout(3, 1));
      f.setSize(1200, 1800);

      JLabel l1 = new JLabel();
      JLabel l2 = new JLabel();
      JLabel l3 = new JLabel();
      f.add(l1);
      f.add(l2);
      f.add(l3);

      BufferedImage imgsrc = ImageIO.read(new File(fileName));
      int w = imgsrc.getWidth();
      int h = imgsrc.getHeight();
      
      // Size Normalization
      
      int limit = 400;
      if(w < h){
          w = w * limit / h;
          h = limit;
      }else{
          h = h * limit / w;
          w = limit;
      }
      
      img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
      Graphics2D grp = (Graphics2D) img.getGraphics();
      grp.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      grp.drawImage(imgsrc, 0, 0, w, h, null);
      grp.dispose();

      l1.setIcon(new ImageIcon(img));

		originalImage = img.getRGB(0, 0, w, h, null, 0, w);
      currentImage = new int[originalImage.length];
      
      // Gaussian Filter
      applyGaussianFilter(w, h);
      //applyAverageFilter(w, h);
      l2.setIcon(new ImageIcon(img));

      // Color Reduction
      reduceColor();
      
      img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
      img.setRGB(0, 0, w, h, currentImage, 0, w);
      l3.setIcon(new ImageIcon(img));

      // Tagging
      tagColor(w, h);
      
      // Aggregate
      computeCoherence(w, h);
      
      // Display
      for(int i = 0; i < alpha.length; ++i){
          if(alpha[i] == 0 && beta[i] == 0) continue;
          System.out.printf("%2d (%3d, %3d)%n", i, alpha[i], beta[i]);
      }

      f.setVisible(true);
	}
	
	public static double similarityMeasure(double[] alphaQuery, double[] alphaDoc, double[] betaQuery, double[] betaDoc){
		double result = 0;
		
		for(int i = 0; i < alphaQuery.length; i++){
			if(alphaQuery[i] == 0 && alphaQuery[i] == 0) continue;
			
			double difference = Math.abs( alphaQuery[i] - alphaDoc[i] );
			double max = (alphaQuery[i] > alphaDoc[i]) ? alphaQuery[i] : alphaDoc[i];
			result += alphaQuery[i] * (1 - difference / max );
	    }
		
		return result;
	}
	
	public static String[] findSimilarResults(String fileName) throws IOException{
    	ArrayList<Document> list = new ArrayList<Document>();
    	
    	computeCCV(fileName);
    	double[] alpha1 = alpha;
    	double[] beta1 = beta;
    	
    	for (int i = 1; i <= NUM_OF_FILES; i++){

    		String newName = FILE_PATH + i + ".jpg";
    		if (fileName.equals(newName)){
    			continue;
    		}
    		System.out.println("Processing image: " + i);

    		computeCCV(newName);
        	double[] alpha2 = alpha;
        	double[] beta2 = beta;
        	
    		double result = similarityMeasure(alpha1, alpha2, beta1, beta2);
    		Document doc = new Document(newName, result);
    		list.add(doc);
    	}
    	
		Collections.sort(list);
    	
		String[] results = new String[list.size()];
		for ( int i = 0; i < list.size(); i++){
			results[i] = list.get(i).getFileName();
		}
		
    	return results;
    }
	
    public static void main(String[] args) throws IOException{
    	
//    	computeCCV(FILE_PATH + "query1.jpg");
//    	double[] alpha1 = alpha;
//    	double[] beta1 = beta;
//    	
//    	computeCCV(FILE_PATH + "15.jpg");
//    	double[] alpha2 = alpha;
//    	double[] beta2 = beta;
//    	
//    	double result = similarityMeasure(alpha1, alpha2, beta1, beta2);
//    	System.out.println("Result is: " + result);
    	
//    	String[] results = new String[NUM_OF_FILES];
//    	results = findSimilarResults(FILE_PATH + "query1.jpg");
//    	
//    	for (int i = 0; i < NUM_OF_FILES - 1; i++){
//    		System.out.println(results[i]);
//    	}
    }
}