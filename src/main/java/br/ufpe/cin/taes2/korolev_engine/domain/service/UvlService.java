package br.ufpe.cin.taes2.korolev_engine.domain.service;

import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to parse (import) and serialize (export) Universal Variability Language (UVL) models.
 * Operates with zero external dependencies, supporting indentation-based parsing,
 * alternative XOR mapping to excludesList, and cross-tree constraints mapping.
 */
@Slf4j
@Service
public class UvlService {

    /**
     * Exports the provided feature flags into a standardized UVL string.
     */
    public String exportUvl(List<FeatureFlag> allFlags) {
        log.info("[UvlService] - Export UVL - Building UVL string");
        if (allFlags.isEmpty()) {
            return "features\n";
        }

        Map<String, List<FeatureFlag>> childrenMap = allFlags.stream()
                .filter(f -> f.getParentName() != null)
                .collect(Collectors.groupingBy(FeatureFlag::getParentName));

        List<FeatureFlag> roots = allFlags.stream()
                .filter(f -> f.getParentName() == null)
                .sorted(Comparator.comparing(FeatureFlag::getName))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("features\n");
        for (FeatureFlag root : roots) {
            exportNode(sb, root, 1, childrenMap);
        }

        // Cross-Tree Constraints
        StringBuilder constrBuilder = new StringBuilder();
        Set<String> printedExcludes = new HashSet<>();

        for (FeatureFlag flag : allFlags) {
            if (flag.getRequiresList() != null) {
                for (String reqTarget : flag.getRequiresList()) {
                    constrBuilder.append("\t").append(flag.getName()).append(" requires ").append(reqTarget).append("\n");
                }
            }
            if (flag.getExcludesList() != null) {
                for (String exName : flag.getExcludesList()) {
                    String pair = flag.getName().compareTo(exName) < 0
                            ? flag.getName() + "-" + exName
                            : exName + "-" + flag.getName();

                    // Skip printing alternative siblings as explicit constraints since they are already covered by alternative group semantics
                    if (isAlternativeSibling(flag, exName, allFlags)) {
                        continue;
                    }

                    if (printedExcludes.add(pair)) {
                        constrBuilder.append("\t").append(flag.getName()).append(" excludes ").append(exName).append("\n");
                    }
                }
            }
        }

        if (!constrBuilder.isEmpty()) {
            sb.append("\nconstraints\n").append(constrBuilder);
        }

        return sb.toString();
    }

    public List<FeatureFlag> parseUvl(String content) {
        List<FeatureFlag> flags = new ArrayList<>();
        Map<String, FeatureFlag> flagMap = new HashMap<>();

        String[] lines = content.split("\\r?\\n");
        String currentSection = "";

        List<IndentLevel> stack = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("#")) {
                continue;
            }

            if (trimmed.equals("features")) {
                currentSection = "features";
                continue;
            } else if (trimmed.equals("constraints")) {
                currentSection = "constraints";
                continue;
            }

