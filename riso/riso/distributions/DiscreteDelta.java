package risotto.distributions;
import java.io.*;
import java.rmi.*;
import java.util.*;
import numerical.*;
import SmarterTokenizer;

/** An object of this class represents a discrete distribution 
  * whose mass is concentrated at one point. An instance of this
  * class can be used anywhere that an instance of <tt>Discrete</tt>
  * can be used.
  */
public class DiscreteDelta extends Discrete implements Delta
{
	/** The point on which the mass of this distribution is concentrated.
	  */
	public int[] support_point = null;

	/** Do-nothing constructor.
	  */
	public DiscreteDelta() throws RemoteException {}

	/** Given the dimensions of a discrete probability space and a support point,
	  * this method constructs a discrete delta distribution.
	  */
	public DiscreteDelta( int[] dimensions_in, int[] support_point_in ) throws RemoteException
	{
		ndims = dimensions_in.length;
		dimensions = (int[]) dimensions_in.clone();
		support_point = (int[]) support_point_in.clone();

		int i, ii = 0, nprobabilities = 1;
		for ( i = 0; i < ndims; i++ )
			nprobabilities *= dimensions[i];

		probabilities = new double[ nprobabilities ];

		// Compute indexing polynomial, then set table value.

		for ( i = 0; i < ndims-1; i++ )
			ii = dimensions[i+1] * (ii + support_point[i]);
		ii += support_point[ndims-1];

		probabilities[ii] = 1;
	}

	/** Return the point on which the mass of this density is concentrated.
	  */
	public double[] get_support() throws RemoteException { return random(); }

	/** Computes the density at the point <code>x</code>. 
	  *
	  * @return 1 if <tt>x</tt> is equal to the support point and 0 otherwise.
	  * @param x Point at which to evaluate density.
	  * @throws RemoteException If the support point is not defined.
	  */
	public double p( double[] x ) throws RemoteException
	{
		if ( support_point != null )
		{
			for ( int i = 0; i < support_point.length; i++ )
				if ( (int)x[i] != support_point[i] )
					return 0;
			return 1;
		}
		else
			throw new RemoteException( "DiscreteDelta.p: support point not defined." );
	}

	/** Return an instance of a random variable from this distribution.
	  * This function always returns the point on which this distribution
	  * is concentrated, since all the mass is there.
	  */
	public double[] random() throws RemoteException
	{
		if ( support_point != null )
		{
			double[] r = new double[ support_point.length ];
			for ( int i = 0; i < r.length; i++ ) r[i] = support_point[i];
			return r;
		}
		else
			throw new RemoteException( "DiscreteDelta.random: support point not defined." );
	}

	/** Place-holder for method to update the parameters of this distribution;
	  * not implemented -- see exception description.
	  * @throws RemoteException Because this method can't be meaningfully implemented
	  *   for this type of distribution.
	  */
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception, RemoteException
	{
		throw new RemoteException( "DiscreteDelta.update: not meaningful for this distribution." );
	}

	/** Read a description of this distribution model from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param st Supplies sequence of tokens for this method; 
	  *   should not parse numbers, but rather should read them as string values.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException, RemoteException
	{
		throw new IOException( "DiscreteDelta.pretty_input: not implemented." );
	}

	/** Write a description of this distribution model to an output stream.
	  * The description is human-readable; this is different from object
	  * serialization. 
	  * @param os Output stream to write to.
	  * @param leading_ws This parameter is ignored.
	  */
	public void pretty_output( OutputStream os, String leading_ws ) throws IOException, RemoteException
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		dest.print( format_string( leading_ws ) );
	}

	/** Create a description of this distribution model as a string.
	  * @param leading_ws Leading whitespace; this argument is ignored
	  *   by this method.
	  */
	public String format_string( String leading_ws ) throws RemoteException
	{
		String result = "";
		result += this.getClass().getName()+" "+"{"+" ";
		if ( support_point != null )
		{
			result += "support-point { ";
			for ( int i = 0; i < support_point.length; i++ )
				result += support_point[i]+" ";
			result += "}"+" ";
		}
		result += "}"+"\n";
		return result;
	}
}
