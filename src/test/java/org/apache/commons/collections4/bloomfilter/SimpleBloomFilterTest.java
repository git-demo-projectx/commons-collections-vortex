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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for the {@link SimpleBloomFilter}.
 */
public class SimpleBloomFilterTest extends AbstractBloomFilterTest<SimpleBloomFilter> {

    @Override
    protected SimpleBloomFilter createEmptyFilter(final Shape shape) {
        return new SimpleBloomFilter(shape);
    }

    private boolean compilesCopyAssignment(final String assignment, final Path output) throws IOException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final JavaFileObject source = new SimpleJavaFileObject(URI.create("string:///CopyAssignment.java"),
                JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
                return "import org.apache.commons.collections4.bloomfilter.*;"
                        + "class CopyAssignment { void copy() {"
                        + "Shape shape = Shape.fromNP(10000, 0.01);"
                        + assignment + "} }";
            }
        };
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, null, null)) {
            return compiler.getTask(null, manager, diagnostics,
                    Arrays.asList("-proc:none", "-classpath", System.getProperty("java.class.path"),
                            "-d", output.toString()), null, Collections.singletonList(source)).call();
        }
    }

    @Test
    public void testCopyAssignmentCompiles(final @TempDir Path output) throws IOException {
        assertTrue(compilesCopyAssignment("BloomFilter<SimpleBloomFilter> filter = new SimpleBloomFilter(shape);"
                + "SimpleBloomFilter copy = filter.copy();", output));
    }

    @Test
    public void testIncompatibleCopyAssignmentDoesNotCompile(final @TempDir Path output) throws IOException {
        assertFalse(compilesCopyAssignment("SparseBloomFilter copy = new SimpleBloomFilter(shape).copy();", output));
        assertFalse(compilesCopyAssignment("BloomFilter<SimpleBloomFilter> filter = new SimpleBloomFilter(shape);"
                + "SparseBloomFilter copy = filter.copy();", output));
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
