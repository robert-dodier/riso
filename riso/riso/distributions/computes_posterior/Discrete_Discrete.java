package riso.distributions.computes_posterior;
import riso.distributions.*;
import riso.distributions.computes_lambda.*;

public class Discrete_Discrete extends AbstractPosteriorHelper
{
	public Distribution compute_posterior( Distribution pi, Distribution lambda ) throws Exception
	{
		Distribution[] p = new Distribution[2];
		p[0] = pi;
		p[1] = lambda;

		return (new riso.distributions.computes_lambda.Discrete()).compute_lambda( p );
	}
}
