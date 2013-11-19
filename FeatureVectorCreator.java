package Versioning;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

//args[0] - train / test / label
//args[1] - cluster type / EVALUATION
//args[2] - product file name
//args[3] - debug flag / none
//args[4] - eval flag / none
//args[5] - time /none 
public class FeatureVectorCreator {
	
	static HashMap<String, String> featureValueMap = new HashMap<String, String>();
	static HashMap<String, ArrayList<String>> featureValueListMap = new HashMap<String, ArrayList<String>>();
	static String[] colorList = {"white","black","pink","gray","red","purple","yellow","cyan","blue","brown","orange","green","gold","ivory","magenta","maroon","silver","tan","violet"};
	static String[] metricList = { "gb","hour","pound","minute","mm","mp" }; 
	static boolean DEBUG_FLAG = false;//arg[3] is debug flag
	static boolean EVAL_FLAG = false;//arg[4] is evaluation flag
	static boolean TIME_FLAG = false;//arg[5] is time variance feature flag
	
	String inPath = "/home/priya/Desktop/EntityRanking";//for local machine
	//String inPath = "/home/iiit/priya/EntityRanking";//for abacus 
	
	//Attribute\&value features
	//1.title 2.titleExt 3.author 4.isbn 5.publDate 6.publisher 7.edition 8.language 9.catHier 10.rrDate 
	//11.modelNum 12.weight 13.newerModel 14.freqBoughtTitle 15.nextBoughtTitle 16.nextBoughtPublisher
	//17.release date
	
	public static void main(String[] args) {
		FeatureVectorCreator fvCreator = new FeatureVectorCreator();
		if(args[3].equalsIgnoreCase("debug")) fvCreator.DEBUG_FLAG = true;
		if(args[4].equalsIgnoreCase("eval")) fvCreator.EVAL_FLAG = true;
		if(args[5].equalsIgnoreCase("time")) fvCreator.TIME_FLAG = true;
		
		fvCreator.createAttributeValueFV( args[1], args[2]);//arg[2] is VIW Value tagged file
		fvCreator.createContextFV(args[1], args[2]);
		fvCreator.createLinguisticFV(args[1], args[2]);
		
		fvCreator.outputUnigramFV(args[0]);
		
		
	}//End main
	
	//creates featureValue and featureValueList Maps 
	public void createAttributeValueFV(String category, String filename){
		String read, taggedFile;

		if(DEBUG_FLAG){
			System.out.println(filename);
		}
		
		//category is i/p cluster type		
		if(category.contains("EVALUATION")){
			taggedFile = inPath+"/actualCrawl/"+filename;
		} else {
			taggedFile = inPath+"/dataset/"+category+"/"+filename;
		}
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(taggedFile));	
			int idx =0, newModelIdx = 0, catIdx =0, freqBoughtIdx=0, nextBoughtTitleIdx=0;
			String brandName = null, productName=null, versionName=null, titleLine = null, titleExtLine = null, authorLine = null, isbnLine = null,
					publDateLine = null, catHierLine = null, rrDateLine = null, publisherLine = null, editionLine = null, langLine = null, 
					shippingWeightLine=null,newModelLine = null, freqBoughtLine = null, modelNumLine=null, releaseDateLine=null, 
					nextBoughtTitleLine=null, nextBoughtPublisherLine=null;
			
			String candidateWords = "candidateWords";
			String candidateWordPairs = "candidateWordPairs";
			String publishDateWords = "publishDateWords";
			String rrDateWords = "rrDateWords";
			String releaseDateWords = "releaseDateWords";
			String catWords = "catWords";
			String freqBoughtWords = "freqBoughtWords";
			String nextBoughtTitleWords = "nextBoughtTitleWords";
			
			ArrayList<String> candidateWordsVaules = new ArrayList<String>();
			ArrayList<String> candidateWordPairsValues = new ArrayList<String>();
			int publishDateValue = 0;
			long rrDateValue = 0;
			ArrayList<String> releaseDateWordsValues = new ArrayList<String>();
			ArrayList<String> catWordsValues = new ArrayList<String>();
			ArrayList<String> freqBoughtWordsValues = new ArrayList<String>();
			ArrayList<String> nextBoughtTitleWordsValues = new ArrayList<String>();
			boolean nextBoughtFlag = false;
			
			//Preprcessing
			//the words - & do not convey any temporal info. so remove them
			String[] FilteredWords = {"-","&","for","and","is","with","pack"};//,"white","black","pink","gray","red"};
			String[] FilteredUnits = { "gb","hour","pound","minute","mm","mp" }; 
			
