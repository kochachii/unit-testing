package ru.hh.school.unittesting.example.homework;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.hh.school.unittesting.example.base.LibraryManagerBaseTest;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class LibraryManagerTest extends LibraryManagerBaseTest {

  @ParameterizedTest(name = "getAvailableCopies(\"{0}\") should return {2}")
  @MethodSource("provideGetAvailableCopiesTestArguments_normal")
  @DisplayName("Получение доступных копий книг для книги с нормальным bookId")
  void getAvailableCopies_shouldReturnExpectedCount_whenBookIdIsNormal(
      String bookId, int quantity, int expected
  ) {
    getAvailableCopies_checkWithTestCase(bookId, quantity, expected);
  }

  private void getAvailableCopies_checkWithTestCase(String bookId, int quantity, int expected) {
    int actual = addBooksAndGetAvailableCopies(bookId, quantity);
    assertEquals(expected, actual,
        String.format(
            "Для bookId \"%s\" ожидается %d копий, но получено %d",
            bookId, expected, actual)
    );
  }

  // хоть тест и такой же, но по факту здесь может быть другая логика
  @ParameterizedTest(name = "getAvailableCopies(\"{0}\") should return {2}")
  @MethodSource("provideGetAvailableCopiesTestArguments_blank")
  @DisplayName("Получение доступных копий книг для книги с пустым bookId")
  void getAvailableCopies_shouldReturnExpectedCount_whenBookIdIsBlank(
      String bookId, int quantity, int expected
  ) {
    getAvailableCopies_checkWithTestCase(bookId, quantity, expected);
  }

  @Test
  @DisplayName("Добавление новых книг с различными количествами")
  void addBook_shouldAddNewBooksWithVariousQuantities_whenBookIdIsNew() {
    addBook_checkWithTestCases(addBooks_getDefaultTestCases());
  }

  private void addBook_checkWithTestCases(Object[][] testCases) {
    Arrays.stream(testCases).forEach(testCase -> {
      String bookId = (String) testCase[0];
      int quantity = (int) testCase[1];
      int expected = (int) testCase[2];
      int actual = addBooksAndGetAvailableCopies(bookId, quantity);
      assertEquals(expected, actual,
          String.format(
              "После добавления %d копий к существующей книге с ID \"%s\" ожидается количество %d, но получено %d",
              quantity, bookId, expected, actual
          )
      );
    });
  }

  @Test
  @DisplayName("Добавление копий к существующей книге c нормальным bookId")
  void addBook_shouldModifyExistingBooksWithVariousChanges_whenBookIdExists() {
    libraryManager.addBook(DEFAULT_BOOK_ID, ZERO_QUANTITY);
    addBook_checkWithTestCases(addBooks_getTestCasesForExistentBook(DEFAULT_BOOK_ID));
  }

  // хоть тест и такой же, но по факту здесь может быть другая логика
  @Test
  @DisplayName("Добавление копий книг к существующей книге с пустым bookId")
  void addBook_shouldHandleCornerCaseIds_whenBookIdIsBlank() {
    libraryManager.addBook(EMPTY_BOOK_ID, ZERO_QUANTITY);
    libraryManager.addBook(NULL_BOOK_ID, ZERO_QUANTITY);
    addBook_checkWithTestCases(addBooks_getTestCasesForExistentBook(EMPTY_BOOK_ID));
    addBook_checkWithTestCases(addBooks_getTestCasesForExistentBook(NULL_BOOK_ID));
  }

  @Test
  @DisplayName("Попытка взять книгу неактивным пользователем")
  void borrowBook_shouldReturnFalse_whenUserIsInactive() {
    libraryManager.addBook(DEFAULT_BOOK_ID, DEFAULT_QUANTITY);
    boolean result = borrowBookAsInactiveUser(DEFAULT_BOOK_ID);
    assertFalse(result, "Метод должен вернуть false, т.к. пользователь не активен");
    verify(notificationService, times(1))
        .notifyUser(DEFAULT_USER_ID, INACTIVE_MESSAGE);
    verifyNoMoreInteractions(notificationService);
  }


  @Test
  @DisplayName("Попытка взять книгу, которой нет в наличии")
  void borrowBook_shouldReturnFalse_whenBookDoesNotExist() {
    boolean result = borrowNonExistentBookAsUser(DEFAULT_USER_ID);
    assertFalse(result, "Метод должен вернуть false, т.к. книги нет в наличии");
    verify(notificationService, never())
        .notifyUser(anyString(), anyString());
  }

  @Test
  @DisplayName("Попытка взять книгу без доступных копий")
  void borrowBook_shouldReturnFalse_whenNoAvailableCopies() {
    libraryManager.addBook(DEFAULT_BOOK_ID, ZERO_QUANTITY);
    boolean result = borrowBookAsUser(DEFAULT_BOOK_ID, DEFAULT_USER_ID);
    assertFalse(result, "Метод должен вернуть false, т.к. нет доступных копий");
    verify(notificationService, never())
        .notifyUser(anyString(), anyString());
  }

  @Test
  @DisplayName("Успешное получение книги активным пользователем")
  void borrowBook_shouldReturnTrue_whenUserIsActiveAndBookIsAvailable() {
    libraryManager.addBook(DEFAULT_BOOK_ID, DEFAULT_QUANTITY);
    boolean result = borrowBookAsUser(DEFAULT_BOOK_ID, DEFAULT_USER_ID);
    assertTrue(result, "Метод должен вернуть true, т.к. книга доступна и пользователь активен");
    assertEquals(DEFAULT_QUANTITY - 1, libraryManager.getAvailableCopies(DEFAULT_BOOK_ID),
        "Количество доступных копий должно уменьшиться на 1");
    verify(notificationService, times(1))
        .notifyUser(DEFAULT_USER_ID, BORROWED_MESSAGE + DEFAULT_BOOK_ID);
  }

  @Test
  @DisplayName("Попытка вернуть книгу, которой пользователь не брал")
  void returnBook_shouldReturnFalse_whenBookWasNotBorrowedByUser() {
    libraryManager.addBook(DEFAULT_BOOK_ID, DEFAULT_QUANTITY);
    boolean result = returnBookAsUser(DEFAULT_BOOK_ID, DEFAULT_USER_ID);
    assertFalse(result, "Метод должен вернуть false, т.к. книга не была взята пользователем");
    verify(notificationService, never())
        .notifyUser(anyString(), anyString());
  }

  @Test
  @DisplayName("Попытка вернуть книгу, взятую другим пользователем")
  void returnBook_shouldReturnFalse_whenBookWasBorrowedByAnotherUser() {
    libraryManager.addBook(DEFAULT_BOOK_ID, DEFAULT_QUANTITY);
    borrowBookAsUser(DEFAULT_BOOK_ID, OTHER_USER_ID);
    boolean result = returnBookAsUser(DEFAULT_BOOK_ID, DEFAULT_USER_ID);
    assertFalse(result, "Метод должен вернуть false, т.к. книга была взята другим пользователем");
    verify(notificationService, never())
        .notifyUser(anyString(), anyString());
  }

  @Test
  @DisplayName("Успешное возвращение книги пользователем")
  void returnBook_shouldReturnTrue_whenBookIsBorrowedByUser() {
    libraryManager.addBook(DEFAULT_BOOK_ID, DEFAULT_QUANTITY);
    borrowBookAsUser(DEFAULT_BOOK_ID, DEFAULT_USER_ID);
    boolean result = returnBookAsUser(DEFAULT_BOOK_ID, DEFAULT_USER_ID);
    assertTrue(result, "Метод должен вернуть true, т.к. книга успешно возвращена пользователем");
    assertEquals(DEFAULT_QUANTITY, libraryManager.getAvailableCopies(DEFAULT_BOOK_ID),
        "Количество доступных копий не должно отличаться от исходного");
    verify(notificationService, times(1))
        .notifyUser(DEFAULT_USER_ID, RETURNED_MESSAGE + DEFAULT_BOOK_ID);
  }

  @ParameterizedTest(name = "calculateDynamicLateFee({0}, {1}, {2}) should throw IllegalArgumentException")
  @MethodSource("provideCalculateDynamicLateFeeTestArguments_invalid")
  @DisplayName("Попытка подсчета штрафа, но дни просрочки отрицательны")
  void calculateDynamicLateFee_shouldThrowException_whenOverdueDaysIsNegative(
      int overdueDays, boolean isBestseller, boolean isPremiumMember
  ) {
    assertThrows(IllegalArgumentException.class, () ->
            libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember),
        "Ожидается IllegalArgumentException для отрицательных дней просрочки");
  }

  @ParameterizedTest(name = "calculateDynamicLateFee({0}, {1}, {2}) should return {3}")
  @MethodSource("provideCalculateDynamicLateFeeTestArguments_valid")
  @DisplayName("Успешный подсчет штрафа")
  void calculateDynamicLateFee_shouldReturnExpectedFee_whenInputsAreValid(
      int overdueDays, boolean isBestseller, boolean isPremiumMember, double expected
  ) {
    double actual = libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember);
    assertEquals(expected, actual,
        String.format("Для overdueDays=%d, isBestseller=%b, isPremiumMember=%b ожидается штраф %.2f, но получен %.2f",
            overdueDays, isBestseller, isPremiumMember, expected, actual));
  }
}

