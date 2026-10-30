/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.collections4.bloomfilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;

public class BloomFilterCopyTest {

    @Test
    public void testCopyThroughInterfaces() {
        final Shape shape = Shape.fromNP(10000, 0.01);
        final BloomFilter<SimpleBloomFilter> simple = new SimpleBloomFilter(shape);
        final SimpleBloomFilter simpleCopy = simple.copy();
        assertNotSame(simple, simpleCopy);
        assertEquals(shape, simpleCopy.getShape());

        final BloomFilter<SparseBloomFilter> sparse = new SparseBloomFilter(shape);
        final SparseBloomFilter sparseCopy = sparse.copy();
        assertNotSame(sparse, sparseCopy);
        assertEquals(shape, sparseCopy.getShape());

        final CountingBloomFilter<ArrayCountingBloomFilter> counting = new ArrayCountingBloomFilter(shape);
        final ArrayCountingBloomFilter countingCopy = counting.copy();
        assertNotSame(counting, countingCopy);
        assertEquals(shape, countingCopy.getShape());
    }

    @Test
    public void testCopyCannotReturnUnrelatedType() throws IOException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        final JavaFileObject source = new SimpleJavaFileObject(URI.create("string:///InvalidCopy.java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
                return "import org.apache.commons.collections4.bloomfilter.*;"
                        + "class InvalidCopy {"
                        + "SparseBloomFilter concrete = new SimpleBloomFilter(Shape.fromNP(10000, 0.01)).copy();"
                        + "SparseBloomFilter copy(BloomFilter<SimpleBloomFilter> filter) { return filter.copy(); }"
                        + "}";
            }
        };
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            assertFalse(compiler.getTask(null, fileManager, diagnostics,
                    Arrays.asList("-proc:none", "-classpath", System.getProperty("java.class.path")),
                    null, Collections.singletonList(source)).call());
        }
        assertEquals(2, diagnostics.getDiagnostics().stream()
                .filter(d -> d.getKind() == Diagnostic.Kind.ERROR).count());
    }
}
