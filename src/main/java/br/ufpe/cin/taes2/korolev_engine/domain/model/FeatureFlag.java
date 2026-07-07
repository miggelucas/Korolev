package br.ufpe.cin.taes2.korolev_engine.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFlag {
    private String name;

    @Builder.Default
    private boolean active = false;

    private String parentName;

    @Builder.Default
    private boolean mandatory = false;

    @Builder.Default
    private List<String> requiresList = new ArrayList<>();
    
    @Builder.Default
    private List<String> excludesList = new ArrayList<>();
}
