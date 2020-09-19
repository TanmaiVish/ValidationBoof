package boofcv.metrics.vo;

import boofcv.abst.disparity.StereoDisparitySparse;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.sfm.d3.StereoVisualOdometry;
import boofcv.abst.tracker.PointTrackerTwoPass;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.disparity.ConfigDisparityBM;
import boofcv.factory.disparity.DisparityError;
import boofcv.factory.disparity.FactoryStereoDisparity;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.factory.tracker.FactoryPointTrackerTwoPass;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import georegression.struct.se.Se3_F64;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
* @author Peter Abeles
*/
public class OutputForKITTI {

	public static <T extends ImageGray<T>>
	void computeOdometry( SequenceStereoImages data,
						  StereoVisualOdometry<T> alg ,
						  Class<T> imageType ,
						  PrintStream output ) {

		T inputLeft = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		T inputRight = GeneralizedImageOps.createSingleBand(imageType,1,1);

		alg.setCalibration(data.getCalibration());
		int totalFrames = 0;

		long before = System.currentTimeMillis();

		data.next();
		inputLeft.reshape(data.getLeft().getWidth(),data.getLeft().getHeight());
		inputRight.reshape(data.getRight().getWidth(),data.getRight().getHeight());

		do {
			ConvertBufferedImage.convertFrom(data.getLeft(), inputLeft,true);
			ConvertBufferedImage.convertFrom(data.getRight(), inputRight,true);

			if( !alg.process(inputLeft,inputRight) ) {
				throw new RuntimeException("Updated failed!??!");
			} else {
				Se3_F64 found = alg.getCameraToWorld();

				for( int row = 0; row < 3; row++ ) {
					for( int i = 0; i < 3; i++ )
						output.printf("%15e ", found.getR().get(row, i));
					output.printf("%15e", found.getT().getIdx(row));
					if( row != 2 )
						output.print(" ");
				}
				output.println();
			}
			System.out.println("Processed "+totalFrames++);
		} while( data.next() );
		long after = System.currentTimeMillis();
		System.out.print("Frame Average: "+(after-before)/(totalFrames*1000.0));
	}

	public static void main( String args[] ) throws FileNotFoundException {

		Class imageType = GrayF32.class;
		Class derivType = GrayF32.class;

		for( int dataSet = 0; dataSet < 11; dataSet++ ) {
			ConfigPKlt configKlt = new ConfigPKlt();
			configKlt.pyramidLevels = ConfigDiscreteLevels.levels(4);
			configKlt.templateRadius = 3;

			PointTrackerTwoPass tracker =
					FactoryPointTrackerTwoPass.klt(configKlt, new ConfigGeneralDetector(600, 3, 1),
							imageType, derivType);

			ConfigDisparityBM configDisparity = new ConfigDisparityBM();
			configDisparity.errorType = DisparityError.SAD;
			configDisparity.disparityMin = 10;
			configDisparity.disparityRange = 120;
			configDisparity.maxPerPixelError = 30;
			configDisparity.regionRadiusX = 2;
			configDisparity.regionRadiusY = 2;
			configDisparity.texture = 0.1;
			configDisparity.subpixel = true;

			StereoDisparitySparse<GrayF32> disparity =
					FactoryStereoDisparity.sparseRectifiedBM(configDisparity, imageType);

			StereoVisualOdometry alg = FactoryVisualOdometry.stereoDepth(1.5,120, 2,200,50,false,disparity, tracker,imageType);

			String dataID = String.format("%02d",dataSet);

			SequenceStereoImages data = new WrapParseKITTI("data/KITTI",dataID);
			PrintStream output = new PrintStream(new FileOutputStream(dataID+".txt"));

			computeOdometry(data,alg,imageType,output);

			output.close();
		}
	}
}
