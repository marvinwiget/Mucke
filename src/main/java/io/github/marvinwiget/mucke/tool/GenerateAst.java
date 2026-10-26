package io.github.marvinwiget.mucke.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

// java src/main/java/io/github/marvinwiget/mucke/tool/GenerateAst.java src/main/java/io/github/marvinwiget/mucke/interpreter/

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }
        String outputDir = args[0];
        defineNode(outputDir, "Program", "List<ConfigEntry> config, List<Stmt> main");
        defineNode(outputDir, "Field", "Token name, Expr value");

        defineAst(outputDir, "ConfigEntry", Arrays.asList(
            "Setting : Token name, Expr value",
            "Instrument : Token name, List<Field> fields"
        ));
        defineAst(outputDir, "Stmt", Arrays.asList(
            "Assign : Token name, Expr value",
            "Set : Token object, Token property, Expr value",
            "Call : Token object, Token method, List<Expr> arguments"
        ));
        defineAst(outputDir, "Expr", Arrays.asList(
            "Literal : Object value",
            "Ratio : Token numerator, Token denominator",
            "Note : Token note",
            "Rest : Token keyword",
            "ListLiteral : List<Expr> elements"
        ));
    }

    private static void defineNode(
            String outputDir, String className, String fieldList)
            throws IOException {
        String path = outputDir + "/" + className + ".java";
        try (PrintWriter writer = new PrintWriter(path, "UTF-8")) {
            writer.println("package io.github.marvinwiget.mucke.interpreter;");
            writer.println();
            writer.println("import java.util.List;");
            writer.println();
            writer.println("class " + className + " {");
            writeConstructor(writer, "    ", className, fieldList);
            writer.println();
            writeFields(writer, "    ", fieldList);
            writer.println("}");
            writer.println();
        }
    }

    private static void defineAst(
            String outputDir, String baseName, List<String> types) 
            throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        try (PrintWriter writer = new PrintWriter(path, "UTF-8")) {
            writer.println("package io.github.marvinwiget.mucke.interpreter;");
            writer.println();
            writer.println("import java.util.List;");
            writer.println();
            writer.println("abstract class " + baseName + " {");

            for (String type : types) {
                String[] parts = type.split(":", 2);
                String className = parts[0].trim();
                String fields = parts[1].trim();
                defineType(writer, baseName, className, fields);
            }

            writer.println("}");
            writer.println();
        }
    }

    private static void defineType(
            PrintWriter writer, String baseName,
            String className, String fieldList) {
        
        writer.println("    static class " + className + " extends " + baseName + " {");
        writeConstructor(writer, "        ", className, fieldList);
        
        writer.println();
        writeFields(writer, "        ", fieldList);
        writer.println("    }");
        writer.println();
    }

    private static void writeConstructor(
            PrintWriter writer, String indent, String className, String fieldList) {
        writer.println(indent + className + "(" + fieldList + ") {");
        for (String field : fieldList.split(", ")) {
            String name = field.substring(field.lastIndexOf(' ') + 1);
            writer.println(indent + "    this." + name + " = " + name + ";");
        }
        writer.println(indent + "}");
    }

    private static void writeFields(
            PrintWriter writer, String indent, String fieldList) {
        for (String field : fieldList.split(", ")) {
            writer.println(indent + "final " + field + ";");
        }
    }
}
