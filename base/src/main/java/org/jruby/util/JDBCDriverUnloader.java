package org.jruby.util;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class gets specially loaded directly in the JRubyClassLoader so that
 * we can unregister all JDBC drivers that were loaded/registered. It gets
 * invoked as part of the Ruby runtime tear down.
 *
 * See http://bugs.jruby.org/4226.
 */
public class JDBCDriverUnloader implements Runnable, Iterable<Driver> {
    public void run() {
        for (Driver d : this) {
            try {
                DriverManager.deregisterDriver(d);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Iterator<Driver> iterator() {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        ArrayList<Driver> driverList = new ArrayList();
        while (drivers.hasMoreElements()) {
            Driver d = drivers.nextElement();
            // JRUBY-5528: Don't unload drivers loaded by parent classloaders.
            if (d.getClass().getClassLoader() == JDBCDriverUnloader.class.getClassLoader()) {
                driverList.add(d);
            }
        }
        return driverList.iterator();
    }
}
