package onlineQuiz;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.*;
import javax.swing.*;

public class blank implements ActionListener{
	JFrame f;
	JButton b1,b2,b3,b4,b5;
	//JComboBox c;
	
	blank(){
		f=new JFrame();
		f.setSize(900, 700);
		f.setLayout(new BorderLayout());
		f.setVisible(true);
		f.setBackground(Color.BLUE);
		
		//ImageIcon i=new ImageIcon("src/onlineQuiz/images/1 (6).jpg");		
		JLabel l=new JLabel();
		l.setLayout(null);    l.setBounds(0, 0, 500, 500);  l.setBackground(Color.white); l.setOpaque(true);   f.add(l);
		
		JLabel l1=new JLabel("Select Subject :");  l1.setBounds(00,10 ,900, 80); l1.setForeground(Color.white);
		l1.setFont(new Font("Serif",Font.BOLD,50));  l1.setOpaque(true);   l1.setBackground(Color.blue);  l.add(l1);
		//String s[]={"Select Subject","Chemistry","English","Physics","GK","Maths"};
		//JComboBox c=new JComboBox(s);  c.setBounds(100,100,200,100);  l.add(c);
		ImageIcon i1=new ImageIcon("src/onlineQuiz/images2/chem.gif");	
		ImageIcon i2=new ImageIcon("src/onlineQuiz/images2/gk.png");	
		ImageIcon i3=new ImageIcon("src/onlineQuiz/images2/phy.jpg");	
		ImageIcon i4=new ImageIcon("src/onlineQuiz/images2/eng.jpg");	
		ImageIcon i5=new ImageIcon("src/onlineQuiz/images2/math.jpg");	
		
		b1=new JButton(i1);  
		b1.setBounds(100, 100, 200, 200);
		l.add(b1);
		b1.addActionListener(this);
		b2=new JButton(i2);  
		b2.setBounds(350, 100, 200, 200);
		l.add(b2);
		b2.addActionListener(this);
		b3=new JButton(i3);  
		b3.setBounds(600, 100, 200, 200);
		l.add(b3);
		b3.addActionListener(this);
		b4=new JButton(i4);  
		b4.setBounds(250, 400, 200, 200);
		l.add(b4);
		b4.addActionListener(this);
		/*b4.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){
			
		}});  */
		
		b5=new JButton(i5);  
		b5.setBounds(500, 400, 200, 200);
		l.add(b5);
		b5.addActionListener(this);
		}
	public void actionPerformed(ActionEvent e){
		if(e.getSource()==b1){
			new PChem();
			f.setVisible(false);
		}
		else if(e.getSource()==b2){
			new PGK();
			f.setVisible(false);
		}else if(e.getSource()==b3){
			new Pphy();
			f.setVisible(false);
		}
		else if(e.getSource()==b4){
			new main();
			f.setVisible(false);
		}
		else {
			new PMath();
			f.setVisible(false);
		}
		
	}
	public static void main(String[] args) {
	new blank();
	    }

}
