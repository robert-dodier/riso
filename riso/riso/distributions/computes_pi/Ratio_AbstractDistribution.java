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
import java.util.*;
import riso.distributions.*;
import riso.general.*;

/** @see PiHelper
  */
public class Ratio_AbstractDistribution implements PiHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	public static double MIN_DISPERSION_RATIO = 1/50.0;
	public static int NGRID_MINIMUM = 256;
	public static double SUPPORT_EPSILON = 1e-4;

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>Ratio</tt>
	  * followed by two of <tt>AbstractDistribution</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.Ratio", 1 );
		s[1] = new SeqTriple( "riso.distributions.AbstractDistribution", 2 );
		description_array = s;
	}

	public Distribution compute_pi( ConditionalDistribution py_in, Distribution[] pi_messages ) throws Exception
	{
		// Take logs and transform ratio to difference, convolve, and transform back.

		Vector log_distributions = new Vector();
		log_distributions.addElement( new LogDensity( pi_messages[0] ) );
		log_distributions.addElement( new ReflectedDensity( new LogDensity( pi_messages[1] ) ) );
		
		SplineDensity d = (SplineDensity) Sum_AbstractDistribution.convolution( log_distributions );

		double[] expy = new double[1];
		double[] inv_xform_x = new double[d.spline.x.length], inv_xform_f = new double[d.spline.x.length];

		for ( int i = 0; i < d.spline.x.length; i++ )
		{
			expy[0] = Math.exp( d.spline.x[i] );
			inv_xform_x[i] = expy[0];
			inv_xform_f[i] = d.spline.f[i] / expy[0];
		}

		return new SplineDensity( inv_xform_x, inv_xform_f );	// need to recompute spline coefficients
	}
}

class ReflectedDensity extends AbstractDistribution
{
	Distribution d;
	ReflectedDensity( Distribution d ) { this.d = d; }

	public double p( double[] x ) throws Exception
	{
		double[] negx = new double[1];
		negx[0] = -x[0];
		return d.p(negx);
	}

	public double[] effective_support( double epsilon ) throws Exception
	{
		double[] supt = d.effective_support(epsilon);
		double x = supt[0];
		supt[0] = -supt[1];
		supt[1] = -x;
		return supt;
	}

	public double expected_value() throws Exception
	{
		return -d.expected_value();
	}

	public double sqrt_variance() throws Exception
	{
		return d.sqrt_variance();
	}
}
