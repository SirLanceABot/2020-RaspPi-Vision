import java.util.HashMap;
import java.util.Map;

import java.util.ArrayList;
import java.lang.invoke.MethodHandles;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.CvType;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;

import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;

public class ImageOperator implements Runnable {
    static {System.out.println("Starting class: " + MethodHandles.lookup().lookupClass().getCanonicalName());}
    
    private static final String pId = new String("[ImageOperator]");
    private static final double VERTICAL_CAMERA_ANGLE_OF_VIEW = 35.0;

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
        System.out.println(pId + " Starting run");

        this.setDebuggingEnabled(Main.debug);

        int height = 24;
        int width = 320;
        int fps = 10; // it paces itself to match contours for the process but server goes at this speed
        int jpegQuality = 10; // lower is more compression - less quality, 100 is no compression, -1 is default compression

        Mat mat; // Mat to draw on

        // The following single statement does correctly define a camera server for the OpenCV image to be displayed in ShuffleBoard
        // The limitation is there is no visibility to the MjpegSever parameters such as Quailty (compression)
        // outputStream = CameraServer.getInstance().putVideo("OperatorImage", 640, 45);

        // Or start the video stream this way but then it isn't shown by its "nice" name and ShuffleBoard does display it
        // We have control over the port used, Quality (compression) parameters and more
        // CvSource outputStream = new CvSource("OperatorImage", VideoMode.PixelFormat.kMJPEG, 640, 45, 30);
        // MjpegServer mjpegServer = new MjpegServer("serve_OperatorImage", 1186);
        // mjpegServer.setCompression(50);
        // mjpegServer.setSource(outputStream);

        // The following will display the image on ShuffleBoard and reveal the MjpegServer parameters
        CvSource outputStream = new CvSource("OperatorImage", VideoMode.PixelFormat.kMJPEG, width, height, fps);
        MjpegServer mjpegServer = CameraServer.getInstance().startAutomaticCapture(outputStream);
        mjpegServer.setResolution(width, height);
        mjpegServer.setFPS(fps);
        mjpegServer.setCompression(jpegQuality);

        //////////////////
        // put to the Shuffleboard now
        // Widget in Shuffleboard Tab
        Map<String, Object> cameraWidgetProperties = new HashMap<String, Object>();
        cameraWidgetProperties.put("Show crosshair", false);
        cameraWidgetProperties.put("Show controls", false);

        synchronized (Main.tabLock)
        {
            Main.cameraTab.add("High Power Port Alignment", outputStream).withWidget(BuiltInWidgets.kCameraStream)
                    .withProperties(cameraWidgetProperties)
                    .withSize(12, 1)
                    .withPosition(20, 0)
            ;

            Shuffleboard.update();
        }
        //
        //////////////////

        while(true){
            try{
                // DRAW HERE
                double portDistance;
                double angleToTurn;
                int contourIndex;
                boolean isTargetFound;
                // could consider black & white mat to save network bandwidth but it's pretty small even with color
                mat = Mat.zeros(height, width, CvType.CV_8UC3); // blank color Mat to draw on

                synchronized (Main.tapeLock)
                {
                    // get the 3 data points to draw the "cartoon" image
                    if (!Main.isDistanceAngleFresh)
                    {
                       Main.tapeLock.wait();
                    }

                    portDistance = Main.tapeDistance;
                    angleToTurn = Main.tapeAngle;
                    contourIndex = Main.tapeContours;
                    isTargetFound = Main.isTargetFound;
                    // Are we using the test shape or the real tape?
                    // Look in TargetSelectionB for the definition of the shape
                    System.out.println(pId + " test shape quality " + Main.shapeQuality); // TODO: display quality of shape on Shuffleboard?
                    Main.isDistanceAngleFresh = false; // these data captured to be processed so mark them as used
                }

                // Draw the green cross representing the center of the camera frame.
                Imgproc.drawMarker(mat, new Point(mat.width() / 2.0, mat.height() / 2.0), 
                    new Scalar(0, 255, 0), Imgproc.MARKER_CROSS, 40);
                // Draw a green circle around the green cross
                Imgproc.circle(mat, new Point(mat.width() / 2.0, mat.height() / 2.0), (int)(mat.height() / 2.0), 
                    new Scalar(0, 255, 0), 2);

                // Red hexagon:
                int offset;
                
                if(!isTargetFound)
                {
                    Imgproc.putText(mat, "NO TARGET", new Point(34, 13),
                            Core.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(255, 255, 255), 2);
                }
                else
                {
                    offset = (int) // where to place the hexagon "target" image
                    ( ((double)mat.width()/VERTICAL_CAMERA_ANGLE_OF_VIEW)*angleToTurn + (double)mat.width()/2.0 );
                    ArrayList<MatOfPoint> listOfHexagonPoints = new ArrayList<MatOfPoint>();
                    listOfHexagonPoints.add(new MatOfPoint
                        (
                        new Point(              offset, mat.height()), // bottom
                        new Point( height*0.5 + offset, mat.height()*0.667), // lower right
                        new Point( height*0.5 + offset, mat.height()*0.333), // upper right
                        new Point(              offset, 0.),                 // top
                        new Point( -height*0.5+ offset, mat.height()*0.333), // upper left
                        new Point( -height*0.5+ offset, mat.height()*0.667)  // lower left
                        )
                    ); 

                    if( (contourIndex > 0) || (Main.shapeQuality  > Main.shapeQualityBad))  // 0 is the index of the first contour; should be the only one
                    {
                        Imgproc.fillConvexPoly(mat, listOfHexagonPoints.get(0), new Scalar(0, 0, 255), 1, 0);
                        // Imgproc.putText(mat, String.format("%d", contourIndex+1), new Point(offset-3, mat.height() / 2 + 4),
                        //     Core.FONT_HERSHEY_SIMPLEX, 0.4, new Scalar(50, 50, 50), 2);
                    }
                    else
                    {
                        Imgproc.polylines(mat, listOfHexagonPoints, true, new Scalar(0xad, 0xa9, 0xaa), 2, 1);
                    }
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

                // Draw distance text.
                String printDistance = String.format("%d", (int)(portDistance+.5));

                Imgproc.putText(mat, printDistance, new Point(5, 13),
                    Core.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(255, 255, 255), 2);

                Imgproc.putText(mat, printDistance, new Point(mat.width() - 37, 13),
                    Core.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(255, 255, 255), 2);

                Imgproc.putText(mat, "inches", new Point(5, 22),
                    Core.FONT_HERSHEY_SIMPLEX, .3, new Scalar(255, 255, 255), 1);

                Imgproc.putText(mat, "inches", new Point(mat.width() - 37, 22),
                    Core.FONT_HERSHEY_SIMPLEX, .3, new Scalar(255, 255, 255), 1);

                outputStream.putFrame(mat);

             }  catch (Exception exception)
                {
                    System.out.println(pId + " error " + exception);
                    exception.printStackTrace();
                } 
            }
        }
    }
