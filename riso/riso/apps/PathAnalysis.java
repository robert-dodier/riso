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

public class PathAnalysis
{
	public PathAnalysis() {}

	public static void compile_paths( AbstractVariable x1, AbstractVariable x2, Hashtable path_sets ) throws RemoteException
	{
		Vector path_set = new Vector();
		Stack path_stack = new Stack();

		find_all_paths( x1, x2, path_set, path_stack );

		VariablePair vp = new VariablePair( x1, x2 );
		// System.err.println( "  PathAnalysis.compile_paths: put a path for "+vp );
		path_sets.put( vp, path_set );
	}

	public static Hashtable compile_all_paths( AbstractBeliefNetwork bn ) throws RemoteException
	{
		Hashtable path_sets = new Hashtable();

		// SHOULD SYNCHRONIZE ON bn TO PREVENT EDITING WHILE compile_all_paths IS RUNNING !!!

		AbstractVariable[] variables = bn.get_variables();

		for ( int i = 0; i < variables.length; i++ )
		{
			// Skip through the other variables, up to and including the current variable;
			// we only need half the connectivity matrix, since it is symmetric.

			for ( int j = i+1; j < variables.length; j++ )
			{
				compile_paths( variables[i], variables[j], path_sets );
			}
		}

		return path_sets;
	}

	public static void find_all_paths( AbstractVariable x, AbstractVariable end, Vector path_set, Stack path_stack ) throws RemoteException
	{
		int i;

		path_stack.push( x );

		if ( x == end )
		{
			// Construct a path from the beginning to the end, using what's on the stack.

			AbstractVariable[] path = new AbstractVariable[ path_stack.size() ];

			// System.err.println( "\tFound path: " );
			Enumeration e;
			for ( i = 0, e = path_stack.elements(); e.hasMoreElements(); i++ )
			{
				path[i] = (AbstractVariable)e.nextElement();
				// System.err.print( path[i].get_name()+" " );
			}
			// System.err.println("");

			path_set.addElement( path );
			path_stack.pop();
			return;
		}

		AbstractVariable[] parents = x.get_parents();
		for ( i = 0; i < parents.length; i++ )
		{
			Enumeration e = path_stack.elements();
			boolean is_on_stack = false;
			while ( e.hasMoreElements() )
				if ( e.nextElement() == parents[i] )
				{
					is_on_stack = true;
					break;
				}

			if ( ! is_on_stack )
				find_all_paths( parents[i], end, path_set, path_stack );
		}

		AbstractVariable[] children = x.get_children();
		for ( i = 0; i < children.length; i++ )
		{
			Enumeration e = path_stack.elements();
			boolean is_on_stack = false;
			while ( e.hasMoreElements() )
				if ( e.nextElement() == children[i] )
				{
					is_on_stack = true;
					break;
				}

			if ( ! is_on_stack )
				find_all_paths( children[i], end, path_set, path_stack );
		}

		path_stack.pop();
	}

	public static boolean are_d_connected( AbstractVariable x1, AbstractVariable x2, Vector evidence ) throws RemoteException
	{
		// Find all paths between x1 and x2, then see if there is some path which is
		// d-connecting given the evidence. If so, return true, otherwise false.

		Hashtable path_sets = new Hashtable();	// HEY !!! THIS OUGHT TO BE CACHED SOMEWHERE !!!
		PathAnalysis.compile_paths( x1, x2, path_sets );

		Vector path_set = (Vector) path_sets.get( new VariablePair( x1, x2 ) );
		if ( path_set == null )
			// No connections whatsoever.
			return false;

		Enumeration path_set_enum = path_set.elements();
		while ( path_set_enum.hasMoreElements() )
		{
			AbstractVariable[] path = (AbstractVariable[]) path_set_enum.nextElement();
			if ( is_d_connecting( path, evidence ) )
			{
				System.err.print( "PathAnalysis.are_d_connected: path " );
				int i;
				for ( i = 0; i < path.length; i++ )
					System.err.print( path[i].get_name()+" " );
				System.err.print( "is d-connected given evidence " );
				for ( i = 0; i < evidence.size(); i++ )
					System.err.print( ((AbstractVariable)evidence.elementAt(i)).get_name()+" " );
				System.err.println("");
				return true;
			}
		}

		return false;
	}

