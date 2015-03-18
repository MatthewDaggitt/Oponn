package com.mld46.sim.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.sillysoft.lux.Country;

public class ClusterManager
{
	//List<List<Country>> clusters;
	List<List<Country>> mapping;
	List<List<Country>> clustersBorders;
	
	Country [] countries;
	final private int numberOfCountries;
	final private int ID;
	
	public ClusterManager(Country [] countries, int ID)
	{
		numberOfCountries = countries.length;
		this.countries = countries;
		this.ID = ID;
		
		clustersBorders = new ArrayList<List<Country>>();
		mapping = new ArrayList<List<Country>>();
		for(int i = 0; i < numberOfCountries; i++)
		{
			mapping.add(null);
		}
		
		List<Country> borders;
		Stack<Country> toLookAt = new Stack<Country>();
		Country country;
		Country neighbour;
		for(int cc = 0; cc < countries.length; cc++)
		{
			if(countries[cc].getOwner() == ID && mapping.get(cc) == null)
			{
				borders = new ArrayList<Country>();
				toLookAt.clear();
				toLookAt.push(countries[cc]);
				
				while(!toLookAt.isEmpty())
				{
					country = toLookAt.pop();
					
					if(mapping.get(country.getCode()) == null)
					{
						mapping.set(country.getCode(),borders);
						
						for(int nc : country.getAdjoiningCodeList())
						{
							neighbour = countries[nc];
							if(neighbour.getOwner() == ID && mapping.get(nc) == null)
							{
								toLookAt.push(neighbour);
							}
							else if(neighbour.getOwner() != ID && !borders.contains(country))
							{
								borders.add(country);
							}
						}
					}
				}
				clustersBorders.add(borders);
			}
		}
	}
	
	public void countryConquered(Country newCountry)
	{
		List<List<Country>> clustersInvolved = new ArrayList<List<Country>>();
		List<Country> clusterBorders;
		
		for(Country neighbour : newCountry.getAdjoiningList())
		{
			clusterBorders = mapping.get(neighbour.getCode());
			if(neighbour.getOwner() == ID && !clustersInvolved.contains(clusterBorders))
			{
				clustersInvolved.add(clusterBorders);
			}
		}
		clusterBorders = new ArrayList<Country>();
		clusterBorders.add(newCountry);
		clustersInvolved.add(clusterBorders);
		clustersBorders.add(clusterBorders);
		
		List<Country> newBorders = new ArrayList<Country>();
		
		for(List<Country> cluster : clustersInvolved)
		{
			for(Country country : cluster)
			{
				if(mapping.get(country.getCode()) != newBorders)
				{
					mapping.set(country.getCode(),newBorders);
					
					for(Country neighbour : country.getAdjoiningList())
					{
						if(neighbour.getOwner() != ID)
						{
							newBorders.add(country);
							break;
						}
					}
				}
			}
			
			for(int i = 0; i < numberOfCountries; i++)
			{
				if(mapping.get(i) == cluster)
				{
					mapping.set(i,newBorders);
				}
			}
			
			clustersBorders.remove(cluster);
		}
		
		clustersBorders.add(newBorders);
	}

	public List<Country> getClusterBorders(int country)
	{
		return mapping.get(country);
	}
}
