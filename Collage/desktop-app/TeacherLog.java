package onlineQuiz;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

import javax.swing.*;

public class TeacherLog implements ActionListener{
	JFrame f;
	JButton b,b1,b2;
	JTextField t1,t2,t3,t4,t0;
	JRadioButton r1,r2,r3,r4,rb5,rb1,rb2,rb3,rb4;
TeacherLog(){
	initU();
}
private void initU(){
	f=new JFrame("Teacher Login Page!!");
	f.setSize(1350,750);
	f.setLayout(new BorderLayout());
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
    
	JLabel l=new JLabel();  l.setBounds(0, 0, 1350, 750); l.setBackground(Color.CYAN); 
	l.setOpaque(true); l.setLayout(null); f.add(l);
	JLabel l1=new JLabel("Insert Questions:");  l1.setBounds(100, 10, 1000, 70); l1.setBackground(Color.WHITE);  l1.setOpaque(true); 
	l.add(l1); 
	l1.setForeground(Color.BLACK);   l1.setFont(new Font("Serif", Font.BOLD, 50));
	
	JLabel l2=new JLabel("Select Subject");          l2.setBounds(50, 110, 100, 40);  l.add(l2);
	rb1=new JRadioButton("Chemistry");  rb1.setBounds(190, 110, 100, 40); l.add(rb1);
	rb2=new JRadioButton("English");    rb2.setBounds(330, 110, 80, 40); l.add(rb2);
	rb3=new JRadioButton("Maths");      rb3.setBounds(460, 110, 80, 40); l.add(rb3);
	rb4=new JRadioButton("GK");         rb4.setBounds(580, 110, 80, 40); l.add(rb4);
	rb5=new JRadioButton("Physics");    rb5.setBounds(700, 110, 80, 40); l.add(rb5);
	ButtonGroup g=new ButtonGroup();   g.add(rb5);  g.add(rb4);  g.add(rb3);  g.add(rb2);   g.add(rb1);
	
	 t0=new JTextField();    t0.setBounds(50,170,1200,100);        l.add(t0);
	 t1=new JTextField();    t1.setBounds(50,300,1200,40);        l.add(t1);
	 t2=new JTextField();    t2.setBounds(50,370,1200,40);        l.add(t2);
	 t3=new JTextField();    t3.setBounds(50,440,1200,40);        l.add(t3);
	 t4=new JTextField();    t4.setBounds(50,510,1200,40);        l.add(t4);
	
	JLabel l3=new JLabel("Select Correct Option"); l3.setBounds(50, 580, 200, 40);  l.add(l3);
	r1=new JRadioButton("Option 1"); r1.setBounds(290, 580, 80, 40); l.add(r1);
	r2=new JRadioButton("Option 2"); r2.setBounds(410, 580, 80, 40); l.add(r2);
	r3=new JRadioButton("Option 3"); r3.setBounds(530, 580, 80, 40); l.add(r3);
    r4=new JRadioButton("Option 4"); r4.setBounds(650, 580, 80, 40); l.add(r4);
	ButtonGroup g2=new ButtonGroup(); g2.add(r1); g2.add(r2); g2.add(r3); g2.add(r4);
	
	b=new JButton("Submit");     b.setBounds(100,650,100,30);       l.add(b);       
	b.addActionListener(new ActionListener(){
	public void actionPerformed(ActionEvent e){
		bActionPerformed(e);}
	});
	
	b1=new JButton("Cancel");    b1.setBounds(300,650,100,30);       l.add(b1);     b1.addActionListener(this);
    b2=new JButton("Next");      b2.setBounds(500,650,100,30);       l.add(b2);     b2.addActionListener(this);
	
}
public void actionPerformed(ActionEvent e){
	
		
		
	if(e.getSource()==b1){
		new WelcomePage2();
		f.setVisible(false);
	}else {
	  new TeacherLog();
	  f.setVisible(false);
	}
}
	public static void main(String[] args) {
      new TeacherLog();
	}
private void bActionPerformed(ActionEvent e){
	if(e.getSource()==b){
		int s=1,ss=1;
		String s0=t0.getText();
		String s1=t1.getText();
		String s2=t2.getText();
		String s3=t3.getText();
		String s4=t4.getText();
		if(r1.isSelected())
			ss=1;
		else if(r2.isSelected())
			ss=2;
		else if(r3.isSelected())
			ss=3;
		else if(r4.isSelected())
			ss=4;
		else 
			JOptionPane.showMessageDialog(null,"Please,Select the Correct Option!!");
		//System.out.println(s+s0+s1+s2+s3+s4+ss);
		if(rb1.isSelected()){
			try{
				Class.forName("com.mysql.jdbc.Driver");
				Connection con=DriverManager.getConnection("jdbc:mysql://localhost:3306/QnlineQuiz","root","1234");
				Statement stmt=con.createStatement();
				ResultSet rs=stmt.executeQuery("select QId from chemistry");
				while(rs.next()){
					s=rs.getInt(1);
				}
				PreparedStatement ps=con.prepareStatement("insert into chemistry values(?,?,?,?,?,?,?)");
				ps.setInt(1, s=s+1);
				ps.setString(2, s0);
				ps.setString(3, s1);
				ps.setString(4, s2);
				ps.setString(5, s3);
				ps.setString(6, s4);
				ps.setInt(7, ss);
				//System.out.println(s+s0+s1+s2+s3+s4+ss);
				int sa=ps.executeUpdate();
				JOptionPane.showMessageDialog(null,"Congratulate!! You Succesfuly added the Question.");
				new TeacherLog();
				f.setVisible(false);
			}catch(Exception ee){
				System.out.println(ee);
			}
		}else if(rb2.isSelected()){
		
		
		try{
			Class.forName("com.mysql.jdbc.Driver");
			Connection con=DriverManager.getConnection("jdbc:mysql://localhost:3306/QnlineQuiz","root","1234");
			Statement stmt=con.createStatement();
			ResultSet rs=stmt.executeQuery("select QId from english");
			while(rs.next()){
				s=rs.getInt(1);
			}
			PreparedStatement ps=con.prepareStatement("insert into english values(?,?,?,?,?,?,?)");
			ps.setInt(1, s=s+1);
			ps.setString(2, s0);
			ps.setString(3, s1);
			ps.setString(4, s2);
			ps.setString(5, s3);
			ps.setString(6, s4);
			ps.setInt(7, ss);
			//System.out.println(s+s0+s1+s2+s3+s4+ss);
			int sa=ps.executeUpdate();
			JOptionPane.showMessageDialog(null,"Congratulate!! You Succesfuly added the Question.");
			new TeacherLog();
			f.setVisible(false);
		}catch(Exception ee){
			System.out.println(ee);
		}
		}else if(rb3.isSelected()){
			try{
				Class.forName("com.mysql.jdbc.Driver");
				Connection con=DriverManager.getConnection("jdbc:mysql://localhost:3306/QnlineQuiz","root","1234");
				Statement stmt=con.createStatement();
				ResultSet rs=stmt.executeQuery("select QId from maths");
				while(rs.next()){
					s=rs.getInt(1);
				}
				PreparedStatement ps=con.prepareStatement("insert into maths values(?,?,?,?,?,?,?)");
				ps.setInt(1, s=s+1);
				ps.setString(2, s0);
				ps.setString(3, s1);
				ps.setString(4, s2);
				ps.setString(5, s3);
				ps.setString(6, s4);
				ps.setInt(7, ss);
				//System.out.println(s+s0+s1+s2+s3+s4+ss);
				int sa=ps.executeUpdate();
				JOptionPane.showMessageDialog(null,"Congratulate!! You Succesfuly added the Question.");
				new TeacherLog();
				f.setVisible(false);
			}catch(Exception ee){
				System.out.println(ee);
			}
		}else if(rb4.isSelected()){
			try{
				Class.forName("com.mysql.jdbc.Driver");
				Connection con=DriverManager.getConnection("jdbc:mysql://localhost:3306/QnlineQuiz","root","1234");
				Statement stmt=con.createStatement();
				ResultSet rs=stmt.executeQuery("select QId from gk");
				while(rs.next()){
					s=rs.getInt(1);
				}
				PreparedStatement ps=con.prepareStatement("insert into gk values(?,?,?,?,?,?,?)");
				ps.setInt(1, s=s+1);
				ps.setString(2, s0);
				ps.setString(3, s1);
				ps.setString(4, s2);
				ps.setString(5, s3);
				ps.setString(6, s4);
				ps.setInt(7, ss);
				//System.out.println(s+s0+s1+s2+s3+s4+ss);
				int sa=ps.executeUpdate();
				JOptionPane.showMessageDialog(null,"Congratulate!! You Succesfuly added the Question.");
				new TeacherLog();
				f.setVisible(false);
			}catch(Exception ee){
				System.out.println(ee);
			}
		}else if(rb5.isSelected()){
			try{
				Class.forName("com.mysql.jdbc.Driver");
				Connection con=DriverManager.getConnection("jdbc:mysql://localhost:3306/QnlineQuiz","root","1234");
				Statement stmt=con.createStatement();
				ResultSet rs=stmt.executeQuery("select QId from physics");
				while(rs.next()){
					s=rs.getInt(1);
				}
				PreparedStatement ps=con.prepareStatement("insert into physics values(?,?,?,?,?,?,?)");
				ps.setInt(1, s=s+1);
				ps.setString(2, s0);
				ps.setString(3, s1);
				ps.setString(4, s2);
				ps.setString(5, s3);
				ps.setString(6, s4);
				ps.setInt(7, ss);
				//System.out.println(s+s0+s1+s2+s3+s4+ss);
				int sa=ps.executeUpdate();
				JOptionPane.showMessageDialog(null,"Congratulate!! You Succesfuly added the Question.");
				new TeacherLog();
				f.setVisible(false);
			}catch(Exception ee){
				System.out.println(ee);
			}
		}else
			JOptionPane.showMessageDialog(null,"Please,Select A Subject!!");
	
}
}     }