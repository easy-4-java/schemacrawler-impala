/*
 * Copyright (c) 2018-present, easy-4-java (https://github.com/easy-4-java).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package schemacrawler.server.impala;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import schemacrawler.schemacrawler.DatabaseServerType;
import schemacrawler.tools.executable.commandline.PluginCommand;

/**
 * Unit tests for {@link ImpalaDatabaseConnector}.
 *
 * <p>The class under test calls {@code Class.forName("com.cloudera.impala.jdbc41.Driver")}
 * from its constructor, but the Cloudera JDBC driver is not part of the project's
 * runtime classpath. To exercise the non-throw paths we materialise a tiny stand-in
 * class file (compiled from a stub source on disk) into the Maven {@code target/test-classes}
 * directory before any test in this class runs. The class is reachable through the
 * standard application class loader because the production code resolves it via
 * {@code Class.forName(name)}.</p>
 *
 * <p>The "missing-driver" scenario is tested by loading the
 * {@link ImpalaDatabaseConnector} class via a {@link URLClassLoader} that includes
 * {@code target/classes} (where the production code lives) but intentionally
 * <strong>excludes</strong> {@code target/test-classes} (where the stub driver lives),
 * thereby avoiding the classloader caching that occurs when the driver has already
 * been loaded by the application classloader.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see ImpalaDatabaseConnector
 * @see DatabaseServerType
 * @see PluginCommand
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ImpalaDatabaseConnectorTest {

    /**
     * The fully qualified name of the Impala driver the connector expects to load.
     */
    private static final String DRIVER_FQCN = "com.cloudera.impala.jdbc41.Driver";

    /**
     * The package portion of {@link #DRIVER_FQCN}, used to locate the output directory.
     */
    private static final String DRIVER_PACKAGE = DRIVER_FQCN.substring(0, DRIVER_FQCN.lastIndexOf('.'));

    /**
     * The simple class name of the driver.
     */
    private static final String DRIVER_SIMPLE_NAME = DRIVER_FQCN.substring(DRIVER_FQCN.lastIndexOf('.') + 1);

    /**
     * Materialises the stub driver on the classpath before any test method runs.
     *
     * @throws Exception if the stub cannot be written to the build output directory
     */
    @BeforeClass
    public static void installStubDriver() throws Exception {
        File classesDir = locateClassesDirectory();
        File packageDir = new File(classesDir, DRIVER_PACKAGE.replace('.', File.separatorChar));
        if (!packageDir.exists() && !packageDir.mkdirs()) {
            throw new IllegalStateException("Cannot create package directory: " + packageDir);
        }
        File driverFile = new File(packageDir, DRIVER_SIMPLE_NAME + ".class");
        if (!driverFile.exists()) {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (Objects.isNull(compiler)) {
                throw new IllegalStateException("Tests require a JDK compiler");
            }
            File sourceFile = new File(packageDir, DRIVER_SIMPLE_NAME + ".java");
            String source = "package " + DRIVER_PACKAGE + "; public class "
                    + DRIVER_SIMPLE_NAME + " {}";
            Files.write(sourceFile.toPath(), source.getBytes(StandardCharsets.UTF_8));
            try {
                int exitCode = compiler.run(null, null, null, "-d", classesDir.getAbsolutePath(),
                        sourceFile.getAbsolutePath());
                if (exitCode != 0) {
                    throw new IllegalStateException("Cannot compile stub driver: " + exitCode);
                }
            } finally {
                Files.deleteIfExists(sourceFile.toPath());
            }
        }
    }

    /**
     * Locates the build output directory that is on the application classpath.
     *
     * @return the {@code target/test-classes} directory of the Maven module
     * @throws IllegalStateException if the directory cannot be located
     */
    private static File locateClassesDirectory() {
        String classPath = System.getProperty("java.class.path");
        if (classPath == null) {
            throw new IllegalStateException("java.class.path system property is not set");
        }
        for (String entry : classPath.split(File.pathSeparator)) {
            File file = new File(entry);
            String name = file.getName();
            if ("test-classes".equals(name) && file.isDirectory()) {
                return file;
            }
        }
        throw new IllegalStateException("Unable to locate target/test-classes in classpath: " + classPath);
    }

    /**
     * Builds a {@link URLClassLoader} that includes {@code target/classes} and all
     * Maven dependency JARs, but <strong>excludes</strong> {@code target/test-classes}.
     * This allows loading {@link ImpalaDatabaseConnector} in isolation from the
     * stub driver.
     *
     * @return a classloader without the stub driver on its classpath
     * @throws Exception if classpath entries cannot be resolved
     */
    private static URLClassLoader buildClassLoaderWithoutStub() throws Exception {
        String classPath = System.getProperty("java.class.path");
        List<URL> urls = new ArrayList<>();
        for (String entry : classPath.split(File.pathSeparator)) {
            File file = new File(entry);
            if ("test-classes".equals(file.getName())) {
                continue; // exclude -- stub driver lives here
            }
            urls.add(file.toURI().toURL());
        }
        return new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getSystemClassLoader().getParent());
    }

    // ---- Happy-path tests (run alphabetically before zz_*) ----

    /**
     * The connector must build a non-null {@link PluginCommand} via
     * {@code getHelpCommand()} that mentions every configured option
     * (server, host, port, database).
     *
     * @throws Exception if construction or help-command retrieval fails
     */
    @Test
    public void shouldBuildHelpCommandWhenDriverIsAvailable() throws Exception {
        ImpalaDatabaseConnector connector = new ImpalaDatabaseConnector();
        PluginCommand command = connector.getHelpCommand();
        assertNotNull(command);
        String rendered = command.toString();
        assertTrue("Help output should mention the 'server' option: " + rendered,
                rendered.contains("server"));
        assertTrue("Help output should mention the 'host' option: " + rendered,
                rendered.contains("host"));
        assertTrue("Help output should mention the 'port' option: " + rendered,
                rendered.contains("port"));
        assertTrue("Help output should mention the 'database' option: " + rendered,
                rendered.contains("database"));
    }

    /**
     * The URL predicate must accept Hive2 URLs and reject every other form
     * such as MySQL, PostgreSQL, or plain-text strings.
     *
     * @throws Exception if reflection or construction fails
     */
    @Test
    public void shouldAcceptHive2UrlsAndRejectOthers() throws Exception {
        ImpalaDatabaseConnector connector = new ImpalaDatabaseConnector();
        Method method = ImpalaDatabaseConnector.class.getDeclaredMethod("supportsUrlPredicate");
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Predicate<String> predicate = (Predicate<String>) method.invoke(connector);

        assertTrue(predicate.test("jdbc:hive2://localhost:21050/default"));
        assertTrue(predicate.test("jdbc:hive2://node1:10000/;auth=noSasl"));
        assertFalse(predicate.test("jdbc:mysql://localhost/test"));
        assertFalse(predicate.test("jdbc:postgresql://localhost/test"));
        assertFalse(predicate.test(""));
        assertFalse(predicate.test("not a url"));
    }

    /**
     * The connector must be an instance of
     * {@link schemacrawler.tools.databaseconnector.DatabaseConnector}.
     *
     * @throws Exception if construction fails
     */
    @Test
    public void shouldBeInstanceOfDatabaseConnector() throws Exception {
        ImpalaDatabaseConnector connector = new ImpalaDatabaseConnector();
        assertTrue("Connector should be a DatabaseConnector",
                connector instanceof schemacrawler.tools.databaseconnector.DatabaseConnector);
    }

    /**
     * The connector's {@code getHelpCommand()} must return the same non-null
     * instance on repeated calls (idempotent and stable).
     *
     * @throws Exception if construction fails
     */
    @Test
    public void shouldReturnStableHelpCommandOnRepeatedCalls() throws Exception {
        ImpalaDatabaseConnector connector = new ImpalaDatabaseConnector();
        PluginCommand first = connector.getHelpCommand();
        PluginCommand second = connector.getHelpCommand();
        assertNotNull(first);
        assertNotNull(second);
        assertEquals("Help command should have consistent output across calls",
                first.toString(), second.toString());
    }

    /**
     * Confirms that the connector's constructor throws a {@link RuntimeException}
     * wrapping a {@link ClassNotFoundException} when the Impala JDBC driver is
     * genuinely missing. This test uses a {@link URLClassLoader} that includes
     * {@code target/classes} (production code) but excludes {@code target/test-classes}
     * (where the stub driver lives), so {@code Class.forName} cannot find the driver.
     *
     * @throws Exception if classloader setup or reflection fails
     */
    @Test
    public void zz_shouldRejectConstructionWhenImpalaDriverIsUnavailable() throws Exception {
        try (URLClassLoader isolatedLoader = buildClassLoaderWithoutStub()) {
            // Load the ImpalaDatabaseConnector class in the isolated classloader
            Class<?> connectorClass = isolatedLoader.loadClass(ImpalaDatabaseConnector.class.getName());
            Constructor<?> ctor = connectorClass.getDeclaredConstructor();
            try {
                ctor.newInstance();
                fail("Expected RuntimeException because Impala JDBC driver is not on the isolated classpath");
            } catch (Exception e) {
                // Unwrap InvocationTargetException to get the actual RuntimeException
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                assertTrue("Expected RuntimeException but got " + cause.getClass().getName(),
                        cause instanceof RuntimeException);
                RuntimeException rte = (RuntimeException) cause;
                assertEquals("Could not load Impala JDBC driver", rte.getMessage());
                assertNotNull("Original cause must be preserved", rte.getCause());
            }
        }
    }
}