			while((read = br.readLine())!=null)
			{
				String line = read.trim();				
				++idx;
				
				//Extract the manual tags "brand, product and version" 
				//Pattern pBrand = Pattern.compile("\\{[a-zA-Z0-9 -]+\\}_brand");
				Pattern pBrand = Pattern.compile("\\{[a-zA-Z0-9 \\-+&]+\\}_b");
				Matcher mBrand = pBrand.matcher(line);
				while(mBrand.find())
				{
					String brandName0 = mBrand.group();
					brandName = brandName0.replace("{", "");
					//brandName = brandName.replace("}_brand", "");
					brandName = brandName.replace("}_b", "");
					line = line.replace(brandName0,brandName);
					//System.out.println("Brand = "+brandName);
				}
				//Pattern pProduct = Pattern.compile("\\{[a-zA-Z0-9 -]+\\}_product");
				Pattern pProduct = Pattern.compile("\\{[a-zA-Z0-9 \\-+./#]+\\}_p");
				Matcher mProduct = pProduct.matcher(line);
				while(mProduct.find())
				{
					String productName0 = mProduct.group();					
					productName = productName0.replace("{", "");
					//productName = productName.replace("}_product", "");
					productName = productName.replace("}_p", "");
					line = line.replace(productName0, productName);
					//System.out.println("Product = "+productName);
				}				
				//Pattern pVersion = Pattern.compile("\\{[a-zA-Z0-9 -]+\\}_version");
				Pattern pVersion = Pattern.compile("\\{[a-zA-Z0-9 \\-+./#]+\\}_v");
				Matcher mVersion = pVersion.matcher(line);
				while(mVersion.find())
				{
					String versionName0 = mVersion.group();					
					versionName = versionName0.replace("{", "");
					//versionName = versionName.replace("}_version", "");
					versionName = versionName.replace("}_v", "");
					line = line.replace(versionName0, versionName);
					//System.out.println("Version = "+versionName);
				}				
				
				//Filter title words from productName
				if(line.contains("productName:")){
					line = line.replace("productName:", "");
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
						String prevWord = null;
						for(String tWord : titleLine.split("[ ]+")){
							//populate candidateWordPairs
							if(prevWord == null){
								prevWord = tWord;
							} else {
								candidateWordPairsValues.add(prevWord+" "+tWord);
								prevWord = tWord;
							}
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
						String prevWord = null;
						for(String tWord : titleExtLine.split("[ ]+")){
							//populate candidateWordPairs
							if(prevWord == null){
								prevWord = tWord;
							} else {
								candidateWordPairsValues.add(prevWord+" "+tWord);
								prevWord = tWord;
							}
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
				}//End if productName
				
				//Extract publisher				
				if(line.contains("publisher;")){
					
					line = line.replace("publisher;", " "); //remove publisher; token
					Pattern p1 = Pattern.compile("\\([0-9a-zA-Z, ]+\\)");
					Matcher m1 = p1.matcher(line);
					if(m1.find())
					{
						publDateLine = m1.group();
						line = line.replace(publDateLine, " ");
						publDateLine = publDateLine.trim();
						
						SimpleDateFormat format = new SimpleDateFormat("MMM dd,yyyy");					 
						Date d1 = null;
						Date d2 = null;
				 
						try {
							d1 = format.parse("january 01, 2000");
							d2 = format.parse(publDateLine);
							publishDateValue = d2.getYear() - d1.getYear();
						} catch (Exception e) {
							e.printStackTrace();
						}						
						
					}					
			
					if(line.contains(";")){
						String[] publisherTokens = line.split(";");
						if ( publisherTokens.length == 2){
							publisherLine = publisherTokens[0];
							editionLine = publisherTokens[1];
							//System.out.println("Found edition "+publisherLine);
						} else {
							publisherLine = line.trim();
						}
					}
				}//End if publisher
				
				//Extract lang				
				if(line.contains("language;")){
					line = line.replace("language;", " "); //remove language; token					
					Pattern p2 = Pattern.compile("[a-zA-Z]+");
					Matcher m2 = p2.matcher(line);
					if(m2.find())
					{
						langLine = m2.group();
						line = line.replace(langLine, " ");
						langLine = langLine.trim();
					}					
				}//End if lang	
				
				//Filter categoryHierarchies:				
				if(line.contains("categoryHierarchies:"))
					catIdx = idx + 1;
				if (idx == catIdx){
						if (!line.isEmpty()){
							catHierLine =line.trim();
							catIdx += 1;
							for(String tWord : catHierLine.split("[ ]+")){
								if(tWord.equals(">")||tWord.equals("&")||tWord.equals(",")){
									continue;
								} else {
									catWordsValues.add(tWord);
									//System.out.println(tWord);
								}
							}
						} else {
							catIdx = 0;
						}
				}//End catHier		
				
				//Filter review date				
				if(line.contains("rr.date:")){
					rrDateLine = line.replace("rr.date:", " "); //remove rr.date: token					
					rrDateLine = rrDateLine.trim();
					//System.out.println("rrDateLine = "+rrDateLine);
					
					SimpleDateFormat format = new SimpleDateFormat("MMM dd,yyyy");					 
					Date d1 = null;
					Date d2 = null;
			 
					try {
						d1 = format.parse("january 01, 2000");
						d2 = format.parse(rrDateLine);
			 
						//in milliseconds
						long diff = d2.getYear() - d1.getYear();
									 
						//System.out.print(diff + " years, ");
//Assumption: rrDate is used to assess product age. So date with lower diff to jan 01, 2000 shows higher age.
//Thus rrDate indicates earliest date this product was reviewed.
						if(rrDateValue > diff || rrDateValue == 0){
							rrDateValue = diff;
						}
			 
					} catch (Exception e) {
						e.printStackTrace();
					}
						
				}//End Extract rr.date: 

				//Extract model num				
				if(line.contains("item model number;")){
					line = line.replace("item model number;", " "); //remove item model number;					
					Pattern p4 = Pattern.compile("[a-z0-9-]+");
					Matcher m4 = p4.matcher(line);
					if(m4.find())
					{
						modelNumLine = m4.group();
						//line = line.replace(langLine, " ");
						modelNumLine = modelNumLine.trim();						
					}					
				}//End if model

				//Extract weight				
				if(line.contains("shipping weight;")){
					line = line.replace("shipping weight;", " "); //remove shipping weight;					
					Pattern p5 = Pattern.compile("[0-9.]+[ ]+[pounds]?[ounces]?");
					Matcher m5 = p5.matcher(line);
					if(m5.find())
					{
						shippingWeightLine = m5.group();
		//Assumption : We are storing only the weight number
						shippingWeightLine = shippingWeightLine.replace("pounds","");
						shippingWeightLine = shippingWeightLine.replace("ounces","");
						shippingWeightLine = shippingWeightLine.trim();
					}					
				}//End if wt			
				
				//Extract  newer model:
				if(line.contains("newerModel:"))
					newModelIdx = idx + 2;
				if (idx == newModelIdx){
						if (!line.isEmpty()){
							newModelLine = line.trim();
							newModelLine = newModelLine.replace("#", "");
							//System.out.println(newModelLine);

							newModelIdx =0;
						} else {
							newModelIdx =0;
						}
				}//End if newer model
				
				//Extract freqBoughtTogether
				if(line.contains("freqBoughtTogether:"))
					freqBoughtIdx = idx + 2;
				if(idx == freqBoughtIdx){
					if(!line.isEmpty()){
						if(line.trim().startsWith("$")){
							freqBoughtIdx += 1;
						} else {
							freqBoughtLine = line.trim();
							freqBoughtIdx += 1;							
							for(String tWord : freqBoughtLine.split("[ ]+")){
								freqBoughtWordsValues.add(tWord.trim());
								//System.out.println(tWord);								
							}							
						}
					} else {
						freqBoughtIdx = 0;
					}
					
				}//End if freqBoughtTogether
				//System.out.println("Freq bought = "+freqBoughtLine);
				
				//Extract otherItemsBoughtAfterViewingThisItem:
				if(line.contains("otherItemsBoughtAfterViewingThisItem:"))
					nextBoughtTitleIdx = idx + 2;
				if(idx == nextBoughtTitleIdx){
					if(!line.isEmpty() & !line.contains("explore similar items")){//
						if(line.trim().startsWith("$")){
							nextBoughtTitleIdx += 1;
						} else  if(line.contains("out of 5 stars")){ 
							nextBoughtTitleIdx += 2;
						} else {
							if(nextBoughtFlag==false){
								nextBoughtTitleLine = line.trim();
								nextBoughtTitleIdx += 1;	
								nextBoughtFlag = true;
								for(String tWord : nextBoughtTitleLine.split("[ ]+")){
									nextBoughtTitleWordsValues.add(tWord.trim());
									//System.out.println(tWord);								
								}							
							} else {
								line = line.trim();
								if(line.contains("by"))
									line = line.replace("by", "");
								if(line.contains("paperback"))
									line = line.replace("paperback", "");
								if(line.contains("hardcover"))
									line = line.replace("hardcover", "");
								nextBoughtPublisherLine = line;
								nextBoughtTitleIdx += 2;
								nextBoughtFlag = false;
							}
						}
					} else {
						nextBoughtTitleIdx = 0;
					}
				}//End if nextBought
				
				//Extract release date;
				if(line.contains("release date:")){
					line = line.replace("release date:", " "); //remove rr.date: token					
					Pattern p6 = Pattern.compile("\\([0-9a-zA-Z, ]+\\)");
					Matcher m6 = p6.matcher(line);
					if(m6.find())
					{
						releaseDateLine = m6.group();
						//line = line.replace(rrDateLine, " ");
						releaseDateLine = releaseDateLine.trim();
						for(String tWord : releaseDateLine.split("[ ]+")){
							if(tWord.equals("(")|| tWord.equals(",") || tWord.equals(")")){
								continue;								
							} else {
								releaseDateWordsValues.add(tWord);
								//System.out.println(tWord);
							}
						}
					}
				}//End Extract release date:		
				
			}//End while
			
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
			if(candidateWordPairsValues.size() > 0){
				featureValueListMap.put(candidateWordPairs, candidateWordPairsValues);
			}
			if(publDateLine != null){
				featureValueMap.put("publDateLine", String.valueOf(publishDateValue));
			}					

			if(publisherLine != null){
							featureValueMap.put("publisherLine",publisherLine.trim());
			}
			if(editionLine != null){
							featureValueMap.put("editionLine", editionLine.trim());
			}
			if(langLine != null){
				featureValueMap.put("langLine", langLine);
			}					
			if(catHierLine != null){
				featureValueMap.put("catHierLine", catHierLine);
				if(catWordsValues.size()>0){
					featureValueListMap.put(catWords, catWordsValues);
				}
			}
			if(rrDateLine != null & rrDateValue > 0){
				featureValueMap.put("rrDateLine", String.valueOf(rrDateValue));									
			}//End Extract rr.date: 
			
			if(modelNumLine != null){
				featureValueMap.put("modelNumLine", modelNumLine);
			}					
			if(shippingWeightLine != null){
				featureValueMap.put("shippingWeightLine", shippingWeightLine);
			}
			if(newModelLine != null){
				featureValueMap.put("newModelLine", newModelLine);
			}
			if(nextBoughtTitleLine != null){
				featureValueMap.put("nextBoughtTitleLine", nextBoughtTitleLine);
				if(nextBoughtTitleWordsValues.size()>0){
					featureValueListMap.put(nextBoughtTitleWords, nextBoughtTitleWordsValues);
				}
			}
			if(releaseDateLine!=null){
				featureValueMap.put(releaseDateLine, releaseDateLine);
				if(releaseDateWordsValues.size()>0){
					featureValueListMap.put(releaseDateWords, releaseDateWordsValues);
				}
			}
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Unable to open productItem File "+taggedFile);
			e.printStackTrace();
		}		
		
	}//End createAttributeValueFV
	
