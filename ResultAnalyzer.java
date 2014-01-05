package Version2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ResultAnalyzer {

	String inPath = "/home/priya/Desktop/EntityRanking/version2.0";//for local machine
	public static void main(String[] args) {
		ResultAnalyzer ra = new ResultAnalyzer();
		String read;
		int idx = 0;
		String labelFile1 = ra.inPath+"/"+args[0];
		String labelFile2 = ra.inPath+"/"+args[1];
		
		int brand_brand = 0, na_na = 0, product_product = 0, version_version = 0,
				brand_product = 0, brand_version = 0, brand_na = 0,
				product_brand = 0, product_version = 0, product_na = 0,
				version_brand = 0, version_product = 0, version_na = 0,
				na_brand =0, na_product = 0, na_version = 0;

		try {
			BufferedReader br1 = new BufferedReader(new FileReader(labelFile1));
			BufferedReader br2 = new BufferedReader(new FileReader(labelFile2));
			
			while((read = br1.readLine())!=null)
			{
				String line1 = read.trim();
				//System.out.println(idx+"File 1 = "+line1);
				String[] line1Words = line1.split("[ ]+");
				++idx;//current line number

				//read this line in next file
				String line2 = br2.readLine();
				//System.out.println(idx+"File 2 = "+line2);
				if(line2 != null){
					String[] line2Words = line2.split("[ ]+");
					//System.out.println("first word = "+line1Words[0]);
					//System.out.println("last word = "+line2Words[line2Words.length - 1]);
					if(line1Words[0].trim().equalsIgnoreCase(line2Words[line2Words.length - 1])){
						String targetLabel = line2Words[0].trim();
						//System.out.println("Current label "+line1Words[1].trim());
						//System.out.println("Target label "+targetLabel);
						//found corresponding lines
						switch(line1Words[1].trim()){
						case "brand":
							if(targetLabel.equalsIgnoreCase("brand")){
								++brand_brand;
								//System.out.println("Found BB");
							} else if(targetLabel.equalsIgnoreCase("product")){
								++brand_product;
							} else if(targetLabel.equalsIgnoreCase("version")){
								++brand_version;
							} else if(targetLabel.equalsIgnoreCase("na")){
								++brand_na;
							}								
							break;
						case "product":
							if(targetLabel.equalsIgnoreCase("product")){
								++product_product;
								//System.out.println("Found PP");
							} else if(targetLabel.equalsIgnoreCase("brand")){
								++product_brand;
							} else if(targetLabel.equalsIgnoreCase("version")){
								++product_version;
							} else if(targetLabel.equalsIgnoreCase("na")){
								++product_na;
							}	
							break;
						case "version":
							if(targetLabel.equalsIgnoreCase("version")){
								++version_version;
								//System.out.println("Found VV");
							} else if(targetLabel.equalsIgnoreCase("brand")){
								++version_brand;
							} else if(targetLabel.equalsIgnoreCase("product")){
								++version_product;
							} else if(targetLabel.equalsIgnoreCase("na")){
								++version_na;
							}					
							break;
						case "na":
							if(targetLabel.equalsIgnoreCase("na")){
								++na_na;
								//System.out.println("Found NN");
							} else if(targetLabel.equalsIgnoreCase("brand")){
								++na_brand;
							} else if(targetLabel.equalsIgnoreCase("version")){
								++na_version;
							} else if(targetLabel.equalsIgnoreCase("product")){
								++na_product;
							}							
							break;
						default:
							System.out.println("Error on "+line1Words[1].trim()+"at line"+idx);
							break;
						}//End switch
					}//End if words match
				}//End if line2 read
			}//End while read
			
//			System.out.println("BB = "+brand_brand+", BP = "+brand_product+", BV = "+brand_version+", BN = "+brand_na);
//			System.out.println("PB = "+product_brand+", PP = "+product_product+", PV = "+product_version+", PN = "+product_na);
//			System.out.println("VB = "+version_brand+", VP = "+version_product+", VV = "+version_version+", VN = "+version_na);
//			System.out.println("NB = "+na_brand+", NP = "+na_product+", NV = "+na_version+", NN = "+na_na);
		
			///*fourBYfour start
			double rB = (double) brand_brand / (brand_brand + brand_product + brand_version + brand_na) ;
			double pB = (double) brand_brand / (brand_brand + product_brand + version_brand + na_brand);
			double fB = 2 * pB * rB / (pB + rB);
			
			double rP = (double) product_product / (product_brand + product_product + product_version + product_na );
			double pP = (double) product_product / (brand_product + product_product + version_product + na_product);
			double fP = 2 * pP * rP / (pP + rP);
			
			double rV = (double) version_version / (version_brand + version_product + version_version + version_na );
			double pV = (double) version_version / (brand_version + product_version + version_version + na_version);
			double fV = 2 * pV * rV / (pV + rV);
			
			double rN = (double) na_na / (na_brand + na_product + na_version + na_na );
			double pN = (double) na_na / (brand_na + product_na + version_na + na_na);
			double fN = 2 * pN * rN / (pN + rN);
			
			System.out.println(" *Brand* P = "+pB+", R = "+rB+", F = "+fB);
			System.out.println(" *Product* P = "+pP+", R = "+rP+", F = "+fP);
			System.out.println(" *Version* P = "+pV+", R = "+rV+", F = "+fV);
			System.out.println(" *na* P = "+pN+", R = "+rN+", F = "+fN);
			
			System.out.println(pB+","+rB+","+fB+", ,"+pP+","+rP+","+fP+", ,"+pV+","+rV+","+fV+", ,"+pN+","+rN+","+fN);
			//fourBYfour end*/
			
			/*threeBythree - strat
			double rB = (double) brand_brand / (brand_brand + brand_product + brand_version + brand_na) ;
			double pB = (double) brand_brand / (brand_brand + product_brand + version_brand + na_brand);
			double fB = 2 * pB * rB / (pB + rB);
			
			
			double rVP = (double) (product_product + product_version + version_version + version_product) / (product_product + product_version + version_version + version_product + product_brand + version_brand + version_na + product_na );
			double pVP = (double) (product_product + product_version + version_version + version_product) / (product_product + product_version + version_version + version_product + brand_product + brand_version + na_product + na_version);
			double fVP = 2 * pVP * rVP / (pVP + rVP);

			double rN = (double) na_na / (na_brand + na_product + na_version + na_na );
			double pN = (double) na_na / (brand_na + product_na + version_na + na_na);
			double fN = 2 * pN * rN / (pN + rN);
			
//			System.out.println(" *Brand* P = "+pB+", R = "+rB+", F = "+fB);
//			System.out.println(" *Product or Label* P = "+pVP+", R = "+rVP+", F = "+fVP);
//			System.out.println(" *na* P = "+pN+", R = "+rN+", F = "+fN);
			
			System.out.println(pB+","+rB+","+fB+", ,"+pVP+","+rVP+","+fVP+", ,"+pN+","+rN+","+fN);
			//threeBythree - end*/
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Unable to open result File "+ labelFile1);
			e.printStackTrace();
		}
	}//End main

}//End class
