package riso.distributions.computes_pi;
import java.util.*;
import riso.distributions.*;

/** @see PiHelper
  */
public class ConditionalDiscrete_Discrete implements PiHelper
{
	public Distribution compute_pi( ConditionalDistribution py_in, Distribution[] pi_messages ) throws Exception
	{
		int i, j, k;

		ConditionalDiscrete py = (ConditionalDiscrete) py_in;

		if ( py.ndimensions_child() != 1 )
			throw new IllegalArgumentException( "computes_pi.ConditionalDiscrete_Discrete.compute_pi: this node has "+py.ndimensions_child()+" dimensions (should have 1)"+"\n" );

		double[] ix = new double[ pi_messages.length ], iy = new double[1];
		double[] pye = new double[ py.dimensions_child[0] ];

		for ( iy[0] = 0; iy[0] < py.dimensions_child[0]; iy[0] += 1 )
			pye[(int)iy[0]] = prediction_summation( py, ix, iy, 0, pi_messages );

		Discrete pye_density = new Discrete();
		pye_density.ndims = 1;
		pye_density.dimensions = new int[1];
		pye_density.dimensions[0] = pye.length;
		pye_density.probabilities = (double[]) pye.clone();

		return pye_density;
	}

	double prediction_summation( ConditionalDiscrete py, double[] ix, double[] iy, int m, Distribution[] pxe ) throws Exception
	{
		// These loops ought to be recoded using daxpy-like efficient
		// code, but til then, this works.

		double sum = 0;

		Discrete pxe_m = (Discrete) pxe[m];

		for ( ix[m] = 0; ix[m] < pxe_m.probabilities.length; ix[m] += 1 )
		{
			double[] iix = new double[1];
			iix[0] = ix[m];

			if ( m == pxe.length-1 )
			{
				sum += pxe_m.p(iix) * py.p(iy,ix);
			}
			else
			{
				sum += pxe_m.p(iix) * prediction_summation( py, ix, iy, m+1, pxe );
			}
		}

		return sum;
	}
}
