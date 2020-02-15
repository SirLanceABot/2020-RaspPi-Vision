import java.util.HashMap;
import java.util.Map;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.CvType;
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

        outputStream = CameraServer.getInstance().putVideo("OperatorImage", 640, 160);

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

                int d;
                int a;

                mat = Mat.zeros(160, 640, CvType.CV_8UC3); // blank color Mat to draw on

                synchronized (Main.tapeLock)
                {
                    // get the 2 data points to draw the "cartoon" image
                    if (!Main.isDistanceAngleFresh)
                    {
                       Main.tapeLock.wait();
                    }

                    d = Main.tapeDistance;
                    a = Main.tapeAngle;
                    Main.isDistanceAngleFresh = false;
                }

                Imgproc.putText(mat, String.format("Mr. Thomas was here %d  %d", a, d), new Point(15, 40),
                        Core.FONT_HERSHEY_SIMPLEX, .6, new Scalar(90, 255, 255), 1);

                outputStream.putFrame(mat);

             } catch (Exception e)
                {
                System.out.println(pId + " error " + e);
                e.printStackTrace();
                } 
            }
        }
    }
