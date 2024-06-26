package hr.algebra.fruity.repository;

import hr.algebra.fruity.model.CadastralMunicipality;
import hr.algebra.fruity.model.CadastralParcel;
import hr.algebra.fruity.model.User;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CadastralParcelRepository extends PagingAndSortingRepository<CadastralParcel, Long>, JpaRepository<CadastralParcel, Long> {

  Optional<CadastralParcel> findByIdAndUser(Long id, User user);

  Optional<CadastralParcel> findByIdAndUserId(Long id, Long userFk);

  Set<CadastralParcel> findAllByUser(User user);

  Set<CadastralParcel> findAllByUserId(Long userFk);

  Optional<CadastralParcel> findByNameAndUser(String name, User user);

  Optional<CadastralParcel> findByNameAndUserId(String name, Long userFk);

  Optional<CadastralParcel> findByCadastralMunicipalityIdAndCadastralNumber(Integer cadastralMunicipalityFk, String cadastralNumber);

  Optional<CadastralParcel> findByCadastralMunicipalityAndCadastralNumber(CadastralMunicipality cadastralMunicipality, String cadastralNumber);

}
