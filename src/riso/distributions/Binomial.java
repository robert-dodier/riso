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

 /* An instance of this class represents the distribution of a counting
  * process in which zero or one counts in a unit time interval can be
  * directly observed, but two or more are truncated (in the survival
  * analysis sense). It is assumed that successive unit time intervals
  * are independent. Then the total count in <tt>n</tt> intervals is
  * binomial with parameters <tt>n</tt> and <tt>p = 1 - exp(-t/a)</tt> ???
  * RECORD THIS IN NOTEBOOK AND STRIKE THIS COMMENT !!!
  */

/** An instance of this class represents a binomial distribution.
  */
public class Binomial extends AbstractDistribution
{
    /** Number of trials for this distribution.
      */
	protected double n_trials;

    /** Probability of "success" for this distribution.
      */
	protected double p_success;

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
		Binomial copy = (Binomial) super.clone();
		copy.n_trials = this.n_trials;
		copy.p_success = this.p_success;
        copy.p_cache = (double[]) this.p_cache.clone();
        copy.cdf_cache = (double[]) this.cdf_cache.clone();
		return copy;
	}

	/** Constructs a binomial distribution with specified parameters.
	  */
	public Binomial (double n_trials, double p_success)
	{
		this.n_trials = n_trials;
        this.p_success = p_success;
        allocate_cache();
	}

	/** Default constructor for this class. 
      * Sets <tt>n_trials</tt> to 1 and <tt>p_success</tt> to 1/2.
	  */
	public Binomial()
    {
        n_trials = 1;
        p_success = 0.5;
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

    /** Returns <tt>(n choose x) p^x (1-p)^(n-x)</tt>.
      */
    public double compute_p (double x)
    {
        if (p_success == 0)
        {
            if (x == 0) return 1; else return 0;
        }
        else if (p_success == 1)
        {
            if (x == n_trials) return 1; else return 0;
        }
        else
        {
            // beta(p,q) == lgamma(p) + lgamma(q) - lgamma(p+q), 
            // a! == gamma(a+1), => (n choose x) == 1/beta(x+1,n-x+1) 1/(n+1)

            double log_n_choose_x = -SpecialMath.logBeta (x+1, n_trials-x+1) -Math.log (n_trials+1);
            double expt = log_n_choose_x + x * Math.log (p_success) + (n_trials - x) * Math.log (1-p_success);
            return Math.exp (expt);
        }
    }

	/** Computes the log of the prior probability of the parameters of
	  * this distribution, assuming some prior distribution has been 
	  * established. This may not be meaningful for all distributions.
	  */
	public double log_prior() throws Exception
	{
		throw new Exception( "Binomial.log_prior: not implemented." );
	}

	/** Return an instance of a random variable from this distribution.
      * If <tt>n</tt> is small, use a direct method. 
      * Otherwise use a rejection method. 
      * In both cases, the samples are exactly binomial; this method
      * does not use Gaussian or other approximations.
	  */
	public double[] random() throws Exception
	{
        double[] x = new double [1];

        if (n_trials < cdf_cache.length) // given current caching scheme, this test always succeeds !!!
        {
            // Direct method.

            double U = Math.random ();

            int i = 0;
            while (i <= n_trials && U > cdf_cache[i])
                ++i;

            x[0] = i;
        }
        else
        {
            // Rejection method.
            throw new Exception ("Binomial.random: rejection method not implemented.");
        }
            
        return x;
	}

	/** Use data to modify the parameters of the distribution.
	  * This method is not implemented.
	  */
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception
	{
		throw new Exception( "Binomial.update: not implemented." );
	}

	/** Returns the expected value of this distribution.
	  * This is equal to <tt>n_trials * p_success</tt>.
	  */
	public double expected_value() 
	{
		return n_trials * p_success;
	}

	/** Returns the square root of the variance of this distribution.
	  * This is equal to the square root of <tt>n_trials * p_success * (1-p_success)</tt>.
	  */
	public double sqrt_variance()
	{
		return Math.sqrt (n_trials * p_success * (1-p_success));
	}

	/** Returns an interval which contains almost all the mass of this
	  * distribution; uses a numerical search to find <tt>x</tt> such that
	  * the tail mass to the right of <tt>x</tt> is less than <tt>epsilon</tt>.
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
	  */
	public String format_string( String leading_ws )
	{
		String result = "";
		result += this.getClass().getName()+"\n"+"{"+"\n";
		result += leading_ws+"\t"+"n"+n_trials+"\n";
		result += leading_ws+"\t"+"p"+p_success+"\n";
		result += leading_ws+"}"+"\n";
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
				throw new IOException( "Binomial.pretty_input: input doesn't have opening bracket." );

			for ( st.nextToken(); !found_closing_bracket && st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				if (st.ttype == StreamTokenizer.TT_WORD && st.sval.equals("n"))
				{
					st.nextToken();
					n_trials = Double.parseDouble( st.sval );
				}
				else if (st.ttype == StreamTokenizer.TT_WORD && st.sval.equals("p"))
				{
					st.nextToken();
					p_success = Double.parseDouble( st.sval );
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
			throw new IOException( "Binomial.pretty_input: attempt to read object failed:\n"+e );
		}

		if ( ! found_closing_bracket )
			throw new IOException( "Binomial.pretty_input: no closing bracket on input." );

        allocate_cache();
	}

    void allocate_cache()
    {
        p_cache = new double [(int) n_trials+1];
        
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
                System.err.println ("Binomial.main: usage: java riso.distributions.Binomial ntrials psuccess n");
                return;
            }

            double ntrials = Double.parseDouble (args[0]);
            double psuccess = Double.parseDouble (args[1]);
            int n = Integer.parseInt (args[2]);
            System.err.println ("Binomial.main: generate "+n+" random numbers with parameters "+ntrials+" and "+psuccess);

            Binomial p = new Binomial (ntrials, psuccess);

            for (int i = 0; i < n; i++)
                System.out.println (" "+p.random()[0]);
        }
        catch (Exception e) { e.printStackTrace(); }
    }
}
