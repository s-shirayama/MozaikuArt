import org.bytedeco.javacpp.opencv_core.IplImage;

import java.util.*;

public class Imgs {
	public int x;
	public int y;
	public int rgb[];
	public IplImage img;
	public int coef = 0;
	public List<int[]> xy = new ArrayList<int[]>();
	
	Imgs(int[] _rgb, int _x, int _y){
		x = _x;
		y = _y;
		rgb = _rgb;
	}
	
	Imgs(IplImage _img, int _x, int _y){
		x = _x;
		y = _y;
		img = _img;
	}
	
	Imgs(int[] _rgb, IplImage _img){
		rgb = _rgb;
		img = _img;
	}
}