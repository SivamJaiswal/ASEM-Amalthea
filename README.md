AMALTHEA  ↔  ASEM
Semantic Overlaps and Consistency Preservation Rules

1  Introduction
This document describes the semantic overlap between the AMALTHEA metamodel (APP4MC 3.3.0) and the ASEM metamodel (KIT), and specifies the resulting consistency preservation rules.
 
Semantic overlap exists when two metamodels, developed independently for different purposes, describe conceptually equivalent or corresponding information. When both models are kept in a Virtual Single Underlying Model (VSUM) via the Vitruv framework, any change in one model must be automatically propagated to keep the other consistent.
 

2  Metamodel Anchor Points

2.1  AMALTHEA elements in scope
Component — in CompOnentsModel.components; has name, runnables[], labels[], ports[]
Task / ISR — in SWModel.tasks / .isrs; key refs: stimuli[], activityGraph
Runnable — in SWModel.runnables; execution described by activityGraph.items
Label (constant=true) — constant data; maps to ASEM parameter/constant kinds
Label (constant=false) — mutable data; maps to ASEM variable kinds
BaseTypeDefinition — primitive type carrier; disambiguation via size.value in bits
Array — nested inside DataTypeDefinition.dataType; holds numberElements
 
2.2  ASEM elements in scope
Module / Class — top-level component classifiers; methods[] + typedElements[]
Method — named operation; returnType (ReturnType or null) + parameters[]
Parameter / Constant / Variable / Message — data exchange elements, all extend TypedElement
BooleanType / UnsignedDiscreteType / SignedDiscreteType / ContinuousType — primitives under asem.primitivetypes
 
3  Semantic Overlap Summary
The ten correspondences below cover every row in Table 5.1. Each is expanded fully in Section 4.
 
Rule	AMALTHEA	ASEM  /  Condition
R1	Component	Module  —  no OCL filter
R2	Task	Task / InitTask / SoftwareTask / TimeTableTask
R3	ISR	InterruptTask  —  no OCL filter
R4	Runnable	Method  —  returnType=null AND parameters.size=0
R5	Label  (constant=true)	Parameter / Constant / Systemconstant  —  self.constant=true
R6	Label  (constant=false)	Variable / Message / Argument / Input / Output  —  self.constant=false
R7	BaseTypeDefinition	BooleanType  —  self.size=1
R8	BaseTypeDefinition	UnsignedDiscreteType / SignedDiscreteType  —  Set{8,16,32}->includes(self.size)
R9	BaseTypeDefinition	ContinuousType  —  Set{32,64}->includes(self.size)
R10	Array	ComposedType  —  no OCL filter
 
4  Correspondence Rules in Detail
Each rule gives the class correspondence, OCL invariants, the attribute-level mapping, and notes on sub-constraints from the paper's footnotes.
 
Rule 1  —  Component  ↔  Module
AMALTHEA: Component
ASEM: Module  in asem.classifiers
No OCL filter — every Component maps to exactly one Module
 
Attribute correspondences

Component.name	Module.name
Component.runnables[]	Module.methods[]  (see Rule 4)
Component.labels[]	Module.typedElements[]  (see footnote *)
 
Sub-constraint  *  —  Module elements must be Messages
Context asem::classifiers::Module
  Inv: self->collect(elements)->oclIsTypeOf(Message)
  -- Only Message instances in typedElements correspond to Component labels
 
Sub-constraint  **  —  Module methods must be void with no arguments
Context asem::classifiers::Module
  Inv: self->collect(methods)->ret.oclIsUndefined()
  Inv: self->collect(methods)->collect(arguments)->size()=0
 
Every Method inside the Module must have returnType = null
Every such Method must have an empty parameters list
 
Rule 2  —  Task  ↔  Task / InitTask / SoftwareTask / TimeTableTask
AMALTHEA: Task (extends AbstractProcess)
ASEM: Method — one of the four ASCET task kinds
No OCL filter at the class level
 
Attribute correspondences
M
Task.name	Method.name
Task priority  (via TaskAllocation)	Method priority field
Task activityGraph → RunnableCalls  ****	Method.processes[]
 
