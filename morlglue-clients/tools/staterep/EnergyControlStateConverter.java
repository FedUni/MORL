package tools.staterep;

import org.rlcommunity.rlglue.codec.types.Observation;
import tools.staterep.interfaces.StateConverter;

public class EnergyControlStateConverter implements StateConverter {

    @Override
    public int getStateNumber( Observation observation ) {
        double pe = getPredictedEnergy( observation );
        double l = getLoad( observation );
        double bc = getBatteryCharge( observation );


        return discretizeValue(pe) + ( discretizeValue(l) - 1) * 80  +  (discretizeValue(bc) - 1) * 80 * 100;
    }

    public double getPredictedEnergy(Observation observation) {

        assert observation != null : "Observation object is null";
        double predictedEnergy = observation.getDouble( 0 );
        assert predictedEnergy < 783  && predictedEnergy >= 0 : "Predicted energy is not in a specified range";

        return predictedEnergy;
    }
    public double getLoad(Observation observation) {

        assert observation != null : "Observation object is null";
        double load = observation.getDouble( 1 );
        assert load < 965  && load >= 0 : "Load is not in a specified range";

        return load;
    }
    public double getBatteryCharge(Observation observation) {

        assert observation != null : "Observation object is null";
        double batteryCharge = observation.getDouble( 2 );
        assert batteryCharge <= 2000  && batteryCharge >= 0 : "Battery charge is not in a specified range";

        return batteryCharge;
    }

    public int discretizeValue(double input) {
        return  (int) Math.floor( input / 10 ) + 1;
    }
}
