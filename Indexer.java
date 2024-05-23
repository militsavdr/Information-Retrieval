//kaliopi oikonomou 5099
//militsa voudouri 5104
package lucene_project;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import java.io.FileWriter;
import java.io.PrintWriter;




public class Indexer {

    private CsvLoader loader = new CsvLoader();
    private static final int PAPER_FIELDS = 5; //  source_id, year, title, abstract, full_text
    private static final int AUTHOR_FIELDS = 4; //  source_id, first_name, last_name, institution

    public Map<String, List<String[]>> loadDataIntoMap(String resourcePath, int keyIndex) {
        List<String[]> data = loader.loadData(resourcePath);
        Map<String, List<String[]>> map = new HashMap<>();
        for (String[] row : data) {
            if (row.length > keyIndex) { // Ensure the key index is within bounds
                List<String[]> current = map.getOrDefault(row[keyIndex], new ArrayList<>());
                current.add(row);
                map.put(row[keyIndex], current);
            }
        }
        return map;
    }

    public Map<String, List<String[]>> mergeData(Map<String, List<String[]>> papers, Map<String, List<String[]>> authors) {
        Map<String, List<String[]>> mergedData = new HashMap<>();

        for (String key : authors.keySet()) {
            List<String[]> authorsRows = authors.get(key);
            List<String[]> papersRows = papers.getOrDefault(key, new ArrayList<>());

            for (String[] papersRow : papersRows) {
                for (String[] authorsRow : authorsRows) {
                    // Ensure rows are correctly formatted
                    if (papersRow.length == PAPER_FIELDS && authorsRow.length == AUTHOR_FIELDS) {
                        String[] combined = new String[PAPER_FIELDS + AUTHOR_FIELDS - 1];
                        System.arraycopy(papersRow, 0, combined, 0, PAPER_FIELDS);
                        System.arraycopy(authorsRow, 1, combined, PAPER_FIELDS, AUTHOR_FIELDS - 1);
                        mergedData.computeIfAbsent(key, k -> new ArrayList<>()).add(combined);
                    }
                }
            }
        }
        return mergedData;
    }


public Document createDocument(String[] row) {
    Document document = new Document();
    String cleanedYear = row[1].replaceAll("[^\\d]", "");
    
    boolean validYear = isYearInRange(row[1]);
    if (row.length <= PAPER_FIELDS + AUTHOR_FIELDS - 1 && row.length >= AUTHOR_FIELDS) {
        // This is a merged row
        if (row[0].length() < 50) {
            document.add(new TextField("source_id", row[0], Field.Store.YES));
        }
        if (validYear) { // Only add 'year' if it's valid
            document.add(new TextField("year", cleanedYear, Field.Store.YES));
        }
        if (row[2].length() < 50) {
            document.add(new TextField("title", row[2], Field.Store.YES));
        }
        document.add(new TextField("abstract", row[3], Field.Store.YES)); // No length check for abstract
        document.add(new TextField("full_text", row[4], Field.Store.YES)); // No length check for full text
        if (row[5].length() < 50) {
            document.add(new TextField("first_name", row[5], Field.Store.YES));
        }
        if (row[6].length() < 50) {
            document.add(new TextField("last_name", row[6], Field.Store.YES));
        }
        if (row[7].length() < 50) {
            document.add(new TextField("institution", row[7], Field.Store.YES));
        }
    } else if (row.length <= AUTHOR_FIELDS) {
        // This is an authors-only row
        if (row[0].length() < 50) {
            document.add(new TextField("source_id", row[0], Field.Store.YES));
        }
        if (row[1].length() < 50) {
            document.add(new TextField("first_name", row[1], Field.Store.YES));
        }
        if (row[2].length() < 50) {
            document.add(new TextField("last_name", row[2], Field.Store.YES));
        }
        if (row[3].length() < 50) {
            document.add(new TextField("institution", row[3], Field.Store.YES));
        }
    } else if (row.length <= PAPER_FIELDS) {
        // This is a papers-only row
        if (row[0].length() < 50) {
            document.add(new TextField("source_id", row[0], Field.Store.YES));
        }
        if (validYear) { // Only add 'year' if it's valid
            document.add(new TextField("year", cleanedYear, Field.Store.YES));
        }
        if (row[2].length() < 50) {
            document.add(new TextField("title", row[2], Field.Store.YES));
        }
        document.add(new TextField("abstract", row[3], Field.Store.YES)); // No length check for abstract
        document.add(new TextField("full_text", row[4], Field.Store.YES)); // No length check for full text
    }
    return document;
}
    private boolean isYearInRange(String year) {
    //	String cleanedYear = year.replaceAll("[^\\d]", ""); // Strip non-digit characters
    	   
        try {
            int yearInt = Integer.parseInt(year);
            return yearInt >= 1800 && yearInt <= 2024;
        } catch (NumberFormatException e) {
            return false; // Year is not a valid integer
        }
    }
    
    public static void main(String[] args) {
        PrintWriter writer = null;
        try {
            Indexer indexer = new Indexer();
            Map<String, List<String[]>> authors = indexer.loadDataIntoMap("authors.csv", 0);
            Map<String, List<String[]>> papers = indexer.loadDataIntoMap("papers.csv", 0);
            Map<String, List<String[]>> mergedData = indexer.mergeData(papers, authors);
            Directory dir = FSDirectory.open(Paths.get("Index"));
            IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
            IndexWriter indexWriter = new IndexWriter(dir, config);

            writer = new PrintWriter(new FileWriter("indexer_stats.txt"));

            int count = 0;
            //int invalidYearCount = 0; // Counter for year values in range

            for (String key : mergedData.keySet()) {
                List<String[]> documentEntries = mergedData.get(key);
                for (String[] entry : documentEntries) {
                    Document doc = indexer.createDocument(entry);
                    //if (!indexer.isYearInRange(entry[1])) { // Check if 'year' is in range
                    //    invalidYearCount++;
                     //   writer.println("Invalid year detected in document with: source_id"+entry[0] );
                        
                        
                        
                    //}
                    indexWriter.addDocument(doc);

                    // Output to file:

                //if (count < 3) {  // Limit output to first 3 documents
                    writer.println("Indexed document " + (count + 1) + ":");
                    writer.println("Source ID: " + doc.get("source_id"));
                    writer.println("First Name: " + doc.get("first_name"));
                    writer.println("Last Name: " + doc.get("last_name"));
                    writer.println("Institution: " + doc.get("institution"));
                    writer.println("Year: " + doc.get("year"));
                    writer.println("Title: " + doc.get("title"));
                    //writer.println("Abstract: " + doc.get("abstract"));
                    //writer.println("Full Text: " + doc.get("full_text"));
                    writer.println("---------------------------");
                    count++;
                //}
                }
            }

            indexWriter.commit();
            indexWriter.close();

            //writer.println("Indexing completed. Total documents indexed: " + mergedData.size());
            //writer.println("Year values not in range 1800-2024: " + invalidYearCount);


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    
}
