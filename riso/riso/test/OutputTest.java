package riso.approximation;
import java.io.*;
import riso.distributions.*;
import numerical.*;
import SmarterTokenizer;

public class OutputTest
{
	public static void main( String[] args )
	{
		try
		{
			int i;
			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );

			System.err.print( "give name of file describing distribution: " );
			st.nextToken();
			FileInputStream fis = new FileInputStream( st.sval );
			SmarterTokenizer p_st = new SmarterTokenizer( new BufferedReader( new InputStreamReader( fis ) ) );
			Distribution p = null;

			try 
			{
				p_st.nextToken();
				Class p_class = java.rmi.server.RMIClassLoader.loadClass( p_st.sval );
				p = (Distribution) p_class.newInstance();
				p_st.nextBlock();
				p.parse_string( p_st.sval );
			}
			catch (Exception e)
			{
				System.err.println( "OutputTest.main: attempt to construct distribution failed: "+e );
				System.exit(1);
			}

			System.err.print( "give lower and upper bounds on effective support of distribution: " );
			st.nextToken();
			double a = Format.atof( st.sval );
			st.nextToken();
			double b = Format.atof( st.sval );

			double x[] = new double[1];

			for ( i = 0; i < 50; i++ )
			{
				x[0] = a + (i+0.5)*(b-a)/50;
				System.out.println( "x: "+x[0]+" p: "+p.p(x) );
			}
		}
		catch (Exception e)
		{
			System.err.println( "OutputTest.main: something went ker-blooey:" );
			e.printStackTrace();
		}

		System.exit(1);
	}
}
