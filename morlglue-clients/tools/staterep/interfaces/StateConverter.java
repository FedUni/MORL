package tools.staterep.interfaces;

import org.rlcommunity.rlglue.codec.types.Observation;

public interface StateConverter {
    int getStateNumber(Observation observation);
}
