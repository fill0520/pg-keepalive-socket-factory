# pg-keepalive-socket-factory

**pg-keepalive-socket-factory** is a Java library that provides a custom JDBC `SocketFactory` for PostgreSQL with advanced TCP keep-alive configuration. By leveraging `ExtendedSocketOptions`, it allows fine-grained control over important keep-alive properties (such as idle time, interval, and maximum retry count). This is especially useful in production environments where reliable, persistent connections are critical.

---

## Why Use pg-keepalive-socket-factory?
- **Fine-Tuned Keep-Alive**: Avoid dropped connections by setting custom TCP keep-alive parameters (e.g., idle period, interval, retry count).
- **Easy Integration**: Simply set a few properties and specify the custom socket factory in your JDBC connection.
- **Better Stability**: Especially helpful in containers, Kubernetes, or any network environments prone to silent disconnections.

---

## Requirements
- **Java 8+** (Recommended Java 11+ for robust `ExtendedSocketOptions` support).
- **PostgreSQL JDBC Driver** (e.g., `org.postgresql:postgresql`).

> **Note**: Certain `ExtendedSocketOptions` may not be available on all JVMs or operating systems. If unsupported, a warning will be logged.

---

## Getting Started

### 1. Add the Dependency

pg-keepalive-socket-factory is available in **Maven Central**. Add it to your project:

**Maven**
```xml
<dependency>
    <groupId>io.github.fill0520.pgkeepalivesocketfactory</groupId>
    <artifactId>pg-keepalive-socket-factory</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Configure Your Connection

You’ll typically provide custom socket factory options through the JDBC `Properties` object:

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class Example {
    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        // Specify the custom socket factory class
        props.setProperty("socketFactory", "io.github.fill0520.pgkeepalivesocketfactory.PgKeepAliveSocketFactory");

        // Enable keep-alive
        props.setProperty("keepAlive", "true");
        // Optional: keepAliveIdle in seconds (some OSes interpret it differently)
        props.setProperty("keepAliveIdle", "60");
        // Optional: keepAliveInterval in seconds
        props.setProperty("keepAliveInterval", "15");
        // Optional: keepAliveCount (retries before killing connection)
        props.setProperty("keepAliveCount", "5");

        // Provide other usual PostgreSQL settings
        props.setProperty("user", "postgres");
        props.setProperty("password", "secret");

        String url = "jdbc:postgresql://localhost:5432/mydatabase";
        try (Connection conn = DriverManager.getConnection(url, props)) {
            System.out.println("Connected using custom keep-alive socket factory!");
            // Use the connection...
        }
    }
}
```

Once set, the library automatically applies the custom keep-alive parameters upon socket creation.

---

## Configuration Properties

- **`socketFactory`** (required)
  - Must be `io.github.fill0520.pgkeepalivesocketfactory.PgKeepAliveSocketFactory`.

- **`keepAlive`** (`true` / `false`)
  - When enabled (`true`), TCP keep-alive is turned on.

- **`keepAliveIdle`** (integer)
  - Idle time (in seconds) before the first keep-alive probe is sent.

- **`keepAliveInterval`** (integer)
  - Interval (in seconds) between subsequent keep-alive probes.

- **`keepAliveCount`** (integer)
  - Number of keep-alive probes before the connection is considered dead.

If a value is out of range or not supported by the operating system, a warning is logged and that property is ignored.

---

## Troubleshooting

1. **UnsupportedOperationException**:
   - Indicates that the JVM or OS does not support one or more `ExtendedSocketOptions`.
   - The library will log a warning and ignore the unsupported options.
2. **Connection Timeouts**:
   - Double-check that `keepAlive` is `true` and that your idle/interval/count settings are correct.
3. **Firewall Issues**:
   - Keep-alive packets may still be dropped by firewalls. Ensure your network settings allow keep-alive traffic.

---

## Contributing

Contributions are welcome! Please submit a pull request or open an issue on GitHub.

---

## License

This project is licensed under the **Apache License 2.0**. See the [LICENSE](LICENSE) file for details.
