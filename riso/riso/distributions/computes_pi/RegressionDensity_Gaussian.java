package riso.distributions.computes_pi;
import riso.distributions.*;

/** @see PiHelper
  */
public class RegressionDensity_Gaussian implements PiHelper
{
	public Distribution compute_pi( ConditionalDistribution y_in, Distribution[] pi ) throws Exception
	{
		int i, j, k;

		for ( i = 0; i < pi.length; i++ )
			if ( ! (pi[i] instanceof Gaussian) )
				throw new IllegalArgumentException( "computes_pi.RegressionDensity_Gaussian.compute_pi: pi-message "+i+" is not Gaussian, but rather "+pi[i].getClass().getName()+"\n" );

		for ( i = 0; i < pi.length; i++ )
			if ( pi[i].ndimensions() != 1 )
				throw new IllegalArgumentException( "computes_pi.RegressionDensity_Gaussian.compute_pi: pi-message "+i+" has "+pi[i].ndimensions()+" dimensions (should have 1)"+"\n" );

		RegressionDensity y = (RegressionDensity) y_in;

		if ( y.ndimensions_child() != 1 )
			throw new IllegalArgumentException( "computes_pi.RegressionDensity_Gaussian.compute_pi: this node has "+y.ndimensions_child()+" dimensions (should have 1)"+"\n" );

		double[] Ex = new double[ pi.length ];
		for ( i = 0; i < pi.length; i++ )
			Ex[i] = ((Gaussian)pi[i]).mu[0];

		double[] gradF = y.regression_model.dFdx(Ex)[0];

		double[] sigma2_x = new double[ pi.length ];
		for ( i = 0; i < pi.length; i++ )
		{
			Gaussian g = (Gaussian) pi[i];
			double[][] s = g.get_Sigma();
			sigma2_x[i] = s[0][0];
		}

		double sigma2_y = 0;
		for ( i = 0; i < pi.length; i++ )
			sigma2_y += sigma2_x[i] * gradF[i]*gradF[i];

		double[] mu_y = y.regression_model.F(Ex);
		double[][] Sigma_y = new double[1][1];
		Sigma_y[0][0] = sigma2_y;

		return new Gaussian( mu_y, Sigma_y );
	}

}
