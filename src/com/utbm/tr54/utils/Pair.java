package com.utbm.tr54.utils;


/**
 * The Class Pair.
 *
 * @param <X> the generic type of the first element
 * @param <Y> the generic type of the second element
 */
public class Pair<X, Y> {
	
	/** The first element of the pair. */
	public final X first;
	
	/** The second element of the pair. */
	public final Y second;

	/**
	 * Instantiates a new pair.
	 *
	 * @param _first the first element
	 * @param _second the second element
	 */
	public Pair(final X _first, final Y _second) {
		this.first = _first;
		this.second = _second;
	}
}
