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
import SmarterTokenizer;

/** An object of this class represents an or gate. The output is 1 if any input is 1,
  * and the output is 0 if all inputs are 0. This class is implemented as a special
  * case of <tt>NoisyOrGate</tt> with the leak probability equal to 0.
  */
public class OrGate extends NoisyOrGate 
{
	/** Default constructor for an or gate.
	  */
	public OrGate() { p_leak = 0; }

	/** This constructor specifies the number of inputs for the or gate.
	  */
	public OrGate( int ninputs_in ) { ninputs = ninputs_in; }

	/** Return a deep copy of this object. If this object is remote,
	  * <tt>clone</tt> will create a new remote object.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		OrGate copy = new OrGate();
		copy.associated_variable = this.associated_variable;
		copy.ninputs = this.ninputs;
		return copy;
	}

	/** Create a description of this or gate as a string.
	  * If this distribution is associated with a variable, the number of inputs
	  * is not into the output string.
	  * @param leading_ws This argument is ignored.
	  */
	public String format_string( String leading_ws ) throws IOException
	{
		String result = this.getClass()+" { ";
		if ( associated_variable == null )
			result += "ninputs "+ninputs+" ";
		result += "}\n";
		return result;
	}
}
