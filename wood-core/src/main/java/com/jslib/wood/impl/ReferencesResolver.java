package com.jslib.wood.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import com.jslib.wood.FilePath;
import com.jslib.wood.IReferenceHandler;
import com.jslib.wood.Reference;
import com.jslib.wood.SourceReader;
import com.jslib.wood.Variables;
import com.jslib.wood.WoodException;
import com.jslib.wood.util.FilesUtil;

/**
 * Resolve resource references that may exist into a variable value. Resources resolving is a text replace process; it replaces
 * reference with variable value or media file URL path. The actual resource processing is delegated to external reference
 * handler.
 * <p>
 * This class provides a parsing method for variable value, see {@link #parse(String, FilePath, IReferenceHandler)}. It is
 * invoked by {@link Variables#get(String, Reference, FilePath, IReferenceHandler)} with found variable value. Value reference
 * may point to a new value that in its turn may have references, creating a tree of values linked by references. This class
 * parsing occurs in a recursive loop, in depth-first order, till entire values tree is resolved. See {@link SourceReader} for a
 * discussion on references tree iteration.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class ReferencesResolver {
	/**
	 * Parse variable value and invoke {@link IReferenceHandler reference handler} when encounter a resource reference. Return
	 * the value with resource references resolved.
	 * 
	 * @param value variable value,
	 * @param sourceFile source file that define the scope of the variable reference,
	 * @param referenceHandler reference handler to invoke for reference processing.
	 * @return value with references resolved.
	 */
	public String parse(String value, FilePath sourceFile, IReferenceHandler referenceHandler) {
		assert value != null && !value.isEmpty(): "Value argument is null or empty";

		// variable file is a synthetic file used only to convey information about related component path
		FilePath varFile = sourceFile.isSynthetic() ? sourceFile : sourceFile.cloneTo(FileType.VAR);

		Reader reader = new SourceReader(new StringReader(value), varFile, referenceHandler);
		StringWriter writer = new StringWriter();
		try {
			FilesUtil.copy(reader, writer);
		} catch (IOException e) {
			throw new WoodException(e);
		}
		return writer.toString();
	}
}
