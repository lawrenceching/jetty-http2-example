package me.imlc.example.jettyhttp2;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static java.lang.System.out;

public class SimpleHttp2Server {

    public static void main(String[] args) throws Exception {

        int port = Integer.parseInt(System.getProperty("http.port", "8443"));

        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setSecurePort(port);
        httpConfiguration.setSecureScheme("https");
        httpConfiguration.addCustomizer(new SecureRequestCustomizer());

        HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfiguration);

        HTTP2ServerConnectionFactory http2ServerConnectionFactory = new HTTP2ServerConnectionFactory(httpConfiguration);

        ALPNServerConnectionFactory alpnServerConnectionFactory = new ALPNServerConnectionFactory();
        alpnServerConnectionFactory.setDefaultProtocol(httpConnectionFactory.getProtocol());

        SslContextFactory sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath(SimpleHttp2Server.class.getResource("/dev.jks").toExternalForm());
        sslContextFactory.setKeyStorePassword("changeit");
        sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);

        SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(sslContextFactory, alpnServerConnectionFactory.getProtocol());

        Server server = new Server();

        ServerConnector serverConnector = new ServerConnector(
                server,
                sslConnectionFactory,
                alpnServerConnectionFactory,
                http2ServerConnectionFactory,
                httpConnectionFactory
        );

        serverConnector.setPort(port);

        server.addConnector(serverConnector);
        server.setHandler(new HelloWorldHandler());

        server.start();

        out.println("Application started at https://localhost:" + port);

        server.join();
    }

    private static class HelloWorldHandler extends AbstractHandler {
        @Override
        public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
            httpServletResponse.setContentType("text/plain; charset=utf-8");
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            httpServletResponse.getWriter().write("Hello, world!");
            request.setHandled(true);
        }
    }

}
