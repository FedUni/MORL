package tools.spreadsheet;
// Class for saving results from my RL experiments to Excel.
// Created as abstract so that in future I can switch to a different implementation of the
// Excel interface, if the version I'm currently using fails to be kept up-to-date.
public abstract class ExcelWriter 
{
	protected String[] SUMMARY_FUNCTIONS = {"AVERAGE","MIN","MAX","MEDIAN"};
	
	// Create a new worksheet within the current workbook, with the given name and index, and make
	// it the active worksheet for writing data to.
	public abstract void moveToNewSheet(String name, int index);
	
	// Write out a consecutive series of columns filled with numeric data contained in the array parameter
	// then move to the next row in the current sheet.
	public abstract void writeNextRowNumbers(double data[]);
	
	public abstract void writeNextRowNumbers(int data[]);
	
	// Write out a consecutive series of columns filled with text extracted from the string
	// passed in as a parameter - breaks are indicated by &
	public abstract void writeNextRowText(String text);
	
	// Write out a consecutive series of columns filled with text extracted from the string
	// passed in as a parameter - breaks are indicated by & - followed by columns from the data array
	public abstract void writeNextRowTextAndNumbers(String text, double data[]);
	
	// Write out a consecutive series of columns filled with formulas extracted from the string
	// passed in as a parameter - breaks are indicated by &
	public abstract void writeNextRowFormula(String text);	
	
	// Write out a consecutive series of columns filled with text and then a series of formulas extracted from the strings
	// passed in as a parameter - breaks are indicated by &
	public abstract void writeNextRowTextAndFormula(String text, String formulas);
	
	// Return the address as a string given the column and row indices (starting from 0)
	public abstract String getAddress(int column, int row);
	
	// Return the address as a string given the sheet, column and row indices (starting from 0)
	public abstract String getAddress(int sheet, int column, int row);
	
	// Create a worksheet summarising the results across a specified range of prior sheets using
	// various aggregate statistics. Assumes that the data sheets have indices 0..(numSheets-1).
	// Column headers are passed in as a String with & between columns.
	public abstract void makeSummarySheet(int numSheets, String columnHeaders, int numLabels, int numHeaders, int numDataColumns, int numDataRows);
	
	// finalise writing and close the Excel file
    public abstract void closeFile();

}
