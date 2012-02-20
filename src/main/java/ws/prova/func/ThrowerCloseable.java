package ws.prova.func;

import java.io.Closeable;

/**
 * A functor that given E, takes a class A to a computation Thrower<A,E> that can throw E
 *
 * @param <A>
 * @param <E>
 */
public interface ThrowerCloseable<A, C extends Closeable, E extends Throwable> {

    /**
     * Unwrap A from its monadic form
     *
     * @return
     * @throws E
     */
    public Pair<C, A> extract() throws E;

}