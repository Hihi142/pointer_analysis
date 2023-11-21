package pku;


import java.util.HashMap;

import pascal.taie.language.classes.JClass;
import pascal.taie.language.type.*;
import pascal.taie.ir.stmt.*;

public class WObject {
    New nw;
    HashMap<String, Integer>field;
    int tester_id = 0;
    static boolean ispointer(Type t) {
        while(t instanceof ArrayType)
            t = ((ArrayType)t).baseType();
        return t instanceof ClassType;
    }
    WObject(New nw0, int tester_id0) {
        nw = nw0;
        tester_id = tester_id0;
        field = new HashMap<>();
        
        Type t = nw.getRValue().getType();
        while(t instanceof ArrayType)
            t = ((ArrayType)t).baseType();
        
        assert(t instanceof ClassType);
        JClass jc = ((ClassType)t).getJClass();
        while(jc != null) {
            if(!jc.isApplication()) break;
            var jfs = jc.getDeclaredFields();
            for(var jf: jfs) {
                if(jf.isStatic()) continue;
                if(ispointer(jf.getType())) 
                    field.put(jf.getName(), PreprocessResult.var_num++);
            }
            jc = jc.getSuperClass();
        }
    }
};