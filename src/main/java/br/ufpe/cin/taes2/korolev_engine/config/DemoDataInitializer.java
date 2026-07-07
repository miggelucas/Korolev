package br.ufpe.cin.taes2.korolev_engine.config;

import br.ufpe.cin.taes2.korolev_engine.domain.model.FeatureFlag;
import br.ufpe.cin.taes2.korolev_engine.infrastructure.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Initializes the repository with a large synthetic Feature Model (> 100 flags)
 * based on a Financial Loan Product domain, for demonstration and performance testing.
 * Only runs if the environment variable 'korolev.demo-mode' is set to 'true'.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "korolev.demo-mode", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {

    private final FeatureFlagRepository repository;

    @Override
    public void run(ApplicationArguments args) {
        log.info("=========================================================");
        log.info("[Demo Mode] Generating >100 Feature Flags for Financial Loan Product...");
        repository.clear();

        List<FeatureFlag> flags = new ArrayList<>();

        // Root Feature
        flags.add(FeatureFlag.builder().name("Loan_System").active(true).build());

        // Core Components (Mandatory under Loan_System)
        flags.add(FeatureFlag.builder().name("Risk_Engine").parentName("Loan_System").mandatory(true).active(true).build());
        flags.add(FeatureFlag.builder().name("Credit_Bureau_Integration").parentName("Loan_System").mandatory(true).active(true).build());
        flags.add(FeatureFlag.builder().name("Core_Banking_Sync").parentName("Loan_System").mandatory(true).active(true).build());

        // Product Types (Optional under Loan_System)
        flags.add(FeatureFlag.builder().name("Personal_Loan").parentName("Loan_System").active(true).build());
        flags.add(FeatureFlag.builder().name("Auto_Loan").parentName("Loan_System").active(true).build());
        flags.add(FeatureFlag.builder().name("Mortgage_Loan").parentName("Loan_System").active(false).build());
        flags.add(FeatureFlag.builder().name("Payroll_Loan").parentName("Loan_System").active(true).build());

        // 1. Generate 1000 Risk Rules inside Risk_Engine
        for (int i = 1; i <= 1000; i++) {
            boolean active = i % 2 != 0; // Half active, half inactive
            flags.add(FeatureFlag.builder()
                    .name("Risk_Rule_" + i)
                    .parentName("Risk_Engine")
                    .active(active)
                    .build());
        }

        // 2. Generate 27 State deployments for Personal_Loan (Brazil States)
        String[] states = {"AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"};
        for (String state : states) {
            flags.add(FeatureFlag.builder()
                    .name("Personal_Loan_" + state)
                    .parentName("Personal_Loan")
                    .active(true)
                    .build());
        }

        // 3. Credit Bureau Providers (Alternative group simulated via mutual exclusion)
        flags.add(FeatureFlag.builder().name("Serasa_API").parentName("Credit_Bureau_Integration").active(true).excludesList(List.of("SPC_API", "BoaVista_API")).build());
        flags.add(FeatureFlag.builder().name("SPC_API").parentName("Credit_Bureau_Integration").active(false).excludesList(List.of("Serasa_API", "BoaVista_API")).build());
        flags.add(FeatureFlag.builder().name("BoaVista_API").parentName("Credit_Bureau_Integration").active(false).excludesList(List.of("Serasa_API", "SPC_API")).build());

        // 4. Auto_Loan partner integrations (Stress test with 5000 partners)
        for (int i = 1; i <= 5000; i++) {
            flags.add(FeatureFlag.builder()
                    .name("Auto_Dealer_Partner_" + i)
                    .parentName("Auto_Loan")
                    .active(true)
                    .build());
        }

        // 5. Payroll_Loan Public Agencies (Stress test with 5000 agencies)
        for (int i = 1; i <= 5000; i++) {
            flags.add(FeatureFlag.builder()
                    .name("Public_Agency_" + i)
                    .parentName("Payroll_Loan")
                    .active(false)
                    // Every public agency requires a specific risk rule to be active
                    .requiresList(List.of("Risk_Rule_" + ((i % 1000) + 1)))
                    .build());
        }

        // Save all to repository
        for (FeatureFlag flag : flags) {
            repository.save(flag);
        }

        log.info("[Demo Mode] Successfully populated {} Feature Flags.", flags.size());
        log.info("=========================================================");
    }
}
