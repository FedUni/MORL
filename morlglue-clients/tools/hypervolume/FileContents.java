/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.hypervolume;

import java.util.ArrayList;

/**
 *
 * @author rusashi
 */
public class FileContents {
    int nFronts;
    ArrayList<Front> fronts;

    public FileContents() {
        nFronts = 0;
        fronts = new ArrayList();        
    }

    public ArrayList<Front> getFronts() {
        return fronts;
    }

    public void setFronts(ArrayList<Front> fronts) {
        this.fronts = fronts;
    }

    public int getnFronts() {
        return nFronts;
    }

    public void setnFronts(int nFronts) {
        this.nFronts = nFronts;
    }
    
    public void increaseNumberOfFronts() {
        nFronts++;
    }
    public void decreaseNumberOfFronts() {
        nFronts--;
    }
        
    
}
