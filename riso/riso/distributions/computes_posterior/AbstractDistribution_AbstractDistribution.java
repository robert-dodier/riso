package riso.distributions.computes_posterior;
import riso.distributions.*;
import riso.approximation.*;
import SeqTriple;

public class AbstractDistribution_AbstractDistribution extends AbstractPosteriorHelper
{
	/** Returns a description of the sequences of distributions 
	  * accepted by this helper -- namely, two <tt>AbstractDistribution</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[1];
		s[0] = new SeqTriple( "riso.distributions.AbstractDistribution", 2 );
		description_array = s;
	}

	public Distribution compute_posterior( Distribution pi, Distribution lambda ) throws Exception
	{
		Distribution[] p_l = new Distribution[2];
		p_l[0] = pi;
		p_l[1] = lambda;
		DistributionProduct dp = new DistributionProduct( false, pi instanceof Discrete,  p_l );

		SplineDensity q = SplineApproximation.do_approximation( (Distribution)dp, dp.support );

		return q;
	}
}
