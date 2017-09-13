package tools.hypervolume;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author rusashi
 */
public class Front {
    int nPoints;
    int nObjectives;    
    Map <Integer, Point> points;

    public Front() {
        nPoints = 0;
        nObjectives = 0;
        points = new HashMap();
    }

    public int getnObjectives() {
        return nObjectives;
    }

    public void setnObjectives(int nObjectives) {
        this.nObjectives = nObjectives;
    }

    public int getnPoints() {
        return nPoints;
    }

    public void setnPoints(int nPoints) {
        this.nPoints = nPoints;
    }

    public Map<Integer, Point> getPoints() {
        return points;
    }

    public void setPoints(Map<Integer, Point> points) {
        this.points = points;
    }

    public void increaseNumberOfPoints() {
        nPoints++;
    }
    public void decreaseNumberOfPoints() {
        nPoints--;
    }
    
    public void increaseNumberOfObjectives() {
        nObjectives++;
    }    
}
