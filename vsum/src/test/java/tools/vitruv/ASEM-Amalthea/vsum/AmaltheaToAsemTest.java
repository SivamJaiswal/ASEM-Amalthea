package tools.vitruv.methodologist.amalthea.asem.vsum;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.app4mc.amalthea.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import edu.kit.ipd.sdq.metamodels.asem.classifiers.*;
import edu.kit.ipd.sdq.metamodels.asem.dataexchange.*;
import edu.kit.ipd.sdq.metamodels.asem.primitivetypes.*;

import tools.vitruv.framework.vsum.VirtualModel;

/**
 * AmaltheaToAsemTest
 *
 * Tests every existence (E) and property (P) rule that starts on the
 * AMALTHEA side and must produce a consistent state on the ASEM side.
 *
 * Rules covered: E1, E2, E5, E6, E9, E10, E11, E14, E15, E16, E17
 *                P1, P3, P5, P8, P9, P10, P11, P12
 *
 * Each test method creates its own VSUM via @TempDir so tests are
 * completely isolated — no shared state, no ordering dependencies.
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class AmaltheaToAsemTest {

    // ── E1 / E2 — Component ↔ Module ─────────────────────────────────────────

    @Test
    @DisplayName("E1 – Component created → Module with same name exists in ASEM view")
    void e1_componentCreated_moduleCreated(@TempDir Path tempDir) {
        VirtualModel vsum = VSUMRunner.createDefaultVirtualModel(tempDir);

        Component comp = VSUMRunner.addComponent(vsum, tempDir, "BrakeController");

        // the Module must appear in the ASEM view
        assertEquals(1,
                VSUMRunner.getView(vsum, List.of(Module.class)).getRootObjects().size(),
                "exactly one Module must exist after Component creation");

        Module module = VSUMRunner.getCorresponding(vsum, comp, Module.class);
        assertNotNull(module,  "correspondence between Component and Module must exist");
        assertEquals("BrakeController", module.getName(),
                "Module.name must match Component.name");
    }

    @Test
    @DisplayName("E2 – Component deleted → corresponding Module is removed from ASEM")
    void e2_componentDeleted_moduleRemoved(@TempDir Path tempDir) {
        VirtualModel vsum = VSUMRunner.createDefaultVirtualModel(tempDir);
        Component comp = VSUMRunner.addComponent(vsum, tempDir, "TempSensor");
        Module module  = VSUMRunner.getCorresponding(vsum, comp, Module.class);

        VSUMRunner.deleteAndPropagate(vsum, comp);

        assertEquals(0,
                VSUMRunner.getView(vsum, List.of(Module.class)).getRootObjects().size(),
                "Module must be removed after Component deletion");
        assertFalse(VSUMRunner.isAlive(module));
    }

    // ── E5 / E6 — Runnable ↔ Method ──────────────────────────────────────────

    @Test
    @DisplayName("E5 – Runnable created → void no-param Method in parent Module")
    void e5_runnableCreated_methodCreated(@TempDir Path tempDir) {
        VirtualModel vsum = VSUMRunner.createDefaultVirtualModel(tempDir);
        Component comp    = VSUMRunner.addComponent(vsum, tempDir, "IgnitionCtrl");
        Runnable runnable = VSUMRunner.addRunnable(vsum, comp, "fireIgnition");

        Method method = VSUMRunner.getCorresponding(vsum, runnable, Method.class);
        Module module = VSUMRunner.getCorresponding(vsum, comp,     Module.class);

        assertNotNull(method, "Method must be created for Runnable");
        assertEquals("fireIgnition", method.getName());

        // Table 5.1 OCL: self.returnType.oclIsUndefined()
        assertNull(method.getReturnType(),
                "Method.returnType must be null — Rule 4 OCL invariant");

        // Table 5.1 OCL: self.parameters->size() = 0
        assertTrue(method.getParameters().isEmpty(),
                "Method must have no parameters — Rule 4 OCL invariant");

        // structural: Method must be inside the parent Module
        assertTrue(module.getMethods().contains(method),
                "Method must be contained in Module.methods");
    }

    @Test
    @DisplayName("E6 – Runnable deleted → Method is removed from parent Module")
    void e6_runnableDeleted_methodRemoved(@TempDir Path tempDir) {
        VirtualModel vsum = VSUMRunner.createDefaultVirtualModel(tempDir);
        Component comp    = VSUMRunner.addComponent(vsum, tempDir, "GearBox");
        Runnable runnable = VSUMRunner.addRunnable(vsum, comp, "shiftGear");
        Method method     = VSUMRunner.getCorresponding(vsum, runnable, Method.class);

        VSUMRunner.deleteAndPropagate(vsum, runnable);

        assertFalse(VSUMRunner.isAlive(method),
                "Method must be removed when its Runnable is deleted");
    }

    // ── E9 / E10 / E11 — Label ↔ Message / Constant ──────────────────────────

    @Test
    @DisplayName("E9 – Non-constant Label created → Message in Module.typedElements")
    void e9_nonConstantLabel_messageCreated(@TempDir Path tempDir) {
        VirtualModel vsum = VSUMRunner.createDefaultVirtualModel(tempDir);
        Component comp    = VSUMRunner.addComponent(vsum, tempDir, "SpeedSensor");
        Label label       = VSUMRunner.addLabel(vsum, comp, "vehicleSpeed", false);

        Message message = VSUMRunner.getCorresponding(vsum, label, Message.class);
        Module module   = VSUMRunner.getCorresponding(vsum, comp,  Module.class);

        assertNotNull(message, "Message must be created for non-constant Label");
        assertEquals("vehicleSpeed", message.getName());
        assertTrue(module.getTypedElements().contains(message),
                "Message must be in Module.typedElements");

        // Rule 1 footnote *: every typedElement inside a Module must be a Message
        module.getTypedElements().forEach(te ->
                assertInstanceOf(Message.class, te,
                        "all typedElements must be Message instances (Table 5.1 footnote *)"));
    }

    @Test
    @DisplayName("E10 – Constant Label created → Constant in Module.typedElements")
    void e10_constantLabel_asemConstantCreated(@TempDir Path tempDir) {
        VirtualModel vsum = VSUMRunner.createDefaultVirtualModel(tempDir);
        Component comp    = VSUMRunner.addComponent(vsum, tempDir, "Calibration");
        Label label       = VSUMRunner.addLabel(vsum, comp, "MAX_TORQUE", true);

        Constant constant = VSUMRunner.getCorresponding(vsum, label, Constant.class);
        Module module     = VSUMRunner.getCorresponding(vsum, comp,  Module.class);

        assertNotNull(constant, "Constant must be created for constant Label");
        assertEquals("MAX_TORQUE", constant.getName());
        assertTrue(module.getTypedElements().contains(constant),
                "Constant must be in Module.typedElements");
    }

    @Test
    @DisplayName("E11 – Non-constant Label deleted → Message is removed")
    void e11_nonConstantLabelDeleted_messageRemoved(@TempDir Path tempDir) {
        VirtualModel vsum = VSUMRunner.createDefaultVirtualModel(tempDir);
        Component comp    = VSUMRunner.addComponent(vsum, tempDir, "RPMSensor");
        Label label       = VSUMRunner.addLabel(vsum, comp, "engineRPM", false);
        Message msg       = VSUMRunner.getCorresponding(vsum, label, Message.class);

        VSUMRunner.deleteAndPropagate(vsum, label);

        assertFalse(VSUMRunner.isAlive(msg),
                "Message must be removed when its Label is deleted");
    }

    @Test
    @DisplayName("E11 – Constant Label deleted → Constant is removed")
    void e11_constantLabelDeleted_asemConstantRemoved(@TempDir Path tempDir) {
        VirtualModel vsum = VSUMRunner.createDefaultVirtualModel(tempDir);
        Component comp    = VSUMRunner.addComponent(vsum, tempDir, "LimitConfig");
        Label label       = VSUMRunner.addLabel(vsum, comp, "MAX_RPM", true);
        Constant constant = VSUMRunner.getCorresponding(vsum, label, Constant.class);

        VSUMRunner.deleteAndPropagate(vsum, label);

        assertFalse(VSUMRunner.isAlive(constant),
                "Constant must be removed when its Label is deleted");
    }

    // ── E14 / E15 / E16 / E17 — type definitions ─────────────────────────────

    @Test
    @DisplayName("E14 – BaseTypeDefinition size=1 → BooleanType created")
    void e14_sizeOne_booleanTypeCreated(@TempDir Path tempDir) {
        VirtualModel vsum       = VSUMRunner.createDefaultVirtualModel(tempDir);
        BaseTypeDefinition btd  = VSUMRunner.addBaseTypeDefinition(vsum, tempDir, "LogType", 1, null);

        BooleanType bt = VSUMRunner.getCorresponding(vsum, btd, BooleanType.class);
        assertNotNull(bt, "BooleanType must be created for size=1 BTD");
        assertEquals("LogType", bt.getName());
    }

    @Test
    @DisplayName("E15 – BaseTypeDefinition size=8 unsigned alias → UnsignedDiscreteType")
    void e15_size8_unsigned_udiscCreated(@TempDir Path tempDir) {
        VirtualModel vsum      = VSUMRunner.createDefaultVirtualModel(tempDir);
        BaseTypeDefinition btd = VSUMRunner.addBaseTypeDefinition(vsum, tempDir, "uint8", 8, "u8");

        assertNotNull(VSUMRunner.getCorresponding(vsum, btd, UnsignedDiscreteType.class),
                "UnsignedDiscreteType must be created for unsigned size=8 BTD");
    }

    @Test
    @DisplayName("E15 – BaseTypeDefinition size=16 signed alias → SignedDiscreteType")
    void e15_size16_signed_sdiscCreated(@TempDir Path tempDir) {
        VirtualModel vsum      = VSUMRunner.createDefaultVirtualModel(tempDir);
        BaseTypeDefinition btd = VSUMRunner.addBaseTypeDefinition(vsum, tempDir, "sint16", 16, "sint16");

        assertNotNull(VSUMRunner.getCorresponding(vsum, btd, SignedDiscreteType.class),
                "SignedDiscreteType must be created for signed size=16 BTD");
    }

    @Test
    @DisplayName("E16 – BaseTypeDefinition size=32 float alias → ContinuousType (not DiscreteType)")
    void e16_size32_floatAlias_continuousCreated(@TempDir Path tempDir) {
        VirtualModel vsum      = VSUMRunner.createDefaultVirtualModel(tempDir);
        // size=32 is in both R8 and R9 — the float alias resolves to R9
        BaseTypeDefinition btd = VSUMRunner.addBaseTypeDefinition(vsum, tempDir, "float32", 32, "float32");

        assertNotNull(VSUMRunner.getCorresponding(vsum, btd, ContinuousType.class),
                "ContinuousType must be created when alias contains 'float'");
        assertNull(VSUMRunner.getCorresponding(vsum, btd, UnsignedDiscreteType.class),
                "DiscreteType must NOT be created for float32");
    }

    @Test
    @DisplayName("E16 – BaseTypeDefinition size=64 → ContinuousType")
    void e16_size64_continuousCreated(@TempDir Path tempDir) {
        VirtualModel vsum      = VSUMRunner.createDefaultVirtualModel(tempDir);
        BaseTypeDefinition btd = VSUMRunner.addBaseTypeDefinition(vsum, tempDir, "double64", 64, "double64");

        assertNotNull(VSUMRunner.getCorresponding(vsum, btd, ContinuousType.class));
    }

    @Test
    @DisplayName("E17 – Array inside DataTypeDefinition → ComposedType created")
    void e17_array_composedTypeCreated(@TempDir Path tempDir) {
        VirtualModel vsum      = VSUMRunner.createDefaultVirtualModel(tempDir);
        DataTypeDefinition dtd = VSUMRunner.addArrayTypeDefinition(vsum, tempDir, "SpeedBuffer", 10);

        assertNotNull(VSUMRunner.getCorresponding(vsum, dtd, ComposedType.class),
                "ComposedType must be created for Array DataTypeDefinition");
    }

    // ── P1 — Component.name → Module.name ────────────────────────────────────

    @Test
    @DisplayName("P1 – Component.name changed → Module.name updated")
    void p1_componentRenamed_moduleNameUpdated(@TempDir Path tempDir) {
        VirtualModel vsum = VSUMRunner.createDefaultVirtualModel(tempDir);
        Component comp    = VSUMRunner.addComponent(vsum, tempDir, "OldName");

        comp.setName("NewName");
        vsum.propagateChange(comp);

        Module module = VSUMRunner.getCorresponding(vsum, comp, Module.class);
        assertEquals("NewName", module.getName(),
                "Module.name must follow Component.name after rename");
    }

    // ── P3 — Runnable.name → Method.name ─────────────────────────────────────

    @Test
    @DisplayName("P3 – Runnable.name changed → Method.name updated")
    void p3_runnableRenamed_methodNameUpdated(@TempDir Path tempDir) {
        VirtualModel vsum = VSUMRunner.createDefaultVirtualModel(tempDir);
        Component comp    = VSUMRunner.addComponent(vsum, tempDir, "ECU");
        Runnable runnable = VSUMRunner.addRunnable(vsum, comp, "oldRoutine");

        runnable.setName("newRoutine");
        vsum.propagateChange(runnable);

        Method method = VSUMRunner.getCorresponding(vsum, runnable, Method.class);
        assertEquals("newRoutine", method.getName());
    }

    // ── P5 — Label.name → Message.name / Constant.name ───────────────────────

    @Test
    @DisplayName("P5 – Non-constant Label.name changed → Message.name updated")
    void p5_variableLabelRenamed_messageNameUpdated(@TempDir Path tempDir) {
        VirtualModel vsum = VSUMRunner.createDefaultVirtualModel(tempDir);
        Component comp    = VSUMRunner.addComponent(vsum, tempDir, "ABS");
        Label label       = VSUMRunner.addLabel(vsum, comp, "oldSignal", false);

        label.setName("newSignal");
        vsum.propagateChange(label);

        Message msg = VSUMRunner.getCorresponding(vsum, label, Message.class);
        assertEquals("newSignal", msg.getName());
    }

    @Test
    @DisplayName("P5 – Constant Label.name changed → Constant.name updated")
    void p5_constantLabelRenamed_asemConstantNameUpdated(@TempDir Path tempDir) {
        VirtualModel vsum = VSUMRunner.createDefaultVirtualModel(tempDir);
        Component comp    = VSUMRunner.addComponent(vsum, tempDir, "Config");
        Label label       = VSUMRunner.addLabel(vsum, comp, "OLD_LIMIT", true);

        label.setName("NEW_LIMIT");
        vsum.propagateChange(label);

        Constant constant = VSUMRunner.getCorresponding(vsum, label, Constant.class);
        assertEquals("NEW_LIMIT", constant.getName());
    }

    // ── P8 / P9 — Label.constant flag flipped ────────────────────────────────

    @Test
    @DisplayName("P8 – Label.constant flipped false→true: Message replaced by Constant")
    void p8_labelFlippedToConstant_messageReplacedByConstant(@TempDir Path tempDir) {
        VirtualModel vsum = VSUMRunner.createDefaultVirtualModel(tempDir);
        Component comp    = VSUMRunner.addComponent(vsum, tempDir, "Sensor");
        Label label       = VSUMRunner.addLabel(vsum, comp, "reading", false);

        assertNotNull(VSUMRunner.getCorresponding(vsum, label, Message.class),
                "initial state must have a Message");

        label.setConstant(true);
        vsum.propagateChange(label);

        assertNull(VSUMRunner.getCorresponding(vsum, label, Message.class),
                "Message must be removed when Label becomes constant");
        assertNotNull(VSUMRunner.getCorresponding(vsum, label, Constant.class),
                "Constant must be created when Label becomes constant");
        assertEquals("reading",
                VSUMRunner.getCorresponding(vsum, label, Constant.class).getName());
    }

    @Test
    @DisplayName("P9 – Label.constant flipped true→false: Constant replaced by Message")
    void p9_labelFlippedToVariable_constantReplacedByMessage(@TempDir Path tempDir) {
        VirtualModel vsum = VSUMRunner.createDefaultVirtualModel(tempDir);
        Component comp    = VSUMRunner.addComponent(vsum, tempDir, "Config");
        Label label       = VSUMRunner.addLabel(vsum, comp, "threshold", true);

        assertNotNull(VSUMRunner.getCorresponding(vsum, label, Constant.class));

        label.setConstant(false);
        vsum.propagateChange(label);

        assertNull(VSUMRunner.getCorresponding(vsum, label, Constant.class),
                "Constant must be removed when Label becomes non-constant");
        assertNotNull(VSUMRunner.getCorresponding(vsum, label, Message.class),
                "Message must be created when Label becomes non-constant");
    }

    // ── P11 — BaseTypeDefinition size changed → ASEM primitive replaced ───────

    @Test
    @DisplayName("P11 – BTD size changed 8→64: UnsignedDiscreteType replaced by ContinuousType")
    void p11_btdSizeChanged_primitiveTypeReplaced(@TempDir Path tempDir) {
        VirtualModel vsum      = VSUMRunner.createDefaultVirtualModel(tempDir);
        BaseTypeDefinition btd = VSUMRunner.addBaseTypeDefinition(vsum, tempDir, "dynType", 8, "u8");

        assertNotNull(VSUMRunner.getCorresponding(vsum, btd, UnsignedDiscreteType.class),
                "should start as UnsignedDiscreteType");

        // change size to 64 and swap alias to double
        var ds = AmaltheaFactory.eINSTANCE.createDataSize();
        ds.setValue(BigInteger.valueOf(64));
        ds.setUnit(DataSizeUnit.BIT);
        btd.setSize(ds);
        btd.getAliases().clear();
        var a = AmaltheaFactory.eINSTANCE.createAlias();
        a.setAlias("double64");
        a.setTarget("ASEM");
        btd.getAliases().add(a);
        vsum.propagateChange(btd);

        assertNull(VSUMRunner.getCorresponding(vsum, btd, UnsignedDiscreteType.class),
                "UnsignedDiscreteType must be removed after size change");
        assertNotNull(VSUMRunner.getCorresponding(vsum, btd, ContinuousType.class),
                "ContinuousType must be created after size change to 64 with float alias");
    }

    // ── P12 — Array.numberElements changed ───────────────────────────────────

    @Test
    @DisplayName("P12 – Array.numberElements changed → ComposedType size property updated")
    void p12_arraySizeChanged_composedTypeSizeUpdated(@TempDir Path tempDir) {
        VirtualModel vsum      = VSUMRunner.createDefaultVirtualModel(tempDir);
        DataTypeDefinition dtd = VSUMRunner.addArrayTypeDefinition(vsum, tempDir, "Buffer", 5);
        ComposedType ct        = VSUMRunner.getCorresponding(vsum, dtd, ComposedType.class);
        assertNotNull(ct);

        // change element count
        Array arr = (Array) dtd.getDataType();
        arr.setNumberElements(20);
        vsum.propagateChange(arr);

        // size is stored as a custom property keyed "size" — see P12 in reactions
        var sizeProp = ct.getCustomProperties().stream()
                .filter(p -> "size".equals(p.getKey()))
                .findFirst();
        assertTrue(sizeProp.isPresent(), "size custom property must exist");
        assertEquals("20",
                ((StringObject) sizeProp.get().getValue()).getValue(),
                "size property must reflect the new element count");
    }

    // ── Rule 1 sub-constraint checks (footnotes * and **) ────────────────────

    @Test
    @DisplayName("R1** – All Methods in Module must have null returnType and no parameters")
    void r1_moduleMethodsMustBeVoid(@TempDir Path tempDir) {
        VirtualModel vsum = VSUMRunner.createDefaultVirtualModel(tempDir);
        Component comp    = VSUMRunner.addComponent(vsum, tempDir, "ECM");
        VSUMRunner.addRunnable(vsum, comp, "runA");
        VSUMRunner.addRunnable(vsum, comp, "runB");

        Module module = VSUMRunner.getCorresponding(vsum, comp, Module.class);
        module.getMethods().forEach(m -> {
            assertNull(m.getReturnType(),
                    "returnType must be null on every Module Method (footnote **)");
            assertTrue(m.getParameters().isEmpty(),
                    "parameters must be empty on every Module Method (footnote **)");
        });
    }

    @Test
    @DisplayName("R1* – All typedElements in Module must be Message instances")
    void r1_moduleTypedElementsMustBeMessages(@TempDir Path tempDir) {
        VirtualModel vsum = VSUMRunner.createDefaultVirtualModel(tempDir);
        Component comp    = VSUMRunner.addComponent(vsum, tempDir, "SensorHub");
        VSUMRunner.addLabel(vsum, comp, "signal1", false);
        VSUMRunner.addLabel(vsum, comp, "signal2", false);

        Module module = VSUMRunner.getCorresponding(vsum, comp, Module.class);
        module.getTypedElements().forEach(te ->
                assertInstanceOf(Message.class, te,
                        "all typedElements must be Message instances (footnote *)"));
    }
}
