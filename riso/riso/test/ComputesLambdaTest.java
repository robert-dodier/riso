package riso.approximation;
import java.io.*;
import riso.distributions.*;
import numerical.*;
import SmarterTokenizer;

public class ComputesLambdaTest
{
	public static boolean debug = false;

	public static void main( String[] args )
	{
		try
		{
			int i;
			// SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );

			// System.err.print( "give mu and sigma for lognormal lambda message: " );
			// st.nextToken(); double mu = Format.atof( st.sval );
			// st.nextToken(); double sigma = Format.atof( st.sval );

			// System.err.print( "give alpha and beta for gamma lambda message: " );
			// st.nextToken(); double alpha = Format.atof( st.sval );
			// st.nextToken();	double beta = Format.atof( st.sval );

			double mu = Format.atof( args[0] );
			double sigma = Format.atof( args[1] );
			double alpha = Format.atof( args[2] );
			double beta = Format.atof( args[3] );

			System.err.println( "mu: "+mu+" sigma: "+sigma );
			System.err.println( "alpha: "+alpha+" beta: "+beta );

			Distribution[] lambda_messages = new Distribution[2];
			lambda_messages[0] = new Lognormal( mu, sigma );
			lambda_messages[1] = new Gamma( alpha, beta );

			LambdaHelper lh = LambdaHelperLoader.load_lambda_helper( lambda_messages );
			System.out.println( "ComputesLambdaTest.main: loaded lambda helper: "+lh.getClass() );

			Distribution q = lh.compute_lambda( lambda_messages );

			System.out.print( "final approximation:\n"+q.format_string("") );
		}
		catch (Exception e)
		{
			System.err.println( "ComputesLambdaTest.main: something went ker-blooey: " );
			e.printStackTrace();
		}

		System.exit(1);
	}
}
