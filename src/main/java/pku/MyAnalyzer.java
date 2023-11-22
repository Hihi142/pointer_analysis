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
    static CallGraph<Invoke, JMethod> CG;
    static boolean is_pointer(Type t) {
        while(t instanceof ArrayType)
            t = ((ArrayType)t).baseType();
        return t instanceof ClassType;
    }
    static void merge(Var merger, Var mergee)
    {
        if(!is_pointer(merger.getType()) || !is_pointer(mergee.getType()))
            return;
        int lid = merger.var_id;
        int rid = mergee.var_id;
        if(lid < 0 || rid < 0)
        {
            return; 
        }
        wvars.get(lid).pointee.merge(wvars.get(rid).pointee);
    }
    static void merge(int merger, int mergee)
    {
        assert(merger >= 0 && mergee >= 0);
        wvars.get(merger).pointee.merge(wvars.get(mergee).pointee);
    }
    static void update(Stmt stmt, JMethod jm) {
        if(stmt instanceof DefinitionStmt)
        {
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
                merge(cp.getLValue(), cp.getRValue());
            }
            else if(stmt instanceof StoreField)
            {
                var sf = (StoreField)stmt;
                var r = sf.getRValue();
                var l = sf.getLValue();
                if(l instanceof InstanceFieldAccess)
                {
                    int base_id = ((InstanceFieldAccess)l).getBase().var_id;
                    var jf = l.getFieldRef().resolveNullable();
                    if(!is_pointer(jf.getType())) return;
                    var str = jf.getName();
                    int rid = r.var_id;
                    for(var obj: wvars.get(base_id).pointee.lst) {
                        var ptr_id = wobjects.get(obj).field.get(str);
                        merge(ptr_id, rid);
                    }
                }
                else if(l instanceof StaticFieldAccess)
                {
                    int merger = l.getFieldRef().resolveNullable().var_id;
                    int mergee = r.var_id;
                    merge(merger, mergee);
                }
                else assert(false);
            }
            else if(stmt instanceof LoadField) 
            {
                var lf = (LoadField)stmt;
                var l = lf.getLValue();
                var r = lf.getRValue();
                if(r instanceof InstanceFieldAccess)
                {
                    int base_id = ((InstanceFieldAccess)r).getBase().var_id;
                    var str = r.getFieldRef().resolveNullable().getName();
                    int lid = l.var_id;
                    for(var obj: wvars.get(base_id).pointee.lst) {
                        var ptr_id = wobjects.get(obj).field.get(str);
                        merge(lid, ptr_id);
                    }
                }
                else if(r instanceof StaticFieldAccess)
                {
                    int merger = l.var_id;
                    int mergee = r.getFieldRef().resolveNullable().var_id;
                    merge(merger, mergee);
                }
            }
            else if(stmt instanceof StoreArray)
            {
                var sa = (StoreArray)stmt;
                merge(sa.getLValue().getBase(), sa.getRValue());
            }
            else if(stmt instanceof LoadArray)
            {
                var la = (LoadArray)stmt;
                merge(la.getLValue(), la.getRValue().getBase());
            }
            else if(stmt instanceof Invoke)
            {
                
                var inv_stmt = (Invoke)stmt;
                var inv_expr = inv_stmt.getInvokeExp();
                var callee_list = CG.getCalleesOf(inv_stmt);
                var arg_list = inv_expr.getArgs();
                for(var callee: callee_list)
                {
                    logger.info("{} invokes {}", stmt.get_stmt_id(), callee.getName());
                    var param_list = callee.getIR().getParams();
                    assert(param_list.size() == arg_list.size());
                    for(int i = 0; i < param_list.size(); ++i) {
                        var merger = param_list.get(i);
                        var mergee = arg_list.get(i);
                        logger.info("{} merges {}", merger.getName(), mergee.getName());
                        merge(merger, mergee);
                    }
                    if(inv_expr instanceof InvokeInstanceExp)
                    {
                        var ths = callee.getIR().getThis();
                        assert(ths != null);
                        merge(ths, ((InvokeInstanceExp)inv_expr).getBase());
                    }
                    else if(inv_expr instanceof InvokeStatic)
                    {
                        assert(callee.isStatic());
                    }
                }
            }
        }
        else if(stmt instanceof Return)
        {
            var ret = (Return)stmt;
            var retval = ret.getValue();
            assert(retval != null);
            if(retval == null) return;
            var caller_list = CG.getCallersOf(jm);
            for(var caller: caller_list) {
                var def = caller.getDef();
                if(def.isPresent())
                {
                    var receiver = def.get();
                    assert(receiver instanceof Var);
                    merge((Var)receiver, retval);
                }
            }
        }
        else if(stmt instanceof Cast)
        { // ???

        }
        else if(stmt instanceof Throw)
        { // ???
            
        }
        else if(stmt instanceof Catch)
        { // ???

        }
        else if(stmt instanceof Monitor)
        { // ???

        }
    }

    static Map<New, Integer>obj_ids;
    static Map<Integer, Var>test_pts;
    static void init(PreprocessResult ppr) {
        wstmts = ppr.wstmts;
        wobjects = ppr.wobjects;
        wvars = ppr.wvars;

        obj_ids = ppr.obj_ids;
        test_pts = ppr.test_pts;
        

        CG = World.get().getResult(CallGraphBuilder.ID);
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
                        update(stmt, method);
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