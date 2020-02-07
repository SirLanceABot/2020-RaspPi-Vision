// IMPORTANT***********************************************************
// Pixel-Inches Readings:
// horizontal distance is HD and angled distance is AD
// (110, HD = 128.5 and AD = 143)
// (30-60, HD = 38 and AD = 72)
// (0, HD = 232 and AD = 240)



import java.util.ArrayList;
import java.util.List;
import java.lang.Math;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
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
            Rect boundRect;

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
                
                // debug output Print all the contours

                //System.out.println("Contour Index = " + contourIndex);
                //System.out.println(contour.dump()); // OpenCV Mat dump one line string of numbers
                // or more control over formating with your own array to manipualte
				//System.out.print("[Vision] " + aContour.size() + " points in contour\n[Vision]"); // a contour is a bunch of points
				// convert MatofPoint to an array of those Points and iterate (could do list of Points but no need for this)
				//for(Point aPoint : aContour.toArray())System.out.print(" " + aPoint); // print each point

                MatOfPoint2f  NewMtx = new MatOfPoint2f(contour.toArray());

                // for(int idx = 0; idx < contour.toArray().length; idx++)
                // {
                //     System.out.println("(" + contour.toArray()[idx].x + ", " + contour.toArray()[idx].y + ")");
                // }
                
                // Create a bounding upright rectangle for the contour's points
                boundRect = Imgproc.boundingRect(NewMtx);

                // Draw a rotatedRect, using lines, that represents the minAreaRect
                Point boxPts[] = new Point[4];
                boxPts[0] = boundRect.tl();
                boxPts[1] = new Point(boundRect.br().x, boundRect.tl().y);
                boxPts[2] = boundRect.br();
                boxPts[3] = new Point(boundRect.tl().x, boundRect.br().y);
                
                // Determine if this is the best contour using center.y
                // TODO: Review this
                // if (nextTargetData.center.y < boundRect.center.y)
                // {
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
 
                    // Find the corner points of the bounding rectangle and the image size
                    nextTargetData.boundingBoxPts[0] = boxPts[0];
                    nextTargetData.boundingBoxPts[1] = boxPts[1];
                    nextTargetData.boundingBoxPts[2] = boxPts[2];
                    nextTargetData.boundingBoxPts[3] = boxPts[3];
                    nextTargetData.imageSize.width = mat.width();
                    nextTargetData.imageSize.height = mat.height();
                    nextTargetData.widthOfPortDistance = 0.0;
                    // Find the shorest distance from the left of the frame to the box
                    nextTargetData.locationOfPortDistance = boundRect.tl().x;
                    System.out.println("Distance in pixels = " + nextTargetData.locationOfPortDistance);

                    /*
                    // Find the center x, center y, width, height, and angle of the bounding rectangle
                    nextTargetData.center = boundRect.center;
                    nextTargetData.size = boundRect.size;
                    nextTargetData.angle = boundRect.angle;
                    */

                    // Imgproc.putText(mat, String.format("%5.0f", boundRect.angle), new Point(15, 15),
                    //   Core.FONT_HERSHEY_SIMPLEX, .6, new Scalar(255, 255, 255), 1);
 
                    // nextTargetData.fixedAngle = 90.0;
 
                    //Update the target
                    nextTargetData.isFreshData = true;
                    nextTargetData.isTargetFound = true;
                // }
            }
            // draw the best - selected - contour
            Imgproc.drawContours(mat, filteredContours, bestContourIndex, new Scalar(0, 0, 255), 1);

            /* 2019 code
            // draw the pointer to the target
            double angleInRadians = nextTargetData.fixedAngle * (Math.PI/180);
            endpoint.x = nextTargetData.center.x - ( (nextTargetData.size.height / 2) * Math.cos(angleInRadians) );
            endpoint.y = nextTargetData.center.y - ( (nextTargetData.size.height / 2) * Math.sin(angleInRadians) );
            Imgproc.line(mat, nextTargetData.center, endpoint,  new Scalar(255, 0, 255), 1, Imgproc.LINE_4);
            Mat bestContour = Mat.zeros(mat.rows(), mat.cols(), CvType.CV_8UC1); // blank Mat to draw the best Contour on
            Imgproc.drawContours(bestContour, filteredContours, bestContourIndex, new Scalar(255), 1, Imgproc.LINE_4);
            */

        } // end of processing all contours in this camera frame
        }
    }
}