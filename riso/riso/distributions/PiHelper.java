package risotto.distributions;
import java.util.*;

public interface PiHelper
{
	/** Compute the predictive support for a variable. This is defined
	  * as follows: This node is <tt>y</tt>, its parents are <tt>x1,
	  * x2,...,xn</tt>, and the evidence on paths above this node is <tt>
	  * ``e above y''<tt>. The predictive support for <tt>y</tt> is
	  * defined as <tt>p(y|``e above y'')</tt>.
	  * <pre>
	  *   p(y|``e above y'') = \int_x1 ... \int_xn p(y|x1,...,xn) \times
	  *      p(x1|``e above y'') ... p(xn|``e above y'') dxn ... dx1
	  *    = \int_x1 p(x1|``e above y'') \int_x2 p(x2|``e above y'')
	  *      ... \int_xn p(xn|``e above y'') p(y|x1,x2,...,xn) dxn ... dx2 dx1
	  * </pre>
	  * In the case of discrete variables, the integrations are summations.
	  * An integration (or summation) can be over more than one dimension.
	  *
	  * @param x Node of interest.
	  * @param pi List of incoming pi messages, <tt>p(xk|``e above y'')</tt>.
	  *   Note that some of the evidence above <tt>y</tt> can be below <tt>xk</tt>.
	  * @return The predictive support, <tt>p(y|``e above y'')</tt>.
	  * @throws Exception If the arguments don't have types which can be
	  *   processed by an implementation of this interface. If the arguments
	  *   have types which match the name schema, then usually a message
	  *   can be computed, although it's not guaranteed that some other
	  *   kind of problem won't arise. 
	  */
	public Distribution compute_pi( ConditionalDistribution y, Distribution[] pi_messages ) throws Exception;
}
