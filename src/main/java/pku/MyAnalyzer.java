package pku;

import java.util.ArrayList;
import java.util.Map;

import pascal.taie.World;

import pascal.taie.ir.stmt.*;
import pascal.taie.ir.exp.*;

public class MyAnalyzer {
    
    static ArrayList<WStmt> wstmts;
    static ArrayList<WObject> wobjects;
    static ArrayList<WVar> wvars;

    static void update(Stmt stmt) {
        if(stmt instanceof DefinitionStmt) { }
        else if(stmt instanceof JumpStmt) { }
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
        
        return null;
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
    static void analyze() {
        int last = -1;
        while(PointsToSet.glb_cnt > last) {
            last = PointsToSet.glb_cnt;
            update_pass();
        }
    }
}