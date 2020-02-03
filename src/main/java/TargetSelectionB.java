import java.util.ArrayList;
import java.util.List;
import java.lang.Math;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.RotatedRect;
import org.opencv.core.CvType;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;

/**
 * This class is used to select the target from the camera frame. The user MUST
 * MODIFY the process() method. The user must create a new gripPowerCellIntakeVisionPipeline class
 * using GRIP, modify the TargetData class, and modify this class.
 * 
 * @author FRC Team 4237
 * @version 2019.01.28.14.20
 */
public class TargetSelectionB
{
    private static final String pId = new String("[TargetSelectionB]");

	// This object is used to run the gripPowerCellIntakeVisionPipeline
    private GRIPPowerPortVisionPipeline gripPowerPortVisionPipeline = new GRIPPowerPortVisionPipeline();

	// This field is used to determine if debugging information should be displayed.
    private boolean debuggingEnabled = false;

    // tables for t statistic one tailed test
    // calculation not readily available so copy a table from a "book"
    // extrapolate past the .999 - not precision math but apparently
    // good enough for decent results and don't care about the exact value
    // just care about the maximum significance
	// table from https://www.itl.nist.gov/div898/handbook/eda/section3/eda3672.htm
	// ENGINEERING STATISTICS HANDBOOK 1.3.6.7.2. Critical Values of the Student's t Distribution
    //alpha .9 to .999 (0 is filler; .9 is the last entry)\
	// .9999 generated in Excel 2003 =ROUND(TINV((1 - 0.9999)*2, df), 3) [infinity is df=100000]
	// Given a specified value for α :

// For a two-sided test, find the column corresponding to 1-α/2 and reject the null hypothesis if the absolute value of the test statistic is greater than the value of t1-α/2,ν in the table below.
// For an upper, one-sided test, find the column corresponding to 1-α and reject the null hypothesis if the test statistic is greater than the table value.
// For a lower, one-sided test, find the column corresponding to 1-α and reject the null hypothesis if the test statistic is less than the negative of the table value.

