package riso.distributions;
import java.util.*;

public class LambdaHelperLoader
{
	public static LambdaHelper load_lambda_helper( Distribution[] lambda_messages ) throws Exception
	{
		// If there's only one message, then lambda is equal to that message.

		if ( lambda_messages.length == 1 )
			return new TrivialSoleLambdaHelper();

		// Before constructing the list of names of lambda message classes,
		// strike out any Noninformative messages. If all messages are
		// Noninformative, then load a trivial helper.

		int ninformative = 0;
		Distribution[] remaining_lambda_messages = new Distribution[ lambda_messages.length ];
		for ( int i = 0; i < lambda_messages.length; i++ )
			if ( lambda_messages[i] != null && ! (lambda_messages[i] instanceof Noninformative) )
			{
				++ninformative;
				remaining_lambda_messages[i] = lambda_messages[i];
			}

		if ( ninformative == 0 )
			return new TrivialNoninformativeLambdaHelper();

		Vector seq = new Vector();
		for ( int i = 0; i < remaining_lambda_messages.length; i++ )
			if ( remaining_lambda_messages[i] != null )
				seq.addElement( remaining_lambda_messages[i].getClass() );

		Class c = PiHelperLoader.find_helper_class( seq, "lambda" );
		return (LambdaHelper) c.newInstance();
	}
}
