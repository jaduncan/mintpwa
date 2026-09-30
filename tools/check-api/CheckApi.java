// Verifies that every android.* method/field referenced by the compiled app exists with the exact
// same signature in a real platform android.jar. Guards against mistakes in build-support/stubs.
// Classes newer than that jar are listed separately for manual review.
// SPDX-License-Identifier: GPL-3.0-or-later
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class CheckApi {
    public static void main(String[] a) throws Exception {
        URLClassLoader cl = new URLClassLoader(new URL[] {Paths.get(a[0]).toUri().toURL()}, null);
        Set<String> allowed = new HashSet<>();
        if (a.length > 2)
            for (String l : Files.readAllLines(Paths.get(a[2]))) {
                l = l.replaceAll("#.*", "").trim();
                if (!l.isEmpty()) allowed.add(l);
            }
        int missing = 0, newer = 0, ok = 0;
        for (String line : new TreeSet<>(Files.readAllLines(Paths.get(a[1])))) {
            if (line.isEmpty()) continue;
            boolean field = line.startsWith("F:");
            String s = field ? line.substring(2) : line;
            int colon = s.indexOf(':'), dot = s.lastIndexOf('.', colon);
            String owner = s.substring(0, dot).replace('/', '.'), name = s.substring(dot + 1, colon),
                    desc = s.substring(colon + 1);
            if (name.startsWith("\"")) name = name.replace("\"", "");
            Class<?> c;
            try { c = Class.forName(owner, false, cl); }
            catch (Throwable t) { System.out.println("NEWER   " + line); newer++; continue; }
            if (found(c, field, name, desc)) ok++;
            else if (allowed.contains(line)) { System.out.println("NEWER   " + line + "  (allow-listed)"); newer++; }
            else { System.out.println("MISSING " + line); missing++; }
        }
        System.out.println(ok + " verified, " + newer + " newer than jar (review), " + missing + " missing");
        System.exit(missing == 0 ? 0 : 1);
    }

    static boolean found(Class<?> c, boolean field, String name, String desc) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            if (field) { for (Field x : k.getDeclaredFields()) if (x.getName().equals(name)) return true; continue; }
            if (name.equals("<init>")) {
                for (Constructor<?> x : k.getDeclaredConstructors())
                    if (d(x.getParameterTypes(), void.class).equals(desc)) return true;
                return false;
            }
            for (Method x : k.getDeclaredMethods())
                if (x.getName().equals(name) && d(x.getParameterTypes(), x.getReturnType()).equals(desc)) return true;
            for (Class<?> i : k.getInterfaces())
                for (Method x : i.getMethods())
                    if (x.getName().equals(name) && d(x.getParameterTypes(), x.getReturnType()).equals(desc)) return true;
        }
        return false;
    }

    static String t(Class<?> c) {
        if (c.isArray()) return "[" + t(c.getComponentType());
        if (c == void.class) return "V"; if (c == int.class) return "I"; if (c == boolean.class) return "Z";
        if (c == float.class) return "F"; if (c == long.class) return "J"; if (c == byte.class) return "B";
        if (c == char.class) return "C"; if (c == double.class) return "D"; if (c == short.class) return "S";
        return "L" + c.getName().replace('.', '/') + ";";
    }

    static String d(Class<?>[] p, Class<?> r) {
        StringBuilder b = new StringBuilder("(");
        for (Class<?> x : p) b.append(t(x));
        return b.append(")").append(t(r)).toString();
    }
}
