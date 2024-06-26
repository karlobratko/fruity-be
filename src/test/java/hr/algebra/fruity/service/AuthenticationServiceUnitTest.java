package hr.algebra.fruity.service;

import hr.algebra.fruity.converter.EmployeeToAuthenticationResponseDtoConverter;
import hr.algebra.fruity.converter.RegisterRequestDtoToEmployeeConverter;
import hr.algebra.fruity.converter.RegisterRequestDtoToUserConverter;
import hr.algebra.fruity.converter.RegistrationTokenToRegistrationTokenResponseDtoConverter;
import hr.algebra.fruity.model.Email;
import hr.algebra.fruity.model.Employee;
import hr.algebra.fruity.repository.EmployeeRepository;
import hr.algebra.fruity.repository.UserRepository;
import hr.algebra.fruity.service.impl.JwtAuthenticationService;
import hr.algebra.fruity.utils.mother.dto.AuthenticationResponseDtoMother;
import hr.algebra.fruity.utils.mother.dto.ConfirmRegistrationRequestDtoMother;
import hr.algebra.fruity.utils.mother.dto.LoginMobileRequestDtoMother;
import hr.algebra.fruity.utils.mother.dto.LoginRequestDtoMother;
import hr.algebra.fruity.utils.mother.dto.RefreshTokenRequestDtoMother;
import hr.algebra.fruity.utils.mother.dto.RegisterRequestDtoMother;
import hr.algebra.fruity.utils.mother.dto.RegistrationTokenResponseDtoMother;
import hr.algebra.fruity.utils.mother.dto.ResendRegistrationRequestDtoMother;
import hr.algebra.fruity.utils.mother.model.EmailMother;
import hr.algebra.fruity.utils.mother.model.EmployeeMother;
import hr.algebra.fruity.utils.mother.model.MobileTokenMother;
import hr.algebra.fruity.utils.mother.model.RefreshTokenMother;
import hr.algebra.fruity.utils.mother.model.RegistrationTokenMother;
import hr.algebra.fruity.utils.mother.model.UserMother;
import hr.algebra.fruity.validator.RegisterRequestDtoValidator;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static com.googlecode.catchexception.apis.BDDCatchException.caughtException;
import static com.googlecode.catchexception.apis.BDDCatchException.when;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Unit Test")
@SuppressWarnings("static-access")
class AuthenticationServiceUnitTest implements ServiceUnitTest {

  @InjectMocks
  private JwtAuthenticationService authenticationService;

  @Mock
  private RegisterRequestDtoToUserConverter fromRegisterRequestDtoToUserConverter;

  @Mock
  private RegisterRequestDtoToEmployeeConverter fromRegisterRequestDtoToEmployeeConverter;

  @Mock
  private RegistrationTokenToRegistrationTokenResponseDtoConverter toRegistrationTokenResponseDtoConverter;

  @Mock
  private EmployeeToAuthenticationResponseDtoConverter toAuthenticationResponseDtoConverter;

  @Mock
  private RegisterRequestDtoValidator registerRequestDtoValidator;

  @Mock
  private AuthenticationManager authenticationManager;

  @Mock
  private UserRepository userRepository;

  @Mock
  private EmployeeRepository employeeRepository;

  @Mock
  private EmailComposerService emailComposerService;

  @Mock
  private EmailSenderService emailSenderService;

  @Mock
  private RegistrationTokenService registrationTokenService;

  @Mock
  private RefreshTokenService refreshTokenService;

  @Mock
  private MobileTokenService mobileTokenService;

  @Nested
  @DisplayName("... WHEN register is called")
  public class WHEN_register {

