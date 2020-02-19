import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
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
    static {System.out.println("Starting class: " + MethodHandles.lookup().lookupClass().getCanonicalName());}

    private static final String pId = new String("[TargetSelectionB]");

    private static final double VERTICAL_CAMERA_ANGLE_OF_VIEW = 35.0;

	// This object is used to run the gripPowerCellIntakeVisionPipeline
    private GRIPPowerPortVisionPipeline gripPowerPortVisionPipeline = new GRIPPowerPortVisionPipeline();

	// This field is used to determine if debugging information should be displayed.
    private boolean debuggingEnabled = false;

    Mat subMat = new Mat(); // place for small image inserted into large image

    // Pixels to Inches Data Table Lookup
    LUT pixelsToInchesTable = new LUT(10); // allocate fixed size array with parameter at least as large as the number of data points - minimum of 2 points

	TargetSelectionB()
	{
        // Enter more data points for more accuracy. The equation should model some sort of sinusoidal function.
        // The x coordinate is pixels and the y coordinate is the horizontal distance to the target in inches.
        // TODO: Notice the LUT CTOR argument is maximum table size - change it if it needs to be larger
        pixelsToInchesTable.add(0.0, 38.0); // enter (x, y) coordinates x ascending order, must add at least 2 data points
        pixelsToInchesTable.add(510.0, 116.0);
        pixelsToInchesTable.add(640.0, 232.0);
        //System.out.println(pixelsToInchesTable); // print the whole table
        // Questionable testing data of Pixel-Inches Readings:
        // horizontal distance is HD and angled distance is AD
        // (110, HD = 128.5 and AD = 143)
        // (30-60, HD = 38 and AD = 72)
        // (0, HD = 232 and AD = 240)
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
        Mat histImage = new Mat();

        if(Main.displayTurretHistogram)
        {
//////////////////////////////////////
// RGB HISTOGRAM OF IMAGE
//
        //  histogram of image mat before any other drawing on mat
        //! [Separate the image in 3 places ( B, G and R )]
        List<Mat> bgrPlanes = new ArrayList<>();
        // try converts
        //Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
        //Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2HLS);
        Core.split(mat, bgrPlanes);
        //! [Separate the image in 3 places ( B, G and R )]

        //! [Establish the number of bins]
        int histSize = 128;
        //! [Establish the number of bins]

        //! [Set the ranges ( for B,G,R) )]
        float[] range = {0, 256}; //the upper boundary is exclusive
        MatOfFloat histRange = new MatOfFloat(range);
        //! [Set the ranges ( for B,G,R) )]

        //! [Set histogram param]
        boolean accumulate = false;
        //! [Set histogram param]

        //! [Compute the histograms]
        Mat bHist = new Mat(), gHist = new Mat(), rHist = new Mat();

        Imgproc.calcHist(bgrPlanes, new MatOfInt(0), new Mat(), bHist, new MatOfInt(histSize), histRange, accumulate);
        Imgproc.calcHist(bgrPlanes, new MatOfInt(1), new Mat(), gHist, new MatOfInt(histSize), histRange, accumulate);
        Imgproc.calcHist(bgrPlanes, new MatOfInt(2), new Mat(), rHist, new MatOfInt(histSize), histRange, accumulate);

        //System.out.println("bHist = " + bHist + "gHist = " + gHist + "rHist = " + rHist);
        //! [Compute the histograms]

        //! [Draw the histograms for B, G and R]

        int histW = 128;
        int histH = 50;
        histImage = new Mat( histH, histW, CvType.CV_8UC3, new Scalar( 0,0,0) );

        int binW = (int) Math.round((double) histW / histSize);

        //! [Draw the histograms for B, G and R]

        //! [Normalize the result to ( 0, histImage.rows )]
        Core.normalize(bHist, bHist, 0, histImage.rows(), Core.NORM_MINMAX);
        Core.normalize(gHist, gHist, 0, histImage.rows(), Core.NORM_MINMAX);
        Core.normalize(rHist, rHist, 0, histImage.rows(), Core.NORM_MINMAX);
        //! [Normalize the result to ( 0, histImage.rows )]

        //! [Draw for each channel]
        float[] bHistData = new float[(int) (bHist.total() * bHist.channels())];
        bHist.get(0, 0, bHistData);
        float[] gHistData = new float[(int) (gHist.total() * gHist.channels())];
        gHist.get(0, 0, gHistData);
        float[] rHistData = new float[(int) (rHist.total() * rHist.channels())];
        rHist.get(0, 0, rHistData);


        for( int i = 1; i < histSize; i++ ) {
            Imgproc.line(histImage, new Point(binW * (i - 1), histH - Math.round(bHistData[i - 1])),
                    new Point(binW * (i), histH - Math.round(bHistData[i])), new Scalar(255, 255, 0), 2, Imgproc.LINE_4);
            Imgproc.line(histImage, new Point(binW * (i - 1), histH - Math.round(gHistData[i - 1])),
                    new Point(binW * (i), histH - Math.round(gHistData[i])), new Scalar(0, 255, 255), 2, Imgproc.LINE_4);
            Imgproc.line(histImage, new Point(binW * (i - 1), histH - Math.round(rHistData[i - 1])),
                    new Point(binW * (i), histH - Math.round(rHistData[i])), new Scalar(255, 0, 255), 2, Imgproc.LINE_4);
        // System.out.print(
        //     binW * (i - 1) + " " +
        //     (histH - Math.round(rHistData[i - 1])) + "   ");
         }
        //! [Draw for each channel]
        // end histogram of image mat before any other drawing on mat

        subMat = mat.submat(0, histImage.rows(), 0, histImage.cols()); // define the insert area on the main image
//
// END RGB HISTOGRAM OF IMAGE - except the presentation of it a couple of lines below
//////////////////////////////////////
        }

  		// Let the gripPowerCellIntakeVisionPipeline filter through the camera frame
        gripPowerPortVisionPipeline.process(mat);
      
        if( ! histImage.empty() )
        {
            Core.addWeighted(subMat, .20, histImage, .80, 0, subMat);
        }
  
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

            nextTargetData.portDistance = -1.;
            nextTargetData.angleToTurn = -1.;
            nextTargetData.isFreshData = true;
            nextTargetData.isTargetFound = false;
        }
        else // if contours were found ...
        {
            if(filteredContours.size() > 1)
            {
                System.err.println(pId + " " + filteredContours.size() + " Contours found");
            }

            Rect boundRect;

            if (debuggingEnabled)
			{
				System.out.println(pId + " " + filteredContours.size() + " contours");

				// Draw all contours at once (negative index).
				// Positive thickness means not filled, negative thickness means filled.
				Imgproc.drawContours(mat, filteredContours, -1, new Scalar(255, 0, 0), 1);
			}

            // Loop through all contours and just remember the last one
            
            int contourIndex = -1;

            for (MatOfPoint contour : filteredContours)
            {
                contourIndex++;
                
                // debug output Print all the contours

                //System.out.println("Contour Index = " + contourIndex);
                //System.out.println(contour.dump()); // OpenCV Mat dump one line string of numbers
                // or more control over formating with your own array to manipualte
				//System.out.println(pId + " " + aContour.size() + " points in contour"); // a contour is a bunch of points
				// convert MatofPoint to an array of those Points and iterate (could do list of Points but no need for this)
				//for(Point aPoint : aContour.toArray())System.out.print(" " + aPoint); // print each point

                MatOfPoint2f NewMtx = new MatOfPoint2f(contour.toArray());

                // for(int idx = 0; idx < contour.toArray().length; idx++)
                // {
                //     System.out.println("(" + contour.toArray()[idx].x + ", " + contour.toArray()[idx].y + ")");
                // }
                
                // Create a bounding upright rectangle for the contour's points
                boundRect = Imgproc.boundingRect(NewMtx);

                // Draw a Rect, using lines, that represents the Rect
                Point boxPts[] = new Point[4];
                boxPts[0] = boundRect.tl();
                boxPts[1] = new Point(boundRect.br().x, boundRect.tl().y);
                boxPts[2] = boundRect.br();
                boxPts[3] = new Point(boundRect.tl().x, boundRect.br().y);
                
                // draw edges of minimum rotated rectangle    
                List<MatOfPoint> listMidContour = new ArrayList<MatOfPoint>();
                listMidContour.add(
                    new MatOfPoint (boxPts[0], boxPts[1], boxPts[2], boxPts[3])
                    );

                Imgproc.polylines (
                    mat,                      // Matrix obj of the image
                    listMidContour,           // java.util.List<MatOfPoint> pts
                    true,                     // isClosed
                    new Scalar(0, 0, 255),  // Scalar object for color
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

                // Find the corner points of the bounding rectangle and the image size
                nextTargetData.boundingBoxPts[0] = boxPts[0];
                nextTargetData.boundingBoxPts[1] = boxPts[1];
                nextTargetData.boundingBoxPts[2] = boxPts[2];
                nextTargetData.boundingBoxPts[3] = boxPts[3];
                nextTargetData.imageSize.width = mat.width();
                nextTargetData.imageSize.height = mat.height();
                nextTargetData.portPositionInFrame = 0.0;

                // Find the degrees to turn by finding the difference between the horizontal center of the camera frame and the horizontal center of the target.
                nextTargetData.angleToTurn = (VERTICAL_CAMERA_ANGLE_OF_VIEW / nextTargetData.imageSize.height) * ((nextTargetData.imageSize.height / 2.0) -
                                                ((nextTargetData.boundingBoxPts[1].y + nextTargetData.boundingBoxPts[2].y) / 2.0)) + Main.calibrateAngle;
                
                if(nextTargetData.angleToTurn <= -VERTICAL_CAMERA_ANGLE_OF_VIEW / 2. || nextTargetData.angleToTurn >= VERTICAL_CAMERA_ANGLE_OF_VIEW / 2.)
                {
                    nextTargetData.portDistance = -1.;
                    nextTargetData.angleToTurn = -1.;
                    nextTargetData.isFreshData = true;
                    nextTargetData.isTargetFound = false;
                }
                else
                {
                    nextTargetData.portDistance = pixelsToInchesTable.lookup(boundRect.br().x);
                    nextTargetData.isFreshData = true;
                    nextTargetData.isTargetFound = true;
                }
            }

            // draw the selected contour
            Imgproc.drawContours(mat, filteredContours, contourIndex, new Scalar(0, 0, 255), 1);

        } // end of processing all contours in this camera frame

        if(!nextTargetData.isTargetFound)
        {
            synchronized(Main.tapeLock)
            {
                Main.tapeDistance = -1.;
                Main.tapeAngle = -1.;
                Main.isTargetFound = false;
                Main.isDistanceAngleFresh = true;
                Main.tapeLock.notify();
            }
        }
        else
        {
            synchronized(Main.tapeLock)
            {
                Main.tapeDistance = nextTargetData.portDistance;
                Main.tapeAngle = nextTargetData.angleToTurn;
                Main.isTargetFound = true;
                Main.isDistanceAngleFresh = true;
                Main.tapeLock.notify();
            }
        }
    }
}

