@GrabConfig(systemClassLoader=true)
@Grab(group='com.h2database', module='h2', version='1.3.168')
@Grab(group='mysql', module='mysql-connector-java', version='5.1.22')

import groovy.sql.Sql
import java.sql.SQLException

@Singleton
class Database {

    private Sql sql

    Database() {
        sql = Sql.newInstance('jdbc:h2:eve;TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=0',
                              'sa', 'sa', 'org.h2.Driver')
    }

    void createLocalDatabase() {
        boolean create = true

        try { create = 0 >= sql.firstRow('SELECT COUNT(*) as count FROM refine;').count }
        catch(SQLException e) {}

        if (!create && !getOptions().forceDB) {
            println 'Local Database Available'
            return
        }

        println 'Building Local Database from predefined MySQL'

        Sql mysql = Sql.newInstance('jdbc:mysql://localhost/eve?user=root')

        sql.execute 'DROP TABLE IF EXISTS types;'
        sql.execute '''
            CREATE TABLE types (
                typeID INT PRIMARY KEY,
                typeName VARCHAR(255),
                volume INT,
                portionSize INT,
                category VARCHAR(255)
            );
        '''

        mysql.eachRow('''
            SELECT typeID, typeName, volume, portionSize, categoryName
            FROM invTypes
            INNER JOIN invGroups ON invTypes.groupID = invGroups.groupID
            INNER JOIN invCategories ON invCategories.categoryID = invGroups.categoryID
            WHERE (
                invCategories.categoryName = 'Asteroid'
                OR invGroups.groupName = 'Mineral'
            ) AND invTypes.published = 1;
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

        def justAsteroids = sql.rows('SELECT typeID FROM types WHERE category = \'Asteroid\'').typeID
        mysql.eachRow("""
            SELECT invTypeMaterials.typeID as typeID, materialTypeID, quantity
            FROM invTypeMaterials
            INNER JOIN invTypes ON invTypes.typeID = invTypeMaterials.materialTypeID
            WHERE invTypeMaterials.typeID IN (${justAsteroids.collect {'?'}.join(', ')});
        """, justAsteroids) {row->
            sql.execute 'INSERT INTO refine (typeID, matID, quantity) VALUES (?, ?, ?)',
                        [row.typeID, row.materialTypeID, row.quantity]
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
