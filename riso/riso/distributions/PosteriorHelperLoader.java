package risotto.distributions;
import java.util.*;

public class PosteriorHelperLoader
{
	public static PosteriorHelper load_posterior_helper( Distribution pi, Distribution lambda )
	{
		if ( lambda instanceof Noninformative )
			return new TrivialPosteriorHelper();

		String class_name;

		class_name = pi.getClass().getName();
		String pi_name = class_name.substring( class_name.lastIndexOf(".")+1 );
		class_name = lambda.getClass().getName();
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
			return null;
		}
	}
}
