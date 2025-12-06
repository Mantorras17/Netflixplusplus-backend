package org.netflixpp.mesh;

public class MeshConstants {
    public static final int CHUNK_SIZE = 10 * 1024 * 1024; // 10MB
    public static final int MESH_PORT = 9001;
    public static final String STORAGE_PATH = "storage/chunks/";
    public static final String PROTOCOL_VERSION = "1.0";

    // Message types
    public static final String MSG_GET_CHUNK = "GET_CHUNK";
    public static final String MSG_CHUNK_DATA = "CHUNK_DATA";
    public static final String MSG_CHUNK_LIST = "CHUNK_LIST";
    public static final String MSG_PEER_LIST = "PEER_LIST";
    public static final String MSG_REGISTER = "REGISTER";

    // Response codes
    public static final int RESPONSE_OK = 200;
    public static final int RESPONSE_NOT_FOUND = 404;
    public static final int RESPONSE_ERROR = 500;
}