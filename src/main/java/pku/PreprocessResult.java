package pku;


import java.util.HashMap;

import pascal.taie.World;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.InvokeStatic;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;

import java.util.Map;

import java.util.ArrayList;

public class PreprocessResult {
    
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

    public ArrayList<WStmt> wstmts;
    public ArrayList<WObject> wobjects;
    static ArrayList<WVar> wvars;
    public PreprocessResult(){
        obj_ids = new HashMap<New, Integer>();
        test_pts = new HashMap<Integer,Var>();
        wstmts = new ArrayList<WStmt>();
        wobjects = new ArrayList<WObject>();
        wvars = new ArrayList<WVar>();
    }
    public void alloc(New stmt, int id) { obj_ids.put(stmt, id); }
    public void test(int id, Var v) { test_pts.put(id, v); }

    static int call_num = 0;
    static int stmt_num = 0;
    static int object_num = 0;
    static int var_num = 0;

    static boolean ispointer(Type t) {
        while(t instanceof ArrayType)
            t = ((ArrayType)t).baseType();
        return t instanceof ClassType;
    }
    Type dearray(Type t) {
        while(t instanceof ArrayType)
            t = ((ArrayType)t).baseType();
        return t;
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
                if(obj_ids.containsKey(nw))
                    wobjects.add(new WObject(nw, obj_ids.get(nw)));
                else
                    wobjects.add(new WObject(nw, 0));
            }
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
                if(field.isStatic())
                {
                    int id = var_num++;
                    field.var_id = id; 
                    wvars.add(new WVar(null, id));
                }
            });
        });
    }
    public void init() {
        count_pass();
        gather_static_pointers();
        // MyDumper.dump(this);
        for(var wvar: wvars) {
            wvar.pointee = new PointsToSet(object_num);
        }
    }
}