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
package riso.approximation;
import java.io.*;
import riso.distributions.*;
import numerical.*;
import SmarterTokenizer;

public class ComputesLambdaTest
{
	public static boolean debug = false;

	public static void main( String[] args )
	{
		try
		{
			int i;
			// SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );

			// System.err.print( "give mu and sigma for lognormal lambda message: " );
			// st.nextToken(); double mu = Double.parseDouble( st.sval );
			// st.nextToken(); double sigma = Double.parseDouble( st.sval );

			// System.err.print( "give alpha and beta for gamma lambda message: " );
			// st.nextToken(); double alpha = Double.parseDouble( st.sval );
			// st.nextToken();	double beta = Double.parseDouble( st.sval );

			double mu = Double.parseDouble( args[0] );
			double sigma = Double.parseDouble( args[1] );
			double alpha = Double.parseDouble( args[2] );
			double beta = Double.parseDouble( args[3] );

			System.err.println( "mu: "+mu+" sigma: "+sigma );
			System.err.println( "alpha: "+alpha+" beta: "+beta );

			Distribution[] lambda_messages = new Distribution[2];
			lambda_messages[0] = new Lognormal( mu, sigma );
			lambda_messages[1] = new Gamma( alpha, beta );

			LambdaHelper lh = LambdaHelperLoader.load_lambda_helper( lambda_messages );
			System.out.println( "ComputesLambdaTest.main: loaded lambda helper: "+lh.getClass() );

			Distribution q = lh.compute_lambda( lambda_messages );

			System.out.print( "final approximation:\n"+q.format_string("") );
		}
		catch (Exception e)
		{
			System.err.println( "ComputesLambdaTest.main: something went ker-blooey: " );
			e.printStackTrace();
		}

		System.exit(1);
	}
}
