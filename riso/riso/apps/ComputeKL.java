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
package riso.apps;
import java.rmi.*;
import riso.belief_nets.*;
import riso.distributions.*;
import riso.approximation.*;
import riso.numerical.*;

/** An instance of this class is a helper to compute the Kullback-Liebler
  * asymmetric divergence from the first argument of the constructor 
  * (some distribution) to the second (some other distribution).
  */
public class ComputeKL
{
	public boolean verbose = false;
	IntegralHelper1d ceih, eih;

	public ComputeKL( Distribution p1, Distribution p2 ) throws Exception
	{
        CrossEntropyIntegrand cei = new CrossEntropyIntegrand( p1, p2 );
		EntropyIntegrand ei = new EntropyIntegrand( p1 );

		double[][] support = new double[1][];

		support[0] = p1.effective_support( 1e-4 );

		boolean is_discrete = p1 instanceof Discrete;
		ceih = new IntegralHelper1d( cei, support, is_discrete );
		eih = new IntegralHelper1d( ei, support, is_discrete );
	}

	public double do_compute_kl() throws Exception
	{
		double ce, e;

		try 
		{
			ce = ceih.do_integral();
			e = eih.do_integral();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			throw new Exception( "ComputeKL.do_compute_kl: attempt failed; "+ex );
		}

		if ( verbose ) System.out.println( "ComputeKL.do_compute_kl: entropy: "+e+"  cross-entropy: "+ce );
		return ce-e;
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

			AbstractBeliefNetwork xbn = (AbstractBeliefNetwork) bnc.get_reference( NameInfo.parse_variable(x_fullname,bnc) );
			AbstractBeliefNetwork ebn = (AbstractBeliefNetwork) bnc.get_reference( NameInfo.parse_variable(e_fullname,bnc) );
			AbstractVariable x = (AbstractVariable) xbn.name_lookup(x_name);
			AbstractVariable e = (AbstractVariable) ebn.name_lookup(e_name);

			if ( args.length == 3 )
			{
				String evalue_string = args[2];
				double evalue = Double.parseDouble(evalue_string);
System.err.println( "evalue: "+evalue );

				ebn.clear_posterior(e);
				Distribution px = xbn.get_posterior(x);
System.err.println( "px: "+px.format_string("") );
				ebn.assign_evidence(e,evalue);
				Distribution pxe = xbn.get_posterior(x);
System.err.println( "pxe: "+pxe.format_string("") );

				ComputeKL kl_doer = new ComputeKL( pxe, px );	// OTHER WAY AROUND ???
				kl_doer.verbose = true;
				System.out.println( "ComputeKL: KL == "+kl_doer.do_compute_kl() );
			}
			else
			{
				Distribution px = xbn.get_posterior(x);
				Distribution pe = ebn.get_posterior(e);
				ComputeKL kl_doer = new ComputeKL( px, pe );
				kl_doer.verbose = true;
				System.out.println( "ComputeKL: KL == "+kl_doer.do_compute_kl() );
			}
		}
		catch (Exception e) { e.printStackTrace(); }
		System.exit(0);
	}
}
