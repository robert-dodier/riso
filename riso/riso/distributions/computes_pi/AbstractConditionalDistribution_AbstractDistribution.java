package riso.distributions.computes_pi;
import java.rmi.*;
import riso.distributions.*;

/** @see PiHelper
  */
public class AbstractConditionalDistribution_AbstractDistribution implements PiHelper
{
	public Distribution compute_pi( ConditionalDistribution y_in, Distribution[] pi ) throws Exception
	{
		throw new Exception( "AbstractConditionalDistribution_AbstractDistribution.compute_pi: not implemented." );
	}
}
