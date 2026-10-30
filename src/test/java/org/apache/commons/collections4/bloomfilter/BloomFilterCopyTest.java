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
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests compile-time typing of Bloom filter copies. */
class BloomFilterCopyTest {

    @TempDir
    Path output;

    private void assertCompiles(final boolean expected, final String declaration) throws IOException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);
        final SimpleJavaFileObject source = new SimpleJavaFileObject(URI.create("string:///CopyCheck.java"),
                javax.tools.JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
                return "import org.apache.commons.collections4.bloomfilter.*; class CopyCheck { "
                        + "void check() { " + declaration + " } }";
            }
        };
        final StringWriter diagnostics = new StringWriter();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(null, null, null)) {
            final boolean compiled = compiler.getTask(diagnostics, files, null,
                    Arrays.asList("-d", output.toString(), "-classpath", System.getProperty("java.class.path")),
                    null, Collections.singletonList(source)).call();
            assertEquals(expected, compiled, diagnostics::toString);
        }
    }

    @Test
    void testConcreteCopies() throws IOException {
        assertCompiles(true, "Shape shape = Shape.fromNP(10000, 0.01);"
                + "SimpleBloomFilter simple = new SimpleBloomFilter(shape).copy();"
                + "SparseBloomFilter sparse = new SparseBloomFilter(shape).copy();"
                + "ArrayCountingBloomFilter counting = new ArrayCountingBloomFilter(shape).copy();"
                + "BloomFilter<SimpleBloomFilter> filter = simple; SimpleBloomFilter copy = filter.copy();"
                + "CountingBloomFilter<ArrayCountingBloomFilter> counter = counting;"
                + "ArrayCountingBloomFilter counterCopy = counter.copy();"
                + "LayerManager<SimpleBloomFilter> manager = LayerManager.<SimpleBloomFilter>builder()"
                + ".setSupplier(() -> new SimpleBloomFilter(shape)).get();"
                + "LayeredBloomFilter<SimpleBloomFilter> layered = new LayeredBloomFilter<>(shape, manager).copy();");
    }

    @Test
    void testWrongCopyType() throws IOException {
        assertCompiles(false, "SparseBloomFilter filter = new SimpleBloomFilter(Shape.fromNP(10000, 0.01)).copy();");
        assertCompiles(false, "BloomFilter<SimpleBloomFilter> filter = new SimpleBloomFilter(Shape.fromNP(10000, 0.01));"
                + "SparseBloomFilter copy = filter.copy();");
        assertCompiles(false, "CountingBloomFilter<ArrayCountingBloomFilter> filter ="
                + "new ArrayCountingBloomFilter(Shape.fromNP(10000, 0.01)); SparseBloomFilter copy = filter.copy();");
    }
}
