package riso.distributions;

public class TrivialSoleLambdaHelper implements LambdaHelper
{
	/** In the case there is only one lambda message, lambda is just
	  * that message.
	  */
	public Distribution compute_lambda( Distribution[] lambda_messages ) throws Exception
	{
		return (Distribution) lambda_messages[0].remote_clone();
	}
}
