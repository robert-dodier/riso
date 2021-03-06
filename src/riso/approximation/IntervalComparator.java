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
package riso.approximation;
import riso.general.Comparator;

/** Helper class to make it possible to sort lists of intervals.
  */
public class IntervalComparator implements Comparator
{
	/** Returns true if the left endpoint of <tt>a</tt> is greater than the left endpoint of <tt>b</tt>.
	  */
	public boolean greater( Object a, Object b )
	{
		return ((double[])a)[0] > ((double[])b)[0];
	}
}
