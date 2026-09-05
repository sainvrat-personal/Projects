package onlineQuiz;
import java.lang.Thread;
import javax.swing.JFrame;

public class Start extends Thread{
public void run(){
	//System.out.println("SASA");
	try{
	     //new Qqstart();
		Thread.sleep(200); 
	   
	   }
	catch (Exception e){
		System.out.println(e);
	}
} 
public void run2(){
	//System.out.println("SASA");
	try{
		new WelcomePage2();
		
	   }
	catch (Exception e){
		System.out.println(e);
	}
} 
public void start(int i){
	if(i==1){
	run();	
	}
	else{
		run2();
	}
}

	public static void main(String[] args) { int i=1;
     Start s=new Start();
     s.start(i);  
     
     Start s2=new Start();  i++;    s.stop(); s2.start(i);
   
     
	}

}
