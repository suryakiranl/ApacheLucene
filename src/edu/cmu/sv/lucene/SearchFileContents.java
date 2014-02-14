package edu.cmu.sv.lucene;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

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

public class SearchFileContents {
	private static RAMDirectory ramDirForIndex = new RAMDirectory();
	private static String folderToSearch; 
	
	private static final String INDEX_KEY = "key";
	private static final String INDEX_VALUE = "value";
	
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
	
	private static void buildIndex() throws IOException {
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_46, analyzer);
		
		IndexWriter iw = new IndexWriter(ramDirForIndex, iwc);
		
		File srcDir = new File(folderToSearch);
		File[] srcFiles = srcDir.listFiles();
		for(File file : srcFiles) {
			if(file.isFile()) {
				Document doc = new Document();
				
				FieldType indexedFieldType = new FieldType();
				indexedFieldType.setIndexed(true);
				
				Field keyField = new Field(INDEX_KEY, file.getCanonicalPath(), indexedFieldType);
				Field valueField = new TextField(INDEX_VALUE, new FileReader(file));
				
				doc.add(keyField);
				doc.add(valueField);
				
				iw.addDocument(doc);	
				System.out.println(" >> ADDED: " + file.getCanonicalPath());
			} else {
				// System.out.println(" << SKIPPED: " + file.getCanonicalPath());
			}
		}
		
		System.out.println("RAM Size in bytes to index this:: " + iw.ramSizeInBytes());
		
		iw.prepareCommit();
		iw.commit();
		iw.close();
	}
	
	private static void parseAndPrintOccurances(String searchText) throws IOException, ParseException {
		@SuppressWarnings("deprecation")
		IndexReader reader = IndexReader.open(ramDirForIndex);		
		IndexSearcher searchBot = new SearcherFactory().newSearcher(reader);
		
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
		QueryParser parser = new QueryParser(Version.LUCENE_46, INDEX_VALUE, analyzer);
		Query query = parser.parse(searchText);
		
		TopDocs results = searchBot.search(query, 100);
		System.out.println("Total hits found = " + results.totalHits);
		for(ScoreDoc sd : results.scoreDocs) {
			System.out.println("======================");
			System.out.println("Doc = " + sd.doc);			
			System.out.println("Score = " + sd.score);
			System.out.println("Shared Index = " + sd.shardIndex);
		}
 	}

}
