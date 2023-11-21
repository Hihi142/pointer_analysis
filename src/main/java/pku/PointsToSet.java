package pku;

import java.util.ArrayList;

public class PointsToSet {
    // managing the possible pointees of *one* pointer
    static int glb_cnt = 0;
    long x[];
    int len, num;
    ArrayList<Integer>lst;
    PointsToSet(int number_of_elements) {
        num = number_of_elements;
        len = (num + 63) / 64;
        x = new long[len];
        lst = new ArrayList<>();
    }
    int bitcount(long x) {
        int res = 0;
        while(x != 0) {
            x ^= (x & (-x));
            res++;
        }
        return res;
    }
    void add(int id_of_element) {
        int index = id_of_element >> 6;
        long bin = 1l << (id_of_element & 63);
        if( (x[index] & bin) == 0) {
            lst.add(id_of_element);
            glb_cnt++;
            x[index] |= bin;
        }
    }
    void merge(PointsToSet mergee) {
        // assert(num == mergee.num);
        var mergee_lst = mergee.lst;
        for(var item: mergee_lst) {
            add(item);
        }
    }
    public static void main(String[] args) 
    {
        System.out.println("Fuck");
        PointsToSet s1 = new PointsToSet(64);
        PointsToSet s2 = new PointsToSet(64);
        s1.add(63);
        s1.add(62);
        s2.add(62);
        System.out.println(glb_cnt);
        s2.merge(s1);
        System.out.println(glb_cnt);
    }
}