package schemacrawler.server.impala;
/**
 * @author <a href="https://github.com/loong10k">@Loong Wan</a>
 */

import java.io.IOException;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import schemacrawler.schemacrawler.DatabaseServerType;
import schemacrawler.tools.databaseconnector.DatabaseConnector;
import schemacrawler.tools.executable.commandline.PluginCommand;
import schemacrawler.tools.iosource.ClasspathInputResource;

public final class ImpalaDatabaseConnector extends DatabaseConnector {
	
	public ImpalaDatabaseConnector() throws IOException {

		super(new DatabaseServerType("impala", "Apache Impala"),
				new ClasspathInputResource("/schemacrawler-impala.config.properties"), (informationSchemaViewsBuilder,
						connection) -> informationSchemaViewsBuilder.fromResourceFolder("/impala.information_schema"));
		try {
			Class.forName("com.cloudera.impala.jdbc41.Driver");
		} catch (final ClassNotFoundException e) {
			throw new RuntimeException("Could not load Impala JDBC driver", e);
		}

	}

	@Override
	public PluginCommand getHelpCommand() {
		final PluginCommand pluginCommand = super.getHelpCommand();
		pluginCommand.addOption("server", "--server=hive2%n" + "Loads SchemaCrawler plug-in for Hive2", String.class)
				.addOption("host", "Host name%n" + "Optional, defaults to localhost", String.class)
				.addOption("port", "Port number%n" + "Optional, defaults to 3306", Integer.class)
				.addOption("database", "Database name", String.class);
		return pluginCommand;
	}

	@Override
	protected Predicate<String> supportsUrlPredicate() {
		return url -> Pattern.matches("jdbc:hive2:.*", url);
	}

}
