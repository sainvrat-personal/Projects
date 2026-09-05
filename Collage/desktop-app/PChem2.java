package onlineQuiz;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;

public class PChem2 implements ActionListener{
	JFrame f;
	JLabel l;
	JRadioButton r1,r2,r3,r4;
	JButton b1,b2,bq1,bq2,bq3,bq4,bq5,bq6,bq7,bq8,bq9,bq10;
	JMenuBar m;
	ButtonGroup g;
	static String ss,ss1, ss2, ss3, ss4;
	static int i2=11;   int ii,x;   boolean b=true;
	public PChem2(){
		main(new String[5]);
	}
	PChem2(int a,String ss,String ss1,String ss2,String ss3,String ss4){
		f=new JFrame("Cheistry Page2 Questions");
		f.setSize(1350,750);
		f.setLayout(null);
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setBackground(Color.green);
	
		JLabel l=new JLabel();    l.setBounds(0, 0,1350, 750);    f.add(l);
		l.setBackground(new Color(251,130,248)); l.setOpaque(true); l.setLayout(null);
		
		m=new JMenuBar();   m.setBounds(0, 0, 1350, 40);   m.setBackground(new Color(255,255,255));
		JMenu mu0=new JMenu("Change");
		JMenu mu1=new JMenu("New");
		JMenu mu2=new JMenu("View");
		JMenu mu4=new JMenu("Tools");
		JMenu mu3=new JMenu("Help");
		//JMenu mu5=new JMenu("Log-Out");
		JMenu mu6=new JMenu("Quiz");
		m.add(mu6);
		m.add(mu0);  m.add(mu1);m.add(mu2);
		m.add(Box.createHorizontalGlue());    m.add(mu4);
		m.add(mu3);
		
		JMenuItem mia=new JMenuItem("Restart Quiz");
		JMenuItem mib=new JMenuItem("Suspend Quiz");
	   JMenuItem mic=new JMenuItem("Resume Quiz");
		JMenuItem mid=new JMenuItem("Save");
		mu6.add(mia);   mu6.add(mib); mu6.add(mic); mu6.add(mid);
		
		JMenuItem mi0a=new JMenuItem("Help page");
		JMenuItem mi0b=new JMenuItem("Additional Help");
		mu3.add(mi0a); mu3.add(mi0b);
		
		JMenuItem mi0c=new JMenuItem("Calculater");
		JMenuItem mi0d=new JMenuItem("NotePed");
		mu4.add(mi0c); mu4.add(mi0d);
		mi0c.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent e){
	   		//int i=JOptionPane.showConfirmDialog(null, "Are you sure,You wants to leave this page");
		    //if(i==0){
		    	new MyCalculator("Calculator");
				//f.setVisible(false);
			//}
			//else{ }
			
		}    }       );
		
		JMenuItem mi0e=new JMenuItem("page");
		JMenuItem mi0f=new JMenuItem("Language");
		mu1.add(mi0e);   mu1.add(mi0f);
		
		JMenuItem mi0=new JMenuItem("Log-Out");
	    mi0.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				int j=JOptionPane.showConfirmDialog(null,"Are you Sure You wants to Log-out");
				if(j==0){
				int i=JOptionPane.showConfirmDialog(null,"Did you wants to see your Result page:"); 
				if(i==0){
					new Result();  f.setVisible(false);    }
				else if(i==1){
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
				else {  }
			}  });

		JMenu mu=new JMenu("Subject");
		mu0.add(mi0);   mu0.add(mu);
		
		JMenuItem mi00=new JMenuItem("Chemistry");
		mi00.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent e){
			JOptionPane.showMessageDialog(null,"Are You sure,You wants to leave this page!!");
			new PChem(); 
			f.setVisible(false);
		}    }       );
		JMenuItem mi01=new JMenuItem("EngLish");
		mi01.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent e){
			JOptionPane.showMessageDialog(null,"Are You sure,You wants to leave this page!!");
			new main(); 
			f.setVisible(false);
		}    }       );
		JMenuItem mi02=new JMenuItem("Physics");
		mi02.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent e){
			JOptionPane.showMessageDialog(null,"Are You sure,You wants to leave this page!!");
			new Pphy(); 
			f.setVisible(false);
		}    }       );
		JMenuItem mi03=new JMenuItem("Maths");
		mi03.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent e){
			JOptionPane.showMessageDialog(null,"Are You sure,You wants to leave this page!!");
			new PMath(); 
			f.setVisible(false);
		}    }       );
		mu.add(mi00);   mu.add(mi01);  mu.add(mi02);  mu.add(mi03);
		
		JMenuItem mi2=new JMenuItem("Result");
		mu2.add(mi2);
		
		//JToolBar tb1=new JToolBar();   tb1.setBounds(0, 0, 1350, 100);  tb1.setBackground(Color.BLUE) ; tb1.setLayout(null);
		
		JButton bq1=new JButton("Q11");      bq1.setBackground(Color.WHITE) ;    bq1.setBounds(10, 10, 90, 80);  
		bq1.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				
			}
		});
	    JButton bq2=new JButton("Q12");  	bq2.setBackground(Color.WHITE) ;    bq2.setBounds(140,10, 100, 80);  
	    bq2.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				
			}
		});
		JButton bq3=new JButton("Q13");      bq3.setBackground(Color.WHITE);    bq3.setBounds(280, 10, 100, 80); 
		bq3.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				
			}
		});
		JButton bq4=new JButton("Q14");      bq4.setBackground(Color.WHITE);     bq4.setBounds(420, 10, 100, 80); 
		bq4.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				
			}
		});
		JButton bq5=new JButton("Q15");      bq5.setBackground(Color.WHITE);     bq5.setBounds(560, 10, 100, 80); 
		bq5.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				
			}
		});
		JButton bq6=new JButton("Q16");      bq6.setBackground(Color.WHITE);     bq6.setBounds(700, 10, 100, 80); 
		bq6.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				
			}
		});
		JButton bq7=new JButton("Q17");      bq7.setBackground(Color.WHITE);     bq7.setBounds(840, 10, 100, 80); 
		bq7.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				
			}
		});
		JButton bq8=new JButton("Q18");      bq8.setBackground(Color.WHITE);     bq8.setBounds(980, 10, 100, 80); 
		bq8.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				
			}
		});
		JButton bq9=new JButton("Q19");      bq9.setBackground(Color.WHITE);     bq9.setBounds(1120, 10, 100, 80); 
		bq9.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				
			}
		});
		JButton bq10=new JButton("Q20");    bq10.setBackground(Color.WHITE);    bq10.setBounds(1250, 10, 90, 80); 
		bq10.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				
			}
		});
		l.add(bq1);  l.add(bq2);  l.add(bq3);  l.add(bq4);  l.add(bq5);  l.add(bq6);  l.add(bq7);
		l.add(bq8);  l.add(bq9);  l.add(bq10);
		//f.add(tb1);
		
		JLabel l2=new JLabel(a+".)"+ss);
		l2.setBounds(100,150,1150,100);  l2.setFont(new Font("Serif",Font.BOLD,20));
		l.add(l2);
		
		r1=new JRadioButton(ss1);           r2=new JRadioButton(ss2);
		r1.setBounds(50,300, 600, 50);    r2.setBounds(700, 300, 600, 50);
		l.add(r1);                       l.add(r2);
		
		r3=new JRadioButton(ss3);           r4=new JRadioButton(ss4);
		r3.setBounds(50, 400, 600, 50);   r4.setBounds(700, 400, 600, 50);
		l.add(r3);                       l.add(r4);
		
		g=new ButtonGroup();  g.add(r1);   g.add(r2); g.add(r3);  g.add(r4);
		b1=new JButton("Submit");       b2=new JButton("Back");            JButton b3=new JButton("Next");
		b1.setBounds(100,550,100,50);    b2.setBounds(300, 550, 100, 50);    b3.setBounds(500,550,100,50);
		l.add(b1);                     l.add(b2);                           l.add(b3);
		b1.addActionListener(this);     b2.addActionListener(this);        b3.addActionListener(this);
		
		f.setJMenuBar(m);
		//refresh
		f.setSize(1350,750);    
		f.setSize(1350,750);
	}
	
	public void actionPerformed(ActionEvent e){
		if(e.getSource()==b1){
			if(ss==null){
				JOptionPane.showMessageDialog(null,"Sorry!! Un-Valid Submition");
			}
			else{ 
				if(b){
            try
            {
                String s1;
                 Class.forName("com.mysql.jdbc.Driver");

                 Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/QnlineQuiz", "root", "1234");
                 Statement s=con.createStatement();
                 ResultSet  rs1=s.executeQuery("select * from chemistry where QId="+i2);
                 while(rs1.next()){
                	 ii=rs1.getInt(7);
                 }
                 rs1.close();    s.close();
                 String sql="insert into result(subjectId,User_ption,Right_Option,QuesId) values(?,?,?,?)";
                java.sql.PreparedStatement ps=con.prepareStatement(sql);
                 ps.setString(1,"gk05");
                 ps.setInt(2, x);
                 ps.setInt(3, ii);
                 ps.setInt(4, i2);
                 con.close();
                 b=false;
                }
       catch (Exception ex) 
             {    System.out.println(ex);     }
                     }else {
                    	JOptionPane.showMessageDialog(null,"You have Submitted the value");
                    	b1.setVisible(false);
                    	}
                    }  }
	else if(e.getSource()==b2){
    	     new PChem();
		     f.setVisible(false);
       }
	 else{
			JOptionPane.showMessageDialog(null, "Soory!!No More Page Available");
			//f.setVisible(false);
		}
	}
	public static void main(String args[]){
		 try
         {
             String s1; 
              Class.forName("com.mysql.jdbc.Driver");

              Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/QnlineQuiz", "root", "1234");
               Statement s=con.createStatement();
              //PreparedStatement ps = con.PreparedStatement("insert into reg values(?,?,?,?,?,?)");
              s1="select * from chemistry where QID=11";
             
              ResultSet rs = s.executeQuery(s1);
              while (rs.next())    {
            	 i2=rs.getInt(1);
            	 if(i2==11){
            	 ss=rs.getString(2); 
            	 ss1=rs.getString(3);
            	 ss2=rs.getString(4);
            	 ss3=rs.getString(5);
            	 ss4=rs.getString(6); 
            	 }
            	 else
            		 i2++;
             }   }
    catch (Exception ex) 
          {    System.out.println(ex);     }
                 
		new PChem2(i2,ss,ss1,ss2,ss3,ss4);
	}
	


}
