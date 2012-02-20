package ws.prova.func;

import java.io.Closeable;

/**
 * A Kleisli arrow that maps A to B but also produces a Closeable C and can throw E.
 * This is a monadic function a->Mb.
 *
 * @param <A>
 * @param <B>
 * @param <E>
 */
public abstract class PartialCloseable<A, B, C extends Closeable, E extends Throwable>
        implements F<A, ThrowerCloseable<B, C, E>> {

    protected abstract Pair<C, B> run(final A a) throws E;

    public final ThrowerCloseable<B, C, E> apply(final A a) {

        final PartialCloseable<A, B, C, E> self = this;

        return new ThrowerCloseable<B, C, E>() {
            public Pair<C, B> extract() throws E {
                return self.run(a);
            }
        };
    }

}

