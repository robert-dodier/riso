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
package riso.test;
import java.io.*;
import java.rmi.*;
import riso.distributions.*;
import riso.numerical.*;
import riso.general.*;

public class ClassloaderTest extends Gamma
{
	public ClassloaderTest() {}

	public int ndimensions() { return 1; }

	public double p( double[] x )
	{
		throw new RuntimeException( "ClassloaderTest.p: not implemented." );
	}

	public double log_prior() throws Exception
	{
		throw new Exception( "ClassloaderTest.log_prior: not implemented." );
	}

	public double[] random() throws Exception
	{
		throw new Exception( "ClassloaderTest.random: not implemented." );
	}

	public double update( double[][] x, double[] responsibility, int niter_max, double stopping_criterion ) throws Exception
	{
		throw new Exception( "ClassloaderTest.update: not implemented." );
	}

	public double expected_value()
	{
		throw new RuntimeException( "ClassloaderTest.expected_value: not implemented." );
	}

	public double sqrt_variance()
	{
		throw new RuntimeException( "ClassloaderTest.sqrt_variance: not implemented." );
	}

	public double[] effective_support( double epsilon ) throws Exception
	{
		throw new Exception( "ClassloaderTest.effective_support: not implemented." );
	}

	public String format_string( String leading_ws )
	{
		String result = "";
		result += this.getClass().getName()+"\n";
		return result;
	}

	public void pretty_input( SmarterTokenizer st ) throws IOException
	{
		return;
	}
}
