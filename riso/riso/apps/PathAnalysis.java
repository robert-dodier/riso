package risotto.belief_nets;

import java.rmi.*;
import java.util.*;

public class PathCompiler
{
	Hashtable path_sets = new Hashtable();

	public PathCompiler() {}

	public void compile_paths( AbstractBeliefNetwork bn ) throws RemoteException
	{
		path_sets.clear();

		// SHOULD SYNCHRONIZE ON bn TO PREVENT EDITING WHILE compile_paths IS RUNNING !!!

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

				Vector current_path_set = new Vector();
				Stack path_stack = new Stack();

				find_paths( current_variable, other_variable, current_path_set, path_stack );

				VariablePair vp = new VariablePair( current_variable, other_variable );
				System.err.println( "  put a path for "+vp );
				path_sets.put( vp, current_path_set );
			}
		}

		// PRINT OUT RESULTS OF PATH FINDING. !!!
		variables = bn.get_variables();
		while ( variables.hasMoreElements() )
		{
			AbstractVariable x = (AbstractVariable) variables.nextElement();
			System.err.println( " --- paths from: "+x.get_name()+" ---" );

			Enumeration other_variables = bn.get_variables();
			while ( other_variables.hasMoreElements() )
			{
				other_variable = (AbstractVariable) other_variables.nextElement();
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
	}

	public void find_paths( AbstractVariable x, AbstractVariable end, Vector path_set, Stack path_stack ) throws RemoteException
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
				find_paths( parent, end, path_set, path_stack );
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
				find_paths( child, end, path_set, path_stack );
		}

		path_stack.pop();
	}

	public static void main( String[] args )
	{
		BeliefNetworkContext.path_list = new String[1];
		BeliefNetworkContext.path_list[0] = ".";

		try
		{
			AbstractBeliefNetwork bn = BeliefNetworkContext.load_network( args[0] );
			PathCompiler pc = new PathCompiler();
			pc.compile_paths( bn );
			System.exit(0);
		}
		catch (Exception e)
		{
			System.err.println( "PathCompiler.main:" );
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
