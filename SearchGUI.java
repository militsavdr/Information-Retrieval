//kaliopi oikonomou 5099
//militsa voudouri 5104
package lucene_project;
import javax.swing.*;
import org.apache.lucene.document.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class SearchGUI extends JFrame {
    private JTextField searchField;
    private JButton searchButton, nextButton, prevButton,historyButton;
    private JPanel resultsPanel;
    private JComboBox<String> searchType;
    private JComboBox<String> fieldSelection;
    private JComboBox<String> sortOptions;	//sort by year or no
    
    private JPanel northPanel;
    private int currentPage = 0;
    private final int rowsPerPage = 10; // You can adjust the number of rows per page

    private Searcher searcher;
    private ArrayList<Document> documents;
    private long totalHits;
    private Map<String, String> fieldMap;

    public SearchGUI() {
        super("Search Engine - Kellomela");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("Button.font", new Font("Algerian", Font.BOLD, 16));
            UIManager.put("TextField.font", new Font("Algerian", Font.PLAIN, 16));
            UIManager.put("TextArea.font", new Font("Algerian", Font.PLAIN, 16));
            UIManager.put("ComboBox.font", new Font("Algerian", Font.PLAIN, 16));
        } catch (Exception e) {
            e.printStackTrace();
        }
        initializeComponents();
        setupLayout();
        addEventListeners();
        initializeSearcher();
        initializeFieldMap();
    }
    private void initializeFieldMap() {
        fieldMap = new HashMap<>();
        fieldMap.put("source id", "source_id");
        fieldMap.put("year", "year");
        fieldMap.put("title", "title");
        fieldMap.put("abstract", "abstract");
        fieldMap.put("full paper", "full_text");
        fieldMap.put("author's first name", "first_name");
        fieldMap.put("author's last name", "last_name");
        fieldMap.put("author's Institution", "institution");
        fieldMap.put("all", "all");
    }

    private void initializeComponents() {
        searchField = new JTextField(30);
        searchButton = new JButton("Search");
        nextButton = new JButton("Next");
        prevButton = new JButton("Previous");
        searchButton.setBackground(new Color(240, 150, 240));
        //searchButton.setForeground(Color.PINK);
        //255, 51, 153
        searchButton.setForeground(new Color(255, 51, 153));
        nextButton.setBackground(new Color(230, 230, 250));
        prevButton.setBackground(new Color(230, 230, 250));
        historyButton = new JButton("History of Searches");
        historyButton.setBackground(new Color(200, 200, 250));

        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setBackground(new Color(229, 204, 255));

        String[] fields = {"source id", "year", "title", "abstract", "full paper", "author's first name", "author's last name", "author's Institution", "all"};
        fieldSelection = new JComboBox<>(fields);

        String[] searchOptions = {"Search by Field", "Search by Keyword", "Search by Synonym"};
        searchType = new JComboBox<>(searchOptions);
        
        searchType.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    switch (value.toString()) {
                        case "Search by Field":
                            setToolTipText("Search By Field: It allows focusing on specific fields to provide the possibility of more targeted searching by the user.");
                            break;
                        case "Search by Keyword":
                            setToolTipText("Search By Keyword: It involves the search of exact words or phrases in order to conduct a more specific and precise search based on the specific terminology of the subject of interest.");
                            break;
                        case "Search by Synonym":
                            setToolTipText("Search By Synonym: It offers a broader range of targeted results, by searching not only for the exact words but also their synonyms.");
                            break;
                    }
                }
                return this;
            }
        });
        
        
        String[] sortOptionsList = {"No Sorting","Sort by Year"};
        sortOptions = new JComboBox<>(sortOptionsList);
        
    }

    private void setupLayout() {
        northPanel = new JPanel(new FlowLayout());
        northPanel.setBackground(new Color(235, 235, 235));

        northPanel.add(searchField);
        northPanel.add(searchType);
        northPanel.add(fieldSelection);
        northPanel.add(sortOptions);
        northPanel.add(searchButton);
        northPanel.add(prevButton);
        northPanel.add(nextButton);
        northPanel.add(historyButton);

        JScrollPane scrollPane = new JScrollPane(resultsPanel);
        setLayout(new BorderLayout());
        add(northPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 500);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void addEventListeners() {
        searchButton.addActionListener(this::performSearch);
        nextButton.addActionListener(this::nextPage);
        prevButton.addActionListener(this::previousPage);
        historyButton.addActionListener(this::viewSearchHistory);
    }

private void viewSearchHistory(ActionEvent e) {
    // Display search history in a new window
    JDialog historyDialog = new JDialog(this, "Search History", Dialog.ModalityType.APPLICATION_MODAL);
    historyDialog.setSize(500, 400);
    historyDialog.setLocationRelativeTo(this);

    JPanel historyPanel = new JPanel();
    historyPanel.setLayout(new BoxLayout(historyPanel, BoxLayout.Y_AXIS));
    JScrollPane historyScrollPane = new JScrollPane(historyPanel);
    historyDialog.add(historyScrollPane);

    // Populate the panel with search history
    for (SearchRecord record : SearchHistory.getInstance().getRecords()) {
        //String buttonText = record.getQuery() + " - " + record.getField() + " (" + record.getSearchOption() + ")";
        
    	String buttonText = record.getQuery() + "   (" + record.getSearchOption() + " , field: " + record.getField() +")" ;

    	JButton historyEntry = new JButton(buttonText);
        historyEntry.addActionListener(event -> {
            documents = record.getDocuments();
            totalHits = documents.size();
            currentPage = 0;
            displayResults();
            historyDialog.dispose();
        });
        historyPanel.add(historyEntry);
    }

    historyDialog.setVisible(true);
}



    private void performSearch(ActionEvent e) {
        currentPage = 0; // Reset to the first page for a new search
        executeSearch();
    }

    private void nextPage(ActionEvent e) {
        currentPage++; // Increment page number
        displayResults();
    }

    private void previousPage(ActionEvent e) {
        if (currentPage > 0) {
            currentPage--; // Decrement page number
            displayResults();
        }
    }

    private void updateButtonStates() {
        int totalPages = (int) ((totalHits + rowsPerPage - 1) / rowsPerPage);
        nextButton.setEnabled(currentPage + 1 < totalPages);
        prevButton.setEnabled(currentPage > 0);
    }

    private void executeSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            resultsPanel.removeAll();
            resultsPanel.revalidate();
            resultsPanel.repaint();
            return;
        }

        try {
        	String userFieldSelection = (String) fieldSelection.getSelectedItem();
            String selectedField = fieldMap.get(userFieldSelection);  // retrieves the actual value of field
            String selectedSearchOption = (String) searchType.getSelectedItem();

            Searcher.SearchResults results = null;

            switch (searchType.getSelectedIndex()) {
                case 0: // "Search by Field"
                    if (selectedField.equals("all")) {
                        results = searcher.findAllWithTotal(query, 0, Integer.MAX_VALUE);
                    } else {
                        results = searcher.findFieldWithTotal(query, selectedField, 0, Integer.MAX_VALUE);
                    }
                    break;
                case 1: // "Search by Keyword"
                    if (selectedField.equals("all")) {
                        results = searcher.findKeywordAllFieldsWithTotal(query, 0, Integer.MAX_VALUE);
                    } else {
                        results = searcher.findKeywordFieldWithTotal(query, selectedField, 0, Integer.MAX_VALUE);
                    }
                    break;
                case 2: // "Search by Synonym"
                    if (!selectedField.equals("all")) {
                        results = searcher.findSynonymFieldWithTotal(query, selectedField, 0, Integer.MAX_VALUE);
                    } else {
                        results = searcher.findSynonymAllFieldsWithTotal(query, 0, Integer.MAX_VALUE);
                    }
                    break;
            }
            // Sort documents based on the selected sorting option
            String selectedSortOption = (String) sortOptions.getSelectedItem();
            if (selectedSortOption != null && selectedSortOption.equals("Sort by Year")) {
                if (results != null && !results.documents.isEmpty()) {
                    results.documents.sort(Comparator.comparingInt(doc -> {
                        String year = doc.get("year");
                        
                        try {
                        	if (year!=null) {
                        		return Integer.parseInt(year.trim());
                        	}return 2050;	//big integer to sort the null last!
                        } catch (NumberFormatException e) {
                            return Integer.MAX_VALUE; // Place non-numeric years at the end
                        }
                    }));
                }
	                documents = results.documents;
	                totalHits = results.totalHits;
                    SearchHistory.getInstance().addRecord(new SearchRecord(query, selectedField, selectedSearchOption, documents));

	                displayResults();
            }

            else {
	            if (results != null) {
	                documents = results.documents;
	                totalHits = results.totalHits;
                    SearchHistory.getInstance().addRecord(new SearchRecord(query, selectedField, selectedSearchOption, documents));

	                displayResults();
	            }
            }
        } catch (Exception ex) {
            resultsPanel.removeAll();
            resultsPanel.revalidate();
            resultsPanel.repaint();
            resultsPanel.add(new JLabel("Error performing search: " + ex.getMessage()));
            ex.printStackTrace();
        }
    }


    private void displayResults() {
        resultsPanel.removeAll(); // Clear previous results

        if (documents == null || documents.isEmpty()) {

            JLabel nolabel = new JLabel("No documents found");
            Font noFont = new Font("Algerian", Font.PLAIN, nolabel.getFont().getSize() + 10);
            nolabel.setFont(noFont);
            resultsPanel.add(nolabel);
        } else {
            int start = currentPage * rowsPerPage;
            int end = Math.min(start + rowsPerPage, documents.size());

            // Add the total documents found and current page display
            JLabel foundLabel = new JLabel("Found " + totalHits + " documents.");
            JLabel pageLabel = new JLabel("Displaying page " + (currentPage + 1));
            Font boldFont = new Font(foundLabel.getFont().getFontName(), Font.BOLD, foundLabel.getFont().getSize() + 2);
            foundLabel.setFont(boldFont);
            pageLabel.setFont(boldFont);

            resultsPanel.add(foundLabel);
            resultsPanel.add(pageLabel);
            resultsPanel.add(new JSeparator());

            for (int i = start; i < end; i++) {
                Document doc = documents.get(i);

                String abstractText = doc.get("abstract");
                StringBuilder formattedAbstract = new StringBuilder();
                int currentWords = 0;
                String[] words = abstractText.split(" "); // Split abstract into words

                for (String word : words) {
                    if (currentWords + word.length() > 100) {
                        formattedAbstract.append("<br>"); // Add HTML line break
                        currentWords = 0;
                    }
                    formattedAbstract.append(word).append(" ");
                    currentWords += word.length() + 1; // Add word length and space
                }

                JPanel docPanel = new JPanel();
                docPanel.setLayout(new BoxLayout(docPanel, BoxLayout.Y_AXIS));
                docPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                docPanel.setBackground(new Color(229, 204, 255));

                JLabel sourceIdLabel = new JLabel("<html>Source ID: " + highlightKeywords(doc.get("source_id")));
                JLabel yearLabel = new JLabel("<html>Year: " + highlightKeywords(doc.get("year")));
                JLabel titleLabel = new JLabel("<html>Title: " + highlightKeywords(doc.get("title")+"</html>"));
                JLabel first_namelabel = new JLabel("<html>First Name: " + highlightKeywords(doc.get("first_name")));
                JLabel last_namelabel = new JLabel("<html>Last Name: " + highlightKeywords(doc.get("last_name")));
                JLabel institutionlabel = new JLabel("<html>Institution: " + highlightKeywords(doc.get("institution")));
                JLabel abstractlabel = new JLabel("<html>Abstract: " + highlightKeywords(formattedAbstract.toString()) + "</html>"); // Highlight keywords in abstract

                sourceIdLabel.setFont(new Font("Algerian", Font.PLAIN, 16));
                yearLabel.setFont(new Font("Algerian", Font.PLAIN, 16));
                titleLabel.setFont(new Font("Algerian", Font.PLAIN, 16));
                first_namelabel.setFont(new Font("Algerian", Font.PLAIN, 16));
                last_namelabel.setFont(new Font("Algerian", Font.PLAIN, 16));
                institutionlabel.setFont(new Font("Algerian", Font.PLAIN, 16));
                abstractlabel.setFont(new Font("Algerian", Font.PLAIN, 16));

                docPanel.add(sourceIdLabel);
                docPanel.add(yearLabel);
                docPanel.add(titleLabel);
                docPanel.add(first_namelabel);
                docPanel.add(last_namelabel);
                docPanel.add(institutionlabel);
                docPanel.add(abstractlabel);

                JButton viewButton = new JButton("View Full Paper");
                viewButton.setFont(new Font("Algerian", Font.PLAIN, 16));
                viewButton.addActionListener(e -> {
                	JTextArea fullPaperArea = new JTextArea(doc.get("full_text"));
                    Font font = new Font("abadi", Font.PLAIN, 18);
                    fullPaperArea.setFont(font);
                    JScrollPane scrollPane = new JScrollPane(fullPaperArea);
                    scrollPane.setPreferredSize(new Dimension(800, 600));
                    JOptionPane.showMessageDialog(this, scrollPane, "Full Paper", JOptionPane.INFORMATION_MESSAGE);
                });

                docPanel.add(viewButton);
                resultsPanel.add(docPanel);
                resultsPanel.add(new JSeparator()); // Add a separator line
                resultsPanel.add(Box.createRigidArea(new Dimension(0, 20))); // Add some spacing between documents
            }

            int totalPages = (int) ((totalHits + rowsPerPage - 1) / rowsPerPage);
            JLabel pagedown = new JLabel("Page " + (currentPage + 1) + " of " + totalPages);
            pagedown.setFont(boldFont);
            resultsPanel.add(pagedown);
        }

        resultsPanel.revalidate();
        resultsPanel.repaint();
        updateButtonStates(); // Update button states based on total hits and current page
    }

    // Function to highlight keywords in the text
    private String highlightKeywords(String text) {
        String query = searchField.getText().trim();
        String userFieldSelection = (String) fieldSelection.getSelectedItem();
        String selectedField = fieldMap.get(userFieldSelection);  // retrieves the actual value of field
        String selectedSearchOption = (String) searchType.getSelectedItem();
        if (text == null) {   // handles specific source_id outputs that were not caught
            return null; // Return null if the input text is null
        }
        
        if (selectedSearchOption.equals("Search by Keyword")) {
        	if (selectedField.equals("abstract")||selectedField.equals("title")||selectedField.equals("year")||selectedField.equals("full_text")||selectedField.equals("institution")
	        		||selectedField.equals("first_name") ||selectedField.equals("last_name")|| selectedField.equals("all")|| selectedField.equals("source_id")) {
	        
		        if (query.isEmpty() || selectedField == null) {
		            return text; // No highlighting if the query or selected field is empty
		        }
		
		        String[] keywords = query.split("\\s+"); // Split query into keywords
		        for (String keyword : keywords) {
		        	text = text.replaceAll("(?i)\\b(" + keyword + ")\\b", "<span style='background-color: rgb(255, 51, 153);'>$1</span>");
		        }
		        return text;
	        }
        }return text;
    }

    private void initializeSearcher() {
        try {
            searcher = new Searcher();  // Initialize the searcher instance
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to initialize searcher: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return;  // Exit if the searcher failed to initialize
        }
        // Initially, no search has been performed, so we assume there are no results
        // Therefore, disable both navigation buttons:
        nextButton.setEnabled(false);
        prevButton.setEnabled(false);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SearchGUI::new);
    }
}
