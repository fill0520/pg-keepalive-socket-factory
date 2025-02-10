# pg-keepalive-socket-factory

[![Maven Central](https://img.shields.io/maven-central/v/io.github.fill0520/pg-keepalive-socket-factory)](https://central.sonatype.com/artifact/io.github.fill0520/pg-keepalive-socket-factory)
[![License](https://img.shields.io/github/license/fill0520/pg-keepalive-socket-factory)](LICENSE)
[![CI](https://github.com/fill0520/pg-keepalive-socket-factory/actions/workflows/ci.yml/badge.svg)](https://github.com/fill0520/pg-keepalive-socket-factory/actions)
[![Coverage](https://codecov.io/gh/fill0520/pg-keepalive-socket-factory/branch/release-1.0.0/graph/badge.svg)](https://codecov.io/gh/fill0520/pg-keepalive-socket-factory)
[![Last Commit](https://img.shields.io/github/last-commit/fill0520/pg-keepalive-socket-factory)](https://github.com/fill0520/pg-keepalive-socket-factory/commits/main)
[![GitHub stars](https://img.shields.io/github/stars/fill0520/pg-keepalive-socket-factory?style=social)](https://github.com/fill0520/pg-keepalive-socket-factory/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/fill0520/pg-keepalive-socket-factory?style=social)](https://github.com/fill0520/pg-keepalive-socket-factory/network/members)


**pg-keepalive-socket-factory** is a Java library that provides a custom JDBC `SocketFactory` for PostgreSQL with advanced TCP keep-alive configuration. By leveraging `ExtendedSocketOptions`, it allows fine-grained control over important keep-alive properties (such as idle time, interval, and maximum retry count). This is especially useful in production environments where reliable, persistent connections are critical.

---

## Why Use pg-keepalive-socket-factory?
- **Fine-Tuned Keep-Alive**: Avoid dropped connections by setting custom TCP keep-alive parameters (e.g., idle period, interval, retry count).
- **Easy Integration**: Simply set a few properties and specify the custom socket factory in your JDBC connection.
- **Better Stability**: Especially helpful in containers, Kubernetes, or any network environments prone to silent disconnections.

---

## Requirements
- **Java 23+**.
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

public class Example {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://localhost:5432/test?"
        + "user=test_user&"
        + "password=password123&"
        + "socketFactory=io.github.fill0520.pgkeepalivesocketfactory.PgKeepAliveSocketFactory&"
        + "keepAlive=true&"
        + "keepAliveIdle=60&"
        + "keepAliveInterval=15&"
        + "keepAliveCount=5";

        try (Connection conn = DriverManager.getConnection(url)) {
            System.out.println("Connected using custom keep-alive socket factory!");
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
