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
import numerical.*;

public class SupportTest
{
	public static void main( String[] args )
	{
		double scale = 1, tolerance = 0.99;

		if ( args.length > 0 )
		{	
			tolerance = Format.atof( args[0] );
			if ( args.length > 1 )
				scale = Format.atof( args[1] );
		}

		System.err.println( "SupportTest: scale: "+scale+"  tolerance: "+tolerance );

		try
		{
			SomeFunction f = new SomeFunction();
			double[] support = Intervals.effective_support( f, scale, tolerance );
			System.err.println( "SupportTest: support: "+support[0]+", "+support[1] );
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}

class SomeFunction implements Callback_1d
{
	public double f( double x )
	{
		double absx = Math.abs(x), absx2 = Math.abs( x-100 );
		return Math.exp( -absx ) + Math.exp( -absx2 );
	}
}

