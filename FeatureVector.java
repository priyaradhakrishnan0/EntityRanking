package Version2;



import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Calendar;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

//args[0] - train / test / label
//args[1] - cluster type / EVALUATION
//args[2] - product file name
//args[3] - debug flag / none
//args[4] - eval flag / none
//args[5] - time /none 

public class FeatureVector {
	
	static HashMap<String, String> featureValueMap = new HashMap<String, String>();
	static HashMap<String, ArrayList<String>> featureValueListMap = new HashMap<String, ArrayList<String>>();
	static String[] colorList = {"white","black","pink","gray","red","purple","yellow","cyan","blue","brown","orange","green","gold","ivory","magenta","maroon","silver","tan","violet"};
	static String[] metricList = { "gb","hour","pound","minute","mm","mp" }; 
	static boolean DEBUG_FLAG = false;//arg[3] is debug flag
	static boolean EVAL_FLAG = false;//arg[4] is evaluation flag
	static boolean TIME_FLAG = false;//arg[5] is time variance feature flag
	
	String inPath = "/home/priya/Desktop/EntityRanking";//for local machine
	//String inPath = "/home/iiit/priya/EntityRanking";//for abacus 
	static String unlabelledTitle = null;
	public void clearMaps(){
		featureValueMap.clear();
		featureValueListMap.clear();
	}
	public static void main(String[] args) {

		FeatureVector fv = new FeatureVector();
		if(args[3].equalsIgnoreCase("debug")) fv.DEBUG_FLAG = true;
		if(args[4].equalsIgnoreCase("eval")) fv.EVAL_FLAG = true;
		if(args[5].equalsIgnoreCase("time")) fv.TIME_FLAG = true;
		
		fv.createFV( args[1], args[2]);//arg[2] is VIW Value tagged file

		if(fv.isProductLabel())
			fv.outputUnigramFV(args[0]);		
		
	}//End main
	
	public void createFV(String category, String filename){
		String taggedFile=null;	
		if(DEBUG_FLAG){
			System.out.println(filename);
		}		
		//category is i/p cluster type		
		if(category.contains("EVALUATION")){
			taggedFile = inPath+"/actualCrawl/"+filename;
		} else {
			taggedFile = inPath+"/dataset/"+category+"/"+filename;
		}	
		createFeatures(taggedFile);
		if(isProductLabel()){
			createAttributeValueFV(taggedFile);
			createContextFV();
			createLinguisticFV();
		}
	}//End createFV
	//create FeatureValueMap and FeatureValueListMap
	public void createFeatures(String taggedFile){
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(taggedFile));	
			String read=null,brandName=null,productName =null, versionName=null;	
			boolean  foundTitleWords = false;

