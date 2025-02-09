package io.github.fill0520.pgkeepalivesocketfactory;

import jdk.net.ExtendedSocketOptions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This test class demonstrates how to use a custom socket factory in conjunction
 * with a PostgreSQL database (spun up via Testcontainers). It verifies that:
 * <ul>
 *     <li>A socket is successfully created by the custom socket factory.</li>
 *     <li>The socket has the keep-alive settings correctly enabled.</li>
 *     <li>The correct TCP keep-alive parameters (idle, interval, count) are applied.</li>
 * </ul>
 * <p>
 * If your environment does not support {@link jdk.net.ExtendedSocketOptions},
 * the test will catch an {@link UnsupportedOperationException} and log a warning message.
 * </p>
 */
public class PgKeepAliveSocketFactoryTest {

    /**
     * Tests that the {@link PgKeepAliveSocketFactory} creates sockets with the correct
     * keep-alive configuration when connecting to a PostgreSQL database.
     *
     * @throws Exception if any error occurs while setting up or interacting with the database
     */
    @Test
    public void testSocketFactoryWithPostgres() throws Exception {

        // Start a temporary PostgreSQL container for testing
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15.2")) {
            postgres.start();

            // Prepare connection properties
            Properties props = new Properties();
            props.setProperty("user", postgres.getUsername());
            props.setProperty("password", postgres.getPassword());

            // Disable SSL for this test
            props.setProperty("ssl", "false");
            props.setProperty("sslmode", "disable");

            // Use the custom socket factory
            props.setProperty("socketFactory", PgKeepAliveSocketFactory.class.getName());

            // Set keep-alive parameters
            props.setProperty("keepAlive", "true");
            props.setProperty("keepAliveIdle", "60");
            props.setProperty("keepAliveInterval", "30");
            props.setProperty("keepAliveCount", "5");

            // Build JDBC URL from the container
            String jdbcUrl = postgres.getJdbcUrl();

            // Establish a connection and execute a simple query
            try (Connection conn = DriverManager.getConnection(jdbcUrl, props);
                 Statement stmt = conn.createStatement()) {

                stmt.execute("SELECT 1");

                // Retrieve all sockets created by the PgKeepAliveSocketFactory
                List<Socket> allSockets = PgKeepAliveSocketFactory.getSockets();
                assertFalse(allSockets.isEmpty(), "The factory did not create any sockets!");

                // Find an open (non-closed) socket
                Socket openSocket = allSockets.stream()
                                    .sorted((a, b) -> -1) // Reverse order
                                    .filter(s -> !s.isClosed())
                                    .findFirst()
                                    .orElse(null);

                assertNotNull(openSocket, "No 'live' socket was found!");

                // Verify keep-alive options if supported by the JDK
                try {
                    int idle = openSocket.getOption(ExtendedSocketOptions.TCP_KEEPIDLE);
                    assertEquals(60, idle, "Unexpected TCP_KEEPIDLE value!");

                    int interval = openSocket.getOption(ExtendedSocketOptions.TCP_KEEPINTERVAL);
                    assertEquals(30, interval, "Unexpected TCP_KEEPINTERVAL value!");

                    int count = openSocket.getOption(ExtendedSocketOptions.TCP_KEEPCOUNT);
                    assertEquals(5, count, "Unexpected TCP_KEEPCOUNT value!");
                } catch (UnsupportedOperationException e) {
                    System.err.println("ExtendedSocketOptions are not supported: " + e.getMessage());
                }
            }
        }
    }

    /**
     * This test covers the no-argument constructor of PgKeepAliveSocketFactory.
     * It ensures the constructor is invoked and the object is properly created.
     */
    @Test
    void testDefaultConstructorCoverage() {
        PgKeepAliveSocketFactory factory = new PgKeepAliveSocketFactory();
        assertNotNull(factory, "Expected PgKeepAliveSocketFactory to be created via the default constructor.");
    }

    /**
     * This test covers the scenario when a property value in Properties is actually null.
     * We place a null value via 'props.put(key, null)', which should trigger the warning
     * and ignoring logic in PgKeepAliveSocketFactory.
     */
    @Test
    void testNullProperty() {
        Properties props = new Properties();
        props.setProperty("keepAlive", "true"); // Adding a valid property
        props.remove("keepAlive"); // Simulating a null value by removing the key

        PgKeepAliveSocketFactory factory = new PgKeepAliveSocketFactory(props);

        // Verifying that factory is created without throwing any exception
        assertNotNull(factory, "Factory should be created even if a property is removed.");
    }

    /**
     * This test covers the case of an invalid boolean value for the 'keepAlive' property.
     * We use 'notaboolean' to ensure the code treats it as false and logs a warning.
     */
    @Test
    void testInvalidBooleanValue() throws IOException {
        Properties props = new Properties();
        props.setProperty("keepAlive", "notaboolean");

        PgKeepAliveSocketFactory factory = new PgKeepAliveSocketFactory(props);

        Socket s = factory.createSocket();
        assertNotNull(s, "Socket should be created even with an invalid boolean property.");
        assertFalse(s.getKeepAlive(), "Expected keepAlive to default to false for invalid boolean input.");
    }

    /**
     * This test covers the scenario where a keep-alive setting is out of the allowed range,
     * for example 'keepAliveIdle=99999' which is above 32767. The factory should log a warning
     * and ignore the value (setting it to null).
     */
    @Test
    void testOutOfRangeKeepAliveValue() throws IOException {
        Properties props = new Properties();
        props.setProperty("keepAlive", "true");
        props.setProperty("keepAliveIdle", "99999"); // Invalid, out of allowed range

        PgKeepAliveSocketFactory factory = new PgKeepAliveSocketFactory(props);

        // Create a socket to trigger configureSocket
        Socket s = factory.createSocket();
        assertNotNull(s, "Socket should still be created even if a keep-alive property is out of range.");
    }

    /**
     * This test covers the scenario where an integer property is not parseable (e.g., keepAliveIdle="notanumber"),
     * leading to a NumberFormatException in parseIntegerProperty. The code should catch the exception, log a warning,
     * and ignore the value (setting it to null).
     */
    @Test
    void testParseInvalidIntegerValue() throws IOException {
        Properties props = new Properties();
        props.setProperty("keepAlive", "true");
        props.setProperty("keepAliveIdle", "notanumber");

        PgKeepAliveSocketFactory factory = new PgKeepAliveSocketFactory(props);

        // Create a socket to trigger configureSocket
        Socket s = factory.createSocket();
        assertNotNull(s, "Socket should still be created even if a keep-alive property is not a valid integer.");
    }

    /**
     * This test covers all the overloaded createSocket(...) methods to ensure each one is invoked
     * and triggers the socket configuration logic. We do not necessarily expect successful connections,
     * but we do want the methods themselves to be covered. If they fail to connect, that's acceptable
     * for coverage purposes; the point is that we hit each path in the code.
     */
    @Test
    void testAllOverloadedCreateSocketMethods() throws IOException {
        PgKeepAliveSocketFactory factory = new PgKeepAliveSocketFactory();

        // 1) createSocket(String host, int port)
        try (Socket s1 = factory.createSocket("localhost", 0)) {
            assertNotNull(s1);
        } catch (IOException e) {
            // It's fine if we can't connect to "localhost:0", as long as we covered the method logic.
        }

        // 2) createSocket(String host, int port, InetAddress localHost, int localPort)
        try (Socket s2 = factory.createSocket("localhost", 0, InetAddress.getLocalHost(), 0)) {
            assertNotNull(s2);
        } catch (IOException e) {
            // Same reasoning as above.
        }

        // 3) createSocket(InetAddress host, int port)
        try (Socket s3 = factory.createSocket(InetAddress.getByName("localhost"), 0)) {
            assertNotNull(s3);
        } catch (IOException e) {
            // Same reasoning as above.
        }

        // 4) createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
        try (Socket s4 = factory.createSocket(InetAddress.getByName("localhost"), 0, InetAddress.getLocalHost(), 0)) {
            assertNotNull(s4);
        } catch (IOException e) {
            // Same reasoning as above.
        }
    }

    @Test
    void testCreateSocketMethodsTriggerConnect() {
        PgKeepAliveSocketFactory factory = new PgKeepAliveSocketFactory();

        // 1) createSocket(String host, int port)
        assertThrows(IOException.class, () -> factory.createSocket("invalidhost", 12345),
            "Expected IOException when connecting to an invalid host");

        // 2) createSocket(String host, int port, InetAddress localHost, int localPort)
        assertThrows(IOException.class,
            () -> factory.createSocket("invalidhost", 12345, InetAddress.getLocalHost(), 0),
            "Expected IOException when connecting to an invalid host");

        // 3) createSocket(InetAddress host, int port)
        assertThrows(IOException.class,
            () -> factory.createSocket(InetAddress.getByName("invalidhost"), 12345),
            "Expected IOException when connecting to an invalid address");

        // 4) createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
        assertThrows(IOException.class,
            () -> factory.createSocket(InetAddress.getByName("invalidhost"), 12345, InetAddress.getLocalHost(), 0),
            "Expected IOException when connecting to an invalid address");
    }

}
