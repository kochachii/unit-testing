package ru.hh.school.unittesting.example.base;

import org.junit.jupiter.params.provider.Arguments;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.hh.school.unittesting.homework.LibraryManager;
import ru.hh.school.unittesting.homework.NotificationService;
import ru.hh.school.unittesting.homework.UserService;

import java.util.stream.Stream;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class LibraryManagerBaseTest extends MockitoBaseTest {

  @Mock
  protected NotificationService notificationService;

  @Mock
  protected UserService userService;

  @InjectMocks
  protected LibraryManager libraryManager;

  protected int addBooksAndGetAvailableCopies(String bookId, int quantity) {
    libraryManager.addBook(bookId, quantity);
    return libraryManager.getAvailableCopies(bookId);
  }

  protected boolean returnBookAsUser(String bookId, String userId) {
    reset(notificationService);
    return libraryManager.returnBook(bookId, userId);
  }

  protected boolean borrowNonExistentBookAsUser(String userId) {
    return borrowBookAsUserWithActivity(NON_EXISTENT_BOOK_ID, userId, Boolean.TRUE);
  }
  
  protected boolean borrowBookAsUser(String bookId, String userId) {
    return borrowBookAsUserWithActivity(bookId, userId, Boolean.TRUE);
  }

  protected boolean borrowBookAsInactiveUser(String bookId) {
    return borrowBookAsUserWithActivity(bookId, DEFAULT_USER_ID, Boolean.FALSE);
  }

  private boolean borrowBookAsUserWithActivity(String bookId, String userId, boolean isActive) {
    when(userService.isUserActive(userId))
        .thenReturn(isActive);
    return libraryManager.borrowBook(bookId, userId);
  }

  protected static Stream<Arguments> provideGetAvailableCopiesTestArguments_normal() {
    return Stream.of(
        Arguments.of(BOOK_ID_1, DEFAULT_QUANTITY, DEFAULT_QUANTITY),
        Arguments.of(BOOK_ID_2, -DEFAULT_QUANTITY, -DEFAULT_QUANTITY),
        Arguments.of(BOOK_ID_3, ZERO_QUANTITY, ZERO_QUANTITY)
    );
  }

  protected static Stream<Arguments> provideGetAvailableCopiesTestArguments_blank() {
    return Stream.of(
        Arguments.of(EMPTY_BOOK_ID, DEFAULT_QUANTITY, DEFAULT_QUANTITY),
        Arguments.of(NULL_BOOK_ID, -DEFAULT_QUANTITY, -DEFAULT_QUANTITY)
    );
  }

  protected static Stream<Arguments> provideCalculateDynamicLateFeeTestArguments_valid() {
    return Stream.of(
        // isBestseller = false, isPremiumMember = false
        Arguments.of(ZERO_DAYS, Boolean.FALSE, Boolean.FALSE,
            round(ZERO_DAYS * BASE_LATE_FEE_PER_DAY)),
        Arguments.of(-ZERO_DAYS, Boolean.FALSE, Boolean.FALSE,
            round(-ZERO_DAYS * BASE_LATE_FEE_PER_DAY)),
        Arguments.of(ONE_DAY, Boolean.FALSE, Boolean.FALSE,
            round(ONE_DAY * BASE_LATE_FEE_PER_DAY)),
        Arguments.of(THREE_DAYS, Boolean.FALSE, Boolean.FALSE,
            round(THREE_DAYS * BASE_LATE_FEE_PER_DAY)),

        // isBestseller = false, isPremiumMember = true
        Arguments.of(ONE_DAY, Boolean.FALSE, Boolean.TRUE,
            round(ONE_DAY * BASE_LATE_FEE_PER_DAY * PREMIUM_MEMBER_DISCOUNT)),
        Arguments.of(THREE_DAYS, Boolean.FALSE, Boolean.TRUE,
            round(THREE_DAYS * BASE_LATE_FEE_PER_DAY * PREMIUM_MEMBER_DISCOUNT)),

        // isBestseller = true, isPremiumMember = false
        Arguments.of(ONE_DAY, Boolean.TRUE, Boolean.FALSE,
            round(ONE_DAY * BASE_LATE_FEE_PER_DAY * BESTSELLER_MULTIPLIER)),
        Arguments.of(THREE_DAYS, Boolean.TRUE, Boolean.FALSE,
            round(THREE_DAYS * BASE_LATE_FEE_PER_DAY * BESTSELLER_MULTIPLIER)),

        // isBestseller = true, isPremiumMember = true
        Arguments.of(ONE_DAY, Boolean.TRUE, Boolean.TRUE,
            round(ONE_DAY * BASE_LATE_FEE_PER_DAY * BESTSELLER_MULTIPLIER * PREMIUM_MEMBER_DISCOUNT)),
        Arguments.of(THREE_DAYS, Boolean.TRUE, Boolean.TRUE,
            round(THREE_DAYS * BASE_LATE_FEE_PER_DAY * BESTSELLER_MULTIPLIER * PREMIUM_MEMBER_DISCOUNT))
    );
  }

  protected static Stream<Arguments> provideCalculateDynamicLateFeeTestArguments_invalid() {
    return Stream.of(
        Arguments.of(-ONE_DAY, Boolean.FALSE, Boolean.FALSE),
        Arguments.of(-THREE_DAYS, Boolean.FALSE, Boolean.TRUE),
        Arguments.of(-ONE_DAY, Boolean.TRUE, Boolean.FALSE),
        Arguments.of(-THREE_DAYS, Boolean.TRUE, Boolean.TRUE)
    );
  }

  protected Object[][] addBooks_getDefaultTestCases() {
    return new Object[][]{
        {BOOK_ID_1, DEFAULT_QUANTITY, DEFAULT_QUANTITY},
        {BOOK_ID_2, -DEFAULT_QUANTITY, -DEFAULT_QUANTITY},
        {BOOK_ID_3, ZERO_QUANTITY, ZERO_QUANTITY}
    };
  }

  protected Object[][] addBooks_getTestCasesForExistentBook(String bookId) {
    return new Object[][]{
        {bookId, DEFAULT_QUANTITY, DEFAULT_QUANTITY},
        {bookId, -DEFAULT_QUANTITY * 2, -DEFAULT_QUANTITY},
        {bookId, ZERO_QUANTITY, -DEFAULT_QUANTITY}
    };
  }
}
