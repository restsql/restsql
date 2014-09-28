/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.restsql.core.BinaryObject;
import org.restsql.core.ColumnMetaData;
import org.restsql.core.InvalidRequestException;
import org.restsql.core.RequestValue;
import org.restsql.core.TableMetaData.TableRole;

/**
 * Represents column (field) metadata.
 * 
 * @author Mark Sawers
 */

@XmlType(name = "ColumnMetaData", namespace = "http://restsql.org/schema")
public class ColumnMetaDataImpl implements ColumnMetaData {

	@XmlAttribute(required = true)
	private String columnLabel;

	@XmlAttribute(required = true)
	private String columnName;

	@XmlAttribute(required = true)
	private int columnNumber;

	@XmlAttribute(required = true)
	private int columnType;

	@XmlAttribute(required = true)
	private String columnTypeName;

	@XmlAttribute(required = true)
	private String databaseName;

	@XmlAttribute
	private boolean nonqueriedForeignKey;

	@XmlAttribute
	private boolean primaryKey;

	@XmlAttribute(required = true)
	private String qualifiedColumnLabel;

	@XmlAttribute(required = true)
	private String qualifiedColumnName;

	@XmlAttribute(required = true)
	private String qualifiedTableName;

	@XmlAttribute
	private boolean readOnly;

	@XmlAttribute
	private boolean sequence;

	@XmlAttribute
	private String sequenceName;

	@XmlAttribute(required = true)
	private String tableName;

	@XmlTransient
	private TableRole tableRole;

	// No-arg ctor required for JAXB
	public ColumnMetaDataImpl() {
	}

	@Override
	public int compareTo(final ColumnMetaData column) {
		if (columnNumber < column.getColumnNumber()) {
			return -1;
		} else if (columnNumber > column.getColumnNumber()) {
			return 1;
		} else {
			return 0;
		}
	}

	@Override
	public String getColumnLabel() {
		return columnLabel;
	}

	@Override
	public String getColumnName() {
		return columnName;
	}

	@Override
	public int getColumnNumber() {
		return columnNumber;
	}

	@Override
	public int getColumnType() {
		return columnType;
	}

	@Override
	public String getColumnTypeName() {
		return columnTypeName;
	}

	@Override
	public String getDatabaseName() {
		return databaseName;
	}

	@Override
	public String getQualifiedColumnLabel() {
		return qualifiedColumnLabel;
	}

	@Override
	public String getQualifiedColumnName() {
		return qualifiedColumnName;
	}

	@Override
	public String getQualifiedTableName() {
		return qualifiedTableName;
	}

	@Override
	public Object getResultByLabel(final ResultSet resultSet) throws SQLException {
		if (isBinaryType()) {
			return new BinaryObject(resultSet.getBytes(qualifiedColumnLabel));
		} else {
			return resultSet.getObject(qualifiedColumnLabel);
		}
	}

	@Override
	public Object getResultByNumber(final ResultSet resultSet) throws SQLException {
		if (isBinaryType()) {
			return new BinaryObject(resultSet.getBytes(columnNumber));
		} else {
			return resultSet.getObject(columnNumber);
		}
	}

	@XmlTransient
	@Override
	public String getSequenceName() {
		return sequenceName;
	}

	@Override
	public String getTableName() {
		return tableName;
	}

	@XmlTransient
	@Override
	public TableRole getTableRole() {
		return tableRole;
	}

	@Override
	public boolean isBinaryType() {
		boolean binaryType = false;
		switch (columnType) {
			case Types.BINARY:
			case Types.BLOB:
			case Types.JAVA_OBJECT:
			case Types.LONGVARBINARY:
				binaryType = true;
				break;

			default:
				// do nothing
		}
		return binaryType;
	}

	@Override
	public boolean isCharOrDateTimeType() {
		boolean charOrDateTimeType = false;
		switch (columnType) {
			case Types.CHAR:
			case Types.NCHAR:
			case Types.VARCHAR:
			case Types.NVARCHAR:
			case Types.LONGVARCHAR:
			case Types.LONGNVARCHAR:
			case Types.TIME:
			case Types.TIMESTAMP:
			case Types.DATE:
			case Types.OTHER: // postgresql driver returns this for char-type enums
				charOrDateTimeType = true;
				break;

			default:
				// do nothing
		}
		return charOrDateTimeType;
	}

