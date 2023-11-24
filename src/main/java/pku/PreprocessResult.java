package pku;


import java.util.HashMap;

import pascal.taie.World;
import pascal.taie.analysis.misc.IRDumper;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.InvokeStatic;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignStmt;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.DefinitionStmt;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

public class PreprocessResult {
    private static final Logger logger = LogManager.getLogger(IRDumper.class);
    public final Map<New, Integer>obj_ids;
    public final Map<Integer, Var>test_pts;
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

    public ArrayList<WMethod> wmethods;
    private void method_init() {
        World.get().getClassHierarchy().applicationClasses().forEach(jclass->{
            for(var method: jclass.getDeclaredMethods()) {
                if(method.isAbstract()) continue;
                WMethod wm = new WMethod(method);
                wmethods.add(wm);
                method.wrapper = wm;
            };
        });

        World.get().getClassHierarchy().applicationClasses().forEach(jclass->{
            for(var method: jclass.getDeclaredMethods()) {
                if(method.isAbstract()) continue;
                var stmts = method.getIR().getStmts();
                for(var stmt: stmts) {
                    if(!(stmt instanceof Invoke)) continue;
                    var inv = (Invoke)stmt;
                    inv.callee_versions = new ArrayList<>();
                    inv.callees = new ArrayList<>();
                    var callee_list = MyAnalyzer.CG.getCalleesOf(inv);
                    for(var callee: callee_list)
                    {
                        if(callee.isAbstract()) continue;
                        if(callee.wrapper == null) continue;
                        var wcallee = callee.wrapper;
                        inv.callees.add(wcallee);
                        inv.callee_versions.add(wcallee.versions++);
                        wcallee.returnee.add(inv);
                    }
                }
            };
        });
    }

    public ArrayList<WObject> wobjects;
    static ArrayList<WVar> wvars;
    

    public PreprocessResult(){
        obj_ids = new HashMap<New, Integer>();
        test_pts = new HashMap<Integer,Var>();
        wobjects = new ArrayList<WObject>();
        wvars = new ArrayList<WVar>();
        wmethods = new ArrayList<>();
    }
    public void alloc(New stmt, int id) { obj_ids.put(stmt, id); }
    public void test(int id, Var v) { test_pts.put(id, v); }

    static int call_num = 0;
    static int stmt_num = 0;
    static int object_num = 0;
    static int var_num = 0;

    static boolean cast_found = false;
    static boolean exception_found = false;

    


    static boolean ispointer(Type t) {
        return t instanceof ClassType || t instanceof ArrayType;
    }
    private void count_pass_ir(IR ir) {
        var stmts = ir.getStmts();
        for (Stmt stmt : stmts) {
            stmt.set_stmt_id(stmt_num++);
            if(stmt instanceof Invoke)
            {
                var invoke = (Invoke)stmt;
                invoke.call_id = ++call_num;
            }
            if(stmt instanceof New)
            {
                var nw = (New)stmt; 
                Type t = nw.getRValue().getType();
                nw.object_id = object_num++;
                if(obj_ids.containsKey(nw))
                    wobjects.add(new WObject(t, obj_ids.get(nw)));
                else
                    wobjects.add(new WObject(t, 0));
            }
            if(stmt instanceof DefinitionStmt)
            {
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
            if(stmt instanceof Cast)
            {
                cast_found = true;
                // throw(new NullPointerException());
            }
            if(stmt instanceof Throw || stmt instanceof Catch)
                exception_found = true;
        }
        var varlist = ir.getVars();
        for(var v: varlist) {
            if(ispointer(v.getType()))
            {
                int id = var_num++;
                v.var_id = id;
                WVar wv = new WVar(v, id);
                wvars.add(wv);
            }
        }
    }
    private void count_pass() {
        World.get().getClassHierarchy().applicationClasses().forEach(jclass->{
            jclass.getDeclaredMethods().forEach(method->{
                if(!method.isAbstract())
                    count_pass_ir(method.getIR());
            });
        });
    }
    private void gather_static_pointers() {
        World.get().getClassHierarchy().applicationClasses().forEach(jclass->{    
            jclass.getDeclaredFields().forEach(field->{
                if(field.isStatic() && ispointer(field.getType()))
                {
                    int id = var_num++;
                    field.var_id = id; 
                    wvars.add(new WVar(null, id));
                }
            });
        });
    }
    public void init() {
        method_init();
        count_pass();
        gather_static_pointers();
        MyDumper.dump(this);
        logger.info("Cast Found: {}", cast_found);
        for(var wvar: wvars) {
            wvar.pointee = new PointsToSet(object_num);
        }
    }
}