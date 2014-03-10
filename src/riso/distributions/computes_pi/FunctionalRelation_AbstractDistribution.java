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
import riso.approximation.*;
import riso.distributions.*;
import riso.numerical.*;
import riso.general.*;

/** @see PiHelper
  */
public class FunctionalRelation_AbstractDistribution implements PiHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	public static int NRANDOM_PER_DIMENSION = 10;
	public static int NGRID = 256;
	public static double SUPPORT_EPSILON = 1e-4;

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>FunctionalRelation</tt>
	  * followed by any number of <tt>AbstractDistribution</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.FunctionalRelation", 1 );
		s[1] = new SeqTriple( "riso.distributions.AbstractDistribution", -1 );
		description_array = s;
	}

	public Distribution compute_pi( ConditionalDistribution pyx_in, Distribution[] pi_messages ) throws Exception
	{
		FunctionalRelation pyx = (FunctionalRelation) pyx_in;

		// How best to go about this depends on how many non-delta pi messages we have.
		// If only one, use the existing code in FunctionalRelation_AbstractDistribution.
		// If more than one, we need to integrate numerically over the non-deltas.

		int ndelta = 0;
		for ( int i = 0; i < pi_messages.length; i++ ) if ( pi_messages[i] instanceof Delta ) ++ndelta;

		if ( pi_messages.length - ndelta == 0 )
			// Well, this is a little strange -- another helper should have picked this up.
			return (new FunctionalRelation_GaussianDelta()).compute_pi( pyx, pi_messages );
		else if ( pi_messages.length - ndelta == 1 )
			return compute_pi_1nondelta( pyx, pi_messages );
		else
			return compute_pi_many_nondelta( pyx, pi_messages );
	}

	/** Figure out which pi-message has the greater variance, and use the inversion formula on that one.
	  * For the other, we need to do a 1-dimensional numerical integration. 
	  */
	public Distribution compute_pi_many_nondelta( FunctionalRelation pyx, Distribution[] pi_messages ) throws Exception
	{
		int k_max = -1;
		double s_max = -1e300;

		for ( int j = 0, i = 0; i < pi_messages.length; i++ )
			if ( !(pi_messages[i] instanceof Delta) )
				if ( pi_messages[i].sqrt_variance() > s_max )
				{
					s_max = pi_messages[i].sqrt_variance();
					k_max = i;
				}

		// Construct the integrand for the computation of p_y, with the parent with the greatest
		// variance in the innermost integration.
		
		double[] a = new double[ pi_messages.length ], b = new double[ pi_messages.length ];
		boolean[] is_discrete = new boolean[ pi_messages.length ], skip_integration = new boolean[ pi_messages.length ];
		
		for ( int i = 0; i < a.length; i++ )
		{
			double[] supt = pi_messages[i].effective_support( SUPPORT_EPSILON );
			a[i] = supt[0];
			b[i] = supt[1];
		}

		for ( int i = 0; i < is_discrete.length; i++ ) 
			if ( pi_messages[i] instanceof Discrete ) is_discrete[i] = true;
		skip_integration[ k_max ] = true;

		FunctionalRelationIntegrand fri = new FunctionalRelationIntegrand( pyx, pi_messages, a, b, k_max );

		IntegralHelper ih = IntegralHelperFactory.make_helper( fri, a, b, is_discrete, skip_integration );
		
		// Find the extreme values of F(x) over the supports of the pi-messages; we will take the
		// resulting range as the range of y.

		double[] y_supt = find_range( pyx, pi_messages ), y = new double[NGRID+1], py = new double[NGRID+1];
		double dy = (y_supt[1]-y_supt[0])/NGRID;

		for ( int i = 0; i < NGRID+1; i++ )
		{
			fri.y = y_supt[0] + i*dy;
			y[i] = fri.y;
			py[i] = ih.do_integral();
System.err.println( "\t"+"compute_pi_2nondelta: i, y, py: "+i+", "+y[i]+", "+py[i] );
		}

		return new SplineDensity( y, py );
	}

	public Distribution compute_pi_1nondelta( FunctionalRelation pyx, Distribution[] pi_messages ) throws Exception
	{
		int k = -1;
		for ( int i = 0; i < pi_messages.length; i++ )
			if ( ! (pi_messages[i] instanceof Delta) ) 
			{
				k = i;
				break;
			}

		return inversion_formula( pyx, pi_messages, k );
	}

	/** Use the usual inversion formula
	  * <pre>
	  *   p_y(y) = sum_{x \in X*} p_x(x)/|dF/dx(x)|
	  * </pre>
	  * where <tt>X*</tt> is the set of roots of the equation <tt>y=F(x)</tt>.
	  */
	public Distribution inversion_formula( FunctionalRelation pyx, Distribution[] pi_messages, int k ) throws Exception
	{
		double[] xk_supt = pi_messages[k].effective_support( SUPPORT_EPSILON ), x = new double[pi_messages.length];
		double[][] ypy = new double[ NGRID+1 ][2];
		Distribution px = pi_messages[k];
		double[] x1 = new double[1];

		// Let x range over the effective support of px, computing the corresponding p(y) as we go.
		// We'll sort on y afterwards.

		for ( int i = 0; i < NGRID+1; i++ )
		{
			x[k] = xk_supt[0] + i*(xk_supt[1]-xk_supt[0])/(double)NGRID;
			ypy[i][0] = pyx.F(x);

			double[] xx = pyx.component_roots( ypy[i][0], k, xk_supt[0], xk_supt[1], x );

			double sum = 0;
			for ( int j = 0; j < xx.length; j++ )
			{
				x[k] = xx[j];
				double[] grad = pyx.dFdx(x);
				if ( grad[k] == 0 )
					; // JUST OMIT THIS POINT !!! THERE IS A SINGULARITY HERE -- WHAT'S THE RIGHT THING TO DO ???
				else
				{
					x1[0] = x[k];
					sum += px.p(x1)/Math.abs(grad[k]);
				}
			}

			ypy[i][1] = sum; // if no roots were found, sum is still zero.
		}

		// Sort by y. Remove any duplicate y's -- take the average of the corresponding p(y).

		ShellSort.do_sort( (Object[])ypy, 0, ypy.length-1, new PairComparator() );

		int ndistinct = 1;
		for ( int i = 1; i < ypy.length; i++ ) if ( ypy[i][0] != ypy[i-1][0] ) ++ndistinct;

		double[] y = new double[ndistinct], py = new double[ndistinct];

		y[0] = ypy[0][0];
		double py_sum = ypy[0][1];
		int n = 1, j = 0;
		
		for ( int i = 1; i < ypy.length; i++ )
		{
			if ( ypy[i][0] != ypy[i-1][0] )
			{
				py[j] = py_sum/n; // fix up py for duplicated y value -- works right if n == 1.
				++j;
				y[j] = ypy[i][0];

				py_sum = ypy[i][1];
				n = 1;
			}
			else
			{
				py_sum += ypy[i][1];
				++n;
			}
		}

		// Fix up final y, py pair.
		py[j] = py_sum/n;

		return new SplineDensity( y, py );
	}

	public double[] find_range( FunctionalRelation pyx, Distribution[] pi_messages ) throws Exception
	{
		// Find the range of each x variable, then spread random points over the 
		// product of the x ranges. Sort the results according to the y coordinate.
		// Polish the few highest and lowest. NEED TO CONSTRAIN x's TO THE SUPPORTS !!!
		// OMIT THE POLISHING STEP FOR NOW !!!

		double[][] supports = new double[pi_messages.length][];
		for ( int i = 0; i < pi_messages.length; i++ )
			supports[i] = pi_messages[i].effective_support(1e-4);
		
		int nrandom = 1;
		for ( int i = 0; i < pi_messages.length; i++ ) nrandom *= NRANDOM_PER_DIMENSION;

System.err.println( "find_range: "+pi_messages.length+" dimensions; nrandom: "+nrandom );
		double y_min = 1e300, y_max = -1e300;
		double[] x_min = null, x_max = null, x = new double[pi_messages.length];

		for ( int i = 0; i < nrandom; i++ )
		{
			for ( int j = 0; j < pi_messages.length; j++ )
				x[j] = supports[j][0] + (supports[j][1]-supports[j][0])*Math.random();

			double y = pyx.F(x);

			if ( y < y_min )
			{
				y_min = y;
				x_min = (double[]) x.clone();
			}

			if ( y > y_max )
			{
				y_max = y;
				x_max = (double[]) x.clone();
			}
		}

		double[] y_support = new double[2];
		y_support[0] = y_min;
		y_support[1] = y_max;
System.err.println( "\t"+"ymax: "+y_min+", "+y_max );
		return y_support;
	}

	class PairComparator implements riso.general.Comparator
	{
		public boolean greater( Object a, Object b )
		{
			return ((double[])a)[0] > ((double[])b)[0]; // sort on first column
		}
	}

	class FunctionalRelationIntegrand implements Callback_nd
	{
		double y; // value of child variable at which to evaluate integrand

		int k; // distinguished x dimension -- integration over this one is carried out by inversion formula
		Distribution[] pi_messages;
		FunctionalRelation pyx;
		double[] a, b; // support of each pi-message; ACTUALLY ONLY a[k] AND b[k] ARE NEEDED; NO BIG DEAL !!!

		public FunctionalRelationIntegrand( FunctionalRelation pyx, Distribution[] pi_messages, double[] a, double[] b, int k )
		{
			this.pyx = pyx;
			this.pi_messages = (Distribution[]) pi_messages.clone();
			this.a = (double[]) a.clone();
			this.b = (double[]) b.clone();
			this.k = k;
		}

		public double f( double[] x ) throws Exception
		{
			double[] x1 = new double[1];
			double pi_product = 1;

			for ( int i = 0; i < pi_messages.length; i++ )
			{
				x1[0] = x[i];
				if ( i == k ) continue;
				else pi_product *= pi_messages[i].p(x1);
			}

			double[] xx = pyx.component_roots( y, k, a[k], b[k], x );
			double inverse_term = 0;

			for ( int j = 0; j < xx.length; j++ )
			{
				x[k] = xx[j];
				double[] grad = pyx.dFdx(x);
				if ( grad[k] == 0 )
					; // JUST OMIT THIS POINT !!! THERE IS A SINGULARITY HERE -- WHAT'S THE RIGHT THING TO DO ???
				else
				{
					x1[0] = x[k];
					inverse_term += pi_messages[k].p(x1)/Math.abs(grad[k]);
				}
			}

			return inverse_term*pi_product;
		}
	}
}
