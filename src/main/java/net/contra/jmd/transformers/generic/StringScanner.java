package net.contra.jmd.transformers.generic;

import net.contra.jmd.util.LogHandler;
import net.contra.jmd.util.NonClassEntries;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;

import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by IntelliJ IDEA.
 * User: Eric
 * Date: Nov 30, 2010
 * Time: 9:25:02 PM
 */
public class StringScanner {
    private static LogHandler logger = new LogHandler("StringScanner");
    private Map<String, ClassGen> cgs = new HashMap<String, ClassGen>();
    boolean replaceMode = false;
    String substitute = "";
    String JAR_NAME;
    String inputScan = "";

    public StringScanner(String jarfile, String scanstring, boolean replace, String replacestring) throws Exception {
        inputScan = scanstring;
        replaceMode = replace;
        substitute = replacestring;
        File jar = new File(jarfile);
        JAR_NAME = jarfile;
        JarFile jf = new JarFile(jar);
        Enumeration<JarEntry> entries = jf.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry == null) {
                break;
            }
            if (entry.isDirectory()) {
            }
            if (entry.getName().endsWith(".class")) {
                ClassGen cg = new ClassGen(new ClassParser(jf.getInputStream(entry), entry.getName()).parse());
                cgs.put(cg.getClassName(), cg);
            } else {
                NonClassEntries.add(entry, jf.getInputStream(entry));
            }
        }
    }

    public void searchConstantPool() {
        for (ClassGen cg : cgs.values()) {
            for (Method m : cg.getMethods()) {
                MethodGen mg = new MethodGen(m, cg.getClassName(), cg.getConstantPool());
                InstructionList list = mg.getInstructionList();
                InstructionHandle[] handles;
                if (list != null && list.size() > 0) {
                    handles = list.getInstructionHandles();
                } else {
                    break;
                }
                for (int x = 0; x < handles.length; x++) {
                    if (handles[x].getInstruction() instanceof LDC) {
                        LDC newldc = (LDC) handles[x].getInstruction();
                        String val = newldc.getValue(cg.getConstantPool()).toString();
                        if (val.contains(inputScan)) {
                            if (!replaceMode) {
                                logger.log(val + " in " + cg.getClassName() + "." + m.getName());
                            } else {
                                String newz = val.replace(inputScan, substitute);
                                int stringRef = cg.getConstantPool().addString(newz);
                                handles[x].setInstruction(new LDC(stringRef));
                                logger.log(val + "->" + newz + " in " + cg.getClassName() + "." + m.getName());
                            }
                        }
                    }
                }
                if (replaceMode) {
                    list.setPositions();
                    mg.setMaxLocals();
                    mg.setMaxStack();
                    cg.replaceMethod(m, mg.getMethod());
                }
            }
        }

    }

    public void scan() {
        logger.log("Generic URL Scanner/Replacer");
        if (!replaceMode) {
            logger.log("Scanning for Strings containing " + inputScan);
        } else {
            logger.log("Replacing " + inputScan + " with " + substitute);
        }
        searchConstantPool();
        logger.log("Operation Completed.");

    }
}