// // Enhance Image if useful - not associated with the Hough Lines below
// // Convert to YUV
// // Apply histogram equalization
// // Convert Back To BGR
// Mat Image_yuv = new Mat(mat.rows(), mat.cols(), CvType.CV_8UC3);
// List<Mat> yuvPlanes = new ArrayList<>();
// Imgproc.cvtColor(mat, Image_yuv, Imgproc.COLOR_BGR2YUV);
// Core.split(Image_yuv, yuvPlanes);
// Imgproc.equalizeHist(yuvPlanes.get(0), yuvPlanes.get(0));
// Core.merge(yuvPlanes, Image_yuv);
// Imgproc.cvtColor(Image_yuv, mat, Imgproc.COLOR_YUV2BGR);



// Hough Lines example in case that is useful to determine target location
// but it is slow so check the frame rates

// The Feature Detection for line segments in GRIP does not work in the OpenCV version in the RPi and roboRIO.
// GRIP has an older version of OpenCV with Line Segment Detection and the RPi and roboRIO have a newer OpenCV
// with only HoughLinesP. The latest OpenCV with Fast Line Segment Detection is not installed in RPi or roboRIO.

//     private HoughLinesRun findLines = new HoughLinesRun(); // testing finding lines

// One way to sharpen an image.  Not needed for line detection. Not necessarily the best sharpener.
// Maybe an OpenCV filter2D edge detector with an appropriate kernel would give good results
// https://en.wikipedia.org/wiki/Kernel_(image_processing)
// // sharpen image
// blur is low pass filter
// subtract the low frequencies from the orignal leaving the middle and higher frequencies
//cv::Mat image = cv::imread(file);
//cv::Mat gaussBlur;
//GaussianBlur(image, gaussBlur, cv::Size(0,0), 3);
//cv::addWeighted(image, 1.5, gaussBlur, -0.5, 0, image);