    double alpha[] =
    {    0,    0.90,    0.95,   0.975,    0.99,   0.995,   0.999,   0.9999};
    double sig[][] =
    {//   0     1        2        3        4        5        6         7  [rows 0-101] [cols 0-7]
    {    0.,   0.,      0.,      0.,      0.,      0.,      0.,       0. // was alpha now just filler since alpha now has its own array
    },{  1.,   3.078,   6.314,  12.706,  31.821,  63.657, 318.313, 3183.099
    },{  2.,   1.886,   2.920,   4.303,   6.965,   9.925,  22.327,   70.700
    },{  3.,   1.638,   2.353,   3.182,   4.541,   5.841,  10.215,   22.204
    },{  4.,   1.533,   2.132,   2.776,   3.747,   4.604,   7.173,   13.034
    },{  5.,   1.476,   2.015,   2.571,   3.365,   4.032,   5.893,    9.678
    },{  6.,   1.440,   1.943,   2.447,   3.143,   3.707,   5.208,    8.025
    },{  7.,   1.415,   1.895,   2.365,   2.998,   3.499,   4.782,    7.063
    },{  8.,   1.397,   1.860,   2.306,   2.896,   3.355,   4.499,    6.442
    },{  9.,   1.383,   1.833,   2.262,   2.821,   3.250,   4.296,    6.010
    },{ 10.,   1.372,   1.812,   2.228,   2.764,   3.169,   4.143,    5.694
    },{ 11.,   1.363,   1.796,   2.201,   2.718,   3.106,   4.024,    5.453
    },{ 12.,   1.356,   1.782,   2.179,   2.681,   3.055,   3.929,    5.263
    },{ 13.,   1.350,   1.771,   2.160,   2.650,   3.012,   3.852,    5.111
    },{ 14.,   1.345,   1.761,   2.145,   2.624,   2.977,   3.787,    4.985
    },{ 15.,   1.341,   1.753,   2.131,   2.602,   2.947,   3.733,    4.880
    },{ 16.,   1.337,   1.746,   2.120,   2.583,   2.921,   3.686,    4.791
    },{ 17.,   1.333,   1.740,   2.110,   2.567,   2.898,   3.646,    4.714
    },{ 18.,   1.330,   1.734,   2.101,   2.552,   2.878,   3.610,    4.648
    },{ 19.,   1.328,   1.729,   2.093,   2.539,   2.861,   3.579,    4.590
    },{ 20.,   1.325,   1.725,   2.086,   2.528,   2.845,   3.552,    4.539
    },{ 21.,   1.323,   1.721,   2.080,   2.518,   2.831,   3.527,    4.493
    },{ 22.,   1.321,   1.717,   2.074,   2.508,   2.819,   3.505,    4.452
    },{ 23.,   1.319,   1.714,   2.069,   2.500,   2.807,   3.485,    4.415
    },{ 24.,   1.318,   1.711,   2.064,   2.492,   2.797,   3.467,    4.382
    },{ 25.,   1.316,   1.708,   2.060,   2.485,   2.787,   3.450,    4.352
    },{ 26.,   1.315,   1.706,   2.056,   2.479,   2.779,   3.435,    4.324
    },{ 27.,   1.314,   1.703,   2.052,   2.473,   2.771,   3.421,    4.299
    },{ 28.,   1.313,   1.701,   2.048,   2.467,   2.763,   3.408,    4.275
    },{ 29.,   1.311,   1.699,   2.045,   2.462,   2.756,   3.396,    4.254
    },{ 30.,   1.310,   1.697,   2.042,   2.457,   2.750,   3.385,    4.234
    },{ 31.,   1.309,   1.696,   2.040,   2.453,   2.744,   3.375,    4.216
    },{ 32.,   1.309,   1.694,   2.037,   2.449,   2.738,   3.365,    4.198
    },{ 33.,   1.308,   1.692,   2.035,   2.445,   2.733,   3.356,    4.182
    },{ 34.,   1.307,   1.691,   2.032,   2.441,   2.728,   3.348,    4.167
    },{ 35.,   1.306,   1.690,   2.030,   2.438,   2.724,   3.340,    4.153
    },{ 36.,   1.306,   1.688,   2.028,   2.434,   2.719,   3.333,    4.140
    },{ 37.,   1.305,   1.687,   2.026,   2.431,   2.715,   3.326,    4.127
    },{ 38.,   1.304,   1.686,   2.024,   2.429,   2.712,   3.319,    4.116
    },{ 39.,   1.304,   1.685,   2.023,   2.426,   2.708,   3.313,    4.105
    },{ 40.,   1.303,   1.684,   2.021,   2.423,   2.704,   3.307,    4.094
    },{ 41.,   1.303,   1.683,   2.020,   2.421,   2.701,   3.301,    4.084
    },{ 42.,   1.302,   1.682,   2.018,   2.418,   2.698,   3.296,    4.075
    },{ 43.,   1.302,   1.681,   2.017,   2.416,   2.695,   3.291,    4.066
    },{ 44.,   1.301,   1.680,   2.015,   2.414,   2.692,   3.286,    4.057
    },{ 45.,   1.301,   1.679,   2.014,   2.412,   2.690,   3.281,    4.049
    },{ 46.,   1.300,   1.679,   2.013,   2.410,   2.687,   3.277,    4.042
    },{ 47.,   1.300,   1.678,   2.012,   2.408,   2.685,   3.273,    4.034
    },{ 48.,   1.299,   1.677,   2.011,   2.407,   2.682,   3.269,    4.027
    },{ 49.,   1.299,   1.677,   2.010,   2.405,   2.680,   3.265,    4.020
    },{ 50.,   1.299,   1.676,   2.009,   2.403,   2.678,   3.261,    4.014
    },{ 51.,   1.298,   1.675,   2.008,   2.402,   2.676,   3.258,    4.008
    },{ 52.,   1.298,   1.675,   2.007,   2.400,   2.674,   3.255,    4.002
    },{ 53.,   1.298,   1.674,   2.006,   2.399,   2.672,   3.251,    3.996
    },{ 54.,   1.297,   1.674,   2.005,   2.397,   2.670,   3.248,    3.991
    },{ 55.,   1.297,   1.673,   2.004,   2.396,   2.668,   3.245,    3.986
    },{ 56.,   1.297,   1.673,   2.003,   2.395,   2.667,   3.242,    3.981
    },{ 57.,   1.297,   1.672,   2.002,   2.394,   2.665,   3.239,    3.976
    },{ 58.,   1.296,   1.672,   2.002,   2.392,   2.663,   3.237,    3.971
    },{ 59.,   1.296,   1.671,   2.001,   2.391,   2.662,   3.234,    3.966
    },{ 60.,   1.296,   1.671,   2.000,   2.390,   2.660,   3.232,    3.962
    },{ 61.,   1.296,   1.670,   2.000,   2.389,   2.659,   3.229,    3.958
    },{ 62.,   1.295,   1.670,   1.999,   2.388,   2.657,   3.227,    3.954
    },{ 63.,   1.295,   1.669,   1.998,   2.387,   2.656,   3.225,    3.950
    },{ 64.,   1.295,   1.669,   1.998,   2.386,   2.655,   3.223,    3.946
    },{ 65.,   1.295,   1.669,   1.997,   2.385,   2.654,   3.220,    3.942
    },{ 66.,   1.295,   1.668,   1.997,   2.384,   2.652,   3.218,    3.939
    },{ 67.,   1.294,   1.668,   1.996,   2.383,   2.651,   3.216,    3.935
    },{ 68.,   1.294,   1.668,   1.995,   2.382,   2.650,   3.214,    3.932
    },{ 69.,   1.294,   1.667,   1.995,   2.382,   2.649,   3.213,    3.929
    },{ 70.,   1.294,   1.667,   1.994,   2.381,   2.648,   3.211,    3.926
    },{ 71.,   1.294,   1.667,   1.994,   2.380,   2.647,   3.209,    3.923
    },{ 72.,   1.293,   1.666,   1.993,   2.379,   2.646,   3.207,    3.920
    },{ 73.,   1.293,   1.666,   1.993,   2.379,   2.645,   3.206,    3.917
    },{ 74.,   1.293,   1.666,   1.993,   2.378,   2.644,   3.204,    3.914
    },{ 75.,   1.293,   1.665,   1.992,   2.377,   2.643,   3.202,    3.911
    },{ 76.,   1.293,   1.665,   1.992,   2.376,   2.642,   3.201,    3.909
    },{ 77.,   1.293,   1.665,   1.991,   2.376,   2.641,   3.199,    3.906
    },{ 78.,   1.292,   1.665,   1.991,   2.375,   2.640,   3.198,    3.904
    },{ 79.,   1.292,   1.664,   1.990,   2.374,   2.640,   3.197,    3.901
    },{ 80.,   1.292,   1.664,   1.990,   2.374,   2.639,   3.195,    3.899
    },{ 81.,   1.292,   1.664,   1.990,   2.373,   2.638,   3.194,    3.896
    },{ 82.,   1.292,   1.664,   1.989,   2.373,   2.637,   3.193,    3.894
    },{ 83.,   1.292,   1.663,   1.989,   2.372,   2.636,   3.191,    3.892
    },{ 84.,   1.292,   1.663,   1.989,   2.372,   2.636,   3.190,    3.890
    },{ 85.,   1.292,   1.663,   1.988,   2.371,   2.635,   3.189,    3.888
    },{ 86.,   1.291,   1.663,   1.988,   2.370,   2.634,   3.188,    3.886
    },{ 87.,   1.291,   1.663,   1.988,   2.370,   2.634,   3.187,    3.884
    },{ 88.,   1.291,   1.662,   1.987,   2.369,   2.633,   3.185,    3.882
    },{ 89.,   1.291,   1.662,   1.987,   2.369,   2.632,   3.184,    3.880
    },{ 90.,   1.291,   1.662,   1.987,   2.368,   2.632,   3.183,    3.878
    },{ 91.,   1.291,   1.662,   1.986,   2.368,   2.631,   3.182,    3.876
    },{ 92.,   1.291,   1.662,   1.986,   2.368,   2.630,   3.181,    3.874
    },{ 93.,   1.291,   1.661,   1.986,   2.367,   2.630,   3.180,    3.873
    },{ 94.,   1.291,   1.661,   1.986,   2.367,   2.629,   3.179,    3.871
    },{ 95.,   1.291,   1.661,   1.985,   2.366,   2.629,   3.178,    3.869
    },{ 96.,   1.290,   1.661,   1.985,   2.366,   2.628,   3.177,    3.868
    },{ 97.,   1.290,   1.661,   1.985,   2.365,   2.627,   3.176,    3.866
    },{ 98.,   1.290,   1.661,   1.984,   2.365,   2.627,   3.175,    3.865
    },{ 99.,   1.290,   1.660,   1.984,   2.365,   2.626,   3.175,    3.863
    },{100.,   1.290,   1.660,   1.984,   2.364,   2.626,   3.174,    3.862
    },{101.,   1.282,   1.645,   1.960,   2.326,   2.576,   3.090,    3.719} // infinity if over 100
    };

