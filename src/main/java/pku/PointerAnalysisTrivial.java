package pku;


import java.io.*;
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
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.New;


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
        var tri = new TrivialTyping();
        var ret = tri.get();
        dump(ret);
        return ret;
    }

    protected void dump(PointerAnalysisResult result) {
        try (PrintStream out = new PrintStream(new FileOutputStream(dumpPath))) {
            out.println(result);
        } catch (FileNotFoundException e) {
            logger.warn("Failed to dump", e);
        }
    }

}
