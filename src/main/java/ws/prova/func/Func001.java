package ws.prova.func;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class Func001 {

	public static void main(String[] args) {
		Thrower<String, Throwable> readLn = new Thrower<String, Throwable>() {
			public String extract() throws IOException {
				return new BufferedReader(new InputStreamReader(System.in))
						.readLine();
			}
		};
		System.out.println(ErrorHandler.extractFrom(Throwers.fmap(length).apply(
				readLn)));
	}

	// A first-class function to get a String's length.
	public static F<String, Integer> length = new F<String, Integer>() {
		public Integer apply(final String s) {
			return s.length();
		}
	};
}
