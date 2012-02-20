package ws.prova.func;

import java.io.Closeable;
import java.io.IOException;

public class ThrowersCloseables {

    /**
     * Unit function of the monad, i.e., a->Ma
     *
     * @param a
     * @return
     */
    public static <A, C extends Closeable, E extends Throwable> ThrowerCloseable<A, C, E> unit(final A a) {
        return new ThrowerCloseable<A, C, E>() {
            public Pair<C, A> extract() throws E {
                return Pairs.pair(null, a);
            }
        };
    }

    /**
     * Bind function of the Thrower monad with signature Ma->(a->Mb)->Mb.
     * This allows us to compose the monadic computations to create infinite pipelines Mb->...->Mb->...Mb.
     *
     * @param a  a monadic value Ma
     * @param fm a monadic function a->Mb, i.e., a function that either returns B or throws E
     * @return the monadic result of the computation Mb
     */
    public static <A, B, C extends Closeable, E extends Throwable> ThrowerCloseable<B, C, E> bind(
            final ThrowerCloseable<A, C, E> a, final F<A, ThrowerCloseable<B, C, E>> fm) {

        return new ThrowerCloseable<B, C, E>() {
            public Pair<C, B> extract() throws E {
                //final Pair<C, A> result = a.extract();
                Pair<C, A> result = null;
                try {
                    result = a.extract();
                    return fm.apply(result.second()).extract();
                } catch (Throwable e) {
                    throw (E) e;
                } finally {
                    if (result!=null) {
                        final Closeable c = result.first();
                        if (c!=null) {
                            System.out.println("Closing "+c.getClass().getSimpleName());
                            try {
                                c.close();
                            } catch (IOException e) {
                                // ignore
                            }
                        }
                    }
                }
            }
        };
    }
}

