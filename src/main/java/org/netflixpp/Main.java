package org.netflixpp;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.netflixpp.controller.*;

public class Main {

    public static void main(String[] args) throws Exception {
        // Criação do ResourceConfig para o Jersey
        ResourceConfig config = new ResourceConfig();

        // Registra os controllers manualmente para injeção de dependências
        config.register(AuthController.class);
        config.register(MovieController.class);
        config.register(UserController.class);
        config.register(AdminController.class);

        // Inicializa o servidor Jetty
        ServletHolder servlet = new ServletHolder(new ServletContainer(config));
        Server server = new Server(5000); // Porta 5000 configurada corretamente
        ServletContextHandler contextHandler = new ServletContextHandler(server, "/");
        contextHandler.addServlet(servlet, "/*");

        // Inicia o servidor
        server.start();
        System.out.println("Servidor iniciado na porta 5000...");
        server.join();
    }
}
