package ws.prova.func;

/**
 * A functor that given E, takes a class A to a computation Thrower<A,E> that can throw E
 *
 * @param <A>
 * @param <E>
 */
public interface Thrower<A, E extends Throwable> {

	/**
	 * Unwrap A from its monadic form
	 * @return
	 * @throws E
	 */
	public A extract() throws E;

}