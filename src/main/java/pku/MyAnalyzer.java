package pku;

import java.util.ArrayList;
import java.util.HashMap;
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
import pascal.taie.language.classes.ClassHierarchy;;

public class MyAnalyzer {
    private static final Logger logger = LogManager.getLogger(IRDumper.class);
    static ArrayList<WStmt> wstmts;
    static ArrayList<WObject> wobjects;
    static ArrayList<WVar> wvars;
    static CallGraph<Invoke, JMethod> CG;
    static ClassHierarchy CH;
    static boolean ispointer(Type t) {
        return t instanceof ClassType || t instanceof ArrayType;
    }
    static void merge(Var merger, Var mergee) {
        if(merger == null || mergee == null) return;
        if(!ispointer(merger.getType()) || !ispointer(mergee.getType())) return;
        int lid = merger.var_id;
        int rid = mergee.var_id;
        if(lid < 0 || rid < 0) { return; }
        wvars.get(lid).pointee.merge(wvars.get(rid).pointee);
    }
    static void merge(int merger, int mergee) {
        if(merger < 0 || mergee < 0) return;
        wvars.get(merger).pointee.merge(wvars.get(mergee).pointee);
    }
    static boolean call_possible(Var caller, JMethod callee) {
        if(PreprocessResult.cast_found) return true;
        var callee_class = callee.getDeclaringClass();
        var obj_list = wvars.get(caller.var_id).pointee.lst;
        for(var obj: obj_list) {
            Type obj_type = wobjects.get(obj).t;
            if( !(obj_type instanceof ClassType) ) continue;
            var obj_class = ((ClassType)obj_type).getJClass();
            if( CH.isSubclass(callee_class, obj_class)|| CH.isSubclass(obj_class, callee_class) )
                return true;
        }
        return false;
    }
    static void update(Stmt stmt, JMethod jm) {
        if(stmt instanceof DefinitionStmt)
        {
            if(stmt instanceof New) 
            {
                var nw = (New)stmt;
                int ptr_id = nw.getLValue().var_id;
                int obj_id = nw.object_id;
                if(obj_id < 0 || ptr_id < 0) return;
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
                    if(!ispointer(jf.getType())) return;
                    var str = jf.getName();
                    int rid = r.var_id;
                    for(var obj: wvars.get(base_id).pointee.lst) {
                        var ptr_id = wobjects.get(obj).field.get(str);
                        if(ptr_id == null) continue;
                        merge(ptr_id, rid);
                    }
                }
                else if(l instanceof StaticFieldAccess)
                {
                    int merger = l.getFieldRef().resolveNullable().var_id;
                    int mergee = r.var_id;
                    merge(merger, mergee);
                }
            }
            else if(stmt instanceof LoadField) 
            {
                var lf = (LoadField)stmt;
                var l = lf.getLValue();
                var r = lf.getRValue();
                if(r instanceof InstanceFieldAccess)
                {
                    int base_id = ((InstanceFieldAccess)r).getBase().var_id;
                    var jf = r.getFieldRef().resolveNullable();
                    if(!ispointer(jf.getType())) return;
                    var str = r.getFieldRef().resolveNullable().getName();
                    int lid = l.var_id;
                    for(var obj: wvars.get(base_id).pointee.lst) {
                        var ptr_id = wobjects.get(obj).field.get(str);
                        if(ptr_id == null) continue;
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
                int base_id = sa.getLValue().getBase().var_id;
                int rid = sa.getRValue().var_id;
                if(sa.getRValue().getType() instanceof ArrayType)
                {
                    merge(base_id, rid);
                }
                else
                {
                    var index = sa.getLValue().getIndex().get_int_const();
                    if(index != null)
                    {
                        if(index != 0)
                        {
                            for(var item: wvars.get(base_id).pointee.lst)
                                merge(wobjects.get(item).ptr1, rid);
                        }
                        else 
                        {
                            for(var item: wvars.get(base_id).pointee.lst)
                                merge(wobjects.get(item).ptr0, rid);
                        }
                    }
                    else
                    {
                        for(var item: wvars.get(base_id).pointee.lst)
                        {
                            merge(wobjects.get(item).ptr0, rid);
                            merge(wobjects.get(item).ptr1, rid);
                        }
                    }
                }
            }
            else if(stmt instanceof LoadArray)
            {
                var la = (LoadArray)stmt;
                int base_id = la.getRValue().getBase().var_id;
                int lid = la.getLValue().var_id;

                if(la.getLValue().getType() instanceof ArrayType)
                {
                    merge(lid, base_id);
                }
                else 
                {
                    var index = la.getRValue().getIndex().get_int_const();
                    if(index != null) 
                    {
                        if(index != 0)
                        {
                            for(var item: wvars.get(base_id).pointee.lst) 
                                merge(lid, wobjects.get(item).ptr1);
                        }
                        else 
                        {
                            for(var item: wvars.get(base_id).pointee.lst) 
                                merge(lid, wobjects.get(item).ptr0);
                        }
                    }
                    else 
                    {
                        for(var item: wvars.get(base_id).pointee.lst) 
                        {
                            merge(lid, wobjects.get(item).ptr0);
                            merge(lid, wobjects.get(item).ptr1);
                        }
                    }
                }
            }
            else if(stmt instanceof Invoke)
            {
                var inv_stmt = (Invoke)stmt;
                var inv_expr = inv_stmt.getInvokeExp();
                var callee_list = CG.getCalleesOf(inv_stmt);
                var arg_list = inv_expr.getArgs();
                if(inv_expr instanceof InvokeStatic)
                {
                    for(var callee: callee_list)
                    {
                        if(callee.isAbstract()) continue;
                        var param_list = callee.getIR().getParams();
                        for(int i = 0; i < param_list.size(); ++i) {
                            var merger = param_list.get(i);
                            var mergee = arg_list.get(i);
                            // logger.info("{} merges {}", merger.getName(), mergee.getName());
                            merge(merger, mergee);
                        }
                    }
                }
                else 
                {
                    for(var callee: callee_list)
                    {
                        if(callee.isAbstract()) continue;
                        // logger.info("{} invokes {}", stmt.get_stmt_id(), callee.getName());
                        // logger.info("{}", call_possible(((InvokeInstanceExp)inv_expr).getBase(), callee));
                        // logger.info("{} calling {}: {}", ((InvokeInstanceExp)inv_expr).getBase(), callee, call_possible( ((InvokeInstanceExp)inv_expr).getBase(), callee));
                        if(!call_possible( ((InvokeInstanceExp)inv_expr).getBase(), callee)) continue;
                        var param_list = callee.getIR().getParams();
                        for(int i = 0; i < param_list.size(); ++i) {
                            var merger = param_list.get(i);
                            var mergee = arg_list.get(i);
                            // logger.info("{} merges {}", merger.getName(), mergee.getName());
                            merge(merger, mergee);
                        }
                        var ths = callee.getIR().getThis();
                        // assert(ths != null);
                        merge(ths, ((InvokeInstanceExp)inv_expr).getBase());
                    }
                }
            }
        }
        else if(stmt instanceof Return)
        {
            var ret = (Return)stmt;
            var retval = ret.getValue();
            if(retval == null || jm.isConstructor()) return;
            var caller_list = CG.getCallersOf(jm);
            for(var caller: caller_list) {
                var def = caller.getDef();
                if(def.isPresent())
                {
                    var receiver = def.get();
                    var inv_expr = caller.getRValue();
                    logger.info("{} called by {}?", stmt.get_stmt_id(), caller.get_stmt_id());
                    if(inv_expr instanceof InvokeStatic || jm.isStatic())
                    {
                        merge((Var)receiver, retval);
                        continue;
                    }
                    // if(inv_expr instanceof InvokeInstanceExp)
                    // assert(receiver instanceof Var);
                    if(call_possible(((InvokeInstanceExp)caller.getRValue()).getBase(), jm))
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
        wvars = PreprocessResult.wvars;

        obj_ids = ppr.obj_ids;
        test_pts = ppr.test_pts;

        CH = World.get().getClassHierarchy();
    }
    static PointerAnalysisResult get_result() {
        var res = new PointerAnalysisResult();
        for(Map.Entry<Integer, Var> entry : test_pts.entrySet()) 
        {
            Integer test_id = entry.getKey();
            TreeSet<Integer> ts = new TreeSet<>();
            var pointees = wvars.get( entry.getValue().var_id ).pointee.lst;
            for(var id: pointees)
                if(wobjects.get(id).tester_id != 0)
                    ts.add(wobjects.get(id).tester_id); 
            res.put(test_id, ts);
            // logger.info("{}: {}", test_id, ts);
        }
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
            last = PointsToSet.glb_cnt;
            update_pass();
        }
        return get_result();
    }
}