            if ("features".equals(currentSection)) {
                int indent = getIndent(line);

                while (!stack.isEmpty() && stack.getLast().spaces >= indent) {
                    stack.removeLast();
                }

                if (isGroupKeyword(trimmed)) {
                    if (!stack.isEmpty()) {
                        stack.getLast().groupType = trimmed.toLowerCase();
                    }
                } else {
                    String name = cleanFeatureName(trimmed);

                    FeatureFlag parent = null;
                    String groupType = "";
                    if (!stack.isEmpty()) {
                        IndentLevel parentLevel = stack.getLast();
                        parent = parentLevel.feature;
                        groupType = parentLevel.groupType;
                    }

                    boolean mandatory = "mandatory".equals(groupType);

                    FeatureFlag flag = FeatureFlag.builder()
                            .name(name)
                            .active(false) // structure import: start as inactive
                            .parentName(parent != null ? parent.getName() : null)
                            .mandatory(mandatory)
                            .excludesList(new ArrayList<>())
                            .build();

                    flags.add(flag);
                    flagMap.put(name, flag);

                    // If parent group is alternative, create mutual exclusions between all siblings
                    if ("alternative".equals(groupType) && parent != null) {
                        for (FeatureFlag sibling : flags) {
                            if (sibling != flag && parent.getName().equals(sibling.getParentName())) {
                                if (!sibling.isMandatory()) {
                                    sibling.getExcludesList().add(name);
                                    flag.getExcludesList().add(sibling.getName());
                                }
                            }
                        }
                    }

                    IndentLevel level = new IndentLevel();
                    level.spaces = indent;
                    level.feature = flag;
                    level.groupType = "";
                    stack.add(level);
                }
            } else if ("constraints".equals(currentSection)) {
                parseConstraint(trimmed, flagMap);
            }
        }

        return flags;
    }

    private void parseConstraint(String line, Map<String, FeatureFlag> flagMap) {
        String[] parts = line.split("\\s+");
        if (parts.length >= 3) {
            String sourceName = parts[0];
            String relation = parts[1].toLowerCase();
            String targetName = parts[2];

            FeatureFlag source = flagMap.get(sourceName);
            FeatureFlag target = flagMap.get(targetName);

            if (source != null && target != null) {
                if ("requires".equals(relation)) {
                    if (!source.getRequiresList().contains(targetName)) {
                        source.getRequiresList().add(targetName);
                    }
                } else if ("excludes".equals(relation)) {
                    if (!source.getExcludesList().contains(targetName)) {
                        source.getExcludesList().add(targetName);
                    }
                    if (!target.getExcludesList().contains(sourceName)) {
                        target.getExcludesList().add(sourceName);
                    }
                }
            }
        }
    }

    private int getIndent(String line) {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ' ') {
                count++;
            } else if (c == '\t') {
                count += 4;
            } else {
                break;
            }
        }
        return count;
    }

    private boolean isGroupKeyword(String text) {
        String lower = text.toLowerCase();
        return "mandatory".equals(lower) || "optional".equals(lower) || "alternative".equals(lower) || "or".equals(lower);
    }

    private String cleanFeatureName(String text) {
        // Strip abstract notations or spaces
        int idx = text.indexOf('{');
        if (idx != -1) {
            return text.substring(0, idx).trim();
        }
        return text.trim();
    }

    private void exportNode(StringBuilder sb, FeatureFlag node, int indent, Map<String, List<FeatureFlag>> childrenMap) {
        indentTabs(sb, indent);
        sb.append(node.getName()).append("\n");

        List<FeatureFlag> children = childrenMap.getOrDefault(node.getName(), Collections.emptyList());
        if (children.isEmpty()) {
            return;
        }

        List<FeatureFlag> mandatoryGroup = new ArrayList<>();
        List<FeatureFlag> alternativeGroup = new ArrayList<>();
        List<FeatureFlag> optionalGroup = new ArrayList<>();

        for (FeatureFlag child : children) {
            if (child.isMandatory()) {
                mandatoryGroup.add(child);
            } else {
                if (isAlternativeSiblingUnderParent(child, children)) {
                    alternativeGroup.add(child);
                } else {
                    optionalGroup.add(child);
                }
            }
        }

        if (!mandatoryGroup.isEmpty()) {
            indentTabs(sb, indent + 1);
            sb.append("mandatory\n");
            mandatoryGroup.sort(Comparator.comparing(FeatureFlag::getName));
            for (FeatureFlag child : mandatoryGroup) {
                exportNode(sb, child, indent + 2, childrenMap);
            }
        }

        if (!alternativeGroup.isEmpty()) {
            indentTabs(sb, indent + 1);
            sb.append("alternative\n");
            alternativeGroup.sort(Comparator.comparing(FeatureFlag::getName));
            for (FeatureFlag child : alternativeGroup) {
                exportNode(sb, child, indent + 2, childrenMap);
            }
        }

        if (!optionalGroup.isEmpty()) {
            indentTabs(sb, indent + 1);
            sb.append("optional\n");
            optionalGroup.sort(Comparator.comparing(FeatureFlag::getName));
            for (FeatureFlag child : optionalGroup) {
                exportNode(sb, child, indent + 2, childrenMap);
            }
        }
    }

    private boolean isAlternativeSiblingUnderParent(FeatureFlag child, List<FeatureFlag> siblings) {
        if (child.getExcludesList() == null) {
            return false;
        }
        for (String exName : child.getExcludesList()) {
            boolean isSibling = siblings.stream()
                    .anyMatch(s -> s.getName().equals(exName) && !s.isMandatory());
            if (isSibling) {
                return true;
            }
        }
        return false;
    }

    private boolean isAlternativeSibling(FeatureFlag flag, String exName, List<FeatureFlag> allFlags) {
        if (flag.getParentName() == null) {
            return false;
        }
        FeatureFlag exFlag = allFlags.stream()
                .filter(f -> f.getName().equals(exName))
                .findFirst()
                .orElse(null);

        if (exFlag == null || !flag.getParentName().equals(exFlag.getParentName())) {
            return false;
        }

        return !flag.isMandatory() && !exFlag.isMandatory();
    }

    private void indentTabs(StringBuilder sb, int count) {
        sb.repeat("\t", Math.max(0, count));
    }

    private static class IndentLevel {
        int spaces;
        FeatureFlag feature;
        String groupType;
    }
}
