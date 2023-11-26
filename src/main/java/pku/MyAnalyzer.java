package pku;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeSet;

import pascal.taie.World;

import pascal.taie.ir.stmt.*;
import pascal.taie.language.type.*;
import pascal.taie.ir.exp.*;
import pascal.taie.analysis.misc.IRDumper;
import pascal.taie.language.classes.JMethod;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.language.classes.ClassHierarchy;

public class MyAnalyzer {
    private static final Logger logger = LogManager.getLogger(IRDumper.class);
    static ArrayList<WObject> wobjects;
    static ArrayList<WVar> wvars;
    static ClassHierarchy CH;
    static boolean ispointer(Type t) {
        return t instanceof ClassType || t instanceof ArrayType;
    }
    static void merge(Var merger, Var mergee, int version) {
        if(merger == null || mergee == null) return;
        if(!ispointer(merger.getType()) || !ispointer(mergee.getType())) return;
        int lid = merger.var_id.get(version);
        int rid = mergee.var_id.get(version);
        if(lid < 0 || rid < 0) { return; }
        wvars.get(lid).pointee.merge(wvars.get(rid).pointee);
    }
    static void merge(int merger, int mergee) {
        if(merger < 0 || mergee < 0) return;
        wvars.get(merger).pointee.merge(wvars.get(mergee).pointee);
    }
    static boolean call_possible(Var caller, int version, JMethod callee) {
        var callee_class = callee.getDeclaringClass();
        var var_id = caller.var_id.get(version);
        // for(var var_id: var_id_list) {
            var obj_list = wvars.get(var_id).pointee.lst;
            for(var obj: obj_list) {
                Type obj_type = wobjects.get(obj).t;
                if( !(obj_type instanceof ClassType) ) continue;
                var obj_class = ((ClassType)obj_type).getJClass();
                if( CH.isSubclass(callee_class, obj_class) )
                    return true;
            }
        // }
        return false;
    }
    static void update(Stmt stmt, WMethod wjm, int version) {
        // logger.info("{} {} {}", stmt.get_stmt_id(), wjm.jm.getName(), version);
        if(stmt instanceof DefinitionStmt)
        {
            if(stmt instanceof New) {
                var nw = (New)stmt;
                int ptr_id = nw.getLValue().var_id.get(version);
                int obj_id = nw.object_id.get(version);
                if(obj_id < 0 || ptr_id < 0) return;
                // logger.info("New:  ptr_id: {} <- obj_id: {}", ptr_id, obj_id);
                wvars.get(ptr_id).pointee.add(obj_id);
            }
            else if(stmt instanceof Copy) {
                var cp = (Copy)stmt;
                // logger.info("Copy!");
                merge(cp.getLValue(), cp.getRValue(), version);
            }
            else if(stmt instanceof StoreField) {
                var sf = (StoreField)stmt;
                var r = sf.getRValue();
                var l = sf.getLValue();
                if(l instanceof InstanceFieldAccess)
                {
                    int base_id = ((InstanceFieldAccess)l).getBase().var_id.get(version);
                    var jf = l.getFieldRef().resolveNullable();
                    if(!ispointer(jf.getType())) return;
                    var str = jf.getDeclaringClass().getName() + "::" + jf.getName();
                    int rid = r.var_id.get(version);
                    for(var obj: wvars.get(base_id).pointee.lst) {
                        var ptr_id = wobjects.get(obj).field.get(str);
                        if(ptr_id == null) continue;
                        merge(ptr_id, rid);
                    }
                }
                else if(l instanceof StaticFieldAccess)
                {
                    if(!ispointer(l.getType()) || !ispointer(r.getType())) return;
                    int merger = l.getFieldRef().resolveNullable().var_id;
                    int mergee = r.var_id.get(version);
                    merge(merger, mergee);
                }
            }
            else if(stmt instanceof LoadField) {
                var lf = (LoadField)stmt;
                var l = lf.getLValue();
                var r = lf.getRValue();
                if(r instanceof InstanceFieldAccess)
                {
                    int base_id = ((InstanceFieldAccess)r).getBase().var_id.get(version);
                    var jf = r.getFieldRef().resolveNullable();
                    if(!ispointer(jf.getType())) return;
                    var str = jf.getDeclaringClass().getName() + "::" + jf.getName();
                    int lid = l.var_id.get(version);
                    for(var obj: wvars.get(base_id).pointee.lst) {
                        var ptr_id = wobjects.get(obj).field.get(str);
                        if(ptr_id == null) continue;
                        merge(lid, ptr_id);
                    }
                }
                else if(r instanceof StaticFieldAccess)
                {
                    if(!ispointer(l.getType()) || !ispointer(r.getType())) return;
                    int merger = l.var_id.get(version);
                    int mergee = r.getFieldRef().resolveNullable().var_id;
                    merge(merger, mergee);
                }
            }
            else if(stmt instanceof StoreArray) {
                var sa = (StoreArray)stmt;
                int base_id = sa.getLValue().getBase().var_id.get(version);
                int rid = sa.getRValue().var_id.get(version);
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
            else if(stmt instanceof LoadArray) {
                var la = (LoadArray)stmt;
                int base_id = la.getRValue().getBase().var_id.get(version);
                int lid = la.getLValue().var_id.get(version);

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
                var arg_list = inv_expr.getArgs();
                var callees = inv_stmt.callees.get(version);
                var callee_versions = inv_stmt.callee_versions.get(version);
                if(inv_expr instanceof InvokeStatic)
                {
                    for(int j = 0; j < callees.size(); ++j)
                    {
                        var callee = callees.get(j).jm;
                        var callee_version = callee_versions.get(j);
                        if(callee.isAbstract()) continue;
                        var param_list = callee.getIR().getParams();
                        for(int i = 0; i < param_list.size(); ++i) {
                            var merger = param_list.get(i);
                            var mergee = arg_list.get(i);
                            if(!ispointer(merger.getType()) || !ispointer(mergee.getType())) continue;
                            merge(merger.var_id.get(callee_version), mergee.var_id.get(version));
                        }
                    }
                }
                else 
                {
                    for(int j = 0; j < callees.size(); ++j)
                    {
                        var callee = callees.get(j).jm;
                        var callee_version = callee_versions.get(j);
                        if(callee.isAbstract()) continue;
                        if(!call_possible( ((InvokeInstanceExp)inv_expr).getBase(), version, callee)) continue;
                        var param_list = callee.getIR().getParams();
                        for(int i = 0; i < param_list.size(); ++i) {
                            var merger = param_list.get(i);
                            var mergee = arg_list.get(i);
                            if(!ispointer(merger.getType()) || !ispointer(mergee.getType())) continue;
                            merge(merger.var_id.get(callee_version), mergee.var_id.get(version));
                        }
                        var ths = callee.getIR().getThis();
                        merge(ths.var_id.get(callee_version), ((InvokeInstanceExp)inv_expr).getBase().var_id.get(version));
                    }
                }
            }
        }
        else if(stmt instanceof Return)
        {
            var ret = (Return)stmt;
            var retval = ret.getValue();
            var jm = wjm.jm;
            if(retval == null || jm.isConstructor()) return;
            var caller_list = wjm.returnee.get(version);
            var caller_version_list = wjm.returnee_version.get(version);
            for(int j = 0; j < caller_list.size(); ++j) {
                var caller = caller_list.get(j);
                var caller_version = caller_version_list.get(j);
                if(caller == null) continue;

                var def = caller.getDef();
                if(def.isPresent())
                {
                    var receiver = (Var)def.get();
                    var inv_expr = caller.getRValue();
                    // logger.info("{} called by {}?", stmt.get_stmt_id(), caller.get_stmt_id());
                    if(inv_expr instanceof InvokeStatic || jm.isStatic())
                        merge(receiver.var_id.get(caller_version), retval.var_id.get(version));
                    // if(inv_expr instanceof InvokeInstanceExp)
                    // assert(receiver instanceof Var);
                    else
                    {
                        var caller_var = ((InvokeInstanceExp)caller.getRValue()).getBase();
                        // var receiver_ids = ((Var)receiver).var_id;
                        // for(int i = 0; i < receiver_ids.size(); ++i)
                            if(call_possible(caller_var, caller_version, jm))
                                merge( receiver.var_id.get(caller_version), retval.var_id.get(version) );
                        // for(var receiver_id: ((Var)receiver).var_id)
                            // merge(receiver_id, retval.var_id.get(version));
                    }
                }
            }
        }
    }

    static Map<New, Integer>obj_ids;
    static Map<Integer, Var>test_pts;
    static void update_pass() {
        World.get().getClassHierarchy().applicationClasses().forEach(jclass->{
            for(var method: jclass.getDeclaredMethods()) {
                if(method.isAbstract()) continue;
                var wmethod = method.wrapper;
                if(wmethod == null) continue;
                var ir = method.getIR();
                var stmts = ir.getStmts();
                for(int i = 0; i < wmethod.versions; ++i)
                    for(var stmt : stmts) 
                        update(stmt, wmethod, i);
            };
        });
    }
    static void init(PreprocessResult ppr) {
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
            var var_id_set = entry.getValue().var_id;
            for(var var_id: var_id_set) {
                logger.info("{}", var_id);
                var pointees = wvars.get(var_id).pointee.lst;
                for(var obj_id: pointees)
                    if(wobjects.get(obj_id).tester_id != 0)
                        ts.add(wobjects.get(obj_id).tester_id); 
            }
            res.put(test_id, ts);
            // logger.info("{}: {}", test_id, ts);
        }
        return res;
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