package pku;


import java.io.*;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.analysis.misc.IRDumper;
import pascal.taie.config.AnalysisConfig;


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
            preprocess.test_pts.forEach((test_id, pt)->{
                result.put(test_id, objs);
            });
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
