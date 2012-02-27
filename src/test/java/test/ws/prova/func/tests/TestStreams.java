package test.ws.prova.func.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import fj.F;
import fj.data.Array;
import fj.data.Stream;

public class TestStreams {

	@Test
	public void testIteratedStream() {
		Stream<Double> str = Stream.<Double>iterateWhile(
				new F<Double, Double>() {
					@Override
					public Double f(Double a) {
						return a + 1;
					}},
				new F<Double, Boolean>() {
					@Override
					public Boolean f(Double a) {
						return a < 10.0;
					}},
				0.0);
		Array<Double> arr = str.toArray();
		assertEquals("Incorrect stream was generated", 10, arr.length());
	}

}
