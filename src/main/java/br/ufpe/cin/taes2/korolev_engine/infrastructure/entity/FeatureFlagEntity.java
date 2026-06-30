package br.ufpe.cin.taes2.korolev_engine.infrastructure.entity;

import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistence entity for the infrastructure layer.
 * Stored in the repository and converted to/from the domain model at the boundary.
 * This guarantees that every read returns a fresh domain object with no shared references.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFlagEntity {

    private String name;
    private boolean active;
    private String parentName;
    private boolean mandatory;
    private String requiresTarget;

    @Builder.Default
    private List<String> excludesList = new ArrayList<>();

    public static FeatureFlagEntity fromDomain(FeatureFlag flag) {
        return FeatureFlagEntity.builder()
                .name(flag.getName())
                .active(flag.isActive())
                .parentName(flag.getParentName())
                .mandatory(flag.isMandatory())
                .requiresTarget(flag.getRequiresTarget())
                .excludesList(flag.getExcludesList() != null ? new ArrayList<>(flag.getExcludesList()) : new ArrayList<>())
                .build();
    }

    public FeatureFlag toDomain() {
        return FeatureFlag.builder()
                .name(this.name)
                .active(this.active)
                .parentName(this.parentName)
                .mandatory(this.mandatory)
                .requiresTarget(this.requiresTarget)
                .excludesList(this.excludesList != null ? new ArrayList<>(this.excludesList) : new ArrayList<>())
                .build();
    }
}