	@Override
	public boolean isNonqueriedForeignKey() {
		return nonqueriedForeignKey;
	}

	@XmlTransient
	@Override
	public boolean isPrimaryKey() {
		return primaryKey;
	}

	@Override
	public boolean isReadOnly() {
		return readOnly;
	}

	@XmlTransient
	@Override
	public boolean isSequence() {
		return sequence;
	}

	@Override
	@SuppressWarnings("fallthrough")
	public void normalizeValue(final RequestValue requestValue) throws InvalidRequestException {
		Object value = requestValue.getValue();
		if (value instanceof String && requestValue.getOperator() != RequestValue.Operator.In) {
			try {
				switch (getColumnType()) {
					case Types.BOOLEAN:
						value = Boolean.valueOf((String) value);
						break;

					case Types.BIT:
					case Types.TINYINT:
					case Types.SMALLINT:
					case Types.INTEGER:
						value = Integer.valueOf((String) value);
						break;

					case Types.BIGINT:
						value = Long.valueOf((String) value);
						break;

					case Types.NUMERIC:
					case Types.DECIMAL:
					case Types.FLOAT:
					case Types.REAL:
						value = Float.valueOf((String) value);
						break;

					case Types.DOUBLE:
						value = Double.valueOf((String) value);
						break;

					case Types.BINARY:
					case Types.BLOB:
					case Types.JAVA_OBJECT:
					case Types.LONGVARBINARY:
						if (BinaryObject.isStringBase64((String) value)) {
							value = BinaryObject.fromString((String) value);
						} else {
							throw new InvalidRequestException(
									InvalidRequestException.MESSAGE_CANNOT_BASE64DECODE,
									requestValue.getName());
						}

					case Types.DATE:
					case Types.TIME:
					case Types.TIMESTAMP:
					default:
						// do nothing
				}
			} catch (final NumberFormatException e) {
				throw new InvalidRequestException("Could not convert " + requestValue.getName() + " value " + value + " to number");
			}
		}

		requestValue.setValue(value);
	}

	/**
	 * Used for all columns declared in the SqlResource select clause.
	 */
	@Override
	public void setAttributes(final int columnNumber, final String databaseName,
			final String qualifiedTableName, final String tableName, final String columnName,
			final String qualifiedColumnName, final String columnLabel, final String qualifiedColumnLabel,
			final String columnTypeName, final int columnType, final boolean readOnly) {
		this.columnNumber = columnNumber;
		this.databaseName = databaseName;
		this.qualifiedTableName = qualifiedTableName;
		this.tableName = tableName;
		this.columnName = columnName;
		this.qualifiedColumnName = qualifiedColumnName;
		this.columnLabel = columnLabel;
		this.qualifiedColumnLabel = qualifiedColumnLabel;
		this.columnTypeName = columnTypeName;
		this.columnType = getColumnType(columnType, columnTypeName);
		this.readOnly = readOnly;

	}

