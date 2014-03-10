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

public class MixTrain
{
	static String[] xnames;
	static int ndata, nskip;
	static Mixture mix;

	public static void main( String[] args )
	{
		int i, j, k, j0, ncomponents = 0;
		String mix_filename = null, data_filename = null;
		boolean do_initial_training = false, do_training = true;
		double delta_mix_crit = 0.001;

		for ( i = 0; i < args.length; i++ )
		{
			if ( args[i].charAt(0) != '-' )
				continue;

			switch ( args[i].charAt(1) )
			{
			case 's':
				nskip = Integer.parseInt( args[++i] );
System.err.println( "skip "+nskip+" data." );
				break;
			case 'c':
				delta_mix_crit = Double.parseDouble( args[++i] );
System.err.println( "delta_mix_crit set to "+delta_mix_crit );
				break;
			case 'x':
				j0 = ++i;
				while ( i < args.length && args[i].charAt(0) != '-' )
					++i;
				xnames = new String[ i-j0 ];
				System.err.println( "MixTrain.main: number of names: "+xnames.length );
				for ( j = j0; j < i; j++ )
					xnames[j-j0] = args[j];
				for ( j = 0; j < xnames.length; j++ )
					System.err.println( "MixTrain.main: xnames["+j+"]: "+xnames[j] );
				--i;
				break;
			case 'n':
				ndata = Integer.parseInt( args[++i] );
System.err.println( "picked up number of data:"+ndata );
				break;
			case 'd':
				data_filename = args[++i];
System.err.println( "picked up data filename: "+data_filename );
				break;
			case 'm':
				mix_filename = args[++i];
System.err.println( "picked up mix filename: "+mix_filename );
				break;
			case 'i':
				if ( args[i].equals("-it") || args[i].equals("-it+") )
					do_initial_training = true;
				else if ( args[i].equals("-it-") )
					do_initial_training = false;
				else 
					System.err.println( "MixTrain.main: unknown command line arg: "+args[i] );
				break;
			case 't':
				if ( args[i].equals("-t") || args[i].equals("-t+") )
					do_training = true;
				else if ( args[i].equals("-t-") )
					do_training = false;
				else
					System.err.println( "MixTrain.main: unknown command line arg: "+args[i] );
				break;
			case 'M':
				ncomponents = Integer.parseInt( args[++i] );
				break;
			default:
				System.err.println( "MixTrain.main: unknown command line arg: "+args[i] );
			}
		}

		Reader mixr = null;
		Reader datar = null;

		try
		{
			if ( mix_filename != null ) mixr = new FileReader( mix_filename );
			datar = new FileReader( data_filename );
		}
		catch (FileNotFoundException e)
		{
			System.err.println( "file not found: "+e );
			System.exit(1);
		}

		try
		{
			if ( mixr == null )
			{
				if ( ncomponents == 0 ) ncomponents = 3*xnames.length*xnames.length;
System.err.println( "ncomponents: "+ncomponents );
				mix = new MixGaussians( xnames.length, ncomponents );
			}
			else
			{
				mix = new Mixture();
				SmarterTokenizer mix_st = new SmarterTokenizer(mixr);
				mix_st.nextToken();	// eat classname
				mix.pretty_input(mix_st);
				mixr.close();
			}
		}
		catch (Exception e) 
		{	
			System.err.println( "error reading mix: " );
			e.printStackTrace();
			System.exit(1);
		}

		double[][] data = new double[ndata][xnames.length];


		try
		{
			// First figure out how many columns there are in the data,
			// and which columns correspond to the variables names we have.

			StreamTokenizer data_st = new SmarterTokenizer(datar);
			data_st.eolIsSignificant(true);

			int column = 0, ncolumns;
			int[] xindex = new int[ xnames.length ];
			j = k = 0;

			data_st.nextToken();
			while ( data_st.ttype != StreamTokenizer.TT_EOL )
			{
				// See if this column is one of the ones we need.

				for ( j = 0; j < xnames.length; j++ )
				{
					if ( xnames[j].equals(data_st.sval) )
					{
System.err.println( "match "+xnames[j]+" to column "+column );
						xindex[j] = column;
						break;
					}
				}

				++column;
				data_st.nextToken();
			}

			ncolumns = column;
System.err.println( "ncolumns: "+ncolumns );

			for ( i = 0; i < nskip; i++ )
				do { data_st.nextToken(); }
				while ( data_st.ttype != StreamTokenizer.TT_EOL );

			data_st.eolIsSignificant(false);

			for ( i = 0; i < ndata; i++ )
			{
				k = 0;
				for ( j = 0; j < ncolumns; j++ )
				{
					data_st.nextToken();
					for ( k = 0; k < xindex.length; k++ )
					{
						if ( j == xindex[k] )
						{
							data[i][k] = Double.parseDouble( data_st.sval );
							break;
						}

					}
				}
			}
		}
		catch (IOException e)
		{
			System.err.println( "input exception: " );
			e.printStackTrace();
			System.exit(1);
		}
	
// System.err.println( "data:" );
// Matrix.pretty_output( data, System.err, " " );

		// OK, now we have the data in hand and the initial mixture
		// distribution too. Update the mixture with the data,
		// then output the updated mixture.

		try
		{
			// Obtain initial mixture components by training each component
			// on half (randomly chosen) of the data. This gives a sensible
			// initial fit and avoids the symmetry that would result from
			// training each on on all the data.

			if ( do_initial_training )
			{
				java.util.Random rand = new java.util.Random();
				for ( i = 0; i < mix.ncomponents(); i++ )
				{
					System.err.println( "MixTrain.main: initial update for component["+i+"]:" );
					int n2 = data.length/2;
					double[][] data2 = new double[n2][];
					for ( j = 0; j < n2; j++ ) data2[j] = data[ (rand.nextInt() & 0x7fffffff) % data.length ];
					mix.components[i].update( data2, null, 50, 0.001 );
System.err.println( " >>> after initial training: <<< " );
System.err.println( mix.components[i].format_string("\t") );
				}
			}

			int niter_max = 50;
			if ( !do_training )
			{
				niter_max = 1;
				mix.component_niter_max = 0;
			}

			mix.update( data, null, niter_max, delta_mix_crit );
		}
		catch (Exception e)
		{
			System.err.println( "MixTrain.main: trying to update mix: " );
			e.printStackTrace();
			System.exit(1);
		}

		try
		{
			if ( mix_filename == null )
				mix.pretty_output( System.out, "" );
			else
			{
				FileOutputStream mix_out = new FileOutputStream( mix_filename );
				mix.pretty_output( mix_out, "" );
			}
		}
		catch (Exception e)
		{
			System.err.println( "MixTrain.main: trying to write mix:" );
			e.printStackTrace();
		}
	}
}
