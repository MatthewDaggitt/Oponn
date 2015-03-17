package com.mld46.tournament;

import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;

public class Debug
{
	private final static boolean DEBUG = true;
	
	private static JTextArea area = new JTextArea();
	private static JFrame frame = new JFrame();
	
	private static boolean newLine = true;
	private static int index;
	
	static
	{
		frame.add(new JScrollPane(area));
		frame.setSize(new Dimension(400,300));
		frame.setLocation(0, Toolkit.getDefaultToolkit().getScreenSize().height-frame.getSize().height-100);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		area.setEditable(false);
	}
	
	public static void output(String s)
	{
		if(DEBUG)
		{
			append("\n"+s);
			newLine = true;
			index = area.getText().length();
		}
	}
	
	public static void outputGameNumber(String s)
	{
		if(DEBUG)
		{
			if(!newLine)
			{
				try
				{
					area.setText(area.getText(0, index));
				} 
				catch(BadLocationException e)
				{
					e.printStackTrace();
				}
			}
			append(s);
			newLine = false;
		}
	}
	
	private static void append(String s)
	{
		area.append(s);
		frame.repaint();
		frame.revalidate();
		area.setCaretPosition(area.getDocument().getLength());
	}
	
	public static void front()
	{
		frame.toFront();
	}
}
