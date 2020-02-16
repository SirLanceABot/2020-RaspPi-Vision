import java.util.HashMap;
import java.util.Map;

import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.core.Scalar;
import org.opencv.core.CvType;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;

import edu.wpi.cscore.CvSource;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;

public class ImageOperator implements Runnable {
    private static final String pId = new String("[ImageOperator]");

    // This object is used to send the image to the Dashboard
    private CvSource outputStream;

    // This field is used to determine if debugging information should be displayed.
    // Use the setDebuggingEnabled() method to set this value.
    private boolean debuggingEnabled = false;

    /**
     * This method sets the field to display debugging information.
     * 
     * @param enabled Set to true to display debugging information.
     */
    public void setDebuggingEnabled(boolean enabled) {
        debuggingEnabled = enabled;
    }

    @Override
    public void run() {
        System.out.println(pId + " Thread Started");

        this.setDebuggingEnabled(Main.debug);

        Mat mat; // Mat to draw on

        outputStream = CameraServer.getInstance().putVideo("OperatorImage", 640, 45);

        // starting the video stream could look like this but then it isn't shown by its
        // "nice" name
        // but we then do have control over the port used
        // CvSource outputStream = new CvSource("DriverView",
        // VideoMode.PixelFormat.kMJPEG, 320, 240, 30);
        // // CvSource cvsource = new CvSource("cvsource", VideoMode.PixelFormat.kMJPEG,
        // width, height, frames_per_sec);
        // MjpegServer mjpegServer = new MjpegServer("serve_DriverView", 1185);
        // mjpegServer.setSource(outputStream);

        //////////////////
        // TODO: Since Main was refactored this might not be the exact or best way to
        ////////////////// get to the Shuffleboard now
        // Widget in Shuffleboard Tab
        Map<String, Object> mapVideo = new HashMap<String, Object>();
        mapVideo.put("Show crosshair", false);
        mapVideo.put("Show controls", false);

        synchronized (Main.tabLock) {
            Main.cameraTab.add("ImageMerge", outputStream).withWidget(BuiltInWidgets.kCameraStream)
                    .withProperties(mapVideo)
            // .withSize(12, 8)
            // .withPosition(1, 2)
            ;

            Shuffleboard.update();
        }
        //////////////////

        while (true) {
            try {
                // DRAW HERE
                int portDistance, angleToTurn;
                //TODO: consider black & white mat to save network bandwidth
                mat = Mat.zeros(45, 640, CvType.CV_8UC3); // blank color Mat to draw on

                synchronized (Main.tapeLock)
                {
                    // get the 2 data points to draw the "cartoon" image
                    if (!Main.isDistanceAngleFresh)
                    {
                       Main.tapeLock.wait();
                    }

                    portDistance = Main.tapeDistance;
                    angleToTurn = Main.tapeAngle;
                    Main.isDistanceAngleFresh = false;
                    //TODO: add is-target-found
                }

                 // Draw shapes
                Imgproc.drawMarker(mat, new Point(mat.width() / 2.0, mat.height() / 2.0), 
                 new Scalar(0, 255, 0), Imgproc.MARKER_CROSS, 40);// green cross representing center of camera frame
                Imgproc.circle(mat, new Point(mat.width() / 2.0, mat.height() / 2.0), 20, 
                 new Scalar(0, 255, 0), 2); // green circle surrounding green cross

                // Red hexagon:
                int offset;
                
                //TODO:  combine <-15 and >+15 and also the variable is-target-found
                if(angleToTurn <= -15)
                {
                    Imgproc.putText(mat, String.format("Target not found"), new Point(15, 35),
                            Core.FONT_HERSHEY_SIMPLEX, .6, new Scalar(255, 255, 255), 1);
                }
                else if(angleToTurn > -15 && angleToTurn < 15)
                {
                    offset = (int)
                    ( ((double)mat.width()/30.0)*angleToTurn + (double)mat.width()/2.0 );
                    //TODO: fix the unchecked conversion error
                    ArrayList<MatOfPoint> listOfHexagonPoints = new ArrayList();
                    listOfHexagonPoints.add(new MatOfPoint
                                (
                                new Point(      offset, mat.height() / 2 + 20),
                                new Point( 20 + offset, mat.height() / 2 + 10), 
                                new Point( 20 + offset, mat.height() / 2 - 10), 
                                new Point(      offset, mat.height() / 2 - 20), 
                                new Point(-20 + offset, mat.height() / 2 - 10), 
                                new Point(-20 + offset, mat.height() / 2 + 10)  
                                )
                            );
                    Imgproc.polylines(mat, listOfHexagonPoints, true, new Scalar(0, 0, 255), 2, 1);

                    //TODO: rotate the hexagon and NOT this way
                    //Mat subMat = mat.submat(mat.height() / 2 - 21, mat.height() / 2 + 21, -21 + offset, 21 + offset);
                    //Creating the transformation matrix M
                    //Mat rotationMatrix = Imgproc.getRotationMatrix2D(new Point(offset, mat.height() /2), 30, 1);
                    //Rotating the given image
                    //Imgproc.warpAffine(subMat, subMat,rotationMatrix, new Size(42, 42));

                    // if(angleToTurn > -15 && angleToTurn < 0)
                    // {
                    //     Imgproc.putText(mat, String.format("Turn left %d deg.", angleToTurn), new Point(15, 35),
                    //         Core.FONT_HERSHEY_SIMPLEX, .4, new Scalar(255, 255, 255), 1);
                    // }
                    // else if(angleToTurn == 0)
                    // {
                    //     Imgproc.putText(mat, String.format("Shoot!"), new Point(15, 35),
                    //         Core.FONT_HERSHEY_SIMPLEX, .4, new Scalar(255, 255, 255), 1);
                    // }
                    // else if(angleToTurn > 0 && angleToTurn < 15)
                    // {
                    //     Imgproc.putText(mat, String.format("Turn right %d deg.", angleToTurn), new Point(15, 35),
                    //         Core.FONT_HERSHEY_SIMPLEX, .4, new Scalar(255, 255, 255), 1);
                    // }
                }
                else
                {
                    Imgproc.putText(mat, String.format("Target not found"), new Point(15, 35),
                            Core.FONT_HERSHEY_SIMPLEX, .8, new Scalar(255, 255, 255), 1);
                }

             // Draw distance text and text
             Imgproc.putText(mat, String.format("%d", portDistance), new Point(15, 15),
                 Core.FONT_HERSHEY_SIMPLEX, .4, new Scalar(255, 255, 255), 1);

                outputStream.putFrame(mat);

             } catch (Exception exception)
                {
                System.out.println(pId + " error " + exception);
                exception.printStackTrace();
                } 
            }
        }
    }
