package risotto.distributions;

public interface LambdaMessageHelper
{
	/** Compute a likelihood message, to be sent to parents. This is defined
	  * as <code>p(``e below''|x)</code> ... NEEDS WORK !!!
	  */
	public Distribution compute_likelihood( ConditionalDistribution x, Distribution[] pi, Distribution[] lambda ) throws Exception;
}
