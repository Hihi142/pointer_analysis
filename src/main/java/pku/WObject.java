package pku;


import java.util.HashMap;

import pascal.taie.language.classes.JClass;
import pascal.taie.language.type.*;

public class WObject {
    Type t;
    HashMap<String, Integer>field;
    int tester_id = 0;
    int ptr0 = -1, ptr1 = -1;
    static boolean is_pointer(Type t) {
        return t instanceof ClassType || t instanceof ArrayType;
    }
    WObject(Type t0, int tester_id0) {

        t = t0;
        tester_id = tester_id0;
        field = new HashMap<>();
        
        assert(t instanceof ArrayType || t instanceof ClassType);
        if(t instanceof ClassType)
        {
            JClass jc = ((ClassType)t).getJClass();
            while(jc != null) {
                if(!jc.isApplication()) break;
                var jfs = jc.getDeclaredFields();
                for(var jf: jfs) {
                    if(jf.isStatic()) continue;
                    if(is_pointer(jf.getType())) 
                    {
                        int id = PreprocessResult.var_num++;
                        var str = jc.getName() + "::" + jf.getName();
                        field.put(str, id);
                        PreprocessResult.wvars.add(new WVar(null, id));
                    }
                }
                jc = jc.getSuperClass();
            }
        }
        else 
        {
            if( ! is_pointer(((ArrayType)t).baseType())) return;
            ptr0 = PreprocessResult.var_num++;
            PreprocessResult.wvars.add(new WVar(null, ptr0));

            ptr1 = PreprocessResult.var_num++;
            PreprocessResult.wvars.add(new WVar(null, ptr1));
        }
    }
};