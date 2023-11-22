package pku;

import pascal.taie.World;
// import pascal.taie.World;
// import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.analysis.graph.callgraph.*;

import java.util.concurrent.TimeUnit;

public class PointerAnalysis extends PointerAnalysisTrivial
{
    public static final String ID = "pku-pta";

    public PointerAnalysis(AnalysisConfig config) {
        super(config);
    }

    void die() {
        try {
            TimeUnit.SECONDS.sleep(61);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    @Override
    public PointerAnalysisResult analyze() {
        
        var ppr = new PreprocessResult();
        ppr.gather_test_info();;
        PointerAnalysisResult trivial_typing = super.trivial_typing(ppr);
        
        // var world = World.get();
        // var main = world.getMainMethod();
        // var jclass = main.getDeclaringClass();

        // You need to use `preprocess` like in PointerAnalysisTrivial
        // when you enter one method to collect infomation given by
        // Benchmark.alloc(id) and Benchmark.test(id, var)
        //
        // As for when and how you enter one method,
        // it's your analysis assignment to accomplish
        PointerAnalysisResult res = null;
        try {
            MyAnalyzer.CG = World.get().getResult(CallGraphBuilder.ID);
        } catch(Exception e) {
            res = trivial_typing;
            dump(res);
            return res;
        }

        try {
            ppr.init();
            MyAnalyzer.init(ppr);
            res = MyAnalyzer.analyze();
        } catch (Exception e) {
            // die();
            res = trivial_typing;
        }
        dump(res);
        return res;
    }
}


