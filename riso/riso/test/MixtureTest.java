package riso.distributions;
import java.io.*;
import java.rmi.*;
import numerical.*;
import SmarterTokenizer;

public class MixtureTest
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

			Mixture m = new Mixture();
			m.pretty_input(st);

			double[] s = m.effective_support( eps );
			System.out.println( "eps: "+eps+"  effective support: "+s[0]+" -- "+s[1] );
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		System.exit(1);
	}
}
