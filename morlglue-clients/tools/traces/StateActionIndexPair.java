// A re-write of Rustam's StateActionDiscrete class to make it easier to support the augmented state information required for
// the SatisficingMOAgent. Instead of storing the Observation, it just stores the state index directly which does away with the need for 
// a StateConverter.
package tools.traces;

import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import tools.staterep.interfaces.StateConverter;


public class StateActionIndexPair {
    private int stateIndex; 
    private Action action;
    
    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public int getState() {
        return stateIndex;
    }

    public void setState(int state) {
        stateIndex = state;
    }

    public StateActionIndexPair(int state, Action action) {
        stateIndex = state;
        this.action = action;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final StateActionIndexPair other = (StateActionIndexPair) obj;
        
        if (stateIndex != other.stateIndex ){
            return false;
        }
        if (this.action.getInt(0) != other.action.getInt(0) ) {
            return false;
        }
        return true;
    }

    
}
