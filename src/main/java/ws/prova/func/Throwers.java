package ws.prova.func;

public class Throwers {

	public static <A, B, E extends Throwable> F<Thrower<A, E>, Thrower<B, E>> fmap(
			final F<A, B> f) {
		return new F<Thrower<A, E>, Thrower<B, E>>() {
			public Thrower<B, E> apply(final Thrower<A, E> a) {
				return new Thrower<B, E>() {
					public B extract() throws E {
						return f.apply(a.extract());
					}
				};
			}
		};
	}

	/**
	 * Unit function of the monad, i.e., a->Ma
	 * 
	 * @param a
	 * @return
	 */
	public static <A, E extends Throwable> Thrower<A, E> unit(final A a) {
		return new Thrower<A, E>() {
			public A extract() {
				return a;
			}
		};
	}

	/**
	 * Bind function of the Thrower monad with signature Ma->(a->Mb)->Mb.
	 * This allows us to compose the monadic computations to create infinite pipelines Mb->...->Mb->...Mb.
	 * @param a a monadic value Ma
	 * @param fm a monadic function a->Mb, i.e., a function that either returns B or throws E
	 * @return the monadic result of the computation Mb
	 */
	public static <A, B, E extends Throwable> Thrower<B, E> bind(
			final Thrower<A, E> a, final F<A, Thrower<B, E>> fm) {
		return new Thrower<B, E>() {
			public B extract() throws E {
				return fm.apply(a.extract()).extract();
			}
		};
	}

}
