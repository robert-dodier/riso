package riso.distributions;
import java.util.*;

public class TrivialPiMessageHelper implements PiMessageHelper
{
	/** In the case that there are no lambda messages to take into account,
	  * i.e. lambda messages coming from children other than the one to which
	  * we are sending the pi message, the pi message is simply <tt>pi</tt>.
	  */
	public Distribution compute_pi_message( Distribution pi,  Distribution[] lambda_messages ) throws Exception
	{
		return pi;
	}
}
