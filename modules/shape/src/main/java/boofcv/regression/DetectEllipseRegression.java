package boofcv.regression;

import boofcv.alg.shapes.ellipse.BinaryEllipseDetector;
import boofcv.common.*;
import boofcv.metrics.DetectEllipseSaveToFile;
import boofcv.metrics.EvaluateEllipseDetector;
import boofcv.metrics.FactoryBinaryEllipse;
import boofcv.struct.image.ImageDataType;
import org.ddogleg.struct.DogArray_F64;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class DetectEllipseRegression extends BaseRegression implements ImageRegression {

	File workDirectory = new File("./tmp");
	File baseDataSetDirectory = new File("data/shape/ellipse");

	RuntimeSummary runtime;
	DogArray_F64 summaryRuntime = new DogArray_F64();

	public DetectEllipseRegression() {
		super(BoofRegressionConstants.TYPE_SHAPE);
	}

	@Override
	public void process(ImageDataType type) throws IOException {
		final Class imageType = ImageDataType.typeToSingleClass(type);

		runtime = new RuntimeSummary();
		runtime.initializeLog(directoryRuntime, getClass(),"RUN_EllipseDetector.txt");

		process("Global", false, new FactoryBinaryEllipse(true,imageType));
		process("Local", true, new FactoryBinaryEllipse(true,imageType));
		process("LocalPixel", true, new FactoryBinaryEllipse(false,imageType));

		runtime.printSummaryResults();
		runtime.out.close();
	}

	private void process(String name, boolean localBinary , FactoryObject<BinaryEllipseDetector> factory)
			throws IOException {

		String outputName = "ACC_EllipseDetector_"+name+".txt";

		EvaluateEllipseDetector evaluator = new EvaluateEllipseDetector();

		PrintStream outputAccuracy = new PrintStream(new File(directoryMetrics,outputName));
		BoofRegressionConstants.printGenerator(outputAccuracy, getClass());
		evaluator.setOutputResults(outputAccuracy);

		List<File> files = BoofRegressionConstants.listAndSort(baseDataSetDirectory);

		summaryRuntime.reset();
		runtime.out.println(name);
		runtime.printUnitsRow(false);

		for( File f : files  ) {
			if( !f.isDirectory() )
				continue;
			outputAccuracy.println("# Data Set = "+f.getName());

			factory.configure(new File(f,"detector.txt"));

			DetectEllipseSaveToFile detection = new DetectEllipseSaveToFile(factory.newInstance(),localBinary);

			detection.processDirectory(f, workDirectory);
			evaluator.evaluate(f, workDirectory);
			outputAccuracy.println();
			runtime.printStatsRow(f.getName(),detection.processingTimeMS);
			summaryRuntime.addAll(detection.processingTimeMS);
		}
		runtime.out.println();
		runtime.saveSummary(name,summaryRuntime);

		outputAccuracy.close();
	}

	public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		BoofRegressionConstants.clearCurrentResults();
		RegressionRunner.main(new String[]{DetectEllipseRegression.class.getName(),ImageDataType.F32.toString()});
	}
}
