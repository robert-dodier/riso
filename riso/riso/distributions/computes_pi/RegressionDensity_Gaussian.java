package riso.distributions.computes_pi;
import java.rmi.*;
import riso.distributions.*;

/** @see PiHelper
  */
public class RegressionDensity_Gaussian implements PiHelper
{
	public Distribution compute_pi( ConditionalDistribution y_in, Distribution[] pi ) throws Exception
	{
		int i, j, k;

		for ( i = 0; i < pi.length; i++ )
			if ( pi[i].ndimensions() != 1 )
				throw new IllegalArgumentException( "computes_pi.RegressionDensity_Gaussian.compute_pi: pi-message "+i+" has "+pi[i].ndimensions()+" dimensions (should have 1)"+"\n" );

		RegressionDensity y = (RegressionDensity) y_in;

		if ( y.ndimensions_child() != 1 )
			throw new IllegalArgumentException( "computes_pi.RegressionDensity_Gaussian.compute_pi: this node has "+y.ndimensions_child()+" dimensions (should have 1)"+"\n" );

		return one_gaussian_pi_approx( pi, y );
	}

	public static Gaussian one_gaussian_pi_approx( Distribution[] pi, RegressionDensity y ) throws RemoteException
	{
		int i;

		double[] Ex = new double[ pi.length ];
		for ( i = 0; i < pi.length; i++ )
			Ex[i] = pi[i].expected_value();

		double[] gradF = y.regression_model.dFdx(Ex)[0];

		double[] sigma2_x = new double[ pi.length ];
		for ( i = 0; i < pi.length; i++ )
		{
			Gaussian g = (Gaussian) pi[i];
			double s = g.sqrt_variance();
			sigma2_x[i] = s*s;
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
