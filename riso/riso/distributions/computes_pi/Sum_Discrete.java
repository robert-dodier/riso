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
package riso.distributions.computes_pi;
import java.util.*;
import riso.distributions.*;
import riso.general.*;
import riso.numerical.*;

/** @see PiHelper
  */
public class Sum_Discrete implements PiHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>Sum</tt>
	  * followed by any number of <tt>Discrete</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.Sum", 1 );
		s[1] = new SeqTriple( "riso.distributions.Discrete", -1 );
		description_array = s;
	}

    /** HMMM, I WONDER WHAT SHOULD BE DONE HERE IF NDIMS > 1 ???
      */
	public Distribution compute_pi( ConditionalDistribution py_in, Distribution[] pi_messages ) throws Exception
	{
        double[] p_convolution = null;

		for ( int i = 0; i < pi_messages.length; i++ )
		{
			Discrete p = (Discrete) pi_messages[i];
            
            if ( p_convolution == null )
                p_convolution = (double[]) p.probabilities.clone();
            else
                p_convolution = Convolve.convolve( p_convolution, p.probabilities );
		}

		Discrete pi = new Discrete();
        
        pi.probabilities = p_convolution;
        pi.dimensions = new int[1];
        pi.dimensions[0] = p_convolution.length;
        pi.ndims = 1;

        return pi;
	}
}
