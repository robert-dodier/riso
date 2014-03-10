/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 2004, Robert Dodier.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of version 2 of the GNU General Public License as
 * published by the Free Software Foundation.
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
package riso.regression;

import java.io.*;
import java.util.*;
import riso.numerical.*;
import riso.general.*;

public class SquashingNetworkBag extends SquashingNetwork
{
    SquashingNetwork[] bag;

    public SquashingNetworkBag() {}

    public SquashingNetworkBag( int nbag, SquashingNetwork net ) throws CloneNotSupportedException
    {
        bag = new SquashingNetwork[nbag];

        for ( int i = 0; i < nbag; i++ )
            bag[i] = (SquashingNetwork) net.clone();
    }

    public void randomize_weights()
    {
        for ( int m = 0; m < bag.length; m++ )
            bag[m].randomize_weights();
    }

	public double[] F( double[] x ) throws Exception
    {
        double[] y = new double[1];     // ASSUME 1 OUTPUT HERE !!!

        for ( int m = 0; m < bag.length; m++ )
        {
            y[0] += bag[m].F(x)[0]/bag.length;
        }

        return y;
    }

	public double update( double[][] x, double[][] y, int niter_max, double stopping_criterion, double[] responsibility ) throws Exception
    {
        // Construct bootstrap training samples.
        // Assume each network in the bag gets all negative examples
        // and positive examples resampled to yield the specified base rate.
        // Assume all networks get the same base rate.

        if ( y.length > 0 && y[0].length > 1 )
            throw new IllegalArgumentException( "SquashingNetworkBag.update: don't know how to handle multiple targets." );

        if ( responsibility != null )
            throw new IllegalArgumentException( "SquashingNetworkBag.update: don't know how to handle responsibility here." );

        int n = y.length;
        int n1 = 0;

        for ( int i = 0; i < n; i++ )
            // Assume a positive example if target > 0. 
            // Could be more precise.
            n1 += y[i][0] > 0 ? 1 : 0;

        int n0 = n -n1;

        double[][] x1 = new double[n1][];
        double[][] y1 = new double[n1][];
        double[][] x0 = new double[n0][];
        double[][] y0 = new double[n0][];

        int i0 = 0, i1 = 0;
        for ( int i = 0; i < n; i++ )
        {
            if ( y[i][0] > 0 )
            {
                x1[i1] = x[i];
                y1[i1] = y[i];
                i1++;
            }
            else
            {
                x0[i0] = x[i];
                y0[i0] = y[i];
                i0++;
            }
        }

        System.err.println( "SquashingNetworkBag.update: n0, n1: "+n0+", "+n1+"; train "+bag.length+" networks" );

        double FOM = 0;

        for ( int m = 0; m < bag.length; m++ )
        {
            // We want: base_rate == n1_resampled/nn == n1_resampled/(n0 +n1_resampled)
            // i.e., n0/n1_resampled == 1/base_rate -1, i.e., n1_resampled == n0/(1/base_rate -1).

            double base_rate = 0.25;    // COULD BE A PARAMETER !!!
            int n1_resampled = (int) (n0/(1/base_rate -1));
            int nn = n0 +n1_resampled;

            System.err.println( "SquashingNetworkBag.update: train bag["+m+"] with "+n0+" negative and "+n1_resampled+" positive, to get base_rate "+base_rate );

            double[][] xx = new double[nn][];
            double[][] yy = new double[nn][];

            for ( int i = 0; i < n0; i++ )
            {
                xx[i] = x0[i];
                yy[i] = y0[i];
            }

            for ( int i = 0; i < n1_resampled; i++ )
            {
                i1 = (rng.nextInt() & 0x7fffffff) % n1;
                xx[i+n0] = x1[i1];
                yy[i+n0] = y1[i1];
            }

            FOM += bag[m].update( xx, yy, niter_max, stopping_criterion, null ) / bag.length;
        }

        return FOM;
    }

	public Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException("SquashingNetworkBag");
    }

	public double[][] dFdx( double[] x ) throws Exception
    {
        throw new Exception("SquashingNetworkBag");
    }

	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		boolean found_closing_bracket = false;

        int m = 0;

		try
		{
			st.nextToken();
			if ( st.ttype != '{' )
				throw new IOException( "SquashingNetworkBag.pretty_input: input doesn't have opening bracket; tokenizer state: "+st );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == '}' )
				{
					found_closing_bracket = true;
					break;
				}
                else if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "nbag" ) )
                {
                    st.nextToken();
                    int nbag = Integer.parseInt( st.sval );
                    bag = new SquashingNetwork[nbag];
                    System.err.println( "SquashingNetworkBag.pretty_input: allocate "+nbag+" models." );
                }
                else
                {
                    System.err.println( "SquashingNetworkBag.pretty_input: load bag["+m+"]; type: "+st.sval );
                    bag[m] = (SquashingNetwork) Class.forName(st.sval).newInstance();
                    bag[m].pretty_input(st);
                    m++;
                }
			}
		}
		catch (Exception e)
		{
			throw new IOException( "SquashingNetworkBag.pretty_input: attempt to read description failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "SquashingNetworkBag.pretty_input: no closing bracket on input; tokenizer state: "+st );
	}

	public String format_string( String leading_ws ) throws IOException
    {
		String result = "";

		result += this.getClass().getName()+"\n"+leading_ws+"{"+"\n";
		String more_leading_ws = leading_ws+"\t";

        result += more_leading_ws+"nbag "+bag.length+"\n";

        for ( int m = 0; m < bag.length; m++ )
            result += more_leading_ws+bag[m].format_string( more_leading_ws )+"\n";

		result += leading_ws+"}"+"\n";
		return result;
    }

	public int ndimensions_in()
    {
        if ( bag.length > 0 )
            return bag[0].ndimensions_in();

        return 0;
    }

	public int ndimensions_out()
    {
        if ( bag.length > 0 )
            return bag[0].ndimensions_out();

        return 0;
    }

    boolean maybe_rescale_output( double[][] Y )
    {
        if ( bag.length > 0 )
            return bag[0].maybe_rescale_output( Y );
        
        return false;
    }
}
