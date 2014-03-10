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
import java.util.*;
import riso.belief_nets.*;
import riso.distributions.*;
import riso.numerical.*;

public class ComputeMeanMI
{
	AbstractVariable[] y;
	int n = 12;
	boolean verbose = false;
	static long delay = 0;

	public ComputeMeanMI() {}

	public double[] do_compute_mean_mi( AbstractVariable x, AbstractVariable e, AbstractVariable[] y ) throws Exception
	{
		if ( y.length == 0 )
		{
			double[] a = new double[2];
			a[0] = do_compute_mi(x,e);
			return a; // a[1] is zero.
		}

		// WE NEED TO SAMPLE FROM THE JOINT DISTRIBUTION OF THE BACKGROUND VARIABLES. !!!
		// TO SIMPLIFY, ASSUME THAT ALL BACKGROUND VARIABLES ARE INDEPENDENT, SO TO !!!
		// SAMPLE FROM THE JOINT WE JUST SAMPLE FROM EACH MARGINAL !!!

		e.get_bn().clear_posterior(e);
		for ( int i = 0; i < y.length; i++ ) y[i].get_bn().clear_posterior(y[i]);

		Distribution[] py = new Distribution[ y.length ];
		for ( int i = 0; i < py.length; i++ ) py[i] = y[i].get_bn().get_posterior(y[i]);

		double sum = 0, sum2 = 0;

		for ( int i = 0; i < n*y.length; i++ ) // SHOULD INCREASE LIKE y.length^n !!!
		{
System.err.print( "\n"+"assign background: " );
			for ( int j = 0; j < y.length; j++ )
			{
				double[] yvalue = py[j].random();
System.err.print( yvalue[0]+", " );
				y[j].get_bn().assign_evidence( y[j], yvalue[0] );
			}
System.err.println("");

			double mi = do_compute_mi(x,e);
System.err.println( "\t"+"MI(x,e): "+mi );
			sum += mi;
			sum2 += mi*mi;
		}
		
		for ( int j = 0; j < y.length; j++ ) y[j].get_bn().clear_posterior(y[j]);

		double[] a = new double[2];
		a[0] = sum/(n*y.length);
		a[1] = sum2/(n*y.length) - a[0]*a[0];
		return a;
	}

	public double do_compute_mi( AbstractVariable x, AbstractVariable e ) throws Exception
	{
		double sum = 0;
		e.get_bn().clear_posterior(e);
		Distribution pe = e.get_bn().get_posterior(e), px = x.get_bn().get_posterior(x);

		if ( pe instanceof Discrete )
		{
			int m = ((Discrete)pe).get_nstates();
			double[] ii = new double[1];			// ...because pe.p() takes an array argument. <sigh>

			for ( int i = 0; i < m; i++ )
			{
				e.get_bn().assign_evidence( e, i );
				Distribution pxe = x.get_bn().get_posterior(x);
				double kl = new ComputeKL( pxe, px ).do_compute_kl();
				if ( verbose ) System.err.println( "ComputeMeanMI.do_compute_mi: e: "+i+", KL( p(x|e), p(x) ): "+kl );

				ii[0] = i;
				sum += kl * pe.p(ii);
			}

			e.get_bn().clear_posterior(e);
			return sum;
		}
		else
		{
			for ( int i = 0; i < n; i++ )
			{
				double[] evalue = pe.random();
				e.get_bn().assign_evidence( e, evalue[0] );
				Distribution pxe = x.get_bn().get_posterior(x);
				double kl = new ComputeKL( pxe, px ).do_compute_kl();
				if ( verbose ) System.err.println( "ComputeMeanMI.do_compute_mi: e: "+evalue[0]+", KL( p(x|e), p(x) ): "+kl );
				sum += kl;

				if ( delay > 0 ) try { Thread.currentThread().sleep(delay); } catch (InterruptedException ex) {}
			}

			e.get_bn().clear_posterior(e);
			return sum/n;
		}
	}

	public static void main( String[] args )
	{
		int n = -1;
		String x_fullname = "", e_fullname = "";
		Vector y_fullnames_vector = new Vector();
		boolean verbose = false;

		for ( int i = 0; i < args.length; i++ )
		{
			switch ( args[i].charAt(1) )
			{
			case 'n':
				n = Integer.parseInt( args[++i] );
				break;
			case 'x':
				x_fullname = args[++i];
				break;
			case 'e':
				e_fullname = args[++i];
				break;
			case 'y':
				y_fullnames_vector.addElement( args[++i] );
				break;
			case 'v':
				verbose = true;
				break;
			case 'd':
				delay = Long.parseLong( args[++i] );
				break;
			}
		}

		String[] y_fullnames = new String[ y_fullnames_vector.size() ];
		y_fullnames_vector.copyInto( y_fullnames );

		try
		{
			BeliefNetworkContext bnc = new BeliefNetworkContext(null);

			ComputeMeanMI mi_doer = new ComputeMeanMI();
			if ( n > 0 ) mi_doer.n = n;
			mi_doer.verbose = verbose;

			AbstractVariable x = fullname_lookup( x_fullname, bnc );
			AbstractVariable e = fullname_lookup( e_fullname, bnc );
System.err.println( "x: "+x.get_fullname()+", e: "+e.get_fullname() );

			AbstractVariable[] y = new AbstractVariable[ y_fullnames.length ];
			for ( int i = 0; i < y_fullnames.length; i++ )
			{
				y[i] = fullname_lookup( y_fullnames[i], bnc );
System.err.println( "y["+i+"]: "+y[i].get_fullname() );
			}

			double[] mmi = mi_doer.do_compute_mean_mi(x,e,y);
			System.err.println( "ComputeMeanMI: mean MI: "+mmi[0]+", std deviation: "+Math.sqrt(mmi[1]) );
		}
		catch (Exception e) { e.printStackTrace(); }
		System.exit(0);
	}

	public static AbstractVariable fullname_lookup( String fullname, BeliefNetworkContext bnc ) throws RemoteException
	{
		String name = fullname.substring( fullname.lastIndexOf(".")+1 );
System.err.println( "fullname_lookup: extracted name: "+name );
		AbstractBeliefNetwork bn = (AbstractBeliefNetwork) bnc.get_reference( NameInfo.parse_variable(fullname,bnc) );
		return (AbstractVariable) bn.name_lookup(name);
	}
}
