package io.github.fill0520.pgkeepalivesocketfactory;

import jdk.net.ExtendedSocketOptions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

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
                        .filter(s -> !s.isClosed())
                        .findFirst()
                        .orElse(null);

                // We expect only one socket in this scenario
                assertEquals(1, allSockets.size(), "Unexpected number of sockets created!");

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
}
