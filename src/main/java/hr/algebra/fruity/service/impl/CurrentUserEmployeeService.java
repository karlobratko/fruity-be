package hr.algebra.fruity.service.impl;

import hr.algebra.fruity.converter.CreateEmployeeRequestDtoToEmployeeConverter;
import hr.algebra.fruity.converter.EmployeeToEmployeeResponseDtoConverter;
import hr.algebra.fruity.converter.EmployeeToFullEmployeeResponseDtoConverter;
import hr.algebra.fruity.converter.WorkToWorkResponseDtoConverter;
import hr.algebra.fruity.dto.request.CreateEmployeeRequestDto;
import hr.algebra.fruity.dto.request.UpdateEmployeeRequestDto;
import hr.algebra.fruity.dto.response.EmployeeResponseDto;
import hr.algebra.fruity.dto.response.FullEmployeeResponseDto;
import hr.algebra.fruity.dto.response.WorkResponseDto;
import hr.algebra.fruity.exception.EntityNotFoundException;
import hr.algebra.fruity.exception.ManagerEmployeeDeleteException;
import hr.algebra.fruity.mapper.EmployeeMapper;
import hr.algebra.fruity.model.Employee;
import hr.algebra.fruity.model.codebook.EmployeeRoles;
import hr.algebra.fruity.repository.EmployeeRepository;
import hr.algebra.fruity.repository.RealisationRepository;
import hr.algebra.fruity.repository.WorkEmployeeRepository;
import hr.algebra.fruity.repository.WorkRepository;
import hr.algebra.fruity.service.CurrentRequestUserService;
import hr.algebra.fruity.service.EmployeeRoleService;
import hr.algebra.fruity.service.EmployeeService;
import hr.algebra.fruity.service.MobileTokenService;
import hr.algebra.fruity.service.RefreshTokenService;
import hr.algebra.fruity.validator.CreateEmployeeRequestDtoValidator;
import hr.algebra.fruity.validator.EmployeeWithUpdateEmployeeRequestDtoValidator;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.hibernate.Session;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserEmployeeService implements EmployeeService {

  private final EmployeeToEmployeeResponseDtoConverter toEmployeeResponseDtoConverter;

  private final EmployeeToFullEmployeeResponseDtoConverter toFullEmployeeResponseDtoConverter;

  private final CreateEmployeeRequestDtoToEmployeeConverter fromCreateEmployeeRequestDtoConverter;

  private final WorkToWorkResponseDtoConverter toWorkResponseDtoConverter;

  private final CreateEmployeeRequestDtoValidator createEmployeeRequestDtoValidator;

  private final EmployeeWithUpdateEmployeeRequestDtoValidator employeeWithUpdateEmployeeRequestDtoValidator;

  private final EmployeeMapper employeeMapper;

  private final CurrentRequestUserService currentRequestUserService;

  private final EmployeeRepository employeeRepository;

  private final RefreshTokenService refreshTokenService;

  private final MobileTokenService mobileTokenService;

  private final EmployeeRoleService employeeRoleService;

  private final WorkRepository workRepository;

  private final WorkEmployeeRepository workEmployeeRepository;

  private final RealisationRepository realisationRepository;

  private final EntityManager entityManager;

  @Override
  public List<EmployeeResponseDto> getAllEmployees() {
    val session = entityManager.unwrap(Session.class);
    session.enableFilter("isNotDeleted");

    val employees = employeeRepository.findAllByUserId(currentRequestUserService.getUserId()).stream()
      .map(toEmployeeResponseDtoConverter::convert)
      .toList();

    session.disableFilter("isNotDeleted");

    return employees;
  }

  @Override
  public FullEmployeeResponseDto getEmployeeById(Long id) {
    return toFullEmployeeResponseDtoConverter.convert(getById(id));
  }

  @Override
  @Transactional
  public FullEmployeeResponseDto createEmployee(CreateEmployeeRequestDto requestDto) {
    createEmployeeRequestDtoValidator.validate(requestDto);

    val employee = Objects.requireNonNull(fromCreateEmployeeRequestDtoConverter.convert(requestDto));
    employee.setRefreshToken(refreshTokenService.createRefreshToken());
    employee.setMobileToken(mobileTokenService.createMobileToken());
    employee.activate();

    val session = entityManager.unwrap(Session.class);
    session.enableFilter("isNotDeleted");

    val savedEmployee = employeeRepository.save(Objects.requireNonNull(employee));

    session.disableFilter("isNotDeleted");

    return toFullEmployeeResponseDtoConverter.convert(savedEmployee);
  }

  @Override
  @Transactional
  public FullEmployeeResponseDto updateEmployeeById(Long id, UpdateEmployeeRequestDto requestDto) {
    val employee = getById(id);

    employeeWithUpdateEmployeeRequestDtoValidator.validate(employee, requestDto);

    val session = entityManager.unwrap(Session.class);
    session.enableFilter("isNotDeleted");

    val savedEmployee = employeeRepository.save(employeeMapper.partialUpdate(employee, requestDto));

    session.disableFilter("isNotDeleted");

    return toFullEmployeeResponseDtoConverter.convert(savedEmployee);
  }

  @Override
  @Transactional
  public void deleteEmployeeById(Long id) {
    val employee = getById(id);

    if (Objects.equals(employee.getRole(), employeeRoleService.getEmployeeRole(EmployeeRoles.ROLE_MANAGER)))
      throw new ManagerEmployeeDeleteException();

    val session = entityManager.unwrap(Session.class);
    session.enableFilter("isNotDeleted");

    workEmployeeRepository.deleteByEmployeeAndWorkFinishedFalse(employee);
    realisationRepository.deleteByEmployeeAndWorkFinishedFalse(employee);

    employeeRepository.delete(employee);

    session.disableFilter("isNotDeleted");
  }

  @Override
  public Employee getById(Long id) {
    val session = entityManager.unwrap(Session.class);
    session.enableFilter("isNotDeleted");

    val employee = employeeRepository.findByIdAndUserId(id, currentRequestUserService.getUserId())
      .orElseThrow(EntityNotFoundException.supplier("Zaposlenik"));

    session.disableFilter("isNotDeleted");

    return employee;
  }

  @Override
  public Employee getByIdIgnoreSoftDelete(Long id) {
    return employeeRepository.findByIdAndUserId(id, currentRequestUserService.getUserId())
      .orElseThrow(EntityNotFoundException.supplier("Zaposlenik"));
  }

  @Override
  public List<WorkResponseDto> getAllAssignedWorks() {
    return workRepository.findAllByEmployee(getById(currentRequestUserService.getEmployeeId())).stream()
      .map(toWorkResponseDtoConverter::convert)
      .toList();
  }

}
