package com.mld46.tournament;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LuxLogReader
{	
	final private String logFilePath;
	final private int numberOfPlayers;
	
	public LuxLogReader(String rootPath, int numberOfPlayers)
	{
		this.logFilePath = rootPath + "Lux/Support/log.txt";
		this.numberOfPlayers = numberOfPlayers;
	}
	
	public List<String> readLog()
	{
		BufferedReader br = null;
		File logFile = new File(logFilePath);
		List<String> output = new ArrayList<String>();
		
		try
		{
			br = new BufferedReader(new FileReader(logFile));
			
			String outputLine = null;
			String winner = null;
			
			String line = br.readLine();
			while(line != null)
			{
				if(line.equals("LuxWorld: run()"))
				{
					if(outputLine != null)
					{
						outputLine += winner;
						
						if(outputLine.split(" ").length == numberOfPlayers)
						{
							output.add(outputLine);
						}
						else
						{
							throw new IllegalStateException("Last game not finished!");
						}
					}
					
					outputLine = "";
				}
				else if(line.contains(" was eliminated: position "))
				{
					outputLine += line.split(" ")[0] + " ";
				}
				else if(line.startsWith("GameIsOver -> "))
				{
					winner = line.split(" ")[2];
				}
				
				line = br.readLine();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				br.close();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
		
		return output;
	}
}
