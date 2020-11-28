package boofcv.metrics;

import boofcv.abst.filter.binary.BinaryLabelContourFinder;
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.abst.shapes.polyline.PointsToPolyline;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.FactoryBinaryContourFinder;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.DogArray_I32;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Detects polygons inside an image and saves the results to a file
 *
 * @author Peter Abeles
 */
public class DetectPolylineSaveToFile<T extends ImageGray<T>> {

	BinaryLabelContourFinder binaryToContour = FactoryBinaryContourFinder.linearChang2004();
	PointsToPolyline contourToPolyline;
	InputToBinary<T> inputToBinary;

	T gray;
	GrayU8 binary = new GrayU8(1,1);
	GrayS32 labeled = new GrayS32(1,1);

	public final DogArray_F64 processingTimeMS = new DogArray_F64();

	public DetectPolylineSaveToFile(PointsToPolyline contourToPolyline , boolean binaryLocal, Class<T> imageType ) {

		this.contourToPolyline = contourToPolyline;

		ConfigThreshold config;
		if( binaryLocal ) {
			config = ConfigThreshold.local(ThresholdType.LOCAL_MEAN,41);
		} else {
			config = ConfigThreshold.global(ThresholdType.GLOBAL_OTSU);
		}
		inputToBinary = FactoryThresholdBinary.threshold(config,imageType);
		gray = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
	}

	public void processDirectory( File inputDir , File outputDir ) {

		if( !outputDir.exists() )
			outputDir.mkdirs();

		List<File> files = Arrays.asList(inputDir.listFiles());
		Collections.sort(files);

		processingTimeMS.reset();
		for( File f : files ) {
			if( !f.isFile() || f.getName().endsWith("txt"))
				continue;

			BufferedImage buffered = UtilImageIO.loadImage(f.getPath());
			if( buffered == null )
				continue;

			String name = UtilShapeDetector.imageToDetectedName(f.getName());
			File outputFile = new File(outputDir,name);
			process(buffered,outputFile);
		}
	}

	private void process( BufferedImage buffered , File outputFile ) {

		gray.reshape(buffered.getWidth(),buffered.getHeight());
		binary.reshape(buffered.getWidth(), buffered.getHeight());

		ConvertBufferedImage.convertFrom(buffered, gray, true);

		inputToBinary.process(gray, binary);
		binaryToContour.process(binary,labeled);

		List<Contour> contours = BinaryImageOps.convertContours(binaryToContour);

		long startNano = System.nanoTime();
		DogArray_I32 vertexes = new DogArray_I32();
		List<List<Point2D_I32>> polylines = new ArrayList<>();
		for (int i = 0; i < contours.size(); i++) {
			Contour c = contours.get(i);
			if( c.external.size() > 30 ) {
				vertexes.reset();
				if( contourToPolyline.process(c.external,vertexes)) {

					List<Point2D_I32> found = new ArrayList<>();
					for (int j = 0; j < vertexes.size; j++) {
						found.add( c.external.get(vertexes.get(j)));
					}
					polylines.add(found);
				}
			}
		}
		long stopNano = System.nanoTime();
		UtilShapeDetector.saveResultsPolyline(polylines,outputFile);

		processingTimeMS.add((stopNano-startNano)/1e6);
	}

}
