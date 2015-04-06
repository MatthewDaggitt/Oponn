package com.mld46.oponn;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import com.sillysoft.lux.Country;

public class MapStats
{
	private static boolean initialised = false;
	
	private static int numberOfCountries;
	private static int numberOfContinents;

	private static List<List<Integer>> continentCountries;
	private static List<List<Integer>> continentInternalCountries;
	private static List<List<Integer>> continentInternalBorders;
	private static List<List<Integer>> continentExternalBorders;
	
	private static SearchNode [] nodes;
	
	private static boolean [][] validReinforcementMoves;
	private static List<List<Entry<Integer, List<Integer>>>> compulsoryFortificationMoves;
	
	public static void initialise(Country [] countries, int numberOfContinents)
	{
		MapStats.numberOfCountries = countries.length;
		MapStats.numberOfContinents = numberOfContinents;
		
		initialiseContinentLists(countries);
		initialiseNodes(countries);
		initialiseCompulsoryReinforcementMoves(countries);
		initialiseValidReinforcementMoves(countries);
	}
	
	private static void initialiseContinentLists(Country [] countries)
	{
		continentCountries = new ArrayList<List<Integer>>();
		continentInternalCountries = new ArrayList<List<Integer>>();
		continentInternalBorders = new ArrayList<List<Integer>>();
		continentExternalBorders = new ArrayList<List<Integer>>();
		
		for(int i = 0; i < numberOfContinents; i++)
		{
			continentCountries.add(new ArrayList<Integer>());
			continentInternalCountries.add(new ArrayList<Integer>());
			continentInternalBorders.add(new ArrayList<Integer>());
			continentExternalBorders.add(new ArrayList<Integer>());
		}
		
		int cContinent;
		int cc, nc;
		boolean internalCountry;
		
		for(Country c : countries)
		{
			cc = c.getCode();
			cContinent = c.getContinent();
			continentCountries.get(cContinent).add(cc);
			
			internalCountry = true;
			
			for(Country n : c.getAdjoiningList())
			{
				if(cContinent != n.getContinent())
				{
					if(!continentInternalBorders.get(cContinent).contains(cc))
					{
						continentInternalBorders.get(cContinent).add(cc);
					}
					
					nc = n.getCode();
					if(!continentExternalBorders.get(cContinent).contains(nc))
					{
						continentExternalBorders.get(cContinent).add(nc);
					}
					
					internalCountry = false;
				}
			}
			
			if(internalCountry)
			{
				continentInternalCountries.get(cContinent).add(cc);
			}
		}
	}
	
	private static void initialiseNodes(Country [] countries)
	{
		List<Integer> borderCodes;
		PriorityQueue<SearchNode> queue;
		SearchNode currentNode;
		int [] adjacentCountries;
		boolean [] visited = new boolean[countries.length];
		
		nodes = new SearchNode[countries.length];
		for(int i = 0; i < numberOfCountries; i++)
		{
			nodes[i] = new SearchNode(countries[i]);
		}
		
		for(int continent = 0; continent < numberOfContinents; continent++)
		{
			if(continent == 9)
			{
				System.out.println("hmm");
			}
			borderCodes = continentInternalBorders.get(continent);
			
			for(int border : borderCodes)
			{
				for(int c : continentCountries.get(continent))
				{
					nodes[c].nextBorder();
					visited[c] = false;
				}
				
				//Initialise the priority queue with starting at border at 0 cost.
				queue = new PriorityQueue<SearchNode>();
				queue.add(nodes[border]);
				nodes[border].currentCost = 0;
				visited[border] = true;
				
				// Perform Dijkstra's algorithm
				SearchNode nextNode;
				currentNode = queue.poll();
				while(currentNode != null)
				{
					adjacentCountries = currentNode.country.getAdjoiningCodeList();
					
					for(int c : adjacentCountries)
					{
						nextNode = nodes[c];
						if(currentNode.currentCost + 1 < nextNode.currentCost && nextNode.country.getContinent() == continent)
						{
							queue.remove(nextNode);
							nextNode.currentCost = currentNode.currentCost + 1;
							queue.add(nextNode);
						}
					}
					
					currentNode = queue.poll();
				}
			}
			
			for(int c : continentCountries.get(continent))
			{
				nodes[c].nextBorder();
			}
		}
		/**
		for(int cont = 0; cont < numberOfContinents; cont++)
		{
			System.out.println("Looking at continent " + cont);
			
			for(int c : continentInternalBorders.get(cont))
			{
				System.out.print(countries[c].getName() + " ");
			}
			System.out.println("");
			
			for(int c : continentCountries.get(cont))
			{
				System.out.print(nodes[c].country.getName());
				for(int distance : nodes[c].costsToBorder)
				{
					System.out.print(" " + distance);
				}
				System.out.println("");
			}
			System.out.println("");
		}
		
		System.out.println("");
		**/
	}
	
