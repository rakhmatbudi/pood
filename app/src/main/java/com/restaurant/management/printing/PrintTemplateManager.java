package com.restaurant.management.printing;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.restaurant.management.models.Order;
import com.restaurant.management.models.OrderItem;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Template-based printer manager that allows customizable print layouts
 * without code compilation. Templates are stored as JSON files in assets/print_templates/
 */
public class PrintTemplateManager {
    private static final String TAG = "PrintTemplateManager";
    private static final String TEMPLATE_DIR = "print_templates";

    // Template types
    public static final String TEMPLATE_KITCHEN_CHECKER = "kitchen_checker";
    public static final String TEMPLATE_CUSTOMER_BILL = "customer_bill";
    public static final String TEMPLATE_PAYMENT_RECEIPT = "payment_receipt";

    // ESC/POS Commands
    private static final byte[] ESC_INIT = {0x1B, 0x40};
    private static final byte[] ESC_ALIGN_CENTER = {0x1B, 0x61, 0x01};
    private static final byte[] ESC_ALIGN_LEFT = {0x1B, 0x61, 0x00};
    private static final byte[] ESC_ALIGN_RIGHT = {0x1B, 0x61, 0x02};
    private static final byte[] ESC_BOLD_ON = {0x1B, 0x45, 0x01};
    private static final byte[] ESC_BOLD_OFF = {0x1B, 0x45, 0x00};
    private static final byte[] ESC_DOUBLE_HEIGHT = {0x1B, 0x21, 0x10};
    private static final byte[] ESC_NORMAL_SIZE = {0x1B, 0x21, 0x00};
    private static final byte[] ESC_CUT_PAPER = {0x1D, 0x56, 0x42, 0x00};
    private static final byte[] ESC_FEED_LINE = {0x0A};

    private final Context context;
    private final Map<String, PrintTemplate> templateCache = new HashMap<>();

    public PrintTemplateManager(Context context) {
        this.context = context;
    }

    /**
     * Print kitchen checker using template
     */
    public void printKitchenChecker(OutputStream outputStream, Order order) throws IOException {
        PrintTemplate template = getTemplate(TEMPLATE_KITCHEN_CHECKER);
        Map<String, Object> data = buildKitchenCheckerData(order);
        executeTemplate(outputStream, template, data);
    }

    /**
     * Print customer bill using template
     */
    public void printCustomerBill(OutputStream outputStream, Order order,
                                  double taxRate, String taxDescription,
                                  double serviceRate, String serviceDescription) throws IOException {
        PrintTemplate template = getTemplate(TEMPLATE_CUSTOMER_BILL);
        Map<String, Object> data = buildCustomerBillData(order, taxRate, taxDescription, serviceRate, serviceDescription);
        executeTemplate(outputStream, template, data);
    }

    /**
     * Print payment receipt using template
     */
    public void printPaymentReceipt(OutputStream outputStream,
                                    String orderNumber, String tableNumber,
                                    double originalAmount, double finalAmount,
                                    double discountAmount, String discountName,
                                    String paymentMethod, double amountPaid,
                                    double taxRate, String taxDescription,
                                    double serviceRate, String serviceDescription) throws IOException {
        PrintTemplate template = getTemplate(TEMPLATE_PAYMENT_RECEIPT);
        Map<String, Object> data = buildPaymentReceiptData(orderNumber, tableNumber,
                originalAmount, finalAmount, discountAmount, discountName,
                paymentMethod, amountPaid, taxRate, taxDescription, serviceRate, serviceDescription);
        executeTemplate(outputStream, template, data);
    }

    /**
     * Load template from assets or return default if not found
     */
    private PrintTemplate getTemplate(String templateType) {
        if (templateCache.containsKey(templateType)) {
            return templateCache.get(templateType);
        }

        try {
            String templateJson = loadTemplateFromAssets(templateType + ".json");
            PrintTemplate template = PrintTemplate.fromJson(templateJson);
            templateCache.put(templateType, template);
            return template;
        } catch (Exception e) {
            Log.w(TAG, "Failed to load template: " + templateType + ", using default", e);
            PrintTemplate defaultTemplate = getDefaultTemplate(templateType);
            templateCache.put(templateType, defaultTemplate);
            return defaultTemplate;
        }
    }

