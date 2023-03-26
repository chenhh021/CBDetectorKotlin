import cross.language.algorithm.HungarianAlgorithm;

import java.util.Arrays;
import java.util.List;

public class testHungarian {

    public static void main(String[] args) {
//        DemoKt.run();
        int n = 3;
        List<Integer> list = Arrays.asList(1500, 4000, 4500, 2000, 6000, 3500, 2000, 4000, 2500);
    /*1500 4000 4500
	  2000 6000 3500
	  2000 4000 2500*/
        HungarianAlgorithm ob = new HungarianAlgorithm(list);
        System.out.println(ob.getFinalCost());
        System.out.println(ob.getFinalAssignment());
    }
}
