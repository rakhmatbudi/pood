package com.restaurant.management.printing;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a complete print template with sections and formatting
 */
public class PrintTemplate {
    private List<Section> sections = new ArrayList<>();
    private boolean cutPaper = true;

    public static PrintTemplate fromJson(String jsonString) throws JSONException {
        JSONObject root = new JSONObject(jsonString);
        PrintTemplate template = new PrintTemplate();

        template.cutPaper = root.optBoolean("cut_paper", true);

        JSONArray sectionsArray = root.optJSONArray("sections");
        if (sectionsArray != null) {
            for (int i = 0; i < sectionsArray.length(); i++) {
                JSONObject sectionObj = sectionsArray.getJSONObject(i);
                template.sections.add(Section.fromJson(sectionObj));
            }
        }

        return template;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject root = new JSONObject();
        root.put("cut_paper", cutPaper);

        JSONArray sectionsArray = new JSONArray();
        for (Section section : sections) {
            sectionsArray.put(section.toJson());
        }
        root.put("sections", sectionsArray);

        return root;
    }

    public static TemplateBuilder builder() {
        return new TemplateBuilder();
    }

    // Getters and setters
    public List<Section> getSections() { return sections; }
    public void setSections(List<Section> sections) { this.sections = sections; }
    public boolean shouldCutPaper() { return cutPaper; }
    public void setCutPaper(boolean cutPaper) { this.cutPaper = cutPaper; }

    /**
     * Represents a section in the template (header, body, footer, etc.)
     */
    public static class Section {
        private String name;
        private Formatting formatting = new Formatting();
        private List<Line> lines = new ArrayList<>();
        private int spacingAfter = 0;

        public static Section fromJson(JSONObject json) throws JSONException {
            Section section = new Section();
            section.name = json.optString("name", "");
            section.spacingAfter = json.optInt("spacing_after", 0);

            JSONObject formattingObj = json.optJSONObject("formatting");
            if (formattingObj != null) {
                section.formatting = Formatting.fromJson(formattingObj);
            }

            JSONArray linesArray = json.optJSONArray("lines");
            if (linesArray != null) {
                for (int i = 0; i < linesArray.length(); i++) {
                    JSONObject lineObj = linesArray.getJSONObject(i);
                    section.lines.add(Line.fromJson(lineObj));
                }
            }

            return section;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("name", name);
            json.put("spacing_after", spacingAfter);
            json.put("formatting", formatting.toJson());

            JSONArray linesArray = new JSONArray();
            for (Line line : lines) {
                linesArray.put(line.toJson());
            }
            json.put("lines", linesArray);

            return json;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Formatting getFormatting() { return formatting; }
        public void setFormatting(Formatting formatting) { this.formatting = formatting; }
        public List<Line> getLines() { return lines; }
        public void setLines(List<Line> lines) { this.lines = lines; }
        public int getSpacingAfter() { return spacingAfter; }
        public void setSpacingAfter(int spacingAfter) { this.spacingAfter = spacingAfter; }
    }

    /**
     * Represents a line in the template
     */
    public static class Line {
        private String type = "text"; // text, separator, items_loop, conditional, total_line
        private String content = "";
        private Formatting formatting = new Formatting();
        private String condition; // For conditional lines
        private List<Line> subLines = new ArrayList<>(); // For loops and conditionals
        private String emptyText; // For items_loop when no items
        private String label; // For total_line
        private String amount; // For total_line
        private int charWidth = 32; // For total_line formatting

        public static Line fromJson(JSONObject json) throws JSONException {
            Line line = new Line();
            line.type = json.optString("type", "text");
            line.content = json.optString("content", "");
            line.condition = json.optString("condition");
            line.emptyText = json.optString("empty_text");
            line.label = json.optString("label");
            line.amount = json.optString("amount");
            line.charWidth = json.optInt("char_width", 32);

            JSONObject formattingObj = json.optJSONObject("formatting");
            if (formattingObj != null) {
                line.formatting = Formatting.fromJson(formattingObj);
            }

            JSONArray subLinesArray = json.optJSONArray("sub_lines");
            if (subLinesArray != null) {
                for (int i = 0; i < subLinesArray.length(); i++) {
                    JSONObject subLineObj = subLinesArray.getJSONObject(i);
                    line.subLines.add(Line.fromJson(subLineObj));
                }
            }

            return line;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("type", type);
            json.put("content", content);
            if (condition != null) json.put("condition", condition);
            if (emptyText != null) json.put("empty_text", emptyText);
            if (label != null) json.put("label", label);
            if (amount != null) json.put("amount", amount);
            json.put("char_width", charWidth);
            json.put("formatting", formatting.toJson());

            if (!subLines.isEmpty()) {
                JSONArray subLinesArray = new JSONArray();
                for (Line subLine : subLines) {
                    subLinesArray.put(subLine.toJson());
                }
                json.put("sub_lines", subLinesArray);
            }

            return json;
        }

        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Formatting getFormatting() { return formatting; }
        public void setFormatting(Formatting formatting) { this.formatting = formatting; }
        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }
        public List<Line> getSubLines() { return subLines; }
        public void setSubLines(List<Line> subLines) { this.subLines = subLines; }
        public String getEmptyText() { return emptyText; }
        public void setEmptyText(String emptyText) { this.emptyText = emptyText; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }
        public int getCharWidth() { return charWidth; }
        public void setCharWidth(int charWidth) { this.charWidth = charWidth; }
    }

