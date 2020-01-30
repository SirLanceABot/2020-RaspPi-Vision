/******************************************************************************
 *  Compilation:  javac EdgeDetector.java
 *  Execution:    java EdgeDetector filename
 * 
 *  Reads in an image from a file, and flips it horizontally.
 *
 *  % java EdgeDetector baboon.jpg
 *
 ******************************************************************************/

import java.lang.Math;
import java.util.List;
import java.util.ArrayList;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Core;
import org.opencv.core.CvType;

public class EdgeDetector {

    // truncate color component to be between 0 and 255
    public static int truncate(int a) {
        if      (a <   0) return 0;
        else if (a > 255) return 255;
        else              return a;
    }

    public static void mainEdgeDetector(Mat picture0) {
        // try {
        //     Thread.sleep(5);
        // } catch (InterruptedException e) {
        //     e.printStackTrace();
        // }

        if(true)
        {
        long t = System.currentTimeMillis();
        // // Try filter2D instead of hand coding the loops
        int row = 0, col = 0;

        Mat filter1 = new Mat(3, 3, CvType.CV_8S);
        filter1.put(row, col, // Sobel one dimension
             -1,  0,  1,
             -2,  0,  2,
             -1,  0,  1 
        );

       Mat filter2 = new Mat(3, 3, CvType.CV_8S);
       filter2.put(row, col, // Sobel the other dimension
              1,  2,  1,
              0,  0,  0,
             -1, -2, -1
        );
        Mat picture0Gray = new Mat(picture0.rows(), picture0.cols(), CvType.CV_8UC1);
        Mat picture0GrayFilter1 = new Mat(picture0.rows(), picture0.cols(), CvType.CV_8UC1);
        Mat picture0GrayFilter1and2 = new Mat(picture0.rows(), picture0.cols(), CvType.CV_8UC1);
        
        // Imgproc.cvtColor(picture0, picture0, Imgproc.COLOR_BGR2GRAY, 1); // convert from 3 color to 1 gray
        // Imgproc.filter2D(picture0, picture0, picture0.depth(), filter1);
        // Imgproc.filter2D(picture0, picture0, picture0.depth(), filter2);

        Imgproc.cvtColor(picture0, picture0Gray, Imgproc.COLOR_BGR2GRAY, 1); // convert from 3 color to 1 gray
        Imgproc.filter2D(picture0Gray, picture0GrayFilter1, picture0Gray.depth(), filter1);
        Imgproc.filter2D(picture0GrayFilter1, picture0GrayFilter1and2, picture0GrayFilter1.depth(), filter2);
        picture0GrayFilter1and2.convertTo(picture0, CvType.CV_8UC3); 
        //System.out.println( "filter2D Sobel " + (-t + (t = System.currentTimeMillis())) );
        }

        // Mat kern = new Mat(3, 3, CvType.CV_8S);
        // int row = 0, col = 0;
        // kern.put(row, col, 0, -1, 0, -1, 5, -1, 0, -1, 0);
        // // Then call the filter2D() function specifying the input, the output image and the kernel to use:
        // Imgproc.filter2D(picture0, dst1, picture0.depth(), kern);

        //  // or --

            //   // Declare variables
            //   Mat src, dst = new Mat();
            //   Mat kernel = new Mat();
            //   Point anchor;
            //   double delta;
            //   int ddepth;
            //   int kernel_size;
            // // Initialize arguments for the filter
            // anchor = new Point( -1, -1);
            // delta = 0.0;
            // ddepth = -1;
            // // Loop - Will filter the image with different kernel sizes each 0.5 seconds
            // int ind = 0;
            // while( true )
            // {
            //     // Update kernel size for a normalized box filter
            //     kernel_size = 3 + 2*( ind%5 ); //update the kernel_size to odd values in the range: [3,11].
            //     Mat ones = Mat.ones( kernel_size, kernel_size, CvType.CV_32F );
            //     // builds the kernel by setting its value to a matrix filled with 1′s and normalizing it
            //     // by dividing it between the number of elements.
            //     Core.multiply(ones, new Scalar(1/(double)(kernel_size*kernel_size)), kernel);
            //     // Apply filter
            //     Imgproc.filter2D(src, dst, ddepth , kernel, anchor, delta, Core.BORDER_DEFAULT );
            //   ind++;
            // }

        if(false)
        {
        long t = System.currentTimeMillis();

        int[][] filter1 = { // Sobel
            { -1,  0,  1 },
            { -2,  0,  2 },
            { -1,  0,  1 }
        };

        int[][] filter2 = {
            {  1,  2,  1 },
            {  0,  0,  0 },
            { -1, -2, -1 }
        };

        // int[][] filter1 = { // Scharr
        //     {  -3,   0,  +3 }, 
        //     { -10,   0, +10 }, 
        //     {  -3,   0,  +3 }
        // };

        // int[][] filter2= {
        //     {  -3, -10,  -3 }, 
        //     {   0,   0,   0 }, 
        //     {  +3, +10,  +3 }
        //    };

        int width    = picture0.cols();
        int height   = picture0.rows();

        Mat tempImage = new Mat(height, width, CvType.CV_8UC1);
        Imgproc.cvtColor(picture0, tempImage, Imgproc.COLOR_BGR2GRAY, 1); // convert tempImage from 3 color to 1 gray 8UC1
        //System.out.println(tempImage);
        //tempImage.convertTo(tempImage, CvType.CV_16UC1); // convert temp image from 3 channels to 1 channel?
        //short[] flatImage = new short[width*height]; // empty flat image
        byte[] flatImage = new byte[width*height]; // empty flat image
        tempImage.get(0, 0, flatImage); // fill the flat image with the image
 
        // int countP=0, countN=0, countZ=0;
        // for(int idx = 0; idx < flatImage.length; idx++)
        // {
        //     if (flatImage[idx] < 0) countN++;
        //     if (flatImage[idx] > 0) countP++;
        //     else countZ++;
        // }
        // System.out.println( countP + " " + countN + " " + countZ);

        // for(int idx = 0; idx < width*height; idx++)
        // {
        //     flatImage [idx] = (short)(flatImage[idx] *2 - 50); // change contrast and brightness
        //     flatImage [idx] = flatImage [idx] > 255 ? 255 : (flatImage [idx] < 0 ? 0 : flatImage [idx]);
        //  }
 
 //       short[] flatEdge  = new short[width*height]; // 1 channel
        byte[] flatEdge  = new byte[width*height]; // 1 channel
        
        for (int idx=0; idx<flatEdge.length; idx++)
            flatEdge[idx] = 0; //initialize everything to get the boundaries - a waste but easier

        int magnitude;
        System.out.println( "In convert " + (-t + (t = System.currentTimeMillis())) );

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                // get 3-by-3 array of colors in neighborhood
                int[][] gray = new int[3][3];
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        int pixel = flatImage[width*(y-1+j) + (x-1+i)];
                        gray[i][j] = pixel >= 0 ?  pixel : 256 + pixel; // U to S conversion
                    }
                }
 
