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
package riso.render;
import java.io.*;
import java.rmi.*;
import riso.belief_nets.*;
import riso.distributions.*;
import riso.numerical.*;
import riso.general.*;

/** This class implements a renderer which simply prints a list of values.
  */
public class TextRenderer implements RenderDistribution
{
	public int npoints = 50;
	public PrintStream ps;

	public void do_render( Distribution q, boolean print_cdf ) throws Exception
	{
		if ( q.ndimensions() > 1 )
			throw new IllegalArgumentException( "TextRenderer.do_render: "+q.ndimensions()+" is too many dimensions." );

		double[] support = q.effective_support( 1e-6 ), x = new double[1];

		if ( q instanceof Discrete )
		{
			for ( int i = 0; i <= (int)support[1]; i++ )
			{	
				x[0] = i;

				if ( print_cdf )
					ps.println( i+"  "+q.cdf(x[0]) );
				else
					ps.println( i+"  "+q.p(x) );
			}
		}
		else
		{
			double dx = (support[1] - support[0])/npoints;
			for ( int i = 0; i < npoints; i++ )
			{
				x[0] = support[0] + (i+0.5)*dx;

				if ( print_cdf )
					ps.println( x[0]+"  "+q.cdf(x[0]) );
				else
					ps.println( x[0]+"  "+q.p(x) );
			}
		}
	}

	public static void main( String[] args )
	{
		AbstractBeliefNetwork bn = null;
		AbstractVariable x = null;
		String description_filename = "", which = "posterior";
		int npoints = 50, which_index = 0;
		boolean print_cdf = false;	// print the density by default.

		try
		{
			for ( int i = 0; i < args.length; i++ )
			{
				switch ( args[i].charAt(1) )
				{
				case 'f':
					description_filename = args[++i];
					break;
				case 'n':
					npoints = Integer.parseInt( args[++i] );
					break;
				case 'b':
					String url = "rmi://"+args[++i];
					System.err.println( "TextRenderer: url: "+url );
					bn = (AbstractBeliefNetwork) Naming.lookup( url );
					break;
				case 'v':
					x = (AbstractVariable) bn.name_lookup(args[++i]);
					System.err.println( "TextRenderer: obtained reference to variable "+x.get_fullname() );
					break;
				case 'p':
					which = args[++i];
					if ( "pi-message".equals(which) || "lambda-message".equals(which) )
					{
						which_index = Integer.parseInt(args[++i]);
						System.err.println( "which_index: "+which_index );
					}
					break;
				case 'c':
					print_cdf = true;
					break;
				}
			}

			AbstractDistribution q = null;

			if ( x == null || bn == null )
			{
				FileInputStream fis = new FileInputStream( description_filename );
				SmarterTokenizer st = new SmarterTokenizer( new BufferedReader( new InputStreamReader( fis ) ) );
			
				st.nextToken();
				System.err.println( "TextRenderer: distribution class: "+st.sval );

				q = (AbstractDistribution) java.rmi.server.RMIClassLoader.loadClass( st.sval ).newInstance();
				q.pretty_input( st );
			}
			else
			{
				if ( "prior".equals(which) )
					q = (AbstractDistribution) bn.get_prior(x);
				else if ( "posterior".equals(which) )
					q = (AbstractDistribution) bn.get_posterior(x);
				else if ( "pi".equals(which) )
					q = (AbstractDistribution) x.get_pi();
				else if ( "lambda".equals(which) )
					q = (AbstractDistribution) x.get_lambda();
				else if ( "pi-message".equals(which) )
					q = (AbstractDistribution) x.get_pi_messages()[which_index];
				else if ( "lambda-message".equals(which) )
					q = (AbstractDistribution) x.get_lambda_messages()[which_index];
				else
				{
					System.err.println( "which: "+which+" ???" );
					System.exit(1);
				}
			}

			TextRenderer tr = new TextRenderer();
			tr.npoints = npoints;
			tr.ps = System.out;

			tr.do_render( q, print_cdf );

			System.exit(1);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
}
