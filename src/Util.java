import static org.bytedeco.javacpp.opencv_highgui.*;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

import java.io.File;

import org.bytedeco.javacpp.opencv_core.IplImage;

import java.util.*;

public class Util {
	
	final static int SCALE_X = 127;
	final static int SCALE_Y = 89;
	final static int SIZE_X = 254;
	
    public static List<Imgs> getImgs(String path) {
        File dir = new File(path);
        File[] files = dir.listFiles();
        List<Imgs> imgs = new ArrayList<Imgs>();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            //System.out.println((i + 1) + ":    " + file.toString());
            
            IplImage src = cvLoadImage(file.toString(),CV_LOAD_IMAGE_COLOR);
            if( src != null){
                IplImage src2 = cutImage(src,SCALE_X,SCALE_Y);
                IplImage src3 = cvCreateImage(cvSize(SIZE_X, SIZE_X * SCALE_Y / SCALE_X), src2.depth(), 3);
                cvResize(src2, src3, CV_INTER_LINEAR);

                if( src3 != null){
                	int[] rgb = getAVG(src3, 0, 0, src3.width(), src3.height());
                	imgs.add(new Imgs(rgb, src3));
                	//System.out.println( "width:"+ src3.width() + " height:" + src3.height());
                    //System.out.println( "B:"+ rgb[0] + " G:" + rgb[1] + " R:" + rgb[2]);
                }
            }
        }
        return imgs;
    }
    
    private static IplImage cutImage(IplImage img, int x, int y){
    	int w = img.width();
    	int h = img.height();
    	if ((double)w/h > (double)x / y){
    		// resize: Width
    		w = h * x / y;
    	} else {
    		// resize: Height
    		h = w * y / x;
    	}
    	
    	Rect roi = new Rect(img.width()/2 - w/2, img.height()/2 - h/2, w, h);
    	Mat ret = new Mat(cvarrToMat(img), roi);
    	return ret.asIplImage();
    }
    
    private static int[] getAVG(IplImage img, int sx, int sy, int ex, int ey){
		int avg_rgb[] = {0, 0, 0};
		for (int w = sx; w < ex; w++){
			for (int h = sy; h < ey; h++){
				CvScalar s = cvGet2D(img, h, w);
				avg_rgb[0] += (int)s.val(0);
				avg_rgb[1] += (int)s.val(1);
				avg_rgb[2] += (int)s.val(2);
			}
		}
		avg_rgb[0] = avg_rgb[0] / ((ex - sx) * (ey - sy));
		avg_rgb[1] = avg_rgb[1] / ((ex - sx) * (ey - sy));
		avg_rgb[2] = avg_rgb[2] / ((ex - sx) * (ey - sy));
		
		return avg_rgb;
    }
    

}