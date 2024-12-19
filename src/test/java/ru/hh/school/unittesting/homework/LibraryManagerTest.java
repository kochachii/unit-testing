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
public class LibraryManagerTest {

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
  void getAvailableCopies_shouldReturnExpectedCount_whenBookIdIsNormal() {
    String bookId = "bookId";
    int quantity = 5;
    libraryManager.addBook(bookId, quantity);
    int actualQuantity = libraryManager.getAvailableCopies(bookId);
    assertEquals(quantity, actualQuantity,
        String.format(
            "Для bookId \"%s\" ожидается %d копий, но получено %d",
            bookId, quantity, actualQuantity)
    );
  }

  @Test
  @DisplayName("Получение доступных копий книг для книги с null bookId")
  void getAvailableCopies_shouldReturnExpectedCount_whenBookIdIsNull() {
    String nullBookId = null;
    int quantity = -5;
    libraryManager.addBook(nullBookId, quantity);
    int actualQuantity = libraryManager.getAvailableCopies(nullBookId);
    assertEquals(quantity, actualQuantity,
        String.format(
            "Для bookId \"%s\" ожидается %d копий, но получено %d",
            nullBookId, quantity, actualQuantity)
    );
  }

  @Test
  @DisplayName("Получение доступных копий книг для книги с пустым bookId")
  void getAvailableCopies_shouldReturnExpectedCount_whenBookIdIsEmpty() {
    String emptyBookId = "";
    int quantity = 5;
    libraryManager.addBook(emptyBookId, quantity);
    int actualQuantity = libraryManager.getAvailableCopies(emptyBookId);
    assertEquals(quantity, actualQuantity,
        String.format(
            "Для bookId \"%s\" ожидается %d копий, но получено %d",
            emptyBookId, quantity, actualQuantity)
    );
  }

  @Test
  @DisplayName("Добавление новых книг с различными количествами")
  void addBook_shouldAddNewBooksWithVariousQuantities_whenBookIdIsNormal() {
    String bookId = "bookId";
    int quantity = 5;
    int expectedQuantity = quantity;
    libraryManager.addBook(bookId, quantity);
    int actualQuantity = libraryManager.getAvailableCopies(bookId);
    assertEquals(expectedQuantity, actualQuantity,
        String.format(
            "Для bookId \"%s\" ожидается %d копий, но получено %d",
            bookId, expectedQuantity, actualQuantity)
    );
    quantity *= -2;
    expectedQuantity += quantity;
    libraryManager.addBook(bookId, quantity);
    actualQuantity = libraryManager.getAvailableCopies(bookId);
    assertEquals(expectedQuantity, actualQuantity,
        String.format(
            "Для bookId \"%s\" ожидается %d копий, но получено %d",
            bookId, expectedQuantity, actualQuantity)
    );
    quantity = 0;
    libraryManager.addBook(bookId, quantity);
    actualQuantity = libraryManager.getAvailableCopies(bookId);
    assertEquals(expectedQuantity, actualQuantity,
        String.format(
            "Для bookId \"%s\" ожидается %d копий, но получено %d",
            bookId, expectedQuantity, actualQuantity)
    );
  }

  @Test
  @DisplayName("Попытка взять книгу неактивным пользователем")
  void borrowBook_shouldReturnFalse_whenUserIsInactive() {
    String bookId = "bookId";
    String userId = "userId";
    int quantity = 5;
    libraryManager.addBook(bookId, quantity);
    when(userService.isUserActive(userId))
        .thenReturn(false);
    boolean wasBorrowed = libraryManager.borrowBook(bookId, userId);
    assertFalse(wasBorrowed, "Метод должен вернуть false, т.к. пользователь не активен");
    verify(notificationService, times(1))
        .notifyUser(userId, "Your account is not active.");
    verifyNoMoreInteractions(notificationService);
  }

  @Test
  @DisplayName("Попытка взять книгу, которой нет в наличии")
  void borrowBook_shouldReturnFalse_whenBookDoesNotExist() {
    String bookId = "nonExistentBookId";
    String userId = "userId";
    when(userService.isUserActive(userId))
        .thenReturn(true);
    boolean wasBorrowed = libraryManager.borrowBook(bookId, userId);
    assertFalse(wasBorrowed, "Метод должен вернуть false, т.к. книги нет в наличии");
    verify(notificationService, never())
        .notifyUser(anyString(), anyString());
  }

