package com.jslib.wood.lang;

import com.jslib.wood.util.StringsUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;

/**
 * Adapter for characters reader to input bytes stream encoded UTF-8. This class allows for reading UTF-8 bytes stream
 * from a characters source. At the core of this reading process there is {@link CharsetEncoder} that takes a sequence
 * of 16-bit Unicode characters and transform it into a sequence of bytes in UTF-8. Charset encoder uses two buffers:
 * {@link #charactersBuffer} for characters source - updated by wrapped reader, and {@link #bytesBuffer} where encoder
 * stores encoding result. All input stream read operations takes bytes from the bytes buffer; if bytes buffer is empty
 * delegates {@link #fillBytesBuffer()}.
 * 
 * @author Iulian Rotaru
 */
public class ReaderInputStream extends InputStream
{
  /** Size of internal characters buffer. See {@link #charactersBuffer}. */
  private static final int CHARACTERS_BUFFER_SIZE = 1024;

  /** Size of internal bytes buffer. See {@link #bytesBuffer}. */
  private static final int BYTES_BUFFER_SIZE = 128;

  /** Constant for end of file mark. */
  private static final int EOF = -1;

  /**
   * Encoder for UTF-8 character set. Encoder transforms a sequence of 16-bit Unicode characters into a sequence of
   * bytes in a specific charset, in this case UTF-8. Basically encoder reads from {@link #charactersBuffer} and writes
   * UTF-8 encoded bytes into {@link #bytesBuffer}.
   */
  private final CharsetEncoder encoder;

  /**
   * Characters buffer used as input for the encoder. Encoder reads characters from this buffer and stores encoded bytes
   * into {@link #bytesBuffer}.
   */
  private final CharBuffer charactersBuffer;

  /**
   * Bytes buffer used as output for the encoder. Encoder stores encoded bytes into this buffer; all input stream read
   * operations takes bytes from this buffer.
   */
  private final ByteBuffer bytesBuffer;

  /** Source reader. */
  private final Reader reader;

  /** Cache last encoder result. */
  private CoderResult lastEncoderResult;

  /** Flag true when end of file is detected by {@link #fillBytesBuffer()}. */
  private boolean endOfInput;

  /**
   * Construct a new input stream for source reader, using UTF-8 character encoding.
   * 
   * @param reader source reader.
   */
  public ReaderInputStream(Reader reader)
  {
    this.reader = reader;

    this.encoder = StandardCharsets.UTF_8.newEncoder();
    this.encoder.onMalformedInput(CodingErrorAction.REPLACE);
    this.encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);

    this.charactersBuffer = CharBuffer.allocate(CHARACTERS_BUFFER_SIZE);
    this.charactersBuffer.flip();
    this.bytesBuffer = ByteBuffer.allocate(BYTES_BUFFER_SIZE);
    this.bytesBuffer.flip();
  }

  /**
   * Reads up to len bytes of data from the input stream into an array of bytes. Returns the number of bytes read or -1
   * if end of stream was reached.
   * <p>
   * This method simple copy {@link #bytesBuffer} into given bytes buffer. If stream buffer is empty delegates
   * {@link #fillBytesBuffer()} to fill it from underlying reader.
   * 
   * @param bytes the bytes buffer to read into,
   * @param off the offset to start reading bytes into,
   * @param len the number of bytes to read.
   * @return the number of bytes read or <code>-1</code> if the end of the stream has been reached.
   * @throws IllegalArgumentException if <code>bytes</code> argument is null.
   * @throws IndexOutOfBoundsException if offset or length is negative or sum of offset and length exceeds buffer size.
   * @throws IOException if read from underlying source fails.
   */
  @Override
  public int read(byte[] bytes, int off, int len) throws IOException
  {
    assert bytes != null: "Bytes argument is null";
    if(len < 0 || off < 0 || (off + len) > bytes.length) {
      throw new IndexOutOfBoundsException(StringsUtil.concat("Array Size=", bytes.length, ", offset=", off, ", length=", len));
    }

    if(len == 0) {
      return 0; // Always return 0 if len == 0
    }

    int bytesRead = 0;
    while(len > 0) {
      if(bytesBuffer.hasRemaining()) {
        int count = Math.min(bytesBuffer.remaining(), len);
        bytesBuffer.get(bytes, off, count);
        off += count;
        len -= count;
        bytesRead += count;
      }
      else {
        fillBytesBuffer();
        if(endOfInput && !bytesBuffer.hasRemaining()) {
          break;
        }
      }
    }

    return bytesRead == 0 && endOfInput ? EOF : bytesRead;
  }

  /**
   * Reads some number of bytes from the input stream and stores them into the buffer array b. Returns the number of
   * bytes read or -1 if end of stream was reached.
   * <p>
   * This method simple copy {@link #bytesBuffer} into given bytes buffer. If stream buffer is empty delegates
   * {@link #fillBytesBuffer()} to fill it from underlying reader.
   * 
   * @param b the byte array to read into
   * @return the number of bytes read or <code>-1</code> if the end of the stream has been reached.
   * @throws IOException if read from underlying source fails.
   */
  @Override
  public int read(byte[] b) throws IOException
  {
    return read(b, 0, b.length);
  }

  /**
   * Reads the next byte of data from the input stream. If internal stream buffer contains at least one byte just return
   * it. Otherwise delegates {@link #fillBytesBuffer()} to add more bytes, bytes that are in the end read and encoded
   * from source reader.
   * 
   * @return either the byte read or <code>-1</code> if the end of the stream has been reached.
   * @throws IOException if read from underlying source reader fails.
   */
  @Override
  public int read() throws IOException
  {
    for(;;) {
      if(bytesBuffer.hasRemaining()) {
        return bytesBuffer.get() & 0xFF;
      }
      else {
        fillBytesBuffer();
        if(endOfInput && !bytesBuffer.hasRemaining()) {
          return EOF;
        }
      }
    }
  }

  /**
   * Close the stream. This method will cause the underlying {@link Reader} to be closed.
   * 
   * @throws IOException if underlying source reader closing fails.
   */
  @Override
  public void close() throws IOException
  {
    reader.close();
  }

  /**
   * Encode characters from {@link #charactersBuffer} and store UTF-8 bytes into {@link #bytesBuffer}. If characters
   * buffer is underflow takes care to read more characters from underlying source reader.
   * 
   * @throws IOException if read from underlying source reader fails.
   * @see #charactersBuffer
   * @see #bytesBuffer
   */
  private void fillBytesBuffer() throws IOException
  {
    // if last encoding result is underflow takes care to fill characters buffer
    if(!endOfInput && (lastEncoderResult == null || lastEncoderResult.isUnderflow())) {
      charactersBuffer.compact();
      int position = charactersBuffer.position();
      int readCount = reader.read(charactersBuffer.array(), position, charactersBuffer.remaining());
      if(readCount == EOF) {
        endOfInput = true;
      }
      else {
        charactersBuffer.position(position + readCount);
      }
      charactersBuffer.flip();
    }

    // get characters from reader buffer, encode them into bytes sequence and add to stream buffer
    bytesBuffer.compact();
    lastEncoderResult = encoder.encode(charactersBuffer, bytesBuffer, endOfInput);
    bytesBuffer.flip();
  }
}
