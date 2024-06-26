package hr.algebra.fruity.service;

import hr.algebra.fruity.converter.UserToFullUserResponseDtoConverter;
import hr.algebra.fruity.mapper.UserMapper;
import hr.algebra.fruity.repository.UserRepository;
import hr.algebra.fruity.service.impl.CurrentUserUserService;
import hr.algebra.fruity.utils.mother.dto.FullUserResponseDtoMother;
import hr.algebra.fruity.utils.mother.dto.UpdateUserRequestDtoMother;
import hr.algebra.fruity.utils.mother.model.UserMother;
import hr.algebra.fruity.validator.UserWithUpdateUserRequestDtoValidator;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Unit Test")
@SuppressWarnings("static-access")
public class UserServiceUnitTest implements ServiceUnitTest {

  @InjectMocks
  private CurrentUserUserService userService;

  @Mock
  private UserToFullUserResponseDtoConverter toFullUserResponseDtoConverter;

  @Mock
  private UserWithUpdateUserRequestDtoValidator userWithUpdateUserRequestDtoValidator;

  @Mock
  private UserMapper userMapper;

  @Mock
  private CurrentRequestUserService currentRequestUserService;

  @Mock
  private UserRepository userRepository;

  @Nested
  @DisplayName("WHEN getCurrentUser is called")
  public class WHEN_getCurrentUser {

    @Test
    @DisplayName("GIVEN void " +
      "... THEN FullUserResponseDto is returned")
    public void GIVEN_void_THEN_FullUserResponseDto() {
      // GIVEN
      // ... CurrentUserService's logged-in User is equal to User
      val user = UserMother.complete().build();
      given(currentRequestUserService.getUser()).willReturn(user);
      // ... UserToFullUserResponseDtoConverter successfully converts
      val expectedResponseDto = FullUserResponseDtoMother.complete().build();
      given(toFullUserResponseDtoConverter.convert(same(user))).willReturn(expectedResponseDto);

      // WHEN
      // ... getUserById is called
      val responseDto = userService.getCurrentUser();

      // THEN
      // ... FullUserResponseDto is returned
      and.then(responseDto)
        .isNotNull()
        .isEqualTo(expectedResponseDto);
    }

  }

  @Nested
  @DisplayName("WHEN updateCurrentUser is called")
  public class WHEN_updateCurrentUser {

    @Test
    @DisplayName("GIVEN ReplaceUserRequestDto and unique oib " +
      "... THEN FullUserResponseDto is returned")
    public void GIVEN_ReplaceUserRequestDtoAndUniqueOib_THEN_FullUserResponseDto() {
      // GIVEN
      // ... ReplaceUserRequestDto
      val requestDto = UpdateUserRequestDtoMother.complete().build();
      // ... CurrentUserService's logged-in User is equal to User
      val user = UserMother.complete().build();
      given(currentRequestUserService.getUser()).willReturn(user);
      // ... UserWithUpdateUserRequestDtoValidator successfully validates User with ReplaceUserRequestDto
      willDoNothing().given(userWithUpdateUserRequestDtoValidator).validate(same(user), same(requestDto));
      // ... UserMapper successfully partially updates User with ReplaceUserRequestDto
      given(userMapper.partialUpdate(same(user), same(requestDto))).willReturn(user);
      // ... UserRepository successfully saves User
      given(userRepository.save(same(user))).willReturn(user);
      // ... UserToFullUserResponseDtoConverter successfully converts
      val expectedResponseDto = FullUserResponseDtoMother.complete().build();
      given(toFullUserResponseDtoConverter.convert(same(user))).willReturn(expectedResponseDto);

      // WHEN
      // ... updateCurrentUser is called
      val responseDto = userService.updateCurrentUser(requestDto);

      // THEN
      // ... FullUserResponseDto is returned
      and.then(responseDto)
        .isNotNull()
        .isEqualTo(expectedResponseDto);
    }

  }

}