	public void createContextFV(String category, String filename){
		String read, taggedFile;
		
		//category is i/p cluster type		
		if(category.contains("EVALUATION")){
			taggedFile = inPath+"/actualCrawl/"+filename;
		} else {
			taggedFile = inPath+"/dataset/"+category+"/"+filename;
		}
		

		try {
			BufferedReader br = new BufferedReader(new FileReader(taggedFile));
			
			int idx =0;
			String pos1 = null, pos2 = null, pos3 = null, pos4 = null, pos5 = null, poslast = null, numPos  = null,
					brandName = null, productName=null, versionName=null;
			
			String candidateWords = "candidateWords";
			String candidateWordPairs = "candidateWordPairs";

			
			ArrayList<String> titleWordsVaules = new ArrayList<String>();
			ArrayList<String> candidateWordsVaules = new ArrayList<String>();
			ArrayList<String> candidateWordPairsValues = new ArrayList<String>();
			boolean  foundTitleWords = false;
			
			//Preprcessing
			//the words - & do not convey any temporal info. so remove them
			String[] FilteredWords = {"-","&","for","and","is","with","pack"};//,"white","black","pink","gray","red"};
			String[] FilteredUnits = { "gb","hour","pound","minute","mm","mp" }; 
			
			while((read = br.readLine())!=null && foundTitleWords == false)
			{
				String line = read.trim();				
				++idx;
				
				//Extract the manual tags "brand, product and version" 
				Pattern pBrand = Pattern.compile("\\{[a-zA-Z0-9 \\-+&]+\\}_b");
				Matcher mBrand = pBrand.matcher(line);
				while(mBrand.find())
				{
					String brandName0 = mBrand.group();
					brandName = brandName0.replace("{", "");
					brandName = brandName.replace("}_b", "");
					line = line.replace(brandName0,brandName);
				}

				Pattern pProduct = Pattern.compile("\\{[a-zA-Z0-9 \\-+./#]+\\}_p");
				Matcher mProduct = pProduct.matcher(line);
				while(mProduct.find())
				{
					String productName0 = mProduct.group();					
					productName = productName0.replace("{", "");
					productName = productName.replace("}_p", "");
					line = line.replace(productName0, productName);
				}				

				Pattern pVersion = Pattern.compile("\\{[a-zA-Z0-9 \\-+./#]+\\}_v");
				Matcher mVersion = pVersion.matcher(line);
				while(mVersion.find())
				{
					String versionName0 = mVersion.group();					
					versionName = versionName0.replace("{", "");
					versionName = versionName.replace("}_v", "");
					line = line.replace(versionName0, versionName);
				}				
				
				//Filter title words from productName
				if(line.contains("productName:")){
					line = line.replace("productName:", "");
					StringBuilder titleLine  = new StringBuilder();
					String[] tSplits = line.trim().split(";");
					for(int i=0; i < (tSplits.length -1); ++i){
						titleLine.append(tSplits[i]);  
						titleLine.append(" ");
					}
					String titleLine1 = titleLine.toString();
					//System.out.println(titleLine1);
					
					String[] productNameTokens = titleLine1.split("[ ]+");
					String prevWord = null;
										
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
					}
					
					//System.out.println("pos1 = "+pos1+" , pos2 = "+pos2+" , pos3 = "+pos3+" , pos4 = "+pos4+" , pos5 = "+pos5);
					
					poslast = productNameTokens[productNameTokens.length -1];
					numPos = String.valueOf(productNameTokens.length);
					foundTitleWords = true;
					
					for(String tWord : productNameTokens){	
							
						titleWordsVaules.add(tWord);						
						//populate candidateWordPairs
						if(prevWord == null){
							prevWord = tWord;
						} else {
							candidateWordPairsValues.add(prevWord+" "+tWord);
							prevWord = tWord;
						}
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
						}
					}//End for tWord
										
				}//End if productName
								
			}//End while
			
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
			if(candidateWordsVaules.size() > 0){
				//System.out.println("DEBUG: "+candidateWordsVaules.size());
				featureValueListMap.put(candidateWords, candidateWordsVaules);	
			}
			if(candidateWordPairsValues.size() > 0){
				featureValueListMap.put(candidateWordPairs, candidateWordPairsValues);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Unable to open productItem File "+taggedFile);
			e.printStackTrace();
		}		
		
	}//End createContextFV

	public void createLinguisticFV(String category, String filename){
		String read, taggedFile;
		
		//category is i/p cluster type		
		if(category.contains("EVALUATION")){
			taggedFile = inPath+"/actualCrawl/"+filename;
		} else {
			taggedFile = inPath+"/dataset/"+category+"/"+filename;
		}
		

		try {
			BufferedReader br = new BufferedReader(new FileReader(taggedFile));
			
			int idx =0;
			String titleTagged = null, brandName = null, productName=null, versionName=null;
			
			String candidateWords = "candidateWords";
			String candidateWordPairs = "candidateWordPairs";

			
			ArrayList<String> titleWordsVaules = new ArrayList<String>();
			ArrayList<String> candidateWordsVaules = new ArrayList<String>();
			ArrayList<String> candidateWordPairsValues = new ArrayList<String>();
			boolean  foundTitleWords = false;
			
			//Preprcessing
			//the words - & do not convey any temporal info. so remove them
			String[] FilteredWords = {"-","&","for","and","is","with","pack"};//,"white","black","pink","gray","red"};
			String[] FilteredUnits = { "gb","hour","pound","minute","mm","mp" }; 
			
			while((read = br.readLine())!=null && foundTitleWords == false)
			{
				String line = read.trim();				
				++idx;
				
				//Extract the manual tags "brand, product and version" 
				Pattern pBrand = Pattern.compile("\\{[a-zA-Z0-9 \\-+&]+\\}_b");
				Matcher mBrand = pBrand.matcher(line);
				while(mBrand.find())
				{
					String brandName0 = mBrand.group();
					brandName = brandName0.replace("{", "");
					brandName = brandName.replace("}_b", "");
					line = line.replace(brandName0,brandName);
				}

				Pattern pProduct = Pattern.compile("\\{[a-zA-Z0-9 \\-+./#]+\\}_p");
				Matcher mProduct = pProduct.matcher(line);
				while(mProduct.find())
				{
					String productName0 = mProduct.group();					
					productName = productName0.replace("{", "");
					productName = productName.replace("}_p", "");
					line = line.replace(productName0, productName);
				}				

				Pattern pVersion = Pattern.compile("\\{[a-zA-Z0-9 \\-+./#]+\\}_v");
				Matcher mVersion = pVersion.matcher(line);
				while(mVersion.find())
				{
					String versionName0 = mVersion.group();					
					versionName = versionName0.replace("{", "");
					versionName = versionName.replace("}_v", "");
					line = line.replace(versionName0, versionName);
				}				
				
				//Filter title words from productName
				if(line.contains("productName:")){
					line = line.replace("productName:", "");
					StringBuilder titleLine  = new StringBuilder();
					String[] tSplits = line.trim().split(";");
					for(int i=0; i < (tSplits.length -1); ++i){
						titleLine.append(tSplits[i]);  
						titleLine.append(" ");
					}
					String titleLine1 = titleLine.toString();
					//System.out.println(titleLine1);
					foundTitleWords = true;					
					//Extract POS tagged titleLine 
					MaxentTagger tagger = new MaxentTagger("/home/priya/Downloads/lib/stanford-postagger-2013-04-04/models/english-left3words-distsim.tagger");//local machines
					titleTagged = tagger.tagString(titleLine1); 
					
					String[] productNameTokens = titleLine1.split("[ ]+");
					String prevWord = null;
					
					for(String tWord : productNameTokens){	
							
						titleWordsVaules.add(tWord);						
						//populate candidateWordPairs
						if(prevWord == null){
							prevWord = tWord;
						} else {
							candidateWordPairsValues.add(prevWord+" "+tWord);
							prevWord = tWord;
						}
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
						}
					}//End for tWord
										
				}//End if productName
								
			}//End while
			
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
			if(titleTagged != null){
				featureValueMap.put("titleTagged", titleTagged);
				//System.out.println("added titleTagged");
			}	
			if(titleWordsVaules.size() > 0){
				featureValueListMap.put("titleWords", titleWordsVaules);
			}
			if(candidateWordsVaules.size() > 0){
				//System.out.println("DEBUG: "+candidateWordsVaules.size());
				featureValueListMap.put(candidateWords, candidateWordsVaules);	
			}
			if(candidateWordPairsValues.size() > 0){
				featureValueListMap.put(candidateWordPairs, candidateWordPairsValues);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Unable to open productItem File "+taggedFile);
			e.printStackTrace();
		}		
		
	}//End createLinguisticFV
	

	
	public StringBuilder getBigramAttributeValueFV(String cWord1, String cWord2){
		StringBuilder outputStr = new StringBuilder();
		
		//feature 3 = author
		if(featureValueMap.get("authorLine") != null){
			if(Arrays.asList(featureValueMap.get("authorLine").split("[ ]+")).contains(cWord1)  || 
				Arrays.asList(featureValueMap.get("authorLine").split("[ ]+")).contains(cWord2) ){
				outputStr.append(" ONE");							
			}else {
				outputStr.append(" ZERO");
			}						
		} else {
			outputStr.append(" ZERO");
		}//End author feature
		
		
		//feature 4. isbn
		if(featureValueMap.get("isbnLine") != null){
			if(featureValueMap.get("isbnLine").contains(cWord1) || featureValueMap.get("isbnLine").contains(cWord2)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			} 
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 5.publDate
		if(featureValueListMap.get("publishDateWords") != null){
			if(featureValueListMap.get("publishDateWords").contains(cWord1) || 
					featureValueListMap.get("publishDateWords").contains(cWord2)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 6.publisher
		if(featureValueMap.get("publisherLine") != null){
			String publisherLine = featureValueMap.get("publisherLine") ; 
			if(Arrays.asList(publisherLine.split("[ ]+")).contains(cWord1) ||
					Arrays.asList(publisherLine.split("[ ]+")).contains(cWord2)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			} 
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 7.edition
		if(featureValueMap.get("editionLine") != null){
			String editionLine = featureValueMap.get("editionLine");
			if(Arrays.asList(editionLine.split("[ ]+")).contains(cWord1) ||
					Arrays.asList(editionLine.split("[ ]+")).contains(cWord2)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 8. language
		if(featureValueMap.get("langLine") != null){
			String langLine = featureValueMap.get("langLine");
			if(langLine.contains(cWord1) || langLine.contains(cWord2)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			} 
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 9.catHier
		if(featureValueListMap.get("catWords").contains(cWord1) || 
				featureValueListMap.get("catWords").contains(cWord2)){
			outputStr.append(" ONE");							
		} else {
			outputStr.append(" ZERO");
		}							
		
		//feature 10.rrDate
		if(featureValueListMap.get("rrDateWords") != null){
			if(featureValueListMap.get("rrDateWords").contains(cWord1) || 
					featureValueListMap.get("rrDateWords").contains(cWord2)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		} else {
			outputStr.append(" ZERO");
		}					
		
		//feature 11. model num
		if(featureValueMap.get("modelNumLine") != null){
			String modelNumLine = featureValueMap.get("modelNumLine");
			if(modelNumLine.contains(cWord1) || modelNumLine.contains(cWord2)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		} else {
			outputStr.append(" ZERO");
		}
		//feature 12. shippingWeight
		if(featureValueMap.get("shippingWeightLine") != null){
			String shippingWeightLine = featureValueMap.get("shippingWeightLine");
			if(shippingWeightLine.contains(cWord1) || shippingWeightLine.contains(cWord2)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		} else {
			outputStr.append(" ZERO");
		}
		//feature 13.newerModel
		if(featureValueMap.get("newModelLine") != null){
			String newModelLine = featureValueMap.get("newModelLine");
			if(Arrays.asList(newModelLine.split("[ ]+")).contains(cWord1) ||
					Arrays.asList(newModelLine.split("[ ]+")).contains(cWord2)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		} else {
			outputStr.append(" ZERO");
		}
		
		// feature 14.freqBoughtTitle
		if(featureValueListMap.get("freqBoughtWords") != null){
			if(featureValueListMap.get("freqBoughtWords").contains(cWord1) || 
					featureValueListMap.get("freqBoughtWords").contains(cWord2)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		}
		//feature 15. nextBoughtTitle
		if(featureValueListMap.get("nextBoughtTitleWords") != null){
			if(featureValueListMap.get("nextBoughtTitleWords").contains(cWord1) ||
					featureValueListMap.get("nextBoughtTitleWords").contains(cWord2)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		}
		//feature 16. nextBoughtTitlePublisher
		if(featureValueMap.get("nextBoughtPublisherLine") != null){
			String nextBoughtPublisherLine = featureValueMap.get("nextBoughtPublisherLine");
			if(Arrays.asList(nextBoughtPublisherLine.split("[ ]+")).contains(cWord1) ||
					Arrays.asList(nextBoughtPublisherLine.split("[ ]+")).contains(cWord2)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		} else {
			outputStr.append(" ZERO");
		}					
		
		//feature 17.releaseDate
		if(featureValueListMap.get("releaseDateWords") != null){
			if(featureValueListMap.get("releaseDateWords").contains(cWord1) ||
					featureValueListMap.get("releaseDateWords").contains(cWord2)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		} else {
			outputStr.append(" ZERO");
		}

		return outputStr;
	}//End getBigramAttributeValueFV
	
	public void outputBigramFV(String trainTest){
			
			//Output feature vectors
			if(DEBUG_FLAG){
				if(featureValueMap.size()>0){
					System.out.println("FV count = "+featureValueMap.size());				
				}
				if(featureValueListMap.size() > 0){
					System.out.println("FV list = "+featureValueListMap.size());
				}
			}
			if(featureValueListMap.get("candidateWordPairs") != null){
				for(String cWordPair : featureValueListMap.get("candidateWordPairs")){
					String[] cWords = cWordPair.split("[ ]+");
					//System.out.println(cWordPair+"->"+cWords[0]+","+cWords[1]);
					StringBuilder outputStr = new StringBuilder();
					StringBuilder labelsStr = new StringBuilder();
					//feature 1 and 2 = title word		
					outputStr.append(cWordPair);
					labelsStr.append(cWordPair);

					//add Attribute Value feature Vectors
					outputStr.append(getBigramAttributeValueFV(cWords[0], cWords[1]));
					
					//label - for TRAIN only
					if(trainTest.contains("train") || trainTest.contains("label")){
						String label=null;
						if(featureValueMap.get("brandName") != null){
							String brandName = featureValueMap.get("brandName");
							if(Arrays.asList(brandName.split("[ ]+")).contains(cWords[0]) ||
									Arrays.asList(brandName.split("[ ]+")).contains(cWords[1])){
								label = "brand";							
							}		
						} 
						if(featureValueMap.get("productName") != null){
							String productName = featureValueMap.get("productName");
							if(Arrays.asList(productName.split("[ ]+")).contains(cWords[0]) ||
									Arrays.asList(productName.split("[ ]+")).contains(cWords[1])){
								label="product";							
							}
						} 
						if(featureValueMap.get("versionName") != null){
							String versionName = featureValueMap.get("versionName");
							if(Arrays.asList(versionName.split("[ ]+")).contains(cWords[0]) ||
									Arrays.asList(versionName.split("[ ]+")).contains(cWords[1])){
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
					if(trainTest.contains("train") || trainTest.contains("test")){
						System.out.println(outputStr.toString());
					} else if(trainTest.contains("label") ){
						System.out.println(labelsStr.toString());
					}
					
				}//End for candWordPair
			}//End if candWordPair
			else{
				System.out.println("DEBUG : from Else of "+featureValueListMap.size());
				for(Entry<String, ArrayList<String>> arr : featureValueListMap.entrySet()){
					System.out.println(arr.getKey());
					System.out.println(arr.getValue());
				}
			}
			
		
	}//End printout Bigrams

	
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
				
		//feature 3 = author
		if(featureValueMap.get("authorLine") != null){
			if(Arrays.asList(featureValueMap.get("authorLine").split("[ ]+")).contains(cWord)){
				outputStr.append(" ONE");							
			}else {
				outputStr.append(" ZERO");
			}						
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 4. isbn
		if(featureValueMap.get("isbnLine") != null){
			if(featureValueMap.get("isbnLine").contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			} 
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 5.publDate 2011 - 2013
		if(featureValueListMap.get("publishDateLine") != null){
			if(Integer.parseInt(featureValueMap.get("publDateLine")) > 10 ){
				outputStr.append(" ONE");
			} else {
				outputStr.append(" ZERO");
			}			
		} else {
			outputStr.append(" ZERO");
		}	
			
		//feature 5.publDate 2010 - 2013
		if(featureValueListMap.get("publishDateLine") != null){
			if(Integer.parseInt(featureValueMap.get("publDateLine")) <= 10 ){
				outputStr.append(" ONE");
			} else {
				outputStr.append(" ZERO");
			}			
		} else {
			outputStr.append(" ZERO");
		}	

		
		//feature 6.publisher
		if(featureValueMap.get("publisherLine") != null){
			if(Arrays.asList(featureValueMap.get("publisherLine").split("[ ]+")).contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			} 
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 7.edition
		if(featureValueMap.get("editionLine") != null){						
			if(Arrays.asList(featureValueMap.get("editionLine").split("[ ]+")).contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 8. language
		if(featureValueMap.get("langLine") != null){
			if(featureValueMap.get("langLine").contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			} 
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 9.catHier
		if(featureValueListMap.get("catWords") != null){
			if(featureValueListMap.get("catWords").contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}							
		}
		//feature 10.rrDate - in 2011 to 2013
		if(featureValueMap.get("rrDateLine") != null){
			if(Integer.parseInt(featureValueMap.get("rrDateLine")) > 10 ){
				outputStr.append(" ONE");
			} else {
				outputStr.append(" ZERO");
			}			
		} else {
			outputStr.append(" ZERO");
		}					

		//feature 11.rrDate - in 2000 to 2010
		if(featureValueMap.get("rrDateLine") != null){
			if(Integer.parseInt(featureValueMap.get("rrDateLine")) <= 10 ){
			outputStr.append(" ONE");
			} else {
				outputStr.append(" ZERO");
			}			
		} else {
			outputStr.append(" ZERO");
		}
		
		//feature 12. model num
		if(featureValueMap.get("modelNumLine") != null){
			if(featureValueMap.get("modelNumLine").contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		} else {
			outputStr.append(" ZERO");
		}
		//feature 13. shippingWeight
		if(featureValueMap.get("shippingWeightLine") != null){
			if(featureValueMap.get("shippingWeightLine").contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		} else {
			outputStr.append(" ZERO");
		}
		//feature 14.newerModel
		if(featureValueMap.get("newModelLine") != null){						
			if(Arrays.asList(featureValueMap.get("newModelLine").split("[ ]+")).contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		} else {
			outputStr.append(" ZERO");
		}
		
		// feature 15.freqBoughtTitle
		if(featureValueMap.get("freqBoughtWords") != null){
			if(featureValueMap.get("freqBoughtWords").contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		}
		//feature 16. nextBoughtTitle
		if(featureValueListMap.get("nextBoughtTitleWords") != null){
			if(featureValueListMap.get("nextBoughtTitleWords").contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		}
		//feature 17. nextBoughtTitlePublisher
		if(featureValueMap.get("nextBoughtPublisherLine") != null){						
			if(Arrays.asList(featureValueMap.get("nextBoughtPublisherLine").split("[ ]+")).contains(cWord)){
				outputStr.append(" ONE");							
			} else {
				outputStr.append(" ZERO");
			}
		} else {
			outputStr.append(" ZERO");
		}					
		
		//feature 18.releaseDate
		if(featureValueListMap.get("releaseDateWords") != null){
			if(featureValueListMap.get("releaseDateWords").contains(cWord)){
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
//		if(DEBUG_FLAG){
//			if(featureValueMap.size()>0){
//				System.out.println("FV count = "+featureValueMap.size());				
//			}
//			if(featureValueListMap.size() > 0){
//				System.out.println("FV list = "+featureValueListMap.size());
//			}
//		}//if debug
		
		
		if(featureValueListMap.get("candidateWords") != null){
		
				for(String cWord : featureValueListMap.get("candidateWords")){
					StringBuilder outputStr = new StringBuilder();
					StringBuilder labelsStr = new StringBuilder();
					StringBuilder evalStr = new StringBuilder();
					//feature 1 and 2 = title word		
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
