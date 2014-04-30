package Version2;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class PredecessorVersionFeatureVector {

	static //String datasetPath = "/home/priya/Desktop/EntityRanking/evaluation/version2.0";//for evaluation
	String datasetPath = "/home/priya/Desktop/EntityRanking/dataset/camera";//for local machine - hand labelled
	//String datasetPath = "/home/iiit/priya/EntityRanking/dataset/camera";//for abacus 
	public static void main(String[] args) {
		PredecessorVersionFeatureVector pred = new PredecessorVersionFeatureVector();
		pred.addRevAgeToFile(args[0]);
	}
	public void addRevAgeToFile(String filename){
		String taggedFile = datasetPath+"/"+filename.trim();
		
		//1.Query and find the latest date
		ReviewDateFetcher rdFetcher = new ReviewDateFetcher();
		String amazonId = filename.replace(".parsed", ""); System.out.println(amazonId);
		String url = "http://www.amazon.com/sthg/product-reviews/"+amazonId+"/ref=cm_cr_pr_top_recent?ie=UTF8&showViewpoints=0&sortBy=bySubmissionDateDescending";
		//System.out.println(url);
		try{
			String lastPageUrl = rdFetcher.lastPage(url);
			String revDate = null;
			if(lastPageUrl != null){ //System.out.println(lastPageUrl);
				revDate = rdFetcher.revDate(lastPageUrl);
			} else {
				revDate = rdFetcher.revDate(url);
			}
			if(revDate != null){ System.out.println(" CandidateVersion : rrDate ="+revDate);
				//2.Append it to file in camera dataset.
					try {
						BufferedWriter bw = new BufferedWriter(new FileWriter(taggedFile,true));
						bw.write("\nearliestReviewDate : "+revDate);
						bw.close();			 
					} catch (Exception e) {
						e.printStackTrace();
					}	

			}//End if revDate
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}//End addrevAgeToFile
}
