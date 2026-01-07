package dev.nozh.api.capability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Groups capability contracts under a mod declaration.
 */
public final class CapabilityContractDeclaration {

    private final String modId;
    private final String displayName;
    private final List<CapabilityContract> contracts;
    private final String notes;

    private CapabilityContractDeclaration(
            String modId,
            String displayName,
            List<CapabilityContract> contracts,
            String notes) {
        this.modId = modId;
        this.displayName = displayName;
        this.contracts = List.copyOf(contracts);
        this.notes = notes == null ? "" : notes;
    }

    public String modId() {
        return modId;
    }

    public String displayName() {
        return displayName;
    }

    public List<CapabilityContract> contracts() {
        return Collections.unmodifiableList(contracts);
    }

    public String notes() {
        return notes;
    }

    public static Builder builder(String modId, String displayName) {
        return new Builder(modId, displayName);
    }

    public static final class Builder {
        private final String modId;
        private final String displayName;
        private final List<CapabilityContract> contracts = new ArrayList<>();
        private String notes;

        private Builder(String modId, String displayName) {
            this.modId = Objects.requireNonNull(modId, "modId");
            this.displayName = Objects.requireNonNull(displayName, "displayName");
        }

        public Builder contract(CapabilityContract contract) {
            if (contract != null) {
                contracts.add(contract);
            }
            return this;
        }

        public Builder contracts(List<CapabilityContract> contracts) {
            if (contracts != null) {
                this.contracts.addAll(contracts);
            }
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public CapabilityContractDeclaration build() {
            return new CapabilityContractDeclaration(modId, displayName, contracts, notes);
        }
    }
}
