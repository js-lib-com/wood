package com.jslib.wood.dom;

/**
 * List of elements returned by elements finders. Instances of this interface are returned by <code>findBy</code> search methods
 * from both document and elements. Search methods always returns instance of this interface, even if empty. User code may use
 * {@link #isEmpty()} to detect if search actually find some elements.
 * <p>
 * Beside standard iterable this interfaces has method for list size and indexed random access. Also provides generic method
 * execution on all list elements.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface EList extends Iterable<Element> {
	/**
	 * Get this list size.
	 * 
	 * @return this list size.
	 */
	int size();

	/**
	 * Get element from index. Returns the element identified by given index or null if index is not valid.
	 * 
	 * @param index element index relative to this list.
	 * @return element from requested index.
	 */
	Element item(int index);

	/**
	 * Test if this list is empty.
	 * 
	 * @return true if this elements list is empty.
	 */
	boolean isEmpty();

	/**
	 * Remove all elements. Remove all elements from this list and also from owning document. After executing this method
	 * {@link #isEmpty} returns true and {@link #iterator} returns an empty iterator.
	 */
	void remove();
}