// findLines.findLines(mat);

//  Hough Transform in OpenCV Lines Parameters
// image	8-bit, single-channel binary source image. The image may be modified by the function.
// lines	output vector of lines(cv.32FC2 type). Each line is represented by a two-element vector (rho,theta) . rho is the distance from the coordinate origin (0,0). theta is the line rotation angle in radians.
// rho	distance resolution of the accumulator in pixels.
// theta	angle resolution of the accumulator in radians.
// threshold	accumulator threshold parameter. Only those lines are returned that get enough votes
// srn	for the multi-scale Hough transform, it is a divisor for the distance resolution rho . The coarse accumulator distance resolution is rho and the accurate accumulator resolution is rho/srn . If both srn=0 and stn=0 , the classical Hough transform is used. Otherwise, both these parameters should be positive.
// stn	for the multi-scale Hough transform, it is a divisor for the distance resolution theta.
// min_theta	for standard and multi-scale Hough transform, minimum angle to check for lines. Must fall between 0 and max_theta.
// max_theta	for standard and multi-scale Hough transform, maximum angle to check for lines. Must fall between min_theta and CV_PI.


//  Probabilistic Hough Transform  Lines Parameters
// image	8-bit, single-channel binary source image. The image may be modified by the function.
// lines	output vector of lines(cv.32SC4 type). Each line is represented by a 4-element vector (x1,y1,x2,y2) ,where (x1,y1) and (x2,y2) are the ending points of each detected line segment.
// rho	distance resolution of the accumulator in pixels.
// theta	angle resolution of the accumulator in radians.
// threshold	accumulator threshold parameter. Only those lines are returned that get enough votes
// minLineLength	minimum line length. Line segments shorter than that are rejected.
// maxLineGap	maximum allowed gap between points on the same line to link them.

