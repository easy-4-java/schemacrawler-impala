package schemacrawler.integration.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import schemacrawler.tools.databaseconnector.DatabaseConnectorRegistry;

public class TestBundledDistributionsIntg {

	@Test
	public void testPlugin_hive() throws Exception {
		final DatabaseConnectorRegistry registry = DatabaseConnectorRegistry.getDatabaseConnectorRegistry();
		assertTrue(registry.hasDatabaseSystemIdentifier("hive"));
	}

}
