package riso.distributions.computes_lambda_message;
import java.rmi.*;
import java.util.*;
import riso.distributions.*;
import riso.belief_nets.*;
import riso.approximation.*;
import numerical.*;

/** This class implements a lambda message helper for a variable <tt>x</tt> with
  * one parents. The pi message from the parent is ignored; it should be null.
  * This helper simply returns a helper from the more general helper class
  * which handles variables with two or more parents.
  */
public class AbstractConditionalDistribution_AbstractDistribution_ implements LambdaMessageHelper
{
	public Distribution compute_lambda_message( ConditionalDistribution pxu, Distribution lambda, Distribution[] pi_messages ) throws Exception
	{
if ( pi_messages.length != 1 || pi_messages[0] != null ) throw new Exception( "AbsCondDist_AbsDist_.compute_lambda_message: pi_messages not 1 null msg." );
		return new IntegralCache( pxu, lambda, pi_messages );
	}
}

