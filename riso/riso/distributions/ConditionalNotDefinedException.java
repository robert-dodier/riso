package riso.distributions;

/** This exception is thrown when a conditional probability is not
  * defined for the given value of the parent or parents.
  */
public class ConditionalNotDefinedException extends Exception
{
	public ConditionalNotDefinedException() { super(); }
	public ConditionalNotDefinedException(String s) { super(s); }
}
