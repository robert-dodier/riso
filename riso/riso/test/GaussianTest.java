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
import numerical.*;
import SmarterTokenizer;

public class GaussianTest
{
	public static void main( String args[] )
	{
		try
		{
			Reader r = new BufferedReader(new InputStreamReader(System.in));
			SmarterTokenizer st = new SmarterTokenizer(r);

			double eps;
			st.nextToken();
			eps = Format.atof( st.sval );

			Gaussian g = new Gaussian();
			g.pretty_input(st);

			double[] s = g.effective_support( eps );
			System.out.println( "eps: "+eps+"  effective support: "+s[0]+" -- "+s[1] );
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		System.exit(1);
	}
}
