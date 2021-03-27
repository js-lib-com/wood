package js.wood.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Console {
	public void print(String format, Object... args) {
		System.out.printf(format, args);
		System.out.println();
	}

	public void print(Object object) {
		System.out.print(object.toString());
		System.out.println();
	}

	public void warning(String warning) {
		System.out.println(warning);
	}

	public void warning(String format, Object... args) {
		System.out.printf(format, args);
		System.out.println();
	}

	public void info(String info) {
		System.out.println(info);
	}

	public void info(String format, Object... args) {
		System.out.printf(format, args);
		System.out.println();
	}

	private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

	public String input(String message, String... defaultValue) throws IOException {
		System.out.print("- ");
		System.out.print(message);
		System.out.print(": ");
		if (defaultValue.length == 1) {
			System.out.printf("[%s]: ", defaultValue[0]);
		}
		String value = reader.readLine();
		return value.isEmpty() ? defaultValue[0] : value;
	}

	public boolean confirm(String message, String positiveAnswer) throws IOException {
		System.out.print(message);
		System.out.print(": ");
		String answer = reader.readLine();
		return answer.equalsIgnoreCase(positiveAnswer);
	}

	public void crlf() {
		System.out.println();
	}
}
