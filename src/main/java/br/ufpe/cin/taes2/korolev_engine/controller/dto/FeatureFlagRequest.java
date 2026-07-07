package br.ufpe.cin.taes2.korolev_engine.controller.dto;

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
public class FeatureFlagRequest {
    private String name;

    @Builder.Default
    private boolean active = false;

    private String parentName;

    @Builder.Default
    private boolean mandatory = false;

    private List<String> requiresList;

    @Builder.Default
    private List<String> excludesList = new ArrayList<>();
}
