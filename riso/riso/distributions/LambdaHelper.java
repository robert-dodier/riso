package risotto.distributions;
import java.util.*;

public interface LambdaHelper
{
	/** Compute the likelihood function for a variable. This is defined
	  * as <code>p(``e below''|x)</code> ... NEEDS WORK !!!
	  */
	public Distribution compute_lambda( ConditionalDistribution px, Enumeration pi_messages, Enumeration lambda_messages ) throws Exception;
}
