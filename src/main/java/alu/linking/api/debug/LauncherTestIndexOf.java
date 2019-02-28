package alu.linking.api.debug;

public class LauncherTestIndexOf {
	public static void main(String[] argv)
	{
		String text = "a bca a aaaa";
		int textIndex = 0;
		while ((textIndex = text.indexOf("a", textIndex)) != -1) {
			System.out.println("Textindex="+textIndex);
			textIndex++;
		}
	}
}
