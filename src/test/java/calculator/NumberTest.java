package calculator;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class NumberTest {

    @ParameterizedTest
    @ValueSource(strings = {"*,d,아,$"})
    @DisplayName("숫자가 아닌 값이 입력 되었을 경우")
    void noneNumber(String input) {
        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> Number.of(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1,-2,-99"})
    @DisplayName("음수가 입력 되었을 경우")
    void negativeNumber(String input) {
        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> Number.of(input));
    }

    @ParameterizedTest
    @CsvSource(value = {"1,3,4", "99,2,101", "47,31,78"})
    void plus(String argument1, String argument2, String result) {
        Number number1 = Number.of(argument1);
        Number number2 = Number.of(argument2);
        Number number3 = Number.of(result);
        assertThat(number1.plus(number2))
            .isEqualTo(number3);
    }
}