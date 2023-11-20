package pku;

import pascal.taie.World;
import pascal.taie.analysis.misc.IRDumper;
import pascal.taie.ir.IR;
import pascal.taie.ir.IRPrinter;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.New;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MyDumper {
    private static final Logger logger = LogManager.getLogger(IRDumper.class);
    private static void dump_method(IR ir) {
        var stmts = ir.getStmts();
        for (var stmt : stmts) {
            logger.info("    " + IRPrinter.toString(stmt));
            if(stmt instanceof Invoke)
                logger.info("    // Calling ID: " + Integer.toString(((Invoke)stmt).call_id));
            if(stmt instanceof New)
                logger.info("    // Object ID: " + Integer.toString(((New)stmt).object_id));
        }
    }
    static void dump() {
        World.get().getClassHierarchy().applicationClasses().forEach(jclass->{
            logger.info("Dumping class {}:", jclass.getName());
            jclass.getDeclaredMethods().forEach(method->{
                logger.info("  Dumping method {}:", method.getName());
                if(!method.isAbstract())
                    dump_method(method.getIR());
            });
        });
    }
}