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
package riso.approximation;
import java.rmi.*;
import riso.belief_nets.*;
import riso.distributions.*;
import numerical.*;

public class ComputeMI
{
	AbstractBeliefNetwork xbn, ebn;
	AbstractVariable x, e;
	int n = 12;

	public ComputeMI() {}

	public double do_compute_mi() throws Exception
	{
		double sum = 0;
		ebn.clear_posterior(e);
		Distribution pe = ebn.get_posterior(e), px = xbn.get_posterior(x);

		for ( int i = 0; i < n; i++ )
		{
			double[] evalue = pe.random();
			ebn.assign_evidence( e, evalue[0] );
			Distribution pxe = xbn.get_posterior(x);
			double kl = new ComputeKL( pxe, px ).do_compute_kl();
System.err.println( "ComputeMI.do_compute_mi: e: "+evalue[0]+", KL( p(x|e), p(x) ): "+kl );
			sum += kl;
		}

		ebn.clear_posterior(e);
		return sum/n;
	}

	public static void main( String[] args )
	{
		try
		{
			String x_fullname = args[0], e_fullname = args[1];
			String x_name = x_fullname.substring( x_fullname.indexOf(".")+1 );
			String e_name = e_fullname.substring( e_fullname.indexOf(".")+1 );

System.err.println( "x_fullname: "+x_fullname+", e_fullname: "+e_fullname );
System.err.println( "x_name: "+x_name+", e_name: "+e_name );

			BeliefNetworkContext bnc = new BeliefNetworkContext(null);

			ComputeMI mi_doer = new ComputeMI();
			mi_doer.xbn = (AbstractBeliefNetwork) bnc.get_reference( NameInfo.parse_variable(x_fullname,bnc) );
			mi_doer.ebn = (AbstractBeliefNetwork) bnc.get_reference( NameInfo.parse_variable(e_fullname,bnc) );
			mi_doer.x = (AbstractVariable) mi_doer.xbn.name_lookup(x_name);
			mi_doer.e = (AbstractVariable) mi_doer.ebn.name_lookup(e_name);

			System.err.println( "ComputeMI: MI == "+mi_doer.do_compute_mi() );
		}
		catch (Exception e) { e.printStackTrace(); }
		System.exit(0);
	}
}
