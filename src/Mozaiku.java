import static org.bytedeco.javacpp.helper.opencv_core.CV_RGB;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_highgui.*;
import static org.bytedeco.javacpp.opencv_imgproc.CV_INTER_LINEAR;
import static org.bytedeco.javacpp.opencv_imgproc.cvResize;

import java.util.*;

import org.bytedeco.javacpp.opencv_core.IplImage;

import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.IOException;

public class Mozaiku{
 
	// Number of images (x direction)
	public static int num_w = 40;
	// Time of non-used
	final static int NOT_USED_TIME = 1;
	// Images cannot be set near from the same image
	final static double MIN_DISTANCE = 0;
	// coef: Math.min(MAX_COEF, (COEF + img.coef) / COEF) * (1 + RANDOM_COEF * Math.random());
	final static int COEF = 20;
	final static double MAX_COEF = 1;
	final static double RANDOM_COEF = 3.0;
	// Scale ratio of output file
	final static int SCALE = 10;
	// Directory path of data/
	final static String DATA_DIR = "data/";

	public static void main (String[] args){
		try {
			// Load input image
			final IplImage src = cvLoadImage(DATA_DIR + "input.jpg", CV_LOAD_IMAGE_COLOR);
			IplImage split_img = cvCloneImage(src);

			final int one_w = src.width() / num_w;
			final int one_h = one_w * 89 / 127; // L size : 127x89
			final int num_h = src.height() / one_h;

			// Print Basic Information
			System.out.println("Width: " + src.width());
			System.out.println("Height: " + src.height());
			System.out.println("One_width: " + one_w);
			System.out.println("One_height: " + one_h);
			System.out.println("Num_width: " + num_w);
			System.out.println("Num_height: " + num_h);
			
			// Color information CSV file
			FileWriter fw = new FileWriter(DATA_DIR + "color.csv", false); 
			PrintWriter pw = new PrintWriter(new BufferedWriter(fw));

			// Initialize List of Imgs
			List<Imgs> srcs = new ArrayList<Imgs>();
			List<Imgs> images = new ArrayList<Imgs>();
			List<Imgs> not_used_images = new ArrayList<Imgs>();
			
			// [1] Load material images
			System.out.print("[1] Load material images...");
			List<Imgs> imgs = Util.getImgs(DATA_DIR + "materials/");
			System.out.println("done");
			
			// [2] Read source image and create RGB map
			System.out.print("[2] Read source image and create RGB map...");
			for (int i = 0; i < num_w; i++){
				for (int j = 0; j < num_h; j++){
					int avg_rgb[] = {0, 0, 0};
					for (int w = 0; w < one_w; w++){
						for (int h = 0; h < one_h; h++){
							CvScalar s = cvGet2D(src, h + j * one_h, w + i * one_w);
							avg_rgb[0] += (int)s.val(0);
							avg_rgb[1] += (int)s.val(1);
							avg_rgb[2] += (int)s.val(2);
						}
					}
					avg_rgb[0] = avg_rgb[0] / (one_w * one_h);
					avg_rgb[1] = avg_rgb[1] / (one_w * one_h);
					avg_rgb[2] = avg_rgb[2] / (one_w * one_h);
					srcs.add(new Imgs(avg_rgb, i, j));
					//System.out.println( "B:"+ avg_rgb[0] + " G:" + avg_rgb[1] + " R:" + avg_rgb[2]);
					pw.println(avg_rgb[0] + "," + avg_rgb[1] + "," + avg_rgb[2]);
				}
			}
			pw.close();
			System.out.println("done");
			
			// Create split image
			for (int i = 0; i < num_w+1; i++){
				cvLine(split_img, cvPoint(one_w*i,0), cvPoint(one_w*i,one_h*num_h), CV_RGB(255, 255, 255), 1, 8, 0);
			}
			for (int j = 0; j < num_h+1; j++){
				cvLine(split_img, cvPoint(0, one_h*j), cvPoint(one_w*num_w, one_h*j), CV_RGB(255, 255, 255), 1, 8, 0);
			}
			cvSaveImage(DATA_DIR + "split.png", split_img);

			
			// [3] Use all images once
			System.out.print("[3] Use all images once...");
			for(Imgs img : imgs){
				int index = 0;
				int min_index = Integer.MAX_VALUE;
				double min_rgb = Double.MAX_VALUE;
				for(Imgs src_img : srcs){
					double dif = 0.0;
					dif += Math.pow(img.rgb[0] - src_img.rgb[0], 2);
					dif += Math.pow(img.rgb[1] - src_img.rgb[1], 2);
					dif += Math.pow(img.rgb[2] - src_img.rgb[2], 2);
					if (dif < min_rgb){
						min_rgb = dif;
						min_index = index;
					}
					index++;
				}
				//System.out.println("Size: " + srcs.size());
				if ( min_index < Integer.MAX_VALUE){
					int[] xy = {srcs.get(min_index).x, srcs.get(min_index).y};
					img.xy.add(img.xy.size(), xy);

					images.add(new Imgs(img.img, srcs.get(min_index).x, srcs.get(min_index).y));

					//System.out.println(min_index);
					//System.out.println(srcs.get(min_index).x);
					//System.out.println(srcs.get(min_index).y);

					srcs.remove(min_index);
				}
			}
			System.out.println("done");
			
			// [4] Chose material images
			System.out.print("[4] Chose material images...");
			for(Imgs src_img : srcs){
				int index = 0;
				int min_index = Integer.MAX_VALUE;
				double min_rgb = Double.MAX_VALUE;
				for(Imgs img : imgs){	
					double dif = 0.0;
					dif += Math.pow(img.rgb[0] - src_img.rgb[0], 2);
					dif += Math.pow(img.rgb[1] - src_img.rgb[1], 2);
					dif += Math.pow(img.rgb[2] - src_img.rgb[2], 2);

					dif = Math.sqrt(dif) * Math.min(MAX_COEF, (COEF + img.coef) / COEF) * (1 + RANDOM_COEF * Math.random());

					// chose one image which has the least difference
					if (dif < min_rgb){
						// check the distance from the same images
						boolean short_distance = false;
						for (int i = 0; i < img.xy.size(); i++){
							double dif_dis = 0.0;
							dif_dis += Math.pow(img.xy.get(i)[0] - src_img.x, 2);
							dif_dis += Math.pow(img.xy.get(i)[1] - src_img.y, 2);
							dif_dis = Math.sqrt(dif_dis);
							if (dif_dis < MIN_DISTANCE){
								short_distance = true;
							}
						}
						if (!short_distance){
							min_rgb = dif;
							min_index = index;
						}
					}
					index++;
				}

				if ( min_index < Integer.MAX_VALUE){
					// add image as past images
					int[] xy = {src_img.x, src_img.y};
					Imgs target_img = imgs.get(min_index);
					target_img.xy.add(target_img.xy.size(), xy);
					images.add(new Imgs(target_img.img, src_img.x, src_img.y));

					//System.out.println(min_index);
					//System.out.println(src_img.x);
					//System.out.println(src_img.y);

					not_used_images.add(imgs.remove(min_index));
					if(not_used_images.size() > NOT_USED_TIME){
						Imgs tmp = not_used_images.remove(0);
						tmp.coef++;
						imgs.add(tmp);
					}
				}
			}
			System.out.println("done");
			
			// [5] Create Mozaiku Image
			System.out.print("[5] Create Mozaiku Image...");
			IplImage mozaiku_img = cvCreateImage(cvSize(src.width()*SCALE, src.height()*SCALE), src.depth(), 3);
			for (Imgs img: images){
				IplImage img2 = cvCreateImage(cvSize(one_w*SCALE, one_h*SCALE), img.img.depth(), 3);
				cvResize(img.img, img2, CV_INTER_LINEAR);
				cvSetImageROI(mozaiku_img, cvRect(img.x * one_w*SCALE, img.y * one_h*SCALE, one_w*SCALE, one_h*SCALE));
				cvCopy(img2, mozaiku_img);
				//System.out.println(img.x);
				//System.out.println(img.y);
			}

			cvResetImageROI(mozaiku_img);
			cvSaveImage(DATA_DIR + "mozaiku.png", mozaiku_img);
			System.out.println("done");
		} catch (IOException ex) {
			//例外時処理
			ex.printStackTrace();
		}
	}	
}