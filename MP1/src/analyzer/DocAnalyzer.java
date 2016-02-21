/**
 *
 */
package analyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import org.tartarus.snowball.ext.porterStemmer;
import structures.LanguageModel;
import structures.Post;
import structures.Token;

/**
 * @author hongning
 * Sample codes for demonstrating OpenNLP package usage
 * NOTE: the code here is only for demonstration purpose,
 * please revise it accordingly to maximize your implementation's efficiency!
 */
public class DocAnalyzer {
	//N-gram to be created
	int m_N;

	//a list of stopwords
	HashSet<String> m_stopwords;

	//you can store the loaded reviews in this arraylist for further processing
	ArrayList<Post> m_reviews;

	//you might need something like this to store the counting statistics for validating Zipf's and computing IDF
	HashMap<String, Token> m_stats;
	HashMap<String, Token> m_ttf;
	HashMap<String, Token> m_df;

	//we have also provided a sample implementation of language model in src.structures.LanguageModel
	Tokenizer m_tokenizer;

	//this structure is for language modeling
	LanguageModel m_langModel;

	public DocAnalyzer(String tokenModel, int N) throws InvalidFormatException, FileNotFoundException, IOException {
		m_N = N;
		m_stopwords = new HashSet<String>();
		m_reviews = new ArrayList<Post>();
		m_stats = new HashMap<String, Token>();
		m_ttf = new HashMap<String, Token>();
		m_df = new HashMap<String, Token>();
		m_tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream(tokenModel)));
	}

	//sample code for loading a list of stopwords from file
	//you can manually modify the stopword file to include your newly selected words
	public void LoadStopwords(String filename) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			String line;

			while ((line = reader.readLine()) != null) {
				//it is very important that you perform the same processing operation to the loaded stopwords
				//otherwise it won't be matched in the text content
				line = SnowballStemming(Normalization(line));
				if (!line.isEmpty())
					m_stopwords.add(line);
			}
			reader.close();
			System.out.format("Loading %d stopwords from %s\n", m_stopwords.size(), filename);
		} catch(IOException e){
			System.err.format("[Error]Failed to open file %s!!", filename);
		}
	}

	public void analyzeDocument(JSONObject json) {
		try {
			JSONArray jarray = json.getJSONArray("Reviews");
			for(int i=0; i<jarray.length(); i++) {
				Post review = new Post(jarray.getJSONObject(i));

				String[] tokens = Tokenize(review.getContent());
				review.setTokens(tokens);

				/**
				 * HINT: perform necessary text processing here based on the tokenization results
				 * e.g., tokens -> normalization -> stemming -> N-gram -> stopword removal -> to vector
				 * The Post class has defined a "HashMap<String, Token> m_vector" field to hold the vector representation
				 * For efficiency purpose, you can accumulate a term's DF here as well
				 */
				// Maintain a N-word history for constructing N-gram
		    List<String> n_history = new LinkedList<String>();
		    for (int j = 0; j < m_N - 1; j++) {
		      n_history.add("");
		    }
		    // For calculate DF
        HashSet<String> token_set = new HashSet<String>();
		    for (String token : tokens) {
		      // Normalization and Stemming
		      String s = SnowballStemming(Normalization(token));
		      String n_gram = "";

		      // N-gram construction and stopword removing
          if (m_stopwords.contains(s)) s = "";
		      if (!s.isEmpty()) {
		        boolean skip = false;
		        for (int j = 0; j < n_history.size(); j++) {
		          if (n_history.get(j).isEmpty()) {
		            n_gram = "";
		            skip = true;
		            break;
		          }
		          n_gram += n_history.get(j) + "_";
		        }
		        if (!skip) n_gram += s;
		      }

		      // Statistics
		      if (!n_gram.isEmpty()) {
		        // Record token in m_stats
            if (m_stats.containsKey(n_gram)) {
              m_stats.get(n_gram).setValue(m_stats.get(n_gram).getValue() + 1);
            } else {
              Token t = new Token(n_gram);
              t.setValue(1);
              m_stats.put(n_gram, t);
            }
            // TTF
            if (m_ttf.containsKey(n_gram)) {
              m_ttf.get(n_gram).setValue(m_ttf.get(n_gram).getValue() + 1);
            } else {
              Token t = new Token(n_gram);
              t.setValue(1);
              m_ttf.put(n_gram, t);
            }
            // DF
            token_set.add(n_gram);
          }
		      n_history.add(s);
		      n_history.remove(0);
		    }

		    // DF
		    for (String token : token_set) {
		      if (m_df.containsKey(token)) {
            m_df.get(token).setValue(m_df.get(token).getValue() + 1);
		      } else {
            Token t = new Token(token);
            t.setValue(1);
            m_df.put(token, t);
		      }
		    }
				m_reviews.add(review);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void createLanguageModel() {
		m_langModel = new LanguageModel(m_N, m_stats.size());

		for(Post review:m_reviews) {
			String[] tokens = Tokenize(review.getContent());
			/**
			 * HINT: essentially you will perform very similar operations as what you have done in analyzeDocument()
			 * Now you should properly update the counts in LanguageModel structure such that we can perform maximum likelihood estimation on it
			 */
		}
	}

	//sample code for loading a json file
	public JSONObject LoadJson(String filename) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			StringBuffer buffer = new StringBuffer(1024);
			String line;

			while((line=reader.readLine())!=null) {
				buffer.append(line);
			}
			reader.close();

			return new JSONObject(buffer.toString());
		} catch (IOException e) {
			System.err.format("[Error]Failed to open file %s!", filename);
			e.printStackTrace();
			return null;
		} catch (JSONException e) {
			System.err.format("[Error]Failed to parse json file %s!", filename);
			e.printStackTrace();
			return null;
		}
	}

	// sample code for demonstrating how to recursively load files in a directory
	public void LoadDirectory(String folder, String suffix) {
		File dir = new File(folder);
		int size = m_reviews.size();
		for (File f : dir.listFiles()) {
		  System.out.println("Loading " + f.getName()); //dg
			if (f.isFile() && f.getName().endsWith(suffix))
				analyzeDocument(LoadJson(f.getAbsolutePath()));
			else if (f.isDirectory())
				LoadDirectory(f.getAbsolutePath(), suffix);
		}
		size = m_reviews.size() - size;
		System.out.println("Loading " + size + " review documents from " + folder);
		System.out.println("Loading " + m_stats.size() + " tokens");
	}

	//sample code for demonstrating how to use Snowball stemmer
	public String SnowballStemming(String token) {
		SnowballStemmer stemmer = new englishStemmer();
		stemmer.setCurrent(token);
		if (stemmer.stem())
			return stemmer.getCurrent();
		else
			return token;
	}

	//sample code for demonstrating how to use Porter stemmer
	public String PorterStemming(String token) {
		porterStemmer stemmer = new porterStemmer();
		stemmer.setCurrent(token);
		if (stemmer.stem())
			return stemmer.getCurrent();
		else
			return token;
	}

	//sample code for demonstrating how to perform text normalization
	//you should implement your own normalization procedure here
	public String Normalization(String token) {
		// remove all non-word characters
		// please change this to removing all English punctuation
		//token = token.replaceAll("\\W+", "");
	  token = token.replaceAll("\\p{Punct}", "");

		// convert to lower case
		token = token.toLowerCase();

		// add a line to recognize integers and doubles via regular expression
		// and convert the recognized integers and doubles to a special symbol "NUM"

		if (token.matches("\\d+(?:\\.\\d+)?")) {
		  token = "NUM";
		}

		return token;
	}

	String[] Tokenize(String text) {
		return m_tokenizer.tokenize(text);
	}

	public void TokenizerDemon(String text) {
		System.out.format("Token\tNormalization\tSnonball Stemmer\tPorter Stemmer\n");
		for(String token:m_tokenizer.tokenize(text)){
			System.out.format("%s\t%s\t%s\t%s\n", token, Normalization(token), SnowballStemming(token), PorterStemming(token));
		}
	}


  public void TokenizerDemon2(String text) {
    System.out.format("\n----------------\n");
    /**
     * HINT: perform necessary text processing here based on the tokenization
     * results e.g., tokens -> normalization -> stemming -> N-gram -> stopword
     * removal -> to vector The Post class has defined a
     * "HashMap<String, Token> m_vector" field to hold the vector representation
     * For efficiency purpose, you can accumulate a term's DF here as well
     */
    List<String> n_history = new LinkedList<String>();
    for (int i = 0; i < m_N - 1; i++) {
      n_history.add("");
    }
    for (String token : m_tokenizer.tokenize(text)) {
      String s = SnowballStemming(Normalization(token));
      String n_gram = "";

      if (m_stopwords.contains(s)) {
        s = "";
      }

      // N-gram construction and stopwords removing
      if (!s.isEmpty()) {
        boolean skip = false;
        for (int i = 0; i < n_history.size(); i++) {
          if (n_history.get(i).isEmpty()) {
            n_gram = "";
            skip = true;
            break;
          }
          n_gram += n_history.get(i) + "_";
        }
        if (!skip) n_gram += s;
      }

      System.out.format("%s\t\t\t%s\n", token, n_gram);
      n_history.add(s);
      n_history.remove(0);
    }
  }

	public void Zipf() {
	  try {
	    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("zipf-ttf.txt"), "utf-8"));
	    for (Token t: m_ttf.values()) {
	      writer.write(t.getToken() + " " + t.getValue() + "\n");
	    }
	    writer.close();
	  }
	  catch (IOException e) {
	    e.printStackTrace();
	  }
	}

	public static Comparator<Token> TokenComparator = new Comparator<Token>() {
	  @Override
    public int compare(Token t1, Token t2) {
	    return Double.compare(t2.getValue(), t1.getValue());
	  }
	};

	public List<Token> get_sorted_ttf() {
	  List<Token> ttf_list = new ArrayList<Token>();
	  for (Token t: m_ttf.values()) {
	    ttf_list.add(t);
	  }
	  Collections.sort(ttf_list, TokenComparator);
	  return ttf_list;
	}

  public List<Token> get_sorted_df() {
    List<Token> df_list = new ArrayList<Token>();
    for (Token t: m_df.values()) {
      df_list.add(t);
    }
    Collections.sort(df_list, TokenComparator);
    return df_list;
  }

	public static void main(String[] args) throws InvalidFormatException, FileNotFoundException, IOException {
	  // create a unigram model for demonstrating Zipf's law
		DocAnalyzer analyzer = new DocAnalyzer("./data/Model/en-token.bin", 1);

		// code for demonstrating tokenization and stemming
		//analyzer.TokenizerDemon("I've practiced for 30 years in pediatrics, and I've never seen anything quite like this. A0a, A.a, A#a, 000, 0.12, A'a");
    //analyzer.TokenizerDemon2("I've practiced for 30 years in pediatrics, and I've never seen anything quite like this. A0a, A.a, A#a, 000, 0.12, A'a");

		// load stopwords
		analyzer.LoadStopwords("./data/english.stop.txt");

		// entry point to deal with a collection of documents
		analyzer.LoadDirectory("./data/yelp/train", ".json");
		System.out.println("Total words: " + analyzer.m_stats.size());
    //analyzer.LoadDirectory("./data/yelp/test", ".json");
    //System.out.println("Total words: " + analyzer.m_stats.size());

		// output ttf to zipf-ttf.txt
		//analyzer.Zipf();

		List<Token> ttf_list = analyzer.get_sorted_ttf();
		System.out.println("Top 50 TTF: ");
		for (int i = 0; i < 50; i++) {
		  Token t = ttf_list.get(i);
		  System.out.print(t.getToken() + "(" + t.getValue() + "), ");
		}
		System.out.println();

		List<Token> df_list = analyzer.get_sorted_df();
		System.out.println("Top 50 DF: ");
		for (int i = 0; i < 50; i++) {
		  Token t = df_list.get(i);
		  System.out.print(t.getToken() + "(" + t.getValue() + "), ");
		}
		System.out.println();

    // create a bigram model
    DocAnalyzer analyzer2 = new DocAnalyzer("./data/Model/en-token.bin", 2);
    analyzer2.LoadStopwords("./data/english.stop.txt");
    analyzer2.LoadDirectory("./data/yelp/train", ".json");
    System.out.println("Total words: " + analyzer2.m_stats.size());

    List<Token> ttf_list2 = analyzer2.get_sorted_ttf();
    System.out.println("Top 50 TTF: ");
    for (int i = 0; i < 50; i++) {
      Token t = ttf_list2.get(i);
      System.out.print(t.getToken() + "(" + t.getValue() + "), ");
    }
    System.out.println();

    List<Token> df_list2 = analyzer2.get_sorted_df();
    System.out.println("Top 50 DF: ");
    for (int i = 0; i < 50; i++) {
      Token t = df_list2.get(i);
      System.out.print(t.getToken() + "(" + t.getValue() + "), ");
    }
    System.out.println();


    // Merge unigram and bigram
    List<Token> merge_voc = new ArrayList<Token>();
    for (Token t : df_list) {
      if (t.getValue() >= 50) {
        merge_voc.add(t);
      }
    }
    for (Token t : df_list2) {
      if (t.getValue() >= 50) {
        merge_voc.add(t);
      }
    }
    Collections.sort(merge_voc, TokenComparator);
    System.out.println("Vocabulary size after merging: " + merge_voc.size());

    System.out.println("Top 100 DF (new stopwords): ");
    for (int i = 0; i < 100; i++) {
      Token t = merge_voc.get(i);
      System.out.print(t.getToken() + "(" + t.getValue() + "), ");
    }
    System.out.println();
    System.out.println("--------");
    for (int i = 0; i < 100; i++) {
      Token t = merge_voc.get(i);
      System.out.println(t.getToken());
    }
    System.out.println("--------");

    // Get controlled vocabulary
    if (merge_voc.size() < 100) return;
    List<Token> ctrl_voc = new ArrayList<Token>();
    for (int i = 100; i < merge_voc.size(); i++) {
      ctrl_voc.add(merge_voc.get(i));
    }
    System.out.println("Controlled Vocabulary size: " + ctrl_voc.size());

    int n_doc = analyzer2.m_reviews.size();
    System.out.println("Top 50 CV: ");
    for (int i = 0; i < 50; i++) {
      Token t = ctrl_voc.get(i);
      double df = t.getValue();
      double idf = 1 + Math.log10(n_doc / df);
      System.out.format("%s (DF=%.0f, IDF=%.2f)\n", t.getToken(), df, idf);
    }
    System.out.println();

    System.out.println("Bottom 50 CV: ");
    for (int i = ctrl_voc.size() - 50; i < ctrl_voc.size(); i++) {
      Token t = ctrl_voc.get(i);
      double df = t.getValue();
      double idf = 1 + Math.log10(n_doc / df);
      System.out.format("%s (DF=%.0f, IDF=%.2f)\n", t.getToken(), df, idf);
    }
    System.out.println();

    // Here we get the controlled vocabulary ctrl_voc.


	}

}
