/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 1999, Robert Dodier.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA, 02111-1307, USA,
 * or visit the GNU web site, www.gnu.org.
 */
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
			// pi message corresponding to parent which receives this lambda message is null; skip it.
			if ( pi_messages[i] != null )
				seq.addElement( pi_messages[i].getClass() );

		Class c = PiHelperLoader.find_helper_class( seq, "lambda_message" );
		return (LambdaMessageHelper) c.newInstance();
	}
}
