package riso.distributions;
import java.util.*;

public class PosteriorHelperLoader
{
	public static PosteriorHelper load_posterior_helper( Distribution pi, Distribution lambda ) throws Exception
	{
		if ( lambda instanceof Noninformative )
			return new TrivialPosteriorHelper();

		Vector seq = new Vector();
		seq.addElement( pi.getClass() );
		seq.addElement( lambda.getClass() );

		Class c = PiHelperLoader.find_helper_class( seq, "posterior" );
		return (PosteriorHelper) c.newInstance();
	}
}
