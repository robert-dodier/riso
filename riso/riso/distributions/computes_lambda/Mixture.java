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
package riso.distributions.computes_lambda;
import java.io.*;
import java.util.*;
import riso.approximation.*;
import riso.distributions.*;
import riso.general.*;

/** Yeah, it's sort of confusing that this class has the same name as
  * in <tt>riso.distributions</tt>, but that's a consequence of the 
  * naming scheme used to locate message helpers. This class implements
  * a helper which can handle a list of <tt>riso.distributions.Mixture</tt>
  * messages.
  */
public class Mixture implements LambdaHelper
{
    public static SeqTriple[] description_array;

    public SeqTriple[] description() { return description_array; }

	/** Returns a description of the sequences of distributions accepted
	  * by this helper -- namely any number of <tt>Mixture</tt>.
	  */
	static
	{
		SeqTriple[] s = new SeqTriple[1];
		s[0] = new SeqTriple( "riso.distributions.Mixture", -1 );
		description_array = s;
	}

	/** @return The product of the incoming likelihood messages, which is
	  *   again a <tt>riso.distributions.Mixture</tt>.
	  * @see LambdaHelper.compute_lambda
	  */
	public Distribution compute_lambda( Distribution[] lambda_messages ) throws Exception
	{
		// Some of the lambda messages may noninformative; skip over those.
		// (And if we're called for a pi message computation, one will be null as well.)
		// Construct a list containing only the informative lambda messages.

		// Some of the lambda messages might be Gaussian; promote them to MixGaussians.
		// Any messages which are not Gaussian must be Mixtures.

		int ninformative = 0;

		for ( int i = 0; i < lambda_messages.length; i++ )
		{
			if ( lambda_messages[i] != null && !( lambda_messages[i] instanceof Noninformative ) )
				++ninformative;
		}
			
		riso.distributions.Mixture[] informative_lambdas = new riso.distributions.Mixture[ ninformative ];

		for ( int i = 0, j = 0; i < lambda_messages.length; i++ )
		{
			if ( lambda_messages[i] == null || lambda_messages[i] instanceof Noninformative )
				continue;

			if ( lambda_messages[i] instanceof riso.distributions.Gaussian )
				informative_lambdas[j++] = new riso.distributions.MixGaussians( (riso.distributions.Gaussian) lambda_messages[i] );
			else
				informative_lambdas[j++] = (riso.distributions.Mixture) lambda_messages[i];
		}

		// return riso.distributions.Mixture.mixture_product( informative_lambdas );
		return mixture_product( informative_lambdas ); // TEMPORARY -- mixture_product WILL MOVE !!!
	}

	/** Computes a mixture from the product of a set of mixtures. MOVE THIS CODE TO distributions.Mixture !!!
	  */
	public static Distribution mixture_product( riso.distributions.Mixture[] mixtures ) throws Exception
	{
		// Flatten all mixtures before trying to compute product.
		for ( int i = 0; i < mixtures.length; i++ ) mixtures[i] = riso.distributions.Mixture.flatten( mixtures[i] );

		if ( mixtures.length == 1 )
			try { return (riso.distributions.Mixture) mixtures[0].clone(); }
			catch (CloneNotSupportedException e) { return mixtures[0]; } // ASSUME IMMUTABLE OBJECT HERE; USUALLY SAFE !!!

		int nproduct = 1;
		for ( int i = 0; i < mixtures.length; i++ )
			nproduct *= mixtures[i].ncomponents();
		riso.distributions.Mixture product = new riso.distributions.Mixture( 1, nproduct );
System.err.println( "computes_lambda.Mixture.mixture_product: nproduct: "+nproduct );

		int[] k = new int[ mixtures.length ], l = new int[1];
		product_inner_loop( mixtures, product, k, l, 0 );

		// Normalize mixing coefficients -- not necessary, but may make a string format more intelligible.
		double sum = 0;
		for ( int i = 0; i < product.ncomponents(); i++ ) sum += product.mix_proportions[i];
		for ( int i = 0; i < product.ncomponents(); i++ ) product.mix_proportions[i] /= sum;
		
		try { product = riso.distributions.MixGaussians.convert_mixture(product); }
		catch (IllegalArgumentException e) {} // eat it; product has some non-Gaussian component -- that's OK.

		if ( product.components.length == 1 )
			return product.components[0];
		else
			return product;
	}
	
	static void product_inner_loop( riso.distributions.Mixture[] mixtures, riso.distributions.Mixture product, int[] k, int[] l, int m )  throws Exception
	{
		if ( m == mixtures.length )
		{
			// Recursion has bottomed out.
			compute_one_product( mixtures, product, k, l );
		}
		else
		{
			for ( int i = 0; i < mixtures[m].ncomponents(); i++ )
			{
				k[m] = i;
				product_inner_loop( mixtures, product, k, l, m+1 );
			}
		}
	}

	static void compute_one_product( riso.distributions.Mixture[] mixtures, riso.distributions.Mixture product, int[] k, int[] l ) throws Exception
	{
		double mix_proportion = 1;
		Vector mix_combo = new Vector();
		boolean all_gaussian = true;
		Distribution d;

		for ( int i = 0; i < mixtures.length; i++ )
		{
			mix_proportion *= mixtures[i].mix_proportions[k[i]];
			if ( ! ((d = mixtures[i].components[k[i]]) instanceof Noninformative) )
			{
				mix_combo.addElement(d);
				if ( ! (d instanceof riso.distributions.Gaussian) ) all_gaussian = false;
			}
		}

		if ( mix_combo.size() == 0 )
		{
			// All components were Noninformative; product is also a Noninformative.
			product.components[l[0]] = new Noninformative();
		}
		else if ( mix_combo.size() == 1 )
		{
			// All components except one were Noninformative; product is that one informative lambda message.
			try { product.components[l[0]] = (Distribution) ((Distribution)mix_combo.elementAt(0)).clone(); }
			catch (CloneNotSupportedException e) { product.components[l[0]] = (Distribution) mix_combo.elementAt(0); } // ASSUME IMMUTABLE OBJECTS; USUALLY SAFE !!!
		}
		else
		{
			// Need to compute product of two or more informative lambda messages.
			// Treat Gaussians as a special case; otherwise punt. COULD BE SMARTER HERE ???

			if ( all_gaussian )
			{
				riso.distributions.Gaussian[] g = new riso.distributions.Gaussian[mix_combo.size()];
				mix_combo.copyInto(g);
				double[] ignored_scale = new double[1];

				product.components[l[0]] = riso.distributions.Gaussian.densities_product(g,ignored_scale);
				mix_proportion *= ignored_scale[0];
			}
			else
			{
				Distribution[] ddd = new Distribution[mix_combo.size()];
				mix_combo.copyInto(ddd);
				double[] mass = new double[1];

				product.components[l[0]] = new DistributionProduct( true, false, ddd );
			}
		}

		product.mix_proportions[l[0]] = mix_proportion;
		++l[0];
	}
	
}
