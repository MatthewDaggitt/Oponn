package com.mld46.tournament;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

abstract public class Tournament
{	
	static final protected String ROOT = "C:/Users/Matthew/Documents/Part_II/Project/";
	
	final protected String [] entrants;
	final protected int gamesPerMatch;
	final protected int players;
	
	final protected String resultsFile;
	
	final private List<List<String>> allMatches;
	
	
	public static void main(String [] args)
	{
		//new RealTournament(new String []{"stinky", "angry", "cluster", "killbot", "communist", "boscoe", "evilpixie", "pixie", "quo", "shaft", "yakool", "modellingmcst", "normalmcst"}, 2, 1).run();
		TournamentAnalyser.analyse(2);
	}
	
	public Tournament(String [] entrants, int playersPerGame, int gamesPerMatch)
	{
		this.entrants = entrants;
		this.players = playersPerGame;
		this.gamesPerMatch = gamesPerMatch;
		
		this.resultsFile = ROOT + "gameResults" + players + ".txt";
		
		this.allMatches = getAllMatches();
		Collections.shuffle(allMatches);
	}
	
	abstract protected List<String> playMatch(List<String> players);
	
	public void run()
	{
		List<List<String>> remainingMatches = getRemainingMatches();
		
		int size = allMatches.size();
		
		int counter = 1;

		while(remainingMatches.size() > 0)
		{
			Debug.output(remainingMatches.size() + " matches remain to be played out of " + size);
			
			List<String> match = remainingMatches.get(0);
			String matchName = getMatchName(match);
			
			Debug.output(counter + "/" + remainingMatches.size() + ": " + new Date().toString().substring(11,19) + " " + matchName);
			appendToResultsFile(playMatch(match));
			
			counter++;
			
			remainingMatches = getRemainingMatches();
		}
	}
	
	/************************/
	/** Match calculations **/
	/************************/
	
	protected List<List<String>> getRemainingMatches()
	{
		List<String> results = readInResultsSoFar();
		Collections.sort(results);
		
		List<List<String>> remainingMatches = new ArrayList<List<String>>();
		
		for(List<String> match : allMatches)
		{
			String matchName = getMatchName(match);
			if(!results.contains(matchName))
			{
				remainingMatches.add(match);
			}
		}
		
		Collections.shuffle(remainingMatches);
		return remainingMatches;
	}
	
	private List<List<String>> getAllMatches()
	{
		List<String> choices = new LinkedList<String>();
		for(String name : entrants)
		{
			choices.add(name);
		}
		
		return combine(players, choices);
	}
	
	private List<List<String>> combine(int k, List<String> choices)
	{
		List<List<String>> output = new ArrayList<List<String>>();
		List<List<String>> newOutput;
		
		if(k == 0)
		{
			output.add(new ArrayList<String>(players));
		}
		else
		{
			for(int i = 0; i < choices.size(); i++)
			{
				newOutput = combine(k-1,choices.subList(i+1,choices.size()));
				for(List<String> s : newOutput)
				{
					s.add(choices.get(i));
				}
				output.addAll(newOutput);
			}
		}
		
		return output;
	}
		
	private String getMatchName(List<String> players)
	{
		Collections.sort(players);
		String matchName = "";
		for(String player : players)
		{
			matchName += " " + player;
		}
		matchName = matchName.substring(1);
		return matchName;
	}

	/********/
	/** IO **/
	/********/
	
	protected List<String> readInResultsSoFar()
	{
		List<String> results = null;
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(new File(resultsFile)));
			
			String line = br.readLine();
			
			results = new ArrayList<String>();
			
			while(line != null && !line.equals(""))
			{
				String [] names = line.split(" ");
				List<String> namesList = new ArrayList<String>();
				for(int i = 0; i < players; i++)
				{
					namesList.add(names[i]);
				}
				String name = getMatchName(namesList);
				
				if(!results.contains(name))
				{
					results.add(name);			
				}
				line = br.readLine();
			}
				
			br.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		return results;
	}
	
	public void appendToResultsFile(List<String> output)
	{
		BufferedWriter bw;
		
		try
		{
			bw = new BufferedWriter(new FileWriter(new File(resultsFile),true));
			
			for(String line : output)
			{
				bw.write(line);
				bw.newLine();
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