    @Test
    @DisplayName("GIVEN RegisterRequestDto " +
      "... THEN email sent")
    public void GIVEN_RegisterRequestDto_THEN_FullEmployeeResponseDto() {
      // GIVEN
      // ... RegisterRequestDto
      val requestDto = RegisterRequestDtoMother.complete().build();
      // ... RegisterRequestDtoValidator successfully validates
      willDoNothing().given(registerRequestDtoValidator).validate(requestDto);
      // ... RegisterRequestDtoToUserConverter successfully converts
      val user = UserMother.complete().build();
      given(fromRegisterRequestDtoToUserConverter.convert(same(requestDto))).willReturn(user);
      // ... UserRepository successfully saves
      given(userRepository.save(same(user))).willReturn(user);
      // ... ConversionService successfully converts
      val employee = EmployeeMother.complete().user(null).registrationToken(null).build();
      given(fromRegisterRequestDtoToEmployeeConverter.convert(same(requestDto))).willReturn(employee);
      // ... RegistrationTokenRepository successfully saves
      val registrationToken = RegistrationTokenMother.complete().build();
      given(registrationTokenService.createRegistrationToken()).willReturn(registrationToken);
      // ... RefreshTokenService successfully saves
      val refreshToken = RefreshTokenMother.complete().build();
      given(refreshTokenService.createRefreshToken()).willReturn(refreshToken);
      // ... MobileTokenService successfully saves
      val mobileToken = MobileTokenMother.complete().build();
      given(mobileTokenService.createMobileToken()).willReturn(mobileToken);
      // ... EmployeeRepository successfully saves
      given(employeeRepository.save(same(employee))).willReturn(employee);
      // ... EmailComposerService successfully composes Email
      val email = EmailMother.complete().build();
      given(emailComposerService.composeConfirmRegistrationEmail(same(employee), same(requestDto.confirmRegistrationUrl()), same(registrationToken.getUuid()))).willReturn(email);
      // ... EmailSenderService successfully sends email
      willDoNothing().given(emailSenderService).send(same(email));

      // WHEN
      // ... register is called
      authenticationService.register(requestDto);

      // THEN
      // ... FullEmployeeResponseDto is returned
      then(emailComposerService).should(times(1)).composeConfirmRegistrationEmail(any(Employee.class), anyString(), any(UUID.class));
      then(emailSenderService).should(times(1)).send(any(Email.class));
      and.then(employee).satisfies(it -> {
        and.then(it.getUser()).isNotNull();
        and.then(it.getRegistrationToken()).isNotNull();
      });
    }

  }

  @Nested
  @DisplayName("... WHEN confirmRegistration is called")
  public class WHEN_confirmRegistration {

    @Test
    @DisplayName("GIVEN ConfirmRegistrationRequestDto " +
      "... THEN RegistrationTokenResponseDto is returned")
    public void GIVEN_UUID_THEN_RegistrationTokenResponseDto() {
      // GIVEN
      // ... UUID
      val requestDto = ConfirmRegistrationRequestDtoMother.complete().build();
      // ... RegistrationTokenService successfully confirms RegistrationToken
      val employee = EmployeeMother.complete().enabled(false).locked(true).build();
      val registrationToken = RegistrationTokenMother.complete()
        .employee(employee)
        .build();
      given(registrationTokenService.confirmRegistrationToken(requestDto.registrationToken())).willReturn(registrationToken);
      // ... EmployeeRepository successfully saves
      given(employeeRepository.save(same(employee))).willReturn(employee);
      // ... RegistrationTokenToRegistrationTokenResponseDtoConverter successfully converts
      val expectedResponseDto = RegistrationTokenResponseDtoMother.complete().build();
      given(toRegistrationTokenResponseDtoConverter.convert(same(registrationToken))).willReturn(expectedResponseDto);

      // WHEN
      // ... confirmRegistration is called
      val responseDto = authenticationService.confirmRegistration(requestDto);

      // THEN
      // ... RegistrationTokenResponseDto is returned
      and.then(employee).satisfies(it -> {
        and.then(it.isEnabled()).isTrue();
        and.then(it.isLocked()).isFalse();
      });
      and.then(responseDto)
        .isNotNull()
        .isEqualTo(expectedResponseDto);
    }

  }

  @Nested
  @DisplayName("... WHEN resendRegistrationToken is called")
  public class WHEN_resendRegistrationToken {

