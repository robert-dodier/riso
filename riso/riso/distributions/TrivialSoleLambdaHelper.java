package riso.distributions;
import SeqTriple;

public class TrivialSoleLambdaHelper implements LambdaHelper
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

	/** In the case there is only one lambda message, lambda is just
	  * that message.
	  */
	public Distribution compute_lambda( Distribution[] lambda_messages ) throws Exception
	{
		return (Distribution) lambda_messages[0].remote_clone();
	}
}
