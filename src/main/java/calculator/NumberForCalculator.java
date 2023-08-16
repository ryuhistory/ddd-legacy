package calculator;

public class NumberForCalculator {
	private final int number;

	public NumberForCalculator(String text) {
		try {
			int number = Integer.parseInt(text);
			checkingNegative(number);
			this.number = number;
		} catch (NumberFormatException e) {
			throw new RuntimeException("숫자가 아닌 값이 존재 합니다.");
		}
	}

	private void checkingNegative(int number) {
		if (Integer.signum(number) == -1) {
			throw new RuntimeException("숫자가 음수입니다.");
		}
	}

	public int getNumber() {
		return number;
	}
}