			while(foundTitleWords == false && (read = br.readLine())!=null)
			{				
				String line = read.trim();				
				if(line.contains("productName:")){
					getProductLabel(line);//store all the words in product Label as a list in FV.
					line = line.replace("productName:", "");				
					//Remove the manual tags "brand, product and version"
					if(featureValueMap.get(brandName) != null){
						line = line.replace(featureValueMap.get(brandName),"");
					}
					if(featureValueMap.get(productName) != null){
						line = line.replace(featureValueMap.get(productName),"");
					}
					if(featureValueMap.get(versionName) != null){
						line = line.replace(featureValueMap.get(versionName),"");
					}
					unlabelledTitle = line;
					foundTitleWords = true;
				}//End if productName								
			}//End while	
			br.close();
		} catch (IOException e) {
			System.out.println("Unable to open productItem File "+taggedFile);
			e.printStackTrace();
		}	
	}//End createFeatures
	public boolean isProductLabel(){
		if(featureValueListMap.get("candidateWords") != null){
			return true;
		} else {
			return false;
		}
	}
	public void createAttributeValueFV(String taggedFile){

		try {
			BufferedReader br = new BufferedReader(new FileReader(taggedFile));	
			int idx =0, wtIdx = 0, pdIdx = 0, catIdx = 0, freqBoughtIdx=0, nextBoughtTitleIdx=0;
			String read;		
			while((read = br.readLine())!=null)
			{
				String line = read.trim();				
				++idx;
				//Extract product AV features and populate in featureValueMap	
				//1.productDescription
				if(line.contains("productDescription:"))
					pdIdx = idx + 1;
				if (idx == pdIdx){
						if (!line.isEmpty()){
							getProductDescription(line.trim());	//productDescription could be lengthy. So store words that occur in title and till wordCount = 100, as a list in FV.
							pdIdx += 1;
						} else {
							pdIdx = 0;
						}
				}//End if productDescription
				//2.url
				if(line.contains("url:")){
					getUrl(line);//store all the words in product URL as a list in FV.
				}//End url
				//3.rrTitle
				if(line.contains("rr.title:")){
					getReviewTitle(line);//store the words in reviewTitle as a list in FV.
				}//End rrTitle
				//4. weight
				if(line.contains("item weight;"))
					wtIdx = idx + 1;
				if (idx == wtIdx){
					if (!line.isEmpty()){
						getWeight(line.trim());							
					}
					wtIdx = 0;						
				}//End wt
				//5.catHier,
				if(line.contains("categoryHierarchies:"))
					catIdx = idx + 1;
				if (idx == catIdx){
						if (!line.isEmpty()){
							getCategoryHierarchies(line.trim());	//categoryHierarchies has many topics / labels. Collect these in a list in FV.
							catIdx += 1;
						} else {
							catIdx = 0;
						}
				}//catHier
				//6 & 7.Bought next. Extract otherItemsBoughtAfterViewingThisItem:
				if(line.contains("otherItemsBoughtAfterViewingThisItem:"))
					nextBoughtTitleIdx = idx + 2;
				if(idx == nextBoughtTitleIdx){
					if(!line.isEmpty() & !line.contains("explore similar items")){
						if(line.trim().startsWith("$")){
							nextBoughtTitleIdx += 1;
						} else  if(line.contains("out of 5 stars")){ 
							nextBoughtTitleIdx += 2;
						} else  if(line.trim().startsWith("by")){ 
							line = line.replace("by", ""); //System.out.println(line.trim());
							getnextBoughtBrand(line.trim()); //7
							nextBoughtTitleIdx += 3;
						} else {						
							getnextBoughtTitle(line.trim()); //6
							nextBoughtTitleIdx += 1;	
						}
					} else {
						nextBoughtTitleIdx = 0;
					}
				}//End if nextBought
				//8.freqBoughtTitle, Extract freqBoughtTogether
				if(line.contains("freqBoughtTogether:"))
					freqBoughtIdx = idx + 2;
				if(idx == freqBoughtIdx){
					if(!line.isEmpty()){
						if(line.trim().startsWith("$")){
							freqBoughtIdx += 1;
						} else {
							getfreqBoughtTitle(line.trim());
							freqBoughtIdx += 2;
						}
					} else {
						freqBoughtIdx = 0;
					}					
				}//End if freqBoughtTogether
				//9.rrDate. Filter review date				
				if(line.contains("rr.date:")){
					getReviewDate(line.trim());
				}//End Extract rr.date: 
				//10.model number "item model number;"
				if(line.contains("item model number;")){
					line = line.replace("item model number;", "");
					String modelLine = line.trim();
					if(modelLine != null){
						featureValueMap.put("model", modelLine);						
					}
				}//End Extract item model number;
				
				//Book features:publDate, release date
				//Camera features:	date first available at amazon.com	
								
			}//End while
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Unable to open productItem File "+taggedFile);
			e.printStackTrace();
		}	
	}//End createAttributeValueFV

	//store all the words in productLabel as a list in FV.
	public void getProductLabel(String line){
		String brandName = null, productName=null, versionName=null;
		line = line.replace("productName:", "");
		
		//Extract the manual tags "brand, product and version" 
		Pattern pBrand = Pattern.compile("\\{[a-zA-Z0-9 \\-+&]+\\}_b");
		Matcher mBrand = pBrand.matcher(line);
		while(mBrand.find())
		{
			String brandName0 = mBrand.group();
			brandName = brandName0.replace("{", "");
			brandName = brandName.replace("}_b", "");
			line = line.replace(brandName0,brandName); 	//System.out.println("Brand = "+brandName);
		}
		Pattern pProduct = Pattern.compile("\\{[a-zA-Z0-9 \\-+./#]+\\}_p");
		Matcher mProduct = pProduct.matcher(line);
		while(mProduct.find())
		{
			String productName0 = mProduct.group();					
			productName = productName0.replace("{", "");
			productName = productName.replace("}_p", "");
			line = line.replace(productName0, productName);//	System.out.println("Product = "+productName);
		}				
		Pattern pVersion = Pattern.compile("\\{[a-zA-Z0-9 \\-+./#]+\\}_v");
		Matcher mVersion = pVersion.matcher(line);
		while(mVersion.find())
		{
			String versionName0 = mVersion.group();					
			versionName = versionName0.replace("{", "");
			versionName = versionName.replace("}_v", "");
			line = line.replace(versionName0, versionName);//		System.out.println("Version = "+versionName);
		}				
	
		String titleLine = null, titleExtLine = null, authorLine = null, isbnLine = null;
		String candidateWords = "candidateWords";
		ArrayList<String> candidateWordsVaules = new ArrayList<String>();
		
		//the words - & do not convey any temporal info. so remove them
		String[] FilteredWords = {"-","&","and","is","pack"};//,"white","black","pink","gray","red"};
		String[] FilteredUnits = { "gb","hour","pound","minute","mm","mp", "ounce" }; 
		
		//Filter title words from productName	
		String[] productNameTokens = line.split(";");
		if ( productNameTokens.length == 2 ) {
			titleLine =  productNameTokens[0];
		} else if ( productNameTokens.length == 3 ){
			titleLine = productNameTokens[0];
			authorLine = productNameTokens[1];
		} else if ( productNameTokens.length == 4 ){
			titleLine = productNameTokens[0];
			authorLine = productNameTokens[1];
			isbnLine = productNameTokens[2];			
		} else if ( productNameTokens.length == 5 ){
			titleLine = productNameTokens[0];
			titleExtLine = productNameTokens[1];
			authorLine = productNameTokens[2];
			isbnLine = productNameTokens[3];
		}
	
		//title and titleTxt words not in the manual tags "brand, product and version" are "na"					
		if(titleLine != null){
			titleLine = titleLine.trim();
			for(String tWord : titleLine.split("[ ]+")){
				//populate candidateWords
				tWord = tWord.replace("(", "");
				tWord = tWord.replace(")", "");
				tWord = tWord.replace(",", "");
				if(!tWord.isEmpty()){
					if(Arrays.asList(FilteredWords).contains(tWord.trim())|| 
							Arrays.asList(FilteredUnits).contains(tWord.trim())){
						continue;
					} else {
						candidateWordsVaules.add(tWord.trim());								
					}
				}//End if tWord non-empty
			}//End for
			
		}
		if(titleExtLine != null){
			titleExtLine = titleExtLine.trim();
			for(String tWord : titleExtLine.split("[ ]+")){
				//populate candidateWords
				tWord = tWord.replace("(", "");
				tWord = tWord.replace(")", "");
				tWord = tWord.replace(",", "");
				if(!tWord.isEmpty()){
					if(Arrays.asList(FilteredWords).contains(tWord.trim()) || 
							Arrays.asList(FilteredUnits).contains(tWord.trim())){
						continue;
					} else {
						candidateWordsVaules.add(tWord.trim());								
					}
				}
			}
		}
		
		//Update feature value map
		if(brandName != null){
			featureValueMap.put("brandName", brandName);	
		}
		if(productName != null){
			featureValueMap.put("productName", productName);
		}
	    if(versionName != null){
	    	featureValueMap.put("versionName", versionName);
	    }	
		if(titleLine != null){
			featureValueMap.put("titleLine", titleLine);
		}
		if(titleExtLine != null){
			featureValueMap.put("titleExtLine", titleExtLine);
		}	
		if(authorLine != null){
			featureValueMap.put("authorLine", authorLine);
		}
		if(isbnLine != null){				
			featureValueMap.put("isbnLine", isbnLine);
		}
		if(candidateWordsVaules.size() > 0){
			//System.out.println("DEBUG: "+candidateWordsVaules.size());
			featureValueListMap.put(candidateWords, candidateWordsVaules);	
		}
		
	}//End of getProductLabel
	//store the words in productDescription as a list in FV.
	public void getProductDescription(String line){
				
		ArrayList<String> pdWordsVaules = new ArrayList<String>();
			
		//the words - & do not convey any temporal info. so remove them
		String[] FilteredWords = {"-","&","for","and","is","with","pack"};//,"white","black","pink","gray","red"};
		String[] FilteredUnits = { "gb","hour","pound","minute","mm","mp" };		
		//Filter title words from productDescription
		line = line.replace("productDescription:", "");
		String[] productDescriptionWords = line.split("[ ]+");		
		int wordIndex = 0, EndIndex =0;
		
		if(productDescriptionWords.length>0){			
			//productDescription could be lengthy. So store words that occur in title first.
			ArrayList<String> labelWords = featureValueListMap.get("candidateWords");
			for(String labelWord : labelWords){
				if(line.contains(labelWord)){
					pdWordsVaules.add(labelWord);				//System.out.println(labelWord);
					line = line.replace(labelWord,"");
				}
			}
			
			//Then store till reaching 100 words Count. //Assumption : limit of 100			
			String tWord = productDescriptionWords[wordIndex];
			if(productDescriptionWords.length <= 100){
				EndIndex = productDescriptionWords.length - 1;
			} else {
				EndIndex = 100;
			}
					
			while(tWord != null){				
				//populate candidateWords
				tWord = tWord.replace("(", "");
				tWord = tWord.replace(")", "");
				tWord = tWord.replace(",", "");
				if(Arrays.asList(FilteredWords).contains(tWord.trim())|| 
						Arrays.asList(FilteredUnits).contains(tWord.trim())){
					//do not do anything.
				} else {
					pdWordsVaules.add(tWord.trim());
				}
			
				if(wordIndex < EndIndex){
					++wordIndex;
					tWord = productDescriptionWords[wordIndex];					
				} else {
					tWord = null;
				}
			}//End while
			
		}//End if productDescriptionWords.length>0
		
		if(pdWordsVaules.size() > 0){
			//System.out.println("DEBUG: "+candidateWordsVaules.size());
			featureValueListMap.put("productDescriptionWords", pdWordsVaules);	
		}
		
	}//End of getProductDescription		
	public void getUrl(String line){
		line = line.replace("url:", "");
		ArrayList<String> urlWordsVaules = new ArrayList<String>();
		Pattern pUrl = Pattern.compile("http://www.amazon.com/[a-zA-Z0-9-]+/dp/");
		Matcher mUrl = pUrl.matcher(line);
		while(mUrl.find())
		{
			String urlName = mUrl.group();
			urlName = urlName.replace("http://www.amazon.com/", "");
			urlName = urlName.replace("/dp/", "");
			for(String urlWord : urlName.split("-")){ //System.out.println(urlWord);
				urlWordsVaules.add(urlWord.toLowerCase().trim());
			}			
		}
		if(urlWordsVaules.size()>0){
			featureValueListMap.put("urlDescriptionWords", urlWordsVaules);			
		}
	}//End getUrl
	public void getReviewTitle(String line){		
		line = line.replace("rr.title:", "");
		ArrayList<String> rrTitleWordsVaules = new ArrayList<String>();
		if(featureValueListMap.get("revTitle") != null){
			rrTitleWordsVaules = featureValueListMap.get("revTitle"); 
		} 			
		for(String revWord : line.split(" ")){ //System.out.println(revWord);
			rrTitleWordsVaules.add(revWord.toLowerCase().trim());
		}					
		if(rrTitleWordsVaules.size()>0){
			featureValueListMap.put("revTitle", rrTitleWordsVaules);			
		}
	}//End getReviewTitle
	public void getWeight(String line){		
		String[] wtWords = line.split("[ ]+");
		if(wtWords.length == 2){
			String wtStr = wtWords[0];
			if(Double.valueOf(wtStr) > 0){ //System.out.println("**"+wtStr+"**");
					featureValueMap.put("weight", wtStr);
			}
		}	
	}//End getWt
	public void getCategoryHierarchies(String line){
		ArrayList<String> catWordsVaules = new ArrayList<String>();
		String catHierLine =line.trim();
		for(String tWord : catHierLine.split("[ ]+")){
			if(tWord.equals(">")||tWord.equals("&")||tWord.equals(",")){
				continue;
			} else {
				catWordsVaules.add(tWord);	//System.out.println(tWord);
			}
		}
		if(catWordsVaules.size()>0){
			featureValueListMap.put("category", catWordsVaules);
		}
	}//End getCategoryHierarchies
	public void getnextBoughtTitle(String line){		
		ArrayList<String> nextBoughtWordsVaules = new ArrayList<String>();
		if(featureValueListMap.get("nextBoughtTitle") != null){
			nextBoughtWordsVaules = featureValueListMap.get("nextBoughtTitle"); 
		} 			
		for(String word : line.split(" ")){ 
			nextBoughtWordsVaules.add(word.toLowerCase().trim());
		}					
		if(nextBoughtWordsVaules.size()>0){
			featureValueListMap.put("nextBoughtTitle", nextBoughtWordsVaules);			
		}
	}//End nextBoughtTitle
	public void getnextBoughtBrand(String line){		
		ArrayList<String> nextBoughtBrandVaules = new ArrayList<String>();
		if(featureValueListMap.get("nextBoughtBrand") != null){
			nextBoughtBrandVaules = featureValueListMap.get("nextBoughtBrand"); 
		} 			
		for(String word : line.split(" ")){ 
			nextBoughtBrandVaules.add(word.toLowerCase().trim());
		}					
		if(nextBoughtBrandVaules.size()>0){
			featureValueListMap.put("nextBoughtBrand", nextBoughtBrandVaules);			
		}
	}//End nextBoughtBrand
	public void getfreqBoughtTitle(String line){		
		ArrayList<String> freqBoughtWordsVaules = new ArrayList<String>();
		if(featureValueListMap.get("freqBoughtTitle") != null){
			freqBoughtWordsVaules = featureValueListMap.get("freqBoughtTitle"); 
		} 			
		for(String word : line.split(" ")){ 
			freqBoughtWordsVaules.add(word.toLowerCase().trim());
		}					
		if(freqBoughtWordsVaules.size()>0){
			featureValueListMap.put("freqBoughtTitle", freqBoughtWordsVaules);			
		}
	}//End freqBoughtTitle
	public void getReviewDate(String line){		
		long rrDateValue = 0;
		if(featureValueMap.get("rrDate") != null){
			rrDateValue = Integer.valueOf(featureValueMap.get("rrDate"));			
		}
		String rrDateLine = line.replace("rr.date:", " "); //remove rr.date: token					
		rrDateLine = rrDateLine.trim();	//System.out.println("rrDateLine = "+rrDateLine);					
		SimpleDateFormat format = new SimpleDateFormat("MMM dd,yyyy");					 
		Date d0 = null, d1 = null;						 
		try {
			d0 = format.parse("january 01, 1994");//Amazon was started in 1994. So earliest review can be in 1994, not before.
			d1 = format.parse(rrDateLine);		
			  Calendar calendar0 = Calendar.getInstance();  
			  Calendar calendar1 = Calendar.getInstance();
		        calendar0.setTime(d0);  
		        calendar1.setTime(d1);		       
			int diffYear =  calendar1.get(Calendar.YEAR) -  calendar0.get(Calendar.YEAR) ;
			--diffYear;//to account for current year
			int diff = diffYear * 365 + calendar1.get(Calendar.DAY_OF_YEAR);	//System.out.print(diff + " days, ");
//Assumption: rrDate is used to assess product age. So date with lower diff to jan 01, 1994 shows higher age.
//Thus rrDate indicates earliest date this product was reviewed.
			if(rrDateValue > diff || rrDateValue == 0){
				rrDateValue = diff;
			}			 
		} catch (Exception e) {
			e.printStackTrace();
		}	
		if(rrDateValue >= 0){ 		//System.out.println("**"+rrDateValue+"**");
			featureValueMap.put("rrDate", String.valueOf(rrDateValue));			
		}	
	}//End ReviewDate
		
	public void createContextFV(){		
		String pos1 = null, pos2 = null, pos3 = null, pos4 = null, pos5 = null, poslast = null, numPos  = null;				
		ArrayList<String> titleWordsVaules = new ArrayList<String>();

		String titleLine = unlabelledTitle.replace(";", " ");
		String[] productNameTokens = titleLine.trim().split("[ ]+");
		//Extract position features
		//Feature 1. Word at position 1
		if(productNameTokens.length == 1){
			pos1 = productNameTokens[0];
		} else if(productNameTokens.length == 2){
			pos1 = productNameTokens[0];
			pos2 = productNameTokens[1];
		} else if(productNameTokens.length == 3){
			pos1 = productNameTokens[0];
			pos2 = productNameTokens[1];
			pos3 = productNameTokens[2];
		} else if(productNameTokens.length == 4){
			pos1 = productNameTokens[0];
			pos2 = productNameTokens[1];
			pos3 = productNameTokens[2];
			pos4 = productNameTokens[3];
		} else if(productNameTokens.length >= 5){
			pos1 = productNameTokens[0];
			pos2 = productNameTokens[1];
			pos3 = productNameTokens[2];
			pos4 = productNameTokens[3];
			pos5 = productNameTokens[4];
		}	//System.out.println("pos1 = "+pos1+" , pos2 = "+pos2+" , pos3 = "+pos3+" , pos4 = "+pos4+" , pos5 = "+pos5);
		poslast = productNameTokens[productNameTokens.length -1];
		numPos = String.valueOf(productNameTokens.length);
		for(String tWord : productNameTokens){
			titleWordsVaules.add(tWord);		
		}//End for tWord
		//Update feature value map
		if(pos1 != null){
			featureValueMap.put("position1", pos1);
			//System.out.println("added pos1");
		}
		if(pos2 != null){
			featureValueMap.put("position2", pos2);
			//System.out.println("added pos2");
		}
		if(pos3 != null){
			featureValueMap.put("position3", pos3);
			//System.out.println("added pos3");
		}
		if(pos4 != null){
			featureValueMap.put("position4", pos4);
		}
		if(pos5 != null){
			featureValueMap.put("position5", pos5);
		}
		if(poslast != null){
			featureValueMap.put("positionLast", poslast);
		}
		if(numPos != null){
			featureValueMap.put("numPositions", numPos);
		}
		if(titleWordsVaules.size() > 0){
			featureValueListMap.put("titleWords", titleWordsVaules);
		}
		
	}//End createContextFV
	public void createLinguisticFV(){
		//Extract POS tagged titleLine 
		String titleLine = unlabelledTitle.replace(";", " ");
		MaxentTagger tagger = new MaxentTagger("/home/priya/Downloads/lib/stanford-postagger-2013-04-04/models/english-left3words-distsim.tagger");//local machines
		String titleTagged = tagger.tagString(titleLine); 
		if(titleTagged != null){
			featureValueMap.put("titleTagged", titleTagged); //System.out.println("added titleTagged");
		}	
	}//End createLinguisticFV	
	
	public StringBuilder getUnigramTimeVariantFV(String featureName, String cWord, boolean date){
		StringBuilder outputStr = new StringBuilder();
		if(date){//Date feature
		
			//Date - in 2011 to 2013
			if(featureValueMap.get(featureName) != null){
				if(Integer.parseInt(featureValueMap.get(featureName)) > 10 ){
					outputStr.append(" ONE");
				} else {
					outputStr.append(" ZERO");
				}			
			} else {
				outputStr.append(" ZERO");
			}					

			//Date - in 2000 to 2010
			if(featureValueMap.get(featureName) != null){
				if(Integer.parseInt(featureValueMap.get(featureName)) <= 10 ){
				outputStr.append(" ONE");
				} else {
					outputStr.append(" ZERO");
				}			
			} else {
				outputStr.append(" ZERO");
			}			
			
		}else {//found in FVmap
			if(featureValueMap.get(featureName) != null){
				if(Arrays.asList(featureValueMap.get(featureName).split("[ ]+")).contains(cWord)){
					outputStr.append(" ONE");							
				}else {
					outputStr.append(" ZERO");
				}						
			} else {
				outputStr.append(" ZERO");
			}
		}
		return outputStr;
	}//End getUnigramTimeVariantFV
		
	public StringBuilder getUnigramTimeInvariantFV(String cWord){
		StringBuilder outputStr = new StringBuilder();
		//Attribute Value features_start
		//feature 1 = author
		if(featureValueMap.get("authorLine") != null){
			if(Arrays.asList(featureValueMap.get("authorLine").split("[ ]+")).contains(cWord)){
				outputStr.append(" ONE");							
			}else {
				outputStr.append(" ZERO");
			}						
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 2. isbn
		if(featureValueMap.get("isbnLine") != null){
			if(featureValueMap.get("isbnLine").contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			} 
		} else {
			outputStr.append(" ZERO");
		}
		

		//feature 3.publisher
		if(featureValueMap.get("publisherLine") != null){
			if(Arrays.asList(featureValueMap.get("publisherLine").split("[ ]+")).contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			} 
		} else {
			outputStr.append(" ZERO");
		}
		

		//feature 4. language
		if(featureValueMap.get("langLine") != null){
			if(featureValueMap.get("langLine").contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			} 
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 5.catHier
		if(featureValueListMap.get("catWords") != null){
			if(featureValueListMap.get("catWords").contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}							
		}
		
		//feature 6. model num
		if(featureValueMap.get("modelNumLine") != null){
			if(featureValueMap.get("modelNumLine").contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		} else {
			outputStr.append(" ZERO");
		}
		//feature 7. shippingWeight
		if(featureValueMap.get("shippingWeightLine") != null){
			if(featureValueMap.get("shippingWeightLine").contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		} else {
			outputStr.append(" ZERO");
		}
		
		// feature 8.freqBoughtTitle
		if(featureValueMap.get("freqBoughtWords") != null){
			if(featureValueMap.get("freqBoughtWords").contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		}
		//feature 9. nextBoughtTitle
		if(featureValueListMap.get("nextBoughtTitleWords") != null){
			if(featureValueListMap.get("nextBoughtTitleWords").contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		}
		//feature 10. nextBoughtTitlePublisher
		if(featureValueMap.get("nextBoughtPublisherLine") != null){						
			if(Arrays.asList(featureValueMap.get("nextBoughtPublisherLine").split("[ ]+")).contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		} else {
			outputStr.append(" ZERO");
		}					
		//Attribute value features end
		
		//Context features_start
		//feature 1 = position 1
		if(featureValueMap.get("position1") != null){
			if(featureValueMap.get("position1").contains(cWord)){
				outputStr.append(" ONE");							
			}else {
				outputStr.append(" ZERO");
			}						
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 2 = position 2
		if(featureValueMap.get("position2") != null){
			if(featureValueMap.get("position2").contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			} 
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 3 .position 3
		if(featureValueMap.get("position3") != null){
			if(featureValueMap.get("position3").contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 4 - position 4
		if(featureValueMap.get("position4") != null){
			if(featureValueMap.get("position4").contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			} 
		} else {
			outputStr.append(" ZERO");
		}

		//feature 5 - position 5
		if(featureValueMap.get("position5") != null){
			if(featureValueMap.get("position5").contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			} 
		} else {
			outputStr.append(" ZERO");
		}

		//feature 6 - position Last
		if(featureValueMap.get("positionLast") != null){
			if(featureValueMap.get("positionLast").contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			} 
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 7.number of words in title is greater than 5 
		if(featureValueMap.get("numPositions") != null){		
			if(Integer.valueOf(featureValueMap.get("numPositions")) > 5){			
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 8. Is numeric
		if(cWord.matches("-?\\d+(\\.\\d+)?")){
			outputStr.append(" ONE");	
		} else {
			outputStr.append(" ZERO");
		}

		//feature 9. Contains number
		if(cWord.matches("(.*)\\d+(.*)")){
			outputStr.append(" ONE");	
		} else {
			outputStr.append(" ZERO");
		}
		
		//Feature 10. word is enclosed in parathesis
		if(featureValueListMap.get("titleWords") != null){
			boolean parenthesisFound = false;
			for(String tWord : featureValueListMap.get("titleWords")){
				if(tWord.contains(cWord)){
					if(tWord.startsWith("(") || tWord.startsWith(")")){
					 	parenthesisFound = true;
					}
				}
			}
			if(parenthesisFound){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			} 
		} else {
			outputStr.append(" ZERO");
		}

		//Feature 11. prev word is "for"
		if(featureValueListMap.get("titleWords") != null){
			boolean forFound = false;
			ArrayList<String> titSplits = featureValueListMap.get("titleWords");
			for(String tWord : titSplits){
				if(tWord.contains(cWord)){
					int prev_index = titSplits.indexOf(tWord) - 1;
					if(prev_index >= 0){
						if(titSplits.get(prev_index).equalsIgnoreCase("for")){
							forFound = true;
						}
					}
				}
			}
			if(forFound){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			} 
		} else {
			outputStr.append(" ZERO");
		}
		
		//Feature 12. prev word is "with"
		if(featureValueListMap.get("titleWords") != null){
			boolean withFound = false;
			ArrayList<String> titSplits = featureValueListMap.get("titleWords");
			for(String tWord : titSplits){
				if(tWord.contains(cWord)){
					int prev_index = titSplits.indexOf(tWord) - 1;
					if(prev_index > 0){
						if(titSplits.get(prev_index).equalsIgnoreCase("with")){
							withFound = true;
						}
					}
				}
			}
			if(withFound){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			} 
		} else {
			outputStr.append(" ZERO");
		}
		
		//Feature 13. is Color in dictionary
		if(Arrays.asList(colorList).contains(cWord)){
			outputStr.append(" ONE");							
		} else {
			outputStr.append(" ZERO");
		}  
		
		//Feature 14. is Metric in dictionary
		if(Arrays.asList(metricList).contains(cWord)){
			outputStr.append(" ONE");							
		} else {
			outputStr.append(" ZERO");
		}  
		//Context features_end
		
		
		//Linguistic features_start
		if(featureValueMap.get("titleTagged") != null){
    		  String titleTag = featureValueMap.get("titleTagged");		      
		      String[] taggedWords = titleTag.split(" ");		   
		      
		      lingFeatLoop:
		      for (String tw : taggedWords) {
		 
		    	  String[] temp = tw.split("_");
		          if(temp.length == 2){
			    	  if(!temp[0].equalsIgnoreCase(cWord)){
			    		  continue;
			    	  } else {
			    		  	//Feature 1. NNP
			        	  	if(temp[1].equalsIgnoreCase("NNP")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			    		  	//Feature 2. NN
			        	  	if(temp[1].equalsIgnoreCase("NN")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			    		  	//Feature 3. NNS
			        	  	if(temp[1].equalsIgnoreCase("NNS")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			    		  	//Feature 3. NNPS
			        	  	if(temp[1].equalsIgnoreCase("NNPS")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
		        	  	  	//Feature 4. MD
			        	  	if(temp[1].equalsIgnoreCase("MD")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}			        	  
		        	  	  	//Feature 5. VB
			        	  	if(temp[1].equalsIgnoreCase("VB")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			        	    //Feature 6. VBP
			        	  	if(temp[1].equalsIgnoreCase("VBP")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			        	  	//Feature 7. VBD
			        	  	if(temp[1].equalsIgnoreCase("VBD")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			        	  	//Feature 8. VBN
			        	  	if(temp[1].equalsIgnoreCase("VBN")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			        	  	//Feature 9. VBG
			        	  	if(temp[1].equalsIgnoreCase("VBG")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			        	  	//Feature 10. VBZ
			        	  	if(temp[1].equalsIgnoreCase("VBZ")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			        	    //Feature 11. JJ
			        	  	if(temp[1].equalsIgnoreCase("JJ")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			    		  	//Feature 12. JJS
			        	  	if(temp[1].equalsIgnoreCase("JJS")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
		        	  	  	//Feature 13. RB
			        	  	if(temp[1].equalsIgnoreCase("RB")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}	
			        	    //Feature 14. IN
			        	  	if(temp[1].equalsIgnoreCase("IN")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}	
		        	  	  	//Feature 15. DT
			        	  	if(temp[1].equalsIgnoreCase("DT")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}	
			        		//Feature 16. CD
			        	  	if(temp[1].equalsIgnoreCase("CD")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			        	  	//Feature 17. CC
			        	  	if(temp[1].equalsIgnoreCase("CC")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
  			        	    //Feature 18. LS
			        	  	if(temp[1].equalsIgnoreCase("LS")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			        	  	//Feature 19. FW	
			        	  	if(temp[1].equalsIgnoreCase("FW")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			        	  //Feature 20. SYM	
			        	  	if(temp[1].equalsIgnoreCase("SYM")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			        	  //Feature 21. PRP	
			        	  	if(temp[1].equalsIgnoreCase("PRP")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			        	  	//Feature 22. PR$	
			        	  	if(temp[1].equalsIgnoreCase("PR$")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}			        	  
				      break lingFeatLoop;//break out of cWord for loop on fetching FV
			          //System.out.println();
			    	  }//End ifelse cWord
		          }//End if temp.length==2       
		      }//for taggedWord
		
		}//End Linguistic feat		
		
		return outputStr;
	}//End getUnigramTimeInvariantFV
	
	public StringBuilder getUnigramLinguisticFV(String cWord){
		StringBuilder outputStr = new StringBuilder();
		if(featureValueMap.get("titleTagged") != null){
			String titleTag = featureValueMap.get("titleTagged");
		      
		      String[] taggedWords = titleTag.split(" ");		   

		      lingFeatLoop:
		      for (String tw : taggedWords) {
		 
		    	  String[] temp = tw.split("_");
		          if(temp.length == 2){
			    	  if(!temp[0].equalsIgnoreCase(cWord)){
			    		  continue;
			    	  } else {
			    		  	//Feature 1. NNP
			        	  	if(temp[1].equalsIgnoreCase("NNP")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			    		  	//Feature 2. NN
			        	  	if(temp[1].equalsIgnoreCase("NN")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			    		  	//Feature 3. NNS
			        	  	if(temp[1].equalsIgnoreCase("NNS")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			    		  	//Feature 3. NNPS
			        	  	if(temp[1].equalsIgnoreCase("NNPS")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
		        	  	  	//Feature 4. MD
			        	  	if(temp[1].equalsIgnoreCase("MD")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}			        	  
		        	  	  	//Feature 5. VB
			        	  	if(temp[1].equalsIgnoreCase("VB")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			        	  //Feature 6. VBP
			        	  	if(temp[1].equalsIgnoreCase("VBP")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			        	  	//Feature 7. VBD
			        	  	if(temp[1].equalsIgnoreCase("VBD")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			        	  	//Feature 8. VBN
			        	  	if(temp[1].equalsIgnoreCase("VBN")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			        	  	//Feature 9. VBG
			        	  	if(temp[1].equalsIgnoreCase("VBG")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			        	  	//Feature 10. VBZ
			        	  	if(temp[1].equalsIgnoreCase("VBZ")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			        	  //Feature 11. JJ
			        	  	if(temp[1].equalsIgnoreCase("JJ")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			    		  	//Feature 12. JJS
			        	  	if(temp[1].equalsIgnoreCase("JJS")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
		        	  	  	//Feature 13. RB
			        	  	if(temp[1].equalsIgnoreCase("RB")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}	
		        	  	  	//Feature 14. MD
			        	  	if(temp[1].equalsIgnoreCase("MD")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}	
			        	  //Feature 15. IN
			        	  	if(temp[1].equalsIgnoreCase("IN")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}	
		        	  	  	//Feature 16. DT
			        	  	if(temp[1].equalsIgnoreCase("DT")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}	
			        		//Feature 17. CD
			        	  	if(temp[1].equalsIgnoreCase("CD")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			        	  	//Feature 18. CC
			        	  	if(temp[1].equalsIgnoreCase("CC")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
  			        	    //Feature 19. LS
			        	  	if(temp[1].equalsIgnoreCase("LS")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			        	  	//Feature 20. FW	
			        	  	if(temp[1].equalsIgnoreCase("FW")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			        	  //Feature 21. SYM	
			        	  	if(temp[1].equalsIgnoreCase("SYM")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			        	  //Feature 22. PRP	
			        	  	if(temp[1].equalsIgnoreCase("PRP")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}
			        	  //Feature 23. PR$	
			        	  	if(temp[1].equalsIgnoreCase("PR$")){
				        		  outputStr.append(" ONE");							
				  			}else {
				  				outputStr.append(" ZERO");
				  			}			        	  
				      break lingFeatLoop;
			          //System.out.println();
			    	  }//End ifelse cWord
		          }//End if temp.length==2       
		      }//for taggedWord
		
		}//End if taggedStr
		return outputStr;
	}//End getUnigramLinguistifFV
	
	public StringBuilder getUnigramContextFV(String cWord){
		StringBuilder outputStr = new StringBuilder();
				
		//feature 1 = position 1
		if(featureValueMap.get("position1") != null){
			if(featureValueMap.get("position1").contains(cWord)){
				outputStr.append(" ONE");							
			}else {
				outputStr.append(" ZERO");
			}						
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 2 = position 2
		if(featureValueMap.get("position2") != null){
			if(featureValueMap.get("position2").contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			} 
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 3 .position 3
		if(featureValueMap.get("position3") != null){
			if(featureValueMap.get("position3").contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 4 - position 4
		if(featureValueMap.get("position4") != null){
			if(featureValueMap.get("position4").contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			} 
		} else {
			outputStr.append(" ZERO");
		}

		//feature 5 - position 5
		if(featureValueMap.get("position5") != null){
			if(featureValueMap.get("position5").contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			} 
		} else {
			outputStr.append(" ZERO");
		}

		//feature 6 - position Last
		if(featureValueMap.get("positionLast") != null){
			if(featureValueMap.get("positionLast").contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			} 
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 7.number of words in title is greater than 5 
		if(featureValueMap.get("numPositions") != null){		
			if(Integer.valueOf(featureValueMap.get("numPositions")) > 5){			
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 8. Is numeric
		if(cWord.matches("-?\\d+(\\.\\d+)?")){
			outputStr.append(" ONE");	
		} else {
			outputStr.append(" ZERO");
		}

		//feature 9. Contains number
		if(cWord.matches("(.*)\\d+(.*)")){
			outputStr.append(" ONE");	
		} else {
			outputStr.append(" ZERO");
		}
		
		//Feature 10. word is enclosed in parathesis
		if(featureValueListMap.get("titleWords") != null){
			boolean parenthesisFound = false;
			for(String tWord : featureValueListMap.get("titleWords")){
				if(tWord.contains(cWord)){
					if(tWord.startsWith("(") || tWord.startsWith(")")){
					 	parenthesisFound = true;
					}
				}
			}
			if(parenthesisFound){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			} 
		} else {
			outputStr.append(" ZERO");
		}

		//Feature 11. prev word is "for"
		if(featureValueListMap.get("titleWords") != null){
			boolean forFound = false;
			ArrayList<String> titSplits = featureValueListMap.get("titleWords");
			for(String tWord : titSplits){
				if(tWord.contains(cWord)){
					int prev_index = titSplits.indexOf(tWord) - 1;
					if(prev_index >= 0){
						if(titSplits.get(prev_index).equalsIgnoreCase("for")){
							forFound = true;
						}
					}
				}
			}
			if(forFound){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			} 
		} else {
			outputStr.append(" ZERO");
		}
		
		//Feature 12. prev word is "with"
		if(featureValueListMap.get("titleWords") != null){
			boolean withFound = false;
			ArrayList<String> titSplits = featureValueListMap.get("titleWords");
			for(String tWord : titSplits){
				if(tWord.contains(cWord)){
					int prev_index = titSplits.indexOf(tWord) - 1;
					if(prev_index > 0){
						if(titSplits.get(prev_index).equalsIgnoreCase("with")){
							withFound = true;
						}
					}
				}
			}
			if(withFound){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			} 
		} else {
			outputStr.append(" ZERO");
		}
		
		//Feature 13. is Color in dictionary
		if(Arrays.asList(colorList).contains(cWord)){
			outputStr.append(" ONE");							
		} else {
			outputStr.append(" ZERO");
		}  
		
		//Feature 14. is Metric in dictionary
		if(Arrays.asList(metricList).contains(cWord)){
			outputStr.append(" ONE");							
		} else {
			outputStr.append(" ZERO");
		}  
			
			
		return outputStr;		
	}//End of getUnigramContextFV
	
	public StringBuilder getUnigramAttributeValueFV(String cWord){
		StringBuilder outputStr = new StringBuilder();
				
		//feature 1. productDescription
		if(featureValueListMap.get("productDescriptionWords") != null){
			if(featureValueListMap.get("productDescriptionWords").contains(cWord)){
				outputStr.append(" ONE");							
			}else {
				outputStr.append(" ZERO");
			}						
		} else {
			outputStr.append(" ZERO");
		}

		//feature 2. URL
		if(featureValueListMap.get("urlDescriptionWords") != null){
			if(featureValueListMap.get("urlDescriptionWords").contains(cWord)){
				outputStr.append(" ONE");							
			}else {
				outputStr.append(" ZERO");
			}						
		} else {
			outputStr.append(" ZERO");
		}

		//feature 3. revTitle
		if(featureValueListMap.get("revTitle") != null){
			if(featureValueListMap.get("revTitle").contains(cWord)){
				outputStr.append(" ONE");							
			}else {
				outputStr.append(" ZERO");
			}						
		} else {
			outputStr.append(" ZERO");
		}		
		
		//feature 4. weight
		if(featureValueMap.get("weight") != null){
			if(featureValueMap.get("weight").equalsIgnoreCase(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			} 
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 5. category
		if(featureValueListMap.get("category") != null){
			if(featureValueListMap.get("category").contains(cWord)){
				outputStr.append(" ONE");
			} else {
				outputStr.append(" ZERO");
			}			
		} else {
			outputStr.append(" ZERO");
		}	
		
		//feature 6. next Bought Title
		if(featureValueListMap.get("nextBoughtTitle") != null){
			if(featureValueListMap.get("nextBoughtTitle").contains(cWord)){
				outputStr.append(" ONE");
			} else {
				outputStr.append(" ZERO");
			}			
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 7. next Bought Brand
		if(featureValueListMap.get("nextBoughtBrand") != null){
			if(featureValueListMap.get("nextBoughtBrand").contains(cWord)){
				outputStr.append(" ONE");
			} else {
				outputStr.append(" ZERO");
			}			
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 8. freq Bought Title
		if(featureValueListMap.get("freqBoughtTitle") != null){
			if(featureValueListMap.get("freqBoughtTitle").contains(cWord)){
				outputStr.append(" ONE");
			} else {
				outputStr.append(" ZERO");
			}			
		} else {
			outputStr.append(" ZERO");
		}	
		
		//feature 9. rrDate
		if(featureValueMap.get("rrDate") != null){
			long rrDateValue = Integer.valueOf(featureValueMap.get("rrDate"));
			if(rrDateValue > 0){
				long pAge=0;
				Pattern pDate1 = Pattern.compile("19[0-9]{2}");//years starting 19**
				Matcher mDate1 = pDate1.matcher(cWord);
				while(mDate1.find())
				{
					String Date1Name = mDate1.group();
					long Date1Year = Integer.decode(Date1Name);
					if(Date1Year >= 1994){
						pAge = Date1Year - 1994;
					} else {
						pAge = 1994 - Date1Year;
					}
				}
				Pattern pDate2 = Pattern.compile("20[0-9]{2}");//years starting 20**
				Matcher mDate2 = pDate2.matcher(cWord);
				while(mDate2.find())
				{
					String Date2Name = mDate2.group();
					long Date2Year = Integer.decode(Date2Name);
					pAge = Date2Year - 1994;					
				}
				if(pAge > 0){
					if(rrDateValue >= pAge){ 
						if((rrDateValue - pAge) <=1){
							outputStr.append(" ONE");
						} else {
							outputStr.append(" ZERO");
						}						
					} else { //rrDateValue < pAge
						if((pAge - rrDateValue ) <=1){
							outputStr.append(" ONE");
						} else {
							outputStr.append(" ZERO");
						}						
					}
				}else {
					outputStr.append(" ZERO");
				}				
			}else {
				outputStr.append(" ZERO");
			}
		}else {
			outputStr.append(" ZERO");
		}	
		
		//feature 10. model
		if(featureValueMap.get("model") != null){
			if(featureValueMap.get("model").equalsIgnoreCase(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			} 
		} else {
			outputStr.append(" ZERO");
		}
		
		return outputStr;		
	}//End of getUnigramAttributeValueFV
	
	public void outputUnigramFV(String trainTest){
		
		//Output feature vectors
		if(DEBUG_FLAG){
			if(featureValueMap.size()>0){
				System.out.println("FV count = "+featureValueMap.size());				
			}
			if(featureValueListMap.size() > 0){
				System.out.println("FV list = "+featureValueListMap.size());
			}
		}//if debug
		
		if(featureValueListMap.get("candidateWords") != null){
		
				for(String cWord : featureValueListMap.get("candidateWords")){
					StringBuilder outputStr = new StringBuilder();
					StringBuilder labelsStr = new StringBuilder();
					StringBuilder evalStr = new StringBuilder();
					outputStr.append(cWord);
					labelsStr.append(cWord);
					evalStr.append(cWord);
					
					if(TIME_FLAG){
						
						outputStr.append(getUnigramTimeInvariantFV(cWord.trim()));						
						//String featureName = "editionLine"; //Allowed values "editionLine" and "newModelLine"
						//String featureName = "newModelLine";
						//outputStr.append(getUnigramTimeVariantFV(featureName, cWord,false));
						
						String featureMapName = "publishDateLine"; 
						//String featureMapName = "rrDateLine";
						//String featureMapName = "releaseDateWords";
						outputStr.append(getUnigramTimeVariantFV(featureMapName, cWord,true));
					
					} else {
						
						//add Attribute Value feature Vectors
						outputStr.append(getUnigramAttributeValueFV(cWord.trim()));	
						
						//add context feature Vectors
						outputStr.append(getUnigramContextFV(cWord.trim()));
						
						//add linguistic feature Vectors
						outputStr.append(getUnigramLinguisticFV(cWord.trim()));
					}
									
					//label - for TRAIN and LABEL only
					if(trainTest.contains("train") || trainTest.contains("label")){
						String label=null;
						if(featureValueMap.get("brandName") != null){						
							if(Arrays.asList(featureValueMap.get("brandName").split("[ ]+")).contains(cWord)){
								label = "brand";							
							}		
						} 
						if(featureValueMap.get("productName") != null){
							if(Arrays.asList(featureValueMap.get("productName").split("[ ]+")).contains(cWord)){
								label="product";							
							}
						} 
						if(featureValueMap.get("versionName") != null){
							if(Arrays.asList(featureValueMap.get("versionName").split("[ ]+")).contains(cWord)){
								label="version";						
							}
						} 
						if(label==null){
							outputStr.append(" na");
							labelsStr.append(" na");
						} else {
							outputStr.append(" "+label);
							labelsStr.append(" "+label);
						}
					}//End train label
					//print it!
					if(EVAL_FLAG){
						System.out.println(evalStr.toString());
					} else {
						if(trainTest.contains("train") || trainTest.contains("test")){
							System.out.println(outputStr.toString());
						} else if(trainTest.contains("label") ){
							System.out.println(labelsStr.toString());
						}
					}
				}//End for cWord
		
		}//End if output			
	
	}//End printout Unigram	
	
}//End class

