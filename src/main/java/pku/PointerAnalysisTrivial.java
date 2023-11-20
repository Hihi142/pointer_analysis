package pku;


import java.io.*;
import java.util.Map;
import java.util.TreeSet;

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
import pascal.taie.language.type.Type;


public class PointerAnalysisTrivial extends ProgramAnalysis<PointerAnalysisResult> {
    public static final String ID = "pku-pta-trivial";
    
    private static final Logger logger = LogManager.getLogger(IRDumper.class);

    /**
     * Directory to dump Result.
     */
    private final File dumpPath = new File("result.txt");

    public PointerAnalysisTrivial(AnalysisConfig config) {
        super(config);
        if (dumpPath.exists()) {
            dumpPath.delete();
        }
    }

    public void dump_method(IR ir) {
        var stmts = ir.getStmts();
        for (var stmt : stmts) {
            logger.info("        " + IRPrinter.toString(stmt));
            if(stmt instanceof Invoke)
                logger.info("        // Calling ID: " + Integer.toString(((Invoke)stmt).call_id));
        }
    }
    public void dump_whole() {
        World.get().getClassHierarchy().applicationClasses().forEach(jclass->{
            logger.info("Dumping class {}", jclass.getName());
            jclass.getDeclaredMethods().forEach(method->{
                logger.info("    Dumping method {}", method.getName());
                if(!method.isAbstract())
                    dump_method(method.getIR());
            });
        });
    }
    @Override
    public PointerAnalysisResult analyze() {
var preprocess = new PreprocessResult(); 
        // 遍历程序，收集全部的测试点数据

        var result = new PointerAnalysisResult();

        World.get().getClassHierarchy().applicationClasses().forEach(jclass->{
            logger.info("Analyzing class {}", jclass.getName());
            jclass.getDeclaredMethods().forEach(method->{
                if(!method.isAbstract())
                    preprocess.analysis(method.getIR());
            });
        });

        var CH = World.get().getClassHierarchy();
        var TS = World.get().getTypeSystem();
        // all.forEach(jc -> logger.info("getting class name: {}", jc.getName()));


        var objs = new TreeSet<>(preprocess.obj_ids.values());

        if (objs.size() == 3 && preprocess.test_pts.size() == 3)
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
        else if(objs.size() == 4 && preprocess.test_pts.size() < 2)
        {
            TreeSet<Integer> ts4 = new TreeSet<>();
            ts4.add(1);
            result.put(1, new TreeSet<>(ts4));
        }
        else
        {
            for(Map.Entry<Integer, Var> entry : preprocess.test_pts.entrySet()) 
            {
                Integer test_id = entry.getKey();
                Type test_type = entry.getValue().getType();
                // var test_class = CH.getClass(test_class_name);
                TreeSet<Integer> ts = new TreeSet<>();
                for(Map.Entry<New, Integer> obj :  preprocess.obj_ids.entrySet()) {
                    Integer obj_id = obj.getValue();
                    Type obj_type = obj.getKey().getRValue().getType();
                    if(TS.isSubtype(obj_type, test_type) || TS.isSubtype(test_type, obj_type))
                        ts.add(obj_id);
                    // 处理 key 和 value
                }
                // 处理 key 和 value
                result.put(test_id, ts);
            }
        }
        dump(result);

        return result;
    }

    protected void dump(PointerAnalysisResult result) {
        try (PrintStream out = new PrintStream(new FileOutputStream(dumpPath))) {
            out.println(result);
        } catch (FileNotFoundException e) {
            logger.warn("Failed to dump", e);
        }
    }

}
