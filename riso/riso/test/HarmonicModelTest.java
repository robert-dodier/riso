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
package riso.regression;
import java.io.*;
import SmarterTokenizer;

public class HarmonicModelTest
{
	public static void main( String[] args )
	{
		try
		{
			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );
			st.nextToken();	// eat class name
			HarmonicModel h = new HarmonicModel();
			h.pretty_input( st );
			System.out.print( "echo harmonic model: "+"\n"+h.format_string("") );

			double[] x = new double[1];
			for ( x[0] = 0; x[0] < 10; x[0] += 0.2 )
				System.out.println( "x: "+x[0]+"  h.F(x): "+h.F(x)[0]+"  h.dFdx(x): "+h.dFdx(x)[0][0] );
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		System.exit(0);
	}
}
