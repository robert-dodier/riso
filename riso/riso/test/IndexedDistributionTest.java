package riso.distributions;
import java.io.*;
import java.rmi.*;
import riso.belief_nets.*;
import riso.regression.*;
import SmarterTokenizer;

public class IndexedDistributionTest
{
	public static void main( String[] args )
	{
		try
		{
			System.err.println( "bn: "+args[0]+", variable: "+args[1] );
			BeliefNetworkContext bnc = new BeliefNetworkContext(null);
			BeliefNetwork bn = (BeliefNetwork) bnc.load_network( args[0] );
			bnc.rebind(bn);
			AbstractVariable rcs = bn.name_lookup( args[1] );

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
