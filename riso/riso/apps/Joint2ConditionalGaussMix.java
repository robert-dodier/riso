package riso.approximation;
import java.rmi.*;
import riso.distributions.*;
import numerical.*;

public class Joint2ConditionalGaussMix
{
	public static MixConditionalGaussians compute_mix_conditional( MixGaussians joint_mix, int[] parent_indexes ) throws RemoteException
	{
		int i, nchildren = joint_mix.ndimensions() - parent_indexes.length;

		MixConditionalGaussians cond_mix = new MixConditionalGaussians();
		cond_mix.components = new ConditionalGaussian[ joint_mix.ncomponents() ];

		for ( i = 0; i < joint_mix.ncomponents(); i++ )
			cond_mix.components[i] = compute_one_conditional( (Gaussian)joint_mix.components[i], parent_indexes );

		cond_mix.parent_marginal = new MixGaussians( parent_indexes.length, joint_mix.ncomponents() );
		
		for ( i = 0; i < joint_mix.ncomponents(); i++ )
		{
			cond_mix.parent_marginal.components[i] = compute_one_marginal( (Gaussian)joint_mix.components[i], parent_indexes );
			cond_mix.parent_marginal.mix_proportions[i] = joint_mix.mix_proportions[i];
		}

		return cond_mix;
	}

	public static Gaussian compute_one_marginal( Gaussian p, int[] parent_indexes ) throws RemoteException
	{
		int i, j;

		double[] m = (double[]) p.mu.clone();
		double[][] S = p.get_Sigma();

		int nparents = parent_indexes.length;

		double[] m2 = new double[nparents];
		double[][] S22 = new double[nparents][nparents];

		for ( i = 0; i < nparents; i++ )
			m2[i] = m[ parent_indexes[i] ];

		for ( i = 0; i < nparents; i++ )
			for ( j = 0; j < nparents; j++ )
				S22[i][j] = S[ parent_indexes[i] ][ parent_indexes[j] ];
		
		return new Gaussian( m2, S22 );
	}

	public static ConditionalGaussian compute_one_conditional( Gaussian p, int[] parent_indexes ) throws RemoteException
	{
		int i, j;

		double[] m = (double[]) p.mu.clone();
		double[][] S = p.get_Sigma();

		int nparents = parent_indexes.length;
		int nchildren = S.length - nparents;

		double[] m1 = new double[nchildren], m2 = new double[nparents];
		double[][] S11 = new double[nchildren][nchildren], S22 = new double[nparents][nparents];
		double[][] S12 = new double[nchildren][nparents];

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

			Gaussian g1 = new Gaussian( m, S );

			m[0] = 10; m[1] = 20; m[2] = 30;
			S[0][0] = 100; S[1][1] = 200; S[2][2] = 300;
			Gaussian g2 = new Gaussian( m, S );

			MixGaussians mix = new MixGaussians( 3, 2 );
			mix.components[0] = g1;
			mix.components[1] = g2;
			mix.mix_proportions[0] = 0.9;
			mix.mix_proportions[1] = 0.1;

			int[] parent_indexes = new int[2];
			parent_indexes[0] = 1; parent_indexes[1] = 2;

			MixConditionalGaussians cg = compute_mix_conditional( mix, parent_indexes );
			System.err.println( "cg: "+cg.format_string( "" ) );

			Distribution xsection1 = cg.get_density( ((Gaussian)cg.parent_marginal.components[0]).mu );
			Distribution xsection2 = cg.get_density( ((Gaussian)cg.parent_marginal.components[1]).mu );

			System.err.println( "xsection1: "+xsection1.format_string("") );
			System.err.println( "xsection2: "+xsection2.format_string("") );
		}
		catch (Exception e)
		{	
			e.printStackTrace();
		}

		System.exit(0);
	}
}
