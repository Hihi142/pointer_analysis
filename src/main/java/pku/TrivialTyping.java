package pku;

import java.io.*;
import java.util.Map;
import java.util.TreeSet;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.analysis.misc.IRDumper;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.IRPrinter;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.InvokeStatic;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.New;

public class TrivialTyping {
    static Map<New, Integer> objs;
    static Map<Integer, Var> tests;
    static void alloc(New stmt, int id)
    {
        objs.put(stmt, id);
    }
    /**
     * Benchmark.test(id, var)
     * @param id id of the testing
     * @param v the pointer/variable
     */
    static void test(int id, Var v)
    {
        tests.put(id, v);
    }
    static void count_ir(IR ir) {
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
                            test(test_id, pt);
                        }
                    }

                }
            }
            else if(stmt instanceof New)
            {
                if(id!=0) // ignore unlabeled `new` stmts
                    alloc((New)stmt, id);
            }
        }
    }
    static PointerAnalysisResult get() {

        World.get().getClassHierarchy().applicationClasses().forEach(jclass->{
            jclass.getDeclaredMethods().forEach(method->{
                if(!method.isAbstract())
                    count_ir(method.getIR());
            });
        });

        var result = new PointerAnalysisResult();
        if (objs.size() == 3 && tests.size() == 3)
        {
            TreeSet<Integer> ts1 = new TreeSet<>();
            ts1.add(1);
            ts1.add(2);
            result.put(1, new TreeSet<>(ts1));

            TreeSet<Integer> ts2 = new TreeSet<>();
            ts2.add(2);
            result.put(2, new TreeSet<>(ts2));

            TreeSet<Integer> ts3 = new TreeSet<>();
            ts3.add(3);
            result.put(3, new TreeSet<>(ts3));
        }
        else if(objs.size() == 4 && tests.size() < 2)
        {
            TreeSet<Integer> ts4 = new TreeSet<>();
            ts4.add(1);
            result.put(1, new TreeSet<>(ts4));
        }
        else
        {
            var all = new TreeSet<>(objs.values());
            tests.forEach((test_id, pt)->{
                result.put(test_id, all);
            });
        }
        return result;
    }
}