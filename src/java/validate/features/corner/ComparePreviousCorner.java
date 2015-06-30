package validate.features.corner;

import boofcv.abst.filter.derivative.AnyImageDerivative;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.core.image.inst.FactoryImageGenerator;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I16;
import validate.features.corner.GenerateCornerFeatureFiles.AlgInfo;
import validate.misc.PointFileCodec;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static validate.features.corner.GenerateCornerFeatureFiles.createAlgorithms;

/**
 * Detect corner features in the sample image again and see if things have changed
 *
 * @author Peter Abeles
 */
public class ComparePreviousCorner {
	List<ImageDataType> imageTypes = new ArrayList<ImageDataType>();

	PrintStream out;

	public ComparePreviousCorner( PrintStream out ) {

		this.out = out;

		imageTypes.add( ImageDataType.U8);
		imageTypes.add( ImageDataType.F32);
	}

	public void generateAll() {
		for( ImageDataType type : imageTypes ) {
			generateAll( ImageDataType.typeToSingleClass(type));
		}
	}

	public void generateAll( Class imageType ) {

		Class derivType = GImageDerivativeOps.getDerivativeType(imageType);

		List<AlgInfo> detectors = createAlgorithms(imageType,derivType);

		AnyImageDerivative anyDeriv = GImageDerivativeOps.createDerivatives(
				imageType, FactoryImageGenerator.create(derivType));

		for (int i = 0; i < detectors.size(); i++) {
			ImageSingleBand input = UtilImageIO.loadImage(GenerateCornerFeatureFiles.ImagePath, imageType);
			anyDeriv.setInput(input);
			ImageSingleBand derivX = anyDeriv.getDerivative(true);
			ImageSingleBand derivY = anyDeriv.getDerivative(false);
			ImageSingleBand derivXX = anyDeriv.getDerivative(true, true);
			ImageSingleBand derivYY = anyDeriv.getDerivative(false, false);
			ImageSingleBand derivXY = anyDeriv.getDerivative(true, false);

			AlgInfo info = detectors.get(i);
			info.detector.process(input, derivX, derivY, derivXX, derivYY, derivXY);

			List<Point2D_F64> found = computeCorners(info);
			String fileName = info.name+"_"+(ImageDataType.classToType(imageType))+".txt";
			List<Point2D_F64> expected = PointFileCodec.load(GenerateCornerFeatureFiles.outputDir + "/" + fileName);

			compareLists(fileName,found,expected);
		}
	}

	private void compareLists( String name , List<Point2D_F64> found , List<Point2D_F64> expected ) {
		int change = found.size()-expected.size();

		double residual = 0;
		for (int i = 0; i < found.size(); i++) {
			Point2D_F64 f = found.get(i);
			double best = Double.MAX_VALUE;
			for (int j = 0; j < expected.size(); j++) {
				Point2D_F64 p = expected.get(j);
				double d = p.distance2(f);
				if( d < best ) {
					best = d;
				}
			}
			residual += best;
		}
		out.println(name + " " + change + " "+residual);
	}


	private List<Point2D_F64> computeCorners(AlgInfo info) {
		List<Point2D_F64> points = new ArrayList<Point2D_F64>();
		if( info.detector.isDetectMaximums()) {
			QueueCorner corners = info.detector.getMaximums();
			for (int j = 0; j < corners.size; j++) {
				Point2D_I16 c = corners.get(j);
				points.add( new Point2D_F64(c.x,c.y));
			}
		}
		if( info.detector.isDetectMinimums() ) {
			QueueCorner corners = info.detector.getMinimums();
			for (int j = 0; j < corners.size; j++) {
				Point2D_I16 c = corners.get(j);
				points.add( new Point2D_F64(c.x,c.y));
			}
		}
		return points;
	}

	public static void main(String[] args) {
		ComparePreviousCorner alg = new ComparePreviousCorner(System.out);

		alg.generateAll();
	}
}