package riso.approximation;
import java.rmi.*;
import riso.distributions.*;
import numerical.*;

public class Joint2ConditionalGaussMix
{
	/** HANDLES ONLY A SINGLE GAUSSIAN FOR NOW!!!
	  */
	public static ConditionalGaussian compute_conditional( Gaussian p, int[] parent_indexes ) throws RemoteException
	{
		int i, j;

		double[][] S = p.get_Sigma();
		double[] m = (double[]) p.mu.clone();

		int nparents = parent_indexes.length;
		int nchildren = S.length - nparents;

		double[][] S11 = new double[nchildren][nchildren], S22 = new double[nparents][nparents];
		double[][] S12 = new double[nchildren][nparents];
		double[] m1 = new double[nchildren], m2 = new double[nparents];

		int[] children_indexes = new int[ nchildren ];
		boolean[] is_parent = new boolean[ S.length ];
		for ( i = 0; i < nparents; i++ )
			is_parent[ parent_indexes[i] ] = true;

		for ( i = 0, j = 0; i < S.length; i++ )
			if ( ! is_parent[i] )
				children_indexes[j++] = i;
		
System.err.print( "parent indexes: " ); for ( i = 0; i < nparents; i++ ) System.err.print( parent_indexes[i]+" " ); System.err.println("");
System.err.print( "children indexes: " ); for ( i = 0; i < nchildren; i++ ) System.err.print( children_indexes[i]+" " ); System.err.println("");

		for ( i = 0; i < nparents; i++ )
			for ( j = 0; j < nparents; j++ )
				S22[i][j] = S[ parent_indexes[i] ][ parent_indexes[j] ];

		for ( i = 0; i < nchildren; i++ )
			for ( j = 0; j < nchildren; j++ )
				S11[i][j] = S[ children_indexes[i] ][ children_indexes[j] ];

		for ( i = 0; i < nchildren; i++ )
			for ( j = 0; j < nparents; j++ )
				S12[i][j] = S[ children_indexes[i] ][ parent_indexes[j] ];
		
		for ( i = 0; i < nparents; i++ ) m2[i] = m[ parent_indexes[i] ];
		for ( i = 0; i < nchildren; i++ ) m1[i] = m[ children_indexes[i] ];

		double[][] S22_inverse = Matrix.inverse( S22 );

		double[][] S21 = new double[nparents][nchildren];
		for ( i = 0; i < nchildren; i++ )
			for ( j = 0; j < nparents; j++ )
				S21[j][i] = S12[i][j];

		double[][] S122221 = Matrix.multiply( S12, Matrix.multiply( S22_inverse, S21 ) );

		double[][] S1given2 = (double[][]) S11.clone();
		for ( i = 0; i < nchildren; i++ )
			for ( j = 0; j < nchildren; j++ )
				S1given2[i][j] -= S122221[i][j];

		double[][] a_mu = Matrix.multiply( S12, S22_inverse );
		double[] b_mu = (double[]) m1.clone();
		Matrix.axpby( 1, b_mu, -1, Matrix.multiply( a_mu, m2 ) );

		ConditionalGaussian cg = new ConditionalGaussian();

		cg.Sigma_22 = S22;
		cg.a_mu_1c2 = a_mu;
		cg.b_mu_1c2 = b_mu;
		cg.Sigma_1c2 = S1given2;

		return cg;
	}

	public static void main( String[] args )
	{
		try
		{
			double[][] S = new double[3][3];
			double[] m = new double[3];

			m[0] = 1; m[1] = 2; m[2] = 3;

			S[0][0] = 10; S[1][1] = 20; S[2][2] = 30;
			S[0][2] = S[2][0] = 7;
			S[0][1] = S[1][0] = 5;
			S[1][2] = S[2][1] = 9;

			Gaussian g123 = new Gaussian( m, S );

			int[] parent_indexes = new int[2];
			parent_indexes[0] = 1; parent_indexes[1] = 2;

			ConditionalGaussian cg = compute_conditional( g123, parent_indexes );
			System.err.println( "cg: "+cg.format_string( "" ) );
		}
		catch (Exception e)
		{	
			e.printStackTrace();
		}

		System.exit(0);
	}
}
