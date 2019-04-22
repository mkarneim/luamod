package net.wizardsoflua.lua.extension;

import static java.util.Objects.requireNonNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.ServiceConfigurationError;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.common.base.Charsets;

public class ServiceLoader {
  private static final String PREFIX = "META-INF/services/";
  private static final Logger LOGGER = LogManager.getLogger();

  public static <S> Set<Class<? extends S>> load(Class<S> service) {
    // The context class loader seems to not be set correctly by forge in eclipse
    // ClassLoader cl = Thread.currentThread().getContextClassLoader();
    ClassLoader cl = ServiceLoader.class.getClassLoader();
    return load(service, cl);
  }

  public static <S> Set<Class<? extends S>> load(Class<S> service, ClassLoader classLoader) {
    requireNonNull(service, "service == null!");
    requireNonNull(classLoader, "classLoader == null!");
    LOGGER
        .debug("Searching for services implementing the " + service.getSimpleName() + " interface");
    Set<Class<? extends S>> result = new HashSet<>();
    String name = PREFIX + service.getName();
    try {
      Enumeration<URL> resources = classLoader.getResources(name);
      while (resources.hasMoreElements()) {
        URL resource = resources.nextElement();
        result.addAll(parse(resource, service, classLoader));
      }
    } catch (IOException ex) {
      throw fail(service, "Error reading configuration file", ex);
    }
    LOGGER.debug("Found " + result.size() + " services: "
        + result.stream().map(s -> s.getName()).collect(Collectors.joining(", ")));
    return result;
  }

  private static <S> Set<Class<? extends S>> parse(URL resource, Class<S> service,
      ClassLoader classLoader) throws ServiceConfigurationError {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(resource.openStream(), Charsets.UTF_8))) {
      Set<Class<? extends S>> result = new HashSet<>();
      String line;
      while ((line = reader.readLine()) != null) {
        int ci = line.indexOf('#');
        if (ci >= 0) {
          line = line.substring(0, ci);
        }
        String providerName = line.trim();
        if (!providerName.isEmpty()) {
          Class<?> cls;
          try {
            cls = Class.forName(providerName, false, classLoader);
          } catch (ClassNotFoundException ex) {
            throw fail(service, "Provider " + providerName + " not found", ex);
          }
          Class<? extends S> provider;
          try {
            provider = cls.asSubclass(service);
          } catch (ClassCastException ex) {
            throw fail(service, "Provider " + providerName + " not a subtype", ex);
          }
          result.add(provider);
        }
      }
      return result;
    } catch (IOException ex) {
      throw fail(service, "Error reading configuration file", ex);
    }
  }

  private static ServiceConfigurationError fail(Class<?> service, String message, Throwable t) {
    throw new ServiceConfigurationError(service.getName() + ": " + message, t);
  }
}
