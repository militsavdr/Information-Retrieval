//kaliopi oikonomou 5099
//militsa voudouri 5104
package lucene_project;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;

import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.synonym.*;
import org.apache.lucene.util.CharsRef;

import java.io.*;

public class Searcher {

    private static IndexReader reader;
    private static IndexSearcher searcher;
    private Analyzer standardAnalyzer;
    private Analyzer synonymAnalyzer;
    private List<String> validFields = Arrays.asList("source_id", "year", "title", "abstract", "full_text", "first_name", "last_name", "institution");
    private static final String indexPath = "Index";

    public class SearchResults {
        public final ArrayList<Document> documents;
        public final long totalHits;   // returns long

        public SearchResults(ArrayList<Document> documents, int totalHits) {
            this.documents = documents;
            this.totalHits = totalHits;
        }
    }

    public Searcher() throws IOException {
        standardAnalyzer = new StandardAnalyzer();
        initializeSynonymAnalyzer();
        createSearcher();
    }

    private void initializeSynonymAnalyzer() throws IOException {
        InputStream stream = getClass().getResourceAsStream("/synonyms.txt");
        if (stream == null) {
            throw new FileNotFoundException("Synonym file not found in resources");
        }
        Reader synonymsReader = new InputStreamReader(stream);

        try {
            // Create a SynonymMap Builder
            SynonymMap.Builder builder = new SynonymMap.Builder(true);
            // Parse each line of the synonym file
            BufferedReader br = new BufferedReader(synonymsReader);
            String line;
            while ((line = br.readLine()) != null) {
                // Assume each line in the file is formatted as "word1, word2, word3"
                String[] synonyms = line.split(",");
                if (synonyms.length > 1) {
                    for (int i = 1; i < synonyms.length; i++) {
                        builder.add(new CharsRef(synonyms[0].trim()), new CharsRef(synonyms[i].trim()), true);
                    }
                }
            }

            final SynonymMap synonymMap = builder.build();

            // Create an analyzer using the synonym map
            synonymAnalyzer = new Analyzer() {
                @Override
                protected TokenStreamComponents createComponents(String fieldName) {
                    Tokenizer tokenizer = new WhitespaceTokenizer();
                    return new TokenStreamComponents(tokenizer, new SynonymGraphFilter(tokenizer, synonymMap, true));
                }
            };
        } catch (IOException e) {
            throw new IOException("Error reading synonym file", e);
        } finally {
            synonymsReader.close(); // Ensure the reader is closed after building the map
        }
    }


    private void createSearcher() throws IOException {
        Directory dir = FSDirectory.open(Paths.get(indexPath));
        reader = DirectoryReader.open(dir);
        searcher = new IndexSearcher(reader);
    }

    

    //    SEARCH METHODS 
    
    public TopDocs searchBySpecificField(String name, String field, int start, int rows) throws ParseException, IOException {
        if (!validFields.contains(field)) {
            throw new IllegalArgumentException("Invalid search field: " + field);
        }
        QueryParser qp = new QueryParser(field, standardAnalyzer);
        Query query = qp.parse(name);
        TopDocs hits = searcher.search(query, start+rows);
        return hits;
    }

    public TopDocs searchByAll(String name, int start, int rows) throws ParseException, IOException {
        MultiFieldQueryParser qp = new MultiFieldQueryParser(validFields.toArray(new String[0]), standardAnalyzer);
        Query query = qp.parse(name);
        TopDocs hits = searcher.search(query, start+rows);
        return hits;
    }

    public TopDocs searchExactPhrase(String phrase, String field,int start, int rows) throws IOException {
        PhraseQuery.Builder builder = new PhraseQuery.Builder();
        String[] words = phrase.split("\\s+");
        for (String word : words) {
            builder.add(new Term(field, word));
        }
        PhraseQuery pq = builder.build();
        return searcher.search(pq, start + rows);
    }

    public TopDocs searchExactPhraseByAll(String phrase, int start, int rows) throws ParseException, IOException {
        MultiFieldQueryParser parser = new MultiFieldQueryParser(validFields.toArray(new String[0]), standardAnalyzer);
        Query query = parser.parse("\"" + phrase + "\"");
        return searcher.search(query, start + rows);
    }


    public TopDocs searchBySynonym(String phrase, String field, int start, int rows) throws ParseException, IOException {
        QueryParser qp = new QueryParser(field, synonymAnalyzer);
        Query query = qp.parse(phrase);
        return searcher.search(query, start + rows);
    }
    public TopDocs searchBySynonymAll(String phrase, int start, int rows) throws ParseException, IOException {
        MultiFieldQueryParser parser = new MultiFieldQueryParser(validFields.toArray(new String[0]), synonymAnalyzer);
        Query query = parser.parse(phrase);
        return searcher.search(query, start + rows);
    }
    
    public ArrayList<Document> findDocuments(TopDocs foundDocs) throws IOException {
        ArrayList<Document> documents = new ArrayList<>();
        for (ScoreDoc sd : foundDocs.scoreDocs) {
            Document doc = searcher.doc(sd.doc);
            documents.add(doc);
        }
        return documents;
    }



    //    FIND METHODS USING findDocuments
    
    public SearchResults findFieldWithTotal(String name, String field, int start, int rows) throws Exception {
        TopDocs foundDocs = searchBySpecificField(name, field, start, rows);
        ArrayList<Document> documents = findDocuments(foundDocs);
        return new SearchResults(documents, (int)foundDocs.totalHits.value);
    }

    public SearchResults findAllWithTotal(String name, int start, int rows) throws Exception {
        TopDocs foundDocs = searchByAll(name, start, rows);
        ArrayList<Document> documents = findDocuments(foundDocs);
        return new SearchResults(documents, (int)foundDocs.totalHits.value);
    }

    public SearchResults findKeywordFieldWithTotal(String name, String field, int start, int rows) throws Exception {
        TopDocs foundDocs = searchExactPhrase(name, field, start, rows);
        ArrayList<Document> documents = findDocuments(foundDocs);
        return new SearchResults(documents, (int)foundDocs.totalHits.value);
    }

    public SearchResults findKeywordAllFieldsWithTotal(String name, int start, int rows) throws Exception {
        TopDocs foundDocs = searchExactPhraseByAll(name, start, rows);
        ArrayList<Document> documents = findDocuments(foundDocs);
        return new SearchResults(documents, (int)foundDocs.totalHits.value);
    }

    public SearchResults findSynonymFieldWithTotal(String name, String field, int start, int rows) throws Exception {
        TopDocs foundDocs = searchBySynonym(name, field, start, rows);
        ArrayList<Document> documents = findDocuments(foundDocs);
        return new SearchResults(documents, (int)foundDocs.totalHits.value);
    }
    public SearchResults findSynonymAllFieldsWithTotal(String phrase, int start, int rows) throws Exception {
        TopDocs foundDocs = searchBySynonymAll(phrase, start, rows);
        ArrayList<Document> documents = findDocuments(foundDocs);
        return new SearchResults(documents, (int)foundDocs.totalHits.value);
    }

  
}

