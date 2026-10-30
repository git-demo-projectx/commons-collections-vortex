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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests the compile-time contract of {@link BloomFilter#copy()}.
 */
class BloomFilterCopyTypeTest {

    @TempDir
    Path outputDirectory;

    private boolean compiles(final String body) throws IOException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);
        final String source = "import org.apache.commons.collections4.bloomfilter.*; class CopyType { " + body + " }";
        final JavaFileObject sourceFile = new SimpleJavaFileObject(URI.create("string:///CopyType.java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
                return source;
            }
        };
        try (StandardJavaFileManager files = compiler.getStandardFileManager(null, null, null)) {
            return compiler.getTask(new StringWriter(), files, null,
                    Arrays.asList("-proc:none", "-source", "8", "-target", "8", "-classpath",
                            System.getProperty("java.class.path"), "-d", outputDirectory.toString()),
                    null, Collections.singletonList(sourceFile)).call();
        }
    }

    @Test
    void testCopiesKeepTheirTypes() throws IOException {
        assertTrue(compiles("void copies(SimpleBloomFilter simple, SparseBloomFilter sparse, "
                + "ArrayCountingBloomFilter counting, LayeredBloomFilter<SimpleBloomFilter> layered, "
                + "BloomFilter<SimpleBloomFilter> filter, CountingBloomFilter<ArrayCountingBloomFilter> counter) { "
                + "SimpleBloomFilter a = simple.copy(); SparseBloomFilter b = sparse.copy(); "
                + "ArrayCountingBloomFilter c = counting.copy(); LayeredBloomFilter<SimpleBloomFilter> d = layered.copy(); "
                + "SimpleBloomFilter e = filter.copy(); ArrayCountingBloomFilter f = counter.copy(); } "
                + "<T extends BloomFilter<T>> T genericCopy(T filter) { return filter.copy(); }"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "SparseBloomFilter filter = new SimpleBloomFilter(Shape.fromNP(10000, 0.01)).copy();",
        "void copy(BloomFilter<SimpleBloomFilter> filter) { SparseBloomFilter wrong = filter.copy(); }",
        "void copy(CountingBloomFilter<ArrayCountingBloomFilter> filter) { SimpleBloomFilter wrong = filter.copy(); }"
    })
    void testCopiesCannotChangeType(final String body) throws IOException {
        assertFalse(compiles(body));
    }
}
