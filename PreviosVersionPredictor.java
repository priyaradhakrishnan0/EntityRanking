package Version2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.zip.DataFormatException;

public class PreviosVersionPredictor {

	/*predict previous version of given function*/
	public static void main(String[] args) {
		String brand = null, product =null, g1 = null, g2=null;
		if(args.length == 3){// brand g1 g2
			brand = args[0];
			g1 = args[1];
			g2 = args[2];
		} else if(args.length == 4){// brand product g1 g2
			brand = args[0];
			product = args[1];
			g1 = args[2];
			g2 = args[3];
		} else {
			System.out.println("class PreviosVersionPredictor : 3 or 4 input arguments");
			return;
		}
		PreviosVersionPredictor predictor = new PreviosVersionPredictor();
		OrderGenerator og = new OrderGenerator();
		og.getFileList(brand); //
		HashMap<String, String> VersionFileMap = og.getVersionFileMap(); //System.out.println("Num of files found with brand = "+brand+" = "+VersionFileMap.size());
		if(VersionFileMap.size()>0){
			if(product!=null){// brand product g1 g2
				//get all the versions
				List<String> VersionList = new ArrayList<String>();
				for(String prodVers : VersionFileMap.keySet()){
					String[] pv = prodVers.split("#");
					if((pv.length == 2) && (pv[1] != null)){
						if(pv[0].equalsIgnoreCase(product)){
							VersionList.add(pv[1]);
						}
					}
				}
				if(VersionFileMap.get(product+"#"+g2) != null){
					//get succFeatureVector for each version	
					for (String candidateVersion:VersionList){
						if(candidateVersion.equalsIgnoreCase(g1)){
							//do nothing. g1 cannot succeed g1
						} else {
							//System.out.println("File 1 = "+VersionFileMap.get(product+"#"+candidateVersion));
							//System.out.println("File 2 = "+VersionFileMap.get(product+"#"+g2));
							//Lexical predecessor
							boolean lexFeat = false;
							if(candidateVersion.compareTo(g1) < 0){
								lexFeat = true;			
							} 
							//Age predecessor
							boolean ageFeat = false;
							ageFeat = predictor.getEarliestAgeSuccessor(og.datasetPath+"/"+VersionFileMap.get(product+"#"+candidateVersion) , og.datasetPath+"/"+VersionFileMap.get(product+"#"+g1));
							//Predecessor Relation
							boolean predSuccFeat = false;
							predSuccFeat = predictor.getPredecessorProof(candidateVersion, og.datasetPath+"/"+VersionFileMap.get(product+"#"+g2));
							
							if(candidateVersion.equalsIgnoreCase(g2)){
								System.out.println(brand+","+g1+","+candidateVersion+","+lexFeat+","+ageFeat+","+predSuccFeat+",true");
							} else {
								System.out.println(brand+","+g1+","+candidateVersion+","+lexFeat+","+ageFeat+","+predSuccFeat+",false");
							}
						}//End if g1
					}//End for candidateVersion
				} else {	//System.out.println("UNATTEMPTED "+brand+" "+product+" "+g1+" "+g2+"As g2 product not present.");
					System.out.println("file 1 = "+VersionFileMap.get(product+"#"+g1));
					System.out.println(brand+" "+product+" "+g2);
				}				
			} else {// brand g1 g2
				if((VersionFileMap.get(g1) != null) || (VersionFileMap.get(g1+"#") != null)){
					String proVer=null;//get proVer
					if(VersionFileMap.get(g1) != null){
						proVer = g1; 
					} else {
						proVer = g1+"#";
					}
					//get predecessorFeatureVector for each version	
					for (String candidateVersion:VersionFileMap.keySet()){
						if(candidateVersion.equalsIgnoreCase(proVer)){
							//sorry. proVer cannot succeed proVer
						} else {
							//System.out.println("File 1 = "+VersionFileMap.get(candidateVersion));
							//System.out.println("File 2 = "+VersionFileMap.get(proVer));
							//Lexical predecessor
							boolean lexFeat = false;
	
							if(candidateVersion.replace("#","").compareTo(proVer.replace("#", "")) < 0){
								lexFeat = true;			
							} 
							//Age predecessor
							boolean ageFeat = false;
							ageFeat = predictor.getEarliestAgeSuccessor( og.datasetPath+"/"+VersionFileMap.get(candidateVersion), og.datasetPath+"/"+VersionFileMap.get(proVer));
							//Predecessor Relation
							boolean predSuccFeat = false;
							predSuccFeat = predictor.getPredecessorProof(candidateVersion.replace("#",""), og.datasetPath+"/"+VersionFileMap.get(proVer));
							//System.out.println(candidateVersion.replace("#","")+","+lexFeat+","+ageFeat+","+predSuccFeat);
							
							if(candidateVersion.replace("#", "").equalsIgnoreCase(g2)){
								System.out.println(brand+","+g1+","+candidateVersion.replace("#", "")+","+lexFeat+","+ageFeat+","+predSuccFeat+",true");
							} else {
								System.out.println(brand+","+g1+","+candidateVersion.replace("#", "")+","+lexFeat+","+ageFeat+","+predSuccFeat+",false");
							}
						}//End if g1(i.e proVer)
						
					}//End for candidateVersion
				} else { //System.out.println("UNATTEMPTED "+brand+" "+product+" "+g1+" "+g2+"As g2 product not present.");
					System.out.println("File 1 = "+VersionFileMap.get(g1+"#"));
					System.out.println(brand+" "+g2);
				}		
			}//End if brand product g1 g2 
		} //End if VersionFileMap.size()>0

	}//End main

