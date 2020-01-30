import java.util.HashMap;
import java.util.Map;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import edu.wpi.cscore.CvSource;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;

public class ImageMerge implements Runnable
{
    private static final String pId = new String("[ImageMerge]");

    // This object is used to send the image to the Dashboard
    private CvSource outputStream;

   	// This field is used to determine if debugging information should be displayed.
	// Use the setDebuggingEnabled() method to set this value.
	private boolean debuggingEnabled = false;

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

    @Override
    public void run()
    {
        System.out.println(pId + " Thread Started");

        this.setDebuggingEnabled(Main.debug);
        
        Mat ImageOverlay = new Mat(); // main image from elevator
        Mat ImageOutput = new Mat(); // main image from elevator + small bumper image inserted then weighted merge
                                     // and saved
        Mat subMat = new Mat(); // place for small bumper image inserted into main elevator image
        Mat insert = new Mat(); // bumper image
        Mat insertSmall = new Mat(); // bumper image shrunk

        outputStream = CameraServer.getInstance().putVideo("MergedImages", 320, 240);

        // starting the video stream could look like this but then it isn't shown by its "nice" name
        // but we then do have control over the port used
        // CvSource outputStream = new CvSource("DriverView",
        // VideoMode.PixelFormat.kMJPEG, 320, 240, 30);
        // // CvSource cvsource = new CvSource("cvsource", VideoMode.PixelFormat.kMJPEG,
        // width, height, frames_per_sec);
        // MjpegServer mjpegServer = new MjpegServer("serve_DriverView", 1185);
        // mjpegServer.setSource(outputStream);

        // //////////////////
        // // uncomment all this to make a Shuffleboard widget for this video stream
        // // Widget in Shuffleboard Tab
		// Map<String, Object> mapVideo = new HashMap<String, Object>();
		// mapVideo.put("Show crosshair", false);
		// mapVideo.put("Show controls", false);
        //
		// synchronized(Main.obj.tabLock)
		// {
		// Main.obj.cameraTab.add("ImageMerge", outputStream)
		// .withWidget(BuiltInWidgets.kCameraStream)
		// .withProperties(mapVideo)
		// //.withSize(12, 8)
		// //.withPosition(1, 2)
		// ;
        //
		// Shuffleboard.update();
 		// }
        // //////////////////
 
        while (true)
        {
            try
            {
                // only get these images from Main once because they will wait for FRESH and the
                // second get would be STALE
                Main.obj.elevatorPipeline.getImage(ImageOverlay); // get the primary elevator image
                Main.obj.bumperPipeline.getImage(insert); // get the insert bumper image

                if (ImageOverlay.dims() <= 1)
                {
                    System.out.println(pId + " elevator too few dimensions");
                    insert.copyTo(ImageOutput);
                    Imgproc.putText(ImageOutput, "Bumper Contours Only", new Point(25, 30), Core.FONT_HERSHEY_SIMPLEX,
                            0.5, new Scalar(100, 100, 255), 1);
                }
                else if (insert.dims() <= 1)
                {
                    System.out.println(pId + " bumper too few dimensions");
                    ImageOverlay.copyTo(ImageOutput);
                    Imgproc.putText(ImageOutput, "Elevator Contours Only", new Point(25, 30), Core.FONT_HERSHEY_SIMPLEX,
                            0.5, new Scalar(100, 100, 255), 1);
                }
                else
                {
                    // start with output image the elevator
                    ImageOverlay.copyTo(ImageOutput);

                    // Scaling the insert smaller
                    // Imgproc.resize(insert, insertSmall, new Size(insert.rows() / 3, insert.rows()
                    // / 3), 0, 0, Imgproc.INTER_AREA);
                    Imgproc.resize(insert, insertSmall, new Size(), 0.6, 0.6, Imgproc.INTER_AREA);

                    // locate the small insert on the overlay
                    // This assumes B image is smaller than E image.
                    // If not, then opencv error in log from catch
                    int rowStart = ImageOutput.rows() - insertSmall.rows(); // for top/down put at bottom
                    int rowEnd = rowStart + insertSmall.rows();
                    int colStart = (ImageOutput.cols() - insertSmall.cols()) / 2; // for left/right align centers of the
                                                                                  // two images
                    int colEnd = colStart + insertSmall.cols();

                    try{
                    // define the insert area on the main image
                    // This assumes B image is smaller than E image.
                    // If not, then opencv error in log from catch
                    subMat = ImageOverlay.submat(rowStart, rowEnd, colStart, colEnd);}
                    catch (Exception e)
                    {
                       System.out.println(pId + " B image not smaller than E image " + e);
                       e.printStackTrace();
                    }
                    insertSmall.copyTo(subMat); // copy the insert to the overlay's insert area
                    double alpha = 0.85f;
                    double beta = 0.25f;
                    // merge the original elevator image with the bumper insert overlaid elevator
                    // image (input+output=new output)
                    // alpha+beta = 1 usually; gamma is added to image; = 0 for no gamma adjustment
                    Core.addWeighted(ImageOverlay, alpha, ImageOutput, beta, 0, ImageOutput);

                    Imgproc.putText(ImageOutput, "Merged Images", new Point(25, 30), Core.FONT_HERSHEY_SIMPLEX, 0.5,
                            new Scalar(100, 100, 255), 1);
                }

                outputStream.putFrame(ImageOutput);

            } catch (Exception e)
            {
                System.out.println(pId + " error " + e);
                e.printStackTrace();
            }
        }
    }
}