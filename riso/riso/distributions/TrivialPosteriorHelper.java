package riso.distributions;
import SeqTriple;

public class TrivialPosteriorHelper implements PosteriorHelper
{
	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely exactly one <tt>AbstractDistribution</tt>
	  * followed by one <tt>Noninformative</tt>.
	  */
	public static SeqTriple[] description()
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.AbstractDistribution", 1 );
		s[1] = new SeqTriple( "riso.distributions.Noninformative", 1 );
		return s;
	}

	public Distribution compute_posterior( Distribution pi, Distribution lambda ) throws Exception
	{
		return pi;
	}
}
