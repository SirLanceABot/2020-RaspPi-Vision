import java.lang.invoke.MethodHandles;
import java.lang.IllegalArgumentException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.CvType;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.imgproc.Moments;

import edu.wpi.first.wpilibj.LinearFilter;

import lsd.LSD;
import lsd.Line;

import lut.LUT;

/**
 * This class is used to select the target from the camera frame. The user MUST
 * MODIFY the process() method. The user must create a new
 * gripPowerCellIntakeVisionPipeline class using GRIP, modify the TargetData
 * class, and modify this class.
 * 
 * @author FRC Team 4237
 * @version 2019.01.28.14.20
 */
public class TargetSelectionB {
    static {
        System.out.println("Starting class: " + MethodHandles.lookup().lookupClass().getCanonicalName());
    }

    private static final String pId = new String("[TargetSelectionB]");

    private static final double VERTICAL_CAMERA_ANGLE_OF_VIEW = 35.0;

    // This object is used to run the gripPowerCellIntakeVisionPipeline
    private GRIPPowerPortVisionPipeline gripPowerPortVisionPipeline = new GRIPPowerPortVisionPipeline();

    // This field is used to determine if debugging information should be displayed.
    private boolean debuggingEnabled = false;

    // Mat subMat = new Mat(); // place for small image inserted into large image

    // Pixels to Inches Data Table Lookup
    LUT pixelsToInchesTable = new LUT(10); // allocate fixed size array with parameter at least as large as the number
                                           // of data points - minimum of 2 points

    LinearFilter f = LinearFilter.movingAverage(10); // factory method is self-instantiating; argument is number of samples to average
    
    MatOfPoint idealContour = new MatOfPoint();
    Mat idealHu=Mat.zeros(7, 1, CvType.CV_64FC1); // initialize mat - need to quiet the compiler but this is likely overkill
    
    TargetSelectionB() {
        // Enter more data points for more accuracy. The equation should model some sort
        // of sinusoidal function.
        // The x coordinate is pixels and the y coordinate is the horizontal distance to
        // the target in inches.
        // TODO: Notice the LUT CTOR argument is maximum table size - change it if it
        // needs to be larger
        pixelsToInchesTable.add(66.0, 38.0); // enter (x, y) coordinates x ascending order, must add at least 2 data
                                             // points
        // pixelsToInchesTable.add(510.0, 116.0);
        pixelsToInchesTable.add(596.0, 238.0);
        System.out.println(pId + " pixelsToInchesTable" + pixelsToInchesTable); // print the whole table
        // Questionable testing data of Pixel-Inches Readings:
        // horizontal distance is HD and angled distance is AD
        // (110, HD = 128.5 and AD = 143)
        // (30-60, HD = 38 and AD = 72)
        // (0, HD = 232 and AD = 240)

        //setup ideal target shape to compare with camera image found contour
        // this can vary greatly with perspective distortion so this might have to be dynamically generated
        // based on distance, angles, etc.
    
        /**** CAUTION CAUTION CAUTION  TEST SHAPE FOR RKT'S BASEMENT SCRAP OF RETROREFLECTIVE TAPE */
        /**** CAUTION CAUTION CAUTION  TEST SHAPE FOR RKT'S BASEMENT SCRAP OF RETROREFLECTIVE TAPE */
        /**** CAUTION CAUTION CAUTION  TEST SHAPE FOR RKT'S BASEMENT SCRAP OF RETROREFLECTIVE TAPE */
        /**** CAUTION CAUTION CAUTION  TEST SHAPE FOR RKT'S BASEMENT SCRAP OF RETROREFLECTIVE TAPE */
        idealContour.fromArray(new Point(0., 0.), new Point(45., 0.), new Point(45., 20.), new Point(0., 20.));
        Moments idealMoments = Imgproc.moments(idealContour);
        Imgproc.HuMoments(idealMoments, idealHu);
    }

