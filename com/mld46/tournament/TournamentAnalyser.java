package com.mld46.tournament;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class TournamentAnalyser
{
	/** Settings **/
	
	private final static double INITIAL_ELO = 1400;
	private final static int ITERATIONS = 100;
	private final static int STANDARD_DEVIATIONS = 2;
	private final static double K1 = 20;
	private final static double K2 = 1;
	private final static int K_TRANSITION = 500;
	
	/** State **/
	
	private static List<String> games = new ArrayList<String>();
	private static Set<String> names = new TreeSet<String>();
	private static double [] points;
	private static int numberOfPlayers;
	
	private static double k;
	
	private static String eloRatingsFile;
	private static String tournamentResultsFile;
	
	public static void analyse(int numberOfPlayers)
	{
		TournamentAnalyser.numberOfPlayers = numberOfPlayers;
		TournamentAnalyser.eloRatingsFile = Tournament.ROOT + "eloResults" + numberOfPlayers + ".txt";
		TournamentAnalyser.tournamentResultsFile = Tournament.ROOT + "gameResults" + numberOfPlayers + ".txt";
				
		points = new double[numberOfPlayers];
		double fraction = numberOfPlayers*(numberOfPlayers-1)/2.0;
		for(int i = 0; i < numberOfPlayers; i++)
		{
			points[i] = i/fraction;
		}
		
		k = K1;

		games.clear();
		names.clear();
		
		readInGames();
		HashMap<String,double [][]> results = calculateMeanSDEloRatings();
		writeOutMeanSDResults(results);
	}
	
	private static void readInGames()
	{
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(new File(tournamentResultsFile)));
			
			String line = br.readLine();
			while(line != null)
			{
				games.add(line);
				
				for(String player : line.split(" "))
				{
					names.add(player);
				}
				
				line = br.readLine();
			}
			br.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private static HashMap<String, double[][]> calculateMeanSDEloRatings()
	{
		int numberOfGames = games.size();
		
		HashMap<String,double[][]> results = new HashMap<String,double[][]>();
		for(String name : names)
		{
			results.put(name,new double[numberOfGames][ITERATIONS]);
		}
		
		List<Double> ratings;
		double [][] collatedResults;
		
		for(int i = 0; i < ITERATIONS; i++)
		{
			HashMap<String, List<Double>> ratingsHistory = calculateEloRatings();
			
			for(String name : names)
			{
				ratings = ratingsHistory.get(name);
				collatedResults = results.get(name);
				for(int j = 0; j < numberOfGames; j++)
				{
					collatedResults[j][i] = ratings.get(j);
				}
			}
			
			System.out.println(i);
		}
		
		HashMap<String,double[][]> finalResults = new HashMap<String,double[][]>();
		for(String name : names)
		{
			collatedResults = results.get(name);
			double [][] meanSTD = new double[numberOfGames][3];
			double mean;
			double std;
			for(int i = 0; i < numberOfGames; i++)
			{
				mean = Stats.calculateMean(collatedResults[i]);
				std = Stats.calculateStandardDeviation(collatedResults[i]);
				
				meanSTD[i][0] = mean - std*STANDARD_DEVIATIONS;
				meanSTD[i][1] = mean;
				meanSTD[i][2] = mean + std*STANDARD_DEVIATIONS;
			}
			finalResults.put(name,meanSTD);
		}
		
		return finalResults;
	}
	
	
	public static HashMap<String,List<Double>> calculateEloRatings()
	{
		Collections.shuffle(games);
		
		HashMap<String, List<Double>> ratingsHistory = new HashMap<String, List<Double>>();
		HashMap<String, Double> currentRatings = new HashMap<String, Double>();
		for(String name : names)
		{
			ratingsHistory.put(name, new ArrayList<Double>(games.size()));
			ratingsHistory.get(name).add(INITIAL_ELO);
			currentRatings.put(name, INITIAL_ELO);
		}
		
		int counter = 1;
		for(String game : games)
		{
			updateRatings(game, currentRatings);
			
			for(String name : names)
			{
				ratingsHistory.get(name).add(currentRatings.get(name));
			}
			
			counter++;
			if(counter == K_TRANSITION)
			{
				k = K2;
			}
		}
		
		return ratingsHistory;
	}
	
	public static void updateRatings(String game, HashMap<String, Double> currentRatings)
	{
		String [] players = game.split(" ");
		
		double [] expectedResults = new double[numberOfPlayers];
		double denominator = numberOfPlayers*(numberOfPlayers-1)/2.0;

		for(int i = 0; i < numberOfPlayers; i++)
		{
			double rating1 = currentRatings.get(players[i]);
				
			for(int j = 0; j < numberOfPlayers; j++)
			{
				if(j != i)
				{
					double rating2 = currentRatings.get(players[j]);
					expectedResults[i] += 1/(1+Math.pow(10,(rating2-rating1)/400));
				}
			}
			expectedResults[i] /= denominator;
		}
		
		for(int i = 0; i < numberOfPlayers; i++)
		{
			currentRatings.put(players[i], currentRatings.get(players[i]) + k*(points[i]-expectedResults[i]));
		}
	}
	
	
	private static void writeOutMeanSDResults(HashMap<String,double[][]> finalResults)
	{
		BufferedWriter bw;
		//List<String> players = new ArrayList<String>(finalResults.keySet());
		String [] players = new String[]{"10000mcst","5000mcst","normalmcst","500mcst","modellingmcst","learningmcst","reaper", "killbot", "evilpixie", "quo", "boscoe", "yakool", "pixie", "cluster", "shaft", "communist", "stinky", "angry"};
		
		int numberOfGames = finalResults.values().iterator().next().length;
		int printSkip = numberOfGames/50;
		
		try
		{
			bw = new BufferedWriter(new FileWriter(new File(eloRatingsFile)));
			
			String line = "";
			for(String name : players)
			{
				line += name + "-L ";
				line += name + "-M ";
				line += name + "-U ";
			}
			bw.write(line);
			bw.newLine();
			
			for(int i = 0; i < numberOfGames; i++)
			{
				line = "";
				double [] results;
				for(String name : players)
				{
					results = finalResults.get(name)[i];
					line += (int)(results[1]-results[0]) + " " + (int)(results[1]) + " " + (int)(results[2]) + " ";
				}
				bw.write(line);
				bw.newLine();
			}
			
			for(String name : players)
			{
				double [] results = finalResults.get(name)[numberOfGames-1];
				System.out.println(name + " " + (int)(results[1]-results[0]) + " " + (int)(results[1]) + " " + (int)(results[2] - results[1])); 
			}
			
			bw.flush();
			bw.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}
