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
        nw.object_id = PreprocessResult.object_num++;
        tester_id = tester_id0;
        field = new HashMap<>();
        
        Type t = nw.getRValue().getType();
        while(t instanceof ArrayType)
            t = ((ArrayType)t).baseType();
        
        if(!(t instanceof ClassType)) return;
        JClass jc = ((ClassType)t).getJClass();
        while(jc != null) {
            if(!jc.isApplication()) break;
            var jfs = jc.getDeclaredFields();
            for(var jf: jfs) {
                if(jf.isStatic()) continue;
                if(ispointer(jf.getType())) 
                {
                    int id = PreprocessResult.var_num++;
                    field.put(jf.getName(), id);
                    PreprocessResult.wvars.add(new WVar(null, id));
                }
            }
            jc = jc.getSuperClass();
        }
    }
};