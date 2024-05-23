//kaliopi oikonomou 5099
//militsa voudouri 5104
package lucene_project;
import java.util.ArrayList;
import java.util.List;

public class SearchHistory {
    private static SearchHistory instance;
    private List<SearchRecord> records;

    private SearchHistory() {
        records = new ArrayList<>();
    }

    public static synchronized SearchHistory getInstance() {
        if (instance == null) {
            instance = new SearchHistory();
        }
        return instance;
    }

    public void addRecord(SearchRecord record) {
        records.add(record);
    }

    public List<SearchRecord> getRecords() {
        return records;
    }
}
