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

class BloomFilterCopyTest {

    @TempDir
    Path outputDirectory;

    private boolean compiles(final String statement) throws IOException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "A JDK is required to test copy return types");
        final JavaFileObject source = new SimpleJavaFileObject(URI.create("string:///CopyTypeCheck.java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
                return "import org.apache.commons.collections4.bloomfilter.*;"
                        + "class CopyTypeCheck { void check() {"
                        + "Shape shape = Shape.fromNP(10000, 0.01);" + statement + "} }";
            }
        };
        try (StandardJavaFileManager files = compiler.getStandardFileManager(null, null, null)) {
            return compiler.getTask(null, files, new DiagnosticCollector<>(),
                    Arrays.asList("-classpath", System.getProperty("java.class.path"), "-d", outputDirectory.toString()),
                    null, Collections.singletonList(source)).call();
        }
    }

    @Test
    void testCopyPreservesConcreteTypes() throws IOException {
        assertTrue(compiles("SimpleBloomFilter simple = new SimpleBloomFilter(shape).copy();"
                + "SparseBloomFilter sparse = new SparseBloomFilter(shape).copy();"
                + "ArrayCountingBloomFilter counting = new ArrayCountingBloomFilter(shape).copy();"
                + "BloomFilter<SimpleBloomFilter> filter = simple; SimpleBloomFilter copy = filter.copy();"
                + "CountingBloomFilter<ArrayCountingBloomFilter> counter = counting;"
                + "ArrayCountingBloomFilter counterCopy = counter.copy();"
                + "class Decorator extends WrappedBloomFilter<Decorator> {"
                + "Decorator(BloomFilter<?> wrapped) { super(wrapped); }"
                + "public Decorator copy() { return new Decorator(getWrapped().copy()); } }"
                + "BloomFilter<Decorator> decorated = new Decorator(simple); Decorator decoratedCopy = decorated.copy();"
                + "LayerManager<SimpleBloomFilter> manager = LayerManager.<SimpleBloomFilter>builder()"
                + ".setSupplier(() -> new SimpleBloomFilter(shape)).get();"
                + "BloomFilter<LayeredBloomFilter<SimpleBloomFilter>> layered = new LayeredBloomFilter<>(shape, manager);"
                + "LayeredBloomFilter<SimpleBloomFilter> layeredCopy = layered.copy();"));
    }

    @Test
    void testCopyRejectsUnrelatedConcreteType() throws IOException {
        assertFalse(compiles("SparseBloomFilter filter = new SimpleBloomFilter(shape).copy();"));
    }

    @Test
    void testCopyRejectsUnrelatedInterfaceType() throws IOException {
        assertFalse(compiles("BloomFilter<SimpleBloomFilter> filter = new SimpleBloomFilter(shape);"
                + "SparseBloomFilter copy = filter.copy();"));
    }
}
