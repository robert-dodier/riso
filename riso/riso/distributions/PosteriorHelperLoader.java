package risotto.distributions;
import java.util.*;

public class PosteriorHelperLoader
{
	public static PosteriorHelper load_posterior_helper( Distribution pi, Distribution lambda )
	{
		if ( lambda instanceof Noninformative )
			return new TrivialPosteriorHelper();

		Vector pi_classes = PiHelperLoader.get_local_superclasses( pi );
		Vector lambda_classes = PiHelperLoader.get_local_superclasses( lambda );

		for ( Enumeration enum = pi_classes.elements(); enum.hasMoreElements(); )
		{
			String class_name = ((Class)enum.nextElement()).getName();
			String pi_name = class_name.substring( class_name.lastIndexOf(".")+1 );

			for ( Enumeration enum2 = lambda_classes.elements(); enum2.hasMoreElements(); )
			{
				class_name = ((Class)enum2.nextElement()).getName();
				String lambda_name = class_name.substring( class_name.lastIndexOf(".")+1 );

				String helper_name = "risotto.distributions.ComputesPosterior_"+pi_name+"_"+lambda_name;

				try
				{
					Class helper_class = Class.forName( helper_name );
					return (PosteriorHelper) helper_class.newInstance();
				}
				catch (Exception e)
				{
System.err.println( "PosteriorHelperLoader.load_posterior_helper: helper not found:" );
System.err.println( "  "+helper_name );
				}
			}
		}

		// If we fall out here, we were unable to locate an appropriate helper.
		return null;
	}
}
