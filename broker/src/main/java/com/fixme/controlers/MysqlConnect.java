package com.fixme.controlers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * MysqlConnect
 */

public final class MysqlConnect {
	public Connection conn;
	private Statement statement;
	private PreparedStatement pStatement;
	public static MysqlConnect db;

	private MysqlConnect() {
		String url = "";

		// if (System.getenv("USER").equals("rmdaba")) {
		// System.out.println("here!!!!!!!!!!!!!!!!!");
		// url = "jdbc:mysql://192.168.99.100:3306/fixme";
		// } else {
		// }
		url = "jdbc:mysql://localhost:3306/fixme?useSSL=false";

		// String dbName = "database_name";
		String driver = "com.mysql.cj.jdbc.Driver";
		String userName = "root";
		String password = "Rootroot3";
		try {
			Class.forName(driver).newInstance();
			this.conn = (Connection) DriverManager.getConnection(url, userName, password);
			System.out.println("Connection created!");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Could not connect to database : Fixme");
		}
	}

	/*
	 *
	 * @return MysqlConnect Database connection object
	 */
	public static synchronized MysqlConnect getDbCon() {
		if (db == null) {
			db = new MysqlConnect();
		}
		return db;
	}

	/*
	 * @param query String The query to be executed
	 *
	 * @return a ResultSet object containing the results or null if not available
	 *
	 * @throws SQLException
	 */
	public ResultSet query(String query) throws SQLException {
		statement = db.conn.createStatement();
		ResultSet res = statement.executeQuery(query);
		return res;
	}

	/*
	 * @desc Method to insert data to a table
	 *
	 * @param insertQuery String The Insert query
	 *
	 * @return boolean
	 *
	 * @throws SQLException
	 */
	public int insert(String insertQuery) throws SQLException {
		statement = db.conn.createStatement();
		int result = statement.executeUpdate(insertQuery);
		return result;
	}

	public int preparedStringInsert(String query, String[] values) throws SQLException {
		int count = 0;
		for (int i = 0; i < query.toCharArray().length; i++) {
			if (query.toCharArray()[i] == '?')
				count++;
		}
		this.pStatement = db.conn.prepareStatement(query);
		for (int i = 0; i < count; i++) {
			this.pStatement.setString(i + 1, values[i]);
		}
		int result = this.pStatement.executeUpdate();
		return result;
	}
}
