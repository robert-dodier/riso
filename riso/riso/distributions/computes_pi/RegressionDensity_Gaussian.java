/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 1999, Robert Dodier.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA, 02111-1307, USA,
 * or visit the GNU web site, www.gnu.org.
 */
package riso.distributions.computes_pi;
import java.rmi.*;
import riso.distributions.*;
import SeqTriple;

/** @see PiHelper
  */
public class RegressionDensity_Gaussian implements PiHelper
{
	/** Gaussian mixture approximation to unit gaussian.
	  */
	public static MixGaussians unit_mix = new MixGaussians( 1, 3 );

	static 
	{
		// From unit-gaussian-3-approx.mix:
		// CROSS ENTROPY[9]: 1.419791403766266; target entropy: 1.4189302861251774
		// effective support of target: -3.90625, 3.90625
		// effective support of approx: -3.876185771377721, 3.8761857713777204
		// equivalent sample size: 10,000

		unit_mix.mix_proportions[0] = 0.288272103409951;
		unit_mix.mix_proportions[1] =  0.4234557931800979;
		unit_mix.mix_proportions[2] =  0.288272103409951;

		unit_mix.components[0] = new Gaussian( -0.9418690940140162, 0.7511850694051084 );
		unit_mix.components[1] = new Gaussian( 4.672385040934143E-17, 0.6167460137184869 );
		unit_mix.components[2] = new Gaussian(  0.9418690940140164, 0.7511850694051082 );
	}

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>RegressionDensity</tt>
	  * followed by any number of <tt>Gaussian</tt>.
	  */
	public static SeqTriple[] description()
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.RegressionDensity", 1 );
		s[1] = new SeqTriple( "riso.distributions.Gaussian", -1 );
		return s;
	}

	public Distribution compute_pi( ConditionalDistribution py, Distribution[] pi_msgs ) throws Exception
	{
		MixGaussians[] mix_msgs = new MixGaussians[ pi_msgs.length ];
		
		for ( int i = 0; i < pi_msgs.length; i++ )
			mix_msgs[i] = gaussian_to_mix( (Gaussian) pi_msgs[i] );

		return (new RegressionDensity_MixGaussians()).compute_pi0( py, mix_msgs );
	}

	public static MixGaussians gaussian_to_mix( Gaussian g ) throws Exception
	{
		double m = g.expected_value(), s = g.sqrt_variance();

		if ( s == 0 ) // it's a delta
		{
			MixGaussians mix1 = new MixGaussians(1,1);
			mix1.mix_proportions[0] = 1;
			mix1.components[0] = g;
			return mix1;
		}

		MixGaussians mix = new MixGaussians( 1, unit_mix.ncomponents() );
		for ( int i = 0; i < unit_mix.ncomponents(); i++ )
		{
			double m0 = unit_mix.components[i].expected_value(), s0 = unit_mix.components[i].sqrt_variance();
			mix.components[i] = new Gaussian( m+s*m0, s*s0 );
			mix.mix_proportions[i] = unit_mix.mix_proportions[i];
		}

		return mix;
	}

	public static Gaussian one_gaussian_pi_approx( Distribution[] pi, RegressionDensity y ) throws Exception
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

		double s = y.noise_model.sqrt_variance();
		double sigma2_y = s*s;
		for ( i = 0; i < pi.length; i++ )
			sigma2_y += sigma2_x[i] * gradF[i]*gradF[i];

		double[] mu_y = y.regression_model.F(Ex);
		double[][] Sigma_y = new double[1][1];
		Sigma_y[0][0] = sigma2_y;

		return new Gaussian( mu_y, Sigma_y );
	}

}
