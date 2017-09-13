package tools.valuefunction;
import java.util.ArrayList;
import java.util.Random;

// Implement functionality related to the Thresholded Lexicographic Ordering multiobjective action selection
// approach of Gabor et al.
// Written by Peter Vamplew, Nov 2015

public abstract class TLO {
	
	static Random r = new Random();
	
	// Compare two sets of values based on TLO ordering
	// Returns 1 if a is ranked higher, -1 if b is higher, 0 if they are equal
	// a and b should be of the same length, and thresholds should be 1 element shorter
	// (ie there is no threshold for the final element in a and b)
	public static int compare(double a[], double b[], double thresholds[])
	{
		double thresholdedA, thresholdedB;
		// first compare the thresholded values
		for (int i=0; i<thresholds.length; i++)
		{
			thresholdedA = Math.min(a[i], thresholds[i]);
			thresholdedB = Math.min(b[i], thresholds[i]);
			if (thresholdedA>thresholdedB)
				return 1;
			else if (thresholdedA<thresholdedB)
				return -1;
		}		
		// test based on the final, unthresholded value
		if (a[thresholds.length]>b[thresholds.length])
			return 1;
		else if (a[thresholds.length]<b[thresholds.length])
			return -1;			
		// equal based on thresholding values, so now test again without applying thresholds
		for (int i=0; i<thresholds.length; i++)
		{
			if (a[i]>b[i])
				return 1;
			else if (a[i]<b[i])
				return -1;
		}			
		// if we get here the two arrays must be exactly equal so return 0
		return 0;
	}
	
	// Returns the index of the highest-ranked action in the provided array
	public static int greedyAction(double actionValues[][], double thresholds[])
	{
        ArrayList<Integer> bestActions = new ArrayList<>();        
        bestActions.add(0);

        for (int a = 1; a < actionValues.length; a++) 
        {
        	int compareResult = compare(actionValues[a], actionValues[bestActions.get(0)], thresholds);
            if (compareResult>0) 
            {
            	bestActions.clear();
            	bestActions.add(a);
            } 
            else if (compareResult==0)
            {
                bestActions.add(a);
            }            
        }
       /* for (int i=0; i<bestActions.size(); i++)
        {
        	System.out.print(bestActions.get(i) + " ");
        }
        System.out.println();*/
        if (bestActions.size() > 1) 
        {
            return bestActions.get(r.nextInt(bestActions.size()));
        } 
        else 
        {
            return bestActions.get(0);
        }		
	}
	
	// Returns a score array with the dominance score of each action (ie the proportion of actions which this action is 
	// equal to our better than according to TLO comparisons)
	public static double[] getDominanceScore(double actionValues[][], double thresholds[])
	{
		double score[] = new double[actionValues.length];
        for (int a = 0; a < actionValues.length; a++)
        {
        	score[a] = 0;
        }
        for (int a = 0; a < actionValues.length-1; a++) 
        	for (int b=a+1; b<actionValues.length; b++)
        	{
        		int compareResult = compare(actionValues[a], actionValues[b], thresholds);	
        		if (compareResult>=0)
        			score[a]++;
        		if (compareResult<=0)
        			score[b]++;
        	}
        // scale to the range 0..1
        for (int a = 0; a < actionValues.length; a++)
        {
        	score[a] /= (actionValues.length-1); // numActions -1 as an action is not compared against itself
        }
        return score;
	}
	
	// Returns a score array with the inverse additive-epsilon score for each action (ie 1 - the maximum difference on any objective
	// between this action and the TLO-optimal action)
	public static double[] getInverseAdditiveEpsilonScore(double actionValues[][], int bestIndex)
	{
		int numObjectives = actionValues[0].length;
		double score[] = new double[actionValues.length];
		// first scale the values, so one objective with a wide range can't dominate the results
		double min[] = new double[numObjectives];
		double max[] = new double[numObjectives];
		for (int i=0; i<min.length; i++)
		{
			double tempMin = actionValues[0][i];
			double tempMax = tempMin;
	        for (int a = 1; a < actionValues.length; a++)
	        {
	        	if (actionValues[a][i]<tempMin)
	        		tempMin = actionValues[a][i];
	        	else if (actionValues[a][i]>tempMax)
	        		tempMax = actionValues[a][i];
	        }  
	        min[i] = tempMin;
	        max[i] = tempMax;
		}
        double scaledBest[] = new double[numObjectives];
        for (int i=0; i<scaledBest.length; i++)
        {
        	scaledBest[i] = (actionValues[bestIndex][i]-min[i])/(max[i]-min[i]);
        }
        // now calculate the additive epsilon for each action, scaling as we go
        // finally subtract the additive epsilon from 1 so better solutions get higher scores
        for (int a = 0; a < actionValues.length; a++)
        {
        	score[a] = 0.0;
        	for (int i=0; i<numObjectives; i++)
        	{
        		double diff = scaledBest[i] - (actionValues[a][i]-min[i])/(max[i]-min[i]);
        		if (diff>score[a])
        			score[a] = diff;
        	}
        	score[a] = 1.0 - score[a];
        }
        return score;        	
	}

}
