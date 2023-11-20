package pku;

import pascal.taie.World;
import pascal.taie.analysis.misc.IRDumper;
import pascal.taie.ir.IR;
import pascal.taie.ir.IRPrinter;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.exp.Var;

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
            var wlval = stmt.getDef();
            if(wlval.isPresent())
            {
                var lval = wlval.get();
                if(lval instanceof Var && ((Var)lval).var_id >= 0)
                    logger.info("    // Pointer ID: " + Integer.toString( ((Var)lval).var_id ));
            } 
        }
    }
    static void dump(PreprocessResult ppr) {
        World.get().getClassHierarchy().applicationClasses().forEach(jclass->{
            logger.info("Dumping class {}:", jclass.getName());
            jclass.getDeclaredMethods().forEach(method->{
                logger.info("  Dumping method {}:", method.getName());
                if(!method.isAbstract())
                    dump_method(method.getIR());
            });
        });
        logger.info("------ Below are the OBJECT-FIELD pointers ------");
        for(int i = 0; i < ppr.wobjects.size(); ++i) {
            logger.info("Field-Pointers in Object " + Integer.toString(i));
            logger.info("  " + ppr.wobjects.get(i).field.toString());
        }
    }
}