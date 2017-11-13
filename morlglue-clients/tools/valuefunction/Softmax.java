// Implements a softmax function for use in exploration
package tools.valuefunction;
import java.util.Random;

public abstract class Softmax 
{
	static Random r = new Random(548);
	
	// Performs softmax selection. Should an error occur in the calculations as temperature gets too low, we detect this
	// and simply return the greedy action instead
	public static int getAction(double actionValues[], double temperature, int greedyAction)
	{
		int numActions = actionValues.length;
		double sumOfSoftmaxTerms[] = new double[numActions];
		sumOfSoftmaxTerms[0] = Math.exp(actionValues[0]/temperature);
		for (int a=1; a<numActions; a++)
		{
			sumOfSoftmaxTerms[a] = Math.exp(actionValues[a]/temperature) + sumOfSoftmaxTerms[a-1];
			if (Double.isInfinite(sumOfSoftmaxTerms[a]))
			{
				return greedyAction;
			}
		}
		double nextRandom = r.nextDouble();
		int selectedAction = 0;
		while((sumOfSoftmaxTerms[selectedAction]/sumOfSoftmaxTerms[numActions-1])<nextRandom)
		{
			selectedAction++;
		}
		return selectedAction;
	}

}
