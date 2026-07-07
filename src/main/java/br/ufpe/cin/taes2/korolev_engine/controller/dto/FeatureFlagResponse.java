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
public class FeatureFlagResponse {
    private String name;
    private boolean active;
    private String parentName;
    private boolean mandatory;
    private List<String> requiresList;
    private List<String> excludesList;
}
