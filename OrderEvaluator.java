package Version2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OrderEvaluator {

	/**Evaluate Orderings with Kendall tau rank correlation coefficient	 */
	public static void main(String[] args) {
		OrderEvaluator oeval = new OrderEvaluator();
		
		OrderGenerator og = new OrderGenerator();
		og.getFileList(args[0]);		
		og.populateProductListMap(); 	
		ArrayList<String> lexOrder = og.getLexicalChain(args[1]); System.out.println("lexOrder "+lexOrder);
		ArrayList<String> DateOrder = og.getDateChain(args[1]);  System.out.println("Date Order "+DateOrder); 
		HashMap<String, ArrayList<String>> versionPredecessorlistMap = og.getFileChain(args[1]);
		//og.clearMaps();
		
		//System.out.println(args[1]+" has "+versionPredecessorlistMap.size()+" versions.");
		double T1 = oeval.evaluate(versionPredecessorlistMap, lexOrder); System.out.println(args[0]+" "+args[1]+" Correlation of lexical ordering with gold std = "+T1);
		double T2 = oeval.evaluate(versionPredecessorlistMap, DateOrder);System.out.println(args[0]+" "+args[1]+" Correlation of Datewise ordering with gold std = "+T2);
		ArrayList<String>  LexTimeTime = oeval.combine(lexOrder, DateOrder, "Time"); System.out.println("LexTimeTime"+ LexTimeTime);
		double T3 = oeval.evaluate(versionPredecessorlistMap, LexTimeTime); System.out.println(args[0]+" "+args[1]+" Correlation of LexTimeTime ordering with gold std = "+T3);		
		ArrayList<String>  LexTimeLex = oeval.combine(lexOrder, DateOrder, "Lex"); System.out.println("LexTimeLex"+ LexTimeLex);
		double T4 = oeval.evaluate(versionPredecessorlistMap, LexTimeLex); System.out.println(args[0]+" "+args[1]+" Correlation of LexTimeLex ordering with gold std = "+ T4);
		System.out.println(" "+T1+" "+T2+" "+T3+" "+T4+" ");
	}
	/*Calculate Kendall tau*/
	public double evaluate(HashMap<String, ArrayList<String>> versionPredecessorlistMap, ArrayList<String> testOrder){
		double tau = 0.0;
		int  concordant = 0, discordant = 0, equalRanked =0;
		for(String fileVersion : versionPredecessorlistMap.keySet()){ //System.out.println("fileVersion "+fileVersion);
			if(testOrder.contains(fileVersion)){ //System.out.println("test succ rank "+testOrder.indexOf(fileVersion));
				for(String predVersion : versionPredecessorlistMap.get(fileVersion)){
					if(testOrder.contains(predVersion)){ //System.out.println("test pred rank "+testOrder.indexOf(predVersion));
						if(testOrder.indexOf(predVersion) < testOrder.indexOf(fileVersion)){
							++concordant;
						} else if(testOrder.indexOf(predVersion) > testOrder.indexOf(fileVersion)){
							++discordant;
						} else {
							++equalRanked;
						}
					}
				}//End for predVersion
			}
		}//End for fileVersion
		//System.out.println("concordant "+concordant+" discordant "+discordant+ " equalRanked "+equalRanked);
		tau =(double) (concordant - discordant)/(concordant + discordant); //System.out.println(tau);
		return tau;
	}
	/*Combine two rankings. Tie breaking using determiner ranking*/
	public ArrayList<String> combine(ArrayList<String> firstOrdering, ArrayList<String> secondOrdering, String determiner){
		ArrayList<String> combinedOrder = new ArrayList<String>();
		//initlalize result array
		for(int j = 0; j < (firstOrdering.size()+secondOrdering.size()); ++j){
			combinedOrder.add(j, null);
		}

		if(firstOrdering.size() >= secondOrdering.size()){
			for(int i=0; i< firstOrdering.size(); ++i){
				String fVersion = firstOrdering.get(i); int sIndex = 0, cIndex =0;
				if(secondOrdering.contains(fVersion)){
					sIndex  = secondOrdering.indexOf(fVersion);
				}
				cIndex = i + sIndex;
				if(combinedOrder.get(cIndex) != null){ 	//tie!
					String existingVersion = combinedOrder.get(cIndex);					
					if(determiner.equalsIgnoreCase("Time")){
						if(secondOrdering.indexOf(existingVersion) > secondOrdering.indexOf(fVersion)){
							//replace
							combinedOrder.remove(cIndex);
							combinedOrder.add(cIndex, fVersion);
							int k= cIndex+1;
							while((combinedOrder.get(k) != null)&(k < combinedOrder.size())){
								++k;
							}
							combinedOrder.add(k,existingVersion);
						} else {
							int k= cIndex+1;
							while((combinedOrder.get(k) != null)&(k < combinedOrder.size())){
								++k;
							}
							combinedOrder.add(k,fVersion);							
						}
					} else if(determiner.equalsIgnoreCase("Lex")){
						if(firstOrdering.indexOf(existingVersion) > firstOrdering.indexOf(fVersion)){
							//replace
							combinedOrder.remove(cIndex);
							combinedOrder.add(cIndex, fVersion);
							int k= cIndex+1;
							while((combinedOrder.get(k) != null)&(k < combinedOrder.size())){
								++k;
							}
							combinedOrder.add(k,existingVersion);
						} else {
							int k= cIndex+1;
							while((combinedOrder.get(k) != null)&(k < combinedOrder.size())){
								++k;
							}
							combinedOrder.add(k,fVersion);							
						}
					} 
				} else { //	System.out.println("Adding "+fVersion+" at "+cIndex);
					combinedOrder.add(cIndex, fVersion);
				}
			
			}
		} else { //firstOrdering.size() < secondOrdering.size()
			for(int i=0; i < secondOrdering.size(); ++i){
				String sVersion = secondOrdering.get(i); int fIndex = 0, cIndex =0;
				if(firstOrdering.contains(sVersion)){
					fIndex  = firstOrdering.indexOf(sVersion);
				}
				cIndex = i + fIndex;
				if(combinedOrder.get(cIndex) != null){ 	//tie!
					String existingVersion = combinedOrder.get(cIndex);					
					if(determiner.equalsIgnoreCase("Time")){
						if(secondOrdering.indexOf(existingVersion) > secondOrdering.indexOf(sVersion)){
							//replace
							combinedOrder.remove(cIndex);
							combinedOrder.add(cIndex, sVersion);
							int k= cIndex+1;
							while((combinedOrder.get(k) != null)&(k < combinedOrder.size())){
								++k;
							}
							combinedOrder.add(k,existingVersion);
						} else {
							int k= cIndex+1;
							while((combinedOrder.get(k) != null)&(k < combinedOrder.size())){
								++k;
							}
							combinedOrder.add(k,sVersion);							
						}
					} else if(determiner.equalsIgnoreCase("Lex")){
						if(firstOrdering.indexOf(existingVersion) > firstOrdering.indexOf(sVersion)){
							//replace
							combinedOrder.remove(cIndex);
							combinedOrder.add(cIndex, sVersion);
							int k= cIndex+1;
							while((combinedOrder.get(k) != null)&(k < combinedOrder.size())){
								++k;
							}
							combinedOrder.add(k,existingVersion);
						} else {
							int k= cIndex+1;
							while((combinedOrder.get(k) != null)&(k < combinedOrder.size())){
								++k;
							}
							combinedOrder.add(k,sVersion);							
						}
					} 
				} else { //	System.out.println("Adding "+sVersion+" at "+cIndex);
					combinedOrder.add(cIndex, sVersion);
				}
			}
		}
		return combinedOrder;
	}
	
}
