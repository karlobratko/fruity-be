package hr.algebra.fruity.service.impl;

import hr.algebra.fruity.exception.InvalidRegistrationTokenException;
import hr.algebra.fruity.exception.RegistrationTokenAlreadyConfirmedException;
import hr.algebra.fruity.exception.RegistrationTokenExpiredException;
import hr.algebra.fruity.model.RegistrationToken;
import hr.algebra.fruity.properties.RegistrationTokenProperties;
import hr.algebra.fruity.repository.RegistrationTokenRepository;
import hr.algebra.fruity.service.RegistrationTokenService;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrationTokenServiceImpl implements RegistrationTokenService {

  private final RegistrationTokenProperties registrationTokenProperties;

  private final RegistrationTokenRepository registrationTokenRepository;

  @Override
  public RegistrationToken createRegistrationToken() {
    val token = RegistrationToken.builder().build();
    initializeRegistrationToken(token);

    return registrationTokenRepository.save(token);
  }

  @Override
  public RegistrationToken confirmRegistrationToken(UUID uuid) {
    val registrationToken = getRegistrationTokenAndValidateIfConfirmed(uuid);

    if (registrationToken.isExpired())
      throw new RegistrationTokenExpiredException();

    registrationToken.confirm();

    return registrationTokenRepository.save(registrationToken);
  }

  @Override
  public RegistrationToken refreshRegistrationToken(UUID uuid) {
    val registrationToken = getRegistrationTokenAndValidateIfConfirmed(uuid);

    initializeRegistrationToken(registrationToken);
    registrationToken.setConfirmDateTime(null);

    return registrationTokenRepository.save(registrationToken);
  }

  private void initializeRegistrationToken(RegistrationToken registrationToken) {
    val now = LocalDateTime.now();
    registrationToken.setCreateDateTime(now);
    registrationToken.setExpireDateTime(now.plusSeconds(registrationTokenProperties.validityDurationInMs().toSeconds()));
  }

  private RegistrationToken getRegistrationTokenAndValidateIfConfirmed(UUID uuid) {
    val registrationToken = registrationTokenRepository.findByUuid(uuid)
      .orElseThrow(InvalidRegistrationTokenException::new);

    if (registrationToken.isConfirmed())
      throw new RegistrationTokenAlreadyConfirmedException();

    return registrationToken;
  }

}
