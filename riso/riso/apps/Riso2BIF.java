package risotto.apps;

import java.io.*;
import java.util.*;
import risotto.belief_nets.*;
import risotto.distributions.*;
import SmarterTokenizer;

public class Riso2BIF
{
	static String header = 
		"<?XML VERSION=\"1.0\"?>\n"+
		"<!-- Bayesian net in Fabio Cozman's BayesNet Interchange Format.\n"+
	 	"     Produced by Riso2BIF translator by Robert Dodier. -->\n"+
		"<!-- DTD for the BIF format -->\n"+
		"<!DOCTYPE BIF [\n"+
		"	<!ELEMENT PROPERTY (#PCDATA)>\n"+
		"	<!ELEMENT TYPE (#PCDATA)>\n"+
		"	<!ELEMENT VALUE (#PCDATA)>\n"+
		"	<!ELEMENT NAME (#PCDATA)>\n"+
		"	<!ELEMENT NETWORK\n"+
		"		( NAME, ( PROPERTY | VARIABLE | PROBABILITY )* )>\n"+
		"	<!ELEMENT VARIABLE ( NAME, TYPE, ( VALUE |  PROPERTY )* ) >\n"+
		"	<!ELEMENT PROBABILITY\n"+
		"		( FOR | GIVEN | TABLE | ENTRY | DEFAULT | PROPERTY )* >\n"+
		"	<!ELEMENT TABLE (#PCDATA)>\n"+
		"	<!ELEMENT DEFAULT (TABLE)>\n"+
		"	<!ELEMENT ENTRY ( VALUE* , TABLE )>\n"+
		"]>\n"+
		"<BIF>\n"+
		"<NETWORK>\n";

	public static void main( String[] args )
	{
		int i;
		Enumeration u;

		try
		{
			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );
			st.nextToken();
			Class bn_class = Class.forName( st.sval );
			BeliefNetwork bn = (BeliefNetwork) bn_class.newInstance();
			bn.pretty_input( st );

			System.out.print( header );
			System.out.println( "<NAME>"+bn.get_name()+"</NAME>" );

			System.out.println( "<!-- Variables -->" );
			for ( u = bn.get_variables(); u.hasMoreElements(); )
			{
				Variable x = (Variable) u.nextElement();
				System.out.println( "<VARIABLE>" );
				System.out.println( "\t<NAME>"+x.get_name()+"</NAME>" );
				System.out.println( "\t<TYPE>discrete</TYPE>" );

				int nstates = 0;
				ConditionalDistribution p = x.get_distribution();
				if ( p.ndimensions_child() > 1 )
					throw new Exception( "variable "+x.get_name()+" has "+p.ndimensions_child()+" dimensions." );

				if ( p instanceof ConditionalDiscrete )
					nstates = ((ConditionalDiscrete)p).dimensions_child[0];
				else if ( p instanceof Discrete )
					nstates = ((Discrete)p).dimensions[0];
				else
					throw new Exception( "variable "+x.get_name()+" is not discrete." );

				for ( i = 0; i < nstates; i++ )
					System.out.println( "\t<VALUE>"+i+"</VALUE>" );

				System.out.println( "</VARIABLE>" );
			}

			System.out.println( "<!-- Probability Tables -->" );
			for ( u = bn.get_variables(); u.hasMoreElements(); )
			{
				Variable x = (Variable) u.nextElement();
				System.out.println( "<PROBABILITY>" );
				System.out.println( "\t<FOR>"+x.get_name()+"</FOR>" );

				String[] xparents = x.get_parents_names();
				for ( i = 0; i < xparents.length; i++ )
					System.out.println( "\t<GIVEN>"+xparents[i]+"</GIVEN>" );

				System.out.print( "\t<TABLE>" );

				if ( x.get_distribution() instanceof ConditionalDiscrete )
				{
					ConditionalDiscrete p = (ConditionalDiscrete) x.get_distribution();
					double[] ix = new double[1];
					for ( i = 0; i < p.dimensions_child[0]; i++ )
					{
						ix[0] = i;
						double[] iparents = new double[ p.ndims_parents ];
						column_major_print( p, ix, iparents, p.ndims_parents );
					}
				}
				else // p must be Discrete; we tested for this above
				{
					Discrete p = (Discrete) x.get_distribution();
					for ( i = 0; i < p.probabilities.length-1; i++ )
						System.out.print( p.probabilities[i]+" " );
					System.out.print( p.probabilities[i] );
				}

				System.out.println( "</TABLE>" );
				System.out.println( "</PROBABILITY>" );
			}

			System.out.println( "</BIF>" );
			System.exit(0);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}

	static void column_major_print( ConditionalDiscrete p, double[] ix, double[] iparents, int m )
	{
		if ( m == 0 )
		{
			// We have a full assignment of parent indexes. Print the corresponding probability.
			System.out.print( " "+p.p( ix, iparents ) );
			return;
		}

		--m;
		for ( int i = 0; i < p.dimensions_parents[m]; i++ )
		{
			iparents[m] = i;
			column_major_print( p, ix, iparents, m );
		}
	}
}
