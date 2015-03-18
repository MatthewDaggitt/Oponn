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

	private List<List<Card>> simDecks;
	private Random random;
	private boolean canRunOutOfCards;
	
	public final String cardProgression;
	private int cardProgressionPos = 0;
	private int [] simCardProgressionPositions;
	
	////////////////////////
	// Real state methods //
	////////////////////////
	
	public CardManager(Country [] allCountries, int numberOfPlayers, int cores, String cardProgression)
	{
		this.cardProgression = cardProgression;
		
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
		
		simDecks = new ArrayList<List<Card>>();
		simCardProgressionPositions = new int[cores];
		for(int i = 0; i < cores; i++)
		{
			simDecks.add(null);
			simCardProgressionPositions[i] = 1;
		}
		
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
		
		cardProgressionPos++;
	}
	
	public void incrementCardProgressionPosition()
	{
		cardProgressionPos++;
	}
	
	////////////////////////
	// Simulation methods //
	////////////////////////
	
	public void simReset(int coreID)
	{
		List<Card> simDeck = new ArrayList<Card>(notInHand);
		simDecks.set(coreID, simDeck);
		// needed to ensure that the same random numbers are drawn each time
		random = new Random(simDeck.hashCode());
		
		simCardProgressionPositions[coreID] = cardProgressionPos;
	}
	
	public Card simDeal(int coreID)
	{
		List<Card> deck = simDecks.get(coreID);
		if(deck.size() == 0)
		{
			if(canRunOutOfCards)
			{
				return null;
			}
			throw new IllegalArgumentException("Invalidly run out of cards!");
		}
		
		return deck.remove(random.nextInt(deck.size()));
	}
	
	public int simCash(Card c1, Card c2, Card c3, int coreID)
	{
		List<Card> deck = simDecks.get(coreID);
		deck.add(c1);
		deck.add(c2);
		deck.add(c3);
		
		int value = getCardCashValue(simCardProgressionPositions[coreID]);
		simCardProgressionPositions[coreID]++;
		
		return value;
	}
	
	public int getNextCardCashValue(int coreID)
	{
		return getCardCashValue(simCardProgressionPositions[coreID]);
	}
	
	private int getCardCashValue(int pos)
	{
		switch(cardProgression)
		{
			case "5, 5, 5...": 
				return 5;
			case "4, 4, 6, 6, 6, 8, 8, 8, 8, 10...":
				return (int) (Math.ceil((Math.sqrt(8*pos+9)-1)/2)*2);
			case "4, 5, 6...":
				return pos+3;
			case "4, 6, 8...":
				return (pos+1)*2;
			case "3, 6, 9...":
				return pos*3;
			case "4, 6, 8, 10, 15, 20...":
				return pos <= 4 ? (pos+1)*2 : (pos-2)*5;
			case "5, 10, 15...":
				return pos*5;
			case "4, 6, 8, 10, 15, 20, 25, 10, 10, 10...":
				return pos <= 4 ? (pos+1)*2 : (pos <= 7 ? (pos-2)*5 : 10);
			default:
				System.out.println("Unrecognised card progression: " + cardProgression);
				throw new IllegalArgumentException();
		}
	}

	public void simReturnToDeck(List<Card> cards, int coreID)
	{
		simDecks.get(coreID).addAll(cards);
	}
}