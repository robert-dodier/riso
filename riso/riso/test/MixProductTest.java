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
import SmarterTokenizer;

public class MixProductTest
{
	public static void main( String[] args )
	{
		try
		{
			MixGaussians[] mixtures = new MixGaussians[ args.length ];
			
			for ( int i = 0; i < args.length; i++ )
			{
				FileInputStream fis = new FileInputStream( args[i] );
				SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( fis ) );

				mixtures[i] = new MixGaussians();
				st.nextToken();	// eat class name
				mixtures[i].pretty_input( st );
			}

			MixGaussians product = MixGaussians.product_mixture( mixtures );

			System.err.println( "product:\n"+product.format_string("") );

			double[] support = product.effective_support( 1e-6 );
			double[] x = new double[1];
			double x0 = support[0], x1 = support[1], dx = (x1-x0)/100;

			System.err.println( "x"+"\t"+"product.p(x)" );
			for ( x[0] = x0+dx/2; x[0] < x1; x[0] += dx )
				System.err.println( x[0]+" "+product.p(x) );
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		System.exit(0);
	}
}
