package pku;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeSet;

import pascal.taie.World;

import pascal.taie.ir.stmt.*;
import pascal.taie.language.type.*;
import pascal.taie.ir.exp.*;
import pascal.taie.analysis.graph.callgraph.*;
import pascal.taie.analysis.misc.IRDumper;
import pascal.taie.language.classes.JMethod;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MyAnalyzer {
    private static final Logger logger = LogManager.getLogger(IRDumper.class);
    static ArrayList<WStmt> wstmts;
    static ArrayList<WObject> wobjects;
    static ArrayList<WVar> wvars;
    static boolean is_pointer(Type t) {
        while(t instanceof ArrayType)
            t = ((ArrayType)t).baseType();
        return t instanceof ClassType;
    }
    static void update(Stmt stmt) {
        if(stmt instanceof New) 
        {
            var nw = (New)stmt;
            int ptr_id = nw.getLValue().var_id;
            int obj_id = nw.object_id;
            wvars.get(ptr_id).pointee.add(obj_id);
        }
        else if(stmt instanceof Copy)
        {
            var cp = (Copy)stmt;
            Var l = cp.getLValue();
            if(!is_pointer(l.getType())) return;
            var lid = l.var_id;
            var rid = cp.getRValue().var_id;
            wvars.get(lid).pointee.merge(wvars.get(rid).pointee);
        }
        // else if(stmt instanceof FieldStmt)
        // {
        //     var fs = (FieldStmt)stmt;
        // }
        // else if(stmt instance )
        // else if(stmt instanceof Invoke) 
        // {
        //     var inv = (Invoke)stmt;
        // }
    }

    static Map<New, Integer>obj_ids;
    static Map<Integer, Var>test_pts;
    static void init(PreprocessResult ppr) {
        wstmts = ppr.wstmts;
        wobjects = ppr.wobjects;
        wvars = ppr.wvars;

        obj_ids = ppr.obj_ids;
        test_pts = ppr.test_pts;
    }
    static PointerAnalysisResult get_result() {
        var res = new PointerAnalysisResult();
        for(Map.Entry<Integer, Var> entry : test_pts.entrySet()) 
        {
            Integer test_id = entry.getKey();
            TreeSet<Integer> ts = new TreeSet<>();
            var pointees = wvars.get( entry.getValue().var_id ).pointee.lst;
            for(var id: pointees)
                if(wobjects.get(id).tester_id > 0)
                    ts.add(wobjects.get(id).tester_id); 
            res.put(test_id, ts);
            logger.info("{}: {}", test_id, ts);
        }
        logger.info("dick");
        return res;
    }
    static void update_pass() {
        World.get().getClassHierarchy().applicationClasses().forEach(jclass->{
            jclass.getDeclaredMethods().forEach(method->{
                if(!method.isAbstract())
                {
                    var ir = method.getIR();
                    var stmts = ir.getStmts();
                    for (var stmt : stmts) 
                        update(stmt);
                }
            });
        });
    }
    static PointerAnalysisResult analyze() {
        int last = -1;
        while(PointsToSet.glb_cnt > last) {
            logger.info("Shit\n");
            last = PointsToSet.glb_cnt;
            update_pass();
        }
        logger.info("Fuck\n");
        return get_result();
    }
}