package tools.spreadsheet;

public class JxlTest
{
    private static ExcelWriter x = new JxlExcelWriter("myTest");
   
    public static void main(String[] args)
    {
    	int data[] = new int[3];
    	String header = "";
        for (int sheet=0; sheet<3; sheet++)
        {
        	// make next sheet
        	x.moveToNewSheet("Trial"+sheet, sheet);
        	// write headers
        	header = "";
        	for (int col=0; col<data.length-1; col++)
    		{
    			header += "Obj " + col + "&";
    		}
        	header += "Obj " + (data.length-1);
        	x.writeNextRowText(header);
        	// write data
        	for (int row = 0; row<10; row++)
        	{
        		for (int col=0; col<data.length; col++)
        		{
        			data[col] = sheet * 100 + row * 10 + col;
        		}
        		x.writeNextRowNumbers(data);
        	}
        	// write some formula
        	String text = "";
        	for (int col=0; col<data.length-1; col++)
    		{
    			text += "AVERAGE(A1:A10)&";
    		}
			text += "AVERAGE(D1:D10)&";
        	x.writeNextRowFormula(text);
        	//x.writeNextRowFormula(x.getAddress(2,0));
        }
        x.makeSummarySheet(3, header, 0, 0, data.length, 10);
        x.closeFile();
    }
}