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
package riso.distributions;
import java.io.*;
import java.rmi.*;
import riso.belief_nets.*;
import numerical.*;
import SmarterTokenizer;

/** An instance of this class represents a first-order autoregressive (AR1) model.
  */
public class AR1 extends AbstractConditionalDistribution
{
	/** This variable represents the correlation coefficient of this model.
	  * The variable's name must contain "rho".
	  */
	AbstractVariable rho_parent = null;

	/** This variable represents the noise magnitude of this model.
	  * The variable's name must contain "sigma".
	  */
	AbstractVariable sigma_parent = null;

	/** This variable represents the previous value of the state of the autoregressive process.
	  * The variable's name must contain "prev".
	  */
	AbstractVariable prev_parent = null;

	/** This is the index of the rho-parent in the list of parents of the associated variable.
	  */
	int rho_parent_index = -1;

	/** This is the index of the sigma-parent in the list of parents of the associated variable.
	  */
	int sigma_parent_index = -1;

	/** This is the index of the previous state parent in the list of parents of the associated variable.
	  */
	int prev_parent_index = -1;

	/** Return a copy of this object. The parent references are copied (not the parent objects).
	  */
	public Object clone() throws CloneNotSupportedException
	{
		try 
		{
			AR1 copy = (AR1) this.getClass().newInstance();
			copy.rho_parent = this.rho_parent;
			copy.sigma_parent = this.sigma_parent;
			copy.prev_parent = this.prev_parent;
			return copy;
		}
		catch (Exception e) { throw new CloneNotSupportedException( this.getClass().getName()+".clone failed: "+e ); }
	}

	/** Return the number of dimensions of the child variable (always 1).
	  */
	public int ndimensions_child() { return 1; }

	/** Return the number of dimensions of the parent variables.
	  * If there is more than one parent, this is the sum of the dimensions
	  * of the parent variables -- since there are always 3 parents, this method always returns 3.
	  */
	public int ndimensions_parent() { return 3; }

	/** For a given value <code>c</code> of the parents, return a distribution
	  * which represents <code>p(x|C=c)</code>. Executing <code>get_density(c).
	  * p(x)</code> will yield the same result as <code>p(x,c)</code>.
	  */
	public Distribution get_density( double[] c ) throws Exception
	{
		double rho = c[rho_parent_index], sigma = c[sigma_parent_index], x_prev = c[prev_parent_index];
		return new Gaussian( rho*x_prev, sigma );
	}

	/** Compute the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density.
	  * @param c Values of parent variables.
	  */
	public double p( double[] x, double[] c ) throws Exception
	{
		double rho = c[rho_parent_index], sigma = c[sigma_parent_index], x_prev = c[prev_parent_index];
		return Gaussian.g1( x[0], rho*x_prev, sigma );
	}

	/** Return an instance of a random variable from this distribution.
	  * @param c Parent variables.
	  */
	public double[] random( double[] c ) throws Exception
	{
		return get_density(c).random();
	}

	/** This method calls <tt>pretty_input</tt>.
	  */
	public void parse_string( String description ) throws IOException
	{
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( description ) );
		pretty_input( st );
	}

	/** This method prints, as comments, the names of the variables which represent the parameters
	  * of this model.
	  * @param leading_ws Leading whitespace string. This is written at
	  *   the beginning of each line of output. Indents are produced by
	  *   appending more whitespace.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		int i, j;
		String result = "", more_ws = leading_ws+"\t", still_more_ws = leading_ws+"\t\t";

		result += this.getClass().getName()+"\n"+leading_ws+"{"+"\n";

		// Should catch RemoteException from get_fullname() and print "(unreachable)" in that case. !!!
		result += more_ws+"% rho parent: "+(rho_parent==null?"(null)":rho_parent.get_fullname())+"\n";
		result += more_ws+"% sigma parent: "+(sigma_parent==null?"(null)":sigma_parent.get_fullname())+"\n";
		result += more_ws+"% prev parent: "+(prev_parent==null?"(null)":prev_parent.get_fullname())+"\n";

		result += leading_ws+"}\n";
		return result;
	}

	/** Since an <tt>AR1</tt> has no parameters, this method simple eats two squiggly braces,
	  * <tt>"{ }"</tt>.  
	  * (The parents which represent the correlation coefficient, the noise magnitude, and the
	  * previous state are specified in the <tt>parents</tt> list of the variable associated with
	  * this <tt>AR1</tt> model.)
	  * @param st Input stream to read from.
	  * @throws IOException If there is a problem reading the input stream.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		// Should check to see these tokens are actually braces. !!!
		st.nextToken();
		st.nextToken();
	}

	/** Figure out which parent is which, in the list of parents of the associated variable.
	  * The name of the rho-parent must contain "rho", the name of the sigma-parent must contain
	  * "sigma", and the name of the previous state parent must contain "prev".
	  */
	public void assign_parents() throws Exception
	{
		if ( associated_variable == null ) throw new Exception( "AR1.assign_parents: associated_variable is null." );
		
		String[] names = associated_variable.get_parents_names();
		AbstractVariable[] parents = associated_variable.get_parents();

		for ( int i = 0; i < names.length; i++ )
			if ( names[i].startsWith("rho") || names[i].endsWith("rho") )
			{
				rho_parent_index = i;
				rho_parent = parents[i];
			}
			else if ( names[i].startsWith("sigma") || names[i].endsWith("sigma") )
			{
				sigma_parent_index = i;
				sigma_parent = parents[i];
			}
			else if ( names[i].startsWith("prev") || names[i].endsWith("prev") )
			{
				prev_parent_index = i;
				prev_parent = parents[i];
			}
			else
			{
				throw new Exception( "AR1.assign_parents: what am I to do with "+names[i]+" ??" );
			}
	}

	/** Create an AR1 model and write it out.
	  */
	public static void main( String[] args )
	{
		try
		{
			System.err.println( "bn: "+args[0]+", variable: "+args[1] );
			BeliefNetworkContext bnc = new BeliefNetworkContext(null);
			AbstractBeliefNetwork bn = (AbstractBeliefNetwork) bnc.get_reference( NameInfo.parse_beliefnetwork(args[0],bnc) );
			AbstractVariable x_ar = (AbstractVariable) bn.name_lookup( args[1] );

			AR1 ar1 = (AR1) x_ar.get_distribution();
			ar1.assign_parents();

			System.err.println( "ar1: "+ar1 );
		}
		catch (Exception e) { e.printStackTrace(); }
	}
}
