package com.mld46.tournament;

import java.util.List;

public class Stats
{
	public static double calculateMean(double [] data)
    {
        double sum = 0.0;
        for(double a : data)
        {
            sum += a;
        }
        return sum/data.length;
    }
	
	public static double calculateStandardDeviation(double [] data)
    {
        double mean = calculateMean(data);
        double sum = 0;
        for(double a :data)
        {
            sum += (mean-a)*(mean-a);
        }
        return Math.sqrt(sum/data.length);    
    }
	
	public static double calculateMean(List<Integer> data)
	{
		double total = 0;
		for(int x : data)
		{
			total += x;
		}
		return total/data.size();
	}
	
	public static double calculateVariance(List<Integer> data)
	{
		double mean = calculateMean(data);
		double total = 0;
		for(int x : data)
		{
			total += (x - mean)*(x-mean);
		}
		return total/data.size();
	}
}