    /**
     * Represents formatting options for sections and lines
     */
    public static class Formatting {
        private String align = "left"; // left, center, right
        private boolean bold = false;
        private boolean doubleHeight = false;

        public static Formatting fromJson(JSONObject json) throws JSONException {
            Formatting formatting = new Formatting();
            formatting.align = json.optString("align", "left");
            formatting.bold = json.optBoolean("bold", false);
            formatting.doubleHeight = json.optBoolean("double_height", false);
            return formatting;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("align", align);
            json.put("bold", bold);
            json.put("double_height", doubleHeight);
            return json;
        }

        // Getters and setters
        public String getAlign() { return align; }
        public void setAlign(String align) { this.align = align; }
        public boolean isBold() { return bold; }
        public void setBold(boolean bold) { this.bold = bold; }
        public boolean isDoubleHeight() { return doubleHeight; }
        public void setDoubleHeight(boolean doubleHeight) { this.doubleHeight = doubleHeight; }
    }

    /**
     * Builder pattern for creating templates programmatically
     */
    public static class TemplateBuilder {
        private PrintTemplate template = new PrintTemplate();
        private Section currentSection;
        private Line currentLine;
        private Formatting currentFormatting = new Formatting();

        public TemplateBuilder addSection(String name) {
            currentSection = new Section();
            currentSection.setName(name);
            currentSection.setFormatting(new Formatting());
            template.sections.add(currentSection);
            return this;
        }

        public TemplateBuilder left() {
            if (currentSection != null) {
                currentSection.getFormatting().setAlign("left");
            }
            currentFormatting.setAlign("left");
            return this;
        }

        public TemplateBuilder center() {
            if (currentSection != null) {
                currentSection.getFormatting().setAlign("center");
            }
            currentFormatting.setAlign("center");
            return this;
        }

        public TemplateBuilder right() {
            if (currentSection != null) {
                currentSection.getFormatting().setAlign("right");
            }
            currentFormatting.setAlign("right");
            return this;
        }

        public TemplateBuilder bold() {
            if (currentSection != null) {
                currentSection.getFormatting().setBold(true);
            }
            currentFormatting.setBold(true);
            return this;
        }

        public TemplateBuilder normal() {
            if (currentSection != null) {
                currentSection.getFormatting().setBold(false);
                currentSection.getFormatting().setDoubleHeight(false);
            }
            currentFormatting.setBold(false);
            currentFormatting.setDoubleHeight(false);
            return this;
        }

        public TemplateBuilder doubleHeight() {
            if (currentSection != null) {
                currentSection.getFormatting().setDoubleHeight(true);
            }
            currentFormatting.setDoubleHeight(true);
            return this;
        }

        public TemplateBuilder addTextLine(String content) {
            Line line = new Line();
            line.setType("text");
            line.setContent(content);
            line.setFormatting(copyFormatting(currentFormatting));
            if (currentSection != null) {
                currentSection.getLines().add(line);
            }
            return this;
        }

        public TemplateBuilder addSeparator() {
            return addSeparator("--------------------------------");
        }

        public TemplateBuilder addSeparator(String separator) {
            Line line = new Line();
            line.setType("separator");
            line.setContent(separator);
            line.setFormatting(copyFormatting(currentFormatting));
            if (currentSection != null) {
                currentSection.getLines().add(line);
            }
            return this;
        }

        public TemplateBuilder addSpacing(int lines) {
            for (int i = 0; i < lines; i++) {
                addTextLine("");
            }
            return this;
        }

        public TemplateBuilder spacingAfter(int lines) {
            if (currentSection != null) {
                currentSection.setSpacingAfter(lines);
            }
            return this;
        }

        public ConditionalBuilder addConditionalLine(String condition) {
            Line line = new Line();
            line.setType("conditional");
            line.setCondition(condition);
            line.setFormatting(copyFormatting(currentFormatting));
            if (currentSection != null) {
                currentSection.getLines().add(line);
            }
            currentLine = line;
            return new ConditionalBuilder(this, line);
        }

        public ItemsLoopBuilder addItemsLoop() {
            Line line = new Line();
            line.setType("items_loop");
            line.setFormatting(copyFormatting(currentFormatting));
            if (currentSection != null) {
                currentSection.getLines().add(line);
            }
            currentLine = line;
            return new ItemsLoopBuilder(this, line);
        }

        public TemplateBuilder addTotalLine(String label, String amount) {
            return addTotalLine(label, amount, 32);
        }

