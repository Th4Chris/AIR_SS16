package infoRetrieval;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.document.Field.Store;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LuceneIndexer {
	private static List<Topic> topics;
	public static void main(String[] args) {
        
		indexDirectory();
		getTopics();
		searchAllTopics();
    }   
	
	private static List<File> getFilesInDirectory(File dir) {
		File[] listOfFiles = dir.listFiles();
		List<File> res=new ArrayList<File>();
	    for (int i = 0; i < listOfFiles.length; i++) {
	      if (listOfFiles[i].isFile()) {
	        res.add(listOfFiles[i]);
	      } else if (listOfFiles[i].isDirectory()) {
	        res.addAll(getFilesInDirectory(listOfFiles[i]));
	      }
	    }
	    return res;
	}
	/*
	 * Procedure indexes all files in directory and its subdirectories
	 */
	 private static void indexDirectory() {          
         try {  
        	 //where the indexes will be saved
         Path path = Paths.get("indexes");
         Directory directory = FSDirectory.open(path);
         IndexWriterConfig config = new IndexWriterConfig(new SimpleAnalyzer());        
         IndexWriter indexWriter = new IndexWriter(directory, config);
         indexWriter.deleteAll();
         //where are the files to be indexed
         File f = new File("collection"); 
         List<File> allFiles=getFilesInDirectory(f);
             for (File file : allFiles) {
                    System.out.println("indexed " + file.getCanonicalPath()); 
                    Document doc = new Document();
                    FileInputStream is = new FileInputStream(file);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuffer stringBuffer = new StringBuffer();
                    String line = null;
                    boolean headline=false;
                    boolean text=false;
                    String h="";
                    String t="";
                    String docno="";
                    while((line = reader.readLine())!=null){
                      stringBuffer.append(line).append("\n");
                      if (line.indexOf("<DOCNO>")!=-1) {
                    	  docno=line.replace("<DOCNO>", "");
                    	  docno=line.replace("</DOCNO>", "");
                    	  docno=line.replace(" ", "");
                    	  doc = new Document();
                          doc.add(new TextField("path", file.getName(), Store.YES));
                          doc.add(new TextField("docno",docno, Store.YES));
                      }
                      else if (line.indexOf("<HEADLINE>")!=-1){
                    	  headline=true;
                      }
                      else if (line.indexOf("<TEXT>")!=-1){
                    	  text=true;
                      }
                      else if (line.indexOf("</HEADLINE>")!=-1){
                    	  headline=false;
                      }
                      else if (line.indexOf("</TEXT>")!=-1){
                    	  text=false;
                      }
                      else if (headline) {
                    	 h+=line; 
                      }
                      else if (text){
                    	  t+=line;
                      } 
                      else if (line.indexOf("</DOC>")!=-1){
                    	  doc.add(new TextField("headline",h, Store.YES));
                 		  doc.add(new TextField("text",t, Store.YES));                             	  
                    	  indexWriter.addDocument(doc);
                    	  h="";
                    	  t="";
                      }
                    	  
                    }
                    reader.close(); 
                    	 }             
             indexWriter.close();           
             directory.close();
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }                   
    }
	/*
	 * Procedure reads topics from text file.
	 */
	 private static void getTopics(){
		 topics=new ArrayList<Topic>();
			BufferedReader br = null;
			try {

				String sCurrentLine;
				int id=0;
				String title="";
				String description="";
				String narrative="";
				boolean desc=true;
				br = new BufferedReader(new FileReader("topicsTREC8Adhoc.txt"));
				while ((sCurrentLine = br.readLine()) != null) {
					if (sCurrentLine.indexOf("<num>")!=-1) {
						id=Integer.parseInt(sCurrentLine.replaceAll("[^0-9]", ""));
					}
					else if (sCurrentLine.indexOf("<title>")!=-1) {
						title=sCurrentLine.replace("<title> ", "");
					}
					else if (sCurrentLine.indexOf("<desc>")!=-1) {
						desc=true;
					}
					else if (sCurrentLine.indexOf("<narr>")!=-1) {
						desc=false;
					}
					else if (sCurrentLine.indexOf("</top>")!=-1) {
						topics.add(new Topic(id,title,description,narrative));
						title="";
						description="";
						narrative="";
					}
					else if (sCurrentLine!="" && sCurrentLine.indexOf("<top>")==-1){
						if (desc) {
							description+=sCurrentLine+" ";
						}
						else {
							narrative+=sCurrentLine+" ";
						}
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (br != null)br.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
			
	 }
	 private static void searchAllTopics(){
		 for (Topic t:topics) {
			 String query=t.getTitle().replace(",", " OR");
			 search(t,query); 
		 }
	 }
     
    private static void search(Topic t,String text) {  
        try {   
            Path path = Paths.get("indexes");
            Directory directory = FSDirectory.open(path);       
            IndexReader indexReader =  DirectoryReader.open(directory);
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);
            MultiFieldQueryParser queryParser = new MultiFieldQueryParser(new String[] {"headline", "text"},new StandardAnalyzer());  
            Query query = queryParser.parse(text);
            TopDocs topDocs = indexSearcher.search(query,1000);
            //System.out.println("totalHits " + topDocs.totalHits);
            int ranking=1;
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {           
            	org.apache.lucene.document.Document document = indexSearcher.doc(scoreDoc.doc);
            	String docno=String.valueOf(document.getField("docno"));
            	docno=docno.substring(docno.indexOf(">")+1, docno.indexOf("</"));
            	System.out.println(t.getId()+" Q "+docno+" "+ranking+" "+scoreDoc.score+" assignment1");
                ranking++;
                
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }               
    }
	 
}
