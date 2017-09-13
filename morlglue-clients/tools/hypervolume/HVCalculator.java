package tools.hypervolume;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Map;
import java.util.Vector;

public class HVCalculator {        
    
    int maxm = 0;
    int maxn = 0;
    int n = 0;     // the number of objectives    
    ArrayList<Front> fs = new ArrayList();      // memory management stuff 
    int fr = 0;     // current depth 
    int frmax = -1; // max depth malloced so far (for opt = 0) 
    Point ref;
      
    public static void main(String[] args) throws Exception {
	    showVolumesFromFile(args[0]);	    
    }
    
    public FileContents readFile(String filename) throws FileNotFoundException {
        Scanner scanner = new Scanner(new FileReader( filename ) );

        FileContents fc = new FileContents();
        int currentFront = 0;
        String line = new String();
        
        while( scanner.hasNextLine() ) {
            line = scanner.nextLine();
            if( line.equals("#") ) {
                Front front = new Front();
                fc.getFronts().add(front);
                currentFront = fc.getnFronts();
                fc.increaseNumberOfFronts();
            } else {
                Front front = fc.getFronts().get(currentFront);
                front.setnObjectives(0);
                String[] points = line.split(" ");
                Point p = new Point();
                for(int i = 0 ; i < points.length ; i++) {
                    double objectiveValue = Double.parseDouble( points[i] );
                    p.getObjectives().put( front.getnObjectives() , new BigDecimal(objectiveValue) );
                    front.increaseNumberOfObjectives();
                }
                
                front.getPoints().put( front.getnPoints(), p );
                front.increaseNumberOfPoints();

            }
        }
        fc.decreaseNumberOfFronts();
        return fc;
    }
    
    // returns true if point 1 Pareto-dominates point2
    private static boolean dominates(double[] point1, double[] point2)
    {
    	for (int i=0; i<point1.length; i++)
    	{
    		if (point1[i]<point2[i])
    			return false;
    	}
    	return true;
    }
    
    public static double getVolumeFromArray(double[][] data) {
	    return getVolumeFromArray(data, new double[] {0.0, 0.0, 0.0});
    }
    
    public static double getVolumeFromArray(double[][] data, double[] refPoint) {
	    HVCalculator hvc = new HVCalculator();
	    
	    Front front = new Front();
	    front.setnObjectives(0);
	    
	    for (double[] points : data) {
		   if (dominates(points, refPoint))
		   {
		 	   Point p = new Point();
		 	   front.setnObjectives(0);
		 	   
		 	   for (double point : points) {		 	   
			 	   p.getObjectives().put(front.getnObjectives(), new BigDecimal(point));
	               front.increaseNumberOfObjectives();
		 	   }
		 	   
		 	   front.getPoints().put(front.getnPoints(), p);
	           front.increaseNumberOfPoints();
       	   }
    	}
    	
    	Point newRefPoint = new Point();
        for (int i = 0; i < refPoint.length; i++) {
        	newRefPoint.getObjectives().put(i, new BigDecimal(refPoint[i]));
        }
    	hvc.setReferencePoint(newRefPoint);        
    	
    	if (front.getnPoints() == 0) {
	    	front.getPoints().put(front.getnPoints(), newRefPoint);
	    	front.increaseNumberOfPoints();
	    	front.setnObjectives(newRefPoint.getnObjectives());
        }
        
    	return hvc.getVolume(front);
    }    
    
    public static void showVolumesFromFile(String filename) throws FileNotFoundException {
	    Scanner scanner = new Scanner(new FileReader(filename));
	    
        String line = new String();

        int numFronts = 0;
        int numPoints = 0;        
        int numObjectives = 0; 
        boolean inFront = false;
        
        while (scanner.hasNextLine()) {
            line = scanner.nextLine();
            
            
            if( line.equals("#") ) {
	            numFronts++;
	            
	            if (numFronts == 1) {
		            inFront = true;
	            } else {
		            inFront = false;
	            }
	            
            } else {
	            if (numObjectives == 0) {
		            String[] points = line.split(" ");
		            numObjectives = points.length;
            	}
            	
				if (inFront) {
	            	numPoints++;
            	}                	
            }
            
        }	    
	    
        numFronts--;
        
        System.out.println("numFronts: " + numFronts + " numPoints: " + numPoints + " numObjectives: " + numObjectives);
        
        
        scanner = new Scanner(new FileReader(filename));
        Vector<double[][]> fronts = new Vector<double[][]>();
        
        double[][] data = null;
        
        int index = 1;
        int pointIndex = 0;
        
        while (scanner.hasNextLine()) {
        	line = scanner.nextLine();
        	
        	if( line.equals("#") ) {
	        	if (data != null) {
		        	//System.out.println("hv(" + (index++) + ") = " + getVolumeFromArray(data, new double[]{0.2, 0.1, 0.4}));
		        	System.out.println("hv(" + (index++) + ") = " + getVolumeFromArray(data));
	        	}
	        	
	            data = new double[numPoints][numObjectives];	            
	            pointIndex = 0;
            } else {
	            String[] points = line.split(" ");
	            
	            for (int i = 0; i < numObjectives; i++) {
	            	data[pointIndex][i] = Double.parseDouble(points[i]);
            	}
	            
	            pointIndex++;
            }
    	}
        
    }
    

    
    public void setReferencePoint(Point point) {
        ref = point;
    }
    
