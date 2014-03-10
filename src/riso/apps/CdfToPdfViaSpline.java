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
import riso.distributions.*;
import riso.numerical.*;
import riso.general.*;

public class CdfToPdfViaSpline
{
	public static void main( String[] args )
	{
		int n = 256;

		for ( int i = 0; i < args.length; i++ )
		{
			switch (args[i].charAt(1))
			{
			case 'n':
				n = Integer.parseInt(args[++i]);
				break;
			}
		}

		try
		{
			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader(System.in) );
			double[] x = new double[n], cdfx = new double[n];

			for ( int i = 0; i < n; i++ )
			{
				st.nextToken(); 
				x[i] = Double.parseDouble(st.sval);
				st.nextToken();
				cdfx[i] = Double.parseDouble(st.sval);
			}

			MonotoneSpline ms = new MonotoneSpline( x, cdfx );

			SplineDensity sd = new SplineDensity( ms.x, ms.d );
			System.err.println( sd.format_string("") );
		}
		catch (Exception e) { e.printStackTrace(); }
	}
}
