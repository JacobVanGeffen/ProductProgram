
public class Example2 {

	public static void main(String[] args) {
		System.out.println(new Example().stuff(1));
	}
	
	public int stuff(int a) {
		int b = a + 1,
			c = a + b + 3;
		System.out.println(b);
		System.out.println(b + 2);
		asdf();
		if(b == c)
			a++;
		else
			a--;
		
		while(a < 5)
			a++;
		return a + 1 + c;
	}
	
	public static void asdf() {
		
	}
	
}
