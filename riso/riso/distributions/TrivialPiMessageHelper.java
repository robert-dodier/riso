package risotto.distributions;
import java.util.*;

public class TrivialPiMessageHelper implements PiMessageHelper
{
	/** In the case that there are no lambda messages to take into account,
	  * i.e. lambda messages coming from children other than the one to which
	  * we are sending the pi message, the pi message is simply <tt>pi</tt>.
	  */
	public Distribution compute_pi_message( Distribution pi,  Enumeration lambda_messages ) throws Exception
	{
		if ( lambda_messages.hasMoreElements() )
			throw new IllegalArgumentException( "TrivialPiMessageHelper.compute_pi_message: there is one or more lambda messages." );

		return pi;
	}
}
