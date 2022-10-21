package com.jslib.wood;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import com.jslib.util.Params;
import com.jslib.wood.impl.LayoutParameters;
import com.jslib.wood.impl.ReferencesResolver;

/**
 * Source file reader with at-meta reference processing. This class is a decorator for a characters stream reader. Beside
 * standard reading it looks for at-meta references and invokes external {@link IReferenceHandler} when discover them. Also
 * inject layout parameters provided by {@link LayoutParameters} when encounter <code>@param</code> reference.
 * <p>
 * General at-meta reference syntax is described here.
 * 
 * <pre>
 * reference = AT type SEP ?(path SEP) name
 * path      = 1*CH           ; optional path, for resource files only
 * name      = 1*CH           ; reference name, unique in scope
 * ; type is defined in references description table
 * 
 * ; terminal symbols definition
 * AT  = "@"                 ; at-meta reference mark
 * SEP = "/"                 ; reference name and optional path separator
 * CH  = ALPHA / DIGIT / "-" ; character is US-ASCII alphanumeric and dash
 * 
 * ; ALPHA and DIGIT are described by RFC5234, Appendix B.1
 * </pre>
 * 
 * <h5>Variables Reference Processing</h5>
 * <p>
 * Source files can contain references to variables. Is legal for variable values to also contain references to variables; this
 * creates a tree of variable references that is traversed recursively in depth-first order. There are a number of methods
 * invoked in a chain that creates this recursive reference scanning.
 * <ol>
 * <li>{@link SourceReader} discovers a variable reference and delegates reference handler,
 * <li>{@link IReferenceHandler} retrieves value from variables instance,
 * <li>{@link Variables#get(String, Reference, FilePath, IReferenceHandler)} invokes value references resolver with found value,
 * <li>{@link ReferencesResolver} discovers a variable reference and delegates reference handler, back to 2,
 * <li>repeat 2..4 till entire values tree is resolved.
 * </ol>
 * Note that variables value getter, at point 3, implements recursive loop level guard.
 * 
 * <h5>Layout Parameters Processing</h5>
 * <p>
 * Component layouts can contain layout parameters defined by `@param/name` parameter reference syntax. For example a child
 * section title provided by parent component.
 * 
 * <pre>
 * <section>
 * 	<h1>@param/title</h1>
 * </section>
 * </pre>
 * <p>
 * When source reader discover a parameter reference uses {@link #layoutParameters} to retrieve named parameter value and text
 * replace parameter reference with its value. Layout parameters map is initialized beforehand and injected via constructor
 * {@link SourceReader#SourceReader(FilePath, LayoutParameters, IReferenceHandler)}.
 * <p>
 * Layout parameters map is initialized from <code>wood:param</code> operator at component creation.
 * 
 * <pre>
 * &lt;div wood:compo="compo/list-view" wood:param="caption:Users Info"&gt;&lt;/div&gt;
 * 
 * &lt;div wood:template="template/dialog#body" wood:param="name:user-edit;caption:Edit User;class:js.dialog.Dialog"&gt;&lt;/div&gt;
 * </pre>
 * 
 * As stated layout parameters are the operand for <code>wood:param</code> operator; see {@link LayoutParameters} for layout
 * parameters syntax.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class SourceReader extends Reader {
	/** Source file define the scope of resource references. */
	private final FilePath sourceFile;

	/** External defined reference handler in charge with resource references processing. */
	private final IReferenceHandler referenceHandler;

	/** External defined reader, decorated by this source reader instance. */
	private final Reader reader;

	/** Resource reference builder. */
	private final MetaBuilder metaBuilder;

	/** Optional layout parameters map, null if source is not a layout file. */
	private LayoutParameters layoutParameters;

	/** Current state determine how every character is processed. */
	private State state;

	/** Current variable value. */
	private String value;

	/** Current variable value index. */
	private int valueIndex;

	/** Store the character after at-meta, used to detect at-meta end. */
	private int charAfterMeta;

	/**
	 * Create source reader decorator for external defined reader. Source may contain resource references and expression
	 * evaluations that are declared into source file as at-meta references. For resources processing this constructor takes
	 * external defined resource references handler. For expression evaluations creates interpreter and references resolver
	 * instances.
	 * 
	 * @param sourceReader source file reader,
	 * @param sourceFile source file used as scope for resource references,
	 * @param referenceHandler external reference handler.
	 */
	public SourceReader(Reader sourceReader, FilePath sourceFile, IReferenceHandler referenceHandler) {
		super();
		Params.notNull(sourceReader, "Source reader");
		Params.notNull(sourceFile, "Source file");
		Params.isTrue(sourceFile.isSynthetic() || sourceFile.exists(), "Source file does not exist");
		Params.notNull(referenceHandler, "Reference handler");

		Reader reader = sourceFile.isLayout() ? new LayoutReader(sourceReader, sourceFile) : sourceReader;
		if (!(reader instanceof BufferedReader)) {
			reader = new BufferedReader(reader);
		}
		this.reader = reader;
		
		this.sourceFile = sourceFile;
		this.referenceHandler = referenceHandler;
		this.metaBuilder = new MetaBuilder(sourceFile);
		this.state = State.TEXT;
	}

	/**
	 * Convenient source reader constructor for a given source file. Create file reader for source file and delegates
	 * {@link SourceReader#SourceReader(Reader, FilePath, IReferenceHandler)}.
	 * 
	 * @param sourceFile source file to create source reader for,
	 * @param referenceHandler external defined reference handler.
	 */
	public SourceReader(FilePath sourceFile, IReferenceHandler referenceHandler) {
		this(reader(sourceFile), sourceFile, referenceHandler);
	}

	/**
	 * Construct source file reader for widget or template layout with parameters. For layout files without parameters
	 * <code>layoutParameters</code> argument is not null but its content is empty. Reference handler resolve values for
	 * variables and media files references.
	 * 
	 * @param sourceFile source file for widget layout,
	 * @param layoutParameters layout parameters, possible empty,
	 * @param referenceHandler external reference handler.
	 */
	public SourceReader(FilePath sourceFile, LayoutParameters layoutParameters, IReferenceHandler referenceHandler) {
		this(reader(sourceFile), sourceFile, referenceHandler);
		Params.isTrue(sourceFile.isLayout(), "Source file is not a widget or template layout");
		this.layoutParameters = layoutParameters;
	}

	/**
	 * Get a reader for the given source file throwing illegal argument if source file is null.
	 * 
	 * @param sourceFile source file path.
	 * @return source file reader.
	 */
	private static Reader reader(FilePath sourceFile) {
		Params.notNull(sourceFile, "Source file");
		return sourceFile.getReader();
	}

	public FilePath getSourceFile() {
		return sourceFile;
	}

	/**
	 * This method is not needed for source reader logic but is required by reader interface. It just fill the buffer, reading
	 * char by char.
	 * 
	 * @param buffer target characters buffer,
	 * @param offset buffer offset,
	 * @param length buffer length.
	 * @return the number of read characters.
	 */
	@Override
	public int read(char[] buffer, int offset, int length) throws IOException {
		int n = 0;
		for (int i = offset; i < length; ++i, ++n) {
			int c = read();
			if (c == -1) {
				return n > 0 ? n : c;
			}
			buffer[i] = (char) c;
		}
		return n;
	}

	/**
	 * Get character from decorated reader and process it accordingly current state value. On the fly updates the state.
	 * <p>
	 * If current reader position is outside at-meta reference this method just return the source text character. When discover
	 * at-meta mark, this method blocks, reading the at-meta content. Once at-meta reference parsed, delegates
	 * {@link #referenceHandler} to process at-meta reference that return a not null, not empty value.
	 * <p>
	 * At this point, this method start returning the value, char by char, instead of reading from decorated reader. When value
	 * is completely retrieved start processing again the source text characters.
	 * 
	 * @return source text or resource value character.
	 */
	@Override
	public int read() throws IOException {
		int c = -1;

		switch (state) {
		case TEXT:
			c = reader.read();
			if (c != Reference.MARK) {
				break;
			}
			state = State.AT_META;
			metaBuilder.reset();
			// fall through next REFERENCE state

		case AT_META:
			while (metaBuilder.add(c)) {
				c = reader.read();
			}

			Reference reference = metaBuilder.getReference();
			if (reference != null) {
				if (reference.getType() == Reference.Type.PARAM) {
					if (layoutParameters == null) {
						throw new WoodException("Found @param at-meta but missing layout parameters for source file |%s|.", sourceFile);
					}
					value = layoutParameters.getValue(sourceFile, reference.getName());
				} else {
					value = referenceHandler.onResourceReference(reference, sourceFile);
				}
			} else {
				// if built reference is not recognized sent it back to source stream unchanged since is valid to have reference
				// mark in source syntax, e.g. @media from CSS file
				value = metaBuilder.toString();
			}

			state = State.VALUE;
			charAfterMeta = c;
			valueIndex = 1;
			if (value == null) {
				throw new WoodException("Null value for at-meta reference |%s| in source file |%s|.", metaBuilder.toString(), sourceFile);
			}
			return value.charAt(0);

		case VALUE:
			if (valueIndex < value.length()) {
				return value.charAt(valueIndex++);
			}
			state = State.TEXT;
			c = charAfterMeta;
			break;
		}

		return c;
	}

	@Override
	public void close() throws IOException {
		reader.close();
	}

	// --------------------------------------------------------------------------------------------
	// Internal classes

	/**
	 * Source reader state machine.
	 * 
	 * @author Iulian Rotaru
	 * @since 1.0
	 */
	private static enum State {
		/** Source file text, outside reference. */
		TEXT,

		/** At-meta reference is parsing. */
		AT_META,

		/** At-meta reference is completely parsed and its value is returning, char by char. */
		VALUE
	}

	/**
	 * At-meta reference builder is enacted when source reader discovers at-meta mark. It collects at-meta reference characters
	 * till end mark or EOF, see {@link #add(int)}. Also detects escape sequence - double <code>at</code> character.
	 * 
	 * @author Iulian Rotaru
	 * @since 1.0
	 */
	private static class MetaBuilder {
		/** Escape sequence. */
		private static final String ESCAPE = "@@";

		private final FilePath sourceFile;

		private final StringBuilder builder;

		/** Store reference separator index for reference instance creation, see {@link #getReference()}. */
		private int separatorIndex;

		/** Flag true only is escape sequence (double <code>at</code> character) was discovered. */
		private boolean escape;

		public MetaBuilder(FilePath sourceFile) {
			super();
			this.sourceFile = sourceFile;
			this.builder = new StringBuilder();
		}

		public void reset() {
			builder.setLength(0);
			separatorIndex = 0;
		}

		/**
		 * Store reference character and return true if collecting is to be continuing. Returns false if reference end mark or
		 * end of file is detected. On the fly updates {@link #separatorIndex}.
		 * <p>
		 * Also return false if escape sequence (double <code>at</code> character) was discovered. In this case builder content
		 * is replaced with a single <code>at</code> character.
		 * <p>
		 * This method is designed for usage in a <code>while</code> loop, see sample code.
		 * 
		 * <pre>
		 * while (referenceBuilder.add(c)) {
		 * 	c = reader.read();
		 * }
		 * </pre>
		 * 
		 * @param c reference character to add.
		 * @return true if reference collecting is to be continuing.
		 */
		public boolean add(int c) {
			if (escape) {
				builder.replace(0, builder.length(), "@");
				return false;
			}
			if (c == -1 || !Reference.isChar(c)) {
				return false;
			}

			// detect separator for both resource reference and expression, that is, '/' or '('
			// if expression separator found initialize expression flag and expression nesting level to 1
			if (separatorIndex == 0) {
				switch (c) {
				case Reference.SEPARATOR:
					separatorIndex = builder.length();
					break;
				}
			}

			builder.append((char) c);
			if (ESCAPE.equals(builder.toString())) {
				escape = true;
			}
			return true;
		}

		/**
		 * Get at-meta reference instance. Create and return a new at-meta reference instance. Return null on at-meta escape or
		 * not recognized reference type; in this case internal builder still contains original source text.
		 * 
		 * @return newly created reference instance or null.
		 */
		public Reference getReference() {
			if (escape || separatorIndex == 0) {
				return null;
			}
			Reference.Type type = Reference.Type.getValueOf(builder.substring(1, separatorIndex));
			if (type == null) {
				return null;
			}
			String name = builder.substring(separatorIndex + 1);
			return new Reference(sourceFile, type, name);
		}

		@Override
		public String toString() {
			return builder.toString();
		}
	}
}
