package riso.distributions;
import SeqTriple;

public class TrivialPiHelper implements PiHelper
{
	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely exactly one <tt>AbstractDistribution</tt>.
	  */
	public static SeqTriple[] description()
	{
		SeqTriple[] s = new SeqTriple[1];
		s[0] = new SeqTriple( "riso.distributions.AbstractDistribution", 1 );
		return s;
	}

	/** This helper is called only when there are no parents of a variable;
	  * in that case, the marginal distribution is just the distribution
	  * conditional on the parents, and pi is the marginal distribution.
	  * So this method just returns the variable's conditional distribution.
	  */
	public Distribution compute_pi( ConditionalDistribution px, Distribution[] pi_messages ) throws Exception
	{
		return (Distribution) px;
	}
}
