package ws.prova.func;

public class States {

	/**
	 * Unit function of the monad, i.e., a->Ma
	 * 
	 * @param a
	 * @return
	 */
	public static <S, A> State<S, A> unit(final A a) {
		return new State<S, A>() {
			public Pair<S, A> run(final S s) {
				return Pairs.pair(s, a);
			}
		};
	}

	/**
	 * Bind function of the State monad with signature Ma->(a->Mb)->Mb.
	 * This allows us to compose the monadic computations to create infinite pipelines Mb->...->Mb->...Mb.
	 * @param a a monadic value Ma
	 * @param fm a monadic function a->Mb, i.e., a function that either returns B or throws E
	 * @return the monadic result of the computation Mb
	 */
	public static <S, A, B> State<S, B> bind(
			final State<S, A> a, final F<A, State<S, B>> fm) {
		return new State<S, B>() {
			public Pair<S, B> run(S s) {
				final Pair<S, A> a_pair = a.run(s);
				return fm.apply(a_pair.second()).run(a_pair.first());
			}
		};
	}

}
