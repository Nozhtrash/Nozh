package dev.nozh.core.capability;

public sealed interface CapabilityValue permits 
    CapabilityValue.IntValue, 
    CapabilityValue.BoolValue {
    
    Object getValue();
    String asString();
    
    record IntValue(int value) implements CapabilityValue {
        @Override
        public Object getValue() {
            return value;
        }
        
        @Override
        public String asString() {
            return String.valueOf(value);
        }
    }
    
    record BoolValue(boolean value) implements CapabilityValue {
        @Override
        public Object getValue() {
            return value;
        }
        
        @Override
        public String asString() {
            return value ? "ON" : "OFF";
        }
    }
    
    static CapabilityValue of(int value) {
        return new IntValue(value);
    }
    
    static CapabilityValue of(boolean value) {
        return new BoolValue(value);
    }
}
