package tools.traces;

import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import tools.staterep.interfaces.StateConverter;

/**
 *
 * @author rustam
 */
public class StateActionDiscrete {
    private Observation observation; 
    private Action action;
    private static StateConverter stateConverter;

    public static void setStateConverter(StateConverter stateConverter) {
        StateActionDiscrete.stateConverter = stateConverter;
    }
    
    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public Observation getObservation() {
        return observation;
    }

    public void setObservation(Observation observation) {
        this.observation = observation;
    }

    public StateActionDiscrete(Observation observation, Action action) {
        this.observation = observation;
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
        final StateActionDiscrete other = (StateActionDiscrete) obj;
        
        if ( stateConverter.getStateNumber( this.observation ) != stateConverter.getStateNumber( other.observation ) ) {
            return false;
        }
        if (this.action.getInt(0) != other.action.getInt(0) ) {
            return false;
        }
        return true;
    }

    
}
