package tools.valuefunction.interfaces;

import org.rlcommunity.rlglue.codec.types.Reward;

public abstract class ValueFunction {
    
    public abstract void calculateErrors(int action, int previousState, int greedyAction, int newState, double gamma, Reward reward);
    
    public abstract void calculateTerminalErrors(int action, int previousState, double gamma, Reward reward);
    
    /* Implementation of this functionality has been moved into the more sepcific classes where it belongs
    public abstract void calculateErrors(int action, int previousState, int greedyAction, int newState, double gamma[], Reward reward);
    
    public abstract void calculateTerminalErrors(int action, int previousState, double gamma[], Reward reward);*/
    
    public abstract void update(int action, int state, double lambda, double alpha);
    
    public abstract double[] getQValues(int action, int state);
    
    public abstract void resetQValues(double initValue[]);
    
}
