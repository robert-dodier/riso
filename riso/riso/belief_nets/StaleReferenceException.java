package riso.belief_nets;

/** This exception is thrown when a method of a stale object is executed.
  */
public class StaleReferenceException extends java.rmi.RemoteException
{
	public StaleReferenceException() { super(); }
	public StaleReferenceException(String s) { super(s); }
}
