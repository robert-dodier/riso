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
package riso.distributions;
import java.io.*;
import java.rmi.*;

/** Interface for all unconditional distribution models. Note that an
  * unconditional distribution is a special kind of conditional distribution.
  */
public interface Distribution extends ConditionalDistribution
{
	/** Return the number of dimensions in which this distribution lives.
	  */
	public int ndimensions();

	/** Compute the cumulative distribution function.
	  * @return Mass of the distribution to the left of <tt>x</tt>.
	  */
	public double cdf( double x ) throws Exception;

	/** Compute the density at the point <code>x</code>.
	  */
	public double p( double[] x ) throws Exception;

	/** Compute the logarithm of the density at the point <tt>x</tt>.
	  */
	public double log_p( double[] x ) throws Exception;

	/** Computes the log of the prior probability of the parameters of
	  * this distribution, assuming some prior distribution has been 
	  * established. This may not be meaningful for all distributions.
	  */
	public double log_prior() throws Exception;

	/** Return an instance of a random variable from this distribution.
	  */
	public double[] random() throws Exception;

	/** Use data to modify the parameters of the distribution. Classes which
	  * implement this method will typically use maximum likelihood or
	  * a similar approach to fit the parameters to the data.
	  * @param x The data. Each row has a number of components equal to the
	  *   number of dimensions of the model, and the number of rows is the
	  *   number of data.
	  * @param responsibility Each component of this vector 
	  *   <code>responsibility[i]</code> is a scalar telling the probability
	  *   that this distribution produced the corresponding datum <code>x[i]</code>.
	  *   This is mostly intended for fitting mixture distributions, although
	  *   other uses can be imagined.
	  * @param niter_max Maximum number of iterations of the update algorithm,
	  *   if applicable.
	  * @param stopping_criterion A number which describes when to stop the
	  *   update algorithm, if applicable.
	  * @return Some indication of goodness-of-fit, such as MSE or negative
	  *   log-likelihood.
	  * @throws Exception If the update algorithm fails; if no exception is
	  *   thrown, the algorithm succeeded.
	  */
	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception;

	/** Returns the expected value of this distribution.
	  */
	public double expected_value() throws Exception;

	/** Returns the square root of the variance of this distribution.
	  */
	public double sqrt_variance() throws Exception;

	/** Returns the support of this distribution, if it is a finite interval;
	  * otherwise returns an interval which contains almost all of the mass.
	  * @param epsilon If an approximation is made, this much mass or less
	  *   lies outside the interval which is returned.
	  * @return An interval represented as a 2-element array.
	  */
	public double[] effective_support( double epsilon ) throws Exception;

	/** Returns a Gaussian mixture which is a reasonable initial
	  * approximation to this distribution. The initial approximation
	  * should be further adjusted before using it to compute probabilities
	  * and what-not; the initial mixture can be a very rough approximation.
	  *
	  * @param support Region of interest; concentrate the mixture here.
	  *   This argument is of greatest interest to approximations of likelihood
	  *   functions, which may be defined everywhere but uninteresting in
	  *   most places. It is NOT guaranteed that the mixture returned has
	  *   support contained within <tt>support</tt>.
	  */
	public MixGaussians initial_mix( double[] support ) throws Exception;
}
