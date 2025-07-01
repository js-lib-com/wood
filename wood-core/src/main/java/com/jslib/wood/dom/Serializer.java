package com.jslib.wood.dom;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Stack;

import com.jslib.wood.util.StringsUtil;
import org.apache.html.dom.HTMLDocumentImpl;
import org.w3c.dom.DocumentType;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Serialize document on external created writer. Serializer accepts both HTML and XML documents for which write the
 * right prolog. After prolog writes document nodes tree and flush the writer. Note that writer is leaved opened; is
 * caller responsibility to close it when done using it. This is because document serialization can be part of a larger
 * serialization process controller by caller.
 * <p>
 * This serializer implementation takes care of empty tags, default attributes and raw text nodes, i.e. not escaped
 * text.
 * <p>
 * This class rationale: Javax Transformer solutions uses short notation for empty elements like &lt;script ... /&gt;.
 * It seems there are browsers that refused to display pages with empty scripts or fail to display properly empty
 * <code>textarea</code>, if HTML code uses short notation for that tags.
 * 
 * @author Iulian Rotaru
 */
final class Serializer
{
  /** Serializer writer. */
  private BufferedWriter writer;

  /**
   * Is XML escape disabled. There are HTML tags, like <code>script</code>, that need to serialize their text nodes with
   * no escape.
   */
  private boolean noescape;
  
  private boolean xmlDeclaration = true;

  private int indentationLevel;

  private Stack<Short> nodeTypes = new Stack<>();

  /**
   * Create serializer instance using given writer for IO operations. This constructor takes care to use
   * {@link BufferedWriter}; if <code>writer</code> parameter is not already buffered create a new buffered instance.
   * 
   * @param writer writer to serialize to.
   */
  public Serializer(Writer writer)
  {
    this.writer = writer instanceof BufferedWriter ? (BufferedWriter)writer : new BufferedWriter(writer);
    this.noescape = false;
  }

  public void setXmlDeclaration(boolean xmlDeclaration)
  {
    this.xmlDeclaration = xmlDeclaration;
  }

  /**
   * Serialize document to the writer initialized by constructor. This method accept both XML and HTML documents and
   * write prolog accordingly: HTML document type, respective XML declaration. After prolog write nodes tree
   * recursively, invoking {@link #write(Node)} with document root.
   * <p>
   * When document nodes tree is complete flush the writer but does not close it.
   * 
   * @param doc document to serialize.
   * @throws IOException if write operation fails.
   */
  public void serialize(DocumentImpl doc) throws IOException
  {
    org.w3c.dom.Document w3cDoc = doc.getDocument();
    if(w3cDoc instanceof HTMLDocumentImpl) {
      // if w3cDoc has no document type default to html5: <!DOCTYPE html>
      writer.write("<!DOCTYPE html");
      DocumentType dt = w3cDoc.getDoctype();
      if(dt != null) {
        if(dt.getPublicId() != null) {
          writer.write(" PUBLIC \"");
          writer.write(dt.getPublicId());
          writer.write("\"");
        }
        if(dt.getSystemId() != null) {
          writer.write(" \"");
          writer.write(dt.getSystemId());
          writer.write("\"");
        }
      }
      writer.write(">");
      crlf();
    }
    else {
      // if not html write xml declaration
      if(xmlDeclaration) {
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        crlf();
      }
    }

    if(doc.getRoot() != null) {
      write(((ElementImpl)doc.getRoot()).getNode());
    }
    crlf();
    writer.flush();
  }

  /**
   * Write a node opening and closing tags and, recursively, child nodes in between. Text note content is escaped less
   * if there is an ancestor node declared as raw, see {@link HTML#RAW_TAGS}. Does not use short notation for end tag
   * unless node is explicitly declared as empty into {@link HTML#EMPTY_TAGS}. While serializing opening tag takes care
   * to serialize attributes to; anyway, if an attribute happens to have default value - see {@link HTML#DEFAULT_ATTRS},
   * skip it.
   * 
   * @param n node to serialize.
   * @throws IOException if write operation fails.
   */
  private void write(Node n) throws IOException
  {
    switch(n.getNodeType()) {
    case Node.TEXT_NODE:
      nodeTypes.push(Node.TEXT_NODE);
      if(noescape) {
        writer.write(n.getTextContent().trim());
      }
      else {
        StringsUtil.escapeXML(n.getTextContent().trim(), writer);
      }
      break;

    case Node.ELEMENT_NODE:
      nodeTypes.push(Node.ELEMENT_NODE);
      if(indentationLevel > 0) {
        crlf();
      }
      String tag = n.getNodeName();
      ++indentationLevel;
      indent();
      writer.write('<');
      writer.write(tag);

      NamedNodeMap attrs = n.getAttributes();
      for(int i = 0; i < attrs.getLength(); i++) {
        Node attr = attrs.item(i);
        String value = attr.getTextContent().trim();
        if(!value.equals(HTML.DEFAULT_ATTRS.get(attr.getNodeName()))) {
          writer.write(' ');
          writer.write(attr.getNodeName());
          writer.write('=');
          writer.write('"');
          StringsUtil.escapeXML(value, writer);
          writer.write('"');
        }
      }

      boolean emptyTag = HTML.EMPTY_TAGS.contains(tag.toLowerCase());
      if(emptyTag) {
        writer.write(' ');
        writer.write('/');
      }
      writer.write('>');
      if(emptyTag) {
        --indentationLevel;
        return;
      }

      noescape = HTML.RAW_TAGS.contains(tag.toLowerCase());
      NodeList children = n.getChildNodes();
      int childElementCount = 0;
      for(int i = 0; i < children.getLength(); i++) {
        if(children.item(i).getNodeType() == Node.ELEMENT_NODE) {
          ++childElementCount;
        }
        write(children.item(i));
      }
      noescape = false;

      if(childElementCount > 0 && nodeType() == Node.ELEMENT_NODE) {
        crlf();
        indent();
      }
      writer.write('<');
      writer.write('/');
      writer.write(tag);
      writer.write('>');

      --indentationLevel;
      break;
    }

    nodeTypes.pop();
  }

  private short nodeType()
  {
    if(nodeTypes.size() < 1) {
      return 0;
    }
    return nodeTypes.get(nodeTypes.size() - 1);
  }

  private void indent() throws IOException
  {
    for(int i = indentationLevel; i > 1; --i) {
      writer.write('\t');
    }
  }

  private void crlf() throws IOException
  {
    writer.write(System.lineSeparator());
  }
}
