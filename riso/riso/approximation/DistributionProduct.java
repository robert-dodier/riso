package riso.approximation;
import java.io.*;
import java.rmi.*;
import java.util.*;
import riso.distributions.*;
import riso.distributions.computes_lambda.*;
import numerical.*;
import SmarterTokenizer;

public class DistributionProduct extends riso.distributions.AbstractDistribution implements Callback_nd
{
	public double Z;
	public Distribution[] distributions;
	public double[][] merged_support;

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

	public DistributionProduct( boolean is_likelihood, boolean is_discrete, Distribution[] distributions ) throws Exception
	{
		super();
System.err.println( "DistributionProduct called." );
		int i;

		this.distributions = (Distribution[]) distributions.clone();
System.err.println( "\tthis: "+this );
		Vector supports_list = new Vector();

		for ( i = 0; i < distributions.length; i++ )
			try { supports_list.addElement( distributions[i].effective_support( 1e-12 ) ); } 
			catch (Exception e) 
			{
				// If distributions[i] doesn't have a well-defined support (e.g., if it is a 
				// likelihood function with unbounded support), leave it off the list.
				// Since we later take the intersection of intervals on the list, this policy
				// is equivalent to setting the support to the entire real line.
				;
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

		double[][] supports = new double[ supports_list.size() ][];
		supports_list.copyInto( supports );
		merged_support = Intervals.intersection_merge_intervals( supports );	// SHOULD BE UNION ???
System.err.print( "DistributionProduct: merged support: " );
for ( i = 0; i < merged_support.length; i++ )
System.err.print( "["+merged_support[i][0]+", "+merged_support[i][1]+"] " );
System.err.println( "" );

		double tolerance = 1e-5;

		Z = 1;	// IMPORTANT !!! This must be set before trying to evaluate integrals !!!

		try
		{
			double sum = 0;
			double[] a1 = new double[1], b1 = new double[1];
			boolean[] is_discrete1 = new boolean[1];
			boolean[] skip_integration = new boolean[1];

			is_discrete1[0] = is_discrete;

			for ( i = 0; i < merged_support.length; i++ )
			{
				a1[0] = merged_support[i][0];
				b1[0] = merged_support[i][1];
				IntegralHelper ih = new IntegralHelper( this, a1, b1, is_discrete1, skip_integration );
				sum += ih.do_integral();
			}

			Z = sum;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new Exception( "DistributionProduct: exception: "+e );
		}

System.err.println( "DistributionProduct: Z: "+Z );
	}

	public double f( double[] x ) throws Exception { return p(x); }

	public double p( double[] x ) throws Exception
	{
		double product = 1/Z;
		for ( int i = 0; i < distributions.length; i++ )
{ if ( distributions[i] == null ) System.err.println( "**** DP.p: this: "+this+"  i == "+i );
			product *= distributions[i].p(x);
}
		return product;
	}

	public int ndimensions() { return 1; }

	/** Formats a string representation of this distribution.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		int i;

		String result = "";
		result += this.getClass().getName()+" { ";

		String more_ws = leading_ws+"\t";
		result += more_ws+"normalizing-constant "+Z+"\n";
		result += more_ws+"merged-support ";
		result += more_ws+"ndistributions "+distributions.length+"\n";

		result += more_ws+"merged-support { ";
		for ( i = 0; i < merged_support.length; i++ )
			result += merged_support[i][0]+" "+merged_support[i][1]+" ";
		result += "}"+"\n";

		String still_more_ws = more_ws+"\t";
		result += more_ws+"distributions"+"\n"+more_ws+"{"+"\n"+still_more_ws;
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
