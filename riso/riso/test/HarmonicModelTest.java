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
