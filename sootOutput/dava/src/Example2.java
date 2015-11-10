import java.io.PrintStream;

public class Example2
{

    public static void main(String[]  r0)
    {


        System.out.println((new Example()).stuff(1));
    }

    public int stuff(int  i0)
    {

        int i1, i2;
        i1 = i0 + 1;
        i2 = i0 + i1 + 3;
        System.out.println(i1);
        System.out.println(i1 + 2);
        Example2.asdf();
        return i0 + 1 + i2;
    }

    public static void asdf()
    {

    }
}
