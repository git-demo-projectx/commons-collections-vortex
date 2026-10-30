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

/**
 * Tests for the {@link SimpleBloomFilter}.
 */
public class SimpleBloomFilterTest extends AbstractBloomFilterTest<SimpleBloomFilter> {

    @Override
    protected SimpleBloomFilter createEmptyFilter(final Shape shape) {
        return new SimpleBloomFilter(shape);
    }

    @Test
    public void testCopyCannotReturnAnotherFilterType() {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        final JavaFileObject source = new SimpleJavaFileObject(URI.create("string:///InvalidCopy.java"),
                JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
                return "import org.apache.commons.collections4.bloomfilter.*;"
                        + "class InvalidCopy { void copy() {"
                        + "BloomFilter<SimpleBloomFilter> filter = new SimpleBloomFilter(Shape.fromNP(10000, 0.01));"
                        + "SparseBloomFilter copy = filter.copy();"
                        + "SparseBloomFilter direct = new SimpleBloomFilter(Shape.fromNP(10000, 0.01)).copy(); }}";
            }
        };
        final Boolean compiled = compiler.getTask(null, null, diagnostics,
                Arrays.asList("-proc:none", "-classpath", System.getProperty("java.class.path")),
                null, Collections.singletonList(source)).call();
        assertFalse(compiled);
        assertEquals(2, diagnostics.getDiagnostics().stream()
                .filter(d -> "compiler.err.prob.found.req".equals(d.getCode())).count());
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
