package pku;


import java.util.HashMap;

import pascal.taie.World;
import pascal.taie.analysis.misc.IRDumper;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.InvokeStatic;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.*;
import pascal.taie.language.type.Type;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.classes.JMethod;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

public class PreprocessResult {

    private static final Logger logger = LogManager.getLogger(IRDumper.class);

    public ClassHierarchy CH;
    public final Map<New, Integer>obj_ids;
    public final Map<Integer, Var>test_pts;
    public void alloc(New stmt, int id) { obj_ids.put(stmt, id); }
    public void test(int id, Var v) { test_pts.put(id, v); }
    public void gather_test_info() {
        World.get().getClassHierarchy().applicationClasses().forEach(jclass->{
            jclass.getDeclaredMethods().forEach(method->{
                if(!method.isAbstract())
                {
                    var ir = method.getIR();
                    var stmts = ir.getStmts();
                    Integer id = 0;
                    for (var stmt : stmts) 
                    {
                        if(stmt instanceof Invoke)
                        {
                            var exp = ((Invoke) stmt).getInvokeExp();
                            if(exp instanceof InvokeStatic)
                            {
                                var methodRef = ((InvokeStatic)exp).getMethodRef();
                                var className = methodRef.getDeclaringClass().getName();
                                var methodName = methodRef.getName();
                                if(className.equals("benchmark.internal.Benchmark")
                                || className.equals("benchmark.internal.BenchmarkN"))
                                {
                                    if(methodName.equals("alloc"))
                                    {
                                        var lit = exp.getArg(0).getConstValue();
                                        assert lit instanceof IntLiteral;
                                        id = ((IntLiteral)lit).getNumber();
                                    }
                                    else if(methodName.equals("test"))
                                    {
                                        var lit = exp.getArg(0).getConstValue();
                                        assert lit instanceof IntLiteral;
                                        var test_id = ((IntLiteral)lit).getNumber();
                                        var pt = exp.getArg(1);
                                        this.test(test_id, pt);
                                    }
                                }
                            }
                        }
                        else if(stmt instanceof New)
                        {
                            if(id != 0) this.alloc((New)stmt, id);
                        }
                    }
                }
            });
        });
    }

    public void set_all_callees_of(Invoke inv) {
        var jm = inv.getMethodRef().resolve();
        var name = jm.getName();
        var typs = jm.getParamTypes();
        var caller_class = jm.getDeclaringClass();
        if(!caller_class.isApplication()) return;
        World.get().getClassHierarchy().applicationClasses().forEach(jclass->{
            if(CH.isSubclass(caller_class, jclass)) {
                    for(var jmethod: jclass.getDeclaredMethods()) {
                    if(jmethod.isAbstract()) continue;
                    if(!name.equals(jmethod.getName())) continue;
                    if(!typs.equals(jmethod.getParamTypes())) continue;
                    inv.all_callees.add(jmethod);
                }
            }
        });
    }
    int[] stk = new int[1000000];
    int top = 0;
    long get_context_value() {
        return stk[top];
        // if(top == 0) return 0;
        // else if(top == 1) return stk[1];
        // else return (stk[top] + stk[top - 1] * 100003l) % 1000000007l;
    }
    private void dfs(WMethod wm, int version) {
        // logger.info("starting dfs: {}[{}]", wm.jm.getName(), version);
        var stmts = wm.jm.getIR().getStmts();
        for(var stmt: stmts) {
            if(!(stmt instanceof Invoke)) continue;
            var inv = (Invoke)stmt;
            var callee_list = inv.all_callees;
            // logger.info("   invoke {}", callee_list.size());

            for(var callee: callee_list)
            {
                // logger.info("    {}", callee.getName());
                if(callee.isAbstract()) continue;
                var wcallee = callee.wrapper;
                if(wcallee == null) continue;
                // logger.info("   valid invoke");

                stk[++top] = wcallee.id + inv.call_id * 4321;
                long context = get_context_value();
                if(wcallee.rem.containsKey(context)) {
                    int callee_version = wcallee.rem.get(context);
                    // logger.info("{}[{}]     ->    {}[{}].", wm.jm.getName(), version, wcallee.jm.getName(), callee_version);
                    wcallee.link_caller(inv, version, callee_version);
                }
                else {
                    int callee_version = wcallee.new_version();
                    wcallee.link_caller(inv, version, callee_version);
                    wcallee.rem.put(context, callee_version);
                    // logger.info("{}[{}]     ->    {}[{}].", wm.jm.getName(), version, wcallee.jm.getName(), callee_version);
                    dfs(wcallee, callee_version);
                }
                top--;
            }
        }
        // logger.info("ending dfs: {}[{}]", wm.jm.getName(), version);
    }
    private void method_init() {
        World.get().getClassHierarchy().applicationClasses().forEach(jclass->{
            for(var method: jclass.getDeclaredMethods()) {
                if(method.isAbstract()) continue;
                WMethod wm = new WMethod(method);
                method.wrapper = wm;
            };
        });

        World.get().getClassHierarchy().applicationClasses().forEach(jclass->{
            for(var method: jclass.getDeclaredMethods()) {
                if(method.isAbstract()) continue;
                var wm = method.wrapper;
                if(wm == null) continue;
                // if(wm.versions > 0) continue;
                int version = wm.new_version();
                top = 1;
                stk[top] = ++call_num;
                // logger.info("");
                dfs(wm, version);
            }
        });
    }