Sub-constraint  ***  —  processes must be void with no arguments
Context Task
  Inv: self->collect(processes)->ret.oclIsUndefined()
  Inv: self->collect(processes)->collect(arguments)->size()=0
 
Footnote  ****  —  called runnables
Refers to all Runnable instances reachable via the Task's activityGraph.items of type RunnableCall. These become entries in the corresponding Method's processes[] list on the ASEM side (see section 4.2.3.1 of the paper).
Note  Priority storage in AMALTHEA 3.3 is indirect — it lives as a SchedulingParameter on TaskAllocation, not a direct attribute of Task. Navigation: MappingModel → taskAllocation → schedulingParameters. Confirm approach with Benedikt before implementing.
 
Rule 3  —  ISR  ↔  InterruptTask
AMALTHEA: ISR (extends AbstractProcess)
ASEM: Method typed as InterruptTask
No OCL filter — every ISR maps to exactly one InterruptTask
 
Attribute correspondences
M
ISR.name	Method.name
ISR priority  (via ISRAllocation)	Method priority field
ISR activityGraph → RunnableCalls  ****	Method.processes[]
 
The same sub-constraint *** and footnote **** from Rule 2 apply here without change.
 
Rule 4  —  Runnable  ↔  Method
AMALTHEA: Runnable
ASEM: Method — must be void with no parameters
 
OCL invariants
Context asem::dataexchange::Method
  Inv void_return:  self.returnType.oclIsUndefined()
  Inv no_args:      self.parameters->size() = 0
 
Attribute correspondences

Runnable.name	Method.name
 
A Runnable models a void procedure with no formal parameters at the interface level
RunnableParameter instances in AMALTHEA do not propagate to ASEM Method parameters — the OCL constraint explicitly forbids parameters on the Method side
 
Rule 5  —  Label (constant=true)  ↔  Parameter / Constant / Systemconstant
OCL guard
Context amalthea::Label
  Inv is_constant:  self.constant = true
 
Attribute correspondences
M
Label.name	Parameter.name  /  Constant.name
Label.dataType	Parameter.type  /  Constant.type  (see Rules 7–9 for type mapping)
 
A constant Label maps to Constant when it holds a fixed scalar value
It maps to Parameter when it represents a tuneable / calibration parameter
Note  'Systemconstant' from Table 5.1 does not appear as a named EClass in the provided ASEM ecore. It may be a Constant with a specific naming convention. Confirm with Benedikt.
 
Rule 6  —  Label (constant=false)  ↔  Variable / Message / Argument / Input / Output
OCL guard
Context amalthea::Label
  Inv is_variable:  self.constant = false
 
Attribute correspondences

Label.name	Variable.name  /  Message.name
Label.dataType	Variable.type  /  Message.type  (→ TypedElement.type)
 
Variable has readable and writable boolean flags; Message extends Variable
Input / Output / Argument are role distinctions — the base type is still Variable or Message
The Rule 1 sub-constraint * further restricts this: within a Module, only Message instances (not plain Variable) correspond to Labels
 
Rule 7  —  BaseTypeDefinition (size=1)  ↔  BooleanType
OCL guard
Context amalthea::BaseTypeDefinition
  Inv is_bool:  self.size.value = 1
 
Attribute correspondences

BaseTypeDefinition.name	BooleanType.name  (e.g. 'bool', 'boolean')
 
A BaseTypeDefinition of exactly 1 bit maps to ASEM BooleanType
ASCET calls this 'Log' (logical / boolean); single-bit representation
 
Rule 8  —  BaseTypeDefinition (size∈{8,16,32})  ↔  UnsignedDiscreteType / SignedDiscreteType
OCL guard
Context amalthea::BaseTypeDefinition
  Inv is_discrete:  Set{8,16,32}->includes(self.size.value)
 
Attribute correspondences

BaseTypeDefinition.name	UnsignedDiscreteType.name  /  SignedDiscreteType.name
BaseTypeDefinition.aliases	Signed vs. unsigned disambiguation (prefix u = unsigned, s = signed)
 