	/** A <tt>path</tt> between <tt>x1</tt> and <tt>x2</tt> is d-connecting given <tt>evidence</tt>
	  * if every interior node <tt>n</tt> in the path has the property that either
	  * <ol>
	  * <li> <tt>n</tt> is linear or diverging and <tt>n</tt> is not in <tt>evidence</tt>, or
	  * <li> <tt>n</tt> is converging, and either <tt>n</tt> or some descendent of <tt>n</tt>
	  *   is in <tt>evidence</tt>.
	  * </ol>
	  */
	public static boolean is_d_connecting( AbstractVariable[] path, Vector evidence ) throws RemoteException
	{
		for ( int i = 1; i < path.length-1; i++ )
		{
			if ( is_converging( path[i-1], path[i], path[i+1] ) )
			{
				// System.err.println( "PathAnalysis.is_d_connecting: "+path[i].get_name()+" is converging." );
				if ( !evidence.contains( path[i] ) && !contains_descendent( evidence, path[i] ) )
					return false;
			}
			else
			{
				// System.err.println( "PathAnalysis.is_d_connecting: "+path[i].get_name()+" is linear or diverging." );
				if ( evidence.contains( path[i] ) )
					return false;
			}
		}

		return true;
	}

	public static boolean is_converging( AbstractVariable a, AbstractVariable b, AbstractVariable c ) throws RemoteException
	{
		AbstractVariable[] bparents = b.get_parents();
		boolean found_a = false, found_c = false;

		for ( int i = 0; i < bparents.length; i++ )
		{
			if ( found_a )
			{
				if ( bparents[i] == c )
					return true;
			}
			else if ( found_c )
			{
				if ( bparents[i] == a )
					return true;
			}
			else
			{
				if ( bparents[i] == a )
					found_a = true;
				else if ( bparents[i] == c )
					found_c = true;
			}
		}
	
		return false;
	}

	public static boolean contains_descendent( Vector evidence, AbstractVariable a ) throws RemoteException
	{
		AbstractVariable[] children = a.get_children();
		if ( children == null )
			return false;

		for ( int i = 0; i < children.length; i++ )
		{
			if ( evidence.contains( children[i] ) )
				return true;
			else if ( contains_descendent( evidence, children[i] ) )
				return true;
		}
		
		return false;
	}

	public static boolean is_ancestor( AbstractVariable possible_ancestor, AbstractVariable x, Stack path_stack ) throws RemoteException
	{
		AbstractVariable[] parents = x.get_parents(); 
		for ( int i = 0; i < parents.length; i++ )
		{
			if ( parents[i] == possible_ancestor )
			{
				path_stack.push( parents[i] );
				return true;
			}
			else if ( is_ancestor( possible_ancestor, parents[i], path_stack ) )
			{
				path_stack.push( parents[i] );
				return true;
			}
		}

		// If we didn't find the ancestor, don't add anything to the path_stack. 
		// This saves a lot of useless pushing and popping, since ordinarily few paths
		// will be directed cycles.
		return false;
	}

	public static boolean is_descendent( AbstractVariable possible_descendent, AbstractVariable x, Stack path_stack ) throws RemoteException
	{
		AbstractVariable[] children = x.get_children();
		for ( int i = 0; i < children.length; i++ )
		{
			if ( children[i] == possible_descendent )
			{
				path_stack.push( children[i] );
				return true;
			}
			else if ( is_descendent( possible_descendent, children[i], path_stack ) )
			{
				path_stack.push( children[i] );
				return true;
			}
		}

		// If we didn't find the descendent, don't add anything to the path_stack. 
		// This saves a lot of useless pushing and popping, since ordinarily few paths
		// will be directed cycles.
		return false;
	}

	/** Returns null if the belief network <tt>bn</tt> has no directed cycles. Otherwise returns
	  * an <tt>Enumeration</tt> of variables, which begins at a variable contained in a directed
	  * cycle, leads through the cycle (parent by parent), and ends at that same variable.
	  */
	public static Enumeration has_directed_cycle( AbstractBeliefNetwork bn ) throws RemoteException
	{
		// If is_ancestor returns true, then this stack contains a path from some variable all the
		// way back to that same variable. If is_ancestor returns false, then this stack remains empty.
		Stack path_stack = new Stack();

		AbstractVariable[] u = bn.get_variables();
		for ( int i = 0; i < u.length; i++ )
		{
			if ( is_ancestor( u[i], u[i], path_stack ) )
			{
				path_stack.push( u[i] );
				return path_stack.elements();
			}
		}

		return null;
	}

