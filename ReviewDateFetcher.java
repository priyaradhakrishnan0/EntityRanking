package Version2;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class ReviewDateFetcher {
		/**Program to fetch review pages of a given prodId from amazon.com */

		    public static void main(String[] args) throws IOException {
		    	ReviewDateFetcher rdFetcher = new ReviewDateFetcher();
			    String url = "http://www.amazon.com/sthg/product-reviews/"+args[0]+"/ref=cm_cr_pr_top_recent?ie=UTF8&showViewpoints=0&sortBy=bySubmissionDateDescending";
			    String lastPageUrl = rdFetcher.lastPage(url);
			    if(lastPageUrl != null){
			    	System.out.println("Fetched last rview page "+lastPageUrl);
			    	System.out.println("rrDate = "+rdFetcher.revDate(lastPageUrl));
			    } else {
			    	System.out.println("rrDate = "+rdFetcher.revDate(url));
			    }
		    }//End main
		    
		    public String lastPage(String url){
		    	String lastPageUrl = null;		    	
				System.getProperties().put("proxySet", true);
				System.getProperties().put("proxyHost", "proxy.iiit.ac.in");
				System.getProperties().put("proxyPort", "8080");
		        Connection con;
		        Document doc;
				try {
					con = Jsoup.connect(url);
					con.timeout(6000);
					doc =con.get();				
			        Elements links = doc.select("div"); // System.out.println("Links: "+links.size());
			        ArrayList<String> urlList = new ArrayList<String>();
			        for (Element link : links) {			        	
			        	if( link.className().equalsIgnoreCase("CMpaginate")){
			        		for(Element revUrl : link.getElementsByAttribute("href")){
			        			urlList.add(revUrl.attr("href"));
			        		}
			        	}
			        }
			        if(urlList.size() > 0){
			        	lastPageUrl = urlList.get(1);
			        }			        
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return lastPageUrl;
		    }//End lastPage

		    public String revDate(String url){
		    	String earliestDate = null;		    	
				System.getProperties().put("proxySet", true);
				System.getProperties().put("proxyHost", "proxy.iiit.ac.in");
				System.getProperties().put("proxyPort", "8080");	
		        Connection con;
		        Document doc;
				try {
					con = Jsoup.connect(url);
					con.timeout(6000);
					doc =con.get();
				
			        Elements links = doc.select("nobr"); //  System.out.println("Links: "+links.size());
			        ArrayList<String> dateList = new ArrayList<String>();
					//Extract the manual tags "brand, product and version" 
					Pattern pBrand = Pattern.compile("[a-zA-Z]+ [0-9]+, [0-9]+");
			        for (Element link : links) {
						Matcher mBrand = pBrand.matcher(link.text());
						while(mBrand.find())
						{
							dateList.add(link.text());
						}
			        }
			        if(dateList.size()>0){
			        	//System.out.println("Dates count = "+dateList.size());
				        SimpleDateFormat format = new SimpleDateFormat("MMMM dd,yyyy");					 
						Date d0 = null, d1 = null;						 
						try {
							earliestDate = dateList.get(0); 							
							Calendar calendar0 = Calendar.getInstance();
							Calendar calendar1 = Calendar.getInstance();
							for(int i=1; i< dateList.size(); ++i){ //System.out.println(dateList.get(i));
								d0 = format.parse(earliestDate);
								d1 = format.parse(dateList.get(i));
						        calendar0.setTime(d0);  
						        calendar1.setTime(d1);								 
								 if(calendar0.after(calendar1)){
									 earliestDate = dateList.get(i);
								 }
							 }
						} catch (Exception e) {
							e.printStackTrace();
						}	
			        }
				} catch (Exception e) {
					e.printStackTrace();
				}
				return earliestDate;
		    }//End getDate
}
