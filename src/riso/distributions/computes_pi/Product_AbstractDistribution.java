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
public class Product_AbstractDistribution implements PiHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>Product</tt>
	  * followed by any number of <tt>AbstractDistribution</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.Product", 1 );
		s[1] = new SeqTriple( "riso.distributions.AbstractDistribution", -1 );
		description_array = s;
	}

	/** This code will try to find a few special cases, and otherwise it compute a simple
	  * approximation.
	  * <ol>
	  * <li> If some pi messages are <tt>GaussianDelta</tt> and the rest are <tt>Lognormal</tt>,
	  *     the result can be computed exactly.
	  * <li> If some pi messages are <tt>Lognormal</tt> and some are <tt>GaussianDelta</tt> and
	  *     the rest are something else, compute an exact result using the <tt>Lognormal</tt>'s and
	  *     <tt>GaussianDelta</tt>'s, and combine that with the remaining messages to obtain
	  *     an approximate result.
	  * <li> If all pi messages have support contained in (0,+infinity), take logarithms to transform
	  *     the product into a sum, then convolve the resulting transformed distributions, and finally
	  *     transform back to a product.
	  * <li> Otherwise, compute the differential of the function <tt>x1 x2 ... xn</tt>, and make a 
	  *     simple Gaussian approximation.
	  * </ol>
	  */
	public Distribution compute_pi( ConditionalDistribution py_in, Distribution[] pi_messages ) throws Exception
	{
		int ndelta = 0, nlognormal = 0;
		Vector others = new Vector();
		double delta_product = 1, mu_sum = 0, sigma2_sum = 0;

		for ( int i = 0; i < pi_messages.length; i++ )
		{
			if ( pi_messages[i] instanceof GaussianDelta )
			{
				++ndelta;
				delta_product *= ((GaussianDelta)pi_messages[i]).expected_value();
			}
			else if ( pi_messages[i] instanceof Lognormal )
			{
				++nlognormal;
				mu_sum += ((Lognormal)pi_messages[i]).mu;
				sigma2_sum += sqr( ((Lognormal)pi_messages[i]).sigma );
			}
			else
				others.addElement( pi_messages[i] );
		}

		if ( others.size() == 0 )
		{
			// All pi messages are GaussianDelta or Lognormal. Compute exact result.
			if ( nlognormal == 0 ) return new GaussianDelta( delta_product );
			return new Lognormal( mu_sum+Math.log(delta_product), sigma2_sum );
		}
		else if ( others.size() == 1 && nlognormal == 0 && others.elementAt(0) instanceof Gaussian )
		{
			// Exact result: one Gaussian and some number of deltas, so rescale Gaussian.
			Gaussian g = (Gaussian) others.elementAt(0);
			return new Gaussian( delta_product*g.expected_value(), delta_product*g.sqrt_variance() );
		}

		// Combine lognormals, if any, before combining with the remainder of
		// the pi messages. We'll drag in the deltas (if any) later.

		Lognormal ln = null;
		if ( nlognormal > 0 )
			ln = new Lognormal( mu_sum, sigma2_sum );

		// Figure out whether all remaining pi messages have support contained in (0,+infinity).

		boolean negative_support = false;
		for ( int i = 0; i < others.size(); i++ )
			if ( ((Distribution)others.elementAt(i)).effective_support(1e-6)[0] < 0 )
			{
				negative_support = true;
				break;
			}
		
		if ( negative_support )
		{
			// Compute differential and make Gaussian approximation.
			throw new Exception( "Product_AbstractDistribution.compute_pi: can't yet handle negative support." );
		}
		else
		{
			// Take logs and transform product to sum, convolve, and transform back.

			Vector log_distributions = new Vector();
			for ( Enumeration e = others.elements(); e.hasMoreElements(); )
				log_distributions.addElement( new LogDensity( (Distribution)e.nextElement() ) );
			if ( ln != null ) log_distributions.addElement( ln.associated_gaussian );
			
			// Convolution usually returns a spline, but a few special cases are known.
			// If convolution doesn't return a spline, then convert return value to a spline.

			SplineDensity d;
			Distribution conv = Sum_AbstractDistribution.convolution( log_distributions );

			try { d = (SplineDensity) conv; }

			catch (ClassCastException e)
			{
				int N = Sum_AbstractDistribution.NGRID_MINIMUM;
				double[] x = new double[ N+1 ], p = new double[ N+1 ], x1 = new double[1];
				double[] supt = conv.effective_support( Sum_AbstractDistribution.SUPPORT_EPSILON );
				double dx = (supt[1] - supt[0])/N;

				for ( int i = 0; i < N+1; i++ )
				{
					x[i] = supt[0] + i*dx;
					x1[0] = x[i];
					p[i] = conv.p( x1 );
				}

				d = new SplineDensity( x, p );
			}

			double[] expy = new double[1];
			double[] inv_xform_x = new double[d.spline.x.length], inv_xform_f = new double[d.spline.x.length];

			for ( int i = 0; i < d.spline.x.length; i++ )
			{
				// Hey, while we're at it, take the delta product into account.
				expy[0] = Math.exp( d.spline.x[i] );
				inv_xform_x[i] = expy[0] * delta_product;
				inv_xform_f[i] = d.spline.f[i] / expy[0] / delta_product;
			}

			return new SplineDensity( inv_xform_x, inv_xform_f );	// need to recompute spline coefficients.
		}
	}

	double sqr( double x ) { return x*x; }
}

class LogDensity extends AbstractDistribution
{
	Distribution d;
	LogDensity( Distribution d )
	{
		this.d = d;
	}

	/** Compute the density function for the <tt>y = log(x)</tt>, namely
	  * <tt>p_x(exp(y)) exp(y)</tt>. 
	  */
	public double p( double[] x ) throws Exception
	{
		double[] expx = new double[1];
		expx[0] = Math.exp(x[0]);
		double px = d.p(expx)*expx[0];
		return px;
	}

	/** Find the effective support of the underlying distribution, and log-transform it.
	  */
	public double[] effective_support( double epsilon ) throws Exception
	{
		double[] supt = d.effective_support(epsilon);
		if ( supt[0] < 0 ) throw new Exception( "LogDensity.effective_support: underlying support: "+supt[0]+", "+supt[1] );
		if ( supt[0] == 0 ) supt[0] = 1e-16;
		supt[0] = Math.log( supt[0] );
		supt[1] = Math.log( supt[1] );
		return supt;
	}

	/** This is just a quick hack -- rough estimate by rectangle rule on a fixed grid.
	  */
	public double expected_value() throws Exception
	{
		int N = 50;
		double[] supt = effective_support(1e-4), x = new double[1];
		double sum_x = 0, dx = (supt[1]-supt[0])/N;

		for ( int i = 1; i <= N; i++ )
		{
			x[0] = supt[0] + i*dx;
			sum_x += p(x)*x[0];
		}
	
		return sum_x*dx;
	}

	/** This is just a quick hack -- rough estimate by rectangle rule on a fixed grid.
	  */
	public double sqrt_variance() throws Exception
	{
		int N = 50;
		double[] supt = effective_support(1e-4), x = new double[1];
		double sum_x = 0, sum_x2 = 0, dx = (supt[1]-supt[0])/N;

		for ( int i = 1; i <= N; i++ )
		{
			x[0] = supt[0] + i*dx;
			double px = p(x);
			sum_x += px*x[0];
			sum_x2 += px*x[0]*x[0];
		}
	
		double m = sum_x*dx, s2 = sum_x2*dx;
		return Math.sqrt( s2 - m*m );
	}
}
