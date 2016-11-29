import java.util.*;
public class Caller {
    public static void main(String[] args) {
        Collection<Simple> objs = new ArrayList<Simple>();
        objs.add(new Simple(4));
        objs.add(new Child(4));
        objs.add(new Child(4));
        objs.add(new Simple(4));

        for (Iterator<Simple> it = objs.iterator(); it.hasNext();) {
            Simple obj = it.next();
            System.out.println("obj: " + obj.getSomething());
        }
    }
}