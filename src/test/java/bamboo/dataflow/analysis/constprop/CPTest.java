/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.dataflow.analysis.constprop;

import org.junit.Test;

import static bamboo.dataflow.analysis.constprop.TestUtils.test;

public class CPTest {

    @Test
    public void testSimpleConstant() {
        test("SimpleConstant");
    }

    @Test
    public void testSimpleBinary() {
        test("SimpleBinary");
    }

    @Test
    public void testSimpleBranch() {
        test("SimpleBranch");
    }
}