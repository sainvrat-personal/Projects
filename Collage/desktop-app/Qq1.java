package onlineQuiz;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.*;
public class Qq1{
	JFrame f;
	JPanel p;
	JLabel l,l1;
	ImageIcon i;
	JButton b;
	Qq1(){
		f=new JFrame("Page1");
		f.setSize(1000, 700);
		//f.setLayout(new BorderLayout());
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		p=new JPanel();
		p.setLayout(null);
		f.add(p);
		
		i=new ImageIcon("src/onlineQuiz/images/1 (113).jpg");
		l=new JLabel(i);
		l.setBounds(0,0,1000,700);   //l.setLayout(new FlowLayout());
		p.add(l);
		
		b=new JButton("click");
		b.setBounds(100, 600, 100, 30);
		l.add(b);
	}
	
	public static void main(String[] args) {
		
new Qq1();
	}

}