    @Test
    @DisplayName("GIVEN ResendRegistrationRequestDto" +
      "... THEN RegistrationTokenResponseDto is returned")
    public void GIVEN_UUID_THEN_RegistrationTokenResponseDto() {
      // GIVEN
      // ... ResendRegistrationRequestDto
      val requestDto = ResendRegistrationRequestDtoMother.complete().build();
      // ... RegistrationTokenService successfully refreshes RegistrationToken
      val employee = EmployeeMother.complete().build();
      val registrationToken = RegistrationTokenMother.complete()
        .employee(employee)
        .createDateTime(LocalDateTime.now().minusMinutes(30))
        .expireDateTime(LocalDateTime.now().minusMinutes(15))
        .build();
      given(registrationTokenService.refreshRegistrationToken(requestDto.registrationToken())).willReturn(registrationToken);
      // ... EmailComposerService successfully composes Email
      val email = EmailMother.complete().build();
      given(emailComposerService.composeConfirmRegistrationEmail(same(employee), same(requestDto.confirmRegistrationUrl()), same(registrationToken.getUuid()))).willReturn(email);
      // ... EmailSenderService successfully sends email
      willDoNothing().given(emailSenderService).send(same(email));
      // ... RegistrationTokenToRegistrationTokenResponseDtoConverter successfully converts
      val expectedResponseDto = RegistrationTokenResponseDtoMother.complete().build();
      given(toRegistrationTokenResponseDtoConverter.convert(same(registrationToken))).willReturn(expectedResponseDto);

      // WHEN
      // ... resendRegistrationToken is called
      val responseDto = authenticationService.resendRegistrationToken(requestDto);

      // THEN
      // ... RegistrationTokenResponseDto is returned
      and.then(responseDto)
        .isNotNull()
        .isEqualTo(expectedResponseDto);
    }

  }

  @Nested
  @DisplayName("... WHEN login is called")
  public class WHEN_login {

    @Test
    @DisplayName("GIVEN invalid LoginRequestDto " +
      "... THEN BadCredentialsException is thrown")
    public void GIVEN_invalidLoginRequestDto_THEN_BadCredentialsException() {
      // GIVEN
      // ... invalid LoginRequestDto
      val requestDto = LoginRequestDtoMother.complete().build();
      // ... AuthenticationManager fails to authenticate UsernamePasswordAuthenticationToken
      val token = new UsernamePasswordAuthenticationToken(requestDto.username(), requestDto.password());
      given(authenticationManager.authenticate(token)).willThrow(BadCredentialsException.class);
      // ... EmployeeRepository fails to find Employee by username
      val username = requestDto.username();
      given(employeeRepository.findByUsername(same(username))).willReturn(Optional.empty());

      // WHEN
      // ... login is called
      when(() -> authenticationService.login(requestDto));

      // THEN
      // ... BadCredentialsException is thrown
      and.then(caughtException())
        .isInstanceOf(BadCredentialsException.class)
        .hasNoCause();
    }

    @Test
    @DisplayName("GIVEN LoginRequestDto and disabled Employee " +
      "... THEN DisabledException is thrown")
    public void GIVEN_LoginRequestDtoAndDisabledEmployee_THEN_DisabledException() {
      // GIVEN
      // ... invalid LoginRequestDto
      val requestDto = LoginRequestDtoMother.complete().build();
      // ... AuthenticationManager fails to authenticate UsernamePasswordAuthenticationToken
      val token = new UsernamePasswordAuthenticationToken(requestDto.username(), requestDto.password());
      given(authenticationManager.authenticate(token)).willThrow(DisabledException.class);
      // ... EmployeeRepository fails to findByUsername
      val username = requestDto.username();
      val employee = EmployeeMother.complete().enabled(false).locked(true).build();
      given(employeeRepository.findByUsername(same(username))).willReturn(Optional.of(employee));

      // WHEN
      // ... login is called
      when(() -> authenticationService.login(requestDto));

      // THEN
      // ... DisabledException is thrown
      and.then(caughtException())
        .isInstanceOf(DisabledException.class)
        .hasNoCause();
    }

    @Test
    @DisplayName("GIVEN LoginRequestDto and locked Employee " +
      "... THEN LockedException is thrown")
    public void GIVEN_LoginRequestDtoAndLockedEmployee_THEN_LockedException() {
      // GIVEN
      // ... invalid LoginRequestDto
      val requestDto = LoginRequestDtoMother.complete().build();
      // ... AuthenticationManager fails to authenticate UsernamePasswordAuthenticationToken
      val token = new UsernamePasswordAuthenticationToken(requestDto.username(), requestDto.password());
      given(authenticationManager.authenticate(token)).willThrow(DisabledException.class);
      // ... EmployeeRepository fails to findByUsername
      val username = requestDto.username();
      val employee = EmployeeMother.complete().enabled(true).locked(true).build();
      given(employeeRepository.findByUsername(same(username))).willReturn(Optional.of(employee));

      // WHEN
      // ... login is called
      when(() -> authenticationService.login(requestDto));

      // THEN
      // ... LockedException is thrown
      and.then(caughtException())
        .isInstanceOf(LockedException.class)
        .hasNoCause();
    }

