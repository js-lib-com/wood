package com.jslib.wood;

import java.io.IOException;
import java.io.Reader;

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
        this.processor = new Processor(sourceFile);
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

    private static class LayoutParseException extends WoodException {
        private static final long serialVersionUID = 9021325135746245337L;

        public LayoutParseException(String message, Object... args) {
            super(message, args);
        }
    }

    private interface IProcessor {

        void batch(char[] buffer, int len);

        int getData(char[] cbuf, int off, int len);

        boolean isEmpty();

    }

    private static class Processor implements IProcessor {
        private final FilePath sourceFile;

        /**
         * Buffer with characters read from source file. Its initial length can be anything between couple chars to full
         * capacity and is most probably refilled multiple times from source file.
         * <p>
         * Source buffer is mutable. If processor discovers custom element with missing WOOD operator, processor inserts on the
         * fly the operator altering source buffer content.
         */
        private final StringBuilder sourceBuffer = new StringBuilder();

        /**
         * Parser finite state machine.
         */
        private State state;

        public Processor(FilePath sourceFile) {
            this.sourceFile = sourceFile;
            this.state = State.WAIT_ROOT;
        }

        @Override
        public void batch(char[] cbuf, int len) {
            sourceBuffer.setLength(0);
            sourceBuffer.append(cbuf, 0, len);

            for (int i = 0; i < sourceBuffer.length(); ++i) {
                int insertedCharsCount = parse(sourceBuffer.charAt(i));
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
         * @param c source buffer current character.
         * @return the number of characters inserted or zero if source buffer is not changed.
         */
        private int parse(char c) {
            switch (state) {
                // ------------------------------------------------------------------------------------
                // state machine for name space detection

                case WAIT_ROOT:
                    if (c != '<') {
                        throw new LayoutParseException("Missing root element from file %s.", sourceFile);
                    }
                    state = State.PROLOG;
                    break;

                case PROLOG:
                    if (c == '?') {
                        state = State.WAIT_ROOT;
                        break;
                    }
                    state = State.ROOT;
                    // fall through ROOT case

                case ROOT:
                    if (c == '>') {
                        state = State.WAIT_START_TAG;
                        break;
                    }
                    break;

                // ------------------------------------------------------------------------------------
                // state machine for custom elements processing

                case WAIT_START_TAG:
                    if (c == '<') {
                        state = State.CHECK_END_TAG;
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
                    if (c == '/' || c == '>') {
                        state = State.WAIT_START_TAG;
                        break;
                    }
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
                            break;
                    }
                    break;

                case SINGLE_QUOTE:
                    if (c == '\'') {
                        state = State.ATTRIBUTES;
                    }
                    break;

                case DOUBLE_QUOTE:
                    if (c == '"') {
                        state = State.ATTRIBUTES;
                    }
                    break;
            }

            return 0;
        }

        private enum State {
            /**
             * Expect / wait for open angular brace (<) delimiting start-tag of the root element.
             */
            WAIT_ROOT,
            /**
             * XML prolog discovered, continue waiting for root element.
             */
            PROLOG,
            /**
             * Root element is scanned for optional name space declaration.
             */
            ROOT,

            /**
             * Wait for open angular brace (<) delimiting a start-tag for a new XHTML element.
             */
            WAIT_START_TAG,
            /**
             * Previous character was an open angular brace and check if this is slash (/) for end-tag marking (</).
             */
            CHECK_END_TAG,
            /**
             * Tag name is collecting till white space or start-tag end.
             */
            TAG_NAME,
            /**
             * Start-tag attributes are collecting.
             */
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
