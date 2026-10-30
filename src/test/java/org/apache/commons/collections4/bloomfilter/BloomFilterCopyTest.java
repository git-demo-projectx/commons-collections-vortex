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
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests the compile-time return type of Bloom filter copies. */
public class BloomFilterCopyTest {

    @TempDir
    Path output;

    private void assertCompiles(final String statement, final boolean expected) throws IOException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "A JDK is required to test copy return types");
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        final JavaFileObject source = new SimpleJavaFileObject(URI.create("string:///CopyCheck.java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
                return "package org.apache.commons.collections4.bloomfilter; class CopyCheck { void check() {"
                        + "Shape shape = Shape.fromNP(10000, 0.01); " + statement + "} }";
            }
        };
        try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, null)) {
            final String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
            final boolean compiled = compiler.getTask(null, files, diagnostics,
                    Arrays.asList("-classpath", classpath, "-d", output.toString(), "-proc:none"),
                    null, Collections.singletonList(source)).call();
            assertEquals(expected, compiled, () -> diagnostics.getDiagnostics().toString());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "SimpleBloomFilter copy = new SimpleBloomFilter(shape).copy();",
        "SparseBloomFilter copy = new SparseBloomFilter(shape).copy();",
        "ArrayCountingBloomFilter copy = new ArrayCountingBloomFilter(shape).copy();",
        "BloomFilter<SimpleBloomFilter> filter = new SimpleBloomFilter(shape); SimpleBloomFilter copy = filter.copy();",
        "CountingBloomFilter<ArrayCountingBloomFilter> filter = new ArrayCountingBloomFilter(shape); ArrayCountingBloomFilter copy = filter.copy();",
        "LayeredBloomFilter<SimpleBloomFilter> filter = LayeredBloomFilterTest.fixed(shape, 2); LayeredBloomFilter<SimpleBloomFilter> copy = filter.copy();",
        "WrappedBloomFilter<WrappedBloomFilterTest.Fixture> filter = new WrappedBloomFilterTest.Fixture(new SimpleBloomFilter(shape)); "
            + "WrappedBloomFilterTest.Fixture copy = filter.copy();"
    })
    public void testCopyReturnsFilterType(final String statement) throws IOException {
        assertCompiles(statement, true);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "SparseBloomFilter copy = new SimpleBloomFilter(shape).copy();",
        "SimpleBloomFilter copy = new SparseBloomFilter(shape).copy();",
        "SimpleBloomFilter copy = new ArrayCountingBloomFilter(shape).copy();",
        "BloomFilter<SimpleBloomFilter> filter = new SimpleBloomFilter(shape); SparseBloomFilter copy = filter.copy();",
        "BloomFilter<?> filter = new SimpleBloomFilter(shape); SparseBloomFilter copy = filter.copy();",
        "CountingBloomFilter<ArrayCountingBloomFilter> filter = new ArrayCountingBloomFilter(shape); SimpleBloomFilter copy = filter.copy();"
    })
    public void testCopyRejectsDifferentFilterType(final String statement) throws IOException {
        assertCompiles(statement, false);
    }
}
