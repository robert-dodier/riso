import java.io.*;
import java.rmi.*;
import riso.distributions.*;
import numerical.*;

public class MixTrain
{
	static String[] xnames;
	static int ndata, nskip;
	static Mixture mix;

	static double lambda_in = 10;
	static double lambda_out = 0.1;

	public static void main( String[] args )
	{
		int i, j, k, j0;
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
				nskip = Format.atoi( args[++i] );
System.err.println( "skip "+nskip+" data." );
				break;
			case 'c':
				delta_mix_crit = Format.atof( args[++i] );
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
System.err.println( "finish w/ -x; i: "+i+" args[i]: "+args[i] );
				--i;
				break;
			case 'l':
				switch ( args[i].charAt(2) )
				{
				case 'i':
					lambda_in = Format.atof( args[++i] );
					break;
				case 'o':
					lambda_out = Format.atof( args[++i] );
					break;
				}
				break;
			case 'n':
				ndata = Format.atoi( args[++i] );
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
			default:
				System.err.println( "MixTrain.main: unknown command line arg: "+args[i] );
			}
		}

		System.err.println( "lambda_in: "+lambda_in+"  lambda_out: "+lambda_out );

		Reader mixr = null;
		Reader datar = null;

		try
		{
			mixr = new FileReader( mix_filename );
			datar = new FileReader( data_filename );
		}
		catch (FileNotFoundException e)
		{
			System.err.println( "file not found: "+e );
			System.exit(1);
		}

		SmarterTokenizer mix_st = new SmarterTokenizer(mixr);

		try
		{
			mix = new Mixture();
			mix_st.nextToken();	// eat classname
			mix.pretty_input(mix_st);
			mixr.close();
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
							data[i][k] = Format.atof( data_st.sval );
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
			// Obtain initial mixture components by training each component on all
			// of the data. This works OK as long as the models can't become the
			// same by training, as is the case with reparametrization of regression
			// models; otherwise it might be necessary to break the symmetry, as is
			// the case with Gaussian mixtures.

			if ( do_initial_training )
			{
				for ( i = 0; i < mix.ncomponents(); i++ )
				{
					System.err.println( "MixTrain.main: initial update for component["+i+"]:" );
					mix.components[i].update( data, null, 50, 0.001 );
// System.err.println( " >>> after initial training: <<< " );
// mix.components[i].pretty_output( System.err, "-+-+-" );
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
			FileOutputStream mix_out = new FileOutputStream( mix_filename );
			mix.pretty_output( mix_out, "" );
		}
		catch (Exception e)
		{
			System.err.println( "MixTrain.main: trying to write mix:" );
			e.printStackTrace();
		}
	}
}
