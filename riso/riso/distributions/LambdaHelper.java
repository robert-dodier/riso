package riso.distributions;
import java.util.*;

public interface LambdaHelper
{
	/** Compute the likelihood function for a variable. This is defined
	  * as <code>p(``e below''|x)</code> ... NEEDS WORK !!!
	  */
	public Distribution compute_lambda( Distribution[] lambda_messages ) throws Exception;
}
