package riso.approximation;
import java.rmi.*;
import riso.distributions.*;
import numerical.*;

/** An instance of this class is a helper to compute the difference in entropy
  * from first argument of the constructor (some distribution) to the second
  * (some other distribution). Typically the first distribution is a prior
  * distribution and the second is the posterior over the same variable as
  * the prior; the entropy of the first is generally greater than the
  * second, and so the value returned by <tt>do_compute_delta_h</tt> is
  * generally positive.
  */
public class ComputeDeltaH
{
	IntegralHelper1d ei1h, ei2h;

	public ComputeDeltaH( Distribution p1, Distribution p2 )
	{
		EntropyIntegrand ei1 = new EntropyIntegrand( p1 );
		EntropyIntegrand ei2 = new EntropyIntegrand( p2 );

		double[][] support1 = new double[1][], support2 = new double[1][];

		try
		{
			support1[0] = p1.effective_support( 1e-6 );
			support2[0] = p2.effective_support( 1e-6 );
		}
		catch (RemoteException e)
		{
			throw new RuntimeException( "ComputeDeltaH: attempt to construct helper failed; "+e );
		}

		ei1h = new IntegralHelper1d( ei1, support1, p1 instanceof Discrete );
		ei2h = new IntegralHelper1d( ei2, support2, p2 instanceof Discrete );
	}

	/** Returns the entropy of the first distribution minus the entropy
	  * of the second distribution.
	  */
	public double do_compute_delta_h() throws Exception
	{
		double e1, e2;

		try 
		{
			e1 = ei1h.do_integral();
			e2 = ei2h.do_integral();
		}
		catch (Exception e)
		{
			throw new Exception( "ComputeDeltaH.do_compute_delta_h: attempt failed; "+e );
		}

System.err.println( "ComputeDeltaH.do_compute_delta_h: entropy1: "+e1+", entropy2: "+e2 );
		return e1-e2;
	}
}
