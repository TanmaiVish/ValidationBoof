package boofcv.regression;

import boofcv.common.*;
import boofcv.metrics.DetectLinesSaveToFile;
import boofcv.metrics.EvaluateHoughLineDetector;
import boofcv.metrics.FactoryLineDetector;
import boofcv.struct.image.ImageDataType;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * @author Peter Abeles
 */
public class DetectLineRegression extends BaseRegression implements ImageRegression {

	File workDirectory = new File("./tmp");
	File baseDirectory = new File("data/shape/line/");

	RuntimeSummary runtime;

	int blurRadius = 3;

	public DetectLineRegression() {
		super(BoofRegressionConstants.TYPE_SHAPE);
	}

	@Override
	public void process(ImageDataType type) throws IOException {
		final Class imageType = ImageDataType.typeToSingleClass(type);

		processFamily(imageType, "Thin", true,FactoryLineDetector.THIN);
		processFamily(imageType, "Edge", false,FactoryLineDetector.EDGE);
	}

	private void processFamily(Class imageType, String typeName, boolean thin, String[] algs ) throws IOException {
		runtime = new RuntimeSummary();
		runtime.initializeLog(directoryRuntime,getClass(),"RUN_"+typeName+"LineDetector.txt");

		runtime.printUnitsRow(true);
		for( String detectorName : algs ) {
			process(detectorName,thin,typeName,imageType);
		}
		runtime.out.close();
	}

	private void process(String name , boolean thin , String typeName, Class imageType) {

		String outputAccuracyName = "ACC_"+typeName+"LineDetector_"+name+".txt";

		EvaluateHoughLineDetector evaluator = new EvaluateHoughLineDetector();

		try (PrintStream outputAccuracy = new PrintStream(new File(directoryMetrics, outputAccuracyName))) {
			BoofRegressionConstants.printGenerator(outputAccuracy, getClass());
			outputAccuracy.println("# blur radius "+blurRadius);
			evaluator.setOutputResults(outputAccuracy);

			DetectLinesSaveToFile detection = new DetectLinesSaveToFile(thin, name, blurRadius, imageType);
			File f = new File(baseDirectory, typeName.toLowerCase());
			detection.processDirectory(f, workDirectory);
			evaluator.evaluate(f, workDirectory);
			outputAccuracy.println();

			runtime.printStatsRow(name,detection.processingTimeMS);

		} catch ( Exception e) {
			System.out.println("Failed! " + outputAccuracyName);
			errorLog.println(e);
		}
	}

	public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
//		DetectLineRegression app = new DetectLineRegression();
//		app.setOutputDirectory(".");
//		app.process(ImageDataType.F32);
		BoofRegressionConstants.clearCurrentResults();
		RegressionRunner.main(new String[]{DetectLineRegression.class.getName(),ImageDataType.F32.toString()});
	}
}
