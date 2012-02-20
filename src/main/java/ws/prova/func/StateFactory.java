package ws.prova.func;

/**
 * A Kleisli arrow that maps A to State<S, B>. This is a monadic function a->Mb.
 *
 * @param <A>
 * @param <B>
 * @param <E>
 */
public abstract class StateFactory<S, A, B> implements
		F<A, State<S, B>> {

	protected abstract Pair<S, B> run(final S s, final A a);

	public final State<S, B> apply(final A a) {
		final StateFactory<S, A, B> self = this;
		return new State<S, B>() {
			public Pair<S, B> run(S s) {
				// State content does not change but the result is computed
				return self.run(s, a);
			}
		};
	}

}
