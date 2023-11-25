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
    
    public void merge_result(PointerAnalysisResult merger, PointerAnalysisResult mergee)
    {
        for (Integer key : merger.keySet()) {
            var merger_set = merger.get(key);
            var mergee_set = mergee.get(key);
            merger_set.retainAll(mergee_set);
        }
    }
    @Override
    public PointerAnalysisResult analyze() {
        
        var ppr = new PreprocessResult();
        ppr.gather_test_info();;
        PointerAnalysisResult trivial_typing = super.trivial_typing(ppr);
        
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
            merge_result(res, trivial_typing);
        } catch (Exception e) {
            res = trivial_typing;
        }
        dump(res);
        return res;
    }
}