    @Test
    @DisplayName("GIVEN LoginRequestDto " +
      "... THEN AuthenticationResponseDto is returned")
    public void GIVEN_LoginRequestDto_THEN_AuthenticationResponseDto() {
      // GIVEN
      // ... LoginRequestDto
      val requestDto = LoginRequestDtoMother.complete().build();
      // ... AuthenticationManager successfully authenticates UsernamePasswordAuthenticationToken
      val token = new UsernamePasswordAuthenticationToken(requestDto.username(), requestDto.password());
      given(authenticationManager.authenticate(token)).willReturn(token);
      // ... EmployeeRepository successfully findByUsername
      val employee = EmployeeMother.complete().build();
      given(employeeRepository.findByUsername(same(token.getName()))).willReturn(Optional.of(employee));
      // ... RefreshTokenService successfully refreshes RefreshToken
      val refreshToken = RefreshTokenMother.complete().build();
      given(refreshTokenService.refreshRefreshToken(same(employee.getRefreshToken().getUuid()))).willReturn(refreshToken);
      // ... EmployeeToAuthenticationResponseDtoConverter successfully converts
      val expectedResponseDto = AuthenticationResponseDtoMother.complete().build();
      given(toAuthenticationResponseDtoConverter.convert(same(employee))).willReturn(expectedResponseDto);

      // WHEN
      // ... login is called
      val responseDto = authenticationService.login(requestDto);

      // THEN
      // ... AuthenticationResponseDto is returned
      and.then(responseDto)
        .isNotNull()
        .isEqualTo(expectedResponseDto);
    }

  }

  @Nested
  @DisplayName("... WHEN loginMobile is called")
  public class WHEN_loginMobile {

    @Test
    @DisplayName("GIVEN LoginMobileRequestDto " +
      "... THEN AuthenticationResponseDto is returned")
    public void GIVEN_LoginMobileRequestDto_THEN_AuthenticationResponseDto() {
      // GIVEN
      // ... LoginMobileRequestDto
      val requestDto = LoginMobileRequestDtoMother.complete().build();
      // ... MobileTokenService successfully verifies mobile
      val mobileToken = MobileTokenMother.complete().build();
      given(mobileTokenService.verifyMobileToken(same(requestDto.mobileToken()))).willReturn(mobileToken);
      // ... ConversionService successfully converts
      val expectedResponseDto = AuthenticationResponseDtoMother.complete().build();
      // ... EmployeeToAuthenticationResponseDtoConverter successfully converts
      given(toAuthenticationResponseDtoConverter.convert(same(mobileToken.getEmployee()))).willReturn(expectedResponseDto);

      // WHEN
      // ... loginMobile is called
      val responseDto = authenticationService.loginMobile(requestDto);

      // THEN
      // ... AuthenticationResponseDto is returned
      and.then(responseDto)
        .isNotNull()
        .isEqualTo(expectedResponseDto);
    }

  }

  @Nested
  @DisplayName("... WHEN refreshToken is called")
  public class WHEN_refreshToken {

    @Test
    @DisplayName("GIVEN RefreshTokenRequestDto " +
      "... THEN AuthenticationResponseDto")
    public void GIVEN_RefreshTokenRequestDto_THEN_AuthenticationResponseDto() {
      // GIVEN
      // ... RefreshTokenRequestDto
      val requestDto = RefreshTokenRequestDtoMother.complete().build();
      // RefreshTokenService successfully verifies RefreshToken by UUID
      val refreshToken = RefreshTokenMother.complete().build();
      given(refreshTokenService.verifyRefreshToken(same(requestDto.refreshToken()))).willReturn(refreshToken);
      // ... EmployeeToAuthenticationResponseDtoConverter successfully converts
      val expectedResponseDto = AuthenticationResponseDtoMother.complete().build();
      given(toAuthenticationResponseDtoConverter.convert(same(refreshToken.getEmployee()))).willReturn(expectedResponseDto);

      // WHEN
      // ... refreshToken is called
      val responseDto = authenticationService.refreshToken(requestDto);

      // THEN
      // ... AuthenticationResponseDto
      and.then(responseDto)
        .isNotNull()
        .isEqualTo(expectedResponseDto);
    }

  }

}