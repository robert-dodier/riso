package riso.distributions;

/** This exception is thrown when the effective support for a distribution
  * cannot be computed because the effective support is not well-defined.
  * Either the effective support is unbounded, or it is very large.
  */
public class SupportNotWellDefinedException extends java.rmi.RemoteException
{
	public SupportNotWellDefinedException() { super(); }
	public SupportNotWellDefinedException(String s) { super(s); }
}
