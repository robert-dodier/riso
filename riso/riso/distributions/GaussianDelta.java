package riso.distributions;
import java.io.*;
import java.rmi.*;
import java.util.*;
import numerical.*;
import SmarterTokenizer;

/** An object of this class represents a continuous distribution 
  * whose mass is concentrated at one point. An instance of this
  * class can be used anywhere that an instance of <tt>Gaussian</tt>
  * can be used.
  */
public class GaussianDelta extends Gaussian implements Delta
{
	public GaussianDelta() throws RemoteException {}

	/** Return the point on which the mass of this density is concentrated.
	  */
	public double[] get_support() { return (double[]) mu.clone(); }

	/** Computes the density at the point <code>x</code>. 
	  * This method is a little strange since this distribution is continuous, and strictly 
	  * speaking a meaningful density can't be defined in that case. Oh, well.
	  *
	  * @return 1 if <tt>x</tt> is equal to the support point and 0 otherwise.
	  * @param x Point at which to evaluate density.
	  * @throws RemoteException If the support point is not defined.
	  */
	public double p( double[] x ) throws RemoteException
	{
		if ( mu != null )
		{
			for ( int i = 0; i < mu.length; i++ )
				if ( x[i] != mu[i] )
					return 0;
			return 1;
		}
		else
			throw new RemoteException( "GaussianDelta.p: support point not defined." );
	}

	/** Return an instance of a random variable from this distribution.
	  * This function always returns the point on which this distribution
	  * is concentrated, since all the mass is there.
	  */
	public double[] random() throws RemoteException
	{
		if ( mu != null )
			return (double[]) mu.clone();
		else
			throw new RemoteException( "GaussianDelta.random: support point not defined." );
	}

	/** Place-holder for method to update the parameters of this distribution;
	  * not implemented -- see exception description.
	  * @throws RemoteException Because this method can't be meaningfully implemented
	  *   for this type of distribution.
	  */
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception, RemoteException
	{
		throw new RemoteException( "GaussianDelta.update: not meaningful for this distribution." );
	}

	/** Read a description of this distribution model from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param st Supplies sequence of tokens for this method; 
	  *   should not parse numbers, but rather should read them as string values.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException, RemoteException
	{
		boolean found_closing_bracket = false;

		try
		{
			st.nextToken();
			if ( st.ttype != '{' ) throw new IOException( "GaussianDelta.pretty_input: input doesn't have opening bracket." );

			if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "support" ) )
			{
				st.nextToken();		// eat left curly brace

				Vector support_vector = new Vector();
				for ( st.nextNumber(); st.ttype == StreamTokenizer.TT_NUMBER; st.nextNumber() )
					support_vector.addElement( new Double( st.nval ) );
				
				mu = new double[ support_vector.size() ];
				for ( int i = 0; i < mu.length; i++ )
					mu[i] = ((Double)support_vector.elementAt(i)).doubleValue();		// Barf! <sigh>...
			}

			if ( st.ttype == '}' ) found_closing_bracket = true;
		}
		catch (IOException e)
		{
			throw new IOException( "GaussianDelta.pretty_input: attempt to read object failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "GaussianDelta.pretty_input: no closing bracket on input." );

		ndims = mu.length;
		Sigma = new double[ndims][ndims];		// initialized w/ zeros
		L_Sigma = new double[ndims][ndims];		// initialized w/ zeros
		Sigma_inverse = null;					// inverse undefined !!!
		det_Sigma = 0;
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
		if ( mu != null )
		{
			result += "support-point { ";
			for ( int i = 0; i < mu.length; i++ )
				result += mu[i]+" ";
			result += "}"+" ";
		}
		result += "}"+"\n";
		return result;
	}
}
