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
package riso.approximation;
import java.io.*;
import java.rmi.*;
import java.util.*;
import riso.distributions.*;
import riso.numerical.*;
import riso.general.*;

/** An instance of this class represents a pointwise product of distributions.
  * Given densities <tt>p_1, p_2, p_3,...</tt> the density of the product is
  * <tt>p_1(x) p_2(x) p_3(x)...</tt>. 
  * Such products arise in the computation of the likelihood and posterior for a variable,
  * since the posterior is proportional to the pointwise product of the likelihood and
  * the prior, and the likelihood is proportional to the pointwise product of the 
  * likelihood messages.
  */
public class DistributionProduct extends riso.distributions.AbstractDistribution implements Callback_nd
{
	public double Z;
	public Distribution[] distributions;
	public double[][] support;

	/** Return a string which briefly describes this object.
	  * The string is just the list of classes of distributions in this product.
	  */
	public String toString()
	{
		String s = this.getClass()+"[";
		for ( int i = 0; i < distributions.length; i++ )
		{
			if ( distributions[i] == null )
				throw new RuntimeException( "DistributionProduct.toString: whence distribution["+i+"] == null ???" );
			s += (i == 0 ? "" : ",");
			s += distributions[i].getClass().getName();
		}
		s += "]";

		return s;
	}

	/** Construct a product of distributions.
	  * An attempt is made to find the support of each element in the list of distributions.
	  * In some cases, the attempt will fail -- typically for likelihood functions,
	  * which do not necessarily have bounded support. If at least one element has bounded
	  * support (and we successfully find it), then the normalizing constant <tt>Z</tt>
	  * is computed.
	  *
	  * @throws SupportNotWellDefinedException If the attempt to find the support fails for 
	  *  every distribution, and this product is not a likelihood function (as indicated by the argument).
	  * @throws Exception If the integration to compute the normalizing constant fails.
	  */
	public DistributionProduct( boolean is_likelihood, boolean is_discrete, Distribution[] distributions ) throws Exception
	{
		super();
		int i;

		this.distributions = (Distribution[]) distributions.clone();
		Vector supports_list = new Vector();

		for ( i = 0; i < distributions.length; i++ )
		{
System.err.println( "DistProd: dist["+i+"]: "+distributions[i].getClass().getName() );
			try { supports_list.addElement( distributions[i].effective_support( 1e-6 ) ); } 
			catch (Exception e) 
			{
				// If distributions[i] doesn't have a well-defined support (e.g., if it is a 
				// likelihood function with unbounded support), leave it off the list.
				// Since we later take the intersection of intervals on the list, this policy
				// is equivalent to setting the support to the entire real line.
System.err.println( "\t"+"supt undefined: "+distributions[i].getClass() );
				;
			}
		}

		if ( supports_list.size() == 0 ) 
		{
			// If this product is a likelihood function, it's OK if
			// we can't carry out the integration; just print a warning.

			String msg = "DistributionProduct: none of the components have a well-defined support, so neither does the product.";
			if ( is_likelihood )
			{
				Z = 1;
				System.err.println( msg );
				return;
			}
			else
				throw new SupportNotWellDefinedException(msg);
		}

		Z = 1;	// This must be set before trying to evaluate p or f !!!
		double[][] all_supports = new double[ supports_list.size() ][];
		supports_list.copyInto( all_supports );
for ( i = 0; i < all_supports.length; i++ )
System.err.println( "\tall_supports["+i+"]: "+all_supports[i][0]+", "+all_supports[i][1] );
		double[][] merged_support = Intervals.intersection_merge_intervals( all_supports );
if ( merged_support == null ) throw new SupportNotWellDefinedException( "DistributionProduct: multiplicands have disjoint support." );
for ( i = 0; i < merged_support.length; i++ )
System.err.println( "\tmerged_support["+i+"]: "+merged_support[i][0]+", "+merged_support[i][1] );
		support = Intervals.trim_support( (Distribution)this, merged_support );
		if ( support == null ) throw new SupportNotWellDefinedException( "DistributionProduct: multiplicands have disjoint support." );
for ( i = 0; i < support.length; i++ )
System.err.println( "\tsupport["+i+"]: "+support[i][0]+", "+support[i][1] );

int N = 200;
double[] x = new double[1];
double dx = (support[0][1]-support[0][0])/200;
for ( i = 0; i < N; i++ ) {
x[0] = (i+0.5)*dx + support[0][0];
System.err.print( "x: "+x[0]+"    p: " );
for ( int j = 0; j < distributions.length; j++ ) {
 double pp = distributions[j].p(x);
 System.err.print( pp+"    " ); }
System.err.println(""); }

		try
		{
			double sum = 0;
			double[] a1 = new double[1], b1 = new double[1];
			boolean[] is_discrete1 = new boolean[1];
			boolean[] skip_integration = new boolean[1];

			is_discrete1[0] = is_discrete;

			for ( i = 0; i < support.length; i++ )
			{
				a1[0] = support[i][0];
				b1[0] = support[i][1];
				IntegralHelper ih = IntegralHelperFactory.make_helper( this, a1, b1, is_discrete1, skip_integration );
				sum += ih.do_integral();
			}

			Z = sum;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new Exception( "DistributionProduct: exception: "+e );
		}
	}

