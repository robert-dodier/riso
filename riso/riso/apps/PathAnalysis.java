package risotto.belief_nets;

import java.rmi.*;
import java.util.*;

public class PathAnalysis
{
	public PathAnalysis() {}

	public static void compile_paths( AbstractVariable x1, AbstractVariable x2, Hashtable path_sets ) throws RemoteException
	{
		Vector path_set = new Vector();
		Stack path_stack = new Stack();

		find_all_paths( x1, x2, path_set, path_stack );

		VariablePair vp = new VariablePair( x1, x2 );
		System.err.println( "  PathAnalysis.compile_paths: put a path for "+vp );
		path_sets.put( vp, path_set );
	}

	public static Hashtable compile_all_paths( AbstractBeliefNetwork bn ) throws RemoteException
	{
		Hashtable path_sets = new Hashtable();

		// SHOULD SYNCHRONIZE ON bn TO PREVENT EDITING WHILE compile_all_paths IS RUNNING !!!

		Enumeration variables = bn.get_variables();
		AbstractVariable current_variable, other_variable;

		while ( variables.hasMoreElements() )
		{
			current_variable = (AbstractVariable) variables.nextElement();

			// ASSUME THAT IF bn IS THE SAME, WE GET THE SAME ENUMERATION OF ITS VARIABLES !!!
			// THIS WILL BE THE CASE SO LONG AS THE ENUMERATION CONSTRUCTOR USES A DETERMINISTIC !!!
			// ALGORITHM, ALTHOUGH THAT IS NOT REQUIRED BY THE DEFINITION OF AN ENUMERATION !!!

			Enumeration other_variables = bn.get_variables();

			// Skip through the other variables, up to and including the current variable;
			// we only need half the connectivity matrix, since it is symmetric.

			while ( other_variables.hasMoreElements() && other_variables.nextElement() != current_variable )
				;

			while ( other_variables.hasMoreElements() )
			{
				other_variable = (AbstractVariable) other_variables.nextElement();
				compile_paths( current_variable, other_variable, path_sets );
			}
		}

		return path_sets;
	}

	public static void find_all_paths( AbstractVariable x, AbstractVariable end, Vector path_set, Stack path_stack ) throws RemoteException
	{
		path_stack.push( x );

		if ( x == end )
		{
			// Construct a path from the beginning to the end, using what's on the stack.

			AbstractVariable[] path = new AbstractVariable[ path_stack.size() ];

			System.err.println( "\tFound path: " );
			int i;
			Enumeration e;
			for ( i = 0, e = path_stack.elements(); e.hasMoreElements(); i++ )
			{
				path[i] = (AbstractVariable)e.nextElement();
				System.err.print( path[i].get_name()+" " );
			}
			System.err.println("");

			path_set.addElement( path );
			path_stack.pop();
			return;
		}

		Enumeration parents_enum = x.get_parents();
		while ( parents_enum.hasMoreElements() )
		{
			AbstractVariable parent = (AbstractVariable) parents_enum.nextElement();
			Enumeration e = path_stack.elements();
			boolean is_on_stack = false;
			while ( e.hasMoreElements() )
				if ( e.nextElement() == parent )
				{
					is_on_stack = true;
					break;
				}

			if ( ! is_on_stack )
				find_all_paths( parent, end, path_set, path_stack );
		}

		Enumeration children_enum = x.get_children();
		while ( children_enum.hasMoreElements() )
		{
			AbstractVariable child = (AbstractVariable) children_enum.nextElement();
			Enumeration e = path_stack.elements();
			boolean is_on_stack = false;
			while ( e.hasMoreElements() )
				if ( e.nextElement() == child )
				{
					is_on_stack = true;
					break;
				}

			if ( ! is_on_stack )
				find_all_paths( child, end, path_set, path_stack );
		}

		path_stack.pop();
	}

	public static void main( String[] args )
	{
		boolean do_compile_all = false;
		String bn_name = "", x1_name = "", x2_name = "";

		BeliefNetworkContext.path_list = new String[1];
		BeliefNetworkContext.path_list[0] = ".";

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
			default:
					System.err.println( "PathAnalysis.main: "+args[i]+" -- huh???" );
			}
		}

		try
		{
			AbstractBeliefNetwork bn = BeliefNetworkContext.load_network( bn_name );
			Hashtable path_sets;

			if ( do_compile_all )
			{
				path_sets = PathAnalysis.compile_all_paths( bn );
			}
			else
			{
				AbstractVariable x1 = bn.name_lookup( x1_name );
				AbstractVariable x2 = bn.name_lookup( x2_name );
				path_sets = new Hashtable();
				PathAnalysis.compile_paths( x1, x2, path_sets );
			}

			System.err.println( "PathAnalysis.main: results of path finding:" );

			Enumeration variables = bn.get_variables();
			while ( variables.hasMoreElements() )
			{
				AbstractVariable x = (AbstractVariable) variables.nextElement();
				System.err.println( " --- paths from: "+x.get_name()+" ---" );

				Enumeration other_variables = bn.get_variables();
				while ( other_variables.hasMoreElements() )
				{
					AbstractVariable other_variable = (AbstractVariable) other_variables.nextElement();
					VariablePair vp = new VariablePair( x, other_variable );
					Vector path_set = (Vector) path_sets.get( vp );
					if ( path_set == null )
						continue;

					Enumeration path_set_enum = path_set.elements();
					while ( path_set_enum.hasMoreElements() )
					{
						AbstractVariable[] path = (AbstractVariable[]) path_set_enum.nextElement();
						System.err.print( " path: " );
						for ( int i = 0; i < path.length; i++ )
						System.err.print( path[i].get_name()+" " );
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