    Mat subMat = new Mat(); // place for small image inserted into large image

	TargetSelectionB()
	{
	}

	/**
	 * This method sets the field to display debugging information.
	 * 
	 * @param enabled
	 *                    Set to true to display debugging information.
	 */
	public void setDebuggingEnabled(boolean enabled)
	{
		debuggingEnabled = enabled;
	}

    // int heightOfMask = 76;
   
    // public int getHeightOfMask()
    // {
    //     return heightOfMask;
    // }

    // public void setHeightOfMask(int newHeight)
    // {
    //     heightOfMask = newHeight;
    // }

    /**
	 * This method is used to select the next target. The user MUST MODIFY this
	 * method.
	 * 
	 * @param mat
	 *                           The camera frame containing the image to process.
	 * @param nextTargetData
	 *                           The target data found in the camera frame.
	 */
    public void process(Mat mat, TargetDataB nextTargetData)
    {
        if(false)
        {
        // Enhance Image
        // Convert to YUV
        // Apply histogram equalization
        // Convert Back To BGR
        Mat Image_yuv = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC3);
        List<Mat> yuvPlanes = new ArrayList<>();
        Imgproc.cvtColor(mat, Image_yuv, Imgproc.COLOR_BGR2YUV);
        Core.split(Image_yuv, yuvPlanes);
        Imgproc.equalizeHist(yuvPlanes.get(0), yuvPlanes.get(0));
        Core.merge(yuvPlanes, Image_yuv);
        Imgproc.cvtColor(Image_yuv, mat, Imgproc.COLOR_YUV2BGR);
        }

