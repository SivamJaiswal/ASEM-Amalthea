package tools.vitruv.methodologist.amalthea.asem.vsum;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;

import org.eclipse.app4mc.amalthea.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import edu.kit.ipd.sdq.metamodels.asem.classifiers.*;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.*;

import tools.vitruv.framework.vsum.VirtualModel;

/**
 * AsemToAmaltheaTest
 *
 * Tests every existence (E) and property (P) rule that starts on the
 * ASEM side and must produce a consistent state on the AMALTHEA side.
 *
 * Rules covered: E3, E4, E7, E8, E12, E13
 *                P2, P4, P6, P7
 *
 * Each test creates its own VSUM via @TempDir — no shared state between tests.
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class AsemToAmaltheaTest {

    // ── E3 / E4 — Module ↔ Component ─────────────────────────────────────────

    @Test
    @DisplayName("E3 – Module created → Component with same name exists in AMALTHEA view")
    void e3_moduleCreated_componentCreated(@TempDir Path tempDir) {
        VirtualModel vsum = VSUMRunner.createDefaultVirtualModel(tempDir);

        Module module = VSUMRunner.addModule(vsum, tempDir, "FuelPump");

        // the Component must appear in the AMALTHEA view
        assertEquals(1,
                VSUMRunner.getView(vsum, List.of(Component.class)).getRootObjects().size(),
                "exactly one Component must exist after Module creation");

        Component comp = VSUMRunner.getCorresponding(vsum, module, Component.class);
        assertNotNull(comp, "correspondence between Module and Component must exist");
        assertEquals("FuelPump", comp.getName(),
                "Component.name must match Module.name");
    }

    @Test
    @DisplayName("E4 – Module deleted → corresponding Component is removed from AMALTHEA")
    void e4_moduleDeleted_componentRemoved(@TempDir Path tempDir) {
        VirtualModel vsum = VSUMRunner.createDefaultVirtualModel(tempDir);
        Module module     = VSUMRunner.addModule(vsum, tempDir, "OilPressure");
        Component comp    = VSUMRunner.getCorresponding(vsum, module, Component.class);

        VSUMRunner.deleteAndPropagate(vsum, module);

        assertEquals(0,
                VSUMRunner.getView(vsum, List.of(Component.class)).getRootObjects().size(),
                "Component must be removed after Module deletion");
        assertFalse(VSUMRunner.isAlive(comp));
    }

    // ── E7 / E8 — Method ↔ Runnable ──────────────────────────────────────────

    @Test
    @DisplayName("E7 – Void no-param Method created → Runnable in parent Component")
    void e7_voidMethodCreated_runnableCreated(@TempDir Path tempDir) {
        VirtualModel vsum = VSUMRunner.createDefaultVirtualModel(tempDir);
        Module module     = VSUMRunner.addModule(vsum, tempDir, "Throttle");

        // only void no-param Methods trigger a Runnable — see Rule 4 OCL guard
        Method method = VSUMRunner.addVoidMethod(vsum, module, "openValve");

        Runnable runnable = VSUMRunner.getCorresponding(vsum, method,  Runnable.class);
        Component comp    = VSUMRunner.getCorresponding(vsum, module, Component.class);

        assertNotNull(runnable, "Runnable must be created for void Method");
        assertEquals("openValve", runnable.getName());
        assertTrue(comp.getRunnables().contains(runnable),
                "Runnable must be contained in Component.runnables");
    }

    @Test
    @DisplayName("E7 – Method with returnType set is ignored (Rule 4 OCL guard)")
    void e7_methodWithReturnType_notPropagated(@TempDir Path tempDir) {
        VirtualModel vsum = VSUMRunner.createDefaultVirtualModel(tempDir);
        Module module     = VSUMRunner.addModule(vsum, tempDir, "ComputeUnit");

        // create a Method that violates the Rule 4 invariant — returnType is set
        Method method = AsemFactory.eINSTANCE.createMethod();
        method.setName("computeResult");
        ReturnType rt = AsemFactory.eINSTANCE.createReturnType();
        method.setReturnType(rt);           // this disqualifies it from E7
        module.getMethods().add(method);
        vsum.propagateChange(method);

        // no Runnable should have been created for this Method
        assertNull(VSUMRunner.getCorresponding(vsum, method, Runnable.class),
                "Methods with a returnType must NOT produce a Runnable");
    }

    @Test
    @DisplayName("E8 – Method deleted → Runnable is removed from parent Component")
    void e8_methodDeleted_runnableRemoved(@TempDir Path tempDir) {
        VirtualModel vsum = VSUMRunner.createDefaultVirtualModel(tempDir);
        Module module     = VSUMRunner.addModule(vsum, tempDir, "Clutch");
        Method method     = VSUMRunner.addVoidMethod(vsum, module, "disengage");
        Runnable runnable = VSUMRunner.getCorresponding(vsum, method, Runnable.class);

        VSUMRunner.deleteAndPropagate(vsum, method);

        assertFalse(VSUMRunner.isAlive(runnable),
                "Runnable must be removed when its Method is deleted");
    }

    // ── E12 / E13 — Message / Constant ↔ Label ───────────────────────────────

    @Test
    @DisplayName("E12 – Message created → non-constant Label in parent Component")
    void e12_messageCreated_nonConstantLabelCreated(@TempDir Path tempDir) {
        VirtualModel vsum = VSUMRunner.createDefaultVirtualModel(tempDir);
        Module module     = VSUMRunner.addModule(vsum, tempDir, "PressureSensor");
        Message msg       = VSUMRunner.addMessage(vsum, module, "oilPressure");

        Label label    = VSUMRunner.getCorresponding(vsum, msg,    Label.class);
        Component comp = VSUMRunner.getCorresponding(vsum, module, Component.class);

        assertNotNull(label, "Label must be created for Message");
        assertEquals("oilPressure", label.getName());
        assertFalse(label.isConstant(),
                "Label.constant must be false for a Message (Rule 6)");
        assertTrue(comp.getLabels().contains(label),
                "Label must be in Component.labels");
    }

    @Test
    @DisplayName("E13 – Constant created → constant Label in parent Component")
    void e13_constantCreated_constantLabelCreated(@TempDir Path tempDir) {
        VirtualModel vsum  = VSUMRunner.createDefaultVirtualModel(tempDir);
        Module module      = VSUMRunner.addModule(vsum, tempDir, "ThrottleConfig");
        Constant asemConst = VSUMRunner.addConstant(vsum, module, "MAX_THROTTLE");

        Label label    = VSUMRunner.getCorresponding(vsum, asemConst, Label.class);
        Component comp = VSUMRunner.getCorresponding(vsum, module,    Component.class);

        assertNotNull(label, "Label must be created for Constant");
        assertEquals("MAX_THROTTLE", label.getName());
        assertTrue(label.isConstant(),
                "Label.constant must be true for a Constant (Rule 5)");
        assertTrue(comp.getLabels().contains(label),
                "Label must be in Component.labels");
    }

    // ── P2 — Module.name → Component.name ────────────────────────────────────

    @Test
    @DisplayName("P2 – Module.name changed → Component.name updated")
    void p2_moduleRenamed_componentNameUpdated(@TempDir Path tempDir) {
        VirtualModel vsum = VSUMRunner.createDefaultVirtualModel(tempDir);
        Module module     = VSUMRunner.addModule(vsum, tempDir, "InitialName");

        module.setName("UpdatedName");
        vsum.propagateChange(module);

        Component comp = VSUMRunner.getCorresponding(vsum, module, Component.class);
        assertEquals("UpdatedName", comp.getName(),
                "Component.name must follow Module.name after rename");
    }

    // ── P4 — Method.name → Runnable.name ─────────────────────────────────────

    @Test
    @DisplayName("P4 – Method.name changed → Runnable.name updated")
    void p4_methodRenamed_runnableNameUpdated(@TempDir Path tempDir) {
        VirtualModel vsum = VSUMRunner.createDefaultVirtualModel(tempDir);
        Module module     = VSUMRunner.addModule(vsum, tempDir, "MCU");
        Method method     = VSUMRunner.addVoidMethod(vsum, module, "oldOp");

        method.setName("newOp");
        vsum.propagateChange(method);

        Runnable runnable = VSUMRunner.getCorresponding(vsum, method, Runnable.class);
        assertEquals("newOp", runnable.getName());
    }

    // ── P6 — Message.name → Label.name ───────────────────────────────────────

    @Test
    @DisplayName("P6 – Message.name changed → Label.name updated")
    void p6_messageRenamed_labelNameUpdated(@TempDir Path tempDir) {
        VirtualModel vsum = VSUMRunner.createDefaultVirtualModel(tempDir);
        Module module     = VSUMRunner.addModule(vsum, tempDir, "CAN_Node");
        Message msg       = VSUMRunner.addMessage(vsum, module, "oldFrame");

        msg.setName("newFrame");
        vsum.propagateChange(msg);

        Label label = VSUMRunner.getCorresponding(vsum, msg, Label.class);
        assertEquals("newFrame", label.getName());
    }

    // ── P7 — Constant.name → Label.name ──────────────────────────────────────

    @Test
    @DisplayName("P7 – Constant.name changed → Label.name updated")
    void p7_constantRenamed_labelNameUpdated(@TempDir Path tempDir) {
        VirtualModel vsum  = VSUMRunner.createDefaultVirtualModel(tempDir);
        Module module      = VSUMRunner.addModule(vsum, tempDir, "LIN_Node");
        Constant asemConst = VSUMRunner.addConstant(vsum, module, "OLD_PARAM");

        asemConst.setName("NEW_PARAM");
        vsum.propagateChange(asemConst);

        Label label = VSUMRunner.getCorresponding(vsum, asemConst, Label.class);
        assertEquals("NEW_PARAM", label.getName());
    }
}
