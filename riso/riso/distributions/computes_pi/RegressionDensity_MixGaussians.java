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
import java.util.*;
import riso.distributions.*;
import riso.general.*;

/** @see PiHelper
  */
public class RegressionDensity_MixGaussians implements PiHelper
{
	/** 3-component Gaussian mixture approximation to unit gaussian.
	  */
	public static MixGaussians unit_3mix = new MixGaussians( 1, 3 );

	/** 5-component Gaussian mixture approximation to unit gaussian.
	  */
	public static MixGaussians unit_5mix = new MixGaussians( 1, 5 );

	static 
	{
		// From unit-gaussian-3-approx.mix:
		// CROSS ENTROPY[9]: 1.419791403766266; target entropy: 1.4189302861251774
		// effective support of target: -3.90625, 3.90625
		// effective support of approx: -3.876185771377721, 3.8761857713777204
		// equivalent sample size: 10,000

		unit_3mix.mix_proportions[0] = 0.288272103409951;
		unit_3mix.mix_proportions[1] =  0.4234557931800979;
		unit_3mix.mix_proportions[2] =  0.288272103409951;

		unit_3mix.components[0] = new Gaussian( -0.9418690940140162, 0.7511850694051084 );
		unit_3mix.components[1] = new Gaussian( 4.672385040934143E-17, 0.6167460137184869 );
		unit_3mix.components[2] = new Gaussian(  0.9418690940140164, 0.7511850694051082 );

		// From unit-gaussian-5-approx.mix:
		// % CROSS ENTROPY[9]: 1.4227573945718415; target entropy: 1.4189302861251774
		// % effective support of unit gaussian: -3.90625, 3.90625
		// % effective support of approx: -6.216647215550652, 6.216647215550651
		// % equivalent sample size: 10,000

		unit_5mix.mix_proportions[0] = 0.040139771625403255;
		unit_5mix.mix_proportions[1] = 0.21020240121518463;
		unit_5mix.mix_proportions[2] = 0.4993156543188242;
		unit_5mix.mix_proportions[3] = 0.2102024012151847;
		unit_5mix.mix_proportions[4] = 0.04013977162540325;

		unit_5mix.components[0] = new Gaussian( -1.2190024231657415, 1.3906489857071056 );
		unit_5mix.components[1] = new Gaussian( -0.9582429381183698, 0.6781199005346445 );
		unit_5mix.components[2] = new Gaussian( 2.6428011839965636E-17, 0.6691588966728722 );
		unit_5mix.components[3] = new Gaussian( 0.9582429381183695, 0.6781199005346445 );
		unit_5mix.components[4] = new Gaussian( 1.2190024231657413, 1.3906489857071054 );
	}

    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>RegressionDensity</tt>
	  * followed by any number of <tt>MixGaussians</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.RegressionDensity", 1 );
		s[1] = new SeqTriple( "riso.distributions.MixGaussians", -1 );
		description_array = s;
	}

	/** Some pi messages might be <tt>Gaussian</tt> -- promote them to <tt>MixGaussian</tt>
	  * by replacing each bump with a several appropriately weighted bumps (as returned
	  * by <tt>gaussian_to_mix</tt>).
	  * All other pi messages must be <tt>MixGaussian</tt>. Call <tt>compute_pi0</tt> to carry
	  * out the real work.
	  */
	public Distribution compute_pi( ConditionalDistribution py_in, Distribution[] pi_messages ) throws Exception
	{
		Distribution[] pi_msgs_w_promotion = new Distribution[ pi_messages.length ];

		for ( int i = 0; i < pi_messages.length; i++ )
			if ( pi_messages[i] instanceof Gaussian )
				pi_msgs_w_promotion[i] = gaussian_to_mix( (Gaussian) pi_messages[i] );
			else
			{
				MixGaussians pi_mix = (MixGaussians) pi_messages[i];
				MixGaussians split_mix = new MixGaussians( 1, pi_mix.ncomponents() );

				for ( int j = 0; j < pi_mix.ncomponents(); j++ )
				{
					split_mix.mix_proportions[j] = pi_mix.mix_proportions[j];
					split_mix.components[j] = gaussian_to_mix( (Gaussian) pi_mix.components[j] );
				}
				
				pi_msgs_w_promotion[i] = Mixture.flatten( split_mix );
			}

		return compute_pi0( py_in, pi_msgs_w_promotion );
	}

	/** Assume all pi messages are <tt>MixGaussians</tt>.
	  */
	public Distribution compute_pi0( ConditionalDistribution py_in, Distribution[] pi_messages ) throws Exception
	{
		int i;

		RegressionDensity py = (RegressionDensity) py_in;

		int ncomponents = 1;
		for ( i = 0; i < pi_messages.length; i++ )
			ncomponents *= ((MixGaussians)pi_messages[i]).ncomponents();
		MixGaussians mix = new MixGaussians( 1, ncomponents );

		int[] k = new int[ pi_messages.length ], l = new int[1];
	
		mix_gaussians_pi_approx_inner_loop( mix, pi_messages, py, k, l, pi_messages.length-1, null );

		// Here's an easy step toward simplification: throw out a component if it
		// appears to be nearly the same as some other component.

		Vector duplicates = new Vector(), duplicated = new Vector();

		for ( i = 0; i < mix.ncomponents(); i++ )
		{
			double m_i = mix.components[i].expected_value();
			double s_i = mix.components[i].sqrt_variance();

			for ( int j = i+1; j < mix.ncomponents(); j++ )
			{
				double m_j = mix.components[j].expected_value();
				double s_j = mix.components[j].sqrt_variance();

				if ( s_i == 0 || s_j == 0 ) continue;

				double dm = Math.abs(m_i-m_j), rs = s_i/s_j, s_ij = Math.sqrt( 1/( 1/(s_i*s_i) + 1/(s_j*s_j) ) );

				if ( dm/s_ij < 7.5e-2 && rs > 1 - 6e-2 && rs < 1 + 6e-2 )
				{
					duplicates.addElement( new Integer(i) );
					duplicated.addElement( new Integer(j) );
					break; // go on to next i
				}
			}
		}

System.err.println( "Reg_MixG.comptes_pi: REMOVE "+duplicates.size()+" duplicate components (out of "+mix.ncomponents()+" in all)" );
		mix.remove_components( duplicates, duplicated );

		if ( mix.ncomponents() == 1 ) return mix.components[0];
		else return mix;
	}

	public static void mix_gaussians_pi_approx_inner_loop( MixGaussians mix, Distribution[] pi_messages, RegressionDensity py, int[] k, int[] l, int m, Distribution[] pi_combo ) throws Exception
	{
		if ( pi_combo == null ) pi_combo = new Distribution[ pi_messages.length ];

		if ( m == -1 )
		{
			double mix_proportion = 1;
			for ( int i = 0; i < pi_messages.length; i++ )
			{
				mix_proportion *= ((MixGaussians)pi_messages[i]).mix_proportions[ k[i] ];
				pi_combo[i] = ((MixGaussians)pi_messages[i]).components[ k[i] ];
			}

			mix.mix_proportions[ l[0] ] = mix_proportion;
			mix.components[ l[0] ] = one_gaussian_pi_approx( pi_combo, py );
			++l[0];
		}
		else
		{
			for ( int i = 0; i < ((MixGaussians)pi_messages[m]).ncomponents(); i++ )
			{
				k[m] = i;
				mix_gaussians_pi_approx_inner_loop( mix, pi_messages, py, k, l, m-1, pi_combo );
			}
		}
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

		MixGaussians mix = new MixGaussians( 1, unit_5mix.ncomponents() );
		for ( int i = 0; i < unit_5mix.ncomponents(); i++ )
		{
			double m0 = unit_5mix.components[i].expected_value(), s0 = unit_5mix.components[i].sqrt_variance();
			mix.components[i] = new Gaussian( m+s*m0, s*s0 );
			mix.mix_proportions[i] = unit_5mix.mix_proportions[i];
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
