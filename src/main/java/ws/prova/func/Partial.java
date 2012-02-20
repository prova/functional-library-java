package ws.prova.func;

/**
 * A Kleisli arrow that maps A to B but can throw E. This is a monadic function a->Mb.
 *
 * @param <A>
 * @param <B>
 * @param <E>
 */
public abstract class Partial<A, B, E extends Throwable> implements
		F<A, Thrower<B, E>> {

	protected abstract B run(final A a) throws E;

	public final Thrower<B, E> apply(final A a) {
		final Partial<A, B, E> self = this;
		return new Thrower<B, E>() {
			public B extract() throws E {
				return self.run(a);
			}
		};
	}

}