  @Test
  @DisplayName("Попытка взять книгу без доступных копий")
  void borrowBook_shouldReturnFalse_whenNoAvailableCopies() {
    String bookId = "bookId";
    String userId = "userId";
    int zeroQuantity = 0;
    libraryManager.addBook(bookId, zeroQuantity);
    when(userService.isUserActive(userId))
        .thenReturn(true);
    boolean wasBorrowed = libraryManager.borrowBook(bookId, userId);
    assertFalse(wasBorrowed, "Метод должен вернуть false, т.к. нет доступных копий");
    verify(notificationService, never())
        .notifyUser(anyString(), anyString());
  }

  @Test
  @DisplayName("Успешное получение книги активным пользователем")
  void borrowBook_shouldReturnTrue_whenUserIsActiveAndBookIsAvailable() {
    String bookId = "bookId";
    String userId = "userId";
    int quantity = 5;
    libraryManager.addBook(bookId, quantity);
    when(userService.isUserActive(userId))
        .thenReturn(true);
    boolean wasBorrowed = libraryManager.borrowBook(bookId, userId);
    assertTrue(wasBorrowed, "Метод должен вернуть true, т.к. книга доступна и пользователь активен");
    assertEquals(quantity - 1, libraryManager.getAvailableCopies(bookId),
        "Количество доступных копий должно уменьшиться на 1");
    verify(notificationService, times(1))
        .notifyUser(userId, "You have borrowed the book: " + bookId);
  }

  @Test
  @DisplayName("Попытка вернуть книгу, которой пользователь не брал")
  void returnBook_shouldReturnFalse_whenBookWasNotBorrowedByUser() {
    String bookId = "bookId";
    String userId = "userId";
    int quantity = 5;
    libraryManager.addBook(bookId, quantity);
    reset(notificationService);
    boolean wasReturned = libraryManager.returnBook(bookId, userId);
    assertFalse(wasReturned, "Метод должен вернуть false, т.к. книга не была взята пользователем");
    verify(notificationService, never())
        .notifyUser(anyString(), anyString());
  }

  @Test
  @DisplayName("Попытка вернуть книгу, взятую другим пользователем")
  void returnBook_shouldReturnFalse_whenBookWasBorrowedByAnotherUser() {
    String bookId = "bookId";
    String userId = "userId";
    String otherUserId = "otherUserId";
    int quantity = 5;
    libraryManager.addBook(bookId, quantity);
    when(userService.isUserActive(otherUserId))
        .thenReturn(true);
    libraryManager.borrowBook(bookId, otherUserId);
    reset(notificationService);
    boolean wasReturned = libraryManager.returnBook(bookId, userId);
    assertFalse(wasReturned, "Метод должен вернуть false, т.к. книга была взята другим пользователем");
    verify(notificationService, never())
        .notifyUser(anyString(), anyString());
  }

  @Test
  @DisplayName("Успешное возвращение книги пользователем")
  void returnBook_shouldReturnTrue_whenBookIsBorrowedByUser() {
    String bookId = "bookId";
    String userId = "userId";
    int quantity = 5;
    libraryManager.addBook(bookId, quantity);
    when(userService.isUserActive(userId))
        .thenReturn(true);
    libraryManager.borrowBook(bookId, userId);
    reset(notificationService);
    boolean wasReturned = libraryManager.returnBook(bookId, userId);
    assertTrue(wasReturned, "Метод должен вернуть true, т.к. книга успешно возвращена пользователем");
    assertEquals(quantity, libraryManager.getAvailableCopies(bookId),
        "Количество доступных копий не должно отличаться от исходного");
    verify(notificationService, times(1))
        .notifyUser(userId, "You have returned the book: " + bookId);
  }

  @ParameterizedTest(name = "calculateDynamicLateFee({0}, {1}, {2}) should throw IllegalArgumentException")
  @CsvSource({
      "-1, false, false, 0.50",
      "-2, false, true, 1.00",
      "-3, true, false, 1.50",
      "-4, true, true, 2.00",
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