	/** Returns <tt>p(x)</tt>.
	  */
	public double f( double[] x ) throws Exception { return p(x); }

	/** Returns the product <tt>(1/Z) prod_{i=0}^{n-1} distributions[i].p(x)</tt>.
	  */
	public double p( double[] x ) throws Exception
	{
		double product = 1/Z;
// System.err.print( "DistributionProduct.p("+x[0]+") == " );
		for ( int i = 0; i < distributions.length; i++ )
		{
			double px = distributions[i].p(x);
// System.err.print( px+"*" );
			product *= px;
		}
// System.err.println( " == "+product );
		return product;
	}

	/** Always returns 1.
	  */
	public int ndimensions() { return 1; }

	/** Formats a string representation of this distribution.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		int i;

		String result = "";
		result += this.getClass().getName()+" {\n";

		String more_ws = leading_ws+"\t";
		result += more_ws+"normalizing-constant "+Z+"\n";
		result += more_ws+"ndistributions "+distributions.length+"\n";

		result += more_ws+"support { ";
		for ( i = 0; i < support.length; i++ )
			result += support[i][0]+" "+support[i][1]+" ";
		result += "}"+"\n";

		String still_more_ws = more_ws+"\t";
		result += more_ws+"distributions"+"\n"+more_ws+"{"+"\n";
		for ( i = 0; i < distributions.length; i++ )
		{
			result += still_more_ws+"% distributions["+i+"]"+"\n";
			result += still_more_ws+distributions[i].format_string( still_more_ws );
		}
		result += more_ws+"}"+"\n";

		result += leading_ws+"}"+"\n";
		return result;
	}

	/** Parse a string containing a description of an instance of this distribution.
	  * The description is contained within curly braces, which are included in the string.
	  */
	public void parse_string( String description ) throws IOException
	{
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( description ) );
		pretty_input( st );
	}

	/** Write an instance of this distribution to an output stream.
	  *
	  * @param os The output stream to print on.
	  * @param leading_ws Since the representation is only one line of output, 
	  *   this argument is ignored.
	  * @throws IOException If the output fails; this is possible, but unlikely.
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.print( format_string( leading_ws ) );
	}

	/** Constructs an initial approximation to the product of distributions.
	  * This initial mixture is constructed by obtaining a Gaussian mixture
	  * approximation to each component, then merging the component 
	  * approximations. (To ``merge'' several Gaussian mixtures, we just create
	  * another mix containing all the bumps in each input mixture;
	  * the mixing proportion for some bump <tt>k</tt> in the result
	  * is equal to <tt>1/n a[k]</tt> where <tt>n</tt> is the number of
	  * input mixtures and <tt>a[k]</tt> is the mixing proportion of the
	  * bump in the mixture it came from.)
	  * 
	  * @param support Region of interest; concentrate the mixture here.
	  *   This argument is of greatest interest to approximations of likelihood
	  *   functions, which may be defined everywhere but uninteresting in
	  *   most places. It is NOT guaranteed that the mixture returned has
	  *   support contained within <tt>support</tt>.
	  */
	public riso.distributions.MixGaussians initial_mix( double[] support )
	{
		try
		{
			riso.distributions.MixGaussians[] q = new riso.distributions.MixGaussians[ distributions.length ];

			int i, j, k, ncomponents = 0;

			for ( i = 0; i < distributions.length; i++ )
			{
System.err.println( "DistributionProduct.initial_mix: get initial mix for "+distributions[i].getClass().getName() );
				q[i] = distributions[i].initial_mix( support );
				ncomponents += q[i].ncomponents();
			}

			riso.distributions.MixGaussians qq = new riso.distributions.MixGaussians( 1, ncomponents );

			for ( i = 0, j = 0; i < q.length; i++ )
			{
				for ( k = 0; k < q[i].ncomponents(); j++, k++ )
				{
					qq.components[j] = q[i].components[k];
					qq.mix_proportions[j] = (1.0/q.length)*q[i].mix_proportions[k];
				}
			}

			return qq;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException( "DistributionProduct: unexpected: "+e );
		}
	}
}
