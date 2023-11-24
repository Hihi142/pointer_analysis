package pku;

import pascal.taie.language.classes.*;
import pascal.taie.ir.stmt.Invoke;
import java.util.ArrayList;

public class WMethod {
    static int method_num = 0;
    JMethod jm;
    int id = -1;
    int versions;
    ArrayList< Invoke >returnee;
    WMethod(JMethod jm0) {
        jm = jm0; 
        id = method_num++;
        callers = new ArrayList<>();
        returnee = new ArrayList<>()
    }
}
