package com.mld46.tournament;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class MatchStripper
{
	public static void main(String [] args) throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(new File("C:/Users/Matthew/Documents/Part_II/Project/gameResults4.txt")));
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File("C:/Users/Matthew/Downloads/output.txt")));
		
		String line = br.readLine();
		
		while(line != null)
		{
			if(line.contains("1000mcst") && !line.contains("1000mcstM"))
			{
				bw.write(line);
				bw.newLine();
			}
			
			line = br.readLine();
		}
		
		bw.flush();
		br.close();
		bw.close();
	}
}
