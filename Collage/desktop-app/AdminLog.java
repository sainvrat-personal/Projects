package onlineQuiz;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

import javax.swing.*;

//import com.mysql.jdbc.Statement;
public class AdminLog extends JFrame implements ActionListener{
	//JFrame f;
	JLabel l;
	JButton b1,b2,b3,ba1,ba2,ba3,ba4,ba5;
	String ss1,ss2,ss3,ss4,ss5,ss6,ss7,ss8;  int ss;
	JTextField tf1,tf2,tf3,tf4,tf5,tf6,tf7,tf8,tf9,tf10;
AdminLog(){
	initU();
}
private void initU(){
	setTitle("Admin Login Page");
	setSize(900, 600);
	setLayout(null);
	setVisible(true);
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	
	JMenuBar m=new JMenuBar();  m.setBounds(0, 0, 900, 40);   m.setBackground(new Color(255,255,255));   setJMenuBar(m);
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
					setVisible(false);
					JOptionPane.showMessageDialog(null, "You Successfuly Logged-Out");
				}catch(Exception ee){
					System.out.println(ee);
				}
			}
			else {  }
			}
			
		  });
    m.add(mu);	m.add(Box.createHorizontalGlue());    mu.add(mi0);

	l=new JLabel();     l.setBounds(0, 0, 1350, 750); l.setBackground(new Color(73,113,243)); 
	l.setOpaque(true); l.setLayout(null);    add(l);
	JLabel l1=new JLabel("Quiz Admin:");  l1.setBounds(0, 10, 1350, 70); l1.setBackground(Color.WHITE);  l1.setOpaque(true); 
	l.add(l1); 
	l1.setForeground(Color.BLUE);   l1.setFont(new Font("Serif", Font.BOLD, 50));
	
	JLabel l2=new JLabel("User-Id :");    l2.setBounds(50, 170, 100, 30);  l.add(l2);
	JLabel l3=new JLabel("User-Name :");  l3.setBounds(50, 230, 100, 30);  l.add(l3);
	JLabel l4=new JLabel("Email-Id :");   l4.setBounds(50, 290, 100, 30);  l.add(l4);
	JLabel l5=new JLabel("Password :");   l5.setBounds(50, 350, 100, 30);  l.add(l5);
	JLabel l6=new JLabel("Country :");      l6.setBounds(450, 170, 100, 30);  l.add(l6);
	JLabel l7=new JLabel("State :");    l7.setBounds(450, 230, 100, 30);  l.add(l7);
	JLabel l8=new JLabel("Phone-No. :");  l8.setBounds(450, 290, 100, 30);  l.add(l8);
	JLabel l9=new JLabel("Role :");       l9.setBounds(450, 350, 100, 30);  l.add(l9);
	JLabel l10=new JLabel("Enter Attribute Name :");    l10.setBounds(50, 410, 150, 30);  l.add(l10);
	JLabel l11=new JLabel("Enter Attribute Value :");   l11.setBounds(450, 410, 150, 30);  l.add(l11);
	tf1=new JTextField();      tf1.setBounds(200,170,200,30);  l.add(tf1);  
	tf2=new JTextField();      tf2.setBounds(200,230,200,30);  l.add(tf2);
	tf3=new JTextField();      tf3.setBounds(200,290,200,30);  l.add(tf3);
	tf4=new JTextField();      tf4.setBounds(200,350,200,30);  l.add(tf4);
	tf5=new JTextField();      tf5.setBounds(600,170,200,30);  l.add(tf5);
	tf6=new JTextField();      tf6.setBounds(600,230,200,30);  l.add(tf6);
	tf7=new JTextField();      tf7.setBounds(600,290,200,30);  l.add(tf7);
	tf8=new JTextField();      tf8.setBounds(600,350,200,30);  l.add(tf8);
	tf9=new JTextField();      tf9.setBounds(200,410,200,30);  l.add(tf9);
	tf10=new JTextField();      tf10.setBounds(600,410,200,30);  l.add(tf10);
	ba1=new JButton("User1"); ba1.setBounds(20,110,80,30); l.add(ba1);
	ba1.addActionListener(new ActionListener(){
	public void actionPerformed(ActionEvent e){
		ba1ActionPerformed(e);
	}
	});
	ba2=new JButton("User2"); ba2.setBounds(130,110,80,30); l.add(ba2);
	ba2.addActionListener(new ActionListener(){
	public void actionPerformed(ActionEvent e){
		ba2ActionPerformed(e);
	}
	});
	ba3=new JButton("User3"); ba3.setBounds(240,110,80,30); l.add(ba3);
	ba3.addActionListener(new ActionListener(){
	public void actionPerformed(ActionEvent e){
		ba3ActionPerformed(e);
	}
	});
	ba4=new JButton("User4"); ba4.setBounds(350,110,80,30); l.add(ba4);
	ba4.addActionListener(new ActionListener(){
	public void actionPerformed(ActionEvent e){
		ba4ActionPerformed(e);
	}
	});
	ba5=new JButton("User5"); ba5.setBounds(460,110,80,30); l.add(ba5);
	ba5.addActionListener(new ActionListener(){
	public void actionPerformed(ActionEvent e){
		ba5ActionPerformed(e);
	}
	});
	//ImageIcon i=new ImageIcon("src/onlineQuiz/images/1 (173).jpg");      
	//JLabel l2=new JLabel(i);   l2.setBounds(100, 110,  500, 500);  l.add(l2); 
	
	b1=new JButton("Update");    b2=new JButton("Delete");     b3=new JButton("Insert");
	b1.setBounds(100, 500, 100, 30);     b2.setBounds(250, 500, 100, 30);      b3.setBounds(400, 500, 100, 30);
    b1.addActionListener(this);	     b2.addActionListener(this);         b3.addActionListener(this);
    l.add(b1);                           l.add(b2);                            l.add(b3);
    }
