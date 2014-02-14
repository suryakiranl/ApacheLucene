package edu.cmu.sv.lucene;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

/**
 * Standalone class to parse a given folder and print files which
 * contain the search text.
 *  
 * @author Surya Kiran
 *
 */
public class SearchFileContents {
	// RAMDirectory class to store the index in memory
	private static RAMDirectory ramDirForIndex = new RAMDirectory();
	
	// List of file names that are included in the search
	private static List<String> files = new ArrayList<String>();
	
	// Name of the folder whose files should be searched.
	private static String folderToSearch; 
	
	private static final String DOC_KEY = "key";
	private static final String DOC_VALUE = "value";
	
	/**
	 * Main method to execute this program.
	 * 
	 * @param args - args[0] : Folder path whose immediate child files 
	 * will be considered for text search.
	 */
	public static void main(String[] args) {
		if(args.length > 0) {
			folderToSearch = args[0];
		} else {
			folderToSearch = "C:\\Temp\\java";
		}
		
		try {
			buildIndex();
			parseAndPrintOccurances("build");
		} catch (IOException | ParseException e) {
			System.out.println("There was an error when processing the program. More details are available below ...");
			e.printStackTrace();
		}

	}
	
	/**
	 * Method to build the RAMDirectory index.
	 *  - It processes all files under the folder specified, and
	 *  	adds them to the index.
	 *  
	 * @throws IOException
	 */
	private static void buildIndex() throws IOException {
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_46, analyzer);
		
		IndexWriter iw = new IndexWriter(ramDirForIndex, iwc);
		
		File srcDir = new File(folderToSearch);
		File[] srcFiles = srcDir.listFiles();
		System.out.println("Files included in search under directory :: " + folderToSearch);
		for(File file : srcFiles) {
			if(file.isFile()) {
				Document doc = new Document();
				
				FieldType indexedFieldType = new FieldType();
				indexedFieldType.setIndexed(true);
				
				Field keyField = new Field(DOC_KEY, file.getCanonicalPath(), indexedFieldType);
				Field valueField = new TextField(DOC_VALUE, new FileReader(file));
				
				doc.add(keyField);
				doc.add(valueField);
				
				iw.addDocument(doc);	
				System.out.println(" >> ADDED FILE TO SEARCH: " + file.getCanonicalPath());
				files.add(file.getCanonicalPath());
			} else {
				System.out.println(" << SKIPPED (DIR): " + file.getCanonicalPath());
			}
		}
		
		System.out.println("RAM Size in bytes to index this:: " + iw.ramSizeInBytes());
		
		iw.prepareCommit();
		iw.commit();
		iw.close();
	}
	
	/**
	 * Method which matches the string to be searched with the RAM index.
	 * 
	 * @param searchText - Text to search for
	 * 
	 * @throws IOException
	 * @throws ParseException
	 */
	private static void parseAndPrintOccurances(String searchText) throws IOException, ParseException {
		@SuppressWarnings("deprecation")
		IndexReader reader = IndexReader.open(ramDirForIndex);		
		IndexSearcher searchBot = new SearcherFactory().newSearcher(reader);
		
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
		QueryParser parser = new QueryParser(Version.LUCENE_46, DOC_VALUE, analyzer);
		Query query = parser.parse(searchText);
		
		System.out.println();
		System.out.println("Matching results for string:: " + searchText);
		TopDocs results = searchBot.search(query, 100);
		System.out.println("Total hits found = " + results.totalHits);
		for(ScoreDoc sd : results.scoreDocs) {
			System.out.println("======================");
			System.out.println("Doc = " + sd.doc);		
			System.out.println("File = " + files.get(sd.doc));
			System.out.println("Score = " + sd.score);
			System.out.println("Shared Index = " + sd.shardIndex);
		}
 	}
}
