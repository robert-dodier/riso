/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 2004, Robert Dodier.
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
import java.io.*;
import java.rmi.*;
import riso.numerical.*;
import riso.general.*;

/** An instance of this class represents a Poisson distribution.
  */
public class Poisson extends AbstractDistribution
{
    /** Parameter of this distribution.
      */
	protected double lambda;

    /** Cache to hold precomputed values of the probability mass function.
      */
    protected double[] p_cache;

    /** Cache to hold precomputed values of the cumulative probability function.
      */
    protected double[] cdf_cache;

	/** Create and return a copy of this distribution.
	  */
	public Object clone() throws CloneNotSupportedException
	{
		Poisson copy = (Poisson) super.clone();
		copy.lambda = this.lambda;
        copy.p_cache = (double[]) this.p_cache.clone();
        copy.cdf_cache = (double[]) this.cdf_cache.clone();
		return copy;
	}

	/** Constructs a Poisson distribution with the specified parameter.
	  */
	public Poisson (double lambda)
	{
		this.lambda = lambda;
        allocate_cache();
	}

	/** Default constructor for this class. Sets parameter to 1.
	  */
	public Poisson()
    {
        lambda = 1;
        allocate_cache();
    }

	/** Returns the number of dimensions in which this distribution lives.
	  * Always returns 1.
	  */
	public int ndimensions() { return 1; }

	/** Computes the density at the point <code>x</code>.
	  * @param x Point at which to evaluate density -- must
	  *   be a one-element array. The point is silently truncated
      *   to an integer if it is not already.
	  */
	public double p (double[] x)
	{
		if ( x[0] < 0 ) return 0;

		int ix = (int) x[0];

        if (ix < p_cache.length)
            return p_cache [ix];
        else
            return compute_p (x[0]);
	}

    /** Returns the cumulative probability function, defined as
      * <pre>
      *    cdf(x) == Pr(X <= x) == sum_{i=0}^x p(x)
      * </pre>
      * Note the "less than or equal".
      */
    public double cdf (double x)
    {
        if (x < 0) return 0;

        int ix = (int) x;

        if (ix < cdf_cache.length)
            return cdf_cache [ix];
        else
        {
            double sum = cdf_cache [cdf_cache.length-1];
            for (int i = cdf_cache.length; i <= ix; i++)
                sum += compute_p (i);
            return sum;
        }
    }

    /** Returns <tt>lambda^x exp(-lambda)/x!</tt>.
      * It is assumed that <tt>x</tt> is an integer.
      */
    public double compute_p (double x)
    {
        if (lambda == 0)
        {
            if (x == 0) return 1; else return 0;
        }
        else
        {
            double expt = x * Math.log (lambda) - riso.numerical.SpecialMath.logGamma (x+1) - lambda;
            return Math.exp (expt);
        }
    }

	/** Computes the log of the prior probability of the parameters of
	  * this distribution, assuming some prior distribution has been 
	  * established. This may not be meaningful for all distributions.
	  */
	public double log_prior() throws Exception
	{
		throw new Exception( "Poisson.log_prior: not implemented." );
	}

	/** Return an instance of a random variable from this distribution.
	  * Use the method described in "Numerical Recipes in C", Section 7.3.
      * If <tt>lambda</tt> is small, use a direct method, and otherwise
      * use a rejection method. In both cases the returned values are
      * exactly Poisson; this method does not use a Gaussian or other
      * approximation.
      *
      * The direct method is adapted from <tt>RandomPoissonDistribution.java</tt>
      * by Laurentiu Cristofor (laur@cs.umb.edu, laur72_98@yahoo.com) and
      * released under GPL. See: <tt>http://www.cs.umb.edu/~laur/</tt>.
      * Laurentiu's code is based on Knuth, TAOCP, vol. 2, second printing,
      * section 3.4.1, algorithm Q on page 117.
      *
      * The rejection method is an implementation of the method
      * described in Numerical Recipes.
	  */
	public double[] random() throws Exception
	{
        double[] x = new double[1];

		if (lambda < 20)
        {
            // Direct method.

            double p = Math.exp (-lambda);
            int N = 0;
            double q = 1;

            while (true)
            {
                double U = Math.random();

                q = q * U;

                if (q >= p)
                    N = N + 1;
                else
                {
                    x[0] = N;
                    return x;
                }
            }
        }
        else
        {
            // Rejection method. Use comparison function 1/(1+x^2).

            double a = lambda, b = Math.sqrt (lambda), c = 1;
            double Ginf = b * c * (Math.PI/2 - Math.atan2 (-a, b));
// System.err.println ("Poisson.random: a: "+a+", b: "+b+", c: "+c+", G(infty): "+Ginf);

            while (true)
            {
                double U = Math.random() * Ginf;
                double Ginv = a + b * Math.tan (U/(b*c) + Math.atan2 (-a, b));
                double z = (Ginv - a)/b;
                double g = c/(1 + z*z);

                x[0] = (int) Ginv;
if (p(x) > g) throw new RuntimeException ("Poisson.random: p("+x[0]+", "+lambda+") > "+g+"; rejection method is broken.");

                if (Math.random() < p(x)/g)
                    return x;
            }
        }
	}

