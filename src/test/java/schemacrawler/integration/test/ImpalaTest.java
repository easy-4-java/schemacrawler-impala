/*
 * Copyright (c) 2018, hiwepy (https://github.com/easy-4-java).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package schemacrawler.integration.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Test;

public class ImpalaTest {

	/*
	 * – impala.driver=org.apache.hive.jdbc.HiveDriver –
	 * impala.url=jdbc:hive2://node2:21050/;auth=noSasl – impala.username= –
	 * impala.password=
	 */
	
	public static Connection getConnection() throws ClassNotFoundException, SQLException {
		String driver = "org.apache.hive.jdbc.HiveDriver";
		String url = "jdbc:hive2://node23:21050/;auth=noSasl";
		String username = "";
		String password = "";
		Connection conn = null;
		Class.forName(driver);
		conn = (Connection) DriverManager.getConnection(url, username, password);
		return conn;
	}

	@Test
	public void select() throws ClassNotFoundException, SQLException {
		Connection conn = getConnection();
		String sql = "select * from t_stu;";
		PreparedStatement ps = conn.prepareStatement(sql);
		ResultSet rs = ps.executeQuery();
		int col = rs.getMetaData().getColumnCount();
		System.out.println("=====================================");
		while (rs.next()) {
			for (int i = 1; i <= col; i++) {
				System.out.print(rs.getString(i) + "\t");
			}
			System.out.print("\n");
		}
		System.out.println("=====================================");
	}

}