	private static void initialiseCompulsoryReinforcementMoves(Country [] countries)
	{
		compulsoryFortificationMoves = new ArrayList<List<Entry<Integer,List<Integer>>>>();
		for(int i = 0; i < numberOfContinents; i++)
		{
			compulsoryFortificationMoves.add(new ArrayList<Entry<Integer,List<Integer>>>());
		}
		
		Country c;
		int continent;
		SearchNode csn, nsn1, nsn2;
		List<Integer> possiblyCompulsory;
		List<Integer> notCompulsory;
		
		for(int cc = 0; cc < numberOfCountries; cc++)
		{
			if(cc == 181)
			{
				System.out.println(cc);
			}
			c = countries[cc];
			continent = c.getContinent();
			csn = nodes[cc];
			
			possiblyCompulsory = new ArrayList<Integer>();
			
			for(int nc : c.getAdjoiningCodeList())
			{
				if(continent == countries[nc].getContinent())
				{
					nsn1 = nodes[nc];
					
					if(nsn1.closerToBorders(csn,true))
					{
						possiblyCompulsory.add(nc);
					}
				}
			}
			
			notCompulsory = new ArrayList<Integer>();
			for(int nc1 : possiblyCompulsory)
			{
				nsn1 = nodes[nc1];
				for(int nc2 : c.getAdjoiningCodeList())
				{
					if(nc1 != nc2)
					{
						nsn2 = nodes[nc2];
						
						if(!nsn1.closerToBorders(nsn2,false))
						{
							notCompulsory.add(nc1);
							break;
						}
					}
				}
			}
			
			possiblyCompulsory.removeAll(notCompulsory);
			
			if(!possiblyCompulsory.isEmpty())
			{
				compulsoryFortificationMoves.get(continent).add(new SimpleImmutableEntry<Integer, List<Integer>>(cc, possiblyCompulsory));
			}
		}
	}
	
	private static void initialiseValidReinforcementMoves(Country [] countries)
	{
		validReinforcementMoves = new boolean[numberOfCountries][numberOfCountries];
		
		Country c1, c2;
		int cont1, cont2;
		SearchNode s1, s2;
		boolean neighbour;
		boolean closer;
		
		for(int cc1 = 0; cc1 < numberOfCountries; cc1++)
		{
			c1 = countries[cc1];
			cont1 = c1.getContinent();
			
			for(int cc2 = 0; cc2 < numberOfCountries; cc2++)
			{
				if(cc1 != cc2)
				{
					c2 = countries[cc2];
					cont2 = c2.getContinent();
					neighbour = false;
					for(int nc : c1.getAdjoiningCodeList())
					{
						neighbour |= nc == cc2;
					}
				
					if(neighbour)
					{
						if(cont1 != cont2)
						{
							validReinforcementMoves[cc1][cc2] = true;
						}
						else
						{
							s1 = nodes[cc1];
							s2 = nodes[cc2];
							closer = true;
							
							for(int border = 0; border < s1.costsToBorder.size(); border++)
							{
								Integer cost1 = s1.costsToBorder.get(border);
								Integer cost2 = s2.costsToBorder.get(border);
								
								if(cost1 != null && cost2 != null)
								{
									closer &= s1.costsToBorder.get(border) <= s2.costsToBorder.get(border);
								}
							}
							validReinforcementMoves[cc1][cc2] = !closer;
						}
					}
				}
			}
		}
		
		/**
		System.out.println("");
		for(int cc1 = 0; cc1 < numberOfCountries; cc1++)
		{
			for(int cc2 = 0; cc2 < numberOfCountries; cc2++)
			{
				if(!validReinforcementMoves[cc1][cc2])
				{
					neighbour = false;
					for(int nc : countries[cc1].getAdjoiningCodeList())
					{
						neighbour |= nc == cc2;
					}
					
					if(neighbour)
					{
						System.out.println("Eliminated " + countries[cc1].getName() + " to " + countries[cc2].getName());
					}
				}
			}
		}**/
	}
	
	public static List<Integer> getContinentCountries(int continent)
	{
		return new ArrayList<Integer>(continentCountries.get(continent));
	}
	
	public static List<Integer> getContinentInternalCountries(int continent)
	{
		return new ArrayList<Integer>(continentInternalCountries.get(continent));
	}
	
	public static List<Integer> getContinentInternalBorders(int continent)
	{
		return new ArrayList<Integer>(continentInternalBorders.get(continent));
	}
	
	public static List<Integer> getContinentExternalBorders(int continent)
	{
		return new ArrayList<Integer>(continentExternalBorders.get(continent));
	}

	public static boolean isBorderCountry(Country country)
	{
		return continentInternalBorders.get(country.getContinent()).contains(country.getCode());
	}
	
	public static List<Entry<Integer,List<Integer>>> getCompulsoryFortifications(int continent)
	{
		return new ArrayList<Entry<Integer,List<Integer>>>(compulsoryFortificationMoves.get(continent));
	}
	
	public static boolean isValidFortificationMoveInOwnedContinent(int source, int destination)
	{
		return validReinforcementMoves[source][destination];
	}
	
	private static class SearchNode implements Comparable<SearchNode>
	{
		Country country;
		List<Integer> costsToBorder;
		int currentCost;
		int numberOfBorders;
		
		public SearchNode(Country country)
		{
			this.country = country;
			costsToBorder = new ArrayList<Integer>();
			currentCost = Integer.MAX_VALUE;
		}

		@Override
		public int compareTo(SearchNode o)
		{
			return currentCost < o.currentCost ? -1 : (currentCost == o.currentCost ? 0 : 1);
		}
		
		public boolean closerToBorders(SearchNode o, boolean strictly)
		{
			boolean closer = true, equal = true;
			for(int border = 0; border < numberOfBorders; border++)
			{
				Integer c1 = costsToBorder.get(border);
				Integer c2 = o.costsToBorder.get(border);
				
				if(c1 != null && c2 != null)
				{
					closer &= c1 <= c2;
					equal &= c1 == c2;
				}
			}
			return (strictly && closer && !equal) || (!strictly && closer);
		}
		
		public void nextBorder()
		{
			costsToBorder.add(currentCost == Integer.MAX_VALUE ? null : currentCost);
			numberOfBorders++;
			currentCost = Integer.MAX_VALUE;
		}
	}
}
