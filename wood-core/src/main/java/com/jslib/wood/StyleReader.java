package com.jslib.wood;

import com.jslib.wood.impl.MediaQueries;
import com.jslib.wood.util.StringsUtil;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Style file reader adds style variants, as media queries sections, to given base style file. This class is used in conjunction
 * with {@link SourceReader}, see sample code below. Style reader appends media sections to style file and source reader
 * resolves at-meta references. Both tasks are processed on the fly, while style file content is reading.
 * <p>
 * Be it a component <code>res/page</code> with a base style file <code>res/page/page.css</code>. Also, component has two style
 * file variants, namely <code>res/page/page_w1200.css</code> and <code>res/page/page_w800.css</code>.
 *
 * <pre>
 * FilePath styleFile = project.getFile("res/page/page.css");
 * Files.copy(new SourceReader(new StyleReader(styleFile)), ...
 * </pre>
 * <p>
 * Resulting style file would be something like snippet below. First <code>body</code> rule set is from base style file
 * <code>res/page/page.css</code> whereas the other two are from style variants, <code>res/page/page_w1200.css</code> respective
 * <code>res/page/page_w800.css</code>. Please notice relation between file path variant and <code>max-width</code> expression
 * from media query.
 *
 * <pre>
 * body {
 *     width: 1000px;
 * }
 *
 * {@literal @}media screen and (max-width:1200px) {
 * body {
 *     width: 600px;
 * }
 * }
 *
 * {@literal @}media screen and (max-width:800px) {
 * body {
 *     width: 400px;
 * }
 * }
 * </pre>
 *
 * @author Iulian Rotaru
 */
public class StyleReader extends Reader {
    /**
     * Media expressions list mapped to style files, possible empty.
     */
    private final List<FilePath> variants;

    private final Iterator<FilePath> variantsIterator;

    /**
     * Currently processed style file reader, base style file or style variants.
     */
    private Reader reader;

    /**
     * Current processed source, media section header or footer.
     */
    private String source;

    /**
     * Currently processed source index.
     */
    private int sourceIndex;

    /**
     * Reader automaton current state.
     */
    private State state;

    /**
     * Remember last character from read buffer. Used to check, and correct, if base style file is not ended with new line.
     */
    private char lastProcessedChar;

    /**
     * Create reader instance for given style file. Style file parameter should be a base style file, that is, it should have no
     * variants.
     *
     * @param styleFile base style file.
     */
    public StyleReader(FilePath styleFile) {
        assert !styleFile.hasVariants() : "Style reader decorates a base style file and supplied file is a variant.";
        this.reader = styleFile.getReader();
        this.state = State.BASE_CONTENT;

        final String basename = styleFile.getBasename();
        FilePath parentDir = styleFile.getParentDir();
        this.variants = parentDir == null ? new ArrayList<>() : parentDir.filter(filePath -> filePath.getBasename().equals(basename) && filePath.getVariants().hasMediaQueries());

        variantsIterator = variants.iterator();
    }

    /**
     * Implementation for abstract {@link Reader#read(char[], int, int)} is also this style reader automaton implementation. The
     * main concern of this method is to read base style file content. After read completes switch state to variants processing,
     * of course if there are any. Read every variant content surrounded by media section header and footer.
     *
     * @param buffer target buffer,
     * @param offset target buffer offset,
     * @param length target buffer length.
     * @throws IOException if read operation fails.
     */
    @SuppressWarnings("incomplete-switch")
    @Override
    public int read(char[] buffer, int offset, int length) throws IOException {
        if (state == State.BASE_CONTENT) {
            int readCount = reader.read(buffer, offset, length);
            if (readCount != -1) {
                lastProcessedChar = buffer[readCount - 1];
                return readCount;
            }
            // end of stream (EOS) on base content reading
            state = State.NEXT_VARIANT;
            if (lastProcessedChar != '\n') {
                // inject line separator if base style file is not properly ended with new line
                char[] lineSeparator = System.lineSeparator().toCharArray();
                assert lineSeparator.length < length : "Line separator length smaller than style reader buffer size";
                System.arraycopy(lineSeparator, 0, buffer, offset, lineSeparator.length);
                return lineSeparator.length;
            }
        }

        int readCount = -1;
        VARIANTS_LOOP:
        for (; ; ) {
            switch (state) {
                case NEXT_VARIANT:
                    reader.close();
                    if (!variantsIterator.hasNext()) {
                        // if no more variants break for loop with readCount set to EOF
                        break VARIANTS_LOOP;
                    }
                    FilePath variantsEntry = variantsIterator.next();
                    // prepare next reader; current one was already closed so is safe to replace it wit a new one
                    reader = variantsEntry.getReader();

                    // prepare source and source index for variant header copy
                    // variants entry contains prepared media expressions ready to be inserted into media block header
                    MediaQueries queries = variantsEntry.getVariants().getMediaQueries();
                    source = HEADER(queries.getMedia(), queries.getExpression());
                    sourceIndex = 0;

                    // prepare next state and fall through
                    state = State.VARIANT_HEADER;

                case VARIANT_HEADER:
                    readCount = copy(buffer, offset, length);
                    if (readCount != -1) {
                        // keep the state till header end
                        break VARIANTS_LOOP;
                    }
                    // header is completely read; change state and fall to variant content processing
                    state = State.VARIANT_CONTENT;

                case VARIANT_CONTENT:
                    readCount = reader.read(buffer, offset, length);
                    if (readCount != -1) {
                        // keep the state till current style file reader end
                        break VARIANTS_LOOP;
                    }
                    // prepare source and source index for footer copy
                    source = FOOTER;
                    sourceIndex = 0;
                    // reader is completely read; move state to footer and fall through
                    state = State.VARIANT_FOOTER;

                case VARIANT_FOOTER:
                    readCount = copy(buffer, offset, length);
                    if (readCount != -1) {
                        // keep copying footer till its end
                        break VARIANTS_LOOP;
                    }
                    state = State.NEXT_VARIANT;
                    // goto NEXT_VARIANT state via for loop continue
            }
        }

        return readCount;
    }

    /**
     * Copy {@link #source} string characters to target buffer at given offset. Is legal that given buffer space to be smaller
     * that source string. This implies is possible this method to be invoked multiple times, for a given source. In order to
     * keep track of source characters position uses {@link #sourceIndex} that is properly initialized before first invocation.
     * <p>
     * Returns the number of characters actually copied or EOF on source end.
     *
     * @param buffer target buffer,
     * @param offset target buffer offset,
     * @param length target buffer space.
     * @return the number of characters processed or EOF on source end.
     */
    private int copy(char[] buffer, int offset, int length) {
        if (sourceIndex == source.length()) {
            return -1;
        }
        int readCount = 0;
        for (int i = offset; sourceIndex < source.length() && i < length; sourceIndex++, i++) {
            buffer[i] = source.charAt(sourceIndex);
            readCount++;
        }
        return readCount;
    }

    /**
     * Close style reader.
     */
    @Override
    public void close() throws IOException {
        // reading loop takes care to close the style file reader
    }

    /**
     * States list for style reader finite automaton.
     *
     * @author Iulian Rotaru
     * @since 1.0
     */
    private enum State {
        /**
         * Base style file content is in processing.
         */
        BASE_CONTENT,

        /**
         * Base style file finished to process. Use this style to add new line if source file is not properly ended
         * with new line.
         */
        BASE_CONTENT_END,

        /**
         * Select next variant, if any.
         */
        NEXT_VARIANT,

        /**
         * Copy header for current variant, selected on {@link #NEXT_VARIANT}.
         */
        VARIANT_HEADER,

        /**
         * Variant style file content is processed.
         */
        VARIANT_CONTENT,

        /**
         * Copy footer for current variant. On completion go back to {@link #NEXT_VARIANT}.
         */
        VARIANT_FOOTER
    }

    /**
     * Media section header.
     */
    private static String HEADER(String media, String expression) {
        assert media != null && !media.isEmpty() : "Media argument is null or empty";
        assert expression != null : "Expression argument is null";
        StringBuilder header = new StringBuilder();
        header.append(System.lineSeparator());
        header.append("@media ");
        header.append(media);
        if (!expression.isEmpty()) {
            header.append(" and ");
            header.append(expression);
        }
        header.append(" {");
        header.append(System.lineSeparator());
        return header.toString();
    }

    /**
     * Media section footer.
     */
    private static final String FOOTER = StringsUtil.concat(System.lineSeparator(), "}", System.lineSeparator());

    // --------------------------------------------------------------------------------------------
    // Test support

    Reader getReader() {
        return reader;
    }

    String getState() {
        return state.toString();
    }

    List<FilePath> getVariants() {
        return variants;
    }
}
