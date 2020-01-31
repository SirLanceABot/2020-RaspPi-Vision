import org.opencv.core.Mat;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Point;
import org.opencv.core.Size;

// this was originally class HoughCircles
public class DetectPowerCell
{
    public void onCameraFrame(Mat input) 
    {
        desaturate(input, input);
        Mat circles = new Mat();
        Imgproc.blur(input, input, new Size(7, 7), new Point(2, 2));
        Imgproc.HoughCircles(input, circles, Imgproc.CV_HOUGH_GRADIENT, 2, 100, 100, 90, 0, 1000);

        System.out.println(String.valueOf("size: " + circles.cols()) + ", " + String.valueOf(circles.rows()));

        if (circles.cols() > 0) 
        {
            for (int x=0; x < Math.min(circles.cols(), 5); x++ ) 
            {
                double circleVec[] = circles.get(0, x);

                if (circleVec == null) 
                {
                    break;
                }

                Point center = new Point((int) circleVec[0], (int) circleVec[1]);
                int radius = (int) circleVec[2];

                Imgproc.circle(input, center, 3, new Scalar(255, 255, 255), 5);
                Imgproc.circle(input, center, radius, new Scalar(255, 255, 255), 2);
            }
        }

        circles.release();
        input.release();
        
    }

    /**
	 * Converts a color image into shades of grey.
	 * @param input The image on which to perform the desaturate.
	 * @param output The image in which to store the output.
	 */
	private void desaturate(Mat input, Mat output) {
		switch (input.channels()) {
			case 1:
				// If the input is already one channel, it's already desaturated
				input.copyTo(output);
				break;
			case 3:
				Imgproc.cvtColor(input, output, Imgproc.COLOR_BGR2GRAY);
				break;
			case 4:
				Imgproc.cvtColor(input, output, Imgproc.COLOR_BGRA2GRAY);
				break;
			default:
				throw new IllegalArgumentException("Input to desaturate must have 1, 3, or 4 channels");
		}
	}
}