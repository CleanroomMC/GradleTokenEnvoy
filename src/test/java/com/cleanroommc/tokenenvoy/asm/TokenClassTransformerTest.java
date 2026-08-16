package com.cleanroommc.tokenenvoy.asm;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TokenClassTransformerTest {

    @Test
    void replacesFieldConstantsMethodStringsAndAnnotations() {
        byte[] original = sampleClass();
        byte[] transformed = TokenClassTransformer.transform(original, Map.of(
                "VERSION", "1.2.3",
                "MOD_ID", "example"
        ));
        assertNotEquals(new String(original), new String(transformed));
        String pool = new String(transformed);
        assertEquals(-1, pool.indexOf("@{VERSION}"));
        assertEquals(-1, pool.indexOf("@{MOD_ID}"));

        ClassNode node = new ClassNode();
        new ClassReader(transformed).accept(node, 0);

        FieldNode version = node.fields.stream().filter(field -> field.name.equals("VERSION")).findFirst().orElseThrow();
        assertEquals("1.2.3", version.value);

        MethodNode message = node.methods.stream().filter(method -> method.name.equals("message")).findFirst().orElseThrow();
        Object ldc = message.instructions.toArray()[0];
        assertInstanceOf(LdcInsnNode.class, ldc);
        assertEquals("mod=example", ((LdcInsnNode) ldc).cst);

        AnnotationNode annotation = node.visibleAnnotations.getFirst();
        assertEquals("1.2.3", annotation.values.get(1));
    }

    @Test
    void doesNotRewriteMethodNames() {
        byte[] transformed = TokenClassTransformer.transform(sampleClass(), Map.of("message", "renamed"));
        ClassNode node = new ClassNode();
        new ClassReader(transformed).accept(node, 0);
        assertEquals("message", node.methods.stream()
                .filter(method -> method.desc.equals("()Ljava/lang/String;"))
                .findFirst()
                .orElseThrow()
                .name);
    }

    @Test
    void replacesInvokeDynamicBootstrapStrings() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "example/Concat", null, "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "recipe", "()V", null, null);
        method.visitCode();
        method.visitInvokeDynamicInsn("makeConcatWithConstants", "()Ljava/lang/String;",
                new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/StringConcatFactory", "makeConcatWithConstants",
                        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;", false),
                "hello @{VERSION}");
        method.visitInsn(Opcodes.POP);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(1, 0);
        method.visitEnd();
        writer.visitEnd();

        ClassNode node = new ClassNode();
        new ClassReader(TokenClassTransformer.transform(writer.toByteArray(), Map.of("VERSION", "9"))).accept(node, 0);
        Object recipe = node.methods.getFirst().instructions.toArray()[0];
        org.objectweb.asm.tree.InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) recipe;
        assertEquals("hello 9", indy.bsmArgs[0]);
    }

    private static byte[] sampleClass() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "example/Sample", null, "java/lang/Object", null);
        AnnotationVisitor annotation = writer.visitAnnotation("Lexample/Mod;", true);
        annotation.visit("version", "@{VERSION}");
        annotation.visitEnd();

        FieldVisitor field = writer.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "VERSION", "Ljava/lang/String;", null, "@{VERSION}");
        field.visitEnd();

        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "message", "()Ljava/lang/String;", null, null);
        method.visitCode();
        method.visitLdcInsn("mod=@{MOD_ID}");
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(1, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

}
