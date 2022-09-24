package com.jslib.wood;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.jslib.io.ReaderInputStream;
import com.jslib.util.Strings;
import com.jslib.wood.impl.ReferencesResolver;

/**
 * Variables hold name/value pairs that can be referenced from source files. In addition to having a name, variable belongs to a
 * type; so to fully refer a variable one need to know both type and name. See {@link ResourceType} for recognized types and
 * {@link Reference} for syntax and sample usage.
 * <p>
 * Variables are resources and are processed the same way as media files: by {@link IReferenceHandler}. When source file is
 * read, {@link SourceReader} discovers references and delegates reference handler. There are distinct reference handler
 * instances for build and preview processes but basically variables references are text replaces by their values; this class
 * provides getters just for that - see {@link #get(Reference, FilePath, IReferenceHandler)} and
 * {@link #get(Locale, Reference, FilePath, IReferenceHandler)}.
 * <p>
 * A variables has a scope; its name is private to a component. Is legal for variables from different component to have the same
 * name. Anyway, asset variables are global. Value retrieving logic from reference handler attempts first to get value from
 * component variables and only if value miss tries asset variables. Also, when locale is requested, attempt first to retrieve
 * that locale and if not found uses default.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class Variables {
	/**
	 * Resource references resolver for this variable values. A variable value may contain references to nested variables,
	 * creating a references tree.
	 */
	private final ReferencesResolver referenceResolver;

	/**
	 * Variable values mapped to locale. Resources without locale variant are identified by null locale. Null locale values are
	 * used when project is not multi-locale. Also used when a value for a given locale is missing.
	 */
	private final Map<Locale, Map<Reference, String>> localeValues = new HashMap<>();

	/** XML stream parser for files containing variables definition. */
	private final SAXParser saxParser;

	/**
	 * Stack for references nesting level trace, used for circular dependencies detection. It is global per execution thread.
	 * Nesting level trace logic assume references tree iteration occurs in a single thread.
	 */
	private static final ThreadLocal<Stack<String>> levelTraceTLS = new ThreadLocal<>();

	/**
	 * Create empty variables instance.
	 * 
	 * @throws WoodException if SAXA parser initialization fails.
	 */
	public Variables() {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			this.saxParser = factory.newSAXParser();
			this.referenceResolver = new ReferencesResolver();
		} catch (Exception e) {
			throw new WoodException(e);
		}
	}

	/**
	 * Create variables instance and load its values from provided directory files. All XML files from directory are scanned,
	 * except component descriptor; by convention descriptor has the same basename as directory.
	 * <p>
	 * If given <code>dirPath</code> parameter is not an existing directory or has no variables definition files, this
	 * constructor does not load values and resulting variables instance is empty.
	 * 
	 * @param dir directory to scan for variables definition files.
	 * @throws WoodException if SAXA parser initialization fails.
	 * @see #loadDir(FilePath)
	 */
	public Variables(FilePath dir) {
		this();
		loadDir(dir);
	}

	/**
	 * Reload variables definition from provided directory files. All XML files from directory are scanned, except component
	 * descriptor; by convention descriptor has the same basename as directory.
	 * <p>
	 * If given <code>dirPath</code> parameter is not an existing directory or has no variables definition files, this method
	 * just clean-up this variables instance values.
	 * 
	 * @param dir directory to scan for variables definition files.
	 * @see #loadDir(FilePath)
	 */
	public void reload(FilePath dir) {
		localeValues.clear();
		loadDir(dir);
	}

	/**
	 * Load variable values from given directory files. Traverses directory files, in no particular order, parsing variables
	 * definition. It is legal for directory to contains XML files that are not variables definition. For that files SAX parsing
	 * is aborted eagerly - see {@link #_load(FilePath)}.
	 * <p>
	 * Note that only direct child files are parsed. Also, if given directory does not exist this method does nothing.
	 * 
	 * @param dir directory path.
	 * @throws WoodException if file reading or parsing fails.
	 */
	private void loadDir(FilePath dir) {
		for (FilePath file : dir) {
			if (file.isVariables()) {
				try {
					_load(file);
				} catch (IOException | SAXException e) {
					throw new WoodException(e);
				}
			}
		}
	}

	/**
	 * Load variable values form variables definition file. This method just delegates {@link #_load(FilePath)} re-throwing
	 * exceptions as library runtime exception.
	 * 
	 * @param file file to load values.
	 * @throws WoodException if file reading or parsing fails.
	 */
	public void load(FilePath file) throws WoodException {
		try {
			_load(file);
		} catch (Exception e) {
			throw new WoodException(e);
		}
	}

	/**
	 * Load variable values from a variables definition file. Values are stored in a dictionary using locale as key - see
	 * {@link #localeValues}. Key locale are retrieved from given file variants; if file is not localized uses project default
	 * locale.
	 * <p>
	 * Variables definition files are XML files and this worker method uses {@link #saxParser} with a new {@link SAXHandler}
	 * instance to perform the actual parsing.
	 * <p>
	 * Variables definition file is a standard XML with root element one of the {@link ResourceType#variables()} values. It is
	 * legal that provided <code>file</code> parameter to not point to a variables definition file. If this is the case SAX
	 * parsing is aborted eagerly by {@ SAXHandler}; this way SAX parsing is also used to detect if file is valid variables
	 * definition.
	 * 
	 * @param file existing variables definition file.
	 * @throws IOException if file reading fails.
	 * @throws SAXException if XML parsing fails.
	 */
	private void _load(FilePath file) throws IOException, SAXException {
		Locale locale = file.getVariants().getLocale();
		// at this point locale can be null for files without locale variant
		Map<Reference, String> values = localeValues.get(locale);
		if (values == null) {
			values = new HashMap<Reference, String>();
		}

		SAXHandler saxHandler = new SAXHandler(file, values);
		try (InputStream stream = new ReaderInputStream(file.getReader())) {
			saxParser.parse(stream, saxHandler);
			localeValues.put(locale, values);
		} catch (NoVariablesDefinitionException unused) {
			// is a legal condition to have XML files that are not variables definition
		}
	}

	// --------------------------------------------------------------------------------------------
	// Variable value retrieving with circular dependencies detection

	/**
	 * Handy alternative for {@link #get(String, Reference, FilePath, IReferenceHandler)} when locale variant is not used.
	 * Returns null if value not found.
	 * 
	 * @param reference variable reference,
	 * @param source source file where reference is declared,
	 * @param listener resource references handler.
	 * @return variable value for requested reference or null if not found.
	 * @throws WoodException if variable value not found.
	 */
	public String get(Reference reference, FilePath source, IReferenceHandler listener) throws WoodException {
		return get(null, reference, source, listener);
	}

	/**
	 * Get variable value for requested locale, taking care to resolve nested references. A nested reference is one used by a
	 * variable definition, e.g.
	 * 
	 * <pre>
	 * <description>This {@literal}string/app description.</description>
	 * </pre>
	 * 
	 * If <code>locale</code> parameter is null uses project default one; default locale is that loaded from files without
	 * explicit locale variant.
	 * <p>
	 * This method attempts to retrieve variable value using next heuristic:
	 * <ol>
	 * <li>attempt to get value from this variables instance, for requested locale,
	 * <li>if not found try to get value for default locale,
	 * <li>if still value not found or if value is empty return null,
	 * <li>if value found attempt to resolve nested references; guard resolver invocation against circular dependencies
	 * </ol>
	 * <p>
	 * Is legal for a variable value to contain nested references. This method takes care to normalize returned value, that is,
	 * it invokes {@link ReferencesResolver} with found value. There is a recursive chain of methods invoked till references
	 * tree is completely resolved. See {@link SourceReader} for a discussion on references tree iteration.
	 * 
	 * @param locale optional locale, null for default,
	 * @param reference variable reference,
	 * @param source source file where reference is declared,
	 * @param handler resource references handler.
	 * @return variable value or null if variable not defined or if is empty.
	 * @throws WoodException if circular dependency is detected on references resolver.
	 */
	public String get(Locale locale, Reference reference, FilePath source, IReferenceHandler handler) throws WoodException {
		String value = null;

		// 1. attempt to get value from this variables instance, for requested locale
		Map<Reference, String> values = localeValues.get(locale);
		if (values != null) {
			value = values.get(reference);
		}

		// 2. if not found try to get value for default locale
		if (value == null) {
			values = localeValues.get(null);
			if (values != null) {
				value = values.get(reference);
			}
		}

		// 3. if still value not found or if value is empty return null
		if (value == null || value.isEmpty()) {
			return null;
		}

		// 4. if value found attempt to resolve nested references; guard resolver invocation against circular dependencies
		Stack<String> levelTrace = levelTrace();
		String trace = String.format("%s:%s", source, reference);
		if (levelTrace.contains(trace)) {
			StringBuilder builder = new StringBuilder("Circular variable references. Trace stack follows:\n");
			for (int i = 0; i < levelTrace.size(); ++i) {
				builder.append(Strings.concat("\t- ", levelTrace.get(i), "\n"));
			}
			throw new WoodException(builder.toString());
		}
		levelTrace.push(trace);

		// resolve nested references; see resolver API
		value = referenceResolver.parse(value, source, handler);
		levelTrace.pop();
		return value;
	}

	/**
	 * Retrieve nesting level trace stack for references resolver, in current thread. Create stack instance on the fly, if
	 * missing.
	 * 
	 * @return nesting level trace stack.
	 */
	private static Stack<String> levelTrace() {
		Stack<String> levelTrace = levelTraceTLS.get();
		if (levelTrace == null) {
			levelTrace = new Stack<String>();
			levelTraceTLS.set(levelTrace);
		}
		return levelTrace;
	}

	// --------------------------------------------------------------------------------------------
	// Internal types

	/**
	 * Scanner for variable values definition file. Variables values are stored into XML files processed by a SAX parser. This
	 * scanner actually implement SAX parser handler.
	 * <p>
	 * See below sample file for expected format. {@link ResourceType} is the root of the XML document. A resources file can
	 * contain only one resource type; this is by design to promote clear types separation. Direct child on root is the
	 * reference name, that is, the name used by source file to refer the variable - see {@link Reference} for syntax.
	 * Everything inside reference element is considered value and is copied as plain text. If reference element has nested
	 * children - e.g. {@link ResourceType#TEXT}, they are processed like a XML fragment and copied with element start/end tags
	 * included. Anyway, the actual value reading from SAX stream is delegated to {@link ValueBuilder}.
	 * 
	 * <pre>
	 *  &lt;type&gt;
	 *      . . .   
	 *      &lt;reference&gt;value&lt;/reference&gt;
	 *      . . .
	 *  &lt;/type&gt;
	 *  
	 *  &lt;body&gt;
	 *      &lt;h1&gt;@type/reference&lt;/h1&gt;
	 *      . . .
	 *  &lt;/body&gt;
	 * </pre>
	 * 
	 * In above example there is a variables definition file and a sample reference from a layout file. After reference
	 * processing <code>value</code> from XML file will be inserted into layout file as text for <code>h1</code> element.
	 * <p>
	 * This SAX handler has mutable internal state and is not thread safe.
	 * 
	 * @author Iulian Rotaru
	 */
	private static class SAXHandler extends DefaultHandler {
		/** Keep values definition file for error tracking. */
		private final FilePath sourceFile;

		/** Variable values storage mapped to related reference. This storage instance is created externally. */
		private final Map<Reference, String> values;

		/**
		 * Variable values definition file has a reference type used to create reference instances. Note that by design all
		 * variables from a file should have the same type.
		 */
		private Reference.Type referenceType;

		/**
		 * Value builder used to actually collect currently processed variable value. There are specialized value builders for
		 * different resource types. Builder instance is reused for entire variables definition file.
		 */
		private ValueBuilder builder;

		/**
		 * Current element nesting level acts as state for finite state automaton controlling this scanner behavior. Level value
		 * start with 0, is incremented on every element start and decremented on element end. This way it keeps track of
		 * nesting level.
		 */
		private int level;

		/**
		 * Create scanner instance that store discovered variable into given variables storage.
		 * 
		 * @param file the path for variables definition file, for error tracking,
		 * @param values external storage for variable values.
		 */
		public SAXHandler(FilePath file, Map<Reference, String> values) {
			super();
			this.sourceFile = file;
			this.values = values;
		}

		/**
		 * Takes care to reset nesting level for every new document.
		 */
		@Override
		public void startDocument() throws SAXException {
			level = 0;
		}

		/**
		 * Handle new element discovered into SAX stream. When detect root element this method initialized
		 * {@link #referenceType} and create value builder instance, see {@link #builder}. For every element direct child to
		 * root, that is, nesting level 1, reset the builder and add parameters from element attributes, if any. For the other
		 * deeper descendants just invoke {@link ValueBuilder#startTag(String)} with element name.
		 * 
		 * @param uri unused namespace URI,
		 * @param localName local name unused because namespace is not used,
		 * @param qName element qualified name,
		 * @param attributes attributes attached to element, possible empty.
		 */
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			switch (level) {
			case 0:
				referenceType = Reference.Type.getValueOf(qName);
				if (referenceType == null) {
					throw new NoVariablesDefinitionException();
				}
				builder = ValueBuilder.instance(referenceType);
				break;

			case 1:
				builder.reset();
				break;

			default:
				if (referenceType != Reference.Type.TEXT) {
					throw new WoodException("Not allowed nested element |%s| in  file |%s|. Only text variables support nested elements.", qName, sourceFile);
				}
				builder.startTag(qName);
			}
			++level;
		}

		/**
		 * Handle element closing tag. For root direct child elements stores value from {@link #builder} to {@link #values
		 * values storage}. For deeper descendants just invoke {@link ValueBuilder#endTag(String)}.
		 * 
		 * @param uri unused namespace URI,
		 * @param localName local name unused because namespace is not used,
		 * @param qName element qualified name.
		 */
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			--level;
			switch (level) {
			case 0:
				break;

			case 1:
				values.put(new Reference(sourceFile, referenceType, qName), builder.toString());
				break;

			default:
				builder.endTag(qName);
			}
		}

		/**
		 * Send text stream to value builder.
		 * 
		 * @param ch buffer of characters from SAX stream,
		 * @param start buffer offset,
		 * @param length buffer capacity.
		 */
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (level > 1) {
				builder.addValue(ch, start, length);
			}
		}
	}

	/**
	 * Exception thrown by {@link SAXHandler} if detects that XML file root is not valid for a variables definition file. A
	 * variable definition file is a XML file with root element one of {@link ResourceType#variables()} values.
	 * 
	 * @author Iulian Rotaru
	 * @since 1.0
	 */
	private static class NoVariablesDefinitionException extends SAXException {
		private static final long serialVersionUID = 6320065817993955976L;
	}

	/**
	 * Base class for variable value builders. Value builders are used in conjunction with XML SAX handler, see
	 * {@link SAXHandler}. A value builder instance is created on root element from variables definition file, considering
	 * {@link ResourceType} encoded by root - see {@link #instance(ResourceType)}. Builder instance is reused for all variable
	 * elements from file; there is {@link #reset()} method that prepare instance for new value. Once variable element
	 * discovered, value builder helps collecting variable value from XML stream via {@link #addValue(char[], int, int)} method.
	 * <p>
	 * This class deals with plain string only. There is specialized value builder for formatted text, see
	 * {@link TextValueBuilder}. Also, this base class defines hooks methods for subclasses benefit:
	 * {@link #addParameter(String, String)}, {@link #startTag(String)} and {@link #endTag(String)}.
	 * 
	 * @author Iulian Rotaru
	 * @since 1.0
	 */
	private static class ValueBuilder {
		/** Value builder storage. */
		protected final StringBuilder value = new StringBuilder();

		/**
		 * Prepare builder instance for new value assembling.
		 */
		public void reset() {
			value.setLength(0);
		}

		/**
		 * Add value characters from buffer.
		 * 
		 * @param buffer XML stream characters buffer,
		 * @param offset buffer offset,
		 * @param length the number of characters to process.
		 */
		public void addValue(char[] buffer, int offset, int length) {
			value.append(buffer, offset, length);
		}

		/**
		 * Called by variables scanner when a new variable element is discovered.
		 * 
		 * @param name variable element name.
		 * @throws UnsupportedOperationException base class does not process this hook.
		 */
		public void startTag(String name) throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}

		/**
		 * Called by variables scanner when a variable element closing tag is discovered.
		 * 
		 * @param name variable element name.
		 * @throws UnsupportedOperationException base class does not process this hook.
		 */
		public void endTag(String name) throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}

		/**
		 * Get value as string.
		 * 
		 * @return builder value.
		 * @see #value
		 */
		@Override
		public String toString() {
			return value.toString();
		}

		/**
		 * Create value builder instance suitable for resource type.
		 * 
		 * @param referenceType resource type.
		 * @return value builder instance for resource type.
		 */
		public static ValueBuilder instance(Reference.Type referenceType) {
			switch (referenceType) {
			case TEXT:
				return new TextValueBuilder();

			default:
				return new ValueBuilder();
			}
		}
	}

	/**
	 * Value builder for {@link ResourceType#TEXT} variables. This builder collect formatted text that is in essence a HTML
	 * fragment. Takes care to collect, beside text characters, start and end tags for formatting elements.
	 * 
	 * @author Iulian Rotaru
	 * @since 1.0
	 */
	private static class TextValueBuilder extends ValueBuilder {
		/**
		 * Add opening tag for formatting element.
		 * 
		 * @param name element tag name.
		 */
		@Override
		public void startTag(String name) {
			value.append('<');
			value.append(name);
			value.append('>');
		}

		/**
		 * Add closing tag for formatting element.
		 * 
		 * @param name element tag name.
		 */
		@Override
		public void endTag(String name) {
			value.append("</");
			value.append(name);
			value.append('>');
		}
	}

	// --------------------------------------------------------------------------------------------
	// Test support

	Map<Locale, Map<Reference, String>> getLocaleValues() {
		return localeValues;
	}

}
