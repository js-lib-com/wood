package com.jslib.wood.dom;

import org.apache.html.dom.HTMLDocumentImpl;
import org.cyberneko.html.parsers.DOMParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static com.jslib.wood.util.StringsUtil.format;

/**
 * Document object builder. Supply factory methods for documents creation, parsing from string and loading from various
 * sources: file, input stream, input source and URL. There are different factory methods for XML and HTML documents and
 * all are in two flavors: with or without name space support. For name space support this class follows W3C DOM
 * notation convention and uses <code>NS</code> suffix.
 * <p>
 * All loaders use XML declaration or HTML meta Content-Type to choose characters encoding; anyway, loader variant using
 * input source can force a particular encoding.
 *
 * @author Iulian Rotaru
 */
final class DocumentBuilderImpl implements DocumentBuilder {
    private final static Logger log = LoggerFactory.getLogger(DocumentBuilderImpl.class);

    private static final DocumentBuilder instance = new DocumentBuilderImpl();

    public static DocumentBuilder getInstance() {
        log.trace("getInstance()");
        return instance;
    }

    /**
     * XML parser feature for name space support.
     */
    private static final String FEAT_NAMESPACES = "http://xml.org/sax/features/namespaces";
    /**
     * XML parser feature for schema validation.
     */
    private static final String FEAT_SCHEMA_VALIDATION = "http://apache.org/xml/features/validation/schema";

    private DocumentBuilderImpl() {
        log.trace("DocumentBuilderImpl()");
    }

    @Override
    public Document createXML(String root) {
        assert root != null && !root.isEmpty() : "Root element argument is null or empty";
        org.w3c.dom.Document doc = getDocumentBuilder(false).newDocument();
        doc.appendChild(doc.createElement(root));
        return new DocumentImpl(doc);
    }

