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
import java.rmi.server.*;
import riso.general.*;

/** Abstract base class for conditional distributions.
  * This class implements only a few methods; most of the methods from the
  * <tt>ConditionalDistribution</tt> interface are not implemented,
  * and so must be provided by subclasses.
  *
  * <p> This classs is helpful in part because message-passing algorithms can be
  * formulated as generic for all conditional distributions -- handlers
  * are named only by classes, not by interfaces.
  */
public abstract class AbstractConditionalDistribution implements ConditionalDistribution, Serializable
{
	/** This conditional distribution is associated with the belief network variable <tt>associated_variable</tt>.
	  * This reference is necessary for some distributions, and generally useful for debugging.
	  * The declared type is <tt>Object</tt> in order to make it possible to compile the base
	  * classes of the <tt>riso.distributions</tt> package before <tt>riso.belief_nets</tt>;
	  * this reference has to be cast to <tt>riso.belief_nets.Variable</tt> or to
	  * <tt>riso.belief_nets.AbstractVariable</tt> in order to do anything interesting with it.
	  */
	public Object associated_variable;

	/** Return -1, which is appropriate for all continuous distributions.
	  */
	public int get_nstates() { return -1; }

	/** Cache a reference to the variable with which this conditional distribution
	  * is associated.
	  */
	public void set_variable( Object x ) { associated_variable = x; }

	/** Return a copy of this conditional distribution.
	  * This implementation copies the <tt>associated_variable</tt> reference (i.e., the
	  * reference is not cloned).
	  * A subclass <tt>clone</tt> method should call this one.
	  * The method should look like this:
	  * <pre>
	  * public Object clone() throws CloneNotSupportedException
	  * {
	  *     Subclass copy = (Subclass) super.clone();
	  *	[subclass-specific code goes here]
	  *     return copy;
	  * }
	  * </pre>
	  */
	public Object clone() throws CloneNotSupportedException 
	{
		try
		{
			AbstractConditionalDistribution copy = (AbstractConditionalDistribution) this.getClass().newInstance();
			copy.associated_variable = this.associated_variable;
			return copy;
		}
		catch (Exception e) { throw new CloneNotSupportedException( this.getClass().getName()+".clone failed; nested: "+e ); }
	}


	/** Parse a string containing a description of an instance of this distribution.
	  * The description is contained within curly braces, which are included in the string.
	  *
	  * <p> This method forms a tokenizer from the string and passes off the job to
	  * <tt>pretty_input</tt>. 
	  * This implementation is sufficient for many derived classes.
	  */
	public void parse_string( String description ) throws IOException
	{
		SmarterTokenizer st = new SmarterTokenizer( new StringReader( description ) );
		pretty_input( st );
	}

	/** Read an instance of this distribution from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  *
	  * <p> The implementation of <tt>pretty_input</tt> in this class does nothing;
	  * this is sufficient for several distribution types which have no parameters
	  * or other descriptive information.
	  *
	  * @param st Stream tokenizer to read from.
	  * @throws IOException If the attempt to read the model fails.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException { return; }

	/** Write an instance of this distribution to an output stream.
	  * This method forms a <tt>PrintStream</tt> from the <tt>OutputStream</tt> given,
	  * and passess off the job to <tt>format_string</tt>. 
	  * This implementation is sufficient for many derived classes.
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

	/** Return a string representation of this object.
	  */
	public String toString()
	{
		try { return format_string(""); }
		catch (IOException e) { return this.getClass().getName()+".toString failed; "+e; }
	}
}
