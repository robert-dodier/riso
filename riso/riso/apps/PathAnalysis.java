package risotto.belief_nets;

import java.util.*;


public class PathCompiler
{
	Hashtable path_sets = new Hashtable();

	public PathCompiler() {}

	public void compile_paths( AbstractBeliefNetwork bn )
	{
		path_set.removeAllElements();
		path_stack.removeAllElements();

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

				path_sets.put( current_variable, current_path_set );
			}
		}

		// PRINT OUT RESULTS OF PATH FINDING. !!!
		variables = bn.get_variables();
		while ( variables.hasMoreElements() )
		{
			Vector path_set = path_sets.get( variables.nextElement() );
			Enumeration path_set_enum = path_set.elements();
			while ( path_set_enum.hasMoreElements() )
			{
				Enumeration path = (Enumeration) path_set_enum.nextElement();
				System.err.print( "PathCompiler.compile_paths: path: " );
				while ( path.hasMoreElements() )
					System.err.print( ((AbstractVariable)path.nextElement()).get_name()+" " );
				System.err.println("");
			}
		}
	}

	public void find_path( AbstractVariable x, AbstractVariable end, Vector path_set, Stack path_stack )
	{
		System.err.println( "PathCompiler.find_path: from: "+x.get_name()+" to: "+end.get_name() );

		path_stack.push( x );

		if ( x == end )
		{
			// Construct a path from the beginning to the end, using what's on the stack.
			path_set.addElement( path_stack.elements() );
			System.err.println( "\tFound path: "+path_stack.elements() );

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
				find_path( parent, end, path_set, path_stack );
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
				find_path( child, end, path_set, path_stack );
		}

		path_stack.pop();
	}
}
