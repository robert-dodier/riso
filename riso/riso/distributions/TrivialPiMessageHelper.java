package riso.distributions;
import java.util.*;
import SeqTriple;

public class TrivialPiMessageHelper implements PiMessageHelper
{
	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely exactly one <tt>AbstractDistribution</tt>
	  * followed by any number of <tt>Noninformative</tt>.
	  */
	public static SeqTriple[] description()
	{
		SeqTriple[] s = new SeqTriple[2];
		s[0] = new SeqTriple( "riso.distributions.AbstractDistribution", 1 );
		s[0] = new SeqTriple( "riso.distributions.Noninformative", -1 );
		return s;
	}

	/** In the case that there are no lambda messages to take into account,
	  * i.e. lambda messages coming from children other than the one to which
	  * we are sending the pi message, the pi message is simply <tt>pi</tt>.
	  */
	public Distribution compute_pi_message( Distribution pi,  Distribution[] lambda_messages ) throws Exception
	{
		return pi;
	}
}
