package com.jslib.wood.dom;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Document object builder. Supply factory methods for documents creation, parsing from string and loading from various
 * sources: file, reader, input stream, input source and URL. There are different factory methods for XML and HTML
 * documents and all are in two flavors: with or without name space support. For name space support this class follows
 * W3C DOM notation convention and uses <code>NS</code> suffix.
 *
 * @author Iulian Rotaru
 */
public interface DocumentBuilder {
    /**
     * Create empty XML document with requested root element. Created document is not name space aware and uses UTF-8 for
     * character encoding.
     *
     * @param root tag name of the root element, null or empty not accepted.
     * @return newly created XML document.
     */
    Document createXML(String root);

    /**
     * Parse XML document form source string. Returned document is not name space aware and uses UTF-8 encoding.
     *
     * @param string non-empty source string.
     * @return newly created document.
     * @throws SAXException if source string is not valid XML document.
     */
    Document parseXML(String string) throws SAXException;

    /**
     * Parse XML document from source string with support for namespaces. Returned XML document is namespace aware and
     * uses UTF-8 encoding.
     *
     * @param string non-empty source string.
     * @return newly created document instance.
     * @throws SAXException if source string is not valid XML document.
     */
    Document parseXMLNS(String string) throws SAXException;

    /**
     * Load XML document from input stream. Returned document is not name space aware and uses UTF-8 encoding. Input
     * stream is closed after document load.
     *
     * @param stream input stream.
     * @return newly created XML document.
     * @throws IOException  if input stream reading fails.
     * @throws SAXException if input stream content is not a valid XML document.
     */
    Document loadXML(InputStream stream) throws IOException, SAXException;

    /**
     * Load XML document with name spaces support from input stream. Returned XML document uses UTF-8 encoding. Input
     * stream is closed after document load.
     *
     * @param stream input stream,
     * @return newly created XML document.
     * @throws IOException  if input stream reading fails.
     * @throws SAXException if input stream content is not a valid XML document.
     */
    Document loadXMLNS(InputStream stream) throws IOException, SAXException;

    /**
     * Load XML document from reader. Returned document is not name space aware and uses UTF-8 encoding. Source reader is
     * closed after document load.
     *
     * @param reader source character stream.
     * @return newly created XML document.
     * @throws IOException  if character stream reading fails.
     * @throws SAXException if character stream content is not a valid XML document.
     */
    Document loadXML(Reader reader) throws IOException, SAXException;

    /**
     * Load XML document with name spaces support from reader. Returned XML document uses UTF-8 encoding. Source reader is
     * closed after document load.
     *
     * @param reader source character stream.
     * @return newly created XML document.
     * @throws IOException  if character stream reading fails.
     * @throws SAXException if character stream content is not a valid XML document.
     */
    Document loadXMLNS(Reader reader) throws IOException, SAXException;

    /**
     * Create empty HTML document.
     *
     * @return new created HTML document.
     */
    Document createHTML();

    /**
     * Parse HTML document from source string. Returned document is not namespace aware.
     *
     * @param string source string, null or empty not accepted.
     * @return newly created HTML document.
     * @throws SAXException if source string is not valid HTML document.
     */
    Document parseHTML(String string) throws SAXException;

    /**
     * Load HTML document from source file using UTF-8 encoding. Returned document has not support for name space.
     *
     * @param file not null, ordinary, source file.
     * @return newly created HTML document.
     * @throws IOException  if source file not found or reading fails.
     * @throws SAXException if source file is not a valid HTML document.
     */
    Document loadHTML(File file) throws IOException, SAXException;

    /**
     * Load HTML document from bytes stream using UTF-8 character set. Returned document is not name space aware. Input
     * stream is closed after document load.
     *
     * @param stream input bytes stream.
     * @return newly created HTML document.
     * @throws IllegalArgumentException if <code>stream</code> is null.
     * @throws SAXException
     * @throws IOException
     */
    Document loadHTML(InputStream stream) throws IOException, SAXException;

    /**
     * Load HTML document from bytes stream using specified character set. Input stream is closed after document load.
     *
     * @param stream   input bytes stream,
     * @param encoding character set used to parse input stream.
     * @return newly created HTML document.
     * @throws IllegalArgumentException if <code>stream</code> is null.
     * @throws IllegalArgumentException if <code>encoding</code> is null or empty.
     * @throws SAXException
     * @throws IOException
     */
    Document loadHTML(InputStream stream, String encoding) throws IOException, SAXException;

    static DocumentBuilder getInstance() {
        return DocumentBuilderImpl.getInstance();
    }
}
