package riso.distributions;
import java.util.*;

public class LambdaMessageHelperLoader
{
	public static LambdaMessageHelper load_lambda_message_helper( ConditionalDistribution px, Distribution lambda, Distribution[] pi_messages ) throws Exception
	{
		if ( lambda instanceof Noninformative )
			return new TrivialLambdaMessageHelper();

		Vector seq = new Vector();
		seq.addElement( px.getClass() );
		seq.addElement( lambda.getClass() );
		for ( int i = 0; i < pi_messages.length; i++ )
			seq.addElement( pi_messages[i].getClass() );

		Class c = PiHelperLoader.find_helper_class( seq, "lambda_message" );
		return (LambdaMessageHelper) c.newInstance();
	}
}
