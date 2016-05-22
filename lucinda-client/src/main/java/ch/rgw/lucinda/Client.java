package ch.rgw.lucinda;

import com.hazelcast.config.Config;
import com.hazelcast.config.NetworkConfig;
import io.netty.handler.codec.http.HttpRequest;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Client library for Lucinda
 * Created by gerry on 10.05.16.
 */
public class Client {
    private String api="/api/1.0/";
    private Vertx vertx;
    private String prefix;
    private boolean connected = false;
    private boolean preferREST = false;
    private Handler messageHandler;
    private Logger log = Logger.getLogger("Lucinda Client");
    private HttpClient http;

    /**
     * Connect to a lucinda server
     *
     * @param prefix  The message prefix to use. This must macth the prefix of the desired server. If in doubt, use 'null'
     * @param netmask The Network to use, e.g. 192.168.0.*
     * @param handler The handler to call for lucinda related messages
     */
    public void connect(final String prefix, final String netmask, final Handler handler) {
        this.prefix = prefix;
        if (prefix == null) {
            this.prefix = "ch.rgw.lucinda";
        }
        this.messageHandler = handler;
        VertxOptions vertxOptions = new VertxOptions().setClustered(true).setMaxEventLoopExecuteTime(5000000000L).setBlockedThreadCheckInterval(3000);
        String ip = Util.matchIP(netmask);
        log.info("trying IP " + ip);
        if (!ip.isEmpty()) {
            Config hazel = new Config();
            NetworkConfig nc = hazel.getNetworkConfig();
            nc.getInterfaces().setEnabled(true).addInterface(ip);
            nc.setPublicAddress(ip);
            vertxOptions.setClusterHost(ip);
            vertxOptions.setClusterManager(new HazelcastClusterManager(hazel));
        }
        try {
            Vertx.clusteredVertx(vertxOptions, result -> {
                if (result.succeeded()) {
                    log.info("Connect succeded");
                    vertx = result.result();
                    vertx.eventBus().send(this.prefix + ".ping", new JsonObject(), msg -> {
                        if (msg.succeeded()) {
                            vertx.eventBus().consumer(this.prefix + ".error", err -> {
                                JsonObject errmsg = (JsonObject) err.body();
                                messageHandler.signal(errmsg.getMap());
                            });
                            connected = true;
                            JsonObject pong=(JsonObject)msg.result().body();
                            String server_ip=pong.getString("pong");
                            String api_version=pong.getString("rest");
                            if(api_version!=null) {
                                api="/api/"+api_version+"/";
                                HttpClientOptions hop = new HttpClientOptions().setDefaultHost(server_ip)
                                        .setDefaultPort(Integer.parseInt(pong.getString("port"))).setTryUseCompression(true)
                                        .setKeepAlive(true).setIdleTimeout(60);
                                http=vertx.createHttpClient(hop);
                                HttpClientRequest htr=http.request(HttpMethod.GET,api+"ping", response ->{
                                    response.bodyHandler(buffer -> {
                                        if(buffer.toString().equals("pong")){
                                            messageHandler.signal(make("status:REST ok"));
                                            log.info("Rest API ok");
                                            preferREST=true;
                                        }
                                    });
                                }).setTimeout(2000L).exceptionHandler(exception -> {
                                    log.severe("REST failure "+exception.getMessage());
                                    exception.printStackTrace();
                                    handler.signal(make("status:failure","message:"+exception.getMessage()));
                                });
                                htr.end();
                            }
                            messageHandler.signal(make("status:connected"));
                        } else {
                            log.warning("ping failed");
                            messageHandler.signal(make("status:failure", "message:" + "ping fail " + msg.cause().getMessage()));
                        }
                    });


                } else {
                    log.warning("connect failed ");
                    messageHandler.signal(make("status:error", "message:" + "connect fail " + result.cause().getMessage()));
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();

        }
    }

    /**
     * Search the index
     *
     * @param queryPhrase What to search
     * @param handler
     */
    public void query(final String queryPhrase, final Handler handler) {
        if (!connected) {
            handler.signal(make("status:error", "message:no connection"));
        } else {
            if(preferREST){
                http.post(api+"query", response -> {
                    if(response.statusCode()==200) {
                        response.bodyHandler(buffer -> {
                            JsonObject result = buffer.toJsonObject();
                            handler.signal(result.getMap());
                        });
                    }else{
                        handler.signal(make("status:error","message:"+response.statusMessage()));
                    }
                }).write(queryPhrase).end();
            }else {
                vertx.eventBus().send(prefix + ".find", new JsonObject().put("query", queryPhrase), result -> {
                    if (result.succeeded()) {
                        JsonObject found = (JsonObject) result.result().body();
                        handler.signal(found.getMap());
                    } else {
                        handler.signal(make("status:error", "message:" + result.cause().getMessage()));
                    }
                });
            }
        }
    }

    /**
     * retrieve a document from its id
     *
     * @param id      uinique id of the document
     * @param handler
     */
    public void get(final String id, final Handler handler) {
        if (!connected) {
            handler.signal(make("status:error", "message:no connection"));
        } else {
            if(preferREST){
                   http.getNow(api+"get/"+id, response -> {
                       if(response.statusCode()==200){
                        response.bodyHandler( buffer -> {
                            byte[] file=buffer.getBytes();
                            handler.signal(add(make("status:ok"), "result", file));
                        });
                       }else if(response.statusCode()==404){
                           handler.signal(make("status:not found"));
                       }else{
                           handler.signal(make("status:error","message:"+response.statusMessage()));
                       }
                   });
            }else {
                vertx.eventBus().send(prefix + ".get", new JsonObject().put("_id", id), result -> {
                    if (result.succeeded()) {
                        JsonObject found = (JsonObject) result.result().body();
                        handler.signal(add(make("status:ok"), "result", found.getBinary("result")));
                    } else {
                        handler.signal(make("status:error", "message:" + result.cause().getMessage()));
                    }
                });
            }
        }
    }

    /**
     * Add a document to the index. Note: The document itself will not be stored, only parsed and added to the index.
     * The caller must handle it by itself and make sure, that it can retrieve the document with the given id
     *
     * @param id       unique id for the document. The caller should be able to retrieve or reconstruct the document later with
     *                 this id
     * @param title    A title for the document.
     * @param doctype a random document type (this is mot the mime-type but rather some application dependant organisational attribute)
     * @param metadata application efined metadata. These are stored with the index and can be queried for. Example: If there is
     *                 an attribute "author: john doe", a later query can search for "author: john*"
     * @param contents The file contents parse. Many file types are supported and recognized by content (not by file extension),
     *                 such as .odt, .doc, .pdf, tif. Image files are parsed throug OCR and any foiund text is indexed
     * @param handler  Handler to call after indexing
     */
    public void addToIndex(final String id, final String title, final String doctype, Map<String, Object> metadata, final byte[] contents, final Handler handler) {

        if (!connected) {
            handler.signal(make("status:error", "message:no connection"));
        } else {
            try {
                JsonObject envelope = prepare(id, title, doctype, metadata, contents);
                if(preferREST){
                    http.post(api+"index", response -> {
                        if(response.statusCode()==200){
                            handler.signal(make("status:ok"));
                        }else{
                            handler.signal(make("status:error","message:"+response.statusMessage()));
                        }
                    });
                }else {

                    vertx.eventBus().send(prefix + ".index", envelope, result -> {
                        if (result.succeeded()) {
                            handler.signal(make("status:ok"));
                        } else {
                            handler.signal(make("status:error", "message:" + result.cause().getMessage()));
                        }
                    });
                }

            } catch (IOException ex) {
                ex.printStackTrace();
                handler.signal(make("status:error", "message:" + ex.getMessage()));
            }
        }
    }

    /**
     * Add a file to the Lucinda store and index.
     * @param filename Name for the file to write (no path, only filename)
     * @param concern some grouping hint for the file (e.g. name of the group). The file will be stored in a subdirectory of that name.
     *                if concern is null, the base directory for imports is used.
     * @param doctype a random document type (this is mot the mime-type but rather some application dependant organisational attribute)
     * @param metadata application efined metadata. These are stored with the index and can be queried for. Example: If there is
     *                 an attribute "author: john doe", a later query can search for "author: john*"
     * @param contents The file contents parse. Many file types are supported and recognized by content (not by file extension),
     *                 such as .odt, .doc, .pdf, tif. Image files are parsed throug OCR and any foiund text is indexed
     * @param handler  Handler to call after the import
     */
    public void addFile(final String filename, final String concern, final String doctype, Map<String, Object> metadata, final byte[] contents, final Handler handler) {

        if (!connected) {
            handler.signal(make("status:error", "message:no connection"));
        } else {
            try{
                JsonObject envelope=prepare("",filename,doctype,metadata,contents);
                if(concern!=null) {
                    envelope.put("concern", concern);
                }
                if(preferREST){
                    http.post(api+"index", response -> {
                        if(response.statusCode()==201){
                            handler.signal(make("status:ok"));
                        }else{
                            handler.signal(make("status:error","message:"+response.statusMessage()));
                        }
                    });

                }else {
                    vertx.eventBus().send(prefix + ".import", envelope, result -> {
                        if (result.succeeded()) {
                            handler.signal(make("status:ok"));
                        } else {
                            handler.signal(make("status:error", "message:" + result.cause().getMessage()));
                        }
                    });
                }

            }catch(IOException ex){
                ex.printStackTrace();
                handler.signal(make("status:error", "message:" + ex.getMessage()));

            }
        }
    }

    private JsonObject prepare(final String id, final String title, final String doctype, Map<String, Object> metadata, final byte[] contents) throws IOException {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        if(!id.isEmpty()) {
            metadata.put("_id", id);
        }
        metadata.put("title", title);
        metadata.put("lucinda_doctype", doctype);
        metadata.put("filename",title);
        metadata.put("payload",contents);
        return new JsonObject(metadata);
    }

    public void shutDown() {
        connected = false;
        vertx.close();
    }

    /* syntactic sugar to create and initialize a Map with a single call */
    private Map<String, Object> make(String... params) {
        Map<String, Object> ret = new HashMap<>();
        for (String param : params) {
            String[] p = param.split(":");
            ret.put(p[0], p[1]);
        }
        return ret;
    }

    /* syntactic sugar to allow fluent api */
    private Map<String, Object> add(Map<String, Object> orig, String key, Object value) {
        orig.put(key, value);
        return orig;
    }
}
