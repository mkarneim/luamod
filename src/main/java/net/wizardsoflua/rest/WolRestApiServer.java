package net.wizardsoflua.rest;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.freeutils.httpserver.HTTPServer;
import net.freeutils.httpserver.HTTPServer.Request;
import net.freeutils.httpserver.HTTPServer.Response;
import net.freeutils.httpserver.HTTPServer.VirtualHost;
import net.wizardsoflua.WizardsOfLua;
import net.wizardsoflua.config.RestApiConfig;
import net.wizardsoflua.file.LuaFile;
import net.wizardsoflua.file.SpellPack;

public class WolRestApiServer {

  private static final String LOGIN_COOKIE_KEY_PREFIX = "__HOST-Login-";

  public interface Context {
    LuaFile getLuaFileByReference(String fileref);

    RestApiConfig getRestApiConfig();

    void saveLuaFileByReference(String fileref, String content);

    boolean isValidLoginToken(UUID uuid, String token);

    SpellPack createSpellPackByReference(String fileref);
  }

  private final Context context;
  private final Logger logger;
  private HTTPServer server;

  public WolRestApiServer(Context context) {
    this.context = checkNotNull(context, "context==null!");
    this.logger = WizardsOfLua.instance.logger;
  }

  public void start() throws IOException {
    logger.debug("[REST] starting WoL REST service");
    File contextRoot = context.getRestApiConfig().getWebDir();
    int port = context.getRestApiConfig().getPort();
    boolean secure = context.getRestApiConfig().isSecure();
    String hostname = context.getRestApiConfig().getHostname();
    String protocol = context.getRestApiConfig().getProtocol();
    final String keystore = context.getRestApiConfig().getKeyStore();
    if (secure) {
      checkNotNull(keystore,
          "Missing keystore! Please configure path to keystore file in WoL config!");
    }
    final char[] keystorePassword = context.getRestApiConfig().getKeyStorePassword();
    final char[] keyPassword = context.getRestApiConfig().getKeyPassword();

    createEmptyContextRoot(contextRoot);

    logger.info("[REST] REST service will cache static files at " + contextRoot.getAbsolutePath());

    server = new HTTPServer(port);
    server.setServerSocketFactory(
        secure ? createSSLServerSocketFactory(keystore, keystorePassword, keyPassword)
            : ServerSocketFactory.getDefault());

    VirtualHost host = new VirtualHost(hostname);
    server.addVirtualHost(host);

    StaticResourceHandlers staticResourceHandlers =
        new StaticResourceHandlers("/www", contextRoot, "/");
    host.addContext("/", staticResourceHandlers);
    host.addContexts(new RestHandlers(staticResourceHandlers));

    logger.info("Starting REST service at " + protocol + "://" + hostname + ":" + port);
    server.start();
  }

  public void stop() {
    if (server != null) {
      server.stop();
    }
  }

