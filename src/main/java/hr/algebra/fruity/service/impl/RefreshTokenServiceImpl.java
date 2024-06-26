package hr.algebra.fruity.service.impl;

import hr.algebra.fruity.exception.InvalidRefreshTokenException;
import hr.algebra.fruity.exception.RefreshTokenExpiredException;
import hr.algebra.fruity.model.RefreshToken;
import hr.algebra.fruity.properties.JwtProperties;
import hr.algebra.fruity.repository.RefreshTokenRepository;
import hr.algebra.fruity.service.RefreshTokenService;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

  private final JwtProperties jwtProperties;

  private final RefreshTokenRepository refreshTokenRepository;

  @Override
  @Transactional
  public RefreshToken createRefreshToken() {
    val refreshToken = RefreshToken.builder().build();
    initializeRefreshToken(refreshToken);

    return refreshTokenRepository.save(refreshToken);
  }

  @Override
  @Transactional
  public RefreshToken refreshRefreshToken(UUID uuid) {
    val refreshToken = getRefreshTokenByUUID(uuid);
    initializeRefreshToken(refreshToken);

    return refreshTokenRepository.save(refreshToken);
  }

  @Override
  public RefreshToken verifyRefreshToken(UUID uuid) {
    val refreshToken = getRefreshTokenByUUID(uuid);

    if (refreshToken.isExpired())
      throw new RefreshTokenExpiredException();

    return refreshToken;
  }

  private void initializeRefreshToken(RefreshToken refreshToken) {
    val now = LocalDateTime.now();
    refreshToken.setExpireDateTime(now.plusSeconds(jwtProperties.refreshValidityDurationInMs().getSeconds()));
  }

  @Override
  public RefreshToken getRefreshTokenByUUID(UUID uuid) {
    return refreshTokenRepository.findByUuid(uuid)
      .orElseThrow(InvalidRefreshTokenException::new);
  }

}