    /**
     * Load template file from assets
     */
    private String loadTemplateFromAssets(String fileName) throws IOException {
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = assetManager.open(TEMPLATE_DIR + "/" + fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line).append("\n");
        }
        reader.close();
        inputStream.close();
        return builder.toString();
    }

    /**
     * Execute template with data
     */
    private void executeTemplate(OutputStream outputStream, PrintTemplate template, Map<String, Object> data) throws IOException {
        // Initialize printer
        outputStream.write(ESC_INIT);

        // Execute each section
        for (PrintTemplate.Section section : template.getSections()) {
            executeSection(outputStream, section, data);
        }

        // Cut paper if specified
        if (template.shouldCutPaper()) {
            outputStream.write(ESC_FEED_LINE);
            outputStream.write(ESC_FEED_LINE);
            outputStream.write(ESC_CUT_PAPER);
        }

        outputStream.flush();
    }

    /**
     * Execute a template section
     */
    private void executeSection(OutputStream outputStream, PrintTemplate.Section section, Map<String, Object> data) throws IOException {
        // Apply section formatting first
        applyFormatting(outputStream, section.getFormatting());

        Log.d(TAG, "Executing section: " + section.getName() +
                " with formatting: align=" + section.getFormatting().getAlign());

        // Process each line in the section
        for (PrintTemplate.Line line : section.getLines()) {
            executeLine(outputStream, line, data, section.getFormatting()); // Pass section formatting
        }

        // Add section spacing
        for (int i = 0; i < section.getSpacingAfter(); i++) {
            outputStream.write(ESC_FEED_LINE);
        }
    }

    private PrintTemplate.Formatting mergeFormatting(PrintTemplate.Formatting sectionFormatting, PrintTemplate.Formatting lineFormatting) {
        if (lineFormatting == null && sectionFormatting == null) {
            return new PrintTemplate.Formatting(); // Default formatting
        }

        if (lineFormatting == null) {
            return sectionFormatting;
        }

        if (sectionFormatting == null) {
            return lineFormatting;
        }

        // Create merged formatting
        PrintTemplate.Formatting merged = new PrintTemplate.Formatting();

        // Line formatting overrides section formatting, but use section as fallback
        merged.setAlign(lineFormatting.getAlign() != null && !lineFormatting.getAlign().equals("left") ?
                lineFormatting.getAlign() : sectionFormatting.getAlign());
        merged.setBold(lineFormatting.isBold() || sectionFormatting.isBold());
        merged.setDoubleHeight(lineFormatting.isDoubleHeight() || sectionFormatting.isDoubleHeight());

        return merged;
    }

    private void executeConditional(OutputStream outputStream, PrintTemplate.Line line, Map<String, Object> data, PrintTemplate.Formatting parentFormatting) throws IOException {
        String condition = line.getCondition();
        if (evaluateCondition(condition, data)) {
            for (PrintTemplate.Line subLine : line.getSubLines()) {
                executeLine(outputStream, subLine, data, parentFormatting); // Pass parent formatting
            }
        }
    }

    private void executeLine(OutputStream outputStream, PrintTemplate.Line line, Map<String, Object> data, PrintTemplate.Formatting sectionFormatting) throws IOException {
        // Determine effective formatting (line formatting overrides section formatting)
        PrintTemplate.Formatting effectiveFormatting = mergeFormatting(sectionFormatting, line.getFormatting());

        // Apply line formatting
        applyFormatting(outputStream, effectiveFormatting);

        if (line.getType().equals("text")) {
            // Simple text line
            String text = replacePlaceholders(line.getContent(), data);
            printLine(outputStream, text);
        } else if (line.getType().equals("separator")) {
            // Separator line
            String separator = line.getContent();
            if (separator == null || separator.isEmpty()) {
                separator = "--------------------------------";
            }
            printLine(outputStream, separator);
        } else if (line.getType().equals("items_loop")) {
            // Loop through order items
            executeItemsLoop(outputStream, line, data);
        } else if (line.getType().equals("conditional")) {
            // Conditional content
            executeConditional(outputStream, line, data, effectiveFormatting); // Pass formatting
        } else if (line.getType().equals("total_line")) {
            // Formatted total line
            executeTotalLine(outputStream, line, data);
        }

        // Reset formatting after line
        outputStream.write(ESC_NORMAL_SIZE);
        outputStream.write(ESC_BOLD_OFF);
    }

    /**
     * Execute a template line
     */
    private void executeLine(OutputStream outputStream, PrintTemplate.Line line, Map<String, Object> data) throws IOException {
        executeLine(outputStream, line, data, null); // No section formatting
    }


    /**
     * Execute items loop
     */
    @SuppressWarnings("unchecked")
    private void executeItemsLoop(OutputStream outputStream, PrintTemplate.Line line, Map<String, Object> data) throws IOException {
        List<OrderItem> items = (List<OrderItem>) data.get("items");
        if (items == null || items.isEmpty()) {
            if (line.getEmptyText() != null) {
                printLine(outputStream, line.getEmptyText());
            }
            return;
        }

        for (OrderItem item : items) {
            Map<String, Object> itemData = new HashMap<>(data);
            itemData.put("item", item);
            itemData.put("item_name", getDisplayName(item));
            itemData.put("item_quantity", item.getQuantity());
            itemData.put("item_price", formatCurrency(item.getUnitPrice()));
            itemData.put("item_total", formatCurrency(item.getTotalPrice()));
            itemData.put("item_notes", hasValidNotes(item) ? item.getNotes() : "");
            itemData.put("has_notes", hasValidNotes(item));

            for (PrintTemplate.Line subLine : line.getSubLines()) {
                executeLine(outputStream, subLine, itemData);
            }
        }
    }

    /**
     * Execute conditional content
     */
    private void executeConditional(OutputStream outputStream, PrintTemplate.Line line, Map<String, Object> data) throws IOException {
        String condition = line.getCondition();
        if (evaluateCondition(condition, data)) {
            for (PrintTemplate.Line subLine : line.getSubLines()) {
                executeLine(outputStream, subLine, data);
            }
        }
    }

    /**
     * Execute total line with proper formatting
     */
    private void executeTotalLine(OutputStream outputStream, PrintTemplate.Line line, Map<String, Object> data) throws IOException {
        String label = replacePlaceholders(line.getLabel(), data);
        String amount = replacePlaceholders(line.getAmount(), data);
        String formatted = formatTotalLine(label, amount, line.getCharWidth());
        printLine(outputStream, formatted);
    }

    /**
     * Apply formatting commands
     */
    private void applyFormatting(OutputStream outputStream, PrintTemplate.Formatting formatting) throws IOException {
        if (formatting == null) {
            Log.d(TAG, "No formatting to apply");
            return;
        }

        Log.d(TAG, "Applying formatting: align=" + formatting.getAlign() +
                ", bold=" + formatting.isBold() + ", doubleHeight=" + formatting.isDoubleHeight());

        // Alignment
        switch (formatting.getAlign()) {
            case "center":
                outputStream.write(ESC_ALIGN_CENTER);
                Log.d(TAG, "Applied CENTER alignment");
                break;
            case "right":
                outputStream.write(ESC_ALIGN_RIGHT);
                Log.d(TAG, "Applied RIGHT alignment");
                break;
            default:
                outputStream.write(ESC_ALIGN_LEFT);
                Log.d(TAG, "Applied LEFT alignment");
                break;
        }

        // Bold
        if (formatting.isBold()) {
            outputStream.write(ESC_BOLD_ON);
            Log.d(TAG, "Applied BOLD");
        }

        // Size
        if (formatting.isDoubleHeight()) {
            outputStream.write(ESC_DOUBLE_HEIGHT);
            Log.d(TAG, "Applied DOUBLE HEIGHT");
        }
    }

    /**
     * Replace placeholders in text with actual data
     */
    private String replacePlaceholders(String text, Map<String, Object> data) {
        if (text == null) return "";

        Pattern pattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
        Matcher matcher = pattern.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1).trim();
            Object value = getNestedValue(data, key);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Get nested value from data map (supports dot notation)
     */
    private Object getNestedValue(Map<String, Object> data, String key) {
        String[] parts = key.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }

        return current;
    }

    /**
     * Evaluate simple conditions
     */
    private boolean evaluateCondition(String condition, Map<String, Object> data) {
        if (condition == null) return false;

        // Simple conditions: "has_discount", "no_discount", "has_notes", etc.
        if (condition.startsWith("has_")) {
            String key = condition.substring(4);
            Object value = data.get(key);
            return value != null && !value.toString().isEmpty() && !value.toString().equals("0") && !value.toString().equals("false");
        }

        if (condition.startsWith("no_")) {
            String key = condition.substring(3);
            Object value = data.get(key);
            return value == null || value.toString().isEmpty() || value.toString().equals("0") || value.toString().equals("false");
        }

        // Direct boolean check
        Object value = data.get(condition);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        return false;
    }

    /**
     * Build data map for kitchen checker
     */
    private Map<String, Object> buildKitchenCheckerData(Order order) {
        Map<String, Object> data = new HashMap<>();
        data.put("order_number", order.getOrderNumber());
        data.put("table_number", order.getTableNumber());
        data.put("customer_name", order.getCustomerName() != null ? order.getCustomerName() : "");
        data.put("current_time", formatDateTime(new Date()));
        data.put("items", order.getItems());
        data.put("has_customer_name", order.getCustomerName() != null && !order.getCustomerName().isEmpty());
        return data;
    }

    /**
     * Build data map for customer bill
     */
    private Map<String, Object> buildCustomerBillData(Order order, double taxRate, String taxDescription, double serviceRate, String serviceDescription) {
        Map<String, Object> data = new HashMap<>();
        data.put("order_number", order.getOrderNumber());
        data.put("table_number", order.getTableNumber());
        data.put("customer_name", order.getCustomerName() != null ? order.getCustomerName() : "");
        data.put("created_at", order.getCreatedAt() != null ? order.getCreatedAt() : "");
        data.put("server_id", order.getServerId());
        data.put("order_type", order.getOrderTypeName() != null ? order.getOrderTypeName() : "");
        data.put("current_time", formatDateTime(new Date()));
        data.put("items", order.getItems());

        // Calculate amounts
        double subtotal = order.getTotalAmount();
        double taxAmount = subtotal * taxRate;
        double serviceAmount = subtotal * serviceRate;

        data.put("subtotal", formatCurrency(subtotal));
        data.put("tax_rate", String.format("%.0f", taxRate * 100));
        data.put("tax_description", taxDescription);
        data.put("tax_amount", formatCurrency(taxAmount));
        data.put("service_rate", String.format("%.0f", serviceRate * 100));
        data.put("service_description", serviceDescription);
        data.put("service_amount", formatCurrency(serviceAmount));
        data.put("final_amount", formatCurrency(order.getFinalAmount()));

        // Conditions
        data.put("has_customer_name", order.getCustomerName() != null && !order.getCustomerName().isEmpty());
        data.put("has_created_at", order.getCreatedAt() != null && !order.getCreatedAt().isEmpty());
        data.put("has_order_type", order.getOrderTypeName() != null && !order.getOrderTypeName().isEmpty());
        data.put("has_tax", taxAmount > 0.01);
        data.put("has_service", serviceAmount > 0.01);

        return data;
    }

    /**
     * Build data map for payment receipt
     */
    private Map<String, Object> buildPaymentReceiptData(String orderNumber, String tableNumber,
                                                        double originalAmount, double finalAmount,
                                                        double discountAmount, String discountName,
                                                        String paymentMethod, double amountPaid,
                                                        double taxRate, String taxDescription,
                                                        double serviceRate, String serviceDescription) {
        Map<String, Object> data = new HashMap<>();
        data.put("receipt_number", String.valueOf(System.currentTimeMillis()));
        data.put("order_number", orderNumber);
        data.put("table_number", tableNumber);
        data.put("current_time", formatDateTime(new Date()));
        data.put("payment_method", paymentMethod.toUpperCase());
        data.put("amount_paid", formatCurrency(amountPaid));
        data.put("final_amount", formatCurrency(finalAmount));

        // Calculate change
        double change = amountPaid - finalAmount;
        data.put("change", formatCurrency(change));
        data.put("has_change", change > 0);

        // Calculate base amount and charges
        boolean hasDiscount = discountAmount > 0.01;
        double baseAmount;

        if (hasDiscount) {
            double totalRate = 1 + taxRate + serviceRate;
            baseAmount = originalAmount / totalRate;
            data.put("original_amount", formatCurrency(originalAmount));
            data.put("discount_amount", formatCurrency(discountAmount));
            data.put("discount_name", discountName != null ? discountName : "");
        } else {
            double totalRate = 1 + taxRate + serviceRate;
            baseAmount = finalAmount / totalRate;
        }

        data.put("base_amount", formatCurrency(baseAmount));
        data.put("tax_rate", String.format("%.0f", taxRate * 100));
        data.put("tax_description", taxDescription);
        data.put("tax_amount", formatCurrency(baseAmount * taxRate));
        data.put("service_rate", String.format("%.0f", serviceRate * 100));
        data.put("service_description", serviceDescription);
        data.put("service_amount", formatCurrency(baseAmount * serviceRate));

        // Conditions
        data.put("has_discount", hasDiscount);
        data.put("has_tax", taxRate > 0.01);
        data.put("has_service", serviceRate > 0.01);

        return data;
    }

    // Helper methods
    private void printLine(OutputStream outputStream, String text) throws IOException {
        outputStream.write(text.getBytes("UTF-8"));
        outputStream.write(ESC_FEED_LINE);
    }

    private String formatTotalLine(String label, String amount, int charWidth) {
        if (charWidth <= 0) charWidth = 32; // Default width
        int labelWidth = charWidth - amount.length();
        if (labelWidth < 0) labelWidth = 0;
        return String.format("%-" + labelWidth + "s%s", label, amount);
    }

    private String formatCurrency(double amount) {
        return String.format(Locale.getDefault(), "%,.0f", amount);
    }

    private String formatDateTime(Date date) {
        return new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(date);
    }

    private String getDisplayName(OrderItem item) {
        return item.getDisplayName();
    }

    private boolean hasValidNotes(OrderItem item) {
        String notes = item.getNotes();
        return notes != null && !notes.trim().isEmpty() && !"null".equalsIgnoreCase(notes.trim());
    }

    /**
     * Get default template if file not found
     */
    private PrintTemplate getDefaultTemplate(String templateType) {
        switch (templateType) {
            case TEMPLATE_KITCHEN_CHECKER:
                return createDefaultKitchenCheckerTemplate();
            case TEMPLATE_CUSTOMER_BILL:
                return createDefaultCustomerBillTemplate();
            case TEMPLATE_PAYMENT_RECEIPT:
                return createDefaultPaymentReceiptTemplate();
            default:
                return new PrintTemplate();
        }
    }

    private PrintTemplate createDefaultKitchenCheckerTemplate() {
        return PrintTemplate.builder()
                .addSection("header")
                .center().bold().doubleHeight()
                .addTextLine("KITCHEN CHECKER")
                .normal()
                .addSeparator()
                .spacingAfter(1)
                .addSection("order_info")
                .left()
                .addTextLine("Order #: {{order_number}}")
                .addTextLine("Table: {{table_number}}")
                .addConditionalLine("has_customer_name")
                .addTextLine("Customer: {{customer_name}}")
                .endConditional()
                .addTextLine("Time: {{current_time}}")
                .addSeparator()
                .spacingAfter(1)
                .addSection("items")
                .left().bold()
                .addTextLine("ITEMS TO PREPARE:")
                .normal()
                .addSeparator()
                .addItemsLoop()
                .emptyText("No items in this order")
                .bold()
                .addTextLine("{{item_quantity}}x {{item_name}}")
                .normal()
                .addConditionalLine("has_notes")
                .addTextLine("Notes: {{item_notes}}")
                .endConditional()
                .addSpacing(1)
                .endLoop()
                .addSection("footer")
                .addSeparator()
                .center()
                .addTextLine("** KITCHEN COPY **")
                .cutPaper(true)
                .build();
    }

    private PrintTemplate createDefaultCustomerBillTemplate() {
        return PrintTemplate.builder()
                .addSection("header")
                .center().bold().doubleHeight()
                .addTextLine("CUSTOMER BILL")
                .normal()
                .spacingAfter(1)
                .addSection("restaurant_info")
                .center()
                .addTextLine("Serendipity")
                .addTextLine("Jalan Durian Barat III no 10")
                .addTextLine("Jakarta, Indonesia")
                .addTextLine("Phone: +62821234568276")
                .addTextLine("@cafeserendipityjagakarsa")
                .addSeparator()
                .spacingAfter(1)
                .addSection("order_info")
                .left()
                .addTextLine("Order #: {{order_number}}")
                .addTextLine("Table: {{table_number}}")
                .addConditionalLine("has_customer_name")
                .addTextLine("Customer: {{customer_name}}")
                .endConditional()
                .addTextLine("Server ID: {{server_id}}")
                .addSeparator()
                .spacingAfter(1)
                .addSection("items")
                .left().bold()
                .addTextLine("ITEMS:")
                .normal()
                .addSeparator()
                .addItemsLoop()
                .emptyText("No items in this order")
                .addTextLine("{{item_name}}")
                .right()
                .addTextLine("{{item_quantity}} x {{item_price}} = {{item_total}}")
                .left()
                .addConditionalLine("has_notes")
                .addTextLine("Note: {{item_notes}}")
                .endConditional()
                .addSpacing(1)
                .endLoop()
                .addSeparator()
                .addSection("totals")
                .left()
                .addTotalLine("Subtotal:", "{{subtotal}}")
                .addConditionalLine("has_tax")
                .endConditional()
                .addTotalLine("TOTAL:", "{{final_amount}}")
                .bold().doubleHeight()
                .spacingAfter(1)
                .cutPaper(true)
                .build();
    }

    private PrintTemplate createDefaultPaymentReceiptTemplate() {
        return PrintTemplate.builder()
                .addSection("header")
                .center().bold().doubleHeight()
                .addTextLine("PAYMENT RECEIPT")
                .normal()
                .spacingAfter(1)
                .addSection("restaurant_info")
                .center()
                .addTextLine("Serendipity")
                .addTextLine("Thank you for dining with us!")
                .addSeparator()
                .spacingAfter(1)
                .addSection("receipt_info")
                .left()
                .addTextLine("Receipt #: {{receipt_number}}")
                .addTextLine("Order #: {{order_number}}")
                .addTextLine("Table: {{table_number}}")
                .addTextLine("Date: {{current_time}}")
                .addSeparator()
                .spacingAfter(1)
                .addSection("payment_details")
                .left()
                .addTotalLine("Total Amount:", "{{final_amount}}")
                .addTotalLine("Payment Method:", "{{payment_method}}")
                .addTotalLine("Amount Paid:", "{{amount_paid}}")
                .addConditionalLine("has_change")
                .endConditional()
                .bold().doubleHeight()
                .addTotalLine("PAID:", "{{final_amount}}")
                .normal()
                .spacingAfter(1)
                .addSection("footer")
                .center()
                .addSeparator()
                .addTextLine("PAYMENT COMPLETED")
                .addTextLine("Please keep this receipt")
                .cutPaper(true)
                .build();
    }
}