package belief_nets;

/** This exception is thrown when a parent variable referred to in a 
  * belief network cannot be located. This may be because the parent doesn't
  * exist in the current belief network or a referred-to belief network,
  * or because a referred-to belief network cannot be located.
  * @see UnknownNetworkException
  */
public class UnknownParentException extends Exception
{
	public UnknownParentException() { super(); }
	public UnknownParentException(String s) { super(s); }
}
