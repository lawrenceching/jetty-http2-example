package me.imlc.example.jettyhttp2;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static java.lang.System.out;
import static me.imlc.example.jettyhttp2.Commons.sleep;

public class Http2WithPushServer {

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
        sslContextFactory.setKeyStorePath(Http2WithPushServer.class.getResource("/dev.jks").toExternalForm());
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
        server.setHandler(new HandlerCollection(new IndexHtmlHandler(), new ApiHandler()));

        server.start();

        out.println("Application started at https://localhost:" + port);

        server.join();
    }

    private static class IndexHtmlHandler extends AbstractHandler {

        @Override
        public void handle(String target, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {

            out.println("> " + target);

            switch(target) {
                case "/":
                case "/index.html": {

                    for(int i=0; i < 20; ++i) {
                        String path = "/api/posts/" + i;
                        request.getPushBuilder()
                                .path(path)
                                .method("GET")
                                .push();
                        out.println("Pushed " + path);
                    }

                    sleep(5000);

                    URL indexHtml = IndexHtmlHandler.class.getResource("/index.html");
                    Objects.requireNonNull(indexHtml);
                    try {
                        String html = Files.readString(Path.of(indexHtml.toURI()));
                        httpServletResponse.setContentType("text/html; charset=utf-8");
                        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                        httpServletResponse.getWriter().write(html);

                        request.setHandled(true);
                        return;

                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                default: {
                    httpServletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    request.setHandled(true);
                }
            }
        }
    }

    private static class ApiHandler extends AbstractHandler {
        @Override
        public void handle(String target, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
            if(target.startsWith("/api/posts/")) {
                sleep(5000);
                httpServletResponse.setContentType("application/x-javascript; charset=utf-8");
                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                httpServletResponse.getWriter().write("Data data data");

                request.setHandled(true);
                return;
            }
        }
    }

}
