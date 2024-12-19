package ru.hh.school.unittesting.example.base;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hh.school.unittesting.example.MethodsEnvironment;

@ExtendWith(MockitoExtension.class)
public abstract class MockitoBaseTest extends MethodsEnvironment {
}