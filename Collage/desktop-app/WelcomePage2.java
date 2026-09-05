package onlineQuiz;
import javax.swing.*;
import java.awt.Color;
import java.awt.event.*;

public class WelcomePage2 extends JFrame implements ActionListener{
	JLabel l,l3,l4,l5,l6,l7,l8;
	JButton b1,b2,b3;
	JPanel p,p1;
	ImageIcon i,i2,i3,i4,i5,i6,i7,i8;
	WelcomePage2(){
		initp();  }
	private void initp(){
		createPage();
		
		setTitle("Welcome Page");
		setSize(1350,750);
		//setLayout(new BorderLayout());
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBackground(Color.WHITE);
	}
	private void createPage(){
		
		  
		     
	    l=new JLabel();   
		l.setBounds(0, 0, 1350, 750);     l.setLayout(null); 
		l.setBackground(Color.WHITE);               l.setOpaque(true);   add(l);
		
		i3=new ImageIcon("src/onlineQuiz/images/1 (48).jpg");   l3=new JLabel(i3);   l3.setBounds(0,0,450,350);   
		l.add(l3);
		i4=new ImageIcon("src/onlineQuiz/images/1 (45).jpg");   l4=new JLabel(i4);   l4.setBounds(450,0,450,350);    
		l.add(l4);
		i5=new ImageIcon("src/onlineQuiz/images/1 (93).jpg");   l5=new JLabel(i5);   l5.setBounds(900,0,450,350);    
		l.add(l5);
		i6=new ImageIcon("src/onlineQuiz/images/1 (17).jpg");   l6=new JLabel(i6);   l6.setBounds(0,350,450,300);    
	
		l.add(l6);
		i7=new ImageIcon("src/onlineQuiz/images/1 (15).jpg");   l7=new JLabel(i7);   l7.setBounds(450,350,450,300);    
		l.add(l7);
		i8=new ImageIcon("src/onlineQuiz/images/1 (27).jpg");   l8=new JLabel(i8);   l8.setBounds(900,350,450,300);    
		l.add(l8);
		
		
		b1=new JButton("Log-In");   	b2=new JButton("Sign-In");   	b3=new JButton("Exit");
		b1.setBounds(200,660,100,30);   b2.setBounds(450, 660, 100,30);    b3.setBounds(700,660,100,30);
       b1.addActionListener(this);        b2.addActionListener(this);        b3.addActionListener(this);
		   l.add(b1);         l.add(b2);        l.add(b3);
		   
		   //for refresh
		   setSize(1350,750);     setSize(1350,750);
	}
    public void actionPerformed(ActionEvent e){
    	if(e.getSource()==b1){
    	new LoginPage();
    	setVisible(false);
    	}
    	else if(e.getSource()==b2){
    		new Registration();
    		setVisible(false);
    	}
    	else if(e.getSource()==b3){
    		setVisible(false);
    	}
    	else
    		JOptionPane.showMessageDialog(null,"Please Select a Option");
    	
    }
	public static void main(String[] args) {
		WelcomePage2 w=new WelcomePage2();

	}

}

