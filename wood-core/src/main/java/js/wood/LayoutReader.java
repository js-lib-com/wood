package js.wood;

import static java.lang.String.format;

import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LayoutReader extends Reader {
	private final char[] buffer;

	private final Reader reader;

	private final IProcessor processor;

	public LayoutReader(Reader reader, FilePath sourceFile) {
		this(reader, sourceFile, new char[512]);
	}

	public LayoutReader(Reader reader, FilePath sourceFile, char[] buffer) {
		super();
		this.buffer = buffer;
		this.reader = reader;

		Project project = sourceFile.getProject();
		IExpectedOperator expectedOperator;
		switch (project.getOperatorsNaming()) {
		case ATTR:
			expectedOperator = this::expectedAttrOperator;
			break;

		case DATA_ATTR:
			expectedOperator = this::expectedDataAttrOperator;
			break;

		case XMLNS:
			expectedOperator = this::expectedXmlnsOperator;
			break;

		default:
			throw new IllegalStateException();
		}

		this.processor = new Processor(project.getCustomElementsRegistry(), expectedOperator);
	}

	private String expectedXmlnsOperator(ICustomElement customElement) {
		return format("%s:%s=\"%s\"", processor.namespace(), customElement.operator(), customElement.compoPath());
	}

	private String expectedAttrOperator(ICustomElement customElement) {
		return format("%s=\"%s\"", customElement.operator(), customElement.compoPath());
	}

	private String expectedDataAttrOperator(ICustomElement customElement) {
		return format("data-%s=\"%s\"", customElement.operator(), customElement.compoPath());
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		if (processor.isEmpty()) {
			int charsCount = reader.read(buffer);
			if (charsCount <= 0) {
				return -1;
			}
			processor.batch(buffer, charsCount);
		}

		return processor.getData(cbuf, off, len);
	}

	@Override
	public void close() throws IOException {
		reader.close();
	}

	// --------------------------------------------------------------------------------------------

	/**
	 * Function that return WOOD operator suitable for a component based on W3C custom element. Returned operator syntax depends
	 * on project operators naming strategy, see {@link Project#getOperatorsNaming()}.
	 * 
	 * @author Iulian Rotaru
	 */
	@FunctionalInterface
	private interface IExpectedOperator {
		String apply(ICustomElement customElement);
	}

	private static class LayoutParseException extends WoodException {
		private static final long serialVersionUID = 9021325135746245337L;

		public LayoutParseException(String message, Object... args) {
			super(message, args);
		}
	}

	private interface IProcessor {

		String namespace();

		void batch(char[] buffer, int len);

		int getData(char[] cbuf, int off, int len);

		boolean isEmpty();

	}

	private static class Processor implements IProcessor {

		private static final Pattern XMLNS = Pattern.compile(String.format("xmlns:(\\w+)=[\"']%s[\"']", WOOD.NS));

		private final ICustomElementsRegistry customElements;

		private final IExpectedOperator expectedOperator;

		/**
		 * Buffer with characters read from source file. Its initial length can be anything between couple chars to full
		 * capacity and is most probably refilled multiple times from source file.
		 * 
		 * Source buffer is mutable. If processor discovers custom element with missing WOOD operator, processor inserts on the
		 * fly the operator altering source buffer content.
		 */
		private final StringBuilder sourceBuffer = new StringBuilder();

		/**
		 * Buffer for opening tag of the currently processing HTML element. This buffer contains tag name and attributes, if
		 * any. This buffer is persisted between source buffer reads.
		 */
		private final StringBuilder elementBuffer = new StringBuilder();
		/**
		 * Buffer for tag name of the currently processing HTML element. This buffer content is preserved between source buffer
		 * reads.
		 */
		private final StringBuilder tagNameBuffer = new StringBuilder();

		/**
		 * Flag true if tag name is a custom element. By W3C convention custom element name has a hyphen that is detected when
		 * tag name buffer is filled.
		 */
		private boolean customElementTagName;

		/**
		 * Optional name space loaded from source file or null if not declared. This field has meaning only for XMLNS operators.
		 */
		private String namespace;

		/** Parser finite state machine. */
		private State state;

		public Processor(ICustomElementsRegistry customElements, IExpectedOperator expectedOperator) {
			this.customElements = customElements;
			this.expectedOperator = expectedOperator;
			this.state = State.WAIT_ROOT;
		}

		@Override
		public String namespace() {
			return namespace;
		}

		@Override
		public void batch(char[] cbuf, int len) {
			sourceBuffer.setLength(0);
			sourceBuffer.append(cbuf, 0, len);

			for (int i = 0; i < sourceBuffer.length(); ++i) {
				int insertedCharsCount = parse(i, sourceBuffer.charAt(i));
				i += insertedCharsCount;
			}
		}

		@Override
		public int getData(char[] cbuf, int off, int len) {
			if (len > sourceBuffer.length()) {
				len = sourceBuffer.length();
			}
			sourceBuffer.getChars(0, len, cbuf, off);
			sourceBuffer.delete(0, len);
			return len;
		}

		@Override
		public boolean isEmpty() {
			return sourceBuffer.length() == 0;
		}

		/**
		 * Parse for custom elements and insert WOOD operator, if missing. This parser read characters from source buffer, see
		 * {@link #sourceBuffer} inserting WOOD operators on the fly, altering source buffer. Returns the number of characters
		 * inserted and zero if source buffer is not changed.
		 * 
		 * @param position character position in source buffer,
		 * @param c source buffer current character.
		 * @return the number of characters inserted or zero if source buffer is not changed.
		 */
		private int parse(int position, char c) {
			switch (state) {
			// ------------------------------------------------------------------------------------
			// state machine for name space detection

			case WAIT_ROOT:
				if (c != '<') {
					throw new LayoutParseException("Missing root element.");
				}
				state = State.PROLOG;
				break;

			case PROLOG:
				if (c == '?') {
					state = State.WAIT_ROOT;
					break;
				}
				elementBuffer.setLength(0);
				state = State.ROOT;
				// fall through ROOT case

			case ROOT:
				if (c == '>') {
					Matcher matcher = XMLNS.matcher(elementBuffer.toString());
					if (matcher.find()) {
						namespace = matcher.group(1);
					}
					state = State.WAIT_START_TAG;
					break;
				}
				elementBuffer.append(c);
				break;

			// ------------------------------------------------------------------------------------
			// state machine for custom elements processing

			case WAIT_START_TAG:
				if (c == '<') {
					state = State.CHECK_END_TAG;
					customElementTagName = false;
					elementBuffer.setLength(0);
					tagNameBuffer.setLength(0);
				}
				break;

			case CHECK_END_TAG:
				// XML syntax does not allow space between < and / in end tag
				if (c == '/') {
					state = State.WAIT_START_TAG;
					break;
				}
				state = State.TAG_NAME;
				// fall through next TAG case

			case TAG_NAME:
				if (Character.isWhitespace(c)) {
					state = State.ATTRIBUTES;
					break;
				}
				if (c == '-') {
					// by W3C convention custom element tag name has hyphen; built in tags has no hyphen
					customElementTagName = true;
				}
				if (c == '/' || c == '>') {
					state = State.WAIT_START_TAG;
					if (customElementTagName) {
						return inserOperator(position);
					}
					break;
				}
				tagNameBuffer.append(c);
				elementBuffer.append(c);
				break;

			case ATTRIBUTES:
				switch (c) {
				case '\'':
					state = State.SINGLE_QUOTE;
					break;

				case '"':
					state = State.DOUBLE_QUOTE;
					break;

				case '/':
				case '>':
					state = State.WAIT_START_TAG;
					if (customElementTagName && !elementBuffer.toString().contains(expectedOperator(tagNameBuffer.toString()))) {
						return inserOperator(position);
					}
					break;
				}
				elementBuffer.append(c);
				break;

			case SINGLE_QUOTE:
				if (c == '\'') {
					state = State.ATTRIBUTES;
				}
				elementBuffer.append(c);
				break;

			case DOUBLE_QUOTE:
				if (c == '"') {
					state = State.ATTRIBUTES;
				}
				elementBuffer.append(c);
				break;
			}

			return 0;
		}

		private String expectedOperator(String tagName) {
			ICustomElement customElement = customElements.getByTagName(tagName);
			if (customElement == null) {
				throw new LayoutParseException("Custom element |%s| not registered.", tagName);
			}
			return expectedOperator.apply(customElement);
		}

		private int inserOperator(int position) {
			String operator = " " + expectedOperator(tagNameBuffer.toString());
			sourceBuffer.insert(position, operator);
			return operator.length();
		}

		private enum State {
			/** Expect / wait for open angular brace (<) delimiting start-tag of the root element. */
			WAIT_ROOT,
			/** XML prolog discovered, continue waiting for root element. */
			PROLOG,
			/** Root element is scanned for optional name space declaration. */
			ROOT,

			/** Wait for open angular brace (<) delimiting a start-tag for a new XHTML element. */
			WAIT_START_TAG,
			/** Previous character was a open angular brace and check if this is slash (/) for end-tag marking (</). */
			CHECK_END_TAG,
			/** Tag name is collecting till white space or start-tag end. */
			TAG_NAME,
			/** Start-tag attributes are collecting. */
			ATTRIBUTES,
			/**
			 * Inside single quotes while collecting attributes. Wait for single quote character (') to go back to
			 * {@link #ATTRIBUTES} state.
			 */
			SINGLE_QUOTE,
			/**
			 * Inside double quotes while collecting attributes. Wait for double quote character (") to go back to
			 * {@link #ATTRIBUTES} state.
			 */
			DOUBLE_QUOTE
		}
	}
}
