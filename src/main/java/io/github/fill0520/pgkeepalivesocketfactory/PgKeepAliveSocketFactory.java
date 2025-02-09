package io.github.fill0520.pgkeepalivesocketfactory;

import java.io.IOException;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.Properties;
import javax.net.SocketFactory;
import jdk.net.ExtendedSocketOptions;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

/**
 * PgKeepAliveSocketFactory provides a configurable socket factory for establishing connections with PostgreSQL.
 * It allows setting socket options such as keep-alive settings using Java's ExtendedSocketOptions.
 */
public class PgKeepAliveSocketFactory extends SocketFactory {

    /**
     * Stores all created sockets for potential tracking or cleanup.
     */
    public static final List<Socket> sockets = new ArrayList<>();

    /**
     * Defines allowable ranges for keep-alive settings.
     */
    private static final Map<String, Integer[]> PROPERTY_LIMITS = Map.of(
        "keepAliveIdle", new Integer[]{1, 32767},
        "keepAliveInterval", new Integer[]{1, 32767},
        "keepAliveCount", new Integer[]{1, 255}
    );

    /**
     * Stores parsed configuration values for socket settings.
     */
    private final Map<String, Object> configValues = new HashMap<>();

    public PgKeepAliveSocketFactory() {}

    /**
     * Constructs a PgKeepAliveSocketFactory with given properties.
     * @param props Properties object containing socket configuration values.
     */
    public PgKeepAliveSocketFactory(Properties props) {
        this.loadProperties(props);
    }

    private void loadProperties(Properties props) {
        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            if (value == null) {
                System.err.println("Warning: Property " + key + " is null and will be ignored.");
                continue;
            }
            switch (key) {
                case "keepAlive":
                    configValues.put(key, parseBooleanProperty(key, value));
                    break;
                case "keepAliveIdle":
                case "keepAliveInterval":
                case "keepAliveCount":
                    configValues.put(key, parseIntegerProperty(key, value));
                    break;
                default:
                    break;
            }
        }
    }

    private Boolean parseBooleanProperty(String propertyName, String value) {
        value = value.trim().toLowerCase();
        if (!value.equals("true") && !value.equals("false")) {
            System.err.println("Warning: Invalid boolean value for " + propertyName + ": " + value + ". Using default (false). ");
            return false;
        }
        return Boolean.parseBoolean(value);
    }

    private Integer parseIntegerProperty(String propertyName, String value) {
        try {
            Integer parsedValue = Integer.parseInt(value.trim());
            if (PROPERTY_LIMITS.containsKey(propertyName)) {
                Integer[] limits = PROPERTY_LIMITS.get(propertyName);
                if (parsedValue < limits[0] || parsedValue > limits[1]) {
                    System.err.println("Warning: Invalid value for " + propertyName + ": " + value + ". Must be between " + limits[0] + " and " + limits[1] + ". Ignoring.");
                    return null;
                }
            }
            return parsedValue;
        } catch (NumberFormatException e) {
            System.err.println("Warning: Invalid integer value for " + propertyName + ": " + value + ". Ignoring.");
            return null;
        }
    }

    @Override
    public Socket createSocket() throws IOException {
        Socket socket = new Socket();
        configureSocket(socket);
        sockets.add(socket);
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        Socket socket = new Socket();
        configureSocket(socket);
        socket.connect(new InetSocketAddress(host, port));
        sockets.add(socket);
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port, java.net.InetAddress localHost, int localPort) throws IOException {
        Socket socket = new Socket();
        configureSocket(socket);
        socket.bind(new InetSocketAddress(localHost, localPort));
        socket.connect(new InetSocketAddress(host, port));
        sockets.add(socket);
        return socket;
    }

    @Override
    public Socket createSocket(java.net.InetAddress host, int port) throws IOException {
        Socket socket = new Socket();
        configureSocket(socket);
        socket.connect(new InetSocketAddress(host, port));
        sockets.add(socket);
        return socket;
    }

    @Override
    public Socket createSocket(java.net.InetAddress address, int port, java.net.InetAddress localAddress, int localPort) throws IOException {
        Socket socket = new Socket();
        configureSocket(socket);
        socket.bind(new InetSocketAddress(localAddress, localPort));
        socket.connect(new InetSocketAddress(address, port));
        sockets.add(socket);
        return socket;
    }

    private void configureSocket(Socket socket) throws IOException {
        Boolean keepAlive = (Boolean) configValues.get("keepAlive");
        if (keepAlive != null) {
            socket.setKeepAlive(keepAlive);
            if (Boolean.TRUE.equals(keepAlive)) {
                try {
                    if (configValues.containsKey("keepAliveIdle") && configValues.get("keepAliveIdle") != null) {
                        socket.setOption(ExtendedSocketOptions.TCP_KEEPIDLE, (Integer) configValues.get("keepAliveIdle"));
                    }
                    if (configValues.containsKey("keepAliveInterval") && configValues.get("keepAliveInterval") != null) {
                        socket.setOption(ExtendedSocketOptions.TCP_KEEPINTERVAL, (Integer) configValues.get("keepAliveInterval"));
                    }
                    if (configValues.containsKey("keepAliveCount") && configValues.get("keepAliveCount") != null) {
                        socket.setOption(ExtendedSocketOptions.TCP_KEEPCOUNT, (Integer) configValues.get("keepAliveCount"));
                    }
                } catch (UnsupportedOperationException e) {
                    System.err.println("Warning: ExtendedSocketOptions are not supported on this system: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Retrieves the list of all created sockets.
     */
    public static List<Socket> getSockets() {
        return sockets;
    }
}
