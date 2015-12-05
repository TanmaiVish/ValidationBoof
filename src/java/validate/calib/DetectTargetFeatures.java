package validate.calib;

import boofcv.abst.fiducial.calib.ConfigChessboard;
import boofcv.abst.geo.calibration.CalibrationDetector;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.factory.calib.FactoryCalibrationTarget;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageFloat32;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

/**
 * Detects calibration points in an image and saves the results to a file.
 *
 * @author Peter Abeles
 */
public class DetectTargetFeatures {

	public static void main( String args[] ) throws FileNotFoundException {
		// detects the calibration target points
		CalibrationDetector detector = FactoryCalibrationTarget.detectorChessboard(new ConfigChessboard(5, 7, 30));

		// load image list
		String directory = "data/calib/stereo/Bumblebee2_Chess";
		List<String> images = BoofMiscOps.directoryList(directory, "jpg");

		Collections.sort(images);

		PrintStream out = new PrintStream(new FileOutputStream("calib_pts.txt"));

		// process and saves results
		for( String name : images ) {
			BufferedImage orig = UtilImageIO.loadImage(name);
			ImageFloat32 input = new ImageFloat32(orig.getWidth(),orig.getHeight());
			ConvertBufferedImage.convertFrom(orig,input);

			if( detector.process(input) ) {
				System.out.println("Found! "+name);

				CalibrationObservation points = detector.getDetectedPoints();

				out.printf("%s %d ",new File(name).getName(),points.size());
				for( CalibrationObservation.Point p : points.points ) {
					out.printf("%f %f ",p.pixel.x,p.pixel.y);
				}
				out.println();

			} else {
				System.out.println("Failed: "+name);
			}
		}
	}
}
