@GrabConfig(systemClassLoader=true)
@Grab(group='com.h2database', module='h2', version='1.3.168')
@Grab(group='mysql', module='mysql-connector-java', version='5.1.22')

import groovy.sql.Sql
import java.sql.SQLException

@Singleton
class Database {

    private Sql sql

    Database() {
        sql = Sql.newInstance('jdbc:h2:eve;TRACE_LEVEL_FILE=1;TRACE_LEVEL_SYSTEM_OUT=0',
                              'sa', 'sa', 'org.h2.Driver')
    }

    void createLocalDatabase() {
        boolean create = true

        try { create = 0 >= sql.firstRow('SELECT COUNT(*) as count FROM refine;').count }
        catch(SQLException e) {}

        if (!create && !getOptions().forceDB) {
            console.updateStatus 'Local Database Available'
            return
        }

        console.updateStatus 'Building Local Database from predefined MySQL'

        Sql mysql = Sql.newInstance(options['cppDump'])

        sql.execute 'DROP TABLE IF EXISTS types;'
        sql.execute '''
            CREATE TABLE types (
                typeID INT PRIMARY KEY,
                typeName VARCHAR(255),
                volume DOUBLE,
                portionSize INT,
                category VARCHAR(255)
            );
        '''

        mysql.eachRow('''
            SELECT invTypes.typeID as typeID, typeName, volume, portionSize, categoryName
            FROM invTypes
            INNER JOIN invGroups ON invTypes.groupID = invGroups.groupID
            INNER JOIN invCategories ON invCategories.categoryID = invGroups.categoryID
            INNER JOIN (
                SELECT DISTINCT typeID FROM invTypeMaterials
                UNION
                SELECT DISTINCT materialTypeID FROM invTypeMaterials
            ) as invTypeMaterials ON invTypes.typeID = invTypeMaterials.typeID
            WHERE invTypes.published = 1;
        ''') {row ->
            sql.execute 'INSERT INTO types (typeID, typeName, volume, portionSize, category) values (?, ?, ?, ?, ?)',
                        [row.typeID, row.typeName, row.volume, row.portionSize, row.categoryName]
        }

        sql.execute 'DROP TABLE IF EXISTS refine;'
        sql.execute '''
            CREATE TABLE refine (
                typeID INT,
                matID INT,
                quantity INT
            );
        '''

        mysql.eachRow('''
            SELECT invTypeMaterials.typeID as typeID, materialTypeID, quantity
            FROM invTypeMaterials
        ''') {row->
            sql.execute 'INSERT INTO refine (typeID, matID, quantity) VALUES (?, ?, ?)',
                        [row.typeID, row.materialTypeID, row.quantity]
        }

        sql.execute 'DROP TABLE IF EXISTS regions;'
        sql.execute '''
            CREATE TABLE regions (
                regionID INT,
                regionName varchar(255),
            );
        '''

        mysql.eachRow('''
            SELECT regionID, regionName
            FROM mapRegions;
        ''') {row->
            sql.execute 'INSERT INTO regions (regionID, regionName) VALUES (?, ?)',
                        [row.regionID, row.regionName]
        }
    }

    Collection<EveType> fetchAllTypes() {
        sql.rows('SELECT * FROM types').collect {row->
            new EveType(row.typeID, row.typeName, row.volume, row.portionSize, row.category)
        }
    }

    Collection<EveRegion> fetchAllRegions() {
        sql.rows('SELECT * FROM regions').collect {row-> new EveRegion(row.regionID, row.regionName) }
    }

    Map<EveType, Integer> fetchRefineYield(EveType source) {
        sql.rows('SELECT refine.typeID as sourceID,' +
                 '       types.typeID as typeID,' +
                 '       types.typeName as typeName,' +
                 '       quantity as quantity ' +
                 'FROM refine ' +
                 'INNER JOIN types ON refine.matID = types.typeID ' +
                 'WHERE refine.typeID = ?', [source.typeID]).collectEntries {row ->
            [(new EveType(typeID: row.typeID, typeName: row.typeName)): row.quantity]
        }
    }

    void printDB() {
        def header = {meta ->
            (1..meta.columnCount).each {
                print "==${meta.getColumnLabel(it)}".padRight(25, "=")
            }
            println()
        }

        def row = {row ->
            row.toRowResult().values().each {
                print " ${it}".padRight(23)[0..22] + ' |'
            }
            println()
        }

        sql.eachRow('SELECT * FROM types;', header, row)
        println()
        sql.eachRow('SELECT typeID, matID, quantity FROM refine;', header, row)
    }

}