	/** Use data to modify the parameters of the distribution.
	  * This method is not implemented.
	  */
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception
	{
		throw new Exception( "Poisson.update: not implemented." );
	}

	/** Returns the expected value of this distribution.
	  * This is equal to the parameter <tt>lambda</tt>.
	  */
	public double expected_value() 
	{
		return lambda;
	}

	/** Returns the square root of the variance of this distribution.
	  * This is equal to the square root of the parameter <tt>lambda</tt>.
	  */
	public double sqrt_variance()
	{
		return Math.sqrt (lambda);
	}

	/** Returns an interval which contains almost all the mass of this
	  * distribution; uses a numerical search to find <tt>x</tt> such that
	  * the tail mass to the right of <tt>x</tt> is less than <tt>epsilon</tt>.
	  *
      * Current revision (2004/04/07) returns the same as this R statement:
      * <pre>
      *    c(qpois(epsilon/2, lambda), qpois(epsilon/2, lambda, lower.tail=F))
      * </pre>
      *
	  * @param epsilon This much mass or less lies outside the interval
	  *   which is returned.
	  * @return An interval represented as a 2-element array; element 0 is
	  *   zero, and element 1 is <tt>x</tt>, as defined above.
	  */
	public double[] effective_support( double epsilon ) throws Exception
	{
        int i = 0;
        double[] x = new double[1];
        
        x[0] = i;
        double sum = 0, px = p(x);

        while (sum+px < epsilon/2)
        {
            sum += px;
            x[0] = ++i;
            px = p(x);
        }

        double[] support = new double[2];
        support[0] = i;

        while (sum+px < 1 - epsilon/2)
        {
            sum += px;
            x[0] = ++i;
            px = p(x);
        }

        support[1] = i;

        return support;
    }

	/** Formats a string representation of this distribution.
	  * Since the representation is only one line of output, 
	  * the argument <tt>leading_ws</tt> is ignored.
	  */
	public String format_string( String leading_ws )
	{
		String result = "";
		result += this.getClass().getName()+" { ";
		result += "lambda "+lambda;
		result += " }"+"\n";
		return result;
	}

	/** Read an instance of this distribution from an input stream.
	  * This is intended for input from a human-readable source; this is
	  * different from object serialization.
	  * @param st Stream tokenizer to read from.
	  * @throws IOException If the attempt to read the model fails.
	  */
	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		boolean found_closing_bracket = false;

		try
		{
			st.nextToken();
			if ( st.ttype != '{' )
				throw new IOException( "Poisson.pretty_input: input doesn't have opening bracket." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if ( st.ttype == StreamTokenizer.TT_WORD && st.sval.equals( "lambda" ) )
				{
					st.nextToken();
					lambda = Double.parseDouble( st.sval );
				}
				else if ( st.ttype == '}' )
				{
					found_closing_bracket = true;
					break;
				}
			}
		}
		catch (IOException e)
		{
			throw new IOException( "Poisson.pretty_input: attempt to read object failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "Poisson.pretty_input: no closing bracket on input." );

        allocate_cache();
	}

    void allocate_cache()
    {
        p_cache = new double [(int) (lambda+4*Math.sqrt(lambda)+10)];
        
        for (int i = 0; i < p_cache.length; i++)
        {
            p_cache[i] = compute_p(i);
        }

        cdf_cache = new double [p_cache.length];

        cdf_cache[0] = p_cache[0];
        for (int i = 1; i < cdf_cache.length; i++)
            cdf_cache[i] = p_cache[i] + cdf_cache[i-1];
    }

    public static void main (String[] args)
    {
        try
        {
            if (args.length == 0)
            {
                System.err.println ("Poisson.main: usage: java riso.distributions.Poisson lambda n");
                return;
            }

            double lambda = Double.parseDouble (args[0]);
            int n = Integer.parseInt (args[1]);
            System.err.println ("Poisson.main: generate "+n+" random numbers with mean "+lambda);

            Poisson p = new Poisson (lambda);

            for (int i = 0; i < n; i++)
                System.out.println (" "+p.random()[0]);
        }
        catch (Exception e) { e.printStackTrace(); }
    }
}
