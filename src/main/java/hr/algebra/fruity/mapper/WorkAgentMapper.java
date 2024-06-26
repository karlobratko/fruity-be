package hr.algebra.fruity.mapper;

import hr.algebra.fruity.dto.request.UpdateWorkAgentRequestDto;
import hr.algebra.fruity.model.UnitOfMeasure;
import hr.algebra.fruity.model.WorkAgent;
import hr.algebra.fruity.service.UnitOfMeasureService;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class WorkAgentMapper {

  @Autowired
  private UnitOfMeasureService unitOfMeasureService;

  @Named(MappingHelpers.mapIdToUnitOfMeasure)
  protected UnitOfMeasure mapIdToUnitOfMeasure(Integer value) {
    return unitOfMeasureService.getById(value);
  }

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
  @Mapping(source = UpdateWorkAgentRequestDto.Fields.agentUnitOfMeasureFk, target = WorkAgent.Fields.agentUnitOfMeasure, qualifiedByName = {MappingHelpers.mapIdToUnitOfMeasure})
  @Mapping(source = UpdateWorkAgentRequestDto.Fields.waterUnitOfMeasureFk, target = WorkAgent.Fields.waterUnitOfMeasure, qualifiedByName = {MappingHelpers.mapIdToUnitOfMeasure})
  public abstract WorkAgent partialUpdate(@MappingTarget WorkAgent workAgent, UpdateWorkAgentRequestDto requestDto);

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class MappingHelpers {

    public static final String mapIdToUnitOfMeasure = "mapIdToUnitOfMeasure";

  }

}