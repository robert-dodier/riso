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
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		System.exit(0);
	}
}
