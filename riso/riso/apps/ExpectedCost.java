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
package riso.apps;
import java.io.*;
import java.rmi.*;
import riso.belief_nets.*;
import riso.distributions.*;
import riso.numerical.*;
import riso.general.*;

public class ExpectedCost
{
	public static void main( String[] args )
	{
		AbstractBeliefNetwork bn = null;
		AbstractVariable x = null;
		String schedule_filename = "";

		try
		{
			for ( int i = 0; i < args.length; i++ )
			{
				switch ( args[i].charAt(1) )
				{
				case 's':
					schedule_filename = args[++i];
					break;
				case 'b':
					String url = "rmi://"+args[++i];
					System.err.println( "ExpectedCost: url: "+url );
					bn = (AbstractBeliefNetwork) Naming.lookup( url );
					break;
				case 'x':
					x = (AbstractVariable) bn.name_lookup(args[++i]);
					System.err.println( "ExpectedCost: obtained reference to variable "+x.get_fullname() );
					break;
				}
			}

			FileInputStream fis = new FileInputStream( schedule_filename );
			SmarterTokenizer st = new SmarterTokenizer( new BufferedReader( new InputStreamReader(fis) ) );

			st.nextToken();
			int N = Integer.parseInt(st.sval);
			double[] alpha = new double[N], beta = new double[N];
			for ( int i = 0; i < N; i++ )
			{
				st.nextToken();
				alpha[i] = Double.parseDouble(st.sval);
				st.nextToken();
				beta[i] = Double.parseDouble(st.sval);
			}

			AbstractDistribution posterior = (AbstractDistribution) bn.get_posterior(x);

			System.out.println( "expected cost: "+expected_cost( alpha, beta, posterior ) );
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static double expected_cost( double[] alpha, double[] beta, Distribution posterior ) throws Exception
	{
		int N = alpha.length;
		double total = 0;

		qk21_IntegralHelper1d ih = new qk21_IntegralHelper1d( new ExpectedCostIntegrand(posterior), new double[1][2], false );

		for ( int k = 0; k < N-1; k++ )
		{
			ih.a[0] = alpha[k];
			ih.b[0] = alpha[k+1];
System.err.println( "a["+k+"], b["+k+"]: "+ih.a[0]+", "+ih.b[0]+"; I(1-F): "+ih.do_integral()+"; beta["+k+"]: "+beta[k]+"; term: "+beta[k]*ih.do_integral() );
			total += beta[k] * ih.do_integral();
		}

		ih.a[0] = alpha[N-1];
		ih.b[0] = posterior.effective_support(1e-4)[1];
System.err.println( "a["+(N-1)+"], b["+(N-1)+"]: "+ih.a[0]+", "+ih.b[0]+"; I(1-F): "+ih.do_integral()+"; beta["+(N-1)+"]: "+beta[N-1]+"; term: "+beta[N-1]*ih.do_integral() );
		if ( ih.a[0] < ih.b[0] )
			total += beta[N-1] * ih.do_integral();

System.err.println( "total: "+total );
		return total;
	}
}

class ExpectedCostIntegrand implements Callback_1d
{
	Distribution p;

	ExpectedCostIntegrand( Distribution p ) { this.p = p; }

	public double f( double x ) throws Exception
	{
		return 1 - p.cdf(x);
	}
}
