/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.ir.exp;

import pascal.taie.ir.Site;
import pascal.taie.java.classes.MethodRef;
import pascal.taie.java.types.Type;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Representation of method invocation expression.
 */
public abstract class InvokeExp implements RValue {

    protected final MethodRef methodRef;

    protected final List<Var> args;

    private Site callSite;

    protected InvokeExp(MethodRef methodRef, List<Var> args) {
        this.methodRef = methodRef;
        this.args = Collections.unmodifiableList(args);
    }

    @Override
    public Type getType() {
        return methodRef.getReturnType();
    }

    public MethodRef getMethodRef() {
        return methodRef;
    }

    public int getArgCount() {
        return args.size();
    }

    public Var getArg(int i) {
        return args.get(i);
    }

    public List<Var> getArgs() {
        return args;
    }

    public Site getCallSite() {
        return callSite;
    }

    public void setCallSite(Site callSite) {
        this.callSite = callSite;
    }

    protected abstract String getInvokeString();

    protected String getArgsString() {
        return "(" + args.stream()
                .map(Var::toString)
                .collect(Collectors.joining(",")) + ")";
    }
}