    /**
     * This method sets the field to display debugging information.
     * 
     * @param enabled Set to true to display debugging information.
     */
    public void setDebuggingEnabled(boolean enabled) {
        debuggingEnabled = enabled;
    }

    /**
     * This method is used to select the next target. The user MUST MODIFY this
     * method.
     * 
     * @param mat            The camera frame containing the image to process.
     * @param nextTargetData The target data found in the camera frame.
     */
    public void process(Mat mat, TargetDataB nextTargetData) {
        // Mat histImage = new Mat();

        // if(Main.displayTurretHistogram)
        // {
        // //////////////////////////////////////
        // // RGB HISTOGRAM OF IMAGE
        // // May be better to try the HSV histogram
        // //
        // // histogram of image mat before any other drawing on mat
        // //! [Separate the image in 3 places ( B, G and R )]
        // List<Mat> bgrPlanes = new ArrayList<>();
        // // try converts
        // //Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
        // //Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2HLS);
        // Core.split(mat, bgrPlanes);
        // //! [Separate the image in 3 places ( B, G and R )]

        // //! [Establish the number of bins]
        // int histSize = 128;
        // //! [Establish the number of bins]

        // //! [Set the ranges ( for B,G,R) )]
        // float[] range = {0, 256}; //the upper boundary is exclusive
        // MatOfFloat histRange = new MatOfFloat(range);
        // //! [Set the ranges ( for B,G,R) )]

        // //! [Set histogram param]
        // boolean accumulate = false;
        // //! [Set histogram param]

        // //! [Compute the histograms]
        // Mat bHist = new Mat(), gHist = new Mat(), rHist = new Mat();

        // Imgproc.calcHist(bgrPlanes, new MatOfInt(0), new Mat(), bHist, new
        // MatOfInt(histSize), histRange, accumulate);
        // Imgproc.calcHist(bgrPlanes, new MatOfInt(1), new Mat(), gHist, new
        // MatOfInt(histSize), histRange, accumulate);
        // Imgproc.calcHist(bgrPlanes, new MatOfInt(2), new Mat(), rHist, new
        // MatOfInt(histSize), histRange, accumulate);

        // //System.out.println("bHist = " + bHist + "gHist = " + gHist + "rHist = " +
        // rHist);
        // //! [Compute the histograms]

        // //! [Draw the histograms for B, G and R]

        // int histW = 128;
        // int histH = 50;
        // histImage = new Mat( histH, histW, CvType.CV_8UC3, new Scalar( 0,0,0) );

        // int binW = (int) Math.round((double) histW / histSize);

        // //! [Draw the histograms for B, G and R]

        // //! [Normalize the result to ( 0, histImage.rows )]
        // Core.normalize(bHist, bHist, 0, histImage.rows(), Core.NORM_MINMAX);
        // Core.normalize(gHist, gHist, 0, histImage.rows(), Core.NORM_MINMAX);
        // Core.normalize(rHist, rHist, 0, histImage.rows(), Core.NORM_MINMAX);
        // //! [Normalize the result to ( 0, histImage.rows )]

        // //! [Draw for each channel]
        // float[] bHistData = new float[(int) (bHist.total() * bHist.channels())];
        // bHist.get(0, 0, bHistData);
        // float[] gHistData = new float[(int) (gHist.total() * gHist.channels())];
        // gHist.get(0, 0, gHistData);
        // float[] rHistData = new float[(int) (rHist.total() * rHist.channels())];
        // rHist.get(0, 0, rHistData);

        // for( int i = 1; i < histSize; i++ ) {
        // Imgproc.line(histImage, new Point(binW * (i - 1), histH -
        // Math.round(bHistData[i - 1])),
        // new Point(binW * (i), histH - Math.round(bHistData[i])), new Scalar(255, 255,
        // 0), 2, Imgproc.LINE_4);
        // Imgproc.line(histImage, new Point(binW * (i - 1), histH -
        // Math.round(gHistData[i - 1])),
        // new Point(binW * (i), histH - Math.round(gHistData[i])), new Scalar(0, 255,
        // 255), 2, Imgproc.LINE_4);
        // Imgproc.line(histImage, new Point(binW * (i - 1), histH -
        // Math.round(rHistData[i - 1])),
        // new Point(binW * (i), histH - Math.round(rHistData[i])), new Scalar(255, 0,
        // 255), 2, Imgproc.LINE_4);
        // // System.out.print(
        // // binW * (i - 1) + " " +
        // // (histH - Math.round(rHistData[i - 1])) + " ");
        // }
        // //! [Draw for each channel]
        // // end histogram of image mat before any other drawing on mat

        // subMat = mat.submat(0, histImage.rows(), 0, histImage.cols()); // define the
        // insert area on the main image
        // //
        // // END RGB HISTOGRAM OF IMAGE - except the presentation of it a couple of
        // lines below
        // //////////////////////////////////////
        // }

        // Let the gripPowerCellIntakeVisionPipeline filter through the camera frame
        gripPowerPortVisionPipeline.process(mat);

        // hopeful that GRIP pipeline found the one and only one target object

        // //could add additional filters if multiple objects found such as Moments or
        // Hu moments
        // //risk in that, too, since the target is significantly distorted at the edges
        // of the camera range - close and far, left and right
        // //see various tutorials such as
        // https://www.learnopencv.com/shape-matching-using-hu-moments-c-python/
        //
        // //start of moment example code hacked from various sources so to use fix it
        // up
        // //look at reference to finish with matchShapes, etc
        // Moments p = Imgproc.moments(contour);
        // Point centerOfMass = new Point(moments.m10 / moments.m00, moments.m01 /
        // moments.m00, 0);
        // int x = (int) (p.get_m10() / p.get_m00());
        // int y = (int) (p.get_m01() / p.get_m00());
        // //add 1e-5 to avoid division by zero
        // mc.add(new Point(mu.get(i).m10 / (mu.get(i).m00 + 1e-5), mu.get(i).m01 /
        // (mu.get(i).m00 + 1e-5)));
        // //draw the COM as a circle
        // Core.circle(smallFrame, new Point(x, y), 4, colors[handIndex]);

        // if( ! histImage.empty() )
        // {
        // Core.addWeighted(subMat, .20, histImage, .80, 0, subMat);
        // }

        // The gripPowerCellIntakeVisionPipeline creates an array of contours that must
        // be searched to find
        // the target.
        ArrayList<MatOfPoint> filteredContours;
        filteredContours = new ArrayList<MatOfPoint>(gripPowerPortVisionPipeline.filterContoursOutput());
        int contourIndex = -1; // initialize here - using same value to indicate no contours and count countours
        double shapeMatchIndex = Double.MAX_VALUE; // initialize here to quiet the compiler

        // Check if no contours were found in the camera frame.
        if (filteredContours.isEmpty()) {
            if (debuggingEnabled) {
                System.out.println(pId + " No Contours");

                // Display a message if no contours are found.
                Imgproc.putText(mat, "No Contours", new Point(20, 20), Core.FONT_HERSHEY_SIMPLEX, 0.25,
                        new Scalar(0, 0, 0), 1);
            }

            nextTargetData.portDistance = -1.;
            nextTargetData.angleToTurn = -1.;
            nextTargetData.isFreshData = true;
            nextTargetData.isTargetFound = false;
        } else {
            // contours were found
            if (filteredContours.size() > 1) { // not good if more than one contour
                System.err.println(pId + " " + filteredContours.size() + " Contours found");
                //TODO: select best contour as the target.  Currently the last one wins.
            }

            Rect boundRect = null; // upright rectangle
            // RotatedRect boundRectAngled; // min area rotated rectangle

            if (debuggingEnabled) {
                System.out.println(pId + " " + filteredContours.size() + " contours");

                // Draw all contours at once (negative index).
                // Positive thickness means not filled, negative thickness means filled.
                Imgproc.drawContours(mat, filteredContours, -1, new Scalar(255, 0, 0), 1);
            }

            // Loop through all contours and just remember the last one

            for (MatOfPoint contour : filteredContours) {
                contourIndex++;

                // debug output Print all the contours

                // System.out.println("Contour Index = " + contourIndex);
                // System.out.println(contour.dump()); // OpenCV Mat dump one line string of
                // numbers
                // or more control over formating with your own array to manipualte
                // System.out.println(pId + " " + aContour.size() + " points in contour"); // a
                // contour is a bunch of points
                // convert MatofPoint to an array of those Points and iterate (could do list of
                // Points but no need for this)
                // for(Point aPoint : aContour.toArray())System.out.print(" " + aPoint); //
                // print each point

                MatOfPoint2f NewMtx = new MatOfPoint2f(contour.toArray());

                // for(int idx = 0; idx < contour.toArray().length; idx++)
                // {
                // System.out.println("(" + contour.toArray()[idx].x + ", " +
                // contour.toArray()[idx].y + ")");
                // }

                // Create a bounding upright rectangle for the contour's points
                boundRect = Imgproc.boundingRect(NewMtx);
                // = Imgproc.minAreaRect(NewMtx); // measurement of the perspective distortion
                // from being off center
                // double angleInnerPort = boundRectAngled.angle;
                // if (angleInnerPort < -45.) angleInnerPort += 90.; // making assumptions about
                // the width/height ratio
                // System.out.println("minAreaRect angle:" + angleInnerPort);

                // Draw a Rect, using lines, that represents the Rect
                Point boxPts[] = new Point[4];
                boxPts[0] = boundRect.tl();
                boxPts[1] = new Point(boundRect.br().x, boundRect.tl().y);
                boxPts[2] = boundRect.br();
                boxPts[3] = new Point(boundRect.tl().x, boundRect.br().y);

                // draw edges of bounding rectangle
                List<MatOfPoint> listMidContour = new ArrayList<MatOfPoint>();
                listMidContour.add(new MatOfPoint(boxPts[0], boxPts[1], boxPts[2], boxPts[3]));

                Imgproc.polylines(mat, // Matrix obj of the image
                        listMidContour, // java.util.List<MatOfPoint> pts
                        true, // isClosed
                        new Scalar(0, 255, 255), // Scalar object for color
                        1, // Thickness of the line
                        Imgproc.LINE_4 // line type
                );

//**************************************** */
//* start match shape
//**************************************** */
            if (Main.turretTargetMatchShape) {
                // TODO: still being developed

                // Use moments to determine shape. Compare moments to moments of known shape.
                // TODO: pick the best match if good enough match

                // draw the contour
                Imgproc.drawContours(mat, filteredContours, contourIndex, new Scalar(0, 0, 255), 1);

                // find the simpler version of the contour - fewer points
                MatOfPoint2f temp = new MatOfPoint2f(filteredContours.get(contourIndex).toArray());
                MatOfPoint2f approxCurve = new MatOfPoint2f();
                Imgproc.approxPolyDP(
                    temp,
                    approxCurve,
                    0.01*Imgproc.arcLength(temp, true),
                    true);

            /*           
                System.out.println(approxCurve + " " + approxCurve.dump());
            Mat [ 7*1*CV_32FC2, isCont=true, isSubmat=false, nativeObj=0x59660870, dataAddr=0x59660900 ]
            [388, 213;  384, 247;  434, 261;  443, 259;  449, 234;  447, 227;  396, 212]
                System.out.println(temp + " " + temp.dump());
            Mat [ 86*1*CV_32FC2, isCont=true, isSubmat=false, nativeObj=0x61c281b8, dataAddr=0x60ee1bc0 ]
           [407, 211; 407, 212; 406, 213; 406, 221; 405, 222; 405, 225; 404, 226; 404, 229; 403, 230; 403, 234; 402, 235;
            402, 241; 403, 242; 405, 242; 406, 243; 408, 243; 409, 244; 412, 244; 413, 245; 415, 245; 416, 246; 419, 246;
            420, 247; 424, 247; 425, 248; 428, 248; 429, 249; 431, 249; 432, 250; 434, 250; 435, 251; 437, 251; 438, 252;
            441, 252; 442, 253; 443, 253; 444, 254; 446, 254; 447, 255; 453, 255; 455, 253; 455, 252; 456, 251; 456, 250;
            457, 249; 457, 247; 458, 246; 458, 244; 459, 243; 459, 241; 460, 240; 460, 237; 461, 236; 461, 233; 462, 232;
            462, 226; 460, 226; 459, 225; 457, 225; 456, 224; 454, 224; 453, 223; 450, 223; 449, 222; 447, 222; 446, 221;
            444, 221; 443, 220; 441, 220; 440, 219; 438, 219; 437, 218; 434, 218; 433, 217; 431, 217; 430, 216; 428, 216;
            427, 215; 425, 215; 424, 214; 422, 214; 421, 213; 419, 213; 418, 212; 415, 212; 414, 211]
            */
                //************************************************************** */
                // start use compareShapes
                List<Point> listApprox = new ArrayList<Point>(approxCurve.toList());
                
                //System.out.println(temp.size() + " " + listApprox.size()); // 1x86 7

                //listMidContour.add(new MatOfPoint(boxPts[0], boxPts[1], boxPts[2], boxPts[3]));
                // draw the simple contour, too
                for (int idx = 0; idx< listApprox.size(); idx++) 
                {
                    Imgproc.line(
                        mat,
                        listApprox.get(idx), // one end of the line
                        idx+1==listApprox.size() ? listApprox.get(0): listApprox.get(idx+1), // other end - close the shape on the last point
                        new Scalar(255, 255, 0),
                        1,
                        Imgproc.LINE_AA,
                        0);
                }

                //System.out.println(moments);
                //System.out.println(hu + hu.dump());

                // double[] huTemp = new double[7];
                // hu.get(0, 0, huTemp);

                // compute moments and Hu moments for our own compareShapes.  matchShapes() does it for us.
                Moments moments;
                Mat actualHu=Mat.zeros(7, 1, CvType.CV_64FC1); // initialize mat - need to quiet the compiler but this is likely overkill
                //moments = Imgproc.moments(approxCurve);
                moments = Imgproc.moments(filteredContours.get(contourIndex));
                Imgproc.HuMoments(moments, actualHu);
                //System.out.println(hu);

                double[] compare = compareShapes(idealHu, actualHu);
                compare = compareShapes(idealHu, actualHu);
                compare = compareShapes(idealHu, actualHu);
                System.out.println(pId + compare[0] + " " + compare[1] + " " + compare[2]);
                // end use compareShapes
                //********************************************* */

                // this matching is rotation invariant so better eliminate incorrect aspect ratio first in filter contours
                // because, for example, rectangles that are 2:1 are liked as well as those that are 1:2
                shapeMatchIndex = Imgproc.matchShapes(idealContour, filteredContours.get(contourIndex), Imgproc.CONTOURS_MATCH_I1, 0.0);
                //System.out.println(shapeMatchIndex);
                //System.out.print("my 1 compare " + compare + " opencv compare " + shapeMatchIndex);
                
                // TODO: display quality of shape on Shuffleboard?


                // also TODO: use the comparison or absolute value of distance sensors to filter bad "targets"
                // at St Joe 2020, Robot shot at a big green team sign in the bleachers thinking it was a small nearby target
            }
//**************************************** */
//* end match shape
//**************************************** */

                // OR the point to point way
                // for(int i = 0; i<4; i++)
                // {
                // Imgproc.line(mat, boxPts[i], boxPts[(i+1)%4], new Scalar(255, 0, 255));
                // }

                // // draw stars at corners of rectangle
                // Imgproc.drawMarker(mat, new Point( boxPts[0].x,
                // Math.min(boxPts[0].y,(double)mat.cols() - 10.) ),
                // new Scalar( 0, 255, 0), Imgproc.MARKER_STAR, 7);// green - always max y - can
                // be > image max y
                // Imgproc.drawMarker(mat, boxPts[1], new Scalar(255, 255, 0),
                // Imgproc.MARKER_STAR, 7);// teal then cw from 0
                // Imgproc.drawMarker(mat, boxPts[2], new Scalar( 0, 255, 255),
                // Imgproc.MARKER_STAR, 7);// yellow
                // Imgproc.drawMarker(mat, boxPts[3], new Scalar(255, 0, 255),
                // Imgproc.MARKER_STAR, 7);// magenta

                // Find the corner points of the bounding rectangle and the image size
                nextTargetData.boundingBoxPts[0] = boxPts[0];
                nextTargetData.boundingBoxPts[1] = boxPts[1];
                nextTargetData.boundingBoxPts[2] = boxPts[2];
                nextTargetData.boundingBoxPts[3] = boxPts[3];
                nextTargetData.imageSize.width = mat.width();
                nextTargetData.imageSize.height = mat.height();
                nextTargetData.portPositionInFrame = 0.0;

                // Find the degrees to turn the turret by finding the difference between the
                // horizontal center of the camera frame
                // and the horizontal center of the target.
                // calibrateAngle is the difference between what the camera sees as the
                // retroreflective tape target and where
                // the Power Cells actually hit - the skew of the shooting process or camera
                // misalignment.
                nextTargetData.angleToTurn = (VERTICAL_CAMERA_ANGLE_OF_VIEW / nextTargetData.imageSize.height)
                        * ((nextTargetData.imageSize.height / 2.0)
                                - ((nextTargetData.boundingBoxPts[1].y + nextTargetData.boundingBoxPts[2].y) / 2.0))
                        + Main.calibrateAngle;

                if (nextTargetData.angleToTurn <= -VERTICAL_CAMERA_ANGLE_OF_VIEW / 2.
                        || nextTargetData.angleToTurn >= VERTICAL_CAMERA_ANGLE_OF_VIEW / 2.) { // target not actually "seen" after the
                                                                                               // calibrateAngle offset was applied
                    nextTargetData.portDistance = -1.;
                    nextTargetData.angleToTurn = -1.;
                    nextTargetData.isFreshData = true;
                    nextTargetData.isTargetFound = false;
                } else { // target still in view
                    nextTargetData.portDistance = pixelsToInchesTable.lookup(boundRect.br().x);
                    nextTargetData.isFreshData = true;
                    nextTargetData.isTargetFound = true;
                    if (Main.displayTurretPixelDistance) {
                        System.out.println(
                                pId + " pixels:" + boundRect.br().x + ", LUT inches:" + nextTargetData.portDistance);
                    }
                }
            } // end of looping through all contours

            // ****************************************************************************************
            //
            // start experimental LSD - the old one used in OpenCV converted to Java  - using the last contour
            // ****************************************************************************************
            if (Main.turretTargetLSD) {
                try {
                    Mat threshold;
                    // check limits of mat but really it's
                    // gripPowerPortVisionPipeline.cvErode1Output() so it had better be based on mat
                    // exception here 0 <= roi.x && 0 <= roi.width && roi.x + roi.width <= m.cols &&
                    // 0 <= roi.y && 0 <= roi.height && roi.y + roi.height <= m.rows
                    Rect lsdRect = new Rect(
                            new Point(Math.max((int) boundRect.tl().x - 8, 0), Math.max((int) boundRect.tl().y - 8, 0)),
                            new Point(Math.min((int) boundRect.br().x + 8, mat.cols()),
                                    Math.min((int) boundRect.br().y + 8, mat.rows())));

                    threshold = gripPowerPortVisionPipeline.cvErode1Output().submat(lsdRect);

                    // System.out.println(threshold.channels() + " " + threshold.total() + " " +
                    // //threshold.toString() +
                    // " " + threshold.cols() + " " + threshold.rows());

                    // int t = threshold.type();
                    // System.out.println(CvType.depth(t) + " " + CvType.CV_8U + " " +
                    // CvType.CV_8S);

                    // example process each pixel with conversion to double
                    //
                    // could stick with the conversion to byte instead of short to save memory but
                    // that means the data are
                    // in Java -128 to 127 signed byte - see example below
                    // Mat temp = new Mat(mat.rows(), mat.cols(), CvType.CV_16SC3);
                    // mat.convertTo(temp, CvType.CV_16SC3);
                    // System.out.println(threshold.dump());

                    byte[] flatMat = new byte[(int) threshold.total()];
                    threshold.get(0, 0, flatMat);

                    double[] flatmatLSD = new double[(int) threshold.total()];

                    for (int idx = 0; idx < threshold.total(); idx++) {
                        flatmatLSD[idx] = flatMat[idx] == 0 ? 0. : 255.;
                    }

                    LSD lsd = new LSD();

                    double[] out = lsd.lsd(flatmatLSD, threshold.cols(), threshold.rows());
                    // System.out.println(lsd.n_out + " " + out.length);

                    HashSet<Line> lines = new HashSet<Line>();

                    for (int i = 0; i < lsd.n_out; i++) {
                        lines.add(new Line(out[7 * i + 0], out[7 * i + 1], out[7 * i + 2], out[7 * i + 3]));
                    }

                    for (Line l : lines) {
                        // System.out.format("{(%f, %f),(%f, %f)}\n",l.x1,l.y1,l.x2,l.y2);
                        Imgproc.line(
                            mat,
                            new Point(l.x1 + lsdRect.tl().x, l.y1 + lsdRect.tl().y),
                            new Point(l.x2 + lsdRect.tl().x, l.y2 + lsdRect.tl().y),
                            new Scalar(255, 255, 255), 1, Imgproc.LINE_AA, 0);
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }

            // ****************************************************************************************
            // end experimental LSD - the old one used in OpenCV converted to Java
            //
            // ****************************************************************************************

            // draw the contour TODO: draw the best one not the last one
            Imgproc.drawContours(mat, filteredContours, contourIndex, new Scalar(0, 0, 255), 1);

        } // end of processing all contours in this camera frames

        // update the target information - TODO: last contour found wins until check for best implemented
        synchronized (Main.tapeLock) {
            Main.tapeDistance = nextTargetData.portDistance;
            Main.tapeAngle = nextTargetData.angleToTurn;
            Main.isTargetFound = nextTargetData.isTargetFound;
            Main.tapeContours = contourIndex;
            Main.shapeQuality = shapeMatchIndex;
            Main.isDistanceAngleFresh = nextTargetData.isFreshData;
            Main.tapeLock.notify();
        }
    }

    // essentially a copy of OpenCV matchShapes without calculating the Moments
    // allows efficiency of not recalculating the target shape moments every time
    // does all 3 comparisons since it is very little more than to do one
    double[] compareShapes(Mat HuShape1, Mat HuShape2){
        if((HuShape1.rows() != 7) || (HuShape2.rows() != 7)) throw new IllegalArgumentException("HuMoments must be 7x1");
        if((HuShape1.cols() != 1) || (HuShape2.cols() != 1)) throw new IllegalArgumentException("HuMoments must be 7x1");

        double[] compare = {0., 0., 0.};    
        boolean anyA = false, anyB = false;
        double[] HuTemp1=new double[7], HuTemp2=new double[7];
        double eps = 1.e-5;        
        HuShape1.get(0, 0, HuTemp1);
        HuShape2.get(0, 0, HuTemp2);

        for(int idx = 0; idx < 7; idx++){
            if(HuTemp1[idx] != 0.) anyA = true;
            if(HuTemp2[idx] != 0.) anyB = true;
            if( (Math.abs(HuTemp1[idx]) > eps) && (Math.abs(HuTemp2[idx]) > eps) )
            {
                double mA = Math.copySign( Math.log10(Math.abs(HuTemp1[idx]) ), HuTemp1[idx] ); // typically seen with minus sign in front
                double mB = Math.copySign( Math.log10(Math.abs(HuTemp2[idx]) ), HuTemp2[idx] ); // but don't bother since it's abs here in compare
                compare[0] += Math.abs( (1./mA) - (1./mB) );
                compare[1] += Math.abs( mA - mB );
                compare[2] = Math.max( compare[2], Math.abs( ( mA - mB ) / mA ) );
            }
        }

        if (anyA != anyB) {
            compare[0] = Double.MAX_VALUE;
            compare[1] = Double.MAX_VALUE;
            compare[2] = Double.MAX_VALUE;
        }

        return compare;
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

    // // example process each pixel with conversion to short
    // //
    // // could stick with the conversion to byte instead of short to save memory but that means the data are
    // // in Java -128 to 127 signed byte - see example below
    // Mat temp = new Mat(mat.rows(), mat.cols(), CvType.CV_16SC3);
    // mat.convertTo(temp, CvType.CV_16SC3);
    // short[] flatMat = new short[(int)temp.total()*temp.channels()];
    // temp.get(0, 0, flatMat);
    
    // for (int idx = 0; idx<mat.total()*mat.channels();idx=idx+mat.channels())
    //  {
    //      flatMat[idx] = 0; // B
    //      flatMat[idx+1] = 0; // G
    //      flatMat[idx+2] = 255; // R
    // //     System.out.println(flatMat[idx] + " " + flatMat[idx+1] + " " + flatMat[idx+2]);
    //  }
    
    //  temp.put(0, 0, flatMat);

    //  temp.convertTo(mat, CvType.CV_8UC3);
    // for (int idx = 0; idx<60 /*mat.total()*mat.channels()*/;idx=idx+mat.channels())
    // {
    //     System.out.println(flatMat[idx] + " " + flatMat[idx+1] + " " + flatMat[idx+2]);
    // }
    // //
    // // end example process each pixel with conversion to short

    // // example process each pixel with byte
    // //
    // // OpenCV data are 0 to 255, which is 0 to 127 then -128 to -1, 255 is -1 in Java, 254 is -2, etc
    // byte[] flatMat = new byte[(int)mat.total()*mat.channels()];
    // mat.get(0, 0, flatMat);
    // int idx = 0;
    // int red;
    // for (int idxR = 0; idxR<mat.rows(); idxR++)
    // for (int idxC = 0; idxC<mat.cols(); idxC++)
    // {
    //     smoothly increase red from 0 to almost 255 from top row to bottom row
    //     same value of red across the whole row - all columns are the same red
    //     flatMat[idx] = 0; // B
    //     flatMat[idx+1] = 0; // G
    //     red = idxR *2;
    //     if (red > 127) red = red - 256;
    //     if (red < -128) red = -128;
    //     flatMat[idx+2] = (byte)red; // R -1 is actually 255, -2 is 254, etc. -128 medium dark
    //     idx = idx + mat.channels();
    //     //     System.out.println(flatMat[idx] + " " + flatMat[idx+1] + " " + flatMat[idx+2]);
    //     }
    // for (int idx2 = 0; idx2<mat.total()*mat.channels(); idx2=idx2+(mat.channels()*mat.cols()))
    // {
    //     System.out.println(flatMat[idx2] + " " + flatMat[idx2+1] + " " + flatMat[idx2+2]);
    // }
    // mat.put(0, 0, flatMat);
    // //
    // // end example process each pixel with byte

    // // example of smoothing values - may help any jitters or maybe useless
    // // example is the first Hu moment
    // double[] huTemp = new double[7];
    // hu.get(0, 0, huTemp);
    // //System.out.println(huTemp[0] + ", smooth=" + f.calculate(huTemp[0]));
    // //-np.sign(hu) * np.log10(np.abs(hu)) log of Hu may be an improvement (np is NumPy; use Math in Java)
