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

public class PiMessageHelperLoader
{
	/** @return <tt>null</tt> if an appropriate message helper class cannot be located.
	  */
	public static PiMessageHelper load_pi_message_helper( Distribution pi,  Distribution[] lambda_messages ) throws Exception
	{
		// Before constructing the list of names of lambda message classes,
		// strike out any Noninformative messages. If all messages are Noninformative,
		// then load a trivial helper.

		int ninformative = 0;
		Distribution[] remaining_lambda_messages = new Distribution[ lambda_messages.length ];
		for ( int i = 0; i < lambda_messages.length; i++ )
			if ( lambda_messages[i] != null && ! (lambda_messages[i] instanceof Noninformative) )
			{
				++ninformative;
				remaining_lambda_messages[i] = lambda_messages[i];
			}

		if ( ninformative == 0 )
			return new TrivialPiMessageHelper();

		Vector seq = new Vector();
		seq.addElement( pi.getClass() );
		for ( int i = 0; i < remaining_lambda_messages.length; i++ )
			if ( remaining_lambda_messages[i] != null )
				seq.addElement( remaining_lambda_messages[i].getClass() );

		Class c = PiHelperLoader.find_helper_class( seq, "pi_message" );
		return (PiMessageHelper) c.newInstance();
	}
}
