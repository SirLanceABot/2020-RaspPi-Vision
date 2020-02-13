package app;

public class App {

/*

Table lookup of X, Y co-ordinates

Example usage:
 public static void main(String[] args)
    {
        App x = new App();

        LUT distanceConversion = x.new LUT(10); // construct with parameter at least as large as the number of data points - min 2
        distanceConversion.add(1., 100.); // enter X, Y co-ordinate
        distanceConversion.add(0., 200.); // enter the data in X order ascending  must add at least 2 data points
        System.out.println("Converted to " + distanceConversion.lookup(1.5)); // lookup returns the value of Y coresponding to the X parameter

        System.out.println(distanceConversion); // print the table
    }
*/

public class LUT
{
    Point LUT[];

    private int capacity=0;
    private int next=0;

    class Point
    {
        double X;
        double Y;
        Point (double  X, double Y){this.X = X; this.Y = Y;}
    }

    LUT(int capacity)
    {
        try{
        if (capacity < 2) throw new Exception("Must have at least 2 data points; you tried to use " + capacity);
        this.capacity = capacity;
        LUT = new Point[capacity];
        }
        catch(Exception e){System.out.println(e);}
    }

    void add(double X, double Y)
    {
        try
        {
        if (next >= capacity) throw new Exception("Too many entries attempted to be added to LUT; max is " + capacity);
        if (next >= 1 && X <= LUT[next-1].X)throw new Exception("X not entered in ascending order");
        LUT[next++] = new Point(X, Y);
        }
        catch(Exception e){System.out.println(e);}
    }

    double lookup(double X)
    {
        double Y=99999.;

        final int tableMin = 0;
        final int tableMax = next - 1;
        int i;
        try
        {
        if (tableMax < 1) throw new Exception("Must have at least 2 data points; number of points is " + next);

        if (X < LUT[tableMin].X)
            {Y = LUT[tableMin].Y;} // below table

        else if (X > LUT[tableMax].X)
            {Y = LUT[tableMax].Y;} // above table

        else
            { // within table, find out where
            for (i = tableMin+1; i <= tableMax-1; i++)
                {if (X < LUT[i].X) break;}
            Y =  LUT[i-1].Y + (LUT[i].Y - LUT[i-1].Y)*((X -LUT[i-1].X)/(LUT[i].X - LUT[i-1].X));
            }
        }
        catch(Exception e) {System.out.println(e);Y = Double.NaN;}

        return Y;
    }

    public String toString()
    {
        String str = "{";
        for (int idx = 0; idx < next; idx++)
        str = str + "{" + LUT[idx].X + ", " + LUT[idx].Y + "}";
        str = str + "}";
        return str;
    }
}
    public static void main(String[] args)
    {
        App x = new App();

        LUT distanceConversion = x.new LUT(10); // construct with parameter at least as large as the number of data points - min 2
        distanceConversion.add(1., 100.); // enter X, Y co-ordinate
        distanceConversion.add(0., 200.); // enter the data in X order ascending  must add at least 2 data points
        System.out.println("Converted to " + distanceConversion.lookup(1.5)); // lookup returns the value of Y coresponding to the X parameter

        System.out.println(distanceConversion);

    }
}
