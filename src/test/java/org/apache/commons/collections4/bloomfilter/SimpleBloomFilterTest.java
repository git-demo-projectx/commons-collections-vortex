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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for the {@link SimpleBloomFilter}.
 */
public class SimpleBloomFilterTest extends AbstractBloomFilterTest<SimpleBloomFilter> {

    @Override
    protected SimpleBloomFilter createEmptyFilter(final Shape shape) {
        return new SimpleBloomFilter(shape);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SimpleBloomFilter", "BloomFilter<SimpleBloomFilter>"})
    public void testCopyRejectsIncompatibleType(final String filterType) {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);
        final String source = "import org.apache.commons.collections4.bloomfilter.*;"
                + "class BadCopy { void copy() { " + filterType
                + " filter = new SimpleBloomFilter(Shape.fromNP(10000, 0.01));"
                + " SparseBloomFilter copy = filter.copy(); } }";
        final JavaFileObject file = new SimpleJavaFileObject(URI.create("string:///BadCopy.java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
                return source;
            }
        };
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        assertFalse(compiler.getTask(null, null, diagnostics,
                Arrays.asList("-proc:none", "-classpath", System.getProperty("java.class.path")), null,
                Collections.singletonList(file)).call());
        assertTrue(diagnostics.getDiagnostics().stream()
                .anyMatch(diagnostic -> "compiler.err.prob.found.req".equals(diagnostic.getCode())));
    }

    @Test
    public void testMergeShortBitMapExtractor() {
        final SimpleBloomFilter filter = createEmptyFilter(getTestShape());
        // create a bitMapExtractor that returns too few values
        // shape expects 2 longs we are sending 1.
        final BitMapExtractor bitMapExtractor = p -> p.test(2L);
        assertTrue(filter.merge(bitMapExtractor));
        assertEquals(1, filter.cardinality());
    }
}
