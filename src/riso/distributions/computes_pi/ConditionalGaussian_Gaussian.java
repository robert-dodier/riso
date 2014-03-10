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
import riso.numerical.*;
import riso.general.*;

/** @see PiHelper
  */
public class ConditionalGaussian_Gaussian implements PiHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>ConditionalGaussian</tt>
	  * followed by any number of <tt>Gaussian</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.ConditionalGaussian", 1 );
		s[1] = new SeqTriple( "riso.distributions.Gaussian", -1 );
		description_array = s;
	}

	public Distribution compute_pi( ConditionalDistribution y, Distribution[] pi ) throws Exception
	{
		if ( y.ndimensions_child() > 1 ) throw new Exception( "computes_pi.ConditionalGaussian_Gaussian: can't handle "+y.ndimensions_child()+" dimensions." );

		return one_gaussian_pi( pi, (ConditionalGaussian) y );
	}

	public static Gaussian one_gaussian_pi( Distribution[] pi, ConditionalGaussian y ) throws Exception
	{
		y.check_matrices();

		double[] mu_x = new double[pi.length];
		for ( int i = 0; i < mu_x.length; i++ )
			mu_x[i] = pi[i].expected_value();

		double[] mu_y = (double[]) y.b_mu_1c2.clone();
		Matrix.add( mu_y, Matrix.multiply( y.a_mu_1c2, mu_x ) );

		double sigma2_y = y.Sigma_1c2[0][0];
		for ( int i = 0; i < pi.length; i++ )
		{
			double as = y.a_mu_1c2[0][i] * pi[i].sqrt_variance();
			sigma2_y += as*as;
		}

		return new Gaussian( mu_y[0], Math.sqrt(sigma2_y) );
	}
}
