import agents.*;
import env.*;
import experiments.*;


public class MORL_Glue_Driver
{

	public static void main(String[] args) 
	{
			Process server = null;
			// try to launch the MORL_Glue server
			Runtime rt = Runtime.getRuntime();
			try 
			{
				server = rt.exec("C:\\MO586rl_glue.exe");//local path
				System.out.println("Launching server");
			} 
			catch (Exception e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		   // launch agent in its own thread
			Thread agent = 
			new Thread(){
		          public void run(){
		            System.out.println("Started agent thread");
		            
		            TLO_Agent.main(null);
		          }
		        };
			agent.start();

	 	   // launch environment in its own thread
			Thread envt = new Thread(){
		          public void run(){
		            System.out.println("Started envt thread");
		            
		            String[] gdstArgs = {"15","4","1","3","0.0","0.0",""+GeneralisedDeepSeaTreasureEnv.CONCAVE,"471"};
		            GeneralisedDeepSeaTreasureEnv.main(gdstArgs);
		            
		          }
		        };
		  envt.start();

	 	   // launch experiment in its own thread
			Thread experiment = new Thread(){
		          public void run(){
		            System.out.println("Started experiment thread");
		            
		            DemoExperiment.main(null);
		          }
		        };
		  experiment.start();
		}
	
}
