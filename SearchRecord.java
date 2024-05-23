//kaliopi oikonomou 5099
//militsa voudouri 5104
package lucene_project;
import org.apache.lucene.document.Document;
import java.util.ArrayList;

public class SearchRecord {
    private String query;
    private String field; // to store the search field
    private String searchOption; // to store the search option
    private ArrayList<Document> documents;

    public SearchRecord(String query, String field, String searchOption, ArrayList<Document> documents) {
        this.query = query;
        this.field = field;
        this.searchOption = searchOption;
        this.documents = documents;
    }

    public String getQuery() {
        return query;
    }
    public String getField() {
    	return field;
    }
    public String getSearchOption() {
    	return searchOption;
    }

    public ArrayList<Document> getDocuments() {
        return documents;
    }
}
