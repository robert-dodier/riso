package regression;

/** Java doesn't support the notion of a pointer to a function, so
  * here's one way of working around that.
  */
public interface FunctionCaller
{
	public double call_function( double x );
	public double call_derivative( double y );
}