	public static void main( String[] args )
	{
		boolean do_compile_all = false;
		String bn_name = "", x1_name = "", x2_name = "";
		Vector evidence_names = new Vector();

		for ( int i = 0; i < args.length; i++ )
		{
			if ( args[i].charAt(0) != '-' ) continue;

			switch ( args[i].charAt(1) )
			{
			case 'b':
				bn_name = args[++i];
				break;
			case 'a':
				do_compile_all = true;
				break;
			case 'x':
				if ( args[i].charAt(2) == '1' )
					x1_name = args[++i];
				else if ( args[i].charAt(2) == '2' )
					x2_name = args[++i];
				else
					System.err.println( "PathAnalysis.main: "+args[i]+" -- huh???" );
				break;
			case 'e':
				evidence_names.addElement( args[++i] );
				break;
			default:
					System.err.println( "PathAnalysis.main: "+args[i]+" -- huh???" );
			}
		}

		try
		{
			BeliefNetworkContext bnc = new BeliefNetworkContext(null);
			bnc.add_path( "/bechtel/users10/krarti/dodier/belief-nets/assorted" );
			AbstractBeliefNetwork bn = bnc.load_network( bn_name );
			Hashtable path_sets;
			Enumeration p;

			if ( (p = PathAnalysis.has_directed_cycle( bn )) == null )
				System.err.println( "PathAnalysis: no directed cycles found in "+bn_name );
			else
			{
				System.err.println( "PathAnalysis.main: "+bn_name+" has a directed cycle; quit." );
				System.err.print( " cycle is: " );
				while ( p.hasMoreElements() )
				{
					System.err.print( ((AbstractVariable)p.nextElement()).get_name() );
					if ( p.hasMoreElements() )
						System.err.print( " -> " );
					else
						System.err.println( "" );
				}

				System.exit(1);
			}

			Vector evidence = new Vector();
			if ( evidence_names.size() > 0 )
			{
				for ( int i = 0; i < evidence_names.size(); i++ )
					evidence.addElement( bn.name_lookup( (String)(evidence_names.elementAt(i)) ) );
			}

			if ( do_compile_all )
			{
				path_sets = PathAnalysis.compile_all_paths( bn );
			}
			else
			{
				AbstractVariable x1 = (AbstractVariable) bn.name_lookup( x1_name );
				AbstractVariable x2 = (AbstractVariable) bn.name_lookup( x2_name );
				path_sets = new Hashtable();
				PathAnalysis.compile_paths( x1, x2, path_sets );
				
				if ( PathAnalysis.are_d_connected( x1, x2, evidence ) )
					System.err.print( x1.get_name()+" and "+x2.get_name()+" are d-connected given evidence " );
				else
					System.err.print( x1.get_name()+" and "+x2.get_name()+" are NOT d-connected given evidence " );

				for ( int i = 0; i < evidence.size(); i++ )
					System.err.print( ((AbstractVariable)evidence.elementAt(i)).get_name()+" " );
				System.err.println("");
			}

			System.err.println( "PathAnalysis.main: results of path finding:" );

			AbstractVariable[] u = bn.get_variables();
			for ( int i = 0; i < u.length; i++ )
			{
				System.err.println( " --- paths from: "+u[i].get_name()+" ---" );

				for ( int j = i+1; j < u.length; j++ )
				{
					VariablePair vp = new VariablePair( u[i], u[j] );
					Vector path_set = (Vector) path_sets.get( vp );
					if ( path_set == null )
						continue;

					Enumeration path_set_enum = path_set.elements();
					while ( path_set_enum.hasMoreElements() )
					{
						AbstractVariable[] path = (AbstractVariable[]) path_set_enum.nextElement();
						System.err.print( " path: " );
						for ( int k = 0; k < path.length; k++ )
							System.err.print( path[k].get_name()+" " );
						System.err.println("");
					}
				}
			}

			System.exit(0);
		}
		catch (Exception e)
		{
			System.err.println( "PathAnalysis.main:" );
			e.printStackTrace();
			System.exit(1);
		}
	}
}

class VariablePair
{
	AbstractVariable x1, x2;

	VariablePair( AbstractVariable x1, AbstractVariable x2 )
	{
		this.x1 = x1;
		this.x2 = x2;
	}

	public int hashCode()
	{
		return x1.hashCode() ^ x2.hashCode();
	}

	public boolean equals( Object another )
	{
		if ( another instanceof VariablePair )
			return this.x1 == ((VariablePair)another).x1 && this.x2 == ((VariablePair)another).x2;
		return false;
	}

	public String toString()
	{
		try { return "["+x1.get_name()+","+x2.get_name()+"]"; }
		catch (RemoteException e) { return "[???,???]"; }
	}
}
