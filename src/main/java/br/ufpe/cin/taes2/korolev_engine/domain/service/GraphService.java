package br.ufpe.cin.taes2.korolev_engine.domain.service;

import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import br.ufpe.cin.taes2.korolev_engine.infrastructure.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates an ASCII tree representation of the current feature flag graph.
 */
@Service
@RequiredArgsConstructor
public class GraphService {

    private final FeatureFlagRepository repository;

    /**
     * Builds a full ASCII tree string of all registered feature flags,
     * grouped by their parent-child hierarchy.
     */
    public String renderGraph() {
        List<FeatureFlag> allFlags = repository.findAll();

        if (allFlags.isEmpty()) {
            return "(empty graph — no feature flags registered)";
        }

        // Index children by parentName
        Map<String, List<FeatureFlag>> childrenMap = allFlags.stream()
                .filter(f -> f.getParentName() != null)
                .collect(Collectors.groupingBy(FeatureFlag::getParentName));

        // Find root nodes (no parent)
        List<FeatureFlag> roots = allFlags.stream()
                .filter(f -> f.getParentName() == null)
                .sorted(Comparator.comparing(FeatureFlag::getName))
                .collect(Collectors.toList());

        // Find orphan nodes (parent declared but parent not in the graph)
        Set<String> allNames = allFlags.stream()
                .map(FeatureFlag::getName)
                .collect(Collectors.toSet());

        List<FeatureFlag> orphans = allFlags.stream()
                .filter(f -> f.getParentName() != null && !allNames.contains(f.getParentName()))
                .sorted(Comparator.comparing(FeatureFlag::getName))
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append("Feature Flag Graph\n");
        sb.append("==================\n\n");

        for (int i = 0; i < roots.size(); i++) {
            renderNode(sb, roots.get(i), "", i == roots.size() - 1 && orphans.isEmpty(), childrenMap, 0);
        }

        if (!orphans.isEmpty()) {
            sb.append("\n⚠ Orphan nodes (parent not found):\n");
            for (int i = 0; i < orphans.size(); i++) {
                renderNode(sb, orphans.get(i), "", i == orphans.size() - 1, childrenMap, 0);
            }
        }

        return sb.toString();
    }

    private void renderNode(StringBuilder sb, FeatureFlag flag, String prefix, boolean isLast,
                            Map<String, List<FeatureFlag>> childrenMap, int depth) {
        String statusIcon = flag.isActive() ? "✓" : "✗";
        String label = formatLabel(flag);

        if (depth == 0) {
            sb.append("[").append(statusIcon).append("] ").append(label).append("\n");
        } else {
            String connector = isLast ? "└── " : "├── ";
            sb.append(prefix).append(connector).append("[").append(statusIcon).append("] ").append(label).append("\n");
        }

        List<FeatureFlag> children = childrenMap.getOrDefault(flag.getName(), Collections.emptyList());
        children.sort(Comparator.comparing(FeatureFlag::getName));

        String childPrefix;
        if (depth == 0) {
            childPrefix = "";
        } else {
            childPrefix = prefix + (isLast ? "    " : "│   ");
        }

        for (int i = 0; i < children.size(); i++) {
            renderNode(sb, children.get(i), childPrefix, i == children.size() - 1, childrenMap, depth + 1);
        }
    }

    private String formatLabel(FeatureFlag flag) {
        StringBuilder label = new StringBuilder(flag.getName());
        List<String> annotations = new ArrayList<>();

        if (flag.isMandatory()) {
            annotations.add("mandatory");
        }
        if (flag.getRequiresList() != null && !flag.getRequiresList().isEmpty()) {
            annotations.add("requires: " + String.join(", ", flag.getRequiresList()));
        }
        if (flag.getExcludesList() != null && !flag.getExcludesList().isEmpty()) {
            annotations.add("excludes: " + String.join(", ", flag.getExcludesList()));
        }

        if (!annotations.isEmpty()) {
            label.append(" (").append(String.join(" | ", annotations)).append(")");
        }

        return label.toString();
    }
}