        public TemplateBuilder addTotalLine(String label, String amount, int charWidth) {
            Line line = new Line();
            line.setType("total_line");
            line.setLabel(label);
            line.setAmount(amount);
            line.setCharWidth(charWidth);
            line.setFormatting(copyFormatting(currentFormatting));
            if (currentSection != null) {
                currentSection.getLines().add(line);
            }
            return this;
        }

        public TemplateBuilder cutPaper(boolean cut) {
            template.setCutPaper(cut);
            return this;
        }

        public PrintTemplate build() {
            return template;
        }

        private Formatting copyFormatting(Formatting source) {
            Formatting copy = new Formatting();
            copy.setAlign(source.getAlign());
            copy.setBold(source.isBold());
            copy.setDoubleHeight(source.isDoubleHeight());
            return copy;
        }

        /**
         * Builder for conditional lines
         */
        public static class ConditionalBuilder {
            private final TemplateBuilder parent;
            private final Line conditionalLine;

            public ConditionalBuilder(TemplateBuilder parent, Line conditionalLine) {
                this.parent = parent;
                this.conditionalLine = conditionalLine;
            }

            public ConditionalBuilder addTextLine(String content) {
                Line line = new Line();
                line.setType("text");
                line.setContent(content);
                line.setFormatting(parent.copyFormatting(parent.currentFormatting));
                conditionalLine.getSubLines().add(line);
                return this;
            }

            public ConditionalBuilder addSeparator() {
                Line line = new Line();
                line.setType("separator");
                line.setContent("--------------------------------");
                line.setFormatting(parent.copyFormatting(parent.currentFormatting));
                conditionalLine.getSubLines().add(line);
                return this;
            }

            public TemplateBuilder endConditional() {
                return parent;
            }
        }

        /**
         * Builder for items loop
         */
        public static class ItemsLoopBuilder {
            private final TemplateBuilder parent;
            private final Line loopLine;
            private Formatting currentFormatting = new Formatting();

            public ItemsLoopBuilder(TemplateBuilder parent, Line loopLine) {
                this.parent = parent;
                this.loopLine = loopLine;
                this.currentFormatting = parent.copyFormatting(parent.currentFormatting);
            }

            public ItemsLoopBuilder bold() {
                currentFormatting.setBold(true);
                return this;
            }

            public ItemsLoopBuilder normal() {
                currentFormatting.setBold(false);
                currentFormatting.setDoubleHeight(false);
                return this;
            }

            public ItemsLoopBuilder left() {
                currentFormatting.setAlign("left");
                return this;
            }

            public ItemsLoopBuilder center() {
                currentFormatting.setAlign("center");
                return this;
            }

            public ItemsLoopBuilder right() {
                currentFormatting.setAlign("right");
                return this;
            }

            public ItemsLoopBuilder addTextLine(String content) {
                Line line = new Line();
                line.setType("text");
                line.setContent(content);
                line.setFormatting(parent.copyFormatting(currentFormatting));
                loopLine.getSubLines().add(line);
                return this;
            }

            public ItemsLoopBuilder addSeparator() {
                Line line = new Line();
                line.setType("separator");
                line.setContent("--------------------------------");
                line.setFormatting(parent.copyFormatting(currentFormatting));
                loopLine.getSubLines().add(line);
                return this;
            }

            public ItemsLoopBuilder addSpacing(int lines) {
                for (int i = 0; i < lines; i++) {
                    addTextLine("");
                }
                return this;
            }

            public ItemsConditionalBuilder addConditionalLine(String condition) {
                Line line = new Line();
                line.setType("conditional");
                line.setCondition(condition);
                line.setFormatting(parent.copyFormatting(currentFormatting));
                loopLine.getSubLines().add(line);
                return new ItemsConditionalBuilder(this, line);
            }

            public ItemsLoopBuilder emptyText(String text) {
                loopLine.setEmptyText(text);
                return this;
            }

            public TemplateBuilder endLoop() {
                return parent;
            }
        }

        /**
         * Builder for conditional lines within items loop
         */
        public static class ItemsConditionalBuilder {
            private final ItemsLoopBuilder parent;
            private final Line conditionalLine;

            public ItemsConditionalBuilder(ItemsLoopBuilder parent, Line conditionalLine) {
                this.parent = parent;
                this.conditionalLine = conditionalLine;
            }

            public ItemsConditionalBuilder addTextLine(String content) {
                Line line = new Line();
                line.setType("text");
                line.setContent(content);
                line.setFormatting(parent.parent.copyFormatting(parent.currentFormatting));
                conditionalLine.getSubLines().add(line);
                return this;
            }

            public ItemsConditionalBuilder addSeparator() {
                Line line = new Line();
                line.setType("separator");
                line.setContent("--------------------------------");
                line.setFormatting(parent.parent.copyFormatting(parent.currentFormatting));
                conditionalLine.getSubLines().add(line);
                return this;
            }

            public ItemsLoopBuilder endConditional() {
                return parent;
            }
        }
    }
}