  private SSLServerSocketFactory createSSLServerSocketFactory(String keystore,
      char[] keystorePassword, char[] keyPassword) {
    try {
      SSLContext sslContext = SSLContext.getInstance("TLS");
      KeyStore ks = KeyStore.getInstance("JKS");
      try (InputStream in = new FileInputStream(new File(keystore))) {
        ks.load(in, keystorePassword);
      }
      KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
      kmf.init(ks, keyPassword);
      sslContext.init(kmf.getKeyManagers(), null, null);

      return sslContext.getServerSocketFactory();
    } catch (UnrecoverableKeyException | KeyManagementException | NoSuchAlgorithmException
        | KeyStoreException | CertificateException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void createEmptyContextRoot(File contextRoot) throws IOException {
    Path dir = contextRoot.toPath();
    if (Files.isDirectory(dir)) {
      deleteDir(dir);
    }
    Files.createDirectories(dir);
  }

  private void deleteDir(Path dirPath) throws IOException {
    Files.walk(dirPath) //
        .map(Path::toFile) //
        .sorted(Comparator.comparing(File::isDirectory)) //
        .forEach(File::delete);
  }

  class LuaFileJson {
    public final String path;
    public final String name;
    public final String reference;
    public final String content;

    public LuaFileJson(String path, String name, String reference, String content) {
      this.path = path;
      this.name = name;
      this.reference = reference;
      this.content = content;
    }
  }

  class FileJson {
    public final String path;
    public final String name;
    public final String type;
    public final List<FileJson> files;

    public FileJson(String path, String name, String type, List<FileJson> files) {
      this.path = path;
      this.name = name;
      this.type = type;
      this.files = files;
    }
  }

  class StaticResourceHandlers implements HTTPServer.ContextHandler {

    private final String resourcePath;
    private final File contextRoot;
    // private final HTTPServer.FileContextHandler cacheDirHandler;

    public StaticResourceHandlers(String resourcePath, File contextRoot, String context)
        throws IOException {
      this.resourcePath = resourcePath;
      this.contextRoot = contextRoot;
      // this.cacheDirHandler = new HTTPServer.FileContextHandler(contextRoot);
    }

    @Override
    public int serve(Request req, Response resp) throws IOException {
      String path = req.getPath();
      if (!path.startsWith("/")) {
        return 404;
      }
      Path cachedFilePath = Paths.get(contextRoot.getAbsolutePath(), path);
      if (!Files.exists(cachedFilePath)) {
        String resource = resourcePath + path;
        URL url = WolRestApiServer.class.getResource(resource);
        if (url == null) {
          WizardsOfLua.instance.logger.warn("WolRestServer couldn't find resource at " + resource);
          return 404;
        }
        File targetFile = new File(contextRoot, path);
        if (!targetFile.getParentFile().exists()) {
          Files.createDirectories(targetFile.getParentFile().toPath());
        }
        IOUtils.copy(url.openStream(), new FileOutputStream(targetFile));
      }
      // return cacheDirHandler.serve(req, resp);
      return HTTPServer.serveFile(contextRoot, "", req, resp);
    }
  }

  class RestHandlers {

    private final StaticResourceHandlers staticResourceHandlers;

    public RestHandlers(StaticResourceHandlers staticResourceHandlers) {
      this.staticResourceHandlers = staticResourceHandlers;
    }

    @HTTPServer.Context(value = "/wol/login", methods = {"GET"})
    public int login(Request req, Response resp) throws IOException {
      try {
        logger.debug("[REST] " + req.getMethod() + " " + req.getPath());
        if (isValidLoginToken(req)) {
          String loginToken = getLoginToken(req);
          UUID playerUuid = getPlayerUuid(req);
          addLoginCookie(resp, new LoginCookie(playerUuid, loginToken));
          return sendLoginSucceeded(req, resp);
        } else {
          deleteLoginCookie(req, resp);
          return sendLoginFailed(req, resp);
        }
      } catch (Exception e) {
        logger.error("Error handling GET: " + req.getPath(), e);
        resp.sendError(500, "Couldn't process GET method! e=" + e.getMessage()
            + "\n See fml-server-latest.log for more info!");
        return 0;
      }
    }

    @HTTPServer.Context(value = "/wol/lua", methods = {"GET"})
    public int get(Request req, Response resp) throws IOException {
      try {
        logger.debug("[REST] " + req.getMethod() + " " + req.getPath());
        LoginCookie loginCookie = getLoginCookie(req);
        if (loginCookie == null || !isValidLoginCookie(loginCookie)) {
          resp.sendError(401,
              "Missing correct login token! Please login first by executing '/wol browser login' in Minecraft.");
          return 0;
        }
        if (!isAuthorized(loginCookie.getPlayerUuid(), req.getPath())) {
          resp.sendError(401, "Not authorized for resource!");
          return 0;
        }
        if (acceptsJson(req)) {
          return sendLuaFile(req, resp);
        } else if (acceptsHtml(req)) {
          addLoginCookie(resp, loginCookie);
          return sendEditor(req, resp);
        } else {
          String accepts = req.getHeaders().get("Accept");
          resp.sendError(503,
              "Only html and json are supported, but your requests only accepts " + accepts);
          return 0;
        }
      } catch (Exception e) {
        logger.error("Error handling GET: " + req.getPath(), e);
        resp.sendError(500, "Couldn't process GET method! e=" + e.getMessage()
            + "\n See fml-server-latest.log for more info!");
        return 0;
      }
    }

    @HTTPServer.Context(value = "/wol/lua", methods = {"POST"})
    public int post(Request req, Response resp) throws IOException {
      try {
        logger.debug("[REST] " + req.getMethod() + " " + req.getPath());

        LoginCookie loginCookie = getLoginCookie(req);
        if (loginCookie == null || !isValidLoginCookie(loginCookie)) {
          resp.sendError(401,
              "Missing correct login token! Please login first by executing '/wol browser login' in Minecraft.");
          return 0;
        }
        if (!isAuthorized(loginCookie.getPlayerUuid(), req.getPath())) {
          resp.sendError(401, "Not authorized for resource!");
          return 0;
        }

        JsonParser parser = new JsonParser();
        String body = IOUtils.toString(req.getBody(), StandardCharsets.UTF_8.name());
        JsonObject root = parser.parse(body).getAsJsonObject();
        Pattern pattern = Pattern.compile("/wol/lua/(.+)");
        Matcher matcher = pattern.matcher(req.getPath());
        if (matcher.matches()) {
          String fileref = matcher.group(1);
          context.saveLuaFileByReference(fileref, root.get("content").getAsString());
          resp.send(200, "OK");
          return 0;
        } else {
          throw new RuntimeException("Unexpected path: '" + req.getPath() + "'");
        }
      } catch (Exception e) {
        logger.error("Error handling POST: " + req.getPath(), e);
        resp.sendError(500, "Couldn't process POST method! e=" + e.getMessage()
            + "\n See fml-server-latest.log for more info!");
        return 0;
      }
    }

    @HTTPServer.Context(value = "/wol/export", methods = {"GET"})
    public int export(Request req, Response resp) throws IOException {
      try {
        logger.debug("[REST] " + req.getMethod() + " " + req.getPath());
        LoginCookie loginCookie = getLoginCookie(req);
        if (loginCookie == null || !isValidLoginCookie(loginCookie)) {
          resp.sendError(401,
              "Missing correct login token! Please login first by executing '/wol browser login' in Minecraft.");
          return 0;
        }
        if (true || acceptsJarFile(req)) {
          addLoginCookie(resp, loginCookie);
          return sendSpellPack(req, resp);
        } else {
          String accepts = req.getHeaders().get("Accept");
          resp.sendError(503,
              "Only java-archive is supported, but your requests only accepts " + accepts);
          return 0;
        }
      } catch (Exception e) {
        logger.error("Error handling GET: " + req.getPath(), e);
        resp.sendError(500, "Couldn't process GET method! e=" + e.getMessage()
            + "\n See fml-server-latest.log for more info!");
        return 0;
      }
    }

    private int sendLuaFile(Request req, Response resp) throws IOException {
      Pattern pattern = Pattern.compile("/wol/lua/(.+)");
      Matcher matcher = pattern.matcher(req.getPath());
      if (matcher.matches()) {
        String fileref = matcher.group(1);
        LuaFile luaFile = context.getLuaFileByReference(fileref);
        LuaFileJson luaFileJson = new LuaFileJson(luaFile.getPath(), luaFile.getName(),
            luaFile.getFileReference(), luaFile.getContent());
        Gson gson = new Gson();
        String content = gson.toJson(luaFileJson);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8.name());
        InputStream body = new ByteArrayInputStream(bytes);
        long[] range = null;
        resp.sendHeaders(200, bytes.length, -1L, null, "application/json", range);
        resp.sendBody(body, bytes.length, null);
        return 0;
      } else {
        throw new RuntimeException("Unexpected path: '" + req.getPath() + "'");
      }
    }

    private int sendSpellPack(Request req, Response resp) throws IOException {
      Pattern pattern = Pattern.compile("/wol/export/(.+)");
      Matcher matcher = pattern.matcher(req.getPath());
      if (matcher.matches()) {
        String fileref = matcher.group(1);
        SpellPack spellPack = context.createSpellPackByReference(fileref);
        InputStream body = spellPack.open();
        long[] range = null;
        resp.sendHeaders(200, spellPack.getSize(), -1L, null, "application/java-archive", range);
        resp.sendBody(body, spellPack.getSize(), null);
        return 0;
      } else {
        throw new RuntimeException("Unexpected path: '" + req.getPath() + "'");
      }
    }

    private int sendEditor(Request req, Response resp) throws IOException {
      req.setPath("/editor.html");
      return staticResourceHandlers.serve(req, resp);
    }

    private int sendLoginSucceeded(Request req, Response resp) throws IOException {
      req.setPath("/loginSucceeded.html");
      return staticResourceHandlers.serve(req, resp);
    }

    private int sendLoginFailed(Request req, Response resp) throws IOException {
      req.setPath("/loginFailed.html");
      return staticResourceHandlers.serve(req, resp);
    }

    private boolean acceptsJson(Request req) throws IOException {
      return acceptsMimetype(req, Sets.newHashSet("application/json", "json"));
    }

    private boolean acceptsHtml(Request req) {
      return acceptsMimetype(req, Sets.newHashSet("text/html", "html"));
    }

    private boolean acceptsJarFile(Request req) {
      return acceptsMimetype(req, Sets.newHashSet("application/java-archive", "java-archive"));
    }

    private boolean acceptsMimetype(Request req, Set<String> expectedMt) {
      String accept = req.getHeaders().get("Accept");
      for (String actualMt : accept.split(",")) {
        if (expectedMt.contains(actualMt.trim().toLowerCase())) {
          return true;
        }
      }
      return false;
    }

  }

  private boolean isValidLoginToken(Request req) {
    Pattern pattern = Pattern.compile("/wol/login/([^/]+)/([^/]+)");
    Matcher matcher = pattern.matcher(req.getPath());
    if (matcher.matches()) {
      String playerUuidStr = matcher.group(1);
      String token = matcher.group(2);
      return context.isValidLoginToken(UUID.fromString(playerUuidStr), token);
    }
    return false;
  }

  public boolean isAuthorized(UUID playerUuid, String path) {
    if (path.startsWith("/wol/lua/shared/")) {
      return true;
    }
    Pattern pattern = Pattern.compile("/wol/lua/([^/]+)/.*");
    Matcher matcher = pattern.matcher(path);
    if (matcher.matches()) {
      String uuidStr = matcher.group(1);
      UUID uuid = UUID.fromString(uuidStr);
      return playerUuid.equals(uuid);
    } else {
      throw new IllegalArgumentException("Request does not match pattern! req.path=" + path);
    }
  }

  public UUID getPlayerUuid(Request req) {
    Pattern pattern = Pattern.compile("/wol/login/([^/]+)/([^/]+)");
    Matcher matcher = pattern.matcher(req.getPath());
    if (matcher.matches()) {
      String uuid = matcher.group(1);
      return UUID.fromString(uuid);
    } else {
      throw new IllegalArgumentException(
          "Request does not match pattern! req.path=" + req.getPath());
    }
  }

  public String getLoginToken(Request req) {
    Pattern pattern = Pattern.compile("/wol/login/([^/]+)/([^/]+)");
    Matcher matcher = pattern.matcher(req.getPath());
    if (matcher.matches()) {
      String token = matcher.group(2);
      return token;
    } else {
      throw new IllegalArgumentException(
          "Request does not match pattern! req.path=" + req.getPath());
    }
  }

  public void addLoginCookie(Response resp, LoginCookie loginCookie) {
    addCookie(resp, getLoginCookieKey(), loginCookie.getPlayerUuid() + "/" + loginCookie.getToken(),
        3600 * 24 * 365);
  }

  public @Nullable LoginCookie getLoginCookie(Request req) {
    String text = req.getHeaders().get("Cookie");
    Map<String, String> entries = parseCookies(text);
    String loginCookieKey = getLoginCookieKey();
    String cookieValue = entries.get(loginCookieKey);
    if (cookieValue != null) {
      int index = cookieValue.indexOf('/');
      if (index != -1) {
        String playerUuidStr = cookieValue.substring(0, index);
        if (cookieValue.length() > index + 1) {
          String token = cookieValue.substring(index + 1);

          return new LoginCookie(UUID.fromString(playerUuidStr), token);
        }
      }
    }
    return null;
  }

  public UUID getPlayerUuidFromRequestPath(Request req) {
    Pattern pattern = Pattern.compile("/wol/[^/]+/([^/]+)/.+");
    Matcher matcher = pattern.matcher(req.getPath());
    if (matcher.matches()) {
      String playerUuidStr = matcher.group(1);
      return UUID.fromString(playerUuidStr);
    } else {
      throw new IllegalArgumentException(
          "Request does not match pattern! req.path=" + req.getPath());
    }
  }

  public boolean isValidLoginCookie(LoginCookie loginCookie) {
    String token = loginCookie.getToken();
    UUID uuid = loginCookie.getPlayerUuid();
    return context.isValidLoginToken(uuid, token);
  }

  public void deleteLoginCookie(Request req, Response resp) {
    addCookie(resp, getLoginCookieKey(), "---", 0);
  }

  private void addCookie(Response resp, String key, String value, int maxAge) {
    boolean secure = context.getRestApiConfig().isSecure();
    String secureFlag = secure ? " Secure;" : "";
    resp.getHeaders().add("Set-Cookie",
        String.format("%s=%s; Max-Age=%s;%s Path=/", key, value, maxAge, secureFlag));
  }

  private Map<String, String> parseCookies(String text) {
    Map<String, String> result = new HashMap<>();
    if (text != null && !text.trim().isEmpty()) {
      String[] tokens = text.split(";");
      for (String token : tokens) {
        int idx = token.indexOf('=');
        String key = token.substring(0, idx).trim();
        String value = token.substring(idx + 1).trim();
        result.put(key, value);
      }
    }
    return result;
  }

  private String getLoginCookieKey() {
    String hostname = context.getRestApiConfig().getHostname();
    int port = context.getRestApiConfig().getPort();
    String server = hostname + ":" + port;
    return LOGIN_COOKIE_KEY_PREFIX + server;
  }

}
