package onlineQuiz;
import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class Registration extends JFrame implements ActionListener {
	 JLabel l1, l2, l3, l4, l5, l6, l7, l8,l9,l0;

	    JTextField tf1, tf2, tf5, tf6, tf7;

	    JButton btn1, b2,b3;

	    JPasswordField p1, p2;
        
	    JRadioButton r1,r2,r3;
	 

	    Registration()
	     {

	        setVisible(true);

	        setSize(500, 500);

	        setLayout(new BorderLayout());

	        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	        setTitle("Registration Form");

	 

	        l1 = new JLabel("Registration Form for Sign-IN:");

	        l1.setForeground(Color.blue);

	        l1.setFont(new Font("Serif", Font.BOLD, 20));

	 

	        l2 = new JLabel("Name:");

	        l3 = new JLabel("Email-ID:");

	        l4 = new JLabel("Create Passowrd:");

	        l5 = new JLabel("Confirm Password:");

	        l6 = new JLabel("Country:");

	        l7 = new JLabel("State:");

	        l8 = new JLabel("Phone No:"); 

	        l9=new JLabel("Role");  l0=new JLabel();
	        
	        tf1 = new JTextField();

	        tf2 = new JTextField();

	        p1 = new JPasswordField();

	        p2 = new JPasswordField();

	        tf5 = new JTextField();

	        tf6 = new JTextField();

	        tf7 = new JTextField();

	        

	        btn1 = new JButton("Submit");

	        b2 = new JButton("Clear");
            b3= new JButton("Back");
	 
            
	        btn1.addActionListener(this);

	        b2.addActionListener(this);
            b3.addActionListener(this);
            
            ImageIcon i=new ImageIcon("src/onlineQuiz/images/1 (42).jpg");
            JLabel l=new JLabel(i);  l.setBounds(0,0,500,500);   l.setLayout(null);  l0.add(l);
	        l0.setLayout(null);      l0.setBounds(0, 0, 500, 500);  l0.setLayout(null);  l0.setOpaque(true);
	        l0.setBackground(Color.WHITE);  add(l0);

	        l1.setBounds(100, 30, 400, 30);

	        l2.setBounds(80, 70, 200, 30);

	        l3.setBounds(80, 110, 200, 30);

	        l4.setBounds(80, 150, 200, 30);

	        l5.setBounds(80, 190, 200, 30);

	        l6.setBounds(80, 230, 200, 30);

	        l7.setBounds(80, 270, 200, 30);

	        l8.setBounds(80, 310, 200, 30);
	        
	        l9.setBounds(80, 350, 100, 30);

	        tf1.setBounds(250, 70, 200, 30);

	        tf2.setBounds(250, 110, 200, 30);

	        p1.setBounds(250, 150, 200, 30);

	        p2.setBounds(250, 190, 200, 30);

	        tf5.setBounds(250, 230, 200, 30);

	        tf6.setBounds(250, 270, 200, 30);

	        tf7.setBounds(250, 310, 200, 30);
	        ButtonGroup g=new ButtonGroup();    
	        r1=new JRadioButton("Student");   r2=new JRadioButton("Teacher");   r3=new JRadioButton("Admin");
	        r1.setBounds(200, 350, 80,30);    r2.setBounds(295, 350, 75,30);     r3.setBounds(380, 350, 70,30);
	        g.add(r1);                         g.add(r2);                         g.add(r3);   
	        l.add(r1);                         l.add(r2);                         l.add(r3);

	        btn1.setBounds(50, 390, 100, 30);

	        b2.setBounds(170, 390, 100, 30);
            b3.setBounds(350,390,100,30);
	 

	        l.add(l1);

	       l.add(l2);

	       l.add(tf1);

	       l.add(l3);

	       l.add(tf2);

	       l.add(l4);

	       l.add(p1);

	       l.add(l5);

	        l.add(p2);

	       l.add(l6);

	        l.add(tf5);

	        l.add(l7);

	        l.add(tf6);

	        l.add(l8);

	        l.add(tf7);
	        
	        l.add(l9);   

	        l.add(btn1);

	       l.add(b2);     l.add(b3);

	    }

	 

	    public void actionPerformed(ActionEvent e) 
	     {

	        if (e.getSource() == btn1)
	         {
	            int x = 0,s=1;

	            String s1 = tf1.getText();

	            String s2 = tf2.getText();

	 

	            char[] s3 = p1.getPassword();

	            char[] s4 = p2.getPassword(); 

	            String s8 = new String(s3);

	            String s9 = new String(s4);

	 

	            String s5 = tf5.getText();

	            String s6 = tf6.getText();

	            String s7 = tf7.getText();
                
	            String s10;
	            if(r1.isSelected())
	            s10="Student";
	            else if(r2.isSelected())
                s10="Teacher";
	            else
	            s10="Admin";

	            if (s8.equals(s9))
	           {

	                try
	               {

	                    Class.forName("com.mysql.jdbc.Driver");

	                    Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/QnlineQuiz", "root", "1234");
	                    Statement ps2=con.createStatement();
	                    //execute quer
	                    ResultSet rs2=ps2.executeQuery("select U_Id from users");
	                    while(rs2.next()){
	                    	s=rs2.getInt(1);
	                    	}
	                    
	                    
	                    
                         PreparedStatement ps = con.prepareStatement("insert into users values(?,?,?,?,?,?,?,?)");
                        ps.setInt(1, s=s+2);
	                     ps.setString(2, s1);  

	                    ps.setString(3, s2);

	                    ps.setString(4, s8);
                      
	                    ps.setString(5, s5);

	                    ps.setString(6, s6);

	                    ps.setString(7, s7);
	                    
	                    ps.setString(8, s10);
	                    
	                //
                        //ResultSet rs1=ps.executeQuery();
	                    int rs = ps.executeUpdate();
	                    //JOptionPane.showMessageDialog(btn1, "Data Saved Successfully");
	                   // System.out.println("dfddf");
                         
	                    x++;

	                    if (x > 0) 
	                    {

	                        JOptionPane.showMessageDialog(btn1, "Data Saved Successfully");

	                    }
	                    con.close();
	               }
	                    

	          catch (Exception ex) 
	                {

	                    System.out.println(ex);

	                }
	          if(s10.equals("Student")){
                  new blank();
                  setVisible(false);}
	          else if(s10.equals("Teacher")){
	        	  new TeacherLog();
	        	  setVisible(false);
	        	  
	          }
	          else if(s10.equals("Admin")){
	        	  new AdminLog();
	        	  setVisible(false);
	          }
	          else
	        	  JOptionPane.showMessageDialog(null,"wrong role!!Select either Student,Teacher or Admin.");
	            }
	          else
	           {

	                JOptionPane.showMessageDialog(btn1, "Password Does Not Match");
                  
	            } 

	        } 
	          else if(e.getSource()==b2)
	       {

	            tf1.setText("");

	            tf2.setText("");

	            p1.setText("");

	            p2.setText("");

	            tf5.setText("");

	            tf6.setText("");

	            tf7.setText("");

	        }
	          else
	          {
	        	  new WelcomePage2();
	        	  setVisible(false);
	          }

	    } 



	public static void main(String[] args) {
		 new Registration();

	}

}

