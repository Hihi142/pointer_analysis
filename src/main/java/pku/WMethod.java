package pku;

import pascal.taie.language.classes.*;
import pascal.taie.ir.stmt.Invoke;
import java.util.ArrayList;
import java.util.HashMap;

public class WMethod {
    static int method_num = 0;
    JMethod jm;
    int id = -1;
    int versions = 0;
    ArrayList< ArrayList<Invoke> >returnee;
    ArrayList< ArrayList<Integer> >returnee_version;
    HashMap<Long, Integer>rem;

    WMethod(JMethod jm0) {
        jm = jm0; 
        id = method_num++;
        returnee = new ArrayList<>();
        returnee_version = new ArrayList<>();
        rem = new HashMap<>();
    }
    int new_version() {
        int ret = versions++;
        var nil = new ArrayList<Invoke>();
        var nil_int1 = new ArrayList<Integer>();
        returnee.add(nil);
        returnee_version.add(nil_int1);

        var stmts = jm.getIR().getStmts();
        for(var stmt: stmts) {
            if(!(stmt instanceof Invoke)) continue;
            var nil_int2 = new ArrayList<Integer>();
            var nil_callee = new ArrayList<WMethod>();
            var inv = (Invoke)stmt;
            inv.callee_versions.add(nil_int2);
            inv.callees.add(nil_callee);
        }
        return ret;
    }
    void link_caller(Invoke caller, int caller_version, int callee_version) {
        caller.callees.get(caller_version).add(this);
        caller.callee_versions.get(caller_version).add(callee_version);

        returnee.get(callee_version).add(caller);
        returnee_version.get(callee_version).add(caller_version);
    }
}
