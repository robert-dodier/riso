/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 2004, Robert Dodier.
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
package riso.distributions;
import java.io.*;
import java.rmi.*;
import riso.belief_nets.*;
import riso.numerical.*;
import riso.general.*;

public class Factorized extends AbstractDistribution
{
    BeliefNetwork belief_net;

	/** Create and return a copy of this distribution.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		Factorized copy = (Factorized) super.clone();
		return copy;
	}

	/** Default constructor for this class.
	  */
	public Factorized() {}

    public Factorized (BeliefNetwork belief_net) { this.belief_net = belief_net; }

	/** Returns the number of dimensions in which this distribution lives.
	  */
	public int ndimensions()
    {
        try { return belief_net.get_variables().length; }  // SHOULD BE SUM OF DIMENSIONS !!!
        catch (RemoteException e) { return 0; } // can't happen: belief_net is not remote
    }

	/** Computes the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density -- must
	  *   be a one-element array.
	  */
	public double p( double[] x )
	{
        return 0;   // !!!
	}

	/** Compute the cumulative distribution function.
	  * THROWS AN EXCEPTION FOR ANY NONTRIVIAL BELIEF NETWORK !!!
	  */
	public double cdf( double x ) throws Exception
	{
        if (ndimensions() == 1)
            // !!! return belief_net.get_variables()[0].cdf (x);
            return 0;
        else
            throw new IllegalArgumentException ("Factorized.cdf: doesn't make sense for multidimensional distribution.");
	}

	/** Computes the log of the prior probability of the parameters of
	  * this distribution, assuming some prior distribution has been 
	  * established. This may not be meaningful for all distributions.
	  */
	public double log_prior() throws Exception
	{
		throw new Exception( "Factorized.log_prior: not implemented." );
	}

	/** Return an instance of a random variable from this distribution.
	  */
	public double[] random() throws Exception
	{
        return null; // !!!
	}

	/** Use data to modify the parameters of the distribution.
	  * This method is not implemented.
	  */
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception
	{
		throw new Exception( "Factorized.update: not implemented." );
	}

	/** Returns the expected value of this distribution.
	  */
	public double expected_value() 
	{
		return 0; // !!!
	}

	/** Returns the square root of the variance of this distribution.
	  */
	public double sqrt_variance()
	{
		return 0; // !!!
	}

	/** Returns an interval which contains almost all the mass of this
	  * distribution. THROWS AN EXCEPTION FOR ANY NONTRIVIAL BELIEF NETWORK !!!
	  */
	public double[] effective_support (double epsilon) throws Exception
	{
		if (belief_net.get_variables().length == 1)
            // !!! return belief_net.get_variables()[0].effective_support (epsilon);
            return null;
        else
            throw new IllegalArgumentException ("Factorized.effective_support: doesn't make sense for multidimensional distribution.");
	}

	/** Formats a string representation of this distribution.
	  */
	public String format_string (String leading_ws)
	{
		String result = "";
		result += this.getClass().getName()+"\n"+leading_ws+ "{"+"\n";

		try { result += belief_net.format_string (); }  // OOPS, CAN'T PUT leading_ws AS AN ARGUMENT !!!
        catch (RemoteException e) {} // can't happen: belief_net is not remote

		result += leading_ws+"}"+"\n";
		return result;
	}

	/** Read an instance of this distribution from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param st Stream tokenizer to read from.
	  * @throws IOException If the attempt to read the model fails.
	  */
	public void pretty_input (SmarterTokenizer st) throws IOException
	{
        st.nextToken();
        if (st.ttype != '{')
            throw new IOException ("Factorized.pretty_input: input doesn't have opening bracket.");

        try
        {
            st.nextToken();
            if (st.ttype == StreamTokenizer.TT_EOF)
                belief_net = null;
            else
            {
                Class bn_class = java.rmi.server.RMIClassLoader.loadClass (st.sval);
                belief_net = (BeliefNetwork) bn_class.newInstance();
            }
        }
        catch (ClassNotFoundException e)
        {
            throw new IOException ("Factorized.pretty_input: can't find belief network class: "+st.sval);
        }
        catch (ClassCastException e)
        {
            throw new IOException ("Factorized.pretty_input: can't load belief network: "+st.sval+" isn't a belief network class.");
        }
        catch (Exception e)
        {
            throw new IOException ("Factorized.pretty_input: can't load belief network:\n"+e);
        }
        
        if (belief_net != null) belief_net.pretty_input (st);

        st.nextToken();
        if (st.ttype == '}')
            throw new IOException ("Factorized.pretty_input: no closing bracket on input.");
	}
}
