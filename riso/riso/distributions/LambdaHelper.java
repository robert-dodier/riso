package densities;

public interface LambdaMessageHelper
{
	/** Compute a likelihood message, to be sent to parents. This is defined
	  * as <code>p(``e below''|x)</code> ... NEEDS WORK !!!
	  */
	public Density compute_likelihood( ConditionalDensity x, Density[] pi, Density[] lambda ) throws Exception;
}
