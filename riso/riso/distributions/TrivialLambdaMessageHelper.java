package riso.distributions;
import java.util.*;
import SeqTriple;

public class TrivialLambdaMessageHelper implements LambdaMessageHelper
{
	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely any conditional, followed by one
	  * <tt>Noninformative</tt>, followed by any number of unconditionals.
	  */
	public static SeqTriple[] description()
	{
		SeqTriple[] s = new SeqTriple[3];
		s[0] = new SeqTriple( "riso.distributions.AbstractConditionalDistribution", 1 );
		s[1] = new SeqTriple( "riso.distributions.Noninformative", 1 );
		s[2] = new SeqTriple( "riso.distributions.AbstractDistribution", -1 );
		return s;
	}

	/** In the case there is no diagnostic support, the lambda message
	  * is noninformative.
	  */
	public Distribution compute_lambda_message( ConditionalDistribution px, Distribution lambda, Distribution[] pi_messages ) throws Exception
	{
		return new Noninformative();
	}
}
