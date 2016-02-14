package com.zendesk.maxwell.schema.ddl;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import com.zendesk.maxwell.MaxwellFilter;
import com.zendesk.maxwell.schema.Database;
import com.zendesk.maxwell.schema.Schema;
import com.zendesk.maxwell.schema.Table;

public class TableAlter extends SchemaChange {
	public String database;
	public String table;
	@JsonProperty("column-changes")
	public ArrayList<ColumnMod> columnMods;
	@JsonProperty("new-database")
	public String newDatabase;
	@JsonProperty("new-table")
	public String newTableName;

	@JsonProperty("convert-charset")
	public String convertCharset;
	@JsonProperty("charset")
	public String defaultCharset;

	@JsonProperty("primary-keys")
	public List<String> pks;

	public TableAlter() {
		this.columnMods = new ArrayList<>();
	}

	public TableAlter(String database, String table) {
		this();
		this.database = database;
		this.table = table;
	}

	@Override
	public String toString() {
		return "TableAlter<database: " + database + ", table:" + table + ">";
	}

	@Override
	public Schema apply(Schema originalSchema) throws SchemaSyncError {
		Schema newSchema = originalSchema.copy();

		Database database = newSchema.findDatabase(this.database);
		if ( database == null ) {
			throw new SchemaSyncError("Couldn't find database: " + this.database);
		}

		Table table = database.findTable(this.table);
		if ( table == null ) {
			throw new SchemaSyncError("Couldn't find table: " + this.database + "." + this.table);
		}


		if ( newTableName != null && newDatabase != null ) {
			Database destDB = newSchema.findDatabase(this.newDatabase);
			if ( destDB == null )
				throw new SchemaSyncError("Couldn't find database " + this.database);

			table.rename(newTableName);

			database.getTableList().remove(table);
			destDB.addTable(table);
		}

		for (ColumnMod mod : columnMods) {
			mod.apply(table);
		}

		if ( this.pks != null ) {
			table.setPKList(this.pks);
		}
		table.setDefaultColumnEncodings();

		return newSchema;
	}

	@Override
	public boolean isBlacklisted(MaxwellFilter filter) {
		if ( filter == null ) {
			return false;
		} else {
			return filter.isTableBlacklisted(this.table);
		}
	}
}