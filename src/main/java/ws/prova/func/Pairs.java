package ws.prova.func;

public class Pairs {

	public static <A, B> Pair<A, B> pair(final A a, final B b) {
		return new Pair<A, B>() {
			public A first() {
				return a;
			}

			public B second() {
				return b;
			}

			public String toString() {
				return "(" + a + ":" + a.getClass().getName() + ", " + b + ":"
						+ b.getClass().getName() + ")";
			}
		};
	}

}