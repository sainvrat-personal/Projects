package onlineQuiz;
import java.awt.Color; 
import javax.swing.*;

public class Qqstart {
	JFrame f;
	JLabel l,l2,l3,l4,l5,l6,l0;
	JPanel p;
	ImageIcon i,i2,i3,i4,i5,i6;
	
	Qqstart(){
		 //c=getContentPane();  
		 //c.setLayout(new FlowLayout());  
		    
		f=new JFrame("startpage");
		
		f.setSize(1350,750);
	    f.setBackground(Color.BLACK);
	    f.setVisible(true); 
		l0=new JLabel(); l0.setBounds(0,0,1400,750);   l0.setLayout(null) ;  l0.setOpaque(true);  l0.setBackground(Color.WHITE);
		f.add(l0);
	
		//f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		//JPanel p=new JPanel();   p.setLayout(null);
		//add(p);
		
		i=new ImageIcon("src/onlineQuiz/images/1 (113).jpg");
		i2=new ImageIcon("src/onlineQuiz/images/1 (1).jpg");
		i3=new ImageIcon("src/onlineQuiz/images/1 (4).jpg");
		i4=new ImageIcon("src/onlineQuiz/images/1 (120).jpg");
		i5=new ImageIcon("src/onlineQuiz/images/1 (22).jpg");
		i6=new ImageIcon("src/onlineQuiz/images/1 (20).jpg");
		l=new JLabel(i);
		l2=new JLabel(i2);
	    l3=new JLabel(i3);
		l4=new JLabel(i4);
		l5=new JLabel(i5);
	    l6=new JLabel(i6);
		l.setBounds(0,0,450, 450);
		l2.setBounds(470, 0,450, 450);
		l3.setBounds(900,0, 450, 450);
		l4.setBounds(0, 470, 450, 300);
		l5.setBounds(470, 470, 450, 300);
		l6.setBounds(900, 470, 450, 300);
		
		l0.add(l);   l0.add(l2);   l0.add(l3);  l0.add(l4);  l0.add(l5);  l0.add(l6);
		if (f.getExtendedState() != JFrame.MAXIMIZED_BOTH) {
            f.setExtendedState(JFrame.MAXIMIZED_BOTH);   }
		
		//   f.setVisible(false);
	}
	/*public void run(){
		
	try{
		new Qqstart();
		   Thread.sleep(10000); 
		   
		   }
		catch (Exception e){
			System.out.println(e);
		} 
	}
	 
	/*public void actionPerformed(ActionEvent e) {  
	Color initialcolor=Color.RED;  
	Color color=JColorChooser.showDialog(this,"Select a color",initialcolor);  
	c.setBackground(color);  
	}  */
	public static void main(String[] args) { 
		
		  //new WelcomePage2();
		new Qqstart();
		
	  
        // f.setVisible(false);  	
	}

}
