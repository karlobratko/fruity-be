package hr.algebra.fruity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.Singular;
import lombok.ToString;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = Row.Constants.tableName)
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode(doNotUseGetters = true, onlyExplicitlyIncluded = true)
@ToString(doNotUseGetters = true, onlyExplicitlyIncluded = true)
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE rows SET delete_date = CURRENT_DATE WHERE row_id = ?", check = ResultCheckStyle.COUNT)
//@Where(clause = "delete_date IS NULL")
@FilterDef(name = "isNotDeleted", defaultCondition = "delete_date IS NULL")
@Filter(name = "isNotDeleted")
public class Row {

  @Id
  @Column(name = Constants.idColumnName)
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @EqualsAndHashCode.Include
  @ToString.Include
  private Long id;

  @Column(name = Constants.uuidColumnName, nullable = false, unique = true)
  @EqualsAndHashCode.Include
  private UUID uuid;

  @CreatedDate
  @Column(name = Constants.createDateColumnName, nullable = false)
  @Temporal(TemporalType.DATE)
  private LocalDate createDate;

  @LastModifiedDate
  @Column(name = Constants.updateDateColumnName, nullable = false)
  @Temporal(TemporalType.DATE)
  private LocalDate updateDate;

  @Column(name = Constants.deleteDateColumnName)
  @Temporal(TemporalType.DATE)
  private LocalDate deleteDate;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = User.Constants.joinColumnName, nullable = false)
  private @NonNull User user;

  @Column(name = Constants.ordinalColumnName, nullable = false)
  @ToString.Include
  private @NonNull Integer ordinal;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = RowCluster.Constants.joinColumnName, nullable = false)
  private @NonNull RowCluster rowCluster;

  @Column(name = Constants.numberOfSeedlingsColumnName, nullable = false)
  private @NonNull Integer numberOfSeedlings;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = FruitCultivar.Constants.joinColumnName, nullable = false)
  private @NonNull FruitCultivar fruitCultivar;

  @Column(name = Constants.plantingYearColumnName, nullable = false)
  private @NonNull Integer plantingYear;

  @OneToMany(mappedBy = WorkRow.Fields.row, fetch = FetchType.LAZY)
  @Singular
  private Set<WorkRow> works;

  @OneToMany(mappedBy = RealisationRow.Fields.row, fetch = FetchType.LAZY)
  @Singular
  private Set<RealisationRow> realisations;

  @PrePersist
  private void prePersist() {
    this.uuid = UUID.randomUUID();
  }

  @PreRemove
  private void preRemove() {
    this.deleteDate = LocalDate.now();
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Fields {

    public static final String id = "id";

    public static final String uuid = "uuid";

    public static final String createDate = "createDate";

    public static final String updateDate = "updateDate";

    public static final String deleteDate = "deleteDate";

    public static final String ordinal = "ordinal";

    public static final String rowCluster = "rowCluster";

    public static final String fruitCultivar = "fruitCultivar";

    public static final String numberOfSeedlings = "numberOfSeedlings";

    public static final String plantingYear = "plantingYear";

    public static final String works = "works";

    public static final String realisations = "realisations";

  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Constants {

    public static final String tableName = "rows";

    public static final String joinColumnName = "row_fk";

    public static final String idColumnName = "row_id";

    public static final String uuidColumnName = "uuid";

    public static final String createDateColumnName = "create_date";

    public static final String updateDateColumnName = "update_date";

    public static final String deleteDateColumnName = "delete_date";

    public static final String ordinalColumnName = "ordinal";

    public static final String numberOfSeedlingsColumnName = "number_of_seedlings";

    public static final String plantingYearColumnName = "planting_year";

    public static final String rowClusterAndOrdinalUniqueConstraintName = "uq_row_row_cluster_ordinal";

  }

}