    public double getVolume(Front ps)  {
        
        if( ps.getPoints().size() > maxm ) {
            maxm = ps.getPoints().size();
        }
        
        for(int i=0;i<ps.getPoints().size();i++) {
            //ps.increaseNumberOfPoints();
            if ( ps.getPoints().get(i).getObjectives().size() > maxn ) {
                maxn = ps.getPoints().get(i).getObjectives().size();
            }
        }
        
        //init reference point to origin
        if (ref == null) {
            ref = new Point();
            for (int i = 0; i < maxn; i++) {
                ref.getObjectives().put(i, new BigDecimal(0.0));
            }
            
        }
        
        n = maxn;
        return hv( ps );

    }    
    
    private double hv(Front ps) {
        double volume = 0;
        for(int i=0 ; i < ps.getnPoints() ; i++) {
            volume += exclhv(ps, i);
        }
        return volume;
    }
    
    private double exclhv(Front ps, int p) {
        double volume = inclhv( ps.getPoints().get(p) );
        if( ps.getnPoints() > p+1 ) {
            makeDominatedBit(ps, p);
            volume -= hv( fs.get(fr - 1) );
            fr--;
        }        
        return volume;
    }
    
    private double inclhv(Point p) {
        double volume = 1;
        for(int i = 0 ; i < n ; i++) {
            volume *= Math.abs( p.getObjectives().get(i).subtract( ref.getObjectives().get(i) ).doubleValue() );
        }
        
        return volume;
    }


    private BigDecimal worse( BigDecimal x, BigDecimal y ) {
        BigDecimal wrs = ( beats( y, x) ) ? x : y;
        return wrs;
    }
    

    private boolean beats(BigDecimal x, BigDecimal y) {
        int status = x.compareTo(y);
        if(status > 0) return true;
        else return false;
    }
            
    private void makeDominatedBit(Front ps, int p) {
        if (fr > frmax) {
            frmax = fr;
            Front front = new Front();
            for (int i = 0; i < maxm; i++) {
                Point point = new Point();
                front.getPoints().put(i, point);
            }
            fs.add(fr, front);
        }
        
//        System.out.println("ps.getnPoints(): " + ps.getnPoints() + " p: " + p);
        
        int z = ps.getnPoints() -1 -p;
        
//        System.out.println("n: " + n + " z: " + z);
        
        for (int i = 0; i < z; i++) {	        
            for (int j = 0; j < n; j++) {
/*	            
	            Map<Integer, Point> points = fs.get(fr).getPoints();
	            Point point = points.get(i);
	            Map<Integer, BigDecimal> objectives = point.getObjectives();
	            
	            Map<Integer, Point> psPoints = ps.getPoints();
	            Point xpoint = psPoints.get(p);
	            Map<Integer, BigDecimal> xobjectives = xpoint.getObjectives();
	            BigDecimal x = xobjectives.get(j);
	            System.out.println("xpoint[" + i + "," + j + "]: " + xpoint);
	            Point ypoint = psPoints.get(p+1+i);
	            System.out.println("ypoint[" + i + "," + (p+1+i) + "]: " + ypoint);
	            Map<Integer, BigDecimal> yobjectives = ypoint.getObjectives();
	            BigDecimal y = yobjectives.get(j);
	            
	            objectives.put(j, worse(x, y)) ;
*/	            
                fs.get(fr).getPoints().get(i).getObjectives().put(j,  worse( ps.getPoints().get(p).getObjectives().get(j), ps.getPoints().get(p+1+i).getObjectives().get(j)) ) ;
            }
        }
        Point t = new Point();
        fs.get(fr).setnPoints(1);
        for (int i = 1; i < z; i++) {
            int j = 0;
            boolean keep = true;
            while (j < fs.get(fr).getnPoints() && keep) {
                switch ( dominates2way( fs.get(fr).getPoints().get(i), fs.get(fr).getPoints().get(j) ) ) {
                    case -1: {
                        t = fs.get(fr).getPoints().get(j);
                        fs.get(fr).decreaseNumberOfPoints();
                        fs.get(fr).getPoints().put(j, fs.get(fr).getPoints().get( fs.get(fr).getnPoints() ) );                        
                        fs.get(fr).getPoints().put(fs.get(fr).getnPoints(), t);
                        break;
                    }
                    case  0: {
                        j++;
                        break;
                    } 
                    default: {
                        keep = false;
                    }
                }
            }
            if (keep) {
                t = fs.get(fr).getPoints().get( fs.get(fr).getnPoints() );
                fs.get(fr).getPoints().put(fs.get(fr).getnPoints(), fs.get(fr).getPoints().get(i));
                fs.get(fr).getPoints().put(i, t);
                fs.get(fr).increaseNumberOfPoints();
            }
            
        }
        fr++;
        
    }
    private int dominates2way(Point p, Point q) {
        for (int i = n - 1; i >= 0; i--) {
            if( beats( p.getObjectives().get(i), q.getObjectives().get(i) ) ) {
                for (int j = i - 1; j >= 0; j--) {
                    if ( beats( q.getObjectives().get(j), p.getObjectives().get(j) ) ) return 0;
                }
                return -1;
            } else if( beats( q.getObjectives().get(i) , p.getObjectives().get(i) ) ) {
                for (int j = i - 1; j >= 0; j--) {
                    if ( beats( p.getObjectives().get(j) , q.getObjectives().get(j) ) )  return 0; 
                }
                return 1;
            }
        }
        return 2;
    }
}
