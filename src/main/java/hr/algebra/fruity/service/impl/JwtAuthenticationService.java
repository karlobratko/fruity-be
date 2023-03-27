package hr.algebra.fruity.service.impl;

import hr.algebra.fruity.dto.request.LoginRequestDto;
import hr.algebra.fruity.dto.request.RegisterRequestDto;
import hr.algebra.fruity.dto.request.ResendRegistrationRequestDto;
import hr.algebra.fruity.dto.response.AuthenticationResponseDto;
import hr.algebra.fruity.dto.response.FullEmployeeResponseDto;
import hr.algebra.fruity.dto.response.RegistrationTokenResponseDto;
import hr.algebra.fruity.exception.InvalidRegistrationTokenException;
import hr.algebra.fruity.exception.RegistrationTokenAlreadyConfirmedException;
import hr.algebra.fruity.exception.RegistrationTokenExpiredException;
import hr.algebra.fruity.exception.UsernameNotFoundException;
import hr.algebra.fruity.model.Employee;
import hr.algebra.fruity.model.RegistrationToken;
import hr.algebra.fruity.model.User;
import hr.algebra.fruity.repository.EmployeeRepository;
import hr.algebra.fruity.repository.RegistrationTokenRepository;
import hr.algebra.fruity.repository.UserRepository;
import hr.algebra.fruity.service.AuthenticationService;
import hr.algebra.fruity.service.EmailComposerService;
import hr.algebra.fruity.service.EmailSenderService;
import hr.algebra.fruity.service.JwtTokenService;
import jakarta.transaction.Transactional;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtAuthenticationService implements AuthenticationService {

  private final ConversionService conversionService;

  private final AuthenticationManager authenticationManager;

  private final JwtTokenService jwtTokenService;

  private final UserRepository userRepository;

  private final RegistrationTokenRepository registrationTokenRepository;

  private final EmployeeRepository employeeRepository;

  private final EmailComposerService emailComposerService;

  private final EmailSenderService emailSenderService;

  @Override
  @Transactional
  public FullEmployeeResponseDto register(RegisterRequestDto requestDto) {
    val user = userRepository.save(
      Objects.requireNonNull(conversionService.convert(requestDto, User.class))
    );

    val registrationToken = registrationTokenRepository.save(RegistrationToken.builder().build());

    val employee = Objects.requireNonNull(conversionService.convert(requestDto, Employee.class));
    employee.setUser(user);
    employee.setRegistrationToken(registrationToken);
    employeeRepository.save(employee);

    composeAndSendConfirmRegistrationEmail(employee, registrationToken, requestDto.confirmRegistrationUrl());

    return conversionService.convert(employee, FullEmployeeResponseDto.class);
  }

  @Override
  @Transactional
  public RegistrationTokenResponseDto confirmRegistration(UUID uuid) {
    val registrationToken = getRegistrationTokenAndValidateIfConfirmed(uuid);

    if (registrationToken.isExpired())
      throw new RegistrationTokenExpiredException();

    registrationToken.confirm();
    registrationTokenRepository.save(registrationToken);

    val employee = registrationToken.getEmployee();
    employee.activate();
    employeeRepository.save(employee);

    return conversionService.convert(registrationToken, RegistrationTokenResponseDto.class);
  }

  @Override
  @Transactional
  public RegistrationTokenResponseDto resendRegistrationToken(UUID uuid, ResendRegistrationRequestDto requestDto) {
    val registrationToken = getRegistrationTokenAndValidateIfConfirmed(uuid);

    registrationToken.reset();
    registrationTokenRepository.save(registrationToken);

    composeAndSendConfirmRegistrationEmail(registrationToken.getEmployee(), registrationToken, requestDto.confirmRegistrationUrl());

    return conversionService.convert(registrationToken, RegistrationTokenResponseDto.class);
  }

  @Override
  @Transactional
  public AuthenticationResponseDto login(LoginRequestDto requestDto) {
    try {
      val authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
          requestDto.username(),
          requestDto.password()
        )
      );

      val employee = employeeRepository.findByUsername(authentication.getName())
        .orElseThrow(() -> new UsernameNotFoundException(authentication.getName()));

      return conversionService.convert(jwtTokenService.generate(employee), AuthenticationResponseDto.class);
    } catch (AuthenticationException e) {
      employeeRepository.findByUsername(requestDto.username())
        .ifPresentOrElse(
          employee -> {
            if (!employee.isEnabled())
              throw new DisabledException("Korisnički račun nije omogućen.");

            if (employee.isLocked())
              throw new LockedException("Korisnički račun je zaključan.");
          },
          () -> {
            throw new BadCredentialsException("Nevažeće korisničko ime i lozinka.");
          }
        );

      throw new IllegalStateException();
    }
  }

  private void composeAndSendConfirmRegistrationEmail(Employee employee, RegistrationToken registrationToken, String registrationUrl) {
    emailSenderService.send(
      emailComposerService.composeConfirmRegistrationEmail(
        employee,
        registrationUrl,
        registrationToken.getUuid()
      )
    );
  }

  private RegistrationToken getRegistrationTokenAndValidateIfConfirmed(UUID uuid) {
    val registrationToken = registrationTokenRepository.findByUuid(uuid)
      .orElseThrow(InvalidRegistrationTokenException::new);

    if (registrationToken.isConfirmed())
      throw new RegistrationTokenAlreadyConfirmedException();
    return registrationToken;
  }


}
