package no.hal.httpfile;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;

public interface InputStreamProvider {

    public InputStream getInputStream(String resource);

    public record Uri(URI baseUri) implements InputStreamProvider {

        public static InputStream getInputStream(URI baseUri, String resource) {
            try {
                return (baseUri != null ? baseUri.resolve(resource) : URI.create(resource)).toURL().openStream();
            } catch (IOException ex) {
                return null;
            }
        }

        @Override
        public InputStream getInputStream(String resource) {
            return getInputStream(baseUri, resource);
        }
    }

    public record Resource(Class<?> context) implements InputStreamProvider {

        public static InputStream getInputStream(Class<?> context, String resource) {
            return context.getResourceAsStream(resource);
        }

        @Override
        public InputStream getInputStream(String resource) {
            return getInputStream(context, resource);
        }
    }

    public record File(Path basePath) implements InputStreamProvider {

        public File(String basePath) {
            this(Path.of(basePath));
        }

        public static InputStream getInputStream(Path basePath, String resource) {
            try {
                return new FileInputStream(basePath.resolve(resource).toFile());
            } catch (IOException ex) {
                return null;
            }
        }

        @Override
        public InputStream getInputStream(String resource) {
            try {
                return new FileInputStream(basePath.resolve(resource).toFile());
            } catch (FileNotFoundException e) {
                return null;
            }
        }
    }

    public class Default implements InputStreamProvider {

        @Override
        public InputStream getInputStream(String resource) {
            if (resource.indexOf(':') >= 4) {
                return Uri.getInputStream(null, resource);
            } else if (resource.startsWith("/")) {
                return Resource.getInputStream(getClass(), resource);
            } else {
                return File.getInputStream(Path.of("./"), resource);
            }
        }
    }
}