package risotto.distributions;

public class TrivialPiHelper implements PiHelper
{
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
