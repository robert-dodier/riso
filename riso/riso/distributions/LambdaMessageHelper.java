package riso.distributions;
import java.util.*;

public interface LambdaMessageHelper
{
	/** Compute the likelihood message from a variable to a parent.
	  * This is defined as <tt>p(``e below''|x)</tt> ... NEEDS WORK !!!
	  */
	public Distribution compute_lambda_message( ConditionalDistribution px, Distribution lambda, Distribution[] pi_messages ) throws Exception;
}
