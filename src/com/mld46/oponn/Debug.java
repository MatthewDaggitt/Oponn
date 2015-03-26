package com.mld46.oponn;

import java.awt.Dimension;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class Debug
{
	public static boolean DEBUGGING = false;
	private static boolean PAUSING = DEBUGGING && false;
	
	private static JTextArea area = new JTextArea();
	private static JFrame frame = new JFrame();
	private static Scanner scanner = new Scanner(System.in);
	
	static
	{
		frame.add(new JScrollPane(area));
		frame.setSize(new Dimension(400,300));
		area.setEditable(false);
	}
	
	public static void output(String s, int indent)
	{
		String ind = "";
		for(int i = 0; i < indent; i++)
		{
			ind += "    ";
		}
		s = ind + s;
		area.append(s+"\n");
		
		if(DEBUGGING)
		{
			frame.setVisible(true);
			
			frame.repaint();
			frame.revalidate();
			area.setCaretPosition(area.getDocument().getLength());
		}
	}
	
	public static void moveSelected()
	{
		if(PAUSING)
		{
			scanner.nextLine();
		}
	}
}
