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
package riso.belief_nets;

/** This exception is thrown when a parent variable referred to in a 
  * belief network cannot be located. This may be because the parent doesn't
  * exist in the current belief network or a referred-to belief network,
  * or because a referred-to belief network cannot be located.
  * @see UnknownNetworkException
  */
public class UnknownParentException extends java.rmi.RemoteException
{
	public UnknownParentException() { super(); }
	public UnknownParentException(String s) { super(s); }
}
