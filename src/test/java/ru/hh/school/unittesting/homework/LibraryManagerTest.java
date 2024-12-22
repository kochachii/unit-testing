package ru.hh.school.unittesting.homework;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {

  @Mock
  protected NotificationService notificationService;

  @Mock
  protected UserService userService;

  @InjectMocks
  protected LibraryManager libraryManager;

  @ParameterizedTest(name = "getAvailableCopies(\"{0}\") should return {2}")
  @CsvSource({
      "bookId1, 5, 5",
      "bookId2, -5, -5",
      "bookId3, 0, 0"
  })
  @DisplayName("Получение доступных копий книг для книги с нормальным bookId")
  void getAvailableCopies_shouldReturnExpectedCount_whenBookIdIsNormal(
      String bookId, int quantity, int expectedQuantity
  ) {
    libraryManager.addBook(bookId, quantity);

    int actualQuantity = libraryManager.getAvailableCopies(bookId);
    assertEquals(expectedQuantity, actualQuantity, "Количество доступных копий не совпадает с полученным");
  }

  @Test
  @DisplayName("Получение доступных копий книг для книги с null bookId")
  void getAvailableCopies_shouldReturnExpectedCount_whenBookIdIsNull() {
    libraryManager.addBook(null, -5);

    int actualQuantity = libraryManager.getAvailableCopies(null);
    assertEquals(-5, actualQuantity, "Количество доступных копий не совпадает с полученным");
  }

  @Test
  @DisplayName("Получение доступных копий книг для книги с пустым bookId")
  void getAvailableCopies_shouldReturnExpectedCount_whenBookIdIsEmpty() {
    libraryManager.addBook("", 5);

    int actualQuantity = libraryManager.getAvailableCopies("");
    assertEquals(5, actualQuantity, "Количество доступных копий не совпадает с полученным");
  }

  @Test
  @DisplayName("Добавление новых книг с различными количествами")
  void addBook_shouldAddNewBooksWithVariousQuantities_whenBookIdIsNormal() {
    libraryManager.addBook("bookId", 5);
    libraryManager.addBook("bookId", -10);
    libraryManager.addBook("bookId", 0);

    int actualQuantity = libraryManager.getAvailableCopies("bookId");
    assertEquals(-5, actualQuantity, "Количество доступных копий не совпадает с добавленным");
  }

  @Test
  @DisplayName("Попытка взять книгу неактивным пользователем")
  void borrowBook_shouldReturnFalse_whenUserIsInactive() {
    libraryManager.addBook("bookId", 5);
    when(userService.isUserActive("userId"))
        .thenReturn(false);

    boolean wasBorrowed = libraryManager.borrowBook("bookId", "userId");
    assertFalse(wasBorrowed, "Метод должен вернуть false, т.к. пользователь не активен");
    verify(notificationService, times(1))
        .notifyUser("userId", "Your account is not active.");
    verifyNoMoreInteractions(notificationService);
  }

  @Test
  @DisplayName("Попытка взять книгу, которой нет в наличии")
  void borrowBook_shouldReturnFalse_whenBookDoesNotExist() {
    when(userService.isUserActive("userId"))
        .thenReturn(true);

    boolean wasBorrowed = libraryManager.borrowBook("nonExistentBookId", "userId");
    assertFalse(wasBorrowed, "Метод должен вернуть false, т.к. книги нет в наличии");
    verify(notificationService, never())
        .notifyUser(anyString(), anyString());
  }

  @Test
  @DisplayName("Попытка взять книгу без доступных копий")
  void borrowBook_shouldReturnFalse_whenNoAvailableCopies() {
    libraryManager.addBook("bookId", 0);
    when(userService.isUserActive("userId"))
        .thenReturn(true);

    boolean wasBorrowed = libraryManager.borrowBook("bookId", "userId");
    assertFalse(wasBorrowed, "Метод должен вернуть false, т.к. нет доступных копий");
    verify(notificationService, never())
        .notifyUser(anyString(), anyString());
  }

  @Test
  @DisplayName("Успешное получение книги активным пользователем")
  void borrowBook_shouldReturnTrue_whenUserIsActiveAndBookIsAvailable() {
    libraryManager.addBook("bookId", 5);
    when(userService.isUserActive("userId"))
        .thenReturn(true);

    boolean wasBorrowed = libraryManager.borrowBook("bookId", "userId");
    assertTrue(wasBorrowed, "Метод должен вернуть true, т.к. книга доступна и пользователь активен");
    assertEquals(4, libraryManager.getAvailableCopies("bookId"),
        "Количество доступных копий должно уменьшиться на 1");
    verify(notificationService, times(1))
        .notifyUser("userId", "You have borrowed the book: " + "bookId");
  }

  @Test
  @DisplayName("Попытка вернуть книгу, которой пользователь не брал")
  void returnBook_shouldReturnFalse_whenBookWasNotBorrowedByUser() {
    libraryManager.addBook("bookId", 5);
    reset(notificationService);

    boolean wasReturned = libraryManager.returnBook("bookId", "userId");
    assertFalse(wasReturned, "Метод должен вернуть false, т.к. книга не была взята пользователем");
    verify(notificationService, never())
        .notifyUser(anyString(), anyString());
  }

  @Test
  @DisplayName("Попытка вернуть книгу, взятую другим пользователем")
  void returnBook_shouldReturnFalse_whenBookWasBorrowedByAnotherUser() {
    libraryManager.addBook("bookId", 5);
    when(userService.isUserActive("otherUserId"))
        .thenReturn(true);
    libraryManager.borrowBook("bookId", "otherUserId");
    reset(notificationService);

    boolean wasReturned = libraryManager.returnBook("bookId", "userId");
    assertFalse(wasReturned, "Метод должен вернуть false, т.к. книга была взята другим пользователем");
    verify(notificationService, never())
        .notifyUser(anyString(), anyString());
  }

  @Test
  @DisplayName("Успешное возвращение книги пользователем")
  void returnBook_shouldReturnTrue_whenBookIsBorrowedByUser() {
    libraryManager.addBook("bookId", 5);
    when(userService.isUserActive("userId"))
        .thenReturn(true);
    libraryManager.borrowBook("bookId","userId");
    reset(notificationService);

    boolean wasReturned = libraryManager.returnBook("bookId", "userId");
    assertTrue(wasReturned, "Метод должен вернуть true, т.к. книга успешно возвращена пользователем");
    assertEquals(5, libraryManager.getAvailableCopies("bookId"),
        "Количество доступных копий не должно отличаться от исходного");
    verify(notificationService, times(1))
        .notifyUser("userId", "You have returned the book: " + "bookId");
  }

  @ParameterizedTest(name = "calculateDynamicLateFee({0}, {1}, {2}) should throw IllegalArgumentException")
  @CsvSource({
      "-1, false, false",
      "-2, false, true",
      "-3, true, false",
      "-4, true, true",
  })
  @DisplayName("Попытка подсчета штрафа, но дни просрочки отрицательны")
  void calculateDynamicLateFee_shouldThrowException_whenOverdueDaysIsNegative(
      int overdueDays, boolean isBestseller, boolean isPremiumMember
  ) {
    assertThrows(IllegalArgumentException.class, () ->
            libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember),
        "Ожидается IllegalArgumentException для отрицательных дней просрочки");
  }

  @ParameterizedTest(name = "calculateDynamicLateFee({0}, {1}, {2}) should return {3}")
  @CsvSource({
      // isBestseller = false, isPremiumMember = false
      "0, false, false, 0.00",
      "-0, false, false, 0.00",
      "1, false, false, 0.50",
      "3, false, false, 1.50",

      // isBestseller = false, isPremiumMember = true
      "1, false, true, 0.40",
      "3, false, true, 1.20",

      // isBestseller = true, isPremiumMember = false
      "1, true, false, 0.75",
      "3, true, false, 2.25",

      // isBestseller = true, isPremiumMember = true
      "1, true, true, 0.60",
      "3, true, true, 1.80",
  })
  @DisplayName("Успешный подсчет штрафа")
  void calculateDynamicLateFee_shouldReturnExpectedFee_whenInputsAreValid(
      int overdueDays, boolean isBestseller, boolean isPremiumMember, double fee
  ) {
    double actualFee = libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember);
    assertEquals(fee, actualFee,
        String.format("Для overdueDays=%d, isBestseller=%b, isPremiumMember=%b ожидается штраф %.2f, но получен %.2f",
            overdueDays, isBestseller, isPremiumMember, fee, actualFee));
  }
}

