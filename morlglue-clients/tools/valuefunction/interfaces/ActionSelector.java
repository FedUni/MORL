package tools.valuefunction.interfaces;

public interface ActionSelector {
    
    public int chooseGreedyAction(int state);
    
    public int choosePossiblyExploratoryAction(double parameter, int state);
    
    public boolean isGreedy(int state, int action);
    
}