UnsignedDiscreteType — for unsigned integer base types (Udisc)
SignedDiscreteType — for signed integer base types (Sdisc)
Note  Size 32 appears in both Rule 8 and Rule 9. Tie-breaker: if the alias contains 'float' or 'double' → Rule 9 (ContinuousType); otherwise → Rule 8 (DiscreteType). E.g. 'sint32'/'uint32' → DiscreteType; 'float32' → ContinuousType.
 
Rule 9  —  BaseTypeDefinition (size∈{32,64})  ↔  ContinuousType
OCL guard
Context amalthea::BaseTypeDefinition
  Inv is_continuous:  Set{32,64}->includes(self.size.value)
 
Attribute correspondences

BaseTypeDefinition.name	ContinuousType.name  (e.g. 'float32', 'double64')
BaseTypeDefinition.aliases	Alias containing 'float' or 'double' triggers this mapping
 
Maps to ContinuousType in asem.primitivetypes — covers floating-point / real-valued types
Overlap with Rule 8 at size=32 resolved by alias — see note in Rule 8
 
Rule 10  —  Array  ↔  ComposedType
AMALTHEA: Array — embedded in DataTypeDefinition.dataType
ASEM: ComposedType with primitiveType reference
No OCL filter — every Array maps to exactly one ComposedType
 
Attribute correspondences

Array.numberElements	ComposedType size  (element count)
Array.dataType	ComposedType.primitiveType  (element type)
Note  A dedicated ArrayType class is referenced in Table 5.1 but does not appear in the provided ASEM ecore. ComposedType with a primitiveType reference is the closest structural equivalent. Confirm with Benedikt.
 
5  OCL Invariant Summary
The OCL expressions below are the formal constraints from Table 5.1 that must be enforced by the consistency preservation mechanism.
 
-- Rule 4: Runnable ↔ Method
Context asem::dataexchange::Method
  Inv void_return:  self.returnType.oclIsUndefined()
  Inv no_args:      self.parameters->size() = 0
 
-- Rule 5: Label (constant) ↔ Constant
Context amalthea::Label
  Inv constant_label:  self.constant = true
 
-- Rule 6: Label (variable) ↔ Variable / Message
Context amalthea::Label
  Inv variable_label:  self.constant = false
 
-- Rule 7: BaseTypeDefinition ↔ BooleanType
Context amalthea::BaseTypeDefinition
  Inv bool_type:  self.size.value = 1
 
-- Rule 8: BaseTypeDefinition ↔ DiscreteType
Context amalthea::BaseTypeDefinition
  Inv discrete_type:  Set{8,16,32}->includes(self.size.value)
 
-- Rule 9: BaseTypeDefinition ↔ ContinuousType
Context amalthea::BaseTypeDefinition
  Inv continuous_type:  Set{32,64}->includes(self.size.value)
 
-- Rule 1 sub-constraint (*): Module elements must be Messages
Context asem::classifiers::Module
  Inv elements_are_messages:
      self.typedElements->forAll(e | e.oclIsTypeOf(asem::dataexchange::Message))
 
-- Rule 1 sub-constraint (**): Module methods must be void with no args
Context asem::classifiers::Module
  Inv methods_void:     self.methods->forAll(m | m.returnType.oclIsUndefined())
  Inv methods_no_args:  self.methods->forAll(m | m.parameters->size() = 0)
 
6  Consistency Preservation Rules
The following rules define what action must be taken in one model when a change occurs in the other. Rules are grouped into four categories: Existence (E), Property (P), Structural (S), and Completeness (C).
 
6.1  Existence Rules  —  element creation and deletion
When an element is created or deleted in one model, the corresponding element is created or deleted in the other.
 
E1  AMALTHEA Component created  →  create ASEM Module with same name; add correspondence
E2  AMALTHEA Component deleted  →  delete corresponding ASEM Module; remove correspondence
E3  ASEM Module created  →  create AMALTHEA Component with same name; add correspondence
E4  ASEM Module deleted  →  delete corresponding AMALTHEA Component; remove correspondence
 
