package riso.distributions;
import java.io.*;
import SmarterTokenizer;

public class MixProductTest
{
	public static void main( String[] args )
	{
		try
		{
			MixGaussians[] mixtures = new MixGaussians[ args.length ];
			
			for ( int i = 0; i < args.length; i++ )
			{
				FileInputStream fis = new FileInputStream( args[i] );
				SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( fis ) );

				mixtures[i] = new MixGaussians();
				st.nextToken();	// eat class name
				mixtures[i].pretty_input( st );
			}

			MixGaussians product = MixGaussians.product_mixture( mixtures );

			System.err.println( "product:\n"+product.format_string("") );

			double[] support = product.effective_support( 1e-6 );
			double[] x = new double[1];
			double x0 = support[0], x1 = support[1], dx = (x1-x0)/100;

			for ( x[0] = x0+dx/2; x[0] < x1; x[0] += dx )
			{
				double prodp = 1;
				for ( int j = 0; j < mixtures.length; j++ )
					prodp *= mixtures[j].p( x );
				System.err.println( "x: "+x[0]+" prodp: "+prodp+" product.p: "+product.p(x) );
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		System.exit(0);
	}
}
