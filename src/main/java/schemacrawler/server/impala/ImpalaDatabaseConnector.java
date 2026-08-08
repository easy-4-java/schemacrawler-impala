package schemacrawler.server.impala;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import schemacrawler.schemacrawler.DatabaseServerType;
import schemacrawler.tools.databaseconnector.DatabaseConnector;
import schemacrawler.tools.executable.commandline.PluginCommand;
import schemacrawler.tools.iosource.ClasspathInputResource;

/**
 * Provides SchemaCrawler connectivity support for Apache Impala through the Hive2 JDBC protocol.
 *
 * <p>The connector loads the Impala JDBC driver, supplies the bundled Impala information-schema
 * definitions, and exposes command-line options for selecting the server, host, port, and database.
 * URLs are accepted when they use the {@code jdbc:hive2:} scheme.</p>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see DatabaseConnector
 * @see DatabaseServerType
 * @see PluginCommand
 */
public final class ImpalaDatabaseConnector extends DatabaseConnector {

    /**
     * Creates a connector configured with the bundled Impala metadata resources.
     *
     * @throws IOException if the bundled connector configuration cannot be read
     * @throws RuntimeException if the Impala JDBC driver is unavailable
     */
    public ImpalaDatabaseConnector() throws IOException {
        super(new DatabaseServerType("impala", "Apache Impala"),
                new ClasspathInputResource("/schemacrawler-impala.config.properties"),
                (informationSchemaViewsBuilder, connection) ->
                        informationSchemaViewsBuilder.fromResourceFolder("/impala.information_schema"));
        try {
            Class.forName("com.cloudera.impala.jdbc41.Driver");
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException("Could not load Impala JDBC driver", e);
        }
    }

    /**
     * Builds the command-line help definition for the Impala connector.
     *
     * @return the inherited command with Impala connection options
     */
    @Override
    public PluginCommand getHelpCommand() {
        final PluginCommand pluginCommand = super.getHelpCommand();
        pluginCommand.addOption("server", "--server=hive2%nLoads SchemaCrawler plug-in for Hive2", String.class)
                .addOption("host", "Host name%nOptional, defaults to localhost", String.class)
                .addOption("port", "Port number%nOptional, defaults to 3306", Integer.class)
                .addOption("database", "Database name", String.class);
        return pluginCommand;
    }

    /**
     * Returns the predicate used to recognize supported JDBC URLs.
     *
     * @return a predicate accepting URLs beginning with {@code jdbc:hive2:}
     */
    @Override
    protected Predicate<String> supportsUrlPredicate() {
        return url -> Pattern.matches("jdbc:hive2:.*", url);
    }
}
