/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.ac.lancs.scc.phd.jesse;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author comqdhb
 */
public class JP {

    /**
     *
     * @param args don't use
     * @throws IOException stuff might not exist
     */
    public static void main(String[] args) throws IOException {
        JavaParser jp = createJavaParser(false);
        /* code to test
        
         */
        String code = "//grr\n"
                + "/**\n"
                + " @return int\n"
                + "*/\n"
                + "    public /* */ int hashCode(/* */) {\n"
                + "        /*\n"
                + "        don't print\n"
                + "        */\n"
                + "        return seq.hashCode();\n"
                + "        /* grr */\n"
                + "\n// grrr\n"
                + "    }";
        //init view
        System.out.println("starting code: \n" + code);
        System.out.println("----");
        //assume a mody (method)
        ParseResult<BodyDeclaration<?>> r = jp.parseBodyDeclaration(code);
        System.out.println("looks like:\n" + r.getResult().get());
        BodyDeclaration<?> z = r.getResult().get();
        removeComments(z);
        System.out.println(">>>\n" + z + "\n<<<\n");
        StringWriter sw = new StringWriter();
        LexicalPreservingPrinter.print(z, sw);
        System.out.println("\n\n===========\ntransformed into:\n" + sw);

        //now try with a file
        File f = new File("./src/main/java/uk/ac/lancs/scc/phd/jesse/JP.java");
        if (f.exists()) {
            ParseResult<CompilationUnit> cu = jp.parse(new FileInputStream(f));

            for (Problem p : cu.getProblems()) {
                System.out.println("" + p);
            }
            if (cu.getProblems().size() == 0) {
                CompilationUnit c = cu.getResult().get();
                removeComments(c);
                System.out.println("+++++\n" + c);
            }
        } else {
            System.out.println("Where is " + f);
        }
    }

    /**
     * Create a source parser . We set up LexicalPreservationEnabled by true,
     * Keep all the syntax in the source code .
     *
     * @return JavaParser
     */
    public static JavaParser createJavaParser(boolean preserve) {
        ParserConfiguration parserConfiguration = new ParserConfiguration();
        if (preserve){parserConfiguration.setLexicalPreservationEnabled(true);}
        return new JavaParser(parserConfiguration);
    }

    /**
     * We only recognize single line comments and block comments
     *
     * @param comment
     * @return true if meet the correct type
     */
    private static boolean isValidCommentType(Comment comment) {
        return (comment instanceof LineComment || comment instanceof BlockComment) && !(comment instanceof JavadocComment);
    }

    public static void removeComments(BodyDeclaration<?> get) {
        List<Comment> comments = get.getAllContainedComments();
        List<Comment> unwantedComments = comments
                .stream()
                .filter(JP::isValidCommentType)
                .collect(Collectors.toList());
        for (Comment c : unwantedComments) {
            get.remove(c);
            try {
                c.remove();
            } catch (Exception e) {
            }
        }

    }

    public static void removeComments(CompilationUnit compilationUnit) {
        List<Comment> comments = compilationUnit.getAllContainedComments();
        List<Comment> unwantedComments = comments
                .stream()
                .filter(JP::isValidCommentType)
                .collect(Collectors.toList());
        for (Comment c : unwantedComments) {
            compilationUnit.remove(c);
            try {
                c.remove();
            } catch (Exception e) {
            }
        }
    }
}
