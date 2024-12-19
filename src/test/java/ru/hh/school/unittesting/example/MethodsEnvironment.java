package ru.hh.school.unittesting.example;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MethodsEnvironment extends ConstantsEnvironment {

  protected static double round(double value) {
    return BigDecimal.valueOf(value)
        .setScale(2, RoundingMode.HALF_UP)
        .doubleValue();
  }
}
