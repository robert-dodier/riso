package riso.distributions.computes_lambda_message;
import java.rmi.*;
import java.util.*;
import riso.distributions.*;
import riso.belief_nets.*;
import riso.approximation.*;
import numerical.*;
import SeqTriple;

/** This class implements a lambda message helper for a variable <tt>x</tt> with
  * one parents. The pi message from the parent is ignored; it should be null.
  * This helper simply returns a helper from the more general helper class
  * which handles variables with two or more parents.
  */
public class AbstractConditionalDistribution_AbstractDistribution_ implements LambdaMessageHelper
{
	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely one <tt>AbstractConditionalDistribution</tt>
	  * followed by one <tt>AbstractDistribution</tt>.
	  */
	public static SeqTriple[] description()
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.AbstractConditionalDistribution", 1 );
		s[1] = new SeqTriple( "riso.distributions.AbstractDistribution", 1 );
		return s;
	}

	public Distribution compute_lambda_message( ConditionalDistribution pxu, Distribution lambda, Distribution[] pi_messages ) throws Exception
	{
if ( pi_messages.length != 1 || pi_messages[0] != null ) throw new Exception( "AbsCondDist_AbsDist_.compute_lambda_message: pi_messages not 1 null msg." );
		return new IntegralCache( pxu, lambda, pi_messages );
	}
}

