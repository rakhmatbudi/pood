package com.restaurant.management.models;

/**
 * Model class for payment methods
 */
public class PaymentMethod {
    private String id;
    private String name;
    private String code;
    private String description;

    public PaymentMethod(String id, String name, String code) {
        this.id = id;
        this.name = name;
        this.description = name;
        this.code = code;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}