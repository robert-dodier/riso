package riso.distributions;
import java.util.*;

public interface PiMessageHelper
{
	/** Compute the pi message from a variable to a child. This is defined
	  * as follows: This node is <tt>x</tt>, its children are <tt>y1,
	  * y2,...,xn</tt>, and the evidence on a path leading up from <tt>yk</tt>
	  * is defined as <tt>``e below yk''</tt>. The predictive support for
	  * <tt>yk</tt> coming from <tt>x</tt> is defined as <tt>p(y|``e above x'')</tt>
	  * <pre>
	  *   p(y|``e above x'') = ??? COMPLETE THIS !!!
	  * </pre>
	  *
	  * @param pi Predictive support for <tt>x</tt>.
	  * @param lambda_messages List of incoming lambda messages, <tt>p(``e below x''|x)</tt>,
	  *   with any message coming from <tt>yk</tt> omitted.
	  * @return The predictive support, <tt>p(yk|``e above x'')</tt>. This pi message
	  *   can be sent to any child not included in the <tt>lambda_messages</tt> list;
	  *   this comprises <tt>yk</tt> and any children for which there is no evidence
	  *   on their sub-tree. CORRECT ???
	  * @throws Exception If the arguments don't have types which can be
	  *   processed by an implementation of this interface. If the arguments
	  *   have types which match the name schema, then usually a message
	  *   can be computed, although it's not guaranteed that some other
	  *   kind of problem won't arise. 
	  */
	public Distribution compute_pi_message( Distribution pi,  Distribution[] lambda_messages ) throws Exception;
}
