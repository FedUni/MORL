package tools.spreadsheet;
// An implementation of ExcelWriter based on the Jxl framework.

import java.io.File;
import java.util.StringTokenizer;

import jxl.Workbook;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.Label;
import jxl.write.WriteException;

public class JxlExcelWriter extends ExcelWriter 
{
    private WritableWorkbook wbook;
    private WritableSheet thisSheet;
    private int rowNumber;

    // Create a writeable workbook
	public JxlExcelWriter(String filename) 
    {
        try
        {
            wbook = Workbook.createWorkbook(new File(filename+".xls"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
	}

	@Override
	// Create a new worksheet within the current workbook, with the given name and index, and make
	// it the active worksheet for writing data to.
	public void moveToNewSheet(String name, int index) 
	{
    	try
    	{
    		thisSheet = wbook.createSheet(name, index);    	
    	}
        catch (Exception e)
        {
            e.printStackTrace();
        }
    	rowNumber = 0;
	}

	@Override
	// Write out a consecutive series of columns filled with numeric data contained in the array parameter
	// then move to the newxt row in the current sheet.
	public void writeNextRowNumbers(double[] data) 
	{
    	try
    	{
			for (int col=0; col<data.length; col++)
			{
	    		thisSheet.addCell(new jxl.write.Number(col, rowNumber, data[col]));
			}
			rowNumber++;
    	}
        catch (Exception e)
        {
            e.printStackTrace();
        }		
	}

	@Override
	// Write out a consecutive series of columns filled with numeric data contained in the array parameter
	// then move to the newxt row in the current sheet.
	public void writeNextRowNumbers(int[] data) 
	{
    	try
    	{
			for (int col=0; col<data.length; col++)
			{
	    		thisSheet.addCell(new jxl.write.Number(col, rowNumber, data[col]));
			}
			rowNumber++;
    	}
        catch (Exception e)
        {
            e.printStackTrace();
        }
	}

	@Override
	// Write out a consecutive series of columns filled with text extracted from the string
	// passed in as a parameter - breaks are indicated by &
	public void writeNextRowText(String text) 
	{
		int col = 0;
    	try
    	{
		     StringTokenizer st = new StringTokenizer(text,"&");
		     while (st.hasMoreTokens()) 
		     {
	    		thisSheet.addCell(new Label(col, rowNumber, st.nextToken()));
	    		col++;
		     }
		     rowNumber++;
    	}
        catch (Exception e)
        {
            e.printStackTrace();
        }
	}
	
	// Write out a consecutive series of columns filled with text extracted from the string
	// passed in as a parameter - breaks are indicated by & - followed by columns from the data array
	public void writeNextRowTextAndNumbers(String text, double data[])
	{
		int col = 0;
    	try
    	{
    		// write out the text columns
		    StringTokenizer st = new StringTokenizer(text,"&");
		    while (st.hasMoreTokens()) 
		    {
		    	thisSheet.addCell(new Label(col, rowNumber, st.nextToken()));
	    		col++;
		    }
		    // write the data columns
			for (int i=0; i<data.length; i++)
			{
	    		thisSheet.addCell(new jxl.write.Number(col, rowNumber, data[i]));
	    		col++;
			}		     
		    rowNumber++;
    	}
        catch (Exception e)
        {
            e.printStackTrace();
        }		
	}

	@Override
	// Write out a consecutive series of columns filled with formula extracted from the extracted from the string
	// passed in as a parameter - breaks are indicated by &
	public void writeNextRowFormula(String text) 
	{
		int col = 0;
    	try
    	{
		     StringTokenizer st = new StringTokenizer(text,"&");
		     while (st.hasMoreTokens()) 
		     {
	    		thisSheet.addCell(new jxl.write.Formula(col, rowNumber, st.nextToken()));
	    		col++;
		     }
		     rowNumber++;
    	}
        catch (Exception e)
        {
            e.printStackTrace();
        }
	}
	
	// Write out a consecutive series of columns filled with text and then a series of formulas extracted from the strings
	// passed in as a parameter - breaks are indicated by &
	public void writeNextRowTextAndFormula(String text, String formulas)
	{
		int col = 0;
    	try
    	{
    		// do the text columns first
		     StringTokenizer st = new StringTokenizer(text,"&");
		     while (st.hasMoreTokens()) 
		     {
	    		thisSheet.addCell(new Label(col, rowNumber, st.nextToken()));
	    		col++;
		     }
		     // then do the formula columns
		     st = new StringTokenizer(formulas,"&");
		     while (st.hasMoreTokens()) 
		     {
	    		thisSheet.addCell(new jxl.write.Formula(col, rowNumber, st.nextToken()));
	    		col++;
		     }		     
		     rowNumber++;
    	}
        catch (Exception e)
        {
            e.printStackTrace();
        }	
	}
	
	// Return the address as a string given the column and row indices (starting from 0)
	public String getAddress(int column, int row)
	{
		return jxl.CellReferenceHelper.getCellReference(column, row);
	}
	
	// Return the address as a string given the sheet, column and row indices (starting from 0)
	public String getAddress(int sheet, int column, int row)
	{
		String names[] = wbook.getSheetNames();
		return "'"+names[sheet]+"'!"+jxl.CellReferenceHelper.getCellReference(column,row);		
	}
	
	// Create a worksheet summarising the results across a specified range of prior sheets using
	// various aggregate statistics. Assumes that the data sheets have indices 0..(numSheets-1).
	// Column headers are passed in as a String with & between columns.
	// numLabels and numHeaders specify how many columns and rows need to be skipped on each data sheet to reach the actual data cells
	public void makeSummarySheet(int numSheets, String columnHeaders, int numLabels, int numHeaders, int numDataColumns, int numDataRows)
	{
		moveToNewSheet("Summary", numSheets); // make new sheet after the data sheets
		// Make a string for use in skipping over the labels at the start of the row
		String labelSkip = "";
		for (int i=0; i<numLabels; i++)
		{
			labelSkip += " &";
		}
		// Add a header row with the names of the stats functions being used
		String header = labelSkip;
		for (int i=0; i<SUMMARY_FUNCTIONS.length; i++)
		{
			header += SUMMARY_FUNCTIONS[i];
			for (int cols=0; cols<numDataColumns; cols++)
			{
				header += "& ";
			}
		}
		writeNextRowText(header);
		// Create a row with the objective labels repeated for each of the summary functions
		String fullHeader=labelSkip;
		for (int i=0; i<SUMMARY_FUNCTIONS.length; i++)
		{
			fullHeader += columnHeaders + "&";
		}
		writeNextRowText(fullHeader);
		// Generate the formulas for the summary stats; repeat for each row of data
		String sheetNames[] = wbook.getSheetNames();
		for (int row=0; row<numDataRows; row++)
		{
			String formulaText="";
			// add formulae to look at the labels from Sheet0
			for (int col=0; col<numLabels; col++)
			{
				formulaText+="'"+sheetNames[0]+"'!"+jxl.CellReferenceHelper.getCellReference(col,row+numHeaders)+"&";
			}
			// repeat for each statistic
			for (int i=0; i<SUMMARY_FUNCTIONS.length; i++)
			{
				// repeat for each data column
				for (int col=0; col<numDataColumns; col++)
				{
					String cellAddress = jxl.CellReferenceHelper.getCellReference(col+numLabels,row+numHeaders);
					formulaText+=SUMMARY_FUNCTIONS[i]+"(";
					// repeat for each sheet
					for (int sheet=0; sheet<numSheets-1; sheet++)
					{
						formulaText+="'"+sheetNames[sheet]+"'!"+cellAddress+",";
					}
					// do the final sheet and close the parentheses for the function
					formulaText+="'"+sheetNames[numSheets-1]+"'!"+cellAddress+")";
					// add an & as a separator between cells for each cell except the very last one
					if (!(row==numDataRows && i==SUMMARY_FUNCTIONS.length-1 && col==numDataColumns-1))
					{
						formulaText+="&";
					}
				}
			}
			writeNextRowFormula(formulaText);
		}
	}

	
	// finalise writing and close the Excel file
    public void closeFile()
    {
        try 
        {
            // Closing the writable work book
            wbook.write();
            wbook.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