    public ArrayList<WObject> wobjects;
    static ArrayList<WVar> wvars;
    

    public PreprocessResult(){
        obj_ids = new HashMap<New, Integer>();
        test_pts = new HashMap<Integer,Var>();
        wobjects = new ArrayList<WObject>();
        wvars = new ArrayList<WVar>();
    }

    static int call_num = 0;
    static int stmt_num = 0;
    static int object_num = 0;
    static int var_num = 0;
    static boolean case_found = false;

    void first_pass() {
        World.get().getClassHierarchy().applicationClasses().forEach(jclass->{
            for(var method: jclass.getDeclaredMethods()) {
                if(method.isAbstract()) continue;
                var stmts = method.getIR().getStmts();
                for (Stmt stmt : stmts) {
                    if(stmt instanceof DefinitionStmt) {
                        var asgn = (DefinitionStmt)stmt;
                        var lhs = asgn.getLValue();
                        var rhs = asgn.getRValue();
                        if(lhs instanceof Var && lhs.getType().getName() == "int" )
                        {
                            ((Var)lhs).assigns++;
                            if(rhs instanceof IntLiteral)
                            {
                                var lit = (IntLiteral)rhs;
                                ((Var)lhs).assign_value = lit.getNumber();
                            }
                            else ((Var)lhs).assigns = 666;
                        }
                    }
                    if(stmt instanceof Cast) {
                        case_found = true;
                        // throw(new NullPointerException());
                    }
                    stmt.set_stmt_id(stmt_num++);
                    if(stmt instanceof Invoke) {
                        var invoke = (Invoke)stmt;
                        set_all_callees_of(invoke);
                        invoke.call_id = ++call_num;
                    }
                }
            }
        });
    }


    static boolean ispointer(Type t) {
        return t instanceof ClassType || t instanceof ArrayType;
    }
    private void count_pass_method(JMethod jm, int version) {
        var ir = jm.getIR();
        var stmts = ir.getStmts();
        for (Stmt stmt : stmts) {
            if(stmt instanceof New)
            {
                var nw = (New)stmt; 
                Type t = nw.getRValue().getType();
                int obj_id = object_num++;
                nw.object_id.add(obj_id);
                if(obj_ids.containsKey(nw))
                    wobjects.add(new WObject(t, obj_ids.get(nw)));
                else
                    wobjects.add(new WObject(t, 0));
            }
        }
        var varlist = ir.getVars();
        for(var v: varlist) {
            if(ispointer(v.getType())) {
                int id = var_num++;
                v.var_id.add(id);
                WVar wv = new WVar(v, id);
                wvars.add(wv);
            }
        }
    }
    private void count_pass() {
        World.get().getClassHierarchy().applicationClasses().forEach(jclass->{
            for(var method: jclass.getDeclaredMethods()) {
                if(method.isAbstract()) continue;
                var wmethod = method.wrapper;
                for(int i = 0; i < wmethod.versions; ++i)
                    count_pass_method(method, i);
            }
        });
    }
    private void gather_static_pointers() {
        World.get().getClassHierarchy().applicationClasses().forEach(jclass->{
            jclass.getDeclaredFields().forEach(field->{
                if(field.isStatic() && ispointer(field.getType())) {
                    int id = var_num++;
                    field.var_id = id;
                    wvars.add(new WVar(null, id));
                }
            });
        });
    }
    public void init() {
        CH = World.get().getClassHierarchy();
        first_pass();
        // MyDumper.dump(this);
        logger.info("---------------------------------------");
        method_init();
        count_pass();
        gather_static_pointers();
        for(var wvar: wvars) {
            wvar.pointee = new PointsToSet(object_num);
        }
    }
}