	/**
	 * Used for foreign key columns not declared in the SqlResource select columns. These are required for writes to
	 * child extensions, parent extensions and child tables.
	 */
	@Override
	public void setAttributes(final String databaseName, final String sqlQualifiedTableName,
			final String tableName, final TableRole tableRole, final String columnName,
			final String qualifiedColumnName, final String columnLabel, final String qualifiedColumnLabel,
			final String columnTypeString) {
		setAttributes(0, databaseName, sqlQualifiedTableName, tableName, columnName, qualifiedColumnName,
				columnLabel, qualifiedColumnLabel, columnTypeString, 0, false);
		setTableRole(tableRole);
		if (columnTypeString.equalsIgnoreCase("BIT")) {
			columnType = Types.BIT;
		} else if (columnTypeString.equalsIgnoreCase("TINYINT")) {
			columnType = Types.TINYINT;
		} else if (columnTypeString.equalsIgnoreCase("SMALLINT")) {
			columnType = Types.SMALLINT;
		} else if (columnTypeString.equalsIgnoreCase("INTEGER")) {
			columnType = Types.INTEGER;
		} else if (columnTypeString.equalsIgnoreCase("BIGINT")) {
			columnType = Types.BIGINT;
		} else if (columnTypeString.equalsIgnoreCase("FLOAT")) {
			columnType = Types.FLOAT;
		} else if (columnTypeString.equalsIgnoreCase("REAL")) {
			columnType = Types.REAL;
		} else if (columnTypeString.equalsIgnoreCase("DOUBLE")) {
			columnType = Types.DOUBLE;
		} else if (columnTypeString.equalsIgnoreCase("NUMERIC")) {
			columnType = Types.NUMERIC;
		} else if (columnTypeString.equalsIgnoreCase("DECIMAL")) {
			columnType = Types.DECIMAL;
		} else if (columnTypeString.equalsIgnoreCase("CHAR")) {
			columnType = Types.CHAR;
		} else if (columnTypeString.equalsIgnoreCase("VARCHAR")) {
			columnType = Types.VARCHAR;
		} else if (columnTypeString.equalsIgnoreCase("LONGVARCHAR")) {
			columnType = Types.LONGVARCHAR;
		} else if (columnTypeString.equalsIgnoreCase("DATE")) {
			columnType = Types.DATE;
		} else if (columnTypeString.equalsIgnoreCase("TIME")) {
			columnType = Types.TIME;
		} else if (columnTypeString.equalsIgnoreCase("TIMESTAMP")) {
			columnType = Types.TIMESTAMP;
		} else if (columnTypeString.equalsIgnoreCase("BINARY")) {
			columnType = Types.BINARY;
		} else if (columnTypeString.equalsIgnoreCase("VARBINARY")) {
			columnType = Types.VARBINARY;
		} else if (columnTypeString.equalsIgnoreCase("LONGVARBINARY")) {
			columnType = Types.LONGVARBINARY;
		} else if (columnTypeString.equalsIgnoreCase("OTHER")) {
			columnType = Types.OTHER;
		} else if (columnTypeString.equalsIgnoreCase("JAVA_OBJECT")) {
			columnType = Types.JAVA_OBJECT;
		} else if (columnTypeString.equalsIgnoreCase("DISTICT")) {
			columnType = Types.DISTINCT;
		} else if (columnTypeString.equalsIgnoreCase("STRUCT")) {
			columnType = Types.STRUCT;
		} else if (columnTypeString.equalsIgnoreCase("ARRAY")) {
			columnType = Types.ARRAY;
		} else if (columnTypeString.equalsIgnoreCase("BLOB")) {
			columnType = Types.BLOB;
		} else if (columnTypeString.equalsIgnoreCase("CLOB")) {
			columnType = Types.CLOB;
		} else if (columnTypeString.equalsIgnoreCase("REF")) {
			columnType = Types.REF;
		} else if (columnTypeString.equalsIgnoreCase("DATALINK")) {
			columnType = Types.DATALINK;
		} else if (columnTypeString.equalsIgnoreCase("BOOLEAN")) {
			columnType = Types.BOOLEAN;
		} else if (columnTypeString.equalsIgnoreCase("ROWID")) {
			columnType = Types.ROWID;
		} else if (columnTypeString.equalsIgnoreCase("NCHAR")) {
			columnType = Types.NCHAR;
		} else if (columnTypeString.equalsIgnoreCase("NVARCHAR")) {
			columnType = Types.NVARCHAR;
		} else if (columnTypeString.equalsIgnoreCase("LONGNVARCHAR")) {
			columnType = Types.LONGNVARCHAR;
		} else if (columnTypeString.equalsIgnoreCase("NCLOB")) {
			columnType = Types.NCLOB;
		} else if (columnTypeString.equalsIgnoreCase("SQLXML")) {
			columnType = Types.SQLXML;
		} else {
			columnType = Types.NULL;
		}

		nonqueriedForeignKey = true;
	}

	@Override
	public void setPrimaryKey(final boolean primaryKey) {
		this.primaryKey = primaryKey;
	}

	@Override
	public void setSequence(final boolean sequence) {
		this.sequence = sequence;
	}

	@Override
	public void setSequenceName(final String sequenceName) {
		this.sequenceName = sequenceName;
	}

	@Override
	public void setTableRole(final TableRole tableRole) {
		this.tableRole = tableRole;
	}

	/**
	 * Method for subclasses to customize column type if desired.
	 * 
	 * @return one of the constants in {java.sql.Types}
	 * @see java.sql.Types
	 */
	protected int getColumnType(final int columnType, final String columnTypeName) {
		return columnType;
	}
}