//kaliopi oikonomou 5099
//militsa voudouri 5104
package lucene_project;
import com.opencsv.CSVReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CsvLoader {
// returns a list of Strings of a file
	
	
    public List<String[]> loadData(String resourcePath) { 
    	// method to load the data from a csv file 
    	
    	
        List<String[]> records = new ArrayList<>();
        // Use class loader to access the resource
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IllegalArgumentException("Resource not found: " + resourcePath);
        }
        
        try (CSVReader csvReader = new CSVReader(new InputStreamReader(
                this.getClass().getClassLoader().getResourceAsStream(resourcePath)))) {
            String[] values;
            while ((values = csvReader.readNext()) != null) {
                records.add(values);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return records;
    }
    
    public static void main(String[] args) {
        CsvLoader loader = new CsvLoader();
        // Use the path relative to the resources directory
        List<String[]> authors = loader.loadData("authors.csv");
        List<String[]> papers = loader.loadData("papers.csv");
        
        // Example of how to print the loaded data
        for (String[] author : authors) {
            for (String detail : author) {
                System.out.print(detail + " ");
            }
            System.out.println();
        }
        
        for (String[] paper : papers) {
            for (String detail : paper) {
                System.out.print(detail + " ");
            }
            System.out.println();
        }
    }
}
