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
            {
                var inv = (Invoke)stmt;
                logger.info("    // Calling ID: {}", inv.call_id);
                logger.info("    // Possible Callees: {} {}", inv.callee_versions.size(), inv.callees.size());
                for(int i = 0; i < inv.callee_versions.size(); ++i)
                    logger.info("      ({}, {})", inv.callees.get(i).jm.getName(), inv.callee_versions.get(i));
            }
            if(stmt instanceof New)
                logger.info("    // Object ID: {}",  (((New)stmt).object_id));
            // var wlval = stmt.getDef();
            // if(wlval.isPresent())
            // {
            //     var lval = wlval.get();
            //     if(lval instanceof Var)
            //         logger.info("    // Pointer ID: {}", ( ((Var)lval).var_id ));
            // } 
        }
    }
    static void dump(PreprocessResult ppr) {
        World.get().getClassHierarchy().applicationClasses().forEach(jclass->{
            logger.info("Dumping class {}:", jclass.getName());
            jclass.getDeclaredMethods().forEach(method->{
                logger.info("  Dumping method {}:", method.getName());
                if(!method.isAbstract())
                {
                    var ir = method.getIR();
                    logger.info("    Called times: {}", method.wrapper.versions);
                    logger.info("    Possible Callers: {}", method.wrapper.returnee.size());
                    for(var stmt: method.wrapper.returnee)
                    {
                        if(stmt == null) logger.info("      Ghost Caller");
                        else logger.info("      {}", stmt.get_stmt_id());
                    }
                    logger.info("    Param List: {}", ir.getParams());
                    logger.info("    this-pointer: {}", ir.getThis());
                    dump_method(method.getIR());
                }
            });
        });
        logger.info("------ Below are the OBJECT-FIELD pointers ------");
        for(int i = 0; i < ppr.wobjects.size(); ++i) {
            logger.info("Dumping Object {}", i);
            logger.info("  TestPoitn ID: {}", ppr.wobjects.get(i).tester_id);
            logger.info("  " + ppr.wobjects.get(i).field.toString());
        }
        // logger.info("------ Below are the STATIC-FIELD pointers ------");
        // World.get().getClassHierarchy().applicationClasses().forEach(jclass->{    
        //     logger.info("Duming static pointers of class {}", jclass.getName());
        //     jclass.getDeclaredFields().forEach(field->{
        //         if(field.var_id != -1)
        //             logger.info("  {}: {}",field.getName(), field.var_id);
        //     });
        // });
    }
}