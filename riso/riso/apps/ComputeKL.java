package riso.approximation;
import java.rmi.*;
import riso.distributions.*;
import numerical.*;

/** An instance of this class is a helper to compute the Kullback-Liebler
  * asymmetric divergence from the first argument of the constructor 
  * (some distribution) to the second (some other distribution).
  */
public class ComputeKL
{
	IntegralHelper1d ceih, eih;

	public ComputeKL( Distribution p1, Distribution p2 )
	{
        CrossEntropyIntegrand cei = new CrossEntropyIntegrand( p1, p2 );
		EntropyIntegrand ei = new EntropyIntegrand( p1 );

		double[][] support = new double[1][];

		try { support[0] = p1.effective_support( 1e-6 ); }
		catch (RemoteException e) { throw new RuntimeException( "ComputeKL: attempt to construct helper failed; "+e ); }

		boolean is_discrete = p1 instanceof Discrete;
		ceih = new IntegralHelper1d( cei, support, is_discrete );
		eih = new IntegralHelper1d( ei, support, is_discrete );
	}

	public double do_compute_kl() throws Exception
	{
		double ce, e;

		try 
		{
			ce = ceih.do_integral();
			e = eih.do_integral();
		}
		catch (Exception ex)
		{
			throw new Exception( "ComputeKL.do_compute_kl: attempt failed; "+ex );
		}

System.err.println( "ComputeKL.do_compute_kl: entropy: "+e+"  cross-entropy: "+ce );
		return ce-e;
	}
}