    @Override
    public Document parseXML(String string) throws SAXException {
        assert string != null && !string.isEmpty() : "Source string argument is null or empty";
        try {
            return loadXML(new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8)));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("JVM with missing support for UTF-8");
        } catch (IOException e) {
            throw new IllegalStateException("IO exception while reading string");
        }
    }

    @Override
    public Document parseXMLNS(String string) throws SAXException {
        assert string != null && !string.isEmpty() : "Source string argument is null or empty";
        try {
            return loadXMLNS(new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8)));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("JVM with missing support for UTF-8");
        } catch (IOException e) {
            throw new IllegalStateException("IO exception while reading string");
        }
    }

    @Override
    public Document loadXML(InputStream stream) throws IOException, SAXException {
        assert stream != null : "Input stream argument is null";
        return loadXML(new InputSource(stream), false);
    }

    @Override
    public Document loadXMLNS(InputStream stream) throws IOException, SAXException {
        assert stream != null : "Input stream argument is null";
        return loadXML(new InputSource(stream), true);
    }

    @Override
    public Document loadXML(Reader reader) throws IOException, SAXException {
        assert reader != null : "Source reader argument is null";
        return loadXML(new InputSource(reader), false);
    }

    @Override
    public Document loadXMLNS(Reader reader) throws IOException, SAXException {
        assert reader != null : "Source reader argument is null";
        return loadXML(new InputSource(reader), true);
    }

    /**
     * Helper method to load XML document from input source.
     *
     * @param source       input source,
     * @param useNamespace flag to control name space awareness.
     * @return newly created XML document.
     * @throws IOException  input source reading fails.
     * @throws SAXException input source content is not a valid XML document.
     */
    private static Document loadXML(InputSource source, boolean useNamespace) throws IOException, SAXException {
        try {
            org.w3c.dom.Document doc = getDocumentBuilder(useNamespace).parse(source);
            return new DocumentImpl(doc);
        } finally {
            close(source);
        }
    }

    @Override
    public Document createHTML() {
        return new DocumentImpl(new HTMLDocumentImpl());
    }

    @Override
    public Document parseHTML(String string) throws SAXException {
        assert string != null && !string.isEmpty() : "Source string argument is null or empty";
        try {
            return loadHTML(new ByteArrayInputStream(string.getBytes()));
        } catch (IOException e) {
            throw new SAXException(e.getMessage());
        }
    }

    @Override
    public Document loadHTML(File file) throws IOException, SAXException {
        assert file != null : "Source file argument is null";
        assert !file.isDirectory() : format("Source file argument %s is a directory", file);
        return loadHTML(Files.newInputStream(file.toPath()));
    }

    @Override
    public Document loadHTML(InputStream stream) throws IOException, SAXException {
        assert stream != null : "Input stream argument is null";
        return loadHTML(new InputSource(stream), "UTF-8");
    }

    @Override
    public Document loadHTML(InputStream stream, String encoding) throws IOException, SAXException {
        assert stream != null : "Input stream argument is null";
        assert encoding != null && !encoding.isEmpty() : "Character encoding argument is null or empty";
        return loadHTML(new InputSource(stream), encoding);
    }

    private static Document loadHTML(InputSource source, String encoding) throws IOException, SAXException {
        assert source != null : "Source argument is null";
        assert encoding != null && !encoding.isEmpty() : "Character encoding argument is null or empty";
        source.setEncoding(encoding);
        try {
            return loadHTML(source);
        } finally {
            close(source);
        }
    }

    /**
     * Utility method for loading HTML document from input source.
     *
     * @param source input source,
     * @return newly created HTML document.
     * @throws IOException  if reading from input stream fails.
     * @throws SAXException if input source is not valid HTML.
     */
    private static Document loadHTML(InputSource source) throws IOException, SAXException {
        assert source != null : "Source argument is null";
        DOMParser parser = new DOMParser();
        // source http://nekohtml.sourceforge.net/faq.html#hierarchy
        parser.setFeature(FEAT_NAMESPACES, false);
        parser.parse(source);
        return new DocumentImpl(parser.getDocument());
    }

    /**
     * Close input source.
     *
     * @param source input source to be closed.
     * @throws IOException if input source closing fails.
     */
    private static void close(InputSource source) throws IOException {
        if (source != null) {
            if (source.getByteStream() != null) {
                close(source.getByteStream());
            }
            if (source.getCharacterStream() != null) {
                close(source.getCharacterStream());
            }
        }
    }

    /**
     * Close closeable converting IO exception to unchecked DOM exception.
     *
     * @param closeable closeable to close.
     * @throws IOException if closing operation fails.
     */
    private static void close(Closeable closeable) throws IOException {
        if (closeable != null) {
            closeable.close();
        }
    }

    /**
     * Document building error handler.
     *
     * @author Iulian Rotaru
     */
    static class ErrorHandlerImpl implements ErrorHandler {
        /**
         * Record parser fatal error to builder class logger.
         */
        public void fatalError(SAXParseException exception) {
            log.error("Fatal error on document building: {}: {}", exception.getClass(), exception.getMessage(), exception);
        }

        /**
         * Record parser error to builder class logger.
         */
        public void error(SAXParseException exception) {
            log.error("Error on document building: {}: {}", exception.getClass(), exception.getMessage(), exception);
        }

        /**
         * Record parser warning to builder class logger.
         */
        public void warning(SAXParseException exception) {
            log.error("Warning on document building: {}: {}", exception.getClass(), exception.getMessage(), exception);
        }
    }

    /**
     * Get XML document builder.
     *
     * @param useNamespace flag to use name space.
     * @return XML document builder.
     */
    private static javax.xml.parsers.DocumentBuilder getDocumentBuilder(boolean useNamespace) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setIgnoringComments(true);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setCoalescing(true);

        try {
            // disable parser XML schema support; it is enabled by default
            dbf.setFeature(FEAT_SCHEMA_VALIDATION, false);
            dbf.setValidating(false);
            dbf.setNamespaceAware(useNamespace);

            javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
            db.setEntityResolver(new EntityResolverImpl());
            db.setErrorHandler(new ErrorHandlerImpl());
            return db;
        } catch (ParserConfigurationException e) {
            // document builder implementation does not support features used by this method
            throw new IllegalStateException(e);
        }
    }
}