public void actionPerformed(ActionEvent e){  
	if(e.getSource()==b1){ 
		int i=JOptionPane.showConfirmDialog(null, "Are You Sure,You wants to perform UPDATE OPERATION!!");
		if(i==0){int j=0;
		ss1=tf9.getText();
		ss2=tf10.getText();
		if(ss1.equals("U_Id")) j=1;
		else if(ss1.equals("Name")) j=2;
		else if(ss1.equals("Email_id")) j=3;
		else if(ss1.equals("Password")) j=4;
		else if(ss1.equals("Country")) j=5;
		else if(ss1.equals("State")) j=6;
		else  if(ss1.equals("Phone_no")) j=7;
		else if(ss1.equals("Role")) j=8;
		else { 
			JOptionPane.showMessageDialog(null, "No Such Attribute exists", "Error",JOptionPane.ERROR_MESSAGE);
		}
		UpdateValue(ss1,ss2,ss,j); }
		else {}
	}
	else if(e.getSource()==b2){
		int i=JOptionPane.showConfirmDialog(null, "Are You Sure,You wants to perform DELETE OPERATION!!");
    if(i==0){
    	DeleteValue(ss);
    	}
	else{ }
		
	}else
	{
		int i=JOptionPane.showConfirmDialog(null, "Are You Sure,You wants to perform INSERT OPERATION!!");
    if(i==0){
    	new Registration();
		setVisible(false);
    	}
    else {}
		
}   }
	public static void main(String[] args) {
	AdminLog g=new AdminLog();
     }
	private void UpdateValue(String ss1,String ss2,int ss,int p){
		try{
			Class.forName("com.mysql.jdbc.Driver");
			Connection con=DriverManager.getConnection("jdbc:mysql://localhost:3306/QnlineQuiz","root","1234");
			PreparedStatement ps=con.prepareStatement("update  users set '"+ss1+"' = ? where U_Id ='"+ss+"'");
			ps.setString(p,"ss2");
			int sq=ps.executeUpdate();
			con.close();
			JOptionPane.showMessageDialog(null,"You have Successfuly Updated the user Details");
		}
		catch(Exception e){
			System.out.println(e);
		}
	}
	
	private void DeleteValue(int ss){
		try{
			Class.forName("com.mysql.jdbc.Driver");
			Connection con=DriverManager.getConnection("jdbc:mysql://localhost:3306/QnlineQuiz","root","1234");
			PreparedStatement ps=con.prepareStatement("delete from users where U_Id ='"+ss+"'");
			int sq=ps.executeUpdate();
		
			con.close();
			JOptionPane.showMessageDialog(null,"You have Successfuly Deleted the user Details");
		}
		catch(Exception e){
			System.out.println(e);
		}
	}
	private void ba1ActionPerformed(ActionEvent e){ //System.out.println("done");	
		try{
			Class.forName("com.mysql.jdbc.Driver");
			Connection con=DriverManager.getConnection("jdbc:mysql://localhost:3306/QnlineQuiz","root","1234");
			Statement s=con.createStatement();
			ResultSet rs=s.executeQuery("select * from users where U_Id=1");
			while(rs.next()){
				//System.out.println("hello");	
				ss=rs.getInt(1);  
				ss1=rs.getString(2);
			
				ss2=rs.getString(3);
				ss3=rs.getString(4);
				ss4=rs.getString(5);
				ss5=rs.getString(6); ss6=rs.getString(7);    ss7=rs.getString(8);
			}
			//System.out.println(ss+","+ss1+","+ss2+","+ss3+","+ss4+","+ss5+","+ss6+","+ss7);
			setValue(ss,ss1,ss2,ss3,ss4,ss5,ss6,ss7);
		}catch(Exception ee){
		System.out.println(ee);	
		}
		
	}
	private void ba2ActionPerformed(ActionEvent e){// System.out.println("done");	
	try{
		Class.forName("com.mysql.jdbc.Driver");
		Connection con=DriverManager.getConnection("jdbc:mysql://localhost:3306/QnlineQuiz","root","1234");
		Statement s=con.createStatement();
		ResultSet rs=s.executeQuery("select * from users where U_Id=3");
		while(rs.next()){
			//System.out.println("hello");	
			ss=rs.getInt(1);  
			ss1=rs.getString(2);
		
			ss2=rs.getString(3);
			ss3=rs.getString(4);
			ss4=rs.getString(5);
			ss5=rs.getString(6); ss6=rs.getString(7);    ss7=rs.getString(8);
		}
		//System.out.println(ss+","+ss1+","+ss2+","+ss3+","+ss4+","+ss5+","+ss6+","+ss7);
		setValue(ss,ss1,ss2,ss3,ss4,ss5,ss6,ss7);
	}catch(Exception ee){
	System.out.println(ee);	
	}
	
}
	private void ba3ActionPerformed(ActionEvent e){ //System.out.println("done");	
	try{
		Class.forName("com.mysql.jdbc.Driver");
		Connection con=DriverManager.getConnection("jdbc:mysql://localhost:3306/QnlineQuiz","root","1234");
		Statement s=con.createStatement();
		ResultSet rs=s.executeQuery("select * from users where U_Id=4");
		while(rs.next()){
			//System.out.println("hello");	
			ss=rs.getInt(1);  
			ss1=rs.getString(2);
		
			ss2=rs.getString(3);
			ss3=rs.getString(4);
			ss4=rs.getString(5);
			ss5=rs.getString(6); ss6=rs.getString(7);    ss7=rs.getString(8);
		}
		//System.out.println(ss+","+ss1+","+ss2+","+ss3+","+ss4+","+ss5+","+ss6+","+ss7);
		setValue(ss,ss1,ss2,ss3,ss4,ss5,ss6,ss7);
	}catch(Exception ee){
	System.out.println(ee);	
	}
	
}
	private void ba4ActionPerformed(ActionEvent e){ //System.out.println("done");	
	try{
		Class.forName("com.mysql.jdbc.Driver");
		Connection con=DriverManager.getConnection("jdbc:mysql://localhost:3306/QnlineQuiz","root","1234");
		Statement s=con.createStatement();
		ResultSet rs=s.executeQuery("select * from users where U_Id=6");
		while(rs.next()){
			//System.out.println("hello");	
			ss=rs.getInt(1);  
			ss1=rs.getString(2);
		
			ss2=rs.getString(3);
			ss3=rs.getString(4);
			ss4=rs.getString(5);
			ss5=rs.getString(6); ss6=rs.getString(7);    ss7=rs.getString(8);
		}
	
		
		setValue(ss,ss1,ss2,ss3,ss4,ss5,ss6,ss7);
	}catch(Exception ee){
		System.out.println(ee);
	}
	}
	
	private void ba5ActionPerformed(ActionEvent e){
		try{
			Class.forName("com.mysql.jdbc.Driver");
			Connection con=DriverManager.getConnection("jdbc:mysql://localhost:3306/QnlineQuiz","root","1234");
			Statement s=con.createStatement();
			ResultSet rs=s.executeQuery("select * from users where U_Id=10");
			while(rs.next()){
				//System.out.println("hello");	
				ss=rs.getInt(1);  
				ss1=rs.getString(2);
			
				ss2=rs.getString(3);
				ss3=rs.getString(4);
				ss4=rs.getString(5);
				ss5=rs.getString(6); ss6=rs.getString(7);    ss7=rs.getString(8);
			}
			//System.out.println(ss+","+ss1+","+ss2+","+ss3+","+ss4+","+ss5+","+ss6+","+ss7);
			setValue(ss,ss1,ss2,ss3,ss4,ss5,ss6,ss7);
		}catch(Exception ee){
		System.out.println(ee);	
		}
}
	
	private void setValue(int ss,String ss1,String ss2,String ss3,String ss4,String ss5,String ss6,String ss7){
		//System.out.println(ss+","+ss1+","+ss2+","+ss3+","+ss4+","+ss5+","+ss6+","+ss7);
	     
		tf2.setText(ss1);
		tf5.setText(ss4);
		tf6.setText(ss5);
		tf8.setText(ss7);
		tf3.setText(ss2);   
		tf4.setText(ss3);
		tf7.setText(ss6);
		 tf1.setText(" "+ss); 
	}
}
