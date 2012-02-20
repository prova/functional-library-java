package test.ws.prova.func.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import ws.prova.func.F;
import ws.prova.func.Pair;
import ws.prova.func.Pairs;
import ws.prova.func.State;
import ws.prova.func.StateFactory;
import ws.prova.func.States;

public class TestStates {

	@Test
	public void testLowercase() {
		F<String, State<Integer, String>> putState = new F<String, State<Integer, String>>() {
			public State<Integer, String> apply(final String a) {
				return new State<Integer, String>() {
					public Pair<Integer, String> run(Integer s) {
						// Ignore and replace the previous state
						return Pairs.pair(a.length(), a);
					}
				};
			}
		};
		StateFactory<Integer, String, String> f1 = new StateFactory<Integer, String, String>() {
			@Override
			protected Pair<Integer, String> run(final Integer s, final String a) {
				return Pairs.pair(s, a.toLowerCase());
			}
		};
		// TODO Fragment, not used yet
//		State<Integer, String> getState = new State<Integer, String>() {
//			@Override
//			public Pair<Integer, String> run(Integer s) {
//				return Pairs.pair(s, s.toString());
//			}
//		};
		Pair<Integer, String> p = States.bind(putState.apply("Toto"),f1).run(0);
		assertEquals(
				"Wrong pipeline result",
				"(4:java.lang.Integer, toto:java.lang.String)",
				p.toString());
	}

	@Test
	public void testDuplicate() {
		F<String, State<Integer, String>> putState = new F<String, State<Integer, String>>() {
			public State<Integer, String> apply(final String a) {
				return new State<Integer, String>() {
					public Pair<Integer, String> run(Integer s) {
						// Ignore and replace the previous state
						return Pairs.pair(a.length(), a);
					}
				};
			}
		};
		StateFactory<Integer, String, String> f2 = new StateFactory<Integer, String, String>() {
			@Override
			protected Pair<Integer, String> run(final Integer s, final String a) {
				return Pairs.pair(s, a+a);
			}
		};
		Pair<Integer, String> p = States.bind(
				States.bind(putState.apply("Toto"),f2),putState).run(0);
		assertEquals(
				"Wrong pipeline result",
				"(8:java.lang.Integer, TotoToto:java.lang.String)",
				p.toString());
	}

	@Test
	public void testFlipcase() {
		F<String, State<Boolean, String>> initState = new F<String, State<Boolean, String>>() {
			public State<Boolean, String> apply(final String a) {
				return new State<Boolean, String>() {
					public Pair<Boolean, String> run(Boolean s) {
						return Pairs.pair(s, a);
					}
				};
			}
		};
		StateFactory<Boolean, String, String> flipcase = new StateFactory<Boolean, String, String>() {
			@Override
			protected Pair<Boolean, String> run(final Boolean s, final String a) {
				return Pairs.pair(!s, s ? a.toUpperCase() : a.toLowerCase());
			}
		};
		Pair<Boolean, String> p = States.bind(
				States.bind(initState.apply("Toto"),flipcase),flipcase).run(false);
		assertEquals(
				"Wrong pipeline result",
				"(false:java.lang.Boolean, TOTO:java.lang.String)",
				p.toString());
	}

}
