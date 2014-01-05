package Version2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrderGenerator {

	String datasetPath = "/home/priya/Desktop/EntityRanking/dataset/camera";//for local machine
	//String datasetPath = "/home/iiit/priya/EntityRanking/dataset/camera";//for abacus 
	static HashMap<String, String> VersionFileMap = new HashMap<String, String>();
	static HashMap<String, ArrayList<String>> ProductFileListMap = new HashMap<String, ArrayList<String>>();
	static HashMap<String, ArrayList<String>> ProductVersionListMap = new HashMap<String, ArrayList<String>>();
	
	public void clearMaps(){
		VersionFileMap.clear();
		ProductFileListMap.clear();
		ProductVersionListMap.clear();
	}
	public HashMap<String, ArrayList<String>> getProductFileListMap(){
		return (this.ProductFileListMap);
	}
	public static void main(String[] args) {
		OrderGenerator og = new OrderGenerator();
		og.getFileList(args[0]);		//System.out.println(args[0] +" has "+VersionFileMap.size() +" versions");
		og.printArrayList(og.getProductList());
		//og.populateProductListMap(); 	System.out.println(args[0] +" has "+ProductFileListMap.size() +" products");
		//System.out.println(args[0] +" has "+ProductVersionListMap.size() +" products");
		//System.out.println(args[1]+" :: Lexical Order "+og.getLexicalChain(args[1]));
		//System.out.println(args[1]+" :: Datewise Order "+og.getDateChain(args[1])); 
		//og.isPredecessor( "B000CDLFRM.parsed","B000NOSUB4.parsed");
		HashMap<String, ArrayList<String>> versionPredecessorlistMap = og.getFileChain(args[1]);
	}//End main
	/*populates VersionFileMap*/
	public void getFileList(String requiredBrand){
		File folder = new File(datasetPath);			
		for (File fileEntry : folder.listFiles()) {	        	
			String taggedFile = datasetPath+"/"+fileEntry.getName();
			try {
				BufferedReader br = new BufferedReader(new FileReader(taggedFile));	
				String read=null,brandName=null, productName =null, versionName=null;	
				boolean  foundTitleWords = false;
				while(foundTitleWords == false && (read = br.readLine())!=null)
				{				
					String line = read.trim();				
					if(line.contains("productName:")){
						foundTitleWords = true;
						Pattern pBrand = Pattern.compile("\\{[a-zA-Z0-9 \\-+&]+\\}_b");
						Matcher mBrand = pBrand.matcher(line);
						while(mBrand.find())
						{
							String brandName0 = mBrand.group();
							brandName = brandName0.replace("{", "");
							brandName = brandName.replace("}_b", "");
							line = line.replace(brandName0,brandName);
						}
						if(brandName != null){
							if(brandName.equalsIgnoreCase(requiredBrand)){
								StringBuilder productVersion = new StringBuilder();
								Pattern pProduct = Pattern.compile("\\{[a-zA-Z0-9 \\-+./#]+\\}_p");
								Matcher mProduct = pProduct.matcher(line);
								while(mProduct.find())
								{
									String productName0 = mProduct.group();					
									productName = productName0.replace("{", "");
									productName = productName.replace("}_p", "");
									productVersion = productVersion.append(productName.trim()).append("#");
									line = line.replace(productName0, productName);//	System.out.println("Product = "+productName);
								}				
								Pattern pVersion = Pattern.compile("\\{[a-zA-Z0-9 \\-+./#]+\\}_v");
								Matcher mVersion = pVersion.matcher(line);
								while(mVersion.find())
								{
									String versionName0 = mVersion.group();					
									versionName = versionName0.replace("{", "");
									versionName = versionName.replace("}_v", "");
									productVersion = productVersion.append(versionName.trim());
									line = line.replace(versionName0, versionName);//		System.out.println("Version = "+versionName);
								}
								String prodVersion = productVersion.toString();
								if(prodVersion != null){
									VersionFileMap.put(prodVersion.trim(), fileEntry.getName());
									//System.out.println("p/V ="+prodVersion+" File = "+fileEntry.getName()+" Brand = "+brandName);
								} 						
							}//End if brands match
						}
					}//End if productName								
				}//End while
				br.close();
			} catch (IOException e) {
				System.out.println("Unable to open productItem File "+taggedFile);
				e.printStackTrace();
			}	
		}//End for fileEntry
	}//End getFileList
	/*populates ProductFileListMap and ProductVersionListMap*/
	public void populateProductListMap(){
		
    	if(VersionFileMap.size()>0){
    		ArrayList<String> productList = getProductList();    		        	
    		for(String product: productList ){ //System.out.println("Generating lexical version chain for : "+product);
    			List<String> VersionOrder = new ArrayList<String>();
	    		for(String prodVers : VersionFileMap.keySet()){
	    			String[] pv = prodVers.split("#");
	    			if((pv.length == 2) && (pv[1] != null)){
	    				if(pv[0].equalsIgnoreCase(product)){
	    					VersionOrder.add(pv[1]);
	    				}
	    			}
	    		}
    			Collections.sort(VersionOrder);
	        	ArrayList<String> fileOrder = new ArrayList<String>();
	        	for(String version : VersionOrder){  		//System.out.println(version +" "+VersionFileMap.get(product+"#"+version));
	        		//System.out.print(version.split("#")[1] +" ");
	        		//lexicographic ordering
	        		fileOrder.add(VersionFileMap.get(product+"#"+version));	        	
	        	}
	        	ArrayList<String> VersionList = new ArrayList<String>(VersionOrder.size());
	        	VersionList.addAll(VersionOrder);
	        	ProductVersionListMap.put(product, VersionList);
	        	ProductFileListMap.put(product, fileOrder);
    		}
    	} 
    	//return VersionOrder;
    }//End populateProductListMap
	/*Gives review_date wise ordering of the versions of given product */
	public ArrayList<String> getDateChain(String product){
		ArrayList<String> VersionOrder = new ArrayList<String>();	    		
    	ArrayList<String> DateChain = new ArrayList<String>();
    	if(ProductVersionListMap.size() > 0){    
    		if(ProductVersionListMap.get(product)!=null){
    			VersionOrder = ProductVersionListMap.get(product);
    		}
    	}	        	
    	SortedMap<Date, String> rrDateFileMap = new TreeMap<Date, String>();	
    	SimpleDateFormat dateformat = new SimpleDateFormat("MMMM dd,yyyy");	
    	if(VersionOrder.size()>0){
        	for(String versionName : VersionOrder){  		
        		ReviewDateFetcher rdFetcher = new ReviewDateFetcher();
        		String amazonId = VersionFileMap.get(product+"#"+versionName).replace(".parsed", "");
			    String url = "http://www.amazon.com/sthg/product-reviews/"+amazonId+"/ref=cm_cr_pr_top_recent?ie=UTF8&showViewpoints=0&sortBy=bySubmissionDateDescending";
			    try{
				    String lastPageUrl = rdFetcher.lastPage(url);
				    if(lastPageUrl != null){
				    	String revDate = rdFetcher.revDate(lastPageUrl);
				    	if(revDate != null){
				    		Date rrDate = dateformat.parse(revDate);
				    		rrDateFileMap.put(rrDate, versionName);
				    	}
				    }
				} catch (Exception e) {
					e.printStackTrace();
				}
        	}
        	//System.out.println("Date wise ordering:");
        	if(rrDateFileMap.size() > 1){
	        	for(Date rrDate : rrDateFileMap.keySet()){
	        		String vName = rrDateFileMap.get(rrDate); 		//System.out.print(" "+vName);
	        		DateChain.add(vName);
	        	}
        	}
    	}
    	return DateChain;
    }//End getDateChain()
	/*get list of all products*/
    public ArrayList<String> getProductList(){
    	ArrayList<String> productList = new ArrayList<String>();
    	if(VersionFileMap.size()>0){    		
    		Iterator<String> it = VersionFileMap.keySet().iterator();    		
    		while(it.hasNext()){
				String key = it.next();
				if((key.split("#").length == 2) && (key.split("#")[1] != null)){
					if(productList.contains(key.split("#")[0])){
						//do not add
					} else {
						productList.add(key.split("#")[0]);
					}
				}			
    		}   
    	}
    	return productList;
    }//End getProductList
    public boolean isPredecessor(String file1, String file2){
    	boolean predecessor = false;
    	if(VersionFileMap.size()>0){
    		String sourceVersion = null;
    		Iterator<String> it = VersionFileMap.keySet().iterator();    		
    		while(it.hasNext() && sourceVersion == null){
    			String prodVers = it.next();    		
    			String[] pv = prodVers.split("#");
    			if((pv.length == 2) && (pv[1] != null)){
    				if(VersionFileMap.get(prodVers).equalsIgnoreCase(file1)){
    					sourceVersion = pv[1];
    					System.out.println("SourceVersion "+sourceVersion);
    				}
    			}
    		}
    		if(sourceVersion != null){
				String taggedFile = datasetPath+"/"+file2;        		
		    	FeatureVector fv = new FeatureVector();        			
	    		fv.createFeatures(taggedFile);
	    		if(fv.isProductLabel()){
	    			fv.createAttributeValueFV(taggedFile);
	    			if(fv.featureValueListMap.get("productDescriptionWords").contains(sourceVersion)){
	    				System.out.println(file1 +" preceeds "+file2);
	    				predecessor = true;
	    			} else if(fv.featureValueListMap.get("urlDescriptionWords").contains(sourceVersion)){
	    				System.out.println(file1 +" preceeds "+file2);
	    				predecessor = true;
	    			} else if(fv.featureValueListMap.get("category").contains(sourceVersion)){
	    				System.out.println(file1 +" preceeds "+file2);
	    				predecessor = true;
	    			} else if(fv.featureValueListMap.get("nextBoughtTitle") != null){
	    				if(fv.featureValueListMap.get("nextBoughtTitle").contains(sourceVersion)){
	    					System.out.println(file1 +" preceeds "+file2);
		    				predecessor = true;
	    				}	    				
	    			} else if(fv.featureValueListMap.get("nextBoughtBrand") != null){
	    				if(fv.featureValueListMap.get("nextBoughtBrand").contains(sourceVersion)){
	    					System.out.println(file1 +" preceeds "+file2);
		    				predecessor = true;
	    				}	    				
	    			} else if(fv.featureValueListMap.get("freqBoughtTitle") != null){
	    				if(fv.featureValueListMap.get("freqBoughtTitle").contains(sourceVersion)){
	    					System.out.println(file1 +" preceeds "+file2);
		    				predecessor = true;
	    				}	    				
	    			} 
	    		}
	    		fv.clearMaps();
    		}
    	} 
    	return predecessor;
    }//End isPredecessor    
    /*get VersionFileMap of given product*/
    public HashMap<String, String> getpVersionFileMap(String product){
		HashMap<String, String> pVersionFileMap = new HashMap<String, String>();
    	if(ProductFileListMap.size() > 0){
    		ArrayList<String> fileList = ProductFileListMap.get(product);
    		//create version list for this product
    		for(String file : fileList){
	    		String sourceVersion = null;
	    		Iterator<String> it = VersionFileMap.keySet().iterator();    		
	    		while(it.hasNext() && sourceVersion == null){
	    			String prodVers = it.next();    		
	    			String[] pv = prodVers.split("#");
	    			if((pv.length == 2) && (pv[1] != null)){
	    				if(VersionFileMap.get(prodVers).equalsIgnoreCase(file)){
	    					sourceVersion = pv[1];
	    					pVersionFileMap.put(sourceVersion, file);//System.out.println("SourceVersion "+sourceVersion);
	    				}
	    			}
	    		}
    		}
    	}
		return pVersionFileMap;
    }//End getpVersionFileMap
    public ArrayList<String> getLexicalChain(String product){
    	ArrayList<String> LexicalChain = new ArrayList<String>();
    	if(ProductVersionListMap.size() > 0){    
    		if(ProductVersionListMap.get(product)!=null){
    			LexicalChain = ProductVersionListMap.get(product);
    		}
    	}
    	return LexicalChain;
    }//End getLexicalChain
    /*Gives predecessor_successor relations based ordering of the versions of given product */
    public ArrayList<String> getPrdecessorChain(String product){
    	ArrayList<String> predecessorChain = new ArrayList<String>();
    	HashMap<String, ArrayList<String>> versionPredecessorlistMap = getFileChain(product);
    	//Generate chain using depth first search
    	if(versionPredecessorlistMap.size() > 0){
    		Graph theGraph = new Graph();
    		ArrayList<String> vertexList = new ArrayList<String>();
    		for(String version : versionPredecessorlistMap.keySet()){
    			theGraph.addVertex(version);
    			vertexList.add(version);
    		}
    		for(String vertex : vertexList){    				 
    			if(versionPredecessorlistMap.get(vertex) != null){
    				for(String predVersion : versionPredecessorlistMap.get(vertex)){
    					theGraph.addEdge(vertexList.indexOf(vertex), vertexList.indexOf(predVersion));
    				}	
    			}

    		}    			
    		System.out.print("Visits: ");
    		predecessorChain = theGraph.dfs();             // depth-first search
    		System.out.println();
    	}//End dfs
    	return predecessorChain;
    }//End getPredecessorChain method
    /*Generates a map of version TO list of predecessor versions */
    public HashMap<String, ArrayList<String>> getFileChain(String product){
    	
    	HashMap<String, ArrayList<String>> versionPredecessorlistMap = new HashMap<String, ArrayList<String>>();
    	if(ProductFileListMap.size() > 0){
    		ArrayList<String> fileList = ProductFileListMap.get(product); //System.out.println("#files for prod "+product+" "+ProductFileListMap.get(product).size());
    		//HashMap<String, String> productVersionFileMap = getpVersionFileMap(product); System.out.println("#fileVesion for prod "+product+" "+ productVersionFileMap.get(product).length());
    		//check version presence in FV and create version_predecessors Map //System.out.println("predecessor - successor Relations");
    		for(String file : fileList){
    			//find the file's version 	//System.out.println("filename :"+file);
    			String fileVersion = null;
    			Iterator<String> it = VersionFileMap.keySet().iterator();    		
	    		while(it.hasNext() && fileVersion == null){
	    			String prodVers = it.next();    		
	    			String[] pv = prodVers.split("#");
	    			if((pv.length == 2) && (pv[1] != null)){
	    				if(VersionFileMap.get(prodVers).equalsIgnoreCase(file)){
	    					fileVersion = pv[1]; //System.out.println("File Version "+fileVersion);
	    				}
	    			}
	    		}
	    		if(fileVersion != null){
	    			ArrayList<String> predVersionList = new ArrayList<String>();
					String taggedFile = datasetPath+"/"+file;        		
			    	FeatureVector fv = new FeatureVector();        			
		    		fv.createFeatures(taggedFile);
		    		if(fv.isProductLabel()){
		    			fv.createAttributeValueFV(taggedFile);
		    			//check for all versions other than files's version in the FV
		    			for(String versionName : ProductVersionListMap.get(product)){		    				
		    				boolean predecessor = false;
		    				if(versionName.equalsIgnoreCase(fileVersion)){
		    					//do nothing
		    				} else {
		    					if(fv.featureValueListMap.get("productDescriptionWords").contains(versionName)){		    	    				
		    	    				predecessor = true;
		    	    			} else if(fv.featureValueListMap.get("urlDescriptionWords").contains(versionName)){
		    	    				predecessor = true;
		    	    			} else if(fv.featureValueListMap.get("category").contains(versionName)){
		    	    				predecessor = true;
		    	    			} else if(fv.featureValueListMap.get("nextBoughtTitle") != null){
		    	    				if(fv.featureValueListMap.get("nextBoughtTitle").contains(versionName)){
		    	    					predecessor = true;
		    	    				}	    				
		    	    			} else if(fv.featureValueListMap.get("nextBoughtBrand") != null){
		    	    				if(fv.featureValueListMap.get("nextBoughtBrand").contains(versionName)){
		    	    					predecessor = true;
		    	    				}	    				
		    	    			} else if(fv.featureValueListMap.get("freqBoughtTitle") != null){
		    	    				if(fv.featureValueListMap.get("freqBoughtTitle").contains(versionName)){
		    	    					predecessor = true;
		    	    				}	    				
		    	    			}
		    				}//End if not fileVersion
		    				if(predecessor){		//System.out.println(VersionFileMap.get(product+"#"+versionName) +" preceeds "+file+"."+versionName +" < "+fileVersion);
		    					//System.out.println(versionName +" < "+fileVersion);
		    					predVersionList.add(versionName);		    					
		    				}
		    			}//End for versionName
		    		}//End if productLabel
		    		versionPredecessorlistMap.put(fileVersion, predVersionList);
	    		}//End if fileVersion
    		}//End for each file
    	}
		return versionPredecessorlistMap;
    }//End getFileChain method    
    
	//Method prints out listing of given ArrayList
	private void printArrayList(ArrayList<String> entries)
	{			
	//Print the Links
			for( String entry : entries){				
				System.out.println(entry);
			}

	}//EndOf ArrayList
}//End class
