package riso.distributions;
import SeqTriple;

public class TrivialLambdaHelper implements LambdaHelper
{
	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely any number of <tt>Noninformative</tt>.
	  */
	public static SeqTriple[] description()
	{
		SeqTriple[] s = new SeqTriple[1];
		s[0] = new SeqTriple( "riso.distributions.Noninformative", -1 );
		return s;
	}

	/** In the case there is no diagnostic support, lambda is noninformative.
	  */
	public Distribution compute_lambda( Distribution[] lambda_messages ) throws Exception
	{
		return new Noninformative();
	}
}