        if(false)
        {
        // Run the Edge Detector
            EdgeDetector.mainEdgeDetector(mat);
        }

        // // get distance to retroflective tape to use as estimate where to cutoff processing white tape
        // int tapeDistance = Main.obj.tapeDistance.get();
 
        if(true)
        {
  		// Let the gripPowerCellIntakeVisionPipeline filter through the camera frame
        gripPowerPortVisionPipeline.process(mat);

        // The gripPowerCellIntakeVisionPipeline creates an array of contours that must be searched to find
        // the target.
        ArrayList<MatOfPoint> filteredContours;
        filteredContours = new ArrayList<MatOfPoint>(gripPowerPortVisionPipeline.filterContoursOutput());

        // gripPowerPortVisionPipeline.maskOutput();

		// Check if no contours were found in the camera frame.
        if (filteredContours.isEmpty())
        {
            if (debuggingEnabled)
            {
                System.out.println(pId + " No Contours");

                // Display a message if no contours are found.
                Imgproc.putText(mat, "No Contours", new Point(20, 20), Core.FONT_HERSHEY_SIMPLEX, 0.25,
                        new Scalar(0, 0, 0), 1);
            }
        }
        else // if contours were found ...
        {
            RotatedRect boundRect;

            if (debuggingEnabled)
			{
				System.out.println(pId + " " + filteredContours.size() + " contours");

				// Draw all contours at once (negative index).
				// Positive thickness means not filled, negative thickness means filled.
				Imgproc.drawContours(mat, filteredContours, -1, new Scalar(255, 0, 0), 1);
			}

            // Loop through all contours to find the best contour
            // Each contour is reduced to a single point - the COG x, y of the contour
            
            int contourIndex = -1;
            int bestContourIndex = -1;
            Point endpoint = new Point();

            for (MatOfPoint contour : filteredContours)
            {
                contourIndex++;
                
                // Print all the contours

                //System.out.println("Contour Index = " + contourIndex);
                //System.out.println(contour.dump());

                MatOfPoint2f  NewMtx = new MatOfPoint2f(contour.toArray());

                // for(int idx = 0; idx < contour.toArray().length; idx++)
                // {
                //     System.out.println("(" + contour.toArray()[idx].x + ", " + contour.toArray()[idx].y + ")");
                // }
                
                // Create a bounding upright rectangle for the contour's points
                boundRect = Imgproc.minAreaRect(NewMtx);

                // Draw a rotatedRect, using lines, that represents the minAreaRect
                Point boxPts[] = new Point[4];
                boundRect.points(boxPts);
                Point boxPtsO[] = new Point[4];
                boundRect.points(boxPtsO);
                
                // Determine if this is the best contour using center.y

                if (nextTargetData.center.y < boundRect.center.y)
                {
                    bestContourIndex = contourIndex;

                //System.out.println(boundRect); // { {76.72164916992188, 101.87628936767578} 9x92 * -66.03751373291016 }
                
                // draw edges of minimum rotated rectangle    
                List<MatOfPoint> listMidContour = new ArrayList<MatOfPoint>();
                listMidContour.add(
                    new MatOfPoint (boxPts[0], boxPts[1], boxPts[2], boxPts[3])
                    );

                Imgproc.polylines (
                    mat,                      // Matrix obj of the image
                    listMidContour,           // java.util.List<MatOfPoint> pts
                    true,                     // isClosed
                    new Scalar(255, 0, 255),  // Scalar object for color
                    1,                        // Thickness of the line
                    Imgproc.LINE_4           // line type
                );

                // OR the point to point way
                // for(int i = 0; i<4; i++)
                // {
                //     Imgproc.line(mat, boxPts[i], boxPts[(i+1)%4], new Scalar(255, 0, 255));
                // }

                // draw stars at corners of minimum rotated rectangle
                Imgproc.drawMarker(mat, new Point( boxPts[0].x, Math.min(boxPts[0].y,(double)mat.cols() - 10.) ),
                     new Scalar(  0, 255,   0), Imgproc.MARKER_STAR, 7);// green - always max y - can be > image max y
                Imgproc.drawMarker(mat, boxPts[1], new Scalar(255, 255,   0), Imgproc.MARKER_STAR, 7);// teal then cw from 0
                Imgproc.drawMarker(mat, boxPts[2], new Scalar(  0, 255, 255), Imgproc.MARKER_STAR, 7);// yellow
                Imgproc.drawMarker(mat, boxPts[3], new Scalar(255,   0, 255), Imgproc.MARKER_STAR, 7);// magenta
                double slope1 = (boxPts[0].y-boxPts[1].y) / (boxPts[0].x-boxPts[1].x);
                double slope2 = (boxPts[3].y-boxPts[0].y) / (boxPts[3].x-boxPts[0].x);
//                System.out.println( (boxPts[0].y-boxPts[1].y) / (boxPts[0].x-boxPts[1].x) + " " +
//                    (boxPts[0].y-boxPts[1].y) / (boxPts[0].x-boxPts[1].x) ) ;

                // Imgproc.putText(
                //     mat,
                //     String.format("%5.2f %5.2f %5.0f",
                //         slope1,
                //         slope2,
                //         slope1>=slope2 ?
                //             Math.atan2(boxPts[0].y-boxPts[1].y, boxPts[0].x-boxPts[1].x)*180./Math.PI :
                //             Math.atan2(boxPts[3].y-boxPts[0].y, boxPts[3].x-boxPts[0].x)*180./Math.PI 
                //         ),
                //     new Point(5, 13),
                //     Core.FONT_HERSHEY_SIMPLEX, .5,
                //     new Scalar(90, 255, 255),
                //     1
                //     );

                //boxPts[0] is the bottom max Y and it may be > Mat max Y
                // find the right point; is it [0] or [1] or [2]?
                //  2 3
                //  1 0
                // or
                //  1 2
                //  0 3
                // verify [0] is max Y
                double minX = 9999999;
                int minXposition = -1;
                for (int i = 0; i < 4; i++) if(boxPts[i].x < minX) minXposition = i;
 
                for (int i = 1; i < 4; i++) 
                    if(boxPts[0].y < boxPts[i].y) // verify 0 at the bottom
                    {
                        System.out.println("ERROR boxPts[0] not at bottom");
                        // say no target
                    }

                    // Find the center x, center y, width, height, and angle of the bounding rectangle
                    nextTargetData.center = boundRect.center;
                    nextTargetData.size = boundRect.size;
                    nextTargetData.angle = boundRect.angle;

                    // Imgproc.putText(mat, String.format("%5.0f", boundRect.angle), new Point(15, 15),
                    //   Core.FONT_HERSHEY_SIMPLEX, .6, new Scalar(255, 255, 255), 1);
 
/*
if height y > x width
straight on and aligned pefectly up and down it is at 0 (leftish) or -90 (rightish)
slight angle from straight up is -10 if left so drive right a little
or -80 if right so drive left a little

          -0|-90
       -10  |  -80
            |
  -80       |       -10
000000000000|000000000000
if height <= width - laying on its side and pointing left / right at 0 degrees

need to figure out squat shape

*/

// find the more vertical edge even if the short side
// or horizontal
// when it's squat assume it is pointing up
// p0 p1 p2 p3

                    // Create an accurate angle
                    if(Math.abs((int)(nextTargetData.size.width - nextTargetData.size.height)) <= 5)
                    {
                        nextTargetData.fixedAngle = 90.0;
                    }
                    else if(nextTargetData.size.width < nextTargetData.size.height)
                    {
                        nextTargetData.fixedAngle = nextTargetData.angle + 90;
                    }
                    else
                    {
                        nextTargetData.fixedAngle = nextTargetData.angle + 180;
                    }

                    //Update the target
                    nextTargetData.isFreshData = true;
                    nextTargetData.isTargetFound = true;
                }
            }
            // draw the best - selected - contour
            Imgproc.drawContours(mat, filteredContours, bestContourIndex, new Scalar(0, 0, 255), 1);
            // draw the pointer to the target
            double angleInRadians = nextTargetData.fixedAngle * (Math.PI/180);
            endpoint.x = nextTargetData.center.x - ( (nextTargetData.size.height / 2) * Math.cos(angleInRadians) );
            endpoint.y = nextTargetData.center.y - ( (nextTargetData.size.height / 2) * Math.sin(angleInRadians) );
            Imgproc.line(mat, nextTargetData.center, endpoint,  new Scalar(255, 0, 255), 1, Imgproc.LINE_4);
            Mat bestContour = Mat.zeros(mat.rows(), mat.cols(), CvType.CV_8UC1); // blank Mat to draw the best Contour on
            Imgproc.drawContours(bestContour, filteredContours, bestContourIndex, new Scalar(255), 1, Imgproc.LINE_4);

            int width[] = new int[bestContour.rows()]; // the width of the contour at each row - left/right scan line of the image
            int midContourX[] = new int[bestContour.rows()];
            byte[] flatRow = new byte[bestContour.cols()]; // one row of the image - for efficiency in using OpenCV get pixels

            for (int idxY = 0; idxY < bestContour.rows(); idxY++)
            {
                bestContour.get(idxY, 0, flatRow);
                int xMin = -1;
                int xMax = -2;

                for (int idxX = 0; idxX < bestContour.cols(); idxX++)
                {
                    if (flatRow[idxX] != 0)
                    {
                        xMin = idxX; // left edge of the contour
                        break;
                    }
                }

                for (int idxX = bestContour.cols()-1; idxX >= 0; idxX--)
                {
                    if (flatRow[idxX] != 0)
                    {
                        xMax = idxX; // right edge of the contour
                        break;
                    }
                }
 
                width[idxY] = xMax - xMin + 1; // width of the contour - 0 if the contour wasn't in this row
                midContourX[idxY] = xMin + width[idxY]/2; // center of the contour for each scan line - -1 if not in contour
             }
    
            // look for the row of the top of the contour
            int startY = -1;
            for (int idxY = 0; idxY < bestContour.rows(); idxY++)
            {
                if(width[idxY] > 0)
                {
                    startY = idxY;
                    break;
                }
            }
            // Look for the row of the bottom of the contour
            int stopY = -1;
            for (int idxY = bestContour.rows()-1; idxY >= 0 ; idxY--)
            {
                if(width[idxY] > 0)
                {
                    stopY = idxY;
                    break;
                }
            }

         } // end of processing all contours in this camera frame
     }
}
}