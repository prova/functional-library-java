package ws.prova.func;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class Func002 {

	public static void main(String[] args) {
		Partial<String, URL, Exception> f1 = new URLBuilder() {
			@Override
			protected URL run(String a) throws Exception {
				return new URL(a);
			}
		};
		Partial<URL, URLConnection, Exception> f2 = new URLConnector() {
			@Override
			protected URLConnection run(URL a) throws Exception {
				HttpURLConnection c = (HttpURLConnection) a.openConnection();
				c.setRequestMethod("GET");
				c.setReadTimeout(10000);
				c.connect();
				return c;
			}
		};
		Partial<URLConnection, String, Exception> f3 = new NetTalker<String>() {
			@Override
			protected String run(URLConnection a) throws Exception {
				BufferedReader rd  = new BufferedReader(new InputStreamReader(a.getInputStream()));
				String line = rd.readLine();
				return line;
			}
		};
		Partial<String, String, Exception> f4 = new Printer<String>() {
			@Override
			protected String run(String a) throws Exception {
				System.out.println(a);
				return "";
			}
		};
		ErrorHandler.extractFrom(
				Throwers.bind(
						Throwers.bind(
								Throwers.bind(
										f1.apply("http://www.cnn.com"),
										f2),
								f3)
						,f4)
		);
	}

}
