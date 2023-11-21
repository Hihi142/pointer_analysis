package pku;

import pascal.taie.ir.exp.Var;

public class WVar {
    Var v;
    int id;
    PointsToSet pointee;
    WVar() { pointee = null; }
    WVar(Var v0, int id0) { v = v0; id = id0; pointee = null; }
};