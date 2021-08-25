/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.ir.exp;

import pascal.taie.ir.proginfo.MethodRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of instance invocation (virtual, interface,
 * and special) expression.
 */
public abstract class InvokeInstanceExp extends InvokeExp {

    protected final Var base;

    protected InvokeInstanceExp(MethodRef methodRef, Var base, List<Var> args) {
        super(methodRef, args);
        this.base = base;
    }

    public Var getBase() {
        return base;
    }

    @Override
    public List<RValue> getUses() {
        List<RValue> uses = new ArrayList<>(args.size() + 1);
        uses.add(base);
        uses.addAll(args);
        return uses;
    }

    @Override
    public String toString() {
        return String.format("%s %s.%s%s", getInvokeString(),
                base.getName(), methodRef.getName(), getArgsString());
    }
}
