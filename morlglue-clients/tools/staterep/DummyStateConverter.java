// Dummy state converter to be used when the Observation has only a single integer value which is the index to the
// discrete state of the environment. This converter simply extracts and returns that index.

package tools.staterep;

import org.rlcommunity.rlglue.codec.types.Observation;
import tools.staterep.interfaces.StateConverter;

public class DummyStateConverter implements StateConverter {

    @Override
    public int getStateNumber( Observation observation ) {
        return observation.getInt(0);
    }

}
