package com.mld46.agent.moves;

import com.mld46.sim.board.SimBoard.Phase;
import com.sillysoft.lux.Card;

public class CardCash extends Move
{
	public final Card card1;
	public final Card card2;
	public final Card card3;
	
	public CardCash(Card card1, Card card2, Card card3, Phase phase)
	{
		super(phase);
		this.card1 = card1;
		this.card2 = card2;
		this.card3 = card3;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof CardCash)
		{
			CardCash c = (CardCash)o; 
			return c.card1.equals(card1) &&
					c.card2.equals(card2) &&
					c.card3.equals(card3);
		}
		return false;
	}
	
	@Override
	public String toString()
	{
		return "CardCash [" + (card1 != null ? "card1=" + card1 + ", " : "") + (card2 != null ? "card2=" + card2 + ", " : "") + (card3 != null ? "card3=" + card3 : "") + "]";
	}
}
