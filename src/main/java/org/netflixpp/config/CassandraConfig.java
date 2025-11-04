package org.netflixpp.config;

import com.datastax.oss.driver.api.core.CqlSession;
import java.net.InetSocketAddress;

public class CassandraConfig {
    private static final String HOST = "localhost";
    private static final int PORT = 9042;
    private static final String KEYSPACE = "netflixchunks";

    private static CqlSession session;

    public static CqlSession getSession() {
        if (session == null) {
            session = CqlSession.builder()
                    .addContactPoint(new InetSocketAddress(HOST, PORT))
                    .withKeyspace(KEYSPACE)
                    .withLocalDatacenter("datacenter1") // nome padrão
                    .build();
        }
        return session;
    }
}
