package io.github.fill0520.configurablesocketfactory;


import org.testcontainers.containers.PostgreSQLContainer;
import java.util.Map;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;


public class TestConfigurableSocketFactory extends ConfigurableSocketFactory {
    public static final List<Socket> sockets = new ArrayList<>();

    public TestConfigurableSocketFactory() {
        super();
    }

    public TestConfigurableSocketFactory(Properties props) {
        super(props);
    }

    @Override
    protected void configureSocket(Socket socket) throws IOException {
        super.configureSocket(socket);
        sockets.add(socket);
    }

    public static List<Socket> getSockets() {
        return sockets;
    }
}
