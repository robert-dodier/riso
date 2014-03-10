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
import riso.numerical.*;
import riso.general.*;

/** @see PiHelper
  */
public class Sum_AbstractDistribution implements PiHelper
{
	public static double MIN_DISPERSION_RATIO = 1/50.0;
	public static int NGRID_MINIMUM = 256;
	public static double SUPPORT_EPSILON = 1e-4;

    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>Sum</tt>
	  * followed by any number of <tt>AbstractDistribution</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.Sum", 1 );
		s[1] = new SeqTriple( "riso.distributions.AbstractDistribution", -1 );
		description_array = s;
	}

	public Distribution compute_pi( ConditionalDistribution py_in, Distribution[] pi_messages ) throws Exception
	{
		// First locate any Gaussians and Gaussian mixtures in the pi messages -- these
		// can be handled as an easy special case. Combine the resulting Gaussian
		// or mixture of Gaussians (if any) with the remaining pi messages. The convolution
		// for the general case is computed numerically.

		Vector gaussians = new Vector(), mix_gaussians = new Vector(), other_distributions = new Vector();
		for ( int i = 0; i < pi_messages.length; i++ )
			if ( pi_messages[i] instanceof Gaussian )
				gaussians.addElement( pi_messages[i] );
			else if ( pi_messages[i] instanceof MixGaussians )
				mix_gaussians.addElement( pi_messages[i] );
			else
				other_distributions.addElement( pi_messages[i] );

		Distribution gaussian_sum = null;
		Distribution mix_gaussians_sum = null;

		if ( gaussians.size() > 0 )
		{
			Distribution[] gaussian_msgs = new Distribution[ gaussians.size() ];
			gaussians.copyInto( gaussian_msgs );
			Sum py = (Sum) ((Sum)py_in).clone();
			gaussian_sum = (new Sum_Gaussian()).compute_pi( py, gaussian_msgs );

			MixGaussians mix1 = new MixGaussians( 1, 1 );
			mix1.components[0] = gaussian_sum;
			mix_gaussians.addElement( mix1 );
		}

		if ( mix_gaussians.size() > 0 )
		{
			Distribution[] mix_gaussian_msgs = new Distribution[ mix_gaussians.size() ];
			mix_gaussians.copyInto( mix_gaussian_msgs );
			Sum py = (Sum) ((Sum)py_in).clone();
			mix_gaussians_sum = (new Sum_MixGaussians()).compute_pi( py, mix_gaussian_msgs );

			other_distributions.addElement( mix_gaussians_sum );
		}

		// At this point, any Gaussians or mixtures of Gaussians have been combined into
		// a single mixture of Gaussians and added to the list of other distributions.
		// Now compute a numerical convolution to combine all the distributions on the
		// other distributions list.

		return convolution( other_distributions );
	}

	/** First discretize all distributions to a common grid.
	  * The grid spacing must be the same for all, but the left and right ends may differ.
	  * Then convolve the discretized distributions, and use the result to construct a table
	  * of values <tt>(x,p(x))</tt>. Finally, construct a spline using the table and return
	  * the spline.
	  *
	  * <p> Unfortunately, the grid spacing will be governed by the least dispersion among
	  * the input distributions, which may mean that the wider distributions are
	  * discretized into bazillions of points. Now the distribution of the sum will
	  * be dominated by the components with the largest dispersions, so there's little
	  * accuracy gained by trying to accomodate the narrow distributions. So to avoid
	  * enormous grids, we'll check the dispersion of each incoming distribution, and 
	  * throw out any distributions which have a dispersion much smaller than the largest
	  * dispersion.
	  *
	  * <p> If there is only one element in <tt>distributions</tt>, that element is returned.
	  *
	  * <p> HOW SHALL WE HANDLE DISCRETE DISTRIBUTIONS HERE ???
	  */
	public static Distribution convolution( Vector distributions )
	{
		// Handle degenerate case.
		if ( distributions.size() == 1 ) return (Distribution) distributions.elementAt(0);

		double s_max = 0, s_min = Double.MAX_VALUE, endpt_fixup = 0;
		double[] s = new double[ distributions.size() ];
		int i = 0;

		for ( Enumeration e = distributions.elements(); e.hasMoreElements(); i++ )
			try { if ( (s[i] = ((Distribution)e.nextElement()).sqrt_variance()) > s_max ) s_max = s[i]; }
			catch (Exception ex) { System.err.println( "computes_pi.convolution: strange; "+ex ); }

		Vector wide_enough = new Vector();
		for ( i = 0; i < s.length; i++ )
			if ( s[i]/s_max > MIN_DISPERSION_RATIO )
			{
				wide_enough.addElement( distributions.elementAt(i) );
				if ( s[i] < s_min ) s_min = s[i];	// find minimum over the wide enough distributions.
			}
			else
			{
				double m = 0;
				try { m = ((Distribution)distributions.elementAt(i)).expected_value(); }
				catch (Exception ex) { System.err.println( "computes_pi.convolution: strange; "+ex ); }
				endpt_fixup += m;	
			}
		
		double dx = 6*s_min/NGRID_MINIMUM, left_endpt = endpt_fixup, right_endpt = endpt_fixup;
		
		double[][] discretized = new double[ wide_enough.size() ][];

		for ( i = 0; i < discretized.length; i++ )
		{
			Distribution d = (Distribution) wide_enough.elementAt(i);
			double[] support, x = new double[1];
			try { support = d.effective_support( SUPPORT_EPSILON ); }
			catch (Exception e) { throw new RuntimeException( "computes_pi.convolution: failed to compute support: "+e ); }

			// Round up the number of points in the support interval, since length may not be an
			// integer multiple of dx.

			int N = 1 + (int) ((support[1]-support[0])/dx);
			discretized[i] = new double[N+1];
			support[1] = support[0] + N*dx;		

			left_endpt += support[0];
			right_endpt += support[1];

			// In the following discretization, we just evaluate the density function at each point. !!!
			// It would probably work better to compute the mass on each interval and work with that. !!!

			for ( int j = 0; j < N; j++ )
			{
				x[0] = support[0] + j*dx;
				try { discretized[i][j] = d.p(x); }
				catch (Exception e) { System.err.println( "computes_pi.convolution: p(x) failed; "+e ); }
			}

			x[0] = support[1];
			try { discretized[i][N] = d.p(x); }
			catch (Exception e) { System.err.println( "computes_pi.convolution: p(x) failed; "+e ); }
		}
		
		double[] conv = discretized[0];
		for ( i = 1; i < discretized.length; i++ )
		{
			conv = Convolve.convolve( conv, discretized[i] );

			// Normalize components to sum to 1; this avoids overflow.

			double sum = 0;
			for ( int j = 0; j < conv.length; j++ )
				sum += conv[j];
			for ( int j = 0; j < conv.length; j++ )
				conv[j] /= sum;
		}

		double[] x = new double[ conv.length ];
		for ( i = 0; i < conv.length-1; i++ )
			x[i] = left_endpt + i*dx;
		x[ conv.length-1 ] = right_endpt;

		try { return new SplineDensity( x, conv ); }
		catch (Exception e) { throw new RuntimeException( "computes_pi.convolution: failed: "+e ); }
	}
}
