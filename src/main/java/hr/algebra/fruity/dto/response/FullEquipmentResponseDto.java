package hr.algebra.fruity.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
public record FullEquipmentResponseDto(
  Long id,
  String name,
  Integer productionYear,
  BigDecimal costPerHour,
  BigDecimal purchasePrice,
  List<AttachmentResponseDto> compatibleAttachments
) {

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Fields {

    public static final String id = "id";

    public static final String name = "name";

    public static final String productionYear = "productionYear";

    public static final String costPerHour = "costPerHour";

    public static final String purchasePrice = "purchasePrice";

    public static final String compatibleAttachments = "compatibleAttachments";

  }

}