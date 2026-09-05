package onlineQuiz;
import javax.swing.*;

import java.sql.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.*;
public class Result implements ActionListener{
JFrame f;
JButton b,b1,b2;
JLabel l,l1,l2,l3,l4,l5;
JTextField tf1,tf2,tf3,tf4;
static int x=0,x1=0;
Result(){
	f=new JFrame("Result Declaration");
	f.setSize(1350,750);
	f.setVisible(true);
	f.setLayout(new BorderLayout());
	f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	
	JMenuBar m=new JMenuBar();  m.setBounds(0, 0, 900, 40);   m.setBackground(new Color(255,255,255));  f.setJMenuBar(m);
	JMenu mu=new JMenu("File");   
	JMenuItem mi0=new JMenuItem("Log-Out");
    mi0.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e){
			int j=JOptionPane.showConfirmDialog(null,"Are you Sure You wants to Log-out");
			if(j==0){
				try{
					Class.forName("com.mysql.jdbc.Driver");
					Connection con=DriverManager.getConnection("jdbc:mysql://localhost:3306/QnlineQuiz","root","1234");
					Statement stmt=con.createStatement();
					String sql="truncate table result";
					ResultSet rs=stmt.executeQuery(sql);
					con.close();
					new WelcomePage2();
					f.setVisible(false);
					JOptionPane.showMessageDialog(null, "You Successfuly Logged-Out");
				}catch(Exception ee){
					System.out.println(ee);
				}
			}
			else {  }
			}
			
		  });
    m.add(mu);	m.add(Box.createHorizontalGlue());    mu.add(mi0);
    
	l=new JLabel(); l.setBounds(0, 0, 1350, 750); f.add(l); 
	l.setBackground(new Color(128,255,255)); l.setOpaque(true); //l.setLayout(null);
	
	l1=new JLabel("Result Page:");  l1.setBounds(0, 10, 1350, 70); 
	l1.setBackground(Color.WHITE);  l1.setOpaque(true);    l.add(l1); 
	l1.setForeground(Color.BLUE);   l1.setFont(new Font("Serif", Font.BOLD, 50));
	
	b2=new JButton("Performance");  b2.setBounds(100, 110, 150, 30);  l.add(b2); b2.addActionListener(this);
}
Result(int x,int x1){
	f=new JFrame("Result Declaration");
	f.setSize(1350,750);
	f.setVisible(true);
	f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	
	JMenuBar m=new JMenuBar();  m.setBounds(0, 0, 900, 40);   m.setBackground(new Color(255,255,255));   f.setJMenuBar(m);
	JMenu mu=new JMenu("File");   
	JMenuItem mi0=new JMenuItem("Log-Out");
    mi0.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e){
			int j=JOptionPane.showConfirmDialog(null,"Are you Sure You wants to Log-out");
			if(j==0){
				try{
					Class.forName("com.mysql.jdbc.Driver");
					Connection con=DriverManager.getConnection("jdbc:mysql://localhost:3306/QnlineQuiz","root","1234");
					Statement stmt=con.createStatement();
					String sql="truncate table result";
					ResultSet rs=stmt.executeQuery(sql);
					con.close();
					new WelcomePage2();
					f.setVisible(false);
					JOptionPane.showMessageDialog(null, "You Successfuly Logged-Out");
				}catch(Exception ee){
					System.out.println(ee);
				}
			}
			else {  }
			}
			
		  });
    m.add(mu);	m.add(Box.createHorizontalGlue());    mu.add(mi0);
    
	l=new JLabel(); l.setBounds(0, 0, 1350, 750); f.add(l); 
	l.setBackground(new Color(128,255,255)); l.setOpaque(true); l.setLayout(null);
	
	l1=new JLabel("Result Page:");  l1.setBounds(0, 10, 1350, 70); 
	l1.setBackground(Color.WHITE);  l1.setOpaque(true);    l.add(l1); 
	l1.setForeground(Color.BLUE);   l1.setFont(new Font("Serif", Font.BOLD, 50));
	
	b2=new JButton("Performance");  b2.setBounds(100, 110, 150, 30);  l.add(b2); b2.addActionListener(this);
	
	l2=new JLabel("Question Attempted");        l2.setBounds(100, 170, 150, 30); l.add(l2);
	l3=new JLabel("Right Answered Question");   l3.setBounds(450, 170, 150, 30); l.add(l3);
	tf1=new JTextField(" "+x1);  tf1.setBounds(300, 170, 100, 30);  
	tf2=new JTextField(" "+x);  tf2.setBounds(650, 170, 100, 30); 
	l4=new JLabel("Accuracy");            l4.setBounds(100, 230, 150, 30);   l.add(l4);
	l5=new JLabel("Your Performance:");   l5.setBounds(450, 230, 150, 30);   l.add(l5);
	System.out.println(x+","+x1+",");
	tf3=new JTextField();  tf3.setBounds(300, 230, 100, 30);  
	tf4=new JTextField();  tf4.setBounds(650, 230, 100, 30);  
	l.add(tf1);     l.add(tf2); l.add(tf4);  l.add(tf3);
	String s2=null;
	int aa=(x*100)/x1;  
	 if(aa<40)  { tf1.setBackground(Color.RED); tf2.setBackground(Color.RED);     
	                tf3.setBackground(Color.RED); tf4.setBackground(Color.RED);   s2="Poor" ;  
   }
	else if(aa>=40 && aa <= 60){ tf1.setBackground(Color.ORANGE); tf2.setBackground(Color.ORANGE);
	                             tf3.setBackground(Color.ORANGE); tf4.setBackground(Color.ORANGE);    s2="Passive"; 
	}
	else if(aa>60 && aa < 80){   tf1.setBackground(new Color(155,255,4)); tf2.setBackground(new Color(155,255,4)); 
	                             tf3.setBackground(new Color(155,255,4)); tf4.setBackground(new Color(155,255,4));   s2="Mediate";
	}
	else if(aa >= 80 && aa <=90){ tf1.setBackground(Color.YELLOW); tf2.setBackground(Color.YELLOW); 
	                              tf3.setBackground(Color.YELLOW);  tf4.setBackground(Color.YELLOW);  s2="Good!!";
	}
	else {   tf1.setBackground(new Color(255,242,0)); tf2.setBackground(new Color(255,242,0)); 
	         tf3.setBackground(new Color(255,242,0)); tf4.setBackground(new Color(255,242,0));  s2="EXCELLENT";
	}  
	   tf4.setText(""+s2); tf3.setText(""+aa); tf3.setFont(new Font("Serif",Font.BOLD,2));  
   
	   l.add(tf1);     l.add(tf2); l.add(tf4);  l.add(tf3);
   
}
	public void actionPerformed(ActionEvent e){
		if(e.getSource()==b2){ int i,i2;
			try{
				Class.forName("com.mysql.jdbc.Driver");
				Connection con=DriverManager.getConnection("jdbc:mysql://localhost:3306/QnlineQuiz","root","1234");
				Statement s=con.createStatement();
				ResultSet rs=s.executeQuery("select * from result");
				while(rs.next()){
					 i=rs.getInt(2);
					 i2=rs.getInt(3);
					x1++;
					if(i==i2)
						x++;
				}
				con.close();
				f.setVisible(false);
			    new	Result(x,x1);
			
				System.out.println(x+","+x1);
			}catch(Exception ee){
				System.out.println(ee);
			}
		}
	}
	 
	public static void main(String[] args) {
		
       new Result(x,x1);
	}

}