	public boolean getAgeSuccessor(String f1, String f2){
		boolean elder = false;
		SimpleDateFormat dateformat = new SimpleDateFormat("MMMM dd,yyyy");
		ReviewDateFetcher rdFetcher = new ReviewDateFetcher();
		String amazonId1 = f1.replace(".parsed", "");
		String url1 = "http://www.amazon.com/sthg/product-reviews/"+amazonId1+"/ref=cm_cr_pr_top_recent?ie=UTF8&showViewpoints=0&sortBy=bySubmissionDateDescending";
		String amazonId2 = f2.replace(".parsed", "");
		String url2 = "http://www.amazon.com/sthg/product-reviews/"+amazonId2+"/ref=cm_cr_pr_top_recent?ie=UTF8&showViewpoints=0&sortBy=bySubmissionDateDescending";
		//System.out.println(url);
		try{
			String lastPageUrl1 = rdFetcher.lastPage(url1);
			String revDate1 = null, revDate2 = null;
			Date rrDate1 = null, rrDate2 = null;
			if(lastPageUrl1 != null){ //System.out.println(lastPageUrl);
				revDate1 = rdFetcher.revDate(lastPageUrl1);
			} else {
				revDate1 = rdFetcher.revDate(url1);
			}
			if(revDate1 != null){ System.out.println(" CandidateVersion : rrDate ="+revDate1);
			rrDate1 = dateformat.parse(revDate1);
			}
			String lastPageUrl2 = rdFetcher.lastPage(url2);		    
			if(lastPageUrl2 != null){ //System.out.println(lastPageUrl2);
				revDate2 = rdFetcher.revDate(lastPageUrl2);
			} else {
				revDate2 = rdFetcher.revDate(url2);
			}
			if(revDate2 != null){ System.out.println(" Successor Version : rrDate ="+revDate2);
			rrDate2 = dateformat.parse(revDate2);
			}
			if((rrDate1 != null) && (rrDate2 != null)){
				if(rrDate1.after(rrDate2)) elder = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return elder;
	}//End getAgeSuccessor
	
	public boolean getEarliestAgeSuccessor(String f1, String f2){
		boolean elder = false;
		String rrDateLine1 = null, rrDateLine2 = null;
		try {
			BufferedReader br1 = new BufferedReader(new FileReader(f1));			
			int age = 0;
			String read;		
			while((read = br1.readLine())!=null)
			{
				String line = read.trim();				
				//Extract earliestReviewDate :
				if(line.contains("earliestReviewDate :")){
					rrDateLine1 = line.replace("earliestReviewDate :", " "); 					
					rrDateLine1 = rrDateLine1.trim();	//System.out.println("rrDateLine1 = "+rrDateLine1);
				}
			}//End while
			br1.close();			
			BufferedReader br2 = new BufferedReader(new FileReader(f2));
			while((read = br2.readLine())!=null)
			{
				String line = read.trim();				
				//Extract earliestReviewDate :
				if(line.contains("earliestReviewDate :")){
					rrDateLine2 = line.replace("earliestReviewDate :", " "); 					
					rrDateLine2 = rrDateLine2.trim();	//System.out.println("rrDateLine1 = "+rrDateLine1);
				}
			}//End while
			br2.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//System.out.println("Unable to open productItem File "+f1+", "+f2);
			e.printStackTrace();
		}	
		
		if(rrDateLine1 != null && rrDateLine2 != null){			
			SimpleDateFormat format = new SimpleDateFormat("MMM dd,yyyy");					 
			Date d0 = null, d1 = null;						 
			try {
				d0 = format.parse(rrDateLine1);
				d1 = format.parse(rrDateLine2);	
				if(d1.after(d0)) 
					elder = true;
			}catch(Exception de){
				System.out.println("Unable to format date");
				de.printStackTrace();
			}			
		}
		return elder;
	}//End of getEarliestAgeSuccecessor
	public boolean getPredecessorProof(String candidateVersion, String f2){
		boolean proofPresent = false;
		String taggedFile = f2;        		
    	FeatureVector fv = new FeatureVector();        			
		fv.createFeatures(taggedFile);
		if(fv.isProductLabel()){
			fv.createAttributeValueFV(taggedFile);
			if(fv.featureValueListMap.get("productDescriptionWords") != null){		    	    				
				if(fv.featureValueListMap.get("productDescriptionWords").contains(candidateVersion)){		    	    				
					proofPresent = true;
				}
			} else if(fv.featureValueListMap.get("urlDescriptionWords") != null){
				if(fv.featureValueListMap.get("urlDescriptionWords").contains(candidateVersion)){
					proofPresent = true;
				}				
			} else if(fv.featureValueListMap.get("category") != null){
				if(fv.featureValueListMap.get("category").contains(candidateVersion)){
					proofPresent = true;
				}
			} else if(fv.featureValueListMap.get("nextBoughtTitle") != null){
				if(fv.featureValueListMap.get("nextBoughtTitle").contains(candidateVersion)){
					proofPresent = true;
				}	    				
			} else if(fv.featureValueListMap.get("nextBoughtBrand") != null){
				if(fv.featureValueListMap.get("nextBoughtBrand").contains(candidateVersion)){
					proofPresent = true;
				}	    				
			} else if(fv.featureValueListMap.get("freqBoughtTitle") != null){
				if(fv.featureValueListMap.get("freqBoughtTitle").contains(candidateVersion)){
					proofPresent = true;
				}	    				
			}
		}//End if productLabel
		return proofPresent;
	}//End getPredecessorProof		

}//End class PreviosVersionPredictor 
