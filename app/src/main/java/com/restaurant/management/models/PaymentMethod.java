package com.restaurant.management.models;

/**
 * Model class for payment methods
 */
public class PaymentMethod {
    private String id;
    private String name;
    private String code;
    private String description;
    private int payment_mode_type_id;
    private boolean is_active;
    private String created_at;
    private String updated_at;

    // Existing constructor for backward compatibility
    public PaymentMethod(String id, String name, String code) {
        this.id = id;
        this.name = name;
        this.description = name;
        this.code = code;
        this.is_active = true;
    }

    // New constructor for API response
    public PaymentMethod(String id, String description) {
        this.id = id;
        this.name = description;
        this.description = description;
        this.code = generateCodeFromDescription(description);
        this.is_active = true;
    }

    // Default constructor
    public PaymentMethod() {
        this.is_active = true;
    }

    // Helper method to generate code from description for backward compatibility
    private String generateCodeFromDescription(String description) {
        if (description == null) return "unknown";

        String lower = description.toLowerCase();
        if (lower.contains("cash")) return "cash";
        if (lower.contains("edc") || lower.contains("card")) return "card";
        if (lower.contains("transfer")) return "transfer";
        if (lower.contains("gopay")) return "gopay";
        if (lower.contains("ovo")) return "ovo";
        if (lower.contains("dana")) return "dana";
        if (lower.contains("qris")) return "qris";

        // Default: use first word in lowercase
        return description.split(" ")[0].toLowerCase();
    }

    // Existing getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name != null ? name : description;
    }

    public void setName(String name) {
        this.name = name;
        if (this.description == null) {
            this.description = name;
        }
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description != null ? description : name;
    }

    public void setDescription(String description) {
        this.description = description;
        if (this.name == null) {
            this.name = description;
        }
        if (this.code == null) {
            this.code = generateCodeFromDescription(description);
        }
    }

    // New getters and setters for API fields
    public int getPayment_mode_type_id() {
        return payment_mode_type_id;
    }

    public void setPayment_mode_type_id(int payment_mode_type_id) {
        this.payment_mode_type_id = payment_mode_type_id;
    }

    public boolean isIs_active() {
        return is_active;
    }

    public void setIs_active(boolean is_active) {
        this.is_active = is_active;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }

    // Helper method to get display name for UI
    public String getDisplayName() {
        return getDescription();
    }

    @Override
    public String toString() {
        return "PaymentMethod{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", description='" + description + '\'' +
                ", is_active=" + is_active +
                '}';
    }
}