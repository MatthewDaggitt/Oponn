package com.mld46.oponn.tests;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.util.Random;

import org.junit.Test;

import com.mld46.oponn.BattleSim;
import com.mld46.oponn.moves.AttackOutcome;

public class BattleSimTests 
{
	@Test
	public void simulateBattleTest()
	{
		Random random = new Random(0);
		int cases = 10;
		
		for(int k = 0; k < cases; k++)
		{
			int a = random.nextInt(150);
			int d = random.nextInt(150);
			
			AttackOutcome [] outcomes = BattleSim.getAttackOutcomes(a, d);
			float [] observed = new float[outcomes.length];
			
			int iterations = 500000;
			for(int i = 0; i < iterations; i++)
			{
				AttackOutcome ao = BattleSim.simulateBattle(a, d);
				for(int j = 0; j < outcomes.length; j++)
				{
					AttackOutcome ao2 = outcomes[j];
					if(ao.equals(ao2))
					{
						observed[j]++;
					}
				}
			}
			
			float [] actual = new float[outcomes.length];
			for(int j = 0; j < outcomes.length; j++)
			{
				actual[j] = outcomes[j].probability;
				observed[j] /= iterations;
			}
			
			// Should do proper statistical test for convergence
			assertArrayEquals(actual, observed, 0.01f);
		}
	}
	
	@Test
	public void outcomeDuplicatesTest()
	{
		Random random = new Random(0);
		int cases = 100;
		
		for(int k = 0; k < cases; k++)
		{
			int a = random.nextInt(200);
			int d = random.nextInt(200);
			
			AttackOutcome [] outcomes = BattleSim.getAttackOutcomes(a, d);
			for(int i = 0; i < outcomes.length; i++)
			{
				for(int j = i+1; j < outcomes.length; j++)
				{
					assertThat(outcomes[i], is(not(equalTo(outcomes[j]))));
				}
			}
		}
	}
	
	@Test
	public void individualBattleTest()
	{
		int iterations = 5000000;
		double precision = 0.001;
		double inc = 1/(double)iterations;
		double [][] results = new double[6][3];
		
		for(int i = 0; i < iterations; i++)
		{
			results[0][BattleSim.simulateIndividualBattle(1, 1)] += inc;
			results[1][BattleSim.simulateIndividualBattle(1, 2)] += inc;
			results[2][BattleSim.simulateIndividualBattle(2, 1)] += inc;
			results[3][BattleSim.simulateIndividualBattle(2, 2)] += inc;
			results[4][BattleSim.simulateIndividualBattle(3, 1)] += inc;
			results[5][BattleSim.simulateIndividualBattle(3, 2)] += inc;
		}
		
		assertEquals(BattleSim.a1_d1_al0,results[0][0],precision);
		assertEquals(BattleSim.a1_d1_al1,results[0][1],precision);
		assertEquals(0.0				,results[0][2],precision);
		
		assertEquals(BattleSim.a1_d2_al0,results[1][0],precision);
		assertEquals(BattleSim.a1_d2_al1,results[1][1],precision);
		assertEquals(0.0				,results[1][2],precision);

		assertEquals(BattleSim.a2_d1_al0,results[2][0],precision);
		assertEquals(BattleSim.a2_d1_al1,results[2][1],precision);
		assertEquals(0.0				,results[2][2],precision);
		
		assertEquals(BattleSim.a2_d2_al0,results[3][0],precision);
		assertEquals(BattleSim.a2_d2_al1,results[3][1],precision);
		assertEquals(BattleSim.a2_d2_al2,results[3][2],precision);
		
		assertEquals(BattleSim.a3_d1_al0,results[4][0],precision);
		assertEquals(BattleSim.a3_d1_al1,results[4][1],precision);
		assertEquals(0.0				,results[4][2],precision);
		
		assertEquals(BattleSim.a3_d2_al0,results[5][0],precision);
		assertEquals(BattleSim.a3_d2_al1,results[5][1],precision);
		assertEquals(BattleSim.a3_d2_al2,results[5][2],precision);
	}
}
