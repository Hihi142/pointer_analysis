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
import pascal.taie.language.classes.JClass;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;

import java.util.Map;

import java.util.ArrayList;

public class PreprocessResult {
    
    public final Map<New, Integer>obj_ids;
    public final Map<Integer, Var>test_pts;
    public ArrayList<WStmt> wstmts;
    public ArrayList<WObject> wobjects;
    public PreprocessResult(){
        obj_ids = new HashMap<New, Integer>();
        test_pts = new HashMap<Integer,Var>();
        wstmts = new ArrayList<WStmt>();
        wobjects = new ArrayList<WObject>();
    }
    public void alloc(New stmt, int id) { obj_ids.put(stmt, id); }
    public void test(int id, Var v) { test_pts.put(id, v); }

    static int call_num = 0;
    static int stmt_num = 0;
    static int object_num = 0;
    static int var_num = 0;

    static boolean ispointer(Type t) {
        assert(!(t instanceof ArrayType) );
        return t instanceof ClassType;
    }
    Type dearray(Type t) {
        while(t instanceof ArrayType)
            t = ((ArrayType)t).baseType();
        return t;
    }
    WObject new_object(Type t) {
        WObject wo = new WObject();
        t = dearray(t);
        if(t instanceof ClassType) {
            JClass jc = ((ClassType)t).getJClass();
            while(jc != null) {
                if(!jc.isApplication()) break;
                var jfs = jc.getDeclaredFields();
                for(var jf: jfs) {
                    if(jf.isStatic()) continue;
                    if(ispointer(jf.getType())) 
                        wo.field.put(jf.getName(), var_num++);
                }
                jc = jc.getOuterClass();
            }
        }
        return wo;
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
                wobjects.add(new_object(nw.getRValue().getType()));
                nw.object_id = object_num++;
            }
            var wlval = stmt.getDef();
            if(wlval.isPresent())
            {
                var lval = wlval.get();
                if(lval instanceof Var){
                    Var v = (Var)lval;
                    if(v.var_id == -1 && ispointer(dearray(v.getType())))
                        v.var_id = var_num++;
                }
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
    private void gather_pass_ir(IR ir) {
        var stmts = ir.getStmts();
        Integer id = 0;
        for (var stmt : stmts) {
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
    private void gather_pass() {
        World.get().getClassHierarchy().applicationClasses().forEach(jclass->{
            
            jclass.getDeclaredMethods().forEach(method->{
                if(!method.isAbstract())
                    gather_pass_ir(method.getIR());
            });
        });
    }
    private void gather_static_pointers() {
        World.get().getClassHierarchy().applicationClasses().forEach(jclass->{    
            jclass.getDeclaredFields().forEach(field->{
                if(field.isStatic())
                {
                    field.var_id = var_num++;
                }
            });
        });
    }
    public void init() {
        count_pass();
        gather_pass();
        gather_static_pointers();
        MyDumper.dump(this);
    }
}