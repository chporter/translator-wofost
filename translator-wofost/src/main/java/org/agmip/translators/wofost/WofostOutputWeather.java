package org.agmip.translators.wofost;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.agmip.core.types.TranslatorOutput;
import org.agmip.util.MapUtil;
import org.agmip.util.MapUtil.BucketEntry;

public class WofostOutputWeather extends WofostOutputSoil {
	
	// todo:
	//1. check default values in case of missing values
	//2. check meaning of first data line (see in code) 
	//3. chack multiple year input, only first year in output
	//4. prepare code to multiple weather sections (now assumed one)
	
	
	private static final Calendar _calendar = new GregorianCalendar();

	public static int calculateDayInYear(int day, int month, int year) {
		
		_calendar.set(Calendar.YEAR, year);
		_calendar.set(Calendar.MONTH, month - 1);
		_calendar.set(Calendar.DAY_OF_MONTH, day);
		return _calendar.get(Calendar.DAY_OF_YEAR);
	}


    public void writeFile(String filePath, Map input) {
        // Write your file out here.
    	
    	NumberFormat nf = NumberFormat.getInstance(Locale.US);
		nf.setMaximumFractionDigits(1);
		nf.setMinimumFractionDigits(1);
		nf.setGroupingUsed(false);
		
    	// assumed there is only one weather section 
    	BucketEntry weatherData = MapUtil.getBucket(input, "weather").get(0);
  	
		String stationNumber = "1";
		String WCCFormat = "2";      // cabo format
		
    	LinkedHashMap<String, String> weatherDataValues = weatherData.getValues();
    	
    	String stationName = weatherDataValues.get("wst_name").replace(",", "_");
    	String lat  = MapUtil.getValueOr(weatherDataValues, "wst_lat", "-99");
		String lon  = MapUtil.getValueOr(weatherDataValues, "wst_long", "-99");
		String elev = MapUtil.getValueOr(weatherDataValues, "elev", "-99");
    	
    	ArrayList<LinkedHashMap<String, String>> daily = weatherData.getDataList();
    	
    	String year   = "";
    	BufferedWriter bw = null;
    	DataOutputStream out = null;
    	try 
    	{	
	    	for (LinkedHashMap<String, String> dailyData : daily  )
	    	{
	    		String date  = MapUtil.getValueOr(dailyData, "w_date", "19010101");
	    		String year1  = String.copyValueOf(date.toCharArray(), 0, 1);
	    		String year2  = String.copyValueOf(date.toCharArray(), 1, 3);
	    		String month  = String.copyValueOf(date.toCharArray(), 4, 2);
	    		String day    = String.copyValueOf(date.toCharArray(), 6, 2);    	
	    			    		
	    		if (!year.equals(year1+year2))
	    		{
	    			if (bw != null)
	    			{
	    				bw.close();
	    				out.close();
	    			}
	    			
	    			String fName = filePath + stationName + year1 + "." + year2;
	    			year = year1 + year2;
	    			
    				FileOutputStream fstream = new FileOutputStream(fName);
    				out = new DataOutputStream(fstream);
    				bw = new BufferedWriter(new OutputStreamWriter(out));		
    					
    				bw.write("*---------------------------------------------------------*\n");
    				bw.write(String.format("*   Country: %s\n", ""));
    				bw.write(String.format("*   Station: %s\n", stationName));
    				bw.write(String.format("*      Year: %s\n", year));
    				bw.write("*    Source:\n");
    				bw.write("*\n");
    				bw.write("*    Author:\n");
    				bw.write(String.format("* Longitude:%s\n", lon));
    				bw.write(String.format("*  Latitude: %s\n", lat));
    				bw.write(String.format("* Elevation: %s m.\n", elev));
    				bw.write("*  Comments:\n");		
    				bw.write("*\n");
    				bw.write("*  Columns:\n");
    				bw.write("*  ========\n");
    				bw.write("*  station number\n");
    				bw.write("*  year\n");
    				bw.write("*  day\n");
    				bw.write("*  irradiation (kJ m-2 d-1)\n");
    				bw.write("*  minimum temperature (degrees Celsius)\n");
    				bw.write("*  maximum temperature (degrees Celsius)\n");
    				bw.write("*  vapour pressure (kPa)\n");
    				bw.write("*  mean wind speed (m s-1)\n");
    				bw.write("*  precipitation (mm d-1)\n");
    				bw.write(String.format("** WCCDESCRIPTION=%s\n", stationName));
    				bw.write(String.format("** WCCFORMAT=2\n", WCCFormat));  													
    				bw.write(String.format("** WCCYEARNR=%4s\n", year));
    				bw.write("*---------------------------------------------------------*\n");
    				
    				//todo: 3. what is this line?
    				bw.write("   5.67  51.97     7. 0.00 0.00	    	\n");
	    			
	    		}
	    		
	    		String irra = MapUtil.getValueOr(dailyData, "srad",  "-99.9"); 
	    		double firra= Float.parseFloat(irra);
	    		if (irra != "-99.9")
	    		{
	    			firra = firra * 1000.0;
	    			irra = nf.format(firra);	
	    		}
	    		
	    		String tmin = MapUtil.getValueOr(dailyData, "tmin",  "-99.9");
	    		String tmax = MapUtil.getValueOr(dailyData, "tmax",  "-99.9");
	    		String vap  = MapUtil.getValueOr(dailyData, "vprsd", "-99.9");
	    		String wind = MapUtil.getValueOr(dailyData, "wind",  "-99.9");
	    		String prec = MapUtil.getValueOr(dailyData, "rain",  "-99.9");
	    		
	    		int iYear = Integer.parseInt(year1+year2);
	    		int iMonth = Integer.parseInt(month);
	    		int iDay = Integer.parseInt(day);
	    		    	
	    		Integer dayNr = calculateDayInYear(iDay, iMonth, iYear);
	    		
	    		bw.write(String.format("%s %4s %3d %8s %5s %5s %7s %5s %5s\n", stationNumber, year, dayNr, irra, tmin, tmax, vap, wind, prec));
	    	}
	    		

	    	if (bw != null)
			{
	    		bw.close();
	    		out.close();
			}
	    	
		} catch (FileNotFoundException e) {
			System.out.println("file not found");
		} catch (IOException e) {
			System.out.println("IO error");
		} 	
    	
    }
    
}