package se.sthlm.jfw;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import twitter4j.GeoLocation;
import twitter4j.IDs;
import twitter4j.PagableResponseList;
import twitter4j.Paging;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.RateLimitStatus;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.UserList;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterScript {

    public static void main(String args[]) {
    	
    	String openshift_data_dir = System.getenv().get("OPENSHIFT_DATA_DIR");
		if(args.length > 0)
			openshift_data_dir = args[0];
		
		String textToDisplay = openshift_data_dir;
//		System.out.println("path: " + textToDisplay);
		
		try {

	        URL oracle = new URL("http://rosebud-scomer.rhcloud.com/welcome");
	        BufferedReader in = new BufferedReader(
	        new InputStreamReader(oracle.openStream()));

	        String inputLine;
	        while ((inputLine = in.readLine()) != null)
	            System.out.println(inputLine);
	        in.close();
		} catch(Exception ee) {
			try {
				logToFile(textToDisplay, "Main", ee.getMessage());
			} catch(Exception e) {
				//Ignored
			}
		}
		
		try {

	        URL oracle = new URL("http://rosenknopp-scomer.rhcloud.com/");
	        BufferedReader in = new BufferedReader(
	        new InputStreamReader(oracle.openStream()));

	        String inputLine;
	        while ((inputLine = in.readLine()) != null)
	            System.out.println(inputLine);
	        in.close();
		} catch(Exception ee) {
			try {
				logToFile(textToDisplay, "Main", ee.getMessage());
			} catch(Exception e) {
				//Ignored
			}
		}
		
		try {
			boolean writeStatistics = false;
			Calendar cal = Calendar.getInstance();
			int currentWeek = cal.get(Calendar.WEEK_OF_YEAR);
	    	BufferedReader currentWeekFile = new BufferedReader(new FileReader(textToDisplay + File.separator + "week.txt"));
	    	int oldWeek = Integer.parseInt(currentWeekFile.readLine());
	    	currentWeekFile.close();
			if(oldWeek != currentWeek) {
				writeStatistics = true;
		    	BufferedWriter copyTo = new BufferedWriter(new FileWriter(textToDisplay + File.separator + "week.txt"));
	    		copyTo.write(currentWeek + "\n");
		    	copyTo.close();
			}
			
			BufferedReader input = new BufferedReader(new FileReader(textToDisplay + File.separator + "accounts.txt"));
			String accountFolder = input.readLine();
			while (accountFolder != null) {
				
				try {
					logToFile(textToDisplay, "Main", accountFolder);
					File freezeFile = new File(textToDisplay + File.separator + accountFolder + File.separator + "freeze.txt");
			    	if(!freezeFile.exists()) {

						Twitter twitter = getTwitter(textToDisplay + File.separator + accountFolder);
						if(writeStatistics) {
							File statisticsFile = new File(textToDisplay + File.separator + accountFolder + File.separator + "statistics.txt");
							if(!statisticsFile.exists())
								statisticsFile.createNewFile();
					    	BufferedWriter statisticsFileWriter = new BufferedWriter(new FileWriter(textToDisplay + File.separator + accountFolder + File.separator + "statistics.txt", true));
					    	User user = twitter.showUser(Long.valueOf(twitter.getId()));
					    	statisticsFileWriter.write(new Timestamp(System.currentTimeMillis()) + " Followers: " + user.getFollowersCount() + ", follows: " + user.getFriendsCount() + "\n");
					    	statisticsFileWriter.close();
						}
						nonstopFavouriter(twitter, textToDisplay + File.separator + accountFolder);
						favouriter(twitter, textToDisplay + File.separator + accountFolder);
						retweeter(twitter, textToDisplay + File.separator + accountFolder);
						unfollowerDM(twitter, textToDisplay + File.separator + accountFolder);
						followerDM(twitter, textToDisplay + File.separator + accountFolder);
						listFollower(twitter, textToDisplay + File.separator + accountFolder);
						listUnfollower(twitter, textToDisplay + File.separator + accountFolder);
						tweeter(twitter, textToDisplay + File.separator + accountFolder);
						responder(twitter, textToDisplay + File.separator + accountFolder);
						listTimelineRetweeter(twitter, textToDisplay + File.separator + accountFolder);
						unfollower(twitter, textToDisplay + File.separator + accountFolder);
						follower(twitter, textToDisplay + File.separator + accountFolder);
						destroyfavourite(twitter, textToDisplay + File.separator + accountFolder);
						tweetWithHashtag(twitter, textToDisplay + File.separator + accountFolder);
						searchAndTweet(twitter, textToDisplay + File.separator + accountFolder);
						dmToNonFollowers(twitter, textToDisplay + File.separator + accountFolder);
			    	} else {
			    		File reportedFile = new File(accountFolder + File.separator + "reported.txt");
			    		if(!reportedFile.exists()) {
			    			reportedFile.createNewFile();
				    		Twitter twitter = getTwitter(textToDisplay + File.separator + "ScomerService");
				    		twitter.sendDirectMessage("ScomerService", accountFolder + " frozen...");
			    		}
			    	}
				} catch(Exception ee) {
					logToFile(textToDisplay, "Main", ee.getMessage());
					logToFile("errorlog.txt", textToDisplay, "Main " + accountFolder, ee.getMessage());
				}

				accountFolder = input.readLine();
			}
			input.close();
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println("Exception: " + e.getMessage());
			try {
				logToFile("errorlog.txt", textToDisplay, "Main", e.getMessage());
			} catch(Exception ee) {
				
			}
		}

   }

	private static void dmToNonFollowers(Twitter twitter, String accountFolder) throws Exception {
		File aFile = new File(accountFolder + File.separator + "senddm.txt");
		if (!aFile.exists()) {
			return;
		}

		BufferedReader firstInput = new BufferedReader(new FileReader(accountFolder + File.separator + "senddm.txt"));
		String firstLine = firstInput.readLine();
		String filterString = null;
		if(firstLine.startsWith("#")) {
			if(!timeFilterIsOk(firstLine, accountFolder)) {
				firstInput.close();
				return;
			}
			filterString = firstLine;
			firstLine = firstInput.readLine();
		}

		int currentCounter = Integer.valueOf(firstLine);
		int resetCounter = Integer.valueOf(firstInput.readLine());
		String directMessage = firstInput.readLine();
		currentCounter ++;
		if (currentCounter >= resetCounter) {
			currentCounter = 0;
			String recipientString = firstInput.readLine();
			Long recipient = null;
			if (recipientString != null) {
				try {
					recipient = Long.valueOf(recipientString);
					twitter.sendDirectMessage(recipient, deslashString(directMessage));
					logToFile(accountFolder, "dmToNonFollowers", "Sent direct message to " + recipient);
				} catch (Exception e) {
					logToFile(accountFolder, "dmToNonFollowers", "Error: " + e.getMessage());
					logToFile(accountFolder, "dmToNonFollowers", "Could not send direct message to " + recipient);
				}
			}
		}

		BufferedWriter newInput = new BufferedWriter(new FileWriter(accountFolder + File.separator + "senddm.tmp"));
    	if(filterString != null)
    		newInput.write(filterString + "\n");
		newInput.write(currentCounter + "\n");
		newInput.write(resetCounter + "\n");
		newInput.write(directMessage + "\n");
		String inputLine = firstInput.readLine();
		while (inputLine != null) {
			newInput.write(inputLine + "\n");
			inputLine = firstInput.readLine();
		}
		newInput.close();
		firstInput.close();

		BufferedReader copyFrom = new BufferedReader(new FileReader(accountFolder + File.separator + "senddm.tmp"));
		BufferedWriter copyTo = new BufferedWriter(new FileWriter(accountFolder + File.separator + "senddm.txt"));
		String copyLine = copyFrom.readLine();
		while (copyLine != null) {
			copyTo.write(copyLine + "\n");
			copyLine = copyFrom.readLine();
		}
		copyFrom.close();
		copyTo.close();

		new File(accountFolder + File.separator + "senddm.tmp").delete();
	}

	private static Twitter getTwitter(String accountFolder) throws Exception {
		BufferedReader input = new BufferedReader(new FileReader(accountFolder + File.separator + "account.txt"));
		String oAuthConsumerKey = input.readLine();
		String oAuthConsumerSecret = input.readLine();
		String oAuthAccessToken = input.readLine();
		String oAuthAccessTokenSecret = input.readLine();
		input.close();
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
			.setOAuthConsumerKey(oAuthConsumerKey)
			.setOAuthConsumerSecret(oAuthConsumerSecret)
			.setOAuthAccessToken(oAuthAccessToken)
			.setOAuthAccessTokenSecret(oAuthAccessTokenSecret);
		TwitterFactory tf = new TwitterFactory(cb.build());
		return tf.getInstance();
	}

   
   public static void nonstopFavouriter(Twitter twitter, String accountFolder) throws Exception {
   	File aFile = new File(accountFolder + File.separator + "nonstopFavouriter.txt");
		if(!aFile.exists())
			return;
		
		BufferedReader input = new BufferedReader(new FileReader(accountFolder + File.separator + "nonstopFavouriter.txt"));
		int currentCounter = Integer.valueOf(input.readLine());
		int resetCounter = Integer.valueOf(input.readLine());
		String account = "";
		currentCounter ++;
		if(currentCounter >= resetCounter) {
			currentCounter = 0;
			int counter = 0;
			boolean statusFavourited = false;
			while(!statusFavourited && counter < 5) {
				counter ++;
				account = input.readLine();
				
				if(account != null && !account.equals("")) {
					try {
						User user = twitter.showUser(Long.valueOf(account));
						String username = user.getName();
						Status status = user.getStatus();
						if(status != null) {
							
							logToFile(accountFolder, "nonstopFavouriter", "Favouriting: " + username + ", " + status.getText());
							twitter.createFavorite(status.getId());
							statusFavourited = true;
						} else {
							logToFile(accountFolder, "nonstopFavouriter", "Account: "  + username + ", " + "No tweet found to favourite.");
						}
		    		} catch(Exception e) {
		    			logToFile(accountFolder, "nonstopFavouriter", "Exception in Favouriter: " + e.getMessage());
		    		}
				} else {
					logToFile(accountFolder, "nonstopFavouriter", "No accounts left for nonstopfavouriter");
					aFile.delete();
					warn("@" + twitter.getScreenName() + ": No accounts left for nonstopfavouriter.", accountFolder);
					input.close();
					return;
				}
			}
		}

   	BufferedWriter newInput = new BufferedWriter(new FileWriter(accountFolder + File.separator + "nonstopFavouriter.tmp"));
   	newInput.write(currentCounter + "\n");
   	newInput.write(resetCounter + "\n");
   	String inputLine = input.readLine();
   	while (inputLine != null) {
   		newInput.write(inputLine + "\n");
   		inputLine = input.readLine();
   	}
//   	if(!account.equals(""))
//   		newInput.write(account + "\n");
   	newInput.close();
		input.close();


   	BufferedReader copyFrom = new BufferedReader(new FileReader(accountFolder + File.separator + "nonstopFavouriter.tmp"));
   	BufferedWriter copyTo = new BufferedWriter(new FileWriter(accountFolder + File.separator + "nonstopFavouriter.txt"));
   	String copyLine = copyFrom.readLine();
   	while (copyLine != null) {
   		copyTo.write(copyLine + "\n");
   		copyLine = copyFrom.readLine();
   	}
   	copyFrom.close();
   	copyTo.close();
   	
   	new File(accountFolder + File.separator + "nonstopFavouriter.tmp").delete();

   }
   
	private static void destroyfavourite(Twitter twitter, String accountFolder) throws Exception {
   	File aFile = new File(accountFolder + File.separator + "destroyFavouritedTweets.txt");
		if(!aFile.exists())
			return;

   	BufferedReader copyFrom = new BufferedReader(new FileReader(accountFolder + File.separator + "destroyFavouritedTweets.txt"));
   	int LIMIT = Integer.valueOf(copyFrom.readLine());
   	copyFrom.close();

		//int noOfFavourites = 0;
		int destroyedFavourites = 0;
		int failed = 0;
		ArrayList<Long> favourites = new ArrayList<Long>();
		
		try {
			int totalNoOfFavourites = twitter.showUser(twitter.getId()).getFavouritesCount();
			if(totalNoOfFavourites <= LIMIT)
				return;
			
			Paging aPage = new Paging (1, 200);
			List<Status> statuses;
			for(int i =0; i< 3 ; i ++) {
				statuses = twitter.getFavorites(aPage);

				for (Status favourite : statuses) {
					favourites.add(favourite.getId());
					
				}

				aPage.setPage(aPage.getPage() + 1);
				
			} 
			int removeIndex = favourites.size() / 2;
			int noOfFavouritesToBeRemoved = totalNoOfFavourites - LIMIT;
			if(noOfFavouritesToBeRemoved > 5)
				noOfFavouritesToBeRemoved = 5;
			for (int i = 0; i < noOfFavouritesToBeRemoved; i ++) {
				if(removeIndex < favourites.size()) {
					twitter.destroyFavorite(favourites.get(removeIndex));
					destroyedFavourites ++;
				}
				removeIndex ++;
			}
			} catch(Exception e) {
			logToFile(accountFolder, "destroyfavourite", e.getMessage());
		}
		
		logToFile(accountFolder, "destroyfavourite", "Listed favourites: " + favourites.size());
		logToFile(accountFolder, "destroyfavourite", "Destroyed favourites: " + destroyedFavourites + ", failed: " + failed);


//			Map<String, RateLimitStatus> rateLimitStatus = twitter.getRateLimitStatus();
//			RateLimitStatus status = rateLimitStatus.get("/favorites/list");
//			int remainingListLimit = status.getRemaining();
//			
//			logToFile(accountFolder, "destroyfavourite", "Remaining calls to the list: " + remainingListLimit);
//			
//			if(remainingListLimit > 0) {
//				logToFile(accountFolder, "destroyfavourite", "Getting statuses...");
//				
//				int noOfCalls = 0;
//				List<Status> statuses;
//				Paging aPage = new Paging (1, 200);
//				do {
//					statuses = twitter.getFavorites(aPage);
//
//					noOfCalls ++;
//					for (Status favourite : statuses) {
//						favourites.add(favourite.getId());
//						
//					}
//
//					aPage.setPage(noOfCalls + 1);
////					if(statuses.size() == 0)
////						remainingListLimit = 0;
//					logToFile(accountFolder, "destroyfavourite", "noOfCalls: " + noOfCalls + ", remainingListLimit: " + remainingListLimit + ", favourites.size(): " + favourites.size() + ", LIMIT: " + LIMIT);
//				} while (noOfCalls < remainingListLimit && favourites.size() <= LIMIT);
//				
//			}
//		} catch(Exception e) {
//			logToFile(accountFolder, "destroyfavourite", e.getMessage());
//		}
//
//		if(favourites.size() > LIMIT) {
//			int removeIndex = favourites.size() / 2;
//			int noOfFavouritesToBeRemoved = favourites.size() - LIMIT;
//			if(noOfFavouritesToBeRemoved > 5)
//				noOfFavouritesToBeRemoved = 5;
//			for (int i = 0; i < noOfFavouritesToBeRemoved; i ++) {
//				if(removeIndex < favourites.size()) {
//					twitter.destroyFavorite(favourites.get(removeIndex));
//					destroyedFavourites ++;
//				}
//				removeIndex ++;
//			}
//			logToFile(accountFolder, "destroyfavourite", "removeIndex: " + removeIndex + ", noOfFavouritesToBeRemoved: " + noOfFavouritesToBeRemoved + ", destroyedFavourites: " + destroyedFavourites);
//		} else {
//			logToFile(accountFolder, "destroyfavourite", "favourites.size() is less than LIMIT: " + favourites.size() + ", " + LIMIT);
//		}
//		
//		logToFile(accountFolder, "destroyfavourite", "Listed favourites: " + favourites.size());
//		logToFile(accountFolder, "destroyfavourite", "Destroyed favourites: " + destroyedFavourites + ", failed: " + failed);
	}
   
   public static void favouriter(Twitter twitter, String accountFolder) throws Exception {
   	File aFile = new File(accountFolder + File.separator + "favourites.txt");
		if(!aFile.exists())
			return;
   	
		String myName = twitter.getScreenName();

		BufferedReader input = new BufferedReader(new FileReader(accountFolder + File.separator + "favourites.txt"));
		int currentCounter = Integer.valueOf(input.readLine());
		int resetCounter = Integer.valueOf(input.readLine());
		currentCounter ++;
		if(currentCounter >= resetCounter) {
			currentCounter = 0;
		}
		
		int linesInFile = 0;
   	BufferedWriter tmpInput = new BufferedWriter(new FileWriter(accountFolder + File.separator + "favourites.tmp"));
   	tmpInput.write(currentCounter + "\n");
   	tmpInput.write(resetCounter + "\n");
   	String fileInputLine = input.readLine();
   	while (fileInputLine != null) {
			linesInFile ++;
			tmpInput.write(fileInputLine + "\n");
			fileInputLine = input.readLine();
   	}
   	tmpInput.close();
		input.close();
		
		BufferedReader copyFromFirst = new BufferedReader(new FileReader(accountFolder + File.separator + "favourites.tmp"));
		BufferedWriter copyToFirst = new BufferedWriter(new FileWriter(accountFolder + File.separator + "favourites.txt"));
		String copyLineFirst = copyFromFirst.readLine();
		while (copyLineFirst != null) {
			copyToFirst.write(copyLineFirst + "\n");
			copyLineFirst = copyFromFirst.readLine();
		}
		copyFromFirst.close();
		copyToFirst.close();

		new File(accountFolder + File.separator + "favourites.tmp").delete();


		if(currentCounter == 0) {
			for(int i = 0; i < linesInFile; i ++) {
				BufferedReader replacerReader = new BufferedReader(new FileReader(accountFolder + File.separator + "favourites.txt"));
				replacerReader.readLine();
				replacerReader.readLine();

				String firstLine = replacerReader.readLine();

				BufferedWriter newInput = new BufferedWriter(new FileWriter(accountFolder + File.separator + "favourites.tmp"));
				newInput.write(currentCounter + "\n");
				newInput.write(resetCounter + "\n");
				String inputLine = replacerReader.readLine();
				while (inputLine != null) {
					newInput.write(inputLine + "\n");
					inputLine = replacerReader.readLine();
				}
				newInput.write(firstLine + "\n");
				newInput.close();
				replacerReader.close();

				BufferedReader copyFrom = new BufferedReader(new FileReader(accountFolder + File.separator + "favourites.tmp"));
				BufferedWriter copyTo = new BufferedWriter(new FileWriter(accountFolder + File.separator + "favourites.txt"));
				String copyLine = copyFrom.readLine();
				while (copyLine != null) {
					copyTo.write(copyLine + "\n");
					copyLine = copyFrom.readLine();
				}
				copyFrom.close();
				copyTo.close();

				new File(accountFolder + File.separator + "favourites.tmp").delete();
				Thread.sleep(2000);
				
	        	Query query = new Query("\"" + firstLine + "\"");
		    	File locationFile = new File(accountFolder + File.separator + "location.txt");
		    	if(locationFile.exists()) {
		    		BufferedReader locationReader = new BufferedReader(new FileReader(accountFolder + File.separator + "location.txt"));
		    		String locationString = locationReader.readLine();
		    		query.setGeoCode(new GeoLocation(Double.parseDouble(locationString.split(",")[0]), Double.parseDouble(locationString.split(",")[1])), Double.parseDouble(locationString.split(",")[2]), Query.KILOMETERS);
		    		locationReader.close();
		    	}
	        	
	            QueryResult result;

	            result = twitter.search(query);
	            List<Status> tweets = result.getTweets();
	            
	            if(tweets.size() > 0) {

		            Status tweet = tweets.get(0);
		        	
		        	String reasonTweetIsNotOKToFavourite = canThisTweetBeFavourited(tweet, firstLine, myName, accountFolder);
		        	if(reasonTweetIsNotOKToFavourite == null) {
		        		//System.out.println(new Timestamp(System.currentTimeMillis()) + " Favouriting: " + tweet.getCreatedAt() + " @" + tweet.getUser().getScreenName() + " - " + tweet.getText());
		        		logToFile(accountFolder, "Favouriter", "Favouriting: " + tweet.getCreatedAt() + " @" + tweet.getUser().getScreenName() + " - " + tweet.getText());
		        		try {            		
		        			twitter.createFavorite(tweet.getId());
		        		} catch(Exception e) {
		        			//System.out.println(new Timestamp(System.currentTimeMillis()) + " Exception in Favouriter: " + e.getMessage());
		        			logToFile(accountFolder, "Favouriter", "Exception in Favouriter: " + e.getMessage());
		        		}
		        		//Only save 100 lines
//		        		trimFile(accountFolder + File.separator + "favouritedTweets.txt", tweet.getId() + "", -1);
		        		trimFile(accountFolder + File.separator + "favouritedTweets.txt", tweet.getId() + "", 100);
//		        		BufferedWriter output = new BufferedWriter(new FileWriter(accountFolder + File.separator + "favouritedTweets.txt", true));
//		        		output.write(tweet.getId() + "\n");
//		        		output.close();
		        		
	
		        	} else {
		        		//System.out.println(new Timestamp(System.currentTimeMillis()) + " Tweet not ok to favourite (" + reasonTweetIsNotOKToFavourite + "): " + tweet.getCreatedAt() + " @" + tweet.getUser().getScreenName() + " - " + tweet.getText());
		        		logToFile(accountFolder, "Favouriter", "Tweet not ok to favourite (" + reasonTweetIsNotOKToFavourite + "): " + tweet.getCreatedAt() + " @" + tweet.getUser().getScreenName() + " - " + tweet.getText());
		        	}
	
		        	if(reasonTweetIsNotOKToFavourite == null) {
		        		break;
		        	}
	            } else {
	            	//System.out.println(new Timestamp(System.currentTimeMillis()) + " No tweet found to favourite containing " + firstLine + ".");
	            	logToFile(accountFolder, "Favouriter", "No tweet found to favourite containing " + firstLine + ".");
	            }
	            if(i > 2)
	            	break;
			}
		} else {
   		//System.out.println(new Timestamp(System.currentTimeMillis()) + " Not time to favorite any tweet.");
   		logToFile(accountFolder, "Favouriter", "Not time to favorite any tweet.");
		}
   }

   public static void retweeter(Twitter twitter, String accountFolder) throws Exception {
   	File aFile = new File(accountFolder + File.separator + "retweets.txt");
		if(!aFile.exists())
			return;
   	
		String myName = twitter.getScreenName();

		BufferedReader input = new BufferedReader(new FileReader(accountFolder + File.separator + "retweets.txt"));
		int currentCounter = Integer.valueOf(input.readLine());
		int resetCounter = Integer.valueOf(input.readLine());
		currentCounter ++;
		if(currentCounter >= resetCounter) {
			currentCounter = 0;
		}
		
		int linesInFile = 0;
   	BufferedWriter tmpInput = new BufferedWriter(new FileWriter(accountFolder + File.separator + "retweets.tmp"));
   	tmpInput.write(currentCounter + "\n");
   	tmpInput.write(resetCounter + "\n");
   	String fileInputLine = input.readLine();
   	while (fileInputLine != null) {
			linesInFile ++;
			tmpInput.write(fileInputLine + "\n");
			fileInputLine = input.readLine();
   	}
   	tmpInput.close();
		input.close();
		
		BufferedReader copyFromFirst = new BufferedReader(new FileReader(accountFolder + File.separator + "retweets.tmp"));
		BufferedWriter copyToFirst = new BufferedWriter(new FileWriter(accountFolder + File.separator + "retweets.txt"));
		String copyLineFirst = copyFromFirst.readLine();
		while (copyLineFirst != null) {
			copyToFirst.write(copyLineFirst + "\n");
			copyLineFirst = copyFromFirst.readLine();
		}
		copyFromFirst.close();
		copyToFirst.close();

		new File(accountFolder + File.separator + "retweets.tmp").delete();


		if(currentCounter == 0) {
			for(int i = 0; i < linesInFile; i ++) {
				BufferedReader replacerReader = new BufferedReader(new FileReader(accountFolder + File.separator + "retweets.txt"));
				replacerReader.readLine();
				replacerReader.readLine();

				String firstLine = replacerReader.readLine();

				BufferedWriter newInput = new BufferedWriter(new FileWriter(accountFolder + File.separator + "retweets.tmp"));
				newInput.write(currentCounter + "\n");
				newInput.write(resetCounter + "\n");
				String inputLine = replacerReader.readLine();
				while (inputLine != null) {
					newInput.write(inputLine + "\n");
					inputLine = replacerReader.readLine();
				}
				newInput.write(firstLine + "\n");
				newInput.close();
				replacerReader.close();

				BufferedReader copyFrom = new BufferedReader(new FileReader(accountFolder + File.separator + "retweets.tmp"));
				BufferedWriter copyTo = new BufferedWriter(new FileWriter(accountFolder + File.separator + "retweets.txt"));
				String copyLine = copyFrom.readLine();
				while (copyLine != null) {
					copyTo.write(copyLine + "\n");
					copyLine = copyFrom.readLine();
				}
				copyFrom.close();
				copyTo.close();

				new File(accountFolder + File.separator + "retweets.tmp").delete();
				Thread.sleep(2000);
				
	        	Query query = new Query("\"" + firstLine + "\"");
		    	File locationFile = new File(accountFolder + File.separator + "location.txt");
		    	if(locationFile.exists()) {
		    		BufferedReader locationReader = new BufferedReader(new FileReader(accountFolder + File.separator + "location.txt"));
		    		String locationString = locationReader.readLine();
		    		query.setGeoCode(new GeoLocation(Double.parseDouble(locationString.split(",")[0]), Double.parseDouble(locationString.split(",")[1])), Double.parseDouble(locationString.split(",")[2]), Query.KILOMETERS);
		    		locationReader.close();
		    	}
	        	
	            QueryResult result;

	            result = twitter.search(query);
	            List<Status> tweets = result.getTweets();
	            
	            if(tweets.size() > 0) {

		            Status tweet = tweets.get(0);
		        	
		        	String reasonTweetIsNotOKToFavourite = canThisTweetBeRetweeted(tweet, firstLine, myName, accountFolder);
		        	if(reasonTweetIsNotOKToFavourite == null) {
		        		//System.out.println(new Timestamp(System.currentTimeMillis()) + " Favouriting: " + tweet.getCreatedAt() + " @" + tweet.getUser().getScreenName() + " - " + tweet.getText());
		        		logToFile(accountFolder, "retweets", "retweets: " + tweet.getCreatedAt() + " @" + tweet.getUser().getScreenName() + " - " + tweet.getText());
		        		try {            		
		        			twitter.retweetStatus(tweet.getId());
		        		} catch(Exception e) {
		        			//System.out.println(new Timestamp(System.currentTimeMillis()) + " Exception in Favouriter: " + e.getMessage());
		        			logToFile(accountFolder, "retweets", "Exception in retweets: " + e.getMessage());
		        		}
		        		//Only save 100 lines
//		        		trimFile(accountFolder + File.separator + "favouritedTweets.txt", tweet.getId() + "", -1);
		        		trimFile(accountFolder + File.separator + "retweetedTweets.txt", tweet.getId() + "", 100);
//		        		BufferedWriter output = new BufferedWriter(new FileWriter(accountFolder + File.separator + "favouritedTweets.txt", true));
//		        		output.write(tweet.getId() + "\n");
//		        		output.close();
		        		
	
		        	} else {
		        		//System.out.println(new Timestamp(System.currentTimeMillis()) + " Tweet not ok to favourite (" + reasonTweetIsNotOKToFavourite + "): " + tweet.getCreatedAt() + " @" + tweet.getUser().getScreenName() + " - " + tweet.getText());
		        		logToFile(accountFolder, "retweets", "Tweet not ok to retweets (" + reasonTweetIsNotOKToFavourite + "): " + tweet.getCreatedAt() + " @" + tweet.getUser().getScreenName() + " - " + tweet.getText());
		        	}
	
		        	if(reasonTweetIsNotOKToFavourite == null) {
		        		break;
		        	}
	            } else {
	            	//System.out.println(new Timestamp(System.currentTimeMillis()) + " No tweet found to favourite containing " + firstLine + ".");
	            	logToFile(accountFolder, "retweets", "No tweet found to retweets containing " + firstLine + ".");
	            }
	            if(i > 2)
	            	break;
			}
		} else {
   		//System.out.println(new Timestamp(System.currentTimeMillis()) + " Not time to favorite any tweet.");
   		logToFile(accountFolder, "retweets", "Not time to retweets any tweet.");
		}
   }

   private static void unfollowerDM(Twitter twitter, String accountFolder) throws Exception {
   	File aFile = new File(accountFolder + File.separator + "unfollowdm.txt");
		if(!aFile.exists()) {
			return;
		}

		BufferedReader firstInput = new BufferedReader(new FileReader(accountFolder + File.separator + "unfollowdm.txt"));
		int currentCounter = Integer.valueOf(firstInput.readLine());
		int resetCounter = Integer.valueOf(firstInput.readLine());
		String shouldIUnfollow = "";
		try {
			shouldIUnfollow = firstInput.readLine();
		} catch (Exception e) {
			
		}
		currentCounter ++;
		if(currentCounter >= resetCounter) {
			currentCounter = 0;
		}
		
   	BufferedWriter tmpInput = new BufferedWriter(new FileWriter(accountFolder + File.separator + "unfollowdm.tmp"));
   	tmpInput.write(currentCounter + "\n");
   	tmpInput.write(resetCounter + "\n");
   	tmpInput.write(shouldIUnfollow + "\n");
   	tmpInput.close();
   	firstInput.close();
		
		BufferedReader copyFromFirst = new BufferedReader(new FileReader(accountFolder + File.separator + "unfollowdm.tmp"));
		BufferedWriter copyToFirst = new BufferedWriter(new FileWriter(accountFolder + File.separator + "unfollowdm.txt"));
		String copyLineFirst = copyFromFirst.readLine();
		while (copyLineFirst != null) {
			copyToFirst.write(copyLineFirst + "\n");
			copyLineFirst = copyFromFirst.readLine();
		}
		copyFromFirst.close();
		copyToFirst.close();


   	
		if(currentCounter == 0) {
			
			
			IDs currentFollowerList = null;

			try {
	            long cursor = -1;
				Map<Long, Long> currentFollowersList = new HashMap<Long, Long>();
				
				do {
					currentFollowerList = twitter.getFollowersIDs(cursor);
					long currentFollowersArray[] = currentFollowerList.getIDs();

					for(Long fid : currentFollowersArray) {
						currentFollowersList.put(fid, fid);
					}

	            } while ((cursor = currentFollowerList.getNextCursor()) != 0);
				
				String fileName = accountFolder + File.separator + "allFollowers.txt";
				File file = new File(fileName);
				if(!file.exists())
					file.createNewFile();
					
				BufferedReader allFollowers = new BufferedReader(new FileReader(accountFolder + File.separator + "allFollowers.txt"));
		    	String followerId = allFollowers.readLine();
		    	int unfollowedAccounts = 0;
		    	while (followerId != null && unfollowedAccounts < 10) {
		    		if(currentFollowersList.get(Long.valueOf(followerId)) == null) {
		    			if(!userIsOnWhiteList(twitter, accountFolder, Long.valueOf(followerId))) {
			    			unfollowedAccounts ++;
		    				boolean followerExist = true; 
							try {
								String userName = followerId;
								
								try {
									userName = twitter.showUser(Long.valueOf(followerId)).getScreenName();
								} catch(Exception e) {
									followerExist = false;
									//ignored
								}
								File logUnfollowersFile = new File(accountFolder + File.separator + "unfollowedAccount.txt");
						    	if(!logUnfollowersFile.exists()) {
									trimFile(accountFolder + File.separator + "unfollowedAccount.txt", new Timestamp(System.currentTimeMillis()) + " @" + userName + " unfollowed you.", -1);
						    	}

								twitter.sendDirectMessage(twitter.showUser(Long.valueOf(twitter.getId())).getScreenName(), "@" + userName + " unfollowed you.");
								//trimFile(accountFolder + File.separator + "unfollowedAccount.txt", new Timestamp(System.currentTimeMillis()) + " @" + userName + " unfollowed you.", -1);
								logToFile(accountFolder, "unfollowDM", "@" + userName + " unfollowed you.");
							} catch(TwitterException te) {
								//twitter.sendDirectMessage(twitter.showUser(Long.valueOf(twitter.getId())).getScreenName(), "An account that followed you was suspended, account id: " + followerId);
								//trimFile(accountFolder + File.separator + "unfollowedAccount.txt", new Timestamp(System.currentTimeMillis()) + " An account that followed you was suspended, account id: " + followerId, -1);
								logToFile(accountFolder, "unfollowDM", "An account that followed you was suspended, account id: " + followerId);
							} catch(Exception e) {
								logToFile(accountFolder, "unfollowDM", "Exception when trying to send dm, exception: " + e.getMessage());
							}
			    			if(!shouldIUnfollow.equals("") && followerExist) {
								try {
				    				logToFile(accountFolder, "unfollowDM", "Unfollowed: " + followerId);
									twitter.destroyFriendship(Long.valueOf(followerId));
								} catch (Exception e) {
				    				logToFile(accountFolder, "unfollowDM", "Exception: " + e.getMessage());
								}
			    			}
		    			}
		    		}
		    		
					followerId = allFollowers.readLine();
		    	}
		    	allFollowers.close();

		    	
		    	BufferedWriter theOutput = new BufferedWriter(new FileWriter(accountFolder + File.separator + "allFollowers.txt"));
		    	
		    	for (Map.Entry<Long, Long> entry : currentFollowersList.entrySet())
		    	{
		            theOutput.write(entry.getKey() + "" + "\n");
		    	}
		    	
//		    	Iterator it = currentFollowersList.entrySet().iterator();
//		        while (it.hasNext()) {
//		            Map.Entry pairs = (Map.Entry)it.next();
//		            theOutput.write(pairs.getKey() + "" + "\n");
//		            it.remove(); // avoids a ConcurrentModificationException
//		        }
//		    	
//				for(Long fid : currentFollowersList) {
//			    		theOutput.write(fid + "" + "\n");
//				}
		    	theOutput.close();

			} catch(TwitterException te) {
				logToFile(accountFolder, "followDM", "Exception when trying to send dm, exception: " + te.getMessage());
				if (te.getErrorCode() == 161 || te.getErrorCode() == 226) {
					new File(accountFolder + File.separator + "freeze.txt").createNewFile();
				}
			
			} catch (Exception e) {
				logToFile(accountFolder, "unfollowDM", "Exception: " + e.getMessage());
			}
			
		}

		new File(accountFolder + File.separator + "unfollowdm.tmp").delete();

}

   private static boolean userIsOnWhiteList(Twitter twitter, String accountFolder, Long followerId) {
		try {
	    	Long idOfList = null;
			List<UserList> listOfLists = twitter.getUserLists(twitter.getId());
			for(UserList userList : listOfLists) {
				logToFile(accountFolder, "unfollowDM", "Listname: " + userList.getName());
				if(userList.getName().toLowerCase().equals("nonstopfollowing")) {
					idOfList = userList.getId();
					
//					int listMembers = 0;
//					Long firstMember = null;
		            try {
		                long cursor = -1;
		                PagableResponseList<User> ids;
		                
		            	do {
		                	ids = twitter.getUserListMembers(idOfList, cursor);
		                    for (User id : ids) {
		                    	if(followerId.equals(id.getId())) {
		                    		logToFile(accountFolder, "unfollowDM", "User is on white list");
		                    		return true;
		                    	}
//		                    	listMembers ++;
		                    }
		                } while ((cursor = ids.getNextCursor()) != 0);
		                
		                
		            } catch (Exception e) {
						logToFile(accountFolder, "unfollowDM", "Exception: " + e.getMessage());
		            }

					
				}
			}
			logToFile(accountFolder, "unfollowDM", "User is not on white list");
		} catch (Exception e) {
			
		}
		return false;
	}

	private static void followerDM(Twitter twitter, String accountFolder) throws Exception {
   	File aFile = new File(accountFolder + File.separator + "followdm.txt");
		if(!aFile.exists()) {
			return;
		}

		BufferedReader firstInput = new BufferedReader(new FileReader(accountFolder + File.separator + "followdm.txt"));
		String firstLine = firstInput.readLine();
		String filterString = null;
		if(firstLine.startsWith("#")) {
			if(!timeFilterIsOk(firstLine, accountFolder)) {
				firstInput.close();
				return;
			}
			filterString = firstLine;
			firstLine = firstInput.readLine();
		}
		int currentCounter = Integer.valueOf(firstLine);
		int resetCounter = Integer.valueOf(firstInput.readLine());
		String response = "";
		String shouldIFollowBack = "";
		
		try {
			response = firstInput.readLine();
			shouldIFollowBack = firstInput.readLine();
		} catch(Exception e) {
			//ignored, something is missing
			
		}
		
		currentCounter ++;
		if(currentCounter >= resetCounter) {
			currentCounter = 0;
		}
		
   	BufferedWriter tmpInput = new BufferedWriter(new FileWriter(accountFolder + File.separator + "followdm.tmp"));
	if(filterString != null)
		tmpInput.write(filterString + "\n");
   	tmpInput.write(currentCounter + "\n");
   	tmpInput.write(resetCounter + "\n");
   	tmpInput.write(response + "\n");
   	if(shouldIFollowBack != null)
   		tmpInput.write(shouldIFollowBack + "\n");
   	tmpInput.close();
   	firstInput.close();
		
		BufferedReader copyFromFirst = new BufferedReader(new FileReader(accountFolder + File.separator + "followdm.tmp"));
		BufferedWriter copyToFirst = new BufferedWriter(new FileWriter(accountFolder + File.separator + "followdm.txt"));
		String copyLineFirst = copyFromFirst.readLine();
		while (copyLineFirst != null) {
			copyToFirst.write(copyLineFirst + "\n");
			copyLineFirst = copyFromFirst.readLine();
		}
		copyFromFirst.close();
		copyToFirst.close();

		//Kolla ratelimits
		Map<String ,RateLimitStatus> rateLimitStatus = twitter.getRateLimitStatus();
		RateLimitStatus statusForGettingFollowersIDs = rateLimitStatus.get("/followers/ids");
		if(statusForGettingFollowersIDs.getRemaining() < 8) {
			logToFile(accountFolder, "followDM", "Ratelimit for getting followers ids too low. Need 8, got " + statusForGettingFollowersIDs.getRemaining());
			new File(accountFolder + File.separator + "followdm.tmp").delete();
			return;
		}
		RateLimitStatus statusForGettingUserName = rateLimitStatus.get("/users/show/:id");
		if(statusForGettingUserName.getRemaining() < 10) {
			logToFile(accountFolder, "followDM", "Ratelimit for getting user name too low. Need 10, got " + statusForGettingUserName.getRemaining());
			new File(accountFolder + File.separator + "followdm.tmp").delete();
			return;
		}
		
		logToFile(accountFolder, "followDM", "currentCounter: " + currentCounter);
		if(currentCounter == 0) {
			
			IDs currentFollowerList = null;
			int realNumberOfFollowers = -1;
			int countedNumberOfFollowers = 0;
			try {
				Map<Long, Long> currentFollowersList = new HashMap<Long, Long>();

				String fileName = accountFolder + File.separator + "allFollowersForDM.txt";
				File file = new File(fileName);
				if(!file.exists())
					file.createNewFile();

				BufferedReader allFollowers = new BufferedReader(new FileReader(accountFolder + File.separator + "allFollowersForDM.txt"));
				
		    	Long followerId = null;
		    	boolean firstTime = false;
//		    	int oldFollowersCount = 0;
		
		    	try {
		    		followerId = Long.valueOf(allFollowers.readLine());
		    	} catch(Exception e) {
		    		firstTime = true;
		    	}
		    	while (followerId != null) {
//		    		oldFollowersCount ++;
					currentFollowersList.put(followerId, followerId);
		    		
			    	followerId = null;
			    	try {
			    		followerId = Long.valueOf(allFollowers.readLine());
			    	} catch (Exception e) {
			    		
			    	}
		    	}
		    	allFollowers.close();

		    	boolean sendDm = true;
		    	realNumberOfFollowers = twitter.showUser(Long.valueOf(twitter.getId())).getFollowersCount();
		    	
//		    	if(realNumberOfFollowers > (oldFollowersCount + 10))
//		    		if(!firstTime)
//		    			sendDm = false;
		    	
				long cursor = -1;
				
		    	BufferedWriter theOutput = new BufferedWriter(new FileWriter(accountFolder + File.separator + "allFollowersForDM.txt", true));
				do {
					currentFollowerList = twitter.getFollowersIDs(cursor);
					long currentFollowersArray[] = currentFollowerList.getIDs();
					int sentdms = 0;

					for(Long fid : currentFollowersArray) {
						countedNumberOfFollowers ++;
			    		if(currentFollowersList.get(Long.valueOf(fid)) == null) {
				            theOutput.write(fid + "" + "\n");
							try {
								if(!firstTime) {
									if(!response.equals("")) {
										if(sendDm && sentdms < 50) {
											User user = twitter.showUser(fid);
											if(checkUserDescriptionForKeyword(user, accountFolder)) {
												String userName = user.getScreenName();
												String message = response.replace("FOLLOWER", "@" + userName);
												logToFile(accountFolder, "followDM", "Sending DM: " + message);
												sentdms ++;
												twitter.sendDirectMessage(userName, deslashString(message));
											}
										}
									}
									if(shouldIFollowBack != null && !shouldIFollowBack.equals("")) {
										if(sendDm && sentdms < 50) {
											logToFile(accountFolder, "followDM", "Followed back: " + fid);
											twitter.createFriendship(fid);
										}
									}
								}
								//trimFile(accountFolder + File.separator + "unfollowedAccount.txt", new Timestamp(System.currentTimeMillis()) + " @" + userName + " unfollowed you.", -1);
							} catch(TwitterException te) {
								logToFile(accountFolder, "followDM", "Exception when trying to send dm, exception: " + te.getMessage());
								if (te.getErrorCode() == 161) {
									new File(accountFolder + File.separator + "freeze.txt").createNewFile();
									break;
								}
								//twitter.sendDirectMessage(twitter.showUser(Long.valueOf(twitter.getId())).getScreenName(), "An account that followed you was suspended, account id: " + followerId);
								//trimFile(accountFolder + File.separator + "unfollowedAccount.txt", new Timestamp(System.currentTimeMillis()) + " An account that followed you was suspended, account id: " + followerId, -1);
							} catch(Exception e) {
								logToFile(accountFolder, "followDM", "Exception when trying to send dm, exception: " + e.getMessage());
							}
			    		}
					}

	            } while ((cursor = currentFollowerList.getNextCursor()) != 0);
		    	theOutput.close();
				

			} catch (NumberFormatException nfe) {
				logToFile(accountFolder, "followDM", "NumberFormatException: Could not read allfollowers file: " + nfe.getMessage());
			} catch (Exception e) {
				logToFile(accountFolder, "followDM", "Exception: " + e.getMessage());
				
			}
			logToFile(accountFolder, "followDM", "realNumberOfFollowers: " + realNumberOfFollowers);
			logToFile(accountFolder, "followDM", "countedNumberOfFollowers: " + countedNumberOfFollowers);
//			if(realNumberOfFollowers != countedNumberOfFollowers) {
//				new File(accountFolder + File.separator + "allFollowersForDM.txt").delete();
//				new File(accountFolder + File.separator + "allFollowersForDM.txt").createNewFile();
//			}
			
		}

		new File(accountFolder + File.separator + "followdm.tmp").delete();

   }

	private static void listFollower(Twitter twitter, String accountFolder) throws Exception {
   	File aFile = new File(accountFolder + File.separator + "listFollow.txt");
		if(!aFile.exists())
			return;
		
		try {
			BufferedReader input = new BufferedReader(new FileReader(accountFolder + File.separator + "listFollow.txt"));
			String firstLine = input.readLine();
			String filterString = null;
			if(firstLine.startsWith("#")) {
				if(!timeFilterIsOk(firstLine, accountFolder)) {
					input.close();
					return;
				}
				filterString = firstLine;
				firstLine = input.readLine();
			}
			int currentCounter = Integer.valueOf(firstLine);
			int resetCounter = Integer.valueOf(input.readLine());
			String accountToFollow = "";
			currentCounter ++;
			if(currentCounter >= resetCounter) {
				currentCounter = 0;
				accountToFollow = input.readLine();
				if(accountToFollow == null) {
					input.close();
					return;
				}
			}
	
	    	BufferedWriter newInput = new BufferedWriter(new FileWriter(accountFolder + File.separator + "listFollow.tmp"));
	    	if(filterString != null)
	    		newInput.write(filterString + "\n");
	    	newInput.write(currentCounter + "\n");
	    	newInput.write(resetCounter + "\n");
	    	String inputLine = input.readLine();
	    	while (inputLine != null) {
	    		newInput.write(inputLine + "\n");
	    		inputLine = input.readLine();
	    	}
	    	newInput.close();
			input.close();
	
			if(!accountToFollow.equals("")) {
	
		    	logToFile(accountFolder, "ListFollower", " Following: " + accountToFollow);
				try {
					File unfollowFile = new File(accountFolder + File.separator + "listUnFollow.txt");
					if(unfollowFile.exists()) {
				    	BufferedWriter unFollowFile = new BufferedWriter(new FileWriter(accountFolder + File.separator + "listUnFollow.txt", true));
				    	unFollowFile.write(System.currentTimeMillis() + "," + accountToFollow + "," + twitter.showUser(Long.valueOf(accountToFollow)).getScreenName() + "\n");
				    	unFollowFile.close();
					}
			    	if(!twitter.showUser(Long.valueOf(accountToFollow)).isFollowRequestSent()) {
			    		twitter.createFriendship(Long.valueOf(accountToFollow));
			    		twitter.createMute(Long.valueOf(accountToFollow));
			    	} else
			    		logToFile(accountFolder, "ListFollower", "We have already asked for follow permission for this account.");
				} catch(Exception e) {
					//System.out.println(new Timestamp(System.currentTimeMillis()) + " Exception in Tweeter when tweeting: " + e.getMessage());
					logToFile(accountFolder, "ListFollower", "Exception in ListFollower when following: " + e.getMessage());
				}
				
			}			
	
	    	BufferedReader copyFrom = new BufferedReader(new FileReader(accountFolder + File.separator + "listFollow.tmp"));
	    	BufferedWriter copyTo = new BufferedWriter(new FileWriter(accountFolder + File.separator + "listFollow.txt"));
	    	String copyLine = copyFrom.readLine();
	    	while (copyLine != null) {
	    		copyTo.write(copyLine + "\n");
	    		copyLine = copyFrom.readLine();
	    	}
	    	copyFrom.close();
	    	copyTo.close();
	    	
	    	new File(accountFolder + File.separator + "listFollow.tmp").delete();
		} catch(Exception e) {
			logToFile(accountFolder, "ListFollower", "Exception in ListFollower when following: " + e.getMessage());
			throw e;
		}
	}

   public static boolean timeFilterIsOk(String inputLine, String accountFolder) throws Exception {
   	try {
			Calendar cal = Calendar.getInstance();
			int currentDay = cal.get(Calendar.DAY_OF_WEEK);
			if(inputLine.charAt(currentDay) == '1') {
				int currentHour = cal.get(Calendar.HOUR_OF_DAY);
				int currentMinute = cal.get(Calendar.MINUTE);
				int currentTime = currentHour * 100 + currentMinute;
				int filterTimeMin = Integer.valueOf(inputLine.substring(9, 13));
				int filterTimeMax = Integer.valueOf(inputLine.substring(14, 18));
				if(currentTime >= filterTimeMin && currentTime <= filterTimeMax)
					return true;
				else
					return false;
			}
   	} catch(Exception e) {
   		e.printStackTrace();
   		//logToFile(accountFolder, "timeFilterIsOk", "Input string not correctly formatted, expecting: .");
   		return false;
   	}
		return false;
	}

	private static void listUnfollower(Twitter twitter, String accountFolder) throws Exception {
   	File aFile = new File(accountFolder + File.separator + "listUnFollow.txt");
		if(!aFile.exists()) {
			//logToFile(accountFolder, "ListUnFollower", "listUnFollow.txt not found.");
			return;
		}
			
		
   	

   	try {
			logToFile(accountFolder, "ListUnFollower", "Time to check for unfollowers.");
   	
			BufferedReader input = new BufferedReader(new FileReader(accountFolder + File.separator + "listUnFollow.txt"));
	
			String accountToUnFollowString = input.readLine();
	
	    	BufferedWriter newInput = new BufferedWriter(new FileWriter(accountFolder + File.separator + "listUnFollow.tmp"));
	    	
	    	Long timestamp = Long.valueOf(accountToUnFollowString.split(",")[0]);
	    	Long accountId = Long.valueOf(accountToUnFollowString.split(",")[1]);
	    	String accountName = accountToUnFollowString.split(",")[2];
	    	
			logToFile(accountFolder, "ListUnFollower", "Checking " + accountName);

	    	
	    	//Long oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
	    	//long LIMIT_IN_MILLISECONDS = 1000 * 60 * 60 * 24 * 7; //One week
	    	long LIMIT_IN_MILLISECONDS = 1000 * 60 * 60 * 24 * 4; //4 days
	    	
			logToFile(accountFolder, "ListUnFollower", "Is " + (timestamp + LIMIT_IN_MILLISECONDS) + " less than " + System.currentTimeMillis() + "?");

	    	if(timestamp + LIMIT_IN_MILLISECONDS < System.currentTimeMillis()) {
	    		//Time to check if we shall unfollow
				logToFile(accountFolder, "ListUnFollower", "Yes.");

	    		
	        	if(isUserFollowingMe(twitter, accountId, accountFolder)) {
			    	logToFile(accountFolder, "ListUnFollower", "User " + accountName + " (" + accountId + ") followed back");
	
	        	} else {
			    	logToFile(accountFolder, "ListUnFollower", "User " + accountName + " (" + accountId + ") did not follow back");
	        		//Unfollow person
			    	try {
			    		twitter.destroyFriendship(accountId);
			    	} catch (Exception e) {
			    		logToFile(accountFolder, "ListUnFollower", "Exception in Unfollower: " + e.getMessage());
			    	}
	        	}
	
	    	} else {
	    		newInput.write(accountToUnFollowString + "\n");
				logToFile(accountFolder, "ListUnFollower", "No.");
	    	}
	    	
	    	String inputLine = input.readLine();
	    	while (inputLine != null) {
	    		newInput.write(inputLine + "\n");
	    		inputLine = input.readLine();
	    	}
	    	newInput.close();
			input.close();
	
	    	BufferedReader copyFrom = new BufferedReader(new FileReader(accountFolder + File.separator + "listUnFollow.tmp"));
	    	BufferedWriter copyTo = new BufferedWriter(new FileWriter(accountFolder + File.separator + "listUnFollow.txt"));
	    	String copyLine = copyFrom.readLine();
	    	while (copyLine != null) {
	    		copyTo.write(copyLine + "\n");
	    		copyLine = copyFrom.readLine();
	    	}
	    	copyFrom.close();
	    	copyTo.close();
	    	
	    	new File(accountFolder + File.separator + "listUnFollow.tmp").delete();
   	} catch(Exception e) {
   		logToFile(accountFolder, "ListUnFollower", "Exception in Unfollower: " + e.getMessage());
   	}
	}

   public static void tweeter(Twitter twitter, String accountFolder) throws Exception {
   	File aFile = new File(accountFolder + File.separator + "tweets.txt");
		if(!aFile.exists())
			return;
		
		BufferedReader input = new BufferedReader(new FileReader(accountFolder + File.separator + "tweets.txt"));
		int currentCounter = Integer.valueOf(input.readLine());
		int resetCounter = Integer.valueOf(input.readLine());
		String tweet = "";
		currentCounter ++;
		if(currentCounter >= resetCounter) {
			currentCounter = 0;
			tweet = input.readLine();
		}

   	BufferedWriter newInput = new BufferedWriter(new FileWriter(accountFolder + File.separator + "tweets.tmp"));
   	newInput.write(currentCounter + "\n");
   	newInput.write(resetCounter + "\n");
   	String inputLine = input.readLine();
   	while (inputLine != null) {
   		newInput.write(inputLine + "\n");
   		inputLine = input.readLine();
   	}
   	if(!tweet.equals(""))
   		newInput.write(tweet + "\n");
   	newInput.close();
		input.close();
		
		if(!tweet.equals("")) {
			
	    	File receiverFile = new File(accountFolder + File.separator + "receivers.txt");
			String receiver = "";
			if(receiverFile.exists()) {

				BufferedReader receiverInput = new BufferedReader(new FileReader(accountFolder + File.separator + "receivers.txt"));
				receiver = receiverInput.readLine();
	
		    	BufferedWriter receiverOutput = new BufferedWriter(new FileWriter(accountFolder + File.separator + "receivers.tmp"));
		    	String nextReceiver = receiverInput.readLine();
		    	while (nextReceiver != null) {
		    		receiverOutput.write(nextReceiver + "\n");
		    		nextReceiver = receiverInput.readLine();
		    	}
		    	receiverOutput.close();
		    	receiverInput.close();
		    	
		    	BufferedReader copyFrom = new BufferedReader(new FileReader(accountFolder + File.separator + "receivers.tmp"));
		    	BufferedWriter copyTo = new BufferedWriter(new FileWriter(accountFolder + File.separator + "receivers.txt"));
		    	String copyLine = copyFrom.readLine();
		    	while (copyLine != null) {
		    		copyTo.write(copyLine + "\n");
		    		copyLine = copyFrom.readLine();
		    	}
		    	copyFrom.close();
		    	copyTo.close();
		    	
		    	new File(accountFolder + File.separator + "receivers.tmp").delete();

			}
			
			
			
			
			
			logToFile(accountFolder, "Tweeter", " Tweeting: " + tweet);
			try {
				File image = null;
				if(tweet.indexOf("IMAGE") != -1) {
					
					int imageStartIndex = tweet.indexOf("IMAGE"); 
					String imageName = tweet.substring(imageStartIndex);
					//tweet = tweet.replace("IMAGE", "");
					tweet = tweet.replace(imageName, "");
					
					image = new File(accountFolder + File.separator + imageName + ".jpg");
				}
				
				if(!receiver.equals("")) {
					String username = "@" + twitter.showUser(Long.valueOf(receiver)).getScreenName();
					tweet = username + " " + tweet;
				}
				
				StatusUpdate status = new StatusUpdate(deslashString(tweet));
				if(image != null)
					status.setMedia(image);
				twitter.updateStatus(status);
				
			} catch(Exception e) {
				logToFile(accountFolder, "Tweeter", "Exception in Tweeter when tweeting: " + e.getMessage());
			}
			
		}			

   	BufferedReader copyFrom = new BufferedReader(new FileReader(accountFolder + File.separator + "tweets.tmp"));
   	BufferedWriter copyTo = new BufferedWriter(new FileWriter(accountFolder + File.separator + "tweets.txt"));
   	String copyLine = copyFrom.readLine();
   	while (copyLine != null) {
   		copyTo.write(copyLine + "\n");
   		copyLine = copyFrom.readLine();
   	}
   	copyFrom.close();
   	copyTo.close();
   	
   	new File(accountFolder + File.separator + "tweets.tmp").delete();
   	
   }
   
   public static void responder(Twitter twitter, String accountFolder) throws Exception {
   	File aFile = new File(accountFolder + File.separator + "responses.txt");
		if(!aFile.exists())
			return;

		BufferedReader firstInput = new BufferedReader(new FileReader(accountFolder + File.separator + "responses.txt"));
		int currentCounter = Integer.valueOf(firstInput.readLine());
		int resetCounter = Integer.valueOf(firstInput.readLine());
		currentCounter ++;
		if(currentCounter >= resetCounter) {
			currentCounter = 0;
		}
		
   	BufferedWriter tmpInput = new BufferedWriter(new FileWriter(accountFolder + File.separator + "responses.tmp"));
   	tmpInput.write(currentCounter + "\n");
   	tmpInput.write(resetCounter + "\n");
   	String fileInputLine = firstInput.readLine();
   	while (fileInputLine != null) {
			tmpInput.write(fileInputLine + "\n");
			fileInputLine = firstInput.readLine();
   	}
   	tmpInput.close();
   	firstInput.close();
		
		BufferedReader copyFromFirst = new BufferedReader(new FileReader(accountFolder + File.separator + "responses.tmp"));
		BufferedWriter copyToFirst = new BufferedWriter(new FileWriter(accountFolder + File.separator + "responses.txt"));
		String copyLineFirst = copyFromFirst.readLine();
		while (copyLineFirst != null) {
			copyToFirst.write(copyLineFirst + "\n");
			copyLineFirst = copyFromFirst.readLine();
		}
		copyFromFirst.close();
		copyToFirst.close();

		new File(accountFolder + File.separator + "responses.tmp").delete();

   	
		String LOCATION_STRING = "[LOCATION]";
		String FAVOURITE_STRING = "[FAVOURITE]";

   	if(currentCounter == 0) {
   		
			String myName = twitter.getScreenName().toLowerCase();
			
				boolean repliedMention = false;
				List<Status> mentions = twitter.getMentionsTimeline();
				for(Status mention : mentions) {

					if(!mention.getUser().getScreenName().toLowerCase().equals(myName) && !mentionAlreadyRespondedTo(mention, accountFolder)) {
						
						String response = "";

						boolean responseOK = false;
						while(!responseOK) {
							response = getResponse(accountFolder);
					    	if(response.indexOf(LOCATION_STRING) != -1 && (mention.getUser().getLocation() == null || mention.getUser().getLocation().equals(""))) {
					    		responseOK = false;
					    	} else {
					    		response = response.replace(LOCATION_STRING, mention.getUser().getLocation());
					    		responseOK = true;
					    	}
						}

                   	String statusString = "@" + mention.getUser().getScreenName();
						StatusUpdate status = new StatusUpdate(statusString + " " + response);
						status.setInReplyToStatusId(mention.getId());
						
						try {
							//System.out.println(new Timestamp(System.currentTimeMillis()) + " Responding: " + status.getStatus());
							logToFile(accountFolder, "Responder", "Responding: " + status.getStatus());
							
							//Only save 100 lines
							trimFile(accountFolder + File.separator + "respondedMentions.txt", mention.getId() + "", 100);
//							BufferedWriter output = new BufferedWriter(new FileWriter(accountFolder + File.separator + "respondedMentions.txt", true));
//							output.write(mention.getId() + "\n");
//							output.close();
							if(!response.equals(FAVOURITE_STRING))
								twitter.updateStatus(status);
							else
								twitter.createFavorite(mention.getId());
							repliedMention = true;

						} catch (Exception e) {
							//System.out.println(new Timestamp(System.currentTimeMillis()) + " Exception in Responder, retrying: " + e.getMessage());
							logToFile(accountFolder, "Responder", "Exception in Responder, retrying: " + e.getMessage());
						}
					}
				}
				if(!repliedMention)
					//System.out.println(new Timestamp(System.currentTimeMillis()) + " No mentions to respond to.");
					logToFile(accountFolder, "Responder", "No mentions to respond to.");

   		
   		
   	} else {
   		//System.out.println(new Timestamp(System.currentTimeMillis()) + " Not time to respond to any tweet.");
   		logToFile(accountFolder, "Responder", "Not time to respond to any tweet.");
   	}

   }


   public static void listTimelineRetweeter(Twitter twitter, String accountFolder) throws Exception {
   	File aFile = new File(accountFolder + File.separator + "listTimelineRetweets.txt");
		if(!aFile.exists())
			return;
   	
		BufferedReader input = new BufferedReader(new FileReader(accountFolder + File.separator + "listTimelineRetweets.txt"));
		int currentCounter = Integer.valueOf(input.readLine());
		int resetCounter = Integer.valueOf(input.readLine());
		currentCounter ++;
		if(currentCounter >= resetCounter) {
			currentCounter = 0;
		}
		
		int linesInFile = 0;
   	BufferedWriter tmpInput = new BufferedWriter(new FileWriter(accountFolder + File.separator + "listTimelineRetweets.tmp"));
   	tmpInput.write(currentCounter + "\n");
   	tmpInput.write(resetCounter + "\n");
   	String fileInputLine = input.readLine();
   	while (fileInputLine != null) {
			linesInFile ++;
			tmpInput.write(fileInputLine + "\n");
			fileInputLine = input.readLine();
   	}
   	tmpInput.close();
		input.close();
		
		BufferedReader copyFromFirst = new BufferedReader(new FileReader(accountFolder + File.separator + "listTimelineRetweets.tmp"));
		BufferedWriter copyToFirst = new BufferedWriter(new FileWriter(accountFolder + File.separator + "listTimelineRetweets.txt"));
		String copyLineFirst = copyFromFirst.readLine();
		while (copyLineFirst != null) {
			copyToFirst.write(copyLineFirst + "\n");
			copyLineFirst = copyFromFirst.readLine();
		}
		copyFromFirst.close();
		copyToFirst.close();

		new File(accountFolder + File.separator + "listTimelineRetweets.tmp").delete();


		if(currentCounter == 0) {
			for(int i = 0; i < linesInFile; i ++) {
				BufferedReader replacerReader = new BufferedReader(new FileReader(accountFolder + File.separator + "listTimelineRetweets.txt"));
				replacerReader.readLine();
				replacerReader.readLine();

				String firstLine = replacerReader.readLine();

				BufferedWriter newInput = new BufferedWriter(new FileWriter(accountFolder + File.separator + "listTimelineRetweets.tmp"));
				newInput.write(currentCounter + "\n");
				newInput.write(resetCounter + "\n");
				String inputLine = replacerReader.readLine();
				while (inputLine != null) {
					newInput.write(inputLine + "\n");
					inputLine = replacerReader.readLine();
				}
				newInput.write(firstLine + "\n");
				newInput.close();
				replacerReader.close();

				BufferedReader copyFrom = new BufferedReader(new FileReader(accountFolder + File.separator + "listTimelineRetweets.tmp"));
				BufferedWriter copyTo = new BufferedWriter(new FileWriter(accountFolder + File.separator + "listTimelineRetweets.txt"));
				String copyLine = copyFrom.readLine();
				while (copyLine != null) {
					copyTo.write(copyLine + "\n");
					copyLine = copyFrom.readLine();
				}
				copyFrom.close();
				copyTo.close();

				new File(accountFolder + File.separator + "listTimelineRetweets.tmp").delete();
				Thread.sleep(2000);

				String listToRetweetFrom = null;
				String keywordToRetweet = null;
				try {
					listToRetweetFrom = firstLine.split(",")[0];
					keywordToRetweet = firstLine.split(",")[1];
				} catch(Exception e) {
					logToFile(accountFolder, "listTimelineRetweets", "Exception in Retweeter: " + e.getMessage());
					return;
				}
				
				try {
					Long idOfList = null;
					try {
						idOfList = Long.parseLong(listToRetweetFrom);
					} catch(NumberFormatException nfe) {
						List<UserList> listOfLists = twitter.getUserLists(twitter.getId());
						for(UserList userList : listOfLists) {
							logToFile(accountFolder, "listTimelineRetweets", " Comparing: " + userList.getName() + " with " + listToRetweetFrom);
							if(userList.getName().equals(listToRetweetFrom)) {
								idOfList = userList.getId();
								logToFile(accountFolder, "listTimelineRetweets", " Found list!");
							}
						}
					}
					
					if(idOfList == null) {
						logToFile(accountFolder, "listTimelineRetweets", " Did not find list: " + listToRetweetFrom);
						return;
					}
					List<Status> statuses = twitter.getUserListStatuses(idOfList, new Paging(1, 50));
					for(Status tweet : statuses) {
						if(tweet.getText().contains(keywordToRetweet)) {
							
							boolean alreadyRetweeted = false;
							BufferedReader retweetedTweets = new BufferedReader(new FileReader(accountFolder + File.separator + "retweetedTweets.txt"));
							String retweetedTweetsLine = retweetedTweets.readLine();
							while (retweetedTweetsLine != null) {
								if(Long.valueOf(retweetedTweetsLine).equals(tweet.getId()))
									alreadyRetweeted = true;
								retweetedTweetsLine = retweetedTweets.readLine();
							}
							retweetedTweets.close();

							
							if(!alreadyRetweeted) {
								try {
				        			trimFile(accountFolder + File.separator + "retweetedTweets.txt", tweet.getId() + "", 100);
					        		logToFile(accountFolder, "listTimelineRetweets", "Retweeting: " + tweet.getCreatedAt() + " @" + tweet.getUser().getScreenName() + " - " + tweet.getText());
				        			twitter.retweetStatus(tweet.getId());
				        			i = linesInFile;
				        			
				        			return;
								} catch(Exception e) {
									//Ignored, might be deleted tweet or something
								}
							} else {
								logToFile(accountFolder, "listTimelineRetweets", "Already retweeted: " + tweet.getCreatedAt() + " @" + tweet.getUser().getScreenName() + " - " + tweet.getText());
							}
						}
					}
				} catch(Exception e) {
					logToFile(accountFolder, "listTimelineRetweets", "Exception in Retweeter: " + e.getMessage());
				}
			}				
		} else {
   		//System.out.println(new Timestamp(System.currentTimeMillis()) + " Not time to favorite any tweet.");
   		//logToFile(accountFolder, "TimelineRetweeter", "Not time to retweet any tweet.");
		}
   }

   
   private static void follower(Twitter twitter, String accountFolder) throws Exception {
   	try {
	    	File aFile = new File(accountFolder + File.separator + "follow.txt");
			if(!aFile.exists())
				return;
	
			BufferedReader firstInput = new BufferedReader(new FileReader(accountFolder + File.separator + "follow.txt"));
			int currentCounter = Integer.valueOf(firstInput.readLine());
			int resetCounter = Integer.valueOf(firstInput.readLine());
			currentCounter ++;
			if(currentCounter >= resetCounter) {
				currentCounter = 0;
			}
			
	    	BufferedWriter tmpInput = new BufferedWriter(new FileWriter(accountFolder + File.separator + "follow.tmp"));
	    	tmpInput.write(currentCounter + "\n");
	    	tmpInput.write(resetCounter + "\n");
	    	String fileInputLine = firstInput.readLine();
	    	while (fileInputLine != null) {
				tmpInput.write(fileInputLine + "\n");
				fileInputLine = firstInput.readLine();
	    	}
	    	tmpInput.close();
	    	firstInput.close();
			
			BufferedReader copyFromFirst = new BufferedReader(new FileReader(accountFolder + File.separator + "follow.tmp"));
			BufferedWriter copyToFirst = new BufferedWriter(new FileWriter(accountFolder + File.separator + "follow.txt"));
			String copyLineFirst = copyFromFirst.readLine();
			while (copyLineFirst != null) {
				copyToFirst.write(copyLineFirst + "\n");
				copyLineFirst = copyFromFirst.readLine();
			}
			copyFromFirst.close();
			copyToFirst.close();
	
			new File(accountFolder + File.separator + "follow.tmp").delete();
	
	    	
			if(currentCounter == 0) {
				
				int followed = 0;
				int failed = 0;
	            long cursor1 = -1;
	            long cursor2 = -1;
	            IDs followers = null;
	            IDs friends = null;
	            
	            ArrayList<Long> friendList = new ArrayList<Long>();
				do {
					friends = twitter.getFriendsIDs(cursor2);
					long[] friendsIDs = friends.getIDs();
					for (long friend : friendsIDs) {
						friendList.add(friend);
					}
				} while ((cursor2 = friends.getNextCursor()) != 0);

				do {
					followers = twitter.getFollowersIDs(cursor1);
					long[] followersIDs = followers.getIDs();
			
				
						for (long follower : followersIDs) {
							boolean isFollowing = false;
							for (long friend : friendList) {
								if (friend == follower) {
									isFollowing = true;
									break;
								}
							}
							if (isFollowing || followed > 20 || twitter.showUser(follower).isFollowRequestSent()) {
								//System.out.println("id:" + follower);
							} else {
								try {
									if(userIsNotOnBlacklist(twitter, accountFolder, follower)) {
										logToFile(accountFolder, "Follower", "Followed: " + follower + "...");
										User user = twitter.createFriendship(follower);
										logToFile(accountFolder, "Follower", "Followed: " + user.getName() + "!");
										followed ++;
									}
								} catch(TwitterException te) {
									if (te.getErrorCode() == 161 || te.getErrorCode() == 226) {
										throw te;
									} else
										logToFile(accountFolder, "Follower", "Exception: " + te.getMessage());
								} catch(Exception e) {
									
									logToFile(accountFolder, "Follower", "Exception: " + e.getMessage());
									//Ignored
									//if(te.getErrorCode() != 160) {
										
										break;
									//}
									//160 = private user
									//161 = rate limit exceeded
									//failed ++;
								}
							}
						}
				} while ((cursor1 = followers.getNextCursor()) != 0);
				logToFile(accountFolder, "Follower", "Followed: " + followed + " accounts, failed: " + failed);
			}
				
		} catch(TwitterException te) {
			logToFile(accountFolder, "follower", "Twitter Exception: " + te.getMessage());
			if (te.getErrorCode() == 161 || te.getErrorCode() == 226) {
				new File(accountFolder + File.separator + "freeze.txt").createNewFile();
			}

   	} catch(Exception e) {
   		try {
   			logToFile(accountFolder, "Follower", e.getMessage());	
   		} catch(Exception ee) {
   			//Ignored
   		}
   		
   		//Ignored
   	}
		
	}

   private static boolean userIsNotOnBlacklist(Twitter twitter, String accountFolder, Long followerId) throws Exception {
   	BufferedReader firstInput = new BufferedReader(new FileReader(accountFolder + File.separator + "blacklist.txt"));
   	try {
			
			String readLine = firstInput.readLine();
			while(readLine != null) {
				Long accountToCheck = Long.valueOf(readLine);
				if(accountToCheck.equals(followerId)) {
					firstInput.close();
					return false;
				}
				readLine = firstInput.readLine();
			}
			
   	} catch(Exception e) {
   		//Ignored
   	}
   	firstInput.close();
		return true;
	}

	private static void unfollower(Twitter twitter, String accountFolder) throws Exception {
   	try {
	    	File aFile = new File(accountFolder + File.separator + "unfollow.txt");
			if(!aFile.exists())
				return;
	
			BufferedReader firstInput = new BufferedReader(new FileReader(accountFolder + File.separator + "unfollow.txt"));
			int currentCounter = Integer.valueOf(firstInput.readLine());
			int resetCounter = Integer.valueOf(firstInput.readLine());
			currentCounter ++;
			if(currentCounter >= resetCounter) {
				currentCounter = 0;
			}
			
	    	BufferedWriter tmpInput = new BufferedWriter(new FileWriter(accountFolder + File.separator + "unfollow.tmp"));
	    	tmpInput.write(currentCounter + "\n");
	    	tmpInput.write(resetCounter + "\n");
	    	String fileInputLine = firstInput.readLine();
	    	while (fileInputLine != null) {
				tmpInput.write(fileInputLine + "\n");
				fileInputLine = firstInput.readLine();
	    	}
	    	tmpInput.close();
	    	firstInput.close();
			
			BufferedReader copyFromFirst = new BufferedReader(new FileReader(accountFolder + File.separator + "unfollow.tmp"));
			BufferedWriter copyToFirst = new BufferedWriter(new FileWriter(accountFolder + File.separator + "unfollow.txt"));
			String copyLineFirst = copyFromFirst.readLine();
			while (copyLineFirst != null) {
				copyToFirst.write(copyLineFirst + "\n");
				copyLineFirst = copyFromFirst.readLine();
			}
			copyFromFirst.close();
			copyToFirst.close();
	
			new File(accountFolder + File.separator + "unfollow.tmp").delete();
	
	    	
			if(currentCounter == 0) {
	
				int unfollowCount = 0;
				int failed = 0;
				
				IDs ids = null;
				IDs friends = null;
				long cursor2 = -1;
	            long cursor1 = -1;

				Map<Long, Long> friendsList = new HashMap<Long, Long>();
				do {
					friends = twitter.getFollowersIDs(cursor2);
					long[] friendsIDs = friends.getIDs();
					for (long friend : friendsIDs) {
						friendsList.put(friend, friend);
					}
				} while ((cursor2 = friends.getNextCursor()) != 0);

				
				
				try {
					do {
						ids = twitter.getFriendsIDs(cursor1);
	
						for(Long id : ids.getIDs()) {
							if(friendsList.get(id) == null) {
								try {
									if(!userIsOnWhiteList(twitter, accountFolder, id)) {
										if(unfollowCount < 20) {
											twitter.destroyFriendship(id);
											unfollowCount ++;
										}
									} else {
										logToFile(accountFolder, "Unfollower", "User is on white list: " + id);
									}
								} catch(Exception e) {
									failed ++;
								}
							}
						}
					} while ((cursor1 = ids.getNextCursor()) != 0);
						
					logToFile("followers.txt", accountFolder, "Unfollower", "Unfollowed " + unfollowCount + " unfollowers, failed: " + failed);
					//twitter.updateStatus("@jfwsthlm Unfollowed " + unfollowCount + " unfollowers, " + failed + " failed. Followers: " + friendsArray.length);
					//twitter.sendDirectMessage("jfwsthlm", "Unfollowed " + unfollowCount + " unfollowers, " + failed + " failed. Followers: " + friendsArray.length);

				} catch(TwitterException te) {
					logToFile(accountFolder, "Unfollower", "Twitter Exception: " + te.getMessage());
					if (te.getErrorCode() == 161 || te.getErrorCode() == 226) {
						new File(accountFolder + File.separator + "freeze.txt").createNewFile();
					}
				} catch (Exception e) {
					logToFile(accountFolder, "Unfollower", "Exception: " + e.getMessage());
				}
				


			}
	    	
		} catch(TwitterException te) {
			logToFile(accountFolder, "Unfollower", "Twitter Exception: " + te.getMessage());
			if (te.getErrorCode() == 161 || te.getErrorCode() == 226) {
				new File(accountFolder + File.separator + "freeze.txt").createNewFile();
			}
   	} catch(Exception e) {
   		//Ignored
   	}
		
	}

   
	public static void tweetWithHashtag(Twitter twitter, String accountFolder) throws Exception {
   	File aFile = new File(accountFolder + File.separator + "tweetWithHashtag.txt");
		if(!aFile.exists())
			return;
   	
		//String myName = twitter.getScreenName();
		//logToFile(accountFolder, "tweetWithHashtag", "Logged in as " + myName + ".");

		BufferedReader input = new BufferedReader(new FileReader(accountFolder + File.separator + "tweetWithHashtag.txt"));
		int currentCounter = Integer.valueOf(input.readLine());
		int resetCounter = Integer.valueOf(input.readLine());
		currentCounter ++;
		if(currentCounter >= resetCounter) {
			currentCounter = 0;
		}
		
		int linesInFile = 0;
   	BufferedWriter tmpInput = new BufferedWriter(new FileWriter(accountFolder + File.separator + "tweetWithHashtag.tmp"));
   	tmpInput.write(currentCounter + "\n");
   	tmpInput.write(resetCounter + "\n");
   	String fileInputLine = input.readLine();
   	while (fileInputLine != null) {
			linesInFile ++;
			tmpInput.write(fileInputLine + "\n");
			fileInputLine = input.readLine();
   	}
   	tmpInput.close();
		input.close();
		
		BufferedReader copyFromFirst = new BufferedReader(new FileReader(accountFolder + File.separator + "tweetWithHashtag.tmp"));
		BufferedWriter copyToFirst = new BufferedWriter(new FileWriter(accountFolder + File.separator + "tweetWithHashtag.txt"));
		String copyLineFirst = copyFromFirst.readLine();
		while (copyLineFirst != null) {
			copyToFirst.write(copyLineFirst + "\n");
			copyLineFirst = copyFromFirst.readLine();
		}
		copyFromFirst.close();
		copyToFirst.close();

		new File(accountFolder + File.separator + "tweetWithHashtag.tmp").delete();


		if(currentCounter == 0) {
       	boolean hasTweeted = false;

			for(int i = 0; i < linesInFile; i ++) {
				BufferedReader replacerReader = new BufferedReader(new FileReader(accountFolder + File.separator + "tweetWithHashtag.txt"));
				replacerReader.readLine();
				replacerReader.readLine();

				String firstLine = replacerReader.readLine();

				BufferedWriter newInput = new BufferedWriter(new FileWriter(accountFolder + File.separator + "tweetWithHashtag.tmp"));
				newInput.write(currentCounter + "\n");
				newInput.write(resetCounter + "\n");
				String inputLine = replacerReader.readLine();
				while (inputLine != null) {
					newInput.write(inputLine + "\n");
					inputLine = replacerReader.readLine();
				}
				newInput.write(firstLine + "\n");
				newInput.close();
				replacerReader.close();

				BufferedReader copyFrom = new BufferedReader(new FileReader(accountFolder + File.separator + "tweetWithHashtag.tmp"));
				BufferedWriter copyTo = new BufferedWriter(new FileWriter(accountFolder + File.separator + "tweetWithHashtag.txt"));
				String copyLine = copyFrom.readLine();
				while (copyLine != null) {
					copyTo.write(copyLine + "\n");
					copyLine = copyFrom.readLine();
				}
				copyFrom.close();
				copyTo.close();

				new File(accountFolder + File.separator + "tweetWithHashtag.tmp").delete();
				Thread.sleep(2000);
				
	        	Query query = new Query("\"" + firstLine.split(",")[0] + "\"");
	            QueryResult result;

	            result = twitter.search(query);
	            List<Status> tweets = result.getTweets();
	            
           	
           	//logToFile(accountFolder, "tweetWithHashtag", "Found: " + tweets.size() + " tweets.");
	            for(Status status : tweets) {
	            	//logToFile(accountFolder, "tweetWithHashtag", "Status: " + status.getText());
	            	String hashtag = statusHasOneHashtag(status.getText(), accountFolder);
	            	if(hashtag != null && hashtagHasBeenUsed(hashtag, accountFolder))
	            		hashtag = null;
	            	if(hashtag != null)
	            		//if(!hashtag.matches(".*\\d+.*"))
	            		if(!hashtagHasTwoDigits(hashtag))
	            			hashtag = null;
	            	
	            	if(hashtag != null) {
	            		String tweet = firstLine.substring(firstLine.indexOf(",") + 1);
	            		if(tweet.indexOf('|') != -1) {
	            			String[] randomTweets = tweet.split("\\|");
	            			tweet = randomTweets[getRandomNumber(randomTweets.length - 1)];
	            			tweet = tweet.replace("|", "");        			
	            		}
	            		
	            		tweet = tweet.replace("#HASHTAG", hashtag);
	            		twitter.updateStatus(tweet);
	            		logToFile(accountFolder, "tweetWithHashtag", "Tweeted: " + tweet);
	            		trimFile(accountFolder + File.separator + "TweetedHashtags.txt", hashtag, 100);
	            		hasTweeted = true;
	            	}
	            	if(hasTweeted)
	            		break;
	            }
           	if(hasTweeted)
           		break;
			}
		}	            
   }

	public static int getRandomNumber(int max) {

	    // NOTE: Usually this should be a field rather than a method
	    // variable so that it is not re-seeded every call.
	    Random rand = new Random();
	    int min = 0;
	    
	    // nextInt is normally exclusive of the top value,
	    // so add 1 to make it inclusive
	    int randomNum = rand.nextInt((max - min) + 1) + min;

	    return randomNum;
	}
	
   private static boolean hashtagHasBeenUsed(String hashtag, String accountFolder) {
   	try {
   		BufferedReader input = new BufferedReader(new FileReader(accountFolder + File.separator + "TweetedHashtags.txt"));
   		String inputLine = input.readLine();
   		while (inputLine != null) {
   			if(inputLine.equals(hashtag)) {
   				input.close();
   				return true;
   			}
   			inputLine = input.readLine();
   		}
   		input.close();

   	} catch (Exception e) {
   		return true;
   	}
   	return false;
   }

	private static String statusHasOneHashtag(String status, String accountFolder) {
		try {
	    	String hashtag = null;
			String[] words = status.split(" ");
			for(String word : words) {
				if(word.toLowerCase().indexOf("http") == -1 && word.indexOf('\n') == -1)
					if(word.startsWith("#")) {
						logToFile(accountFolder, "tweetWithHashtag", "Found hashtag: " + word);
						if(hashtag == null) {
							hashtag = word;	
						} else {
							return null;
						}
					}
			}
			return hashtag;
		} catch (Exception e) {
			
		}
		return null;
	}

	private static boolean hashtagHasTwoDigits(String hashtag) {
		int count = 0;
		for (int i = 0, len = hashtag.length(); i < len; i++) {
		    if (Character.isDigit(hashtag.charAt(i))) {
		        count++;
		    }
		}
		return count == 2;
	}
	
	private static boolean isUserFollowingMe(Twitter twitter, long userid, String accountFolder) throws Exception {
		logToFile(accountFolder, "ListUnFollower", "Is user following me?");
       long cursor = -1;
       IDs ids;
       do {
           ids = twitter.getFollowersIDs(cursor);
           for (long id : ids.getIDs()) {
               if(id == userid) {
           		logToFile(accountFolder, "ListUnFollower", "Yes!");

               	return true;
               }
           }
       } while ((cursor = ids.getNextCursor()) != 0);
		logToFile(accountFolder, "ListUnFollower", "No...");

		return false;
	}

	private static boolean checkUserDescriptionForKeyword(User user, String accountFolder) throws Exception {
		File aFile = new File(accountFolder + File.separator + "descriptionkeywords.txt");
		if(!aFile.exists())
			return true;
		

		BufferedReader input = new BufferedReader(new FileReader(accountFolder + File.separator + "descriptionkeywords.txt"));
		String keyword = "";
		keyword = input.readLine();
		while(keyword != null) {
//			if(user.getDescription().toLowerCase().indexOf(keyword.toLowerCase()) != -1) {
			if(user.getDescription().indexOf(keyword) != -1) {
				input.close();
				return true;
			}
			keyword = input.readLine();
		}

		input.close();
		return false;
	}


	private static String canThisTweetBeFavourited(Status tweet, String searchString, String myName, String accountFolder) throws Exception {
		String reason = null;
		
		BufferedReader input = new BufferedReader(new FileReader(accountFolder + File.separator + "favouritedTweets.txt"));
		String inputLine = input.readLine();
		while (inputLine != null) {
			if(Long.valueOf(inputLine).equals(tweet.getId()))
				reason = "Tweet already favourited";
			inputLine = input.readLine();
		}
		input.close();

   	if(tweet.getUser().getScreenName().toLowerCase().equals(myName.toLowerCase())) {
   		reason = "This is tweet from myself.";
   	}
   	if(tweet.getUser().getScreenName().toLowerCase().indexOf(searchString.toLowerCase().replace("#", "")) != -1) {
   		reason = "Username contains " + searchString;
   	}
   	if(tweet.isRetweet()) {
   		reason = "Tweet is retweet..";
   	}
		
		return reason;
	}

	private static String canThisTweetBeRetweeted(Status tweet, String searchString, String myName, String accountFolder) throws Exception {
		String reason = null;
		
		BufferedReader input = new BufferedReader(new FileReader(accountFolder + File.separator + "retweetedTweets.txt"));
		String inputLine = input.readLine();
		while (inputLine != null) {
			if(Long.valueOf(inputLine).equals(tweet.getId()))
				reason = "Tweet already retweeted";
			inputLine = input.readLine();
		}
		input.close();

   	if(tweet.getUser().getScreenName().toLowerCase().equals(myName.toLowerCase())) {
   		reason = "This is tweet from myself.";
   	}
   	if(tweet.getUser().getScreenName().toLowerCase().indexOf(searchString.toLowerCase().replace("#", "")) != -1) {
   		reason = "Username contains " + searchString;
   	}
   	if(tweet.isRetweet()) {
   		reason = "Tweet is retweet..";
   	}
		
		return reason;
	}

	private static void warn(String string, String accountFolder) throws Exception {
		Twitter twitter = getTwitter(accountFolder + File.separator + ".." + File.separator + "jfwsthlm");
		twitter.sendDirectMessage("jfwsthlm", string);
	}

	public static boolean logToFile(String accountFolder, String feature, String message) throws Exception {
		return logToFile("log.txt", accountFolder, feature, message);
	}

	public static boolean logToFile(String filename, String accountFolder, String feature, String message) throws Exception {
		//Only save 100 lines
		trimFile(accountFolder + File.separator + filename, new Timestamp(System.currentTimeMillis()) + "; " + feature + "; " + message + "", 100);
//   	BufferedWriter newInput = new BufferedWriter(new FileWriter(accountFolder + File.separator + filename, true));
//   	newInput.write(new Timestamp(System.currentTimeMillis()) + "; " + feature + "; " + message + "\n");
//   	newInput.close();
   	return true;
	}
	
   private static void trimFile(String fileName, String stringToAdd, int noOfLines) throws Exception {
		int counter = 0;
		BufferedReader replacerReader = new BufferedReader(new FileReader(fileName));
		//String firstLine = replacerReader.readLine();

		BufferedWriter newInput = new BufferedWriter(new FileWriter(fileName + ".tmp"));
		newInput.write(stringToAdd + "\n");
		String inputLine = replacerReader.readLine();
		while (inputLine != null && (counter < noOfLines || noOfLines == -1)) {
			counter ++;
			newInput.write(inputLine + "\n");
			inputLine = replacerReader.readLine();
		}
		newInput.close();
		replacerReader.close();

		BufferedReader copyFrom = new BufferedReader(new FileReader(fileName + ".tmp"));
		BufferedWriter copyTo = new BufferedWriter(new FileWriter(fileName));
		String copyLine = copyFrom.readLine();
		while (copyLine != null) {
			copyTo.write(copyLine + "\n");
			copyLine = copyFrom.readLine();
		}
		copyFrom.close();
		copyTo.close();

		new File(fileName + ".tmp").delete();		
	}
   
   private static String deslashString(String inputString) {
		String outputString = "";
		ArrayList<String> words = new ArrayList<String>();
		int index = 0;
		int oldIndex = 0;
		index = inputString.indexOf("\\u");
		while(index != -1) {
			System.out.println("oldIndex: " + oldIndex);
			System.out.println("index: " + index);
			words.add(inputString.substring(oldIndex, index));
			String hexValue = inputString.substring(index + 2, index + 6);
			System.out.println("hexValue: " + hexValue);
			int hexVal = Integer.parseInt(hexValue, 16);
			words.add("" + (char)hexVal);
			oldIndex = index + 6;
			index = inputString.indexOf("\\u", oldIndex);
		}
		
		words.add(inputString.substring(oldIndex, inputString.length()));
		
		for(int i=0;i< words.size();i ++) {
			outputString += words.get(i);
			
		}
		
		outputString = outputString.replace("\\n", System.getProperty("line.separator"));
		return outputString;
   }

	private static boolean mentionAlreadyRespondedTo(Status mention, String accountFolder) throws Exception {
		boolean tweetAlreadyRespondedTo = false;
		BufferedReader input = new BufferedReader(new FileReader(accountFolder + File.separator + "respondedMentions.txt"));
		String inputLine = input.readLine();
		while (inputLine != null) {
			if(Long.valueOf(inputLine).equals(mention.getId()))
				tweetAlreadyRespondedTo = true;
			inputLine = input.readLine();
		}
		input.close();

		if(tweetAlreadyRespondedTo) {
			return true;
		}
		return false;

	}

	private static String getResponse(String accountFolder) throws Exception {
		BufferedReader input = new BufferedReader(new FileReader(accountFolder + File.separator + "responses.txt"));
		int currentCounter = Integer.valueOf(input.readLine());
		int resetCounter = Integer.valueOf(input.readLine());
		String response = input.readLine();
		
   	BufferedWriter newInput = new BufferedWriter(new FileWriter(accountFolder + File.separator + "responses.tmp"));
   	newInput.write(currentCounter + "\n");
   	newInput.write(resetCounter + "\n");
   	String inputLine = input.readLine();
   	while (inputLine != null) {
   		newInput.write(inputLine + "\n");
   		inputLine = input.readLine();
   	}
   	newInput.write(response + "\n");
   	newInput.close();
		input.close();
		
   	BufferedReader copyFrom = new BufferedReader(new FileReader(accountFolder + File.separator + "responses.tmp"));
   	BufferedWriter copyTo = new BufferedWriter(new FileWriter(accountFolder + File.separator + "responses.txt"));
   	String copyLine = copyFrom.readLine();
   	while (copyLine != null) {
   		copyTo.write(copyLine + "\n");
   		copyLine = copyFrom.readLine();
   	}
   	copyFrom.close();
   	copyTo.close();
   	
   	new File(accountFolder + File.separator + "responses.tmp").delete();
   	
   	return response;
	}

   public static void searchAndTweet(Twitter twitter, String accountFolder) throws Exception {
   	File aFile = new File(accountFolder + File.separator + "searchAndTweet.txt");
		if(!aFile.exists())
			return;
   	
		BufferedReader input = new BufferedReader(new FileReader(accountFolder + File.separator + "searchAndTweet.txt"));
		int currentCounter = Integer.valueOf(input.readLine());
		int resetCounter = Integer.valueOf(input.readLine());
		currentCounter ++;
		if(currentCounter >= resetCounter) {
			currentCounter = 0;
		}
		
		if(currentCounter == 0) {
	    	String fileInputLine = input.readLine();
	    	while(fileInputLine != null) {
	    		
	    		String firstLine = fileInputLine.split(",")[0];
	        	Query query = new Query("\"" + firstLine + "\"");
	            QueryResult result;

	            result = twitter.search(query);
	            List<Status> tweets = result.getTweets();
	            
	            if(tweets.size() > 0) {

		            Status tweet = tweets.get(0);
		            
		        	if(canThisTweetBeRespondedTo(accountFolder, tweet.getId())) {
		        		trimFile(accountFolder + File.separator + "respondedTweets.txt", "" + tweet.getId(), 100);
		        		try {
		        			String response = fileInputLine.substring(fileInputLine.indexOf(",") + 1);
		            		if(response.indexOf('|') != -1) {
		            			String[] randomTweets = response.split("\\|");
		            			response = randomTweets[getRandomNumber(randomTweets.length - 1)];
		            			response = response.replace("|", "");
		            			
		            		}
		            		
		            		

		            		
		            		logToFile(accountFolder, "searchAndTweet", "Tweeting: " + response);
		            		

		        			StatusUpdate status = new StatusUpdate(response);
		        			//status.setInReplyToStatusId(tweet.getId());
		        			twitter.updateStatus(status);
		        		} catch(Exception e) {
		        			//System.out.println(new Timestamp(System.currentTimeMillis()) + " Exception in Favouriter: " + e.getMessage());
		        			logToFile(accountFolder, "searchAndTweet", "Exception in Favouriter: " + e.getMessage());
		        		}
		        	} else {
		        		logToFile(accountFolder, "searchAndTweet", "Tweet already responded to.");
		        		
		        	}
	            } else {
	            	logToFile(accountFolder, "searchAndTweet", "No tweet found to respond to.");
	            }
		        	
		        	
		        fileInputLine = input.readLine();
	    	}
		}
		input.close();
		 

		BufferedReader replacerReader = new BufferedReader(new FileReader(accountFolder + File.separator + "searchAndTweet.txt"));
		replacerReader.readLine();
		replacerReader.readLine();

		BufferedWriter newInput = new BufferedWriter(new FileWriter(accountFolder + File.separator + "searchAndTweet.tmp"));
		newInput.write(currentCounter + "\n");
		newInput.write(resetCounter + "\n");
		String inputLine = replacerReader.readLine();
		while (inputLine != null) {
			newInput.write(inputLine + "\n");
			inputLine = replacerReader.readLine();
		}
		newInput.close();
		replacerReader.close();

		BufferedReader copyFrom = new BufferedReader(new FileReader(accountFolder + File.separator + "searchAndTweet.tmp"));
		BufferedWriter copyTo = new BufferedWriter(new FileWriter(accountFolder + File.separator + "searchAndTweet.txt"));
		String copyLine = copyFrom.readLine();
		while (copyLine != null) {
			copyTo.write(copyLine + "\n");
			copyLine = copyFrom.readLine();
		}
		copyFrom.close();
		copyTo.close();

		new File(accountFolder + File.separator + "searchAndTweet.tmp").delete();

   }
   
   private static boolean canThisTweetBeRespondedTo(String accountFolder, long id) throws Exception {
   	boolean returnValue = true;
       //This tweet has been responded to
       BufferedReader sinput = new BufferedReader(new FileReader(accountFolder + File.separator + "respondedTweets.txt"));
		String sinputLine = sinput.readLine();
		while (sinputLine != null) {
			if(Long.valueOf(sinputLine).equals(id))
				returnValue = false;
			sinputLine = sinput.readLine();
		}
		sinput.close();
		return returnValue;
		
	}


}