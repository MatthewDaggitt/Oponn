package com.mld46.oponn.sim.agents;

import com.mld46.oponn.MapStats;
import com.sillysoft.lux.*;
import java.util.List;

public class MapHelper 
{
	private Country [] countries;
	private int numberOfContinents;
	
	private SimAgent owner;
	
	public MapHelper(SimAgent owner) 
	{
		this.countries = owner.countries;
		this.numberOfContinents = owner.numberOfContinents;
		this.owner = owner;
	}

	/** Returns the number of countries in <i>continent</i>.
	<p>
	This method first searches through all the <i>countries</i> looking for those 
	which are part of <i>continent</i>.  Once it has that list, it simply 
	iterates over the list and increments a counter as it does so.
	<p>
	To find the countries in the <i>continent</i>, it uses a ContinentIterator.
	<p>
	In the event that <i>continent</i> is invalid, the value returned is zero. 
	<p>
	* @param continent  the continent interested in
	* @param countries  the board
	* @return   		An integer
	* @see Country
	* @see CountryIterator
	* @see ContinentIterator
	*/
	public int getContinentSize(int continent)
	{
		return MapStats.getContinentCountries(continent).size();
	}
	
	/** This method simply determines if <i>player</i> owns all the countries 
	that are part of <i>continent</i>.
	<p>
	This method first searches through all the <i>countries</i> looking for those 
	which are part of <i>continent</i>.  Once it has that list, it inspects each
	to find out first if it is <i>not</i> owned by <i>player</i>.  If it finds
	any that are not, it returns false.  If all the countries are successfully
	checked, then it returns true.
	<p>
	To find the countries in the <i>continent</i>, it uses a ContinentIterator.
	<p>
	In the event that <i>player</i> is invalid, or is no longer in the game, the 
	value returned is the total number of armies on the <i>continent</i> 
	regardless of which player they belong to.
	<p>
	In the event that <i>player</i> is invalid, or is no longer in the game, the 
	value returned is false, which is sane.
	<p>
	Warning:  In the event that <i>continent</i> is invalid, the value returned is
	<i><b>true</b></i>, which is not sane.
	<p>
	* @param player 	the player interested in
	* @param continent  the continent interested in
	* @param countries  the board
	* @return   		A boolean (true or false)
	* @see Country
	* @see CountryIterator
	* @see ContinentIterator
	*/
	protected boolean playerOwnsContinent(int player, int continent)
	{
		List<Integer> continentCountries = MapStats.getContinentCountries(continent);
		
		for(int cc : continentCountries)
		{
			if(player != countries[cc].getOwner())
			{
				return false;
			}
		}
		return true;
	}

	/** The same as playerOwnsAnyContinent() except it only considers continents with positive bonuses. */
	public boolean playerOwnsAnyPositiveContinent(int player)
	{
		for(int i = 0; i < numberOfContinents; i++) 
		{
			if(playerOwnsContinent(player, i) && owner.getContinentBonus(i) > 0) 
			{
				return true;
			}
		}
		return false;
	}

	/** This method simply determines if <i>player</i> owns ANY of the countries
	that are part of <i>continent</i>.
	<p>
	Each country in the <i>continent</i> is checked for its owner.  If any
	country is owned by <i>player</i>, then true is returned.  If all countries
	are checked and none are found to be owned by <i>player</i>, false is 
	returned.
	<p>
	To find the countries in the <i>continent</i>s, it uses a ContinentIterator.
	<p>
	In the event that either <i>player</i> or <i>continent</i> is invalid, or
	the <i>player</i> is no longer in the game, the value returned is
	false.
	<p>
	* @param player 	the player interested in
	* @param continent  the continent interested in
	* @param countries  the board
	* @return   		A boolean (true or false)
	* @see Country
	* @see CountryIterator
	* @see ContinentIterator
	*/
	public boolean playerOwnsCountryInContinent(int player, int continent)
	{
		List<Integer> continentCountries = MapStats.getContinentCountries(continent);
		for(int cc : continentCountries) 
		{
			if(countries[cc].getOwner() == player)
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	This method is the same as getSmallestEmptyCont() except it only
	considers continents that have bonus values of greater than zero.	*/
	public int getSmallestPositiveEmptyContinent()
	{
		int smallestSize = Integer.MAX_VALUE;
		int smallestContinent = -1;
		for(int continent = 0; continent < numberOfContinents; continent++)
		{
			int size = getContinentSize(continent);
			if (size < smallestSize && playerOwnsContinent(-1,continent) && owner.getContinentBonus(continent) > 0)
			{
				smallestSize = size;
				smallestContinent = continent;
			}
		}
		return smallestContinent;
	}
	
	/** Returns the cont-code of the smallest continent that has at least one 
	unowned country.
	<p>
	This method first searches through all the <i>continents</i> and makes a
	note of the number of countries in each.  The index of the one with the 
	smallest number of countries, and which has at least one country owned
	by -1 (ie,the default at-map-creation-time non-player), is returned.
	<p>
	The method iterates over the entire board one continent at a time, checking
	for 'any' ownership by player -1.  If the entire board is found to be owned 
	by active players, than -1 is returned. 
	<p>
	* @param countries  the board
	* @return   		An integer
	* @see Country
	* @see BoardHelper#playerOwnsContinentCountry
	* @see BoardHelper#getContinentSize
	*/

	/**
	This method is the same as getSmallestOpenCont() except it only considers
	continents that have a bonus of greater than zero. */
	public int getSmallestPositiveOpenContinent()
	{
		int smallestSize = Integer.MAX_VALUE;
		int smallestContinent = -1;
		for(int continent = 0; continent < numberOfContinents; continent++)
		{
			int size = getContinentSize(continent);
			if (size < smallestSize && playerOwnsCountryInContinent(-1, continent) && owner.getContinentBonus(continent) > 0)
			{
				smallestSize = size;
				smallestContinent = continent;
			}
		}
		return smallestContinent;
	}
}

