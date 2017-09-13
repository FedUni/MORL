package tools.hypervolume;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author rusashi
 */
public class Point {
    Map<Integer, BigDecimal> objectives = null;

    public Point() {
        objectives = new HashMap<Integer, BigDecimal>();
    }

    public Map<Integer, BigDecimal> getObjectives() {
        return objectives;
    }
    
    public int getnObjectives() {
	    return objectives.size();
    }

    public void setObjectives(Map<Integer, BigDecimal> objectives) {
        this.objectives = objectives;
    }

	public String toString() {
		return "Point objectives: " + objectives;
	}
    
}