//     class HoughLinesRun {
//         public void findLines(Mat src) {
//             // Declare the output variables
//             Mat dst = new Mat(), cdst = new Mat(), cdstP;
//             // Edge detection
//             Imgproc.Canny(src, dst, 50, 200, 3, false);
//             // Copy edges to the images that will display the results in BGR
//             Imgproc.cvtColor(dst, cdst, Imgproc.COLOR_GRAY2BGR);
//             cdstP = cdst.clone();

//             // Standard Hough Line Transform
//             // Line is all the way across the image - probably not what is wanted - it's not a segment
//             // So don't use this
//             Mat lines = new Mat(); // will hold the results of the detection
//             Imgproc.HoughLines(dst, lines, 1, Math.PI/180, 60); // runs the actual detection

//             System.out.println("HoughLines rows = " + lines.rows());
//             // Draw the lines
//             for (int x = 0; x < lines.rows(); x++) {
//                 double rho = lines.get(x, 0)[0],
//                         theta = lines.get(x, 0)[1];
//                 double a = Math.cos(theta), b = Math.sin(theta);
//                 double x0 = a*rho, y0 = b*rho;
//                 Point pt1 = new Point(Math.round(x0 + 1000*(-b)), Math.round(y0 + 1000*(a)));
//                 Point pt2 = new Point(Math.round(x0 - 1000*(-b)), Math.round(y0 - 1000*(a)));
//                 Imgproc.line(src, pt1, pt2, new Scalar(0, 255, 255), 3, Imgproc.LINE_AA, 0);
//             }

//             // Probabilistic Line Transform
//             // Produces Line Segments - probably what is wanted
//             Mat linesP = new Mat(); // will hold the results of the detection
//             Imgproc.HoughLinesP(dst, linesP, 1, Math.PI/180, 50, 50, 10); // runs the actual detection

//             System.out.println("HoughLInesP rows = " + linesP.rows());
//             // Draw the lines
//             for (int x = 0; x < linesP.rows(); x++) {
//                 double[] l = linesP.get(x, 0);
//                 Imgproc.line(src, new Point(l[0], l[1]), new Point(l[2], l[3]), new Scalar(0, 0, 255), 1, Imgproc.LINE_AA, 0);
//             }

//             dst.release();
//             cdst.release();
//             cdstP.release();
//             lines.release();
//             linesP.release();
//         }
//     }
