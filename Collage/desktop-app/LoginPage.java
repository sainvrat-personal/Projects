package onlineQuiz;
import javax.swing.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.*;
import java.sql.*;

public class LoginPage implements ActionListener{  
JFrame f;
JLabel l1,l2,l3;
JTextField t1;
JPasswordField p1;
JRadioButton r1,r2,r3;
JButton b1,b2,b3;
ButtonGroup g;
LoginPage(){
f=new JFrame("login Page");
f.setSize(500,400);
f.setLayout(new BorderLayout());
f.setVisible(true);
f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 

//JPanel p=new JPanel();  p.setLayout(new BorderLayout());   f.add(p);
JLabel l0=new JLabel();  l0.setBounds(0,0,500,400);                           l0.setForeground(Color.WHITE);   
l0.setOpaque(true);     l0.setBackground(Color.WHITE);
f.add(l0);

ImageIcon i=new ImageIcon("src/OnlineQuiz/images/1 (40).jpg");
JLabel l=new JLabel(i);  l.setBounds(50,0,400,400);  l.setLayout(null); l0.add(l);

l1=new JLabel("Email-Id");  l2=new JLabel("password");    l3=new JLabel("Select role");
l1.setBounds(20,50,100,30); l2.setBounds(20,100,100,30);  l3.setBounds(20,150,100,30);
l.add(l1);                     l.add(l2);                  l.add(l3);

t1=new JTextField();       t1.setBounds(150,50,200,30);  l.add(t1);

p1=new JPasswordField();    p1.setBounds(150,100,200,30);  l.add(p1);

g=new ButtonGroup();    
r1=new JRadioButton("Student");   r2=new JRadioButton("Teacher");   r3=new JRadioButton("Admin");
r1.setBounds(20, 200, 100,30);    r2.setBounds(150, 200, 100,30);     r3.setBounds(280, 200, 100,30);
g.add(r1);                         g.add(r2);                         g.add(r3);   
l.add(r1);                         l.add(r2);                         l.add(r3);

b1=new JButton("Submit");       b2=new JButton("Cancel");     b3=new JButton("Back");
b1.setBounds(20,300,100,30);   b2.setBounds(160,300,100,30);  b3.setBounds(300,300,100,30);
l.add(b1);                       l.add(b2);                    l.add(b3);
b1.addActionListener(this);       b2.addActionListener(this);  b3.addActionListener(this);

}
public void actionPerformed(ActionEvent e){
if (e.getSource()==b1){
String s,s3,s4;  
s=t1.getText();   char[] s5=p1.getPassword();   String s2=new String(s5); 
try{
Class.forName("com.mysql.jdbc.Driver");
Connection con=DriverManager.getConnection("jdbc:mysql://localhost:3306/QnlineQuiz", "root", "1234");
Statement stmt=con.createStatement();
ResultSet rs=stmt.executeQuery("Select * from users where Email_Id= '"+s+"' and Password= '"+s2+"'" );
if(!rs.next())
	{
	JOptionPane.showMessageDialog(null,"Wrong Password!! Try Again");
	return ;
	}
//while(rs.next()){
	if(r1.isSelected()){
	  //System.out.println("succesfuly login");
		new blank();
		f.setVisible(false);
	JOptionPane.showMessageDialog(null, "Congratulates Students!!! You Successfully Logged In");
	}
	else if(r2.isSelected()){
		new TeacherLog();
		f.setVisible(false);
		JOptionPane.showMessageDialog(null, "Congratulates Teachers!!! You Successfully Logged In");	
	}
	else if(r3.isSelected()){
		new AdminLog();
		JOptionPane.showMessageDialog(null, "Congratulates Admin!!! You Successfully Logged In");	
		f.setVisible(false);
	}
	else 
		JOptionPane.showMessageDialog(null,"Please!! Select the Role");
	
//}
}catch(Exception e3){
	System.out.println(e3);
}

}
else if(e.getSource()==b2){
	new WelcomePage2();
	f.setVisible(false);
}

else if(e.getSource()==b3){
	new WelcomePage2();
	f.setVisible(false);}
else
	JOptionPane.showMessageDialog(null,"Please Click a Button");

}
public static void main(String args[]){
new LoginPage();
}
}




