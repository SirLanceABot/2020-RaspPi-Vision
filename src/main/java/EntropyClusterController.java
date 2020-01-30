import java.util.Arrays;

public class EntropyClusterController
{
	public static int EntropyClusterControllerMain(int[] dataIn, int startY, int stopY)
	{
		// squish the zeros out of the array
		int NUM_POINTS = stopY - startY + 1;

		if(NUM_POINTS == 0) return 0;

		//int NUM_DIM = 1;
		int NUM_DIM = 2; // 2 Dimensions

		data_point[] point = new data_point[NUM_POINTS];
		data_point[] pointSave = new data_point[NUM_POINTS];

		for(int i = 0; i < NUM_POINTS; i++)
		{
			point[i] = new data_point(NUM_DIM);
			point[i].dimData[0] = dataIn[i+startY];
			point[i].dimData[1] = i; // 2D

			pointSave[i] = new data_point(NUM_DIM);	// save data for later (cluster destroys the input by normalizing it)
			pointSave[i].dimData[0] = point[i].dimData[0];
		}

		int num_clusters;
		float beta = .7f; // required similarity
		float gamma = .20f; // fraction of number of points
		// .7 similarity is a good first guess
		//  0. for fraction of num points that must be neighbors to be a hub for no outliers
		// .05 is good first guess if outliers not to be considered as hubs
		num_clusters = EntropyCluster.cluster( point, NUM_DIM, NUM_POINTS, beta, gamma );

		int demarcation = -1; // Y point splitting between hubs initialze

		// // not sure if this counting in bins will be of any use
		// // space for bins to count points in a cluster - one bin per cluster
		// int[] binHub = new int[num_clusters];

		// // zero out bins to prepare for counting
		// for (int i=0; i<num_clusters; i++) binHub[i] = 0;

		// //look at all points in all clusters and put in proper bin ( negative cluster number is removed out lier)
		// for (int i=0; i<NUM_POINTS; i++) if(point[i].cluster >= 0) binHub[point[i].cluster]++;

		//TODO print hub center point; figure out extreme points of each hub; draw lines at those hub boundaries

		//Arrays.sort(point); // sort built-in the data_point class - hub, image Y ascending
		
		// save highest (lowest number) in first sequence not an outlier (cluster or hub = -1 is outlier)
		// Start at the bottom of the contour, skip the outliers, find the highest of the non-outliers
		// in the sequence that started at the bottom

		// top - lowest index
		//  good points
		//  outliers
		//  good points
		//  outliers
		//  good points save the top-most, lowest index point
		//  outliers
		// bottom - highest index (+ startY)

		for (int i=NUM_POINTS-1; i>=0; i--)
			{
			if (demarcation < 0 && point[i].hub < 0) continue; // skip bottom outliers
			if (point[i].hub >= 0) demarcation = point[i].hub + startY; // assumes hub is also original image Y
			else break;
			}
		// other cluster boundaries could be of use, too, but nothing done with that info yet.

		// debug output

		// System.out.println("[EntropyClusterController] similarity beta = " + beta + " fraction gamma = " + gamma
		// 	+ " Number of clusters = " + num_clusters);

		// System.out.print("Count points per cluster");
		// for (int i=0; i<num_clusters; i++) System.out.print(" " + binHub[i]);
		// System.out.println();


		// System.out.format("\n Point Cluster     Hub\n" +
		// 		"Number  Number  Number  Original data Normalized data\n");
		// for (int i=0; i<NUM_POINTS; i++)
		// {
		// 	System.out.format("%6d  %6d  %6d", i, point[i].cluster, point[i].hub);		
		// 	for (int j=0; j<NUM_DIM; j++) System.out.format(" %10.2f %10.2f", pointSave[i].dimData[j], point[i].dimData[j]);		
		// 	System.out.format("\n");
		// }

		// for (int i=0; i<NUM_POINTS; i++)
		// {
		// 	System.out.println(point[i]);
		// }

		return demarcation;
	}
}
/*
[EntropyClusterController] similarity beta = 0.7 fraction gamma = 0.2 Number of clusters = 4

Cluster -1, hub -1, image Y 0.000000, width 1.000000
Cluster -1, hub -1, image Y 0.008547, width 1.000000
Cluster -1, hub -1, image Y 0.017094, width 0.988764
Cluster -1, hub -1, image Y 0.025641, width 0.966292
Cluster -1, hub -1, image Y 0.034188, width 0.943820
Cluster -1, hub -1, image Y 0.042735, width 0.921348
Cluster -1, hub -1, image Y 0.051282, width 0.910112
Cluster -1, hub -1, image Y 0.059829, width 0.887640
Cluster -1, hub -1, image Y 0.068376, width 0.853933
Cluster -1, hub -1, image Y 0.076923, width 0.797753
Cluster -1, hub -1, image Y 0.085470, width 0.752809
Cluster -1, hub -1, image Y 0.094017, width 0.696629
Cluster -1, hub -1, image Y 0.102564, width 0.629214
Cluster -1, hub -1, image Y 0.111111, width 0.449438
Cluster -1, hub -1, image Y 0.119658, width 0.337079
Cluster -1, hub -1, image Y 0.128205, width 0.213483
Cluster 2, hub 26, image Y 0.136752, width 0.011236
Cluster 2, hub 26, image Y 0.145299, width 0.011236
Cluster 2, hub 26, image Y 0.153846, width 0.000000
Cluster 2, hub 26, image Y 0.162393, width 0.000000
Cluster 2, hub 26, image Y 0.170940, width 0.000000
Cluster 2, hub 26, image Y 0.179487, width 0.011236
Cluster 2, hub 26, image Y 0.188034, width 0.011236
Cluster 2, hub 26, image Y 0.196581, width 0.011236
Cluster 2, hub 26, image Y 0.205128, width 0.011236
Cluster 2, hub 26, image Y 0.213675, width 0.011236
Cluster 2, hub 26, image Y 0.222222, width 0.011236
Cluster 2, hub 26, image Y 0.230769, width 0.011236
Cluster 2, hub 26, image Y 0.239316, width 0.011236
Cluster 2, hub 26, image Y 0.247863, width 0.011236
Cluster 2, hub 26, image Y 0.256410, width 0.022472
Cluster 2, hub 26, image Y 0.264957, width 0.022472
Cluster 2, hub 26, image Y 0.273504, width 0.011236
Cluster 2, hub 26, image Y 0.282051, width 0.022472
Cluster 2, hub 26, image Y 0.290598, width 0.022472
Cluster 2, hub 26, image Y 0.299145, width 0.022472
Cluster 2, hub 26, image Y 0.307692, width 0.022472
Cluster 2, hub 26, image Y 0.316239, width 0.022472
Cluster 1, hub 60, image Y 0.324786, width 0.033708
Cluster 1, hub 60, image Y 0.333333, width 0.033708
Cluster 1, hub 60, image Y 0.341880, width 0.033708
Cluster 1, hub 60, image Y 0.350427, width 0.033708
Cluster 1, hub 60, image Y 0.358974, width 0.044944
Cluster 1, hub 60, image Y 0.367521, width 0.044944
Cluster 1, hub 60, image Y 0.376068, width 0.044944
Cluster 1, hub 60, image Y 0.384615, width 0.044944
Cluster 1, hub 60, image Y 0.393162, width 0.044944
Cluster 1, hub 60, image Y 0.401709, width 0.044944
Cluster 1, hub 60, image Y 0.410256, width 0.056180
Cluster 1, hub 60, image Y 0.418803, width 0.056180
Cluster 1, hub 60, image Y 0.427350, width 0.056180
Cluster 1, hub 60, image Y 0.435897, width 0.067416
Cluster 1, hub 60, image Y 0.444444, width 0.067416
Cluster 1, hub 60, image Y 0.452991, width 0.067416
Cluster 1, hub 60, image Y 0.461538, width 0.056180
Cluster 1, hub 60, image Y 0.470085, width 0.056180
Cluster 1, hub 60, image Y 0.478632, width 0.056180
Cluster 1, hub 60, image Y 0.487179, width 0.056180
Cluster 1, hub 60, image Y 0.495726, width 0.067416
Cluster 1, hub 60, image Y 0.504274, width 0.067416
Cluster 1, hub 60, image Y 0.512821, width 0.067416
Cluster 1, hub 60, image Y 0.521368, width 0.067416
Cluster 1, hub 60, image Y 0.529915, width 0.067416
Cluster 1, hub 60, image Y 0.538462, width 0.078652
Cluster 1, hub 60, image Y 0.547009, width 0.078652
Cluster 1, hub 60, image Y 0.555556, width 0.078652
Cluster 1, hub 60, image Y 0.564103, width 0.078652
Cluster 1, hub 60, image Y 0.572650, width 0.089888
Cluster 1, hub 60, image Y 0.581197, width 0.089888
Cluster 1, hub 60, image Y 0.589744, width 0.089888
Cluster 1, hub 60, image Y 0.598291, width 0.089888
Cluster 1, hub 60, image Y 0.606838, width 0.089888
Cluster 1, hub 60, image Y 0.615385, width 0.101124
Cluster 1, hub 60, image Y 0.623932, width 0.101124
Cluster 1, hub 60, image Y 0.632479, width 0.101124
Cluster 1, hub 60, image Y 0.641026, width 0.101124
Cluster 1, hub 60, image Y 0.649573, width 0.112360
Cluster 1, hub 60, image Y 0.658120, width 0.101124
Cluster 1, hub 60, image Y 0.666667, width 0.101124
Cluster 1, hub 60, image Y 0.675214, width 0.101124
Cluster 1, hub 60, image Y 0.683761, width 0.112360
Cluster 1, hub 60, image Y 0.692308, width 0.112360
Cluster 3, hub 85, image Y 0.700855, width 0.112360
Cluster 3, hub 85, image Y 0.709402, width 0.112360
Cluster 3, hub 85, image Y 0.717949, width 0.112360
Cluster 3, hub 85, image Y 0.726496, width 0.112360
Cluster 3, hub 85, image Y 0.735043, width 0.123596
Cluster 3, hub 85, image Y 0.743590, width 0.123596
Cluster 3, hub 85, image Y 0.752137, width 0.123596
Cluster 0, hub 110, image Y 0.760684, width 0.123596
Cluster 0, hub 110, image Y 0.769231, width 0.123596
Cluster 0, hub 110, image Y 0.777778, width 0.134831
Cluster 0, hub 110, image Y 0.786325, width 0.134831
Cluster 0, hub 110, image Y 0.794872, width 0.134831
Cluster 0, hub 110, image Y 0.803419, width 0.134831
Cluster 0, hub 110, image Y 0.811966, width 0.146067
Cluster 0, hub 110, image Y 0.820513, width 0.146067
Cluster 0, hub 110, image Y 0.829060, width 0.146067
Cluster 0, hub 110, image Y 0.837607, width 0.146067
Cluster 0, hub 110, image Y 0.846154, width 0.157303
Cluster 0, hub 110, image Y 0.854701, width 0.157303
Cluster 0, hub 110, image Y 0.863248, width 0.157303
Cluster 0, hub 110, image Y 0.871795, width 0.157303
Cluster 0, hub 110, image Y 0.880342, width 0.157303
Cluster 0, hub 110, image Y 0.888889, width 0.168539
Cluster 0, hub 110, image Y 0.897436, width 0.168539
Cluster 0, hub 110, image Y 0.905983, width 0.168539
Cluster 0, hub 110, image Y 0.914530, width 0.157303
Cluster 0, hub 110, image Y 0.923077, width 0.168539
Cluster 0, hub 110, image Y 0.931624, width 0.168539
Cluster 0, hub 110, image Y 0.940171, width 0.168539
Cluster 0, hub 110, image Y 0.948718, width 0.168539
Cluster 0, hub 110, image Y 0.957265, width 0.168539
Cluster 0, hub 110, image Y 0.965812, width 0.179775
Cluster 0, hub 110, image Y 0.974359, width 0.179775
Cluster 0, hub 110, image Y 0.982906, width 0.179775
Cluster 0, hub 110, image Y 0.991453, width 0.179775
Cluster 0, hub 110, image Y 1.000000, width 0.179775
*/