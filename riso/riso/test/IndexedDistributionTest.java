package riso.distributions;
import java.io.*;
import riso.belief_nets.*;
import riso.regression.*;
import SmarterTokenizer;

public class IndexedDistributionTest
{
	public static void main( String[] args )
	{
		try
		{
			FileInputStream fis = new FileInputStream( args[0] );
			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( fis ) );
			st.nextToken();		// eat the class name

			BeliefNetwork bn = new BeliefNetwork();
			bn.pretty_input( st );

			// THE REST OF THE CODE IS VERY SPECIFIC TO THE RADAR CROSS
			// SECTION PROBLEM !!!

			Variable rcs = (Variable) bn.name_lookup( "RCS" );

			IndexedDistribution id = (IndexedDistribution) rcs.get_distribution();
			id.assign_indexes();
			id.parse_components_string();

			double[] x = new double[1];

			for ( double theta = 0; theta < 6.28; theta += 0.1 )
			{
				System.out.print( theta+"\t" );
				
				x[0] = theta;
				for ( int i = 0; i < 3; i++ )
				{
					RegressionModel rm = ((RegressionDensity)id.components[i]).regression_model;
					System.out.print( rm.F(x)[0]+"  " );
				}
				System.out.println("");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		System.exit(0);
	}
}