                // // apply filter
                short gray1 = 0, gray2 = 0;
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        gray1 += gray[i][j] * filter1[i][j];
                        gray2 += gray[i][j] * filter2[i][j];
                    }
                }
                // alternate to sum of squares Math.abs(gray1) + Math.abs(gray2)
                magnitude = (int)Math.sqrt((double)(gray1*gray1 + gray2*gray2));// mostly black with white lines
                magnitude = magnitude > 255 ? 255 : (magnitude < 0 ? 0 : magnitude);
                //magnitude = 255 - magnitude; // mostly white with black lines

                byte grayscale = magnitude <= 127 ? (byte)magnitude : (byte)(magnitude -256);

                flatEdge[width*y + x] = grayscale;
              }
        }
        System.out.println( "Filters " + (-t + (t = System.currentTimeMillis())) );
        //tempImage.put(0, 0, flatImage); // this works to show the original image in gray
        tempImage.put(0, 0, flatEdge);

        Imgproc.cvtColor(tempImage, tempImage, Imgproc.COLOR_GRAY2BGR, 3); // change number of channels

        tempImage.convertTo(picture0, CvType.CV_8UC3); // change type - convertTo can't change the channels, for channel change use cvtColor
        
    //     tempImage.convertTo(tempImage, CvType.CV_8UC3); // change type - convertTo can't change the channels, for channel change use cvtColor
    //     Core.bitwise_and(picture0, tempImage, picture0); 
        System.out.println( "Out convert " + (-t + (t = System.currentTimeMillis())) );
    }
    }

    /**
 *  The class {@code Luminance} is a library of static methods related to
 *  the monochrome luminance of a color. It supports computing the monochrome
 *  luminance of a color (r, g, b) using the NTSC formula
 *  Y = 0.299*r + 0.587*g + 0.114*b; converting the color to a grayscale color,
 *  and checking whether two colors are compatible.
 *  <p>
 *  For additional documentation, see <a href="https://introcs.cs.princeton.edu/31oop">Section 3.1</a>
 *  of <i>Computer Science: An Interdisciplinary Approach</i>
 *  by Robert Sedgewick and Kevin Wayne. 
 *
 *  @author Robert Sedgewick
 *  @author Kevin Wayne
 *
 */
public static class Luminance {
    /**
     * Returns the monochrome luminance of the given color as an intensity
     * between 0.0 and 255.0 using the NTSC formula Y = 0.299*r + 0.587*g + 0.114*b.
     *
     * @param color the color to convert
     */
    public static double intensity(double[] color) {
        return 0.299*color[2] + 0.587*color[1] + 0.114*color[0]; // Scalar is OpenCV color BGR
    }
}
}