package com.mld46.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.sillysoft.lux.Card;
import com.sillysoft.lux.Country;

public class CardManager
{
	private final static Card wildcard = new Card(-1,3);
	
	private final Card [] deck;
	
	private final List<Card> notInHand = new ArrayList<Card>();
	private final List<Card> inHand = new ArrayList<Card>();

	private List<Card> simDeck;
	private Random random;
	private boolean canRunOutOfCards;
	
	public CardManager(Country [] allCountries, int numberOfPlayers)
	{
		deck = new Card[allCountries.length];
		
		int counter = 0;
		for(Country c : allCountries)
		{
			Card card = new Card(c.getCode(),counter);
			deck[c.getCode()] = card;
			notInHand.add(card);
			counter = counter == 2 ? 0 : counter+1;
		}
		notInHand.add(wildcard);
		notInHand.add(wildcard);
	
		canRunOutOfCards = numberOfPlayers*5 > allCountries.length+2;
	}
	
	public void resetForNewGame()
	{
		notInHand.addAll(inHand);
		inHand.clear();
	}
			
	public void updateCardsHeld(Card [] realCards)
	{
		for(Card realCard : realCards)
		{
			int cc = realCard.getCode();
			
			if(cc != -1)
			{
				// Test if the card is the real card or a place holder
				Card ourCard = deck[cc];
				if(ourCard != realCard)
				{
					// if real replace all instances of the place holder
					deck[cc] = realCard;
					notInHand.remove(ourCard);
					inHand.remove(ourCard);
				}
			
				// If we don't think it's in our hand, add it our hand.
				Card card = deck[cc];
				if(!inHand.contains(card))
				{
					inHand.add(card);
					notInHand.remove(card);
				}
			}
		}
		
		if(realCards.length != inHand.size())
		{
			inHand.add(wildcard);
			notInHand.remove(wildcard);
		}
	}
	
	public List<Card> getCardsHeld()
	{
		return new ArrayList<Card>(inHand);
	}
	
	public void cashCards(Card c1, Card c2, Card c3)
	{
		notInHand.add(c1);
		notInHand.add(c2);
		notInHand.add(c3);
		
		inHand.remove(c1);
		inHand.remove(c2);
		inHand.remove(c3);
	}
	
	public void simReset()
	{
		simDeck = new ArrayList<Card>(notInHand);
		// needed to ensure that the same random numbers are drawn each time
		random = new Random(simDeck.hashCode());
	}
	
	public Card simDeal()
	{
		if(simDeck.size() == 0)
		{
			if(canRunOutOfCards)
			{
				return null;
			}
			throw new IllegalArgumentException("Invalidly run out of cards!");
		}
		
		return simDeck.remove(random.nextInt(simDeck.size()));
	}
	
	public void simCash(Card c1, Card c2, Card c3)
	{
		simDeck.add(c1);
		simDeck.add(c2);
		simDeck.add(c3);
	}

	
	public void simReturnToDeck(List<Card> cards)
	{
		simDeck.addAll(cards);
	}
}