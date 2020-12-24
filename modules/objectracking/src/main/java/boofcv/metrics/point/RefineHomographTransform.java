package boofcv.metrics.point;

import boofcv.alg.filter.derivative.DerivativeType;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.transform.pyramid.PyramidOps;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import boofcv.struct.pyramid.PyramidDiscrete;
import georegression.struct.homography.Homography2D_F64;
import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.UnconstrainedMinimization;
import org.ddogleg.optimization.UtilOptimize;

/**
 *
 * TODO comment
 *
 * @author Peter Abeles
 */
public class RefineHomographTransform<I extends ImageGray<I>, D extends ImageGray<D>> {

	PyramidDiscrete<I> src;
	D[] srcDericX;
	D[] srcDericY;
	Class<D> derivType;

	PyramidDiscrete<I> dst;


	FitHomographyFunction<I> function;
	FitHomographyGradient<I,D> gradient;

	UnconstrainedMinimization minimizer = FactoryOptimization.quasiNewtonBfgs(null);

	Homography2D_F64 result = new Homography2D_F64();

	public RefineHomographTransform( int levels, Class<I> imageType , Class<D> derivType ) {
		this.derivType = derivType;
		InterpolatePixelS<I> interp = FactoryInterpolation.bilinearPixelS(imageType, BorderType.EXTENDED);

		function = new FitHomographyFunction<>(interp);
		gradient = new FitHomographyGradient<>(interp);

		ImageType<I> type = ImageType.single(imageType);

		ConfigDiscreteLevels configPyr = ConfigDiscreteLevels.levels(levels);

		src = FactoryPyramid.discreteGaussian(configPyr,-1,2,true,type);
		dst = FactoryPyramid.discreteGaussian(configPyr,-1,2,true,type);
	}

	public void setSource( I src ) {

		this.src.process(src);
		srcDericX = PyramidOps.declareOutput(this.src, ImageType.single(derivType));
		srcDericY = PyramidOps.declareOutput(this.src, ImageType.single(derivType));

		for( int i = 0; i < this.src.getNumLayers(); i++ ) {
			D dx = srcDericX[i];
			D dy = srcDericY[i];

			GImageDerivativeOps.gradient(DerivativeType.SOBEL,this.src.getLayer(i), dx, dy, BorderType.EXTENDED);
		}
	}

	public boolean process( I dst, Homography2D_F64 H_init ) {

		this.dst.process(dst);

		Homography2D_F64 H = H_init.copy();

		for( int layer = src.getNumLayers()-1; layer >= 0 ; layer-- ) {
			// adjust the transform for the layer
			double scale = src.getScale(layer);
			Homography2D_F64 H_scale = new Homography2D_F64(scale,0,0,0,scale,0,0,0,1);
			Homography2D_F64 H_scale_inv = H_scale.invert(null);

			Homography2D_F64 H_layer = H_scale.concat(H,null).concat(H_scale_inv,null);

			H_layer = processLayer( src.getLayer(layer),this.dst.getLayer(layer),
					srcDericX[layer],srcDericY[layer],H_layer);

			// convert the scale back up into the original image scale
			H_scale_inv.concat(H_layer,null).concat(H_scale,H);
		}

		result.setTo(H);

		return true;
	}

	private Homography2D_F64 processLayer( I src, I dst , D srcDerivX , D srcDerivY,  Homography2D_F64 H_init ) {
		function.setInputs(src,dst);
		gradient.setInputs(src,dst,srcDerivX,srcDerivY);

		minimizer.setFunction(function,gradient,0);
//		minimizer.setFunction(function,new NumericalGradientForward(function,0.1));

		double param[] = new double[]{
				H_init.a11,H_init.a12,H_init.a13,
				H_init.a21,H_init.a22,H_init.a23,
				H_init.a31,H_init.a32,H_init.a33};
		minimizer.initialize(param,1e-20,1e-20);

//		System.out.println("initial error "+minimizer.getFunctionValue());
		UtilOptimize.process(minimizer, 1000);
//		System.out.println("after error "+minimizer.getFunctionValue());

		Homography2D_F64 result = new Homography2D_F64();

		double found[] = minimizer.getParameters();
		result.a11 = found[0];
		result.a12 = found[1];
		result.a13 = found[2];
		result.a21 = found[3];
		result.a22 = found[4];
		result.a23 = found[5];
		result.a31 = found[6];
		result.a32 = found[7];
		result.a33 = found[8];

		return result;
	}


	public Homography2D_F64 getRefinement() {
		return result;
	}
}
