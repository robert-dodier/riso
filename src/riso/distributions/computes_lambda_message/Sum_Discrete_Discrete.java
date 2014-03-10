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
package riso.distributions.computes_lambda_message;
import java.util.*;
import riso.distributions.*;
import riso.numerical.*;
import riso.general.*;

public class Sum_Discrete_Discrete implements LambdaMessageHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>Sum</tt>
	  * followed by one <tt>Discrete</tt>, followed by any number of
	  * <tt>Discrete</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[3];
		s[0] = new SeqTriple( "riso.distributions.Sum", 1 );
		s[1] = new SeqTriple( "riso.distributions.Discrete", 1 );
		s[2] = new SeqTriple( "riso.distributions.Discrete", -1 );
		description_array = s;
	}

	public Distribution compute_lambda_message( ConditionalDistribution cpd, Distribution lambda_in, Distribution[] pi_messages ) throws Exception
	{
        double[] pi_convolution = null;

		for ( int i = 0; i < pi_messages.length; i++ )
		{
            if (pi_messages[i] == null) continue;

			Discrete p = (Discrete) pi_messages[i];
            
            if ( pi_convolution == null )
                pi_convolution = (double[]) p.probabilities.clone();
            else
                pi_convolution = Convolve.convolve( pi_convolution, p.probabilities );
		}

        Discrete lambda = (Discrete) lambda_in;
        double[] lambda_pi_correlation = discrete_correlation (lambda.probabilities, pi_convolution);

        Discrete lambda_message = new Discrete ();

        int recipient_cardinality = lambda.probabilities.length - pi_convolution.length +1;

        lambda_message.probabilities = new double [recipient_cardinality];
        System.arraycopy (lambda_pi_correlation, pi_convolution.length-1, lambda_message.probabilities, 0, recipient_cardinality);
        lambda_message.dimensions = new int [1];
        lambda_message.dimensions[0] = recipient_cardinality;
        lambda_message.ndims = 1;

		return lambda_message;
	}

    /** Computes the discrete correlation of <tt>p</tt> and <tt>q</tt>, 
      * defined as <tt>reverse (convolution (reverse (p), q))</tt>.
      * This function can move to <tt>riso.numerical.Convolve</tt> after testing.
      */
    double[] discrete_correlation (double[] p, double[] q)
    {
        double[] rev_p = reverse (p);
        double[] conv_rev_p_q = Convolve.convolve (rev_p, q);
        return reverse (conv_rev_p_q);
    }

    /** Returns the argument, with order of elements reversed.
      * A new object is allocated -- reversal is not in-place.
      */
    double[] reverse (double[] x)
    {
        double[] y = new double [x.length];
        for (int i = 0; i < x.length; i++)
            y[i] = x [x.length-1 - i];

        return y;
    }
}