E5  AMALTHEA Runnable created  →  create ASEM Method (returnType=null, parameters=[]) with same name; add to Module.methods
E6  AMALTHEA Runnable deleted  →  delete corresponding ASEM Method from Module.methods
E7  ASEM Method created (void, no params)  →  create AMALTHEA Runnable with same name; add to Component.runnables
E8  ASEM Method deleted  →  delete corresponding AMALTHEA Runnable
 
E9  AMALTHEA Label (constant=false) created  →  create ASEM Message with same name; add to Module.typedElements
E10  AMALTHEA Label (constant=true) created  →  create ASEM Constant with same name; add to Module.typedElements
E11  AMALTHEA Label deleted  →  delete corresponding ASEM Message or Constant
E12  ASEM Message created  →  create AMALTHEA Label (constant=false) with same name
E13  ASEM Constant created  →  create AMALTHEA Label (constant=true) with same name
 
E14  AMALTHEA BaseTypeDefinition (size=1) created  →  create ASEM BooleanType with same name
E15  AMALTHEA BaseTypeDefinition (size∈{8,16,32}) created  →  create UnsignedDiscreteType or SignedDiscreteType (disambiguate via alias)
E16  AMALTHEA BaseTypeDefinition (size∈{32,64}, float alias) created  →  create ASEM ContinuousType with same name
E17  AMALTHEA Array created  →  create ASEM ComposedType; set size from numberElements
 
6.2  Property Rules  —  attribute value changes
When a shared attribute changes in one model, the corresponding attribute is updated in the other.
 
P1  Component.name changed  →  set Module.name = new name
P2  Module.name changed  →  set Component.name = new name
P3  Runnable.name changed  →  set Method.name = new name
P4  Method.name changed  →  set Runnable.name = new name
P5  Label.name changed  →  set corresponding Message.name or Constant.name = new name
P6  Message.name changed  →  set Label.name = new name
P7  Constant.name changed  →  set Label.name = new name
 
P8  Label.constant changed false → true  →  replace ASEM Message with Constant; update correspondence
P9  Label.constant changed true → false  →  replace ASEM Constant with Message; update correspondence
P10  Label.dataType changed  →  update Message.type / Constant.type to corresponding ASEM PrimitiveType
P11  BaseTypeDefinition.size changed  →  replace ASEM PrimitiveType with type matching new size (re-apply Rules 7–9)
P12  Array.numberElements changed  →  update ComposedType size attribute
 
6.3  Structural Rules  —  containment and reference changes
When elements are added to or removed from containment relationships, the corresponding containment in the other model is also updated.
 
S1  Runnable added to Component.runnables  →  add corresponding ASEM Method to Module.methods
S2  Runnable removed from Component.runnables  →  remove corresponding ASEM Method from Module.methods
S3  Method added to Module.methods  →  add corresponding AMALTHEA Runnable to Component.runnables
S4  Method removed from Module.methods  →  remove corresponding AMALTHEA Runnable from Component.runnables
 
S5  Label added to Component.labels  →  add corresponding ASEM Message (or Constant) to Module.typedElements
S6  Label removed from Component.labels  →  remove corresponding ASEM element from Module.typedElements
S7  Message added to Module.typedElements  →  add corresponding AMALTHEA Label (constant=false) to Component.labels
S8  Constant added to Module.typedElements  →  add corresponding AMALTHEA Label (constant=true) to Component.labels
 
6.4  Completeness Rules  —  model-wide invariants
Conditions that must hold globally for the two models to be considered consistent.
 
C1  Every Component has a corresponding Module, and vice versa
C2  Every Runnable in a Component has a corresponding void no-param Method in the corresponding Module, and vice versa
C3  Every Label (constant=false) in a Component has a corresponding Message in Module.typedElements, and vice versa
C4  Every Label (constant=true) in a Component has a corresponding Constant in Module.typedElements, and vice versa
C5  Every BaseTypeDefinition (size=1) has a corresponding BooleanType, and vice versa
C6  Every BaseTypeDefinition (size∈{8,16,32}) has a corresponding DiscreteType, and vice versa
C7  Every BaseTypeDefinition (size∈{32,64}, float alias) has a corresponding ContinuousType, and vice versa
C8  Every Array has a corresponding ComposedType; Array.numberElements must equal ComposedType size, and vice versa
