package risotto.distributions;

public interface PiMessageHelper
{
	/** Compute a prediction message, to be sent to children. This is defined
	  * as follows: This node is <code>y</code>, its parents are <code>x1,
	  * x2,...,xn</code>, and the evidence on paths above this node is <code>
	  * ``e above''<code>. The prediction message for <code>y</code> is
	  * defined as <code>p(y|``e above'')</code>.
	  * <pre>
	  * p(y|``e above'') = \int_x1 ... \int_xn p(y|x1,...,xn) \times
	  *     p(x1|``e above'') ... p(xn|``e above'') dxn ... dx1
	  *   = \int_x1 p(x1|``e above'') \int_x2 p(x2|``e above'')
	  *     ... \int_xn p(xn|``e above'') p(y|x1,x2,...,xn) dxn ... dx2 dx1
	  * </pre>
	  * In the case of discrete variables, the integrations are summations.
	  * An integration (or summation) can be over more than one dimension.
	  *
	  * @param x Node of interest.
	  * @param pi List of incoming messages, <code>p(xk|``e above'')</code>.
	  * @return A message <code>p(y|``e above'')</code> to be sent to child nodes.
	  * @throws Exception If the arguments don't have types which can be
	  *   processed by an implementation of this interface. If the arguments
	  *   have types which match the name schema, then usually a message
	  *   can be computed, although it's not guaranteed that some other
	  *   kind of problem won't arise. 
	  */
	public Distribution compute_pi( ConditionalDistribution y, Distribution[] pi ) throws Exception;
}
